import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../models/emergency_guide.dart';
import '../storage/emergency_guide_storage.dart';
import '../storage/key_value_store.dart';
import '../widgets/emergency_guide_card.dart';

/// Full feature screen for disaster preparedness:
/// - Emergency survival kit packing checklist with offline persistence.
/// - Official Sri Lanka DMC safety guidelines and protocols.
class EmergencyGuideScreen extends StatefulWidget {
  const EmergencyGuideScreen({super.key, this.customStore});

  final KeyValueStore? customStore;

  @override
  State<EmergencyGuideScreen> createState() => _EmergencyGuideScreenState();
}

class _EmergencyGuideScreenState extends State<EmergencyGuideScreen> {
  EmergencyGuideStorage? _storage;
  List<EmergencyKitItem> _kitItems = [];
  bool _isLoading = true;
  KitCategory? _selectedCategory;
  String? _selectedHazard;

  @override
  void initState() {
    super.initState();
    _initStorage();
  }

  Future<void> _initStorage() async {
    if (widget.customStore != null) {
      _storage = EmergencyGuideStorage(widget.customStore!);
    } else {
      final prefs = await SharedPreferences.getInstance();
      _storage = EmergencyGuideStorage(SharedPreferencesStore(prefs));
    }
    await _loadItems();
  }

  Future<void> _loadItems() async {
    if (_storage == null) return;
    final items = await _storage!.loadKitItems();
    if (mounted) {
      setState(() {
        _kitItems = items;
        _isLoading = false;
      });
    }
  }

  Future<void> _toggleItem(String itemId) async {
    if (_storage == null) return;
    final updated = await _storage!.toggleItemPacked(itemId);
    if (mounted) {
      setState(() {
        _kitItems = updated;
      });
    }
  }

