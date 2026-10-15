# Programming Glossary

A personal learning glossary of **programming** concepts encountered while building
this Spring Boot app.
Business/domain logic is documented in [DOMAIN.md](DOMAIN.md).

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
repository (e.g. `ApartmentRepository`) via the constructor — Spring wires it
automatically. It makes
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

### Vertical slice vs horizontal layering
**What:** Vertical slice = build every layer one *feature* needs (entity →
repository → service → controller → view) before moving on. Horizontal layering =
build one layer at a time, or all layers of one entity at a time.
**Why it matters / when I used it:** Every commit delivers something that works end
to end. `Bill` gets its controller only together with `Debt` and the calculation
service, because "enter a bill and see who owes what" is the feature — a Bill
controller on its own delivers nothing usable.

### Fail fast
**What:** When data is in an impossible state, throw immediately with a clear message
instead of continuing silently.
**Why it matters / when I used it:** `CategorySeeder` looks up apartments with
`apartmentRepository.findByName(...).orElseThrow(() -> new IllegalStateException(...))`.
A missing apartment stops startup with "Apartment not found: …" instead of seeding a
category with the wrong participants, which would surface much later as wrong debts.

### is-a vs has-a (inheritance vs association)
**What:** *is-a* = inheritance: the child is a specialized version of the parent.
*has-a* = association: one object refers to another.
**Why it matters / when I used it:** `GasBill` IS-A `Bill` (inheritance); `Debt`
HAS-A `Bill` (association, `@ManyToOne`). Prefer association unless the child truly
is a specialized version of the parent — inheritance couples the two tightly.

### Single Responsibility Principle (SRP)
**What:** A class should have one reason to change.
**Why it matters / when I used it:** Justifies keeping `AppUser` (identity, login)
separate from `Apartment` (physical unit, financial history): a password-policy
change and a billing change should never touch the same class.

### Strategy pattern
**What:** A family of interchangeable algorithms behind one interface; the caller
picks one at runtime instead of branching on a type.
**Why it matters / when I used it:** `SplitCalculator` is the interface, with one
class per `SplitRule` (`EqualSplitCalculator`, `MeterBasedSplitCalculator`);
`BillService` picks one by `bill.getSplitRule()`. Use it when each branch is real
logic that deserves its own class and tests, or new variants are expected. A plain
`switch` is enough when the branches are one-liners and the set is closed.

### Open/Closed Principle (OCP)
**What:** Code should be open for extension, closed for modification: add behavior
by adding code, not by editing working code.
**Why it matters / when I used it:** A new split rule = a new `SplitCalculator`
`@Component`; `BillService` stays unchanged, because it looks the calculator up by
rule instead of switching on it.

### Injecting all implementations of an interface
**What:** A constructor parameter `List<SomeInterface>` receives **every** bean that
implements it.
**Why it matters / when I used it:** `BillService` takes `List<SplitCalculator>` and
turns it into a `Map<SplitRule, SplitCalculator>` with
`Collectors.toMap(SplitCalculator::supportedRule, Function.identity())`. Two
calculators for the same rule make `toMap` throw, so the app fails at startup
instead of silently picking one.

### Pure function
**What:** Same input → same output, with no side effects (no DB, no I/O, no changes
to shared state).
**Why it matters / when I used it:** The split calculators were designed pure: they
only take a bill and its participants and return the shares; `BillService` does the
saving. That makes them the easiest and most reliable code to test — no mocks, no
DB, just input and expected output.

### Pattern matching for instanceof
**What:** `if (!(bill instanceof GasBill gasBill))` checks the type **and** binds a
typed variable in one step (Java 16+), so no explicit cast is needed.
**Why it matters / when I used it:** `MeterBasedSplitCalculator` uses it to reject
a non-gas bill and then work with `gasBill` directly.

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
**Why it matters / when I used it:** Lookups without writing SQL. First real use:
`AppUserRepository.findByEmail(String email)` returning `Optional<AppUser>`.
Pitfall: a typo in the property name fails at startup, which is actually a useful
early warning.

