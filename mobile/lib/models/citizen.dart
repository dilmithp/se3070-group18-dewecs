/// The identify response, also what the app remembers about the citizen (never the NIC).
class Citizen {
  const Citizen({
    required this.id,
    required this.fullName,
    required this.districtId,
    required this.districtName,
    this.created = false,
  });

  final int id;
  final String fullName;
  final int districtId;
  final String districtName;
  final bool created;

  factory Citizen.fromJson(Map<String, dynamic> json) => Citizen(
        id: (json['id'] as num).toInt(),
        fullName: json['fullName'] as String,
        districtId: (json['districtId'] as num).toInt(),
        districtName: json['districtName'] as String,
        created: json['created'] as bool? ?? false,
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'fullName': fullName,
        'districtId': districtId,
        'districtName': districtName,
        'created': created,
      };
}
