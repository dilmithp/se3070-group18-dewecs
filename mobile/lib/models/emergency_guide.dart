import 'dart:convert';

/// Priority levels for emergency preparedness kit items.
enum KitPriority {
  critical,
  essential,
  recommended,
}

/// Category classification for emergency supplies and survival kits.
enum KitCategory {
  medical,
  documents,
  sustenance,
  tools,
  communication,
}

/// Represents an emergency kit item packed or required by a citizen.
class EmergencyKitItem {
  const EmergencyKitItem({
    required this.id,
    required this.title,
    required this.description,
    required this.category,
    required this.priority,
    this.isPacked = false,
    this.recommendedQuantity = '1 unit',
  });

  final String id;
  final String title;
  final String description;
  final KitCategory category;
  final KitPriority priority;
  final bool isPacked;
  final String recommendedQuantity;

  EmergencyKitItem copyWith({
    String? id,
    String? title,
    String? description,
    KitCategory? category,
    KitPriority? priority,
    bool? isPacked,
    String? recommendedQuantity,
  }) {
    return EmergencyKitItem(
      id: id ?? this.id,
      title: title ?? this.title,
      description: description ?? this.description,
      category: category ?? this.category,
      priority: priority ?? this.priority,
      isPacked: isPacked ?? this.isPacked,
      recommendedQuantity: recommendedQuantity ?? this.recommendedQuantity,
    );
  }

  factory EmergencyKitItem.fromJson(Map<String, dynamic> json) {
    return EmergencyKitItem(
      id: json['id'] as String,
      title: json['title'] as String,
      description: json['description'] as String,
      category: KitCategory.values.firstWhere(
        (c) => c.name == json['category'],
        orElse: () => KitCategory.tools,
      ),
      priority: KitPriority.values.firstWhere(
        (p) => p.name == json['priority'],
        orElse: () => KitPriority.recommended,
      ),
      isPacked: json['isPacked'] as bool? ?? false,
      recommendedQuantity: json['recommendedQuantity'] as String? ?? '1 unit',
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'title': title,
        'description': description,
        'category': category.name,
        'priority': priority.name,
        'isPacked': isPacked,
        'recommendedQuantity': recommendedQuantity,
      };

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is EmergencyKitItem &&
          runtimeType == other.runtimeType &&
          id == other.id &&
          title == other.title &&
          category == other.category &&
          priority == other.priority &&
          isPacked == other.isPacked;

  @override
  int get hashCode => Object.hash(id, title, category, priority, isPacked);
}

/// Official emergency guideline or standard protocol for disasters in Sri Lanka.
class SafetyGuideline {
  const SafetyGuideline({
    required this.id,
    required this.hazardType,
    required this.phase,
    required this.headline,
    required this.actionSteps,
    required this.dos,
    required this.donts,
  });

  final String id;
  final String hazardType; // e.g. 'Flood', 'Landslide', 'Cyclone', 'Tsunami'
  final String phase; // e.g. 'Pre-Disaster', 'During Disaster', 'Post-Disaster'
  final String headline;
  final List<String> actionSteps;
  final List<String> dos;
  final List<String> donts;

  factory SafetyGuideline.fromJson(Map<String, dynamic> json) {
    return SafetyGuideline(
      id: json['id'] as String,
      hazardType: json['hazardType'] as String,
      phase: json['phase'] as String,
      headline: json['headline'] as String,
      actionSteps: List<String>.from(json['actionSteps'] as List),
      dos: List<String>.from(json['dos'] as List),
      donts: List<String>.from(json['donts'] as List),
    );
  }

  Map<String, dynamic> toJson() => {
        'id': id,
        'hazardType': hazardType,
        'phase': phase,
        'headline': headline,
        'actionSteps': actionSteps,
        'dos': dos,
        'donts': donts,
      };
}
