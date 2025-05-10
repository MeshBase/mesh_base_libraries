package com.example.mesh_base.router;

import android.util.Log;

import com.example.mesh_base.global_interfaces.ConnectionHandler;
import com.example.mesh_base.global_interfaces.ConnectionHandlerListener;
import com.example.mesh_base.global_interfaces.ConnectionHandlersEnum;
import com.example.mesh_base.global_interfaces.Device;
import com.example.mesh_base.global_interfaces.InternalRouterError;
import com.example.mesh_base.global_interfaces.SendError;

import java.util.HashMap;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;


public class Router {
    String TAG = "my_router";
    UUID id;
    HashMap<ConnectionHandlersEnum, ConnectionHandler> connectionHandlers;
    //TODO: get this data from protocol registry in the future
    HashSet<ProtocolType> typesExpectingResponses;
    HashSet<String> routedSet = new HashSet<>();
    RouterListener routerListener = new RouterListener() {
        @Override
        public void onData(MeshProtocol<?> protocol, Device neighbor) {
            Log.d(TAG, "Received data from " + neighbor.name);
        }

        @Override
        public void onError(Exception exception) {
            Log.d(TAG, "Router error" + exception.getMessage());
        }
    };
    HashMap<Integer, SendListener> listeners = new HashMap<>();

    public Router(HashMap<ConnectionHandlersEnum, ConnectionHandler> connectionHandlers, UUID id, HashSet<ProtocolType> typesExpectingResponses) {
        this.typesExpectingResponses = typesExpectingResponses;
        this.connectionHandlers = connectionHandlers;
        this.id = id;

        for (ConnectionHandler handler : connectionHandlers.values()) {
            handler.subscribe(
                    new ConnectionHandlerListener() {
                        @Override
                        public void onDataReceived(Device device, byte[] data) {
                            handleOnData(device, data);
                        }
                    }
            );
        }
    }

    public void sendData(MeshProtocol<?> protocol, SendListener listener, boolean keepMessageId) {
        //override since router should be concerned about the remaining hops and keeping track of message Ids
        if (!keepMessageId) {
            protocol.messageId = ThreadLocalRandom.current().nextInt();
        }
        protocol.remainingHops = 4;
        setRouted(protocol.messageId, protocol.sender);
        listeners.put(protocol.messageId, listener);
        try {
            floodData(protocol.encode());
        } catch (SendError e) {
            handleOnError(e, protocol.messageId);
        }
    }

    public void sendData(MeshProtocol<?> protocol, SendListener listener) {
        sendData(protocol, listener, false);
    }

    //keep private until it's need is justified
    private void floodData(byte[] data) throws SendError {
        boolean hasAttemptedSending = false;
        for (ConnectionHandler handler : connectionHandlers.values()) {
            try {
                if (handler.isOn() && !handler.getNeighbourDevices().isEmpty()) {
                    handler.send(data);
                    hasAttemptedSending = true;
                }
            } catch (SendError e) {
                //Silent error in case other neighbors have successfully sent
                Log.e(TAG, "Error sending data: " + e.getMessage());
            }
        }
        if (!hasAttemptedSending) {
            throw new SendError("No neighbors available");
        }
    }

    private boolean hasRoutedDataBefore(int messageId, UUID address) {
        String key = messageId + "." + address.toString();
        return routedSet.contains(key);
    }

    private void setRouted(int messageId, UUID address) {
        String key = messageId + "." + address.toString();
        routedSet.add(key);
    }

    private void replyWithAck(MeshProtocol<?> protocol) {
        AckMessageBody ackMessageBody = new AckMessageBody("OK");
        MeshProtocol<AckMessageBody> ackData = new ConcreteMeshProtocol<>(
                0, // Message Type is ACK
                4, // Goes to 4 device before stopping
                protocol.messageId,
                id, // Destination becomes sender
                protocol.sender, // Sender becomes Destination
                ackMessageBody // Sends back 'OK'
        );
        try {
            floodData(ackData.encode());
        } catch (SendError e) {
            Log.e(TAG, "Error sending ack: " + protocol.messageId);
            routerListener.onError(e);
        }
    }


