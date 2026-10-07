import 'package:dewecs_mobile/strings.dart';
import 'package:dewecs_mobile/validators.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  group('NIC', () {
    test('accepts old and new formats, trimmed and in any case', () {
      for (final nic in ['901234567V', '901234567x', ' 901234567v ', '199012345678', ' 199012345678']) {
        expect(validateNic(nic), isNull, reason: nic);
      }
    });

    test('rejects everything else', () {
      for (final nic in [null, '', '   ', '12345', '19901234567', '1990123456789', '90123456AV', '901234567Z', '9012345678V']) {
        expect(validateNic(nic), isNotNull, reason: '$nic');
      }
      expect(validateNic(''), S.nicRequired);
      expect(validateNic('123'), S.nicInvalid);
    });
  });

  group('full name', () {
    test('1 to 120 characters after trimming', () {
      expect(validateFullName(' Nimal '), isNull);
      expect(validateFullName('x' * 120), isNull);
      expect(validateFullName('x' * 121), S.nameTooLong);
      expect(validateFullName('   '), S.nameRequired);
      expect(validateFullName(null), S.nameRequired);
    });
  });

  group('phone', () {
    test('accepts the contract pattern', () {
      for (final phone in ['0771234567', '+94771234567', '077 123 4567', '077-123-4567', '12345678']) {
        expect(validatePhone(phone), isNull, reason: phone);
      }
    });

    test('rejects too short, too long and non-numeric', () {
      for (final phone in ['', '   ', '077', 'abcdefghij', '0771234567890123', '+', '--12345678', '+-1234567']) {
        expect(validatePhone(phone), isNotNull, reason: phone);
      }
      expect(validatePhone(''), S.phoneRequired);
      expect(validatePhone('077'), S.phoneInvalid);
    });
  });

  test('district must be chosen', () {
    expect(validateDistrict(null), S.districtRequired);
    expect(validateDistrict(1), isNull);
  });

  group('server address', () {
    test('needs http or https and a host', () {
      expect(validateBaseUrl('http://10.0.2.2:8080'), isNull);
      expect(validateBaseUrl(' https://example.org '), isNull);
      for (final url in [null, '', 'localhost:8080', 'ftp://host', 'http://', 'just words']) {
        expect(validateBaseUrl(url), S.baseUrlInvalid, reason: '$url');
      }
    });
  });
}
