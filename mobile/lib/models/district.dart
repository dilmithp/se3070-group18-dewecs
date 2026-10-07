class District {
  const District({required this.id, required this.name});

  final int id;
  final String name;

  factory District.fromJson(Map<String, dynamic> json) =>
      District(id: (json['id'] as num).toInt(), name: json['name'] as String);

  Map<String, dynamic> toJson() => {'id': id, 'name': name};

  @override
  bool operator ==(Object other) => other is District && other.id == id && other.name == name;

  @override
  int get hashCode => Object.hash(id, name);
}
