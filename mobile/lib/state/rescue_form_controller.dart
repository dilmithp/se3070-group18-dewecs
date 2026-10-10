import 'package:flutter/foundation.dart';

import '../api/api_exception.dart';
import '../api/operations_api.dart';
import '../models/action_result.dart';
import '../models/rescue_request.dart';

/// What the rescue request form needs: the districts and priorities to choose from, and the submit call. A refusal
/// keeps the server's reason and its field errors in [saveError] for the form to show next to the fields.
class RescueFormController extends ChangeNotifier {
  RescueFormController(this._api);

  final OperationsApi Function() _api;

  RescueRequestFormData? _data;
  bool _loading = false;
  bool _saving = false;
  bool _disposed = false;
  ApiException? _loadError;
  ApiException? _saveError;

  RescueRequestFormData? get data => _data;

  bool get loading => _loading;

  bool get saving => _saving;

  ApiException? get loadError => _loadError;

  ApiException? get saveError => _saveError;

  Future<void> load() async {
    if (_loading) {
      return;
    }
    _loading = true;
    _loadError = null;
    _notify();
    try {
      _data = await _api().getRescueFormData();
    } on ApiException catch (e) {
      _loadError = e;
    } finally {
      _loading = false;
      _notify();
    }
  }

  /// The created request, or null when the server refused it (see [saveError]).
  Future<ActionResult?> submit({
    required int districtId,
    required String requesterName,
    required String requesterPhone,
    double? gpsLat,
    double? gpsLng,
    required String description,
    required String priority,
  }) async {
    if (_saving) {
      return null;
    }
    _saving = true;
    _saveError = null;
    _notify();
    try {
      return await _api().submitRescueRequest(
        districtId: districtId,
        requesterName: requesterName.trim(),
        requesterPhone: requesterPhone.trim(),
        gpsLat: gpsLat,
        gpsLng: gpsLng,
        description: description.trim(),
        priority: priority,
      );
    } on ApiException catch (e) {
      _saveError = e;
      return null;
    } finally {
      _saving = false;
      _notify();
    }
  }

  /// Forget the last refusal, for example when the user starts typing again.
  void clearSaveError() {
    if (_saveError != null) {
      _saveError = null;
      _notify();
    }
  }

  void _notify() {
    if (!_disposed) {
      notifyListeners();
    }
  }

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }
}
