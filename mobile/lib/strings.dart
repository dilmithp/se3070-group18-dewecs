/// Every user-visible string lives here (English only).
class S {
  S._();

  static const appName = 'DEWECS Report';

  // Navigation
  static const navHome = 'My reports';
  static const navNewReport = 'New report';
  static const navSettings = 'Settings';

  // Generic
  static const ok = 'OK';
  static const cancel = 'Cancel';
  static const save = 'Save';
  static const retry = 'Try again';
  static const loading = 'Loading...';
  static const noConnection = 'No connection to the server.';
  static const unexpectedError = 'Something went wrong. Please try again.';

  // Identify
  static const identifyTitle = 'Who are you?';
  static const identifyIntro =
      'Tell us who you are once. Your reports are linked to your NIC so officers can follow them up. '
      'The app does not keep your NIC on this phone.';
  static const needsIdentityTitle = 'Identify yourself first';
  static const needsIdentityBody = 'Reporting needs your name, NIC, phone number and district (one time only).';
  static const identifyButton = 'Identify';
  static const identifyAgain = 'Identify again';
  static const nicLabel = 'NIC number';
  static const nicHelp = '9 digits and V or X, or 12 digits';
  static const nameLabel = 'Full name';
  static const phoneLabel = 'Phone number';
  static const districtLabel = 'District';
  static const districtsMissing = 'The district list is not loaded yet.';
  static const reloadDistricts = 'Load districts';
  static const identifyOffline =
      'No connection. Identifying needs one successful connection. Your answers are kept, try again when you are online.';
  static const identifyDone = 'Identified. You can report now.';
  static const nicRequired = 'Enter your NIC number.';
  static const nicInvalid = 'Enter 9 digits followed by V or X, or 12 digits.';
  static const nameRequired = 'Enter your full name.';
  static const nameTooLong = 'The name can have at most 120 characters.';
  static const phoneRequired = 'Enter a phone number.';
  static const phoneInvalid = 'Enter a valid phone number, for example 0771234567.';
  static const districtRequired = 'Choose a district.';

  // Settings
  static const settingsServer = 'Server';
  static const baseUrlLabel = 'Server address';
  static const baseUrlHelp = 'For the Android emulator use http://10.0.2.2:8080';
  static const baseUrlInvalid = 'Enter a web address starting with http:// or https://.';
  static const baseUrlSaved = 'Server address saved.';
  static const testConnection = 'Test connection';
  static const testing = 'Testing...';
  static String connectionOk(int districts) => 'Connected. The server knows $districts districts.';
  static String connectionFailed(String why) => 'Connection failed: $why';
  static const connectionDemoNote =
      'Demo mode is on, so this tested the built-in demo server, not the address above. Switch Demo mode off to test the real server.';
  static String connectionTried(String address, int ms) => 'Tried $address ($ms ms)';  static const settingsYou = 'You';
  static const notIdentified = 'Not identified yet.';
  static String identifiedAs(String name, String district) => '$name, $district';
  static const demoMode = 'Demo mode';
  static const demoModeHelp = 'Uses a built-in demo server so the app can be shown without a backend.';
  static const demoSwitchTitle = 'Switch demo mode?';
  static const demoSwitchBody =
      'This clears your identity, the waiting reports and the saved data on this phone, because demo data must never '
      'reach the real server.';
  static const demoSwitchConfirm = 'Switch and clear';
  static const demoForceNetwork = 'Demo: simulate no connection';
  static const demoForce500 = 'Demo: simulate a server error (500)';
  static const demoResetServer = 'Demo: wipe the demo server';
  static const demoServerWiped = 'The demo server was wiped.';
  static const settingsDebug = 'Debug';
  static const syncQueueScreen = 'Sync queue';
  static const syncQueueEmpty = 'The queue is empty.';
  static const syncRunning = 'Sending now...';
  static const syncIdle = 'Idle';

  // New report
  static const newReportTitle = 'Report a hazard';
  static const editReportTitle = 'Fix and send again';
  static const categoryLabelText = 'What is happening?';
  static const categoryRequired = 'Choose what is happening.';
  static String categoryName(String raw) {
    switch (raw) {
      case 'FLOOD':
        return 'Flood';
      case 'LANDSLIDE':
        return 'Landslide';
      case 'CYCLONE':
        return 'Cyclone';
      case 'DROUGHT':
        return 'Drought';
    }
    return raw.isEmpty ? raw : raw[0] + raw.substring(1).toLowerCase().replaceAll('_', ' ');
  }

