import 'package:dewecs_mobile/models/sri_lanka_time.dart';
import 'package:flutter_test/flutter_test.dart';

void main() {
  test('adds 5 h 30 min to UTC and formats with milliseconds and no offset', () {
    final utc = DateTime.utc(2026, 10, 7, 8, 33, 11, 123);

    expect(sriLankaNowString(utc), '2026-10-07T14:03:11.123');
  });

  test('crosses midnight into the next day', () {
    final utc = DateTime.utc(2026, 12, 31, 20, 0, 0, 0);

    expect(sriLankaNowString(utc), '2027-01-01T01:30:00.000');
  });

  test('pads milliseconds and ignores the device time zone', () {
    final local = DateTime.parse('2026-10-07T08:33:05.007Z').toLocal();

    expect(sriLankaNowString(local), '2026-10-07T14:03:05.007');
  });

  test('output always matches the contract pattern', () {
    final pattern = RegExp(r'^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}\.\d{3}$');

    expect(sriLankaNowString(), matches(pattern));
    expect(formatContractTime(DateTime(2026, 1, 2, 3, 4, 5)), matches(pattern));
  });

  test('parse and format round-trip', () {
    expect(formatContractTime(parseContractTime('2026-10-07T14:03:11.123')), '2026-10-07T14:03:11.123');
    expect(formatContractTime(parseContractTime('2026-10-07T14:03:11.12')), '2026-10-07T14:03:11.120');
  });
}
