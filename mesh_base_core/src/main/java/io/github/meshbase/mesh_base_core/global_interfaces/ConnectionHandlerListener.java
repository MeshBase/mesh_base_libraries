package io.github.meshbase.mesh_base_core.global_interfaces;

public abstract class ConnectionHandlerListener {
    public void onNeighborConnected(Device device) {
    }

    public void onNeighborDisconnected(Device device) {
    }

    public void onDisconnected() {
    }

    public void onConnected() {
    }

    public void onDataReceived(Device device, byte[] data) {
    }
}
