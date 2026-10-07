import 'package:flutter/material.dart';

import '../state/location_service.dart';
import '../strings.dart';
import '../validators.dart';

/// "Use my location" plus manual latitude and longitude fields (the fallback, and the only way on an emulator
/// without GPS). The controllers belong to the form so it can read and validate them.
class LocationSection extends StatefulWidget {
  const LocationSection({super.key, required this.service, required this.lat, required this.lng});

  final LocationService service;
  final TextEditingController lat;
  final TextEditingController lng;

  @override
  State<LocationSection> createState() => _LocationSectionState();
}

class _LocationSectionState extends State<LocationSection> {
  bool _busy = false;
  String? _message;
  bool _canOpenSettings = false;

  Future<void> _useMyLocation() async {
    setState(() {
      _busy = true;
      _message = null;
      _canOpenSettings = false;
    });
    try {
      final reading = await widget.service.current();
      widget.lat.text = reading.lat.toStringAsFixed(7);
      widget.lng.text = reading.lng.toStringAsFixed(7);
      final accuracy = reading.accuracyMeters;
      final parts = <String>[
        if (accuracy != null) S.accuracyMeters(accuracy.round()),
        if (reading.lastKnown) S.lastKnownPosition,
      ];
      if (mounted) {
        setState(() => _message = parts.isEmpty ? null : parts.join(' '));
      }
    } on LocationException catch (e) {
      if (mounted) {
        setState(() {
          _message = _problemText(e.problem);
          _canOpenSettings = e.problem == LocationProblem.deniedForever || e.problem == LocationProblem.servicesOff;
        });
      }
    } finally {
      if (mounted) {
        setState(() => _busy = false);
      }
    }
  }

  String _problemText(LocationProblem problem) {
    switch (problem) {
      case LocationProblem.servicesOff:
        return S.locationServicesOff;
      case LocationProblem.denied:
        return S.locationDenied;
      case LocationProblem.deniedForever:
        return S.locationDeniedForever;
      case LocationProblem.unavailable:
        return S.locationUnavailable;
    }
  }

  @override
  Widget build(BuildContext context) {
    final theme = Theme.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(S.locationTitle, style: theme.textTheme.titleMedium),
        const SizedBox(height: 8),
        Align(
          alignment: Alignment.centerLeft,
          child: OutlinedButton.icon(
            onPressed: _busy ? null : _useMyLocation,
            icon: _busy
                ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(strokeWidth: 2))
                : const Icon(Icons.my_location),
            label: Text(_busy ? S.locating : S.useMyLocation),
          ),
        ),
        if (_message != null)
          Padding(
            padding: const EdgeInsets.only(top: 8),
            child: Semantics(liveRegion: true, child: Text(_message!)),
          ),
        if (_canOpenSettings)
          Align(
            alignment: Alignment.centerLeft,
            child: TextButton(onPressed: widget.service.openSettings, child: const Text(S.openAppSettings)),
          ),
        const SizedBox(height: 12),
        Row(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Expanded(
              child: TextFormField(
                controller: widget.lat,
                decoration: const InputDecoration(labelText: S.latLabel),
                keyboardType: const TextInputType.numberWithOptions(decimal: true, signed: true),
                validator: validateLatitude,
              ),
            ),
            const SizedBox(width: 12),
            Expanded(
              child: TextFormField(
                controller: widget.lng,
                decoration: const InputDecoration(labelText: S.lngLabel),
                keyboardType: const TextInputType.numberWithOptions(decimal: true, signed: true),
                validator: validateLongitude,
              ),
            ),
          ],
        ),
      ],
    );
  }
}
