import 'dart:io';

import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/queued_report.dart';
import '../models/report_item.dart';
import '../state/app_dependencies.dart';
import '../state/settings_controller.dart';
import '../state/url_opener.dart';
import '../strings.dart';
import '../sync/sync_service.dart';
import '../widgets/category_chips.dart';
import '../widgets/report_card.dart';
import '../widgets/section_card.dart';
import '../widgets/status_chip.dart';
import 'new_report_screen.dart';

/// Everything about one report. The officers' action note shows only when the report was actioned. A report still
/// on this phone can be fixed (when the server refused it) or deleted.
class ReportDetailScreen extends StatelessWidget {
  const ReportDetailScreen({super.key, required this.item});

  final ReportItem item;

  @override
  Widget build(BuildContext context) {
    // A local report is looked up again so the screen follows its state while it is being sent.
    final sync = context.watch<SyncService>();
    final localId = item.localId;
    final queued = localId == null ? null : sync.items.where((i) => i.localId == localId).firstOrNull;
    final current = queued != null ? ReportItem.fromQueued(queued) : item;
    final theme = Theme.of(context);

    return Scaffold(
      appBar: AppBar(title: const Text(S.detailTitle)),
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            SectionCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      CircleAvatar(
                        radius: 24,
                        backgroundColor: theme.colorScheme.primaryContainer,
                        child: Icon(categoryIcon(current.category), size: 26, color: theme.colorScheme.onPrimaryContainer),
                      ),
                      const SizedBox(width: 12),
                      Expanded(child: Text(S.categoryName(current.category), style: theme.textTheme.headlineSmall)),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Align(alignment: Alignment.centerLeft, child: StatusChip.forItem(context, current)),
                  if (current.queueState == QueueState.needsAttention || current.queueState == QueueState.queued)
                    _ProblemBox(item: current),
                ],
              ),
            ),
            const SizedBox(height: 12),
            SectionCard(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _Field(label: S.detailDescription, value: current.description, first: true),
                  _Field(label: S.detailDistrict, value: current.districtName),
                  _Field(label: S.detailSubmitted, value: formatDisplayTime(current.submittedAt)),
                  _Field(label: S.detailLocation, value: '${current.gpsLat}, ${current.gpsLng}'),
                  const SizedBox(height: 12),
                  OutlinedButton.icon(
                    onPressed: () => _openMaps(context, current),
                    icon: const Icon(Icons.map_outlined),
                    label: const Text(S.openInMaps),
                  ),
                ],
              ),
            ),
            if (current.actionNote != null) ...[
              const SizedBox(height: 12),
              Card(
                color: theme.colorScheme.primaryContainer,
                child: Padding(
                  padding: const EdgeInsets.all(16),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Row(children: [
                        Icon(Icons.task_alt, size: 20, color: theme.colorScheme.onPrimaryContainer),
                        const SizedBox(width: 8),
                        Text(S.detailActionNote,
                            style: theme.textTheme.titleSmall?.copyWith(color: theme.colorScheme.onPrimaryContainer)),
                      ]),
                      const SizedBox(height: 6),
                      Text(current.actionNote!, style: TextStyle(color: theme.colorScheme.onPrimaryContainer)),
                    ],
                  ),
                ),
              ),
            ],
            const SizedBox(height: 12),
            SectionCard(
              title: S.detailPhoto,
              icon: Icons.photo_outlined,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  _PhotoBlock(item: current),
                  if (current.photoProblem != null)
                    Padding(
                      padding: const EdgeInsets.only(top: 8),
                      child: Text('${S.photoNotSent} ${current.photoProblem}',
                          style: TextStyle(color: theme.colorScheme.error)),
                    ),
                ],
              ),
            ),
            if (queued != null) ..._actions(context, queued),
          ],
        ),
      ),
    );
  }

  List<Widget> _actions(BuildContext context, QueuedReport queued) {
    final canEdit = queued.state == QueueState.needsAttention && !queued.reportStored;
    final canDelete = queued.state == QueueState.needsAttention || (queued.isActive && !queued.reportStored);
    return [
      const SizedBox(height: 24),
      if (canEdit)
        FilledButton.icon(
          onPressed: () => Navigator.of(context).push(MaterialPageRoute<void>(
            builder: (_) => NewReportScreen(editing: queued, onDone: () => Navigator.of(context).pop()),
          )).then((_) {
            if (context.mounted) {
              Navigator.of(context).pop();
            }
          }),
          icon: const Icon(Icons.edit),
          label: const Text(S.editAndRetry),
        ),
      if (canDelete) ...[
        const SizedBox(height: 8),
        OutlinedButton.icon(
          onPressed: () => _confirmDelete(context, queued),
          icon: const Icon(Icons.delete_outline),
          label: const Text(S.deleteReport),
        ),
      ],
    ];
  }

  Future<void> _confirmDelete(BuildContext context, QueuedReport queued) async {
    final sync = context.read<SyncService>();
    final navigator = Navigator.of(context);
    final messenger = ScaffoldMessenger.of(context);
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text(S.deleteTitle),
        content: const Text(S.deleteBody),
        actions: [
          TextButton(onPressed: () => Navigator.pop(context, false), child: const Text(S.cancel)),
          FilledButton(onPressed: () => Navigator.pop(context, true), child: const Text(S.deleteReport)),
        ],
      ),
    );
    if (confirmed == true) {
      await sync.delete(queued.localId);
      navigator.pop();
      messenger.showSnackBar(const SnackBar(content: Text(S.reportDeleted)));
    }
  }

  Future<void> _openMaps(BuildContext context, ReportItem current) async {
    final opener = context.read<AppDependencies>().opener;
    final messenger = ScaffoldMessenger.of(context);
    if (await opener(geoUri(current.gpsLat, current.gpsLng))) {
      return;
    }
    if (await opener(openStreetMapUri(current.gpsLat, current.gpsLng))) {
      return;
    }
    messenger.showSnackBar(const SnackBar(content: Text(S.mapsFailed)));
  }
}

