import 'ground_report.dart';
import 'queued_report.dart';
import 'sri_lanka_time.dart';

/// One row of My reports: either a report from the server or one that is still on this phone.
/// Status is kept as the raw string the server sent so unknown values are shown as received.
class ReportItem {
  const ReportItem({
    required this.category,
    required this.description,
    required this.districtName,
    required this.gpsLat,
    required this.gpsLng,
    required this.submittedAt,
    required this.status,
    this.localId,
    this.serverId,
    this.queueState,
    this.actionNote,
    this.serverPhotoPath,
    this.localPhotoPath,
    this.photoProblem,
    this.error,
    this.fieldErrors = const {},
    this.attempts = 0,
  });

  final String category;
  final String description;
  final String districtName;
  final double gpsLat;
  final double gpsLng;
  final DateTime submittedAt;

  /// The server status, or PENDING_REVIEW for a report that was sent but not yet listed by the server.
  final String status;

  final String? localId;
  final int? serverId;
  final QueueState? queueState;
  final String? actionNote;

  /// A photo path starting with /api/v1/photos/ (displayable), or null.
  final String? serverPhotoPath;
  final String? localPhotoPath;
  final String? photoProblem;
  final String? error;
  final Map<String, String> fieldErrors;
  final int attempts;

  bool get isLocal => localId != null;

  /// Not yet accepted by the server (waiting, sending or refused).
  bool get isUnsent => queueState == QueueState.queued ||
      queueState == QueueState.sending ||
      queueState == QueueState.needsAttention;

  factory ReportItem.fromServer(GroundReport r) => ReportItem(
        category: r.category,
        description: r.description,
        districtName: r.districtName,
        gpsLat: r.gpsLat,
        gpsLng: r.gpsLng,
        submittedAt: r.submittedAt,
        status: r.status,
        serverId: r.id,
        // The note is only meaningful when the report was actioned.
        actionNote: r.status == 'ACTIONED' ? r.actionNote : null,
        serverPhotoPath: r.hasDisplayablePhoto ? r.photoUrl : null,
      );

  factory ReportItem.fromQueued(QueuedReport q) => ReportItem(
        category: q.category,
        description: q.description,
        districtName: q.districtName,
        gpsLat: q.gpsLat,
        gpsLng: q.gpsLng,
        submittedAt: parseContractTime(q.capturedAt),
        status: 'PENDING_REVIEW',
        localId: q.localId,
        serverId: q.serverId,
        queueState: q.state,
        localPhotoPath: q.photoPath,
        photoProblem: q.photoProblem,
        error: q.lastError,
        fieldErrors: q.fieldErrors,
        attempts: q.attempts,
      );
}
