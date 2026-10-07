import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:dewecs_mobile/api/api_exception.dart';
import 'package:dewecs_mobile/api/http_dewecs_api.dart';
import 'package:dewecs_mobile/models/report_submission.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

String fixtureText(String name) => File('test/fixtures/$name').readAsStringSync();

/// Spring sends application/json without a charset: the body is raw UTF-8 bytes.
http.Response jsonResponse(String body, int status, {String contentType = 'application/json'}) =>
    http.Response.bytes(utf8.encode(body), status, headers: {'content-type': contentType});

const sinhala = 'ගඟ ගලා බසිනවා — පාලම අසල';

ReportSubmission submission({String description = 'River overflowing'}) => ReportSubmission(
      citizenId: 42,
      districtId: 1,
      category: 'FLOOD',
      description: description,
      gpsLat: 6.9271234,
      gpsLng: 79.8612345,
      capturedAt: '2026-10-07T14:03:11.123',
    );

void main() {
  late http.Request seen;

  HttpDewecsApi apiReturning(http.Response Function(http.Request) respond,
      {Duration timeout = const Duration(seconds: 5), String baseUrl = 'http://host:8080'}) {
    return HttpDewecsApi(
      baseUrl: baseUrl,
      timeout: timeout,
      client: MockClient((request) async {
        seen = request;
        return respond(request);
      }),
    );
  }

  test('reference data: GET on the right path and a trailing slash in the base URL is ignored', () async {
    final api = apiReturning((_) => jsonResponse(fixtureText('reference-data.json'), 200),
        baseUrl: 'http://host:8080/');

    final data = await api.getReferenceData();

    expect(seen.method, 'GET');
    expect(seen.url.toString(), 'http://host:8080/api/v1/reference-data');
    expect(data.districts.single.name, 'Colombo');
  });

  test('identify: JSON POST, exact Content-Type, UTF-8 body with Sinhala text', () async {
    final api = apiReturning((_) => jsonResponse(fixtureText('citizen.json'), 201));

    final citizen = await api.identify(
        nic: '199012345678', fullName: 'නිමල් පෙරේරා', phone: '0771234567', districtId: 1);

    expect(seen.method, 'POST');
    expect(seen.url.path, '/api/v1/citizens/identify');
    expect(seen.headers['Content-Type'], 'application/json');
    final body = jsonDecode(utf8.decode(seen.bodyBytes)) as Map<String, dynamic>;
    expect(body, {'nic': '199012345678', 'fullName': 'නිමල් පෙරේරා', 'phone': '0771234567', 'districtId': 1});
    expect(citizen.created, isTrue);
  });

  test('submit: sends the exact capturedAt, 201 means created and 200 means replay', () async {
    var status = 201;
    final api = apiReturning((_) => jsonResponse(fixtureText('report.json'), status));

    final created = await api.submitReport(submission(description: sinhala));
    final body = jsonDecode(utf8.decode(seen.bodyBytes)) as Map<String, dynamic>;
    status = 200;
    final replay = await api.submitReport(submission());

    expect(seen.url.path, '/api/v1/ground-reports');
    expect(body['capturedAt'], '2026-10-07T14:03:11.123');
    expect(body['description'], sinhala);
    expect(body['gpsLat'], 6.9271234);
    expect(created.created, isTrue);
    expect(replay.created, isFalse);
    expect(replay.report.id, 101);
  });

  test('responses are decoded as UTF-8 even though no charset is declared', () async {
    final report = jsonDecode(fixtureText('report.json')) as Map<String, dynamic>
      ..['description'] = sinhala;
    final api = apiReturning((_) => jsonResponse(jsonEncode(report), 200));

    final result = await api.getReport(101);

    expect(result.description, sinhala);
    expect(seen.url.path, '/api/v1/ground-reports/101');
  });

  test('list: page and size go in the query', () async {
    final api = apiReturning((_) => jsonResponse(fixtureText('report-list.json'), 200));

    final page = await api.listCitizenReports(42, page: 2, size: 20);

    expect(seen.url.path, '/api/v1/citizens/42/ground-reports');
    expect(seen.url.queryParameters, {'page': '2', 'size': '20'});
    expect(page.totalItems, 57);
  });

  test('photo upload: multipart with the part named file and the real image type', () async {
    final api = apiReturning((_) => jsonResponse(fixtureText('report.json'), 200));
    final png = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3];

    await api.uploadPhoto(101, png, filename: 'photo.png');

    expect(seen.method, 'POST');
    expect(seen.url.path, '/api/v1/ground-reports/101/photo');
    expect(seen.headers['content-type'], startsWith('multipart/form-data; boundary='));
    final text = latin1.decode(seen.bodyBytes);
    expect(text, contains('name="file"'));
    expect(text, contains('filename="photo.png"'));
    expect(text.toLowerCase(), contains('content-type: image/png'));
  });

  test('400 with fieldErrors becomes a client error that is not retryable', () async {
    final api = apiReturning((_) => jsonResponse(fixtureText('problem-with-field-errors.json'), 400));

    final error = await api.submitReport(submission()).then<ApiException?>((_) => null, onError: (e) => e as ApiException);

    expect(error, isNotNull);
    expect(error!.kind, ApiErrorKind.client);
    expect(error.status, 400);
    expect(error.fieldErrors['description'], 'Describe what you see');
    expect(error.retryable, isFalse);
  });

  test('error bodies are parsed whatever media type they declare', () async {
    final api = apiReturning(
        (_) => jsonResponse(fixtureText('problem.json'), 404, contentType: 'text/plain'));

    final error = await api.getReport(999).then<ApiException?>((_) => null, onError: (e) => e as ApiException);

    expect(error!.detail, 'Ground report not found: 999');
    expect(error.retryable, isFalse);
  });

  test('retry rules follow the contract: 5xx, 408 and 429 retry, other statuses do not', () async {
    final retryable = <int, bool>{};
    for (final status in [400, 404, 405, 408, 413, 415, 429, 500, 502, 503]) {
      final api = apiReturning((_) => jsonResponse('{"detail":"x"}', status));
      final error = await api.getReport(1).then<ApiException?>((_) => null, onError: (e) => e as ApiException);
      retryable[status] = error!.retryable;
    }

    expect(retryable, {
      400: false,
      404: false,
      405: false,
      408: true,
      413: false,
      415: false,
      429: true,
      500: true,
      502: true,
      503: true,
    });
  });

  test('a 5xx with an HTML body gets a generic detail', () async {
    final api = apiReturning((_) => jsonResponse('<html>Bad gateway</html>', 502, contentType: 'text/html'));

    final error = await api.getReport(1).then<ApiException?>((_) => null, onError: (e) => e as ApiException);

    expect(error!.kind, ApiErrorKind.server);
    expect(error.detail, 'The request failed (HTTP 502).');
  });

  test('timeout becomes a retryable timeout error', () async {
    final api = HttpDewecsApi(
      baseUrl: 'http://host:8080',
      timeout: const Duration(milliseconds: 30),
      client: MockClient((request) async {
        await Future<void>.delayed(const Duration(milliseconds: 300));
        return jsonResponse(fixtureText('report.json'), 200);
      }),
    );

    final error = await api.getReport(1).then<ApiException?>((_) => null, onError: (e) => e as ApiException);

    expect(error!.kind, ApiErrorKind.timeout);
    expect(error.retryable, isTrue);
    expect(error.isConnectionDown, isTrue);
  });

  test('a dead connection becomes a retryable network error', () async {
    final api = HttpDewecsApi(
      baseUrl: 'http://host:8080',
      client: MockClient((request) async => throw http.ClientException('connection refused')),
    );

    final error = await api.getReport(1).then<ApiException?>((_) => null, onError: (e) => e as ApiException);

    expect(error!.kind, ApiErrorKind.network);
    expect(error.retryable, isTrue);
    expect(error.isConnectionDown, isTrue);
  });

  test('a malformed success body is a readable server error, never a crash', () async {
    for (final body in ['not json', '[1,2,3]', '{"id": 1}']) {
      final api = apiReturning((_) => jsonResponse(body, 200));

      final error = await api.getReport(1).then<ApiException?>((_) => null, onError: (e) => e as ApiException);

      expect(error, isNotNull, reason: body);
      expect(error!.kind, ApiErrorKind.server);
      expect(error.detail, 'The server sent an unexpected response.');
    }
  });

  test('absolutePhotoUrl prepends the base URL only for API photos', () async {
    final api = apiReturning((_) => jsonResponse(fixtureText('report.json'), 200));
    final withPhoto = (await api.getReport(1)).copyWith(photoUrl: '/api/v1/photos/a.png');
    final legacy = withPhoto.copyWith(photoUrl: 'old.jpg');

    expect(api.absolutePhotoUrl(withPhoto), 'http://host:8080/api/v1/photos/a.png');
    expect(api.absolutePhotoUrl(legacy), isNull);
  });

  test('TimeoutException type is not leaked', () async {
    final api = HttpDewecsApi(
      baseUrl: 'http://host:8080',
      timeout: const Duration(milliseconds: 10),
      client: MockClient((request) async {
        await Future<void>.delayed(const Duration(milliseconds: 100));
        return jsonResponse('{}', 200);
      }),
    );

    await expectLater(api.getReferenceData(), throwsA(isA<ApiException>()));
    expect(true, isTrue, reason: 'no TimeoutException escaped: ${TimeoutException('x')}');
  });
}
