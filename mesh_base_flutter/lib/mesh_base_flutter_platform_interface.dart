import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'mesh_base_flutter.dart';
import 'mesh_base_flutter_method_channel.dart';

abstract class MeshBaseFlutterPlatform extends PlatformInterface {
  /// Constructs a MeshBaseFlutterPlatform.
  MeshBaseFlutterPlatform() : super(token: _token);

  static final Object _token = Object();

  static MeshBaseFlutterPlatform _instance = MethodChannelMeshBaseFlutter();

  /// The default instance of [MeshBaseFlutterPlatform] to use.
  ///
  /// Defaults to [MethodChannelMeshBaseFlutter].
  static MeshBaseFlutterPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [MeshBaseFlutterPlatform] when
  /// they register themselves.
  static set instance(MeshBaseFlutterPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }

  Future<String> getId() {
    throw UnimplementedError('getId() has not been implemented.');
  }

  Future<void> on() async {
    throw UnimplementedError('turnOn() has not been implemented.');
  }

  Future<void> off() async {
    throw UnimplementedError('turnOff() has not been implemented.');
  }

  Future<List<Device>> getNeighbors() async {
    throw UnimplementedError('getNeighbors() has not been implemented.');
  }

  Future<MeshStatus> getStatus() async {
    throw UnimplementedError('getStatus() has not been implemented.');
  }

  Future<SendResult> send({
    required MeshProtocol protocol,
    bool keepMessageId = false,
  }) async {
    throw UnimplementedError('send() has not been implemented.');
  }

  Future<void> subscribe(MeshManagerListener listener) async {
    throw UnimplementedError('subscribe() has not been implemented.');
  }

  Future<void> unsubscribe() async {
    throw UnimplementedError('unsubscribe() has not been implemented.');
  }
}
