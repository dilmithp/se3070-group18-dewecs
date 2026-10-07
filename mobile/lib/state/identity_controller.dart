import 'dart:async';
import 'dart:convert';

import 'package:flutter/foundation.dart';

import '../api/dewecs_api.dart';
import '../models/citizen.dart';
import '../storage/key_value_store.dart';
import 'clearable.dart';

/// Who the app thinks the user is. Only the id, name and district are stored, never the NIC.
class IdentityController extends ChangeNotifier implements Clearable {
  IdentityController(this._store, this._api) {
    final raw = _store.getString(_key);
    if (raw != null) {
      try {
        _citizen = Citizen.fromJson(jsonDecode(raw) as Map<String, dynamic>);
      } on FormatException {
        _citizen = null;
      } on TypeError {
        _citizen = null;
      }
    }
  }

  static const _key = 'identity.citizen';

  final KeyValueStore _store;
  final DewecsApi Function() _api;
  Citizen? _citizen;

  /// Called after every successful identification (the app uses it to re-point and send the waiting reports).
  Future<void> Function(Citizen citizen)? onIdentified;

  Citizen? get citizen => _citizen;

  bool get isIdentified => _citizen != null;

  /// Calls the server (201 or 200) and remembers the returned citizen. Throws ApiException.
  Future<Citizen> identify({
    required String nic,
    required String fullName,
    required String phone,
    required int districtId,
  }) async {
    final result = await _api().identify(nic: nic, fullName: fullName, phone: phone, districtId: districtId);
    _citizen = result;
    await _store.setString(_key, jsonEncode(result.toJson()));
    notifyListeners();
    final callback = onIdentified;
    if (callback != null) {
      unawaited(callback(result));
    }
    return result;
  }

  /// Forgets the citizen (used after a server reset and when Demo mode is switched).
  Future<void> clear() async {
    _citizen = null;
    await _store.remove(_key);
    notifyListeners();
  }

  @override
  Future<void> clearLocalData() => clear();
}
