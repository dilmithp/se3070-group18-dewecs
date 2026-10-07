import 'dart:async';

import '../models/citizen.dart';
import '../models/contract_rules.dart';
import '../models/district.dart';
import '../models/ground_report.dart';
import '../models/reference_data.dart';
import '../models/report_page.dart';
import '../models/report_submission.dart';
import '../models/sri_lanka_time.dart';
import 'api_exception.dart';
import 'dewecs_api.dart';
import 'image_type.dart';

/// In-memory stand-in for the backend (Demo mode and tests). It follows contract v1: replay detection on
/// citizenId + category + capturedAt, photo rules by status, 404s for unknown ids. Two switches force failures
/// so the offline behaviour can be shown without a real network problem.
class FakeDewecsApi implements DewecsApi {
  FakeDewecsApi({this.latency = const Duration(milliseconds: 400), DateTime Function()? now})
      : _now = now ?? DateTime.now {
    _seedReports();
  }

  /// Delay before every answer (tests pass Duration.zero).
  final Duration latency;
  final DateTime Function() _now;

  /// When true every call fails like a dead connection.
  bool failNetwork = false;

  /// When set (for example 500) every call fails with that HTTP status.
  int? failStatus;

  /// Number of calls that reached the server logic (not counting forced failures); handy in tests.
  int callCount = 0;

  static const _districts = [
    District(id: 1, name: 'Colombo'),
    District(id: 2, name: 'Galle'),
    District(id: 3, name: 'Jaffna'),
    District(id: 4, name: 'Kandy'),
  ];
  static const _categories = ['FLOOD', 'LANDSLIDE', 'CYCLONE', 'DROUGHT'];

  final Map<String, Citizen> _citizensByNic = {};
  final List<GroundReport> _reports = [];
  final Map<String, int> _replayIndex = {};
  int _nextCitizenId = 1;
  int _nextReportId = 1001;
  int _nextPhoto = 1;
  bool _seedsAdopted = false;

  /// All reports on the fake server (tests).
  List<GroundReport> get storedReports => List.unmodifiable(_reports);

  /// Simulates the server database being wiped: every citizen and report disappears.
  void resetServer() {
    _citizensByNic.clear();
    _reports.clear();
    _replayIndex.clear();
    _seedsAdopted = false;
    _seedReports();
  }

  void _seedReports() {
    final now = sriLankaNow(_now());
    GroundReport seed(String category, String status, String text, int districtId, Duration age,
        {String? note}) {
      final district = _districts.firstWhere((d) => d.id == districtId);
      return GroundReport(
        id: _nextReportId++,
        citizenId: 0,
        districtId: district.id,
        districtName: district.name,
        category: category,
        description: text,
        gpsLat: 6.9271234,
        gpsLng: 79.8612345,
        photoUrl: null,
        status: status,
        actionNote: note,
        submittedAt: now.subtract(age),
      );
    }

    _reports.addAll([
      seed('FLOOD', 'PENDING_REVIEW', 'River is overflowing near the bridge', 1, const Duration(hours: 1)),
      seed('LANDSLIDE', 'VERIFIED', 'Cracks on the hillside above the road', 4, const Duration(hours: 5)),
      seed('FLOOD', 'ACTIONED', 'Road under water in the market area', 2, const Duration(days: 1),
          note: 'Rescue team dispatched and the road was closed.'),
      seed('CYCLONE', 'NEEDS_INFO', 'Strong winds, roof damage', 3, const Duration(days: 2)),
    ]);
  }

  Future<void> _gate() async {
    if (latency > Duration.zero) {
      await Future<void>.delayed(latency);
    }
    if (failNetwork) {
      throw const ApiException(
          kind: ApiErrorKind.network, detail: 'Could not reach the server (demo: network switched off).');
    }
    final status = failStatus;
    if (status != null) {
      throw ApiException(
        kind: status >= 500 ? ApiErrorKind.server : ApiErrorKind.client,
        status: status,
        detail: 'Simulated server error ($status).',
      );
    }
    callCount++;
  }

  ApiException _bad(String detail, {Map<String, String> fieldErrors = const {}}) =>
      ApiException(kind: ApiErrorKind.client, status: 400, detail: detail, fieldErrors: fieldErrors);

  ApiException _notFound(String detail) =>
      ApiException(kind: ApiErrorKind.client, status: 404, detail: detail);

  @override
  Future<ReferenceData> getReferenceData() async {
    await _gate();
    return const ReferenceData(districts: _districts, categories: _categories);
  }

  @override
  Future<Citizen> identify({
    required String nic,
    required String fullName,
    required String phone,
    required int districtId,
  }) async {
    await _gate();
    final key = normaliseNic(nic);
    if (!nicPattern.hasMatch(key)) {
      throw _bad('NIC must be 9 digits followed by V or X, or 12 digits.');
    }
    final name = fullName.trim();
    if (name.isEmpty || name.length > maxNameLength) {
      throw _bad('Full name must be 1 to $maxNameLength characters.');
    }
    if (!phonePattern.hasMatch(phone)) {
      throw _bad('Phone number is not valid.');
    }
    final district = _districts.where((d) => d.id == districtId);
    if (district.isEmpty) {
      throw _notFound('District not found: $districtId');
    }
    final existing = _citizensByNic[key];
    if (existing != null) {
      return Citizen(
          id: existing.id,
          fullName: existing.fullName,
          districtId: existing.districtId,
          districtName: existing.districtName,
          created: false);
    }
    final citizen = Citizen(
        id: _nextCitizenId++,
        fullName: name,
        districtId: districtId,
        districtName: district.first.name,
        created: true);
    _citizensByNic[key] = citizen;
    if (!_seedsAdopted) {
      // The first citizen to identify owns the demo reports, so the list is not empty in Demo mode.
      _seedsAdopted = true;
      for (var i = 0; i < _reports.length; i++) {
        if (_reports[i].citizenId == 0) {
          _reports[i] = _reports[i].copyWith(citizenId: citizen.id);
        }
      }
    }
    return citizen;
  }

