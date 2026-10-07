import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../models/report_item.dart';
import '../strings.dart';
import 'category_chips.dart';
import 'status_chip.dart';

String formatDisplayTime(DateTime t) => DateFormat('d MMM y, HH:mm').format(t);

/// One row in My reports.
class ReportCard extends StatelessWidget {
  const ReportCard({super.key, required this.item, required this.onTap});

  final ReportItem item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
      child: InkWell(
        onTap: onTap,
        borderRadius: BorderRadius.circular(12),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                children: [
                  Icon(categoryIcon(item.category), color: theme.colorScheme.primary),
                  const SizedBox(width: 8),
                  Expanded(child: Text(S.categoryName(item.category), style: theme.textTheme.titleMedium)),
                  Flexible(child: StatusChip.forItem(context, item)),
                ],
              ),
              const SizedBox(height: 8),
              Text(item.description, maxLines: 2, overflow: TextOverflow.ellipsis),
              const SizedBox(height: 8),
              Text(
                '${item.districtName} - ${formatDisplayTime(item.submittedAt)}',
                style: theme.textTheme.bodySmall,
              ),
              if (item.isUnsent && item.error != null) ...[
                const SizedBox(height: 4),
                Text(item.error!, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
              ],
              if (item.photoProblem != null) ...[
                const SizedBox(height: 4),
                Text(S.photoNotSent, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.error)),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
