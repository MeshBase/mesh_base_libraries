
import 'mesh_base_flutter_platform_interface.dart';

class MeshBaseFlutter {
  Future<String?> getPlatformVersion() {
    return MeshBaseFlutterPlatform.instance.getPlatformVersion();
  }
}
