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