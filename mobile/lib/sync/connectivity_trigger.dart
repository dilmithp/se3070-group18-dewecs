import 'dart:async';

import 'package:connectivity_plus/connectivity_plus.dart';
import 'package:flutter/widgets.dart';

import 'sync_service.dart';

/// Starts a sync when the connection comes back and when the app returns to the foreground.
/// There is no background service: nothing is sent while the app is closed.
class ConnectivityTrigger with WidgetsBindingObserver {
  ConnectivityTrigger(this._sync, {this._changes});

  final SyncService _sync;
  final Stream<List<ConnectivityResult>>? _changes;
  StreamSubscription<List<ConnectivityResult>>? _subscription;

  void start() {
    _subscription = (_changes ?? Connectivity().onConnectivityChanged).listen((results) {
      if (results.any((r) => r != ConnectivityResult.none)) {
        _sync.syncNow(force: true);
      }
    });
    WidgetsBinding.instance.addObserver(this);
  }

  @override
  void didChangeAppLifecycleState(AppLifecycleState state) {
    if (state == AppLifecycleState.resumed) {
      _sync.syncNow();
    }
  }

  void stop() {
    _subscription?.cancel();
    WidgetsBinding.instance.removeObserver(this);
  }
}
