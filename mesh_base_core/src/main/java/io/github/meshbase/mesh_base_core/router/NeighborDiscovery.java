package io.github.meshbase.mesh_base_core.router;

import android.util.Log;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import io.github.meshbase.mesh_base_core.global_interfaces.ConnectionHandler;
import io.github.meshbase.mesh_base_core.global_interfaces.ConnectionHandlersEnum;
import io.github.meshbase.mesh_base_core.global_interfaces.Device;
import io.github.meshbase.mesh_base_core.global_interfaces.SendError;

public class NeighborDiscovery {
    private final String TAG = "Mesh_Heartbeat";
    private final Map<ConnectionHandlersEnum, ConnectionHandler> handlers;

    private final UUID deviceUUID;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    NeighborDiscovery(Map<ConnectionHandlersEnum, ConnectionHandler> handlers,
                      UUID deviceUUID
    ) {
        this.handlers = handlers;
        this.deviceUUID = deviceUUID;
    }

    private void startPeriodicPings() {
        scheduler.scheduleWithFixedDelay(() -> {
            for (ConnectionHandler handler: handlers.values()) {
                if (!handler.isOn()) continue;

                for (Device neighbor: handler.getNeighbourDevices()) {
                    MeshProtocol<SendMessageBody> ping = createPingMessage(neighbor.uuid);
                    try {
                        handler.send(ping.encode());
                    } catch (SendError e) {
                        Log.e(TAG,"Failed to send ping message with error" + e.toString());
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
