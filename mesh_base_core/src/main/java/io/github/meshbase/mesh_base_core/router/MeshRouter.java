package io.github.meshbase.mesh_base_core.router;

import android.graphics.Mesh;
import android.util.Log;

import androidx.annotation.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.meshbase.mesh_base_core.global_interfaces.ConnectionHandler;
import io.github.meshbase.mesh_base_core.global_interfaces.ConnectionHandlerListener;
import io.github.meshbase.mesh_base_core.global_interfaces.ConnectionHandlersEnum;
import io.github.meshbase.mesh_base_core.global_interfaces.Device;
import io.github.meshbase.mesh_base_core.global_interfaces.SendError;


public class MeshRouter {

    private static final int ROUTE_DISCOVERY_TIMEOUT = 5000; // ms
    private final Map<ConnectionHandlersEnum, ConnectionHandler> connectionHandlers;
    private final MessageForwarding messageForwarding;
    private final NeighborDiscovery neighborDiscovery;
    private final RoutingTable routingTable;
    private final RouteDiscovery routeDiscovery;
    private final Deduplication deduplication;

    private final UUID myUUID;

    private static final String TAG = "Mesh_Router";

    Router.RouterListener routerListener = new Router.RouterListener() {
        @Override
        public void onData(MeshProtocol<?> protocol, Device neighbor) {
            Log.d(TAG, "Received data from " + neighbor.name);
        }

        @Override
        public void onError(Exception exception) {
            Log.d(TAG, "Router error" + exception.getMessage());
        }
    };
    private final ConcurrentMap<Integer, SendListener> messageListeners = new ConcurrentHashMap<>();
    // Constants
    private static final UUID BROADCAST_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    public MeshRouter(Map<ConnectionHandlersEnum, ConnectionHandler> connectionHandlers, MessageForwarding messageForwarding, NeighborDiscovery neighborDiscovery, RoutingTable routingTable, RouteDiscovery routeDiscovery, Deduplication deduplication, UUID myUUID) {
        this.connectionHandlers = connectionHandlers;
        this.messageForwarding = messageForwarding;
        this.neighborDiscovery = neighborDiscovery;
        this.routingTable = routingTable;
        this.routeDiscovery = routeDiscovery;
        this.deduplication = deduplication;
        this.myUUID = myUUID;

        for (ConnectionHandler handler : connectionHandlers.values()) {
            handler.subscribe(
                    new ConnectionHandlerListener() {
                        @Override
                        public void onDataReceived(Device device, byte[] data) {
//                            handleOnData(device, data);
                        }
                    }
            );
        }
    }


    // Main entry point for sending data
    public void sendData(MeshProtocol<?> protocol, SendListener listener) {
        // Store listener before any sending occurs
        if (listener != null) {
            messageListeners.put(protocol.messageId, listener);
        }

        if (protocol.destination.equals(BROADCAST_UUID)) {
            handleBroadcast(protocol);
        } else if (protocol.destination.equals(myUUID)) {
            handleLocalMessage(protocol);
        } else {
            messageForwarding.forwardMessage(protocol);
        }
    }

    private void processIncomingData(Device sender, byte[] data) {
        try {

            MeshProtocol<?> protocol = MeshProtocol.decode(data);

            // Deduplication check
            if (deduplication.isDuplicate(protocol.messageId, protocol.sender)) {
                return;
            }

            // Route protocol types to components
            switch (protocol.getByteType()) {
                case SEND_MESSAGE:
                    neighborDiscovery.createPingMessage(protocol.destination);
                    break;
                case RREQ:
                case RREP:
                    routeDiscovery.handleControlMessage(protocol, sender);
                    break;
                case ACK:
                    handleAck(protocol);
                    break;
                default:
                    messageForwarding.handleDataMessage(protocol, sender);
            }
        } catch (Exception e) {
            routerListener.onError(e);
        }
    }

    // Broadcast handling (TTL-based flood)
    private void handleBroadcast(MeshProtocol<?> protocol) {
        protocol.remainingHops--;  // Decrement TTL
        if (protocol.remainingHops <= 0) return;

        // Deliver locally
        routerListener.onData(protocol, null);

        // Re-broadcast
        try {
            for (ConnectionHandler handler : connectionHandlers.values()) {
                if (handler.isOn()) {
                    handler.send(protocol.encode());
                }
            }
            notifyAck(protocol.messageId);
        } catch (SendError e) {
            notifyError(protocol.messageId, e);
        }
    }

