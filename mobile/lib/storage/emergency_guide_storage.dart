import 'dart:convert';
import '../models/emergency_guide.dart';
import 'key_value_store.dart';

/// Storage and service layer for managing emergency disaster guides
/// and citizen preparedness kit checklists.
class EmergencyGuideStorage {
  EmergencyGuideStorage(this._store);

  final KeyValueStore _store;
  static const String _packedItemsKey = 'dewecs_emergency_packed_item_ids';

  /// Returns the current checklist with persisted packed statuses applied.
  Future<List<EmergencyKitItem>> loadKitItems() async {
    final rawPackedIds = _store.getString(_packedItemsKey);
    final Set<String> packedIds = {};

    if (rawPackedIds != null && rawPackedIds.isNotEmpty) {
      try {
        final List<dynamic> decoded = jsonDecode(rawPackedIds) as List<dynamic>;
        packedIds.addAll(decoded.map((e) => e.toString()));
      } catch (_) {
        // Fallback to empty if corrupted
      }
    }

    final defaultItems = getDefaultKitItems();
    return defaultItems.map((item) {
      return item.copyWith(isPacked: packedIds.contains(item.id));
    }).toList();
  }

  /// Toggles the packed state of a specific emergency kit item.
  Future<List<EmergencyKitItem>> toggleItemPacked(String itemId) async {
    final currentItems = await loadKitItems();
    final updatedItems = currentItems.map((item) {
      if (item.id == itemId) {
        return item.copyWith(isPacked: !item.isPacked);
      }
      return item;
    }).toList();

    final packedIds = updatedItems
        .where((item) => item.isPacked)
        .map((item) => item.id)
        .toList();

    await _store.setString(_packedItemsKey, jsonEncode(packedIds));
    return updatedItems;
  }

  /// Resets all checklist items to unpacked state.
  Future<void> resetChecklist() async {
    await _store.remove(_packedItemsKey);
  }

  /// Calculates percentage (0.0 to 1.0) of packed items.
  static double calculateProgress(List<EmergencyKitItem> items) {
    if (items.isEmpty) return 0.0;
    final packedCount = items.where((item) => item.isPacked).length;
    return packedCount / items.length;
  }

  /// Pre-populated disaster safety guidelines for Sri Lanka.
  static List<SafetyGuideline> getDefaultGuidelines() {
    return const [
      SafetyGuideline(
        id: 'guide_flood',
        hazardType: 'Flood',
        phase: 'During Disaster',
        headline: 'Immediate Protocol for Flash Floods and Rising Water',
        actionSteps: [
          'Move immediately to designated higher ground or multi-story shelter centers.',
          'Switch off main electricity breaker and gas cylinders before evacuating.',
          'Do not walk, swim, or drive through moving water currents.',
          'Keep your phone battery conserved and listen to DMC alerts on battery radio.',
        ],
        dos: [
          'Drink only boiled or bottled water to prevent waterborne diseases.',
          'Keep personal documents in a sealed waterproof pouch.',
          'Signal rescue teams using a flashlight or bright cloth.',
        ],
        donts: [
          'Do not touch fallen electric wires or submerged power poles.',
          'Do not consume food items that came into contact with floodwaters.',
          'Do not return home until authorized by local Grama Niladhari or DMC.',
        ],
      ),
      SafetyGuideline(
        id: 'guide_landslide',
        hazardType: 'Landslide',
        phase: 'Pre-Disaster',
        headline: 'National Building Research Organisation (NBRO) Warning Protocol',
        actionSteps: [
          'Watch for sudden appearance of cracks on slopes, roads, or house floors.',
          'Listen for unusual rumbling sounds, cracking trees, or sudden mudflow in streams.',
          'Evacuate immediately upon Level 2 (Amber) or Level 3 (Red) NBRO alert notifications.',
        ],
        dos: [
          'Inform neighbors and elderly residents immediately upon spotting land subsidence.',
          'Follow established pre-mapped evacuation routes away from steep slopes.',
          'Contact the DMC 117 hotline or local police station if escape routes are blocked.',
        ],
        donts: [
          'Do not stay in lower floors or rooms adjacent to mountain-facing slopes.',
          'Do not attempt to cross swollen mountain streams during active mudflows.',
          'Do not delay evacuation to salvage heavy household belongings.',
        ],
      ),
      SafetyGuideline(
        id: 'guide_cyclone',
        hazardType: 'Cyclone / High Winds',
        phase: 'Preparedness',
        headline: 'Precautions for Cyclonic Storms and Severe Gales',
        actionSteps: [
          'Trim dead or overhanging tree branches close to houses and roofs.',
          'Secure loose roof tiles, zinc sheets, and outdoor water storage tanks.',
          'Prepare emergency kit with candles, torches, power banks, and non-perishable food.',
        ],
        dos: [
          'Stay indoors within the most structurally sound room away from glass windows.',
          'Keep emergency contact numbers handy (DMC 117, Police 119, Suwa Seriya 1990).',
          'Disconnect sensitive electrical appliances before wind peaks.',
        ],
        donts: [
          'Do not venture outside during the temporary calm of the cyclone eye.',
          'Do not take shelter under trees, tin sheds, or large billboards.',
          'Do not spread unverified rumors on social media without official DMC validation.',
        ],
      ),
    ];
  }

