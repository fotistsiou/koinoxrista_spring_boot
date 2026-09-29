# Programming Glossary

A personal learning glossary of **programming** concepts encountered while building
this Spring Boot app — transferable knowledge for the PHP → Java transition
(targeting fintech). Business/domain decisions (SplitRule, Debt, Payment, …) live
in [CLAUDE.md](CLAUDE.md), not here.

## How this file works

- A **CONCEPT** is something you must *understand* (how it works, why it exists) —
  transferable knowledge that survives this project.
- A **SETTING / TOOLING** item is something you *configure* (a value, a file) —
  project-specific mechanics.
- Entries are grouped by category and ordered by **learning flow**, not
  alphabetically.
- New entries get added as we encounter important concepts.

---

## 1. Core Concepts

### Dependency Injection / Constructor Injection
**What:** The framework creates an object's collaborators and passes them in,
instead of the object building them with `new`. Constructor injection supplies them
as constructor parameters.
**Why it matters / when I used it:** Every controller/seeder here takes its
`ApartmentRepository` via the constructor — Spring wires it automatically. It makes
dependencies explicit, allows `final` fields, and lets you pass mocks in tests. In
PHP terms it's the container resolving type-hinted constructor args, done for you.

### Bean
**What:** Any object whose lifecycle Spring manages (creates, wires, and disposes)
inside its application context.
**Why it matters / when I used it:** `@Component`, `@Controller`, `@Repository` all
register beans. If it's a bean, Spring can inject it; if it isn't, injection fails.
Understanding "is this a bean?" is the fix for most wiring errors.

### Component Scanning
**What:** At startup Spring scans the base package and its sub-packages for
annotated classes (`@Component`, `@Controller`, …) and registers them as beans.
**Why it matters / when I used it:** Everything lives under
`gr.fotistsou.koinoxrista`, so it's found automatically — no manual registration.
Pitfall: a class outside the main app's package won't be scanned and silently won't
be a bean.

### Idempotency
**What:** An operation you can run many times with the same end state as running it
once.
**Why it matters / when I used it:** The seeder guards with `if (count() == 0)` so
restarts don't duplicate apartments. Heavily interview-relevant in fintech — payment
and transfer APIs must be idempotent so a retried request doesn't charge twice.

### Logging (SLF4J)
**What:** SLF4J is a logging *facade* (an interface); a backend like Logback does
the actual writing. You code against SLF4J, not the implementation.
**Why it matters / when I used it:** The seeder uses a `Logger` instead of
`System.out.println`, so output has levels, timestamps, and can be filtered/routed.
The facade pattern means swapping the backend needs no code change.

### Optional
**What:** A container type (`java.util.Optional<T>`) that either holds a value or is
empty — Java's explicit way to represent "might not be there."
**Why it matters / when I used it:** `AppUserRepository.findByEmail` returns
`Optional<AppUser>`, forcing callers to handle the "no user found" case instead of
risking a NullPointerException on a null return. Signals absence in the type system.

### Service layer
**What:** The layer between controllers and repositories that holds business logic
(calculations, rules, coordinating multiple repositories, transactions).
**Why it matters / when I used it:** Rule of thumb: thin controller (HTTP only), fat
service (logic), narrow repository (DB only). We deliberately skipped a service for
Apartment because its controller only does `findAll` — a pass-through service adds
nothing. It arrives when real logic does (splitting bills into debts).

---

## 2. Persistence & Database

### JPA vs Hibernate vs Spring Data JPA
**What:** JPA is the *specification* (the standard API). Hibernate is an
*implementation* of it. Spring Data JPA is a layer on top that generates repository
code for you.
**Why it matters / when I used it:** Understanding the stack tells you where a
behavior comes from — `@Entity` is JPA, `ddl-auto` is Hibernate, `JpaRepository` is
Spring Data. A common interview question; conflating the three is a red flag.

### JDBC Driver
**What:** A library that translates Java DB calls into a specific database's wire
protocol; PostgreSQL needs the Postgres JDBC driver.
**Why it matters / when I used it:** It's the bottom layer beneath Hibernate. If the
driver dependency is missing or the URL is wrong, nothing above it can connect.

