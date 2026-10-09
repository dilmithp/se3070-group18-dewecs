import 'package:flutter/material.dart';
import 'package:intl/intl.dart';

import '../models/report_item.dart';
import '../strings.dart';
import 'category_chips.dart';
import 'status_chip.dart';

String formatDisplayTime(DateTime t) => DateFormat('d MMM y, HH:mm').format(t);

/// One row in My reports: a white card with a coloured left edge (the status colour), like the web dashboard tiles.
class ReportCard extends StatelessWidget {
  const ReportCard({super.key, required this.item, required this.onTap});

  final ReportItem item;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final accent = StatusChip.accentFor(context, item);
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      child: Card(
        clipBehavior: Clip.antiAlias,
        child: InkWell(
          onTap: onTap,
          child: IntrinsicHeight(
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Container(width: 5, color: accent),
                Expanded(
                  child: Padding(
                    padding: const EdgeInsets.all(14),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          children: [
                            CircleAvatar(
                              radius: 18,
                              backgroundColor: theme.colorScheme.primaryContainer,
                              child: Icon(categoryIcon(item.category), size: 20, color: theme.colorScheme.onPrimaryContainer),
                            ),
                            const SizedBox(width: 10),
                            Expanded(child: Text(S.categoryName(item.category), style: theme.textTheme.titleMedium)),
                            Flexible(child: StatusChip.forItem(context, item)),
                          ],
                        ),
                        const SizedBox(height: 10),
                        Text(item.description, maxLines: 2, overflow: TextOverflow.ellipsis),
                        const SizedBox(height: 10),
                        Row(
                          children: [
                            Icon(Icons.place_outlined, size: 16, color: theme.colorScheme.onSurfaceVariant),
                            const SizedBox(width: 4),
                            Flexible(
                              child: Text(
                                '${item.districtName} - ${formatDisplayTime(item.submittedAt)}',
                                style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                              ),
                            ),
                          ],
                        ),
                        if (item.isUnsent && item.error != null) ...[
                          const SizedBox(height: 6),
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
              ],
            ),
          ),
        ),
      ),
    );
  }
}
