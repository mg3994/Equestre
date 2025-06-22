import 'package:flutter/material.dart';

class StartRecording extends StatefulWidget {
  const StartRecording({super.key, this.eventId});
  final String? eventId;

  @override
  State<StartRecording> createState() => _StartRecordingState();
}

class _StartRecordingState extends State<StartRecording> {

  late String? eventId;

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
    return const Placeholder();
  }
}