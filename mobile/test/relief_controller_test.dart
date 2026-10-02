import 'dart:convert';

import 'package:flutter_test/flutter_test.dart';
import 'package:dewecs_mobile/api/demo_dewecs_api.dart';
import 'package:dewecs_mobile/models/relief_supply.dart';
import 'package:dewecs_mobile/state/relief_controller.dart';
import 'package:dewecs_mobile/storage/key_value_store.dart';

void main() {
  group('ReliefController', () {
    late MemoryKeyValueStore store;
    late DemoDewecsApi api;

    setUp(() async {
      store = MemoryKeyValueStore();
      api = DemoDewecsApi(latency: Duration.zero);
    });

    test('loads from cache initially', () async {
      final supply = const ReliefSupply(
        id: 1,
        name: 'Test',
        type: 'FOOD',
        unit: 'pack',
        quantity: 10,
        districtId: 1,
        districtName: 'D1',
        organizationName: 'Org',
        lowStock: false,
        outOfStock: false,
      );
      await store.setString('cache.reliefSupplies', jsonEncode([supply.toJson()]));
      
      final controller = ReliefController(store, () => api);
      expect(controller.supplies, isNotNull);
      expect(controller.supplies!.length, 1);
      expect(controller.supplies![0].name, 'Test');
    });

    test('fetches from API and caches on refresh', () async {
      final controller = ReliefController(store, () => api);
      
      expect(controller.supplies, isNull);
      
      final ok = await controller.refresh();
      expect(ok, isTrue);
      // DemoDewecsApi has some built-in data.
      expect(controller.supplies, isNotEmpty);
      
      final cached = store.getString('cache.reliefSupplies');
      expect(cached, isNotNull);
    });

    test('handles API errors without losing cache', () async {
      final supply = const ReliefSupply(
        id: 1,
        name: 'Test',
        type: 'FOOD',
        unit: 'pack',
        quantity: 10,
        districtId: 1,
        districtName: 'D1',
        organizationName: 'Org',
        lowStock: false,
        outOfStock: false,
      );
      await store.setString('cache.reliefSupplies', jsonEncode([supply.toJson()]));
      
      api.failNetwork = true;
      
      final controller = ReliefController(store, () => api);
      expect(controller.supplies!.length, 1);
      
      final ok = await controller.refresh();
      expect(ok, isFalse);
      expect(controller.lastError, isNotNull);
      expect(controller.supplies!.length, 1); // Kept cache
    });

    test('clears local data when requested', () async {
      final supply = const ReliefSupply(
        id: 1,
        name: 'Test',
        type: 'FOOD',
        unit: 'pack',
        quantity: 10,
        districtId: 1,
        districtName: 'D1',
        organizationName: 'Org',
        lowStock: false,
        outOfStock: false,
      );
      await store.setString('cache.reliefSupplies', jsonEncode([supply.toJson()]));
      
      final controller = ReliefController(store, () => api);
      expect(controller.supplies, isNotNull);
      
      await controller.clearLocalData();
      expect(controller.supplies, isNull);
      expect(store.getString('cache.reliefSupplies'), isNull);
    });
  });
}
