import 'package:plugin_platform_interface/plugin_platform_interface.dart';

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
}
