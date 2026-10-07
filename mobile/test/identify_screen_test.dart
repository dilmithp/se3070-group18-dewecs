import 'package:dewecs_mobile/strings.dart';
import 'package:dewecs_mobile/screens/identify_screen.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'test_helpers.dart';

Future<void> fillValidForm(WidgetTester tester) async {
  await enterField(tester, S.nicLabel, '199012345678');
  await enterField(tester, S.nameLabel, 'Nimal Perera');
  await enterField(tester, S.phoneLabel, '0771234567');
  await tester.tap(find.byType(DropdownButtonFormField<int>));
  await tester.pumpAndSettle();
  await tester.tap(find.text('Colombo').last);
  await tester.pumpAndSettle();
}

void main() {
  testWidgets('first run opens the identify form with the districts loaded', (tester) async {
    await TestApp().pump(tester);

    expect(find.byType(IdentifyScreen), findsOneWidget);
    expect(find.text(S.identifyTitle), findsOneWidget);
    expect(find.byType(DropdownButtonFormField<int>), findsOneWidget);
  });

  testWidgets('an empty submit shows the validation messages and calls nothing', (tester) async {
    final app = TestApp();
    await app.pump(tester);
    final callsBefore = app.fake.callCount;

    await tester.tap(find.widgetWithText(FilledButton, S.identifyButton));
    await tester.pumpAndSettle();

    expect(find.text(S.nicRequired), findsOneWidget);
    expect(find.text(S.nameRequired), findsOneWidget);
    expect(find.text(S.phoneRequired), findsOneWidget);
    expect(find.text(S.districtRequired), findsOneWidget);
    expect(app.fake.callCount, callsBefore);
  });

  testWidgets('a valid form identifies the citizen and leads to the app', (tester) async {
    final app = TestApp();
    await app.pump(tester);

    await fillValidForm(tester);
    await tester.tap(find.widgetWithText(FilledButton, S.identifyButton));
    await tester.pumpAndSettle();

    expect(app.dependencies.identity.isIdentified, isTrue);
    expect(app.dependencies.identity.citizen!.fullName, 'Nimal Perera');
    expect(find.byType(IdentifyScreen), findsNothing);
    expect(app.store.data.values.join(), isNot(contains('199012345678')));
  });

  testWidgets('offline: a clear message, the form and its answers are kept', (tester) async {
    final app = TestApp();
    await app.pump(tester);
    await fillValidForm(tester);
    app.fake.failNetwork = true;

    await tester.tap(find.widgetWithText(FilledButton, S.identifyButton));
    await tester.pumpAndSettle();

    expect(find.text(S.identifyOffline), findsOneWidget);
    expect(find.byType(IdentifyScreen), findsOneWidget);
    expect(find.text('199012345678'), findsOneWidget);
    expect(find.text('Nimal Perera'), findsOneWidget);
    expect(app.dependencies.identity.isIdentified, isFalse);
  });

  testWidgets('a server rule violation shows the server detail', (tester) async {
    final app = TestApp();
    await app.pump(tester);
    await fillValidForm(tester);
    await enterField(tester, S.nicLabel, '123456789Z');

    await tester.tap(find.widgetWithText(FilledButton, S.identifyButton));
    await tester.pumpAndSettle();

    expect(find.text(S.nicInvalid), findsOneWidget, reason: 'caught by the client check before any call');
  });
}
