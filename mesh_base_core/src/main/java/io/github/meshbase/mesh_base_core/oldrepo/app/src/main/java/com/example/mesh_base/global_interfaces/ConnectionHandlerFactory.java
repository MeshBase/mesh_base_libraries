package com.example.mesh_base.global_interfaces;

import androidx.activity.ComponentActivity;

import com.example.mesh_base.ble.BLEConnectionHandler;

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
