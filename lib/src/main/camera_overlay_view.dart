import 'package:equestre/models/data.dart';
import 'package:equestre/models/overlay_config.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';


class CameraOverlayView extends StatefulWidget {
  const CameraOverlayView({super.key});

  @override
  State<CameraOverlayView> createState() => _CameraOverlayViewState();
}

class _CameraOverlayViewState extends State<CameraOverlayView> {
  static const _channel = MethodChannel('camera_overlay_channel');

  @override
  void initState() {
    super.initState();
    updateOverlay(overlayConfig);
    _channel.setMethodCallHandler(_handleNativeCallbacks);
  }

  Future<void> _handleNativeCallbacks(MethodCall call) async {
    switch (call.method) {
      case 'onRecordingStopped':
        final reason = call.arguments['reason'];
        final filePath = call.arguments['filePath'];
        final success = call.arguments['success'];
        debugPrint('📹 Recording stopped: $reason => $filePath');
        if (mounted && success == true && filePath != null) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text("Video saved: $filePath")),
          );
        }
        break;
      default:
        debugPrint("Unhandled native call: ${call.method}");
    }
  }

  Future<bool> startRecording() async {
    final result = await _channel.invokeMethod<bool>('startRecording');
    return result ?? false;
  }

  Future<bool> stopRecording() async {
    final result = await _channel.invokeMethod<bool>('stopRecording');
    return result ?? false;
  }

  Future<void> updateOverlay(OverlayConfig config) async {
    await _channel.invokeMethod('updateOverlay', config.toMap());
  }

  @override
  Widget build(BuildContext context) {
    return AndroidView(
      viewType: 'camera_overlay_view',
      creationParams: const {}, // Optionally pass initial overlay
      creationParamsCodec: const StandardMessageCodec(),
    );
  }
}
