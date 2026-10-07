import 'dart:async';

import 'package:geolocator/geolocator.dart';

enum LocationProblem { servicesOff, denied, deniedForever, unavailable }

class LocationException implements Exception {
  const LocationException(this.problem);

  final LocationProblem problem;
}

class LocationReading {
  const LocationReading({required this.lat, required this.lng, this.accuracyMeters, this.lastKnown = false});

  final double lat;
  final double lng;
  final double? accuracyMeters;

  /// True when the live fix timed out and the last known position was used instead.
  final bool lastKnown;
}

/// GPS access behind a small interface so tests can plug in a fake.
abstract class LocationService {
  /// Throws LocationException with the reason when no position can be had.
  Future<LocationReading> current();

  Future<void> openSettings();
}

class GeolocatorLocationService implements LocationService {
  static const _timeout = Duration(seconds: 10);

  @override
  Future<LocationReading> current() async {
    if (!await Geolocator.isLocationServiceEnabled()) {
      throw const LocationException(LocationProblem.servicesOff);
    }
    var permission = await Geolocator.checkPermission();
    if (permission == LocationPermission.denied) {
      permission = await Geolocator.requestPermission();
    }
    if (permission == LocationPermission.deniedForever) {
      throw const LocationException(LocationProblem.deniedForever);
    }
    if (permission == LocationPermission.denied) {
      throw const LocationException(LocationProblem.denied);
    }
    try {
      final position = await Geolocator.getCurrentPosition(
        locationSettings: const LocationSettings(accuracy: LocationAccuracy.high, timeLimit: _timeout),
      );
      return LocationReading(lat: position.latitude, lng: position.longitude, accuracyMeters: position.accuracy);
    } on TimeoutException {
      final last = await Geolocator.getLastKnownPosition();
      if (last == null) {
        throw const LocationException(LocationProblem.unavailable);
      }
      return LocationReading(
          lat: last.latitude, lng: last.longitude, accuracyMeters: last.accuracy, lastKnown: true);
    }
  }

  @override
  Future<void> openSettings() async {
    await Geolocator.openAppSettings();
  }
}
