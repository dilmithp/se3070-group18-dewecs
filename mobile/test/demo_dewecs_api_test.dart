import 'package:dewecs_mobile/api/api_exception.dart';
import 'package:dewecs_mobile/api/demo_dewecs_api.dart';
import 'package:dewecs_mobile/models/report_submission.dart';
import 'package:dewecs_mobile/models/sri_lanka_time.dart';
import 'package:flutter_test/flutter_test.dart';

const png = [0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3];

DemoDewecsApi newApi() => DemoDewecsApi(latency: Duration.zero);

Future<ApiException> failure(Future<Object?> call) async {
  try {
    await call;
  } on ApiException catch (e) {
    return e;
  }
  throw StateError('expected an ApiException');
}

ReportSubmission submission(int citizenId,
        {String category = 'FLOOD', String? capturedAt, double lat = 6.9271234, String text = 'Water rising'}) =>
    ReportSubmission(
      citizenId: citizenId,
      districtId: 1,
      category: category,
      description: text,
      gpsLat: lat,
      gpsLng: 79.8612345,
      capturedAt: capturedAt ?? sriLankaNowString(),
    );

Future<int> identified(DemoDewecsApi api, {String nic = '199012345678'}) async =>
    (await api.identify(nic: nic, fullName: 'Nimal', phone: '0771234567', districtId: 1)).id;

