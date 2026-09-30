import 'dart:convert';
import 'dart:io';

import 'package:dewecs_mobile/models/relief_distribution.dart';
import 'package:dewecs_mobile/models/relief_rules.dart';
import 'package:dewecs_mobile/models/relief_supply.dart';
import 'package:dewecs_mobile/models/shelter_summary.dart';
import 'package:flutter_test/flutter_test.dart';

Map<String, dynamic> fixture(String name) =>
    jsonDecode(File('test/fixtures/$name').readAsStringSync())
        as Map<String, dynamic>;

void main() {
  test('a supply is read from the server JSON, including the server-decided stock flags', () {
    final supplies = (fixture('relief-supplies.json')['supplies'] as List)
        .map((s) => ReliefSupply.fromJson(s as Map<String, dynamic>))
        .toList();

    expect(supplies.first.name, 'Rice');
    expect(supplies.first.type, ResourceTypes.food);
    expect(supplies.first.quantity, 120);
    expect(supplies.first.organizationName, 'Lanka Relief Foundation');
    expect(supplies.first.lowStock, isFalse);
    expect(supplies.last.type, ResourceTypes.medicalSupplies);
    expect(supplies.last.lowStock, isTrue);
  });

  test('a supply survives a toJson/fromJson round trip and withStock changes only stock and flags', () {
    final supply = ReliefSupply.fromJson(
      (fixture('relief-supplies.json')['supplies'] as List).first
          as Map<String, dynamic>,
    );

    final copy = ReliefSupply.fromJson(supply.toJson());
    final restocked = supply.withStock(0, low: false, out: true);

    expect(copy.toJson(), supply.toJson());
    expect(restocked.quantity, 0);
    expect(restocked.outOfStock, isTrue);
    expect(restocked.name, supply.name);
  });

  test('missing stock flags default to false instead of failing', () {
    final json =
        Map<String, dynamic>.of(
            (fixture('relief-supplies.json')['supplies'] as List).first
                as Map<String, dynamic>,
          )
          ..remove('lowStock')
          ..remove('outOfStock');

    final supply = ReliefSupply.fromJson(json);

    expect(supply.lowStock, isFalse);
    expect(supply.outOfStock, isFalse);
  });

  test('a distribution reads its timestamps and treats a missing delivery time as null', () {
    final list = (fixture('relief-distributions.json')['distributions'] as List)
        .map((d) => ReliefDistribution.fromJson(d as Map<String, dynamic>))
        .toList();

    expect(list.first.status, DistributionStatuses.dispatched);
    expect(list.first.dispatchedAt, DateTime.utc(2026, 10, 7, 14, 3, 11, 440));
    expect(list.first.deliveredAt, isNull);
    expect(list.last.deliveredAt, DateTime.utc(2026, 10, 6, 12, 30));
    expect(
      ReliefDistribution.fromJson(list.first.toJson()).toJson(),
      list.first.toJson(),
    );
  });

  test('a shelter keeps what the pick-list needs and tolerates missing optional fields', () {
    final shelters = (fixture('shelters.json')['shelters'] as List)
        .map((s) => ShelterSummary.fromJson(s as Map<String, dynamic>))
        .toList();
    final sparse = ShelterSummary.fromJson({'id': 9, 'name': 'Hall'});

    expect(shelters.first.name, 'Colombo Sports Complex');
    expect(shelters.last.currentOccupancy, 80);
    expect(sparse.capacity, 0);
    expect(sparse.status, '');
  });

  test('the new distribution id is taken from the location, with or without a trailing slash', () {
    final plain = DistributionCreated.fromJson({
      'message': 'Relief distribution dispatched.',
      'location': '/relief-distributions/12',
    });
    final slash = DistributionCreated.fromJson({
      'location': '/relief-distributions/13/',
    });

    expect(plain.id, 12);
    expect(plain.message, 'Relief distribution dispatched.');
    expect(slash.id, 13);
    expect(slash.message, '');
    expect(
      () => DistributionCreated.fromJson({
        'location': '/relief-distributions/new',
      }),
      throwsFormatException,
    );
  });
}
