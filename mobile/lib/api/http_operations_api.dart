import 'dart:async';
import 'dart:convert';

import 'package:http/http.dart' as http;

import '../models/action_result.dart';
import '../models/api_problem.dart';
import '../models/rescue_request.dart';
import '../models/shelter.dart';
import 'api_exception.dart';
import 'operations_api.dart';

/// The officer pages over HTTP, asked for JSON. Requests carry `Accept: application/json` (a JSON body for actions
/// that take input); responses are decoded from their bytes as UTF-8, like HttpDewecsApi does.
class HttpOperationsApi implements OperationsApi {
  HttpOperationsApi({
    required String baseUrl,
    http.Client? client,
    this.timeout = const Duration(seconds: 15),
  })  : baseUrl = baseUrl.trim().replaceAll(RegExp(r'/+$'), ''),
        _client = client ?? http.Client();

  final String baseUrl;
  final Duration timeout;
  final http.Client _client;

  @override
  Future<ShelterList> listShelters({String? status, int? districtId}) =>
      _get('/shelters', ShelterList.fromJson, {'status': status, 'districtId': districtId?.toString()});

  @override
  Future<ShelterDetail> getShelter(int id) => _get('/shelters/$id', ShelterDetail.fromJson);

  @override
  Future<ShelterFormData> getShelterFormData() => _get('/shelters/new', ShelterFormData.fromJson);

  @override
  Future<ActionResult> createShelter({
    required int districtId,
    required int organizationId,
    required String name,
    required int capacity,
  }) =>
      _post('/shelters', {'districtId': districtId, 'organizationId': organizationId, 'name': name, 'capacity': capacity});

  @override
  Future<ActionResult> updateShelter(
    int id, {
    required int districtId,
    required int organizationId,
    required String name,
    required int capacity,
  }) =>
      _post('/shelters/$id', {
        'districtId': districtId,
        'organizationId': organizationId,
        'name': name,
        'capacity': capacity,
      });

  @override
  Future<ActionResult> closeShelter(int id) => _post('/shelters/$id/close');

  @override
  Future<ActionResult> reopenShelter(int id) => _post('/shelters/$id/reopen');

  @override
  Future<ActionResult> checkIn(int shelterId, {required String fullName, required String nic}) =>
      _post('/shelters/$shelterId/check-in', {'fullName': fullName, 'nic': nic});

  @override
  Future<ActionResult> checkOut(int shelterId, int occupantId) => _post('/shelters/$shelterId/check-out/$occupantId');

  @override
  Future<RescueRequestList> listRescueRequests({String? status, String? priority, int? districtId}) => _get(
      '/rescue-requests',
      RescueRequestList.fromJson,
      {'status': status, 'priority': priority, 'districtId': districtId?.toString()});

  @override
  Future<RescueRequestDetail> getRescueRequest(int id) => _get('/rescue-requests/$id', RescueRequestDetail.fromJson);

  @override
  Future<RescueRequestFormData> getRescueFormData() => _get('/rescue-requests/new', RescueRequestFormData.fromJson);

  @override
  Future<ActionResult> submitRescueRequest({
    required int districtId,
    required String requesterName,
    required String requesterPhone,
    double? gpsLat,
    double? gpsLng,
    required String description,
    required String priority,
  }) =>
      _post('/rescue-requests', {
        'districtId': districtId,
        'requesterName': requesterName,
        'requesterPhone': requesterPhone,
        'gpsLat': gpsLat,
        'gpsLng': gpsLng,
        'description': description,
        'priority': priority,
      });

  @override
  Future<ActionResult> assignTeam(int requestId, int teamId) =>
      _post('/rescue-requests/$requestId/assign', {'teamId': teamId});

  @override
  Future<ActionResult> completeRescueRequest(int requestId) => _post('/rescue-requests/$requestId/complete');

  @override
  Future<ActionResult> cancelRescueRequest(int requestId) => _post('/rescue-requests/$requestId/cancel');

  Future<T> _get<T>(String path, T Function(Map<String, dynamic>) fromJson, [Map<String, String?> query = const {}]) async {
    final params = {
      for (final entry in query.entries)
        if (entry.value != null && entry.value!.isNotEmpty) entry.key: entry.value!,
    };
    final uri = Uri.parse('$baseUrl$path').replace(queryParameters: params.isEmpty ? null : params);
    return _parse(await _send(http.Request('GET', uri)), fromJson);
  }

  /// An action. Without a [body] nothing is sent but the Accept header (close, reopen, check-out ...). Null values
  /// are left out of the JSON, which the server reads as "not given".
  Future<ActionResult> _post(String path, [Map<String, Object?>? body]) async {
    final request = http.Request('POST', Uri.parse('$baseUrl$path'));
    if (body != null) {
      request
        ..headers['Content-Type'] = 'application/json'
        ..bodyBytes = utf8.encode(jsonEncode({
          for (final entry in body.entries)
            if (entry.value != null) entry.key: entry.value,
        }));
    }
    return _parse(await _send(request), ActionResult.fromJson);
  }

  Future<http.Response> _send(http.Request request) async {
    request.headers['Accept'] = 'application/json';
    try {
      return await Future(() async {
        final streamed = await _client.send(request);
        return http.Response.fromStream(streamed);
      }).timeout(timeout);
    } on TimeoutException {
      throw const ApiException(kind: ApiErrorKind.timeout, detail: 'The server took too long to answer.');
    } on Exception catch (e) {
      throw ApiException(kind: ApiErrorKind.network, detail: 'Could not reach the server ($e).');
    }
  }

  T _parse<T>(http.Response response, T Function(Map<String, dynamic>) fromJson) {
    final status = response.statusCode;
    if (status >= 200 && status < 300) {
      try {
        return fromJson(jsonDecode(utf8.decode(response.bodyBytes)) as Map<String, dynamic>);
      } on FormatException {
        throw _malformed(status);
      } on TypeError {
        throw _malformed(status);
      }
    }
    throw _problem(response);
  }

  ApiException _malformed(int status) =>
      ApiException(kind: ApiErrorKind.server, status: status, detail: 'The server sent an unexpected response.');

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
