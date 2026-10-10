import 'package:dewecs_mobile/strings.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'test_helpers.dart';

Future<TestApp> startIdentified(
  WidgetTester tester, {
  Size size = const Size(500, 1400),
  double textScale = 1.0,
}) async {
  final app = TestApp();
  await app.identify();
  await app.pump(tester, size: size, textScale: textScale);
  return app;
}

Future<void> openTab(WidgetTester tester, String label) async {
  await tester.tap(find.descendant(of: find.byType(NavigationBar), matching: find.text(label)));
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('the bar has five destinations including Shelters and Rescue', (tester) async {
    await startIdentified(tester);

    final bar = find.byType(NavigationBar);
    for (final label in [S.navHome, S.navNewReport, S.navShelters, S.navRescue, S.navSettings]) {
      expect(
        find.descendant(of: bar, matching: find.text(label)),
        findsOneWidget,
        reason: label,
      );
    }
  });

  testWidgets('the Shelters tab shows the shelter list under a Shelters title', (tester) async {
    await startIdentified(tester);

    await openTab(tester, S.navShelters);

    expect(find.descendant(of: find.byType(AppBar), matching: find.text(S.navShelters)), findsOneWidget);
    expect(find.text('4 shelters'), findsOneWidget);
    expect(find.text('Town Hall Shelter'), findsOneWidget);
    expect(find.text(S.newShelterButton), findsOneWidget);
  });

  testWidgets('the Rescue tab shows the rescue list under a Rescue title', (tester) async {
    await startIdentified(tester);

    await openTab(tester, S.navRescue);

    expect(find.descendant(of: find.byType(AppBar), matching: find.text(S.navRescue)), findsOneWidget);
    expect(find.text('4 rescue requests'), findsOneWidget);
    expect(find.text(S.newRescueButton), findsOneWidget);
  });

  testWidgets('the officer lists are not fetched until their tab is opened', (tester) async {
    final app = await startIdentified(tester);
    expect(app.operations.callCount, 0);

    await openTab(tester, S.navShelters);
    final afterShelters = app.operations.callCount;
    expect(afterShelters, greaterThan(0));

    await openTab(tester, S.navRescue);
    expect(app.operations.callCount, greaterThan(afterShelters));
  });

  testWidgets('the shelter and rescue tabs work without identifying first', (tester) async {
    final app = TestApp();
    await app.pump(tester);
    await tester.pageBack();
    await tester.pumpAndSettle();

    await openTab(tester, S.navShelters);
    expect(find.text('Town Hall Shelter'), findsOneWidget);
    expect(find.text(S.needsIdentityTitle), findsNothing);
    await openTab(tester, S.navRescue);
    expect(find.text('Nimal Perera'), findsOneWidget);
  });

  testWidgets('going back to a tab fetches it again, so a change made elsewhere shows up', (tester) async {
    final app = await startIdentified(tester);
    await openTab(tester, S.navShelters);
    expect(find.text('Town Hall Shelter'), findsOneWidget);
    await openTab(tester, S.navSettings);

    await app.operations.createShelter(districtId: 1, organizationId: 2, name: 'Added Meanwhile', capacity: 5);
    await openTab(tester, S.navShelters);

    expect(find.text('5 shelters'), findsOneWidget);
    expect(find.text('Added Meanwhile'), findsOneWidget);
  });

  testWidgets('a tab keeps its filter while another tab is shown', (tester) async {
    await startIdentified(tester);
    await openTab(tester, S.navShelters);
    await tester.tap(find.widgetWithText(ChoiceChip, 'Full'));
    await tester.pumpAndSettle();
    expect(find.text('1 shelter'), findsOneWidget);

    await openTab(tester, S.navRescue);
    await openTab(tester, S.navShelters);

    expect(find.text('1 shelter'), findsOneWidget);
  });

  testWidgets('after Demo mode is switched the lists load again instead of showing an empty state', (tester) async {
    final app = await startIdentified(tester);
    await openTab(tester, S.navShelters);
    await openTab(tester, S.navSettings);

    await app.dependencies.settings.setDemoMode(true);
    await tester.pumpAndSettle();
    // Demo mode clears the identity, so the app opens the identify form; the officer tabs do not need it.
    await tester.pageBack();
    await tester.pumpAndSettle();
    await openTab(tester, S.navShelters);

    expect(find.text('4 shelters'), findsOneWidget);
    expect(find.text('No shelters yet'), findsNothing);
  });

  testWidgets('a shelter can be opened and left again from the tab, returning to the list', (tester) async {
    await startIdentified(tester);
    await openTab(tester, S.navShelters);

    await tester.tap(find.text('Temple Hall'));
    await tester.pumpAndSettle();
    expect(find.text('Current occupants (4)'), findsOneWidget);
    await tester.pageBack();
    await tester.pumpAndSettle();

    expect(find.text('4 shelters'), findsOneWidget);
    expect(find.descendant(of: find.byType(AppBar), matching: find.text(S.navShelters)), findsOneWidget);
  });

  testWidgets('five tabs fit a narrow phone with large text', (tester) async {
    await startIdentified(tester, size: const Size(320, 700), textScale: 1.6);
    await openTab(tester, S.navShelters);
    await openTab(tester, S.navRescue);

    expect(tester.takeException(), isNull);
  });
}
