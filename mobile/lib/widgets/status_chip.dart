import 'package:flutter/material.dart';

import '../config/theme.dart';
import '../models/queued_report.dart';
import '../models/report_item.dart';
import '../strings.dart';

/// A status as a pill badge, the same look as the web `.badge`: a dot, then the text label. The text is always there,
/// colour only reinforces it.
class StatusChip extends StatelessWidget {
  const StatusChip({super.key, required this.label, required this.icon, required this.background, required this.foreground});

  final String label;
  final IconData icon;
  final Color background;
  final Color foreground;

  /// Chip for a report row: the phone state while it is unsent, otherwise the server status.
  factory StatusChip.forItem(BuildContext context, ReportItem item) {
    final b = Theme.of(context).brightness;
    switch (item.queueState) {
      case QueueState.queued:
        return StatusChip._of(S.stateQueued, Icons.schedule, BadgeColors.quiet(b));
      case QueueState.sending:
        return StatusChip._of(S.stateSending, Icons.sync, BadgeColors.info(b));
      case QueueState.needsAttention:
        return StatusChip._of(S.stateNeedsAttention, Icons.error_outline, BadgeColors.bad(b));
      case QueueState.synced:
      case null:
        return StatusChip.forStatus(context, item.status);
    }
  }

  /// Chip for a raw server status. Unknown values are shown exactly as received.
  factory StatusChip.forStatus(BuildContext context, String status) {
    final b = Theme.of(context).brightness;
    final label = S.statusLabel(status);
    switch (status) {
      case 'PENDING_REVIEW':
        return StatusChip._of(label, Icons.hourglass_empty, BadgeColors.attention(b));
      case 'VERIFIED':
        return StatusChip._of(label, Icons.verified_outlined, BadgeColors.info(b));
      case 'ACTIONED':
        return StatusChip._of(label, Icons.task_alt, BadgeColors.good(b));
      case 'REJECTED':
        return StatusChip._of(label, Icons.block, BadgeColors.bad(b));
      case 'NEEDS_INFO':
        return StatusChip._of(label, Icons.help_outline, BadgeColors.attention(b));
      case 'PENDING_SYNC':
        return StatusChip._of(label, Icons.schedule, BadgeColors.quiet(b));
    }
    return StatusChip._of(label, Icons.info_outline, BadgeColors.quiet(b));
  }

  factory StatusChip._of(String label, IconData icon, BadgeColors colors) =>
      StatusChip(label: label, icon: icon, background: colors.background, foreground: colors.foreground);

  /// The colour that marks a status on the left edge of a card.
  static Color accentFor(BuildContext context, ReportItem item) {
    final chip = StatusChip.forItem(context, item);
    return chip.foreground;
  }

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(color: background, borderRadius: BorderRadius.circular(999)),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Container(width: 8, height: 8, decoration: BoxDecoration(color: foreground, shape: BoxShape.circle)),
          const SizedBox(width: 6),
          Flexible(
            child: Text(label, style: TextStyle(color: foreground, fontWeight: FontWeight.w700, fontSize: 13)),
          ),
        ],
      ),
    );
  }
}
