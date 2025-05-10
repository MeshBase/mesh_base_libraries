package com.example.mesh_base.ble;

import static com.example.mesh_base.ble.CommonConstants.CCCD_UUID;
import static com.example.mesh_base.ble.CommonConstants.ID_UUID;
import static com.example.mesh_base.ble.CommonConstants.MESSAGE_UUID;
import static com.example.mesh_base.ble.CommonConstants.SERVICE_UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseSettings;
import android.util.Log;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

@SuppressLint("MissingPermission")
public class PeripheralBLEEventHandler {

  BLEConnectionHandler handler;
  Peripheral peripheral;
  String TAG;
  private final BluetoothGattServerCallback serverCallback = new BluetoothGattServerCallback() {

    @Override
    public void onServiceAdded(int status, BluetoothGattService service) {
      super.onServiceAdded(status, service);
      boolean shouldContinue = handler.getPending() instanceof StartGattServer;
      if (!shouldContinue) {
        Log.w(TAG, "current task is not start gatt server, skipping");
        return;
      }

      boolean isKnownService = service.getUuid().equals(SERVICE_UUID);
      boolean isSuccessful = status == BluetoothGatt.GATT_SUCCESS;

      if (peripheral.getIsOn() && isKnownService && isSuccessful) {
        Log.d(TAG, "service added successfully" + service.getUuid());
        handler.addToQueue(new Advertise());
        handler.taskEnded();
      } else {
        Log.w(TAG, "cant confirm gatt server started because peripheralOff:" + !peripheral.getIsOn() + " notIsKnownService" + !isKnownService + " notSuccessful:" + (!isSuccessful) + " gattCode:" + status);
        handler.addToQueue(new CloseGatt());
        handler.taskEnded();
      }
    }


    @Override
    public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
      String address = device.getAddress();
      String name = device.getName();
      HashMap<String, BluetoothDevice> connectingDevices = peripheral.getConnectingDevices();
      HashMap<UUID, BluetoothDevice> connectedDevices = peripheral.getConnectedDevices();

      boolean anticipatingDisconnect = handler.getPending() instanceof DisconnectCentral && ((DisconnectCentral) handler.getPending()).device.getAddress().equals(device.getAddress());
      boolean exists = connectingDevices.containsKey(address) || (peripheral.getCentralUUID(address) != null);

      if (newState == BluetoothGatt.STATE_CONNECTED) {
        if (exists) {
          Log.w(TAG, name + " (" + address + ") attempted to connect twice. Ignoring");
          handler.addToQueue(new DisconnectCentral(device));
          return;
        }
        if (!peripheral.getIsOn()) {
          Log.d(TAG, "disconnecting because peripheral is off: " + name + address);
          handler.addToQueue(new DisconnectCentral(device));
          return;
        }

        connectingDevices.put(device.getAddress(), device);
        Log.d(TAG, "Central connected (not fully though): " + name + address + ". Now have " + connectingDevices.size() + "connecting centrals. status:" + status);

        //so that server.cancelConnection() causes disconnect events. According to https://stackoverflow.com/questions/38762758/bluetoothgattserver-cancelconnection-does-not-cancel-the-connection
        handler.addToQueue(new ConnectCentral(device));
        handler.notifyDiscovered(device.getName(), device.getAddress());

      } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
        if (anticipatingDisconnect) {
          Log.d(TAG, "anticipated disconnect of " + name + address + " is successful");
        }

        if (!exists) {
          //Can happen when both central and peripheral decide to disconnect at the same time
          Log.w(TAG, name + address + " was already not connected. Ignoring disconnect.");
          if (anticipatingDisconnect) handler.taskEnded();
          return;
        }

        Log.d(TAG, "Central disconnected: " + name + address + " status:" + status);
        UUID uuid = peripheral.getCentralUUID(address);
        connectingDevices.remove(address);
        connectedDevices.remove(uuid);
        handler.notifyDisconnect(uuid);
        handler.addToQueue(new Scan());
        if (anticipatingDisconnect) handler.taskEnded();

      } else {
        Log.w(TAG, "Unknown state: " + newState + " status: " + status);
      }
    }


    @Override
    public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
      super.onDescriptorReadRequest(device, requestId, offset, descriptor);
      boolean isRequestingIndication = descriptor.getUuid().equals(CCCD_UUID);
      byte[] messageDescValue = peripheral.getMessageDescriptor().getValue();

      if (peripheral.getIsOn() && isRequestingIndication && messageDescValue != null) {
        Log.d(TAG, "indications read request received from " + device.getName() + ":" + descriptor.getUuid());
        peripheral.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, peripheral.getMessageDescriptor().getValue());
      } else {
        Log.w(TAG, "rejecting indication read request from" + device.getName() + ":" + descriptor.getUuid() + "because didn'tRequestNotification" + !isRequestingIndication + " peripheralIsOff:" + !peripheral.getIsOn() + " messageDescValueIsNull:" + (messageDescValue == null));
        peripheral.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
        handler.addToQueue(new DisconnectCentral(device));
      }
    }


    @Override
    public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
      super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);
      boolean isRequestingIndication = descriptor.getUuid().equals(CCCD_UUID);
      boolean isEnable = Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);

      if (peripheral.getIsOn() && isRequestingIndication && isEnable) {
        Log.d(TAG, "indications write request received from " + device.getName() + ":" + descriptor.getUuid());
        descriptor.setValue(value);
        peripheral.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);
      } else {
        Log.w(TAG, "rejecting indication write request from" + device.getName() + ":" + descriptor.getUuid() + "because didn'tRequestNotification" + !isRequestingIndication + " peripheralIsOff:" + !peripheral.getIsOn() + "notIsEnable" + !isEnable);
        peripheral.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
        handler.addToQueue(new DisconnectCentral(device));
      }
    }


    @Override
    public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
      super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
      boolean isRequestingId = characteristic.getUuid().equals(ID_UUID);

      if (peripheral.getIsOn() && isRequestingId) {
        Log.d(TAG, "Id read request received from " + device.getName() + ":" + characteristic.getUuid());
        peripheral.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, ConvertUUID.uuidToBytes(handler.getId()));
      } else {
        Log.w(TAG, "rejecting read request from" + device.getName() + ":" + characteristic.getUuid() + "because didn'tRequestID" + isRequestingId + " peripheralIsOff:" + !peripheral.getIsOn());
        peripheral.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
        handler.addToQueue(new DisconnectCentral(device));
      }
    }


    @Override
    public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

      if (characteristic.getUuid().equals(MESSAGE_UUID)) {
        String message = new String(value, StandardCharsets.UTF_8);
        Log.d(TAG, "Received (string?): " + message + " from " + device.getName() + device.getAddress());
        UUID uuid = peripheral.getCentralUUID(device.getAddress());

        if (uuid == null) {
          Log.d(TAG, "central send a message but was not connected" + device.getName() + device.getAddress() + " disconnecting");
          handler.addToQueue(new DisconnectCentral(device));
          if (responseNeeded)
            peripheral.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
          return;
        }

        handler.notifyData(uuid, value);
        if (responseNeeded)
          peripheral.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);

      } else if (characteristic.getUuid().equals(ID_UUID)) {
        UUID otherId;
        try {
          otherId = ConvertUUID.bytesToUUID(value);
        } catch (Exception e) {
          Log.d(TAG, "couldn't parse uuid from central" + device.getName() + device.getAddress() + " where value is" + Arrays.toString(value));
          handler.addToQueue(new DisconnectCentral(device));
          if (responseNeeded)
            peripheral.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
          return;
        }

        if (handler.connectedExists(otherId)) {
          Log.d(TAG, "central is already connected " + device.getName() + device.getAddress() + " with uuid" + otherId + ", disconnecting");
          handler.addToQueue(new DisconnectCentral(device));
          if (responseNeeded)
            peripheral.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
          return;
        }

        peripheral.getConnectedDevices().put(otherId, device);
        peripheral.getConnectingDevices().remove(device.getAddress());
        handler.notifyConnect(otherId);
        handler.addToQueue(new Scan());
        if (responseNeeded)
          peripheral.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, null);

      } else {
        Log.e(TAG, "unexpected characteristic was written:" + characteristic.getUuid());
        handler.addToQueue(new DisconnectCentral(device));
        if (responseNeeded)
          peripheral.sendResponse(device, requestId, BluetoothGatt.GATT_FAILURE, offset, null);
      }
    }


    @Override
    public void onNotificationSent(BluetoothDevice device, int status) {
      super.onNotificationSent(device, status);

      boolean shouldContinue = handler.getPending() instanceof IndicateCharacteristic && ((IndicateCharacteristic) handler.getPending()).device.getAddress().equals(device.getAddress());
      if (!shouldContinue) {
        Log.w(TAG, "current task is not indication or the address of device does not match, skipping");
        return;
      }
      IndicateCharacteristic task = (IndicateCharacteristic) handler.getPending();

      if (status == BluetoothGatt.GATT_SUCCESS) {
        Log.d(TAG, "sent data to central" + device.getName() + device.getAddress() + " successfully!");
        handler.taskEnded();
        return;
      }

      if (task.remainingRetries <= 0) {
        Log.d(TAG, "could not indicate after retries, stopping");
        handler.addToQueue(new DisconnectCentral(device));
        handler.taskEnded();
        return;
      }

      Log.d(TAG, "retrying indication");
      handler.addToQueue(new IndicateCharacteristic(task.remainingRetries - 1, task.characteristic, task.value, task.device));
      handler.taskEnded();
    }
  };

  PeripheralBLEEventHandler(Peripheral peripheral) {
    this.peripheral = peripheral;
    this.handler = peripheral.getBLEHandler();
  }

  AdvertiseCallback getAdvertisementCallback() {
    return advertisementCallback;
  }

  BluetoothGattServerCallback getServerCallback() {
    return serverCallback;
  }


  private final AdvertiseCallback advertisementCallback = new AdvertiseCallback() {
    @Override
    public void onStartSuccess(AdvertiseSettings settingsInEffect) {
      super.onStartSuccess(settingsInEffect);
      boolean shouldContinue = handler.getPending() instanceof Advertise;
      if (!shouldContinue) {
        Log.w(TAG, "current task is not advertisement, skipping");
        return;
      }
      Log.d(TAG, "Advertisement started successfully");
      handler.addToQueue(new Scan());
      handler.taskEnded();
    }

    @Override
    public void onStartFailure(int errorCode) {
      super.onStartFailure(errorCode);

      boolean shouldContinue = handler.getPending() instanceof Advertise;
      if (!shouldContinue) {
        Log.w(TAG, "current task is not advertisement, skipping canceling it");
        return;
      }
      Log.d(TAG, "Advertisement failed:" + errorCode);
      if (peripheral.getAdvertiser() == null) {
        Log.w(TAG, "advertiser was null onStartFailure!, skipping .stopAdvertising");
      } else {
        peripheral.getAdvertiser().stopAdvertising(advertisementCallback);
        peripheral.nullAdvertiser();
      }
      handler.addToQueue(new CloseGatt());
      handler.taskEnded();
    }


  };

}