    private void handleOnData(Device neighbor, byte[] byteArray) {
        MeshProtocol<?> protocol;
        ProtocolType messageProtocolType = MeshProtocol.getByteType(byteArray);

        switch (messageProtocolType) {
            case ACK:
                protocol = MeshProtocol.decode(byteArray, AckMessageBody::decode);
                break;
            case SEND_MESSAGE:
                protocol = MeshProtocol.decode(byteArray, SendMessageBody::decode);
                break;
            default:
                Log.e(TAG, "Unknown byte array. Can't decode data");
                return;
        }

        boolean selfIsDestination = protocol.destination != null && protocol.destination.equals(id);
        boolean expectsResponse = typesExpectingResponses.contains(messageProtocolType);

        if (hasRoutedDataBefore(protocol.messageId, protocol.sender)) {
            Log.d(TAG, "already routed data. skipping. messageId=" + protocol.messageId + " sender=" + protocol.sender);
        } else if (selfIsDestination && messageProtocolType == ProtocolType.ACK) {
            handleOnAck(protocol);
        } else if (selfIsDestination && hasRoutedDataBefore(protocol.messageId, id)) {
            //Consider as response if the protocol has the same messageId, and this is the destination
            handleOnResponse(protocol);
        } else if (selfIsDestination && expectsResponse) {
            //Not ACKing letting the user send a response protocol
            routerListener.onData(protocol, neighbor);
        } else if (selfIsDestination) {
            //Safe to reply with ACK if not expecting a response
            routerListener.onData(protocol, neighbor);
            replyWithAck(protocol);
        } else if (protocol.remainingHops <= 0) {
            Log.d(TAG, "finished remaining hops, cant route anymore. messageId=" + protocol.messageId + " sender=" + protocol.sender);
        } else {
            setRouted(protocol.messageId, protocol.sender);
            protocol.remainingHops -= 1;
            Log.d(TAG, "relaying data " + protocol.messageId + "sender=" + protocol.sender + " remainingHops=" + protocol.remainingHops);
            try {
                floodData(protocol.encode());
            } catch (SendError e) {
                Log.e(TAG, "Error relaying data: " + e.getMessage());
                routerListener.onError(e);
            }
        }
    }

    public void setListener(RouterListener listener) {
        this.routerListener = listener;
    }

    private void handleOnAck(MeshProtocol<?> protocol) {
        int messageId = protocol.messageId;
        try {
            SendListener listener = getListener(messageId);
            listener.onAck();
        } catch (Exception e) {
            Log.e(TAG, "error when handling on ack: " + e.getMessage());
            routerListener.onError(e);
        } finally {
            listeners.remove(messageId);
        }
    }

    private void handleOnError(SendError error, int messageId) {
        try {
            SendListener listener = getListener(messageId);
            listener.onError(error);
        } catch (Exception e) {
            Log.e(TAG, "error when handle on error" + messageId);
            routerListener.onError(e);
        } finally {
            listeners.remove(messageId);
        }
    }

    //TODO: throw error for unexpected response type
    private void handleOnResponse(MeshProtocol<?> response) {
        int messageId = response.messageId;
        try {
            SendListener listener = getListener(messageId);
            listener.onResponse(response);
            replyWithAck(response);
        } catch (Exception e) {
            Log.e(TAG, "error when handling response" + e.getMessage());
            routerListener.onError(e);
        } finally {
            listeners.remove(messageId);
        }
    }

    private SendListener getListener(int messageId) throws Exception {
        SendListener listener = listeners.get(messageId);
        if (listener == null) {
            throw new InternalRouterError("Could not find listener for messageId: " + messageId);
        }
        return listener;
    }

    public interface RouterListener {
        void onData(MeshProtocol<?> protocol, Device neighbor);

        void onError(Exception exception);
    }
}
