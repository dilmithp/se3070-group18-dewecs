import 'sri_lanka_time.dart';

/// A report as the server returns it. Status and category stay raw strings so unknown values survive.
class GroundReport {
  const GroundReport({
    required this.id,
    required this.citizenId,
    required this.districtId,
    required this.districtName,
    required this.category,
    required this.description,
    required this.gpsLat,
    required this.gpsLng,
    required this.photoUrl,
    required this.status,
    required this.actionNote,
    required this.submittedAt,
  });

  final int id;
  final int citizenId;
  final int districtId;
  final String districtName;
  final String category;
  final String description;
  final double gpsLat;
  final double gpsLng;
  final String? photoUrl;
  final String status;
  final String? actionNote;
  final DateTime submittedAt;

  factory GroundReport.fromJson(Map<String, dynamic> json) => GroundReport(
        id: (json['id'] as num).toInt(),
        citizenId: (json['citizenId'] as num).toInt(),
        districtId: (json['districtId'] as num).toInt(),
        districtName: json['districtName'] as String,
        category: json['category'] as String,
        description: json['description'] as String,
        gpsLat: (json['gpsLat'] as num).toDouble(),
        gpsLng: (json['gpsLng'] as num).toDouble(),
        photoUrl: json['photoUrl'] as String?,
        status: json['status'] as String,
        actionNote: json['actionNote'] as String?,
        submittedAt: parseContractTime(json['submittedAt'] as String),
      );

  Map<String, dynamic> toJson() => {
        'id': id,
        'citizenId': citizenId,
        'districtId': districtId,
        'districtName': districtName,
        'category': category,
        'description': description,
        'gpsLat': gpsLat,
        'gpsLng': gpsLng,
        'photoUrl': photoUrl,
        'status': status,
        'actionNote': actionNote,
        'submittedAt': formatContractTime(submittedAt),
      };

  GroundReport copyWith({int? citizenId, String? photoUrl, String? status, String? actionNote}) => GroundReport(
        id: id,
        citizenId: citizenId ?? this.citizenId,
        districtId: districtId,
        districtName: districtName,
        category: category,
        description: description,
        gpsLat: gpsLat,
        gpsLng: gpsLng,
        photoUrl: photoUrl ?? this.photoUrl,
        status: status ?? this.status,
        actionNote: actionNote ?? this.actionNote,
        submittedAt: submittedAt,
      );

  /// True when the photo URL is a server photo the app can show (legacy rows hold arbitrary text).
  bool get hasDisplayablePhoto => photoUrl != null && photoUrl!.startsWith('/api/v1/photos/');
}
