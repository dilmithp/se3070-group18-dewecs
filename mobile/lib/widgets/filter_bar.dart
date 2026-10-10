import 'package:flutter/material.dart';

import '../models/district.dart';
import '../strings.dart';

/// The filters above a list: status chips you can scroll sideways, and a district drop-down. The same two controls
/// as the web `.filter-bar`. [statusLabel] turns a raw status into the words shown on its chip.
class FilterBar extends StatelessWidget {
  const FilterBar({
    super.key,
    required this.statuses,
    required this.statusLabel,
    required this.selectedStatus,
    required this.districts,
    required this.selectedDistrictId,
    required this.onChanged,
    this.priorities = const [],
    this.priorityLabel,
    this.selectedPriority,
    this.onPriorityChanged,
  });

  final List<String> statuses;
  final String Function(String) statusLabel;
  final String? selectedStatus;
  final List<District> districts;
  final int? selectedDistrictId;

  /// Called with the new status and district whenever either one changes.
  final void Function(String? status, int? districtId) onChanged;

  /// Optional third filter (rescue requests): shown as a second row of chips.
  final List<String> priorities;
  final String Function(String)? priorityLabel;
  final String? selectedPriority;
  final ValueChanged<String?>? onPriorityChanged;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Container(
      color: theme.colorScheme.surface,
      padding: const EdgeInsets.fromLTRB(16, 10, 16, 12),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          _ChipRow(
            label: S.filterStatus,
            values: statuses,
            labelOf: statusLabel,
            selected: selectedStatus,
            onSelected: (value) => onChanged(value, selectedDistrictId),
          ),
          if (priorities.isNotEmpty && priorityLabel != null && onPriorityChanged != null) ...[
            const SizedBox(height: 8),
            _ChipRow(
              label: S.filterPriority,
              values: priorities,
              labelOf: priorityLabel!,
              selected: selectedPriority,
              onSelected: onPriorityChanged!,
            ),
          ],
          if (districts.isNotEmpty) ...[
            const SizedBox(height: 10),
            DropdownButtonFormField<int?>(
              key: ValueKey('district-$selectedDistrictId'),
              initialValue: selectedDistrictId,
              isExpanded: true,
              decoration: const InputDecoration(
                labelText: S.filterDistrict,
                isDense: true,
                prefixIcon: Icon(Icons.place_outlined),
              ),
              items: [
                const DropdownMenuItem<int?>(value: null, child: Text(S.filterAllDistricts)),
                for (final d in districts) DropdownMenuItem<int?>(value: d.id, child: Text(d.name)),
              ],
              onChanged: (value) => onChanged(selectedStatus, value),
            ),
          ],
        ],
      ),
    );
  }
}

class _ChipRow extends StatelessWidget {
  const _ChipRow({
    required this.label,
    required this.values,
    required this.labelOf,
    required this.selected,
    required this.onSelected,
  });

  final String label;
  final List<String> values;
  final String Function(String) labelOf;
  final String? selected;
  final ValueChanged<String?> onSelected;

  @override
  Widget build(BuildContext context) {
    return Semantics(
      container: true,
      label: label,
      child: SingleChildScrollView(
        scrollDirection: Axis.horizontal,
        child: Row(
          children: [
            ChoiceChip(label: const Text(S.filterAll), selected: selected == null, onSelected: (_) => onSelected(null)),
            for (final value in values) ...[
              const SizedBox(width: 8),
              ChoiceChip(
                label: Text(labelOf(value)),
                selected: selected == value,
                onSelected: (_) => onSelected(selected == value ? null : value),
              ),
            ],
          ],
        ),
      ),
    );
  }
}