class _Field extends StatelessWidget {
  const _Field({required this.label, required this.value, this.first = false});

  final String label;
  final String value;
  final bool first;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Padding(
      padding: EdgeInsets.only(top: first ? 0 : 14),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(label, style: theme.textTheme.labelLarge?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
          const SizedBox(height: 2),
          Text(value, style: theme.textTheme.bodyLarge),
        ],
      ),
    );
  }
}

class _ProblemBox extends StatelessWidget {
  const _ProblemBox({required this.item});

  final ReportItem item;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final lines = <String>[
      if (item.error != null) item.error!,
      ...item.fieldErrors.values,
      if (item.attempts > 0) S.detailAttempts(item.attempts),
    ];
    if (lines.isEmpty) {
      return const SizedBox.shrink();
    }
    return Padding(
      padding: const EdgeInsets.only(top: 12),
      child: Card(
        color: item.queueState == QueueState.needsAttention
            ? theme.colorScheme.errorContainer
            : theme.colorScheme.surfaceContainerHighest,
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              if (item.queueState == QueueState.needsAttention) Text(S.detailProblem, style: theme.textTheme.titleSmall),
              for (final line in lines) Text(line),
            ],
          ),
        ),
      ),
    );
  }
}

class _PhotoBlock extends StatelessWidget {
  const _PhotoBlock({required this.item});

  final ReportItem item;

  @override
  Widget build(BuildContext context) {
    final local = item.localPhotoPath;
    final server = item.serverPhotoPath;
    Widget placeholder(String text) => Container(
          height: 120,
          alignment: Alignment.center,
          decoration: BoxDecoration(
            color: Theme.of(context).colorScheme.surfaceContainerHighest,
            borderRadius: BorderRadius.circular(8),
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [const Icon(Icons.image_not_supported_outlined), const SizedBox(height: 4), Text(text)],
          ),
        );

    if (server != null) {
      final url = context.read<SettingsController>().photoUrlFor(server);
      if (url != null) {
        return ClipRRect(
          borderRadius: BorderRadius.circular(8),
          child: Image.network(
            url,
            fit: BoxFit.cover,
            loadingBuilder: (context, child, progress) =>
                progress == null ? child : const SizedBox(height: 120, child: Center(child: CircularProgressIndicator())),
            errorBuilder: (_, _, _) => placeholder(S.photoLoadFailed),
          ),
        );
      }
    }
    if (local != null) {
      return ClipRRect(
        borderRadius: BorderRadius.circular(8),
        child: Image.file(File(local), fit: BoxFit.cover, errorBuilder: (_, _, _) => placeholder(S.photoLoadFailed)),
      );
    }
    return placeholder(S.photoPlaceholder);
  }
}