void main() {
  reliefTests();

  test('reference data has four districts and the four categories', () async {
    final data = await newApi().getReferenceData();

    expect(data.districts.map((d) => d.name), ['Colombo', 'Galle', 'Jaffna', 'Kandy']);
    expect(data.categories, ['FLOOD', 'LANDSLIDE', 'CYCLONE', 'DROUGHT']);
  });

  test('identify creates once, then returns the stored citizen unchanged', () async {
    final api = newApi();

    final first = await api.identify(nic: '901234567v', fullName: 'Nimal', phone: '0771234567', districtId: 1);
    final again = await api.identify(nic: '901234567V', fullName: 'Other', phone: '0779999999', districtId: 2);

    expect(first.created, isTrue);
    expect(again.created, isFalse);
    expect(again.id, first.id);
    expect(again.fullName, 'Nimal');
    expect(again.districtName, 'Colombo');
  });

  test('identify rejects bad input and unknown districts', () async {
    final api = newApi();

    expect((await failure(api.identify(nic: '123', fullName: 'A', phone: '0771234567', districtId: 1))).status, 400);
    expect((await failure(api.identify(nic: '199012345678', fullName: ' ', phone: '0771234567', districtId: 1))).status, 400);
    expect((await failure(api.identify(nic: '199012345678', fullName: 'A', phone: 'abc', districtId: 1))).status, 400);
    expect((await failure(api.identify(nic: '199012345678', fullName: 'A', phone: '0771234567', districtId: 99))).status, 404);
  });

  test('the first citizen to identify owns the seeded demo reports, including an ACTIONED one with a note', () async {
    final api = newApi();
    final id = await identified(api);

    final page = await api.listCitizenReports(id);

    expect(page.totalItems, 4);
    expect(page.items.map((r) => r.status).toSet(), {'PENDING_REVIEW', 'VERIFIED', 'ACTIONED', 'NEEDS_INFO'});
    expect(page.items.firstWhere((r) => r.status == 'ACTIONED').actionNote, isNotNull);
    final other = await identified(api, nic: '199512345678');
    expect((await api.listCitizenReports(other)).totalItems, 0);
  });

  test('submit stores a PENDING_REVIEW report and a replay returns the same one with created false', () async {
    final api = newApi();
    final id = await identified(api);
    final s = submission(id, capturedAt: '2026-10-07T10:00:00.123');

    final first = await api.submitReport(s);
    final replay = await api.submitReport(s);

    expect(first.created, isTrue);
    expect(first.report.status, 'PENDING_REVIEW');
    expect(first.report.submittedAt, parseContractTime('2026-10-07T10:00:00.123'));
    expect(replay.created, isFalse);
    expect(replay.report.id, first.report.id);
    expect(api.storedReports.where((r) => r.id == first.report.id), hasLength(1));
  });

  test('same time but another category is a different report', () async {
    final api = newApi();
    final id = await identified(api);

    final flood = await api.submitReport(submission(id, capturedAt: '2026-10-07T10:00:00.000'));
    final slide = await api.submitReport(submission(id, category: 'landslide', capturedAt: '2026-10-07T10:00:00.000'));

    expect(slide.created, isTrue);
    expect(slide.report.category, 'LANDSLIDE');
    expect(slide.report.id, isNot(flood.report.id));
  });

  test('submit validates like the server: unknown citizen 404, bad category and future time 400', () async {
    final api = newApi();
    final id = await identified(api);

    expect((await failure(api.submitReport(submission(9999)))).status, 404);
    expect((await failure(api.submitReport(submission(id, category: 'EARTHQUAKE')))).status, 400);
    expect((await failure(api.submitReport(submission(id, capturedAt: '2999-01-01T00:00:00.000')))).status, 400);
    final badLat = await failure(api.submitReport(submission(id, lat: 91)));
    expect(badLat.status, 400);
    expect(badLat.fieldErrors, contains('gpsLat'));
  });

  test('photo upload sets a photo URL, replaces it, and is refused after review or for non-images', () async {
    final api = newApi();
    final id = await identified(api);
    final created = (await api.submitReport(submission(id))).report;

    final first = await api.uploadPhoto(created.id, png);
    final second = await api.uploadPhoto(created.id, png);

    expect(first.photoUrl, startsWith('/api/v1/photos/'));
    expect(second.photoUrl, isNot(first.photoUrl));
    expect((await failure(api.uploadPhoto(created.id, [1, 2, 3]))).status, 400);
    expect((await failure(api.uploadPhoto(created.id, []))).status, 400);
    final verified = (await api.listCitizenReports(id)).items.firstWhere((r) => r.status == 'VERIFIED');
    expect((await failure(api.uploadPhoto(verified.id, png))).status, 400);
    expect((await failure(api.uploadPhoto(424242, png))).status, 404);
  });

  test('list is newest first and pages', () async {
    final api = newApi();
    final id = await identified(api);
    final recent = (await api.submitReport(submission(id, capturedAt: sriLankaNowString()))).report;

    final first = await api.listCitizenReports(id, page: 0, size: 2);
    final second = await api.listCitizenReports(id, page: 1, size: 2);

    expect(first.items.first.id, recent.id);
    expect(first.items, hasLength(2));
    expect(first.totalItems, 5);
    expect(first.totalPages, 3);
    expect(first.hasMore, isTrue);
    expect(second.items, hasLength(2));
    expect((await api.listCitizenReports(id, page: -4, size: 1000)).size, 50);
  });

  test('the network switch makes every call fail like a dead connection', () async {
    final api = newApi()..failNetwork = true;

    final error = await failure(api.getReferenceData());

    expect(error.kind, ApiErrorKind.network);
    expect(error.retryable, isTrue);
    expect(api.callCount, 0);
  });

  test('the 500 switch makes every call fail with a retryable server error', () async {
    final api = newApi()..failStatus = 500;

    final error = await failure(api.getReferenceData());

    expect(error.kind, ApiErrorKind.server);
    expect(error.status, 500);
    expect(error.retryable, isTrue);
  });

  test('a server reset makes the old citizen unknown (404)', () async {
    final api = newApi();
    final id = await identified(api);

    api.resetServer();

    expect((await failure(api.listCitizenReports(id))).status, 404);
    expect((await failure(api.submitReport(submission(id)))).status, 404);
  });
}

