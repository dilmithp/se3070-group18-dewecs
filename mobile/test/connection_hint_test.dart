import 'package:dewecs_mobile/api/api_exception.dart';
import 'package:dewecs_mobile/connection_hint.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter_test/flutter_test.dart';

const network = ApiException(kind: ApiErrorKind.network, detail: 'x');
const timeout = ApiException(kind: ApiErrorKind.timeout, detail: 'x');

void main() {
  group('connectionAdvice', () {
    test('localhost on a phone explains that it is the phone itself', () {
      final text = connectionAdvice(network, 'http://localhost:8080', isWeb: false, platform: TargetPlatform.android);
      expect(text, contains('phone itself'));
      expect(text, contains('10.0.2.2'));
    });

    test('10.0.2.2 on a real phone says it only works in the emulator', () {
      final text = connectionAdvice(network, 'http://10.0.2.2:8080', isWeb: false, platform: TargetPlatform.android);
      expect(text, contains('emulator'));
    });

    test('a browser failure mentions CORS and the local profile', () {
      final text = connectionAdvice(network, 'http://localhost:8080', isWeb: true, platform: TargetPlatform.windows);
      expect(text, contains('local profile'));
      expect(connectionAdvice(network, 'http://192.168.1.2:8080', isWeb: true), contains('CORS'));
    });

    test('a timeout points at the server, port, firewall and Wi-Fi', () {
      final text = connectionAdvice(timeout, 'http://192.168.1.2:8080', isWeb: false, platform: TargetPlatform.android);
      expect(text, allOf(contains('firewall'), contains('Wi-Fi')));
    });

    test('HTTP statuses are explained', () {
      ApiException status(int code) => ApiException(kind: ApiErrorKind.client, status: code, detail: 'x');
      expect(connectionAdvice(status(404), 'http://a:1'), contains('not DEWECS'));
      expect(connectionAdvice(status(403), 'http://a:1'), contains('403'));
      expect(connectionAdvice(status(500), 'http://a:1'), contains('HTTP 500'));
      expect(connectionAdvice(status(418), 'http://a:1'), contains('HTTP 418'));
    });

    test('any other failure gets the general checklist', () {
      final text = connectionAdvice(network, 'http://192.168.1.2:8080', isWeb: false, platform: TargetPlatform.android);
      expect(text, contains('same Wi-Fi'));
    });
  });

  group('insecureAddressWarning', () {
    test('plain http to another machine is flagged', () {
      expect(insecureAddressWarning('http://192.168.1.20:8080'), contains('not encrypted'));
    });

    test('https, localhost and the emulator address are fine', () {
      expect(insecureAddressWarning('https://dewecs.example.com'), isNull);
      expect(insecureAddressWarning('http://localhost:8080'), isNull);
      expect(insecureAddressWarning('http://10.0.2.2:8080'), isNull);
      expect(insecureAddressWarning('not a url'), isNull);
    });
  });
}
