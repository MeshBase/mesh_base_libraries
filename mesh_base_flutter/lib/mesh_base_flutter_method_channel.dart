import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'mesh_base_flutter_platform_interface.dart';

/// An implementation of [MeshBaseFlutterPlatform] that uses method channels.
class MethodChannelMeshBaseFlutter extends MeshBaseFlutterPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('mesh_base_flutter');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
