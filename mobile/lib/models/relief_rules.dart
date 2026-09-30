/// Relief vocabulary of the officer routes, copied from the backend enums (ResourceType, ConsignmentStatus).
/// Values are kept as strings, like the ground-report categories, so a value added on the server never breaks parsing.
class ResourceTypes {
  ResourceTypes._();

  static const food = 'FOOD';
  static const water = 'WATER';
  static const medicalSupplies = 'MEDICAL_SUPPLIES';
  static const shelterMaterials = 'SHELTER_MATERIALS';
  static const equipment = 'EQUIPMENT';
  static const fuel = 'FUEL';
  static const other = 'OTHER';

  static const all = [
    food,
    water,
    medicalSupplies,
    shelterMaterials,
    equipment,
    fuel,
    other,
  ];
}

/// Distribution (consignment) statuses. The backend creates a distribution as DISPATCHED; only a DISPATCHED one can be
/// delivered or cancelled. PREPARING and IN_TRANSIT exist on the server but are never set by it today.
class DistributionStatuses {
  DistributionStatuses._();

  static const preparing = 'PREPARING';
  static const dispatched = 'DISPATCHED';
  static const inTransit = 'IN_TRANSIT';
  static const delivered = 'DELIVERED';
  static const cancelled = 'CANCELLED';

  static const all = [preparing, dispatched, inTransit, delivered, cancelled];
}
