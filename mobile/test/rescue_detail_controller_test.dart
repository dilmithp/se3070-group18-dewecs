import 'package:dewecs_mobile/api/fake_operations_api.dart';
import 'package:dewecs_mobile/state/rescue_detail_controller.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  late FakeOperationsApi api;

  RescueDetailController controllerFor(int id) => RescueDetailController(() => api, id);

  setUp(() => api = FakeOperationsApi(latency: Duration.zero));

  test('load fetches the request and the available teams', () async {
    final controller = controllerFor(1);

    await controller.load();

    expect(controller.detail!.request.requesterName, 'Nimal Perera');
    expect(controller.detail!.availableTeams.map((t) => t.name), ['Alpha Team', 'Bravo Team']);
    expect(controller.loadError, isNull);
  });

  test('an unknown request leaves no detail and a 404 error', () async {
    final controller = controllerFor(99);

    await controller.load();

    expect(controller.detail, isNull);
    expect(controller.loadError!.detail, 'Rescue request not found: 99');
  });

  test('assign returns the message, shows the team and removes it from the available ones', () async {
    final controller = controllerFor(1);
    await controller.load();

    final message = await controller.assign(2);

    expect(message, 'Rescue team assigned.');
    expect(controller.detail!.request.status, 'ASSIGNED');
    expect(controller.detail!.request.assignedTeamName, 'Bravo Team');
    expect(controller.detail!.availableTeams.map((t) => t.name), ['Alpha Team']);
  });

  test('complete and cancel follow the status rules', () async {
    final assigned = controllerFor(2);
    await assigned.load();
    final pending = controllerFor(3);
    await pending.load();

    final tooEarly = await pending.complete();
    final done = await assigned.complete();
    final cancelled = await pending.cancel();

    expect(tooEarly, isNull);
    expect(pending.actionError, isNull); // replaced by the successful cancel below
    expect(done, 'Rescue request completed.');
    expect(assigned.detail!.request.status, 'COMPLETED');
    expect(cancelled, 'Rescue request cancelled.');
    expect(pending.detail!.request.status, 'CANCELLED');
  });

  test('a refusal keeps its reason', () async {
    final controller = controllerFor(3);
    await controller.load();

    final message = await controller.complete();

    expect(message, isNull);
    expect(controller.actionError!.detail, 'Only assigned requests can be completed.');
    controller.dismissActionError();
    expect(controller.actionError, isNull);
  });

  test('a refusal also refreshes a screen that was out of date', () async {
    final controller = controllerFor(1);
    await controller.load();
    await api.assignTeam(1, 1); // someone else assigned a team

    await controller.assign(2);

    expect(controller.actionError!.detail, 'Only pending requests can be assigned.');
    expect(controller.detail!.request.status, 'ASSIGNED');
    expect(controller.detail!.request.assignedTeamName, 'Alpha Team');
  });

  test('a connection lost during an action keeps the old detail', () async {
    final controller = controllerFor(1);
    await controller.load();
    api.failNetwork = true;

    final message = await controller.cancel();

    expect(message, isNull);
    expect(controller.actionError!.isConnectionDown, isTrue);
    expect(controller.detail!.request.status, 'PENDING');
  });

  test('a second action while one runs is ignored', () async {
    api = FakeOperationsApi(latency: const Duration(milliseconds: 20));
    final controller = controllerFor(1);
    await controller.load();

    final first = controller.assign(1);
    final second = await controller.cancel();
    await first;

    expect(second, isNull);
    expect(controller.detail!.request.status, 'ASSIGNED');
  });

  test('disposing while a call is running does not throw', () async {
    api = FakeOperationsApi(latency: const Duration(milliseconds: 20));
    final controller = controllerFor(1);

    final pending = controller.load();
    controller.dispose();

    await pending;
  });
}
