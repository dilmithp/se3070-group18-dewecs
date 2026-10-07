import 'package:dewecs_mobile/api/dewecs_api.dart';
import 'package:dewecs_mobile/api/fake_dewecs_api.dart';
import 'package:dewecs_mobile/main.dart';
import 'package:dewecs_mobile/state/location_service.dart';
import 'package:dewecs_mobile/state/photo_picker.dart';
import 'package:dewecs_mobile/storage/photo_store.dart';
import 'package:dewecs_mobile/state/app_dependencies.dart';
import 'package:dewecs_mobile/storage/key_value_store.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';

/// A GPS fix, or a failure, chosen by the test.
class FakeLocation implements LocationService {
  LocationReading? reading = const LocationReading(lat: 6.9271, lng: 79.8612, accuracyMeters: 12);
  LocationProblem? problem;
  int settingsOpened = 0;

  @override
  Future<LocationReading> current() async {
    final p = problem;
    if (p != null) {
      throw LocationException(p);
    }
    return reading!;
  }

  @override
  Future<void> openSettings() async => settingsOpened++;
}

/// Returns a path in the in-memory photo store, or null (the user cancelled).
class FakePicker implements PhotoPicker {
  FakePicker(this.photos);

  final MemoryPhotoStore photos;
  bool cancel = false;

  @override
  Future<String?> pick(PhotoSource source) async {
    if (cancel) {
      return null;
    }
    photos.put('picked-${source.name}.jpg', [0xFF, 0xD8, 0xFF, 0xE0, 1, 2, 3]);
    return 'picked-${source.name}.jpg';
  }
}

/// Everything a widget test needs: in-memory storage, the fake server with no latency and fake device services.
class TestApp {
  TestApp({MemoryKeyValueStore? store, FakeDewecsApi? fake, DewecsApi? api})
      : store = store ?? MemoryKeyValueStore(),
        fake = fake ?? FakeDewecsApi(latency: Duration.zero) {
    picker = FakePicker(photos);
    dependencies = AppDependencies.create(
      this.store,
      apiOverride: api ?? this.fake,
      photoStore: photos,
      location: location,
      picker: picker,
      opener: (uri) async {
        opened.add(uri);
        return openResults.isEmpty ? true : openResults.removeAt(0);
      },
      autoSchedule: false,
    );
  }

  final MemoryKeyValueStore store;
  final FakeDewecsApi fake;
  final MemoryPhotoStore photos = MemoryPhotoStore();
  final FakeLocation location = FakeLocation();
  late final FakePicker picker;

  /// Every address the app tried to open (maps), and the answers to give in order (default: success).
  final List<Uri> opened = [];
  final List<bool> openResults = [];
  late final AppDependencies dependencies;

  /// Identifies a citizen before the app starts, as if the user had done it earlier.
  Future<void> identify({String nic = '199012345678'}) async {
    await dependencies.identity.identify(nic: nic, fullName: 'Nimal Perera', phone: '0771234567', districtId: 1);
  }

  Future<void> pump(
    WidgetTester tester, {
    Size size = const Size(800, 1800),
    double textScale = 1.0,
    Brightness brightness = Brightness.light,
  }) async {
    tester.view.physicalSize = size;
    tester.view.devicePixelRatio = 1.0;
    tester.platformDispatcher.textScaleFactorTestValue = textScale;
    tester.platformDispatcher.platformBrightnessTestValue = brightness;
    addTearDown(tester.view.resetPhysicalSize);
    addTearDown(tester.view.resetDevicePixelRatio);
    addTearDown(tester.platformDispatcher.clearAllTestValues);
    await tester.pumpWidget(DewecsApp(dependencies: dependencies));
    await tester.pumpAndSettle();
  }
}

Future<void> enterField(WidgetTester tester, String label, String text) async {
  await tester.enterText(find.widgetWithText(TextFormField, label), text);
}
