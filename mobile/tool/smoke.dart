// Pure-Dart smoke test of HttpDewecsApi against a running backend (local profile, never a Neon datasource).
//
//   dart run tool/smoke.dart http://localhost:8092
//
// It walks the real flow once: reference-data, identify (created, then existing), submit, replay, photo upload,
// get, list, and two error cases. Every mismatch with contract v1 is printed and makes the exit code 1.
import 'dart:io';

import 'package:dewecs_mobile/api/api_exception.dart';
import 'package:dewecs_mobile/api/http_dewecs_api.dart';
import 'package:dewecs_mobile/models/report_submission.dart';
import 'package:dewecs_mobile/models/sri_lanka_time.dart';

var failures = 0;

void check(String what, bool ok, [Object? detail]) {
  stdout.writeln('${ok ? 'PASS' : 'FAIL'}  $what${ok || detail == null ? '' : '  ($detail)'}');
  if (!ok) {
    failures++;
  }
}

/// A valid 1x1 PNG.
const tinyPng = <int>[
  0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, //
  0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01, 0x08, 0x02, 0x00, 0x00, 0x00, 0x90, 0x77, 0x53,
  0xDE, 0x00, 0x00, 0x00, 0x0C, 0x49, 0x44, 0x41, 0x54, 0x08, 0xD7, 0x63, 0xF8, 0xCF, 0xC0, 0x00,
  0x00, 0x03, 0x01, 0x01, 0x00, 0x18, 0xDD, 0x8D, 0xB0, 0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E,
  0x44, 0xAE, 0x42, 0x60, 0x82,
];

