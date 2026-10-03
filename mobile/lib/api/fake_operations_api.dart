import 'dart:async';

import '../models/action_result.dart';
import '../models/district.dart';
import '../models/organization.dart';
import '../models/rescue_request.dart';
import '../models/shelter.dart';
import '../models/sri_lanka_time.dart';
import 'api_exception.dart';
import 'operations_api.dart';

/// In-memory stand-in for the officer pages (Demo mode and tests). It applies the same rules and uses the same
/// messages as the backend services, so the screens behave as they will against the real server. The two failure
/// switches work like the ones of FakeDewecsApi.
class FakeOperationsApi implements OperationsApi {
  FakeOperationsApi({this.latency = const Duration(milliseconds: 400), DateTime Function()? now})
    : _now = now ?? DateTime.now {
    _seed();
  }

  /// Delay before every answer (tests pass Duration.zero).
  final Duration latency;
  final DateTime Function() _now;

  /// When true every call fails like a dead connection.
  bool failNetwork = false;

  /// When set (for example 500) every call fails with that HTTP status.
  int? failStatus;

  /// Number of calls that reached the server logic (not counting forced failures).
  int callCount = 0;

  static const _districts = [
    District(id: 1, name: 'Colombo'),
    District(id: 2, name: 'Galle'),
    District(id: 3, name: 'Jaffna'),
    District(id: 4, name: 'Kandy'),
  ];
  static const _organizations = [
    Organization(id: 1, name: 'Disaster Management Centre', type: 'GOVERNMENT'),
    Organization(id: 2, name: 'Red Cross', type: 'NGO'),
  ];
  static const _shelterStatuses = ['OPEN', 'FULL', 'CLOSED'];
  static const _rescueStatuses = ['PENDING', 'ASSIGNED', 'COMPLETED', 'CANCELLED'];
  static const _priorities = ['LOW', 'MODERATE', 'HIGH', 'CRITICAL'];

  final List<_Shelter> _shelters = [];
  final List<_Occupant> _occupants = [];
  final List<_Team> _teams = [];
  final List<_Rescue> _rescues = [];
  int _nextShelterId = 1;
  int _nextOccupantId = 1;
  int _nextRescueId = 1;

  /// Simulates the server database being wiped and seeded again.
  void resetServer() {
    _shelters.clear();
    _occupants.clear();
    _teams.clear();
    _rescues.clear();
    _nextShelterId = 1;
    _nextOccupantId = 1;
    _nextRescueId = 1;
    _seed();
  }

  void _seed() {
    final now = sriLankaNow(_now());
    const people = [
      ('Amaya Silva', '200000000101'),
      ('Kasun Perera', '199012345678'),
      ('Dilani Jayasinghe', '857412369V'),
      ('Ruwan Fernando', '199545612389'),
    ];

    void shelter(String name, int districtId, int organizationId, int capacity, int occupants, String status) {
      final s = _Shelter(_nextShelterId++, name, districtId, organizationId, capacity, status);
      _shelters.add(s);
      for (var i = 0; i < occupants; i++) {
        final p = people[i % people.length];
        _occupants.add(
          _Occupant(
            _nextOccupantId++,
            s.id,
            i < people.length ? p.$1 : '${p.$1} ${i + 1}',
            p.$2,
            now.subtract(Duration(hours: 2 + i)),
          ),
        );
      }
      s.occupancy = occupants;
    }

    shelter('Town Hall Shelter', 1, 2, 50, 3, 'OPEN');
    shelter('Temple Hall', 2, 1, 4, 4, 'FULL');
    shelter('Kandy Central School', 4, 2, 80, 0, 'CLOSED');
    shelter('Jaffna Community Centre', 3, 1, 30, 12, 'OPEN');

    _teams
      ..add(_Team(1, 'Alpha Team', 1, 'AVAILABLE'))
      ..add(_Team(2, 'Bravo Team', 2, 'AVAILABLE'))
      ..add(_Team(3, 'Charlie Team', 2, 'DISPATCHED'));

    void rescue(
      String name,
      String phone,
      int districtId,
      double? lat,
      double? lng,
      String text,
      String priority,
      String status,
      Duration age, {
      int? teamId,
    }) {
      final submitted = now.subtract(age);
      _rescues.add(
        _Rescue(_nextRescueId++, name, phone, districtId, lat, lng, text, priority, status, submitted)
          ..teamId = teamId
          ..assignedAt = teamId == null ? null : submitted.add(const Duration(minutes: 20))
          ..completedAt = status == 'COMPLETED' ? submitted.add(const Duration(hours: 3)) : null,
      );
    }

    rescue(
      'Nimal Perera',
      '0771234567',
      1,
      6.9271234,
      79.8612345,
      'Family of five stuck on the roof, water rising.',
      'CRITICAL',
      'PENDING',
      const Duration(minutes: 40),
    );
    rescue(
      'Saman Kumara',
      '0712223344',
      2,
      null,
      null,
      'Road cut off by a landslide, two people need to be moved.',
      'MODERATE',
      'ASSIGNED',
      const Duration(hours: 5),
      teamId: 3,
    );
    rescue(
      'Kamala Fernando',
      '0765558899',
      4,
      7.2906,
      80.6337,
      'Elderly neighbour cannot leave the house.',
      'HIGH',
      'PENDING',
      const Duration(hours: 2),
    );
    rescue(
      'Ruwan Silva',
      '0701112233',
      3,
      null,
      null,
      'Boat needed to reach a flooded farm.',
      'LOW',
      'COMPLETED',
      const Duration(days: 1),
      teamId: 1,
    );
  }

