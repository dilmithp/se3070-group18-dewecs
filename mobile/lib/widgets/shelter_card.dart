import 'package:flutter/material.dart';

import '../config/theme.dart';
import '../models/shelter.dart';
import 'occupancy_bar.dart';
import 'ops_chips.dart';

/// One row in the shelter list: a white card with a coloured left edge (the status colour), like the report cards.
class ShelterCard extends StatelessWidget {
  const ShelterCard({super.key, required this.shelter, required this.onTap});

  final Shelter shelter;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
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
                Container(width: 5, color: OpsChips.shelterAccent(context, shelter.status)),
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
                              child: Icon(Icons.night_shelter_outlined, size: 20, color: AppColors.shelter),
                            ),
                            const SizedBox(width: 10),
                            // Wrap: the badge sits beside a short name and drops below a long one, so the name
                            // never has to share its width with the badge.
                            Expanded(
                              child: Wrap(
                                spacing: 8,
                                runSpacing: 6,
                                crossAxisAlignment: WrapCrossAlignment.center,
                                children: [
                                  Text(shelter.name, style: theme.textTheme.titleMedium),
                                  OpsChips.shelterStatus(context, shelter.status),
                                ],
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 10),
                        OccupancyBar(occupancy: shelter.currentOccupancy, capacity: shelter.capacity),
                        const SizedBox(height: 10),
                        Row(
                          children: [
                            Icon(Icons.place_outlined, size: 16, color: theme.colorScheme.onSurfaceVariant),
                            const SizedBox(width: 4),
                            Flexible(
                              child: Text(
                                '${shelter.districtName} - ${shelter.organizationName}',
                                style: theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant),
                              ),
                            ),
                          ],
                        ),
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
