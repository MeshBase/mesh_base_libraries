package io.github.MeshBase.mesh_base_flutter;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;
import io.github.meshbase.mesh_base_core.global_interfaces.ConnectionHandlersEnum;
import io.github.meshbase.mesh_base_core.global_interfaces.Device;
import io.github.meshbase.mesh_base_core.global_interfaces.SendError;
import io.github.meshbase.mesh_base_core.mesh_manager.MeshManagerListener;
import io.github.meshbase.mesh_base_core.mesh_manager.Status;
import io.github.meshbase.mesh_base_core.router.ConcreteMeshProtocol;
import io.github.meshbase.mesh_base_core.router.MeshProtocol;
import io.github.meshbase.mesh_base_core.router.SendListener;
import io.github.meshbase.mesh_base_core.temptest.TempTest;
import io.github.meshbase.mesh_base_core.mesh_manager.MeshManager;

public class MeshBaseFlutterPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware,  PluginRegistry.ActivityResultListener, EventChannel.StreamHandler {
  private MethodChannel methodChannel;
  private EventChannel eventChannel;
  private EventChannel.EventSink eventSink;
  private MeshManager meshManager = null;
  String TAG = "my_plugin";

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
    methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "mesh_base_flutter");
    methodChannel.setMethodCallHandler(this);
    eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "mesh_manager/events");
    // 2. set this class as the handler for stream events
    eventChannel.setStreamHandler(this);
    Log.d(TAG, new TempTest().getName() );
  }

  @Override
  public void onListen(Object arguments, EventChannel.EventSink events) {
    this.eventSink = events;
  }

  @Override
  public void onCancel(Object arguments) {
    this.eventSink = null;
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    methodChannel.setMethodCallHandler(null);
    eventChannel.setStreamHandler(null);
    eventChannel=null;
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    if (meshManager == null){
      meshManager = new MeshManager(binding.getActivity());
    }
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() { }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    if (meshManager == null){
      meshManager = new MeshManager(binding.getActivity());
    }
  }

  @Override
  public void onDetachedFromActivity() { }

  @Override
  public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    if (meshManager != null){
      meshManager.onActivityResult(requestCode);
    }
    return false;
  }

  ///
  @Override
  public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
    Log.d(TAG, "methodCall, method: "+call.method + " ");
    switch (call.method) {
      case "getPlatformVersion":
        result.success("Android " + Build.VERSION.RELEASE);
        break;

      case "on":
        meshManager.on();
        result.success(null);
        break;

      case "off":
        meshManager.off();
        result.success(null);
        break;

      case "getNeighbors":
        result.success(devicesToListOfMap(meshManager.getNeighbors()));
        break;
      case "getStatus":
        result.success(statusToMap(meshManager.getStatus()));
        break;

      case "send":
        ChannelMeshProtocol channelProtocol = ChannelMeshProtocol.fromMap(call.argument("protocol"));
        boolean keepMessageId = Boolean.TRUE.equals(call.argument("keepMessageId"));
        MeshProtocol<RawBytesBody> protocol =
            new ConcreteMeshProtocol<>(1, -1, -1, UUID.fromString(channelProtocol.sender),
                UUID.fromString(channelProtocol.destination),
                new RawBytesBody(channelProtocol.body));

        meshManager.send(
            protocol,
            new SendListener() {
              @Override
              public void onAck() {
                result.success(toSendResponseMap(true, null, null));
              }

              @Override
              public void onError(SendError e) {
                result.success(toSendResponseMap(false, null, e.getMessage()));
              }

              @Override
              public void onResponse(MeshProtocol<?> protocol) {
                ChannelMeshProtocol channelProtocol =  protocolToChannelProtocol(protocol);
                result.success(toSendResponseMap(false, channelProtocol.toMap(),null));
              }
            },
            keepMessageId
        );
        break;

      case "subscribe":
        subscribeFlutterListener();
        result.success(null);
        break;

      case "unsubscribe":
        unsubscribeAllListeners();
        result.success(null);
        break;

      default:
        Log.e(TAG, "unknown method in plugin:"+call.method);
        result.notImplemented();
    }
  }


  // subscribe unsubscribe utils
  private void subscribeFlutterListener() {
    MeshManagerListener listener = new MeshManagerListener() {
      @Override
      public void onDataReceivedForSelf(MeshProtocol<?> protocol) {
        invokeFlutterEvent("data", protocolToChannelProtocol(protocol).toMap());
      }

      @Override
      public void onStatusChange(Status status) {
        invokeFlutterEvent("status", statusToMap(status));
      }

      @Override
      public void onNeighborConnected(Device device) {
        invokeFlutterEvent("neighborConnected", deviceToMap(device));
      }

      @Override
      public void onNeighborDisconnected(Device device) {
        invokeFlutterEvent("neighborDisconnected", deviceToMap(device));
      }

      @Override
      public void onError(Exception e) {
        invokeFlutterEvent("error", Map.of("message", Objects.requireNonNull(e.getMessage())));
      }
    };
    meshManager.subscribe(listener);
  }

  private void unsubscribeAllListeners() {
    throw new RuntimeException("unsubscribeAllListeners() is not implemented" );
  }

  // --- conversion helpers ---
  private Map<String, Object> toSendResponseMap(boolean acked, Map<String, Object> response, String error){
    Map<String,Object> map = new HashMap<>();
    map.put("acked", acked);
    map.put("response", response);
    Map<String, String> errorMap = new HashMap<>();
    errorMap.put("message", error);
    map.put("error", errorMap);
    return map;
  }

  private ChannelMeshProtocol protocolToChannelProtocol(MeshProtocol<?> protocol) {
    return new ChannelMeshProtocol(
        protocol.getByteType().ordinal(),
        //TODO: make hops publicly visible
        -1,
        protocol.getMessageId(),
        protocol.sender.toString(),
        protocol.destination.toString(),
        protocol.body.encode()
    );
  }

  private List<Map<String,Object>> devicesToListOfMap(ArrayList<Device> list) {
    List<Map<String,Object>> out = new ArrayList<>();
    for (Device device : list) {
      out.add(deviceToMap(device));
    }
    return out;
  }

  private Map<String, Object> deviceToMap(Device device) {
    Map<String,Object> map = new HashMap<>();
    map.put("name", device.name);
    map.put("uuid", device.uuid.toString());
    return map;
  }

  private Map<String,Object> statusToMap(Status status) {
    Map<String,Object> map = new HashMap<>();
    map.put("isOn", status.isOn());
    Map<String, Map<String,Object>> handlers = new HashMap<>();
    for (Map.Entry<ConnectionHandlersEnum, Status.Property> e
        : status.getConnectionStatuses().entrySet()) {
      Status.Property p = e.getValue();
      handlers.put(e.getKey().name(), Map.of(
          "isSupported", p.isSupported(),
          "isOn", p.isOn(),
          "isAllowed", p.isAllowed()
      ));
    }
    map.put("statuses", handlers);
    return map;
  }

  private void invokeFlutterEvent(String type, Object payload) {
    Map<String,Object> map = new HashMap<>();
    map.put("type", type);
    map.put("payload", payload);
    Log.d(TAG, "invoking type:"+type+ " payload:"+payload);
    eventSink.success(map);
  }

}