  @override
  Future<SubmitResult> submitReport(ReportSubmission s) async {
    await _gate();
    final category = s.category.trim().toUpperCase();
    if (!_categories.contains(category)) {
      throw _bad('Invalid category: ${s.category}');
    }
    final text = s.description.trim();
    if (text.isEmpty || text.length > maxDescriptionLength) {
      throw _bad('Description must be 1 to $maxDescriptionLength characters.');
    }
    if (s.gpsLat < -90 || s.gpsLat > 90) {
      throw _bad('Validation failed', fieldErrors: {'gpsLat': 'gpsLat must be between -90 and 90'});
    }
    if (s.gpsLng < -180 || s.gpsLng > 180) {
      throw _bad('Validation failed', fieldErrors: {'gpsLng': 'gpsLng must be between -180 and 180'});
    }
    final captured = parseContractTime(s.capturedAt);
    if (captured.isAfter(sriLankaNow(_now()).add(const Duration(minutes: 5)))) {
      throw _bad('The capture time is in the future.');
    }
    if (!_citizensByNic.values.any((c) => c.id == s.citizenId)) {
      throw _notFound('Citizen not found: ${s.citizenId}');
    }
    final district = _districts.where((d) => d.id == s.districtId);
    if (district.isEmpty) {
      throw _notFound('District not found: ${s.districtId}');
    }
    final replayKey = '${s.citizenId}|$category|${formatContractTime(captured)}';
    final replayId = _replayIndex[replayKey];
    if (replayId != null) {
      return SubmitResult(report: _reports.firstWhere((r) => r.id == replayId), created: false);
    }
    final report = GroundReport(
      id: _nextReportId++,
      citizenId: s.citizenId,
      districtId: s.districtId,
      districtName: district.first.name,
      category: category,
      description: text,
      gpsLat: double.parse(s.gpsLat.toStringAsFixed(7)),
      gpsLng: double.parse(s.gpsLng.toStringAsFixed(7)),
      photoUrl: null,
      status: 'PENDING_REVIEW',
      actionNote: null,
      submittedAt: captured,
    );
    _reports.add(report);
    _replayIndex[replayKey] = report.id;
    return SubmitResult(report: report, created: true);
  }

  @override
  Future<GroundReport> uploadPhoto(int reportId, List<int> bytes, {String filename = 'photo.jpg'}) async {
    await _gate();
    final index = _reports.indexWhere((r) => r.id == reportId);
    if (index < 0) {
      throw _notFound('Ground report not found: $reportId');
    }
    if (!photoAllowedStatuses.contains(_reports[index].status)) {
      throw _bad('A photo can only be added while the report is waiting for review or needs information.');
    }
    if (bytes.isEmpty) {
      throw _bad('The photo is empty.');
    }
    if (bytes.length > maxPhotoBytes) {
      throw _bad('The photo is larger than 5 MB.');
    }
    final mime = sniffImageMimeType(bytes);
    if (mime == null) {
      throw _bad('The photo must be a JPEG, PNG or WebP image.');
    }
    final extension = mime == 'image/png' ? 'png' : (mime == 'image/webp' ? 'webp' : 'jpg');
    final name = 'demo-photo-${_nextPhoto++}.$extension';
    _reports[index] = _reports[index].copyWith(photoUrl: '/api/v1/photos/$name');
    return _reports[index];
  }

  @override
  Future<GroundReport> getReport(int reportId) async {
    await _gate();
    final found = _reports.where((r) => r.id == reportId);
    if (found.isEmpty) {
      throw _notFound('Ground report not found: $reportId');
    }
    return found.first;
  }

  @override
  Future<ReportPage> listCitizenReports(int citizenId, {int page = 0, int size = 20}) async {
    await _gate();
    if (!_citizensByNic.values.any((c) => c.id == citizenId)) {
      throw _notFound('Citizen not found: $citizenId');
    }
    final safePage = page < 0 ? 0 : page;
    final safeSize = size.clamp(1, 50);
    final mine = _reports.where((r) => r.citizenId == citizenId).toList()
      ..sort((a, b) {
        final byTime = b.submittedAt.compareTo(a.submittedAt);
        return byTime != 0 ? byTime : b.id.compareTo(a.id);
      });
    final start = safePage * safeSize;
    final items = start >= mine.length ? <GroundReport>[] : mine.skip(start).take(safeSize).toList();
    return ReportPage(
      items: items,
      page: safePage,
      size: safeSize,
      totalItems: mine.length,
      totalPages: (mine.length / safeSize).ceil(),
    );
  }

  @override
  String? absolutePhotoUrl(GroundReport report) => null;
}
