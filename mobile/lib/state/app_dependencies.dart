import 'package:flutter/widgets.dart';
import 'package:provider/provider.dart';

import '../api/dewecs_api.dart';
import '../api/operations_api.dart';
import '../storage/file_photo_store.dart';
import '../storage/key_value_store.dart';
import '../storage/photo_store.dart';
import '../storage/queue_repository.dart';
import '../sync/connectivity_trigger.dart';
import '../sync/sync_service.dart';
import 'identity_controller.dart';
import 'location_service.dart';
import 'photo_picker.dart';
import 'reference_data_controller.dart';
import 'reports_controller.dart';
import 'rescue_requests_controller.dart';
import 'url_opener.dart';
import 'settings_controller.dart';
import 'shelters_controller.dart';

/// Creates the controllers once and hands them to the widget tree with Provider.
class AppDependencies {
  AppDependencies._({
    required this.settings,
    required this.identity,
    required this.reference,
    required this.sync,
    required this.reports,
    required this.shelters,
    required this.rescue,
    required this.opener,
    required this.photos,
    required this.location,
    required this.picker,
  });

  final SettingsController settings;
  final IdentityController identity;
  final ReferenceDataController reference;
  final SyncService sync;
  final ReportsController reports;
  final SheltersController shelters;
  final RescueRequestsController rescue;
  final UrlOpener opener;
  final PhotoStore photos;
  final LocationService location;
  final PhotoPicker picker;
  ConnectivityTrigger? _trigger;

  /// The optional arguments let tests plug in the fake server, an in-memory photo store and fake device services.
  factory AppDependencies.create(
    KeyValueStore store, {
    DewecsApi? apiOverride,
    OperationsApi? operationsOverride,
    PhotoStore? photoStore,
    LocationService? location,
    PhotoPicker? picker,
    UrlOpener? opener,
    bool autoSchedule = true,
  }) {
    final settings = SettingsController(store, apiOverride: apiOverride, operationsOverride: operationsOverride);
    DewecsApi api() => settings.api;
    final identity = IdentityController(store, api);
    final reference = ReferenceDataController(store, api);
    final photos = photoStore ?? FilePhotoStore();
    final sync = SyncService(
      repository: QueueRepository(store),
      api: api,
      photos: photos,
      citizenId: () => identity.citizen?.id,
      onIdentityLost: identity.clear,
      autoSchedule: autoSchedule,
    );
    final reports = ReportsController(store, api, identity, sync);
    final shelters = SheltersController(() => settings.operations);
    final rescue = RescueRequestsController(() => settings.operations);
    // After a new identification (for example after a server reset) unsent reports move to the new citizen.
    identity.onIdentified = (citizen) async {
      await sync.adoptCitizen(citizen.id);
      await sync.syncNow(force: true);
    };
    settings
      ..registerClearable(identity)
      ..registerClearable(reference)
      ..registerClearable(sync)
      ..registerClearable(reports)
      ..registerClearable(shelters)
      ..registerClearable(rescue);
    return AppDependencies._(
      settings: settings,
      identity: identity,
      reference: reference,
      sync: sync,
      reports: reports,
      shelters: shelters,
      rescue: rescue,
      opener: opener ?? launchExternally,
      photos: photos,
      location: location ?? GeolocatorLocationService(),
      picker: picker ?? ImagePickerPhotoPicker(),
    );
  }

  /// Loads the saved queue and sends what is waiting (app start). Not used by widget tests.
  void start() {
    sync.load();
    if (identity.isIdentified) {
      sync.syncNow(force: true);
    }
    _trigger = ConnectivityTrigger(sync)..start();
  }

  void dispose() {
    _trigger?.stop();
  }

  Widget provide({required Widget child}) {
    return MultiProvider(
      providers: [
        Provider<AppDependencies>.value(value: this),
        ChangeNotifierProvider<SettingsController>.value(value: settings),
        ChangeNotifierProvider<IdentityController>.value(value: identity),
        ChangeNotifierProvider<ReferenceDataController>.value(value: reference),
        ChangeNotifierProvider<SyncService>.value(value: sync),
        ChangeNotifierProvider<ReportsController>.value(value: reports),
        ChangeNotifierProvider<SheltersController>.value(value: shelters),
        ChangeNotifierProvider<RescueRequestsController>.value(value: rescue),
      ],
      child: child,
    );
  }
}
