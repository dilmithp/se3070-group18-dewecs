import 'package:flutter/material.dart';

import '../models/queued_report.dart';
import '../models/report_item.dart';
import '../strings.dart';

/// A status as a coloured chip. The text label is always there, colour only reinforces it.
class StatusChip extends StatelessWidget {
  const StatusChip({super.key, required this.label, required this.icon, required this.background, required this.foreground});

  final String label;
  final IconData icon;
  final Color background;
  final Color foreground;

  /// Chip for a report row: the phone state while it is unsent, otherwise the server status.
  factory StatusChip.forItem(BuildContext context, ReportItem item) {
    final scheme = Theme.of(context).colorScheme;
    switch (item.queueState) {
      case QueueState.queued:
        return StatusChip(
            label: S.stateQueued,
            icon: Icons.schedule,
            background: scheme.surfaceContainerHighest,
            foreground: scheme.onSurface);
      case QueueState.sending:
        return StatusChip(
            label: S.stateSending,
            icon: Icons.sync,
            background: scheme.primaryContainer,
            foreground: scheme.onPrimaryContainer);
      case QueueState.needsAttention:
        return StatusChip(
            label: S.stateNeedsAttention,
            icon: Icons.error_outline,
            background: scheme.errorContainer,
            foreground: scheme.onErrorContainer);
      case QueueState.synced:
      case null:
        return StatusChip.forStatus(context, item.status);
    }
  }

  /// Chip for a raw server status. Unknown values are shown exactly as received.
  factory StatusChip.forStatus(BuildContext context, String status) {
    final scheme = Theme.of(context).colorScheme;
    final label = S.statusLabel(status);
    switch (status) {
      case 'PENDING_REVIEW':
        return StatusChip(
            label: label,
            icon: Icons.hourglass_empty,
            background: scheme.secondaryContainer,
            foreground: scheme.onSecondaryContainer);
      case 'VERIFIED':
        return StatusChip(
            label: label,
            icon: Icons.verified_outlined,
            background: scheme.tertiaryContainer,
            foreground: scheme.onTertiaryContainer);
      case 'ACTIONED':
        return StatusChip(
            label: label,
            icon: Icons.task_alt,
            background: scheme.primaryContainer,
            foreground: scheme.onPrimaryContainer);
      case 'REJECTED':
        return StatusChip(
            label: label,
            icon: Icons.block,
            background: scheme.errorContainer,
            foreground: scheme.onErrorContainer);
      case 'NEEDS_INFO':
        return StatusChip(
            label: label,
            icon: Icons.help_outline,
            background: scheme.tertiaryContainer,
            foreground: scheme.onTertiaryContainer);
      case 'PENDING_SYNC':
        return StatusChip(
            label: label,
            icon: Icons.schedule,
            background: scheme.surfaceContainerHighest,
            foreground: scheme.onSurface);
    }
    return StatusChip(
        label: label,
        icon: Icons.info_outline,
        background: scheme.surfaceContainerHighest,
        foreground: scheme.onSurface);
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: background, borderRadius: BorderRadius.circular(16)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 16, color: foreground),
          const SizedBox(width: 6),
          Flexible(
            child: Text(label, style: TextStyle(color: foreground, fontWeight: FontWeight.w600)),
          ),
        ],
      ),
    );
  }
}
