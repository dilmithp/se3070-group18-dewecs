# DEWECS mobile API (contract v1)

JSON API for the Flutter ground-reporting app. Citizens identify themselves by NIC, submit hazard reports with
optional photos, and read their own reports. Officers review the same reports on the web pages at `/ground-reports`.
The contract below is frozen: the backend implements it exactly and the Flutter app is built against it.

Machine-readable samples of every response shape are in `doc/api-samples/*.json` (a backend test fails if a real
response drifts from them). A Postman collection is in `doc/DEWECS-mobile-api.postman_collection.json`.

## DEWECS MOBILE API - CONTRACT v1

### General

- Base path `/api/v1`. JSON in and out, UTF-8. No authentication in v1 (known limitation: anyone who knows a NIC can
  report as that citizen).
- Timestamps: ISO-8601 local date-time in Sri Lanka time (UTC+05:30, no daylight saving), no offset suffix, at most
  millisecond precision, for example `2026-10-07T14:03:11.123`. Clients convert from UTC by adding 5 h 30 min.
- Coordinates: JSON numbers, decimal degrees (WGS84). The server rounds to 7 decimals.
- Errors: RFC 7807 problem JSON. Always present: `status`, `title`, `detail`. Bean-validation failures on body fields
  (400) also carry `fieldErrors`, an object of field name -> message. Other 400s (rules checked in the service: a NIC
  that is invalid after normalisation, a photo upload that is not allowed in the report's status, an unsupported, empty
  or over-5-MB image) carry `status`, `title` and `detail` only. 413 appears only if the servlet container's multipart
  limit trips before the 5 MB check. Parse error bodies as JSON whatever media type they declare.
- Report statuses: `PENDING_SYNC`, `PENDING_REVIEW`, `VERIFIED`, `REJECTED`, `NEEDS_INFO`, `ACTIONED`. A new report
  starts as `PENDING_REVIEW`. Clients must tolerate unknown values by showing them as raw text.

### Report object (returned by every report endpoint)

```json
{"id":101,"citizenId":42,"districtId":1,"districtName":"Colombo","category":"FLOOD",
 "description":"River is overflowing near the bridge","gpsLat":6.9271234,"gpsLng":79.8612345,
 "photoUrl":null,"status":"PENDING_REVIEW","actionNote":null,"submittedAt":"2026-10-07T14:03:11.123"}
```

- `photoUrl` is null, or a path starting with `/api/v1/photos/` (prepend the server base URL), or (legacy rows only)
  any other text, which clients treat as "no displayable photo".
- `actionNote` is non-null only when status is `ACTIONED`. No reviewer names, phone numbers or other personal data are
  ever returned.

### Endpoints

1. `GET /api/v1/reference-data`
   200 `{"districts":[{"id":1,"name":"Colombo"}],"categories":["FLOOD","LANDSLIDE","CYCLONE","DROUGHT"]}`
   Districts are sorted by name (case-insensitive); categories are the HazardType values.

2. `POST /api/v1/citizens/identify`
   body `{"nic":"199012345678","fullName":"Nimal Perera","phone":"0771234567","districtId":1}`
   201 when a citizen was created, 200 when the NIC already existed (stored name, phone and district are never
   overwritten).
   response `{"id":42,"fullName":"Nimal Perera","districtId":1,"districtName":"Colombo","created":true}`
   Rules: `nic` is trimmed, upper-cased and must match `^(\d{9}[VX]|\d{12})$` (old 9-digit-plus-letter or new 12-digit
   Sri Lankan NIC); `fullName` is trimmed, 1-120 characters, not blank; `phone` matches `^\+?[0-9][0-9 -]{7,14}$`;
   `districtId` must exist (404 otherwise).

3. `POST /api/v1/ground-reports`
   body `{"citizenId":42,"districtId":1,"category":"FLOOD","description":"River is overflowing near the bridge",
   "gpsLat":6.9271234,"gpsLng":79.8612345,"capturedAt":"2026-10-07T14:03:11.123"}`
   201 + `Location` header `/api/v1/ground-reports/101` + report object. `photoUrl` cannot be set here.
   200 + the stored report, unchanged, when a report with the same `citizenId`, `category` and `capturedAt` already
   exists (idempotent replay, so retries are safe).
   Rules: `citizenId`, `districtId`, `category`, `description`, `gpsLat`, `gpsLng` are required; `capturedAt` is
   optional (default: now, which also means no replay detection); `category` is one of the reference-data categories
   (case-insensitive); `description` is trimmed, 1-2000 characters; `gpsLat` -90..90; `gpsLng` -180..180 (deliberately
   no Sri Lanka bounding box: emulators default to California); `capturedAt` may not be later than the server's now + 5
   minutes; `submittedAt` = `capturedAt` when given. Unknown `citizenId` or `districtId` -> 404.