    private void handleLocalMessage(MeshProtocol<?> protocol) {
        routerListener.onData(protocol, null);
        sendAck(protocol.sender, protocol.messageId);
        notifyAck(protocol.messageId);
    }

    private void sendAck(UUID destination, int messageId) {
        AckMessageBody ackMessageBody = new AckMessageBody("OK");
        MeshProtocol<AckMessageBody> ackData = new ConcreteMeshProtocol<>(
                0, // Message Type is ACK
                100, // TTL
                messageId,
                myUUID,
                destination,
                ackMessageBody
        );
        try {
            for (ConnectionHandler handler : connectionHandlers.values()) {
                if (handler.isOn()) {
                    handler.send(ackData.encode());
                    return;
                }
            }
        } catch (SendError e) {
            Log.e("Router", "Error sending ack: " + messageId);
        }
    }

    // Listener notification methods
    private void notifyAck(int messageId) {
        SendListener listener = messageListeners.remove(messageId);
        if (listener != null) {
            listener.onAck();
        }
    }

    private void notifyError(int messageId, SendError error) {
        SendListener listener = messageListeners.remove(messageId);
        if (listener != null) {
            listener.onError(error);
        }
    }

    private void notifyResponse(int messageId, MeshProtocol<?> response) {
        SendListener listener = messageListeners.remove(messageId);
        if (listener != null) {
            listener.onResponse(response);
        }
    }

    private void handleAck(MeshProtocol<?> protocol) {
        if (protocol.destination.equals(myUUID)) {
            notifyAck(protocol.messageId);
        }
    }

    private class NeighborDiscovery {
        private final String TAG = "Mesh_Heartbeat";
        private final Map<ConnectionHandlersEnum, ConnectionHandler> handlers;
        private final RoutingTable routingTable;

        private final UUID deviceUUID;
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        NeighborDiscovery(Map<ConnectionHandlersEnum, ConnectionHandler> handlers,
                          RoutingTable routingTable,
                          UUID deviceUUID
        ) {
            this.handlers = handlers;
            this.routingTable = routingTable;
            this.deviceUUID = deviceUUID;
        }

        private void startPeriodicPings() {
            scheduler.scheduleWithFixedDelay(() -> {
                for (ConnectionHandler handler : handlers.values()) {
                    if (!handler.isOn()) continue;

                    for (Device neighbor : handler.getNeighbourDevices()) {
                        MeshProtocol<SendMessageBody> ping = createPingMessage(neighbor.uuid);
                        try {
                            handler.send(ping.encode());
                        } catch (SendError e) {
                            Log.e(TAG, "Failed to send ping message with error" + e.toString());
                            // TODO: Update routing table with the error
                        }
                    }
                }
            }, 0, 5, TimeUnit.SECONDS);
        }

        MeshProtocol<SendMessageBody> createPingMessage(UUID destination) {
            SendMessageBody pingMessage = new SendMessageBody(
                    1,
                    false,
                    "ping");

            return new ConcreteMeshProtocol<>(
                    1,
                    1,
                    0,
                    deviceUUID,
                    destination,
                    pingMessage
            );
        }
    }

    private static class Deduplication {
        private final ConcurrentHashMap<String, Long> seenMessages = new ConcurrentHashMap<>();
        private final long ttlMillis = TimeUnit.MINUTES.toMillis(5);

        boolean isDuplicate(int messageId, UUID sender) {
            String key = messageId + "|" + sender;
            long currentTime = System.currentTimeMillis();

            // Clean up old entry if expired
            Long timestamp = seenMessages.get(key);
            if (timestamp != null) {
                if (currentTime - timestamp < ttlMillis) {
                    return true;
                } else {
                    seenMessages.remove(key); // stale
                }
            }

            seenMessages.put(key, currentTime);
            return false;
        }
    }

    private class RoutingTable {
        private final ConcurrentMap<UUID, RouteEntry> routes = new ConcurrentHashMap<>();

        RoutingTable() {
            ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();
            cleaner.scheduleWithFixedDelay(this::cleanExpiredRoutes, 1, 1, TimeUnit.MINUTES);
        }

