import 'package:equestre/models/overlay_config.dart';

final overlayConfig = OverlayConfig(
  horseName: OverlayItem(
    text: 'Lightning Bolt',
    textSizePx: 28,
    bgColor: '#FF000000',
    fgColor: '#00FF00',
    x: -0.8,
    y: 0.75,
  ),
  horseNumber: OverlayItem(
    text: '#7',
    textSizePx: 26,
    bgColor: '#AA111111',
    fgColor: '#FFFFFF',
    x: -0.9,
    y: 0.65,
  ),
  rider: OverlayItem(
    text: 'John Doe',
    textSizePx: 24,
    bgColor: '#88110088',
    fgColor: '#FFD700',
    x: -0.85,
    y: 0.85,
  ),
  penalty: OverlayItem(
    text: '0.5s',
    textSizePx: 22,
    bgColor: '#88FF0000',
    fgColor: '#FFFFFF',
    x: 0.6,
    y: -0.95,
  ),
  timeTaken: OverlayItem(
    text: '52.34s',
    textSizePx: 22,
    bgColor: '#880000FF',
    fgColor: '#FFFFFF',
    x: 0.75,
    y: -0.95,
  ),
  rank: OverlayItem(
    text: '2nd Place',
    textSizePx: 22,
    bgColor: '#FF880000',
    fgColor: '#FFFFFF',
    x: 0.85,
    y: -0.95,
  ),
  gapToBest: OverlayItem(
    text: '+1.23s',
    textSizePx: 22,
    bgColor: '#FF008800',
    fgColor: '#FFFFFF',
    x: 0.95,
    y: -0.95,
  ),
  liveMsg: OverlayItem(
    text: 'LIVE: Round 2 in Progress',
    textSizePx: 26,
    bgColor: '#CC000000',
    fgColor: '#00FFFF',
    x: 0.0,
    y: 0.95,
  ),
);

// final map = overlayConfig.toMap();

// await methodChannel.invokeMethod('updateOverlay', map);
