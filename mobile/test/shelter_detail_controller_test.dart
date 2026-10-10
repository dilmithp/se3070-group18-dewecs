import 'package:dewecs_mobile/api/fake_operations_api.dart';
import 'package:dewecs_mobile/state/shelter_detail_controller.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late FakeOperationsApi api;

  ShelterDetailController controllerFor(int id) => ShelterDetailController(() => api, id);

  setUp(() => api = FakeOperationsApi(latency: Duration.zero));

  test('load fetches the shelter and its occupants', () async {
    final controller = controllerFor(1);

    await controller.load();

    expect(controller.detail!.shelter.name, 'Town Hall Shelter');
    expect(controller.detail!.occupants, hasLength(3));
    expect(controller.loadError, isNull);
  });

  test('an unknown shelter leaves no detail and a 404 error', () async {
    final controller = controllerFor(99);

    await controller.load();

    expect(controller.detail, isNull);
    expect(controller.loadError!.status, 404);
  });

  test('check-in returns the server message and shows the new occupant', () async {
    final controller = controllerFor(1);
    await controller.load();

    final message = await controller.checkIn(fullName: ' Ama ', nic: ' 1234 ');

    expect(message, 'Occupant checked in.');
    expect(controller.detail!.shelter.currentOccupancy, 4);
    expect(controller.detail!.occupants.last.fullName, 'Ama');
    expect(controller.detail!.occupants.last.nic, '1234');
    expect(controller.actionError, isNull);
  });

  test('a refused check-in returns null and keeps the reason', () async {
    final controller = controllerFor(3);
    await controller.load();

    final message = await controller.checkIn(fullName: 'Ama', nic: '1');

    expect(message, isNull);
    expect(controller.actionError!.detail, 'Cannot check in: shelter is closed.');
    expect(controller.detail!.shelter.currentOccupancy, 0);
    controller.dismissActionError();
    expect(controller.actionError, isNull);
  });

  test('a refusal also refreshes a screen that was out of date', () async {
    final controller = controllerFor(1);
    await controller.load();
    await api.closeShelter(1);

    await controller.checkIn(fullName: 'A', nic: '1');

    expect(controller.actionError!.detail, 'Cannot check in: shelter is closed.');
    expect(controller.detail!.shelter.status, 'CLOSED');
  });

  test('check-out frees the place; close and reopen change the status', () async {
    final controller = controllerFor(2);
    await controller.load();

    await controller.checkOut(controller.detail!.occupants.first.id);
    expect(controller.detail!.shelter.status, 'OPEN');
    expect(controller.detail!.occupants, hasLength(3));
    await controller.close();
    expect(controller.detail!.shelter.status, 'CLOSED');
    await controller.reopen();
    expect(controller.detail!.shelter.status, 'OPEN');
  });

  test('a new action clears the previous error', () async {
    final controller = controllerFor(1);
    await controller.load();
    await controller.reopen();
    expect(controller.actionError!.detail, 'Only a closed shelter can be reopened.');

    await controller.close();

    expect(controller.actionError, isNull);
  });

  test('a failed action because of the connection keeps the old detail', () async {
    final controller = controllerFor(1);
    await controller.load();
    api.failNetwork = true;

    final message = await controller.close();

    expect(message, isNull);
    expect(controller.actionError!.isConnectionDown, isTrue);
    expect(controller.detail!.shelter.status, 'OPEN');
  });

  test('a second action while one runs is ignored', () async {
    api = FakeOperationsApi(latency: const Duration(milliseconds: 20));
    final controller = controllerFor(1);
    await controller.load();

    final first = controller.checkIn(fullName: 'A', nic: '1');
    final second = await controller.checkIn(fullName: 'B', nic: '2');
    await first;

    expect(second, isNull);
    expect(controller.detail!.occupants.map((o) => o.fullName), contains('A'));
    expect(controller.detail!.occupants.map((o) => o.fullName), isNot(contains('B')));
  });

  test('disposing while a call is running does not throw', () async {
    api = FakeOperationsApi(latency: const Duration(milliseconds: 20));
    final controller = controllerFor(1);

    final pending = controller.load();
    controller.dispose();

    await pending;
  });
}
