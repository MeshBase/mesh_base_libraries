package com.example.mesh_base.ble;

import static com.example.mesh_base.ble.BLEConnectionHandler.CTRL;
import static com.example.mesh_base.ble.CommonConstants.CCCD_UUID;
import static com.example.mesh_base.ble.CommonConstants.MESSAGE_UUID;
import static com.example.mesh_base.ble.CommonConstants.SERVICE_UUID;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.UUID;


@SuppressLint("MissingPermission")
public class Central {
  static final int MAX_MTU_SIZE = 517;
  private static final int MAX_PERIPHERAL_RETRIES = 7;
  private static final long SCAN_TIME_GAP = 6_500; //6.5 seconds
  public final Map<String, Integer> connectTryCount = new HashMap<>();
  final String TAG;
  private final HashMap<String, BluetoothDevice> connectingDevices = new HashMap<>();
  private final Map<UUID, BluetoothGatt> connectedDevices = new HashMap<>();
  private final HashSet<String> scanResultAddresses = new HashSet<>();
  private final BLEConnectionHandler handler;
  CentralBLEEventHandler eventHandler;
  private BluetoothLeScanner scanner;
  private boolean isOn = false;
  private boolean isScanning = false;
  private long lastScanTime = 0;

  Central(BLEConnectionHandler handler) {
    this.handler = handler;
    this.TAG = handler.TAG + CTRL;
    this.eventHandler = new CentralBLEEventHandler(this);
  }

  BluetoothGatt getPeripheral(UUID id) {
    return connectedDevices.get(id);
  }

  HashSet<String> getScanResultAddresses() {
    return scanResultAddresses;
  }

  boolean getIsOn() {
    return isOn;
  }

  Map<String, BluetoothDevice> getConnectingDevices() {
    return connectingDevices;
  }

  Map<UUID, BluetoothGatt> getConnectedDevices() {
    return connectedDevices;
  }

  Map<String, Integer> getConnectTryCount() {
    return connectTryCount;
  }

  BLEConnectionHandler getBLEHandler() {
    return handler;
  }

  void start() {
    isOn = true;
    //1 device to not make users wait too much for the first connection
    if (!isScanning) handler.addToQueue(new Scan(1));
  }

