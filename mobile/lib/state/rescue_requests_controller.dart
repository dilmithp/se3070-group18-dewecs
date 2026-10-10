import 'package:flutter/foundation.dart';

import '../api/api_exception.dart';
import '../api/operations_api.dart';
import '../models/district.dart';
import '../models/rescue_request.dart';
import 'clearable.dart';

/// The rescue request list with its three filters (status, priority, district). A failed refresh keeps the last list
/// on screen and remembers why.
class RescueRequestsController extends ChangeNotifier implements Clearable {
  RescueRequestsController(this._api);

  final OperationsApi Function() _api;

  List<RescueRequest> _requests = [];
  List<District> _districts = [];
  List<String> _statuses = const [];
  List<String> _priorities = const [];
  String? _status;
  String? _priority;
  int? _districtId;
  bool _loaded = false;
  bool _loading = false;
  bool _again = false;
  ApiException? _error;

  List<RescueRequest> get requests => _requests;

  /// What the filter controls offer (empty until the first answer).
  List<District> get districts => _districts;

  List<String> get statuses => _statuses;

  List<String> get priorities => _priorities;

  String? get statusFilter => _status;

  String? get priorityFilter => _priority;

  int? get districtFilter => _districtId;

  bool get hasFilter => _status != null || _priority != null || _districtId != null;

  /// True once a list has been received, even an empty one.
  bool get loaded => _loaded;

  bool get loading => _loading;

  ApiException? get error => _error;

  /// The last refresh failed because the connection is down: what is shown is the last list received.
  bool get offline => _error?.isConnectionDown ?? false;

  Future<void> refresh() async {
    if (_loading) {
      _again = true;
      return;
    }
    _loading = true;
    notifyListeners();
    try {
      do {
        _again = false;
        _error = null;
        try {
          final list = await _api().listRescueRequests(status: _status, priority: _priority, districtId: _districtId);
          _requests = list.requests;
          _districts = list.districts;
          _statuses = list.statuses;
          _priorities = list.priorities;
          _loaded = true;
        } on ApiException catch (e) {
          _error = e;
        }
      } while (_again);
    } finally {
      _loading = false;
      notifyListeners();
    }
  }

  Future<void> setFilters({String? status, String? priority, int? districtId}) {
    _status = status;
    _priority = priority;
    _districtId = districtId;
    return refresh();
  }

  @override
  Future<void> clearLocalData() async {
    _requests = [];
    _districts = [];
    _statuses = const [];
    _priorities = const [];
    _status = null;
    _priority = null;
    _districtId = null;
    _loaded = false;
    _error = null;
    notifyListeners();
  }
}
