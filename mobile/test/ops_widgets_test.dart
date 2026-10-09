import 'package:dewecs_mobile/config/theme.dart';
import 'package:dewecs_mobile/models/rescue_request.dart';
import 'package:dewecs_mobile/models/shelter.dart';
import 'package:dewecs_mobile/widgets/occupancy_bar.dart';
import 'package:dewecs_mobile/widgets/ops_chips.dart';
import 'package:dewecs_mobile/widgets/rescue_card.dart';
import 'package:dewecs_mobile/widgets/shelter_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

Shelter shelter({String status = 'OPEN', int occupancy = 20, int capacity = 50}) => Shelter(
  id: 1,
  name: 'Town Hall Shelter',
  districtId: 1,
  districtName: 'Colombo',
  organizationName: 'Red Cross',
  capacity: capacity,
  currentOccupancy: occupancy,
  status: status,
);

RescueRequest request({String status = 'PENDING', String priority = 'CRITICAL', String? team}) => RescueRequest(
  id: 3,
  requesterName: 'Nimal Perera',
  requesterPhone: '0771234567',
  districtId: 1,
  districtName: 'Colombo',
  gpsLat: 6.9,
  gpsLng: 79.8,
  description: 'Family stuck on the roof',
  priority: priority,
  status: status,
  assignedTeamName: team,
  submittedAt: DateTime.utc(2026, 10, 9, 10),
  assignedAt: null,
  completedAt: null,
);

Future<void> show(WidgetTester tester, Widget child, {Brightness brightness = Brightness.light}) async {
  await tester.pumpWidget(
    MaterialApp(
      theme: brightness == Brightness.dark ? AppTheme.dark() : AppTheme.light(),
      home: Scaffold(body: SingleChildScrollView(child: child)),
    ),
  );
}

