import 'package:flutter/material.dart';
import 'package:provider/provider.dart';

import '../models/relief_rules.dart';
import '../models/relief_supply.dart';
import '../state/relief_controller.dart';
import '../strings.dart';

class SupplyListScreen extends StatefulWidget {
  const SupplyListScreen({super.key});

  @override
  State<SupplyListScreen> createState() => _SupplyListScreenState();
}

class _SupplyListScreenState extends State<SupplyListScreen> {
  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) {
        context.read<ReliefController>().refresh();
      }
    });
  }

  Future<void> _refresh() async {
    await context.read<ReliefController>().refresh();
  }

  @override
  Widget build(BuildContext context) {
    final relief = context.watch<ReliefController>();
    final supplies = relief.supplies;
    final loading = relief.loading;
    final error = relief.lastError;
    final theme = Theme.of(context);
    final currentType = relief.currentType;

    final chips = SingleChildScrollView(
      scrollDirection: Axis.horizontal,
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
      child: Row(
        children: [
          FilterChip(
            label: const Text(S.reliefAllTypes),
            selected: currentType == null,
            onSelected: (selected) {
              if (selected) relief.setType(null);
            },
          ),
          const SizedBox(width: 8),
          for (final t in ResourceTypes.all)
            Padding(
              padding: const EdgeInsets.only(right: 8),
              child: FilterChip(
                label: Text(S.reliefType(t)),
                selected: currentType == t,
                onSelected: (selected) {
                  relief.setType(selected ? t : null);
                },
              ),
            ),
        ],
      ),
    );

    final Widget content;
    if (supplies == null && loading) {
      content = const Center(child: CircularProgressIndicator());
    } else if (supplies == null && error != null) {
      content = Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.error_outline, size: 48),
              const SizedBox(height: 16),
              Text(S.unexpectedError, style: theme.textTheme.titleLarge, textAlign: TextAlign.center),
              const SizedBox(height: 8),
              Text(error.detail, textAlign: TextAlign.center),
              const SizedBox(height: 24),
              FilledButton(onPressed: _refresh, child: const Text(S.retry)),
            ],
          ),
        ),
      );
    } else if (supplies != null && supplies.isEmpty) {
      content = Center(
        child: Padding(
          padding: const EdgeInsets.all(24),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              const Icon(Icons.inventory_2_outlined, size: 48),
              const SizedBox(height: 16),
              Text(S.supplyListEmptyTitle, style: theme.textTheme.titleLarge, textAlign: TextAlign.center),
              const SizedBox(height: 8),
              const Text(S.supplyListEmptyBody, textAlign: TextAlign.center),
              const SizedBox(height: 24),
              FilledButton(onPressed: _refresh, child: const Text(S.retry)),
            ],
          ),
        ),
      );
    } else if (supplies != null) {
      content = ListView.builder(
        physics: const AlwaysScrollableScrollPhysics(),
        padding: const EdgeInsets.only(bottom: 24),
        itemCount: supplies.length,
        itemBuilder: (context, index) {
          final supply = supplies[index];
          return _SupplyCard(supply: supply);
        },
      );
    } else {
      content = const SizedBox.shrink();
    }

    return Column(
      children: [
        chips,
        const Divider(height: 1),
        Expanded(
          child: RefreshIndicator(
            onRefresh: _refresh,
            child: (supplies == null || supplies.isEmpty) && !loading
                ? LayoutBuilder(
                    builder: (context, constraints) => SingleChildScrollView(
                      physics: const AlwaysScrollableScrollPhysics(),
                      child: SizedBox(height: constraints.maxHeight, child: content),
                    ),
                  )
                : content,
          ),
        ),
      ],
    );
  }
}

class _SupplyCard extends StatelessWidget {
  const _SupplyCard({required this.supply});

  final ReliefSupply supply;

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    final isLow = supply.outOfStock || supply.lowStock;
    final quantityStyle = theme.textTheme.titleMedium?.copyWith(
      fontWeight: FontWeight.w700,
      color: supply.outOfStock
          ? theme.colorScheme.error
          : (supply.lowStock ? theme.colorScheme.tertiary : null),
    );

    return Card(
      margin: const EdgeInsets.symmetric(horizontal: 16, vertical: 6),
      clipBehavior: Clip.antiAlias,
      child: InkWell(
        onTap: () {
          // Supply detail screen goes here (Slice 4)
        },
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(supply.name, style: theme.textTheme.titleMedium),
                        const SizedBox(height: 4),
                        Text(S.reliefType(supply.type), style: theme.textTheme.bodySmall),
                      ],
                    ),
                  ),
                  const SizedBox(width: 12),
                  Text('${supply.quantity} ${supply.unit}', style: quantityStyle),
                ],
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  const Icon(Icons.location_on_outlined, size: 16),
                  const SizedBox(width: 4),
                  Expanded(child: Text(supply.districtName, style: theme.textTheme.bodyMedium)),
                ],
              ),
              const SizedBox(height: 4),
              Row(
                children: [
                  const Icon(Icons.business_outlined, size: 16),
                  const SizedBox(width: 4),
                  Expanded(child: Text(supply.organizationName, style: theme.textTheme.bodyMedium)),
                ],
              ),
              if (isLow) ...[
                const SizedBox(height: 12),
                Row(
                  children: [
                    if (supply.outOfStock)
                      _Badge(text: S.outOfStock, color: theme.colorScheme.errorContainer)
                    else if (supply.lowStock)
                      _Badge(text: S.lowStock, color: theme.colorScheme.tertiaryContainer),
                  ],
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}

class _Badge extends StatelessWidget {
  const _Badge({required this.text, required this.color});
  final String text;
  final Color color;
  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
      decoration: BoxDecoration(color: color, borderRadius: BorderRadius.circular(4)),
      child: Text(text, style: Theme.of(context).textTheme.labelSmall),
    );
  }
}
