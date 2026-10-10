import 'package:dewecs_mobile/api/fake_operations_api.dart';
import 'package:dewecs_mobile/state/shelters_controller.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late FakeOperationsApi api;
  late SheltersController controller;

  setUp(() {
    api = FakeOperationsApi(latency: Duration.zero);
    controller = SheltersController(() => api);
  });

  test('starts empty and not loaded', () {
    expect(controller.loaded, isFalse);
    expect(controller.shelters, isEmpty);
    expect(controller.hasFilter, isFalse);
  });

  test('refresh loads the shelters and the filter values', () async {
    await controller.refresh();

    expect(controller.loaded, isTrue);
    expect(controller.shelters, hasLength(4));
    expect(controller.districts, isNotEmpty);
    expect(controller.statuses, ['OPEN', 'FULL', 'CLOSED']);
    expect(controller.error, isNull);
  });

  test('setFilters asks the server with the filters and clearing them shows everything', () async {
    await controller.refresh();

    await controller.setFilters(status: 'FULL');
    expect(controller.shelters.map((s) => s.name), ['Temple Hall']);
    expect(controller.hasFilter, isTrue);
    await controller.setFilters(status: 'OPEN', districtId: 3);
    expect(controller.shelters.map((s) => s.name), ['Jaffna Community Centre']);
    await controller.setFilters();
    expect(controller.shelters, hasLength(4));
    expect(controller.hasFilter, isFalse);
  });

  test('a failed refresh keeps the last list and remembers why', () async {
    await controller.refresh();
    api.failNetwork = true;

    await controller.refresh();

    expect(controller.shelters, hasLength(4));
    expect(controller.offline, isTrue);
    expect(controller.error, isNotNull);
    api.failNetwork = false;
    await controller.refresh();
    expect(controller.error, isNull);
  });

  test('a first refresh that fails leaves it not loaded', () async {
    api.failStatus = 500;

    await controller.refresh();

    expect(controller.loaded, isFalse);
    expect(controller.offline, isFalse);
    expect(controller.error!.status, 500);
  });

  test('changing the filter while a load runs is not lost', () async {
    final slow = FakeOperationsApi(latency: const Duration(milliseconds: 20));
    final c = SheltersController(() => slow);

    final first = c.refresh();
    await c.setFilters(status: 'CLOSED');
    await first;

    expect(c.shelters.map((s) => s.status).toSet(), {'CLOSED'});
  });

  test('clearing local data forgets the list and the filters', () async {
    await controller.setFilters(status: 'OPEN');

    await controller.clearLocalData();

    expect(controller.loaded, isFalse);
    expect(controller.shelters, isEmpty);
    expect(controller.hasFilter, isFalse);
  });

  test('listeners hear about loading and the result', () async {
    var notifications = 0;
    controller.addListener(() => notifications++);

    await controller.refresh();

    expect(notifications, greaterThanOrEqualTo(2));
  });
}
