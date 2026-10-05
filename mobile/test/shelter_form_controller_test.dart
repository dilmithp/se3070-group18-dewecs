import 'package:dewecs_mobile/api/fake_operations_api.dart';
import 'package:dewecs_mobile/state/shelter_form_controller.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late FakeOperationsApi api;
  late ShelterFormController controller;

  setUp(() {
    api = FakeOperationsApi(latency: Duration.zero);
    controller = ShelterFormController(() => api);
  });

  test('load fetches the districts and organizations', () async {
    await controller.load();

    expect(controller.data!.districts, isNotEmpty);
    expect(controller.data!.organizations.map((o) => o.name), contains('Red Cross'));
    expect(controller.loadError, isNull);
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

  test('create returns the new shelter and trims the name', () async {
    final result = await controller.create(districtId: 1, organizationId: 2, name: '  Hall  ', capacity: 25);

    expect(result!.message, 'Shelter created.');
    final created = (await api.getShelter(result.id!)).shelter;
    expect([created.name, created.capacity, created.status], ['Hall', 25, 'OPEN']);
  });

  test('a refused create returns null and keeps the field errors', () async {
    final result = await controller.create(districtId: 1, organizationId: 2, name: ' ', capacity: 0);

    expect(result, isNull);
    expect(controller.saveError!.fieldErrors.keys, containsAll(['name', 'capacity']));
    controller.clearSaveError();
    expect(controller.saveError, isNull);
  });

  test('update changes the name and capacity', () async {
    final result = await controller.update(1, districtId: 1, organizationId: 2, name: 'Renamed', capacity: 60);

    expect(result!.message, 'Shelter updated.');
    final shelter = (await api.getShelter(1)).shelter;
    expect([shelter.name, shelter.capacity], ['Renamed', 60]);
  });

  test('update below the occupancy is refused with the server rule', () async {
    final result = await controller.update(1, districtId: 1, organizationId: 2, name: 'x', capacity: 1);

    expect(result, isNull);
    expect(controller.saveError!.detail, 'Capacity cannot be lower than the current occupancy (3).');
    expect(controller.saveError!.fieldErrors, isEmpty);
  });

  test('a second save while one runs is ignored', () async {
    api = FakeOperationsApi(latency: const Duration(milliseconds: 20));
    controller = ShelterFormController(() => api);

    final first = controller.create(districtId: 1, organizationId: 2, name: 'A', capacity: 5);
    final second = await controller.create(districtId: 1, organizationId: 2, name: 'B', capacity: 5);
    await first;

    expect(second, isNull);
    expect((await api.listShelters()).shelters.map((s) => s.name), contains('A'));
    expect((await api.listShelters()).shelters.map((s) => s.name), isNot(contains('B')));
  });
}
