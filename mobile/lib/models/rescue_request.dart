import 'district.dart';
import 'sri_lanka_time.dart';

/// A rescue request as the officer pages return it. [status] (PENDING, ASSIGNED, COMPLETED, CANCELLED) and
/// [priority] (LOW, MODERATE, HIGH, CRITICAL) stay raw strings so unknown values survive.
class RescueRequest {
  const RescueRequest({
    required this.id,
    required this.requesterName,
    required this.requesterPhone,
    required this.districtId,
    required this.districtName,
    required this.gpsLat,
    required this.gpsLng,
    required this.description,
    required this.priority,
    required this.status,
    required this.assignedTeamName,
    required this.submittedAt,
    required this.assignedAt,
    required this.completedAt,
  });

  final int id;
  final String requesterName;
  final String requesterPhone;
  final int districtId;
  final String districtName;
  final double? gpsLat;
  final double? gpsLng;
  final String description;
  final String priority;
  final String status;
  final String? assignedTeamName;
  final DateTime? submittedAt;
  final DateTime? assignedAt;
  final DateTime? completedAt;

  bool get hasLocation => gpsLat != null && gpsLng != null;

  bool get canAssign => status == 'PENDING';

  bool get canComplete => status == 'ASSIGNED';

  bool get canCancel => status == 'PENDING' || status == 'ASSIGNED';

  factory RescueRequest.fromJson(Map<String, dynamic> json) => RescueRequest(
        id: (json['id'] as num).toInt(),
        requesterName: json['requesterName'] as String,
        requesterPhone: json['requesterPhone'] as String,
        districtId: (json['districtId'] as num).toInt(),
        districtName: json['districtName'] as String,
        gpsLat: (json['gpsLat'] as num?)?.toDouble(),
        gpsLng: (json['gpsLng'] as num?)?.toDouble(),
        description: json['description'] as String,
        priority: json['priority'] as String,
        status: json['status'] as String,
        assignedTeamName: json['assignedTeamName'] as String?,
        submittedAt: _time(json['submittedAt']),
        assignedAt: _time(json['assignedAt']),
        completedAt: _time(json['completedAt']),
      );
}

/// A rescue team that can be assigned (the server only offers teams that are AVAILABLE).
class RescueTeam {
  const RescueTeam({required this.id, required this.name, required this.districtName, required this.status});

  final int id;
  final String name;
  final String? districtName;
  final String status;

  factory RescueTeam.fromJson(Map<String, dynamic> json) {
    final district = json['district'];
    return RescueTeam(
      id: (json['id'] as num).toInt(),
      name: json['name'] as String,
      districtName: district is Map<String, dynamic> ? district['name'] as String? : null,
      status: (json['status'] as String?) ?? 'AVAILABLE',
    );
  }
}

/// `GET /rescue-requests`: the requests that match the filters, plus the values the filter drop-downs offer.
class RescueRequestList {
  const RescueRequestList({
    required this.requests,
    required this.districts,
    required this.statuses,
    required this.priorities,
  });

  final List<RescueRequest> requests;
  final List<District> districts;
  final List<String> statuses;
  final List<String> priorities;

  factory RescueRequestList.fromJson(Map<String, dynamic> json) => RescueRequestList(
        requests: _list(json['requests'], RescueRequest.fromJson),
        districts: _list(json['districts'], District.fromJson),
        statuses: _strings(json['statuses']),
        priorities: _strings(json['priorities']),
      );
}

/// `GET /rescue-requests/{id}`: one request and the teams it could be assigned to.
class RescueRequestDetail {
  const RescueRequestDetail({required this.request, required this.availableTeams});

  final RescueRequest request;
  final List<RescueTeam> availableTeams;

  factory RescueRequestDetail.fromJson(Map<String, dynamic> json) => RescueRequestDetail(
        request: RescueRequest.fromJson(json['request'] as Map<String, dynamic>),
        availableTeams: _list(json['availableTeams'], RescueTeam.fromJson),
      );
}

/// `GET /rescue-requests/new`: what the submit form offers to choose from.
class RescueRequestFormData {
  const RescueRequestFormData({required this.districts, required this.priorities});

  final List<District> districts;
  final List<String> priorities;

  factory RescueRequestFormData.fromJson(Map<String, dynamic> json) => RescueRequestFormData(
        districts: _list(json['districts'], District.fromJson),
        priorities: _strings(json['priorities']),
      );
}

List<T> _list<T>(Object? raw, T Function(Map<String, dynamic>) parse) =>
    (raw as List<dynamic>? ?? const []).map((e) => parse(e as Map<String, dynamic>)).toList();

List<String> _strings(Object? raw) => (raw as List<dynamic>? ?? const []).map((s) => s as String).toList();

DateTime? _time(Object? raw) => raw is String ? parseContractTime(raw) : null;
