import 'dart:async';

import 'package:flutter/foundation.dart';

import '../api/api_exception.dart';
import '../api/dewecs_api.dart';
import '../models/queued_report.dart';
import '../models/report_submission.dart';
import '../state/clearable.dart';
import '../storage/photo_store.dart';
import '../storage/queue_repository.dart';
import 'backoff.dart';

enum _Outcome { next, stop }

/// Sends queued reports, oldest first, one run at a time (a second call during a run just asks for another pass).
///
/// - submitReport first; on 200 or 201 the server id is stored, then the photo is uploaded, then the item is synced.
/// - Retryable errors keep the item queued with a back-off; only a dead connection stops the whole run.
/// - Permanent errors move the item to "needs attention". A photo failure never undoes the report.
/// - A 404 on submit means either an unknown citizen (the server was reset) or a district that no longer exists.
class SyncService extends ChangeNotifier implements Clearable {
  SyncService({
    required this._repository,
    required this._api,
    required this._photos,
    required this._citizenId,
    required this._onIdentityLost,
    DateTime Function()? clock,
    this.autoSchedule = true,
  }) : _clock = clock ?? DateTime.now;

  /// Schedules a timer for the next due retry while the app is running (tests switch it off).
  final bool autoSchedule;

  final QueueRepository _repository;
  final DewecsApi Function() _api;
  final PhotoStore _photos;
  final int? Function() _citizenId;
  final Future<void> Function() _onIdentityLost;
  final DateTime Function() _clock;

  final List<QueuedReport> _items = [];
  bool _running = false;
  bool _again = false;
  bool _againForce = false;
  bool _disposed = false;
  Timer? _timer;

  List<QueuedReport> get items => List.unmodifiable(_items);

  bool get running => _running;

  /// Reports that are still waiting or being sent.
  int get activeCount => _items.where((i) => i.isActive).length;

  int get _nowMs => _clock().millisecondsSinceEpoch;

  void load() {
    _items
      ..clear()
      ..addAll(_repository.load());
    notifyListeners();
  }

  /// Adds the report and persists it at once. A second add with the same local id is ignored (double taps).
  Future<QueuedReport> enqueue(QueuedReport report) async {
    final existing = _items.where((i) => i.localId == report.localId);
    if (existing.isNotEmpty) {
      return existing.first;
    }
    _items.add(report);
    await _save();
    notifyListeners();
    return report;
  }

  Future<void> delete(String localId) async {
    final index = _items.indexWhere((i) => i.localId == localId);
    if (index < 0) {
      return;
    }
    final item = _items.removeAt(index);
    await _deletePhoto(item);
    await _save();
    notifyListeners();
  }

  /// Edit after a permanent error. Resets capturedAt, which is safe because the server never stored the report.
  Future<void> editAndRetry(
    String localId, {
    required int districtId,
    required String districtName,
    required String category,
    required String description,
    required double gpsLat,
    required double gpsLng,
    required String capturedAt,
    String? photoPath,
  }) async {
    final item = _items.where((i) => i.localId == localId).firstOrNull;
    if (item == null || item.reportStored) {
      return;
    }
    if (item.photoPath != null && item.photoPath != photoPath) {
      await _deletePhoto(item);
    }
    item.editAndRequeue(
      districtId: districtId,
      districtName: districtName,
      category: category,
      description: description,
      gpsLat: gpsLat,
      gpsLng: gpsLng,
      capturedAt: capturedAt,
      photoPath: photoPath,
    );
    await _save();
    notifyListeners();
    await syncNow(force: true);
  }

  /// After the user identified again (new server database): point every unsent report at the new citizen.
  Future<void> adoptCitizen(int citizenId) async {
    for (final item in _items.where((i) => !i.reportStored && i.state != QueueState.synced)) {
      item.citizenId = citizenId;
      if (item.state == QueueState.queued) {
        item.lastError = null;
        item.nextAttemptAtMs = null;
      }
    }
    await _save();
    notifyListeners();
  }

  /// Sent items stay on the phone until the server list returns their id; then they are dropped.
  Future<void> pruneKnown(Set<int> serverIds) async {
    final known = _items.where((i) => i.state == QueueState.synced && serverIds.contains(i.serverId)).toList();
    if (known.isEmpty) {
      return;
    }
    for (final item in known) {
      _items.remove(item);
      await _deletePhoto(item);
    }
    await _save();
    notifyListeners();
  }

