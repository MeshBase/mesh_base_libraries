package io.github.meshbase.mesh_base_core.ble;

import io.github.meshbase.mesh_base_core.global_interfaces.Device;

import java.util.UUID;

class BLEDevice extends Device {

    String address;

    BLEDevice(UUID uuid, String name, String address) {
        super(uuid, name);
        this.address = address;
    }
}