4. `POST /api/v1/ground-reports/{id}/photo`
   multipart/form-data with one part named `file`: JPEG, PNG or WebP (checked by content, not by the client's
   Content-Type), 1 byte to 5 MB.
   200 + report object with `photoUrl` set. Uploading again replaces the photo. Allowed only while the status is
   `PENDING_SYNC`, `PENDING_REVIEW` or `NEEDS_INFO` (otherwise 400). Unknown id -> 404.

5. `GET /api/v1/ground-reports/{id}`
   200 report object, or 404.

6. `GET /api/v1/citizens/{id}/ground-reports?page=0&size=20`
   200 `{"items":[report objects],"page":0,"size":20,"totalItems":57,"totalPages":3}`, newest first (`submittedAt`
   desc, `id` desc). A page below 0 becomes 0; size is clamped to 1..50 (default 20). Unknown citizen -> 404.

7. `GET /api/v1/photos/{filename}`
   200 image bytes with the matching `image/*` type, `Cache-Control` public with max-age one day,
   `X-Content-Type-Options: nosniff`. Filenames are server-generated (a UUID plus `.jpg`, `.png` or `.webp`); any
   other name -> 404.

### Client rules (for the Flutter app)

- Create `capturedAt` once, when the user taps Submit, and resend the identical value on every retry.
- `POST /ground-reports` answering 200 or 201 means the report is stored. Then upload the photo, if any. A failed photo
  upload never undoes the report.
- Retry later on: network error, timeout, 5xx, 408, 429. Do not retry automatically (needs the user): 400, 404, 405,
  413, 415 and every other status.
- Responses carry no charset: decode the body bytes as UTF-8 (never as Latin-1) and send requests as UTF-8 with
  Content-Type `application/json`.
- Addresses of the dev machine: Android emulator `http://10.0.2.2:8080`; physical phone `http://<laptop LAN IP>:8080`;
  or run `adb reverse tcp:8080 tcp:8080` and use `http://localhost:8080`.

## END OF CONTRACT v1

## Error semantics (as implemented)

Every response under `/api/` is problem JSON (`Content-Type: application/problem+json`), even if the client sends
`Accept: text/html`. The body also contains `type` (`about:blank`) and `instance` (the request path), which clients can
ignore. Example (`doc/api-samples/problem.json`):

```json
{"type":"about:blank","title":"Not Found","status":404,"detail":"Ground report not found: 999","instance":"/api/v1/ground-reports/999"}
```

| Status | When |
|---|---|
| 400 | Body validation (with `fieldErrors`); malformed or missing JSON; a wrong-typed path or query value; a missing `file` part; a service rule (bad NIC after normalisation, unknown category, future `capturedAt`, photo not allowed in this status, unsupported/empty/over-5-MB image) |
| 404 | Unknown citizen, district or report; unknown URL; photo name that is not server-generated or not found |
| 405 | Wrong HTTP method |
| 413 | Upload larger than the container limit (6 MB per file, 7 MB per request); between 5 and 6 MB the service answers 400 first |
| 415 | Wrong content type (JSON body sent to the photo endpoint, non-JSON body to a JSON endpoint) |
| 500 | Unexpected error: generic detail, no stack trace, no internal message |

`fieldErrors` example (`doc/api-samples/problem-with-field-errors.json`): `{"fieldErrors":{"description":"Describe what you see"}}`.
The 400 for a NIC, name length or category carries no `fieldErrors`: the service checks them after trimming.

## Retry guidance

`capturedAt` is the idempotency key together with `citizenId` and `category`. Create it when the user taps Submit and
send the same value on every retry: the server answers 200 with the stored report instead of inserting a duplicate.
Only submissions that carry `capturedAt` are protected. After 200 or 201, upload the photo (repeating the upload is
harmless: it replaces the photo). Retry automatically on network errors, timeouts, 5xx, 408 and 429, with back-off.
Everything else needs the user.

