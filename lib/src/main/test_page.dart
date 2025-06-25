// import 'package:equestre/src/main/camera_overlay_view.dart';
// import 'package:flutter/material.dart';

// class CameraPage extends StatefulWidget {
//   const CameraPage({super.key});

//   @override
//   State<CameraPage> createState() => _CameraPageState();
// }

// class _CameraPageState extends State<CameraPage> {
//   final GlobalKey<_CameraOverlayViewState> _cameraKey = GlobalKey();

//   late final CameraOverlayView _cameraView;

//   @override
//   void initState() {
//     super.initState();
//     _cameraView = CameraOverlayView(key: _cameraKey);
//   }

//   void _updateOverlay() {
//     final overlay = OverlayConfig(
//       horseName: OverlayItem(
//         text: 'Lightning Bolt',
//         textSizePx: 28,
//         bgColor: '#FF000000',
//         fgColor: '#00FF00',
//         x: -0.8,
//         y: 0.75,
//       ),
//       rider: OverlayItem(
//         text: 'John Doe',
//         textSizePx: 24,
//         bgColor: '#AA111111',
//         fgColor: '#FFD700',
//         x: -0.85,
//         y: 0.85,
//       ),
//       penalty: OverlayItem(
//         text: '0.5s',
//         textSizePx: 22,
//         bgColor: '#88FF0000',
//         fgColor: '#FFFFFF',
//         x: 0.6,
//         y: -0.95,
//       ),
//       timeTaken: OverlayItem(
//         text: '52.34s',
//         textSizePx: 22,
//         bgColor: '#880000FF',
//         fgColor: '#FFFFFF',
//         x: 0.75,
//         y: -0.95,
//       ),
//       rank: OverlayItem(
//         text: '2nd Place',
//         textSizePx: 22,
//         bgColor: '#FF880000',
//         fgColor: '#FFFFFF',
//         x: 0.85,
//         y: -0.95,
//       ),
//       gapToBest: OverlayItem(
//         text: '+1.23s',
//         textSizePx: 22,
//         bgColor: '#FF008800',
//         fgColor: '#FFFFFF',
//         x: 0.95,
//         y: -0.95,
//       ),
//       liveMsg: OverlayItem(
//         text: 'LIVE: Round 2 in Progress',
//         textSizePx: 26,
//         bgColor: '#CC000000',
//         fgColor: '#00FFFF',
//         x: 0.0,
//         y: 0.95,
//       ),
//     );
//     _cameraKey.currentState?.updateOverlay(overlay);
//   }

//   void _startRecording() async {
//     final started = await _cameraKey.currentState?.startRecording() ?? false;
//     debugPrint('Recording started: $started');
//   }

//   void _stopRecording() async {
//     final stopped = await _cameraKey.currentState?.stopRecording() ?? false;
//     debugPrint('Recording stopped: $stopped');
//   }

//   @override
//   Widget build(BuildContext context) {
//     return Scaffold(
//       appBar: AppBar(title: const Text('Camera Overlay Demo')),
//       body: Column(
//         children: [
//           Expanded(child: _cameraView),
//           Row(
//             mainAxisAlignment: MainAxisAlignment.spaceEvenly,
//             children: [
//               ElevatedButton(onPressed: _updateOverlay, child: const Text('Update Overlay')),
//               ElevatedButton(onPressed: _startRecording, child: const Text('Start')),
//               ElevatedButton(onPressed: _stopRecording, child: const Text('Stop')),
//             ],
//           ),
//         ],
//       ),
//     );
//   }
// }