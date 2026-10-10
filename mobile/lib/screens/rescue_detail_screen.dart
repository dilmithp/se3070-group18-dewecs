import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../config/theme.dart';
import '../models/rescue_request.dart';
import '../state/rescue_detail_controller.dart';
import '../state/settings_controller.dart';
import '../strings.dart';
import '../widgets/detail_field.dart';
import '../widgets/ops_chips.dart';
import '../widgets/report_card.dart';
import '../widgets/section_card.dart';
import '../widgets/state_views.dart';

/// One rescue request: who asked, where, how urgent, which team has it; assign a team while it is pending, complete
/// it once assigned, or cancel it while it is still open.
class RescueDetailScreen extends StatelessWidget {
  const RescueDetailScreen({super.key, required this.request});

  /// What the list already knows; shown in the title while the detail loads.
  final RescueRequest request;

  @override
  Widget build(BuildContext context) {
    final settings = context.read<SettingsController>();
    return ChangeNotifierProvider(
      create: (_) => RescueDetailController(() => settings.operations, request.id)..load(),
      child: _RescueDetailView(request: request),
    );
  }
}

class _RescueDetailView extends StatefulWidget {
  const _RescueDetailView({required this.request});

  final RescueRequest request;

  @override
  State<_RescueDetailView> createState() => _RescueDetailViewState();
}

class _RescueDetailViewState extends State<_RescueDetailView> {
  int? _teamId;

  void _say(String? message) {
    if (message != null && message.isNotEmpty && mounted) {
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
    }
  }

  Future<void> _assign(RescueDetailController controller, List<RescueTeam> teams) async {
    final id = _teamId ?? teams.firstOrNull?.id;
    if (id == null) {
      return;
    }
    _say(await controller.assign(id));
    if (mounted) {
      setState(() => _teamId = null);
    }
  }

