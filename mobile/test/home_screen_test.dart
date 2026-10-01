import 'package:dewecs_mobile/api/demo_dewecs_api.dart';
import 'package:dewecs_mobile/models/queued_report.dart';
import 'package:dewecs_mobile/models/report_page.dart';
import 'package:dewecs_mobile/models/report_submission.dart';
import 'package:dewecs_mobile/models/sri_lanka_time.dart';
import 'package:dewecs_mobile/screens/identify_screen.dart';
import 'package:dewecs_mobile/strings.dart';
import 'package:dewecs_mobile/widgets/status_chip.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'test_helpers.dart';

/// Hides chosen report ids from the list, like a server that has not indexed a new report yet.
class StaleListApi extends DemoDewecsApi {
  StaleListApi() : super(latency: Duration.zero);

  final Set<int> hidden = {};

  @override
  Future<ReportPage> listCitizenReports(int citizenId, {int page = 0, int size = 20}) async {
    final real = await super.listCitizenReports(citizenId, page: page, size: size);
    return ReportPage(
      items: real.items.where((r) => !hidden.contains(r.id)).toList(),
      page: real.page,
      size: real.size,
      totalItems: real.totalItems,
      totalPages: real.totalPages,
    );
  }
}

QueuedReport queued(TestApp app, String id, {int createdAt = 1}) => QueuedReport(
      localId: id,
      createdAtMs: createdAt,
      citizenId: app.dependencies.identity.citizen!.id,
      districtId: 1,
      districtName: 'Colombo',
      category: 'LANDSLIDE',
      description: 'Local report $id',
      gpsLat: 6.9,
      gpsLng: 79.8,
      capturedAt: sriLankaNowString(),
    );

