import '../models/citizen.dart';
import '../models/ground_report.dart';
import '../models/reference_data.dart';
import '../models/report_page.dart';
import '../models/report_submission.dart';

/// One method per endpoint of contract v1. Every failure is an ApiException.
abstract class DewecsApi {
  Future<ReferenceData> getReferenceData();

  /// 201 (created) and 200 (already existed) both succeed; see Citizen.created.
  Future<Citizen> identify({
    required String nic,
    required String fullName,
    required String phone,
    required int districtId,
  });

  /// 201 means created, 200 means an identical earlier submission was found (idempotent replay).
  Future<SubmitResult> submitReport(ReportSubmission submission);

  Future<GroundReport> uploadPhoto(int reportId, List<int> bytes, {String filename = 'photo.jpg'});

  Future<GroundReport> getReport(int reportId);

  Future<ReportPage> listCitizenReports(int citizenId, {int page = 0, int size = 20});

  /// Base URL plus the photo path, or null when there is nothing the app can load.
  String? absolutePhotoUrl(GroundReport report);
}

class SubmitResult {
  const SubmitResult({required this.report, required this.created});

  final GroundReport report;
  final bool created;
}
