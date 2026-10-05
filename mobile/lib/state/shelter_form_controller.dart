import 'package:flutter/foundation.dart';

import '../api/api_exception.dart';
import '../api/operations_api.dart';
import '../models/action_result.dart';
import '../models/shelter.dart';

/// What the shelter form needs: the districts and organizations to choose from, and the save call. A refusal keeps
/// the server's reason and its field errors in [saveError] for the form to show next to the fields.
class ShelterFormController extends ChangeNotifier {
  ShelterFormController(this._api);

  final OperationsApi Function() _api;

  ShelterFormData? _data;
  bool _loading = false;
  bool _saving = false;
  bool _disposed = false;
  ApiException? _loadError;
  ApiException? _saveError;

  ShelterFormData? get data => _data;

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
      _data = await _api().getShelterFormData();
    } on ApiException catch (e) {
      _loadError = e;
    } finally {
      _loading = false;
      _notify();
    }
  }

  /// The created shelter, or null when the server refused it (see [saveError]).
  Future<ActionResult?> create({
    required int districtId,
    required int organizationId,
    required String name,
    required int capacity,
  }) => _save(
    () => _api().createShelter(
      districtId: districtId,
      organizationId: organizationId,
      name: name.trim(),
      capacity: capacity,
    ),
  );

  /// The server answers an update with the whole form again, so the fixed district and organization go along.
  Future<ActionResult?> update(
    int id, {
    required int districtId,
    required int organizationId,
    required String name,
    required int capacity,
  }) => _save(
    () => _api().updateShelter(
      id,
      districtId: districtId,
      organizationId: organizationId,
      name: name.trim(),
      capacity: capacity,
    ),
  );

  /// Forget the last refusal, for example when the user starts typing again.
  void clearSaveError() {
    if (_saveError != null) {
      _saveError = null;
      _notify();
    }
  }

  Future<ActionResult?> _save(Future<ActionResult> Function() call) async {
    if (_saving) {
      return null;
    }
    _saving = true;
    _saveError = null;
    _notify();
    try {
      return await call();
    } on ApiException catch (e) {
      _saveError = e;
      return null;
    } finally {
      _saving = false;
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
