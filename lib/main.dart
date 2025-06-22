import 'package:equestre/src/main/main_page.dart' show MainPage;
import 'package:equestre/src/recoding/manage.dart' show ManageRecordings;
import 'package:equestre/src/recoding/start.dart' show StartRecording;
import 'package:equestre/src/settings/settings.dart' show Settings;
import 'package:equestre/src/sports/sports_events.dart' show SportsEvents;
import 'package:flutter/material.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({super.key});


  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'Flutter Demo',
      theme: ThemeData(
       
        colorScheme: ColorScheme.fromSeed(seedColor: Colors.deepPurple),
      ),
    initialRoute: '/',
    routes: {
      '/': (context) => MainPage(),
      '/sports-events': (context) => SportsEvents(),
      '/manage-recordings': (context) => ManageRecordings(),
      '/settings': (context) => Settings(),
    },
    onGenerateRoute: (settings) {
      if (settings.name == '/start-recording') {
      final eventId = settings.arguments as String?;
      if (eventId == null || eventId.isEmpty /* or add your own validation here */) {
        return MaterialPageRoute(
        builder: (context) => SportsEvents(),
        );
      }
      return MaterialPageRoute(
        builder: (context) => StartRecording(eventId: eventId),
      );
      }
      return null;
    },
    );
  }
}
