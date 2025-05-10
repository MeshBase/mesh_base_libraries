package com.example.mesh_base.mesh_manager;

import com.example.mesh_base.global_interfaces.ConnectionHandlersEnum;

import java.util.HashMap;

public class Status {
  private final boolean isOn;
  private final HashMap<ConnectionHandlersEnum, Property> connectionStatuses;

  Status(boolean isOn, HashMap<ConnectionHandlersEnum, Property> connections) {
    this.isOn = isOn;
    this.connectionStatuses = connections;
  }

  public HashMap<ConnectionHandlersEnum, Property> getStatus() {
    return connectionStatuses;
  }

  public Property getStatus(ConnectionHandlersEnum connectionType) {
    return connectionStatuses.get(connectionType);
  }

  public HashMap<ConnectionHandlersEnum, Property> getConnectionStatuses() {
    return connectionStatuses;
  }

  public boolean isOn() {
    return isOn;
  }

  public static class Property {

    private final boolean isSupported;
    private final boolean isOn;
    private final boolean isAllowed;

    public Property(boolean isSupported, boolean isOn, boolean isAllowed) {
      this.isSupported = isSupported;
      this.isOn = isOn;
      this.isAllowed = isAllowed;
    }

    public boolean isSupported() {
      return isSupported;
    }

    public boolean isOn() {
      return isOn;
    }

    public boolean isAllowed() {
      return isAllowed;
    }
  }

}
