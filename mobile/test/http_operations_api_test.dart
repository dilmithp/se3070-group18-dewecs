import 'dart:convert';
import 'dart:io';

import 'package:dewecs_mobile/api/api_exception.dart';
import 'package:dewecs_mobile/api/http_operations_api.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

String fixtureText(String name) => File('test/fixtures/$name').readAsStringSync();

http.Response jsonResponse(String body, int status, {String contentType = 'application/json'}) =>
    http.Response.bytes(utf8.encode(body), status, headers: {'content-type': contentType});

Map<String, dynamic> sentBody(http.Request request) =>
    jsonDecode(utf8.decode(request.bodyBytes)) as Map<String, dynamic>;

void main() {
  late http.Request seen;

  HttpOperationsApi apiReturning(http.Response Function(http.Request) respond, {String baseUrl = 'http://host:8080'}) {
    return HttpOperationsApi(
      baseUrl: baseUrl,
      timeout: const Duration(seconds: 5),
      client: MockClient((request) async {
        seen = request;
        return respond(request);
      }),
    );
  }

  Future<ApiException?> failure(Future<Object?> call) =>
      call.then<ApiException?>((_) => null, onError: (e) => e as ApiException);

  group('shelters', () {
    test('the list asks for JSON and sends only the filters that are set', () async {
      final api = apiReturning((_) => jsonResponse(fixtureText('shelter-list.json'), 200), baseUrl: 'http://host:8080/');

      final list = await api.listShelters(status: 'OPEN', districtId: 2);

      expect(seen.method, 'GET');
      expect(seen.url.path, '/shelters');
      expect(seen.url.queryParameters, {'status': 'OPEN', 'districtId': '2'});
      expect(seen.headers['Accept'], 'application/json');
      expect(list.shelters, hasLength(2));
    });

    test('an unfiltered list has no query string', () async {
      final api = apiReturning((_) => jsonResponse(fixtureText('shelter-list.json'), 200));

      await api.listShelters(status: '');

      expect(seen.url.hasQuery, isFalse);
    });

    test('detail and form data use their own paths', () async {
      final api = apiReturning((request) => jsonResponse(
          fixtureText(request.url.path == '/shelters/new' ? 'shelter-form.json' : 'shelter-detail.json'), 200));

      final detail = await api.getShelter(1);
      expect(seen.url.path, '/shelters/1');
      final form = await api.getShelterFormData();
      expect(seen.url.path, '/shelters/new');

      expect(detail.occupants, hasLength(2));
      expect(form.organizations, hasLength(2));
    });

    test('create posts a flat JSON body and reads the 201 message and location', () async {
      final api = apiReturning(
          (_) => jsonResponse('{"message":"Shelter created.","location":"/shelters/9"}', 201));

      final result = await api.createShelter(districtId: 1, organizationId: 5, name: 'Town Hall', capacity: 30);

      expect(seen.method, 'POST');
      expect(seen.url.path, '/shelters');
      expect(seen.headers['Content-Type'], 'application/json');
      expect(seen.headers['Accept'], 'application/json');
      expect(sentBody(seen), {'districtId': 1, 'organizationId': 5, 'name': 'Town Hall', 'capacity': 30});
      expect(result.message, 'Shelter created.');
      expect(result.id, 9);
    });

    test('update posts to the shelter path with the whole form', () async {
      final api = apiReturning((_) => jsonResponse('{"message":"Shelter updated.","location":"/shelters/9"}', 200));

      await api.updateShelter(9, districtId: 1, organizationId: 5, name: 'Hall', capacity: 40);

      expect(seen.url.path, '/shelters/9');
      expect(sentBody(seen)['capacity'], 40);
    });

    test('close, reopen and check-out send no body and no content type', () async {
      final api = apiReturning((_) => jsonResponse('{"message":"ok","location":"/shelters/1"}', 200));

      await api.closeShelter(1);
      expect(seen.url.path, '/shelters/1/close');
      expect(seen.bodyBytes, isEmpty);
      expect(seen.headers.containsKey('Content-Type'), isFalse);
      await api.reopenShelter(1);
      expect(seen.url.path, '/shelters/1/reopen');
      await api.checkOut(1, 11);
      expect(seen.url.path, '/shelters/1/check-out/11');
      expect(seen.method, 'POST');
    });

    test('check-in sends the name and NIC, including Sinhala text as UTF-8', () async {
      final api = apiReturning((_) => jsonResponse('{"message":"Occupant checked in.","location":"/shelters/1"}', 200));

      await api.checkIn(1, fullName: 'නිමල් පෙරේරා', nic: '199012345678');

      expect(seen.url.path, '/shelters/1/check-in');
      expect(sentBody(seen), {'fullName': 'නිමල් පෙරේරා', 'nic': '199012345678'});
    });

    test('a rule violation is a 400 whose detail is the text to show', () async {
      final api = apiReturning((_) => jsonResponse(
          '{"status":400,"title":"Bad Request","detail":"Cannot check in: shelter is closed."}', 400,
          contentType: 'application/problem+json'));

      final error = await failure(api.checkIn(1, fullName: 'A', nic: '1'));

      expect(error!.status, 400);
      expect(error.detail, 'Cannot check in: shelter is closed.');
      expect(error.retryable, isFalse);
    });

    test('field errors of the create form come through', () async {
      final api = apiReturning((_) => jsonResponse(
          '{"status":400,"detail":"Validation failed","fieldErrors":{"name":"Enter a name"}}', 400));

      final error = await failure(api.createShelter(districtId: 1, organizationId: 1, name: '', capacity: 1));

      expect(error!.fieldErrors, {'name': 'Enter a name'});
    });
  });

  group('rescue requests', () {
    test('the list sends status, priority and district filters', () async {
      final api = apiReturning((_) => jsonResponse(fixtureText('rescue-list.json'), 200));

      final list = await api.listRescueRequests(status: 'PENDING', priority: 'CRITICAL', districtId: 1);

      expect(seen.url.path, '/rescue-requests');
      expect(seen.url.queryParameters, {'status': 'PENDING', 'priority': 'CRITICAL', 'districtId': '1'});
      expect(list.requests, hasLength(2));
    });

    test('detail and form data use their own paths', () async {
      final api = apiReturning((request) => jsonResponse(
          fixtureText(request.url.path == '/rescue-requests/new' ? 'rescue-form.json' : 'rescue-detail.json'), 200));

      final detail = await api.getRescueRequest(3);
      expect(seen.url.path, '/rescue-requests/3');
      final form = await api.getRescueFormData();
      expect(seen.url.path, '/rescue-requests/new');

      expect(detail.availableTeams.single.id, 8);
      expect(form.priorities, hasLength(4));
    });

    test('submit leaves out a position the user did not give', () async {
      final api = apiReturning(
          (_) => jsonResponse('{"message":"Rescue request submitted.","location":"/rescue-requests/5"}', 201));

      final result = await api.submitRescueRequest(
        districtId: 1,
        requesterName: 'Nimal',
        requesterPhone: '0771234567',
        description: 'Trapped',
        priority: 'HIGH',
      );

      expect(seen.url.path, '/rescue-requests');
      expect(sentBody(seen), {
        'districtId': 1,
        'requesterName': 'Nimal',
        'requesterPhone': '0771234567',
        'description': 'Trapped',
        'priority': 'HIGH',
      });
      expect(result.id, 5);
    });

    test('submit sends the position when it is given', () async {
      final api = apiReturning((_) => jsonResponse('{"message":"m","location":"/rescue-requests/5"}', 201));

      await api.submitRescueRequest(
        districtId: 1,
        requesterName: 'Nimal',
        requesterPhone: '0771234567',
        gpsLat: 6.9271234,
        gpsLng: 79.8612345,
        description: 'Trapped',
        priority: 'HIGH',
      );

      expect(sentBody(seen)['gpsLat'], 6.9271234);
      expect(sentBody(seen)['gpsLng'], 79.8612345);
    });

    test('assign sends the team id; complete and cancel send nothing', () async {
      final api = apiReturning((_) => jsonResponse('{"message":"ok","location":"/rescue-requests/3"}', 200));

      await api.assignTeam(3, 8);
      expect(seen.url.path, '/rescue-requests/3/assign');
      expect(sentBody(seen), {'teamId': 8});
      await api.completeRescueRequest(3);
      expect(seen.url.path, '/rescue-requests/3/complete');
      expect(seen.bodyBytes, isEmpty);
      await api.cancelRescueRequest(3);
      expect(seen.url.path, '/rescue-requests/3/cancel');
    });
  });

  group('failures', () {
    test('an unknown id is a 404 that does not retry', () async {
      final api = apiReturning((_) => jsonResponse('{"status":404,"detail":"Shelter not found: 99"}', 404));

      final error = await failure(api.getShelter(99));

      expect(error!.status, 404);
      expect(error.detail, 'Shelter not found: 99');
      expect(error.retryable, isFalse);
    });

    test('a 5xx with an HTML body gets a generic detail and may retry', () async {
      final api = apiReturning((_) => jsonResponse('<html>Bad gateway</html>', 502, contentType: 'text/html'));

      final error = await failure(api.listShelters());

      expect(error!.kind, ApiErrorKind.server);
      expect(error.detail, 'The request failed (HTTP 502).');
      expect(error.retryable, isTrue);
    });

    test('an HTML page where JSON was expected is an unexpected response', () async {
      final api = apiReturning((_) => jsonResponse('<html>page</html>', 200, contentType: 'text/html'));

      final error = await failure(api.listRescueRequests());

      expect(error!.detail, 'The server sent an unexpected response.');
    });

    test('a dead connection and a timeout are connection errors', () async {
      final dead = HttpOperationsApi(
          baseUrl: 'http://host:8080', client: MockClient((_) async => throw http.ClientException('refused')));
      final slow = HttpOperationsApi(
        baseUrl: 'http://host:8080',
        timeout: const Duration(milliseconds: 30),
        client: MockClient((_) async {
          await Future<void>.delayed(const Duration(milliseconds: 300));
          return jsonResponse(fixtureText('shelter-list.json'), 200);
        }),
      );

      final network = await failure(dead.listShelters());
      final timeout = await failure(slow.listShelters());

      expect(network!.kind, ApiErrorKind.network);
      expect(timeout!.kind, ApiErrorKind.timeout);
      expect(network.isConnectionDown && timeout.isConnectionDown, isTrue);
    });
  });
}
