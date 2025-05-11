import 'package:flutter_test/flutter_test.dart';
import 'package:mesh_base_flutter/mesh_base_flutter.dart';
import 'package:mesh_base_flutter/mesh_base_flutter_method_channel.dart';
import 'package:mesh_base_flutter/mesh_base_flutter_platform_interface.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockMeshBaseFlutterPlatform
    with MockPlatformInterfaceMixin
    implements MeshBaseFlutterPlatform {
  @override
  Future<String?> getPlatformVersion() => Future.value('42');

  @override
  Future<void> on() async {
    return;
  }

  @override
  Future<void> off() async {
    return;
  }

  @override
  Future<List<Device>> getNeighbors() async {
    return [
      Device(name: 'Test Device 1', uuid: '1'),
      Device(name: 'Test Device 2', uuid: '2'),
    ];
  }

  @override
  Future<MeshStatus> getStatus() async {
    return MeshStatus(isOn: true, statuses: {});
  }

  @override
  Future<SendResult> send({
    required MeshProtocol protocol,
    bool keepMessageId = false,
  }) async {
    return SendResult(acked: true, error: null, response: null);
  }

  @override
  Future<void> subscribe(MeshManagerListener listener) async {}

  @override
  Future<void> unsubscribe() async {}
}

void main() {
  final MeshBaseFlutterPlatform initialPlatform =
      MeshBaseFlutterPlatform.instance;

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
