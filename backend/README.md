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
mvn -B test        # 152 tests, H2 only, no network
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

All 44 routes are in `../doc/DEWECS.postman_collection.json`. Pages are HTML; successful actions redirect with a
flash `message`, rule violations redirect with a flash `error`.

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
- No pagination. `PostEventReport` and `ReportMetric` belong to the shared domain model and have no service or page.
- The artifact is a WAR but has no `SpringBootServletInitializer`: run it with `java -jar`, not on an external Tomcat.