void main() {
  group('chips', () {
    testWidgets('shelter statuses read as words', (tester) async {
      await show(
        tester,
        Builder(
          builder: (context) => Column(
            children: [
              for (final s in ['OPEN', 'FULL', 'CLOSED']) OpsChips.shelterStatus(context, s),
            ],
          ),
        ),
      );

      expect(find.text('Open'), findsOneWidget);
      expect(find.text('Full'), findsOneWidget);
      expect(find.text('Closed'), findsOneWidget);
    });

    testWidgets('rescue statuses and priorities read as words, unknown values as received', (tester) async {
      await show(
        tester,
        Builder(
          builder: (context) => Column(
            children: [
              for (final s in ['PENDING', 'ASSIGNED', 'COMPLETED', 'CANCELLED', 'SOMETHING_NEW'])
                OpsChips.rescueStatus(context, s),
              for (final p in ['LOW', 'MODERATE', 'HIGH', 'CRITICAL']) OpsChips.priority(context, p),
            ],
          ),
        ),
      );

      for (final label in [
        'Waiting for a team',
        'Team assigned',
        'Completed',
        'Cancelled',
        'SOMETHING_NEW',
        'Low priority',
        'Moderate priority',
        'High priority',
        'Critical priority',
      ]) {
        expect(find.text(label), findsOneWidget, reason: label);
      }
    });

    testWidgets('a finished request is marked by its status, an open one by its priority', (tester) async {
      late BuildContext captured;
      await show(
        tester,
        Builder(
          builder: (context) {
            captured = context;
            return const SizedBox();
          },
        ),
      );

      final open = OpsChips.rescueAccent(captured, 'PENDING', 'CRITICAL');
      final done = OpsChips.rescueAccent(captured, 'COMPLETED', 'CRITICAL');

      expect(open, OpsChips.priority(captured, 'CRITICAL').foreground);
      expect(done, OpsChips.rescueStatus(captured, 'COMPLETED').foreground);
      expect(open, isNot(done));
    });
  });

  group('occupancy bar', () {
    testWidgets('says how many places are taken and free', (tester) async {
      await show(tester, const OccupancyBar(occupancy: 20, capacity: 50));

      expect(find.text('20 of 50 places taken'), findsOneWidget);
      expect(find.text('30 places free'), findsOneWidget);
      expect(tester.widget<LinearProgressIndicator>(find.byType(LinearProgressIndicator)).value, 0.4);
    });

    testWidgets('a full shelter says no places are free and the bar is full', (tester) async {
      await show(tester, const OccupancyBar(occupancy: 10, capacity: 10));

      expect(find.text('No places free'), findsOneWidget);
      expect(tester.widget<LinearProgressIndicator>(find.byType(LinearProgressIndicator)).value, 1.0);
    });

    testWidgets('one place left is singular and zero capacity does not divide by zero', (tester) async {
      await show(
        tester,
        const Column(children: [OccupancyBar(occupancy: 9, capacity: 10), OccupancyBar(occupancy: 0, capacity: 0)]),
      );

      expect(find.text('1 place free'), findsOneWidget);
      expect(tester.takeException(), isNull);
    });

    testWidgets('exposes one readable label to screen readers', (tester) async {
      final handle = tester.ensureSemantics();
      await show(tester, const OccupancyBar(occupancy: 20, capacity: 50));

      expect(find.bySemanticsLabel('20 of 50 places taken. 30 places free'), findsOneWidget);
      handle.dispose();
    });
  });

  group('cards', () {
    testWidgets('a shelter card shows name, status, occupancy, district and organization, and can be tapped', (
      tester,
    ) async {
      var taps = 0;
      await show(tester, ShelterCard(shelter: shelter(), onTap: () => taps++));

      expect(find.text('Town Hall Shelter'), findsOneWidget);
      expect(find.text('Open'), findsOneWidget);
      expect(find.text('20 of 50 places taken'), findsOneWidget);
      expect(find.text('Colombo - Red Cross'), findsOneWidget);
      await tester.tap(find.byType(ShelterCard));
      expect(taps, 1);
    });

    testWidgets('a rescue card shows requester, status, priority, team and time', (tester) async {
      var taps = 0;
      await show(
        tester,
        RescueCard(
          request: request(team: 'Alpha Team', status: 'ASSIGNED'),
          onTap: () => taps++,
        ),
      );

      expect(find.text('Nimal Perera'), findsOneWidget);
      expect(find.text('Team assigned'), findsOneWidget);
      expect(find.text('Critical priority'), findsOneWidget);
      expect(find.text('Team: Alpha Team'), findsOneWidget);
      expect(find.text('Family stuck on the roof'), findsOneWidget);
      expect(find.text('Colombo - 9 Oct 2026, 10:00'), findsOneWidget);
      await tester.tap(find.byType(RescueCard));
      expect(taps, 1);
    });

    testWidgets('a request without a team says so', (tester) async {
      await show(tester, RescueCard(request: request(), onTap: () {}));

      expect(find.text('No team assigned yet'), findsOneWidget);
      expect(find.text('Waiting for a team'), findsOneWidget);
    });

    testWidgets('a long name gets the whole title width and the badge drops below it', (tester) async {
      tester.view.physicalSize = const Size(320, 800);
      tester.view.devicePixelRatio = 1.0;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      const longName = 'Northern Coastal Region Community Relief Shelter';
      await show(
        tester,
        ShelterCard(
          shelter: Shelter(
            id: 1,
            name: longName,
            districtId: 1,
            districtName: 'Colombo',
            organizationName: 'Red Cross',
            capacity: 50,
            currentOccupancy: 20,
            status: 'OPEN',
          ),
          onTap: () {},
        ),
      );

      final name = tester.getRect(find.text(longName));
      final badge = tester.getRect(find.text('Open'));
      expect(name.width, greaterThan(150));
      expect(badge.top, greaterThanOrEqualTo(name.bottom - 1));
    });

    testWidgets('cards fit a narrow phone with large text in dark mode', (tester) async {
      tester.view.physicalSize = const Size(320, 800);
      tester.view.devicePixelRatio = 1.0;
      tester.platformDispatcher.textScaleFactorTestValue = 1.6;
      addTearDown(tester.view.resetPhysicalSize);
      addTearDown(tester.view.resetDevicePixelRatio);
      addTearDown(tester.platformDispatcher.clearAllTestValues);
      await show(
        tester,
        Column(
          children: [
            ShelterCard(
              shelter: shelter(status: 'FULL', occupancy: 50),
              onTap: () {},
            ),
            RescueCard(
              request: request(team: 'Alpha Team Colombo North', status: 'ASSIGNED'),
              onTap: () {},
            ),
          ],
        ),
        brightness: Brightness.dark,
      );

      expect(tester.takeException(), isNull);
    });
  });
}
