import 'package:flutter_test/flutter_test.dart';
import 'package:texture_camera/texture_camera.dart';
import 'package:texture_camera/texture_camera_platform_interface.dart';
import 'package:texture_camera/texture_camera_method_channel.dart';
import 'package:plugin_platform_interface/plugin_platform_interface.dart';

class MockTextureCameraPlatform
    with MockPlatformInterfaceMixin
    implements TextureCameraPlatform {

  @override
  Future<String?> getPlatformVersion() => Future.value('42');
}

void main() {
  final TextureCameraPlatform initialPlatform = TextureCameraPlatform.instance;

  test('$MethodChannelTextureCamera is the default instance', () {
    expect(initialPlatform, isInstanceOf<MethodChannelTextureCamera>());
  });

  test('getPlatformVersion', () async {
    TextureCamera textureCameraPlugin = TextureCamera();
    MockTextureCameraPlatform fakePlatform = MockTextureCameraPlatform();
    TextureCameraPlatform.instance = fakePlatform;

    expect(await textureCameraPlugin.getPlatformVersion(), '42');
  });
}
