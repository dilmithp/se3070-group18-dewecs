import 'dart:convert';

import 'package:flutter/foundation.dart';

import '../api/api_exception.dart';
import '../api/dewecs_api.dart';
import '../models/relief_supply.dart';
import '../storage/key_value_store.dart';
import 'clearable.dart';

/// Fetches relief supplies. Cached after every successful fetch so it works offline.
class ReliefController extends ChangeNotifier implements Clearable {
  ReliefController(this._store, this._api) {
    final raw = _store.getString(_key);
    if (raw != null) {
      try {
        final decoded = jsonDecode(raw) as List<dynamic>;
        _supplies = decoded.map((e) => ReliefSupply.fromJson(e as Map<String, dynamic>)).toList();
      } on FormatException {
        _supplies = null;
      } on TypeError {
        _supplies = null;
      }
    }
  }

  static const _key = 'cache.reliefSupplies';

  final KeyValueStore _store;
  final DewecsApi Function() _api;
  
  List<ReliefSupply>? _supplies;
  bool _loading = false;
  ApiException? _lastError;
  String? _currentType;

  List<ReliefSupply>? get supplies => _supplies;
  bool get loading => _loading;
  ApiException? get lastError => _lastError;
  String? get currentType => _currentType;

  /// Fetches fresh data based on the current filter.
  Future<bool> refresh() async {
    _loading = true;
    _lastError = null;
    notifyListeners();
    try {
      final fresh = await _api().listSupplies(type: _currentType);
      _supplies = fresh;
      await _store.setString(_key, jsonEncode(fresh.map((s) => s.toJson()).toList()));
      return true;
    } on ApiException catch (e) {
      _lastError = e;
      return false;
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  void setType(String? type) {
    if (_currentType == type) return;
    _currentType = type;
    notifyListeners();
    refresh();
  }

  @override
  Future<void> clearLocalData() async {
    _supplies = null;
    _currentType = null;
    await _store.remove(_key);
    notifyListeners();
  }
}

