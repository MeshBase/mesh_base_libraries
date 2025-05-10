package com.example.mesh_base.ble;

import static com.example.mesh_base.ble.CommonConstants.CCCD_UUID;
import static com.example.mesh_base.ble.CommonConstants.ID_UUID;
import static com.example.mesh_base.ble.CommonConstants.MESSAGE_UUID;
import static com.example.mesh_base.ble.CommonConstants.SERVICE_UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.util.Log;

import java.util.HashMap;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class Peripheral {
  private final HashMap<String, BluetoothDevice> connectingDevices = new HashMap<>();
  private final HashMap<UUID, BluetoothDevice> connectedDevices = new HashMap<>();
  private final String TAG;
  private final BLEConnectionHandler handler;
  private final PeripheralBLEEventHandler eventHandler;
  private boolean isOn = false;
  private BluetoothGattServer server;
  private BluetoothLeAdvertiser advertiser;
  private BluetoothGattCharacteristic messageCharacteristic;
  private BluetoothGattDescriptor messageDescriptor;

  Peripheral(BLEConnectionHandler handler) {
    this.handler = handler;
    this.TAG = handler.TAG + BLEConnectionHandler.PRFL;
    this.eventHandler = new PeripheralBLEEventHandler(this);
  }

  BluetoothGattCharacteristic getMessageCharacteristic() {
    return messageCharacteristic;
  }

  BluetoothDevice getCentral(UUID id) {
    return connectedDevices.get(id);
  }

  BLEConnectionHandler getBLEHandler() {
    return handler;
  }

  boolean getIsOn() {
    return isOn;
  }

  HashMap<String, BluetoothDevice> getConnectingDevices() {
    return connectingDevices;
  }

  HashMap<UUID, BluetoothDevice> getConnectedDevices() {
    return connectedDevices;
  }

  BluetoothGattDescriptor getMessageDescriptor() {
    return messageDescriptor;
  }

  void nullAdvertiser() {
    this.advertiser = null;
  }

  BluetoothLeAdvertiser getAdvertiser() {
    return advertiser;
  }

  public void start() {
    if (isOn) {
      Log.d(TAG, "is already on");
      return;
    }

    isOn = true;
    handler.addToQueue(new StartGattServer());
  }


  void startGattServer(StartGattServer task) {
    if (server != null || !isOn) {
      Log.d(TAG, "skipping starting gatt server due to isNotNull:" + (server != null) + " peripheralIsOff:" + !isOn);
      handler.taskEnded();
      return;
    }

    BluetoothManager btManager = (BluetoothManager) handler.getContext().getSystemService(Context.BLUETOOTH_SERVICE);
    server = btManager.openGattServer(handler.getContext(), eventHandler.getServerCallback());

    BluetoothGattService service = new BluetoothGattService(SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

    messageCharacteristic = new BluetoothGattCharacteristic(
            MESSAGE_UUID,
            BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_INDICATE,
            BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ
    );

    messageDescriptor = new BluetoothGattDescriptor(
            CCCD_UUID,
            BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ
    );

    messageCharacteristic.addDescriptor(messageDescriptor);

    BluetoothGattCharacteristic idCharacteristic = new BluetoothGattCharacteristic(
            ID_UUID,
            BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE,
            BluetoothGattCharacteristic.PERMISSION_READ | BluetoothGattCharacteristic.PERMISSION_WRITE
    );

    service.addCharacteristic(messageCharacteristic);
    service.addCharacteristic(idCharacteristic);
    server.addService(service);
  }

  void expireStartGattServer(StartGattServer task) {
    handler.addToQueue(new CloseGatt());
    handler.taskEnded();
  }

  void startAdvertising(Advertise task) {
    boolean alreadyAdvertising = advertiser != null;
    if (server == null || !isOn || alreadyAdvertising) {
      Log.d(TAG, "skipping starting advertising due to gatIsNull:" + (server == null) + " peripheralIsOff:" + !isOn + "alreadyAdvertising:" + alreadyAdvertising);
      handler.taskEnded();
      return;
    }
    AdvertiseSettings settings = new AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setConnectable(true)
            .setTimeout(0)
            .build();

    AdvertiseData data = new AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addServiceUuid(new android.os.ParcelUuid(SERVICE_UUID))
            .build();

    advertiser = BluetoothAdapter.getDefaultAdapter().getBluetoothLeAdvertiser();
    try {
      advertiser.startAdvertising(settings, data, eventHandler.getAdvertisementCallback());
    } catch (Exception e) {
      Log.w(TAG, "couldn't advertise due to error:" + e + ", closing gatt");
      advertiser.stopAdvertising(eventHandler.getAdvertisementCallback());
      advertiser = null;
      handler.addToQueue(new CloseGatt());
      handler.taskEnded();
    }
  }

  void expireStartAdvertising(Advertise task) {
    Log.d(TAG, "advertising expired, closing gatt server");
    advertiser.stopAdvertising(eventHandler.getAdvertisementCallback());
    advertiser = null;
    handler.addToQueue(new CloseGatt());
    handler.taskEnded();
  }

  UUID getCentralUUID(String address) {
    for (UUID key : connectedDevices.keySet()) {
      BluetoothDevice device = connectedDevices.get(key);
      if (device != null && device.getAddress().equals(address)) {
        return key;
      }
    }
    return null;
  }

  void startConnectCentral(ConnectCentral task) {
    //so that server.cancelConnection() causes disconnect events. According to https://stackoverflow.com/questions/38762758/bluetoothgattserver-cancelconnection-does-not-cancel-the-connection
    if (server == null) {
      Log.d(TAG, "gatt server has been already closed, skipping connecting");
      handler.taskEnded();
    } else {
      server.connect(task.device, false);
      handler.taskEnded();
    }
  }

  void startIndicateCharacteristic(IndicateCharacteristic task) {
    if (task.characteristic == null || server == null) {
      Log.w(TAG, "can not indicate message to central" + task.device.getName() + task.device.getAddress() + " because characteristicIsNull:" + (task.characteristic == null) + " gattServerIsNull" + (server == null));
      handler.taskEnded();
      return;
    }
    task.characteristic.setValue(task.value);
    server.notifyCharacteristicChanged(task.device, task.characteristic, true);
  }


  void expireIndicateCharacteristic(IndicateCharacteristic task) {
    if (task.remainingRetries > 0) {
      handler.addToQueue(new IndicateCharacteristic(task.remainingRetries - 1, task.characteristic, task.value, task.device));
    } else {
      handler.addToQueue(new DisconnectCentral(task.device));
    }
  }


  void startDisconnectCentral(DisconnectCentral task) {
    if (server == null) {
      Log.d(TAG, "gatt server already closed, skipping disconnecting");
      handler.taskEnded();
      return;
    }
    boolean isConnecting = (connectingDevices.containsKey(task.device.getAddress()));
    boolean isConnected = getCentralUUID(task.device.getAddress()) != null;
    if (!isConnecting && !isConnected) {
      Log.d(TAG, "skipping disconnecting to " + task.device.getName() + task.device.getAddress() + " as it is already not connected");
      handler.taskEnded();
      return;
    }
    server.cancelConnection(task.device);
  }

  void expireDisconnectCentral(DisconnectCentral task) {
    UUID uuid = getCentralUUID(task.device.getAddress());
    handler.notifyDisconnect(uuid);
    connectingDevices.remove(task.device.getAddress());
    connectedDevices.remove(uuid);
  }

  void sendResponse(BluetoothDevice device, int requestId, int newState, int offset, byte[] data) {
    if (server == null) {
      Log.d(TAG, "gatt server already closed, skipping sending response");
    } else {
      server.sendResponse(device, requestId, newState, offset, data);
    }
  }

  public void stop() {
    if (!isOn) {
      Log.d(TAG, "is already off");
      return;
    }
    isOn = false;
    if (advertiser != null) {
      advertiser.stopAdvertising(eventHandler.getAdvertisementCallback());
      advertiser = null;
    } else {
      Log.w(TAG, "advertiser is null, skipping stopping advertising");
    }

    for (BluetoothDevice device : connectingDevices.values()) {
      handler.addToQueue(new DisconnectCentral(device));
    }

    for (BluetoothDevice device : connectedDevices.values()) {
      handler.addToQueue(new DisconnectCentral(device));
    }

    handler.addToQueue(new CloseGatt());
  }

  void startClosingGatt(CloseGatt task) {
    if (server == null) {
      Log.d(TAG, "gatt has already been stopped, skipping");
      handler.addToQueue(new Scan());
      handler.taskEnded();
      return;
    }
    if (advertiser != null) {
      Log.d(TAG, "closing advertiser along with closing gatt");
      advertiser.stopAdvertising(eventHandler.getAdvertisementCallback());
      advertiser = null;
    }

    server.close();
    server = null;
    handler.addToQueue(new Scan());
    handler.taskEnded();
  }


}
