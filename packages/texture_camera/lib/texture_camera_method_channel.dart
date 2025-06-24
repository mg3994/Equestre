import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'texture_camera_platform_interface.dart';

/// An implementation of [TextureCameraPlatform] that uses method channels.
class MethodChannelTextureCamera extends TextureCameraPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('texture_camera');

  @override
  Future<String?> getPlatformVersion() async {
    final version = await methodChannel.invokeMethod<String>('getPlatformVersion');
    return version;
  }
}