  void startScan(Scan task) {
    boolean isOnlyTask = handler.getQueue().isEmpty();
    if (!isOn || isScanning || !isOnlyTask) {
      Log.d(TAG, "ignoring scan task due to centralIsOff: " + !isOn + " scanning already: " + isScanning + " notOnlyTaskInQueue:" + !isOnlyTask + " (queueSize=" + handler.getQueue().size() + ")");
      handler.taskEnded();
      return;
    }

    if (scanner == null) {
      BluetoothManager btManager = (BluetoothManager) handler.getContext().getSystemService(Context.BLUETOOTH_SERVICE);
      if (btManager == null) {
        Log.e(TAG, "btManager is null, cant scan");
        handler.taskEnded();
        return;
      }

      scanner = btManager.getAdapter().getBluetoothLeScanner();
      if (scanner == null) {
        Log.e(TAG + CTRL, "scanner is null, cant scan");
        handler.taskEnded();
        return;
      }
    }


    long timeGap = System.currentTimeMillis() - lastScanTime;
    if (timeGap < SCAN_TIME_GAP) {
      long addAfter = SCAN_TIME_GAP - timeGap;
      Log.d(TAG, "scanning too early for the 5 scans per 30 seconds rule. re adding task after " + addAfter + " milliseconds");

      handler.taskEnded();
      new Handler(Looper.getMainLooper()).postDelayed(() -> handler.addToQueue(new Scan(task.devicesBeforeConnect)), addAfter);

      return;
    }

    scanResultAddresses.clear();
    isScanning = true;
    ScanFilter filter = new ScanFilter.Builder().setServiceUuid(new android.os.ParcelUuid(CommonConstants.SERVICE_UUID)).build();
    scanner.startScan(Collections.singletonList(filter), new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE) //detects far away devices
            .build(), eventHandler.getScanCallback());
    lastScanTime = System.currentTimeMillis();
  }


  void expireScan(Scan task) {
    stopScan();
  }

  void stopScan() {
    isScanning = false;
    if (scanner == null) {
      Log.e(TAG + CTRL, "scanner is null, skipping stopping scan");
    } else {
      scanner.stopScan(eventHandler.getScanCallback());
    }
  }

  UUID getPeripheralUUID(String address) {
    for (UUID key : connectedDevices.keySet()) {
      BluetoothGatt gatt = connectedDevices.get(key);
      assert gatt != null;
      if (gatt.getDevice().getAddress().equals(address)) {
        return key;
      }
    }
    return null;
  }


  boolean avoidConnectingToPeripheral(BluetoothDevice device) {
    String address = device.getAddress();
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    String name = adapter != null ? adapter.getName() : "unknown";
    String otherName = device.getName() == null ? "unknown" : device.getName();
    int retries = connectTryCount.getOrDefault(address, 0);
    boolean hasBiggerHash = (name.hashCode() ^ 0x5bf03635) > (otherName.hashCode() ^ 0x5bf03635);

    boolean tooManyRetries = retries > MAX_PERIPHERAL_RETRIES;
    boolean isAlreadyConnecting = connectingDevices.containsKey(address);
    boolean isAlreadyConnected = getPeripheralUUID(address) != null;
    //To avoid duplicate central-peripheral connection in both directions. XOR so that hashes are more evenly distributed.
    boolean avoidConViaCentral = hasBiggerHash && isOn && handler.peripheralIsOn();

    boolean avoid = !isOn || isAlreadyConnecting || tooManyRetries || isAlreadyConnected || avoidConViaCentral;
    if (avoid && !isAlreadyConnected) {
      Log.d(TAG, "Avoiding connection - "
              + "centralOff: " + !isOn
              + ", connecting: " + isAlreadyConnecting
              + ", retriesOverMax: " + (retries > MAX_PERIPHERAL_RETRIES)
              + ", avoidConViaCentral: " + avoidConViaCentral);
    }

    return avoid;
  }

  void startConnectToPeripheral(ConnectToPeripheral task) {
    BluetoothDevice device = task.device;
    if (avoidConnectingToPeripheral(task.device)) {
      Log.d(TAG, "dropping connecting to" + device.getName());
      handler.taskEnded();
      return;
    }

    connectingDevices.put(device.getAddress(), device);
    device.connectGatt(handler.getContext(), false, eventHandler.getGattCallback(), BluetoothDevice.TRANSPORT_LE);
  }

  void expireConnectToPeripheral(ConnectToPeripheral task) {
    int count = connectTryCount.getOrDefault(task.device.getAddress(), 0);
    connectTryCount.put(task.device.getAddress(), count + 1);
    connectingDevices.remove(task.device.getAddress());
  }

  void startDisconnectPeripheral(DisconnectPeripheral task) {
    boolean isConnecting = connectingDevices.containsKey(task.gatt.getDevice().getAddress());
    boolean isConnected = connectedDevices.containsKey(getPeripheralUUID(task.gatt.getDevice().getAddress()));
    if (!isConnecting && !isConnected) {
      Log.d(TAG, "skipping disconnect because" + task.gatt.getDevice().getName() + task.gatt.getDevice().getAddress() + "is already disconnected");
      handler.taskEnded();
      return;
    }

    task.gatt.disconnect();
  }

  void expireDisconnectPeripheral(DisconnectPeripheral task) {
    String address = task.gatt.getDevice().getAddress();
    task.gatt.close();
    UUID uuid = getPeripheralUUID(address);
    handler.notifyDisconnect(uuid);
    connectingDevices.remove(address);
    connectedDevices.remove(uuid);
    if (task.forgetRetries) {
      connectTryCount.remove(address);
    }
  }

  void startNegotiateMTU(NegotiateMTU task) {
    task.gatt.requestMtu(task.size);
  }

  void expireNegotiateMTU(NegotiateMTU task) {
    handler.addToQueue(new DisconnectPeripheral(task.gatt));
  }

  void startDiscoverServices(DiscoverServices task) {
    //in ui thread to prevent waiting for an service discovered callback that was actually dropped somehow. according to https://punchthrough.com/android-ble-guide/
    new Handler(Looper.getMainLooper()).post(() -> task.gatt.discoverServices());
  }

  void expireDiscoverServices(DiscoverServices task) {
    handler.addToQueue(new DisconnectPeripheral(task.gatt));
  }

  void startReadingCharacteristic(ReadCharacteristic task) {
    task.gatt.readCharacteristic(task.characteristic);
  }

  void expireReadingCharacteristic(ReadCharacteristic task) {
    handler.addToQueue(new DisconnectPeripheral(task.gatt));
  }

  void startEnablingIndication(EnableIndication task) {
    BluetoothGattDescriptor descriptor = task.characteristic.getDescriptor(CCCD_UUID);
    boolean success = task.gatt.setCharacteristicNotification(task.characteristic, true);
    if (!success || descriptor == null) {
      Log.w(TAG, "could not enable indication due to couldSetCharNotification:" + !success + " descriptorIsNull:" + (descriptor == null));
      handler.addToQueue(new DisconnectPeripheral(task.gatt));
      handler.taskEnded();
      return;
    }

    descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
    task.gatt.writeDescriptor(descriptor);
  }

  void expireEnablingIndication(EnableIndication task) {
    handler.addToQueue(new DisconnectPeripheral(task.gatt));
  }

  void startWritingCharacteristic(WriteCharacteristic task) {
    task.characteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT);
    task.characteristic.setValue(task.data);
    task.gatt.writeCharacteristic(task.characteristic);
  }


  void expireWritingCharacteristic(WriteCharacteristic task) {
    if (task.remainingRetries > 0) {
      handler.addToQueue(new WriteCharacteristic(task.gatt, task.characteristic, task.data, task.remainingRetries - 1, task.uuid));
    } else {
      handler.addToQueue(new DisconnectPeripheral(task.gatt));
    }
  }

  void stop() {
    if (!isOn) {
      Log.d(TAG, "already off");
      return;
    }

    isOn = false;
    Log.d(TAG, "disconnecting to all connected devices:" + connectedDevices.size() + " but has " + connectingDevices.size() + " connecting devices");
    for (BluetoothGatt gatt : connectedDevices.values()) {
      DisconnectPeripheral task = new DisconnectPeripheral(gatt, true, false);
      //If bluetooth is off, the onConnectionStateChange may not be called by android. so time out faster
      task.expireMilli = 1500L;
      handler.addToQueue(task);
    }
    //so that max retry devices have a chance to try connecting again
    connectTryCount.clear();
    if (handler.getPending() instanceof Scan) stopScan();
  }

  BluetoothGattCharacteristic getMessageCharacteristic(BluetoothGatt gatt) {
    BluetoothGattService service = gatt.getService(SERVICE_UUID);
    if (service == null) {
      Log.e(TAG, "service is null when trying to get message char");
      throw new RuntimeException("service is null when trying to get message char");
    }
    BluetoothGattCharacteristic characteristic = service.getCharacteristic(MESSAGE_UUID);
    if (characteristic == null) {
      Log.e(TAG, "service is null when trying to get message char");
      throw new RuntimeException("service is null when trying to get message char");
    }
    return characteristic;
  }


}
