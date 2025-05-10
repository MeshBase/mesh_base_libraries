package io.github.meshbase.mesh_base_core.global_interfaces;

import java.util.ArrayList;

public interface NearbyDevicesListener {
    void onEvent(ArrayList<Device> devices);
}
