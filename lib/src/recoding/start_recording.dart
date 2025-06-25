import 'dart:async';
import 'dart:io';
import 'dart:math';

// import 'package:camerawesome/camerawesome_plugin.dart';
// import 'package:camerawesome/pigeon.dart';
// import 'package:camerawesome/camerawesome_plugin.dart';
// import 'package:camerawesome/pigeon.dart';
// import 'package:equestre/utils/extensions/open.dart';
import 'package:equestre/models/data.dart';
import 'package:equestre/models/overlay_config.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:path/path.dart' as p;
import 'package:path_provider/path_provider.dart';
// import 'package:texture_camera/texture_camera.dart';

class StartRecording extends StatefulWidget {
  const StartRecording({super.key, this.eventId});
  final String? eventId;

  @override
  State<StartRecording> createState() => _StartRecordingState();
}

class _StartRecordingState extends State<StartRecording> {
  // String _platformVersion = 'Unknown';
  // final _textureCameraPlugin = TextureCamera();
  // SensorDeviceData? sensorDeviceData;
  // bool? isMultiCamSupported;
  // PipShape shape = PipShape.circle;
  Timer? _horseTimer;
  int _horseNumber = 7; // initial horse number

//   @override
//   void initState() {
//     super.initState();
//
//     // Start the timer to increment horse number every second
//     _horseTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
//       setState(() {
//         _horseNumber++;
//         final horse = HorseInfo(
//           name: "Thunderbolt",
//           number: _horseNumber,
//           rider: 'Manish Gautam',
//         );
//         HorseApi().updateHorseInfo(horse);
//       });
//     });
//
//     // ... rest of your initState code ...
//     final horse = HorseInfo(
//       name: "Thunderbolt",
//       number: _horseNumber,
//       rider: 'Manish Gautam',
//     );
//     HorseApi().updateHorseInfo(horse);
//
//     //
//     final status = LiveMatchStatus(isLive: true, message: "Match is now live!");
//     LiveMatchApi().updateLiveStatus(status);
//     //
//     PenaltyInfo penaltyInfo = PenaltyInfo(
//       description: "Foul play detected",
//       value: 5,
//     );
//     PenaltyApi().updatePenalty(penaltyInfo);
// //
// TimeInfo timeInfo = TimeInfo(seconds:0 , display: "00:00:00s");
//
//     TimeApi().updateTime(timeInfo);
//     //
//     RankInfo rankInfo = RankInfo(position: 1,label: "win By: 0.34s",);
//
//
//     RankApi().updateRank(rankInfo);
//     //
//     GapToBestInfo gapToBestInfo = GapToBestInfo(
//      gapSeconds: -1.23,
//       display: "-1.23s",
//       isFaster: false,
//     );
//     GapToBestApi().updateGapToBest(gapToBestInfo);
//     //
//
//     //  initPlatformState();
//
//     CamerawesomePlugin.getSensors().then((value) {
//       setState(() {
//         sensorDeviceData = value;
//       });
//     });
//
//     CamerawesomePlugin.isMultiCamSupported().then((value) {
//       setState(() {
//         debugPrint("📸 isMultiCamSupported: $value");
//         isMultiCamSupported = value;
//       });
//     });
//   }
  // Platform messages are asynchronous, so we initialize in an async method.
  // Future<void> initPlatformState() async {
  //   String platformVersion;
  //   // Platform messages may fail, so we use a try/catch PlatformException.
  //   // We also handle the message potentially returning null.
  //   try {
  //     platformVersion =
  //         await _textureCameraPlugin.getPlatformVersion() ?? 'Unknown platform version';
  //   } on PlatformException {
  //     platformVersion = 'Failed to get platform version.';
  //   }

  //   // If the widget was removed from the tree while the asynchronous platform
  //   // message was in flight, we want to discard the reply rather than calling
  //   // setState to update our non-existent appearance.
  //   if (!mounted) return;

  //   setState(() {
  //     _platformVersion = platformVersion;
  //   });
  // }
  @override
  void dispose() {
    _horseTimer?.cancel();
    super.dispose();
  }

