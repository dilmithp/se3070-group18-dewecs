import 'package:flutter/material.dart';
import '../config/theme.dart';
import '../models/emergency_guide.dart';

/// Interactive tile displaying an emergency kit item with checkbox and priority indicator.
class EmergencyKitItemTile extends StatelessWidget {
  const EmergencyKitItemTile({
    super.key,
    required this.item,
    required this.onChanged,
  });

  final EmergencyKitItem item;
  final ValueChanged<bool> onChanged;

  IconData _categoryIcon(KitCategory category) {
    switch (category) {
      case KitCategory.medical:
        return Icons.medical_services_outlined;
      case KitCategory.documents:
        return Icons.folder_zip_outlined;
      case KitCategory.sustenance:
        return Icons.water_drop_outlined;
      case KitCategory.tools:
        return Icons.build_circle_outlined;
      case KitCategory.communication:
        return Icons.cell_tower_outlined;
    }
  }

  BadgeColors _priorityColors(BuildContext context, KitPriority priority) {
    final brightness = Theme.of(context).brightness;
    switch (priority) {
      case KitPriority.critical:
        return BadgeColors.bad(brightness);
      case KitPriority.essential:
        return BadgeColors.attention(brightness);
      case KitPriority.recommended:
        return BadgeColors.info(brightness);
    }
  }

  String _priorityLabel(KitPriority priority) {
    switch (priority) {
      case KitPriority.critical:
        return 'Critical';
      case KitPriority.essential:
        return 'Essential';
      case KitPriority.recommended:
        return 'Recommended';
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final badge = _priorityColors(context, item.priority);

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      elevation: item.isPacked ? 0 : 1,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(
          color: item.isPacked
              ? theme.colorScheme.outlineVariant.withValues(alpha: 0.5)
              : theme.colorScheme.outlineVariant,
        ),
      ),
      child: InkWell(
        borderRadius: BorderRadius.circular(12),
        onTap: () => onChanged(!item.isPacked),
        child: Padding(
          padding: const EdgeInsets.all(12),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Checkbox(
                value: item.isPacked,
                onChanged: (val) {
                  if (val != null) onChanged(val);
                },
                shape: RoundedRectangleBorder(
                  borderRadius: BorderRadius.circular(4),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Icon(
                          _categoryIcon(item.category),
                          size: 16,
                          color: theme.colorScheme.primary,
                        ),
                        const SizedBox(width: 6),
                        Expanded(
                          child: Text(
                            item.title,
                            style: theme.textTheme.titleMedium?.copyWith(
                              decoration: item.isPacked
                                  ? TextDecoration.lineThrough
                                  : null,
                              color: item.isPacked
                                  ? theme.textTheme.bodyMedium?.color
                                      ?.withValues(alpha: 0.6)
                                  : null,
                              fontWeight: FontWeight.w600,
                            ),
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(
                              horizontal: 8, vertical: 2),
                          decoration: BoxDecoration(
                            color: badge.background,
                            borderRadius: BorderRadius.circular(999),
                          ),
                          child: Text(
                            _priorityLabel(item.priority),
                            style: TextStyle(
                              color: badge.foreground,
                              fontSize: 11,
                              fontWeight: FontWeight.w700,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 6),
                    Text(
                      item.description,
                      style: theme.textTheme.bodyMedium?.copyWith(
                        color: theme.textTheme.bodyMedium?.color
                            ?.withValues(alpha: 0.8),
                      ),
                    ),
                    const SizedBox(height: 6),
                    Text(
                      'Qty: ${item.recommendedQuantity}',
                      style: theme.textTheme.bodySmall?.copyWith(
                        fontStyle: FontStyle.italic,
                        color: theme.colorScheme.secondary,
                      ),
                    ),
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}

/// Expandable card presenting safety protocol guidelines with action steps and dos/don'ts.
class SafetyGuidelineCard extends StatefulWidget {
  const SafetyGuidelineCard({
    super.key,
    required this.guideline,
  });

  final SafetyGuideline guideline;

  @override
  State<SafetyGuidelineCard> createState() => _SafetyGuidelineCardState();
}

class _SafetyGuidelineCardState extends State<SafetyGuidelineCard> {
  bool _expanded = false;

  IconData _hazardIcon(String hazard) {
    if (hazard.toLowerCase().contains('flood')) {
      return Icons.water_damage_outlined;
    } else if (hazard.toLowerCase().contains('landslide')) {
      return Icons.landscape_outlined;
    } else if (hazard.toLowerCase().contains('cyclone')) {
      return Icons.air_outlined;
    }
    return Icons.warning_amber_rounded;
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final g = widget.guideline;

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      child: Padding(
        padding: const EdgeInsets.all(16),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                CircleAvatar(
                  backgroundColor:
                      theme.colorScheme.primaryContainer.withValues(alpha: 0.5),
                  child: Icon(
                    _hazardIcon(g.hazardType),
                    color: theme.colorScheme.primary,
                  ),
                ),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        g.hazardType,
                        style: theme.textTheme.titleMedium
                            ?.copyWith(fontWeight: FontWeight.bold),
                      ),
                      Text(
                        'Phase: ${g.phase}',
                        style: theme.textTheme.bodySmall?.copyWith(
                          color: theme.colorScheme.secondary,
                        ),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  icon: Icon(_expanded
                      ? Icons.keyboard_arrow_up
                      : Icons.keyboard_arrow_down),
                  onPressed: () => setState(() => _expanded = !_expanded),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Text(
              g.headline,
              style: theme.textTheme.bodyLarge?.copyWith(
                fontWeight: FontWeight.w600,
              ),
            ),
            if (_expanded) ...[
              const Divider(height: 24),
              Text(
                'Key Action Steps',
                style: theme.textTheme.titleSmall
                    ?.copyWith(fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              ...g.actionSteps.map(
                (step) => Padding(
                  padding: const EdgeInsets.symmetric(vertical: 3),
                  child: Row(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text('• ',
                          style: TextStyle(fontWeight: FontWeight.bold)),
                      Expanded(
                          child:
                              Text(step, style: theme.textTheme.bodyMedium)),
                    ],
                  ),
                ),
              ),
              const SizedBox(height: 14),
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: Colors.green.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Row(
                            children: [
                              Icon(Icons.check_circle,
                                  color: Colors.green, size: 16),
                              SizedBox(width: 4),
                              Text('Do',
                                  style: TextStyle(
                                      color: Colors.green,
                                      fontWeight: FontWeight.bold)),
                            ],
                          ),
                          const SizedBox(height: 6),
                          ...g.dos.map((item) => Text('• $item',
                              style: const TextStyle(fontSize: 12))),
                        ],
                      ),
                    ),
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: Colors.red.withValues(alpha: 0.08),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Row(
                            children: [
                              Icon(Icons.cancel, color: Colors.red, size: 16),
                              SizedBox(width: 4),
                              Text("Don't",
                                  style: TextStyle(
                                      color: Colors.red,
                                      fontWeight: FontWeight.bold)),
                            ],
                          ),
                          const SizedBox(height: 6),
                          ...g.donts.map((item) => Text('• $item',
                              style: const TextStyle(fontSize: 12))),
                        ],
                      ),
                    ),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}