void main() {
  test('status labels follow the spec and unknown values are shown as received', () {
    expect(S.statusLabel('PENDING_REVIEW'), 'Waiting for review');
    expect(S.statusLabel('VERIFIED'), 'Reviewed by officers');
    expect(S.statusLabel('ACTIONED'), 'Action taken');
    expect(S.statusLabel('REJECTED'), 'Not accepted');
    expect(S.statusLabel('NEEDS_INFO'), 'More information needed');
    expect(S.statusLabel('PENDING_SYNC'), 'Waiting to send');
    expect(S.statusLabel('ESCALATED'), 'ESCALATED');
  });

  testWidgets('an unknown status chip shows the raw text', (tester) async {
    await tester.pumpWidget(MaterialApp(
      home: Builder(builder: (context) => Scaffold(body: StatusChip.forStatus(context, 'ESCALATED'))),
    ));

    expect(find.text('ESCALATED'), findsOneWidget);
  });

  testWidgets('lists the server reports newest first with a status chip each', (tester) async {
    final app = TestApp();
    await app.identify();
    await app.pump(tester);

    expect(find.text('Waiting for review'), findsOneWidget);
    expect(find.text('Reviewed by officers'), findsOneWidget);
    expect(find.text('Action taken'), findsOneWidget);
    expect(find.text('More information needed'), findsOneWidget);
    final first = tester.getTopLeft(find.text('River is overflowing near the bridge')).dy;
    final last = tester.getTopLeft(find.text('Strong winds, roof damage')).dy;
    expect(first, lessThan(last));
  });

  testWidgets('empty state when the citizen has no reports', (tester) async {
    final app = TestApp();
    await app.demo.identify(nic: '199512345678', fullName: 'First', phone: '0771234567', districtId: 1);
    await app.identify();
    await app.pump(tester);

    expect(find.text(S.homeEmptyTitle), findsOneWidget);
    expect(find.text(S.homeEmptyAction), findsOneWidget);

    await tester.tap(find.text(S.homeEmptyAction));
    await tester.pumpAndSettle();
    expect(find.text(S.categoryLabelText), findsOneWidget, reason: 'the button opens the report form');
  });

  testWidgets('waiting items come first with their own chip and a Send now button', (tester) async {
    final app = TestApp();
    await app.identify();
    app.demo.failNetwork = true;
    await app.dependencies.sync.enqueue(queued(app, 'a'));
    await app.pump(tester);

    expect(find.text('Local report a'), findsOneWidget);
    expect(find.text(S.stateQueued), findsOneWidget);
    expect(find.text(S.waitingToSend(1)), findsOneWidget);
    expect(find.text(S.sendNow), findsOneWidget);
    final local = tester.getTopLeft(find.text('Local report a')).dy;
    expect(find.text('River is overflowing near the bridge'), findsNothing, reason: 'no saved copy yet');
    expect(local, greaterThan(0));

    app.demo.failNetwork = false;
    await tester.tap(find.text(S.sendNow));
    await tester.pumpAndSettle();

    expect(app.dependencies.sync.items.single.state, QueueState.synced);
    expect(find.text(S.sendNow), findsNothing);
  });

  testWidgets('offline: the saved list stays visible under an offline banner', (tester) async {
    final app = TestApp();
    await app.identify();
    await app.pump(tester);
    expect(find.text('Action taken'), findsOneWidget);

    app.demo.failNetwork = true;
    await app.dependencies.reports.refresh();
    await tester.pumpAndSettle();

    expect(find.text(S.offlineBanner), findsOneWidget);
    expect(find.text('Action taken'), findsOneWidget);
  });

  testWidgets('a fresh start without a connection shows the cached list and the banner', (tester) async {
    final first = TestApp();
    await first.identify();
    await first.dependencies.reports.refresh();

    final second = TestApp(store: first.store, demo: DemoDewecsApi(latency: Duration.zero)..failNetwork = true);
    await second.pump(tester);

    expect(find.text(S.offlineBanner), findsOneWidget);
    expect(find.text('Action taken'), findsOneWidget);
  });

  testWidgets('a server error with nothing to show gives an error state with Try again', (tester) async {
    final app = TestApp();
    await app.identify();
    app.demo.failStatus = 500;
    await app.pump(tester);

    expect(find.text(S.unexpectedError), findsOneWidget);
    expect(find.text(S.retry), findsOneWidget);

    app.demo.failStatus = null;
    await tester.tap(find.text(S.retry));
    await tester.pumpAndSettle();
    expect(find.text('Action taken'), findsOneWidget);
  });

  testWidgets('Load more appends the next page of 20', (tester) async {
    final app = TestApp();
    await app.identify();
    final id = app.dependencies.identity.citizen!.id;
    for (var i = 0; i < 25; i++) {
      await app.demo.submitReport(ReportSubmission(
        citizenId: id,
        districtId: 1,
        category: 'FLOOD',
        description: 'Bulk $i',
        gpsLat: 1,
        gpsLng: 2,
        capturedAt: '2026-01-01T00:${i.toString().padLeft(2, '0')}:00.000',
      ));
    }
    await app.pump(tester);
    final reports = app.dependencies.reports;

    expect(reports.items, hasLength(20));
    expect(reports.hasMore, isTrue);

    await reports.loadMore();
    await tester.pumpAndSettle();

    expect(reports.items, hasLength(29));
    expect(reports.hasMore, isFalse);
    expect(reports.items.map((r) => r.id).toSet(), hasLength(29), reason: 'no duplicates');
  });

  testWidgets('a sent report not yet in the server list is kept, then dropped once the list returns its id', (tester) async {
    final api = StaleListApi();
    final app = TestApp(demo: api);
    await app.identify();
    await app.pump(tester);
    final item = queued(app, 'a');
    await app.dependencies.sync.enqueue(item);
    await app.dependencies.sync.syncNow(force: true);
    api.hidden.add(item.serverId!);

    await app.dependencies.reports.refresh();
    await tester.pumpAndSettle();
    expect(find.text('Local report a'), findsOneWidget);
    expect(app.dependencies.sync.items, hasLength(1));

    api.hidden.clear();
    await app.dependencies.reports.refresh();
    await tester.pumpAndSettle();

    expect(app.dependencies.sync.items, isEmpty);
    expect(find.text('Local report a'), findsOneWidget, reason: 'now shown from the server list');
  });

  testWidgets('a 404 on the list resets the identity, explains it and keeps the waiting reports', (tester) async {
    final app = TestApp();
    await app.identify();
    app.demo.failNetwork = true;
    await app.dependencies.sync.enqueue(queued(app, 'a'));
    await app.pump(tester);
    app.demo.failNetwork = false;
    app.demo.resetServer();

    await app.dependencies.reports.refresh();
    await tester.pumpAndSettle();

    expect(app.dependencies.identity.isIdentified, isFalse);
    expect(find.byType(IdentifyScreen), findsOneWidget);
    expect(find.text(S.identityReset), findsOneWidget);
    expect(app.dependencies.sync.items, hasLength(1), reason: 'the queue is kept');

    await enterField(tester, S.nicLabel, '199012345678');
    await enterField(tester, S.nameLabel, 'Nimal Perera');
    await enterField(tester, S.phoneLabel, '0771234567');
    await tester.tap(find.byType(DropdownButtonFormField<int>));
    await tester.pumpAndSettle();
    await tester.tap(find.text('Colombo').last);
    await tester.pumpAndSettle();
    await tester.tap(find.widgetWithText(FilledButton, S.identifyButton));
    await tester.pumpAndSettle();

    final newId = app.dependencies.identity.citizen!.id;
    final stored = app.demo.storedReports.where((r) => r.description == 'Local report a');
    expect(stored, hasLength(1), reason: 'sent after identifying again');
    expect(stored.single.citizenId, newId, reason: 'rewritten to the new citizen');
    final left = app.dependencies.sync.items.where((i) => i.localId == 'a');
    expect(left.every((i) => i.state == QueueState.synced), isTrue, reason: 'at most a sent copy is left');
  });
}
