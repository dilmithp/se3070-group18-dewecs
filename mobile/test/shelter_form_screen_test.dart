import 'package:dewecs_mobile/config/theme.dart';
import 'package:dewecs_mobile/models/shelter.dart';
import 'package:dewecs_mobile/screens/shelter_detail_screen.dart';
import 'package:dewecs_mobile/screens/shelter_form_screen.dart';
import 'package:dewecs_mobile/screens/shelter_list_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'test_helpers.dart';

const _townHall = Shelter(
  id: 1,
  name: 'Town Hall Shelter',
  districtId: 1,
  districtName: 'Colombo',
  organizationName: 'Red Cross',
  capacity: 50,
  currentOccupancy: 3,
  status: 'OPEN',
);

void sizeView(WidgetTester tester, {Size size = const Size(500, 2000), double textScale = 1.0}) {
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
  Shelter? editing,
  Size size = const Size(500, 2000),
  double textScale = 1.0,
  Brightness brightness = Brightness.light,
}) async {
  sizeView(tester, size: size, textScale: textScale);
  await tester.pumpWidget(
    app.dependencies.provide(
      child: MaterialApp(
        theme: brightness == Brightness.dark ? AppTheme.dark() : AppTheme.light(),
        home: ShelterFormScreen(editing: editing),
      ),
    ),
  );
  await tester.pumpAndSettle();
}

Future<void> choose(WidgetTester tester, String label, String option) async {
  await tester.tap(find.widgetWithText(DropdownButtonFormField<int>, label));
  await tester.pumpAndSettle();
  await tester.tap(find.text(option).last);
  await tester.pumpAndSettle();
}

