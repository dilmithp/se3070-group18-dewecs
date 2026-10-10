import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/rescue_request.dart';
import '../state/rescue_form_controller.dart';
import '../state/settings_controller.dart';
import '../strings.dart';
import '../widgets/section_card.dart';
import '../widgets/state_views.dart';
import 'rescue_detail_screen.dart';

/// Enter a rescue request (staff type it in from a call or a message). On success this screen replaces itself with
/// the new request's detail screen, as the web page redirects to it.
class RescueFormScreen extends StatelessWidget {
  const RescueFormScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final settings = context.read<SettingsController>();
    return ChangeNotifierProvider(
      create: (_) => RescueFormController(() => settings.operations)..load(),
      child: const _RescueFormView(),
    );
  }
}

class _RescueFormView extends StatefulWidget {
  const _RescueFormView();

  @override
  State<_RescueFormView> createState() => _RescueFormViewState();
}

class _RescueFormViewState extends State<_RescueFormView> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _phone = TextEditingController();
  final _lat = TextEditingController();
  final _lng = TextEditingController();
  final _description = TextEditingController();
  int? _districtId;
  String? _priority;

  /// The server's field errors, shown under the field until the user edits it.
  Map<String, String> _serverErrors = const {};

  @override
  void dispose() {
    _name.dispose();
    _phone.dispose();
    _lat.dispose();
    _lng.dispose();
    _description.dispose();
    super.dispose();
  }

  void _typed(String field) {
    final controller = context.read<RescueFormController>();
    if (_serverErrors.containsKey(field) || controller.saveError != null) {
      setState(() => _serverErrors = {..._serverErrors}..remove(field));
      controller.clearSaveError();
    }
  }

  double? _number(TextEditingController c) => double.tryParse(c.text.trim().replaceAll(',', '.'));

  Future<void> _submit(RescueFormController controller, RescueRequestFormData data) async {
    setState(() => _serverErrors = const {});
    if (!_formKey.currentState!.validate()) {
      return;
    }
    final lat = _lat.text.trim().isEmpty ? null : _number(_lat);
    final lng = _lng.text.trim().isEmpty ? null : _number(_lng);
    final result = await controller.submit(
      districtId: _districtId!,
      requesterName: _name.text,
      requesterPhone: _phone.text,
      gpsLat: lat,
      gpsLng: lng,
      description: _description.text,
      priority: _priority!,
    );
    if (!mounted) {
      return;
    }
    if (result == null) {
      setState(() => _serverErrors = controller.saveError?.fieldErrors ?? const {});
      _formKey.currentState?.validate();
      return;
    }
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(result.message)));
    final navigator = Navigator.of(context);
    final id = result.id;
    if (id == null) {
      navigator.pop(true);
      return;
    }
    final stub = RescueRequest(
      id: id,
      requesterName: _name.text.trim(),
      requesterPhone: _phone.text.trim(),
      districtId: _districtId!,
      districtName: data.districts.where((d) => d.id == _districtId).firstOrNull?.name ?? '',
      gpsLat: lat,
      gpsLng: lng,
      description: _description.text.trim(),
      priority: _priority!,
      status: 'PENDING',
      assignedTeamName: null,
      submittedAt: null,
      assignedAt: null,
      completedAt: null,
    );
    navigator.pushReplacement(MaterialPageRoute<void>(builder: (_) => RescueDetailScreen(request: stub)));
  }

  String? _coordinate(String? value, {required double limit, required String notNumber, required String range}) {
    final text = (value ?? '').trim();
    if (text.isEmpty) {
      return null;
    }
    final n = double.tryParse(text.replaceAll(',', '.'));
    if (n == null) {
      return notNumber;
    }
    return n < -limit || n > limit ? range : null;
  }

  @override
  Widget build(BuildContext context) {
    final controller = context.watch<RescueFormController>();
    final data = controller.data;
    final loadError = controller.loadError;

    final Widget body;
    if (data == null && controller.loading) {
      body = const Center(child: CircularProgressIndicator());
    } else if (data == null) {
      body = StateMessage(
        icon: loadError?.isConnectionDown ?? false ? Icons.cloud_off : Icons.error_outline,
        title: loadError?.isConnectionDown ?? false ? S.noConnection : S.formLoadFailed,
        body: loadError?.detail ?? S.unexpectedError,
        actionLabel: S.retry,
        onAction: controller.load,
      );
    } else {
      body = _form(context, controller, data);
    }

    return Scaffold(
      appBar: AppBar(title: const Text(S.newRescueTitle)),
      body: SafeArea(child: body),
    );
  }

  Widget _form(BuildContext context, RescueFormController controller, RescueRequestFormData data) {
    final theme = Theme.of(context);
    final saving = controller.saving;
    final error = controller.saveError;
    // A reason that belongs to no field (for example an invalid priority) is shown above the form.
    final general = error != null && error.fieldErrors.isEmpty ? error.detail : null;
    const coordinateKeyboard = TextInputType.numberWithOptions(decimal: true, signed: true);

    return Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          if (general != null) ...[
            Semantics(
              liveRegion: true,
              child: Container(
                decoration: BoxDecoration(
                  color: theme.colorScheme.errorContainer,
                  borderRadius: BorderRadius.circular(8),
                ),
                padding: const EdgeInsets.all(12),
                child: Row(
                  children: [
                    Icon(Icons.error_outline, color: theme.colorScheme.onErrorContainer),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(general, style: TextStyle(color: theme.colorScheme.onErrorContainer)),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
          ],
          SectionCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                DropdownButtonFormField<int>(
                  initialValue: _districtId,
                  isExpanded: true,
                  decoration: InputDecoration(
                    labelText: S.filterDistrict,
                    prefixIcon: const Icon(Icons.place_outlined),
                    errorText: _serverErrors['districtId'],
                  ),
                  hint: const Text(S.selectPlaceholder),
                  items: [for (final d in data.districts) DropdownMenuItem(value: d.id, child: Text(d.name))],
                  validator: (v) => v == null ? S.districtSelectRequired : null,
                  onChanged: saving
                      ? null
                      : (v) {
                          setState(() => _districtId = v);
                          _typed('districtId');
                        },
                ),
                const SizedBox(height: 16),
                DropdownButtonFormField<String>(
                  initialValue: _priority,
                  isExpanded: true,
                  decoration: InputDecoration(
                    labelText: S.priorityLabelField,
                    prefixIcon: const Icon(Icons.flag_outlined),
                    errorText: _serverErrors['priority'],
                  ),
                  hint: const Text(S.selectPlaceholder),
                  items: [for (final p in data.priorities) DropdownMenuItem(value: p, child: Text(S.priorityLabel(p)))],
                  validator: (v) => v == null ? S.prioritySelectRequired : null,
                  onChanged: saving
                      ? null
                      : (v) {
                          setState(() => _priority = v);
                          _typed('priority');
                        },
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _name,
                  enabled: !saving,
                  textInputAction: TextInputAction.next,
                  textCapitalization: TextCapitalization.words,
                  decoration: InputDecoration(
                    labelText: S.requesterNameLabel,
                    prefixIcon: const Icon(Icons.person_outline),
                    errorText: _serverErrors['requesterName'],
                  ),
                  validator: (v) => (v ?? '').trim().isEmpty ? S.requesterNameRequired : null,
                  onChanged: (_) => _typed('requesterName'),
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _phone,
                  enabled: !saving,
                  keyboardType: TextInputType.phone,
                  textInputAction: TextInputAction.next,
                  decoration: InputDecoration(
                    labelText: S.requesterPhoneLabel,
                    prefixIcon: const Icon(Icons.phone_outlined),
                    errorText: _serverErrors['requesterPhone'],
                  ),
                  validator: (v) => (v ?? '').trim().isEmpty ? S.requesterPhoneRequired : null,
                  onChanged: (_) => _typed('requesterPhone'),
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _lat,
                  enabled: !saving,
                  keyboardType: coordinateKeyboard,
                  textInputAction: TextInputAction.next,
                  decoration: InputDecoration(
                    labelText: S.gpsLatLabel,
                    helperText: S.gpsLatHelp,
                    prefixIcon: const Icon(Icons.my_location_outlined),
                    errorText: _serverErrors['gpsLat'],
                  ),
                  validator: (v) => _coordinate(v, limit: 90, notNumber: S.latitudeNumber, range: S.latitudeRange),
                  onChanged: (_) => _typed('gpsLat'),
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _lng,
                  enabled: !saving,
                  keyboardType: coordinateKeyboard,
                  textInputAction: TextInputAction.next,
                  decoration: InputDecoration(
                    labelText: S.gpsLngLabel,
                    helperText: S.gpsLngHelp,
                    prefixIcon: const Icon(Icons.my_location_outlined),
                    errorText: _serverErrors['gpsLng'],
                  ),
                  validator: (v) => _coordinate(v, limit: 180, notNumber: S.longitudeNumber, range: S.longitudeRange),
                  onChanged: (_) => _typed('gpsLng'),
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _description,
                  enabled: !saving,
                  minLines: 3,
                  maxLines: 6,
                  keyboardType: TextInputType.multiline,
                  textCapitalization: TextCapitalization.sentences,
                  decoration: InputDecoration(
                    labelText: S.rescueDescriptionLabel,
                    alignLabelWithHint: true,
                    errorText: _serverErrors['description'],
                  ),
                  validator: (v) => (v ?? '').trim().isEmpty ? S.rescueDescriptionRequired : null,
                  onChanged: (_) => _typed('description'),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: saving ? null : () => _submit(controller, data),
            icon: saving
                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.send_outlined),
            label: const Text(S.submitRescue),
          ),
          const SizedBox(height: 8),
          TextButton(onPressed: saving ? null : () => Navigator.of(context).pop(), child: const Text(S.cancel)),
        ],
      ),
    );
  }
}
