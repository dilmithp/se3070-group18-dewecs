import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;
import 'package:http_parser/http_parser.dart';

import '../models/api_problem.dart';
import '../models/citizen.dart';
import '../models/ground_report.dart';
import '../models/relief_distribution.dart';
import '../models/relief_supply.dart';
import '../models/reference_data.dart';
import '../models/report_page.dart';
import '../models/report_submission.dart';
import '../models/shelter_summary.dart';
import 'api_exception.dart';
import 'dewecs_api.dart';
import 'image_type.dart';

/// The real backend over HTTP. Requests are UTF-8 JSON; every response is decoded from its bytes as UTF-8
/// (Spring sends application/json without a charset, so package:http would otherwise decode it as Latin-1).
class HttpDewecsApi implements DewecsApi {
  HttpDewecsApi({
    required String baseUrl,
    http.Client? client,
    this.timeout = const Duration(seconds: 15),
    this.uploadTimeout = const Duration(seconds: 60),
  })  : baseUrl = baseUrl.trim().replaceAll(RegExp(r'/+$'), ''),
        _client = client ?? http.Client();

  final String baseUrl;
  final Duration timeout;
  final Duration uploadTimeout;
  final http.Client _client;

  Uri _uri(String path, [Map<String, String>? query]) =>
      Uri.parse('$baseUrl$path').replace(queryParameters: query);

  @override
  Future<ReferenceData> getReferenceData() async {
    final response = await _send(http.Request('GET', _uri('/api/v1/reference-data')), timeout);
    return _parse(response, (json) => ReferenceData.fromJson(json));
  }

  @override
  Future<Citizen> identify({
    required String nic,
    required String fullName,
    required String phone,
    required int districtId,
  }) async {
    final request = _jsonRequest('POST', '/api/v1/citizens/identify', {
      'nic': nic,
      'fullName': fullName,
      'phone': phone,
      'districtId': districtId,
    });
    final response = await _send(request, timeout);
    return _parse(response, (json) => Citizen.fromJson(json));
  }

  @override
  Future<SubmitResult> submitReport(ReportSubmission submission) async {
    final response = await _send(_jsonRequest('POST', '/api/v1/ground-reports', submission.toJson()), timeout);
    final report = _parse(response, (json) => GroundReport.fromJson(json));
    return SubmitResult(report: report, created: response.statusCode == 201);
  }

  @override
  Future<GroundReport> uploadPhoto(int reportId, List<int> bytes, {String filename = 'photo.jpg'}) async {
    final mime = sniffImageMimeType(bytes) ?? 'application/octet-stream';
    final request = http.MultipartRequest('POST', _uri('/api/v1/ground-reports/$reportId/photo'))
      ..headers['Accept'] = 'application/json'
      ..files.add(http.MultipartFile.fromBytes('file', bytes,
          filename: filename, contentType: MediaType.parse(mime)));
    final response = await _send(request, uploadTimeout);
    return _parse(response, (json) => GroundReport.fromJson(json));
  }

  @override
  Future<GroundReport> getReport(int reportId) async {
    final response = await _send(http.Request('GET', _uri('/api/v1/ground-reports/$reportId')), timeout);
    return _parse(response, (json) => GroundReport.fromJson(json));
  }

  @override
  Future<ReportPage> listCitizenReports(int citizenId, {int page = 0, int size = 20}) async {
    final uri = _uri('/api/v1/citizens/$citizenId/ground-reports', {'page': '$page', 'size': '$size'});
    final response = await _send(http.Request('GET', uri), timeout);
    return _parse(response, (json) => ReportPage.fromJson(json));
  }

  @override
  Future<List<ReliefSupply>> listSupplies({String? type, int? districtId, bool lowStockOnly = false}) async {
    final query = <String, String>{
      'type': ?type,
      'districtId': ?districtId?.toString(),
      if (lowStockOnly) 'lowStockOnly': 'true',
    };
    final response = await _send(http.Request('GET', _uri('/relief-supplies', query)), timeout);
    return _parse(response, (json) => _list(json, 'supplies', ReliefSupply.fromJson));
  }

  @override
  Future<ReliefSupply> getSupply(int supplyId) async {
    final response = await _send(http.Request('GET', _uri('/relief-supplies/$supplyId')), timeout);
    return _parse(response, (json) => ReliefSupply.fromJson(json['supply'] as Map<String, dynamic>));
  }