  /// Runs one pass. [force] ignores back-off times (Send now, pull-to-refresh, connection regained, after Submit).
  Future<void> syncNow({bool force = false}) async {
    if (_running) {
      _again = true;
      _againForce = _againForce || force;
      return;
    }
    _running = true;
    notifyListeners();
    try {
      var passForce = force;
      do {
        _again = false;
        _againForce = false;
        if (_citizenId() == null) {
          break;
        }
        await _pass(passForce);
        passForce = _againForce;
      } while (_again);
    } finally {
      _running = false;
      _scheduleNext();
      if (!_disposed) {
        notifyListeners();
      }
    }
  }

  Future<void> _pass(bool force) async {
    final todo = _items.where((i) => i.state == QueueState.queued).toList()
      ..sort((a, b) => a.createdAtMs.compareTo(b.createdAtMs));
    for (final item in todo) {
      if (!_items.contains(item) || item.state != QueueState.queued) {
        continue;
      }
      final due = item.nextAttemptAtMs;
      if (!force && due != null && due > _nowMs) {
        continue;
      }
      if (await _process(item) == _Outcome.stop) {
        return;
      }
    }
  }

  Future<_Outcome> _process(QueuedReport item) async {
    item.markSending();
    notifyListeners();

    if (!item.reportStored) {
      try {
        final result = await _api().submitReport(ReportSubmission(
          citizenId: item.citizenId,
          districtId: item.districtId,
          category: item.category,
          description: item.description,
          gpsLat: item.gpsLat,
          gpsLng: item.gpsLng,
          capturedAt: item.capturedAt,
        ));
        item.serverId = result.report.id;
        await _save();
      } on ApiException catch (e) {
        return _submitFailed(item, e);
      }
    }

    if (item.photoPending) {
      try {
        final bytes = await _photos.read(item.photoPath!);
        await _api().uploadPhoto(item.serverId!, bytes);
        item.photoUploaded = true;
      } on ApiException catch (e) {
        if (e.retryable) {
          return _retryLater(item, e);
        }
        item.photoProblem = e.detail;
      } on Exception {
        item.photoProblem = 'The photo file is missing on this phone.';
      }
    }

    item.markSynced();
    await _save();
    notifyListeners();
    return _Outcome.next;
  }

  Future<_Outcome> _submitFailed(QueuedReport item, ApiException e) async {
    if (e.retryable) {
      return _retryLater(item, e);
    }
    if (e.status == 404) {
      return _notFound(item, e);
    }
    item.markNeedsAttention(e.detail, e.fieldErrors);
    await _save();
    notifyListeners();
    return _Outcome.next;
  }

  /// 404: either the citizen is unknown to the server (reset) or the district is gone. The reference data decides.
  Future<_Outcome> _notFound(QueuedReport item, ApiException e) async {
    final bool districtStillListed;
    try {
      final data = await _api().getReferenceData();
      districtStillListed = data.districts.any((d) => d.id == item.districtId);
    } on ApiException catch (lookup) {
      return _retryLater(item, lookup);
    }
    if (districtStillListed) {
      item.state = QueueState.queued;
      item.lastError = 'Waiting for you to identify again.';
      await _save();
      notifyListeners();
      await _onIdentityLost();
      return _Outcome.stop;
    }
    item.markNeedsAttention(e.detail, e.fieldErrors);
    await _save();
    notifyListeners();
    return _Outcome.next;
  }

  Future<_Outcome> _retryLater(QueuedReport item, ApiException e) async {
    final attemptsAfter = item.attempts + 1;
    item.markRetry(e.detail, _nowMs + backoffFor(attemptsAfter).inMilliseconds);
    await _save();
    notifyListeners();
    return e.isConnectionDown ? _Outcome.stop : _Outcome.next;
  }

  void _scheduleNext() {
    _timer?.cancel();
    _timer = null;
    if (!autoSchedule || _disposed) {
      return;
    }
    final due = _items
        .where((i) => i.state == QueueState.queued && i.nextAttemptAtMs != null)
        .map((i) => i.nextAttemptAtMs!)
        .fold<int?>(null, (best, next) => best == null || next < best ? next : best);
    if (due == null) {
      return;
    }
    final wait = Duration(milliseconds: (due - _nowMs).clamp(1000, 24 * 3600 * 1000));
    _timer = Timer(wait, () => syncNow());
  }

  Future<void> _save() => _repository.save(_items);

  Future<void> _deletePhoto(QueuedReport item) async {
    final path = item.photoPath;
    if (path != null) {
      await _photos.delete(path);
    }
  }

  @override
  Future<void> clearLocalData() async {
    _timer?.cancel();
    for (final item in List.of(_items)) {
      await _deletePhoto(item);
    }
    _items.clear();
    await _repository.clear();
    notifyListeners();
  }

  @override
  void dispose() {
    _disposed = true;
    _timer?.cancel();
    super.dispose();
  }
}
