package com.example.mesh_base.ble;

import static com.example.mesh_base.ble.CommonConstants.CCCD_UUID;
import static com.example.mesh_base.ble.CommonConstants.ID_UUID;
import static com.example.mesh_base.ble.CommonConstants.MESSAGE_UUID;
import static com.example.mesh_base.ble.CommonConstants.SERVICE_UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.util.Log;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;

@SuppressLint("MissingPermission")
class CentralBLEEventHandler {
  BLEConnectionHandler handler;
  Central central;
  String TAG;
  private final BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
      String address = gatt.getDevice().getAddress();
      String name = gatt.getDevice().getName();
      Map<String, BluetoothDevice> connectingDevices = central.getConnectingDevices();
      Map<UUID, BluetoothGatt> connectedDevices = central.getConnectedDevices();
      Map<String, Integer> connectTryCount = central.getConnectTryCount();

      boolean anticipatedConnect = handler.getPending() instanceof ConnectToPeripheral && ((ConnectToPeripheral) handler.getPending()).device.getAddress().equals(address);
      boolean anticipatedDisconnect = handler.getPending() instanceof DisconnectPeripheral && ((DisconnectPeripheral) handler.getPending()).gatt.getDevice().getAddress().equals(address);

      if (newState == BluetoothGatt.STATE_CONNECTED) {
        if (!anticipatedConnect) {
          Log.w(TAG, "did not anticipate connecting to " + name + address + ", disconnecting");
          handler.addToQueue(new DisconnectPeripheral(gatt));
          handler.taskEnded();
          return;
        }
        Log.d(TAG, "Connected (not fully) to: " + name + address);
        handler.notifyDiscovered(name, address);
        handler.addToQueue(new DiscoverServices(gatt));
        handler.taskEnded();

      } else if (newState == BluetoothGatt.STATE_DISCONNECTED) {

        Log.w(TAG, "Disconnected from: " + name + address + " anticipated:" + (anticipatedConnect || anticipatedDisconnect) + ". status:" + status);
        gatt.close();
        UUID uuid = central.getPeripheralUUID(address);
        handler.notifyDisconnect(uuid);

        connectingDevices.remove(address);
        connectedDevices.remove(uuid);


        if (handler.getPending() instanceof DisconnectPeripheral && ((DisconnectPeripheral) handler.getPending()).forgetRetries) {
          Log.d(TAG, "forgetting retry count of " + name + address);
          connectTryCount.remove(address);
        }

        if (handler.getPending() instanceof DisconnectPeripheral && ((DisconnectPeripheral) handler.getPending()).tryReconnect) {

          if (central.avoidConnectingToPeripheral(gatt.getDevice())) {
            Log.d(TAG, "skip trying to reconnect to " + name + address);
          } else {
            int count = connectTryCount.getOrDefault(address, 0);
            connectTryCount.put(address, count + 1);

            Log.d(TAG, "Retrying to connect to " + name + address + " " + connectTryCount.getOrDefault(address, -1) + "tries done");
            handler.addToQueue(new ConnectToPeripheral(gatt.getDevice()));
          }
        }

        handler.addToQueue(new Scan());
        if (anticipatedConnect || anticipatedDisconnect) handler.taskEnded();
      } else {
        Log.d(TAG, "unknown connection state " + newState + " , disconnecting");
        handler.addToQueue(new DisconnectPeripheral(gatt));
      }

    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
      boolean shouldContinue = handler.getPending() instanceof DiscoverServices && ((DiscoverServices) handler.getPending()).gatt.getDevice().getAddress().equals(gatt.getDevice().getAddress());
      if (!shouldContinue) {
        Log.w(TAG, "current task is not discover services or not for this device, skipping");
        return;
      }

      if (status != BluetoothGatt.GATT_SUCCESS) {
        Log.d(TAG, "Services discovery failed for " + gatt.getDevice().getName());
        handler.addToQueue(new DisconnectPeripheral(gatt));
        handler.taskEnded();
        return;
      }
      Log.d(TAG, "Services discovered for " + gatt.getDevice().getAddress());