void main() {
  group('create', () {
    testWidgets('shows the four fields and a Create shelter button', (tester) async {
      await pumpForm(tester, TestApp());

      expect(find.text('New shelter'), findsOneWidget);
      expect(find.text('District'), findsOneWidget);
      expect(find.text('Owning organization'), findsOneWidget);
      expect(find.widgetWithText(TextFormField, 'Name'), findsOneWidget);
      expect(find.widgetWithText(TextFormField, 'Capacity'), findsOneWidget);
      expect(find.text('Create shelter'), findsOneWidget);
    });

    testWidgets('empty fields show the server wording and send nothing', (tester) async {
      final app = TestApp();
      await pumpForm(tester, app);
      final before = app.operations.callCount;

      await tester.tap(find.text('Create shelter'));
      await tester.pumpAndSettle();

      expect(find.text('Select a district'), findsOneWidget);
      expect(find.text('Select an owning organization'), findsOneWidget);
      expect(find.text('Enter a name'), findsOneWidget);
      expect(find.text('Enter a capacity'), findsOneWidget);
      expect(app.operations.callCount, before);
    });

    testWidgets('a capacity of zero is refused before sending; only digits can be typed', (tester) async {
      await pumpForm(tester, TestApp());

      await enterField(tester, 'Capacity', '0');
      await tester.tap(find.text('Create shelter'));
      await tester.pumpAndSettle();
      expect(find.text('Capacity must be positive'), findsOneWidget);

      await enterField(tester, 'Capacity', '1a2-');
      expect(tester.widget<TextFormField>(find.widgetWithText(TextFormField, 'Capacity')).controller!.text, '12');
    });

    testWidgets('a valid form creates the shelter and opens its detail', (tester) async {
      final app = TestApp();
      await pumpForm(tester, app);

      await choose(tester, 'District', 'Galle');
      await choose(tester, 'Owning organization', 'Red Cross');
      await enterField(tester, 'Name', 'Community Hall');
      await enterField(tester, 'Capacity', '35');
      await tester.tap(find.text('Create shelter'));
      await tester.pumpAndSettle();

      expect(find.byType(ShelterDetailScreen), findsOneWidget);
      expect(find.text('Shelter created.'), findsOneWidget);
      expect(find.text('Community Hall'), findsWidgets);
      expect(find.text('0 of 35 places taken'), findsOneWidget);
      expect(find.text('Galle'), findsOneWidget);
      final created = (await app.operations.listShelters()).shelters.last;
      expect([created.name, created.districtName, created.organizationName], ['Community Hall', 'Galle', 'Red Cross']);
    });

    testWidgets('with no connection the form offers Try again', (tester) async {
      final app = TestApp();
      app.operations.failNetwork = true;
      await pumpForm(tester, app);

      expect(find.text('No connection to the server.'), findsOneWidget);
      app.operations.failNetwork = false;
      await tester.tap(find.text('Try again'));
      await tester.pumpAndSettle();

      expect(find.text('Create shelter'), findsOneWidget);
    });

    testWidgets('a connection lost while saving keeps the typed values and says why', (tester) async {
      final app = TestApp();
      await pumpForm(tester, app);
      await choose(tester, 'District', 'Galle');
      await choose(tester, 'Owning organization', 'Red Cross');
      await enterField(tester, 'Name', 'Community Hall');
      await enterField(tester, 'Capacity', '35');
      app.operations.failNetwork = true;

      await tester.tap(find.text('Create shelter'));
      await tester.pumpAndSettle();

      expect(find.byType(ShelterFormScreen), findsOneWidget);
      expect(find.textContaining('Could not reach the server'), findsOneWidget);
      expect(
        tester.widget<TextFormField>(find.widgetWithText(TextFormField, 'Name')).controller!.text,
        'Community Hall',
      );
    });
  });

  group('edit', () {
    testWidgets('fills the fields and shows the district and organization as fixed', (tester) async {
      await pumpForm(tester, TestApp(), editing: _townHall);

      expect(find.text('Edit shelter'), findsOneWidget);
      expect(
        tester.widget<TextFormField>(find.widgetWithText(TextFormField, 'Name')).controller!.text,
        'Town Hall Shelter',
      );
      expect(tester.widget<TextFormField>(find.widgetWithText(TextFormField, 'Capacity')).controller!.text, '50');
      expect(find.text('Colombo'), findsOneWidget);
      expect(find.text('Red Cross'), findsOneWidget);
      expect(find.text('The district is fixed after creation.'), findsOneWidget);
      expect(find.text('The organization is fixed after creation.'), findsOneWidget);
      expect(find.byType(DropdownButtonFormField<int>), findsNothing);
      expect(find.text('3 people are checked in now.'), findsOneWidget);
      expect(find.text('Save changes'), findsOneWidget);
    });

    testWidgets('saving changes the shelter and goes back with a message', (tester) async {
      final app = TestApp();
      sizeView(tester);
      late BuildContext host;
      await tester.pumpWidget(
        app.dependencies.provide(
          child: MaterialApp(
            theme: AppTheme.light(),
            home: Builder(
              builder: (context) {
                host = context;
                return const Scaffold(body: Text('before'));
              },
            ),
          ),
        ),
      );
      final result = Navigator.of(host)
          .push<bool>(MaterialPageRoute<bool>(builder: (_) => const ShelterFormScreen(editing: _townHall)));
      await tester.pumpAndSettle();

      await enterField(tester, 'Name', 'Town Hall (North)');
      await enterField(tester, 'Capacity', '60');
      await tester.tap(find.text('Save changes'));
      await tester.pumpAndSettle();

      expect(await result, isTrue);
      expect(find.text('Shelter updated.'), findsOneWidget);
      final saved = (await app.operations.getShelter(1)).shelter;
      expect([saved.name, saved.capacity, saved.organizationName], ['Town Hall (North)', 60, 'Red Cross']);
    });

    testWidgets('a capacity below the occupancy shows the server rule above the form', (tester) async {
      final app = TestApp();
      await pumpForm(tester, app, editing: _townHall);

      await enterField(tester, 'Capacity', '2');
      await tester.tap(find.text('Save changes'));
      await tester.pumpAndSettle();

      expect(find.byType(ShelterFormScreen), findsOneWidget);
      expect(find.text('Capacity cannot be lower than the current occupancy (3).'), findsOneWidget);
      expect((await app.operations.getShelter(1)).shelter.capacity, 50);
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
                      Navigator.of(context)
                          .push(MaterialPageRoute<bool>(builder: (_) => const ShelterFormScreen(editing: _townHall))),
                  child: const Text('open'),
                ),
              ),
            ),
          ),
        ),
      );
      await tester.tap(find.text('open'));
      await tester.pumpAndSettle();
      await enterField(tester, 'Name', 'Changed');

      await tester.tap(find.text('Cancel'));
      await tester.pumpAndSettle();

      expect(find.byType(ShelterFormScreen), findsNothing);
      expect((await app.operations.getShelter(1)).shelter.name, 'Town Hall Shelter');
    });
  });

  group('from the other screens', () {
    testWidgets('the list has a New shelter button that opens the form, and the new shelter shows after saving', (
      tester,
    ) async {
      final app = TestApp();
      sizeView(tester);
      await tester.pumpWidget(
        app.dependencies.provide(
          child: MaterialApp(
            theme: AppTheme.light(),
            home: const Scaffold(body: ShelterListScreen()),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.text('New shelter'));
      await tester.pumpAndSettle();
      expect(find.byType(ShelterFormScreen), findsOneWidget);
      await choose(tester, 'District', 'Kandy');
      await choose(tester, 'Owning organization', 'Red Cross');
      await enterField(tester, 'Name', 'Kandy Temple');
      await enterField(tester, 'Capacity', '20');
      await tester.tap(find.text('Create shelter'));
      await tester.pumpAndSettle();
      await tester.pageBack();
      await tester.pumpAndSettle();

      expect(find.text('5 shelters'), findsOneWidget);
      expect(find.text('Kandy Temple'), findsOneWidget);
    });

    testWidgets('the detail has an Edit action; after saving the detail shows the new values', (tester) async {
      final app = TestApp();
      sizeView(tester);
      await tester.pumpWidget(
        app.dependencies.provide(
          child: MaterialApp(
            theme: AppTheme.light(),
            home: const ShelterDetailScreen(shelter: _townHall),
          ),
        ),
      );
      await tester.pumpAndSettle();

      await tester.tap(find.byTooltip('Edit'));
      await tester.pumpAndSettle();
      await enterField(tester, 'Name', 'Renamed Hall');
      await enterField(tester, 'Capacity', '10');
      await tester.tap(find.text('Save changes'));
      await tester.pumpAndSettle();

      expect(find.byType(ShelterFormScreen), findsNothing);
      expect(find.text('Renamed Hall'), findsWidgets);
      expect(find.text('3 of 10 places taken'), findsOneWidget);
      expect(find.text('Shelter updated.'), findsOneWidget);
    });
  });

  testWidgets('fits a narrow phone with large text in dark mode', (tester) async {
    await pumpForm(tester, TestApp(), size: const Size(320, 900), textScale: 1.6, brightness: Brightness.dark);
    await tester.tap(find.text('Create shelter'));
    await tester.pumpAndSettle();

    expect(tester.takeException(), isNull);
  });
}