### Entity
**What:** A Java class mapped to a database table; each instance is a row. Marked
`@Entity`.
**Why it matters / when I used it:** `Apartment` is the first one. Fields become
columns, and JPA manages loading/saving them so you rarely write SQL by hand.

### Repository (Spring Data)
**What:** An interface (not a class) that Spring Data implements at runtime,
providing CRUD methods for one entity.
**Why it matters / when I used it:** `ApartmentRepository extends JpaRepository`
gives `findAll`, `save`, `count`, etc. with zero implementation code — you declare
intent, the framework provides the plumbing.

### Long vs long for id (wrapper vs primitive)
**What:** `Long` is an object and can be `null`; `long` is a primitive and defaults
to `0`.
**Why it matters / when I used it:** The id is `Long` so an unsaved entity has a
`null` id (clearly "no id yet") instead of a misleading `0`. JPA/Hibernate rely on
this null-vs-value distinction to tell new rows from existing ones.

### IDENTITY vs SEQUENCE (id generation)
**What:** Two `@GeneratedValue` strategies. IDENTITY uses the DB's auto-increment
column; SEQUENCE uses a dedicated sequence object Hibernate can pre-fetch from.
**Why it matters / when I used it:** `Apartment` uses IDENTITY (simple, Postgres
supports it). Trade-off worth knowing: IDENTITY forces an INSERT per row and blocks
JDBC batch inserts, so SEQUENCE scales better for bulk writes.

### Derived query methods
**What:** Spring Data generates the query from the method *name* — e.g.
`findByName(String name)` becomes a `WHERE name = ?` query.
**Why it matters / when I used it:** Not used yet (only `findAll`), but it's how
you'll add lookups without writing SQL. Pitfall: a typo in the property name fails
at startup, which is actually a useful early warning.

### BigDecimal for money
**What:** An arbitrary-precision decimal type. Unlike `double`/`float`, it represents
decimal values exactly.
**Why it matters / when I used it:** `Bill.amount` and `GasBill.fixedCharge` are
`BigDecimal`. `double`/`float` store decimals as binary approximations
(`0.1 + 0.2 = 0.30000000000000004`), and the errors accumulate — unacceptable for
money. Rule: never `double`/`float` for currency; always `BigDecimal` (or integer
cents). Map with `@Column(precision, scale=2)`.

### enum vs entity (who defines the values?)
**What:** Decision rule for fixed-value concepts: if the DEVELOPER defines the values
in code and they don't change at runtime, use an enum; if the USER adds/changes them
at runtime, use an entity (stored in the DB).
**Why it matters / when I used it:** `Role` and `SplitRule` are enums
(developer-defined, fixed). `Category` is an entity because the admin can add new
categories at runtime. Same rule, opposite outcomes — driven by who owns the values.

### @Enumerated: STRING vs ORDINAL
**What:** How JPA maps an enum field to a column. ORDINAL (the default) stores the
enum's position number (`0,1,2`); STRING stores its name (`"ADMIN"`).
**Why it matters / when I used it:** `Role` and `SplitRule` use
`@Enumerated(EnumType.STRING)`. ORDINAL is dangerous: reordering the enum or inserting
a value in the middle silently remaps existing rows to the wrong meaning. Always
STRING for persisted enums — reorder-safe and readable in the DB.

### @ManyToOne vs @OneToOne
**What:** Relationship annotations. `@OneToOne` = one-to-one (AppUser ↔ Apartment).
`@ManyToOne` = many rows point to one (many Bills → one Category). `@JoinColumn` puts
the foreign key on this ("owning") side's table.
**Why it matters / when I used it:** `Bill` has `@ManyToOne` to `Category`
(`category_id` FK lives in the bill table). `AppUser` has `@OneToOne` to `Apartment`.
The owning side holds the FK; nullable on the FK expresses a domain rule (a user may
have no apartment; a bill must have a category).

### JPA inheritance strategies
**What:** Three ways JPA maps a class hierarchy to tables. SINGLE_TABLE: one table
with all columns + a discriminator (extra columns are null for other types). JOINED: a
base table plus one table per subclass, linked by a shared-id FK. TABLE_PER_CLASS: one
full table per concrete class (rarely ideal).
**Why it matters / when I used it:** `GasBill extends Bill` with JOINED. Chosen to keep
each table clean (no null meter columns on electricity bills), honoring the "each type
only its own fields" decision. Trade-off accepted: reading a `GasBill` needs a JOIN —
negligible at this scale. In the DB, `gas_bill`'s id is also a FK to `bill`.

