package io.github.meshbase.mesh_base_core.router;

import io.github.meshbase.mesh_base_core.global_interfaces.Device;

public interface RouterListener {
    void onData(MeshProtocol<?> protocol, Device neighbor);

    void onError(Exception exception);
}
