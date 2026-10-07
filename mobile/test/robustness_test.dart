import 'dart:convert';

import 'package:dewecs_mobile/api/http_dewecs_api.dart';
import 'package:dewecs_mobile/models/citizen.dart';
import 'package:dewecs_mobile/models/queued_report.dart';
import 'package:dewecs_mobile/models/sri_lanka_time.dart';
import 'package:dewecs_mobile/storage/key_value_store.dart';
import 'package:dewecs_mobile/strings.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:http/http.dart' as http;
import 'package:http/testing.dart';

import 'test_helpers.dart';

const phone = Size(360, 740);

QueuedReport longReport(TestApp app) => QueuedReport(
      localId: 'long',
      createdAtMs: 9,
      citizenId: app.dependencies.identity.citizen!.id,
      districtId: 1,
      districtName: 'Colombo',
      category: 'FLOOD',
      description: 'A very long description of the situation near the river that goes on and on to test wrapping ' * 3,
      gpsLat: 6.9,
      gpsLng: 79.8,
      capturedAt: sriLankaNowString(),
    )..markNeedsAttention('Validation failed: the description is far too long for the server to accept it');

Future<TestApp> identifiedApp() async {
  final app = TestApp();
  await app.identify();
  await app.dependencies.sync.enqueue(longReport(app));
  return app;
}

void main() {
  group('200 percent text scale on a small phone: nothing overflows', () {
    testWidgets('My reports', (tester) async {
      final app = await identifiedApp();
      await app.pump(tester, size: phone, textScale: 2.0);

      expect(tester.takeException(), isNull);
      expect(find.text(S.navHome), findsWidgets);
    });

    testWidgets('New report form', (tester) async {
      final app = await identifiedApp();
      await app.pump(tester, size: phone, textScale: 2.0);
      await tester.tap(find.text(S.navNewReport));
      await tester.pumpAndSettle();
      final send = find.widgetWithText(FilledButton, S.submitReport);
      await tester.scrollUntilVisible(send, 300, scrollable: find.byType(Scrollable).first);
      await tester.tap(send);
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
    });

    testWidgets('Identify', (tester) async {
      final app = TestApp();
      await app.pump(tester, size: phone, textScale: 2.0);

      expect(tester.takeException(), isNull);
      expect(find.text(S.identifyTitle), findsOneWidget);
    });

    testWidgets('Settings', (tester) async {
      final app = await identifiedApp();
      await app.pump(tester, size: phone, textScale: 2.0);
      await tester.tap(find.text(S.navSettings));
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
    });

    testWidgets('Report detail', (tester) async {
      final app = await identifiedApp();
      await app.pump(tester, size: phone, textScale: 2.0);
      await tester.tap(find.textContaining('A very long description').first);
      await tester.pumpAndSettle();

      expect(tester.takeException(), isNull);
      expect(find.text(S.detailProblem), findsOneWidget);
    });
  });

  group('accessibility guidelines', () {
    testWidgets('tap targets are at least 48 dp and labelled', (tester) async {
      final handle = tester.ensureSemantics();
      final app = await identifiedApp();
      await app.pump(tester, size: phone);

      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));

      await tester.tap(find.text(S.navNewReport));
      await tester.pumpAndSettle();
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));

      await tester.tap(find.text(S.navSettings));
      await tester.pumpAndSettle();
      await expectLater(tester, meetsGuideline(androidTapTargetGuideline));
      await expectLater(tester, meetsGuideline(labeledTapTargetGuideline));
      handle.dispose();
    });

    for (final brightness in Brightness.values) {
      testWidgets('text contrast in the ${brightness.name} theme', (tester) async {
        final handle = tester.ensureSemantics();
        final app = await identifiedApp();
        await app.pump(tester, size: phone, brightness: brightness);

        await expectLater(tester, meetsGuideline(textContrastGuideline));
        handle.dispose();
      });
    }
  });

  testWidgets('both themes render the main screens without errors', (tester) async {
    for (final brightness in Brightness.values) {
      final app = await identifiedApp();
      await app.pump(tester, size: phone, brightness: brightness);
      for (final tab in [S.navNewReport, S.navSettings, S.navHome]) {
        await tester.tap(find.text(tab));
        await tester.pumpAndSettle();
        expect(tester.takeException(), isNull, reason: '$tab in ${brightness.name}');
      }
    }
  });

  testWidgets('malformed server data gives a readable error, never a crash', (tester) async {
    final client = MockClient((request) async => http.Response('<html>captive portal</html>', 200));
    final store = MemoryKeyValueStore()
      ..data['identity.citizen'] = jsonEncode(
          const Citizen(id: 1, fullName: 'Nimal', districtId: 1, districtName: 'Colombo').toJson());
    final app = TestApp(store: store, api: HttpDewecsApi(baseUrl: 'http://host:8080', client: client));
    await app.pump(tester, size: phone);

    expect(tester.takeException(), isNull);
    expect(find.text(S.unexpectedError), findsOneWidget);
    expect(find.text('The server sent an unexpected response.'), findsOneWidget);
  });

  testWidgets('the debug Sync queue screen lists each item with its state and last error', (tester) async {
    final app = await identifiedApp();
    await app.pump(tester, size: phone);
    await tester.tap(find.text(S.navSettings));
    await tester.pumpAndSettle();

    await tester.scrollUntilVisible(find.text(S.syncQueueScreen), 300, scrollable: find.byType(Scrollable).first);
    await tester.drag(find.byType(Scrollable).first, const Offset(0, -200));
    await tester.pumpAndSettle();
    await tester.tap(find.text(S.syncQueueScreen));
    await tester.pumpAndSettle();

    expect(find.textContaining('state: needsAttention'), findsOneWidget);
    expect(find.textContaining('last error: Validation failed'), findsOneWidget);
    expect(find.textContaining('capturedAt:'), findsOneWidget);
  });
}
