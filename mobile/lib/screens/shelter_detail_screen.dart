import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../config/theme.dart';
import '../models/shelter.dart';
import '../state/settings_controller.dart';
import '../state/shelter_detail_controller.dart';
import '../strings.dart';
import '../widgets/detail_field.dart';
import '../widgets/occupancy_bar.dart';
import '../widgets/ops_chips.dart';
import '../widgets/report_card.dart';
import '../widgets/section_card.dart';
import '../widgets/state_views.dart';
import 'shelter_form_screen.dart';

/// One shelter: its status and occupancy, close or reopen, check someone in, and the people checked in now.
class ShelterDetailScreen extends StatelessWidget {
  const ShelterDetailScreen({super.key, required this.shelter});

  /// What the list already knows; shown in the title while the detail loads.
  final Shelter shelter;

  @override
  Widget build(BuildContext context) {
    final settings = context.read<SettingsController>();
    return ChangeNotifierProvider(
      create: (_) => ShelterDetailController(() => settings.operations, shelter.id)..load(),
      child: _ShelterDetailView(shelter: shelter),
    );
  }
}

class _ShelterDetailView extends StatefulWidget {
  const _ShelterDetailView({required this.shelter});

  final Shelter shelter;

  @override
  State<_ShelterDetailView> createState() => _ShelterDetailViewState();
}

class _ShelterDetailViewState extends State<_ShelterDetailView> {
  final _formKey = GlobalKey<FormState>();
  final _name = TextEditingController();
  final _nic = TextEditingController();

  @override
  void dispose() {
    _name.dispose();
    _nic.dispose();
    super.dispose();
  }

  void _say(String? message) {
    if (message != null && message.isNotEmpty && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
    }
  }

  Future<void> _checkIn(ShelterDetailController controller) async {
    if (!_formKey.currentState!.validate()) {
      return;
    }
    final message = await controller.checkIn(fullName: _name.text, nic: _nic.text);
    if (message != null) {
      _name.clear();
      _nic.clear();
      _formKey.currentState?.reset();
    }
    _say(message);
  }

  Future<void> _checkOut(ShelterDetailController controller, ShelterOccupant occupant) async {
    final ok = await _confirm(title: S.checkOutTitle(occupant.fullName), body: S.checkOutBody, confirm: S.checkOut);
    if (ok) {
      _say(await controller.checkOut(occupant.id));
    }
  }

  Future<void> _edit(ShelterDetailController controller, Shelter shelter) async {
    final saved = await Navigator.of(context)
        .push<bool>(MaterialPageRoute<bool>(builder: (_) => ShelterFormScreen(editing: shelter)));
    if (saved == true && mounted) {
      await controller.load();
    }
  }

  Future<void> _close(ShelterDetailController controller) async {
    final ok = await _confirm(title: S.closeShelterTitle, body: S.closeShelterBody, confirm: S.closeShelter);
    if (ok) {
      _say(await controller.close());
    }
  }

