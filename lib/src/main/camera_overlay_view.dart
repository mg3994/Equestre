import 'dart:async';

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
  //
  Timer? _horseTimer;
  int _horseNumber = 7; // initial horse number
  bool _isRecording = false;
  //
  @override
  void dispose() {
    // TODO: implement dispose
    _horseTimer?.cancel();
    super.dispose();
  }

  @override
  void initState() {
    super.initState();
    updateOverlay(overlayConfig);
    _horseTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
       _horseNumber++;
       setState(() {
         updateOverlay(overlayConfig.copyWith(
           horseNumber: OverlayItem(
             text: '#$_horseNumber',
             textSizePx: 26,
             bgColor: '#AA111111',
             fgColor: '#FFFFFF',
             x: -0.9,
             y: 0.65,
           ),
         ));
       });
    });
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
    return Scaffold(
      body: Stack(
        children: [
          Container(
        color: Colors.white,
        child: AndroidView(
          viewType: 'camera_overlay_view',
          creationParams: const {}, // Optionally pass initial overlay
          creationParamsCodec: const StandardMessageCodec(),
        ),
          ),
          Positioned(
            top: 20,
            left: 20,
            child: ElevatedButton(
              onPressed: () async {
                if (_isRecording) {
                  final stopped = await stopRecording();
                  debugPrint('Recording stopped: $stopped');
                } else {
                  final started = await startRecording();
                  debugPrint('Recording started: $started');
                }
                setState(() {
                  _isRecording = !_isRecording;
                });
              },
              child: Text(_isRecording ? 'Stop Recording' : 'Start Recording'),
            ),
          ),
        ],
      ),
    );
  }
}
