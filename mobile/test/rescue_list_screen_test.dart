import 'package:dewecs_mobile/config/theme.dart';
import 'package:dewecs_mobile/models/rescue_request.dart';
import 'package:dewecs_mobile/screens/rescue_list_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'test_helpers.dart';

Future<void> pumpList(
  WidgetTester tester,
  TestApp app, {
  void Function(RescueRequest)? onOpen,
  Size size = const Size(500, 1800),
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
        home: Scaffold(body: RescueListScreen(onOpen: onOpen)),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

Finder chip(String label) => find.widgetWithText(ChoiceChip, label);

/// The chip rows scroll sideways, so a chip may start off screen: scroll it into view, then tap it.
Future<void> tapChip(WidgetTester tester, String label, {bool first = false}) async {
  final finder = first ? chip(label).first : chip(label);
  await tester.ensureVisible(finder);
  await tester.pumpAndSettle();
  await tester.tap(finder);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('shows every request with requester, status, priority and team', (tester) async {
    await pumpList(tester, TestApp());

    expect(find.text('4 rescue requests'), findsOneWidget);
    expect(find.text('Nimal Perera'), findsOneWidget);
    expect(find.text('Waiting for a team'), findsWidgets);
    expect(find.text('Critical priority'), findsWidgets);
    expect(find.text('Team: Charlie Team'), findsOneWidget);
    expect(find.text('No team assigned yet'), findsWidgets);
    expect(find.text('Completed'), findsWidgets);
  });

  testWidgets('tapping a card hands the request to onOpen', (tester) async {
    RescueRequest? opened;
    await pumpList(tester, TestApp(), onOpen: (r) => opened = r);

    await tester.tap(find.text('Saman Kumara'));

    expect(opened?.id, 2);
  });

  testWidgets('status chips filter the list and All brings everything back', (tester) async {
    await pumpList(tester, TestApp());

    await tapChip(tester, 'Team assigned');
    expect(find.text('1 rescue request'), findsOneWidget);
    expect(find.text('Saman Kumara'), findsOneWidget);

    await tapChip(tester, 'All', first: true);
    expect(find.text('4 rescue requests'), findsOneWidget);
  });

  testWidgets('priority chips filter the list and combine with the status', (tester) async {
    await pumpList(tester, TestApp());

    await tapChip(tester, 'High priority');
    expect(find.text('Kamala Fernando'), findsOneWidget);
    expect(find.text('1 rescue request'), findsOneWidget);

    await tapChip(tester, 'Completed');
    expect(find.text('No rescue requests match'), findsOneWidget);
    expect(find.text('Try another status, priority or district.'), findsOneWidget);
  });

  testWidgets('tapping the selected chip again turns that filter off', (tester) async {
    await pumpList(tester, TestApp());

    await tapChip(tester, 'Low priority');
    expect(find.text('1 rescue request'), findsOneWidget);
    await tapChip(tester, 'Low priority');

    expect(find.text('4 rescue requests'), findsOneWidget);
  });

  testWidgets('the district drop-down filters the list', (tester) async {
    await pumpList(tester, TestApp());

    await tester.tap(find.byType(DropdownButtonFormField<int?>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Kandy').last);
    await tester.pumpAndSettle();

    expect(find.text('Kamala Fernando'), findsOneWidget);
    expect(find.text('Nimal Perera'), findsNothing);
  });

  testWidgets('Clear filters resets an empty result', (tester) async {
    await pumpList(tester, TestApp());
    await tapChip(tester, 'Cancelled');
    expect(find.text('No rescue requests match'), findsOneWidget);

    await tester.tap(find.text('Clear filters'));
    await tester.pumpAndSettle();

    expect(find.text('4 rescue requests'), findsOneWidget);
  });

  testWidgets('with no connection the first load shows an error with Try again that recovers', (tester) async {
    final app = TestApp();
    app.operations.failNetwork = true;
    await pumpList(tester, app);

    expect(find.text('No connection to the server.'), findsOneWidget);
    app.operations.failNetwork = false;
    await tester.tap(find.text('Try again'));
    await tester.pumpAndSettle();

    expect(find.text('4 rescue requests'), findsOneWidget);
  });

  testWidgets('a server error on the first load shows its reason', (tester) async {
    final app = TestApp();
    app.operations.failStatus = 500;
    await pumpList(tester, app);

    expect(find.text('The rescue requests could not be loaded.'), findsOneWidget);
    expect(find.text('The server failed (simulated).'), findsOneWidget);
  });

  testWidgets('pull to refresh after going offline keeps the list and shows a banner', (tester) async {
    final app = TestApp();
    await pumpList(tester, app);
    app.operations.failNetwork = true;

    await tester.fling(find.byType(ListView), const Offset(0, 800), 3000);
    await tester.pumpAndSettle();

    expect(find.text('Offline - showing the last list received'), findsOneWidget);
    expect(find.text('Nimal Perera'), findsOneWidget);
  });

  testWidgets('fits a narrow phone with large text in dark mode', (tester) async {
    await pumpList(tester, TestApp(), size: const Size(320, 800), textScale: 1.6, brightness: Brightness.dark);

    expect(tester.takeException(), isNull);
    expect(find.text('Nimal Perera'), findsOneWidget);
  });
}