## Worked examples

Start the backend first (below). On Windows PowerShell write JSON to a file and send it with `curl.exe` (the quotes
around `@body.json` matter: an unquoted `@body.json` is PowerShell splatting), or use `Invoke-RestMethod`.

```powershell
$base = "http://localhost:8080"

# 1. reference data
curl.exe -s "$base/api/v1/reference-data"

# 2. identify (201 first time, 200 afterwards)
'{"nic":"199012345678","fullName":"Nimal Perera","phone":"0771234567","districtId":1}' | Set-Content body.json -Encoding utf8
curl.exe -s -i -X POST "$base/api/v1/citizens/identify" -H "Content-Type: application/json" --data-binary "@body.json"

# 3. submit a report (201 + Location; send the same capturedAt again to get 200 and the same id)
'{"citizenId":1,"districtId":1,"category":"FLOOD","description":"River is overflowing near the bridge","gpsLat":6.9271234,"gpsLng":79.8612345,"capturedAt":"2026-10-07T14:03:11.123"}' | Set-Content body.json -Encoding utf8
curl.exe -s -i -X POST "$base/api/v1/ground-reports" -H "Content-Type: application/json" --data-binary "@body.json"

# 4. upload a photo (multipart, part name "file")
curl.exe -s -X POST "$base/api/v1/ground-reports/1/photo" -F "file=@photo.png;type=image/png"

# 5. get one report
curl.exe -s "$base/api/v1/ground-reports/1"

# 6. list a citizen's reports
curl.exe -s "$base/api/v1/citizens/1/ground-reports?page=0&size=20"

# 7. fetch a photo (use the photoUrl from step 4)
curl.exe -s -o photo-out.png "$base/api/v1/photos/<uuid>.png"
```

The same calls with `Invoke-RestMethod`:

```powershell
$citizen = Invoke-RestMethod -Method Post -Uri "$base/api/v1/citizens/identify" -ContentType "application/json" `
  -Body (@{nic="199012345678"; fullName="Nimal Perera"; phone="0771234567"; districtId=1} | ConvertTo-Json)
$report = Invoke-RestMethod -Method Post -Uri "$base/api/v1/ground-reports" -ContentType "application/json" `
  -Body (@{citizenId=$citizen.id; districtId=1; category="FLOOD"; description="River overflowing"; gpsLat=6.9271234; gpsLng=79.8612345} | ConvertTo-Json)
```

## Running the backend for the app

```powershell
cd backend
mvn -B -DskipTests package
java -jar target\dewecs-0.0.1-SNAPSHOT.war --spring.profiles.active=local
```

The `local` profile uses in-memory H2 (no environment variables, the shared Neon database is never touched), seeds
demo districts and citizens, stores photos under the system temp directory and allows CORS from `http://localhost:*`
so a Flutter web build can call the API. Reach it from the app at:

- Android emulator: `http://10.0.2.2:8080`
- Physical phone on the same Wi-Fi: `http://<laptop LAN IP>:8080` (find it with `ipconfig`; allow Java through the
  Windows firewall)
- Or run `adb reverse tcp:8080 tcp:8080` and use `http://localhost:8080`

Officers see the reports (and photos) at http://localhost:8080/ground-reports.

## Known limitations

- No authentication: anyone who knows a NIC can report as that citizen. The NIC is checked for format only, not
  verified against any registry.
- Photos are stored on the local disk of the server (`dewecs.photos.dir`, default `uploads/ground-reports`); they are
  not backed up and are lost if the directory is. Type and size are checked by content, nothing scans for malware.
- No push updates: the app polls `GET /citizens/{id}/ground-reports` to see status changes.
- Replay detection is check-then-insert (no schema change was allowed), so two simultaneous retries can in rare cases
  insert two rows; later retries always return the oldest one. A submission without `capturedAt` is never de-duplicated.
- Coordinates are stored as `numeric(10,7)` only after `doc/neon-gps-precision.sql` has been run on the shared Neon
  database; until then Neon keeps 2 decimals (a POST response still shows 7, a later GET shows 2).
- Citizens created by the app also appear in the staff drop-downs (reviewer and "issued by" lists) of the web pages.
