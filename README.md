# equestre

A new Flutter project.

## Getting Started

This project is a starting point for a Flutter application.

A few resources to get you started if this is your first Flutter project:

- [Lab: Write your first Flutter app](https://docs.flutter.dev/get-started/codelab)
- [Cookbook: Useful Flutter samples](https://docs.flutter.dev/cookbook)

For help getting started with Flutter development, view the
[online documentation](https://docs.flutter.dev/), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

```dart
    final horse = HorseInfo(
      name: "Thunderbolt",
      number: 7,
      rider: 'Manish Gautam',
    );
    HorseApi().updateHorseInfo(horse);
    // 
    final status = LiveMatchStatus(isLive: true, message: "Match is now live!");
    LiveMatchApi().updateLiveStatus(status);
    // 
    PenaltyInfo penaltyInfo = PenaltyInfo(
      description: "Foul play detected",
      value: 5,
    );
    PenaltyApi().updatePenalty(penaltyInfo);
// 
TimeInfo timeInfo = TimeInfo(seconds:0 , display: "00:00:00s");
     
    TimeApi().updateTime(timeInfo);
    //
    RankInfo rankInfo = RankInfo(position: 1,label: "Gold Medalist",);

      
    RankApi().updateRank(rankInfo);
    //
    GapToBestInfo gapToBestInfo = GapToBestInfo(
     gapSeconds: -1.23,
      display: "-1.23s",
      isFaster: false,
    );
    GapToBestApi().updateGapToBest(gapToBestInfo);
```

```dart
final channel = MethodChannel('camera_overlay_channel');
final bool started = await channel.invokeMethod('startRecording');
final bool stopped = await channel.invokeMethod('stopRecording');
```

```dart
await methodChannel.invokeMethod('updateOverlay', {
  // ───── HORSE NAME ─────
  'horseName': 'Lightning Bolt',
  'horseNameTextSizePx': 28,
  'horseNameBgColor': '#FF000000',
  'horseNameFgColor': '#00FF00',
  'horseNameX': -0.8,
  'horseNameY': 0.75,

  // ───── HORSE NUMBER ─────
  'horseNumber': '#7',
  'horseNumberTextSizePx': 26,
  'horseNumberBgColor': '#AA111111',
  'horseNumberFgColor': '#FFFFFF',
  'horseNumberX': -0.9,
  'horseNumberY': 0.65,

  // ───── RIDER ─────
  'rider': 'John Doe',
  'riderTextSizePx': 24,
  'riderBgColor': '#88110088',
  'riderFgColor': '#FFD700',
  'riderX': -0.85,
  'riderY': 0.85,

  // ───── PENALTY ─────
  'penalty': '0.5s',
  'penaltyTextSizePx': 22,
  'penaltyBgColor': '#88FF0000',
  'penaltyFgColor': '#FFFFFF',
  'penaltyX': 0.6,
  'penaltyY': -0.95,

  // ───── TIME TAKEN ─────
  'timeTaken': '52.34s',
  'timeTakenTextSizePx': 22,
  'timeTakenBgColor': '#880000FF',
  'timeTakenFgColor': '#FFFFFF',
  'timeTakenX': 0.75,
  'timeTakenY': -0.95,

  // ───── RANK ─────
  'rank': '2nd Place',
  'rankTextSizePx': 22,
  'rankBgColor': '#FF880000',
  'rankFgColor': '#FFFFFF',
  'rankX': 0.85,
  'rankY': -0.95,

  // ───── GAP TO BEST ─────
  'gapToBest': '+1.23s',
  'gapToBestTextSizePx': 22,
  'gapToBestBgColor': '#FF008800',
  'gapToBestFgColor': '#FFFFFF',
  'gapToBestX': 0.95,
  'gapToBestY': -0.95,

  // ───── LIVE MESSAGE ─────
  'liveMsg': 'LIVE: Round 2 in Progress',
  'liveMsgTextSizePx': 26,
  'liveMsgBgColor': '#CC000000',
  'liveMsgFgColor': '#00FFFF',
  'liveMsgX': 0.0,
  'liveMsgY': 0.95,
});
```



```dart
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

// Your existing OverlayItem and OverlayConfig classes (unchanged)
class OverlayItem {
  final String? text;
  final int? textSizePx;
  final String? bgColor; // Hex string, e.g., "#FF0000" or "FF0000"
  final String? fgColor; // Hex string
  final double? x; // Relative 0.0 to 1.0
  final double? y; // Relative 0.0 to 1.0

  OverlayItem({
    this.text,
    this.textSizePx,
    this.bgColor,
    this.fgColor,
    this.x,
    this.y,
  });

  OverlayItem copyWith({
    String? text,
    int? textSizePx,
    String? bgColor,
    String? fgColor,
    double? x,
    double? y,
  }) {
    return OverlayItem(
      text: text ?? this.text,
      textSizePx: textSizePx ?? this.textSizePx,
      bgColor: bgColor ?? this.bgColor,
      fgColor: fgColor ?? this.fgColor,
      x: x ?? this.x,
      y: y ?? this.y,
    );
  }

  Map<String, dynamic> toMap(String prefix) {
    final map = <String, dynamic>{};
    if (text != null) map['${prefix}Text'] = text;
    if (textSizePx != null) map['${prefix}TextSizePx'] = textSizePx;
    if (bgColor != null) map['${prefix}BgColor'] = bgColor;
    if (fgColor != null) map['${prefix}FgColor'] = fgColor;
    if (x != null) map['${prefix}X'] = x;
    if (y != null) map['${prefix}Y'] = y;
    return map;
  }
}

class OverlayConfig {
  final OverlayItem? horseName;
  final OverlayItem? horseNumber;
  final OverlayItem? rider;
  final OverlayItem? penalty;
  final OverlayItem? timeTaken;
  final OverlayItem? rank; // Renamed to avoid conflict with `OverlayItem` class name
  final OverlayItem? gapToBest;
  final OverlayItem? liveMsg;

  OverlayConfig({
    this.horseName,
    this.horseNumber,
    this.rider,
    this.penalty,
    this.timeTaken,
    this.rank,
    this.gapToBest,
    this.liveMsg,
  });

  OverlayConfig copyWith({
    OverlayItem? horseName,
    OverlayItem? horseNumber,
    OverlayItem? rider,
    OverlayItem? penalty,
    OverlayItem? timeTaken,
    OverlayItem? rank,
    OverlayItem? gapToBest,
    OverlayItem? liveMsg,
  }) {
    return OverlayConfig(
      horseName: horseName ?? this.horseName,
      horseNumber: horseNumber ?? this.horseNumber,
      rider: rider ?? this.rider,
      penalty: penalty ?? this.penalty,
      timeTaken: timeTaken ?? this.timeTaken,
      rank: rank ?? this.rank,
      gapToBest: gapToBest ?? this.gapToBest,
      liveMsg: liveMsg ?? this.liveMsg,
    );
  }

  Map<String, dynamic> toMap() {
    final map = <String, dynamic>{};
    if (horseName != null) map.addAll(horseName!.toMap('horseName'));
    if (horseNumber != null) map.addAll(horseNumber!.toMap('horseNumber'));
    if (rider != null) map.addAll(rider!.toMap('rider'));
    if (penalty != null) map.addAll(penalty!.toMap('penalty'));
    if (timeTaken != null) map.addAll(timeTaken!.toMap('timeTaken'));
    if (rank != null) map.addAll(rank!.toMap('rank'));
    if (gapToBest != null) map.addAll(gapToBest!.toMap('gapToBest'));
    if (liveMsg != null) map.addAll(liveMsg!.toMap('liveMsg'));
    return map;
  }
}
// End of your existing OverlayItem and OverlayConfig classes

class MyApp extends StatelessWidget {
  const MyApp({super.key});

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Simple Camera Recording with Overlay',
      theme: ThemeData(
        primarySwatch: Colors.blue,
      ),
      home: const CameraOverlayScreen(),
    );
  }
}

class CameraOverlayScreen extends StatefulWidget {
  const CameraOverlayScreen({super.key});

  @override
  State<CameraOverlayScreen> createState() => _CameraOverlayScreenState();
}

class _CameraOverlayScreenState extends State<CameraOverlayScreen> {
  static const MethodChannel _cameraOverlayChannel = MethodChannel('camera_overlay_channel');
  static const MethodChannel _permissionsChannel = MethodChannel('ch.zeitmessungen.equestre/permissions');

  bool _isRecording = false;
  String? _recordedVideoPath;
  String _recordingStatus = "Idle";

  OverlayConfig _currentConfig = OverlayConfig(
    horseName: OverlayItem(
      text: "Spirit",
      textSizePx: 80,
      bgColor: "#8000FF00", // Semi-transparent green (AARRGGBB)
      fgColor: "#FFFFFFFF", // White text (AARRGGBB)
      x: 0.05,
      y: 0.1,
    ),
    rider: OverlayItem(
      text: "Jessica R.",
      textSizePx: 50,
      fgColor: "#FFFFFF00", // Yellow text
      x: 0.05,
      y: 0.2,
    ),
    timeTaken: OverlayItem(
      text: "00:00.000",
      textSizePx: 100,
      fgColor: "#FFFFFFFF",
      bgColor: "#80000000", // Semi-transparent black background
      x: 0.5, // Centered horizontally
      y: 0.5, // Centered vertically
    ),
    liveMsg: OverlayItem(
      text: "LIVE!",
      textSizePx: 120,
      fgColor: "#FFFFFFFF",
      bgColor: "#80FF0000", // Semi-transparent red
      x: 0.5, // Centered horizontally
      y: 0.8,
    ),
  );

  @override
  void initState() {
    super.initState();
    _requestPermissions(); // Request permissions when the screen initializes
    _cameraOverlayChannel.setMethodCallHandler(_handleNativeMethodCall); // Listen for native events
  }

  Future<void> _handleNativeMethodCall(MethodCall call) async {
    switch (call.method) {
      case 'onRecordingStart':
        setState(() {
          _recordingStatus = "Recording...";
        });
        print('Recording started. Output: ${call.arguments}');
        break;
      case 'onRecordingEnd':
        setState(() {
          _recordingStatus = "Finished: ${call.arguments}";
          _recordedVideoPath = call.arguments;
        });
        print('Recording ended. Output: ${call.arguments}');
        break;
      case 'onRecordingError':
        setState(() {
          _recordingStatus = "Error: ${call.arguments}";
        });
        print('Recording error: ${call.arguments}');
        break;
      default:
        print('Unknown method ${call.method}');
    }
  }

  Future<void> _requestPermissions() async {
    try {
      final bool? granted = await _permissionsChannel.invokeMethod('requestCameraAndAudioPermissions');
      if (granted == true) {
        print("Camera and Audio permissions granted via Flutter.");
        _updateOverlay(); // Only update overlay if permissions are granted
      } else {
        print("Permissions denied. Cannot proceed with camera.");
      }
    } on PlatformException catch (e) {
      print("Failed to request permissions: '${e.message}'.");
    }
  }

  Future<void> _updateOverlay() async {
    try {
      await _cameraOverlayChannel.invokeMethod('updateOverlay', {'data': _currentConfig.toMap()});
      print("Overlay data updated successfully!");
    } on PlatformException catch (e) {
      print("Failed to update overlay data: '${e.message}'.");
    }
  }

  Future<void> _startRecording() async {
    setState(() {
      _isRecording = true;
      _recordingStatus = "Starting recording...";
    });
    try {
      final bool? started = await _cameraOverlayChannel.invokeMethod('startRecording');
      if (started != true) {
        setState(() {
          _isRecording = false;
          _recordingStatus = "Failed to start recording.";
        });
      }
    } on PlatformException catch (e) {
      setState(() {
        _isRecording = false;
        _recordingStatus = "Error starting recording: ${e.message}";
      });
      print("Failed to start recording: '${e.message}'.");
    }
  }

  Future<void> _stopRecording() async {
    setState(() {
      _isRecording = false;
      _recordingStatus = "Stopping recording...";
    });
    try {
      await _cameraOverlayChannel.invokeMethod('stopRecording');
    } on PlatformException catch (e) {
      setState(() {
        _recordingStatus = "Error stopping recording: ${e.message}";
      });
      print("Failed to stop recording: '${e.message}'.");
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Simple Camera Recording with Overlay'),
      ),
      body: Stack(
        children: [
          AndroidView(
            viewType: 'camera_overlay_view',
            layoutDirection: TextDirection.ltr,
            creationParams: _currentConfig.toMap(), // Pass initial data
            creationParamsCodec: const StandardMessageCodec(),
            onPlatformViewCreated: (int id) {
              print('Platform view created with ID: $id');
              // This might update twice if permissions are granted, but safe
              _updateOverlay();
            },
          ),
          Align(
            alignment: Alignment.bottomCenter,
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Text(_recordingStatus, style: const TextStyle(color: Colors.white, fontSize: 16)),
                  const SizedBox(height: 10),
                  ElevatedButton(
                    onPressed: () {
                      setState(() {
                        // Example: Dynamic time update and toggle live message
                        _currentConfig = _currentConfig.copyWith(
                          timeTaken: _currentConfig.timeTaken?.copyWith(
                            text: "Time: ${DateTime.now().second.toString().padLeft(2, '0')}:${DateTime.now().millisecond.toString().padLeft(3, '0')}",
                            fgColor: (_currentConfig.timeTaken?.fgColor == "#FFFFFFFF") ? "#FF00FF00" : "#FFFFFFFF",
                          ),
                          liveMsg: _currentConfig.liveMsg?.copyWith(
                            text: _currentConfig.liveMsg?.text == "LIVE!" ? "REC" : "LIVE!",
                            bgColor: (_currentConfig.liveMsg?.bgColor == "#80FF0000") ? "#800000FF" : "#80FF0000",
                          ),
                          rider: _currentConfig.rider?.copyWith(
                            text: "Rider: Alice " + (DateTime.now().second % 10).toString(),
                          ),
                        );
                      });
                      _updateOverlay();
                    },
                    child: const Text('Update Overlay Data'),
                  ),
                  const SizedBox(height: 10),
                  FloatingActionButton.extended(
                    onPressed: _isRecording ? _stopRecording : _startRecording,
                    label: Text(_isRecording ? 'Stop Recording' : 'Start Recording'),
                    icon: Icon(_isRecording ? Icons.stop : Icons.videocam),
                    backgroundColor: _isRecording ? Colors.red : Colors.green,
                  ),
                  if (_recordedVideoPath != null)
                    Padding(
                      padding: const EdgeInsets.only(top: 10.0),
                      child: Text(
                        "Video saved to: $_recordedVideoPath",
                        style: const TextStyle(color: Colors.white, fontSize: 12),
                        textAlign: TextAlign.center,
                      ),
                    ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }
}
```