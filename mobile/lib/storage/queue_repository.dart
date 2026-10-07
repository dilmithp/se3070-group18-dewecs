import 'dart:convert';

import '../models/queued_report.dart';
import 'key_value_store.dart';

/// Persists the queue as one JSON list. A corrupt entry is skipped instead of losing the whole queue.
class QueueRepository {
  QueueRepository(this._store);

  static const _key = 'queue.items';

  final KeyValueStore _store;

  List<QueuedReport> load() {
    final raw = _store.getString(_key);
    if (raw == null) {
      return [];
    }
    try {
      final list = jsonDecode(raw) as List<dynamic>;
      final items = <QueuedReport>[];
      for (final entry in list) {
        try {
          items.add(QueuedReport.fromJson(entry as Map<String, dynamic>));
        } on TypeError {
          continue;
        }
      }
      return items;
    } on FormatException {
      return [];
    } on TypeError {
      return [];
    }
  }

  Future<void> save(List<QueuedReport> items) =>
      _store.setString(_key, jsonEncode(items.map((i) => i.toJson()).toList()));

  Future<void> clear() => _store.remove(_key);
}
