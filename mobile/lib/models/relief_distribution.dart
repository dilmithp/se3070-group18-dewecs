import 'sri_lanka_time.dart';

/// A distribution of one supply to one shelter (a "consignment" on the server), as listed by
/// GET /relief-distributions. Timestamps are Sri Lanka local time, like the rest of the contract.
class ReliefDistribution {
  const ReliefDistribution({
    required this.id,
    required this.resourceName,
    required this.resourceUnit,
    required this.quantity,
    required this.shelterId,
    required this.shelterName,
    required this.status,
    required this.organizationName,
    this.dispatchedAt,
    this.deliveredAt,
  });

  final int id;
  final String resourceName;
  final String resourceUnit;
  final int quantity;
  final int shelterId;
  final String shelterName;
  final String status;
  final String organizationName;
  final DateTime? dispatchedAt;
  final DateTime? deliveredAt;

  factory ReliefDistribution.fromJson(Map<String, dynamic> json) =>
      ReliefDistribution(
        id: (json['id'] as num).toInt(),
        resourceName: json['resourceName'] as String,
        resourceUnit: json['resourceUnit'] as String,
        quantity: (json['quantity'] as num).toInt(),
        shelterId: (json['shelterId'] as num).toInt(),
        shelterName: json['shelterName'] as String,
        status: json['status'] as String,
        organizationName: json['organizationName'] as String? ?? '',
        dispatchedAt: _time(json['dispatchedAt']),
        deliveredAt: _time(json['deliveredAt']),
      );

  Map<String, dynamic> toJson() => {
    'id': id,
    'resourceName': resourceName,
    'resourceUnit': resourceUnit,
    'quantity': quantity,
    'shelterId': shelterId,
    'shelterName': shelterName,
    'status': status,
    'organizationName': organizationName,
    'dispatchedAt': dispatchedAt == null
        ? null
        : formatContractTime(dispatchedAt!),
    'deliveredAt': deliveredAt == null
        ? null
        : formatContractTime(deliveredAt!),
  };

  static DateTime? _time(Object? raw) =>
      raw is String ? parseContractTime(raw) : null;
}

/// The answer to a successful POST /relief-distributions: the server's message and the id of the new distribution,
/// taken from the `location` field (for example /relief-distributions/12).
class DistributionCreated {
  const DistributionCreated({required this.id, required this.message});

  final int id;
  final String message;

  /// Throws a [FormatException] when the location does not end in a numeric id.
  factory DistributionCreated.fromJson(Map<String, dynamic> json) {
    final location = (json['location'] as String).replaceAll(
      RegExp(r'/+$'),
      '',
    );
    return DistributionCreated(
      id: int.parse(location.substring(location.lastIndexOf('/') + 1)),
      message: json['message'] as String? ?? '',
    );
  }
}
