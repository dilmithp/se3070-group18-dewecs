import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../api/api_exception.dart';
import '../config/theme.dart';
import '../connection_hint.dart';
import '../state/identity_controller.dart';
import '../state/settings_controller.dart';
import '../strings.dart';
import '../validators.dart';
import '../widgets/section_card.dart';
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
  _TestResult? _result;

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

  /// Tests the address typed in the field (even before it is saved). Whatever goes wrong, the result panel shows
  /// it: the button never stays stuck on "Testing..." and never fails silently.
  Future<void> _test() async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    final settings = context.read<SettingsController>();
    final address = _url.text.trim();
    setState(() {
      _testing = true;
      _result = null;
    });
    final started = DateTime.now();
    _TestResult result;
    try {
      final data = await settings.apiFor(address).getReferenceData();
      result = _TestResult.ok(S.connectionOk(data.districts.length), demo: settings.demoMode);
    } on ApiException catch (e) {
      result = _TestResult.failed(S.connectionFailed(e.detail), advice: connectionAdvice(e, address));
    } catch (e) {
      result = _TestResult.failed(S.connectionFailed('$e'),
          advice: connectionAdvice(const ApiException(kind: ApiErrorKind.network, detail: ''), address));
    }
    final ms = DateTime.now().difference(started).inMilliseconds;
    if (mounted) {
      setState(() {
        _testing = false;
        _result = result.withTimings(settings.demoMode ? null : S.connectionTried(address, ms));
      });
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
      // The last test result was for the other mode, so it no longer applies.
      if (mounted) {
        setState(() => _result = null);
      }
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
        SectionCard(
          title: S.settingsServer,
          icon: Icons.dns_outlined,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
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
              ValueListenableBuilder<TextEditingValue>(
                valueListenable: _url,
                builder: (context, value, _) {
                  final warning = insecureAddressWarning(value.text);
                  if (warning == null) {
                    return const SizedBox.shrink();
                  }
                  return Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Row(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Icon(Icons.lock_open, size: 18, color: theme.colorScheme.tertiary),
                        const SizedBox(width: 6),
                        Expanded(child: Text(warning, style: theme.textTheme.bodySmall)),
                      ],
                    ),
                  );
                },
              ),
              const SizedBox(height: 12),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  FilledButton(onPressed: _save, child: const Text(S.save)),
                  OutlinedButton.icon(
                    onPressed: _testing ? null : _test,
                    icon: _testing
                        ? const SizedBox(width: 16, height: 16, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.network_check),
                    label: Text(_testing ? S.testing : S.testConnection),
                  ),
                ],
              ),
              if (_result != null) _ResultPanel(result: _result!),
            ],
          ),
        ),
        const SizedBox(height: 12),
        SectionCard(
          title: S.settingsYou,
          icon: Icons.person_outline,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(citizen == null ? S.notIdentified : S.identifiedAs(citizen.fullName, citizen.districtName)),
              const SizedBox(height: 12),
              OutlinedButton(
                onPressed: () =>
                    Navigator.of(context).push(MaterialPageRoute<bool>(builder: (_) => const IdentifyScreen())),
                child: const Text(S.identifyAgain),
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),
        SectionCard(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
          child: SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: const Text(S.officerMode),
            subtitle: const Text(S.officerModeHelp),
            value: settings.officerMode,
            onChanged: (value) => settings.setOfficerMode(value),
          ),
        ),
        const SizedBox(height: 12),
        SectionCard(
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
          child: Column(
            children: [
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: const Text(S.demoMode),
                subtitle: const Text(S.demoModeHelp),
                value: settings.demoMode,
                onChanged: _toggleDemo,
              ),
              if (settings.demoMode) ...[
                const Divider(),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text(S.demoForceNetwork),
                  value: settings.demo.failNetwork,
                  onChanged: settings.setDemoNetworkFailure,
                ),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text(S.demoForce500),
                  value: settings.demo.failStatus == 500,
                  onChanged: settings.setDemoServerError,
                ),
                ListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text(S.demoResetServer),
                  onTap: () {
                    final messenger = ScaffoldMessenger.of(context);
                    settings.resetDemoServer();
                    messenger.showSnackBar(const SnackBar(content: Text(S.demoServerWiped)));
                  },
                ),
              ],
            ],
          ),
        ),
        if (kDebugMode) ...[
          const SizedBox(height: 12),
          SectionCard(
            title: S.settingsDebug,
            child: ListTile(
              contentPadding: EdgeInsets.zero,
              leading: const Icon(Icons.sync_alt),
              title: const Text(S.syncQueueScreen),
              onTap: () =>
                  Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => const SyncQueueScreen())),
            ),
          ),
        ],
      ],
    );
  }
}

/// What the last connection test found.
class _TestResult {
  const _TestResult._({required this.ok, required this.message, this.advice, this.note, this.detail});

  factory _TestResult.ok(String message, {required bool demo}) =>
      _TestResult._(ok: true, message: message, note: demo ? S.connectionDemoNote : null);

  factory _TestResult.failed(String message, {required String advice}) =>
      _TestResult._(ok: false, message: message, advice: advice);

  final bool ok;
  final String message;
  final String? advice;
  final String? note;
  final String? detail;

  _TestResult withTimings(String? text) =>
      _TestResult._(ok: ok, message: message, advice: advice, note: note, detail: text);
}

class _ResultPanel extends StatelessWidget {
  const _ResultPanel({required this.result});

  final _TestResult result;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final colors = result.ok ? BadgeColors.good(theme.brightness) : BadgeColors.bad(theme.brightness);
    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: Semantics(
        liveRegion: true,
        child: Container(
          width: double.infinity,
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(color: colors.background, borderRadius: BorderRadius.circular(8)),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Icon(result.ok ? Icons.check_circle_outline : Icons.error_outline, color: colors.foreground),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(result.message,
                        style: TextStyle(color: colors.foreground, fontWeight: FontWeight.w700)),
                  ),
                ],
              ),
              if (result.advice != null) ...[
                const SizedBox(height: 8),
                Text(result.advice!, style: TextStyle(color: colors.foreground)),
              ],
              if (result.note != null) ...[
                const SizedBox(height: 8),
                Text(result.note!, style: TextStyle(color: colors.foreground)),
              ],
              if (result.detail != null) ...[
                const SizedBox(height: 8),
                Text(result.detail!, style: theme.textTheme.bodySmall?.copyWith(color: colors.foreground)),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
