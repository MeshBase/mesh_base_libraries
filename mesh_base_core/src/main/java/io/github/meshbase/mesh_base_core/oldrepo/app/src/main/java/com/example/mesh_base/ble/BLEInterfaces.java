package com.example.mesh_base.ble;

import com.example.mesh_base.global_interfaces.Device;

import java.util.UUID;

class BLEDevice extends Device {

    String address;

    BLEDevice(UUID uuid, String name, String address) {
        super(uuid, name);
        this.address = address;
    }
}
