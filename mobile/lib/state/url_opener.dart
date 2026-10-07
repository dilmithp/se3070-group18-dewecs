import 'package:url_launcher/url_launcher.dart';

/// Opens a map. A function type so tests can replace it. Returns false when nothing could handle the address.
typedef UrlOpener = Future<bool> Function(Uri uri);

Future<bool> launchExternally(Uri uri) async {
  try {
    return await launchUrl(uri, mode: LaunchMode.externalApplication);
  } on Exception {
    return false;
  }
}

/// A geo: address opens a map app; the OpenStreetMap page is the fallback when none is installed.
Uri geoUri(double lat, double lng) => Uri.parse('geo:$lat,$lng?q=$lat,$lng(Report)');

Uri openStreetMapUri(double lat, double lng) =>
    Uri.parse('https://www.openstreetmap.org/?mlat=$lat&mlon=$lng#map=16/$lat/$lng');
