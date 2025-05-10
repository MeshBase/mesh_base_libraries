package com.example.mesh_base.ble;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

import java.util.Arrays;
import java.util.UUID;

public abstract class BLETask {
    public long expireMilli = 5_000L;
    public abstract String asString();
}

class Scan extends BLETask {
    int devicesBeforeConnect;
    static long MAX_SCAN_DURATION = 3_000;

    Scan(int devicesBeforeConnect){
        this.devicesBeforeConnect =devicesBeforeConnect;
        this.expireMilli = MAX_SCAN_DURATION;
    }

    Scan(){
        this.devicesBeforeConnect = 2; //for less wait time in sparsely populated places
        this.expireMilli = MAX_SCAN_DURATION;
    }

    @Override
    public String asString() {
        return "Scan: devicesBeforeConnect = " + devicesBeforeConnect;
    }
}

abstract class CentralTask extends BLETask {

}

class ConnectToPeripheral extends CentralTask {
    BluetoothDevice device;

    ConnectToPeripheral(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public String asString() {
        @SuppressLint("MissingPermission") String deviceName = (device.getName() != null) ? device.getName() : "Unknown";
        return "ConnectToPeripheral: device = " + deviceName + " (" + device.getAddress() +")";}
}

class DiscoverServices extends CentralTask {
    BluetoothGatt gatt;

    DiscoverServices(BluetoothGatt gatt) {
        this.gatt = gatt;
    }

    @Override
    public String asString() {
        BluetoothDevice device = gatt.getDevice();
        @SuppressLint("MissingPermission") String deviceName = (device.getName() != null) ? device.getName() : "Unknown";
        return "DiscoverServices: device = " + deviceName
                + " (" + device.getAddress() + ")";
    }
}

class ReadCharacteristic extends CentralTask{
    BluetoothGattCharacteristic characteristic;
    BluetoothGatt gatt;
    ReadCharacteristic(BluetoothGattCharacteristic characteristic, BluetoothGatt gatt){
        this.characteristic = characteristic;
        this.gatt = gatt;
    }

    @SuppressLint("MissingPermission")
    @Override
    public String asString() {
        return "read characteristic - "+characteristic.getUuid()+" device:"+gatt.getDevice().getName()+gatt.getDevice().getAddress();
    }
}

class NegotiateMTU extends  CentralTask{

    BluetoothGatt gatt;
    int size;
    NegotiateMTU(BluetoothGatt gatt, int size){
        this.gatt = gatt;
        this.size = size;
        this.expireMilli = 2000L;
    }

    @SuppressLint("MissingPermission")
    @Override
    public String asString() {
        return "Negotiate MTU: "+ gatt.getDevice().getName()+gatt.getDevice().getAddress() + " size:"+size;
    }
}

class EnableIndication extends CentralTask{
    BluetoothGattCharacteristic characteristic;
    BluetoothGatt gatt;
    EnableIndication( BluetoothGattCharacteristic characteristic , BluetoothGatt gatt){
        this.characteristic = characteristic;
        this.gatt = gatt;
    }

    @SuppressLint("MissingPermission")
    @Override
    public String asString() {
        return "Enable Indication - "+characteristic.getUuid()+" device:"+gatt.getDevice().getName()+gatt.getDevice().getAddress();
    }

}
class WriteCharacteristic extends CentralTask {
    BluetoothGatt gatt;
    byte[] data;
    int remainingRetries;
    BluetoothGattCharacteristic characteristic;
    UUID uuid;

    WriteCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, byte[] data, int remainingRetries, UUID uuid) {
        this.gatt = gatt;
        this.data = data;
        this.characteristic = characteristic;
        this.remainingRetries = remainingRetries;
        this.expireMilli = 2000;
        this.uuid = uuid;
    }

    @Override
    public String asString() {
        BluetoothDevice device = gatt.getDevice();
        @SuppressLint("MissingPermission") String deviceName = (device.getName() != null) ? device.getName() : "Unknown";
        return "WriteCharacteristic: device = " + deviceName
                + " (" + device.getAddress() + "), data = " + Arrays.toString(data)
                + ", remainingRetries = " + remainingRetries;
    }
}

class DisconnectPeripheral extends CentralTask {
    BluetoothGatt gatt;
    boolean forgetRetries = false;
    boolean tryReconnect = true;

    DisconnectPeripheral(BluetoothGatt gatt) {
        this.gatt = gatt;
    }
    DisconnectPeripheral(BluetoothGatt gatt,  boolean forgetRetries, boolean tryReconnect) {
        this.gatt = gatt;
        this.forgetRetries = forgetRetries;
        this.tryReconnect = tryReconnect;
    }

    @Override
    public String asString() {
        BluetoothDevice device = gatt.getDevice();
        @SuppressLint("MissingPermission") String deviceName = (device.getName() != null) ? device.getName() : "Unknown";
        return "DisconnectPeripheral: device = " + deviceName
                + " (" + device.getAddress() + ")" ;
    }
}



abstract class PeripheralTask extends BLETask {

}
class StartGattServer extends PeripheralTask {

    @Override
    public String asString() {
        return "StartGattServer";
    }
}

class Advertise extends PeripheralTask {

    Advertise(){
        this.expireMilli = 10_000; //needs longer time
    }
    @Override
    public String asString() {
        return "Advertise";
    }
}

class ConnectCentral extends PeripheralTask {
    BluetoothDevice device;

    ConnectCentral(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public String asString() {
        @SuppressLint("MissingPermission") String deviceName = (device.getName() != null) ? device.getName() : "Unknown";
        return "ConnectCentral: device = " + deviceName
                + " (" + device.getAddress() + ")";
    }
}


class IndicateCharacteristic extends PeripheralTask{

    int remainingRetries;
    byte[] value;
    BluetoothDevice device;
    BluetoothGattCharacteristic characteristic;
    IndicateCharacteristic(int remainingRetries,BluetoothGattCharacteristic characteristic, byte[] value, BluetoothDevice device){
        this.remainingRetries = remainingRetries;
        this.characteristic = characteristic;
        this.value = value;
        this.device = device;
        this.expireMilli = 2000L;
    }
    @Override
    public String asString() {
        return "IndicateCharacteristic, retries:"+remainingRetries;

    }
}

class DisconnectCentral extends PeripheralTask {
    BluetoothDevice device;

    DisconnectCentral(BluetoothDevice device) {
        this.device = device;
    }

    @Override
    public String asString() {
        @SuppressLint("MissingPermission") String deviceName = (device.getName() != null) ? device.getName() : "Unknown";
        return "DisconnectCentral: device = " + deviceName
                + " (" + device.getAddress() + ")";
    }
}

class CloseGatt extends PeripheralTask {

    @Override
    public String asString() {
        return "CloseGatt";
    }
}