### BigDecimal for money
**What:** An arbitrary-precision decimal type. Unlike `double`/`float`, it represents
decimal values exactly.
**Why it matters / when I used it:** `Bill.amount` and `GasBill.consumptionCharge`
are `BigDecimal`. `double`/`float` store decimals as binary approximations
(`0.1 + 0.2 = 0.30000000000000004`), and the errors accumulate — unacceptable for
money. Rule: never `double`/`float` for currency; always `BigDecimal` (or integer
cents). Map with `@Column(precision, scale=2)`.

### BigDecimal arithmetic and rounding
**What:** `BigDecimal` is immutable and has no operators: use `add`, `subtract`,
`multiply`, `divide`, each returning a new value. `divide` needs a scale and a
`RoundingMode` whenever the result might not terminate.
**Why it matters / when I used it:** In the split calculators. Rules:
- Divide always with scale and `RoundingMode` — `10 / 3` without them throws
  `ArithmeticException` (non-terminating decimal).
- Multiply before dividing, and round **once** at the end, so errors don't stack.
- Distribute the leftover cents so the shares sum exactly to the total
  (`EqualSplitCalculator`).
- Create values from strings: `new BigDecimal("10.01")`, never
  `new BigDecimal(10.01)` — the `double` constructor carries its binary error in.

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
The owning side holds the FK; nullable on the FK expresses a domain rule (a user must
have an apartment; a bill must have a category).

### optional = false vs nullable = false
**What:** `nullable = false` (on `@Column`/`@JoinColumn`) is a **DB** constraint: it
goes into the DDL as a `NOT NULL` column. `optional = false` (on `@OneToOne`/
`@ManyToOne`) is a **JPA-level** rule: Hibernate checks it before insert and uses it
to load the association more efficiently (it knows the target always exists).
**Why it matters / when I used it:** Used together on `AppUser.apartment` — the DB
rejects a missing apartment even from raw SQL, and Hibernate catches it earlier, in
Java, with a clearer error.

### @ManyToMany and join tables
**What:** "Many" on both sides means neither table can hold the FK, so a third
(join) table holds the pairs. `@JoinTable` names it: `joinColumns` = this side's FK,
`inverseJoinColumns` = the other side's FK. Collections are LAZY by default.
**Why it matters / when I used it:** `Category.apartments` maps to
`category_apartment (category_id, apartment_id)` — the apartments that share a
category's bills.

### Set vs List for @ManyToMany
**What:** Use a `Set`, not a `List`, for a `@ManyToMany` collection.
**Why it matters / when I used it:** A `Set` rejects duplicate pairs, and Hibernate
handles an unordered `List` (a "bag") badly: removing one element deletes and
re-inserts all rows. `Category.apartments` is initialized with `new HashSet<>()` and
filled via `getApartments().add(...)` / `addAll(...)`. Pitfall: never assign
`Set.of(...)` — it is immutable, so Hibernate fails when it tries to modify it.

### JPA inheritance strategies
**What:** Three ways JPA maps a class hierarchy to tables. SINGLE_TABLE: one table
with all columns + a discriminator (extra columns are null for other types). JOINED: a
base table plus one table per subclass, linked by a shared-id FK. TABLE_PER_CLASS: one
full table per concrete class (rarely ideal).
**Why it matters / when I used it:** `GasBill extends Bill` with JOINED. Chosen to keep
each table clean (no null meter columns on electricity bills), honoring the "each type
only its own fields" decision. Trade-off accepted: reading a `GasBill` needs a JOIN —
negligible at this scale. In the DB, `gas_bill`'s id is also a FK to `bill`.