  @override
  void initState() {
    // TODO: implement initState
    super.initState();
    //     // Start the timer to increment horse number every second
    _horseTimer = Timer.periodic(const Duration(seconds: 1), (timer) {
      setState(() {
        _horseNumber++;
        MethodChannel('camera_overlay_channel').invokeMethod(
          'updateOverlay',
          overlayConfig.copyWith(horseNumber:OverlayItem(
    text: '#$_horseNumber',
    textSizePx: 26,
    bgColor: '#AA111111',
    fgColor: '#FFFFFF',
    x: -0.9,
    y: 0.65,
  ), ).toMap()
          // {
          //   'horseName': 'Storm Fury',
          //   'rider': 'Rider: $_horseNumber John Doe'
          // },
        );
      });
    });

  }
  @override
  Widget build(BuildContext context) {
    final screenSize = MediaQuery.of(context).size;
    return Scaffold(
      body: Container(
        color: Colors.white,
        child: AndroidView(
          viewType: 'camera_overlay_view',
          creationParams: {
            'horseNumber': '#07',
            'horseName': 'Thunder Bolt',
            'rider': 'Rider: A. Smith',
          },
          creationParamsCodec: const StandardMessageCodec(),
        )
            // Center(
            //   child: Text(
            //    'Running on: $_platformVersion\n',
            //     style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
            //   ),
            // ),
            // sensorDeviceData != null && isMultiCamSupported != null
            // ? CameraAwesomeBuilder.awesome(
            //     saveConfig: SaveConfig.video(
            //       pathBuilder: (sensors) async {
            //         Directory? downloadsDir;
            //         if (Platform.isAndroid) {
            //           downloadsDir = await getDownloadsDirectory();
            //           downloadsDir = await Directory(
            //             p.join(
            //               downloadsDir?.path ??
            //                   Directory('/storage/emulated/0/Download/').path,
            //               'Equestre',
            //             ),
            //           ).create(recursive: true);
            //         } else if (Platform.isIOS) {
            //           downloadsDir = await getApplicationDocumentsDirectory();
            //           downloadsDir = await Directory(
            //             p.join(downloadsDir.path, 'Equestre'),
            //           ).create(recursive: true);
            //         }
            //         if (downloadsDir == null) {
            //           throw Exception(
            //             'Could not determine downloads directory',
            //           );
            //         }
            //         if (sensors.length > 1) {
            //           return MultipleCaptureRequest({
            //             for (final sensor in sensors)
            //               sensor: p.join(
            //                 downloadsDir.path,
            //                 'video_${sensor.position}_${DateTime.now().millisecondsSinceEpoch}.mp4',
            //               ),
            //           });
            //         } else {
            //           return SingleCaptureRequest(
            //             p.join(
            //               downloadsDir.path,
            //               'video_${DateTime.now().millisecondsSinceEpoch}.mp4',
            //             ),
            //             sensors.first,
            //           );
            //         }
            //       },
            //       videoOptions: VideoOptions(enableAudio: true),
            //     ),
            //
            //     sensorConfig: SensorConfig.single(
            //       flashMode: FlashMode.auto,
            //       aspectRatio: CameraAspectRatios.ratio_16_9,
            //     ),
            //     // previewFit: CameraPreviewFit.cover,
            //     onMediaTap: (mediaCapture) {
            //       mediaCapture.captureRequest.when(
            //         single: (single) => single.file?.open(),
            //       );
            //     },
                // pictureInPictureConfigBuilder: (index, sensor) {
                //   const width = 300.0;
                //   return PictureInPictureConfig(
                //     isDraggable: true,
                //     startingPosition: Offset(
                //       -50,
                //       screenSize.height - 420,
                //     ),
                //     onTap: () {
                //       debugPrint('on preview tap');
                //     },
                //     sensor: sensor,
                //     pictureInPictureBuilder: (preview, aspectRatio) {
                //       return SizedBox(
                //         width: width,
                //         height: width,
                //         child: ClipPath(
                //           clipper: _MyCustomPipClipper(
                //             width: width,
                //             height: width * aspectRatio,
                //             shape: shape,
                //           ),
                //           child: SizedBox(
                //             width: width,
                //             child: preview,
                //           ),
                //         ),
                //       );
                //     },
                //   );
                // },
                // previewDecoratorBuilder: (state, _) {
                //   return Column(
                //     mainAxisSize: MainAxisSize.min,
                //     mainAxisAlignment: MainAxisAlignment.center,
                //     crossAxisAlignment: CrossAxisAlignment.start,
                //     children: [
                //       Container(
                //         color: Colors.white70,
                //         margin: const EdgeInsets.only(left: 8),
                //         child: const Text("Change picture in picture's shape:"),
                //       ),
                //       GridView.builder(
                //         gridDelegate:
                //             const SliverGridDelegateWithFixedCrossAxisCount(
                //           crossAxisCount: 3,
                //           childAspectRatio: 16 / 9,
                //         ),
                //         shrinkWrap: true,
                //         padding: EdgeInsets.zero,
                //         itemCount: PipShape.values.length,
                //         itemBuilder: (context, index) {
                //           final shape = PipShape.values[index];
                //           return GestureDetector(
                //             onTap: () {
                //               setState(() {
                //                 this.shape = shape;
                //               });
                //             },
                //             child: Container(
                //               color: Colors.red.withValues(alpha: 0.5),
                //               margin: const EdgeInsets.all(8.0),
                //               child: Center(
                //                 child: Text(
                //                   shape.name,
                //                   textAlign: TextAlign.center,
                //                 ),
                //               ),
                //             ),
                //           );
                //         },
                //       ),
                //     ],
                //   );
                // },
              // )
            // : const SizedBox.shrink(),
      ),
    );
  }
}

