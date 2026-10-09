# DEWECS — Progress Notes

SE3070 Group 18 · Smart Disaster Early-Warning and Emergency Coordination System (Sri Lanka)
Web-dashboard scope · Spring Boot 3.5.16 · Java 21 · Thymeleaf · PostgreSQL (Neon) / H2 (tests)

> **What this file is.** The original Claude Code chat was lost, so this note was rebuilt on **6 Oct 2026** from the Git history and the source code in this folder (commit `3f8e285`, the same as `origin/main`; the working tree matched it apart from line endings in the Postman JSON). Everything below was read from the repo. Anything that could not be checked is marked **(unverified)**. Phase numbers come from the old chat and only survive in two code comments, so the phase-to-feature mapping is partly inferred.
>
> **Deadline:** Assignment 02 is due **Fri 9 Oct 2026, 11:59 PM**.

---

## 1. Status at a glance

Done, committed and pushed:

- [x] Domain model (16 entities, 13 enums) and 16 Spring Data repositories
- [x] Warning issuance: draft, edit, publish, retract, automatic expiry
- [x] Shelter management: create/edit/close/reopen, occupant check-in and check-out
- [x] Rescue requests: submit, assign team, complete, cancel
- [x] Relief supplies (stock, restock, low-stock) and relief distributions (stock deducted and restored)
- [x] Ground-report review workflow (reviewed, actioned with note, dismissed): this is the "Reporting" use case
- [x] Dashboard with five live counts
- [x] 19 Thymeleaf pages + CSS, 44 routes
- [x] 83 tests, all green (7 Oct, Phase 6): 43 original + MockMvc flow tests, error-page tests, query tests
- [x] Phase 6: environment-variable datasource, `local` H2 demo profile with seed data, `@Transactional` services, HTML error page, `doc/DEMO_SCRIPT.md`
- [x] Postman collection with 44 requests

Still open (details in the sections below):

- [ ] **The old Neon password is still in git history** (§9). Rotate it in the Neon console and tell the team (the code no longer contains it).
- [ ] Shared Neon schema may not match the Phase 2-5 entities (§8). Run `doc/neon-schema-check.sql` by hand.
- [ ] No end-to-end run against the real database is recorded anywhere (deliberately never done by Claude).
- [ ] No authentication, no optimistic locking (§11).
- [ ] Proposals awaiting a decision: reject distributions to CLOSED shelters; exclude overdue warnings from the dashboard's open count (§11).

---

## 2. Project context

- **System:** DEWECS, built against Group 20's corrected design. Five use cases overall; four are server-rendered Thymeleaf dashboards in this repo's backend, the fifth (ground hazard reporting) is the Flutter mobile app owned by a teammate.
- **This repo's scope** (`backend/`), four web use cases, all built: Warning Issuance, Shelter & Rescue, Relief Distribution, and **Reporting = ground-report review** (Phase 5) plus a dashboard. This mapping was confirmed by the project owner on 6 Oct 2026. **Update 8 Oct 2026:** the owner then asked for the fifth function, Generate Post-Event Analysis Report. It is built on the existing `PostEventReport` and `ReportMetric` entities (unchanged, no schema change): `PostEventReportService`, `PostEventReportController` (`/post-event-reports`, 3 routes), two pages, 10 new tests (164 total). Metric definitions are in `backend/README.md` (Known limitations). The Neon tables `post_event_reports`, `post_event_report_warnings`, `post_event_report_shelters` and `report_metrics` must already exist there (check with a human, `doc/neon-schema-check.sql` does not list them).
- **Shared database:** all four group members use one Neon (serverless Postgres) database. That is why existing enum values were reused rather than renamed (see the comments in `WarningStatus` and `GroundReportStatus`).
- **Repo layout:** `backend/` (Maven project), `doc/DEWECS.postman_collection.json`, `.env.example`, `.gitignore`. Remote: `origin` on GitHub (visibility **unverified**).

### Vocabulary used in the UI vs. stored values

| UI / service wording | Stored as |
|---|---|
| Relief supply | `Resource` entity, table `resources` |
| Relief distribution | `ReliefConsignment` + `ConsignmentItem` |
| Warning published | `WarningStatus.ISSUED` |
| Warning retracted | `WarningStatus.CANCELLED` |
| Ground report reviewed | `GroundReportStatus.VERIFIED` |
| Ground report dismissed | `GroundReportStatus.REJECTED` |

---

## 3. Timeline (from Git)

| Commit | When (+0530) | Message | Contents |
|---|---|---|---|
| `615f7e6` | Fri 25 Sep 2026, 16:05 | Scaffold Spring Boot backend: domain model, repositories, DB profiles, base tests | "Phase 1". 60 files, +1,819. `pom.xml`, `DewecsApplication`, 26 domain classes, 15 bare repository interfaces (no custom queries), `GlobalExceptionHandler` + `ResourceNotFoundException`, YAML config (datasource from env vars, `local-init` profile), 3 `@DataJpaTest` tests, README. |
| `9c802d3` | Sun 27 Sep 2026, 14:02 | whole individual scope done. | "Phases 2–5". 103 files, +5,978 / −11. 7 controllers, 7 service interfaces + 7 impls, 15 DTOs, 6 mappers, 5 validation exceptions, `RescueRequest` + `RescueRequestStatus` + `ShelterStatus`, 19 Thymeleaf pages + CSS, 7 service test classes (40 tests), `.env.example`, `.gitignore`, entity changes (§8). |
| `3f8e285` | Tue 6 Oct 2026, 20:34 | property update and postman collection | YAML replaced by `.properties`; profile `local-init` renamed `dev` and **now always active**; datasource with credentials hard-coded; `<packaging>war</packaging>` added; Postman collection (44 requests). |

Phase mapping (inferred): Phase 2 = Warning issuance, Phase 3 = Shelter & Rescue, Phase 4 = Relief distribution (a code comment says so), Phase 5 = ground-report review, i.e. the "Reporting" use case (a code comment says so). The dashboard page most likely came with Phase 5.

---

## 4. What each area does (business rules as coded)

### 4.1 Warning issuance — `/warnings`

