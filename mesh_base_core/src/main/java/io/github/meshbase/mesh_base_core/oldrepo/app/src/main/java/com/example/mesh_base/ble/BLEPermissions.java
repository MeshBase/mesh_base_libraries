package com.example.mesh_base.ble;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;
import static android.Manifest.permission.ACCESS_FINE_LOCATION;
import static android.Manifest.permission.BLUETOOTH;
import static android.Manifest.permission.BLUETOOTH_ADMIN;
import static android.Manifest.permission.BLUETOOTH_ADVERTISE;
import static android.Manifest.permission.BLUETOOTH_CONNECT;
import static android.Manifest.permission.BLUETOOTH_SCAN;
import static android.content.Context.LOCATION_SERVICE;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.util.Log;

import androidx.activity.ComponentActivity;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.IntentSenderRequest;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;

import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;

import java.util.ArrayList;
import java.util.Map;


public class BLEPermissions {

  private final String TAG = "my_BlePermissions";

  private final ComponentActivity activity;
  private final Listener defaultListener = new Listener() {
    @Override
    public void onEnabled() {
      Log.d(TAG, "BLE enabled (listener not set yet)");
    }

    @Override
    public void onDisabled() {
      Log.d(TAG, "BLE disabled (listener not set yet)");
    }
  };
  private Listener listener = defaultListener;
  ActivityResultLauncher<String[]> permissionLauncher;
  ActivityResultLauncher<IntentSenderRequest> locationLauncher;
  ActivityResultCallback<Map<String, Boolean>> permissionsCallback = o -> {
    if (!hasPermissions()) {
      Log.d(TAG, "Permission not granted");
      listener.onDisabled();
    } else if (isEnabled()) {
      Log.d(TAG, "Permission granted");
      listener.onEnabled();
    } else {
      enable();
    }
  };
  BroadcastReceiver locationCallback = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.d(TAG, "broadcast received" + action);

      //may risk receiving events that are not about the location being turned on or off
      //assuming that multiple listener.enable() calls don't cause problems