### Polymorphic queries
**What:** A query on the base entity returns instances of the actual subclass. With
JOINED inheritance, Hibernate LEFT JOINs the subclass tables and builds the right
type per row.
**Why it matters / when I used it:** `BillRepository.findAll()` returns real
`GasBill` instances for gas rows (Hibernate LEFT JOINs `gas_bill`), so no separate
`GasBillRepository` is needed.

### Unidirectional vs bidirectional relationships
**What:** Unidirectional = only the owning side (`@ManyToOne` + `@JoinColumn`) knows
the relation. Bidirectional adds `@OneToMany(mappedBy = "...")` on the other side.
The DB schema is identical either way; only Java navigation changes.
**Why it matters / when I used it:** All relationships here stay unidirectional. A
derived query (e.g. a future `debtRepository.findByBill`) gives the same result as
`bill.getDebts()`, and a bidirectional relation must be kept in sync on both sides
by hand.

### toString() in entities
**What:** An entity's `toString()` prints its own fields. A single-valued relation
may print only the related **id** (e.g. `billId=…`, not the whole `Bill`);
collections must not appear at all.
**Why it matters / when I used it:** Printing a related entity can trigger a
`LazyInitializationException` (if the relation becomes LAZY) or a
`StackOverflowError` (if it becomes bidirectional and both sides print each other).
Printing a collection iterates it, which loads the lazy collection (an extra query,
or a `LazyInitializationException` outside a session). Used in `Bill`, `Debt`,
`Payment`; `Category` leaves out `apartments`.

### @Transactional and atomicity
**What:** All-or-nothing: every DB write inside a `@Transactional` method commits
together, and a (runtime) exception rolls back every write in the method.
**Why it matters / when I used it:** `BillService.registerBill` saves the bill and
then its debts; if the calculation fails after the bill is saved, the bill is rolled
back too — never a bill without debts. Pitfall: it only works when the method is
called from **another bean**. Spring wraps the bean in a proxy that starts the
transaction; a call from inside the same class (self-invocation) bypasses the proxy,
so no transaction is started.

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
entity changes without manual SQL. Pitfall: `update` only **adds** missing
tables/columns; it never alters or drops existing ones (changed nullability, type,
renamed/removed fields stay as they were). After such a change, reset the DB in dev
(see RUNBOOK.md); in prod, write a migration. Interview/fintech caveat: never `update` in
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
**Why it matters / when I used it:** `ApartmentSeeder` and `CategorySeeder` implement
it to seed data on startup. The go-to hook for "run this once when the app boots."

### @Order
**What:** Sets the execution order of beans of the same kind, such as several
`CommandLineRunner`s; a lower value runs first. Without it, Spring does not guarantee
any order.
**Why it matters / when I used it:** `ApartmentSeeder` is `@Order(1)` and
`CategorySeeder` is `@Order(2)`, because categories need the apartments to exist
before they can link to them.

### Mockito as a Java agent
**SETTING.** **What:** Mockito instruments classes through a Java agent. Java 21
warns when an agent is loaded dynamically at runtime (future JDKs will block it), so
the agent is passed to the test JVM at startup instead.
**Why it matters / when I used it:** In `pom.xml`, `maven-dependency-plugin` (goal
`properties`) exposes the jar path as `${org.mockito:mockito-core:jar}`, and
`maven-surefire-plugin` passes it with
`<argLine>-javaagent:${org.mockito:mockito-core:jar}</argLine>`. IntelliJ picks the
setting up after a Maven sync.

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

---

## 6. Testing

### Unit vs integration test (the test pyramid)
**What:** A unit test checks one class in isolation, in memory, in milliseconds. An
integration test runs several real parts together (Spring context, DB). The test
pyramid: many fast unit tests at the bottom, fewer slower integration tests above.
**Why it matters / when I used it:** The calculator and `BillService` tests are unit
tests; `KoinoxristaApplicationTests` (`@SpringBootTest`) is an integration test that
needs the Docker DB. Limit: a unit test with mocked repositories cannot prove that a
rollback happened — there is no real transaction. That needs an integration test.

