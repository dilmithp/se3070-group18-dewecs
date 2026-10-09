import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/shelter.dart';
import '../state/shelters_controller.dart';
import '../strings.dart';
import '../widgets/filter_bar.dart';
import '../widgets/shelter_card.dart';
import '../widgets/state_views.dart';
import 'shelter_detail_screen.dart';
import 'shelter_form_screen.dart';

/// Shelters: filter by status and district, see how full each one is, open one for its detail.
class ShelterListScreen extends StatefulWidget {
  const ShelterListScreen({super.key, this.onOpen});

  /// Called when a shelter card is tapped. By default the detail screen opens, and the list is fetched again when
  /// the user comes back, because check-ins and closing change what the cards show.
  final void Function(Shelter shelter)? onOpen;

  @override
  State<ShelterListScreen> createState() => _ShelterListScreenState();
}

class _ShelterListScreenState extends State<ShelterListScreen> {
  @override
  void initState() {
    super.initState();
    // After the first frame: refresh notifies listeners, which is not allowed during build.
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        context.read<SheltersController>().refresh();
      }
    });
  }

  Future<void> _open(Shelter shelter) async {
    final onOpen = widget.onOpen;
    if (onOpen != null) {
      onOpen(shelter);
      return;
    }
    final controller = context.read<SheltersController>();
    await Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => ShelterDetailScreen(shelter: shelter)));
    if (mounted) {
      controller.refresh();
    }
  }

  Future<void> _create() async {
    final controller = context.read<SheltersController>();
    await Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => const ShelterFormScreen()));
    if (mounted) {
      controller.refresh();
    }
  }

  @override
  Widget build(BuildContext context) {
    final controller = context.watch<SheltersController>();
    final theme = Theme.of(context);
    final shelters = controller.shelters;
    final error = controller.error;

    final Widget content;
    if (!controller.loaded && controller.loading) {
      content = const Center(child: CircularProgressIndicator());
    } else if (!controller.loaded && error != null) {
      content = StateMessage(
        icon: error.isConnectionDown ? Icons.cloud_off : Icons.error_outline,
        title: error.isConnectionDown ? S.noConnection : S.sheltersLoadFailed,
        body: error.detail,
        actionLabel: S.retry,
        onAction: controller.refresh,
      );
    } else if (shelters.isEmpty) {
      content = StateMessage(
        icon: Icons.night_shelter_outlined,
        title: controller.hasFilter ? S.sheltersEmptyFilteredTitle : S.sheltersEmptyTitle,
        body: controller.hasFilter ? S.sheltersEmptyFilteredBody : S.sheltersEmptyBody,
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
              text: error.isConnectionDown ? S.sheltersShowingLast : S.refreshFailed(error.detail),
              icon: error.isConnectionDown ? Icons.cloud_off : Icons.error_outline,
              color: error.isConnectionDown ? theme.colorScheme.tertiaryContainer : theme.colorScheme.errorContainer,
            ),
          Padding(
            padding: const EdgeInsets.fromLTRB(20, 14, 20, 2),
            child: Text(
              S.sheltersCount(shelters.length),
              style: theme.textTheme.labelLarge?.copyWith(color: theme.colorScheme.onSurfaceVariant),
            ),
          ),
          for (final shelter in shelters) ShelterCard(shelter: shelter, onTap: () => _open(shelter)),
        ],
      );
    }

    final column = Column(
      children: [
        if (controller.loaded || controller.districts.isNotEmpty)
          FilterBar(
            statuses: controller.statuses,
            statusLabel: S.shelterStatusLabel,
            selectedStatus: controller.statusFilter,
            districts: controller.districts,
            selectedDistrictId: controller.districtFilter,
            onChanged: (status, districtId) => controller.setFilters(status: status, districtId: districtId),
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
        // Both lists live in the tab stack at once, so each button needs its own hero tag.
        heroTag: 'new-shelter',
        onPressed: _create,
        icon: const Icon(Icons.add),
        label: const Text(S.newShelterButton),
      ),
    );
  }
}
