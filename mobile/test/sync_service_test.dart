import 'package:dewecs_mobile/api/api_exception.dart';
import 'package:dewecs_mobile/api/demo_dewecs_api.dart';
import 'package:dewecs_mobile/models/ground_report.dart';
import 'package:dewecs_mobile/models/queued_report.dart';
import 'package:dewecs_mobile/models/reference_data.dart';
import 'package:dewecs_mobile/models/report_submission.dart';
import 'package:dewecs_mobile/models/sri_lanka_time.dart';
import 'package:dewecs_mobile/storage/key_value_store.dart';
import 'package:dewecs_mobile/storage/photo_store.dart';
import 'package:dewecs_mobile/storage/queue_repository.dart';
import 'package:dewecs_mobile/sync/sync_service.dart';
import 'package:flutter_test/flutter_test.dart';

const png = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3];

/// Fails the first [photoFailures] uploads like a dropped connection.
class PhotoFlakyApi extends DemoDewecsApi {
  PhotoFlakyApi({this.photoFailures = 0}) : super(latency: Duration.zero);

  int photoFailures;
  bool referenceDataFails = false;

  @override
  Future<GroundReport> uploadPhoto(int reportId, List<int> bytes, {String filename = 'photo.jpg'}) async {
    if (photoFailures > 0) {
      photoFailures--;
      throw const ApiException(kind: ApiErrorKind.network, detail: 'photo connection dropped');
    }
    return super.uploadPhoto(reportId, bytes, filename: filename);
  }

  @override
  Future<ReferenceData> getReferenceData() async {
    if (referenceDataFails) {
      throw const ApiException(kind: ApiErrorKind.network, detail: 'down');
    }
    return super.getReferenceData();
  }
}

