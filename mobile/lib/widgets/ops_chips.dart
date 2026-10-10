import 'package:flutter/material.dart';

import '../config/theme.dart';
import '../strings.dart';
import 'status_chip.dart';

/// Pill badges for the shelter and rescue screens, the same look and palette as the web `.badge-*` rules.
/// The label is always text; the colour only reinforces it. Unknown values are shown exactly as received.
class OpsChips {
  OpsChips._();

  static StatusChip shelterStatus(BuildContext context, String status) {
    final b = Theme.of(context).brightness;
    final label = S.shelterStatusLabel(status);
    switch (status) {
      case 'OPEN':
        return _chip(label, BadgeColors.good(b));
      case 'FULL':
        return _chip(label, BadgeColors.attention(b));
    }
    return _chip(label, BadgeColors.quiet(b));
  }

  static StatusChip rescueStatus(BuildContext context, String status) {
    final b = Theme.of(context).brightness;
    final label = S.rescueStatusLabel(status);
    switch (status) {
      case 'PENDING':
        return _chip(label, BadgeColors.attention(b));
      case 'ASSIGNED':
        return _chip(label, BadgeColors.info(b));
      case 'COMPLETED':
        return _chip(label, BadgeColors.good(b));
    }
    return _chip(label, BadgeColors.quiet(b));
  }

  static StatusChip priority(BuildContext context, String priority) {
    final b = Theme.of(context).brightness;
    final label = S.priorityLabel(priority);
    switch (priority) {
      case 'CRITICAL':
        return _chip(label, BadgeColors.bad(b));
      case 'HIGH':
        return _chip(label, BadgeColors.attention(b));
      case 'MODERATE':
        return _chip(label, BadgeColors.info(b));
    }
    return _chip(label, BadgeColors.quiet(b));
  }

  /// The colour that marks a shelter on the left edge of its card.
  static Color shelterAccent(BuildContext context, String status) => shelterStatus(context, status).foreground;

  /// The colour that marks a rescue request on the left edge of its card: its priority while it is still open,
  /// its status once it is finished.
  static Color rescueAccent(BuildContext context, String status, String priority) {
    if (status == 'COMPLETED' || status == 'CANCELLED') {
      return rescueStatus(context, status).foreground;
    }
    return OpsChips.priority(context, priority).foreground;
  }

  static StatusChip _chip(String label, BadgeColors colors) =>
      StatusChip(label: label, icon: Icons.circle, background: colors.background, foreground: colors.foreground);
}