  @override
  Future<List<ReliefDistribution>> listDistributions({String? status, int? shelterId, int? supplyId}) async {
    final query = <String, String>{
      'status': ?status,
      'shelterId': ?shelterId?.toString(),
      // The server names the supply filter resourceId.
      'resourceId': ?supplyId?.toString(),
    };
    final response = await _send(http.Request('GET', _uri('/relief-distributions', query)), timeout);
    return _parse(response, (json) => _list(json, 'distributions', ReliefDistribution.fromJson));
  }

  @override
  Future<ReliefDistribution> getDistribution(int distributionId) async {
    final response = await _send(http.Request('GET', _uri('/relief-distributions/$distributionId')), timeout);
    return _parse(
        response, (json) => ReliefDistribution.fromJson(json['distribution'] as Map<String, dynamic>));
  }

  @override
  Future<List<ShelterSummary>> listShelters() async {
    final response = await _send(http.Request('GET', _uri('/shelters')), timeout);
    return _parse(response, (json) => _list(json, 'shelters', ShelterSummary.fromJson));
  }

  @override
  Future<DistributionCreated> createDistribution({
    required int supplyId,
    required int shelterId,
    required int quantity,
  }) async {
    // The server names the supply field resourceId.
    final request = _jsonRequest('POST', '/relief-distributions', {
      'resourceId': supplyId,
      'shelterId': shelterId,
      'quantity': quantity,
    });
    final response = await _send(request, timeout);
    return _parse(response, DistributionCreated.fromJson);
  }

  /// Officer pages wrap a collection in the page model, for example {"supplies":[...],"types":[...]}.
  List<T> _list<T>(Map<String, dynamic> json, String key, T Function(Map<String, dynamic>) fromJson) =>
      (json[key] as List<dynamic>).map((e) => fromJson(e as Map<String, dynamic>)).toList();

  @override
  String? absolutePhotoUrl(GroundReport report) =>
      report.hasDisplayablePhoto ? '$baseUrl${report.photoUrl}' : null;

  http.Request _jsonRequest(String method, String path, Map<String, dynamic> body) {
    return http.Request(method, _uri(path))
      ..headers['Content-Type'] = 'application/json'
      ..headers['Accept'] = 'application/json'
      ..bodyBytes = utf8.encode(jsonEncode(body));
  }

  Future<http.Response> _send(http.BaseRequest request, Duration limit) async {
    request.headers.putIfAbsent('Accept', () => 'application/json');
    try {
      return await Future(() async {
        final streamed = await _client.send(request);
        return http.Response.fromStream(streamed);
      }).timeout(limit);
    } on TimeoutException {
      throw const ApiException(
          kind: ApiErrorKind.timeout, detail: 'The server took too long to answer.');
    } on Exception catch (e) {
      throw ApiException(kind: ApiErrorKind.network, detail: 'Could not reach the server ($e).');
    }
  }

  T _parse<T>(http.Response response, T Function(Map<String, dynamic>) fromJson) {
    final status = response.statusCode;
    if (status >= 200 && status < 300) {
      try {
        final decoded = jsonDecode(utf8.decode(response.bodyBytes));
        return fromJson(decoded as Map<String, dynamic>);
      } on FormatException {
        throw _malformed(status);
      } on TypeError {
        throw _malformed(status);
      }
    }
    throw _problem(response);
  }

  ApiException _malformed(int status) => ApiException(
      kind: ApiErrorKind.server, status: status, detail: 'The server sent an unexpected response.');

  ApiException _problem(http.Response response) {
    final status = response.statusCode;
    ApiProblem? problem;
    try {
      final decoded = jsonDecode(utf8.decode(response.bodyBytes));
      if (decoded is Map<String, dynamic>) {
        problem = ApiProblem.fromJson(decoded);
      }
    } on FormatException {
      problem = null;
    }
    return ApiException(
      kind: status >= 500 ? ApiErrorKind.server : ApiErrorKind.client,
      status: status,
      detail: problem?.detail ?? problem?.title ?? 'The request failed (HTTP $status).',
      fieldErrors: problem?.fieldErrors ?? const {},
    );
  }
}
