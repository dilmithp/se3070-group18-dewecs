/// The deployed backend (AWS). Plain http for now: see the network security config in android/ and the warning the
/// Settings screen shows. Move to https as soon as the server has a certificate, then change this line.
const deployedBaseUrl = 'http://localhost:8080';

/// The server address a fresh install starts with. Override it for development without editing code:
///   flutter run --dart-define=DEWECS_BASE_URL=http://10.0.2.2:8080     (Android emulator to a local backend)
/// The address can always be changed later in Settings.
String defaultBaseUrl() => const String.fromEnvironment('DEWECS_BASE_URL', defaultValue: deployedBaseUrl);
