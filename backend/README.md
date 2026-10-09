# DEWECS Backend

Spring Boot backend for the Smart Disaster Early-Warning and Emergency Coordination System
(SE3070, Group 18). Four use cases are built as server-rendered Thymeleaf dashboards. The
mobile ground-reporting app and its REST API are not part of this repository.

Java 21, Spring Boot 3.5.16, Maven, Spring Web, Spring Data JPA, Thymeleaf, Bean Validation,
PostgreSQL (Neon) / H2, JUnit 5, Mockito, MockMvc, JaCoCo. There is no Maven wrapper: use `mvn`.

## Project layout

```
controller/    thin Thymeleaf MVC controllers (7)
service/       one interface per use case; service/impl holds the business rules
repository/    Spring Data JPA interfaces (16)
domain/        JPA entities and enums
dto/ mapper/   *FormRequest / *Response objects; entities never reach the views
exception/     ResourceNotFoundException, one *ValidationException per aggregate, GlobalExceptionHandler
config/        DemoDataLoader (local profile only)
resources/templates/   Thymeleaf pages, fragments/layout.html (nav), error.html
```

## Environment variables

The default, `dev` and production-like runs read the datasource from the environment. There are no defaults,
so a missing variable fails fast.

| Variable | Example |
|---|---|
| `DB_URL` | `jdbc:postgresql://<host>/<database>?sslmode=require` |
| `DB_USERNAME` | `<user>` |
| `DB_PASSWORD` | `<password>` |

PowerShell (current window only):

```powershell
$env:DB_URL = "jdbc:postgresql://<host>/<database>?sslmode=require"
$env:DB_USERNAME = "<user>"
$env:DB_PASSWORD = "<password>"
```

**Production profile (`prod`).** Copy `config/application-prod.properties.example` to
`src/main/resources/application-prod.properties` (git-ignored), fill in the values, rebuild and run:
`java -jar target\dewecs-0.0.1-SNAPSHOT.war --spring.profiles.active=prod`. Warning: Maven packages this file into the
WAR, so a WAR built with it contains the secrets. Never share or upload that WAR; on a host, prefer environment
variables. For S3 the file can also hold `dewecs.photos.s3.access-key` / `secret-key`.

**Photo storage.** By default photos are saved in a local folder. To use S3 set `DEWECS_PHOTOS_STORAGE=s3`,
`DEWECS_PHOTOS_S3_BUCKET`, `DEWECS_PHOTOS_S3_REGION` and the standard `AWS_ACCESS_KEY_ID` /
`AWS_SECRET_ACCESS_KEY`. The `/api/v1/photos/{name}` URLs and rules do not change: the app streams the object, the
bucket stays private (`S3PhotoStorageService`, objects under `ground-reports/`). The IAM user needs only
`s3:PutObject`, `s3:GetObject` and `s3:DeleteObject` on that bucket.

IntelliJ: Run | Edit Configurations | the Spring Boot configuration | Environment variables:
`DB_URL=...;DB_USERNAME=...;DB_PASSWORD=...`. Never commit real values. `.env` is git-ignored and
`.env.example` holds placeholders only.

## Profiles and how to run each

| Profile | Database | Schema | Run |
|---|---|---|---|
| default | PostgreSQL from `DB_*` | `ddl-auto=validate` (fails fast on a mismatch) | `mvn spring-boot:run` |
| `dev` | PostgreSQL from `DB_*` | `ddl-auto=update` (adds missing tables and columns, never drops or alters) | `mvn spring-boot:run "-Dspring-boot.run.profiles=dev"` |
| `local` | in-memory H2 (PostgreSQL mode) | `create-drop`, seeded by `DemoDataLoader` | `mvn spring-boot:run "-Dspring-boot.run.profiles=local"` |
| `test` | in-memory H2 | `create-drop` | used automatically by the tests |

`local` needs no environment variables and is the profile for demos and the viva. Or run the built archive:

```powershell
mvn -B -DskipTests package
java -jar target\dewecs-0.0.1-SNAPSHOT.war --spring.profiles.active=local
```

Then open http://localhost:8080. The click path is in `../doc/DEMO_SCRIPT.md`.

`dev` against the shared Neon database should be run once, and only after agreeing it with the team (the
database is shared). Check the schema first with `../doc/neon-schema-check.sql` (read-only).

