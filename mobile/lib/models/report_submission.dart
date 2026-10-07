/// Body of POST /ground-reports. capturedAt is created once on the device and resent unchanged on every retry.
class ReportSubmission {
  const ReportSubmission({
    required this.citizenId,
    required this.districtId,
    required this.category,
    required this.description,
    required this.gpsLat,
    required this.gpsLng,
    required this.capturedAt,
  });

  final int citizenId;
  final int districtId;
  final String category;
  final String description;
  final double gpsLat;
  final double gpsLng;
  final String capturedAt;

  Map<String, dynamic> toJson() => {
        'citizenId': citizenId,
        'districtId': districtId,
        'category': category,
        'description': description,
        'gpsLat': gpsLat,
        'gpsLng': gpsLng,
        'capturedAt': capturedAt,
      };
}
