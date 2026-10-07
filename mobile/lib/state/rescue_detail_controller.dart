import 'package:flutter/foundation.dart';

import '../api/api_exception.dart';
import '../api/operations_api.dart';
import '../models/action_result.dart';
import '../models/rescue_request.dart';

/// One rescue request and the teams that can be assigned to it, and the actions on it (assign, complete, cancel).
/// After every action the detail is fetched again, also after a refusal, so the screen shows what the server holds.
/// A refused action (not pending, team not available ...) keeps its reason in [actionError].
class RescueDetailController extends ChangeNotifier {
  RescueDetailController(this._api, this.requestId);

  final OperationsApi Function() _api;
  final int requestId;

  RescueRequestDetail? _detail;
  bool _loading = false;
  bool _busy = false;
  bool _disposed = false;
  ApiException? _loadError;
  ApiException? _actionError;

  RescueRequestDetail? get detail => _detail;

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
      _detail = await _api().getRescueRequest(requestId);
    } on ApiException catch (e) {
      _loadError = e;
    } finally {
      _loading = false;
      _notify();
    }
  }

  /// Each action returns the server's message, or null when it was refused (see [actionError]).
  Future<String?> assign(int teamId) => _act(() => _api().assignTeam(requestId, teamId));

  Future<String?> complete() => _act(() => _api().completeRescueRequest(requestId));

  Future<String?> cancel() => _act(() => _api().cancelRescueRequest(requestId));

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
      await _reload();
      return result.message;
    } on ApiException catch (e) {
      _actionError = e;
      if (!e.isConnectionDown) {
        await _reload();
      }
      return null;
    } finally {
      _busy = false;
      _notify();
    }
  }

  Future<void> _reload() async {
    try {
      _detail = await _api().getRescueRequest(requestId);
      _loadError = null;
    } on ApiException catch (e) {
      _loadError = e;
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
