import 'package:dewecs_mobile/models/queued_report.dart';
import 'package:dewecs_mobile/models/sri_lanka_time.dart';
import 'package:dewecs_mobile/state/url_opener.dart';
import 'package:dewecs_mobile/strings.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'home_screen_test.dart' show StaleListApi;
import 'test_helpers.dart';

Future<TestApp> openDetail(WidgetTester tester, String description) async {
  final app = TestApp();
  await app.identify();
  await app.pump(tester);
  await tester.tap(find.text(description));
  await tester.pumpAndSettle();
  return app;
}

QueuedReport refused(TestApp app, {String id = 'bad', String? photo}) => QueuedReport(
      localId: id,
      createdAtMs: 5,
      citizenId: app.dependencies.identity.citizen!.id,
      districtId: 1,
      districtName: 'Colombo',
      category: 'EARTHQUAKE',
      description: 'Shaking ground',
      gpsLat: 6.9,
      gpsLng: 79.8,
      capturedAt: sriLankaNowString(),
      photoPath: photo,
    );

void main() {
  testWidgets('an actioned report shows the officers action note', (tester) async {
    await openDetail(tester, 'Road under water in the market area');

    expect(find.text(S.detailActionNote), findsOneWidget);
    expect(find.text('Rescue team dispatched and the road was closed.'), findsOneWidget);
    expect(find.text('Action taken'), findsWidgets);
    expect(find.text('Galle'), findsOneWidget);
  });

  testWidgets('other statuses never show an action note', (tester) async {
    await openDetail(tester, 'River is overflowing near the bridge');

    expect(find.text(S.detailActionNote), findsNothing);
    expect(find.text('Waiting for review'), findsWidgets);
  });

  testWidgets('no photo gives a placeholder', (tester) async {
    await openDetail(tester, 'River is overflowing near the bridge');

    expect(find.text(S.photoPlaceholder), findsOneWidget);
  });

  testWidgets('Open in maps tries the geo address first and falls back to OpenStreetMap', (tester) async {
    final app = TestApp();
    await app.identify();
    await app.pump(tester);
    await tester.tap(find.text('River is overflowing near the bridge'));
    await tester.pumpAndSettle();
    app.openResults.addAll([false, true]);

    await tester.tap(find.text(S.openInMaps));
    await tester.pumpAndSettle();

    expect(app.opened.map((u) => u.scheme), ['geo', 'https']);
    expect(app.opened.first.toString(), geoUri(6.9271234, 79.8612345).toString());
    expect(app.opened.last.host, 'www.openstreetmap.org');
    expect(find.text(S.mapsFailed), findsNothing);
  });

  testWidgets('if nothing can open a map the user is told', (tester) async {
    final app = TestApp();
    await app.identify();
    await app.pump(tester);
    await tester.tap(find.text('River is overflowing near the bridge'));
    await tester.pumpAndSettle();
    app.openResults.addAll([false, false]);

    await tester.tap(find.text(S.openInMaps));
    await tester.pumpAndSettle();

    expect(find.text(S.mapsFailed), findsOneWidget);
  });

  testWidgets('a refused report shows the server reason and can be deleted', (tester) async {
    final app = TestApp();
    await app.identify();
    await app.dependencies.sync.enqueue(refused(app));
    await app.dependencies.sync.syncNow(force: true);
    await app.pump(tester);
    expect(find.text(S.stateNeedsAttention), findsOneWidget);

    await tester.tap(find.text('Shaking ground'));
    await tester.pumpAndSettle();
    expect(find.text(S.detailProblem), findsOneWidget);
    expect(find.textContaining('Invalid category'), findsWidgets);
    expect(find.text(S.editAndRetry), findsOneWidget);

    await tester.tap(find.text(S.deleteReport));
    await tester.pumpAndSettle();
    expect(find.text(S.deleteTitle), findsOneWidget);
    await tester.tap(find.widgetWithText(FilledButton, S.deleteReport));
    await tester.pumpAndSettle();

    expect(app.dependencies.sync.items, isEmpty);
    expect(find.text(S.reportDeleted), findsOneWidget);
    expect(find.text('Shaking ground'), findsNothing);
  });

  testWidgets('Edit and send again fixes the report and sends it with a fresh capturedAt', (tester) async {
    final app = TestApp();
    await app.identify();
    final item = refused(app);
    await app.dependencies.sync.enqueue(item);
    await app.dependencies.sync.syncNow(force: true);
    final oldCapturedAt = item.capturedAt;
    await app.pump(tester);

    await tester.tap(find.text('Shaking ground'));
    await tester.pumpAndSettle();
    await tester.tap(find.text(S.editAndRetry));
    await tester.pumpAndSettle();
    expect(find.text(S.editReportTitle), findsOneWidget);
    expect(find.text('Shaking ground'), findsOneWidget, reason: 'prefilled');

    await tester.tap(find.widgetWithText(ChoiceChip, S.categoryName('FLOOD')));
    await tester.pump();
    await tester.tap(find.widgetWithText(FilledButton, S.submitReport));
    await tester.pumpAndSettle();

    final fixed = app.dependencies.sync.items.single;
    expect(fixed.category, 'FLOOD');
    expect(fixed.state, QueueState.synced);
    expect(fixed.capturedAt, isNot(oldCapturedAt));
    expect(app.fake.storedReports.where((r) => r.description == 'Shaking ground'), hasLength(1));
  });

  testWidgets('a queued report that is not yet stored can be deleted but not edited', (tester) async {
    final app = TestApp();
    await app.identify();
    app.fake.failNetwork = true;
    await app.dependencies.sync.enqueue(refused(app, id: 'q')..category = 'FLOOD');
    await app.pump(tester);

    await tester.tap(find.text('Shaking ground'));
    await tester.pumpAndSettle();

    expect(find.text(S.editAndRetry), findsNothing);
    expect(find.text(S.deleteReport), findsOneWidget);
  });

  testWidgets('a sent report whose photo was refused shows the note until the server list returns it', (tester) async {
    final api = StaleListApi();
    final app = TestApp(fake: api);
    await app.identify();
    app.photos.put('bad.jpg', [1, 2, 3, 4]);
    final path = await app.photos.save('bad.jpg');
    final item = refused(app, photo: path)..category = 'FLOOD';
    await app.dependencies.sync.enqueue(item);
    await app.dependencies.sync.syncNow(force: true);
    api.hidden.add(item.serverId!);
    await app.pump(tester);

    expect(find.text(S.photoNotSent), findsOneWidget, reason: 'on the list card');
    await tester.tap(find.text('Shaking ground'));
    await tester.pumpAndSettle();
    expect(find.textContaining(S.photoNotSent), findsOneWidget);
  });
}
