import 'dart:convert';
import 'dart:io';

import 'package:dewecs_mobile/models/action_result.dart';
import 'package:dewecs_mobile/models/rescue_request.dart';
import 'package:dewecs_mobile/models/shelter.dart';
import 'package:flutter_test/flutter_test.dart';

Map<String, dynamic> fixture(String name) =>
    jsonDecode(File('test/fixtures/$name').readAsStringSync()) as Map<String, dynamic>;

void main() {
  group('shelter models', () {
    test('the list keeps shelters, filter values and raw statuses', () {
      final list = ShelterList.fromJson(fixture('shelter-list.json'));

      expect(list.shelters.map((s) => s.name), ['Town Hall Shelter', 'Temple Hall']);
      expect(list.districts.map((d) => d.name), ['Colombo', 'Galle']);
      expect(list.statuses, ['OPEN', 'FULL', 'CLOSED']);
      final first = list.shelters.first;
      expect(first.organizationName, 'Red Cross');
      expect(first.freePlaces, 30);
      expect(first.fill, 0.4);
      expect(list.shelters.last.isFull, isTrue);
    });

    test('occupancy maths never goes below zero or above one', () {
      const over = Shelter(
          id: 1, name: 'x', districtId: 1, districtName: 'd', organizationName: 'o', capacity: 5,
          currentOccupancy: 9, status: 'CLOSED');
      const none = Shelter(
          id: 1, name: 'x', districtId: 1, districtName: 'd', organizationName: 'o', capacity: 0,
          currentOccupancy: 0, status: 'OPEN');

      expect(over.freePlaces, 0);
      expect(over.fill, 1.0);
      expect(over.isClosed, isTrue);
      expect(none.fill, 0.0);
    });

    test('the detail reads the current occupants and their check-in time', () {
      final detail = ShelterDetail.fromJson(fixture('shelter-detail.json'));

      expect(detail.shelter.currentOccupancy, 2);
      expect(detail.occupants.map((o) => o.fullName), ['Amaya Silva', 'Kasun Perera']);
      expect(detail.occupants.last.checkInTime, DateTime.utc(2026, 10, 9, 9, 40, 30, 250));
    });

    test('the form data lists districts and organizations', () {
      final form = ShelterFormData.fromJson(fixture('shelter-form.json'));

      expect(form.districts, hasLength(2));
      expect(form.organizations.map((o) => o.name), ['Red Cross', 'Disaster Management Centre']);
      expect(form.organizations.first.type, 'NGO');
    });
  });

  group('rescue models', () {
    test('the list reads optional GPS and the assigned team', () {
      final list = RescueRequestList.fromJson(fixture('rescue-list.json'));

      expect(list.requests, hasLength(2));
      expect(list.priorities, ['LOW', 'MODERATE', 'HIGH', 'CRITICAL']);
      final pending = list.requests.first;
      expect(pending.hasLocation, isTrue);
      expect(pending.gpsLat, 6.9271234);
      expect(pending.assignedTeamName, isNull);
      final assigned = list.requests.last;
      expect(assigned.hasLocation, isFalse);
      expect(assigned.assignedTeamName, 'Alpha Team');
      expect(assigned.assignedAt, DateTime.utc(2026, 10, 9, 8));
    });

    test('which actions are allowed follows the status', () {
      final requests = RescueRequestList.fromJson(fixture('rescue-list.json')).requests;
      final pending = requests.first;
      final assigned = requests.last;

      expect([pending.canAssign, pending.canComplete, pending.canCancel], [true, false, true]);
      expect([assigned.canAssign, assigned.canComplete, assigned.canCancel], [false, true, true]);
    });

    test('the detail reads the available teams with their district', () {
      final detail = RescueRequestDetail.fromJson(fixture('rescue-detail.json'));

      expect(detail.request.requesterName, 'Nimal Perera');
      expect(detail.availableTeams.single.name, 'Alpha Team');
      expect(detail.availableTeams.single.districtName, 'Colombo');
    });

    test('the form data lists districts and priorities', () {
      final form = RescueRequestFormData.fromJson(fixture('rescue-form.json'));

      expect(form.districts, hasLength(2));
      expect(form.priorities.last, 'CRITICAL');
    });
  });

  group('action result', () {
    test('takes the record id from the location path', () {
      final result = ActionResult.fromJson({'message': 'Shelter created.', 'location': '/shelters/7'});

      expect(result.message, 'Shelter created.');
      expect(result.id, 7);
    });

    test('has no id when the location does not end in a number', () {
      expect(ActionResult.fromJson({'message': 'x', 'location': '/shelters'}).id, isNull);
      expect(ActionResult.fromJson(const {}).message, '');
    });
  });
}
