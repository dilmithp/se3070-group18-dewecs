/// Where a report on this phone is in its journey to the server.
/// "sending" exists only in memory: it is never persisted, and on app start it becomes "queued" again.
enum QueueState { queued, sending, synced, needsAttention }

/// A report saved on the phone first and sent when a connection exists. [capturedAt] is created once, when the user
/// taps Submit, and is resent unchanged on every retry, so a lost response can never create a duplicate.
class QueuedReport {
  QueuedReport({
    required this.localId,
    required this.createdAtMs,
    required this.citizenId,
    required this.districtId,
    required this.districtName,
    required this.category,
    required this.description,
    required this.gpsLat,
    required this.gpsLng,
    required this.capturedAt,
    this.photoPath,
    this.state = QueueState.queued,
    this.attempts = 0,
    this.nextAttemptAtMs,
    this.lastError,
    Map<String, String>? fieldErrors,
    this.serverId,
    this.photoUploaded = false,
    this.photoProblem,
  }) : fieldErrors = fieldErrors ?? {};

  final String localId;
  final int createdAtMs;
  int citizenId;
  int districtId;
  String districtName;
  String category;
  String description;
  double gpsLat;
  double gpsLng;
  String capturedAt;
  String? photoPath;

  QueueState state;
  int attempts;
  int? nextAttemptAtMs;
  String? lastError;
  Map<String, String> fieldErrors;

  /// Set as soon as the server answered 200 or 201 for the report.
  int? serverId;
  bool photoUploaded;

  /// Why the photo could not be sent for good (the report itself is stored).
  String? photoProblem;

  bool get reportStored => serverId != null;

  bool get photoPending => photoPath != null && !photoUploaded && photoProblem == null;

  /// Still on its way to the server (waiting or being sent).
  bool get isActive => state == QueueState.queued || state == QueueState.sending;

  void markSending() {
    state = QueueState.sending;
  }

  /// A retryable failure: stay queued, count the attempt and wait until [nextAttemptAtMs].
  void markRetry(String detail, int nextAttemptAtMs) {
    state = QueueState.queued;
    attempts++;
    lastError = detail;
    this.nextAttemptAtMs = nextAttemptAtMs;
  }

  /// A permanent failure: the user must edit or delete the report.
  void markNeedsAttention(String detail, [Map<String, String> fieldErrors = const {}]) {
    state = QueueState.needsAttention;
    lastError = detail;
    this.fieldErrors = Map.of(fieldErrors);
    nextAttemptAtMs = null;
  }

  void markSynced() {
    state = QueueState.synced;
    lastError = null;
    fieldErrors = {};
    nextAttemptAtMs = null;
    attempts = 0;
  }

  /// Edit and retry after a permanent error. Only valid while nothing is stored on the server; the new capturedAt is
  /// safe because the server has never seen the old one.
  void editAndRequeue({
    required int districtId,
    required String districtName,
    required String category,
    required String description,
    required double gpsLat,
    required double gpsLng,
    required String capturedAt,
    String? photoPath,
  }) {
    assert(!reportStored, 'a stored report cannot be edited');
    this.districtId = districtId;
    this.districtName = districtName;
    this.category = category;
    this.description = description;
    this.gpsLat = gpsLat;
    this.gpsLng = gpsLng;
    this.capturedAt = capturedAt;
    this.photoPath = photoPath;
    photoUploaded = false;
    photoProblem = null;
    state = QueueState.queued;
    attempts = 0;
    nextAttemptAtMs = null;
    lastError = null;
    fieldErrors = {};
  }

  Map<String, dynamic> toJson() => {
        'localId': localId,
        'createdAtMs': createdAtMs,
        'citizenId': citizenId,
        'districtId': districtId,
        'districtName': districtName,
        'category': category,
        'description': description,
        'gpsLat': gpsLat,
        'gpsLng': gpsLng,
        'capturedAt': capturedAt,
        'photoPath': photoPath,
        // "sending" is never persisted
        'state': (state == QueueState.sending ? QueueState.queued : state).name,
        'attempts': attempts,
        'nextAttemptAtMs': nextAttemptAtMs,
        'lastError': lastError,
        'fieldErrors': fieldErrors,
        'serverId': serverId,
        'photoUploaded': photoUploaded,
        'photoProblem': photoProblem,
      };

  factory QueuedReport.fromJson(Map<String, dynamic> json) {
    final stored = QueueState.values.where((s) => s.name == json['state']).firstOrNull ?? QueueState.queued;
    final errors = <String, String>{};
    final rawErrors = json['fieldErrors'];
    if (rawErrors is Map) {
      rawErrors.forEach((k, v) => errors[k.toString()] = v.toString());
    }
    return QueuedReport(
      localId: json['localId'] as String,
      createdAtMs: (json['createdAtMs'] as num).toInt(),
      citizenId: (json['citizenId'] as num).toInt(),
      districtId: (json['districtId'] as num).toInt(),
      districtName: json['districtName'] as String,
      category: json['category'] as String,
      description: json['description'] as String,
      gpsLat: (json['gpsLat'] as num).toDouble(),
      gpsLng: (json['gpsLng'] as num).toDouble(),
      capturedAt: json['capturedAt'] as String,
      photoPath: json['photoPath'] as String?,
      state: stored == QueueState.sending ? QueueState.queued : stored,
      attempts: (json['attempts'] as num?)?.toInt() ?? 0,
      nextAttemptAtMs: (json['nextAttemptAtMs'] as num?)?.toInt(),
      lastError: json['lastError'] as String?,
      fieldErrors: errors,
      serverId: (json['serverId'] as num?)?.toInt(),
      photoUploaded: json['photoUploaded'] as bool? ?? false,
      photoProblem: json['photoProblem'] as String?,
    );
  }
}
