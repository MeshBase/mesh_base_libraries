package com.example.mesh_base.global_interfaces;


import android.content.Context;

import java.util.ArrayList;
import java.util.UUID;


//TODO: add isOn(), isOff(), isEnabled() status
public abstract class ConnectionHandler {

  protected ArrayList<ConnectionHandlerListener> connectionHandlerListeners = new ArrayList<>();
  protected Context context;
  protected UUID id;

  public ConnectionHandler(Context context, UUID id) {
    this.context = context;
    this.id = id;
  }

  public void subscribe(ConnectionHandlerListener connectionHandlerListener) {
    this.connectionHandlerListeners.add(connectionHandlerListener);
  }

  public void unSubscribe(ConnectionHandlerListener connectionHandlerListener) {
    this.connectionHandlerListeners.remove(connectionHandlerListener);
  }


  public abstract ArrayList<Device> getNeighbourDevices();

  public abstract void start() throws Exception;

  public abstract void stop();

  public abstract boolean isOn();

  public abstract void send(byte[] data) throws SendError; //Send to all neighbors

  public abstract void send(byte[] data, Device neighbor) throws SendError; //Send through a specific neighbor

  public abstract void enable();

  public abstract boolean isEnabled();

  public abstract boolean isSupported();


  protected void onNeighborConnected(Device device) {
    for (ConnectionHandlerListener listener : connectionHandlerListeners) {
      listener.onNeighborConnected(device);
    }
  }


  protected void onNeighborDisconnected(Device device) {
    for (ConnectionHandlerListener listener : connectionHandlerListeners) {
      listener.onNeighborDisconnected(device);
    }
  }


  protected void onDisconnected() {
    for (ConnectionHandlerListener listener : connectionHandlerListeners) {
      listener.onDisconnected();
    }

  }


  protected void onConnected() {
    for (ConnectionHandlerListener listener : connectionHandlerListeners) {
      listener.onConnected();
    }
  }


  protected void onDataReceived(Device device, byte[] data) {
    for (ConnectionHandlerListener listener : connectionHandlerListeners) {
      listener.onDataReceived(device, data);
    }
  }
}
