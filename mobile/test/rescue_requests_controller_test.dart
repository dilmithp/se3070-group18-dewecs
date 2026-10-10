import 'package:dewecs_mobile/api/fake_operations_api.dart';
import 'package:dewecs_mobile/state/rescue_requests_controller.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late FakeOperationsApi api;
  late RescueRequestsController controller;

  setUp(() {
    api = FakeOperationsApi(latency: Duration.zero);
    controller = RescueRequestsController(() => api);
  });

  test('starts empty and not loaded', () {
    expect(controller.loaded, isFalse);
    expect(controller.requests, isEmpty);
    expect(controller.hasFilter, isFalse);
  });

  test('refresh loads the requests and what the filters offer', () async {
    await controller.refresh();

    expect(controller.loaded, isTrue);
    expect(controller.requests, hasLength(4));
    expect(controller.statuses, ['PENDING', 'ASSIGNED', 'COMPLETED', 'CANCELLED']);
    expect(controller.priorities, ['LOW', 'MODERATE', 'HIGH', 'CRITICAL']);
    expect(controller.districts, isNotEmpty);
  });

  test('the three filters combine and clearing them shows everything', () async {
    await controller.refresh();

    await controller.setFilters(status: 'PENDING');
    expect(controller.requests.map((r) => r.id), [1, 3]);
    await controller.setFilters(status: 'PENDING', priority: 'HIGH');
    expect(controller.requests.map((r) => r.id), [3]);
    await controller.setFilters(status: 'PENDING', priority: 'HIGH', districtId: 1);
    expect(controller.requests, isEmpty);
    expect(controller.hasFilter, isTrue);
    await controller.setFilters();
    expect(controller.requests, hasLength(4));
    expect(controller.hasFilter, isFalse);
  });

  test('a failed refresh keeps the last list and remembers why', () async {
    await controller.refresh();
    api.failNetwork = true;

    await controller.refresh();

    expect(controller.requests, hasLength(4));
    expect(controller.offline, isTrue);
    api.failNetwork = false;
    await controller.refresh();
    expect(controller.error, isNull);
  });

  test('a first refresh that fails leaves it not loaded with the status', () async {
    api.failStatus = 500;

    await controller.refresh();

    expect(controller.loaded, isFalse);
    expect(controller.offline, isFalse);
    expect(controller.error!.status, 500);
  });

  test('changing the filter while a load runs is not lost', () async {
    final slow = FakeOperationsApi(latency: const Duration(milliseconds: 20));
    final c = RescueRequestsController(() => slow);

    final first = c.refresh();
    await c.setFilters(priority: 'LOW');
    await first;

    expect(c.requests.map((r) => r.priority).toSet(), {'LOW'});
  });

  test('clearing local data forgets the list and the filters', () async {
    await controller.setFilters(status: 'ASSIGNED');

    await controller.clearLocalData();

    expect(controller.loaded, isFalse);
    expect(controller.requests, isEmpty);
    expect(controller.hasFilter, isFalse);
  });
}
