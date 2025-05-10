package com.example.mesh_base.mesh_manager;

import android.util.Log;

import com.example.mesh_base.global_interfaces.Device;
import com.example.mesh_base.router.MeshProtocol;

public abstract class MeshManagerListener {
    String TAG = "my_meshManager_listener";

    static MeshManagerListener createEmpty() {
        return new MeshManagerListener() {
            @Override
            public void onDataReceivedForSelf(MeshProtocol<?> data) {
                Log.d(TAG, "[from empty listener] received data");
            }

            @Override
            public void onStatusChange(Status status) {
                Log.d(TAG, "[from empty listener] status changed" + status.toString());
            }

            @Override
            public void onNeighborConnected(Device device) {
                Log.d(TAG, "[from empty listener] device connected");
            }

            @Override
            public void onNeighborDisconnected(Device device) {
                Log.d(TAG, "[from empty listener] device disconnected");
            }

            @Override
            public void onError(Exception e) {
                Log.d(TAG, "[from empty listener] error");
            }
        };
    }

    // Is going to be called only when the received data destination matches the current
    // user UUID, In other words if we are the destination.
    abstract public void onDataReceivedForSelf(MeshProtocol<?> protocol);

    abstract public void onStatusChange(Status status);

    abstract public void onNeighborConnected(Device device);

    abstract public void onNeighborDisconnected(Device device);

    abstract public void onError(Exception e);
}
