/// Sri Lanka time is UTC+05:30 all year (no daylight saving). The API uses it as a local date-time without offset.
const sriLankaOffset = Duration(hours: 5, minutes: 30);

/// The current Sri Lanka wall-clock time as a plain DateTime (its fields matter, not its zone).
DateTime sriLankaNow([DateTime? now]) => (now ?? DateTime.now()).toUtc().add(sriLankaOffset);

/// Exactly the contract format, for example 2026-10-07T14:03:11.123 (milliseconds, no offset suffix).
String formatContractTime(DateTime t) {
  String two(int n) => n.toString().padLeft(2, '0');
  final ms = t.millisecond.toString().padLeft(3, '0');
  return '${t.year.toString().padLeft(4, '0')}-${two(t.month)}-${two(t.day)}'
      'T${two(t.hour)}:${two(t.minute)}:${two(t.second)}.$ms';
}

/// capturedAt for a report created now.
String sriLankaNowString([DateTime? now]) => formatContractTime(sriLankaNow(now));

/// Parses a contract timestamp (any number of fraction digits). The fields are kept as written and the result is
/// flagged UTC, the same as sriLankaNow, so values compare consistently whatever the phone time zone is.
DateTime parseContractTime(String text) {
  final t = DateTime.parse(text);
  return DateTime.utc(t.year, t.month, t.day, t.hour, t.minute, t.second, t.millisecond);
}
