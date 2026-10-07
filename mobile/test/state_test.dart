import 'package:dewecs_mobile/api/api_exception.dart';
import 'package:dewecs_mobile/api/fake_dewecs_api.dart';
import 'package:dewecs_mobile/config/app_config.dart';
import 'package:dewecs_mobile/state/clearable.dart';
import 'package:dewecs_mobile/state/identity_controller.dart';
import 'package:dewecs_mobile/state/reference_data_controller.dart';
import 'package:dewecs_mobile/state/settings_controller.dart';
import 'package:dewecs_mobile/storage/key_value_store.dart';
import 'package:flutter_test/flutter_test.dart';

class _Probe implements Clearable {
  int cleared = 0;

  @override
  Future<void> clearLocalData() async => cleared++;
}

void main() {
  late MemoryKeyValueStore store;
  late FakeDewecsApi fake;

  setUp(() {
    store = MemoryKeyValueStore();
    fake = FakeDewecsApi(latency: Duration.zero);
  });

  group('SettingsController', () {
    test('starts with the platform default address and Demo mode off', () {
      final settings = SettingsController(store);

      expect(settings.baseUrl, defaultBaseUrl());
      expect(settings.demoMode, isFalse);
    });

    test('the address is persisted', () async {
      await SettingsController(store).setBaseUrl(' http://192.168.1.20:8080 ');

      expect(SettingsController(store).baseUrl, 'http://192.168.1.20:8080');
    });

    test('switching Demo mode clears everything registered and persists the choice', () async {
      final settings = SettingsController(store);
      final probe = _Probe();
      settings.registerClearable(probe);

      await settings.setDemoMode(true);

      expect(probe.cleared, 1);
      expect(settings.demoMode, isTrue);
      expect(SettingsController(store).demoMode, isTrue);
      expect(settings.api, same(settings.fake));

      await settings.setDemoMode(true);
      expect(probe.cleared, 1, reason: 'no change means nothing is cleared');

      await settings.setDemoMode(false);
      expect(probe.cleared, 2);
      expect(settings.api, isNot(same(settings.fake)));
    });

    test('each switch into Demo mode starts with a fresh fake server', () async {
      final settings = SettingsController(store);
      await settings.setDemoMode(true);
      final first = settings.fake;
      await settings.setDemoMode(false);
      await settings.setDemoMode(true);

      expect(settings.fake, isNot(same(first)));
    });
  });

  group('IdentityController', () {
    test('identify stores id, name and district but never the NIC', () async {
      final identity = IdentityController(store, () => fake);

      final citizen = await identity.identify(
          nic: '199012345678', fullName: 'Nimal', phone: '0771234567', districtId: 1);

      expect(identity.isIdentified, isTrue);
      expect(citizen.created, isTrue);
      final stored = store.data.values.join(' ');
      expect(stored, contains('Nimal'));
      expect(stored, isNot(contains('199012345678')));
      expect(stored, isNot(contains('0771234567')));
      expect(IdentityController(store, () => fake).citizen?.fullName, 'Nimal');
    });

    test('a failed identify stores nothing and rethrows', () async {
      fake.failNetwork = true;
      final identity = IdentityController(store, () => fake);

      await expectLater(
        identity.identify(nic: '199012345678', fullName: 'Nimal', phone: '0771234567', districtId: 1),
        throwsA(isA<ApiException>()),
      );

      expect(identity.isIdentified, isFalse);
      expect(store.data, isEmpty);
    });

    test('clear forgets the citizen', () async {
      final identity = IdentityController(store, () => fake);
      await identity.identify(nic: '199012345678', fullName: 'Nimal', phone: '0771234567', districtId: 1);

      await identity.clear();

      expect(identity.isIdentified, isFalse);
      expect(IdentityController(store, () => fake).isIdentified, isFalse);
    });

    test('a corrupted stored value is ignored', () {
      store.data['identity.citizen'] = 'not json';

      expect(IdentityController(store, () => fake).isIdentified, isFalse);
    });
  });

  group('ReferenceDataController', () {
    test('caches after a successful fetch and keeps the cache when the next fetch fails', () async {
      final controller = ReferenceDataController(store, () => fake);
      expect(await controller.refresh(), isTrue);
      expect(controller.data!.districts, hasLength(4));

      fake.failNetwork = true;
      final offline = ReferenceDataController(store, () => fake);
      expect(offline.data!.districts, hasLength(4), reason: 'loaded from the cache without a call');
      expect(await offline.refresh(), isFalse);
      expect(offline.lastError!.isConnectionDown, isTrue);
      expect(offline.data!.districts, hasLength(4));
    });

    test('clearLocalData wipes the cache', () async {
      final controller = ReferenceDataController(store, () => fake);
      await controller.refresh();

      await controller.clearLocalData();

      expect(controller.data, isNull);
      expect(ReferenceDataController(store, () => fake).data, isNull);
    });
  });
}
