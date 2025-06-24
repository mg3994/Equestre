// import 'package:camerawesome/camerawesome_plugin.dart';
// import 'package:flutter/material.dart';

// class WidgetCameraOrient extends StatefulWidget {
//   const WidgetCameraOrient({
//     super.key,
//     required this.builder,
//     this.rotateWithDevice = true,
//   });

//   /// Builder that receives the current orientation and turns
//   final Widget Function(CameraOrientations orientation, double turns) builder;
//   final bool rotateWithDevice;

//   @override
//   State<WidgetCameraOrient> createState() => _WidgetCameraOrientState();
// }

// class _WidgetCameraOrientState extends State<WidgetCameraOrient> {
//   CameraOrientations currentOrientation = CameraOrientations.portrait_up;
//   double turns = 0;

//   @override
//   void initState() {
//     super.initState();
//     _fetchInitialOrientation();
//   }

//   Future<void> _fetchInitialOrientation() async {
//     final stream = CamerawesomePlugin.getNativeOrientation();
//     if (stream == null) return;
//     await for (final orientation in stream) {
//       setState(() {
//         currentOrientation = orientation;
//         turns = getTurns(orientation);
//       });
//       break;
//     }
//   }

//   @override
//   Widget build(BuildContext context) {
//     if (!widget.rotateWithDevice) {
//       return widget.builder(currentOrientation, 0);
//     }

//     return StreamBuilder<CameraOrientations>(
//       stream: CamerawesomePlugin.getNativeOrientation(),
//       initialData: currentOrientation,
//       builder: (context, snapshot) {
//         final orientation = snapshot.data;
//         if (orientation != null && orientation != currentOrientation) {
//           turns = shortestTurnsToReachTarget(
//             current: turns,
//             target: getTurns(orientation),
//           );
//           currentOrientation = orientation;
//         }

//         return AnimatedRotation(
//           turns: turns,
//           duration: const Duration(milliseconds: 200),
//           curve: Curves.easeInOut,
//           child: widget.builder(currentOrientation, turns),
//         );
//       },
//     );
//   }

//   double getTurns(CameraOrientations orientation) {
//     switch (orientation) {
//       case CameraOrientations.landscape_left:
//         return 0.75;
//       case CameraOrientations.landscape_right:
//         return 0.25;
//       case CameraOrientations.portrait_up:
//         return 0;
//       case CameraOrientations.portrait_down:
//         return 0.5;
//     }
//   }

//   double shortestTurnsToReachTarget({
//     required double current,
//     required double target,
//   }) {
//     final currentDegree = current * 360;
//     final targetDegree = target * 360;
//     final clockwise = (targetDegree - currentDegree + 540) % 360 - 180 > 0;

//     double resultDegree = currentDegree;
//     do {
//       resultDegree += clockwise ? 90 : -90;
//     } while (resultDegree % 360 != targetDegree % 360);

//     return resultDegree / 360;
//   }
// }
