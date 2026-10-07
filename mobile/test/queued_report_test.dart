import 'dart:convert';

import 'package:dewecs_mobile/models/queued_report.dart';
import 'package:dewecs_mobile/storage/key_value_store.dart';
import 'package:dewecs_mobile/storage/queue_repository.dart';
import 'package:dewecs_mobile/sync/backoff.dart';
import 'package:flutter_test/flutter_test.dart';

QueuedReport sample({String id = 'a', String? photo}) => QueuedReport(
      localId: id,
      createdAtMs: 1000,
      citizenId: 7,
      districtId: 1,
      districtName: 'Colombo',
      category: 'FLOOD',
      description: 'Water rising',
      gpsLat: 6.9271234,
      gpsLng: 79.8612345,
      capturedAt: '2026-10-07T14:03:11.123',
      photoPath: photo,
    );

void main() {
  group('state machine', () {
    test('starts queued and active', () {
      final r = sample();

      expect(r.state, QueueState.queued);
      expect(r.isActive, isTrue);
      expect(r.reportStored, isFalse);
    });

    test('a retryable failure stays queued, counts the attempt and keeps capturedAt', () {
      final r = sample()..markSending();

      r.markRetry('no connection', 5000);

      expect(r.state, QueueState.queued);
      expect(r.attempts, 1);
      expect(r.nextAttemptAtMs, 5000);
      expect(r.lastError, 'no connection');
      expect(r.capturedAt, '2026-10-07T14:03:11.123');
    });

    test('a permanent failure needs attention and keeps the server detail and field errors', () {
      final r = sample()..markNeedsAttention('Validation failed', {'description': 'too long'});

      expect(r.state, QueueState.needsAttention);
      expect(r.isActive, isFalse);
      expect(r.fieldErrors, {'description': 'too long'});
      expect(r.nextAttemptAtMs, isNull);
    });

    test('synced clears errors and attempts', () {
      final r = sample()
        ..markRetry('x', 1)
        ..markSynced();

      expect(r.state, QueueState.synced);
      expect(r.lastError, isNull);
      expect(r.attempts, 0);
    });

    test('edit and retry resets capturedAt, attempts and errors, and queues it again', () {
      final r = sample(photo: 'p.jpg')
        ..markNeedsAttention('bad', {'x': 'y'})
        ..attempts = 3;

      r.editAndRequeue(
        districtId: 2,
        districtName: 'Galle',
        category: 'LANDSLIDE',
        description: 'new text',
        gpsLat: 1,
        gpsLng: 2,
        capturedAt: '2026-10-08T10:00:00.000',
      );

      expect(r.state, QueueState.queued);
      expect(r.attempts, 0);
      expect(r.lastError, isNull);
      expect(r.fieldErrors, isEmpty);
      expect(r.capturedAt, '2026-10-08T10:00:00.000');
      expect(r.districtName, 'Galle');
      expect(r.photoPath, isNull);
    });

    test('photoPending only while a photo exists and is neither uploaded nor refused', () {
      final r = sample(photo: 'p.jpg');
      expect(r.photoPending, isTrue);
      r.photoUploaded = true;
      expect(r.photoPending, isFalse);
      final refused = sample(photo: 'p.jpg')..photoProblem = 'too big';
      expect(refused.photoPending, isFalse);
      expect(sample().photoPending, isFalse);
    });
  });

  group('persistence', () {
    test('sending is never persisted: it becomes queued', () {
      final r = sample()..markSending();

      expect(r.toJson()['state'], 'queued');
      expect(QueuedReport.fromJson({...r.toJson(), 'state': 'sending'}).state, QueueState.queued);
    });

    test('everything survives a JSON round trip', () {
      final r = sample(photo: 'p.jpg')
        ..serverId = 55
        ..photoUploaded = true
        ..markSynced();

      final back = QueuedReport.fromJson(jsonDecode(jsonEncode(r.toJson())) as Map<String, dynamic>);

      expect(back.serverId, 55);
      expect(back.state, QueueState.synced);
      expect(back.photoUploaded, isTrue);
      expect(back.capturedAt, '2026-10-07T14:03:11.123');
      expect(back.photoPath, 'p.jpg');
    });

    test('an unknown stored state becomes queued', () {
      expect(QueuedReport.fromJson({...sample().toJson(), 'state': 'mystery'}).state, QueueState.queued);
    });

    test('the repository loads what it saved and turns sending into queued', () async {
      final store = MemoryKeyValueStore();
      final repo = QueueRepository(store);
      final a = sample(id: 'a')..markSending();
      final b = sample(id: 'b')..markNeedsAttention('bad');

      await repo.save([a, b]);
      final loaded = repo.load();

      expect(loaded.map((r) => r.localId), ['a', 'b']);
      expect(loaded[0].state, QueueState.queued);
      expect(loaded[1].state, QueueState.needsAttention);
    });

    test('a corrupt entry or file never loses the rest', () async {
      final store = MemoryKeyValueStore();
      final good = jsonEncode(sample().toJson());
      store.data['queue.items'] = '[$good, {"nonsense": true}, 5]';
      expect(QueueRepository(store).load(), hasLength(1));

      store.data['queue.items'] = 'not json';
      expect(QueueRepository(store).load(), isEmpty);
      expect(QueueRepository(MemoryKeyValueStore()).load(), isEmpty);
    });
  });

  test('back-off schedule: 5 s, 15 s, 60 s, 5 min, then every 5 min', () {
    expect([for (var i = 1; i <= 7; i++) backoffFor(i)], const [
      Duration(seconds: 5),
      Duration(seconds: 15),
      Duration(seconds: 60),
      Duration(minutes: 5),
      Duration(minutes: 5),
      Duration(minutes: 5),
      Duration(minutes: 5),
    ]);
    expect(backoffFor(0), const Duration(seconds: 5));
  });
}
