import 'ground_report.dart';

class ReportPage {
  const ReportPage({
    required this.items,
    required this.page,
    required this.size,
    required this.totalItems,
    required this.totalPages,
  });

  final List<GroundReport> items;
  final int page;
  final int size;
  final int totalItems;
  final int totalPages;

  bool get hasMore => page + 1 < totalPages;

  factory ReportPage.fromJson(Map<String, dynamic> json) => ReportPage(
        items: (json['items'] as List<dynamic>)
            .map((r) => GroundReport.fromJson(r as Map<String, dynamic>))
            .toList(),
        page: (json['page'] as num).toInt(),
        size: (json['size'] as num).toInt(),
        totalItems: (json['totalItems'] as num).toInt(),
        totalPages: (json['totalPages'] as num).toInt(),
      );

  Map<String, dynamic> toJson() => {
        'items': items.map((r) => r.toJson()).toList(),
        'page': page,
        'size': size,
        'totalItems': totalItems,
        'totalPages': totalPages,
      };
}
