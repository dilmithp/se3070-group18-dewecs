/// A relief supply as listed by GET /relief-supplies. [quantity] is the stock that is still available: the server
/// reduces it when a distribution is logged. [lowStock] and [outOfStock] are decided by the server, never by the app.
class ReliefSupply {
  const ReliefSupply({
    required this.id,
    required this.name,
    required this.type,
    required this.unit,
    required this.quantity,
    required this.districtId,
    required this.districtName,
    required this.organizationName,
    required this.lowStock,
    required this.outOfStock,
  });

  final int id;
  final String name;
  final String type;
  final String unit;
  final int quantity;
  final int districtId;
  final String districtName;
  final String organizationName;
  final bool lowStock;
  final bool outOfStock;

  factory ReliefSupply.fromJson(Map<String, dynamic> json) => ReliefSupply(
    id: (json['id'] as num).toInt(),
    name: json['name'] as String,
    type: json['type'] as String,
    unit: json['unit'] as String,
    quantity: (json['quantity'] as num).toInt(),
    districtId: (json['districtId'] as num).toInt(),
    districtName: json['districtName'] as String,
    organizationName: json['organizationName'] as String,
    lowStock: json['lowStock'] as bool? ?? false,
    outOfStock: json['outOfStock'] as bool? ?? false,
  );

  Map<String, dynamic> toJson() => {
    'id': id,
    'name': name,
    'type': type,
    'unit': unit,
    'quantity': quantity,
    'districtId': districtId,
    'districtName': districtName,
    'organizationName': organizationName,
    'lowStock': lowStock,
    'outOfStock': outOfStock,
  };

  /// Returns a copy with another remaining stock and the matching flags (used by the Demo mode server).
  ReliefSupply withStock(
    int newQuantity, {
    required bool low,
    required bool out,
  }) => ReliefSupply(
    id: id,
    name: name,
    type: type,
    unit: unit,
    quantity: newQuantity,
    districtId: districtId,
    districtName: districtName,
    organizationName: organizationName,
    lowStock: low,
    outOfStock: out,
  );
}