  // Shelters

  @override
  Future<ShelterList> listShelters({String? status, int? districtId}) async {
    await _enter();
    final wanted = _known(status, _shelterStatuses);
    return ShelterList(
      shelters: [
        for (final s in _shelters)
          if ((wanted == null || s.status == wanted) && (districtId == null || s.districtId == districtId))
            _shelterOf(s),
      ],
      districts: _districts,
      statuses: _shelterStatuses,
    );
  }

  @override
  Future<ShelterDetail> getShelter(int id) async {
    await _enter();
    final s = _findShelter(id);
    return ShelterDetail(
      shelter: _shelterOf(s),
      occupants: [
        for (final o in _occupants)
          if (o.shelterId == id && o.checkOutTime == null)
            ShelterOccupant(id: o.id, fullName: o.fullName, nic: o.nic, checkInTime: o.checkInTime),
      ],
    );
  }

  @override
  Future<ShelterFormData> getShelterFormData() async {
    await _enter();
    return const ShelterFormData(districts: _districts, organizations: _organizations);
  }

  @override
  Future<ActionResult> createShelter({
    required int districtId,
    required int organizationId,
    required String name,
    required int capacity,
  }) async {
    await _enter();
    _requireShelterFields(name: name, capacity: capacity);
    _district(districtId);
    _organization(organizationId);
    final s = _Shelter(_nextShelterId++, name.trim(), districtId, organizationId, capacity, 'OPEN');
    _shelters.add(s);
    return ActionResult(message: 'Shelter created.', location: '/shelters/${s.id}');
  }

  @override
  Future<ActionResult> updateShelter(
    int id, {
    required int districtId,
    required int organizationId,
    required String name,
    required int capacity,
  }) async {
    await _enter();
    final s = _findShelter(id);
    _requireShelterFields(name: name, capacity: capacity);
    if (capacity < s.occupancy) {
      throw _rule('Capacity cannot be lower than the current occupancy (${s.occupancy}).');
    }
    s
      ..name = name.trim()
      ..capacity = capacity;
    if (s.status != 'CLOSED') {
      s.status = s.occupancy >= capacity ? 'FULL' : 'OPEN';
    }
    return ActionResult(message: 'Shelter updated.', location: '/shelters/$id');
  }

  @override
  Future<ActionResult> closeShelter(int id) async {
    await _enter();
    _findShelter(id).status = 'CLOSED';
    return ActionResult(message: 'Shelter closed.', location: '/shelters/$id');
  }

  @override
  Future<ActionResult> reopenShelter(int id) async {
    await _enter();
    final s = _findShelter(id);
    if (s.status != 'CLOSED') {
      throw _rule('Only a closed shelter can be reopened.');
    }
    s.status = s.occupancy >= s.capacity ? 'FULL' : 'OPEN';
    return ActionResult(message: 'Shelter reopened.', location: '/shelters/$id');
  }

  @override
  Future<ActionResult> checkIn(int shelterId, {required String fullName, required String nic}) async {
    await _enter();
    final s = _findShelter(shelterId);
    if (fullName.trim().isEmpty || nic.trim().isEmpty) {
      throw _rule('Enter both a name and NIC to check in an occupant.');
    }
    if (s.status == 'CLOSED') {
      throw _rule('Cannot check in: shelter is closed.');
    }
    if (s.occupancy >= s.capacity) {
      throw _rule('Cannot check in: shelter is at full capacity.');
    }
    _occupants.add(_Occupant(_nextOccupantId++, shelterId, fullName.trim(), nic.trim(), sriLankaNow(_now())));
    s.occupancy++;
    if (s.occupancy >= s.capacity) {
      s.status = 'FULL';
    }
    return ActionResult(message: 'Occupant checked in.', location: '/shelters/$shelterId');
  }

