import 'dart:async';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/district.dart';
import '../models/queued_report.dart';
import '../models/sri_lanka_time.dart';
import '../state/app_dependencies.dart';
import '../state/identity_controller.dart';
import '../state/photo_picker.dart';
import '../state/reference_data_controller.dart';
import '../strings.dart';
import '../sync/sync_service.dart';
import '../validators.dart';
import '../widgets/category_chips.dart';
import '../widgets/location_section.dart';
import '../widgets/photo_section.dart';
import '../widgets/section_card.dart';

/// The report form. Submit saves the report on the phone first, then tries to send it; a new report gets its
/// capturedAt at that moment. With [editing] it fixes a report the server refused and sends it again.
class NewReportScreen extends StatefulWidget {
  const NewReportScreen({super.key, required this.onDone, this.editing});

  final VoidCallback onDone;
  final QueuedReport? editing;

  @override
  State<NewReportScreen> createState() => _NewReportScreenState();
}

class _NewReportScreenState extends State<NewReportScreen> {
  final _formKey = GlobalKey<FormState>();
  final _description = TextEditingController();
  final _lat = TextEditingController();
  final _lng = TextEditingController();

  late String _localId;
  String? _category;
  int? _districtId;
  String? _photoPath;
  String? _categoryError;
  String? _photoError;
  bool _photoBusy = false;
  bool _submitting = false;
  bool _photoKept = false;

