package io.github.meshbase.mesh_base_core.global_interfaces;

import java.util.UUID;

public abstract class Device {
  public UUID uuid;
  public String name;

  public Device(UUID uuid, String name) {

    this.uuid = uuid;
    this.name = name;
  }
}
