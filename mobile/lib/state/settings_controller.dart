import 'package:flutter/foundation.dart';
import 'package:http/http.dart' as http;

import '../api/dewecs_api.dart';
import '../api/demo_dewecs_api.dart';
import '../api/fake_operations_api.dart';
import '../api/http_dewecs_api.dart';
import '../api/http_operations_api.dart';
import '../api/operations_api.dart';
import '../config/app_config.dart';
import '../storage/key_value_store.dart';
import 'clearable.dart';

/// Server address and Demo mode, and the API object the rest of the app uses.
class SettingsController extends ChangeNotifier {
  SettingsController(this._store, {this.apiOverride, this.operationsOverride, this.httpClient}) {
    _baseUrl = _store.getString(_kBaseUrl) ?? defaultBaseUrl();
    _demoMode = _store.getString(_kDemo) == 'true';
    _officerMode = _store.getString(_kOfficerMode) == 'true';
  }

  static const _kBaseUrl = 'settings.baseUrl';
  static const _kDemo = 'settings.demoMode';
  static const _kOfficerMode = 'settings.officerMode';

  final KeyValueStore _store;
  /// Tests plug the demo server in here, whatever the settings say.
  final DewecsApi? apiOverride;

  /// Tests plug the fake officer pages in here, whatever the settings say.
  final OperationsApi? operationsOverride;
  final http.Client? httpClient;
  final List<Clearable> _clearables = [];

  late String _baseUrl;
  late bool _demoMode;
  late bool _officerMode;
  DemoDewecsApi? _demo;
  HttpDewecsApi? _http;
  FakeOperationsApi? _fakeOps;
  HttpOperationsApi? _httpOps;

  String get baseUrl => _baseUrl;

  bool get demoMode => _demoMode;

  bool get officerMode => _officerMode;

  /// The demo server (only meaningful in Demo mode); created on first use.
  DemoDewecsApi get demo => _demo ??= DemoDewecsApi();

  /// The API for the current settings. Callers fetch it each time, so a changed address takes effect at once.
  DewecsApi get api {
    final override = apiOverride;
    if (override != null) {
      return override;
    }
    if (_demoMode) {
      return demo;
    }
    final current = _http;
    if (current != null && current.baseUrl == _baseUrl.replaceAll(RegExp(r'/+$'), '')) {
      return current;
    }
    return _http = HttpDewecsApi(baseUrl: _baseUrl, client: httpClient);
  }

  /// The fake officer pages (only meaningful in Demo mode); created on first use.
  FakeOperationsApi get fakeOperations => _fakeOps ??= FakeOperationsApi();

  /// The shelter and rescue pages of the officer dashboard for the current settings, fetched each time like [api].
  OperationsApi get operations {
    final override = operationsOverride;
    if (override != null) {
      return override;
    }
    if (_demoMode) {
      return fakeOperations;
    }
    final current = _httpOps;
    if (current != null && current.baseUrl == _baseUrl.trim().replaceAll(RegExp(r'/+$'), '')) {
      return current;
    }
    return _httpOps = HttpOperationsApi(baseUrl: _baseUrl, client: httpClient);
  }

  /// An API for an address that is typed but not saved yet (the Test connection button).
  DewecsApi apiFor(String address) {
    final override = apiOverride;
    if (override != null) {
      return override;
    }
    return _demoMode ? demo : HttpDewecsApi(baseUrl: address, client: httpClient);
  }

  /// Address of a server photo, or null in Demo mode (the demo server has no photo files to load).
  String? photoUrlFor(String photoPath) =>
      _demoMode ? null : '${_baseUrl.replaceAll(RegExp(r'/+$'), '')}$photoPath';

  void registerClearable(Clearable clearable) => _clearables.add(clearable);

  Future<void> setBaseUrl(String value) async {
    _baseUrl = value.trim();
    await _store.setString(_kBaseUrl, _baseUrl);
    notifyListeners();
  }

  /// Demo ids must never reach the real backend, so everything stored on the phone is wiped first.
  Future<void> setDemoMode(bool value) async {
    if (value == _demoMode) {
      return;
    }
    for (final clearable in _clearables) {
      await clearable.clearLocalData();
    }
    _demo = null;
    _fakeOps = null;
    _demoMode = value;
    await _store.setString(_kDemo, value.toString());
    notifyListeners();
  }

  /// Wipes the demo server (Demo mode only), as if the real server database had been reset.
  Future<void> setOfficerMode(bool value) async {
    if (value == _officerMode) {
      return;
    }
    _officerMode = value;
    await _store.setString(_kOfficerMode, value.toString());
    notifyListeners();
  }

  void resetDemoServer() {
    demo.resetServer();
    _fakeOps?.resetServer();
    notifyListeners();
  }

  void setDemoNetworkFailure(bool value) {
    demo.failNetwork = value;
    fakeOperations.failNetwork = value;
    notifyListeners();
  }

  void setDemoServerError(bool value) {
    demo.failStatus = value ? 500 : null;
    fakeOperations.failStatus = value ? 500 : null;
    notifyListeners();
  }
}