  static const descriptionLabel = 'Describe what you see';
  static const descriptionRequired = 'Describe what you see.';
  static const descriptionTooLong = 'The description can have at most 2000 characters.';
  static const locationTitle = 'Location';
  static const useMyLocation = 'Use my location';
  static const locating = 'Finding your position...';
  static const latLabel = 'Latitude';
  static const lngLabel = 'Longitude';
  static const latRequired = 'Enter the latitude or use your location.';
  static const latInvalid = 'Latitude must be a number.';
  static const latRange = 'Latitude must be between -90 and 90.';
  static const lngRequired = 'Enter the longitude or use your location.';
  static const lngInvalid = 'Longitude must be a number.';
  static const lngRange = 'Longitude must be between -180 and 180.';
  static String accuracyMeters(int meters) => 'Accuracy about $meters m';
  static const lastKnownPosition = 'Using your last known position (a fresh one was not available in time).';
  static const locationServicesOff = 'Location is switched off. Turn it on in the phone settings or type the position below.';
  static const locationDenied = 'Location permission was denied. Allow it, or type the position below.';
  static const locationDeniedForever =
      'Location permission is blocked. Open the app settings to allow it, or type the position below.';
  static const locationUnavailable = 'No position could be found. Try again outdoors or type the position below.';
  static const openAppSettings = 'Open app settings';
  static const photoTitle = 'Photo (optional)';
  static const takePhoto = 'Camera';
  static const choosePhoto = 'Gallery';
  static const removePhoto = 'Remove photo';
  static const photoPreview = 'Photo attached to the report';
  static const photoPickFailed = 'The photo could not be added.';
  static const submitReport = 'Send report';
  static const reportSavedOnPhone = 'Saved on this phone. It will be sent when there is a connection.';
  static const reportSent = 'Report sent.';
  static const reportNeedsAttentionNow = 'The server refused the report. Open it in My reports to fix it.';

  // My reports
  static const homeEmptyTitle = 'No reports yet';
  static const homeEmptyBody = 'Reports you send will appear here with their status.';
  static const homeEmptyAction = 'Report a hazard';
  static const offlineBanner = 'Offline - showing saved data';
  static String refreshFailed(String why) => 'Could not refresh: $why';
  static const loadMore = 'Load more';
  static const refreshHint = 'Pull down to refresh';
  static String waitingToSend(int count) =>
      count == 1 ? '1 report is waiting to be sent.' : '$count reports are waiting to be sent.';
  static const sendNow = 'Send now';
  static const summaryUnsent = 'Unsent';
  static const summaryInReview = 'In review';
  static const summaryResolved = 'Actioned';
  static const sectionOnPhone = 'On this phone';
  static const sectionSent = 'Sent';
  static const photoNotSent = 'The photo could not be sent.';
  static const identityReset =
      'The server no longer knows you. Identify again; reports waiting on this phone are kept.';

  static String statusLabel(String raw) {
    switch (raw) {
      case 'PENDING_REVIEW':
        return 'Waiting for review';
      case 'VERIFIED':
        return 'Reviewed by officers';
      case 'ACTIONED':
        return 'Action taken';
      case 'REJECTED':
        return 'Not accepted';
      case 'NEEDS_INFO':
        return 'More information needed';
      case 'PENDING_SYNC':
        return 'Waiting to send';
    }
    return raw;
  }

  static const stateQueued = 'Waiting to send';
  static const stateSending = 'Sending';
  static const stateNeedsAttention = 'Needs attention';

  // Report detail
  static const detailTitle = 'Report';
  static const detailCategory = 'Category';
  static const detailDescription = 'Description';
  static const detailDistrict = 'District';
  static const detailLocation = 'Location';
  static const detailSubmitted = 'Submitted';
  static const detailStatus = 'Status';
  static const detailActionNote = 'What the officers did';
  static const detailPhoto = 'Photo';
  static const photoPlaceholder = 'No photo';
  static const photoLoadFailed = 'The photo could not be loaded.';
  static const openInMaps = 'Open in maps';
  static const mapsFailed = 'No map app could be opened.';
  static const detailProblem = 'Why it was not sent';
  static String detailAttempts(int attempts) => attempts == 1 ? '1 attempt so far' : '$attempts attempts so far';
  static const editAndRetry = 'Edit and send again';
  static const deleteReport = 'Delete';
  static const deleteTitle = 'Delete this report?';
  static const deleteBody = 'It has not reached the server yet and will be removed from this phone.';
  static const reportDeleted = 'Report deleted.';

  // Placeholders used until a screen is built
  static const comingSoon = 'Coming soon';
}
