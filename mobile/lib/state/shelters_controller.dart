import 'package:flutter/foundation.dart';

import '../api/api_exception.dart';
import '../api/operations_api.dart';
import '../models/district.dart';
import '../models/shelter.dart';
import 'clearable.dart';

/// The shelter list with its two filters. A failed refresh keeps the last list on screen and remembers why.
class SheltersController extends ChangeNotifier implements Clearable {
  SheltersController(this._api);

  final OperationsApi Function() _api;

  List<Shelter> _shelters = [];
  List<District> _districts = [];
  List<String> _statuses = const [];
  String? _status;
  int? _districtId;
  bool _loaded = false;
  bool _loading = false;
  bool _again = false;
  ApiException? _error;

  List<Shelter> get shelters => _shelters;

  /// The districts and statuses the filter drop-downs offer (empty until the first answer).
  List<District> get districts => _districts;

  List<String> get statuses => _statuses;

  String? get statusFilter => _status;

  int? get districtFilter => _districtId;

  bool get hasFilter => _status != null || _districtId != null;

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
          final list = await _api().listShelters(status: _status, districtId: _districtId);
          _shelters = list.shelters;
          _districts = list.districts;
          _statuses = list.statuses;
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

  Future<void> setFilters({String? status, int? districtId}) {
    _status = status;
    _districtId = districtId;
    return refresh();
  }

  @override
  Future<void> clearLocalData() async {
    _shelters = [];
    _districts = [];
    _statuses = const [];
    _status = null;
    _districtId = null;
    _loaded = false;
    _error = null;
    notifyListeners();
  }
}
