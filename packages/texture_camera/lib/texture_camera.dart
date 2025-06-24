
import 'texture_camera_platform_interface.dart';

class TextureCamera {
  Future<String?> getPlatformVersion() {
    return TextureCameraPlatform.instance.getPlatformVersion();
  }
}
