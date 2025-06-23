import 'package:camerawesome/camerawesome_plugin.dart';
import 'package:flutter/material.dart';
class PreviewDecorationWiget extends StatefulWidget {
  final CameraState state;
  final AnalysisPreview preview;

  const PreviewDecorationWiget({
    super.key,
    required this.state,
    required this.preview,
  });


  @override
  State<PreviewDecorationWiget> createState() => _PreviewDecorationWigetState();
}

class _PreviewDecorationWigetState extends State<PreviewDecorationWiget> {
  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Preview Decoration'),
      ),
      body: Center(
        child: Container(
          width: 200,
          height: 200,
          decoration: BoxDecoration(
            color: Colors.blue[100],
            border: Border.all(color: Colors.blue, width: 3),
            borderRadius: BorderRadius.circular(16),
            boxShadow: [
              BoxShadow(
                color: Colors.blue,
                blurRadius: 8,
                offset: const Offset(4, 4),
              ),
            ],
          ),
          child: const Center(
            child: Text(
              'Preview',
              style: TextStyle(fontSize: 24, color: Colors.blue),
            ),
          ),
        ),
      ),
    );
  }
}