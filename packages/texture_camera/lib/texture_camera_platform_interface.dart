import 'package:plugin_platform_interface/plugin_platform_interface.dart';

import 'texture_camera_method_channel.dart';

abstract class TextureCameraPlatform extends PlatformInterface {
  /// Constructs a TextureCameraPlatform.
  TextureCameraPlatform() : super(token: _token);

  static final Object _token = Object();

  static TextureCameraPlatform _instance = MethodChannelTextureCamera();

  /// The default instance of [TextureCameraPlatform] to use.
  ///
  /// Defaults to [MethodChannelTextureCamera].
  static TextureCameraPlatform get instance => _instance;

  /// Platform-specific implementations should set this with their own
  /// platform-specific class that extends [TextureCameraPlatform] when
  /// they register themselves.
  static set instance(TextureCameraPlatform instance) {
    PlatformInterface.verifyToken(instance, _token);
    _instance = instance;
  }

  Future<String?> getPlatformVersion() {
    throw UnimplementedError('platformVersion() has not been implemented.');
  }
}
