/// An organization that owns shelters and rescue teams (a drop-down entry of the officer pages).
class Organization {
  const Organization({required this.id, required this.name, this.type});

  final int id;
  final String name;
  final String? type;

  factory Organization.fromJson(Map<String, dynamic> json) =>
      Organization(id: (json['id'] as num).toInt(), name: json['name'] as String, type: json['type'] as String?);

  @override
  bool operator ==(Object other) => other is Organization && other.id == id && other.name == name && other.type == type;

  @override
  int get hashCode => Object.hash(id, name, type);
}