void reliefTests() {
  group('relief (UC-04)', () {
    test('seed data mirrors the backend demo data, with stock flags decided by the server side', () async {
      final api = newApi();

      final supplies = await api.listSupplies();
      final medkits = supplies.firstWhere((s) => s.name == 'First-aid kits');

      expect(supplies.map((s) => s.type), ['FOOD', 'WATER', 'MEDICAL_SUPPLIES', 'SHELTER_MATERIALS']);
      expect(medkits.quantity, 6);
      expect(medkits.lowStock, isTrue);
      expect(supplies.where((s) => s.lowStock).length, 1);
      expect((await api.listShelters()).map((s) => s.name),
          ['Galle Central College', 'Kandy Town Hall', 'Colombo Sports Complex']);
    });

    test('supplies can be filtered by type, district and low stock', () async {
      final api = newApi();

      expect((await api.listSupplies(type: 'WATER')).single.name, 'Bottled water');
      expect((await api.listSupplies(districtId: 4)).map((s) => s.id), [3, 4]);
      expect((await api.listSupplies(lowStockOnly: true)).single.id, 3);
      expect(await api.listSupplies(type: 'FUEL'), isEmpty);
    });

    test('a supply and a distribution are found by id and unknown ids are 404', () async {
      final api = newApi();

      expect((await api.getSupply(2)).organizationName, 'Lanka Relief Foundation');
      expect((await api.getDistribution(2)).status, 'DELIVERED');
      expect((await failure(api.getSupply(99))).status, 404);
      expect((await failure(api.getDistribution(99))).status, 404);
    });

    test('distributions can be filtered by status, shelter and supply', () async {
      final api = newApi();

      expect((await api.listDistributions()).length, 3);
      expect((await api.listDistributions(status: 'DISPATCHED')).single.resourceName, 'Rice (50 kg bags)');
      expect((await api.listDistributions(shelterId: 3)).single.status, 'DELIVERED');
      expect((await api.listDistributions(supplyId: 3)).single.status, 'CANCELLED');
      expect(await api.listDistributions(status: 'IN_TRANSIT'), isEmpty);
    });

    test('logging a distribution reduces the stock and dispatches it', () async {
      final api = newApi();

      final created = await api.createDistribution(supplyId: 1, shelterId: 3, quantity: 40);
      final distribution = await api.getDistribution(created.id);

      expect((await api.getSupply(1)).quantity, 80);
      expect(distribution.status, 'DISPATCHED');
      expect(distribution.quantity, 40);
      expect(distribution.shelterName, 'Colombo Sports Complex');
      expect(distribution.dispatchedAt, isNotNull);
      expect(distribution.deliveredAt, isNull);
      expect((await api.listDistributions(supplyId: 1)).map((d) => d.id), contains(created.id));
    });

    test('taking the last units flags the supply out of stock; a small remainder flags low stock', () async {
      final api = newApi();

      await api.createDistribution(supplyId: 3, shelterId: 1, quantity: 6);
      await api.createDistribution(supplyId: 4, shelterId: 1, quantity: 40);

      final medkits = await api.getSupply(3);
      final tarps = await api.getSupply(4);
      expect(medkits.outOfStock, isTrue);
      expect(medkits.quantity, 0);
      expect(tarps.quantity, 5);
      expect(tarps.lowStock, isTrue);
      expect(tarps.outOfStock, isFalse);
    });

    test('rules: over-stock, zero quantity and unknown ids are refused and leave the stock unchanged', () async {
      final api = newApi();

      final over = await failure(api.createDistribution(supplyId: 1, shelterId: 3, quantity: 121));
      final zero = await failure(api.createDistribution(supplyId: 1, shelterId: 3, quantity: 0));
      final noSupply = await failure(api.createDistribution(supplyId: 99, shelterId: 3, quantity: 1));
      final noShelter = await failure(api.createDistribution(supplyId: 1, shelterId: 99, quantity: 1));

      expect(over.status, 400);
      expect(over.detail, 'Requested quantity exceeds available stock (120 bags available).');
      expect(zero.fieldErrors['quantity'], isNotNull);
      expect(noSupply.status, 404);
      expect(noShelter.status, 404);
      expect((await api.getSupply(1)).quantity, 120);
      expect((await api.listDistributions()).length, 3);
    });

    test('the failure switches apply to relief calls and the server reset restores the seed', () async {
      final api = newApi();
      await api.createDistribution(supplyId: 1, shelterId: 3, quantity: 40);

      api.failNetwork = true;
      expect((await failure(api.listSupplies())).kind, ApiErrorKind.network);
      api.failNetwork = false;
      api.failStatus = 500;
      expect((await failure(api.createDistribution(supplyId: 1, shelterId: 3, quantity: 1))).status, 500);
      api.failStatus = null;
      api.resetServer();

      expect((await api.getSupply(1)).quantity, 120);
      expect((await api.listDistributions()).length, 3);
    });
  });
}
