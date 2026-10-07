import 'district.dart';

class ReferenceData {
  const ReferenceData({required this.districts, required this.categories});

  final List<District> districts;
  final List<String> categories;

  factory ReferenceData.fromJson(Map<String, dynamic> json) => ReferenceData(
        districts: (json['districts'] as List<dynamic>)
            .map((d) => District.fromJson(d as Map<String, dynamic>))
            .toList(),
        categories: (json['categories'] as List<dynamic>).map((c) => c as String).toList(),
      );

  Map<String, dynamic> toJson() => {
        'districts': districts.map((d) => d.toJson()).toList(),
        'categories': categories,
      };
}