  Future<void> _cancel(RescueDetailController controller) async {
    final answer = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text(S.cancelRequestTitle),
        content: const Text(S.cancelRequestBody),
        actions: [
          TextButton(onPressed: () => Navigator.of(context).pop(false), child: const Text(S.keepRequest)),
          FilledButton(
            onPressed: () => Navigator.of(context).pop(true),
            style: FilledButton.styleFrom(
              backgroundColor: Theme.of(context).colorScheme.error,
              foregroundColor: Theme.of(context).colorScheme.onError,
            ),
            child: const Text(S.cancelRequest),
          ),
        ],
      ),
    );
    if (answer == true) {
      _say(await controller.cancel());
    }
  }

  @override
  Widget build(BuildContext context) {
    final controller = context.watch<RescueDetailController>();
    final detail = controller.detail;
    final loadError = controller.loadError;

    final Widget body;
    if (detail == null && controller.loading) {
      body = const Center(child: CircularProgressIndicator());
    } else if (detail == null) {
      body = StateMessage(
        icon: loadError?.isConnectionDown ?? false ? Icons.cloud_off : Icons.error_outline,
        title: loadError?.isConnectionDown ?? false ? S.noConnection : S.rescueLoadOneFailed,
        body: loadError?.detail ?? S.unexpectedError,
        actionLabel: S.retry,
        onAction: controller.load,
      );
    } else {
      body = _content(context, controller, detail);
    }

    return Scaffold(
      appBar: AppBar(title: const Text(S.rescueDetailTitle)),
      body: SafeArea(child: body),
    );
  }

  Widget _content(BuildContext context, RescueDetailController controller, RescueRequestDetail detail) {
    final theme = Theme.of(context);
    final request = detail.request;
    final busy = controller.busy;
    final actionError = controller.actionError;
    final loadError = controller.loadError;
    final teams = detail.availableTeams;
    final selectedTeam = teams.any((t) => t.id == _teamId) ? _teamId : teams.firstOrNull?.id;

    String time(DateTime? t) => t == null ? S.notGiven : formatDisplayTime(t);

    return RefreshIndicator(
      onRefresh: controller.load,
      child: ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.all(16),
        children: [
          if (actionError != null) ...[
            Semantics(
              liveRegion: true,
              child: Container(
                decoration: BoxDecoration(
                  color: theme.colorScheme.errorContainer,
                  borderRadius: BorderRadius.circular(8),
                ),
                padding: const EdgeInsets.fromLTRB(12, 4, 4, 4),
                child: Row(
                  children: [
                    Icon(Icons.error_outline, color: theme.colorScheme.onErrorContainer),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(actionError.detail, style: TextStyle(color: theme.colorScheme.onErrorContainer)),
                    ),
                    IconButton(
                      tooltip: S.dismiss,
                      onPressed: controller.dismissActionError,
                      icon: Icon(Icons.close, color: theme.colorScheme.onErrorContainer),
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 12),
          ],
          if (loadError != null) ...[
            _InlineNotice(
              text: loadError.isConnectionDown ? S.rescueShowingLast : S.refreshFailed(loadError.detail),
              color: theme.colorScheme.tertiaryContainer,
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
                      backgroundColor: theme.colorScheme.errorContainer,
                      child: const Icon(Icons.support_outlined, size: 26, color: AppColors.rescue),
                    ),
                    const SizedBox(width: 12),
                    Expanded(child: Text(request.requesterName, style: theme.textTheme.headlineSmall)),
                  ],
                ),
                const SizedBox(height: 12),
                DetailField(
                  first: true,
                  label: S.rescueFieldStatus,
                  child: Align(alignment: Alignment.centerLeft, child: OpsChips.rescueStatus(context, request.status)),
                ),
                DetailField(
                  label: S.rescueFieldPriority,
                  child: Align(alignment: Alignment.centerLeft, child: OpsChips.priority(context, request.priority)),
                ),
                DetailField(label: S.rescueFieldRegion, value: request.districtName),
                DetailField(label: S.rescueFieldPhone, value: request.requesterPhone),
                DetailField(
                  label: S.rescueFieldGps,
                  value: request.hasLocation ? '${request.gpsLat}, ${request.gpsLng}' : S.notGiven,
                ),
                DetailField(label: S.rescueFieldDescription, value: request.description),
                DetailField(label: S.rescueFieldTeam, value: request.assignedTeamName ?? S.notGiven),
                DetailField(label: S.rescueFieldSubmitted, value: time(request.submittedAt)),
                DetailField(label: S.rescueFieldAssigned, value: time(request.assignedAt)),
                DetailField(label: S.rescueFieldCompleted, value: time(request.completedAt)),
              ],
            ),
          ),
          if (request.canAssign) ...[
            const SizedBox(height: 12),
            SectionCard(
              title: S.assignTitle,
              icon: Icons.groups_outlined,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  if (teams.isEmpty)
                    Text(
                      S.noTeamAvailable,
                      style: theme.textTheme.bodyMedium?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                    )
                  else
                    DropdownButtonFormField<int>(
                      initialValue: selectedTeam,
                      isExpanded: true,
                      decoration: const InputDecoration(
                        labelText: S.assignTeamLabel,
                        prefixIcon: Icon(Icons.groups_outlined),
                      ),
                      items: [
                        for (final t in teams)
                          DropdownMenuItem(
                            value: t.id,
                            child: Text(
                              t.districtName == null ? t.name : '${t.name} (${t.districtName})',
                              overflow: TextOverflow.ellipsis,
                            ),
                          ),
                      ],
                      onChanged: busy ? null : (v) => setState(() => _teamId = v),
                    ),
                  const SizedBox(height: 16),
                  FilledButton.icon(
                    onPressed: busy || teams.isEmpty ? null : () => _assign(controller, teams),
                    icon: busy
                        ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                        : const Icon(Icons.assignment_ind_outlined),
                    label: const Text(S.assignButton),
                  ),
                ],
              ),
            ),
          ],
          if (request.canComplete) ...[
            const SizedBox(height: 12),
            FilledButton.icon(
              onPressed: busy ? null : () async => _say(await controller.complete()),
              icon: const Icon(Icons.task_alt),
              label: const Text(S.completeRequest),
            ),
          ],
          if (request.canCancel) ...[
            const SizedBox(height: 12),
            OutlinedButton.icon(
              onPressed: busy ? null : () => _cancel(controller),
              style: OutlinedButton.styleFrom(
                foregroundColor: theme.colorScheme.error,
                side: BorderSide(color: theme.colorScheme.error),
              ),
              icon: const Icon(Icons.cancel_outlined),
              label: const Text(S.cancelRequest),
            ),
          ],
        ],
      ),
    );
  }
}

/// A notice bar inside a page (not at the very top of a list), announced when it appears.
class _InlineNotice extends StatelessWidget {
  const _InlineNotice({required this.text, required this.color});

  final String text;
  final Color color;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      liveRegion: true,
      child: Container(
        decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(8)),
        padding: const EdgeInsets.all(12),
        child: Row(
          children: [
            const Icon(Icons.cloud_off),
            const SizedBox(width: 8),
            Expanded(child: Text(text)),
          ],
        ),
      ),
    );
  }
}
