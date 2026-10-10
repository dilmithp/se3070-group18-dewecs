import 'package:dewecs_mobile/config/theme.dart';
import 'package:dewecs_mobile/models/shelter.dart';
import 'package:dewecs_mobile/screens/shelter_detail_screen.dart';
import 'package:dewecs_mobile/screens/shelter_list_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'test_helpers.dart';

const _stub = Shelter(
  id: 1,
  name: 'Town Hall Shelter',
  districtId: 1,
  districtName: 'Colombo',
  organizationName: 'Red Cross',
  capacity: 50,
  currentOccupancy: 3,
  status: 'OPEN',
);

Future<void> pumpDetail(
  WidgetTester tester,
  TestApp app, {
  int id = 1,
  Size size = const Size(500, 2200),
  double textScale = 1.0,
  Brightness brightness = Brightness.light,
}) async {
  tester.view.physicalSize = size;
  tester.view.devicePixelRatio = 1.0;
  tester.platformDispatcher.textScaleFactorTestValue = textScale;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  addTearDown(tester.platformDispatcher.clearAllTestValues);
  final stub = Shelter(
    id: id,
    name: _stub.name,
    districtId: 1,
    districtName: 'Colombo',
    organizationName: 'Red Cross',
    capacity: 50,
    currentOccupancy: 3,
    status: 'OPEN',
  );
  await tester.pumpWidget(
    app.dependencies.provide(
      child: MaterialApp(
        theme: brightness == Brightness.dark ? AppTheme.dark() : AppTheme.light(),
        home: ShelterDetailScreen(shelter: stub),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

Finder field(String label) => find.widgetWithText(TextFormField, label);

void main() {
  testWidgets('shows status, occupancy, district, organization and the current occupants', (tester) async {
    await pumpDetail(tester, TestApp());

    expect(find.text('Town Hall Shelter'), findsWidgets);
    expect(find.text('Open'), findsOneWidget);
    expect(find.text('3 of 50 places taken'), findsOneWidget);
    expect(find.text('6% full'), findsOneWidget);
    expect(find.text('Colombo'), findsOneWidget);
    expect(find.text('Red Cross'), findsOneWidget);
    expect(find.text('Current occupants (3)'), findsOneWidget);
    expect(find.text('Amaya Silva'), findsOneWidget);
    expect(find.text('200000000101'), findsOneWidget);
    expect(find.textContaining('Checked in'), findsWidgets);
  });

  testWidgets('checking someone in updates the list, clears the form and says so', (tester) async {
    final app = TestApp();
    await pumpDetail(tester, app);

    await enterField(tester, 'Full name', 'Nadeesha Gunawardena');
    await enterField(tester, 'NIC', '199812345678');
    await tester.tap(find.text('Check in'));
    await tester.pumpAndSettle();

    expect(find.text('Occupant checked in.'), findsOneWidget);
    expect(find.text('Nadeesha Gunawardena'), findsOneWidget);
    expect(find.text('Current occupants (4)'), findsOneWidget);
    expect(find.text('4 of 50 places taken'), findsOneWidget);
    expect(tester.widget<TextFormField>(field('Full name')).controller!.text, isEmpty);
  });

  testWidgets('empty fields show the server wording and send nothing', (tester) async {
    final app = TestApp();
    await pumpDetail(tester, app);
    final before = app.operations.callCount;

    await tester.tap(find.text('Check in'));
    await tester.pumpAndSettle();

    expect(find.text("Enter the occupant's full name"), findsOneWidget);
    expect(find.text("Enter the occupant's NIC"), findsOneWidget);
    expect(app.operations.callCount, before);
  });

  testWidgets('checking someone out asks first, then removes them', (tester) async {
    await pumpDetail(tester, TestApp());

    await tester.tap(find.widgetWithText(OutlinedButton, 'Check out').first);
    await tester.pumpAndSettle();
    expect(find.text('Check out Amaya Silva?'), findsOneWidget);
    await tester.tap(find.text('Cancel'));
    await tester.pumpAndSettle();
    expect(find.text('Current occupants (3)'), findsOneWidget);

    await tester.tap(find.widgetWithText(OutlinedButton, 'Check out').first);
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Check out'));
    await tester.pumpAndSettle();

    expect(find.text('Occupant checked out.'), findsOneWidget);
    expect(find.text('Current occupants (2)'), findsOneWidget);
    expect(find.text('Amaya Silva'), findsNothing);
  });

  testWidgets('closing asks first, then shows Closed and the reopen button', (tester) async {
    await pumpDetail(tester, TestApp());

    await tester.tap(find.text('Close shelter'));
    await tester.pumpAndSettle();
    expect(find.text('Close this shelter?'), findsOneWidget);
    await tester.tap(find.widgetWithText(FilledButton, 'Close shelter'));
    await tester.pumpAndSettle();

    expect(find.text('Shelter closed.'), findsOneWidget);
    expect(find.text('Closed'), findsOneWidget);
    expect(find.text('Reopen shelter'), findsOneWidget);
    expect(find.text('This shelter is closed. Reopen it to check people in.'), findsOneWidget);
    expect(tester.widget<TextFormField>(field('Full name')).enabled, isFalse);
  });

  testWidgets('a closed shelter can be reopened', (tester) async {
    await pumpDetail(tester, TestApp(), id: 3);

    await tester.tap(find.text('Reopen shelter'));
    await tester.pumpAndSettle();

    expect(find.text('Shelter reopened.'), findsOneWidget);
    expect(find.text('Open'), findsOneWidget);
    expect(find.text('Close shelter'), findsOneWidget);
  });

  testWidgets('a full shelter explains why check-in is off', (tester) async {
    await pumpDetail(tester, TestApp(), id: 2);

    expect(find.text('This shelter is full. Check someone out to free a place.'), findsOneWidget);
    expect(tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Check in')).onPressed, isNull);
  });

  testWidgets('a refusal from the server shows its reason and can be dismissed', (tester) async {
    final app = TestApp();
    await pumpDetail(tester, app);
    await app.operations.closeShelter(1); // closed behind the screen's back
    await enterField(tester, 'Full name', 'Ama');
    await enterField(tester, 'NIC', '1');

    await tester.tap(find.text('Check in'));
    await tester.pumpAndSettle();

    expect(find.text('Cannot check in: shelter is closed.'), findsOneWidget);
    expect(find.text('Closed'), findsOneWidget);
    await tester.tap(find.byTooltip('Dismiss'));
    await tester.pumpAndSettle();
    expect(find.text('Cannot check in: shelter is closed.'), findsNothing);
  });

  testWidgets('an empty shelter says nobody is checked in', (tester) async {
    await pumpDetail(tester, TestApp(), id: 3);

    expect(find.text('No occupants are checked in right now.'), findsOneWidget);
  });

  testWidgets('with no connection the screen offers Try again', (tester) async {
    final app = TestApp();
    app.operations.failNetwork = true;
    await pumpDetail(tester, app);

    expect(find.text('No connection to the server.'), findsOneWidget);
    app.operations.failNetwork = false;
    await tester.tap(find.text('Try again'));
    await tester.pumpAndSettle();

    expect(find.text('Current occupants (3)'), findsOneWidget);
  });

  testWidgets('an unknown shelter shows the server reason', (tester) async {
    await pumpDetail(tester, TestApp(), id: 99);

    expect(find.text('The shelter could not be loaded.'), findsOneWidget);
    expect(find.text('Shelter not found: 99'), findsOneWidget);
  });

  testWidgets('opening a shelter from the list and coming back shows the changed occupancy', (tester) async {
    final app = TestApp();
    tester.view.physicalSize = const Size(500, 2200);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(
      app.dependencies.provide(
        child: MaterialApp(
          theme: AppTheme.light(),
          home: const Scaffold(body: ShelterListScreen()),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Town Hall Shelter'));
    await tester.pumpAndSettle();
    await enterField(tester, 'Full name', 'Ama');
    await enterField(tester, 'NIC', '1');
    await tester.tap(find.text('Check in'));
    await tester.pumpAndSettle();
    await tester.pageBack();
    await tester.pumpAndSettle();

    expect(find.text('4 of 50 places taken'), findsOneWidget);
  });

  testWidgets('fits a narrow phone with large text in dark mode', (tester) async {
    await pumpDetail(tester, TestApp(), size: const Size(320, 900), textScale: 1.6, brightness: Brightness.dark);

    expect(tester.takeException(), isNull);
    expect(find.text('Town Hall Shelter'), findsWidgets);
    await tester.scrollUntilVisible(find.text('Current occupants (3)'), 300, scrollable: find.byType(Scrollable).first);
    expect(tester.takeException(), isNull);
  });
}