  /// Default emergency bag checklist recommended by disaster management authorities.
  static List<EmergencyKitItem> getDefaultKitItems() {
    return const [
      EmergencyKitItem(
        id: 'kit_first_aid',
        title: 'Emergency First Aid Kit',
        description: 'Antiseptic solution, gauze bandages, plaster, pain relief tablets, and scissors.',
        category: KitCategory.medical,
        priority: KitPriority.critical,
        recommendedQuantity: '1 kit',
      ),
      EmergencyKitItem(
        id: 'kit_water_tablets',
        title: 'Water Purification Tablets',
        description: 'Chlorine/Halazone tablets or portable water filter for safe drinking water.',
        category: KitCategory.sustenance,
        priority: KitPriority.critical,
        recommendedQuantity: '1 strip (20 tablets)',
      ),
      EmergencyKitItem(
        id: 'kit_documents_bag',
        title: 'Waterproof Document Pouch',
        description: 'National Identity Card (NIC), passports, birth certificates, and land deeds in zip lock.',
        category: KitCategory.documents,
        priority: KitPriority.critical,
        recommendedQuantity: '1 pouch per family',
      ),
      EmergencyKitItem(
        id: 'kit_flashlight',
        title: 'LED Torch & Spare Batteries',
        description: 'Heavy duty waterproof flashlight or rechargeable headlamp.',
        category: KitCategory.tools,
        priority: KitPriority.essential,
        recommendedQuantity: '2 torches',
      ),
      EmergencyKitItem(
        id: 'kit_whistle',
        title: 'Rescue Whistle',
        description: 'Loud pea-less signaling whistle for calling for help when trapped.',
        category: KitCategory.tools,
        priority: KitPriority.essential,
        recommendedQuantity: '1 per person',
      ),
      EmergencyKitItem(
        id: 'kit_dry_rations',
        title: 'Dry Food Rations (Ready to Eat)',
        description: 'High energy biscuits, Samaposha, canned fish, and glucose.',
        category: KitCategory.sustenance,
        priority: KitPriority.essential,
        recommendedQuantity: '3-day supply',
      ),
      EmergencyKitItem(
        id: 'kit_powerbank',
        title: 'Charged Power Bank & Cable',
        description: 'Portable phone charger for maintaining communication during power outages.',
        category: KitCategory.communication,
        priority: KitPriority.recommended,
        recommendedQuantity: '10,000+ mAh',
      ),
      EmergencyKitItem(
        id: 'kit_radio',
        title: 'Portable AM/FM Battery Radio',
        description: 'Receives National Disaster Management Radio broadcasts when mobile towers fail.',
        category: KitCategory.communication,
        priority: KitPriority.recommended,
        recommendedQuantity: '1 unit',
      ),
    ];
  }
}
