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
import io.github.meshbase.mesh_base_core.router.ProtocolType;
import io.github.meshbase.mesh_base_core.router.RawBytesBody;
import io.github.meshbase.mesh_base_core.router.FileTransferBody;
import io.github.meshbase.mesh_base_core.temptest.TempTest;
import io.github.meshbase.mesh_base_core.mesh_manager.MeshManager;
import io.github.meshbase.mesh_base_core.router.SendListener;

public class MeshBaseFlutterPlugin implements FlutterPlugin, MethodCallHandler, ActivityAware, PluginRegistry.ActivityResultListener,
        PluginRegistry.RequestPermissionsResultListener

        , EventChannel.StreamHandler {
    private MethodChannel methodChannel;
    private EventChannel eventChannel;
    private EventChannel.EventSink eventSink;
    private MeshManager meshManager = null;
    String TAG = "my_example_plugin";

    private boolean hasSubscribed = false;

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        methodChannel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "mesh_base_flutter");
        methodChannel.setMethodCallHandler(this);
        eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "mesh_manager/events");
        // 2. set this class as the handler for stream events
        eventChannel.setStreamHandler(this);
        Log.d(TAG, new TempTest().getName());
    }

    @Override
    public void onListen(Object arguments, EventChannel.EventSink events) {
        Log.d(TAG, "onListen");
        this.eventSink = events;
    }

    @Override
    public void onCancel(Object arguments) {
        Log.d(TAG, "onCancel");
        this.eventSink = null;
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        methodChannel.setMethodCallHandler(null);
        eventChannel.setStreamHandler(null);
        eventChannel = null;
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        if (meshManager == null) {
            meshManager = new MeshManager(binding.getActivity());
        }
        binding.addActivityResultListener(this);
        binding.addRequestPermissionsResultListener(this);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        if (meshManager == null) {
            meshManager = new MeshManager(binding.getActivity());
        }
    }

    @Override
    public void onDetachedFromActivity() {
    }

    @Override
    public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (meshManager != null) {
            meshManager.onPermissionResult(requestCode);
        }
        return false;
    }

    @Override
    public boolean onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (meshManager != null) {
            meshManager.onPermissionResult(requestCode);
        }
        return false;
    }


    ///
    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {
        Log.d(TAG, "methodCall, method: " + call.method + " ");
        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + Build.VERSION.RELEASE);
                break;

            case "getId":
                result.success(meshManager.getId().toString());
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
                HashMap<String, Object> protocolMap = call.argument("protocol");
                if (protocolMap == null) {
                    throw new RuntimeException("protocol map from flutter is null, map=" + call.arguments);
                }
                ChannelMeshProtocol channelProtocol = ChannelMeshProtocol.fromMap(protocolMap);
                boolean keepMessageId = Boolean.TRUE.equals(call.argument("keepMessageId"));
                //Assuming only RawBytesBody  is used
                MeshProtocol<?> protocol;

                if (channelProtocol.messageType == ProtocolType.RAW_BYTES_MESSAGE) {
                    protocol =
                            new ConcreteMeshProtocol<>(3, channelProtocol.remainingHops, channelProtocol.messageId, UUID.fromString(channelProtocol.sender),
                                    UUID.fromString(channelProtocol.destination),
                                    new RawBytesBody(channelProtocol.body));
                } else {
                    // assume sending file
                    FileTransferBody fileTransferBody = new FileTransferBody(
                            -1, -1, (short) 0, (short) 0, channelProtocol.body
                    );
                    protocol = new ConcreteMeshProtocol<FileTransferBody>(7, channelProtocol.remainingHops, channelProtocol.messageId, UUID.fromString(channelProtocol.sender),
                            UUID.fromString(channelProtocol.destination),
                            fileTransferBody
                    );
                }

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
                                ChannelMeshProtocol channelProtocol = protocolToChannelProtocol(protocol);
                                result.success(toSendResponseMap(false, channelProtocol.toMap(), null));
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
                Log.e(TAG, "unknown method in plugin:" + call.method);
                result.notImplemented();
        }
    }


    // subscribe unsubscribe utils
    private void subscribeFlutterListener() {
        //To avoid multiple broadcasts about the same event
        //TODO: implement ids to identify each listener so that its easy to unsubscribe
        if (hasSubscribed) return;
        hasSubscribed = true;
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
        throw new RuntimeException("unsubscribeAllListeners() is not implemented");
    }

    // --- conversion helpers ---
    private Map<String, Object> toSendResponseMap(boolean acked, Map<String, Object> response, String error) {
        Map<String, Object> map = new HashMap<>();
        map.put("acked", acked);
        map.put("response", response);
        map.put("error", null);
        if (error != null) {
            Map<String, String> errorMap = new HashMap<>();
            errorMap.put("message", error);
            map.put("error", errorMap);
        }
        return map;
    }

    private ChannelMeshProtocol protocolToChannelProtocol(MeshProtocol<?> protocol) {

        byte[] content;
        if (protocol.getByteType() == ProtocolType.RAW_BYTES_MESSAGE) {
            content = RawBytesBody.decode(protocol.body.encode()).getContent();
        } else {
            content = protocol.body.encode();
        }

        return new ChannelMeshProtocol(
                protocol.getByteType(),
                protocol.getRemainingHops(),
                protocol.getMessageId(),
                protocol.sender.toString(),
                protocol.destination.toString(),
                content
        );
    }

    private List<Map<String, Object>> devicesToListOfMap(ArrayList<Device> list) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (Device device : list) {
            out.add(deviceToMap(device));
        }
        return out;
    }

    private Map<String, Object> deviceToMap(Device device) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", device.name);
        map.put("uuid", device.uuid.toString());
        return map;
    }

    private Map<String, Object> statusToMap(Status status) {
        Map<String, Object> map = new HashMap<>();
        map.put("isOn", status.isOn());
        Map<String, Map<String, Object>> handlers = new HashMap<>();
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
        Map<String, Object> map = new HashMap<>();
        map.put("type", type);
        map.put("payload", payload);
        Log.d(TAG, "invoking type:" + type + " payload:" + payload);
        eventSink.success(map);
    }

}
