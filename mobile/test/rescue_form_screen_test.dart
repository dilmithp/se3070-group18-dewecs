import 'package:dewecs_mobile/config/theme.dart';
import 'package:dewecs_mobile/screens/rescue_detail_screen.dart';
import 'package:dewecs_mobile/screens/rescue_form_screen.dart';
import 'package:dewecs_mobile/screens/rescue_list_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'test_helpers.dart';

void sizeView(WidgetTester tester, {Size size = const Size(500, 2400), double textScale = 1.0}) {
  tester.view.physicalSize = size;
  tester.view.devicePixelRatio = 1.0;
  tester.platformDispatcher.textScaleFactorTestValue = textScale;
  addTearDown(tester.view.resetPhysicalSize);
  addTearDown(tester.view.resetDevicePixelRatio);
  addTearDown(tester.platformDispatcher.clearAllTestValues);
}

Future<void> pumpForm(
  WidgetTester tester,
  TestApp app, {
  Size size = const Size(500, 2400),
  double textScale = 1.0,
  Brightness brightness = Brightness.light,
}) async {
  sizeView(tester, size: size, textScale: textScale);
  await tester.pumpWidget(
    app.dependencies.provide(
      child: MaterialApp(
        theme: brightness == Brightness.dark ? AppTheme.dark() : AppTheme.light(),
        home: const RescueFormScreen(),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> choose<T>(WidgetTester tester, String label, String option) async {
  await tester.tap(find.widgetWithText(DropdownButtonFormField<T>, label));
  await tester.pumpAndSettle();
  await tester.tap(find.text(option).last);
  await tester.pumpAndSettle();
}

Future<void> fillValid(WidgetTester tester) async {
  await choose<int>(tester, 'District', 'Galle');
  await choose<String>(tester, 'Priority', 'High priority');
  await enterField(tester, 'Requester name', 'Ama Perera');
  await enterField(tester, 'Requester phone', '0771111111');
  await enterField(tester, 'Description', 'Need a boat to reach the farm');
}

String valueOf(WidgetTester tester, String label) =>
    tester.widget<TextFormField>(find.widgetWithText(TextFormField, label)).controller!.text;

void main() {
  testWidgets('shows the fields of the web form and a Submit request button', (tester) async {
    await pumpForm(tester, TestApp());

    expect(find.text('New rescue request'), findsOneWidget);
    expect(find.text('District'), findsOneWidget);
    expect(find.text('Priority'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Requester name'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Requester phone'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'GPS latitude (optional)'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'GPS longitude (optional)'), findsOneWidget);
    expect(find.text('Between -90 and 90.'), findsOneWidget);
    expect(find.text('Between -180 and 180.'), findsOneWidget);
    expect(find.widgetWithText(TextFormField, 'Description'), findsOneWidget);
    expect(find.text('Submit request'), findsOneWidget);
  });

  testWidgets('empty required fields show the server wording and send nothing', (tester) async {
    final app = TestApp();
    await pumpForm(tester, app);
    final before = app.operations.callCount;

    await tester.tap(find.text('Submit request'));
    await tester.pumpAndSettle();

    expect(find.text('Select a district'), findsOneWidget);
    expect(find.text('Select a priority'), findsOneWidget);
    expect(find.text("Enter the requester's name"), findsOneWidget);
    expect(find.text('Enter a contact phone number'), findsOneWidget);
    expect(find.text('Describe the situation'), findsOneWidget);
    expect(app.operations.callCount, before);
  });

  testWidgets('the position is optional but must be a number inside the range when given', (tester) async {
    final app = TestApp();
    await pumpForm(tester, app);
    await fillValid(tester);
    await enterField(tester, 'GPS latitude (optional)', '95');
    await enterField(tester, 'GPS longitude (optional)', 'abc');
    final before = app.operations.callCount;

    await tester.tap(find.text('Submit request'));
    await tester.pumpAndSettle();

    expect(find.text('Latitude must be between -90 and 90'), findsOneWidget);
    expect(find.text('Longitude must be a number'), findsOneWidget);
    expect(app.operations.callCount, before);

    await enterField(tester, 'GPS longitude (optional)', '200');
    await tester.tap(find.text('Submit request'));
    await tester.pumpAndSettle();
    expect(find.text('Longitude must be between -180 and 180'), findsOneWidget);
  });

  testWidgets('a valid request without a position is created and its detail opens', (tester) async {
    final app = TestApp();
    await pumpForm(tester, app);
    await fillValid(tester);

    await tester.tap(find.text('Submit request'));
    await tester.pumpAndSettle();

    expect(find.byType(RescueDetailScreen), findsOneWidget);
    expect(find.text('Rescue request submitted.'), findsOneWidget);
    expect(find.text('Ama Perera'), findsOneWidget);
    expect(find.text('Waiting for a team'), findsOneWidget);
    expect(find.text('High priority'), findsOneWidget);
    expect(find.text('Need a boat to reach the farm'), findsOneWidget);
    final created = (await app.operations.listRescueRequests()).requests.last;
    expect([created.districtName, created.hasLocation], ['Galle', false]);
  });

  testWidgets('a position with a decimal comma is sent as a number', (tester) async {
    final app = TestApp();
    await pumpForm(tester, app);
    await fillValid(tester);
    await enterField(tester, 'GPS latitude (optional)', '6,9271');
    await enterField(tester, 'GPS longitude (optional)', '-79.8612');

    await tester.tap(find.text('Submit request'));
    await tester.pumpAndSettle();

    final created = (await app.operations.listRescueRequests()).requests.last;
    expect([created.gpsLat, created.gpsLng], [6.9271, -79.8612]);
    expect(find.text('6.9271, -79.8612'), findsOneWidget);
  });

  testWidgets('with no connection the form offers Try again', (tester) async {
    final app = TestApp();
    app.operations.failNetwork = true;
    await pumpForm(tester, app);

    expect(find.text('No connection to the server.'), findsOneWidget);
    app.operations.failNetwork = false;
    await tester.tap(find.text('Try again'));
    await tester.pumpAndSettle();

    expect(find.text('Submit request'), findsOneWidget);
  });

  testWidgets('a connection lost while saving keeps the typed values and says why', (tester) async {
    final app = TestApp();
    await pumpForm(tester, app);
    await fillValid(tester);
    app.operations.failNetwork = true;

    await tester.tap(find.text('Submit request'));
    await tester.pumpAndSettle();

    expect(find.byType(RescueFormScreen), findsOneWidget);
    expect(valueOf(tester, 'Requester name'), 'Ama Perera');
    expect(valueOf(tester, 'Description'), 'Need a boat to reach the farm');
  });

  testWidgets('Cancel leaves without saving', (tester) async {
    final app = TestApp();
    sizeView(tester);
    await tester.pumpWidget(
      app.dependencies.provide(
        child: MaterialApp(
          theme: AppTheme.light(),
          home: Builder(
            builder: (context) => Scaffold(
              body: TextButton(
                onPressed: () =>
                    Navigator.of(context).push(MaterialPageRoute<void>(builder: (_) => const RescueFormScreen())),
                child: const Text('open'),
              ),
            ),
          ),
        ),
      ),
    );
    await tester.tap(find.text('open'));
    await tester.pumpAndSettle();
    await enterField(tester, 'Requester name', 'Someone');

    await tester.tap(find.text('Cancel'));
    await tester.pumpAndSettle();

    expect(find.byType(RescueFormScreen), findsNothing);
    expect((await app.operations.listRescueRequests()).requests, hasLength(4));
  });

  testWidgets('the list has a New request button; after saving, the new request shows in the list', (tester) async {
    final app = TestApp();
    sizeView(tester);
    await tester.pumpWidget(
      app.dependencies.provide(
        child: MaterialApp(
          theme: AppTheme.light(),
          home: const Scaffold(body: RescueListScreen()),
        ),
      ),
    );
    await tester.pumpAndSettle();

    await tester.tap(find.text('New request'));
    await tester.pumpAndSettle();
    expect(find.byType(RescueFormScreen), findsOneWidget);
    await fillValid(tester);
    await tester.tap(find.text('Submit request'));
    await tester.pumpAndSettle();
    await tester.pageBack();
    await tester.pumpAndSettle();

    expect(find.text('5 rescue requests'), findsOneWidget);
    expect(find.text('Ama Perera'), findsOneWidget);
  });

  testWidgets('fits a narrow phone with large text in dark mode', (tester) async {
    await pumpForm(tester, TestApp(), size: const Size(320, 900), textScale: 1.6, brightness: Brightness.dark);
    await tester.tap(find.text('Submit request'));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
  });
}