## Tests and coverage

```powershell
mvn -B test        # 187 tests, H2 only, no network
mvn -B verify      # also builds the WAR; JaCoCo report in target/site/jacoco/index.html
```

| Kind | Classes |
|---|---|
| Mockito service tests (40) | `service/*ServiceImplTest` |
| `@DataJpaTest` (3 mapping + 6 query tests) | `repository/*RepositoryTest`, `CustomQueryRepositoryTest` |
| MockMvc flow tests (full context, H2, rolled back) | `web/WarningFlowTest`, `ShelterFlowTest`, `RescueFlowTest`, `ReliefFlowTest`, `GroundReportFlowTest`, `DashboardFlowTest`, `PageRenderingTest`, `ErrorHandlingTest`, `UnexpectedErrorTest`, and for the mobile API `ApiFlowTest`, `ApiErrorTest`, `ApiContractShapeTest`, `ApiCorsTest` |

Last measured: 82% of lines and 70% of branches overall (controller.api 89%/62%, service.impl 92%/73%).

## Use cases and routes

| Use case | Routes |
|---|---|
| Warning issuance | `/warnings`, `/warnings/new`, `/warnings/{id}`, `/{id}/edit`, POST `/{id}/publish`, `/{id}/retract` |
| Shelter and rescue | `/shelters` (+ `new`, `{id}`, `edit`, `close`, `reopen`, `check-in`, `check-out/{occupantId}`); `/rescue-requests` (+ `new`, `{id}`, `assign`, `complete`, `cancel`) |
| Relief distribution | `/relief-supplies` (+ `new`, `{id}`, `edit`, `restock`); `/relief-distributions` (+ `new`, `{id}`, `deliver`, `cancel`) |
| Reporting (ground-report review) | `/ground-reports`, `/ground-reports/{id}`, POST `/{id}/review`, `/{id}/action`, `/{id}/dismiss`; dashboard at `/` and `/dashboard` |
| Post-event analysis | `/post-event-reports` (list + generate form), POST `/post-event-reports` (hazardEventId), `/post-event-reports/{id}` |

All 47 routes are in `../doc/DEWECS.postman_collection.json`. Pages are HTML; successful actions redirect with a
flash `message`, rule violations redirect with a flash `error`.

## JSON for the officer pages

Every officer page also answers in JSON on the same URL. The controllers are unchanged: a filter and an interceptor in
`config/` (`JsonBodyFilter`, `JsonResponseInterceptor`, `OfficerJsonConfig`) do the translation. `/api/v1` (the mobile
contract) is not touched.

| You send | You get |
|---|---|
| `GET /shelters` with `Accept: application/json` (or `?format=json`) | the page model as JSON: `{"shelters":[...],"statuses":[...],...}`; form beans and binding results are left out; staff and citizen users in drop-downs are reduced to `id` and `fullName` |
| `POST /shelters` with `Content-Type: application/json` and a flat JSON body (arrays allowed, e.g. `broadcastChannels`) | **201** `{"message","location"}` and a `Location` header for a create; **200** for other actions (`/shelters/1/close` needs no body) |
| invalid fields | **400** `application/problem+json` with `fieldErrors` |
| a business-rule violation (closed shelter, over capacity ...) | **400** problem with the rule text in `detail` |
| unknown id | **404** problem |

Browsers (`Accept` has `text/html`) and plain form posts behave exactly as before. A JSON body is converted into the
request parameters, so the same validation runs. Like the pages, this has no authentication yet (see Known
limitations), so do not expose it beyond a trusted network.

## UI (Thymeleaf pages)

No CDN, web font, icon font or JavaScript framework: one stylesheet (`static/css/style.css`), one 10-line script
(`static/js/app.js`, only the optional confirm dialog) and inline SVG icons. Every page works with JavaScript off.

- **Fragments** in `templates/fragments/layout.html`: `head(title)`, `nav(active)`, `pageHeader(title, subtitle)`,
  `flash`, `badge(value)` (status, severity or priority as a pill with a readable label), `enumLabel(value)` and
  `time(value)` (`yyyy-MM-dd HH:mm`, a dash when empty). Always pass every parameter a fragment declares.
