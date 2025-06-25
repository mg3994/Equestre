# texture_camera

A new Flutter camera with defined texture

## Getting Started

This project is a starting point for a Flutter
[plug-in package](https://flutter.dev/to/develop-plugins),
a specialized package that includes platform-specific implementation code for
Android and/or iOS.

For help getting started with Flutter development, view the
[online documentation](https://docs.flutter.dev), which offers tutorials,
samples, guidance on mobile development, and a full API reference.

```dart
static const _channel = MethodChannel('equestre_plugin');

Future<void> sendHorseData({
  required String horseNumber,
  required String horseName,
  required String rider,
  String? gap,
  String? penalties,
  String? time,
  String? rank,
}) async {
  await _channel.invokeMethod('updateOverlay', {
    'horseNumber': horseNumber,
    'horseName': horseName,
    'rider': rider,
    'gap': gap ?? '',
    'penalties': penalties ?? '',
    'time': time ?? '',
    'rank': rank ?? '',
  });
}
```