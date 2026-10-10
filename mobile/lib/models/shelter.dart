import 'district.dart';
import 'organization.dart';
import 'sri_lanka_time.dart';

/// A shelter as the officer pages return it. [status] stays a raw string (OPEN, FULL, CLOSED) so unknown values survive.
class Shelter {
  const Shelter({
    required this.id,
    required this.name,
    required this.districtId,
    required this.districtName,
    required this.organizationName,
    required this.capacity,
    required this.currentOccupancy,
    required this.status,
  });

  final int id;
  final String name;
  final int districtId;
  final String districtName;
  final String organizationName;
  final int capacity;
  final int currentOccupancy;
  final String status;

  bool get isClosed => status == 'CLOSED';

  bool get isFull => status == 'FULL';

  int get freePlaces => (capacity - currentOccupancy).clamp(0, capacity);

  /// Occupancy as 0..1 for the progress bar.
  double get fill => capacity <= 0 ? 0 : (currentOccupancy / capacity).clamp(0.0, 1.0);

  factory Shelter.fromJson(Map<String, dynamic> json) => Shelter(
        id: (json['id'] as num).toInt(),
        name: json['name'] as String,
        districtId: (json['districtId'] as num).toInt(),
        districtName: json['districtName'] as String,
        organizationName: (json['organizationName'] as String?) ?? '',
        capacity: (json['capacity'] as num).toInt(),
        currentOccupancy: (json['currentOccupancy'] as num?)?.toInt() ?? 0,
        status: json['status'] as String,
      );
}

/// A person currently checked in at a shelter.
class ShelterOccupant {
  const ShelterOccupant({required this.id, required this.fullName, required this.nic, required this.checkInTime});

  final int id;
  final String fullName;
  final String nic;
  final DateTime? checkInTime;

  factory ShelterOccupant.fromJson(Map<String, dynamic> json) => ShelterOccupant(
        id: (json['id'] as num).toInt(),
        fullName: json['fullName'] as String,
        nic: json['nic'] as String,
        checkInTime: _time(json['checkInTime']),
      );
}

/// `GET /shelters`: the shelters that match the filters, plus the values the filter drop-downs offer.
class ShelterList {
  const ShelterList({required this.shelters, required this.districts, required this.statuses});

  final List<Shelter> shelters;
  final List<District> districts;
  final List<String> statuses;

  factory ShelterList.fromJson(Map<String, dynamic> json) => ShelterList(
        shelters: _list(json['shelters'], Shelter.fromJson),
        districts: _list(json['districts'], District.fromJson),
        statuses: (json['statuses'] as List<dynamic>? ?? const []).map((s) => s as String).toList(),
      );
}

/// `GET /shelters/{id}`: one shelter and the people checked in now.
class ShelterDetail {
  const ShelterDetail({required this.shelter, required this.occupants});

  final Shelter shelter;
  final List<ShelterOccupant> occupants;

  factory ShelterDetail.fromJson(Map<String, dynamic> json) => ShelterDetail(
        shelter: Shelter.fromJson(json['shelter'] as Map<String, dynamic>),
        occupants: _list(json['occupants'], ShelterOccupant.fromJson),
      );
}

/// `GET /shelters/new`: what the create form offers to choose from.
class ShelterFormData {
  const ShelterFormData({required this.districts, required this.organizations});

  final List<District> districts;
  final List<Organization> organizations;

  factory ShelterFormData.fromJson(Map<String, dynamic> json) => ShelterFormData(
        districts: _list(json['districts'], District.fromJson),
        organizations: _list(json['organizations'], Organization.fromJson),
      );
}

List<T> _list<T>(Object? raw, T Function(Map<String, dynamic>) parse) =>
    (raw as List<dynamic>? ?? const []).map((e) => parse(e as Map<String, dynamic>)).toList();

DateTime? _time(Object? raw) => raw is String ? parseContractTime(raw) : null;
