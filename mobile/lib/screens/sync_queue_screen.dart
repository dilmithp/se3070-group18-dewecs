import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/queued_report.dart';
import '../models/sri_lanka_time.dart';
import '../strings.dart';
import '../sync/sync_service.dart';

/// Debug-only view of the queue: one row per item with its state, attempts, next try and last error.
/// It makes the offline behaviour easy to show and to explain.
class SyncQueueScreen extends StatelessWidget {
  const SyncQueueScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final sync = context.watch<SyncService>();
    final items = sync.items;
    final theme = Theme.of(context);
    return Scaffold(
      appBar: AppBar(title: const Text(S.syncQueueScreen)),
      body: SafeArea(
        child: items.isEmpty
            ? const Center(child: Text(S.syncQueueEmpty))
            : ListView(
                padding: const EdgeInsets.all(12),
                children: [
                  Text(sync.running ? S.syncRunning : S.syncIdle, style: theme.textTheme.titleSmall),
                  const SizedBox(height: 8),
                  for (final item in items) _QueueRow(item: item),
                ],
              ),
      ),
    );
  }
}

class _QueueRow extends StatelessWidget {
  const _QueueRow({required this.item});

  final QueuedReport item;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final next = item.nextAttemptAtMs;
    final lines = <String>[
      'state: ${item.state.name}, attempts: ${item.attempts}',
      'capturedAt: ${item.capturedAt}',
      'server id: ${item.serverId ?? '-'}, photo: ${item.photoPath == null ? 'none' : (item.photoUploaded ? 'sent' : (item.photoProblem ?? 'waiting'))}',
      if (next != null)
        'next try: ${formatContractTime(DateTime.fromMillisecondsSinceEpoch(next))} (local time)',
      if (item.lastError != null) 'last error: ${item.lastError}',
    ];
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text('${item.category} - ${item.description}', maxLines: 1, overflow: TextOverflow.ellipsis, style: theme.textTheme.titleSmall),
            const SizedBox(height: 4),
            for (final line in lines) Text(line, style: theme.textTheme.bodySmall),
          ],
        ),
      ),
    );
  }
}
