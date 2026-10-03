import 'package:dewecs_mobile/api/api_exception.dart';
import 'package:dewecs_mobile/api/fake_operations_api.dart';
import 'package:flutter_test/flutter_test.dart';

FakeOperationsApi newFake() => FakeOperationsApi(latency: Duration.zero);

Future<ApiException?> failure(Future<Object?> call) =>
    call.then<ApiException?>((_) => null, onError: (e) => e as ApiException);

void main() {
  group('shelters', () {
    test('the seeded list has open, full and closed shelters and the filter values', () async {
      final list = await newFake().listShelters();

      expect(list.shelters.map((s) => s.status).toSet(), {'OPEN', 'FULL', 'CLOSED'});
      expect(list.statuses, ['OPEN', 'FULL', 'CLOSED']);
      expect(list.districts, isNotEmpty);
    });

    test('filters by status and district together, and ignores an unknown status', () async {
      final api = newFake();

      expect((await api.listShelters(status: 'FULL')).shelters.map((s) => s.name), ['Temple Hall']);
      expect((await api.listShelters(status: 'open', districtId: 1)).shelters.single.name, 'Town Hall Shelter');
      expect((await api.listShelters(status: 'open', districtId: 2)).shelters, isEmpty);
      expect((await api.listShelters(status: 'NOPE')).shelters, hasLength(4));
    });

    test('create starts OPEN and empty; blank name and zero capacity give field errors', () async {
      final api = newFake();

      final result = await api.createShelter(districtId: 1, organizationId: 2, name: ' Hall ', capacity: 20);
      final detail = await api.getShelter(result.id!);
      final error = await failure(api.createShelter(districtId: 1, organizationId: 2, name: ' ', capacity: 0));

      expect(result.message, 'Shelter created.');
      expect(detail.shelter.name, 'Hall');
      expect([detail.shelter.status, detail.shelter.currentOccupancy], ['OPEN', 0]);
      expect(error!.status, 400);
      expect(error.fieldErrors.keys, containsAll(['name', 'capacity']));
    });

    test('update cannot lower capacity below the occupancy and re-evaluates FULL', () async {
      final api = newFake();

      final tooLow = await failure(api.updateShelter(1, districtId: 1, organizationId: 2, name: 'x', capacity: 2));
      await api.updateShelter(1, districtId: 1, organizationId: 2, name: 'Renamed', capacity: 3);
      final full = (await api.getShelter(1)).shelter;
      await api.updateShelter(1, districtId: 1, organizationId: 2, name: 'Renamed', capacity: 9);

      expect(tooLow!.detail, 'Capacity cannot be lower than the current occupancy (3).');
      expect([full.name, full.status], ['Renamed', 'FULL']);
      expect((await api.getShelter(1)).shelter.status, 'OPEN');
    });

    test('check-in fills the shelter and refuses when it is full or closed', () async {
      final api = newFake();

      final full = await failure(api.checkIn(2, fullName: 'A', nic: '1'));
      final closed = await failure(api.checkIn(3, fullName: 'A', nic: '1'));
      await api.closeShelter(1);
      final nowClosed = await failure(api.checkIn(1, fullName: 'A', nic: '1'));
      await api.reopenShelter(1);
      for (var i = 0; i < 47; i++) {
        await api.checkIn(1, fullName: 'Person $i', nic: '$i');
      }

      expect(full!.detail, 'Cannot check in: shelter is at full capacity.');
      expect(closed!.detail, 'Cannot check in: shelter is closed.');
      expect(nowClosed!.detail, 'Cannot check in: shelter is closed.');
      expect((await api.getShelter(1)).shelter.status, 'FULL');
    });

    test('check-in needs both name and NIC', () async {
      final error = await failure(newFake().checkIn(1, fullName: ' ', nic: '1'));

      expect(error!.detail, 'Enter both a name and NIC to check in an occupant.');
    });

    test('check-out frees a place, reopens a FULL shelter and cannot be repeated', () async {
      final api = newFake();
      final occupant = (await api.getShelter(2)).occupants.first;

      await api.checkOut(2, occupant.id);
      final after = await api.getShelter(2);
      final again = await failure(api.checkOut(2, occupant.id));
      final other = await failure(api.checkOut(1, (await api.getShelter(2)).occupants.first.id));
      final unknown = await failure(api.checkOut(1, 9999));

      expect(after.shelter.status, 'OPEN');
      expect(after.shelter.currentOccupancy, 3);
      expect(after.occupants.map((o) => o.id), isNot(contains(occupant.id)));
      expect(again!.detail, 'Occupant has already checked out.');
      expect(other!.detail, 'Occupant does not belong to this shelter.');
      expect(unknown!.status, 404);
    });

    test('close keeps the occupants; reopen only works on a closed shelter', () async {
      final api = newFake();

      await api.closeShelter(1);
      final closed = await api.getShelter(1);
      final notClosed = await failure(api.reopenShelter(4));
      await api.reopenShelter(1);

      expect([closed.shelter.status, closed.occupants.length], ['CLOSED', 3]);
      expect(notClosed!.detail, 'Only a closed shelter can be reopened.');
      expect((await api.getShelter(1)).shelter.status, 'OPEN');
    });

    test('an unknown shelter is a 404', () async {
      final error = await failure(newFake().getShelter(99));

      expect([error!.status, error.detail], [404, 'Shelter not found: 99']);
    });
  });

  group('rescue requests', () {
    test('the seeded list has every status and filters by status, priority and district', () async {
      final api = newFake();

      expect((await api.listRescueRequests()).requests.map((r) => r.status).toSet(), {
        'PENDING',
        'ASSIGNED',
        'COMPLETED',
      });
      expect((await api.listRescueRequests(status: 'PENDING', priority: 'CRITICAL')).requests.single.id, 1);
      expect((await api.listRescueRequests(districtId: 2)).requests.single.assignedTeamName, 'Charlie Team');
    });

    test('only available teams are offered, and assigning dispatches the team', () async {
      final api = newFake();

      final before = (await api.getRescueRequest(1)).availableTeams.map((t) => t.name);
      await api.assignTeam(1, 1);
      final after = await api.getRescueRequest(3);
      final request = (await api.getRescueRequest(1)).request;

      expect(before, ['Alpha Team', 'Bravo Team']);
      expect(after.availableTeams.map((t) => t.name), ['Bravo Team']);
      expect([request.status, request.assignedTeamName], ['ASSIGNED', 'Alpha Team']);
      expect(request.assignedAt, isNotNull);
    });

    test('assign refuses a request that is not pending and a team that is not available', () async {
      final api = newFake();

      final notPending = await failure(api.assignTeam(2, 1));
      final busy = await failure(api.assignTeam(1, 3));
      final unknownTeam = await failure(api.assignTeam(1, 99));

      expect(notPending!.detail, 'Only pending requests can be assigned.');
      expect(busy!.detail, 'Team is not available for assignment.');
      expect(unknownTeam!.status, 404);
    });

    test('complete and cancel free the team and follow the status rules', () async {
      final api = newFake();

      final notAssigned = await failure(api.completeRescueRequest(1));
      await api.completeRescueRequest(2);
      final done = (await api.getRescueRequest(2)).request;
      final cannotCancel = await failure(api.cancelRescueRequest(2));
      await api.assignTeam(1, 1);
      await api.cancelRescueRequest(1);

      expect(notAssigned!.detail, 'Only assigned requests can be completed.');
      expect(done.status, 'COMPLETED');
      expect(done.completedAt, isNotNull);
      expect(cannotCancel!.detail, 'Only pending or assigned requests can be cancelled.');
      expect((await api.getRescueRequest(1)).request.status, 'CANCELLED');
      expect((await api.getRescueRequest(1)).availableTeams.map((t) => t.name), contains('Alpha Team'));
      expect((await api.getRescueRequest(1)).availableTeams.map((t) => t.name), contains('Charlie Team'));
    });

    test('submit creates a PENDING request and validates each field like the server', () async {
      final api = newFake();

      final result = await api.submitRescueRequest(
        districtId: 1,
        requesterName: 'Ama',
        requesterPhone: '0771111111',
        description: 'Need a boat',
        priority: 'high',
      );
      final created = (await api.getRescueRequest(result.id!)).request;
      final empty = await failure(
        api.submitRescueRequest(
          districtId: 1,
          requesterName: '',
          requesterPhone: '',
          gpsLat: 120,
          gpsLng: 200,
          description: '',
          priority: '',
        ),
      );
      final badPriority = await failure(
        api.submitRescueRequest(
          districtId: 1,
          requesterName: 'a',
          requesterPhone: '1',
          description: 'd',
          priority: 'URGENT',
        ),
      );

      expect([created.status, created.priority, created.hasLocation], ['PENDING', 'HIGH', false]);
      expect(
        empty!.fieldErrors.keys,
        containsAll(['requesterName', 'requesterPhone', 'gpsLat', 'gpsLng', 'description', 'priority']),
      );
      expect(badPriority!.detail, 'Invalid priority: URGENT');
    });
  });

  group('switches', () {
    test('a forced network failure and a forced 500 match the other fake server', () async {
      final api = newFake()..failNetwork = true;
      final network = await failure(api.listShelters());
      api
        ..failNetwork = false
        ..failStatus = 500;
      final server = await failure(api.listRescueRequests());

      expect(network!.isConnectionDown, isTrue);
      expect([server!.status, server.retryable], [500, true]);
    });

    test('resetServer brings back the seed data', () async {
      final api = newFake();
      await api.closeShelter(1);

      api.resetServer();

      expect((await api.getShelter(1)).shelter.status, 'OPEN');
    });
  });
}