- Lifecycle: `DRAFT → ISSUED → CANCELLED` (retract) or `EXPIRED` (automatic). `UPDATED` exists in the enum but is never set.
- **Create draft:** needs a hazard event and the issuing officer (chosen from a dropdown, there is no login). Severity LOW / MODERATE / HIGH / CRITICAL, message (up to 2,000 chars), optional expiry time, broadcast channels (SMS, EMAIL, RADIO, TV, SIREN, APP_PUSH, SOCIAL_MEDIA, stored in `warning_broadcast_channels`). Nothing is actually sent. `issuedAt` stays empty until publish.
- **Edit:** drafts only.
- **Publish:** drafts only. Blocked if the hazard event has no district ("affected region") or the message is blank. Sets `ISSUED` and stamps `issuedAt`.
- **Retract:** `DRAFT` or `ISSUED` → `CANCELLED`; anything else is rejected.
- **Expiry is lazy:** every list or detail call first flips `ISSUED` warnings whose `expiresAt` has passed to `EXPIRED`. There is no scheduler.
- List filters: status, district (through the hazard event's district).

### 4.2 Shelters — `/shelters`

- Status `OPEN` / `FULL` / `CLOSED`. Create: capacity > 0, starts `OPEN` with occupancy 0.
- **Update:** name and capacity. Capacity must be > 0 and not below the current occupancy. Re-evaluates `OPEN`/`FULL` unless the shelter is `CLOSED`.
- **Close:** any state → `CLOSED` (current occupants are not checked out). **Reopen:** only from `CLOSED`, goes to `OPEN` or `FULL` by occupancy.
- **Check-in** (full name + NIC): rejected if closed or at capacity. Creates a `ShelterOccupant` with a check-in time, occupancy +1, flips to `FULL` when capacity is reached.
- **Check-out:** the occupant must belong to this shelter and not already be checked out. Sets check-out time, occupancy −1 (never below 0), `FULL → OPEN` when space frees up.
- Detail page lists current occupants (those without a check-out time).

### 4.3 Rescue requests — `/rescue-requests`

- Lifecycle: `PENDING → ASSIGNED → COMPLETED`; `PENDING` or `ASSIGNED → CANCELLED`.
- **Submit** (staff enter it on a form): district, requester name and phone, optional GPS, description, priority (LOW…CRITICAL).
- **Assign:** `PENDING` only; the team must be `AVAILABLE`. The team becomes `DISPATCHED`; the request stores the team and `assignedAt`. The dropdown lists only available teams.
- **Complete:** `ASSIGNED` only. Frees the team (`AVAILABLE`) and sets `completedAt`.
- **Cancel:** `PENDING` or `ASSIGNED`; frees the team if one was assigned.
- Filters: status, priority, district. Team statuses `FULL` and `NEEDS_SUPPORT` are never set by this workflow.

### 4.4 Relief supplies and distributions

**Supplies — `/relief-supplies`** (`Resource`): name, category (FOOD, WATER, MEDICAL_SUPPLIES, SHELTER_MATERIALS, EQUIPMENT, FUEL, OTHER), unit, quantity, district, owning organization.
- Create with quantity ≥ 0. Edit changes name, category and unit only. **Restock** adds a positive quantity.
- Low stock means quantity **< 10** (`Resource.LOW_STOCK_THRESHOLD`). `lowStockOnly=true` on the list ignores the other filters.

**Distributions — `/relief-distributions`** (`ReliefConsignment` with one `ConsignmentItem`):
- **Create:** one supply + destination shelter + quantity (> 0 and ≤ available stock). Stock is deducted immediately and the distribution is created as `DISPATCHED` (`dispatchedAt` = now, organization taken from the supply).
- **Deliver:** `DISPATCHED` only → `DELIVERED`, sets `deliveredAt`.
- **Cancel:** `DISPATCHED` only → `CANCELLED`, and the quantity goes back into stock.
- `PREPARING` and `IN_TRANSIT` exist in the enum but are never used. The shelter's status is not checked, so a closed or full shelter can still be chosen.

### 4.5 Ground-report review (web side, Phase 5, the "Reporting" use case) — `/ground-reports`

- Reports are created by the mobile app (reporter is a `Citizen`). **This repo has no endpoint that creates one.**
- Statuses: `PENDING_SYNC`, `PENDING_REVIEW`, `VERIFIED`, `REJECTED`, `NEEDS_INFO`, `ACTIONED`. `PENDING_SYNC` and `NEEDS_INFO` are never set by this workflow.
- **Mark reviewed:** `PENDING_REVIEW → VERIFIED`, records the reviewing user.
- **Action:** `VERIFIED` only, a non-blank note is required → `ACTIONED`, note saved in `actionNote`.
- **Dismiss:** any state except `ACTIONED` / `REJECTED` → `REJECTED`.
- The reviewer is picked from a dropdown (no login). Filters: status, district, category (FLOOD, LANDSLIDE, CYCLONE, DROUGHT).

### 4.6 Dashboard — `/` and `/dashboard`

Five counts: open (`ISSUED`) warnings, `FULL` shelters, `PENDING` rescue requests, low-stock supplies (< 10), unreviewed (`PENDING_REVIEW`) ground reports. It does not trigger warning expiry, so the open-warnings number can include overdue warnings until a warnings page is opened.

---

## 5. Architecture and conventions

- **Layers:** `controller` (thin, Thymeleaf) → `service` interface → `service/impl` → `repository`. `dto` + `mapper` keep entities out of the views. Constructor injection everywhere, no field injection.
- **Forms:** bound to `*FormRequest` DTOs with Bean Validation messages; views read `*Response` DTOs.
- **Post/Redirect/Get:** successful actions redirect with a flash `message`; rule violations redirect with a flash `error` (action buttons) or are added to the `BindingResult` and the form is re-shown (create forms).
- **Exceptions:** `ResourceNotFoundException` plus one validation exception per aggregate (`Warning…`, `Shelter…`, `RescueRequest…`, `Relief…`, `GroundReport…`). `GlobalExceptionHandler` (`@RestControllerAdvice`) maps them to RFC 7807 `ProblemDetail` JSON: not found → 404, validation → 400, `MethodArgumentNotValidException` → 400 with `fieldErrors`, anything else → 500 "Unexpected error occurred".
- **Filters** are parsed leniently: an unknown enum value in a query string means "no filter".
- **JPA choices:** `Citizen extends User` with `JOINED` inheritance; `open-in-view=false`, so small owned collections (`Warning.broadcastChannels`, `ReliefConsignment.items`) are fetched eagerly; `Severity` is shared by hazard events, warnings and rescue priorities.
- **UI (Phase 9):** pages share the fragments in `templates/fragments/layout.html` (`head`, `nav`, `pageHeader`, `flash`, `badge`, `enumLabel`, `time`); `static/css/style.css` is built on design tokens; `static/js/app.js` only adds confirm dialogs (`data-confirm`). Enums are shown as readable badges, timestamps as `yyyy-MM-dd HH:mm`, empty values as a dash. See `backend/README.md`, section "UI".
- **Package contents (main):** controller 7, domain 29, dto 15, exception 7, mapper 6, repository 16, service 14 (7 interfaces + 7 impls), plus `DewecsApplication`. `config/` is empty.

---

## 6. Routes (44 handlers; `/` is an alias of `/dashboard`) — matches the Postman collection

| Area | Method and path | Purpose |
|---|---|---|
| Dashboard | `GET /`, `GET /dashboard` | Five counts |
| Warnings | `GET /warnings?status&districtId` | List |
| | `GET /warnings/new` · `POST /warnings` | Create draft |
| | `GET /warnings/{id}` | Detail |
| | `GET /warnings/{id}/edit` · `POST /warnings/{id}` | Edit draft |
| | `POST /warnings/{id}/publish` | Draft → issued |
| | `POST /warnings/{id}/retract` | Draft or issued → cancelled |
| Shelters | `GET /shelters?status&districtId` | List |
| | `GET /shelters/new` · `POST /shelters` | Create |
| | `GET /shelters/{id}` | Detail + current occupants |
| | `GET /shelters/{id}/edit` · `POST /shelters/{id}` | Update name/capacity |
| | `POST /shelters/{id}/close` · `/reopen` | Close / reopen |
| | `POST /shelters/{id}/check-in` | Occupant in (`fullName`, `nic`) |
| | `POST /shelters/{id}/check-out/{occupantId}` | Occupant out |
| Rescue | `GET /rescue-requests?status&priority&districtId` | List |
| | `GET /rescue-requests/new` · `POST /rescue-requests` | Submit |
| | `GET /rescue-requests/{id}` | Detail |
| | `POST /rescue-requests/{id}/assign` (`teamId`) · `/complete` · `/cancel` | Workflow |
| Supplies | `GET /relief-supplies?type&districtId&lowStockOnly` | List |
| | `GET /relief-supplies/new` · `POST /relief-supplies` | Create |
| | `GET /relief-supplies/{id}` | Detail |
| | `GET /relief-supplies/{id}/edit` · `POST /relief-supplies/{id}` | Update |
| | `POST /relief-supplies/{id}/restock` | Add stock |
| Distributions | `GET /relief-distributions?status&shelterId&resourceId` | List |
| | `GET /relief-distributions/new` · `POST /relief-distributions` | Create (`resourceId`, `shelterId`, `quantity`) |
| | `GET /relief-distributions/{id}` | Detail |
| | `POST /relief-distributions/{id}/deliver` · `/cancel` | Workflow |
| Ground reports | `GET /ground-reports?status&districtId&category` | List |
| | `GET /ground-reports/{id}` | Detail |
| | `POST /ground-reports/{id}/review` · `/action` (`note`) · `/dismiss` | Review workflow (all take `reviewingUserId`) |

Postman variables: `baseUrl=http://localhost:8080`, `id=1`, `occupantId=1`. The collection holds no secrets.

Dropdowns are filled from existing rows: hazard events, users, districts, organizations, rescue teams (available only), supplies, shelters. **No screen in this repo creates those**, so they must already exist in the database.

---

### Mobile API (Phase 7, contract v1, JSON, `/api/v1`)

| Method and path | Purpose |
|---|---|
| `GET /api/v1/reference-data` | Districts (sorted, case-insensitive) and categories |
| `POST /api/v1/citizens/identify` | Find or create a citizen by NIC (201 / 200) |
| `POST /api/v1/ground-reports` | Submit a report (201 + Location; 200 on replay with the same `capturedAt`) |
| `POST /api/v1/ground-reports/{id}/photo` | Upload or replace the photo (multipart part `file`) |
| `GET /api/v1/ground-reports/{id}` | One report |
| `GET /api/v1/citizens/{id}/ground-reports?page&size` | A citizen's reports, newest first |
| `GET /api/v1/photos/{filename}` | Photo bytes |

Full contract: `doc/MOBILE_API.md`; samples: `doc/api-samples/*.json`; Postman: `doc/DEWECS-mobile-api.postman_collection.json` (the web collection is unchanged and still lists the 44 web routes).

---

## 7. Domain model quick reference

| Entity (table) | Key fields |
|---|---|
| `User` (`users`), `Citizen` (`citizens`) | fullName, phone, address, district; Citizen adds unique `nic` (JOINED inheritance) |
| `District` (`districts`), `Organization` (`organizations`) | name; organization type (GOVERNMENT, ARMED_FORCES, NGO, PRIVATE_DONOR) |
| `HazardEvent` (`hazard_events`) | hazardType, severityLevel, status (ACTIVE, CONTAINED, RESOLVED), district, occurredAt |
| `Warning` (`warnings`, `warning_broadcast_channels`) | hazardEvent, severity, status, channels, message, issuedAt (null for drafts), expiresAt, issuedBy |
| `Shelter` (`shelters`), `ShelterOccupant` (`shelter_occupants`) | name, district, organization, capacity, currentOccupancy, status; occupant fullName, nic, check-in/out times |
| `RescueTeam` (`rescue_teams`), `RescueRequest` (`rescue_requests`) | team status AVAILABLE / DISPATCHED / FULL / NEEDS_SUPPORT; request requester, GPS, description, priority, status, assignedTeam, timestamps |
| `Resource` (`resources`) | name, type, unit, quantity, district, organization |
| `ReliefConsignment` (`relief_consignments`), `ConsignmentItem` (`consignment_items`) | organization, shelter, status, dispatchedAt, deliveredAt; item resource + quantity |
| `GroundReport` (`ground_reports`) | reportedBy (Citizen), district, category, photoUrl, GPS, description, status, verifiedBy, actionNote, submittedAt |
| `PostEventReport`, `ReportMetric` | Used since 8 Oct 2026 by `PostEventReportServiceImpl` and the `/post-event-reports` pages (entities unchanged) |

---

## 8. Shared Neon database — schema drift checklist

The app now starts with `ddl-auto=update` (see §9). Hibernate `update` adds missing tables and columns but **never drops columns, relaxes `NOT NULL`, or widens `CHECK` constraints**, and it cannot add a `NOT NULL` column to a table that already has rows (it logs the DDL error and keeps starting). Whether the shared Neon schema ever caught up with Phases 2–5 is **unknown**: the session that wrote this note never connected to Neon.

What changed between the Phase 1 model and today:

| Table | Change | Risk on an already-populated shared DB |
|---|---|---|
| `warnings` | `+message`, `+expires_at`; `issued_at` no longer `@NotNull` | An old `issued_at NOT NULL` makes draft inserts fail |
| `warnings`, `ground_reports` | New enum values `DRAFT` and `ACTIONED` | Possible enum `CHECK` constraints will reject them (unverified) |
| `ground_reports` | `+district_id` NOT NULL, `+category` NOT NULL, `+action_note` | Add-column fails if rows exist; the mobile teammate's inserts must now supply both values |
| `shelters` | `+status` NOT NULL | Add-column fails if rows exist |
| `resources` | `+unit` NOT NULL, `+district_id` NOT NULL | Add-column fails if rows exist |
| `relief_consignments` | `district_id` replaced by `shelter_id`; `+delivered_at` | Old `district_id NOT NULL` stays in the table, so every new insert fails |
| `rescue_requests` | New table | Should be created automatically |

**Step 1 — read-only checks** (run in the Neon SQL editor):

```sql
-- Do the new columns exist, and are they nullable the way the code expects?
SELECT table_name, column_name, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND (table_name, column_name) IN (
    ('warnings','issued_at'), ('warnings','message'), ('warnings','expires_at'),
    ('ground_reports','district_id'), ('ground_reports','category'), ('ground_reports','action_note'),
    ('shelters','status'),
    ('resources','unit'), ('resources','district_id'),
    ('relief_consignments','district_id'), ('relief_consignments','shelter_id'), ('relief_consignments','delivered_at'))
ORDER BY table_name, column_name;

-- Expected: warnings.issued_at = YES; relief_consignments.district_id absent or YES.

SELECT to_regclass('public.rescue_requests');   -- should not be NULL

-- Enum CHECK constraints that ddl-auto=update will not widen:
SELECT conrelid::regclass AS table_name, conname, pg_get_constraintdef(oid) AS definition
FROM pg_constraint
WHERE contype = 'c'
  AND conrelid::regclass::text IN ('warnings','ground_reports','shelters','rescue_requests','resources','relief_consignments')
ORDER BY 1, 2;
```

**Step 2 — fixes, only if Step 1 shows a problem, and only after telling the teammates** (shared data):

```sql
ALTER TABLE warnings ALTER COLUMN issued_at DROP NOT NULL;
ALTER TABLE relief_consignments ALTER COLUMN district_id DROP NOT NULL;  -- or drop the column once nobody uses it

-- If a status CHECK lacks DRAFT / ACTIONED, replace it (use the constraint name returned by Step 1):
ALTER TABLE warnings DROP CONSTRAINT <name_from_step_1>;
ALTER TABLE warnings ADD CONSTRAINT warnings_status_check
  CHECK (status IN ('DRAFT','ISSUED','UPDATED','CANCELLED','EXPIRED'));
ALTER TABLE ground_reports DROP CONSTRAINT <name_from_step_1>;
ALTER TABLE ground_reports ADD CONSTRAINT ground_reports_status_check
  CHECK (status IN ('PENDING_SYNC','PENDING_REVIEW','VERIFIED','REJECTED','NEEDS_INFO','ACTIONED'));
```

If a new `NOT NULL` column could not be added because rows already exist, add it as nullable, backfill it, then `SET NOT NULL`. Look for `Error executing DDL` lines in the console at application startup to see exactly what Hibernate could not apply.

Safer option for demos and the viva: use a separate Neon branch (or a local Postgres) so the shared data is untouched.

---

## 9. Configuration and running

| File | Notes |
|---|---|
| `backend/src/main/resources/application.properties` | App name `dewecs`; datasource from `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}` with **no defaults** and no active profile; `open-in-view=false`; `ddl-auto=validate`; `format_sql=true`; port 8080 |
| `application-dev.properties` | Opt-in `ddl-auto=update` (adds missing tables/columns only) |
| `application-local.properties` | In-memory H2 (PostgreSQL mode, `NON_KEYWORDS=VALUE`), `create-drop`; `DemoDataLoader` seeds it |
| `backend/src/test/resources/application-test.properties` | H2 in memory, `create-drop`; every Spring-booting test uses `@ActiveProfiles("test")` |
| `.env.example` | Placeholders only; `.env` is git-ignored |

Phase 7 settings: `dewecs.photos.dir` (default `uploads/ground-reports`, git-ignored; `local` profile uses `${java.io.tmpdir}/dewecs-photos`, `test` uses `${java.io.tmpdir}/dewecs-test-photos`); `spring.servlet.multipart.max-file-size=6MB`, `max-request-size=7MB` (the service enforces 5 MB itself); `dewecs.api.cors.allowed-origin-patterns` (empty = no CORS; `local` allows `http://localhost:*,http://127.0.0.1:*`; applies to `/api/**` only). GPS columns are now `numeric(10,7)` in the mapping; Neon needs `doc/neon-gps-precision.sql` (run by hand).

Run: see `backend/README.md` (profiles table). Default profile validates the schema against `DB_*`; `dev` updates it; `local` needs nothing. `<packaging>war</packaging>` builds with `mvn package` and starts with `java -jar` (checked on the `local` profile); there is no `SpringBootServletInitializer`, so it is not deployable to an external Tomcat. H2 is now a `runtime` dependency so the `local` profile works from the jar.

### Security finding — committed credentials

**Status after Phase 6: the working tree no longer contains the password; the old value remains in git history until the Neon password is rotated.** The Neon password sat in plain text in `backend/src/main/resources/application.properties` (commit `3f8e285`) and in `application.yml` of commit `9c802d3`. Both commits were pushed. Commit `615f7e6` still used environment variables. **This note deliberately does not repeat the value.**

What to do:

1. Treat the password as exposed (even a private repo gives it to every collaborator and every clone). Reset the database role's password in the Neon console and tell the three teammates.
2. Put the datasource back on environment variables: `${DB_URL}`, `${DB_USERNAME}`, `${DB_PASSWORD}` (as in commit 1 and `.env.example`), and keep the real values in a git-ignored `.env` or in the IDE run configuration.
3. Rewriting history (`git filter-repo`) is optional and needs the whole group to re-clone. Rotation is what actually removes the risk.
4. Check whether the GitHub repository is public. Also check teammates' repos for the same mistake, since they share the database.

---

## 10. Tests

**152 tests, 0 failures (`mvn -B verify`, 7 Oct 2026, Phase 7).** JaCoCo: 82% lines, 70% branches overall (controller 82%/67%, controller.api 89%/62%, service.impl 92%/73%, dto.api 100%, exception 94%/77%; `config.DemoDataLoader` is only exercised by the `local` run). New classes: `CitizenServiceImpl` 97%/89%, `GroundReportSubmissionServiceImpl` 99%/91%, `FileSystemPhotoStorageService` 86%/72%, `GroundReportApiMapper` 100%/100%.

| Class | Tests | Kind |
|---|---|---|
| `WarningServiceImplTest` / `ShelterServiceImplTest` / `RescueRequestServiceImplTest` | 7 / 6 / 9 | Mockito |
| `ReliefSupplyServiceImplTest` / `ReliefDistributionServiceImplTest` | 4 / 6 | Mockito |
| `GroundReportServiceImplTest` / `DashboardServiceImplTest` | 7 / 1 | Mockito |
| `WarningRepositoryTest`, `HazardEventRepositoryTest`, `DistrictRepositoryTest` | 3 | `@DataJpaTest` mapping |
| `CustomQueryRepositoryTest` | 6 | `@DataJpaTest`: three `search` queries, `findAvailable`, `findByQuantityLessThan`, counts |
| `WarningFlowTest` / `ShelterFlowTest` / `RescueFlowTest` | 5 / 5 / 5 | MockMvc, full context on H2 |
| `ReliefFlowTest` / `GroundReportFlowTest` / `DashboardFlowTest` | 3 / 4 / 2 | MockMvc |
| `PageRenderingTest` | 2 | every list/new/detail/edit page returns 200 HTML |
| `ErrorHandlingTest` / `UnexpectedErrorTest` | 6 / 2 | HTML error page vs ProblemDetail JSON; 404, 400, 405, 500 |
| `GpsPrecisionTest` | 2 | `@DataJpaTest`: coordinates keep 7 decimals; the four columns are `numeric(10,7)` |
| `SubmissionQueriesRepositoryTest` | 3 | `@DataJpaTest`: NIC lookup, replay lookup (oldest match), paged newest-first list |
| `CitizenServiceImplTest` / `GroundReportSubmissionServiceImplTest` / `FileSystemPhotoStorageServiceTest` | 7 / 17 / 10 | Mockito and `@TempDir`: every contract rule |
| `GroundReportApiMapperTest` | 3 | order, millisecond truncation, `actionNote` only when ACTIONED |
| `ApiFlowTest` / `ApiErrorTest` / `ApiContractShapeTest` / `ApiCorsTest` | 6 / 13 / 1 / 4 | MockMvc on H2: whole flow, every failure as ProblemDetail, keys vs `doc/api-samples`, CORS on and off |

Flow tests extend `FlowTestSupport` (`@SpringBootTest`, `@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, `@Transactional` so each test rolls back) and build their own rows; the demo loader is not active.

Not covered: Postgres-specific behaviour (tests run on H2), concurrency, `DemoDataLoader` in the test suite.

---|---|---|
| `WarningServiceImplTest` | 7 | Mockito |
| `ShelterServiceImplTest` | 6 | Mockito |
| `RescueRequestServiceImplTest` | 9 | Mockito |
| `ReliefSupplyServiceImplTest` | 4 | Mockito |
| `ReliefDistributionServiceImplTest` | 6 | Mockito |
| `GroundReportServiceImplTest` | 7 | Mockito |
| `DashboardServiceImplTest` | 1 | Mockito |
| `WarningRepositoryTest`, `HazardEventRepositoryTest`, `DistrictRepositoryTest` | 3 | `@DataJpaTest` on H2 |

Coverage of the guards: each service test class covers the happy path and the main rejections (invalid status transition, over-capacity, over-stock, missing note, invalid enum text, blank message), plus lazy expiry, stock restore on cancel, and `FULL`↔`OPEN` flips.

Not covered: controllers (no MockMvc tests), Thymeleaf pages, the custom `search` queries, the exception handler, application start-up (`@SpringBootTest`), and anything Postgres-specific, because tests run on H2. The JaCoCo percentage was not recorded.

---

## 11. Known gaps and technical debt

1. **No authentication or authorization.** There is no Spring Security dependency. 26 `// TODO: restrict to … role once auth lands` comments mark every protected action (roles named: DMC-officer, rescue-coordinator, logistics-coordinator). "Who did it" fields are chosen from dropdowns.
2. **Fixed in Phase 6:** services are now `@Transactional`, so multi-step writes commit or roll back together. Still no locking or `@Version` (not allowed: shared schema), so concurrent check-ins or distributions can oversubscribe.
3. **Fixed in Phase 6:** browsers get `templates/error.html` with the correct status (404 unknown id/URL, 400 bad path variable or missing parameter, 405, 500 generic); other clients keep ProblemDetail JSON. Before the fix `/shelters/abc` and unknown URLs returned 500.
4. **Seed data exists only for the `local` profile** (`DemoDataLoader`). There are still no screens to create districts, organizations, users, hazard events, rescue teams, citizens or ground reports. Forms are unusable on an empty database.
5. **No REST API for the mobile app in this repo.** The README plans one for the fifth use case, but there is no `@RestController`.
6. **Unused model parts:** `PostEventReport`, `ReportMetric` (+ repositories; shared domain model, out of scope); enum values `WarningStatus.UPDATED`, `ConsignmentStatus.PREPARING` / `IN_TRANSIT`, `GroundReportStatus.PENDING_SYNC` / `NEEDS_INFO`, `RescueTeamStatus.FULL` / `NEEDS_SUPPORT`; `ShelterService.listAvailable()` / `ShelterRepository.findAvailable()` are never called.
7. **Warnings are not broadcast.** Channels are only stored. Published warnings cannot be edited (`UPDATED` is never used). Expiry is lazy.
8. Distribution ignores the shelter's status and supports one item per consignment, although the model allows several.
9. No pagination: lists use `findAll()` and dropdowns load every row.
10. **Docs refreshed in Phase 6** (README, `application-dev.properties`, `.env.example` check).
11. `ddl-auto=update` is now opt-in (`dev` profile only).
13. **Mobile API (Phase 7) limits:** no authentication (anyone who knows a NIC can report as that citizen), NIC format-checked only, photos on local disk, no push updates, replay detection is check-then-insert (no schema change allowed). The older services still read `LocalDateTime.now()` in the JVM zone; only the new submission service uses the Asia/Colombo `Clock`.
14. **Citizens in staff drop-downs:** `listUsersForSelection` returns every `User`, so citizens created by the app appear in the reviewer list on `/ground-reports/{id}` and in the "Issued by" list of the warning form (ask-first item, not changed). The web ground-report list still has no `ORDER BY` (ask-first).
12. **Proposals, not implemented (business-rule changes, need a decision):** (a) reject a distribution to a CLOSED shelter; (b) the dashboard's open-warnings count includes an overdue ISSUED warning until a warnings page is opened (a fresh `local` start shows 2, then 1).

---

## 12. Suggested next steps (3 days left)

**Today / tomorrow**

1. Rotate the Neon password and move the datasource back to environment variables (§9). Tell the group.
2. Run `cd backend && mvn test`. Confirm 43/43 after the `war` change and keep the JaCoCo report for the submission.
3. Run the read-only checks in §8 and agree any fix with the teammates.
4. Do the first full smoke test, which doubles as the demo script:
   - Create a warning draft → edit → publish → retract (and check one with an expiry time in the past).
   - Create a shelter → check in until it flips to FULL → check out → close → reopen.
   - Submit a rescue request → assign a team → complete; submit another → cancel and confirm the team is free again.
   - Create a supply → create a distribution (stock drops) → cancel (stock returns) → create another → deliver. Restock a supply and watch the low-stock count.
   - Open a ground report → mark reviewed → action with a note; dismiss another.
   - Check that the dashboard counts follow.
5. Fix whatever the smoke test breaks, then update the README.

**Before submission**

6. Be ready to explain in the viva why `PostEventReport` / `ReportMetric` have no service or page: they belong to the shared domain model, and your fourth use case, Reporting, is the ground-report review plus the dashboard (confirmed 6 Oct).
7. Cheap hardening that is easy to explain in the viva: `@Transactional` on the multi-save service methods, an HTML-aware error handler, one `@SpringBootTest` context test, a few MockMvc tests.
8. Seed data for a demo database (separate Neon branch or local Postgres).
9. Assignment 02 report: the critique report document from the earlier chat is not in this repo and had TODO placeholders in its final-submission skeleton. Re-check the brief for the exact submission items (code link, screenshots, test evidence).

---

## 13. Resume in a new Claude Code session

Paste this as the first message:

```text
Read PROGRESS_NOTES.md in the repo root first. It documents what is already built
(warnings, shelters, rescue requests, relief supplies/distributions, ground-report
review, dashboard), the conventions to follow, the known gaps, and the schema-drift
checklist for the shared Neon database.

Rules for this session:
- Never print or commit credentials, and never connect to the shared Neon database
  without asking me first. Run tests with `cd backend && mvn test` (H2 only).
- Follow the existing conventions: controller -> service interface -> service/impl ->
  repository, DTO + mapper, one ValidationException per aggregate, constructor
  injection, post/redirect/get with flash "message" / "error".

Task: <describe the next task>
```

### Getting the lost chat back

Claude Code keeps sessions per working directory, so it may still be on your PC.

1. In a terminal run `claude --resume` (the VS Code extension and the desktop app keep their own session lists). In the picker press `Ctrl+A` to show sessions from every project on the machine, type part of the title or a word like "warning" to search, and press `Space` to preview.
2. Transcripts are files at `C:\Users\dilmithp\.claude\projects\<folder>\<session-id>.jsonl`, where `<folder>` is the project path with every non-letter/digit replaced by `-`. If you ran the chat from another path, the folder name will differ. PowerShell: `Get-ChildItem $env:USERPROFILE\.claude\projects | Where-Object Name -match 'dewecs|se3070'`.
3. Default retention is 30 days (`cleanupPeriodDays`), so sessions from 25–27 Sep should survive until roughly late October unless they were cleaned.
4. Once you have a session ID, `claude --resume <session-id>` works from any directory on current versions.

Reference: https://code.claude.com/docs/en/sessions

---

## 14. Viva cheat-sheet

| Question | Short answer (grounded in this code) |
|---|---|
| Why service interfaces and impls? | Controllers depend on `WarningService`, not the implementation (DIP). Tests construct the impl with mocked repositories. |
| Why DTOs and mappers? | Entities never reach the views. Forms bind to `*FormRequest` with validation messages, pages read `*Response`. |
| Where are business rules enforced? | In the service impls (status checks, capacity, stock), never in controllers or templates. A broken rule throws that aggregate's `*ValidationException`; the controller shows it as a flash error or form error. |
| SOLID examples? | SRP: entities hold data, mappers convert, services decide, controllers route. LSP: `Citizen extends User`. ISP: small repositories and one service interface per use case. DIP: constructor-injected interfaces. OCP is only a possible extension point (pluggable broadcast senders), not implemented. |
| Why reuse `ISSUED` / `CANCELLED` / `VERIFIED` / `REJECTED`? | The database is shared with teammates; renaming enum values would break their data (see comments in `WarningStatus` and `GroundReportStatus`). |
| How is shelter occupancy kept consistent? | Check-in and check-out change `currentOccupancy` and flip `OPEN`↔`FULL` in the same service method. Tests: `checkIn_lastAvailableSlot_flipsStatusToFull`, `checkOut_whenFull_flipsStatusBackToOpen`. |
| How do you stop over-distribution? | Quantity above stock is rejected, stock is deducted at dispatch and restored on cancel. Tests: `create_withQuantityOverStock_throwsValidationException`, `cancel_onDispatchedDistribution_restoresStock`. Honest limit: no transaction or locking yet. |
| Why lazy expiry? | No scheduler to run or test: due warnings flip to `EXPIRED` on read. Limit: the dashboard count can lag until a warnings page is opened. |
| How was it tested? | 83 tests: 40 Mockito service tests, 9 `@DataJpaTest`, 34 MockMvc tests on H2; JaCoCo 79% lines / 62% branches. |
| Why `@Transactional` and where? | Service impls are `@Transactional(readOnly = true)`; methods that save override it with `@Transactional` (e.g. `ReliefDistributionServiceImpl.create` deducts stock and writes the consignment together; `WarningServiceImpl.list` writes when it expires overdue warnings). Test: `ReliefFlowTest.distributionDeductsStockCancelRestoresItAndDeliveredCannotBeCancelled`. |
| How are errors shown? | `GlobalExceptionHandler` (`@ControllerAdvice`) returns `error.html` when the request accepts HTML, ProblemDetail JSON otherwise; framework 4xx are no longer 500. Tests: `ErrorHandlingTest`, `UnexpectedErrorTest`. |
| How do you know the whole flow works, not just services? | MockMvc flow tests per use case (`WarningFlowTest`, `ShelterFlowTest`, `RescueFlowTest`, `ReliefFlowTest`, `GroundReportFlowTest`), `DashboardFlowTest` for the five counts, `PageRenderingTest` for every page. |
| How do you demo without the shared database? | `local` profile: H2 plus `DemoDataLoader`; script in `doc/DEMO_SCRIPT.md`. Credentials come from environment variables only. |
| Are the custom queries tested? | `CustomQueryRepositoryTest` (null-means-any filters, strict less-than for low stock, `findAvailable` excludes FULL and CLOSED). |
| Why a new submission service instead of extending the review service? | `GroundReportServiceImpl` is the officer workflow with a three-argument constructor and its own test; citizen submission needs different collaborators (citizen and photo storage, a `Clock`). New behaviour went into `GroundReportSubmissionServiceImpl` (open/closed, single responsibility); the review service is untouched. |
| How are retries safe without a schema change? | The client sends the same `capturedAt`; `submit` looks up citizen + category + `submittedAt` (`findFirstByReportedBy_IdAndCategoryAndSubmittedAtOrderByIdAsc`) and returns the stored report with 200. It is check-then-insert, not a constraint, so two overlapping retries could insert twice; the lookup returns the oldest row so later retries still work. Tests: `GroundReportSubmissionServiceImplTest.submit_replay_...`, `SubmissionQueriesRepositoryTest`, `ApiFlowTest.wholeFlowSubmitReplayPhotoGetAndList`. |
| Why are photos validated by content? | The client's file name and Content-Type are attacker-controlled. `FileSystemPhotoStorageService` checks magic bytes (JPEG/PNG/WebP), size (5 MB), generates a UUID name and refuses any name that does not match or leaves the directory. Tests: `FileSystemPhotoStorageServiceTest` (text renamed .jpg, `../x`, absolute paths), `ApiErrorTest.photoRulesAre400`. |
| Why are coordinates `numeric(10,7)`? | Hibernate's default for an unannotated `BigDecimal` is `numeric(38,2)`: `GpsPrecisionTest` showed 6.9271234 reloading as 6.93 (about 1 km). 7 decimals is about 1 cm and 10 digits fit +/-180. Neon keeps the old columns until `doc/neon-gps-precision.sql` is run; `validate` does not compare precision, so startup is unaffected (checked against an old-style H2 file). |
| Why is `CitizenServiceImpl.identify` not transactional? | Two requests can race on a new NIC; the unique constraint rejects the second. In one transaction that would leave it rollback-only and the re-read of the winner would fail, so each repository call has its own transaction and the exception is caught outside (`CitizenServiceImplTest.identify_raceOnUniqueNic_rereadsTheWinner`). |
| How do API and web errors differ? | `GlobalExceptionHandler` picks by path: anything under `/api/` is always problem+json (explicit content type, even for `Accept: text/html`); the Thymeleaf pages get `error.html`. Tests: `ApiErrorTest`, `ErrorHandlingTest`. |
| Why offline-first? | In a disaster the connection is the least reliable part. Submit stores the report in `QueueRepository` first and `SyncService` sends it later, so a report is never lost to a dropped connection. Tests: `sync_service_test.dart` (offline then online, 500 then success), `new_report_screen_test.dart` (offline submit is saved on the phone). |
| How do retries stay idempotent without a schema change? | The phone creates `capturedAt` once, when the user taps Submit, and resends the identical string. The backend looks up citizen + category + `submittedAt` and answers 200 with the stored report instead of inserting again (`GroundReportSubmissionServiceImpl`). Test: `sync_service_test.dart` "replay" and "the same capturedAt goes out on every retry". |
| Why is `capturedAt` created on the device? | Only the device knows when the user saw the hazard, and a value fixed before the first attempt is what makes the retry key stable. The server rejects times more than 5 minutes ahead, so a wrong phone clock is a visible permanent error. |
| What does Provider do here? | `ChangeNotifier` controllers (`SettingsController`, `IdentityController`, `ReferenceDataController`, `SyncService`, `ReportsController`) are created once in `AppDependencies` and handed to the widget tree; screens `watch` them and rebuild on `notifyListeners()`. No code generation, nothing hidden. |
| How are failures classified? | One `ApiException` with `retryable` following the contract: network, timeout, 5xx, 408 and 429 retry later; other statuses need the user. A 404 on submit asks the reference data whether the citizen or the district is gone. Tests: `http_dewecs_api_test.dart` "retry rules", `sync_service_test.dart` 404 cases. |
| Why decode responses as UTF-8 bytes? | Spring sends `application/json` without a charset and package:http would decode it as Latin-1, garbling Sinhala text. `HttpDewecsApi` always uses `utf8.decode(response.bodyBytes)`. Test: Sinhala and em dash in `http_dewecs_api_test.dart`. |
| What would you add to the app next? | Background sync with WorkManager, authentication (so a NIC alone cannot report as a citizen), Sinhala and Tamil strings, a map view, push notifications for status changes, and iOS. |
| What would you add next? | Spring Security with the roles named in the TODOs, `@Transactional` and optimistic locking, MockMvc and context tests, pagination, a scheduled expiry job, the mobile REST API. |

---

## 15. Numbers (verified from Git at `3f8e285`)

- 3 commits · 105 Java files (95 main + 10 test) · about 5,100 main and 1,200 test lines
- 16 entities + 13 enums = 29 domain classes · 16 repositories · 7 controllers · 7 service interfaces + 7 impls · 15 DTOs · 6 mappers · 7 exception classes
- 19 Thymeleaf templates + `style.css` · 44 routes · 44 Postman requests in 7 folders
- Phase 6 (7 Oct): 83 tests (40 Mockito + 9 `@DataJpaTest` + 34 MockMvc) · JaCoCo 79% lines / 62% branches · 26 auth TODO comments
- Phase 7 (7 Oct): 152 tests · JaCoCo 82% lines / 70% branches · 7 API endpoints · 6 sample JSON files
- Phase 8 (7 Oct): Flutter app, 152 widget/unit tests, 29 smoke checks against the real backend
- Original figures at `3f8e285`: 43 tests (40 Mockito + 3 repository slice)

## 16. Mobile app (Phase 8, `mobile/`)

Flutter app "DEWECS Report" for the fifth use case (ground hazard reporting by citizens). Android only
(`flutter create --platforms android`), Flutter 3.47.6 kept in `projects/DevTools/flutter` with the pub cache and Gradle
home beside it so the space is easy to reclaim. It talks to the Spring backend over HTTP only (contract v1,
`doc/MOBILE_API.md`); it has no database credentials, no Google Maps and no Firebase.

- **What exists:** identify once (NIC never stored), report form (category chips, description, GPS or typed coordinates,
  optional photo), offline queue and sync engine, My reports (cached, status chips, officers' note when ACTIONED, photo,
  Open in maps), Settings (server address, Test connection, Demo mode with a fake server), debug-only Sync queue screen.
- **How to run it:** `mobile/README.md` (emulator `http://10.0.2.2:8080`, phone via LAN IP or `adb reverse tcp:8080 tcp:8080`,
  backend started with `--spring.profiles.active=local`). The Android SDK is not installed on this machine yet, so the
  app has been verified by tests and by `tool/smoke.dart`, not on an emulator.
- **Tests:** 152 Flutter tests (models and fixtures from `doc/api-samples`, `HttpDewecsApi` with `MockClient`, the fake server,
  validators, queue state machine, sync engine, controllers, widgets, 200% text scale, tap-target and contrast guidelines,
  both themes) and `dart run tool/smoke.dart http://localhost:8092`: 29 checks against the real backend (local profile,
  port 8092, stopped by PID afterwards), all passed.
- **Dependencies added** (all from the allowed list): http (HTTP client), http_parser (multipart content type),
  provider (state), shared_preferences (settings, identity, queue, caches), geolocator (GPS), image_picker (camera and
  gallery), path_provider and path (photo folder), connectivity_plus (send when the connection returns),
  url_launcher (Open in maps), intl (date format); cupertino_icons and flutter_lints come from `flutter create`.
- **Limitations:** no background sync (only while the app is open), no authentication, no map tiles, English only, check
  the phone clock (a phone more than 5 minutes ahead of Sri Lanka time gets a permanent 400), cleartext HTTP only in the
  debug manifest, web not created.

---

## Phase 6 log

- **Stage 1 (done):** `application.properties` now reads `${DB_URL}`/`${DB_USERNAME}`/`${DB_PASSWORD}` with no defaults and no active profile. `dev` = opt-in `ddl-auto=update`; new `local` profile = H2 in-memory (PostgreSQL mode, `create-drop`). H2 scope changed test -> runtime in `pom.xml`. The three `@DataJpaTest` classes already had `@ActiveProfiles("test")`; `.env` is git-ignored and `.env.example` holds placeholders only. Old password still in git history: rotate it.
- **Stage 2 (done):** `mvn -B test` = 43 run, 0 failed (war packaging did not break it). `mvn -B -DskipTests package` builds `target/dewecs-0.0.1-SNAPSHOT.war`; `java -jar` with `--spring.profiles.active=local` starts in ~12 s and `/dashboard` returns 200 (empty DB). Baseline JaCoCo before Phase 6 tests: line 40%, branch 34% (service.impl 76%/52%, controller/mapper 0%). Next: Stage 3 demo data and flow tests.
- **Stage 3 (done):** `config/DemoDataLoader` (local profile only, idempotent) and 26 new MockMvc tests in `src/test/java/.../web` (Warning/Shelter/Rescue/Relief/GroundReport/Dashboard flows + `PageRenderingTest`); total now 69 tests, all green. App started on `local`, all 24 GET pages return 200; `doc/DEMO_SCRIPT.md` written from steps actually run. H2 URLs got `NON_KEYWORDS=VALUE` (the out-of-scope `ReportMetric.value` column otherwise breaks H2 DDL; no domain change). Bugs seen: `/shelters/abc` and unknown URLs return 500 (fix in Stage 4b); 404 returns JSON to browsers (Stage 4b). Fresh dashboard counts the overdue warning as open (ask-first item).
- **Stage 4 (done):** (a) service impls are `@Transactional(readOnly = true)` at class level; every method that saves is `@Transactional`, plus Warning `getById`/`list` (they expire overdue warnings). (b) `GlobalExceptionHandler` is now a `@ControllerAdvice` that returns `templates/error.html` for requests that accept `text/html` and the same ProblemDetail JSON otherwise; Spring MVC errors keep their 4xx status (previously 500) and unexpected errors are logged and shown generically. (c) `CustomQueryRepositoryTest` covers the 3 `search` queries, `findAvailable`, `findByQuantityLessThan` and the count methods. 83 tests green. Left: docs (Stage 5).
- **Stage 5 (done):** `backend/README.md` rewritten; `doc/neon-schema-check.sql` (SELECT-only, never run); `CLAUDE.md` (31 lines); this file updated (sections 1, 9, 10, 11, 14, 15). Left: final `clean verify`, secret scan, Postman check, final report.
- **Stage 6 (done):** `mvn -B clean verify` green (83 tests, WAR built); secret scan clean in tracked/new project files (the only hits are `.claude-flow/` plugin logs that record my own grep/sed patterns, not a real value); no controller or route changed, so the Postman collection (44 requests) still matches. Hand-over items are in the final report.

---

## Phase 7 log

- **Stage 1 (done):** Proved the bug first: `GpsPrecisionTest` showed `numeric(38,2)` and 6.9271234 reloading as 6.93. Added `@Column(precision = 10, scale = 7)` to `gpsLat`/`gpsLng` in `GroundReport` and `RescueRequest`, and `@DecimalMin/@DecimalMax` (-90..90, -180..180) to `RescueRequestFormRequest` (tests in `RescueFlowTest`). A new-mapping app started with `ddl-auto=validate` against an old-style file-based H2 schema (numeric(38,2)), so startup is unaffected. `doc/neon-gps-precision.sql` written, never run. Next: Stage 2.
- **Stage 2 (done):** New `CitizenService`/`CitizenServiceImpl` (not @Transactional on purpose, re-reads the winner on a unique-NIC race), `GroundReportSubmissionService`/`Impl` (submit with replay by citizen+category+capturedAt, attachPhoto not one transaction, paged `listForCitizen`), `PhotoStorageService`/`FileSystemPhotoStorageService` (magic bytes, UUID names, containment check), `ClockConfig` (Asia/Colombo), `CitizenValidationException` (mapped to 400), repository queries, multipart 6MB/7MB, `dewecs.photos.dir`, `uploads/` in .gitignore. 37 new tests. Next: Stage 3 API layer.
- **Stage 3 (done):** `doc/api-samples/*.json`; `dto/api` records, `GroundReportApiMapper`, four `*ApiController` classes under `controller/api`; `GlobalExceptionHandler` now answers every `/api/` path with problem+json (explicit content type, so `Accept: text/html` still gets JSON) and handles malformed JSON (400) and `MaxUploadSizeExceededException` (413); optional CORS via `dewecs.api.cors.allowed-origin-patterns` (`ApiCorsConfig`, on for the local profile); test profile photo dir in tmp. Tests: `ApiFlowTest`, `ApiErrorTest`, `ApiContractShapeTest`, `ApiCorsTest`, `GroundReportApiMapperTest`; 151 total, green. Next: Stage 4 web photo.
- **Stage 4 (done):** `ground-reports/detail.html` shows a photo whose URL starts with `/api/v1/photos/` as an `<img class="report-photo">` inside a link (legacy text kept as text); CSS class added; test in `GroundReportFlowTest`. Phase 6 flow tests still pass (152 total). Next: Stage 5 docs.
- **Stage 5 (done):** `doc/MOBILE_API.md` (contract copied exactly plus error semantics, retry guidance, examples, run instructions, limitations), new `doc/DEWECS-mobile-api.postman_collection.json` (existing collection untouched), this file (sections 6, 9, 10, 11, 14, 15), README and CLAUDE.md. Left: final verify and smoke run (Stage 6).
- **Stage 6 (done):** `mvn -B clean verify` green: 154 tests, WAR built, JaCoCo 82% lines / 70% branches. A first clean run exposed a flaky assertion (Jackson dropped trailing zeros, `...11.44`), so `JacksonConfig` now always writes three fraction digits (`ApiTimestampFormatTest`). Smoke run on the `local` profile: reference-data, identify (201 then 200), submit (201, replay 200 same id), PNG upload, report, photo bytes identical, list, 6.5 MB upload gives JSON 413, officer page shows the `<img>`. App stopped by PID. Secret scan clean. Left for the owner: run `doc/neon-gps-precision.sql`, hand `doc/MOBILE_API.md` to the Flutter teammate, push.

---

## Phase 8 log

- **Stage 1 (done):** `mobile/` created with `flutter create --platforms android` (Flutter 3.47.6 at `DevTools\flutter`; pub cache and Gradle home kept in `DevTools` for my sessions). Material 3 light/dark shell with three destinations, `strings.dart`, theme, folders per spec, Android permissions and https/geo `<queries>` in the main manifest, cleartext only in the debug manifest, dependencies added (http, http_parser, provider, shared_preferences, geolocator, image_picker, path_provider, path, connectivity_plus, url_launcher, intl). The Android SDK is not installed yet, so the app cannot run on Android; analyze and test pass.
- **Stage 2 (done):** pure-Dart `lib/models` (District, ReferenceData, Citizen, GroundReport, ReportPage, ApiProblem, ReportSubmission, contract rules, Sri Lanka time helper) and `lib/api` (`DewecsApi`, `HttpDewecsApi` with UTF-8 byte decoding and one `ApiException` whose `retryable` follows the client rules, `FakeDewecsApi` with replay, photo rules, failure switches and `resetServer`). Fixtures copied from `doc/api-samples`. All contract timestamps are UTC-flagged naive values so they compare consistently on any phone time zone (a bug found by the fake-server test). 42 tests green, analyze clean.
- **Stage 3 (done):** settings (persisted server address, Test connection, Demo mode switch with confirmation that clears identity and cached data, fake-server switches), cached reference data, identify screen (contract-exact validators in `validators.dart`, NIC never stored, offline message keeps the form), Home/New report gated on identity with a first-run identify push. Provider + ChangeNotifier controllers wired in `AppDependencies`. A widget test caught a real bug (refresh notified during build); fixed by starting it after the first frame. 72 tests green, analyze clean.
- **Stage 4 (done):** offline queue and sync: `QueuedReport` state machine (sending never persisted), `QueueRepository`, `SyncService` (single-flight, oldest first, back-off 5s/15s/60s/5min, stops only on network/timeout, permanent errors need attention, photo failure never undoes the report, 404 -> reference data decides identity reset vs district gone, same `capturedAt` on every retry), `ConnectivityTrigger` (connection regained, app resumed), device services behind interfaces (geolocator, image_picker), report form with category chips, location with permission messages and manual fields, photo copied into the app folder. Widget tests caught a real bug (a conditional list child shifted the form fields and dropped their validation state). 121 tests green, analyze clean.
- **Stage 5 (done):** My reports: `ReportsController` (first page cached for offline, load more 20 at a time, prunes sent items once the server list returns their id, 404 resets the identity), Home list (waiting items, sent-but-not-listed items, server reports, status chips with labels and colours, offline banner, empty/error states, Send now, pull-to-refresh), detail screen (action note only when ACTIONED, photo, Open in maps with geo: then OpenStreetMap fallback, edit and send again, delete). The shell explains and opens Identify when the identity disappears. 141 tests green, analyze clean.
- **Stage 6 (done):** loading, empty and error states on every screen; text-scale 200% test on a 360x740 phone for all screens (overflows fixed by making the prompt and message screens scrollable); Android tap-target, labelled-target and text-contrast guidelines pass in light and dark; malformed server data shows a readable error; debug-only Sync queue screen under Settings. 152 tests green, analyze clean.
- **Stage 7 (done):** `mobile/tool/smoke.dart` ran 29 checks against the real backend (local profile on port 8092, stopped by PID): reference data, identify (201 then 200, never overwritten), submit with Sinhala text and an em dash, replay (200, same id), photo upload and download, list, and the error cases; all passed, no mismatch with the contract. `mobile/README.md` written, notes section 16 and the viva cheat-sheet extended. Final: analyze clean, 152 tests green, secret scan clean.

---

## Phase 9 log

- **Stages 1-3 (done together, tests green):** `fragments/layout.html` is now a fragment library (`head`, `nav`, `pageHeader`, `flash`, `badge`, `enumLabel`, `time`); all 21 templates use it (lang, viewport, skip link, one `<main id="main">`, favicon as data URI, no duplicated head or flash). `style.css` rebuilt on design tokens with components (buttons, cards, tables, filter grid, forms with aria-invalid/aria-describedby, detail grid, stat tiles, occupancy progress, badges); `static/js/app.js` (10 lines) adds the optional confirm dialog (`data-confirm`) on retract, close, cancel and dismiss. Timestamps render as yyyy-MM-dd HH:mm, empty values as a dash, enums as readable labels. No Java, URL, field name or form action changed; 154 tests pass untouched.
- **Stage 4-6 (done):** contrast table computed for every text/background token pair (0 failures; one border colour darkened to reach 3:1), all 37 URLs return 200 on the local profile (port 8091, stopped by PID) with no leaked template syntax, Whitelabel or exception text, labels and `for`/`id` pairs checked. 40 screenshots (1280 and 390 px) in `doc/screenshots/`; the 390 px set is taken through a 390 px iframe because headless Edge lays a direct 390 px window out wider. Screenshots caught a CSS specificity bug (coloured buttons rendered as plain ones) and "Sms/Tv" channel labels; both fixed. `doc/DEMO_SCRIPT.md` updated for the new status wording; `backend/README.md` has a UI section. Presentation only: no Java, URL, field name or form action changed.

## Phase 10 (9 Oct 2026): Flutter UI restyle and connection test
- Flutter theme now follows the web design tokens (navy bar, #1D4F91 actions, bordered white cards, pill badges, same status colours); new widgets/section_card.dart, summary tiles on My reports, accent-edged report cards, card-based detail, form, identify and settings screens. No behaviour or API change.
- Settings Test connection: result panel with advice (lib/connection_hint.dart), shows the address and time tried, notes when Demo mode tested the fake server, warns on plain http to another machine, never stays stuck. Release APK still blocks plain http (cleartext only in debug builds): use https, or a debug build for a LAN backend. 162 Flutter tests, analyze clean.

- Seed data (9 Oct 2026): doc/neon-seed-sri-lanka.sql inserts Sri Lankan dummy data (10 districts, 8 organisations, officers, 12 citizens, teams, shelters, supplies, hazard events, warnings, rescue requests, ground reports, distributions). Idempotent, one transaction, syntax-checked with a PostgreSQL parser, run by a human in the Neon SQL editor. The app has no screen or endpoint to create districts, organisations, users, events or teams, which is why it is SQL.

- Officer JSON (9 Oct 2026): the officer pages answer in JSON on the same URLs (Accept: application/json or ?format=json; actions take a JSON body), see backend/README.md. 16 tests in OfficerJsonTest. Found and fixed a real bug on the way: Post-Event Report list and detail pages threw LazyInitializationException in production (open-in-view is false; FlowTestSupport hides it because it runs in one transaction); regression test PostEventReportNoSessionTest has no test transaction. 187 backend tests.

- UML (9 Oct 2026): doc/uml/ holds PlantUML source for the use case, class (domain, layers, mobile) and seven sequence diagrams, syntax-checked with PlantUML 1.2025.4; see doc/uml/README.md.

- Deployed backend (9 Oct 2026): the app now defaults to http://13.201.118.235:8080 (deployedBaseUrl in lib/config/app_config.dart, override with --dart-define=DEWECS_BASE_URL). The server is plain http, so release builds allow cleartext for that one IP only (res/xml/network_security_config.xml); debug builds allow http everywhere. Switch to https and remove the domain-config when the server has a certificate. Release APK rebuilt, 162 Flutter tests pass.
