import 'package:dewecs_mobile/api/http_operations_api.dart';
import 'package:dewecs_mobile/models/shelter.dart';
import 'package:dewecs_mobile/screens/rescue_detail_screen.dart';
import 'package:dewecs_mobile/screens/rescue_form_screen.dart';
import 'package:dewecs_mobile/screens/shelter_detail_screen.dart';
import 'package:dewecs_mobile/screens/shelter_form_screen.dart';
import 'package:dewecs_mobile/strings.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'test_helpers.dart';

const phone = Size(360, 760);

Future<void> openTab(WidgetTester tester, String label) async {
  await tester.tap(find.descendant(of: find.byType(NavigationBar), matching: find.text(label)));
  await tester.pumpAndSettle();
}

/// Scrolls the list of the current tab until [target] is on screen.
Future<void> scrollTo(WidgetTester tester, Finder target) async {
  final list = find.descendant(of: find.byType(RefreshIndicator), matching: find.byType(Scrollable)).first;
  await tester.scrollUntilVisible(target, 300, scrollable: list);
  await tester.ensureVisible(target.first);
  await tester.pumpAndSettle();
}

/// Opens the form behind the floating button of the current tab, then comes back.
Future<void> visit(WidgetTester tester, String button, Type screen) async {
  await tester.tap(find.text(button));
  await tester.pumpAndSettle();
  expect(find.byType(screen), findsOneWidget);
  await tester.pageBack();
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('both themes render every shelter and rescue screen without errors', (tester) async {
    for (final brightness in Brightness.values) {
      final app = TestApp();
      await app.identify();
      await app.pump(tester, size: phone, brightness: brightness);
      final reason = brightness.name;

      await openTab(tester, S.navShelters);
      expect(tester.takeException(), isNull, reason: 'shelter list in $reason');
      await visit(tester, S.newShelterButton, ShelterFormScreen);
      expect(tester.takeException(), isNull, reason: 'shelter form in $reason');
      await tester.tap(find.text('Temple Hall'));
      await tester.pumpAndSettle();
      expect(find.byType(ShelterDetailScreen), findsOneWidget);
      expect(tester.takeException(), isNull, reason: 'shelter detail in $reason');
      await tester.pageBack();
      await tester.pumpAndSettle();

      await openTab(tester, S.navRescue);
      expect(tester.takeException(), isNull, reason: 'rescue list in $reason');
      await visit(tester, S.newRescueButton, RescueFormScreen);
      expect(tester.takeException(), isNull, reason: 'rescue form in $reason');
      await tester.tap(find.text('Nimal Perera'));
      await tester.pumpAndSettle();
      expect(find.byType(RescueDetailScreen), findsOneWidget);
      expect(tester.takeException(), isNull, reason: 'rescue detail in $reason');
      await tester.pageBack();
      await tester.pumpAndSettle();
    }
  });

  testWidgets('very long names, Sinhala text and a huge occupancy fit a narrow phone with large text', (tester) async {
    final app = TestApp();
    final sinhala = 'ගඟ ගලා බසිනවා — පාලම අසල ' * 6;
    final longName = 'The Very Long Named Community Disaster Relief Shelter of the Northern Coastal Region ' * 2;
    await app.operations.createShelter(districtId: 1, organizationId: 1, name: longName, capacity: 9999);
    await app.operations.submitRescueRequest(
      districtId: 1,
      requesterName: 'නිමල් පෙරේරා ' * 5,
      requesterPhone: '0771234567',
      description: sinhala,
      priority: 'CRITICAL',
    );
    await app.identify();
    await app.pump(tester, size: const Size(320, 700), textScale: 1.6);

    await openTab(tester, S.navShelters);
    await scrollTo(tester, find.textContaining('The Very Long Named'));
    expect(tester.takeException(), isNull);
    await tester.tap(find.textContaining('The Very Long Named').first);
    await tester.pumpAndSettle();
    expect(find.byType(ShelterDetailScreen), findsOneWidget);
    expect(tester.takeException(), isNull);
    await tester.pageBack();
    await tester.pumpAndSettle();

    await openTab(tester, S.navRescue);
    await scrollTo(tester, find.textContaining('නිමල්'));
    expect(tester.takeException(), isNull);
    await tester.tap(find.textContaining('නිමල්').first);
    await tester.pumpAndSettle();
    expect(find.byType(RescueDetailScreen), findsOneWidget);
    expect(tester.takeException(), isNull);
  });

  testWidgets('a server that sends a web page instead of JSON gives a readable error on both lists', (tester) async {
    final client = MockClient((request) async => http.Response('<html>captive portal</html>', 200));
    final app = TestApp(
      operationsApi: HttpOperationsApi(baseUrl: 'http://host:8080', client: client),
    );
    await app.identify();
    await app.pump(tester, size: phone);

    await openTab(tester, S.navShelters);
    expect(tester.takeException(), isNull);
    expect(find.text(S.sheltersLoadFailed), findsOneWidget);
    expect(find.text('The server sent an unexpected response.'), findsOneWidget);

    await openTab(tester, S.navRescue);
    expect(tester.takeException(), isNull);
    expect(find.text(S.rescueLoadFailed), findsOneWidget);
    expect(find.text('The server sent an unexpected response.'), findsOneWidget);
  });

  testWidgets('a list item with a missing field is an unexpected response, not a crash', (tester) async {
    final client = MockClient(
      (request) async => http.Response(
        '{"shelters":[{"id":1,"name":"No district"}],"statuses":[],"districts":[]}',
        200,
        headers: {'content-type': 'application/json'},
      ),
    );
    final app = TestApp(
      operationsApi: HttpOperationsApi(baseUrl: 'http://host:8080', client: client),
    );
    await app.identify();
    await app.pump(tester, size: phone);

    await openTab(tester, S.navShelters);

    expect(tester.takeException(), isNull);
    expect(find.text('The server sent an unexpected response.'), findsOneWidget);
  });

  testWidgets('a server error with a JSON problem on a detail screen shows its reason', (tester) async {
    final client = MockClient(
      (request) async => http.Response(
        '{"status":500,"title":"Internal Server Error","detail":"Unexpected error occurred"}',
        500,
        headers: {'content-type': 'application/problem+json'},
      ),
    );
    final app = TestApp(
      operationsApi: HttpOperationsApi(baseUrl: 'http://host:8080', client: client),
    );
    await app.identify();
    await app.pump(tester, size: phone);
    const stub = Shelter(
      id: 1,
      name: 'Town Hall Shelter',
      districtId: 1,
      districtName: 'Colombo',
      organizationName: 'Red Cross',
      capacity: 50,
      currentOccupancy: 3,
      status: 'OPEN',
    );

    await tester.pumpWidget(
      app.dependencies.provide(
        child: const MaterialApp(home: ShelterDetailScreen(shelter: stub)),
      ),
    );
    await tester.pumpAndSettle();

    expect(find.text(S.shelterLoadFailed), findsOneWidget);
    expect(find.text('Unexpected error occurred'), findsOneWidget);
    expect(find.text('Town Hall Shelter'), findsOneWidget); // the title still shows what the list knew
  });

  testWidgets('the forced failures of Demo mode reach the officer screens', (tester) async {
    final app = TestApp();
    await app.identify();
    await app.pump(tester, size: phone);
    await openTab(tester, S.navShelters);
    expect(find.text('Town Hall Shelter'), findsOneWidget);

    app.operations.failStatus = 500;
    await tester.tap(find.descendant(of: find.byType(NavigationBar), matching: find.text(S.navShelters)));
    await tester.pumpAndSettle();

    expect(find.textContaining('Could not refresh'), findsOneWidget);
    expect(find.text('Town Hall Shelter'), findsOneWidget);
  });
}