        void addRoute(UUID destination, UUID nextHop, int cost, long expiresAt) {
            routes.put(destination, new RouteEntry(nextHop, cost, expiresAt));
        }

        void addNeighbor(UUID neighborId, int cost, long expiresAt) {
            routes.put(neighborId, new RouteEntry(neighborId, cost, expiresAt));
        }

        Optional<RouteEntry> getRoute(UUID destination) {
            return Optional.ofNullable(routes.get(destination));
        }

        private void cleanExpiredRoutes() {
            long now = System.currentTimeMillis();
            routes.entrySet().removeIf(entry -> entry.getValue().expiresAt < now);
        }
    }

    private class RouteDiscovery {
        private final MeshRouter router;
        private final RoutingTable routingTable;
        private final Deduplication deduplication;
        private final UUID nodeId;
        private final Map<Integer, PendingRouteRequest> pendingRequests = new ConcurrentHashMap<>();
        private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        private final AtomicInteger rreqIdGenerator = new AtomicInteger(0);

        RouteDiscovery(MeshRouter router, RoutingTable routingTable, Deduplication deduplication, UUID nodeId) {
            this.router = router;
            this.routingTable = routingTable;
            this.deduplication = deduplication;
            this.nodeId = nodeId;
        }

        void discoverRoute(UUID destination, MeshProtocol<?> originalMessage) {
            int querySequenceNumber = rreqIdGenerator.incrementAndGet();
            PendingRouteRequest pending = new PendingRouteRequest(querySequenceNumber, destination, originalMessage);
            pendingRequests.put(querySequenceNumber, pending);

            // Broadcast RREQ
            MeshProtocol<RREQBody> rreq = createRREQ(querySequenceNumber, destination);
            for (ConnectionHandler handler : connectionHandlers.values()) {
                if (handler.isOn()) {
                    try {
                        handler.send(rreq.encode());
                    } catch (SendError e) {
                        Log.e("Mesh_Router_Discovery", "Failed to send a Request");
                    }
                }
            }

            // Set timeout
            scheduler.schedule(() -> {
                if (pendingRequests.remove(querySequenceNumber) != null) {
                    Log.w("Mesh_Router_Discovery", "Route discovery to " + destination + " failed (timeout)");

                }
            }, ROUTE_DISCOVERY_TIMEOUT, TimeUnit.MILLISECONDS);
        }

        MeshProtocol<RREQBody> createRREQ(int querySequenceNumber, UUID destination) {
            UUID querySequenceId = UUID.randomUUID();
            int MAX_ALLOWED_HOPS = 10;
            RREQBody body = new RREQBody(
                    querySequenceNumber,
                    querySequenceId,
                    myUUID,
                    0,
                    MAX_ALLOWED_HOPS
            );

            return new ConcreteMeshProtocol<>(
                    4,
                    8,
                    0,
                    myUUID,
                    destination,
                    body
            );
        }

        void handleControlMessage(MeshProtocol<?> protocol, Device sender) {
            if (protocol.getByteType() == ProtocolType.RREQ) {
                processRREQ(protocol, sender);
            } else if (protocol.getByteType() == ProtocolType.RREP) {
                processRREP(protocol, sender);
            }
        }

        private void processRREQ(MeshProtocol<?> protocol, Device sender) {
            RREQBody body = (RREQBody) protocol.body;

            // Check if we're the destination
            if (protocol.destination.equals(nodeId)) {
                sendRREP(protocol.sender, body.querySequenceId, sender);
            }
            // Check if we have fresh route
            else if (routingTable.getRoute(protocol.destination)
                    .filter(route -> route.expiresAt > System.currentTimeMillis())
                    .isPresent()) {
                sendRREP(protocol.sender, body.querySequenceId, sender);
            }
            // Re-broadcast RREQ
            else {
                routingTable.addRoute(protocol.sender, sender.uuid, 1,
                        System.currentTimeMillis() + 60000);
                broadcastRREQ(protocol);
            }
        }

        private void broadcastRREQ(MeshProtocol<?> protocol) {
            UUID rreqSender = protocol.sender;

            for (ConnectionHandler handler : connectionHandlers.values()) {
                List<Device> devices = handler.getNeighbourDevices();
                for (var nextNode : devices) {
                    if (handler.isOn() && nextNode.uuid.equals(rreqSender)) {
                        try {
                            handler.send(protocol.encode());
                        } catch (SendError e) {
                            Log.e("Mesh_Router_BroadcastRREQ", "Failed to forward RREQ to " + nextNode.uuid);
                        }
                    }
                }
            }
        }


