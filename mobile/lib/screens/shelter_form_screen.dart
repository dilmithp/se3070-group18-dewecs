import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';

import '../models/shelter.dart';
import '../state/settings_controller.dart';
import '../state/shelter_form_controller.dart';
import '../strings.dart';
import '../widgets/section_card.dart';
import '../widgets/state_views.dart';
import 'shelter_detail_screen.dart';

/// Create a shelter, or (with [editing]) change its name and capacity. As on the web, the district and the owning
/// organization are fixed after creation, so the edit form shows them but does not let them change.
///
/// Create replaces itself with the new shelter's detail screen; edit pops with `true`.
class ShelterFormScreen extends StatelessWidget {
  const ShelterFormScreen({super.key, this.editing});

  final Shelter? editing;

  @override
  Widget build(BuildContext context) {
    final settings = context.read<SettingsController>();
    return ChangeNotifierProvider(
      create: (_) => ShelterFormController(() => settings.operations)..load(),
      child: _ShelterFormView(editing: editing),
    );
  }
}

class _ShelterFormView extends StatefulWidget {
  const _ShelterFormView({required this.editing});

  final Shelter? editing;

  @override
  State<_ShelterFormView> createState() => _ShelterFormViewState();
}

class _ShelterFormViewState extends State<_ShelterFormView> {
  final _formKey = GlobalKey<FormState>();
  late final TextEditingController _name = TextEditingController(text: widget.editing?.name ?? '');
  late final TextEditingController _capacity = TextEditingController(
    text: widget.editing == null ? '' : '${widget.editing!.capacity}',
  );
  int? _districtId;
  int? _organizationId;

  /// The server's field errors, shown under the field until the user edits it.
  Map<String, String> _serverErrors = const {};

  bool get _isEdit => widget.editing != null;

  @override
  void initState() {
    super.initState();
    _districtId = widget.editing?.districtId;
  }

  @override
  void dispose() {
    _name.dispose();
    _capacity.dispose();
    super.dispose();
  }

  /// An edit knows the shelter's organization only by name, so it is looked up in the list the form loaded.
  int? _editOrganizationId(ShelterFormData data) {
    final name = widget.editing?.organizationName;
    return data.organizations.where((o) => o.name == name).firstOrNull?.id ?? data.organizations.firstOrNull?.id;
  }

  void _typed(String field) {
    if (_serverErrors.containsKey(field) || context.read<ShelterFormController>().saveError != null) {
      setState(() => _serverErrors = {..._serverErrors}..remove(field));
      context.read<ShelterFormController>().clearSaveError();
    }
  }

  Future<void> _submit(ShelterFormController controller, ShelterFormData data) async {
    setState(() => _serverErrors = const {});
    if (!_formKey.currentState!.validate()) {
      return;
    }
    final name = _name.text;
    final capacity = int.parse(_capacity.text.trim());
    final districtId = _districtId!;
    final organizationId = _isEdit ? _editOrganizationId(data)! : _organizationId!;
    final editing = widget.editing;
    final result = editing == null
        ? await controller.create(
            districtId: districtId,
            organizationId: organizationId,
            name: name,
            capacity: capacity,
          )
        : await controller.update(
            editing.id,
            districtId: districtId,
            organizationId: organizationId,
            name: name,
            capacity: capacity,
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
    if (editing != null) {
      navigator.pop(true);
      return;
    }
    final created = result.id;
    if (created == null) {
      navigator.pop(true);
      return;
    }
    final shelter = Shelter(
      id: created,
      name: name.trim(),
      districtId: districtId,
      districtName: data.districts.where((d) => d.id == districtId).firstOrNull?.name ?? '',
      organizationName: data.organizations.where((o) => o.id == organizationId).firstOrNull?.name ?? '',
      capacity: capacity,
      currentOccupancy: 0,
      status: 'OPEN',
    );
    navigator.pushReplacement(MaterialPageRoute<void>(builder: (_) => ShelterDetailScreen(shelter: shelter)));
  }

  @override
  Widget build(BuildContext context) {
    final controller = context.watch<ShelterFormController>();
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
      appBar: AppBar(title: Text(_isEdit ? S.editShelterTitle : S.newShelterTitle)),
      body: SafeArea(child: body),
    );
  }