void main() {
  late PhotoFlakyApi api;
  late MemoryKeyValueStore store;
  late MemoryPhotoStore photos;
  late SyncService sync;
  late DateTime now;
  int? citizenId;
  int identityLostCalls = 0;

  setUp(() async {
    api = PhotoFlakyApi();
    store = MemoryKeyValueStore();
    photos = MemoryPhotoStore();
    now = DateTime(2026, 10, 7, 12);
    identityLostCalls = 0;
    citizenId = (await api.identify(nic: '199012345678', fullName: 'Nimal', phone: '0771234567', districtId: 1)).id;
    sync = SyncService(
      repository: QueueRepository(store),
      api: () => api,
      photos: photos,
      citizenId: () => citizenId,
      onIdentityLost: () async {
        identityLostCalls++;
        citizenId = null;
      },
      clock: () => now,
      autoSchedule: false,
    );
  });

  tearDown(() => sync.dispose());

  QueuedReport report(String id, {int createdAt = 1, String? photo, String? category, int? districtId, String? capturedAt}) =>
      QueuedReport(
        localId: id,
        createdAtMs: createdAt,
        citizenId: citizenId!,
        districtId: districtId ?? 1,
        districtName: 'Colombo',
        category: category ?? 'FLOOD',
        description: 'Water rising $id',
        gpsLat: 6.9271234,
        gpsLng: 79.8612345,
        capturedAt: capturedAt ?? '2026-10-07T${(10 + createdAt).toString().padLeft(2, '0')}:00:00.000',
        photoPath: photo,
      );

  QueuedReport byId(String id) => sync.items.firstWhere((i) => i.localId == id);

  test('success: the report is stored on the server and the item becomes synced', () async {
    await sync.enqueue(report('a'));

    await sync.syncNow(force: true);

    expect(byId('a').state, QueueState.synced);
    expect(byId('a').serverId, isNotNull);
    final stored = api.storedReports.firstWhere((r) => r.id == byId('a').serverId);
    expect(stored.status, 'PENDING_REVIEW');
    expect(stored.description, 'Water rising a');
  });

  test('offline then online: stays queued with a back-off, stops the run, then sends', () async {
    await sync.enqueue(report('a', createdAt: 1));
    await sync.enqueue(report('b', createdAt: 2));
    api.failNetwork = true;

    await sync.syncNow(force: true);

    expect(byId('a').state, QueueState.queued);
    expect(byId('a').attempts, 1);
    expect(byId('a').lastError, isNotNull);
    expect(byId('b').attempts, 0, reason: 'a dead connection stops the whole run');
    expect(byId('a').nextAttemptAtMs, now.add(const Duration(seconds: 5)).millisecondsSinceEpoch);

    api.failNetwork = false;
    await sync.syncNow();
    expect(byId('a').state, QueueState.queued, reason: 'not due yet, back-off respected');

    now = now.add(const Duration(seconds: 6));
    await sync.syncNow();
    expect(byId('a').state, QueueState.synced);
    expect(byId('b').state, QueueState.synced);
  });

  test('force ignores the back-off', () async {
    await sync.enqueue(report('a'));
    api.failNetwork = true;
    await sync.syncNow(force: true);
    api.failNetwork = false;

    await sync.syncNow(force: true);

    expect(byId('a').state, QueueState.synced);
  });

  test('back-off grows with every failed attempt', () async {
    await sync.enqueue(report('a'));
    api.failNetwork = true;
    final waits = <int>[];
    for (var i = 0; i < 5; i++) {
      await sync.syncNow(force: true);
      waits.add(byId('a').nextAttemptAtMs! - now.millisecondsSinceEpoch);
    }

    expect(waits, [5000, 15000, 60000, 300000, 300000]);
  });

  test('a 500 keeps every item queued but continues with the next one, then succeeds', () async {
    await sync.enqueue(report('a', createdAt: 1));
    await sync.enqueue(report('b', createdAt: 2));
    api.failStatus = 500;

    await sync.syncNow(force: true);

    expect(byId('a').attempts, 1);
    expect(byId('b').attempts, 1, reason: 'a server error does not stop the run');
    expect(byId('a').state, QueueState.queued);

    api.failStatus = null;
    await sync.syncNow(force: true);
    expect(sync.items.every((i) => i.state == QueueState.synced), isTrue);
  });

  test('a 429 is retried later like a server error', () async {
    await sync.enqueue(report('a'));
    api.failStatus = 429;

    await sync.syncNow(force: true);

    expect(byId('a').state, QueueState.queued);
    expect(byId('a').attempts, 1);
  });

  test('a 400 is permanent: needs attention with the server detail, the next item still goes out', () async {
    await sync.enqueue(report('bad', createdAt: 1, category: 'EARTHQUAKE'));
    await sync.enqueue(report('good', createdAt: 2));

    await sync.syncNow(force: true);

    expect(byId('bad').state, QueueState.needsAttention);
    expect(byId('bad').lastError, contains('Invalid category'));
    expect(byId('good').state, QueueState.synced);

    await sync.syncNow(force: true);
    expect(byId('bad').attempts, 0, reason: 'permanent errors are not retried automatically');
  });

  test('edit and retry sends the corrected report with a new capturedAt', () async {
    await sync.enqueue(report('bad', category: 'EARTHQUAKE'));
    await sync.syncNow(force: true);
    final oldCapturedAt = byId('bad').capturedAt;

    await sync.editAndRetry(
      'bad',
      districtId: 1,
      districtName: 'Colombo',
      category: 'FLOOD',
      description: 'fixed',
      gpsLat: 6.9,
      gpsLng: 79.8,
      capturedAt: sriLankaNowString(),
    );

    expect(byId('bad').state, QueueState.synced);
    expect(byId('bad').capturedAt, isNot(oldCapturedAt));
    expect(api.storedReports.where((r) => r.description == 'fixed'), hasLength(1));
  });

  test('replay: a lost response never creates a duplicate and the item still ends up synced', () async {
    final item = report('a');
    // The first attempt reached the server, but the phone never saw the answer.
    final first = await api.submitReport(_submission(item));
    await sync.enqueue(item);

    await sync.syncNow(force: true);

    expect(byId('a').state, QueueState.synced);
    expect(byId('a').serverId, first.report.id);
    expect(api.storedReports.where((r) => r.description == 'Water rising a'), hasLength(1));
  });

  test('the same capturedAt goes out on every retry', () async {
    await sync.enqueue(report('a', capturedAt: '2026-10-07T09:30:00.000'));
    api.failStatus = 503;
    await sync.syncNow(force: true);
    await sync.syncNow(force: true);
    api.failStatus = null;
    await sync.syncNow(force: true);

    expect(byId('a').capturedAt, '2026-10-07T09:30:00.000');
    final stored = api.storedReports.where((r) => r.description == 'Water rising a').single;
    expect(formatLocal(stored), '2026-10-07T09:30:00.000');
  });

  test('photo: uploaded after the report, then synced', () async {
    photos.put('picked.jpg', png);
    final path = await photos.save('picked.jpg');
    await sync.enqueue(report('a', photo: path));

    await sync.syncNow(force: true);

    expect(byId('a').state, QueueState.synced);
    expect(byId('a').photoUploaded, isTrue);
    final stored = api.storedReports.firstWhere((r) => r.id == byId('a').serverId);
    expect(stored.photoUrl, startsWith('/api/v1/photos/'));
  });

  test('a photo failure never undoes the report and is retried separately', () async {
    api.photoFailures = 1;
    photos.put('picked.jpg', png);
    final path = await photos.save('picked.jpg');
    await sync.enqueue(report('a', photo: path));

    await sync.syncNow(force: true);

    expect(byId('a').serverId, isNotNull, reason: 'the report is stored');
    expect(byId('a').state, QueueState.queued, reason: 'photo retried later');
    expect(byId('a').photoPending, isTrue);
    final storedBefore = api.storedReports.length;

    await sync.syncNow(force: true);

    expect(byId('a').state, QueueState.synced);
    expect(byId('a').photoUploaded, isTrue);
    expect(api.storedReports.length, storedBefore, reason: 'the report was not submitted again');
  });

  test('a permanent photo error leaves the item synced with a visible note', () async {
    photos.put('bad.jpg', [1, 2, 3, 4]);
    final path = await photos.save('bad.jpg');
    await sync.enqueue(report('a', photo: path));

    await sync.syncNow(force: true);

    expect(byId('a').state, QueueState.synced);
    expect(byId('a').photoProblem, contains('JPEG, PNG or WebP'));
    expect(byId('a').photoUploaded, isFalse);
  });

  test('a missing photo file is a permanent photo problem, not a crash', () async {
    await sync.enqueue(report('a', photo: 'memory://gone'));

    await sync.syncNow(force: true);

    expect(byId('a').state, QueueState.synced);
    expect(byId('a').photoProblem, isNotNull);
  });

  test('404 on submit with the district still listed: the citizen is unknown, identity is reset, the queue is kept',
      () async {
    await sync.enqueue(report('a'));
    api.resetServer();

    await sync.syncNow(force: true);

    expect(identityLostCalls, 1);
    expect(byId('a').state, QueueState.queued);
    expect(byId('a').lastError, contains('identify'));

    await sync.syncNow(force: true);
    expect(identityLostCalls, 1, reason: 'no identity, nothing is sent');

    final fresh = await api.identify(nic: '199012345678', fullName: 'Nimal', phone: '0771234567', districtId: 1);
    citizenId = fresh.id;
    await sync.adoptCitizen(fresh.id);
    await sync.syncNow(force: true);

    expect(byId('a').state, QueueState.synced);
    expect(byId('a').citizenId, fresh.id);
  });

  test('404 on submit with the district gone: needs attention with the server detail', () async {
    await sync.enqueue(report('a', districtId: 99));

    await sync.syncNow(force: true);

    expect(byId('a').state, QueueState.needsAttention);
    expect(byId('a').lastError, contains('District not found'));
    expect(identityLostCalls, 0);
  });

  test('404 but the reference data cannot be fetched: stays queued and retries later', () async {
    await sync.enqueue(report('a'));
    api.resetServer();
    api.referenceDataFails = true;

    await sync.syncNow(force: true);

    expect(byId('a').state, QueueState.queued);
    expect(byId('a').attempts, 1);
    expect(identityLostCalls, 0);
  });

  test('single flight: two overlapping runs send the report once', () async {
    final slow = PhotoFlakyApi();
    api = slow;
    final slowApi = DemoDewecsApi(latency: const Duration(milliseconds: 30));
    final id = (await slowApi.identify(nic: '199012345678', fullName: 'N', phone: '0771234567', districtId: 1)).id;
    citizenId = id;
    final service = SyncService(
      repository: QueueRepository(MemoryKeyValueStore()),
      api: () => slowApi,
      photos: photos,
      citizenId: () => id,
      onIdentityLost: () async {},
      autoSchedule: false,
    );
    await service.enqueue(report('a'));
    final before = slowApi.callCount;

    final first = service.syncNow(force: true);
    final second = service.syncNow(force: true);
    await Future.wait([first, second]);

    expect(slowApi.callCount - before, 1);
    expect(service.items.single.state, QueueState.synced);
    service.dispose();
  });

  test('oldest first', () async {
    await sync.enqueue(report('c', createdAt: 3));
    await sync.enqueue(report('a', createdAt: 1));
    await sync.enqueue(report('b', createdAt: 2));

    await sync.syncNow(force: true);

    expect(byId('a').serverId!, lessThan(byId('b').serverId!));
    expect(byId('b').serverId!, lessThan(byId('c').serverId!));
  });

  test('enqueue ignores a second add with the same local id (double tap)', () async {
    await sync.enqueue(report('a'));
    await sync.enqueue(report('a'));

    expect(sync.items, hasLength(1));
  });

  test('without an identified citizen nothing is sent', () async {
    await sync.enqueue(report('a'));
    final calls = api.callCount;
    citizenId = null;

    await sync.syncNow(force: true);

    expect(api.callCount, calls);
    expect(byId('a').state, QueueState.queued);
  });

  test('the queue is persisted and a stale "sending" comes back as queued after a restart', () async {
    await sync.enqueue(report('a'));
    byId('a').markSending();
    await sync.enqueue(report('b'));

    final restarted = SyncService(
      repository: QueueRepository(store),
      api: () => api,
      photos: photos,
      citizenId: () => citizenId,
      onIdentityLost: () async {},
      autoSchedule: false,
    )..load();

    expect(restarted.items.map((i) => i.localId), ['a', 'b']);
    expect(restarted.items.every((i) => i.state == QueueState.queued), isTrue);
    restarted.dispose();
  });

  test('pruneKnown drops synced items the server list now contains, and their photo', () async {
    photos.put('picked.jpg', png);
    final path = await photos.save('picked.jpg');
    await sync.enqueue(report('a', photo: path));
    await sync.enqueue(report('b', createdAt: 2));
    await sync.syncNow(force: true);
    final idA = byId('a').serverId!;

    await sync.pruneKnown({idA});

    expect(sync.items.map((i) => i.localId), ['b']);
    expect(photos.files.containsKey(path), isFalse);
  });

  test('delete removes the item and its photo', () async {
    photos.put('picked.jpg', png);
    final path = await photos.save('picked.jpg');
    await sync.enqueue(report('a', photo: path));

    await sync.delete('a');

    expect(sync.items, isEmpty);
    expect(photos.files.containsKey(path), isFalse);
    expect(QueueRepository(store).load(), isEmpty);
  });

  test('clearLocalData wipes items, photos and the stored queue', () async {
    photos.put('picked.jpg', png);
    final path = await photos.save('picked.jpg');
    await sync.enqueue(report('a', photo: path));

    await sync.clearLocalData();

    expect(sync.items, isEmpty);
    expect(photos.files.containsKey(path), isFalse);
    expect(QueueRepository(store).load(), isEmpty);
  });
}

ReportSubmission _submission(QueuedReport r) => ReportSubmission(
      citizenId: r.citizenId,
      districtId: r.districtId,
      category: r.category,
      description: r.description,
      gpsLat: r.gpsLat,
      gpsLng: r.gpsLng,
      capturedAt: r.capturedAt,
    );

String formatLocal(GroundReport r) => formatContractTime(r.submittedAt);
