import 'package:flutter_test/flutter_test.dart';
import 'package:mesh_base_flutter/mesh_base_flutter.dart';
import 'package:mesh_base_flutter/mesh_base_flutter_platform_interface.dart';
import 'package:mesh_base_flutter/mesh_base_flutter_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockMeshBaseFlutterPlatform
    with MockPlatformInterfaceMixin
    implements MeshBaseFlutterPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final MeshBaseFlutterPlatform initialPlatform = MeshBaseFlutterPlatform.instance;

  test('$MethodChannelMeshBaseFlutter is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelMeshBaseFlutter>());
  });

  test('getPlatformVersion', () async {
    MeshBaseFlutter meshBaseFlutterPlugin = MeshBaseFlutter();
    MockMeshBaseFlutterPlatform fakePlatform = MockMeshBaseFlutterPlatform();
    MeshBaseFlutterPlatform.instance = fakePlatform;

    expect(await meshBaseFlutterPlugin.getPlatformVersion(), '42');
  });
}
