import 'package:dewecs_mobile/api/fake_operations_api.dart';
import 'package:dewecs_mobile/state/rescue_form_controller.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late FakeOperationsApi api;
  late RescueFormController controller;

  setUp(() {
    api = FakeOperationsApi(latency: Duration.zero);
    controller = RescueFormController(() => api);
  });

  test('load fetches the districts and priorities', () async {
    await controller.load();

    expect(controller.data!.districts, isNotEmpty);
    expect(controller.data!.priorities, ['LOW', 'MODERATE', 'HIGH', 'CRITICAL']);
  });

  test('a failed load keeps the reason and can be retried', () async {
    api.failNetwork = true;
    await controller.load();
    expect(controller.data, isNull);
    expect(controller.loadError!.isConnectionDown, isTrue);

    api.failNetwork = false;
    await controller.load();

    expect(controller.data, isNotNull);
    expect(controller.loadError, isNull);
  });

  test('submit creates a pending request and trims the text', () async {
    final result = await controller.submit(
      districtId: 1,
      requesterName: '  Ama ',
      requesterPhone: ' 0771111111 ',
      gpsLat: 6.9,
      gpsLng: 79.8,
      description: ' Need a boat ',
      priority: 'HIGH',
    );

    expect(result!.message, 'Rescue request submitted.');
    final created = (await api.getRescueRequest(result.id!)).request;
    expect([created.requesterName, created.requesterPhone, created.description], ['Ama', '0771111111', 'Need a boat']);
    expect([created.status, created.priority, created.hasLocation], ['PENDING', 'HIGH', true]);
  });

  test('a position is optional', () async {
    final result = await controller.submit(
      districtId: 1,
      requesterName: 'Ama',
      requesterPhone: '1',
      description: 'd',
      priority: 'LOW',
    );

    expect((await api.getRescueRequest(result!.id!)).request.hasLocation, isFalse);
  });

  test('a refused submit returns null and keeps the field errors', () async {
    final result = await controller.submit(
      districtId: 1,
      requesterName: ' ',
      requesterPhone: '',
      gpsLat: 120,
      description: '',
      priority: '',
    );

    expect(result, isNull);
    expect(
      controller.saveError!.fieldErrors.keys,
      containsAll(['requesterName', 'requesterPhone', 'gpsLat', 'description']),
    );
    controller.clearSaveError();
    expect(controller.saveError, isNull);
  });

  test('an invalid priority is a rule violation without a field', () async {
    final result = await controller.submit(
      districtId: 1,
      requesterName: 'a',
      requesterPhone: '1',
      description: 'd',
      priority: 'URGENT',
    );

    expect(result, isNull);
    expect(controller.saveError!.detail, 'Invalid priority: URGENT');
    expect(controller.saveError!.fieldErrors, isEmpty);
  });

  test('a second submit while one runs is ignored', () async {
    api = FakeOperationsApi(latency: const Duration(milliseconds: 20));
    controller = RescueFormController(() => api);

    final first = controller.submit(
      districtId: 1,
      requesterName: 'A',
      requesterPhone: '1',
      description: 'd',
      priority: 'LOW',
    );
    final second = await controller.submit(
      districtId: 1,
      requesterName: 'B',
      requesterPhone: '1',
      description: 'd',
      priority: 'LOW',
    );
    await first;

    expect(second, isNull);
    final names = (await api.listRescueRequests()).requests.map((r) => r.requesterName);
    expect(names, contains('A'));
    expect(names, isNot(contains('B')));
  });
}
