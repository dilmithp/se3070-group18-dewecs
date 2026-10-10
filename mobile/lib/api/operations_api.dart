import '../models/action_result.dart';
import '../models/rescue_request.dart';
import '../models/shelter.dart';

/// The shelter and rescue pages of the officer dashboard, used as a JSON API (`Accept: application/json`).
/// One method per page or action. Every failure is an ApiException: a rule the server refuses (closed shelter,
/// no free place ...) is a 400 whose `detail` is the text to show.
abstract class OperationsApi {
  // Shelters

  Future<ShelterList> listShelters({String? status, int? districtId});

  Future<ShelterDetail> getShelter(int id);

  Future<ShelterFormData> getShelterFormData();

  Future<ActionResult> createShelter({
    required int districtId,
    required int organizationId,
    required String name,
    required int capacity,
  });

  /// The server validates the whole form again, so the district and organization are sent with the new values.
  Future<ActionResult> updateShelter(
    int id, {
    required int districtId,
    required int organizationId,
    required String name,
    required int capacity,
  });

  Future<ActionResult> closeShelter(int id);

  Future<ActionResult> reopenShelter(int id);

  Future<ActionResult> checkIn(int shelterId, {required String fullName, required String nic});

  Future<ActionResult> checkOut(int shelterId, int occupantId);

  // Rescue requests

  Future<RescueRequestList> listRescueRequests({String? status, String? priority, int? districtId});

  Future<RescueRequestDetail> getRescueRequest(int id);

  Future<RescueRequestFormData> getRescueFormData();

  Future<ActionResult> submitRescueRequest({
    required int districtId,
    required String requesterName,
    required String requesterPhone,
    double? gpsLat,
    double? gpsLng,
    required String description,
    required String priority,
  });

  Future<ActionResult> assignTeam(int requestId, int teamId);

  Future<ActionResult> completeRescueRequest(int requestId);

  Future<ActionResult> cancelRescueRequest(int requestId);
}