- **Design tokens** are CSS custom properties at the top of `style.css` (colour, spacing, radius, shadow, type scale).
  Text and background pairs were checked for 4.5:1 contrast; status colour is never the only signal.
- **Components:** `.btn` (`btn-primary`, `btn-secondary`, `btn-danger`, `btn-link`), `.card`, `.table-wrap` + `table`,
  `.filter-bar`, `.form-grid` + `.field`, `.detail-grid`, `.stat-tile`, `progress.occupancy`, `.badge`.
- **Add a page:** copy a list, detail or form template. Start with `head('Title')`, `nav('section')`, then a
  `<main id="main" class="container">` that contains `pageHeader` and `flash`, and end with the `app.js` script tag.
  A new status or severity value needs one `.badge-<value>` rule (underscores become dashes). A destructive button gets
  `data-confirm="Are you sure?"`. Form fields keep the `th:field` bindings and add `aria-invalid` and
  `aria-describedby` for their error message (see `shelters/form.html`).
- **Screenshots** of every page at 1280 px and 390 px are in `../doc/screenshots/`.

## Mobile API

The Flutter ground-reporting app talks to a JSON API under `/api/v1` (contract v1, frozen): reference data,
citizen identification by NIC, report submission (idempotent through `capturedAt`), photo upload and download, and a
citizen's report list. Officers review the same reports on `/ground-reports` (photos show there). Run the `local`
profile and point the app at `http://10.0.2.2:8080` (emulator), the laptop's LAN IP, or use `adb reverse`.

| What | Where |
|---|---|
| Contract, examples, limitations | `../doc/MOBILE_API.md` |
| Response samples (shape-tested) | `../doc/api-samples/*.json` |
| Postman collection | `../doc/DEWECS-mobile-api.postman_collection.json` |
| Code | `controller/api`, `dto/api`, `mapper/GroundReportApiMapper`, `CitizenService`, `GroundReportSubmissionService`, `PhotoStorageService` |
| Settings | `dewecs.photos.dir`, multipart limits (6 MB / 7 MB), `dewecs.api.cors.allowed-origin-patterns` |

GPS columns are `numeric(10,7)`; the shared Neon database needs `../doc/neon-gps-precision.sql` (run by hand after
telling the team).

## Design notes (SOLID, with real examples)

- **Single responsibility:** `ShelterController` only routes, `ShelterServiceImpl` decides (capacity, status flips),
  `ShelterMapper` converts, `ShelterRepository` stores.
- **Open/closed and dependency inversion:** controllers depend on `WarningService`, not `WarningServiceImpl`, and every
  dependency is constructor-injected. The Mockito tests build each impl with mocked repositories.
- **Liskov:** `Citizen extends User` (JOINED inheritance) can be used wherever a `User` is expected, for example as the
  reviewer or issuer in a dropdown.
- **Interface segregation:** one small service interface per use case and one repository per entity.
- **Consistency:** `GlobalExceptionHandler` is the single place that turns exceptions into a status code: an HTML error
  page for browsers, RFC 7807 ProblemDetail JSON for other clients.

## Known limitations

- No authentication or roles (26 `TODO: restrict ... once auth lands` comments mark the protected actions).
- No optimistic locking, so two simultaneous check-ins or distributions can oversubscribe.
- Warnings are stored, not broadcast. Expiry is lazy (a warning flips to EXPIRED when a warnings page is read), so the
  dashboard can briefly count an overdue warning as open.
- A distribution to a CLOSED shelter is accepted. One item per distribution.
- No screens to create districts, organizations, users, hazard events, teams or citizens: use the `local` demo data.
- No pagination.
- Post-event report: a snapshot, so each generation creates a new report (nothing is recalculated later). It has four
  metrics: alert timeline (minutes from event start to the first published warning), citizens reached (citizens
  registered in the district, counted only when a warning was published; there is no delivery log, so this is an
  audience size, not confirmed receipt), shelter occupancy (% across the district's shelters at generation time) and
  resources distributed (units in DELIVERED consignments to the district's shelters since the event started). Related
  warnings are the event's ISSUED, UPDATED and EXPIRED warnings. There is no PDF/CSV export.
- The artifact is a WAR but has no `SpringBootServletInitializer`: run it with `java -jar`, not on an external Tomcat.