      if (!locationIsOn()) {
        listener.onDisabled();
      } else if (isEnabled()) {
        listener.onEnabled();
      } else {
        enable();
      }
    }
  };
  BroadcastReceiver bluetoothCallback = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action = intent.getAction();
      Log.d(TAG, "broadcast received" + action);

      int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR);
      boolean isOnOrOff = state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_ON;
      if (!isOnOrOff) {
        Log.d(TAG, "unknown bluetooth state");
        return;
      }
      if (!bluetoothIsOn()) {
        listener.onDisabled();
      } else if (isEnabled()) {
        listener.onEnabled();
      } else {
        enable();
      }
    }
  };


  public BLEPermissions(ComponentActivity activity, Listener listener) {
    this.activity = activity;
    this.listener = listener;

    //setup permissions
    permissionLauncher = activity.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissionsCallback);

    //setup bluetooth
    IntentFilter bluetoothFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
    activity.registerReceiver(bluetoothCallback, bluetoothFilter);


    //setup location
    locationLauncher = activity.registerForActivityResult(
            new ActivityResultContracts.StartIntentSenderForResult(),
            o -> Log.d(TAG, "location prompt ended")
    );
    IntentFilter locationFilter = new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION);
    activity.registerReceiver(locationCallback, locationFilter);
  }

  public void setListener(Listener listener) {
    this.listener = listener;
  }

  public void enable() {
    if (listener == defaultListener) {
      throw new RuntimeException("please set a listener first before calling enable on BLE Permissions");
    }
    //TODO: handle permanent denial of permissions
    if (isEnabled()) {
      listener.onEnabled();
      return;
    }

    if (!isSupported()) {
      Log.e(TAG, "bluetooth is not supported, ignoring enable() call");
      return;
    }
    Log.d(TAG, "trying to enable bluetooth and its permissions");
    //permissions first, then bluetooth, then location so users see the reasoning better
    if (!hasPermissions()) {
      Log.d(TAG, "requesting permissions");
      permissionLauncher.launch(getPermissions());
    } else if (!bluetoothIsOn()) {
      promptBluetooth();
    } else if (!locationIsOn()) {
      promptLocation();
    }
  }

  public boolean isSupported() {
    BluetoothManager bluetoothManager = activity.getSystemService(BluetoothManager.class);
    if (bluetoothManager == null) return false;
    BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
    return bluetoothAdapter != null;
  }

  private String[] getPermissions() {
    ArrayList<String> permissions = new ArrayList<>();
    //following startScan permissions -  https://developer.android.com/reference/android/bluetooth/le/BluetoothLeScanner#startScan(android.bluetooth.le.ScanCallback)
    permissions.add(ACCESS_COARSE_LOCATION);
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      permissions.add(ACCESS_FINE_LOCATION);
    }
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
      permissions.add(BLUETOOTH_ADMIN);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions.add(BLUETOOTH_SCAN);
      permissions.add(ACCESS_FINE_LOCATION);
    }

    //stop scan uses the same permissions as start scan

    // following connect gatt permissions - https://developer.android.com/reference/android/bluetooth/BluetoothDevice#connectGatt(android.content.Context,%20boolean,%20android.bluetooth.BluetoothGattCallback)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions.add(BLUETOOTH_CONNECT);
    }

    //following advertise permissions - https://developer.android.com/reference/android/bluetooth/le/BluetoothLeAdvertiser
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions.add(BLUETOOTH_ADVERTISE);
    }
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
      permissions.add(BLUETOOTH_ADMIN);
    }

    //following device.getname permissions - https://developer.android.com/reference/android/bluetooth/BluetoothDevice#public-methods
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
      permissions.add(BLUETOOTH);
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions.add(BLUETOOTH_CONNECT);
    }

    //following gatt.writeCharacteristic permissions -https://developer.android.com/reference/android/bluetooth/BluetoothGatt#writeCharacteristic(android.bluetooth.BluetoothGattCharacteristic)
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R) {
      permissions.add(BLUETOOTH);
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
      permissions.add(BLUETOOTH_CONNECT);
    }

    //gatt.readCharacteristic has same permissions
    //gattServer.sendResponse has same permissions
    // new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE) also uses the above permissions

    return permissions.toArray(new String[0]);
  }

  private boolean hasPermissions() {
    for (String permission : this.getPermissions()) {
      if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
        return false;
      }
    }
    return true;
  }

  private boolean bluetoothIsOn() {
    BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
    if (adapter == null) return false;
    return adapter.isEnabled();
  }

  private boolean locationIsOn() {
    LocationManager manager = (LocationManager) activity.getSystemService(LOCATION_SERVICE);
    if (manager == null) return false;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      return manager.isLocationEnabled();
    } else {
      return manager.isProviderEnabled(LocationManager.GPS_PROVIDER) || manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
    }
  }

  public boolean isEnabled() {
    return hasPermissions() && bluetoothIsOn() && locationIsOn();
  }

  @SuppressLint("MissingPermission")
  private void promptBluetooth() {
    Log.d(TAG, "prompting bluetooth");
    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
    activity.startActivity(enableBtIntent);
  }

  private void promptLocation() {
    Log.d(TAG, "prompting location");
    LocationRequest locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY).setMinUpdateIntervalMillis(5000).build();
    LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder().addLocationRequest(locationRequest);

    //doesn't trigger any prompt, just checking the settings
    Task<LocationSettingsResponse> task = LocationServices.getSettingsClient(activity).checkLocationSettings(builder.build());
    task.addOnSuccessListener(activity, locationSettingsResponse -> {
      Log.e(TAG, "location is already configured properly in the settings but promptLocation() was still called!");
      enable();
    });

    //triggers prompt
    task.addOnFailureListener(activity, e -> {
      Log.e(TAG, "user needs to enable location in settings" + e);
      if (!(e instanceof ResolvableApiException)) {
        Log.e(TAG, "is not resolvable exception" + e);
        listener.onDisabled();
        return;
      }

      Log.e(TAG, "is resolvable exception");
      IntentSenderRequest request = new IntentSenderRequest.Builder(((ResolvableApiException) e).getResolution()).build();
      locationLauncher.launch(request);

    });
  }

  public interface Listener {
    void onEnabled();

    void onDisabled();
  }

}
