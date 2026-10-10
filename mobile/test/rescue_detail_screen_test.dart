import 'package:dewecs_mobile/config/theme.dart';
import 'package:dewecs_mobile/models/rescue_request.dart';
import 'package:dewecs_mobile/screens/rescue_detail_screen.dart';
import 'package:dewecs_mobile/screens/rescue_list_screen.dart';
import 'package:dewecs_mobile/widgets/rescue_card.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'test_helpers.dart';

RescueRequest stub(int id) => RescueRequest(
  id: id,
  requesterName: 'Stub',
  requesterPhone: '0',
  districtId: 1,
  districtName: 'Colombo',
  gpsLat: null,
  gpsLng: null,
  description: 'd',
  priority: 'LOW',
  status: 'PENDING',
  assignedTeamName: null,
  submittedAt: null,
  assignedAt: null,
  completedAt: null,
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
  await tester.pumpWidget(
    app.dependencies.provide(
      child: MaterialApp(
        theme: brightness == Brightness.dark ? AppTheme.dark() : AppTheme.light(),
        home: RescueDetailScreen(request: stub(id)),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('a pending request shows every field the web page shows', (tester) async {
    await pumpDetail(tester, TestApp());

    expect(find.text('Nimal Perera'), findsOneWidget);
    expect(find.text('Waiting for a team'), findsOneWidget);
    expect(find.text('Critical priority'), findsOneWidget);
    expect(find.text('Colombo'), findsOneWidget);
    expect(find.text('0771234567'), findsOneWidget);
    expect(find.text('6.9271234, 79.8612345'), findsOneWidget);
    expect(find.textContaining('Family of five'), findsOneWidget);
    expect(find.text('Assigned team'), findsOneWidget);
    expect(find.text('Submitted at'), findsOneWidget);
    expect(find.text('Completed at'), findsOneWidget);
    // team, assigned at and completed at are empty
    expect(find.text('—'), findsNWidgets(3));
  });

  testWidgets('a request without a position shows a dash for GPS', (tester) async {
    await pumpDetail(tester, TestApp(), id: 2);

    expect(find.text('GPS'), findsOneWidget);
    expect(find.text('Charlie Team'), findsOneWidget);
    expect(find.text('Team assigned'), findsOneWidget);
    expect(find.text('Moderate priority'), findsOneWidget);
  });

  testWidgets('assigning picks the first available team by default and shows the result', (tester) async {
    await pumpDetail(tester, TestApp());
    expect(find.text('Assign a team'), findsOneWidget);

    await tester.tap(find.text('Assign'));
    await tester.pumpAndSettle();

    expect(find.text('Rescue team assigned.'), findsOneWidget);
    expect(find.text('Team assigned'), findsOneWidget);
    expect(find.text('Alpha Team'), findsOneWidget);
    expect(find.text('Assign a team'), findsNothing);
    expect(find.text('Complete'), findsOneWidget);
  });

  testWidgets('another team can be chosen before assigning', (tester) async {
    final app = TestApp();
    await pumpDetail(tester, app);

    await tester.tap(find.byType(DropdownButtonFormField<int>));
    await tester.pumpAndSettle();
    await tester.tap(find.textContaining('Bravo Team').last);
    await tester.pumpAndSettle();
    await tester.tap(find.text('Assign'));
    await tester.pumpAndSettle();

    expect(find.text('Bravo Team'), findsOneWidget);
    expect((await app.operations.getRescueRequest(1)).request.assignedTeamName, 'Bravo Team');
  });

  testWidgets('with no team available the form says so and Assign is off', (tester) async {
    final app = TestApp();
    await app.operations.assignTeam(3, 1);
    await app.operations.assignTeam(1, 2);
    final created = await app.operations.submitRescueRequest(
      districtId: 1,
      requesterName: 'Ama',
      requesterPhone: '0771111111',
      description: 'Need a boat',
      priority: 'HIGH',
    );
    await pumpDetail(tester, app, id: created.id!);

    expect(find.text('No team is available right now.'), findsOneWidget);
    expect(find.byType(DropdownButtonFormField<int>), findsNothing);
    expect(tester.widget<FilledButton>(find.widgetWithText(FilledButton, 'Assign')).onPressed, isNull);
  });

  testWidgets('completing an assigned request shows Completed and removes the actions', (tester) async {
    await pumpDetail(tester, TestApp(), id: 2);

    await tester.tap(find.text('Complete'));
    await tester.pumpAndSettle();

    expect(find.text('Rescue request completed.'), findsOneWidget);
    expect(find.text('Completed'), findsOneWidget);
    expect(find.text('Complete'), findsNothing);
    expect(find.text('Cancel request'), findsNothing);
  });

  testWidgets('cancelling asks first; keeping it changes nothing, confirming cancels', (tester) async {
    await pumpDetail(tester, TestApp(), id: 2);

    await tester.tap(find.text('Cancel request'));
    await tester.pumpAndSettle();
    expect(find.text('Cancel this rescue request?'), findsOneWidget);
    expect(find.text('An assigned team becomes available again.'), findsOneWidget);
    await tester.tap(find.text('Keep it'));
    await tester.pumpAndSettle();
    expect(find.text('Team assigned'), findsOneWidget);

    await tester.tap(find.text('Cancel request'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Cancel request'));
    await tester.pumpAndSettle();

    expect(find.text('Rescue request cancelled.'), findsOneWidget);
    expect(find.text('Cancelled'), findsOneWidget);
    expect(find.text('Cancel request'), findsNothing);
  });

  testWidgets('a finished request has no action buttons at all', (tester) async {
    await pumpDetail(tester, TestApp(), id: 4);

    expect(find.text('Completed'), findsWidgets);
    expect(find.text('Assign'), findsNothing);
    expect(find.text('Complete'), findsNothing);
    expect(find.text('Cancel request'), findsNothing);
  });

  testWidgets('a refusal because the screen was out of date shows the reason and the real state', (tester) async {
    final app = TestApp();
    await pumpDetail(tester, app);
    await app.operations.assignTeam(1, 1); // another officer assigned a team

    await tester.tap(find.text('Assign'));
    await tester.pumpAndSettle();

    expect(find.text('Only pending requests can be assigned.'), findsOneWidget);
    expect(find.text('Team assigned'), findsOneWidget);
    await tester.tap(find.byTooltip('Dismiss'));
    await tester.pumpAndSettle();
    expect(find.text('Only pending requests can be assigned.'), findsNothing);
  });

  testWidgets('with no connection the screen offers Try again', (tester) async {
    final app = TestApp();
    app.operations.failNetwork = true;
    await pumpDetail(tester, app);

    expect(find.text('No connection to the server.'), findsOneWidget);
    app.operations.failNetwork = false;
    await tester.tap(find.text('Try again'));
    await tester.pumpAndSettle();

    expect(find.text('Nimal Perera'), findsOneWidget);
  });

  testWidgets('an unknown request shows the server reason', (tester) async {
    await pumpDetail(tester, TestApp(), id: 99);

    expect(find.text('The rescue request could not be loaded.'), findsOneWidget);
    expect(find.text('Rescue request not found: 99'), findsOneWidget);
  });

  testWidgets('opening a request from the list and coming back shows its new status', (tester) async {
    final app = TestApp();
    tester.view.physicalSize = const Size(500, 2200);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    await tester.pumpWidget(
      app.dependencies.provide(
        child: MaterialApp(
          theme: AppTheme.light(),
          home: const Scaffold(body: RescueListScreen()),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('Nimal Perera'));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Cancel request'));
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, 'Cancel request'));
    await tester.pumpAndSettle();
    await tester.pageBack();
    await tester.pumpAndSettle();

    expect(find.widgetWithText(RescueCard, 'Cancelled'), findsOneWidget);
    expect(find.widgetWithText(RescueCard, 'Waiting for a team'), findsOneWidget); // only Kamala is still waiting
  });

  testWidgets('fits a narrow phone with large text in dark mode', (tester) async {
    await pumpDetail(tester, TestApp(), size: const Size(320, 900), textScale: 1.6, brightness: Brightness.dark);

    expect(tester.takeException(), isNull);
    expect(find.text('Nimal Perera'), findsOneWidget);
  });
}