  Widget _form(BuildContext context, ShelterFormController controller, ShelterFormData data) {
    final theme = Theme.of(context);
    final saving = controller.saving;
    final error = controller.saveError;
    // A reason that belongs to no field (for example the capacity rule) is shown above the form.
    final general = error != null && error.fieldErrors.isEmpty ? error.detail : null;
    final editing = widget.editing;
    final organizationName = editing?.organizationName ?? '';
    final districtName =
        data.districts.where((d) => d.id == _districtId).firstOrNull?.name ?? editing?.districtName ?? '';

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
                if (_isEdit) ...[
                  _FixedValue(label: S.filterDistrict, value: districtName, help: S.districtFixedHelp),
                  const SizedBox(height: 16),
                  _FixedValue(label: S.owningOrganizationLabel, value: organizationName, help: S.organizationFixedHelp),
                ] else ...[
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
                  DropdownButtonFormField<int>(
                    initialValue: _organizationId,
                    isExpanded: true,
                    decoration: InputDecoration(
                      labelText: S.owningOrganizationLabel,
                      prefixIcon: const Icon(Icons.apartment_outlined),
                      errorText: _serverErrors['organizationId'],
                    ),
                    hint: const Text(S.selectPlaceholder),
                    items: [for (final o in data.organizations) DropdownMenuItem(value: o.id, child: Text(o.name))],
                    validator: (v) => v == null ? S.organizationSelectRequired : null,
                    onChanged: saving
                        ? null
                        : (v) {
                            setState(() => _organizationId = v);
                            _typed('organizationId');
                          },
                  ),
                ],
                const SizedBox(height: 16),
                TextFormField(
                  controller: _name,
                  enabled: !saving,
                  textInputAction: TextInputAction.next,
                  textCapitalization: TextCapitalization.words,
                  decoration: InputDecoration(
                    labelText: S.shelterNameLabel,
                    prefixIcon: const Icon(Icons.night_shelter_outlined),
                    errorText: _serverErrors['name'],
                  ),
                  validator: (v) => (v ?? '').trim().isEmpty ? S.shelterNameRequired : null,
                  onChanged: (_) => _typed('name'),
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: _capacity,
                  enabled: !saving,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  textInputAction: TextInputAction.done,
                  decoration: InputDecoration(
                    labelText: S.shelterCapacityLabel,
                    prefixIcon: const Icon(Icons.groups_outlined),
                    helperText: editing == null ? null : S.capacityCheckedInHelp(editing.currentOccupancy),
                    errorText: _serverErrors['capacity'],
                  ),
                  validator: (v) {
                    final text = (v ?? '').trim();
                    if (text.isEmpty) {
                      return S.capacityRequired;
                    }
                    final n = int.tryParse(text);
                    return n == null || n <= 0 ? S.capacityPositive : null;
                  },
                  onChanged: (_) => _typed('capacity'),
                  onFieldSubmitted: (_) => _submit(controller, data),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),
          FilledButton.icon(
            onPressed: saving ? null : () => _submit(controller, data),
            icon: saving
                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                : Icon(_isEdit ? Icons.save_outlined : Icons.add),
            label: Text(_isEdit ? S.saveChanges : S.createShelter),
          ),
          const SizedBox(height: 8),
          TextButton(onPressed: saving ? null : () => Navigator.of(context).pop(), child: const Text(S.cancel)),
        ],
      ),
    );
  }
}

/// A value the form shows but does not let the user change, with the reason under it.
class _FixedValue extends StatelessWidget {
  const _FixedValue({required this.label, required this.value, required this.help});

  final String label;
  final String value;
  final String help;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Semantics(
      label: '$label: $value. $help',
      excludeSemantics: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: theme.textTheme.labelLarge?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
          const SizedBox(height: 2),
          Text(value, style: theme.textTheme.bodyLarge),
          const SizedBox(height: 2),
          Text(help, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
        ],
      ),
    );
  }
}
