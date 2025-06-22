import 'dart:convert';

import 'package:equestre/models/consumer_model.dart' show ConsumerModel;
import 'package:flutter/material.dart';
import 'package:socket_io_client/socket_io_client.dart' as IO;

class SportsEvents extends StatefulWidget {
  const SportsEvents({super.key});

  @override
  State<SportsEvents> createState() => _SportsEventsState();
}

class _SportsEventsState extends State<SportsEvents> {
 
  late IO.Socket socket;
  Map<String, List<ConsumerModel>> categorizedEvents = {};

  @override
  void initState() {
    super.initState();
    _initSocket();
  }

  @override
  void dispose() {
    // if (socket.connected) {
    //   socket.emit('unsubscribe', 'consumer');
    // }
    socket.dispose();
    super.dispose();
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

    socket.connect();

    socket.onConnect((_) {
      socket.emit('subscribe', 'consumer');
    });

    socket.on('events', (data) {
      final jsonData = (data is String) ? jsonDecode(data) : data;
      if (jsonData is List) {
        List<ConsumerModel> events = jsonData
        .map((e) => ConsumerModel.fromJson(Map<String, dynamic>.from(e)))
        .toList();
debugPrint('Received ${events.length} events and data is ${jsonData}');
        Map<String, List<ConsumerModel>> grouped = {};
        for (var event in events) {
          final key = event.info != null && event.info!.category != null
          ? event.info!.category!
          : 'Unknown';
          grouped.putIfAbsent(key, () => []).add(event);
        }

        setState(() {
          categorizedEvents = grouped;
        });
      }
    });
  }

 

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(
        title: const Text('Sports Events'),
        bottom: PreferredSize(
          preferredSize: const Size.fromHeight(56),
          child: Padding(
        padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
        child: TextField(
          decoration: InputDecoration(
            hintText: 'Search events...',
            prefixIcon: const Icon(Icons.search),
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(24),
              borderSide: BorderSide.none,
            ),
            filled: true,
            fillColor: Colors.white,
            contentPadding: const EdgeInsets.symmetric(vertical: 0),
          ),
          onChanged: (query) {
            setState(() {
              if (query.isEmpty) {
          // Show all events again by triggering a socket re-subscription
          socket.emit('unsubscribe', 'consumer');
          socket.emit('subscribe', 'consumer');
          return;
              } else {
          final filtered = <String, List<ConsumerModel>>{};
          categorizedEvents.forEach((category, events) {
            final matches = events.where((e) =>
              (e.info?.eventTitle?.toLowerCase().contains(query.toLowerCase()) ?? false) ||
              (e.info?.category?.toLowerCase().contains(query.toLowerCase()) ?? false)
            ).toList();
            if (matches.isNotEmpty) {
              filtered[category] = matches;
            }
          });
          categorizedEvents = filtered;
              }
            });
          },
        ),
          ),
        ),
      ),
      body: categorizedEvents.isEmpty
          ? Center(child: CircularProgressIndicator())
            : ListView.builder(
              itemCount: categorizedEvents.values.expand((e) => e).length,
              itemBuilder: (context, index) {
                // Flatten all events into a single list
                final allEvents = categorizedEvents.values.expand((e) => e).toList();
                final consumer = allEvents[index];
                return GestureDetector(
                  onTap: () => Navigator.pushNamed(
                    context,
                    '/start-recording',
                    arguments: consumer.id,
                  ),
                  child: Card(
                  child: ListTile(
                    leading: consumer.info?.live == true
                      ? BlinkingLiveIcon()
                      : const Icon(Icons.circle_outlined, color: Colors.grey),
                    title: Text(consumer.info?.eventTitle ?? 'No Title'),
                    subtitle: Text(
                    consumer.info?.eventTime != null
                      ? 'Event Start Time: ${consumer.info!.eventTime!}'
                      : 'Event Time: Unknown',
                    ),
                    trailing: Text(consumer.info?.category ?? ''),
                  ),
                  ),
                );
              },
              ));
}}


class BlinkingLiveIcon extends StatefulWidget {
  const BlinkingLiveIcon({super.key});

  @override
  State<BlinkingLiveIcon> createState() => _BlinkingLiveIconState();
}

class _BlinkingLiveIconState extends State<BlinkingLiveIcon>
    with SingleTickerProviderStateMixin {
  late AnimationController _controller;
  late Animation<double> _animation;

  @override
  void initState() {
    super.initState();
    _controller = AnimationController(
      duration: const Duration(milliseconds: 800),
      vsync: this,
    )..repeat(reverse: true);
    _animation = Tween<double>(begin: 0.3, end: 1.0).animate(_controller);
  }

  @override
  void dispose() {
    _controller.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return FadeTransition(
      opacity: _animation,
      child: const Icon(
        Icons.circle,
        color: Colors.red,
        size: 20,
      ),
    );
  }
}
