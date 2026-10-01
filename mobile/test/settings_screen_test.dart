import 'package:dewecs_mobile/screens/identify_screen.dart';
import 'package:dewecs_mobile/strings.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'test_helpers.dart';

Future<TestApp> openSettings(WidgetTester tester, {bool identified = false}) async {
  final app = TestApp();
  if (identified) {
    await app.dependencies.identity
        .identify(nic: '199012345678', fullName: 'Nimal', phone: '0771234567', districtId: 1);
  }
  await app.pump(tester);
  if (!identified) {
    await tester.pageBack();
    await tester.pumpAndSettle();
  }
  await tester.tap(find.text(S.navSettings));
  await tester.pumpAndSettle();
  return app;
}

void main() {
  testWidgets('test connection reports the district count', (tester) async {
    await openSettings(tester);

    await tester.tap(find.text(S.testConnection));
    await tester.pumpAndSettle();

    expect(find.text(S.connectionOk(4)), findsOneWidget);
  });

  testWidgets('test connection shows a readable error when the server is unreachable', (tester) async {
    final app = await openSettings(tester);
    app.demo.failNetwork = true;

    await tester.tap(find.text(S.testConnection));
    await tester.pumpAndSettle();

    expect(find.textContaining('Connection failed'), findsOneWidget);
  });

  testWidgets('a failed test also says what to try next', (tester) async {
    final app = await openSettings(tester);
    app.demo.failNetwork = true;
    await tester.enterText(find.widgetWithText(TextFormField, S.baseUrlLabel), 'http://10.0.2.2:8080');

    await tester.tap(find.text(S.testConnection));
    await tester.pumpAndSettle();

    expect(find.textContaining('Connection failed'), findsOneWidget);
    // The emulator address was typed, so the advice is about the emulator.
    expect(find.textContaining('emulator'), findsWidgets);
    expect(find.text(S.testConnection), findsOneWidget, reason: 'the button is usable again, not stuck on Testing');
  });

  testWidgets('a plain http address on another machine shows the encryption warning', (tester) async {
    await openSettings(tester);

    await tester.enterText(find.widgetWithText(TextFormField, S.baseUrlLabel), 'http://192.168.1.20:8080');
    await tester.pump();

    expect(find.textContaining('not encrypted'), findsOneWidget);
  });

  testWidgets('an invalid address is not saved', (tester) async {
    final app = await openSettings(tester);

    await tester.enterText(find.widgetWithText(TextFormField, S.baseUrlLabel), 'not a url');
    await tester.tap(find.widgetWithText(FilledButton, S.save));
    await tester.pumpAndSettle();

    expect(find.text(S.baseUrlInvalid), findsOneWidget);
    expect(app.dependencies.settings.baseUrl, isNot('not a url'));
  });

  testWidgets('a valid address is saved', (tester) async {
    final app = await openSettings(tester);

    await tester.enterText(find.widgetWithText(TextFormField, S.baseUrlLabel), 'http://192.168.1.20:8080');
    await tester.tap(find.widgetWithText(FilledButton, S.save));
    await tester.pumpAndSettle();

    expect(app.dependencies.settings.baseUrl, 'http://192.168.1.20:8080');
    expect(find.text(S.baseUrlSaved), findsOneWidget);
  });

  testWidgets('shows who the app thinks the user is', (tester) async {
    await openSettings(tester, identified: true);

    expect(find.text(S.identifiedAs('Nimal', 'Colombo')), findsOneWidget);
  });

  testWidgets('switching Demo mode asks first and clears the stored identity', (tester) async {
    final app = await openSettings(tester, identified: true);

    await tester.tap(find.byType(Switch).first);
    await tester.pumpAndSettle();
    expect(find.text(S.demoSwitchTitle), findsOneWidget);
    await tester.tap(find.text(S.cancel));
    await tester.pumpAndSettle();
    expect(app.dependencies.settings.demoMode, isFalse);
    expect(app.dependencies.identity.isIdentified, isTrue);

    await tester.tap(find.byType(Switch).first);
    await tester.pumpAndSettle();
    await tester.tap(find.text(S.demoSwitchConfirm));
    await tester.pumpAndSettle();

    expect(app.dependencies.settings.demoMode, isTrue);
    expect(app.dependencies.identity.isIdentified, isFalse);
    // The identity is gone, so the app explains and opens the identify form.
    expect(find.byType(IdentifyScreen), findsOneWidget);
    await tester.pageBack();
    await tester.pumpAndSettle();
    expect(find.text(S.demoForceNetwork), findsOneWidget);
  });
}
