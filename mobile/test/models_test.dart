import 'dart:convert';
import 'dart:io';

import 'package:dewecs_mobile/models/api_problem.dart';
import 'package:dewecs_mobile/models/citizen.dart';
import 'package:dewecs_mobile/models/ground_report.dart';
import 'package:dewecs_mobile/models/reference_data.dart';
import 'package:dewecs_mobile/models/report_page.dart';
import 'package:flutter_test/flutter_test.dart';

Map<String, dynamic> fixture(String name) =>
    jsonDecode(File('test/fixtures/$name').readAsStringSync()) as Map<String, dynamic>;

void main() {
  test('reference-data sample parses', () {
    final data = ReferenceData.fromJson(fixture('reference-data.json'));

    expect(data.districts.single.name, 'Colombo');
    expect(data.categories, ['FLOOD', 'LANDSLIDE', 'CYCLONE', 'DROUGHT']);
    expect(ReferenceData.fromJson(data.toJson()).districts.single.id, 1);
  });

  test('citizen sample parses', () {
    final citizen = Citizen.fromJson(fixture('citizen.json'));

    expect(citizen.id, 42);
    expect(citizen.fullName, 'Nimal Perera');
    expect(citizen.districtName, 'Colombo');
    expect(citizen.created, isTrue);
  });

  test('report sample parses and round-trips with the contract time format', () {
    final report = GroundReport.fromJson(fixture('report.json'));

    expect(report.id, 101);
    expect(report.gpsLat, 6.9271234);
    expect(report.photoUrl, isNull);
    expect(report.actionNote, isNull);
    expect(report.status, 'PENDING_REVIEW');
    expect(report.submittedAt.hour, 14);
    expect(report.toJson()['submittedAt'], '2026-10-07T14:03:11.123');
    expect(GroundReport.fromJson(report.toJson()).id, 101);
  });

  test('report list sample parses with paging fields', () {
    final page = ReportPage.fromJson(fixture('report-list.json'));

    expect(page.items, hasLength(1));
    expect(page.page, 0);
    expect(page.totalItems, 57);
    expect(page.totalPages, 3);
    expect(page.hasMore, isTrue);
  });

  test('unknown status and legacy photo text survive parsing', () {
    final json = fixture('report.json')
      ..['status'] = 'ESCALATED'
      ..['photoUrl'] = 'legacy-file.jpg';

    final report = GroundReport.fromJson(json);

    expect(report.status, 'ESCALATED');
    expect(report.hasDisplayablePhoto, isFalse);
    expect(report.toJson()['status'], 'ESCALATED');
  });

  test('an API photo path is displayable', () {
    final json = fixture('report.json')..['photoUrl'] = '/api/v1/photos/abc.png';

    expect(GroundReport.fromJson(json).hasDisplayablePhoto, isTrue);
  });

  test('problem samples parse, with and without field errors', () {
    final plain = ApiProblem.fromJson(fixture('problem.json'));
    final withFields = ApiProblem.fromJson(fixture('problem-with-field-errors.json'));

    expect(plain.status, 404);
    expect(plain.detail, 'Ground report not found: 999');
    expect(plain.fieldErrors, isEmpty);
    expect(withFields.status, 400);
    expect(withFields.fieldErrors['description'], 'Describe what you see');
  });

  test('problem parsing never throws on odd bodies', () {
    final problem = ApiProblem.fromJson({'status': 'x', 'fieldErrors': 'nope', 'detail': 5});

    expect(problem.status, isNull);
    expect(problem.detail, '5');
    expect(problem.fieldErrors, isEmpty);
  });

  test('missing required fields raise a TypeError the API layer turns into a readable error', () {
    expect(() => GroundReport.fromJson({'id': 1}), throwsA(isA<TypeError>()));
  });
}
