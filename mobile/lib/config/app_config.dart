import 'package:flutter/foundation.dart';

/// The Android emulator reaches the laptop at 10.0.2.2; everywhere else the backend is localhost.
String defaultBaseUrl() =>
    defaultTargetPlatform == TargetPlatform.android ? 'http://10.0.2.2:8080' : 'http://localhost:8080';
