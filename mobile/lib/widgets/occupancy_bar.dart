import 'package:flutter/material.dart';

import '../strings.dart';

/// How full a shelter is: a text line and a bar. The text carries the meaning, the bar colour only reinforces it
/// (the same as the web `progress.occupancy`).
class OccupancyBar extends StatelessWidget {
  const OccupancyBar({super.key, required this.occupancy, required this.capacity, this.showFree = true});

  final int occupancy;
  final int capacity;
  final bool showFree;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final fill = capacity <= 0 ? 0.0 : (occupancy / capacity).clamp(0.0, 1.0);
    final free = (capacity - occupancy).clamp(0, capacity);
    final color = fill >= 1
        ? theme.colorScheme.error
        : (fill >= 0.8 ? theme.colorScheme.tertiary : theme.colorScheme.primary);
    final taken = S.placesTaken(occupancy, capacity);
    final left = free == 0 ? S.noPlacesFree : S.placesFree(free);
    return Semantics(
      label: '$taken. $left',
      excludeSemantics: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          ClipRRect(
            borderRadius: BorderRadius.circular(999),
            child: LinearProgressIndicator(
              value: fill,
              minHeight: 8,
              color: color,
              backgroundColor: theme.colorScheme.surfaceContainerHighest,
            ),
          ),
          const SizedBox(height: 6),
          Wrap(
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            spacing: 12,
            children: [
              Text(taken, style: theme.textTheme.bodySmall),
              if (showFree)
                Text(left, style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant)),
            ],
          ),
        ],
      ),
    );
  }
}
