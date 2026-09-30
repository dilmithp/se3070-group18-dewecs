import '../models/citizen.dart';
import '../models/ground_report.dart';
import '../models/relief_distribution.dart';
import '../models/relief_supply.dart';
import '../models/reference_data.dart';
import '../models/report_page.dart';
import '../models/report_submission.dart';
import '../models/shelter_summary.dart';

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

  /// UC-04, officer routes (JSON through the Accept header; the server has no authentication on them yet).
  /// Supplies, optionally filtered by ResourceTypes value, district, or only those the server flags as low stock.
  Future<List<ReliefSupply>> listSupplies({String? type, int? districtId, bool lowStockOnly = false});

  Future<ReliefSupply> getSupply(int supplyId);

  /// Distributions, optionally filtered by DistributionStatuses value, shelter or supply.
  Future<List<ReliefDistribution>> listDistributions({String? status, int? shelterId, int? supplyId});

  Future<ReliefDistribution> getDistribution(int distributionId);

  /// Shelters for the destination pick-list (UC-03 data, read only).
  Future<List<ShelterSummary>> listShelters();

  /// Logs a distribution and reduces the supply's stock on the server. Callers must not retry this automatically or
  /// queue it for later: the request carries no idempotency key, so a retry after a lost response could deduct the
  /// stock twice. A failure is shown to the user, who decides whether to try again.
  Future<DistributionCreated> createDistribution({
    required int supplyId,
    required int shelterId,
    required int quantity,
  });

  /// Base URL plus the photo path, or null when there is nothing the app can load.
  String? absolutePhotoUrl(GroundReport report);
}

class SubmitResult {
  const SubmitResult({required this.report, required this.created});

  final GroundReport report;
  final bool created;
}
