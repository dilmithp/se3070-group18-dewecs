import 'package:flutter/material.dart';

import '../config/theme.dart';
import '../models/rescue_request.dart';
import '../strings.dart';
import 'ops_chips.dart';
import 'report_card.dart';

/// One row in the rescue request list: the priority colour on the left edge while the request is open, the status
/// colour once it is finished.
class RescueCard extends StatelessWidget {
  const RescueCard({super.key, required this.request, required this.onTap});

  final RescueRequest request;
  final VoidCallback onTap;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final muted = theme.textTheme.bodySmall?.copyWith(color: theme.colorScheme.onSurfaceVariant);
    final submitted = request.submittedAt;
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
                Container(width: 5, color: OpsChips.rescueAccent(context, request.status, request.priority)),
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
                              backgroundColor: theme.colorScheme.errorContainer,
                              child: Icon(Icons.support_outlined, size: 20, color: AppColors.rescue),
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
                                  Text(request.requesterName, style: theme.textTheme.titleMedium),
                                  OpsChips.rescueStatus(context, request.status),
                                ],
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 10),
                        Text(request.description, maxLines: 2, overflow: TextOverflow.ellipsis),
                        const SizedBox(height: 10),
                        Wrap(
                          spacing: 8,
                          runSpacing: 6,
                          crossAxisAlignment: WrapCrossAlignment.center,
                          children: [
                            OpsChips.priority(context, request.priority),
                            Text(
                              request.assignedTeamName == null
                                  ? S.noTeamYet
                                  : S.teamAssigned(request.assignedTeamName!),
                              style: muted,
                            ),
                          ],
                        ),
                        const SizedBox(height: 10),
                        Row(
                          children: [
                            Icon(Icons.place_outlined, size: 16, color: theme.colorScheme.onSurfaceVariant),
                            const SizedBox(width: 4),
                            Flexible(
                              child: Text(
                                submitted == null
                                    ? request.districtName
                                    : '${request.districtName} - ${formatDisplayTime(submitted)}',
                                style: muted,
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