      if (!central.getIsOn()) {
        Log.d(TAG, "can't continue connection ladder because central is off");
        handler.addToQueue(new DisconnectPeripheral(gatt));
        handler.taskEnded();
        return;
      }

      handler.addToQueue(new NegotiateMTU(gatt, Central.MAX_MTU_SIZE));
      handler.taskEnded();
    }

    @Override
    public void onMtuChanged(BluetoothGatt gatt, int mtu, int status) {
      super.onMtuChanged(gatt, mtu, status);

      boolean isMtuTask = handler.getPending() instanceof NegotiateMTU;
      boolean isSameAddress = isMtuTask && ((NegotiateMTU) handler.getPending()).gatt.getDevice().getAddress().equals(gatt.getDevice().getAddress());
      boolean shouldContinue = isMtuTask && isSameAddress;
      if (!shouldContinue) {
        Log.w(TAG, "current task is not mtu changing or not this device, skipping");
        return;
      }

      boolean notSuccess = status != BluetoothGatt.GATT_SUCCESS;
      boolean centralIsOff = !central.getIsOn();
      if (notSuccess || centralIsOff) {
        Log.w(TAG, "[on mtu changed] stopping from fully connecting" + gatt.getDevice().getName() + gatt.getDevice().getAddress() + "due to notSuccess:" + notSuccess + " centralIsOff" + centralIsOff);
        handler.addToQueue(new DisconnectPeripheral(gatt));
        handler.taskEnded();
        return;
      }

      Log.d(TAG, "new mtu value is " + mtu);
      try {
        BluetoothGattCharacteristic messageCharacteristic = gatt.getService(SERVICE_UUID).getCharacteristic(MESSAGE_UUID);
        handler.addToQueue(new EnableIndication(messageCharacteristic, gatt));
        handler.taskEnded();
      } catch (Exception e) {
        //see if any null errors
        Log.e(TAG, "error on mtu changed: " + e);
        handler.addToQueue(new DisconnectPeripheral(gatt));
        handler.taskEnded();
      }
    }

    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
      super.onDescriptorWrite(gatt, descriptor, status);

      boolean isEnableIndication = handler.getPending() instanceof EnableIndication;
      boolean isSameAddress = isEnableIndication && ((EnableIndication) handler.getPending()).gatt.getDevice().getAddress().equals(gatt.getDevice().getAddress());
      boolean shouldContinue = isEnableIndication && isSameAddress;
      if (!shouldContinue) {
        Log.w(TAG, "current task is not descriptor writing or not this device, skipping");
        return;
      }

      boolean notIndication = !descriptor.getUuid().equals(CCCD_UUID);
      boolean centralIsOff = !central.getIsOn();
      if (notIndication || centralIsOff) {
        Log.w(TAG, "[on desc write] stopping from fully connecting" + gatt.getDevice().getName() + gatt.getDevice().getAddress() + "due to notIndication:" + notIndication + " centralIsOff" + centralIsOff);
        handler.addToQueue(new DisconnectPeripheral(gatt));
        handler.taskEnded();
        return;
      }

      try {
        BluetoothGattCharacteristic idCharacteristic = gatt.getService(CommonConstants.SERVICE_UUID).getCharacteristic(ID_UUID);
        if (idCharacteristic == null) throw new Exception("id characteristic is null");
        handler.addToQueue(new ReadCharacteristic(idCharacteristic, gatt));
        handler.addToQueue(new Scan());
        handler.taskEnded();
      } catch (Exception e) {
        Log.e(TAG, "error on descriptor write" + e);
        handler.addToQueue(new DisconnectPeripheral(gatt));
        handler.taskEnded();
      }

    }

    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
      super.onCharacteristicRead(gatt, characteristic, status);

      Map<UUID, BluetoothGatt> connectedDevices = central.getConnectedDevices();

      boolean isReadCharInstance = handler.getPending() instanceof ReadCharacteristic;
      boolean isSameChar = isReadCharInstance && ((ReadCharacteristic) handler.getPending()).characteristic.getUuid().equals(characteristic.getUuid());
      boolean isSameAddress = isReadCharInstance && ((ReadCharacteristic) handler.getPending()).gatt.getDevice().getAddress().equals(gatt.getDevice().getAddress());

      boolean shouldContinue = isReadCharInstance && isSameChar && isSameAddress;
      if (!shouldContinue) {
        Log.w(TAG, "current task is not read characteristic or not this characteristic, skipping");
        return;
      }


      boolean notSuccessful = status != BluetoothGatt.GATT_SUCCESS;
      boolean notId = !characteristic.getUuid().equals(ID_UUID);
      boolean centralIsOff = !central.getIsOn();
      if (notSuccessful || notId || centralIsOff) {
        Log.w(TAG, "[on char read] stopping from fully connecting" + gatt.getDevice().getName() + gatt.getDevice().getAddress() + "due to notSuccessful:" + notSuccessful + " notId:" + notId + " centralIsOff" + centralIsOff);
        handler.addToQueue(new DisconnectPeripheral(gatt));
        handler.taskEnded();
        return;
      }

      Log.d(TAG, "Read characteristic from " + gatt.getDevice().getName() + " char:" + characteristic.getUuid() + " val:" + Arrays.toString(characteristic.getValue()));
      UUID uuid;
      BluetoothGattCharacteristic idCharacteristic;
      try {
        uuid = ConvertUUID.bytesToUUID(characteristic.getValue());
        idCharacteristic = gatt.getService(CommonConstants.SERVICE_UUID).getCharacteristic(CommonConstants.ID_UUID);
        if (idCharacteristic == null)
          throw new Exception("id characteristic should not be null");
      } catch (Exception e) {
        Log.e(TAG, "error when reading uuid / idCharacteristic" + e);
        handler.addToQueue(new DisconnectPeripheral(gatt));
        handler.taskEnded();
        return;
      }

      Log.d(TAG, "id received" + uuid);
      if (connectedDevices.containsKey(uuid)) {
        Log.d(TAG, "peripheral" + gatt.getDevice().getName() + gatt.getDevice().getName() + " is already connected with uuid " + uuid + ",disconnecting ");
        handler.addToQueue(new DisconnectPeripheral(gatt));
        handler.taskEnded();
        return;
      }

      Log.d(TAG, "Device UUID of " + gatt.getDevice().getName() + " is : " + uuid);
      handler.addToQueue(new WriteCharacteristic(gatt, idCharacteristic, ConvertUUID.uuidToBytes(handler.getId()), 3, uuid));
      handler.addToQueue(new Scan());
      handler.taskEnded();
    }

    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

      Map<UUID, BluetoothGatt> connectedDevices = central.getConnectedDevices();
      Map<String, BluetoothDevice> connectingDevices = central.getConnectingDevices();
      Map<String, BluetoothDevice> connectTryCount = central.getConnectingDevices();

      super.onCharacteristicWrite(gatt, characteristic, status);
      boolean shouldContinue = handler.getPending() instanceof WriteCharacteristic && ((WriteCharacteristic) handler.getPending()).characteristic == characteristic;
      if (!shouldContinue) {
        Log.w(TAG, "current task is not characteristic writing or not this characteristic, skipping");
        return;
      }
      WriteCharacteristic task = (WriteCharacteristic) handler.getPending();
      String name = task.gatt.getDevice().getName();
      String address = task.gatt.getDevice().getAddress();

      if (status == BluetoothGatt.GATT_SUCCESS && characteristic.getUuid().equals(ID_UUID) && handler.connectedExists(task.uuid)) {
        Log.d(TAG, "device " + name + address + " is already connected, disconnecting");
        handler.addToQueue(new DisconnectPeripheral(gatt, true, false));
        return;
      }

      if (status == BluetoothGatt.GATT_SUCCESS && characteristic.getUuid().equals(ID_UUID)) {
        Log.d(TAG, name + address + " is now connected fully!");
        connectingDevices.remove(address);
        connectedDevices.put(task.uuid, gatt);
        connectTryCount.remove(address);
        handler.notifyConnect(task.uuid);
        handler.taskEnded();
        return;
      }

      if (status == BluetoothGatt.GATT_SUCCESS) {
        Log.d(TAG, "sent data " + Arrays.toString(task.data) + " successfully!");
        handler.taskEnded();
        return;
      }

      if (task.remainingRetries <= 0) {
        Log.d(TAG, "could not send to characteristic after retries, stopping");
        handler.addToQueue(new DisconnectPeripheral(gatt));
        handler.taskEnded();
        return;
      }

      Log.d(TAG, "retrying writing characteristic");
      handler.addToQueue(new WriteCharacteristic(gatt, characteristic, task.data, task.remainingRetries - 1, task.uuid));
      handler.taskEnded();

    }

    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
      super.onCharacteristicChanged(gatt, characteristic);
      Log.d(TAG, "characteristic changed");
      BluetoothDevice device = gatt.getDevice();

      if (characteristic.getUuid().equals(MESSAGE_UUID)) {
        Log.d(TAG, "Received data from " + device.getName() + device.getAddress());
        UUID uuid = central.getPeripheralUUID(device.getAddress());

        if (uuid == null) {
          Log.d(TAG, "peripheral sent a message but was not connected" + device.getName() + device.getAddress() + " disconnecting");
          handler.addToQueue(new DisconnectPeripheral(gatt));
          return;
        }

        handler.notifyData(uuid, characteristic.getValue());
      } else {
        Log.e(TAG, "unexpected characteristic was written:" + characteristic.getUuid());
        handler.addToQueue(new DisconnectCentral(device));
      }
    }
  };

  CentralBLEEventHandler(Central central) {
    this.handler = central.getBLEHandler();
    this.central = central;
    this.TAG = central.TAG;
  }

  ScanCallback getScanCallback() {
    return scanCallback;
  }

  BluetoothGattCallback getGattCallback() {
    return gattCallback;
  }


  private final ScanCallback scanCallback = new ScanCallback() {
    @Override
    public void onScanResult(int callbackType, ScanResult result) {
      BluetoothDevice device = result.getDevice();
      HashSet<String> scanResultAddresses = central.getScanResultAddresses();

      boolean shouldContinue = handler.getPending() instanceof Scan;
      if (!shouldContinue) {
        Log.w(TAG, "current task is not scan, skipping. stopping scan");
        central.stopScan();
        handler.addToQueue(new Scan());
        return;
      }

      if (scanResultAddresses.contains(device.getAddress())) return;
      scanResultAddresses.add(device.getAddress());

      if (central.avoidConnectingToPeripheral(device)) {
        Log.d(TAG, "skipping connecting to" + device.getName() + device.getAddress());
        return;
      }

      handler.addToQueue(new ConnectToPeripheral(device));

      ((Scan) handler.getPending()).devicesBeforeConnect -= 1;
      int remaining = ((Scan) handler.getPending()).devicesBeforeConnect;
      if (remaining <= 0) {
        central.stopScan();
        handler.taskEnded();
      }
    }

    @Override
    public void onScanFailed(int errorCode) {
      super.onScanFailed(errorCode);
      boolean shouldContinue = handler.getPending() instanceof Scan;
      if (!shouldContinue) {
        Log.w(TAG, "current task is not scan, skipping handling scan fail");
        return;
      }

      Log.d(TAG, "scan failed, code: " + errorCode + " adding scan task again");
      handler.addToQueue(new Scan(((Scan) handler.getPending()).devicesBeforeConnect));

      central.stopScan();
      handler.taskEnded();
    }
  };


}
