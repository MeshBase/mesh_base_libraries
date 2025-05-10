package com.example.mesh_base.global_interfaces;

public interface DataListener {
    void onEvent(byte[] data, Device neighbor);
}