  @override
  void initState() {
    super.initState();
    _newLocalId();
    final editing = widget.editing;
    if (editing != null) {
      _category = editing.category;
      _districtId = editing.districtId;
      _description.text = editing.description;
      _lat.text = editing.gpsLat.toString();
      _lng.text = editing.gpsLng.toString();
      _photoPath = editing.photoPath;
    } else {
      _districtId = context.read<IdentityController>().citizen?.districtId;
    }
    // Make sure the categories exist even if this is the first start without a connection history.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted && context.read<ReferenceDataController>().data == null) {
        context.read<ReferenceDataController>().refresh();
      }
    });
  }

  void _newLocalId() => _localId = 'r${DateTime.now().microsecondsSinceEpoch}';

  @override
  void dispose() {
    _description.dispose();
    _lat.dispose();
    _lng.dispose();
    _discardUnusedPhoto();
    super.dispose();
  }

  void _discardUnusedPhoto() {
    final path = _photoPath;
    if (path != null && !_photoKept && path != widget.editing?.photoPath) {
      unawaited(context.read<AppDependencies>().photos.delete(path));
    }
  }

  Future<void> _pickPhoto(PhotoSource source) async {
    final deps = context.read<AppDependencies>();
    setState(() {
      _photoBusy = true;
      _photoError = null;
    });
    try {
      final picked = await deps.picker.pick(source);
      if (picked != null) {
        final stored = await deps.photos.save(picked);
        _discardUnusedPhoto();
        if (mounted) {
          setState(() => _photoPath = stored);
        }
      }
    } on Exception {
      if (mounted) {
        setState(() => _photoError = S.photoPickFailed);
      }
    } finally {
      if (mounted) {
        setState(() => _photoBusy = false);
      }
    }
  }

  void _removePhoto() {
    _discardUnusedPhoto();
    setState(() => _photoPath = null);
  }

  Future<void> _submit() async {
    if (_submitting) {
      return;
    }
    final formOk = _formKey.currentState!.validate();
    setState(() => _categoryError = _category == null ? S.categoryRequired : null);
    if (!formOk || _category == null) {
      return;
    }
    setState(() => _submitting = true);

    final identity = context.read<IdentityController>();
    final sync = context.read<SyncService>();
    final districts = context.read<ReferenceDataController>().data?.districts ?? const <District>[];
    final messenger = ScaffoldMessenger.of(context);
    final citizen = identity.citizen!;
    final districtId = _districtId ?? citizen.districtId;
    final districtName = districts.where((d) => d.id == districtId).map((d) => d.name).firstOrNull ??
        (districtId == citizen.districtId ? citizen.districtName : '#$districtId');
    final lat = parseCoordinate(_lat.text)!;
    final lng = parseCoordinate(_lng.text)!;
    final editing = widget.editing;

    final String localId = editing?.localId ?? _localId;
    if (editing != null) {
      await _boundedWait(sync.editAndRetry(
        editing.localId,
        districtId: districtId,
        districtName: districtName,
        category: _category!,
        description: _description.text.trim(),
        gpsLat: lat,
        gpsLng: lng,
        capturedAt: sriLankaNowString(),
        photoPath: _photoPath,
      ));
    } else {
      await sync.enqueue(QueuedReport(
        localId: localId,
        createdAtMs: DateTime.now().millisecondsSinceEpoch,
        citizenId: citizen.id,
        districtId: districtId,
        districtName: districtName,
        category: _category!,
        description: _description.text.trim(),
        gpsLat: lat,
        gpsLng: lng,
        capturedAt: sriLankaNowString(),
        photoPath: _photoPath,
      ));
      await _boundedWait(sync.syncNow(force: true));
    }
    _photoKept = true;

    final item = sync.items.where((i) => i.localId == localId).firstOrNull;
    final String message;
    if (item != null && item.state == QueueState.needsAttention) {
      message = S.reportNeedsAttentionNow;
    } else if (item != null && item.reportStored) {
      message = S.reportSent;
    } else {
      message = S.reportSavedOnPhone;
    }
    messenger.showSnackBar(SnackBar(content: Text(message)));
    if (!mounted) {
      return;
    }
    _resetForm();
    widget.onDone();
  }

  /// Never keep the user waiting on a slow network: after 3 s the report is shown as saved and sending goes on.
  Future<void> _boundedWait(Future<void> work) async {
    try {
      await work.timeout(const Duration(seconds: 3));
    } on TimeoutException {
      // sending continues in the background of this app session
    }
  }

  void _resetForm() {
    _formKey.currentState?.reset();
    setState(() {
      _description.clear();
      _lat.clear();
      _lng.clear();
      _category = null;
      _categoryError = null;
      _photoPath = null;
      _photoKept = false;
      _districtId = context.read<IdentityController>().citizen?.districtId;
      _submitting = false;
      _newLocalId();
    });
  }

  @override
  Widget build(BuildContext context) {
    final reference = context.watch<ReferenceDataController>();
    final citizen = context.watch<IdentityController>().citizen;
    final deps = context.read<AppDependencies>();
    final theme = Theme.of(context);
    final categories = reference.data?.categories ?? const <String>[];
    final districts = reference.data?.districts ?? const <District>[];
    final dropdownItems = districts.isNotEmpty
        ? districts
        : [if (citizen != null) District(id: citizen.districtId, name: citizen.districtName)];
    final selectedDistrict = dropdownItems.any((d) => d.id == _districtId) ? _districtId : null;

    final form = Form(
      key: _formKey,
      child: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Each block is a card in a fixed list slot. Inside a card a slot may appear or vanish (an error line), which
          // only shifts that card's own children: inserting or removing a list child here would shift the form fields
          // below it, and Flutter would then throw away their state (including validation errors).
          SectionCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(S.categoryLabelText, style: theme.textTheme.titleMedium),
                const SizedBox(height: 12),
                if (categories.isEmpty)
                  Text(reference.loading ? S.loading : S.districtsMissing)
                else
                  CategoryChips(
                    categories: categories,
                    selected: _category,
                    onSelected: (value) => setState(() {
                      _category = value;
                      _categoryError = null;
                    }),
                  ),
                if (_categoryError != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(_categoryError!, style: TextStyle(color: theme.colorScheme.error)),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          SectionCard(
            child: Column(
              children: [
                TextFormField(
                  controller: _description,
                  decoration: const InputDecoration(labelText: S.descriptionLabel, alignLabelWithHint: true),
                  minLines: 3,
                  maxLines: 6,
                  maxLength: 2000,
                  textCapitalization: TextCapitalization.sentences,
                  validator: validateDescription,
                ),
                const SizedBox(height: 8),
                DropdownButtonFormField<int>(
                  key: ValueKey('district-$selectedDistrict-${dropdownItems.length}'),
                  initialValue: selectedDistrict,
                  decoration: const InputDecoration(labelText: S.districtLabel),
                  items: [for (final d in dropdownItems) DropdownMenuItem(value: d.id, child: Text(d.name))],
                  onChanged: (value) => setState(() => _districtId = value),
                  validator: validateDistrict,
                ),
              ],
            ),
          ),
          const SizedBox(height: 12),
          SectionCard(child: LocationSection(service: deps.location, lat: _lat, lng: _lng)),
          const SizedBox(height: 12),
          SectionCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                PhotoSection(photoPath: _photoPath, busy: _photoBusy, onPick: _pickPhoto, onRemove: _removePhoto),
                if (_photoError != null)
                  Padding(
                    padding: const EdgeInsets.only(top: 8),
                    child: Text(_photoError!, style: TextStyle(color: theme.colorScheme.error)),
                  ),
              ],
            ),
          ),
          const SizedBox(height: 24),
          FilledButton.icon(
            onPressed: _submitting ? null : _submit,
            icon: _submitting
                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.send),
            label: const Text(S.submitReport),
          ),
        ],
      ),
    );

    if (widget.editing == null) {
      return form;
    }
    return Scaffold(appBar: AppBar(title: const Text(S.editReportTitle)), body: SafeArea(child: form));
  }
}
