import 'package:flutter/foundation.dart';

import '../api/api_exception.dart';
import '../api/operations_api.dart';
import '../models/action_result.dart';
import '../models/shelter.dart';

/// One shelter and the people checked in now, and the actions on it (check in or out, close, reopen). After every
/// successful action the detail is fetched again, so the screen always shows what the server holds. A refused action
/// (closed shelter, no free place ...) keeps its reason in [actionError] and the detail is fetched again as well.
class ShelterDetailController extends ChangeNotifier {
  ShelterDetailController(this._api, this.shelterId);

  final OperationsApi Function() _api;
  final int shelterId;

  ShelterDetail? _detail;
  bool _loading = false;
  bool _busy = false;
  bool _disposed = false;
  ApiException? _loadError;
  ApiException? _actionError;

  ShelterDetail? get detail => _detail;

  /// A load is running.
  bool get loading => _loading;

  /// An action is running; the buttons stay disabled meanwhile so nothing is sent twice.
  bool get busy => _busy;

  /// Why the last load failed (the previous detail, if any, is kept).
  ApiException? get loadError => _loadError;

  /// Why the last action was refused or failed.
  ApiException? get actionError => _actionError;

  Future<void> load() async {
    if (_loading) {
      return;
    }
    _loading = true;
    _loadError = null;
    _notify();
    try {
      _detail = await _api().getShelter(shelterId);
    } on ApiException catch (e) {
      _loadError = e;
    } finally {
      _loading = false;
      _notify();
    }
  }

  /// Returns the server's message when the occupant was checked in, or null when it was refused (see [actionError]).
  Future<String?> checkIn({required String fullName, required String nic}) =>
      _act(() => _api().checkIn(shelterId, fullName: fullName.trim(), nic: nic.trim()));

  Future<String?> checkOut(int occupantId) => _act(() => _api().checkOut(shelterId, occupantId));

  Future<String?> close() => _act(() => _api().closeShelter(shelterId));

  Future<String?> reopen() => _act(() => _api().reopenShelter(shelterId));

  void dismissActionError() {
    if (_actionError != null) {
      _actionError = null;
      _notify();
    }
  }

  Future<String?> _act(Future<ActionResult> Function() action) async {
    if (_busy) {
      return null;
    }
    _busy = true;
    _actionError = null;
    _notify();
    try {
      final result = await action();
      try {
        _detail = await _api().getShelter(shelterId);
        _loadError = null;
      } on ApiException catch (e) {
        _loadError = e;
      }
      return result.message;
    } on ApiException catch (e) {
      _actionError = e;
      if (!e.isConnectionDown) {
        // A refusal usually means the screen was out of date (someone else closed the shelter): show the truth.
        try {
          _detail = await _api().getShelter(shelterId);
          _loadError = null;
        } on ApiException {
          // The reason of the first failure stays on screen.
        }
      }
      return null;
    } finally {
      _busy = false;
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