Future<void> main(List<String> args) async {
  final base = args.isEmpty ? 'http://localhost:8092' : args.first;
  stdout.writeln('Smoke test against $base');
  final api = HttpDewecsApi(baseUrl: base);

  final reference = await api.getReferenceData();
  check('reference-data has districts', reference.districts.isNotEmpty, reference.districts.length);
  check('reference-data categories are the four hazard types',
      reference.categories.toSet().containsAll({'FLOOD', 'LANDSLIDE', 'CYCLONE', 'DROUGHT'}), reference.categories);
  final names = reference.districts.map((d) => d.name.toLowerCase()).toList();
  check('districts are sorted by name, ignoring case', names.toList().toString() == (List.of(names)..sort()).toString(), names);
  final district = reference.districts.first;

  final stamp = DateTime.now().microsecondsSinceEpoch.toString();
  final nic = '19${stamp.substring(stamp.length - 10)}';
  final first = await api.identify(nic: nic, fullName: 'Smoke Tester නිමල්', phone: '0771234567', districtId: district.id);
  check('identify creates a citizen', first.created, first.created);
  check('identify returns the district name', first.districtName == district.name, first.districtName);
  check('identify keeps Sinhala text intact', first.fullName == 'Smoke Tester නිමල්', first.fullName);
  final second = await api.identify(nic: nic.toLowerCase(), fullName: 'Someone Else', phone: '0779999999', districtId: district.id);
  check('identify again finds the same citizen', !second.created && second.id == first.id, second.created);
  check('identify never overwrites the stored name', second.fullName == first.fullName, second.fullName);

  final capturedAt = sriLankaNowString();
  final submission = ReportSubmission(
    citizenId: first.id,
    districtId: district.id,
    category: 'FLOOD',
    description: 'ගඟ ගලා බසිනවා — smoke test',
    gpsLat: 6.9271234,
    gpsLng: 79.8612345,
    capturedAt: capturedAt,
  );
  final created = await api.submitReport(submission);
  check('submit answers 201 (created)', created.created, created.created);
  check('submit starts as PENDING_REVIEW', created.report.status == 'PENDING_REVIEW', created.report.status);
  check('submit keeps Sinhala text and the em dash', created.report.description == submission.description, created.report.description);
  check('submit returns 7 decimal coordinates', created.report.gpsLat == 6.9271234 && created.report.gpsLng == 79.8612345,
      '${created.report.gpsLat}, ${created.report.gpsLng}');
  check('submittedAt equals capturedAt', formatContractTime(created.report.submittedAt) == capturedAt,
      '${formatContractTime(created.report.submittedAt)} vs $capturedAt');
  check('new report has no photo and no action note', created.report.photoUrl == null && created.report.actionNote == null);

  final replay = await api.submitReport(submission);
  check('replay answers 200 (not created)', !replay.created, replay.created);
  check('replay returns the same report', replay.report.id == created.report.id, '${replay.report.id} vs ${created.report.id}');

  final withPhoto = await api.uploadPhoto(created.report.id, tinyPng, filename: 'smoke.png');
  check('photo upload sets a /api/v1/photos/ URL', withPhoto.hasDisplayablePhoto, withPhoto.photoUrl);

  final fetched = await api.getReport(created.report.id);
  check('GET report shows the photo URL', fetched.photoUrl == withPhoto.photoUrl, fetched.photoUrl);
  final photoUrl = api.absolutePhotoUrl(fetched);
  check('absolute photo URL is built', photoUrl != null && photoUrl.startsWith(base), photoUrl);
  if (photoUrl != null) {
    final client = HttpClient();
    final request = await client.getUrl(Uri.parse(photoUrl));
    final response = await request.close();
    final bytes = await response.fold<List<int>>([], (all, chunk) => all..addAll(chunk));
    check('photo downloads with image/png', response.statusCode == 200 && response.headers.contentType?.mimeType == 'image/png',
        '${response.statusCode} ${response.headers.contentType}');
    check('photo bytes are identical', bytes.length == tinyPng.length && _same(bytes, tinyPng), bytes.length);
    check('photo has nosniff and a one day cache', response.headers.value('x-content-type-options') == 'nosniff' &&
        (response.headers.value('cache-control') ?? '').contains('max-age=86400'), response.headers.value('cache-control'));
    client.close();
  }

  final page = await api.listCitizenReports(first.id);
  check('list contains the report, newest first', page.items.isNotEmpty && page.items.first.id == created.report.id,
      page.items.map((r) => r.id).toList());
  check('list paging fields', page.page == 0 && page.size == 20 && page.totalItems == 1 && page.totalPages == 1,
      '${page.page}/${page.size}/${page.totalItems}/${page.totalPages}');

  // Error cases: the contract says what each one looks like.
  await _expectError('unknown citizen on submit is 404, not retryable', () => api.submitReport(ReportSubmission(
        citizenId: 999999999,
        districtId: district.id,
        category: 'FLOOD',
        description: 'x',
        gpsLat: 1,
        gpsLng: 1,
        capturedAt: sriLankaNowString(),
      )), status: 404, retryable: false);
  await _expectError('a capture time in the future is a permanent 400', () => api.submitReport(ReportSubmission(
        citizenId: first.id,
        districtId: district.id,
        category: 'FLOOD',
        description: 'x',
        gpsLat: 1,
        gpsLng: 1,
        capturedAt: formatContractTime(sriLankaNow().add(const Duration(hours: 1))),
      )), status: 400, retryable: false);
  await _expectError('a bad NIC is 400 with a detail', () => api.identify(nic: '12345', fullName: 'A', phone: '0771234567', districtId: district.id),
      status: 400, retryable: false);
  await _expectError('unknown report on photo upload is 404', () => api.uploadPhoto(999999999, tinyPng), status: 404, retryable: false);
  await _expectError('a text file is refused as a photo (400)', () => api.uploadPhoto(created.report.id, 'not an image'.codeUnits),
      status: 400, retryable: false);

  stdout.writeln(failures == 0 ? '\nAll checks passed.' : '\n$failures check(s) FAILED.');
  exit(failures == 0 ? 0 : 1);
}

bool _same(List<int> a, List<int> b) {
  for (var i = 0; i < a.length; i++) {
    if (a[i] != b[i]) {
      return false;
    }
  }
  return true;
}

Future<void> _expectError(String what, Future<Object?> Function() call, {required int status, required bool retryable}) async {
  try {
    await call();
    check(what, false, 'no error was thrown');
  } on ApiException catch (e) {
    check(what, e.status == status && e.retryable == retryable && e.detail.isNotEmpty, '${e.status} ${e.kind.name} ${e.detail}');
  }
}