---

## 3. Web Layer (Spring MVC & Thymeleaf)

### @Controller vs @RestController
**What:** `@Controller` returns a *view name* to be rendered into HTML.
`@RestController` returns data (usually JSON) written straight to the response body.
**Why it matters / when I used it:** `ApartmentController` is `@Controller` because
it renders a Thymeleaf page. Switch to `@RestController` the day this exposes a JSON
API instead of HTML.

### Model
**What:** A key-value container for data the controller passes to the view.
**Why it matters / when I used it:** `model.addAttribute("apartments", …)` makes the
list available to `apartments.html`. It's the hand-off point between the web layer
and the template.

### Thymeleaf (server-side rendering)
**What:** A template engine that produces the final HTML on the server before
sending it to the browser.
**Why it matters / when I used it:** Renders `apartments.html` with real data. Unlike
a client-side SPA, the page arrives complete — closest analogue to PHP echoing HTML,
which eases the transition.

---

## 4. Configuration & Tooling

### ddl-auto
**SETTING.** **What:** Hibernate's schema-management mode
(`spring.jpa.hibernate.ddl-auto`): `none`, `validate`, `update`, `create`,
`create-drop`.
**Why it matters / when I used it:** Set to `update` locally so the schema follows
entity changes without manual SQL. Interview/fintech caveat: never `update` in
production — use `validate` plus real migrations (Flyway/Liquibase) so schema changes
are reviewed and versioned.

### Spring Profiles
**SETTING.** **What:** Named sets of configuration (`local`, `prod`, …) activated via
`spring.profiles.active`; `application-<profile>.properties` overrides the base file.
**Why it matters / when I used it:** `local` is active by default and holds the DB
connection in `application-local.properties`, keeping env-specific values out of the
shared config.

### Maven / pom.xml
**SETTING/TOOLING.** **What:** Maven is the build tool; `pom.xml` declares
dependencies, plugins, and the Java version.
**Why it matters / when I used it:** `./mvnw` builds and runs the app and pulls
dependencies. Rough PHP analogue: Maven ≈ Composer, `pom.xml` ≈ `composer.json` — but
Maven also compiles and packages.

### CommandLineRunner
**What:** An interface with one `run(...)` method that Spring executes once, after
the context is ready.
**Why it matters / when I used it:** `ApartmentSeeder` implements it to seed data on
startup. The go-to hook for "run this once when the app boots."

---

## 5. Docker & DevOps

### Image vs Container
**What:** An image is an immutable template (the blueprint); a container is a running
instance of an image.
**Why it matters / when I used it:** `postgres:17` is the image; `koinoxrista-db` is
the container running from it. One image → many containers. The class-vs-object
analogy holds.

### Volume
**What:** Docker-managed storage that lives outside a container's lifecycle, so data
survives the container being removed.
**Why it matters / when I used it:** `koinoxrista-pgdata` keeps the Postgres data
between restarts. Pitfall: `docker compose down -v` deletes the volume and wipes the
database.

### docker-compose.yml vs Dockerfile
**What:** A Dockerfile describes how to *build one image*. A compose file describes
how to *run one or more containers* together (ports, env, volumes).
**Why it matters / when I used it:** This project only has `docker-compose.yml` — it
runs a stock Postgres image, no custom image to build. You'd add a Dockerfile only if
you containerized the Spring app itself.

### Container vs Virtual Machine
**What:** A VM virtualizes a whole OS with its own kernel; a container shares the host
kernel and isolates only the process and its filesystem.
**Why it matters / when I used it:** Explains why the Postgres container starts in
seconds and stays lightweight. Common interview question — the key line is "shared
kernel vs full guest OS."

### .env file
**SETTING.** **What:** A file of `KEY=value` pairs Docker Compose reads to fill
variables like `${POSTGRES_PASSWORD}`.
**Why it matters / when I used it:** Keeps DB credentials out of `docker-compose.yml`;
`.env` is git-ignored and `.env.example` is the committed template. Its values must
match `application-local.properties`.
