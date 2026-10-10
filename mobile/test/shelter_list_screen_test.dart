import 'package:dewecs_mobile/config/theme.dart';
import 'package:dewecs_mobile/models/shelter.dart';
import 'package:dewecs_mobile/screens/shelter_list_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'test_helpers.dart';

Future<void> pumpList(
  WidgetTester tester,
  TestApp app, {
  void Function(Shelter)? onOpen,
  Size size = const Size(500, 1400),
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
        home: Scaffold(body: ShelterListScreen(onOpen: onOpen)),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('shows every shelter with its status and occupancy', (tester) async {
    await pumpList(tester, TestApp());

    expect(find.text('4 shelters'), findsOneWidget);
    expect(find.text('Town Hall Shelter'), findsOneWidget);
    expect(find.text('3 of 50 places taken'), findsOneWidget);
    expect(find.text('Temple Hall'), findsOneWidget);
    expect(find.text('No places free'), findsOneWidget);
    expect(find.text('Kandy Central School'), findsOneWidget);
    expect(find.text('Closed'), findsWidgets);
  });

  testWidgets('tapping a card hands the shelter to onOpen', (tester) async {
    Shelter? opened;
    await pumpList(tester, TestApp(), onOpen: (s) => opened = s);

    await tester.tap(find.text('Temple Hall'));

    expect(opened?.id, 2);
  });

  testWidgets('a status chip filters the list and All brings everything back', (tester) async {
    await pumpList(tester, TestApp());

    await tester.tap(find.widgetWithText(ChoiceChip, 'Full'));
    await tester.pumpAndSettle();
    expect(find.text('1 shelter'), findsOneWidget);
    expect(find.text('Temple Hall'), findsOneWidget);
    expect(find.text('Town Hall Shelter'), findsNothing);

    await tester.tap(find.widgetWithText(ChoiceChip, 'All'));
    await tester.pumpAndSettle();
    expect(find.text('4 shelters'), findsOneWidget);
  });

  testWidgets('the district drop-down filters the list', (tester) async {
    await pumpList(tester, TestApp());

    await tester.tap(find.byType(DropdownButtonFormField<int?>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Galle').last);
    await tester.pumpAndSettle();

    expect(find.text('Temple Hall'), findsOneWidget);
    expect(find.text('Town Hall Shelter'), findsNothing);
  });

  testWidgets('a filter with no match explains itself and Clear filters resets it', (tester) async {
    await pumpList(tester, TestApp());

    await tester.tap(find.widgetWithText(ChoiceChip, 'Closed'));
    await tester.pumpAndSettle();
    await tester.tap(find.byType(DropdownButtonFormField<int?>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Colombo').last);
    await tester.pumpAndSettle();

    expect(find.text('No shelters match'), findsOneWidget);
    await tester.tap(find.text('Clear filters'));
    await tester.pumpAndSettle();
    expect(find.text('4 shelters'), findsOneWidget);
  });

  testWidgets('with no connection the first load shows an error with Try again that recovers', (tester) async {
    final app = TestApp();
    app.operations.failNetwork = true;
    await pumpList(tester, app);

    expect(find.text('No connection to the server.'), findsOneWidget);
    expect(find.text('Try again'), findsOneWidget);
    app.operations.failNetwork = false;
    await tester.tap(find.text('Try again'));
    await tester.pumpAndSettle();

    expect(find.text('4 shelters'), findsOneWidget);
  });

  testWidgets('a server error on the first load shows its reason', (tester) async {
    final app = TestApp();
    app.operations.failStatus = 500;
    await pumpList(tester, app);

    expect(find.text('The shelters could not be loaded.'), findsOneWidget);
    expect(find.text('The server failed (simulated).'), findsOneWidget);
  });

  testWidgets('pull to refresh after going offline keeps the list and shows a banner', (tester) async {
    final app = TestApp();
    await pumpList(tester, app);
    app.operations.failNetwork = true;

    await tester.fling(find.byType(ListView), const Offset(0, 400), 1000);
    await tester.pumpAndSettle();

    expect(find.text('Offline - showing the last list received'), findsOneWidget);
    expect(find.text('Town Hall Shelter'), findsOneWidget);
  });

  testWidgets('fits a narrow phone with large text in dark mode', (tester) async {
    await pumpList(tester, TestApp(), size: const Size(320, 700), textScale: 1.6, brightness: Brightness.dark);

    expect(tester.takeException(), isNull);
    expect(find.text('Town Hall Shelter'), findsOneWidget);
  });
}
