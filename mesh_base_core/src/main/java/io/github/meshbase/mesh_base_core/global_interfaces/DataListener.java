package io.github.meshbase.mesh_base_core.global_interfaces;

public interface DataListener {
    void onEvent(byte[] data, Device neighbor);
}
