import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/rescue_request.dart';
import '../state/rescue_requests_controller.dart';
import '../strings.dart';
import '../widgets/filter_bar.dart';
import '../widgets/rescue_card.dart';
import '../widgets/state_views.dart';
import 'rescue_detail_screen.dart';
import 'rescue_form_screen.dart';

/// Rescue requests: filter by status, priority and district, see who is waiting for a team, open one for its detail.
class RescueListScreen extends StatefulWidget {
  const RescueListScreen({super.key, this.onOpen});

  /// Called when a request card is tapped. By default the detail screen opens, and the list is fetched again when
  /// the user comes back, because assigning, completing or cancelling changes what the cards show.
  final void Function(RescueRequest request)? onOpen;

  @override
  State<RescueListScreen> createState() => _RescueListScreenState();
}

class _RescueListScreenState extends State<RescueListScreen> {
  @override
  void initState() {
    super.initState();
    // After the first frame: refresh notifies listeners, which is not allowed during build.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        context.read<RescueRequestsController>().refresh();
      }
    });
  }

  Future<void> _open(RescueRequest request) async {
    final onOpen = widget.onOpen;
    if (onOpen != null) {
      onOpen(request);
      return;
    }
    final controller = context.read<RescueRequestsController>();
    await Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => RescueDetailScreen(request: request)));
    if (mounted) {
      controller.refresh();
    }
  }

  Future<void> _create() async {
    final controller = context.read<RescueRequestsController>();
    await Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => const RescueFormScreen()));
    if (mounted) {
      controller.refresh();
    }
  }

  @override
  Widget build(BuildContext context) {
    final controller = context.watch<RescueRequestsController>();
    final theme = Theme.of(context);
    final requests = controller.requests;
    final error = controller.error;

    final Widget content;
    if (!controller.loaded && controller.loading) {
      content = const Center(child: CircularProgressIndicator());
    } else if (!controller.loaded && error != null) {
      content = StateMessage(
        icon: error.isConnectionDown ? Icons.cloud_off : Icons.error_outline,
        title: error.isConnectionDown ? S.noConnection : S.rescueLoadFailed,
        body: error.detail,
        actionLabel: S.retry,
        onAction: controller.refresh,
      );
    } else if (requests.isEmpty) {
      content = StateMessage(
        icon: Icons.support_outlined,
        title: controller.hasFilter ? S.rescueEmptyFilteredTitle : S.rescueEmptyTitle,
        body: controller.hasFilter ? S.rescueEmptyFilteredBody : S.rescueEmptyBody,
        actionLabel: controller.hasFilter ? S.clearFilters : null,
        onAction: controller.hasFilter ? () => controller.setFilters() : null,
      );
    } else {
      content = ListView(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.only(bottom: 88),
        children: [
          if (error != null)
            InfoBanner(
              text: error.isConnectionDown ? S.rescueShowingLast : S.refreshFailed(error.detail),
              icon: error.isConnectionDown ? Icons.cloud_off : Icons.error_outline,
              color: error.isConnectionDown ? theme.colorScheme.tertiaryContainer : theme.colorScheme.errorContainer,
            ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 14, 20, 2),
            child: Text(
              S.rescueCount(requests.length),
              style: theme.textTheme.labelLarge?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ),
          for (final request in requests) RescueCard(request: request, onTap: () => _open(request)),
        ],
      );
    }

    final column = Column(
      children: [
        if (controller.loaded || controller.districts.isNotEmpty)
          FilterBar(
            statuses: controller.statuses,
            statusLabel: S.rescueStatusLabel,
            selectedStatus: controller.statusFilter,
            priorities: controller.priorities,
            priorityLabel: S.priorityLabel,
            selectedPriority: controller.priorityFilter,
            onPriorityChanged: (priority) => controller.setFilters(
              status: controller.statusFilter,
              priority: priority,
              districtId: controller.districtFilter,
            ),
            districts: controller.districts,
            selectedDistrictId: controller.districtFilter,
            onChanged: (status, districtId) =>
                controller.setFilters(status: status, priority: controller.priorityFilter, districtId: districtId),
          ),
        if (controller.loading && controller.loaded) const LinearProgressIndicator(minHeight: 2),
        const Divider(height: 1),
        Expanded(
          child: RefreshIndicator(
            onRefresh: controller.refresh,
            // Every state is scrollable, so pull-to-refresh also works on the empty and error views.
            child: LayoutBuilder(
              builder: (context, constraints) => content is ListView
                  ? content
                  : SingleChildScrollView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      child: SizedBox(height: constraints.maxHeight, child: content),
                    ),
            ),
          ),
        ),
      ],
    );
    return Scaffold(
      backgroundColor: Colors.transparent,
      body: column,
      floatingActionButton: FloatingActionButton.extended(
        onPressed: _create,
        icon: const Icon(Icons.add),
        label: const Text(S.newRescueButton),
      ),
    );
  }
}
