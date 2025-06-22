import 'package:camerawesome/camerawesome_plugin.dart';
import 'package:camerawesome/pigeon.dart';
import 'package:equestre/utils/extensions/open.dart';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:socket_io_client/socket_io_client.dart' as IO;

class StartRecording extends StatefulWidget {
  const StartRecording({super.key, this.eventId});
  final String? eventId;

  @override
  State<StartRecording> createState() => _StartRecordingState();
}

class _StartRecordingState extends State<StartRecording> {

  late String? eventId;
    late IO.Socket socket;

  bool showRank = true;
  bool showTimeAndPenalties = true;
  bool showEverything = true;

  @override
  void initState() {
    eventId = widget.eventId;
    _loadSettings();
    _initSocket();
    super.initState();
  }

   Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      showRank = prefs.getBool('showRank') ?? true;
      showTimeAndPenalties = prefs.getBool('showTimeAndPenalties') ?? true;
      showEverything = prefs.getBool('showEverything') ?? true;
    });
  }

   void _initSocket() {
    socket = IO.io(
      'http://185.48.228.171:21741',
      IO.OptionBuilder()
        .setTransports(['websocket', 'polling', 'webtransport'])
        .enableAutoConnect()
        .enableForceNew()
        .build(),
    );

    if (showRank || showTimeAndPenalties || showEverything) {
      socket.connect();
    }

    socket.onConnect((_) {
      socket.emit('subscribe',eventId);
    });
        // Example of how you might log similar information in Dart when handling a socket event:
    socket.on('info', (data) {
      print("[emit] socket:info");
      print("Event info: $data");
    });
    socket.on('startlist', (data) {
      print("[emit] socket:startlist");
      print("Startlist: $data");
    });
    socket.on('competitors', (data) {
      print("[emit] socket:competitors");
      print("Competitors: $data");
    });
    socket.on('horses', (data) {
      print("[emit] socket:horses");
      print("Horses: $data");
    });
    socket.on('riders', (data) {
      print("[emit] socket:riders");
      print("Riders: $data");
    });
    socket.on('judges', (data) {
      print("[emit] socket:judges");
      print("Judges: $data");
    });
    socket.on('teams', (data) {
      print("[emit] socket:teams");
      print("Teams: $data");
    });
    socket.on('ranking', (data) {
      print("[emit] socket:ranking");
      print("Ranking: $data");
    });
    socket.on('cc-ranking', (data) {
      print("[emit] socket:ccranking");
      print("CC Ranking: $data");
    });
    socket.on('realtime', (data) {
      print("[emit] socket:realtime(initial)");
      print("Realtime: $data");
    });
    socket.on('resume', (_) {
      print("[emit] socket:resume");
    });
    socket.on('nofifyResume', (data) {
      print("[emit] socket:nofifyResume");
      print("Notify Resume: $data");
    });
    socket.on('final', (_) {
      print("[emit] socket:final");
    });
    socket.on('ready', (_) {
      print("[emit] socket:ready");
    });
    socket.on('live_info', (data) {
      print("[emit] socket:live_info");
      print("Live Info: $data");
    });
    }


@override
  void dispose() {
socket.dispose();
    super.dispose();
  }

  @override
  void didChangeDependencies() {
    super.didChangeDependencies();
    if (widget.eventId != null) {
      eventId = widget.eventId;
    } else {
      final args = ModalRoute.of(context)?.settings.arguments;
      if (args is String) {
        eventId = args;
      } else {
        eventId = null;
      }
    }
  }
  @override
  Widget build(BuildContext context) {
    return Scaffold(
    
      body:eventId != null ? 
       CameraAwesomeBuilder.awesome(
              saveConfig: SaveConfig.video(
                // pathBuilder: (sensors) {
                //   return '/storage/emulated/0/Download/Equestre/${eventId!}';
                // },
                videoOptions: VideoOptions(
                  enableAudio: true,
                  
                ),
              ),
             
              sensorConfig: SensorConfig.single(
                flashMode: FlashMode.auto,
                aspectRatio: CameraAspectRatios.ratio_16_9,
              ),
              previewFit: CameraPreviewFit.cover,
              onMediaTap: (mediaCapture) {
                mediaCapture.captureRequest.when(
                  single: (single) => single.file?.open(),
                );
              },
            )
          
        : Center(
          child: Text('No event ID provided'),
        ),
//        CameraAwesomeBuilder.custom(
//   saveConfig: SaveConfig.photoAndVideo(),
//   builder: (CameraState cameraState,  AnalysisPreview previewRect) {
//     // Return your UI (a Widget)
//     return cameraState.when(
//       onPreparingCamera: (state) => const Center(child: CircularProgressIndicator()),
//       // onPhotoMode: (state) => TakePhotoUI(state),
//       // onVideoMode: (state) => RecordVideoUI(state, recording: false),
//       // onVideoRecordingMode: (state) => RecordVideoUI(state, recording: true),
//     );
//   },
// )
// : SizedBox.shrink()
    );
  }
}