### Arrange – Act – Assert, @Test, @BeforeEach
**What:** Each test has three parts: set up the input (Arrange), call the code
(Act), check the result (Assert). `@Test` marks a test method; `@BeforeEach` runs
before **every** test, so each test starts from fresh objects (test isolation — no
test depends on another's leftovers).
**Why it matters / when I used it:** All test classes build their calculator and
apartments in `@BeforeEach`. Test classes and methods are package-private (JUnit 5
doesn't need `public`), and test names describe behavior, e.g.
`givesLeftoverCentToLowestIdApartment`.

### AssertJ
**What:** A fluent assertion library: `assertThat(actual).…`.
**Why it matters / when I used it:**
- `assertThatThrownBy(...)` — check the exception type **and** its message, so the
  test fails if the code throws for the wrong reason.
- `isEqualByComparingTo("40.00")` for `BigDecimal` — `equals()` also compares the
  scale, so `40.0` ≠ `40.00`; `compareTo` compares only the value.
- `extracting(Debt::getApartment)`, `allSatisfy(...)` — check a property across a
  whole list.
- `isSameAs(...)` — the same instance, not just an equal one.

### @ParameterizedTest + @CsvSource
**What:** One test method run once per row of data; each CSV row becomes the method
arguments (JUnit converts the strings to `BigDecimal`, `Integer`, …).
**Why it matters / when I used it:** `MeterBasedSplitCalculatorTest.matchesExcelResults`
uses real months from the gas Excel as rows (incl. Ω = 0 and a negative bill). Adding
a case = adding a line, and the Excel is the oracle.

### State-based vs interaction-based testing
**What:** State-based: check the **return value** / resulting state.
Interaction-based: check what the code **sent to its dependencies** (which methods
were called, with which arguments).
**Why it matters / when I used it:** The calculators are pure, so their tests check
the returned shares. `BillService` returns little of interest; its job is to save a
bill and debts, so its test checks what it passed to the repositories.

### Mockito
**What:** A library that creates fake implementations (mocks) of dependencies.
`@Mock` creates one; `@ExtendWith(MockitoExtension.class)` initializes them.
Stubbing: `when(...).thenReturn(...)` / `thenAnswer(...)` (compute the result from
the arguments). Checking: `verify(mock).method(...)`, `verify(mock, never())`,
`verifyNoInteractions(mock)`.
**Why it matters / when I used it:** `BillServiceTest` mocks the three repositories:
isolation (no DB), speed, control over what they return (e.g. `Optional.empty()` for
a missing category), and observation of the calls. `billRepository.save` uses
`thenAnswer(invocation -> invocation.getArgument(0))` to return the bill it was given.
Strict stubs (the extension's default): a stub the test never uses fails with
`UnnecessaryStubbingException`, which keeps tests free of dead setup.

### ArgumentCaptor / @Captor
**What:** Captures an argument passed to a mock so the test can inspect it.
**Why it matters / when I used it:** The `List<Debt>` is built **inside**
`registerBill` and only handed to `debtRepository.saveAll`; the captor is the only
way to see it. `@Captor` (instead of `ArgumentCaptor.forClass`) keeps the generic
type `ArgumentCaptor<List<Debt>>` without an unchecked cast.

### Sociable vs solitary unit tests
**What:** A solitary test replaces every collaborator with a mock; a sociable test
uses the real collaborators where they are cheap and safe.
**Why it matters / when I used it:** `BillServiceTest` mocks only the repositories
and uses the **real** (pure) calculators — they need no setup, and the test then
also checks that service and calculators work together.

### ReflectionTestUtils.setField
**What:** A Spring test helper that sets a private field by name via reflection.
**Why it matters / when I used it:** Entities have no `setId` (by design), but tests
need apartments with ids (ordering by id, `meterApartment` matching).
`ReflectionTestUtils.setField(apartment, "id", 1L)` sets it without adding a setter
to production code.
