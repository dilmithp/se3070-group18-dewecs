import 'package:flutter/foundation.dart';

import 'api/api_exception.dart';

bool _isLoopback(String host) => host == 'localhost' || host == '127.0.0.1' || host == '::1' || host == '[::1]';

String _hostOf(String address) => Uri.tryParse(address.trim())?.host.toLowerCase() ?? '';

/// What to try next after a failed connection test. Plain sentences for the likely cause, never a stack trace.
/// [isWeb] and [platform] are parameters so tests can cover every branch.
String connectionAdvice(ApiException error, String address, {bool? isWeb, TargetPlatform? platform}) {
  final web = isWeb ?? kIsWeb;
  final target = platform ?? defaultTargetPlatform;
  final host = _hostOf(address);
  final status = error.status;

  if (status != null) {
    if (status == 404) {
      return 'A server answered, but it is not DEWECS (no /api/v1/reference-data). Check the address and the port.';
    }
    if (status == 403) {
      return 'The server refused the request (403). If you are testing from a browser, the backend must allow '
          'it (run the backend with the local profile for Flutter web).';
    }
    if (status >= 500) {
      return 'The server is running but failed (HTTP $status). Check the server log; with the prod profile also '
          'check the database settings.';
    }
    return 'The server answered with HTTP $status. Check the address.';
  }

  if (error.kind == ApiErrorKind.timeout) {
    return 'The server did not answer in time. Check that it is running, that the port is right, and that the '
        'firewall allows it. A phone must be on the same Wi-Fi as the computer.';
  }

  if (_isLoopback(host)) {
    if (!web && target == TargetPlatform.android) {
      return 'localhost on a phone is the phone itself, not your computer. Use the computer\'s IP address, '
          'for example http://192.168.1.20:8080 (Android emulator: http://10.0.2.2:8080).';
    }
    if (web) {
      return 'The browser could not reach http://localhost. Check that the backend is running on that port and '
          'that it was started with the local profile (it allows browser requests).';
    }
  }

  if (host == '10.0.2.2' && !web && target == TargetPlatform.android) {
    return '10.0.2.2 only works inside the Android emulator. On a real phone use the computer\'s IP address.';
  }

  if (web) {
    return 'The browser blocked or could not complete the request. Check that the backend is running and, if it '
        'is on another address, that it allows browser requests (CORS).';
  }
  return 'Check the address and port, that the server is running, and that the phone and the computer are on '
      'the same Wi-Fi.';
}

/// A warning for an unencrypted address that is not on this machine or the emulator, or null when it is fine.
String? insecureAddressWarning(String address) {
  final uri = Uri.tryParse(address.trim());
  if (uri == null || uri.scheme != 'http' || uri.host.isEmpty) {
    return null;
  }
  final host = uri.host.toLowerCase();
  if (_isLoopback(host) || host == '10.0.2.2') {
    return null;
  }
  return 'http:// is not encrypted. Use https:// for a real server; plain http is only for testing on your own '
      'network.';
}
