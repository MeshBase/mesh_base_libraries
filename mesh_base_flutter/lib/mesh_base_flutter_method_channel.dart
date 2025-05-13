import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'mesh_base_flutter.dart';
import 'mesh_base_flutter_platform_interface.dart';

/// An implementation of [MeshBaseFlutterPlatform] that uses method channels.
class MethodChannelMeshBaseFlutter extends MeshBaseFlutterPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final _channel = const MethodChannel('mesh_base_flutter');

  static const _eventChannel = EventChannel('mesh_manager/events');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await _channel.invokeMethod<String>('getPlatformVersion');
    return version;
  }

  @override
  Future<String> getId() async {
    final String id = await _channel.invokeMethod('getId');
    return id;
  }

  @override
  Future<void> on() async {
    await _channel.invokeMethod('on');
  }

  @override
  Future<void> off() async {
    await _channel.invokeMethod('off');
  }

  @override
  Future<List<Device>> getNeighbors() async {
    final List result = await _channel.invokeMethod('getNeighbors');
    return result
        .map((e) => Device.fromMap(Map<String, dynamic>.from(e)))
        .toList();
  }

  @override
  Future<MeshStatus> getStatus() async {
    final Map result = await _channel.invokeMethod('getStatus');
    return MeshStatus.fromMap(Map<String, dynamic>.from(result));
  }

  @override
  Future<SendResult> send({
    required MeshProtocol protocol,
    bool keepMessageId = false,
  }) async {
    final result = await _channel.invokeMethod('send', {
      'protocol': protocol.toMap(),
      'keepMessageId': keepMessageId,
    });
    return SendResult.fromMap(Map<String, dynamic>.from(result));
  }

  //listener

  @override
  Future<void> subscribe(MeshManagerListener listener) async {
    _eventChannel.receiveBroadcastStream().listen((dynamic event) {
      final Map map = event;
      var payload = map['payload'];
      switch (map['type']) {
        case 'data':
          //TODO: assuming byte[]
          listener.onDataReceivedForSelf?.call(MeshProtocol.fromMap(payload));
          break;
        case 'status':
          listener.onStatusChange?.call(MeshStatus.fromMap(payload));
          break;
        case 'neighborConnected':
          listener.onNeighborConnected?.call(Device.fromMap(payload));
          break;
        case 'neighborDisconnected':
          listener.onNeighborDisconnected?.call(Device.fromMap(payload));
          break;
        case 'error':
          listener.onError?.call(MeshError.fromMap(payload));
          break;
        default:
          throw Exception("unknown event type:${map['type']} map:$map");
      }
    });
    await _channel.invokeMethod('subscribe');
  }

  //TODO: implement unsubscribe
}