  Future<void> _resetChecklist() async {
    if (_storage == null) return;
    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Reset Checklist?'),
        content: const Text(
            'This will uncheck all packed supplies in your emergency kit.'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx, false),
            child: const Text('Cancel'),
          ),
          FilledButton(
            onPressed: () => Navigator.pop(ctx, true),
            child: const Text('Reset'),
          ),
        ],
      ),
    );

    if (confirmed == true) {
      await _storage!.resetChecklist();
      await _loadItems();
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);

    return DefaultTabController(
      length: 2,
      child: Scaffold(
        appBar: AppBar(
          title: const Text('Preparedness & Guide'),
          actions: [
            IconButton(
              icon: const Icon(Icons.refresh),
              tooltip: 'Reset Checklist',
              onPressed: _resetChecklist,
            ),
          ],
          bottom: const TabBar(
            tabs: [
              Tab(icon: Icon(Icons.checklist), text: 'Evacuation Kit'),
              Tab(icon: Icon(Icons.shield_outlined), text: 'Safety Rules'),
            ],
          ),
        ),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : TabBarView(
                children: [
                  _buildKitChecklistTab(theme),
                  _buildSafetyProtocolsTab(theme),
                ],
              ),
      ),
    );
  }

  Widget _buildKitChecklistTab(ThemeData theme) {
    final progress = EmergencyGuideStorage.calculateProgress(_kitItems);
    final packedCount = _kitItems.where((i) => i.isPacked).length;

    final filteredItems = _selectedCategory == null
        ? _kitItems
        : _kitItems.where((i) => i.category == _selectedCategory).toList();

    return Column(
      children: [
        // Progress header card
        Container(
          margin: const EdgeInsets.all(16),
          padding: const EdgeInsets.all(16),
          decoration: BoxDecoration(
            color: theme.colorScheme.primaryContainer.withValues(alpha: 0.3),
            borderRadius: BorderRadius.circular(16),
            border: Border.all(color: theme.colorScheme.primary.withValues(alpha: 0.2)),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Row(
                mainAxisAlignment: MainAxisAlignment.spaceBetween,
                children: [
                  Text(
                    'Emergency Bag Readiness',
                    style: theme.textTheme.titleMedium
                        ?.copyWith(fontWeight: FontWeight.bold),
                  ),
                  Text(
                    '${(progress * 100).toInt()}%',
                    style: theme.textTheme.titleMedium?.copyWith(
                      fontWeight: FontWeight.bold,
                      color: theme.colorScheme.primary,
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 8),
              ClipRRect(
                borderRadius: BorderRadius.circular(99),
                child: LinearProgressIndicator(
                  value: progress,
                  minHeight: 8,
                ),
              ),
              const SizedBox(height: 8),
              Text(
                '$packedCount of ${_kitItems.length} essential items packed and ready.',
                style: theme.textTheme.bodySmall,
              ),
            ],
          ),
        ),

        // Category filter chips
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.symmetric(horizontal: 16),
          child: Row(
            children: [
              FilterChip(
                label: const Text('All'),
                selected: _selectedCategory == null,
                onSelected: (_) => setState(() => _selectedCategory = null),
              ),
              const SizedBox(width: 8),
              ...KitCategory.values.map(
                (cat) => Padding(
                  padding: const EdgeInsets.only(right: 8),
                  child: FilterChip(
                    label: Text(cat.name[0].toUpperCase() + cat.name.substring(1)),
                    selected: _selectedCategory == cat,
                    onSelected: (selected) {
                      setState(() {
                        _selectedCategory = selected ? cat : null;
                      });
                    },
                  ),
                ),
              ),
            ],
          ),
        ),
        const SizedBox(height: 8),

        // List of items
        Expanded(
          child: ListView.builder(
            itemCount: filteredItems.length,
            itemBuilder: (ctx, idx) {
              final item = filteredItems[idx];
              return EmergencyKitItemTile(
                item: item,
                onChanged: (_) => _toggleItem(item.id),
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _buildSafetyProtocolsTab(ThemeData theme) {
    final allGuidelines = EmergencyGuideStorage.getDefaultGuidelines();
    final filteredGuidelines = _selectedHazard == null
        ? allGuidelines
        : allGuidelines.where((g) => g.hazardType == _selectedHazard).toList();

    return Column(
      children: [
        // Hazard filter chips
        SingleChildScrollView(
          scrollDirection: Axis.horizontal,
          padding: const EdgeInsets.fromLTRB(16, 12, 16, 8),
          child: Row(
            children: [
              FilterChip(
                label: const Text('All Hazards'),
                selected: _selectedHazard == null,
                onSelected: (_) => setState(() => _selectedHazard = null),
              ),
              const SizedBox(width: 8),
              ...allGuidelines.map((g) => g.hazardType).toSet().map(
                    (hazard) => Padding(
                      padding: const EdgeInsets.only(right: 8),
                      child: FilterChip(
                        label: Text(hazard),
                        selected: _selectedHazard == hazard,
                        onSelected: (selected) {
                          setState(() {
                            _selectedHazard = selected ? hazard : null;
                          });
                        },
                      ),
                    ),
                  ),
            ],
          ),
        ),

        // Guidelines list
        Expanded(
          child: ListView.builder(
            itemCount: filteredGuidelines.length,
            itemBuilder: (ctx, idx) {
              return SafetyGuidelineCard(guideline: filteredGuidelines[idx]);
            },
          ),
        ),

        // Quick Emergency Numbers banner
        Container(
          margin: const EdgeInsets.all(16),
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          decoration: BoxDecoration(
            color: theme.colorScheme.surfaceContainerHighest,
            borderRadius: BorderRadius.circular(12),
          ),
          child: Row(
            mainAxisAlignment: MainAxisAlignment.spaceAround,
            children: [
              _buildHotlineButton('DMC Hotline', '117', Icons.phone_in_talk, Colors.red),
              _buildHotlineButton('Police', '119', Icons.local_police, Colors.blue),
              _buildHotlineButton('Ambulance', '1990', Icons.medical_services, Colors.green),
            ],
          ),
        ),
      ],
    );
  }

  Widget _buildHotlineButton(String label, String number, IconData icon, Color color) {
    return Column(
      mainAxisSize: MainAxisSize.min,
      children: [
        CircleAvatar(
          radius: 18,
          backgroundColor: color.withValues(alpha: 0.15),
          child: Icon(icon, color: color, size: 18),
        ),
        const SizedBox(height: 4),
        Text(label, style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w500)),
        Text(number, style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: color)),
      ],
    );
  }
}