        private void sendRREP(UUID destination, UUID queryId, Device nextHop) {
            List<UUID> routePath = new ArrayList<>();
            routePath.add(myUUID);
            RREPBody rrepBody = new RREPBody(
                    rreqIdGenerator.incrementAndGet(),            // You might need to pass actual Qseq here, adjust accordingly
                    queryId,
                    routePath
            );

            MeshProtocol<RREPBody> rrep = new ConcreteMeshProtocol<>(
                    5,
                    8,                    // remaining hops (can be max hops or TTL)
                    0,                    // messageId if you use one
                    nodeId,               // sender = current node
                    destination,          // destination = original source
                    rrepBody
            );

            try {
                for (ConnectionHandler handler : connectionHandlers.values()) {
                    List<Device> neighbors = handler.getNeighbourDevices();
                    for (var ignored : neighbors) {
                        handler.send(rrep.encode());
                    }
                }
            } catch (SendError e) {
                Log.e("Mesh_Router_SendRREP", "Failed to send RREP to " + nextHop.uuid);
            }
        }


        private void processRREP(MeshProtocol<?> protocol, Device sender) {
            RREPBody body = (RREPBody) protocol.body;
            PendingRouteRequest pending = pendingRequests.remove(body.querySequenceNumber);

            if (pending != null) {
                // Add route to routing table
                routingTable.addRoute(
                        pending.destination,
                        sender.uuid,
                        protocol.remainingHops,
                        System.currentTimeMillis() + 120000
                );

                // Forward original message
            } else {
                // Forward RREP towards source
                messageForwarding.forwardMessage(protocol);
            }
        }

    }

    private static class PendingRouteRequest {
        int rreqId;
        UUID destination;
        MeshProtocol<?> originalMessage;

        PendingRouteRequest(int rreqId, UUID destination, MeshProtocol<?> originalMessage) {
            this.rreqId = rreqId;
            this.destination = destination;
            this.originalMessage = originalMessage;
        }
    }

    private static class RouteEntry {
        UUID nextHop;
        int cost;
        long expiresAt;

        RouteEntry(UUID nextHop, int cost, long expiresAt) {
            this.nextHop = nextHop;
            this.cost = cost;
            this.expiresAt = expiresAt;
        }
    }

    private class MessageForwarding {
        private final MeshRouter router;
        private final RoutingTable routingTable;
        private final Deduplication deduplication;
        private final Map<ConnectionHandlersEnum, ConnectionHandler> handlers;

        MessageForwarding(MeshRouter router,
                          RoutingTable routingTable,
                          Deduplication deduplication,
                          Map<ConnectionHandlersEnum, ConnectionHandler> handlers) {
            this.router = router;
            this.routingTable = routingTable;
            this.deduplication = deduplication;
            this.handlers = handlers;
        }

        void forwardMessage(MeshProtocol<?> protocol) {
            // Check if we're the destination
            if (protocol.destination.equals(router.myUUID)) {
                router.handleLocalMessage(protocol);
                return;
            }

            // Lookup route
            Optional<RouteEntry> route = routingTable.getRoute(protocol.destination);

            if (route.isPresent()) {
                forwardToNextHop(protocol, route.get().nextHop);
            } else {
                // Trigger route discovery
                router.routeDiscovery.discoverRoute(protocol.destination, protocol);
            }
        }

        private void forwardToNextHop(MeshProtocol<?> protocol, UUID nextHop) {
            try {
                // Find connection handler that can reach nextHop
                for (ConnectionHandler handler : handlers.values()) {
                    if (handler.isOn()) {
                        handler.send(protocol.encode());
                        return;
                    }
                }
                throw new SendError("Next hop unreachable: " + nextHop);
            } catch (SendError e) {
                routingTable.routes.remove(protocol.destination);
                router.notifyError(protocol.messageId, e);
            }
        }

        void handleDataMessage(MeshProtocol<?> protocol, Device sender) {
            // Update reverse path
            routingTable.addRoute(protocol.sender, sender.uuid, 1,
                    System.currentTimeMillis() + 60000);

            // Continue forwarding
            forwardMessage(protocol);
        }
    }
}




