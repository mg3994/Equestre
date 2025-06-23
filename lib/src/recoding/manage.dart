import 'dart:io';

import 'package:flutter/material.dart';
import 'package:permission_handler/permission_handler.dart';
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';
import 'package:path/path.dart' as p;

class ManageRecordings extends StatefulWidget {
  const ManageRecordings({super.key});

  @override
  State<ManageRecordings> createState() => _ManageRecordingsState();
}

class _ManageRecordingsState extends State<ManageRecordings> {
  List<FileSystemEntity> _videos = [];
  bool _loading = true;

  @override
  void initState() {
    super.initState();
    _requestPermissionAndLoad();
  }

  Future<void> _requestPermissionAndLoad() async {
    var status = await Permission.storage.request();
    if (status.isGranted) {
      await _loadVideos();
    } else {
      setState(() {
        _loading = false;
      });
    }
  }

  Future<void> _loadVideos() async {
    Directory? downloadsDir;
    if (Platform.isAndroid) {
      downloadsDir = await getDownloadsDirectory() ; //Directory('/storage/emulated/0/Download/Equestre');
      downloadsDir = Directory(p.join(downloadsDir?.path ??Directory('/storage/emulated/0/Download/').path, 'Equestre'));
    } else if (Platform.isIOS) {
      downloadsDir = await getApplicationDocumentsDirectory();
      downloadsDir = Directory(p.join(downloadsDir.path, 'Equestre'));
    }
    if (downloadsDir != null && await downloadsDir.exists()) {
      final files = downloadsDir
          .listSync()
          .where((f) => f is File && _isVideoFile(f.path))
          .toList();
      setState(() {
        _videos = files;
        _loading = false;
      });
    } else {
      setState(() {
        _videos = [];
        _loading = false;
      });
    }
  }

  bool _isVideoFile(String path) {
    final ext = p.extension(path).toLowerCase();
    return ['.mp4', '.mov', '.avi', '.mkv', '.webm'].contains(ext);
  }

  Future<void> _deleteVideo(FileSystemEntity file) async {
    var status = await Permission.manageExternalStorage.request();
    if (!status.isGranted) {
      if (mounted) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Permission denied to manage files.')),
      );
      }
      return;
    }
    await file.delete();
    await _loadVideos();
    setState(() {
      
    });
  }

  void _openVideo(FileSystemEntity file) {
    OpenFilex.open(file.path);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Manage Recordings')),
      body: _loading
          ? const Center(child: CircularProgressIndicator())
          : _videos.isEmpty
              ? const Center(child: Text('No videos found in Equestre folder.'))
              : ListView.builder(
                  itemCount: _videos.length,
                  itemBuilder: (context, index) {
                    final file = _videos[index];
                    return ListTile(
                      leading: const Icon(Icons.videocam),
                      title: Text(p.basename(file.path)),
                      trailing: Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          IconButton(
                            icon: const Icon(Icons.play_arrow),
                            onPressed: () => _openVideo(file),
                          ),
                          IconButton(
                            icon: const Icon(Icons.delete, color: Colors.red),
                            onPressed: () async {
                              final confirm = await showDialog<bool>(
                                context: context,
                                builder: (ctx) => AlertDialog(
                                  title: const Text('Delete Video'),
                                  content: const Text('Are you sure you want to delete this video?'),
                                  actions: [
                                    TextButton(
                                      onPressed: () => Navigator.pop(ctx, false),
                                      child: const Text('Cancel'),
                                    ),
                                    TextButton(
                                      onPressed: () => Navigator.pop(ctx, true),
                                      child: const Text('Delete'),
                                    ),
                                  ],
                                ),
                              );
                              if (confirm == true) {
                                await _deleteVideo(file);
                              }
                            },
                          ),
                        ],
                      ),
                    );
                  },
                ),
    );
  }
}
