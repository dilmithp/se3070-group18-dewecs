import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_exception.dart';
import '../state/identity_controller.dart';
import '../state/settings_controller.dart';
import '../strings.dart';
import '../validators.dart';
import 'identify_screen.dart';
import 'sync_queue_screen.dart';

class SettingsScreen extends StatefulWidget {
  const SettingsScreen({super.key});

  @override
  State<SettingsScreen> createState() => _SettingsScreenState();
}

class _SettingsScreenState extends State<SettingsScreen> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _url;
  bool _testing = false;
  String? _testResult;
  bool _testFailed = false;

  @override
  void initState() {
    super.initState();
    _url = TextEditingController(text: context.read<SettingsController>().baseUrl);
  }

  @override
  void dispose() {
    _url.dispose();
    super.dispose();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    final messenger = ScaffoldMessenger.of(context);
    await context.read<SettingsController>().setBaseUrl(_url.text);
    messenger.showSnackBar(const SnackBar(content: Text(S.baseUrlSaved)));
  }

  Future<void> _test() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    final settings = context.read<SettingsController>();
    setState(() {
      _testing = true;
      _testResult = null;
    });
    try {
      // Tests the address typed in the field (even before saving); Demo mode tests the fake server.
      final data = await settings.apiFor(_url.text).getReferenceData();
      if (mounted) {
        setState(() {
          _testFailed = false;
          _testResult = S.connectionOk(data.districts.length);
        });
      }
    } on ApiException catch (e) {
      if (mounted) {
        setState(() {
          _testFailed = true;
          _testResult = S.connectionFailed(e.detail);
        });
      }
    } finally {
      if (mounted) {
        setState(() => _testing = false);
      }
    }
  }

  Future<void> _toggleDemo(bool value) async {
    final settings = context.read<SettingsController>();
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text(S.demoSwitchTitle),
        content: const Text(S.demoSwitchBody),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text(S.cancel)),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text(S.demoSwitchConfirm)),
        ],
      ),
    );
    if (confirmed == true) {
      await settings.setDemoMode(value);
    }
  }

  @override
  Widget build(BuildContext context) {
    final settings = context.watch<SettingsController>();
    final citizen = context.watch<IdentityController>().citizen;
    final theme = Theme.of(context);
    return ListView(
      padding: const EdgeInsets.all(16),
      children: [
        Text(S.settingsServer, style: theme.textTheme.titleMedium),
        const SizedBox(height: 8),
        Form(
          key: _formKey,
          child: TextFormField(
            controller: _url,
            decoration: const InputDecoration(labelText: S.baseUrlLabel, helperText: S.baseUrlHelp),
            keyboardType: TextInputType.url,
            autocorrect: false,
            validator: validateBaseUrl,
          ),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          children: [
            FilledButton(onPressed: _save, child: const Text(S.save)),
            OutlinedButton(
              onPressed: _testing ? null : _test,
              child: Text(_testing ? S.testing : S.testConnection),
            ),
          ],
        ),
        if (_testResult != null)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Semantics(
              liveRegion: true,
              child: Text(_testResult!, style: TextStyle(color: _testFailed ? theme.colorScheme.error : null)),
            ),
          ),
        const Divider(height: 32),
        Text(S.settingsYou, style: theme.textTheme.titleMedium),
        const SizedBox(height: 8),
        Text(citizen == null ? S.notIdentified : S.identifiedAs(citizen.fullName, citizen.districtName)),
        const SizedBox(height: 8),
        Align(
          alignment: Alignment.centerLeft,
          child: OutlinedButton(
            onPressed: () => Navigator.of(context).push(MaterialPageRoute<bool>(builder: (_) => const IdentifyScreen())),
            child: const Text(S.identifyAgain),
          ),
        ),
        const Divider(height: 32),
        SwitchListTile(
          contentPadding: EdgeInsets.zero,
          title: const Text(S.demoMode),
          subtitle: const Text(S.demoModeHelp),
          value: settings.demoMode,
          onChanged: _toggleDemo,
        ),
        if (settings.demoMode) ...[
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text(S.demoForceNetwork),
            value: settings.fake.failNetwork,
            onChanged: settings.setFakeNetworkFailure,
          ),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text(S.demoForce500),
            value: settings.fake.failStatus == 500,
            onChanged: settings.setFakeServerError,
          ),
          ListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text(S.demoResetServer),
            onTap: () {
              final messenger = ScaffoldMessenger.of(context);
              settings.resetFakeServer();
              messenger.showSnackBar(const SnackBar(content: Text(S.demoServerWiped)));
            },
          ),
        ],
        if (kDebugMode) ...[
          const Divider(height: 32),
          Text(S.settingsDebug, style: theme.textTheme.titleMedium),
          ListTile(
            contentPadding: EdgeInsets.zero,
            leading: const Icon(Icons.sync_alt),
            title: const Text(S.syncQueueScreen),
            onTap: () => Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => const SyncQueueScreen())),
          ),
        ],
      ],
    );
  }
}