  Future<bool> _confirm({required String title, required String body, required String confirm}) async {
    final answer = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: Text(title),
        content: Text(body),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text(S.cancel)),
          FilledButton(onPressed: () => Navigator.of(context).pop(true), child: Text(confirm)),
        ],
      ),
    );
    return answer ?? false;
  }

  @override
  Widget build(BuildContext context) {
    final controller = context.watch<ShelterDetailController>();
    final detail = controller.detail;
    final loadError = controller.loadError;

    final Widget body;
    if (detail == null && controller.loading) {
      body = const Center(child: CircularProgressIndicator());
    } else if (detail == null) {
      body = StateMessage(
        icon: loadError?.isConnectionDown ?? false ? Icons.cloud_off : Icons.error_outline,
        title: loadError?.isConnectionDown ?? false ? S.noConnection : S.shelterLoadFailed,
        body: loadError?.detail ?? S.unexpectedError,
        actionLabel: S.retry,
        onAction: controller.load,
      );
    } else {
      body = _content(context, controller, detail);
    }

    return Scaffold(
      appBar: AppBar(
        title: Text(detail?.shelter.name ?? widget.shelter.name, overflow: TextOverflow.ellipsis),
        actions: [
          if (detail != null)
            IconButton(
              tooltip: S.editShelter,
              onPressed: controller.busy ? null : () => _edit(controller, detail.shelter),
              icon: const Icon(Icons.edit_outlined),
            ),
        ],
      ),
      body: SafeArea(child: body),
    );
  }

  Widget _content(BuildContext context, ShelterDetailController controller, ShelterDetail detail) {
    final theme = Theme.of(context);
    final shelter = detail.shelter;
    final busy = controller.busy;
    final actionError = controller.actionError;
    final loadError = controller.loadError;
    final percent = shelter.capacity > 0 ? (shelter.currentOccupancy * 100 ~/ shelter.capacity) : 0;
    final canCheckIn = !shelter.isClosed && !shelter.isFull;

    return RefreshIndicator(
      onRefresh: controller.load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        children: [
          if (actionError != null) ...[
            _ErrorNotice(text: actionError.detail, onDismiss: controller.dismissActionError),
            const SizedBox(height: 12),
          ],
          if (loadError != null) ...[
            Semantics(
              liveRegion: true,
              child: Container(
                decoration: BoxDecoration(
                  color: theme.colorScheme.tertiaryContainer,
                  borderRadius: BorderRadius.circular(8),
                ),
                padding: const EdgeInsets.all(12),
                child: Row(
                  children: [
                    const Icon(Icons.cloud_off),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        loadError.isConnectionDown ? S.sheltersShowingLast : S.refreshFailed(loadError.detail),
                      ),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
          ],
          SectionCard(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    CircleAvatar(
                      radius: 24,
                      backgroundColor: theme.colorScheme.primaryContainer,
                      child: const Icon(Icons.night_shelter_outlined, size: 26, color: AppColors.shelter),
                    ),
                    const SizedBox(width: 12),
                    Expanded(child: Text(shelter.name, style: theme.textTheme.headlineSmall)),
                  ],
                ),
                const SizedBox(height: 12),
                DetailField(
                  first: true,
                  label: S.shelterStatusField,
                  child: Align(alignment: Alignment.centerLeft, child: OpsChips.shelterStatus(context, shelter.status)),
                ),
                DetailField(
                  label: S.nounOccupancy,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      OccupancyBar(occupancy: shelter.currentOccupancy, capacity: shelter.capacity),
                      const SizedBox(height: 2),
                      Text(
                        S.percentFull(percent),
                        style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                      ),
                    ],
                  ),
                ),
                DetailField(label: S.shelterDistrict, value: shelter.districtName),
                DetailField(label: S.shelterOrganization, value: shelter.organizationName),
              ],
            ),
          ),
          const SizedBox(height: 12),
          if (shelter.isClosed)
            FilledButton.icon(
              onPressed: busy ? null : () async => _say(await controller.reopen()),
              icon: const Icon(Icons.lock_open),
              label: const Text(S.reopenShelter),
            )
          else
            OutlinedButton.icon(
              onPressed: busy ? null : () => _close(controller),
              style: OutlinedButton.styleFrom(
                foregroundColor: theme.colorScheme.error,
                side: BorderSide(color: theme.colorScheme.error),
              ),
              icon: const Icon(Icons.lock_outline),
              label: const Text(S.closeShelter),
            ),
          const SizedBox(height: 12),
          SectionCard(
            title: S.checkInTitle,
            icon: Icons.person_add_alt_1_outlined,
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (!canCheckIn) ...[
                    Text(
                      shelter.isClosed ? S.checkInClosedNote : S.checkInFullNote,
                      style: theme.textTheme.bodyMedium?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                    ),
                    const SizedBox(height: 12),
                  ],
                  TextFormField(
                    controller: _name,
                    enabled: canCheckIn && !busy,
                    textInputAction: TextInputAction.next,
                    textCapitalization: TextCapitalization.words,
                    autocorrect: false,
                    decoration: const InputDecoration(
                      labelText: S.occupantNameLabel,
                      prefixIcon: Icon(Icons.person_outline),
                    ),
                    validator: (v) => (v ?? '').trim().isEmpty ? S.occupantNameRequired : null,
                  ),
                  const SizedBox(height: 12),
                  TextFormField(
                    controller: _nic,
                    enabled: canCheckIn && !busy,
                    textInputAction: TextInputAction.done,
                    autocorrect: false,
                    decoration: const InputDecoration(
                      labelText: S.occupantNicLabel,
                      prefixIcon: Icon(Icons.badge_outlined),
                    ),
                    validator: (v) => (v ?? '').trim().isEmpty ? S.occupantNicRequired : null,
                    onFieldSubmitted: (_) => _checkIn(controller),
                  ),
                  const SizedBox(height: 16),
                  FilledButton.icon(
                    onPressed: canCheckIn && !busy ? () => _checkIn(controller) : null,
                    icon: busy
                        ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.login),
                    label: const Text(S.checkInButton),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 12),
          SectionCard(
            title: '${S.occupantsTitle} (${detail.occupants.length})',
            icon: Icons.groups_outlined,
            child: detail.occupants.isEmpty
                ? Text(
                    S.occupantsEmpty,
                    style: theme.textTheme.bodyMedium?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                  )
                : Column(
                    children: [
                      for (var i = 0; i < detail.occupants.length; i++) ...[
                        if (i > 0) const Divider(),
                        _OccupantRow(
                          occupant: detail.occupants[i],
                          onCheckOut: busy ? null : () => _checkOut(controller, detail.occupants[i]),
                        ),
                      ],
                    ],
                  ),
          ),
        ],
      ),
    );
  }
}