  @override
  Future<ActionResult> checkOut(int shelterId, int occupantId) async {
    await _enter();
    final s = _findShelter(shelterId);
    final o = _occupants.where((o) => o.id == occupantId).firstOrNull;
    if (o == null) {
      throw _notFound('Occupant not found: $occupantId');
    }
    if (o.shelterId != shelterId) {
      throw _rule('Occupant does not belong to this shelter.');
    }
    if (o.checkOutTime != null) {
      throw _rule('Occupant has already checked out.');
    }
    o.checkOutTime = sriLankaNow(_now());
    s.occupancy = s.occupancy > 0 ? s.occupancy - 1 : 0;
    if (s.status == 'FULL' && s.occupancy < s.capacity) {
      s.status = 'OPEN';
    }
    return ActionResult(message: 'Occupant checked out.', location: '/shelters/$shelterId');
  }

  // Rescue requests

  @override
  Future<RescueRequestList> listRescueRequests({String? status, String? priority, int? districtId}) async {
    await _enter();
    final wantedStatus = _known(status, _rescueStatuses);
    final wantedPriority = _known(priority, _priorities);
    return RescueRequestList(
      requests: [
        for (final r in _rescues)
          if ((wantedStatus == null || r.status == wantedStatus) &&
              (wantedPriority == null || r.priority == wantedPriority) &&
              (districtId == null || r.districtId == districtId))
            _rescueOf(r),
      ],
      districts: _districts,
      statuses: _rescueStatuses,
      priorities: _priorities,
    );
  }

  @override
  Future<RescueRequestDetail> getRescueRequest(int id) async {
    await _enter();
    return RescueRequestDetail(
      request: _rescueOf(_findRescue(id)),
      availableTeams: [
        for (final t in _teams)
          if (t.status == 'AVAILABLE')
            RescueTeam(id: t.id, name: t.name, districtName: _district(t.districtId).name, status: t.status),
      ],
    );
  }

  @override
  Future<RescueRequestFormData> getRescueFormData() async {
    await _enter();
    return const RescueRequestFormData(districts: _districts, priorities: _priorities);
  }

  @override
  Future<ActionResult> submitRescueRequest({
    required int districtId,
    required String requesterName,
    required String requesterPhone,
    double? gpsLat,
    double? gpsLng,
    required String description,
    required String priority,
  }) async {
    await _enter();
    final fields = <String, String>{
      if (requesterName.trim().isEmpty) 'requesterName': "Enter the requester's name",
      if (requesterPhone.trim().isEmpty) 'requesterPhone': 'Enter a contact phone number',
      if (gpsLat != null && (gpsLat < -90 || gpsLat > 90)) 'gpsLat': 'Latitude must be between -90 and 90',
      if (gpsLng != null && (gpsLng < -180 || gpsLng > 180)) 'gpsLng': 'Longitude must be between -180 and 180',
      if (description.trim().isEmpty) 'description': 'Describe the situation',
      if (priority.trim().isEmpty) 'priority': 'Select a priority',
    };
    if (fields.isNotEmpty) {
      throw _validation(fields);
    }
    _district(districtId);
    final wanted = _priorities.where((p) => p == priority.trim().toUpperCase()).firstOrNull;
    if (wanted == null) {
      throw _rule('Invalid priority: $priority');
    }
    final r = _Rescue(
      _nextRescueId++,
      requesterName.trim(),
      requesterPhone.trim(),
      districtId,
      gpsLat,
      gpsLng,
      description.trim(),
      wanted,
      'PENDING',
      sriLankaNow(_now()),
    );
    _rescues.add(r);
    return ActionResult(message: 'Rescue request submitted.', location: '/rescue-requests/${r.id}');
  }

  @override
  Future<ActionResult> assignTeam(int requestId, int teamId) async {
    await _enter();
    final r = _findRescue(requestId);
    if (r.status != 'PENDING') {
      throw _rule('Only pending requests can be assigned.');
    }
    final team = _teams.where((t) => t.id == teamId).firstOrNull;
    if (team == null) {
      throw _notFound('Rescue team not found: $teamId');
    }
    if (team.status != 'AVAILABLE') {
      throw _rule('Team is not available for assignment.');
    }
    team.status = 'DISPATCHED';
    r
      ..teamId = teamId
      ..status = 'ASSIGNED'
      ..assignedAt = sriLankaNow(_now());
    return ActionResult(message: 'Rescue team assigned.', location: '/rescue-requests/$requestId');
  }

  @override
  Future<ActionResult> completeRescueRequest(int requestId) async {
    await _enter();
    final r = _findRescue(requestId);
    if (r.status != 'ASSIGNED') {
      throw _rule('Only assigned requests can be completed.');
    }
    _freeTeam(r);
    r
      ..status = 'COMPLETED'
      ..completedAt = sriLankaNow(_now());
    return ActionResult(message: 'Rescue request completed.', location: '/rescue-requests/$requestId');
  }

