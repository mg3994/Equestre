import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';

class Settings extends StatefulWidget {
  const Settings({super.key});

  @override
  State<Settings> createState() => _SettingsState();
}

class _SettingsState extends State<Settings> {
  bool showRank = true;
  bool showTimeAndPenalties = true;
  bool showEverything = true;

  @override
  void initState() {
    super.initState();
    _loadSettings();
  }

  Future<void> _loadSettings() async {
    final prefs = await SharedPreferences.getInstance();
    setState(() {
      showRank = prefs.getBool('showRank') ?? true;
      showTimeAndPenalties = prefs.getBool('showTimeAndPenalties') ?? true;
      showEverything = prefs.getBool('showEverything') ?? true;
    });
  }

  Future<void> _updateSetting(String key, bool value) async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.setBool(key, value);
  }

  void _onShowEverythingChanged(bool? value) {
    if (value == null) return;
    setState(() {
      showEverything = value;
      showRank = value;
      showTimeAndPenalties = value;
    });
    _updateSetting('showEverything', value);
    _updateSetting('showRank', value);
    _updateSetting('showTimeAndPenalties', value);
  }

  void _onShowRankChanged(bool? value) {
    if (value == null) return;
    setState(() {
      showRank = value;
      showEverything = showRank && showTimeAndPenalties;
    });
    _updateSetting('showRank', value);
    _updateSetting('showEverything', showEverything);
  }

  void _onShowTimeAndPenaltiesChanged(bool? value) {
    if (value == null) return;
    setState(() {
      showTimeAndPenalties = value;
      showEverything = showRank && showTimeAndPenalties;
    });
    _updateSetting('showTimeAndPenalties', value);
    _updateSetting('showEverything', showEverything);
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.blueAccent.shade100,
      body: Center(
      child: ListView(
        shrinkWrap: true,
        padding: const EdgeInsets.all(16),
        children: [
        CheckboxListTile(
          controlAffinity: ListTileControlAffinity.leading,
          title: const Text('Show Everything'),
          value: showEverything,
          onChanged: _onShowEverythingChanged,
        ),
        CheckboxListTile(
          controlAffinity: ListTileControlAffinity.leading,
          title: const Text('Show Rank'),
          value: showRank,
          onChanged: showEverything ? null : _onShowRankChanged,
        ),
        CheckboxListTile(
          controlAffinity: ListTileControlAffinity.leading,
          title: const Text('Show Time and Penalties'),
          value: showTimeAndPenalties,
          onChanged: showEverything ? null : _onShowTimeAndPenaltiesChanged,
        ),
        ],
      ),
      ),
    );
  }
}