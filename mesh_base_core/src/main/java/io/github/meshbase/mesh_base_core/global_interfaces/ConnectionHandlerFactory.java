package io.github.meshbase.mesh_base_core.global_interfaces;

import androidx.activity.ComponentActivity;

import io.github.meshbase.mesh_base_core.ble.BLEConnectionHandler;

import java.util.UUID;

import kotlin.NotImplementedError;


public class ConnectionHandlerFactory {
    private static final String TAG = "ConnectionHandlerFactory";

    public ConnectionHandler createConnectionHandler(
            ConnectionHandlersEnum type,
            ComponentActivity context,
            UUID id
    ) {
        ConnectionHandler handler;
        switch (type) {
            case BLE:
                handler = new BLEConnectionHandler(context, id);
                break;
            //Todo: add wifi direct
            default:
                throw new NotImplementedError();
        }
        return handler;
    }
}
