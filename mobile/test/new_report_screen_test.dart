import 'package:dewecs_mobile/models/queued_report.dart';
import 'package:dewecs_mobile/state/location_service.dart';
import 'package:dewecs_mobile/strings.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

import 'test_helpers.dart';

Future<TestApp> openForm(WidgetTester tester) async {
  final app = TestApp();
  await app.identify();
  await app.pump(tester);
  await tester.tap(find.text(S.navNewReport));
  await tester.pumpAndSettle();
  return app;
}

Future<void> fillForm(WidgetTester tester) async {
  await tester.tap(find.widgetWithText(ChoiceChip, S.categoryName('FLOOD')));
  await tester.pump();
  await enterField(tester, S.descriptionLabel, 'Water over the road near the school');
  await enterField(tester, S.latLabel, '6.9271234');
  await enterField(tester, S.lngLabel, '79.8612345');
}

Finder get sendButton => find.widgetWithText(FilledButton, S.submitReport);

void main() {
  testWidgets('an empty submit shows every error and enqueues nothing', (tester) async {
    final app = await openForm(tester);

    await tester.tap(sendButton);
    await tester.pumpAndSettle();

    expect(find.text(S.categoryRequired), findsOneWidget);
    expect(find.text(S.descriptionRequired), findsOneWidget);
    expect(find.text(S.latRequired), findsOneWidget);
    expect(find.text(S.lngRequired), findsOneWidget);
    expect(app.dependencies.sync.items, isEmpty);
  });

  testWidgets('a valid submit enqueues exactly one item, sends it and says so', (tester) async {
    final app = await openForm(tester);
    await fillForm(tester);

    await tester.tap(sendButton);
    await tester.pumpAndSettle();

    final items = app.dependencies.sync.items;
    expect(items, hasLength(1));
    expect(items.single.state, QueueState.synced);
    expect(items.single.category, 'FLOOD');
    expect(items.single.description, 'Water over the road near the school');
    expect(items.single.gpsLat, 6.9271234);
    expect(items.single.capturedAt, matches(RegExp(r'^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}$')));
    expect(find.text(S.reportSent), findsOneWidget);
    expect(app.fake.storedReports.where((r) => r.description == 'Water over the road near the school'), hasLength(1));
  });

  testWidgets('offline: the report is saved on the phone and waits', (tester) async {
    final app = await openForm(tester);
    await fillForm(tester);
    app.fake.failNetwork = true;

    await tester.tap(sendButton);
    await tester.pumpAndSettle();

    final item = app.dependencies.sync.items.single;
    expect(item.state, QueueState.queued);
    expect(item.reportStored, isFalse);
    expect(find.text(S.reportSavedOnPhone), findsOneWidget);
  });

  testWidgets('a double tap cannot create two items', (tester) async {
    final app = await openForm(tester);
    await fillForm(tester);

    await tester.tap(sendButton);
    await tester.tap(sendButton, warnIfMissed: false);
    await tester.pumpAndSettle();

    expect(app.dependencies.sync.items, hasLength(1));
  });

  testWidgets('the form is cleared after a submit so the next report starts empty', (tester) async {
    await openForm(tester);
    await fillForm(tester);
    await tester.tap(sendButton);
    await tester.pumpAndSettle();

    await tester.tap(find.text(S.navNewReport));
    await tester.pumpAndSettle();

    expect(find.text('Water over the road near the school'), findsNothing);
  });

  testWidgets('out-of-range and non-numeric coordinates are refused', (tester) async {
    final app = await openForm(tester);
    await fillForm(tester);
    await enterField(tester, S.latLabel, '91');
    await enterField(tester, S.lngLabel, 'abc');

    await tester.tap(sendButton);
    await tester.pumpAndSettle();

    expect(find.text(S.latRange), findsOneWidget);
    expect(find.text(S.lngInvalid), findsOneWidget);
    expect(app.dependencies.sync.items, isEmpty);
  });

  testWidgets('a decimal comma is accepted and California coordinates are fine', (tester) async {
    final app = await openForm(tester);
    await fillForm(tester);
    await enterField(tester, S.latLabel, '37,4220');
    await enterField(tester, S.lngLabel, '-122.0841');

    await tester.tap(sendButton);
    await tester.pumpAndSettle();

    expect(app.dependencies.sync.items.single.gpsLat, 37.422);
    expect(app.dependencies.sync.items.single.gpsLng, -122.0841);
  });

  testWidgets('Use my location fills the fields and shows the accuracy', (tester) async {
    await openForm(tester);

    await tester.tap(find.text(S.useMyLocation));
    await tester.pumpAndSettle();

    expect(find.widgetWithText(TextFormField, S.latLabel), findsOneWidget);
    expect(find.text('6.9271000'), findsOneWidget);
    expect(find.text('79.8612000'), findsOneWidget);
    expect(find.text(S.accuracyMeters(12)), findsOneWidget);
  });

  testWidgets('location problems get a clear message and the manual fields stay usable', (tester) async {
    final app = await openForm(tester);
    app.location.problem = LocationProblem.deniedForever;

    await tester.tap(find.text(S.useMyLocation));
    await tester.pumpAndSettle();

    expect(find.text(S.locationDeniedForever), findsOneWidget);
    await tester.tap(find.text(S.openAppSettings));
    expect(app.location.settingsOpened, 1);

    app.location.problem = LocationProblem.servicesOff;
    await tester.tap(find.text(S.useMyLocation));
    await tester.pumpAndSettle();
    expect(find.text(S.locationServicesOff), findsOneWidget);

    app.location.problem = LocationProblem.denied;
    await tester.tap(find.text(S.useMyLocation));
    await tester.pumpAndSettle();
    expect(find.text(S.locationDenied), findsOneWidget);

    app.location.problem = LocationProblem.unavailable;
    await tester.tap(find.text(S.useMyLocation));
    await tester.pumpAndSettle();
    expect(find.text(S.locationUnavailable), findsOneWidget);
  });

  testWidgets('a picked photo is copied into the store and travels with the report', (tester) async {
    final app = await openForm(tester);
    await fillForm(tester);

    await tester.tap(find.text(S.takePhoto));
    await tester.pumpAndSettle();
    expect(find.text(S.removePhoto), findsOneWidget);

    await tester.tap(sendButton);
    await tester.pumpAndSettle();

    final item = app.dependencies.sync.items.single;
    expect(item.photoPath, startsWith('memory://'));
    expect(item.photoUploaded, isTrue);
    expect(app.fake.storedReports.firstWhere((r) => r.id == item.serverId).photoUrl, isNotNull);
  });

  testWidgets('removing the photo deletes the stored copy', (tester) async {
    final app = await openForm(tester);

    await tester.tap(find.text(S.choosePhoto));
    await tester.pumpAndSettle();
    expect(app.photos.files.keys.where((k) => k.startsWith('memory://')), hasLength(1));

    await tester.tap(find.text(S.removePhoto));
    await tester.pumpAndSettle();

    expect(app.photos.files.keys.where((k) => k.startsWith('memory://')), isEmpty);
    expect(find.text(S.takePhoto), findsOneWidget);
  });

  testWidgets('cancelling the picker changes nothing', (tester) async {
    final app = await openForm(tester);
    app.picker.cancel = true;

    await tester.tap(find.text(S.takePhoto));
    await tester.pumpAndSettle();

    expect(find.text(S.removePhoto), findsNothing);
  });
}
