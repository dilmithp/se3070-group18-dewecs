# DEWECS Backend

Spring Boot backend for the Smart Disaster Early-Warning and Emergency Coordination
System (SE3070, Group 18). Serves 4 of the 5 use cases as server-rendered Thymeleaf
dashboards; the 5th (ground hazard reporting) is exposed as a REST API for the
Flutter mobile app.

No `/docs` critique report or class diagram export was found in the repo when this
was scaffolded, so the domain model below reflects the corrected design as described
directly to the assistant that built this. If a critique report is added later,
reconcile the entities in `domain/` against it.

## Tech stack

Java 21, Spring Boot 3.5.16, Maven, Spring Web, Spring Data JPA, Thymeleaf, Bean
Validation, PostgreSQL (Neon) / H2 (tests), JUnit 5 + Mockito, JaCoCo.

> Spring Initializr (start.spring.io) now only generates Spring Boot 4.x projects,
> so this project was hand-assembled to stay on the requested 3.x line (3.5.16 is
> the latest 3.x release still published to Maven Central). Worth knowing if you
> add dependencies later — check they support Boot 3.x, not just 4.x.

## Project layout

```
domain/       JPA entities and enums
repository/   Spring Data JPA interfaces (one per entity)
service/      Service interfaces (+ service/impl for implementations)
controller/   Thymeleaf MVC controllers (thin; delegate to services)
dto/          Request/response DTOs (controllers never expose entities directly)
mapper/       Entity <-> DTO conversion
exception/    Custom exceptions + GlobalExceptionHandler (@RestControllerAdvice)
config/       Datasource / app configuration
```

`service`, `controller`, `dto`, `mapper` and `config` are currently empty scaffolding
(placeholder `.gitkeep` files) — business logic for each use case is implemented in
a later session.

## Running it

1. Copy the repo-root `.env.example` to `.env` (or otherwise export these into your
   shell) with your Neon connection details:
   ```
   DB_URL=jdbc:postgresql://<host>/<database>?sslmode=require
   DB_USERNAME=<username>
   DB_PASSWORD=<password>
   ```
2. First time only, against a fresh Neon database, run with the `local-init`
   profile so Hibernate creates the schema:
   ```
   mvn spring-boot:run -Dspring-boot.run.profiles=local-init
   ```
   After that, always run with the default profile (`ddl-auto=validate`, so the
   app fails fast instead of silently altering the schema):
   ```
   mvn spring-boot:run
   ```
3. The app starts on `http://localhost:8080`.

## Profiles

| Profile      | Datasource        | `ddl-auto` | When to use                          |
|--------------|--------------------|------------|---------------------------------------|
| (default)    | Neon (env vars)    | `validate` | Normal day-to-day running             |
| `local-init` | Neon (env vars)    | `update`   | Once, to create the schema initially  |
| `test`       | H2 in-memory       | `create-drop` | Automatically used by tests        |

## Tests & coverage

```
mvn test
```

Runs the JUnit 5 suite (H2 in-memory, `test` profile) and generates a JaCoCo
coverage report at:

```
target/site/jacoco/index.html
```

Open that file in a browser to view line/branch coverage per package.

### Testing pattern

See `src/test/java/com/group18/dewecs/repository/*RepositoryTest.java` for the
`@DataJpaTest` pattern used to verify entity mappings: annotate with
`@DataJpaTest` and `@ActiveProfiles("test")`, `@Autowired` the repositories you
need, persist an entity graph, then re-fetch and assert. Follow this pattern for
repository tests on your own use case's entities.

## SOLID notes

- **SRP** — entities hold no business logic; controllers (once added) will only
  translate HTTP <-> DTOs and delegate to services; mapping logic lives in
  `mapper/`, not `service/`.
- **OCP** — not forced anywhere yet; revisit if a use case needs pluggable
  strategies (e.g. multiple broadcast channel senders).
- **LSP** — `Citizen extends User` via JPA `JOINED` inheritance, adding only
  `nic`; anywhere a `User` is expected (e.g. `Warning.issuedBy`), a `Citizen`
  substitutes cleanly since it adds state but changes no behavior.
- **ISP** — each repository interface only extends `JpaRepository<T, Long>` with
  no speculative finder methods; add methods as individual use cases need them,
  not preemptively.
- **DIP** — service interfaces (once implemented) depend on repository
  *interfaces*, injected via constructor; no `@Autowired` fields, no `new`-ing
  collaborators.