  @override
  Future<ActionResult> cancelRescueRequest(int requestId) async {
    await _enter();
    final r = _findRescue(requestId);
    if (r.status != 'PENDING' && r.status != 'ASSIGNED') {
      throw _rule('Only pending or assigned requests can be cancelled.');
    }
    _freeTeam(r);
    r.status = 'CANCELLED';
    return ActionResult(message: 'Rescue request cancelled.', location: '/rescue-requests/$requestId');
  }

  // Helpers

  Future<void> _enter() async {
    if (latency > Duration.zero) {
      await Future<void>.delayed(latency);
    }
    if (failNetwork) {
      throw const ApiException(kind: ApiErrorKind.network, detail: 'Could not reach the server (simulated).');
    }
    final status = failStatus;
    if (status != null) {
      throw ApiException(kind: ApiErrorKind.server, status: status, detail: 'The server failed (simulated).');
    }
    callCount++;
  }

  /// Unknown filter values are ignored, like the controllers do.
  String? _known(String? raw, List<String> values) {
    final value = raw?.trim().toUpperCase();
    return values.contains(value) ? value : null;
  }

  void _requireShelterFields({required String name, required int capacity}) {
    final fields = <String, String>{
      if (name.trim().isEmpty) 'name': 'Enter a name',
      if (capacity <= 0) 'capacity': 'Capacity must be positive',
    };
    if (fields.isNotEmpty) {
      throw _validation(fields);
    }
  }

  void _freeTeam(_Rescue r) {
    final team = _teams.where((t) => t.id == r.teamId).firstOrNull;
    if (team != null) {
      team.status = 'AVAILABLE';
    }
  }

  _Shelter _findShelter(int id) =>
      _shelters.where((s) => s.id == id).firstOrNull ?? (throw _notFound('Shelter not found: $id'));

  _Rescue _findRescue(int id) =>
      _rescues.where((r) => r.id == id).firstOrNull ?? (throw _notFound('Rescue request not found: $id'));

  District _district(int id) =>
      _districts.where((d) => d.id == id).firstOrNull ?? (throw _notFound('District not found: $id'));

  Organization _organization(int id) =>
      _organizations.where((o) => o.id == id).firstOrNull ?? (throw _notFound('Organization not found: $id'));

  Shelter _shelterOf(_Shelter s) => Shelter(
    id: s.id,
    name: s.name,
    districtId: s.districtId,
    districtName: _district(s.districtId).name,
    organizationName: _organization(s.organizationId).name,
    capacity: s.capacity,
    currentOccupancy: s.occupancy,
    status: s.status,
  );

  RescueRequest _rescueOf(_Rescue r) => RescueRequest(
    id: r.id,
    requesterName: r.requesterName,
    requesterPhone: r.requesterPhone,
    districtId: r.districtId,
    districtName: _district(r.districtId).name,
    gpsLat: r.gpsLat,
    gpsLng: r.gpsLng,
    description: r.description,
    priority: r.priority,
    status: r.status,
    assignedTeamName: _teams.where((t) => t.id == r.teamId).firstOrNull?.name,
    submittedAt: r.submittedAt,
    assignedAt: r.assignedAt,
    completedAt: r.completedAt,
  );

  ApiException _notFound(String detail) => ApiException(kind: ApiErrorKind.client, status: 404, detail: detail);

  ApiException _rule(String detail) => ApiException(kind: ApiErrorKind.client, status: 400, detail: detail);

  ApiException _validation(Map<String, String> fields) =>
      ApiException(kind: ApiErrorKind.client, status: 400, detail: 'Validation failed', fieldErrors: fields);
}

class _Shelter {
  _Shelter(this.id, this.name, this.districtId, this.organizationId, this.capacity, this.status);

  final int id;
  String name;
  final int districtId;
  final int organizationId;
  int capacity;
  int occupancy = 0;
  String status;
}

class _Occupant {
  _Occupant(this.id, this.shelterId, this.fullName, this.nic, this.checkInTime);

  final int id;
  final int shelterId;
  final String fullName;
  final String nic;
  final DateTime checkInTime;
  DateTime? checkOutTime;
}

class _Team {
  _Team(this.id, this.name, this.districtId, this.status);

  final int id;
  final String name;
  final int districtId;
  String status;
}

class _Rescue {
  _Rescue(
    this.id,
    this.requesterName,
    this.requesterPhone,
    this.districtId,
    this.gpsLat,
    this.gpsLng,
    this.description,
    this.priority,
    this.status,
    this.submittedAt,
  );

  final int id;
  final String requesterName;
  final String requesterPhone;
  final int districtId;
  final double? gpsLat;
  final double? gpsLng;
  final String description;
  final String priority;
  String status;
  final DateTime submittedAt;
  int? teamId;
  DateTime? assignedAt;
  DateTime? completedAt;
}
