import 'dart:convert';

import 'package:flutter/foundation.dart';

import '../api/api_exception.dart';
import '../api/dewecs_api.dart';
import '../models/reference_data.dart';
import '../storage/key_value_store.dart';
import 'clearable.dart';

/// Districts and categories. Cached after every successful fetch so forms work offline.
class ReferenceDataController extends ChangeNotifier implements Clearable {
  ReferenceDataController(this._store, this._api) {
    final raw = _store.getString(_key);
    if (raw != null) {
      try {
        _data = ReferenceData.fromJson(jsonDecode(raw) as Map<String, dynamic>);
      } on FormatException {
        _data = null;
      } on TypeError {
        _data = null;
      }
    }
  }

  static const _key = 'cache.referenceData';

  final KeyValueStore _store;
  final DewecsApi Function() _api;
  ReferenceData? _data;
  bool _loading = false;
  ApiException? _lastError;

  ReferenceData? get data => _data;

  bool get loading => _loading;

  ApiException? get lastError => _lastError;

  /// Fetches fresh data. On failure the cached copy stays; the error is kept in [lastError].
  Future<bool> refresh() async {
    _loading = true;
    _lastError = null;
    notifyListeners();
    try {
      final fresh = await _api().getReferenceData();
      _data = fresh;
      await _store.setString(_key, jsonEncode(fresh.toJson()));
      return true;
    } on ApiException catch (e) {
      _lastError = e;
      return false;
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  @override
  Future<void> clearLocalData() async {
    _data = null;
    await _store.remove(_key);
    notifyListeners();
  }
}