class _OccupantRow extends StatelessWidget {
  const _OccupantRow({required this.occupant, required this.onCheckOut});

  final ShelterOccupant occupant;
  final VoidCallback? onCheckOut;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final muted = theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant);
    final time = occupant.checkInTime;
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 6),
      child: Row(
        children: [
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(occupant.fullName, style: theme.textTheme.titleSmall),
                Text(occupant.nic, style: muted),
                if (time != null) Text(S.occupantCheckedIn(formatDisplayTime(time)), style: muted),
              ],
            ),
          ),
          const SizedBox(width: 8),
          OutlinedButton(
            onPressed: onCheckOut,
            style: OutlinedButton.styleFrom(
              minimumSize: const Size(48, 40),
              padding: const EdgeInsets.symmetric(horizontal: 14),
            ),
            child: const Text(S.checkOut),
          ),
        ],
      ),
    );
  }
}

class _ErrorNotice extends StatelessWidget {
  const _ErrorNotice({required this.text, required this.onDismiss});

  final String text;
  final VoidCallback onDismiss;

  @override
  Widget build(BuildContext context) {
    final scheme = Theme.of(context).colorScheme;
    return Semantics(
      liveRegion: true,
      child: Container(
        decoration: BoxDecoration(color: scheme.errorContainer, borderRadius: BorderRadius.circular(8)),
        padding: const EdgeInsets.fromLTRB(12, 4, 4, 4),
        child: Row(
          children: [
            Icon(Icons.error_outline, color: scheme.onErrorContainer),
            const SizedBox(width: 8),
            Expanded(
              child: Text(text, style: TextStyle(color: scheme.onErrorContainer)),
            ),
            IconButton(
              tooltip: S.dismiss,
              onPressed: onDismiss,
              icon: Icon(Icons.close, color: scheme.onErrorContainer),
            ),
          ],
        ),
      ),
    );
  }
}
