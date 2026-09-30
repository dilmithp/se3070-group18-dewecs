import 'dart:convert';

import 'package:dewecs_mobile/api/api_exception.dart';
import 'package:dewecs_mobile/api/http_dewecs_api.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'http_dewecs_api_test.dart' show fixtureText, jsonResponse;

void main() {
  late http.Request seen;

  HttpDewecsApi apiReturning(http.Response Function(http.Request) respond) =>
      HttpDewecsApi(
        baseUrl: 'http://host:8080',
        client: MockClient((request) async {
          seen = request;
          return respond(request);
        }),
      );

  test('supplies: GET /relief-supplies asks for JSON and sends no query when there is no filter', () async {
    final api = apiReturning(
      (_) => jsonResponse(fixtureText('relief-supplies.json'), 200),
    );

    final supplies = await api.listSupplies();

    expect(seen.method, 'GET');
    expect(seen.url.path, '/relief-supplies');
    expect(seen.url.queryParameters, isEmpty);
    expect(seen.headers['Accept'], 'application/json');
    expect(supplies.map((s) => s.id), [1, 3]);
  });

  test('supplies: type, district and low-stock filters use the backend parameter names', () async {
    final api = apiReturning(
      (_) => jsonResponse(fixtureText('relief-supplies.json'), 200),
    );

    await api.listSupplies(type: 'FOOD', districtId: 2, lowStockOnly: true);

    expect(seen.url.queryParameters, {
      'type': 'FOOD',
      'districtId': '2',
      'lowStockOnly': 'true',
    });
  });

  test('supply detail is read from the supply key of the page model', () async {
    final supply =
        (jsonDecode(fixtureText('relief-supplies.json'))['supplies'] as List)
            .last;
    final api = apiReturning(
      (_) => jsonResponse(jsonEncode({'supply': supply}), 200),
    );

    final result = await api.getSupply(3);

    expect(seen.url.path, '/relief-supplies/3');
    expect(result.name, 'First-aid kits');
    expect(result.lowStock, isTrue);
  });

  test(
    'a 404 on a supply becomes a client ApiException with the server text',
    () async {
      final api = apiReturning(
        (_) => jsonResponse(
          jsonEncode({
            'status': 404,
            'title': 'Not Found',
            'detail': 'Relief supply not found: 99',
          }),
          404,
          contentType: 'application/problem+json',
        ),
      );

      await expectLater(
        api.getSupply(99),
        throwsA(
          isA<ApiException>()
              .having((e) => e.status, 'status', 404)
              .having((e) => e.detail, 'detail', 'Relief supply not found: 99'),
        ),
      );
    },
  );

  test('distributions: the supply filter is sent as resourceId', () async {
    final api = apiReturning(
      (_) => jsonResponse(fixtureText('relief-distributions.json'), 200),
    );

    final list = await api.listDistributions(
      status: 'DISPATCHED',
      shelterId: 2,
      supplyId: 1,
    );

    expect(seen.url.path, '/relief-distributions');
    expect(seen.url.queryParameters, {
      'status': 'DISPATCHED',
      'shelterId': '2',
      'resourceId': '1',
    });
    expect(list.first.shelterName, 'Colombo Sports Complex');
  });

  test('distribution detail is read from the distribution key', () async {
    final item =
        (jsonDecode(fixtureText('relief-distributions.json'))['distributions']
                as List)
            .first;
    final api = apiReturning(
      (_) => jsonResponse(jsonEncode({'distribution': item}), 200),
    );

    final result = await api.getDistribution(7);

    expect(seen.url.path, '/relief-distributions/7');
    expect(result.quantity, 40);
  });

  test('shelters come from GET /shelters', () async {
    final api = apiReturning(
      (_) => jsonResponse(fixtureText('shelters.json'), 200),
    );

    final shelters = await api.listShelters();

    expect(seen.url.path, '/shelters');
    expect(shelters.map((s) => s.name), [
      'Colombo Sports Complex',
      'Galle Town Hall',
    ]);
  });

  test('create distribution: JSON body uses resourceId and the 201 location gives the new id', () async {
    final api = apiReturning(
      (_) => jsonResponse(
        jsonEncode({
          'message': 'Relief distribution dispatched.',
          'location': '/relief-distributions/12',
        }),
        201,
      ),
    );

    final created = await api.createDistribution(
      supplyId: 1,
      shelterId: 2,
      quantity: 40,
    );

    expect(seen.method, 'POST');
    expect(seen.url.path, '/relief-distributions');
    expect(seen.headers['Content-Type'], 'application/json');
    expect(jsonDecode(utf8.decode(seen.bodyBytes)), {
      'resourceId': 1,
      'shelterId': 2,
      'quantity': 40,
    });
    expect(created.id, 12);
  });

  test(
    'create distribution: an over-stock rule violation keeps the server detail',
    () async {
      final api = apiReturning(
        (_) => jsonResponse(
          jsonEncode({
            'status': 400,
            'detail': 'Requested quantity exceeds available stock (120 bags available).',
          }),
          400,
          contentType: 'application/problem+json',
        ),
      );

      await expectLater(
        api.createDistribution(supplyId: 1, shelterId: 2, quantity: 500),
        throwsA(
          isA<ApiException>()
              .having((e) => e.retryable, 'retryable', isFalse)
              .having(
                (e) => e.detail,
                'detail',
                contains('exceeds available stock'),
              ),
        ),
      );
    },
  );

  test('create distribution: field errors from validation are exposed by field name', () async {
    final api = apiReturning(
      (_) => jsonResponse(
        jsonEncode({
          'status': 400,
          'fieldErrors': {'quantity': 'Quantity must be positive'},
        }),
        400,
        contentType: 'application/problem+json',
      ),
    );

    await expectLater(
      api.createDistribution(supplyId: 1, shelterId: 2, quantity: 0),
      throwsA(
        isA<ApiException>().having(
          (e) => e.fieldErrors['quantity'],
          'quantity',
          'Quantity must be positive',
        ),
      ),
    );
  });

  test('a body without the expected collection is reported as a malformed response', () async {
    final api = apiReturning((_) => jsonResponse('{"other": []}', 200));

    await expectLater(
      api.listSupplies(),
      throwsA(
        isA<ApiException>().having((e) => e.kind, 'kind', ApiErrorKind.server),
      ),
    );
  });
}
