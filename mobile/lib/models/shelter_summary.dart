/// A shelter as listed by GET /shelters, reduced to what the relief screens need (the pick-list for a distribution).
/// Shelters themselves belong to UC-03; the app only reads them here.
class ShelterSummary {
  const ShelterSummary({
    required this.id,
    required this.name,
    required this.districtName,
    required this.capacity,
    required this.currentOccupancy,
    required this.status,
  });

  final int id;
  final String name;
  final String districtName;
  final int capacity;
  final int currentOccupancy;
  final String status;

  factory ShelterSummary.fromJson(Map<String, dynamic> json) => ShelterSummary(
    id: (json['id'] as num).toInt(),
    name: json['name'] as String,
    districtName: json['districtName'] as String? ?? '',
    capacity: (json['capacity'] as num?)?.toInt() ?? 0,
    currentOccupancy: (json['currentOccupancy'] as num?)?.toInt() ?? 0,
    status: json['status'] as String? ?? '',
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'districtName': districtName,
    'capacity': capacity,
    'currentOccupancy': currentOccupancy,
    'status': status,
  };
}