// enum PipShape {
//   square,
//   circle,
//   roundedSquare,
//   triangle,
//   hexagon;
//
//   Path getPath(Offset center, double width, double height) {
//     switch (this) {
//       case PipShape.square:
//         return Path()..addRect(
//           Rect.fromCenter(
//             center: center,
//             width: min(width, height),
//             height: min(width, height),
//           ),
//         );
//       case PipShape.circle:
//         return Path()..addOval(
//           Rect.fromCenter(
//             center: center,
//             width: min(width, height),
//             height: min(width, height),
//           ),
//         );
//       case PipShape.triangle:
//         return Path()
//           ..moveTo(center.dx, center.dy - min(width, height) / 2)
//           ..lineTo(
//             center.dx + min(width, height) / 2,
//             center.dy + min(width, height) / 2,
//           )
//           ..lineTo(
//             center.dx - min(width, height) / 2,
//             center.dy + min(width, height) / 2,
//           )
//           ..close();
//       case PipShape.roundedSquare:
//         return Path()..addRRect(
//           RRect.fromRectAndRadius(
//             Rect.fromCenter(
//               center: center,
//               width: min(width, height),
//               height: min(width, height),
//             ),
//             const Radius.circular(20.0),
//           ),
//         );
//       case PipShape.hexagon:
//         return Path()
//           ..moveTo(center.dx, center.dy - min(width, height) / 2)
//           ..lineTo(
//             center.dx + min(width, height) / 2,
//             center.dy - min(width, height) / 4,
//           )
//           ..lineTo(
//             center.dx + min(width, height) / 2,
//             center.dy + min(width, height) / 4,
//           )
//           ..lineTo(center.dx, center.dy + min(width, height) / 2)
//           ..lineTo(
//             center.dx - min(width, height) / 2,
//             center.dy + min(width, height) / 4,
//           )
//           ..lineTo(
//             center.dx - min(width, height) / 2,
//             center.dy - min(width, height) / 4,
//           )
//           ..close();
//     }
//   }
// }
//
// class _MyCustomPipClipper extends CustomClipper<Path> {
//   final double width;
//   final double height;
//   final PipShape shape;
//
//   const _MyCustomPipClipper({
//     required this.width,
//     required this.height,
//     required this.shape,
//   });
//
//   @override
//   Path getClip(Size size) {
//     return shape.getPath(size.center(Offset.zero), width, height);
//   }
//
//   @override
//   bool shouldReclip(covariant _MyCustomPipClipper oldClipper) {
//     return width != oldClipper.width ||
//         height != oldClipper.height ||
//         shape != oldClipper.shape;
//   }
// }
//
// // class GalleryPage extends StatefulWidget {
// //   final MultipleCaptureRequest multipleCaptureRequest;
//
// //   const GalleryPage({super.key, required this.multipleCaptureRequest});
//
// //   @override
// //   State<GalleryPage> createState() => _GalleryPageState();
// // }
//
// // class _GalleryPageState extends State<GalleryPage> {
// //   @override
// //   Widget build(BuildContext context) {
// //     return Scaffold(
// //       appBar: AppBar(
// //         title: const Text('Gallery'),
// //       ),
// //       body: GridView.builder(
// //         gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
// //           crossAxisCount: 3,
// //         ),
// //         itemCount: widget.multipleCaptureRequest.fileBySensor.length,
// //         itemBuilder: (context, index) {
// //           final sensor =
// //               widget.multipleCaptureRequest.fileBySensor.keys.toList()[index];
// //           final file = widget.multipleCaptureRequest.fileBySensor[sensor];
// //           return GestureDetector(
// //             onTap: () => file.open(),
// //             child: file!.path.endsWith("jpg")
// //                 ? Image.file(
// //                     File(file.path),
// //                     fit: BoxFit.cover,
// //                   )
// //                 : VideoPreview(file: File(file.path)),
// //           );
// //         },
// //       ),
// //     );
// //   }
// // }
//
// // // class VideoPreview extends StatefulWidget {
// // //   final File file;
//
// // //   const VideoPreview({super.key, required this.file});
//
// // //   @override
// // //   State<StatefulWidget> createState() {
// // //     return _VideoPreviewState();
// // //   }
// // // }
//
// // // class _VideoPreviewState extends State<VideoPreview> {
// // //   late VideoPlayerController _controller;
//
// // //   @override
// // //   void initState() {
// // //     super.initState();
// // //     _controller = VideoPlayerController.file(widget.file)
// // //       ..setLooping(true)
// // //       ..initialize().then((_) {
// // //         setState(() {});
// // //         _controller.play();
// // //       });
// // //   }
//
// // //   @override
// // //   Widget build(BuildContext context) {
// // //     return Center(
// // //       child: _controller.value.isInitialized
// // //           ? AspectRatio(
// // //               aspectRatio: _controller.value.aspectRatio,
// // //               child: VideoPlayer(_controller),
// // //             )
// // //           : const SizedBox.shrink(),
// // //     );
// // //   }
// // // }
