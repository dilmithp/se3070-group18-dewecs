import 'dart:convert';

import 'package:flutter/foundation.dart';

import '../api/api_exception.dart';
import '../api/dewecs_api.dart';
import '../models/ground_report.dart';
import '../storage/key_value_store.dart';
import '../sync/sync_service.dart';
import 'clearable.dart';
import 'identity_controller.dart';

/// The citizen's reports from the server, newest first, 20 per page. The first page is cached so the list still
/// shows offline. A 404 means the server no longer knows this citizen, which resets the identity.
class ReportsController extends ChangeNotifier implements Clearable {
  ReportsController(this._store, this._api, this._identity, this._sync) {
    _loadCache();
  }

  static const _key = 'cache.reports';
  static const pageSize = 20;

  final KeyValueStore _store;
  final DewecsApi Function() _api;
  final IdentityController _identity;
  final SyncService _sync;

  List<GroundReport> _items = [];
  int? _ownerId;
  int _page = 0;
  bool _hasMore = false;
  bool _loading = false;
  bool _loadingMore = false;
  bool _offline = false;
  ApiException? _error;

  /// The cached or fetched reports, only if they belong to the citizen who is identified now.
  List<GroundReport> get items => _ownerId != null && _ownerId == _identity.citizen?.id ? _items : const [];

  bool get hasMore => _hasMore;

  bool get loading => _loading;

  bool get loadingMore => _loadingMore;

  /// The last refresh failed because the connection is down: what is shown is saved data.
  bool get offline => _offline;

  /// The last request failed for another reason (for example a server error).
  ApiException? get error => _error;

  void _loadCache() {
    final raw = _store.getString(_key);
    if (raw == null) {
      return;
    }
    try {
      final json = jsonDecode(raw) as Map<String, dynamic>;
      _ownerId = (json['citizenId'] as num).toInt();
      _items = (json['items'] as List<dynamic>).map((r) => GroundReport.fromJson(r as Map<String, dynamic>)).toList();
    } on FormatException {
      _items = [];
    } on TypeError {
      _items = [];
    }
  }

  Future<void> _saveCache(int citizenId) => _store.setString(
        _key,
        jsonEncode({'citizenId': citizenId, 'items': _items.take(pageSize).map((r) => r.toJson()).toList()}),
      );

  /// Fetches the first page. On failure the cached list stays and the reason is kept.
  Future<void> refresh() async {
    final citizen = _identity.citizen;
    if (citizen == null || _loading) {
      return;
    }
    _loading = true;
    _error = null;
    notifyListeners();
    try {
      final page = await _api().listCitizenReports(citizen.id, page: 0, size: pageSize);
      _items = page.items;
      _ownerId = citizen.id;
      _page = 0;
      _hasMore = page.hasMore;
      _offline = false;
      await _saveCache(citizen.id);
      await _sync.pruneKnown(_items.map((r) => r.id).toSet());
    } on ApiException catch (e) {
      await _failed(e);
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<void> loadMore() async {
    final citizen = _identity.citizen;
    if (citizen == null || _loading || _loadingMore || !_hasMore) {
      return;
    }
    _loadingMore = true;
    _error = null;
    notifyListeners();
    try {
      final page = await _api().listCitizenReports(citizen.id, page: _page + 1, size: pageSize);
      final known = _items.map((r) => r.id).toSet();
      _items = [..._items, ...page.items.where((r) => !known.contains(r.id))];
      _page = page.page;
      _hasMore = page.hasMore;
      _offline = false;
      await _sync.pruneKnown(_items.map((r) => r.id).toSet());
    } on ApiException catch (e) {
      await _failed(e);
    } finally {
      _loadingMore = false;
      notifyListeners();
    }
  }

  Future<void> _failed(ApiException e) async {
    _error = e;
    _offline = e.isConnectionDown;
    if (e.status == 404) {
      // Unknown citizen: the server database was reset. Forget the identity; the queue is kept.
      _items = [];
      _ownerId = null;
      _hasMore = false;
      await _store.remove(_key);
      await _identity.clear();
    }
  }

  @override
  Future<void> clearLocalData() async {
    _items = [];
    _ownerId = null;
    _hasMore = false;
    _offline = false;
    _error = null;
    await _store.remove(_key);
    notifyListeners();
  }
}
