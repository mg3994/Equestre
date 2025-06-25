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