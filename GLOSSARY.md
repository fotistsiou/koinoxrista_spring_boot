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

### Collectors.groupingBy with LinkedHashMap::new
**What:** `groupingBy(key, mapFactory, downstream)` turns a flat list into a
`Map<key, List<…>>`. The plain `groupingBy(key)` returns a `HashMap`, which loses
the order; passing `LinkedHashMap::new` keeps the keys in the order they first
appear.
**Why it matters / when I used it:** `BillService.findBillsWithDebts` groups the
flat debt list (already sorted newest first by the query) by bill:
`groupingBy(Debt::getBill, LinkedHashMap::new, Collectors.toList())`, so the bills
page keeps the query's order.

### Java record
**What:** An immutable data carrier: `record ApartmentBalance(Long apartmentId, …)`
generates final fields, the canonical constructor, accessors **without** `get`
(`apartmentId()`), and value-based `equals` / `hashCode` / `toString`. A **compact
constructor** (`public ApartmentBalance { … }`, no parameter list) normalizes the
values before they are assigned.
**Why it matters / when I used it:** `ApartmentBalance` is a record; its compact
constructor turns `null` sums into `0`, and `balance()` is an extra method. Use a
record for read-only data (query results). Use a class when an object is filled
step by step (forms: empty instance + setters) or must be mutable (JPA entities need
a no-args constructor and setters). Packages are named by **role** (`dto`), not by
language construct, so the record sits next to the form classes.

### BigDecimal.signum()
**What:** Returns `-1`, `0` or `1` for a negative, zero or positive value.
**Why it matters / when I used it:** The safe sign check for money:
`balance.signum() > 0` = owes. `equals(BigDecimal.ZERO)` would be wrong, because
`equals` also compares the scale (`0.00` ≠ `0`). Used in the balances page and to
prefill the payment amount only when the apartment owes.

### Enum with fields
**What:** Enum constants can carry data: each constant calls the enum's constructor
(`EQUAL_SPLIT("Ισόποσα")`), which stores it in a `final` field. The constructor is
implicitly private — no code outside the enum can create new constants.
**Why it matters / when I used it:** `SplitRule` has a Greek `label` for the UI
(`${rule.label}` in Thymeleaf). With `@Enumerated(STRING)` the DB still stores the
constant **name** (`EQUAL_SPLIT`), so the label can change freely without touching
existing rows.

### Defense in depth
**What:** Enforce the same rule at more than one layer, so one missing check doesn't
let bad data through.
**Why it matters / when I used it:** The filtered dropdowns and the form validation
are conveniences for the user; the same rules are enforced again in the service —
the `BillService` guard (gas bill ⇒ `METER_BASED` category) and the
`CategoryService` check (`METER_BASED` ⇒ exactly 2 apartments). Data can reach the
server without the form (a crafted POST, another caller, a future API), and the
service is the last line before the DB.

### Default methods in interfaces
**What:** A `default` method is an interface method **with a body**; every
implementation inherits it. Spring Data leaves default methods alone — it only
generates the abstract ones.
**Why it matters / when I used it:** `ApartmentRepository.findBalance(apartmentId)`
was first a default method built on `findAllBalances()` (load every balance, filter
in Java). It was replaced by its own `@Query` with `where a.id = :apartmentId` —
see *Filtering in the database vs in Java*. Default methods still fit small helpers
that combine other repository methods without loading extra data.

### Normalizing emails (one static helper, Locale.ROOT)
**What:** Emails are compared case-insensitively, so they are stored and looked up
in one canonical form: `email.trim().toLowerCase(Locale.ROOT)`.
**Why it matters / when I used it:** `UserService.normalizeEmail` is a `static`
helper used both when **storing** (`createResident`) and when **looking up**
(`AppUserDetailsService.loadUserByUsername`), so the two can never diverge — one
place to change. `Locale.ROOT` matters: plain `toLowerCase()` uses the JVM's
default locale, and in Turkish `"I".toLowerCase()` is a dotless `ı`, so
`ADMIN@…` would not match `admin@…`.

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
Names can also walk **across relationships**: `existsByApartmentId(Long id)` means
`apartment.id = ?` (property `apartment`, then its `id`), and
`findAllByOrderByApartmentIdAsc()` sorts by it.
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

### FetchType: EAGER vs LAZY (and the defaults)
**What:** `fetch` decides when a related entity is loaded. Defaults: `@ManyToOne` /
`@OneToOne` are **EAGER**, `@OneToMany` / `@ManyToMany` are **LAZY**. EAGER does not
mean "joined": in a JPQL or derived query that doesn't `join fetch` the relation,
Hibernate loads it with an **extra SELECT** per row — a hidden N+1.
**Why it matters / when I used it:** Every to-one relation here is
`fetch = FetchType.LAZY` (`AppUser.apartment`, `Bill.category`, `Debt.bill`,
`Debt.apartment`, `GasBill.meterApartment`, `Payment.apartment`). Nothing is loaded
unless a page asks for it explicitly (JOIN FETCH / `@EntityGraph`), so every query
loads exactly what it says.

### Hibernate proxy
**What:** A lazy to-one relation is filled with a **proxy**: a generated subclass of
the entity that holds only the id. `getId()` answers from the proxy without touching
the DB; any other getter loads the row — which needs an **open session**.
**Why it matters / when I used it:** `ResidentAccountService` calls
`user.getApartment().getId()` — no query. `toString()` methods print related ids for
the same reason. Pitfall: a proxy of `Bill` is not an `instanceof GasBill` even when
the row is a gas bill; only a really loaded entity has the right subclass.

### LAZY @OneToOne only works on the owning side
**What:** On the side that holds the FK (the owning side), Hibernate knows from the
FK column whether a related row exists, so it can put a proxy (or `null`) there. On
the inverse side (`mappedBy`), it can't know without querying, so it loads eagerly
anyway (unless bytecode enhancement is used).
**Why it matters / when I used it:** `AppUser.apartment` is the owning side
(`app_user.apartment_id`), so its `LAZY` really is lazy. Another reason to keep
relations unidirectional and owned by the side with the FK.

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
full table per concrete class (rarely ideal). With JOINED, saving a subclass is
**two inserts with the same id**: first `bill` (the IDENTITY id is generated there),
then `gas_bill` with that id.
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
`LazyInitializationException` (all to-one relations are LAZY now) or a
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

### @Transactional(readOnly = true)
**What:** Marks a service method as read-only: the Hibernate session is read-only,
so loaded entities get **no dirty-checking snapshot** (less memory) and the session
is **never flushed**; the driver/DB may optimize the transaction too.
**Why it matters / when I used it:** `BillService.findBillsWithDebts` and
`ResidentAccountService.loadAccount` only read. It also documents intent: a read
method that accidentally modifies an entity won't write it back.

### getReferenceById
**What:** Returns a reference (proxy) that holds only the id — **no SELECT**. The
row is loaded only if a field other than the id is accessed.
**Why it matters / when I used it:** The admin controllers turn the `categoryId`
and `meterApartmentId` chosen in a form into `Category` / `Apartment` references
to set on the new bill. Pitfall: an id that doesn't exist is not detected here, but
later (on access or on insert, as a FK violation).

### @Query and JPQL
**What:** `@Query` puts a hand-written query on a repository method. JPQL queries
**entities and fields** (`Debt d`, `d.bill`), not tables and columns; Hibernate
translates it to SQL.
**Why it matters / when I used it:** `DebtRepository.findAllWithBillAndApartment`.
Multi-line queries use Java text blocks (`"""…"""`), so they stay readable without
string concatenation.
**Parameters:** `where d.apartment.id = :apartmentId` + `@Param("apartmentId") Long
apartmentId` (`DebtRepository.findByApartmentIdWithBill`). The value is sent to the
DB **separately** from the query text (a bound parameter), so it is always treated as
data, never as SQL — that is what prevents SQL injection. Never build a query by
concatenating user input into the string.

### N+1 problem and JOIN FETCH
**What:** Loading N rows with 1 query, then 1 extra query per row to load a related
entity = 1 + N queries. `join fetch` in JPQL loads the relation in the **same**
query.
**Why it matters / when I used it:** `debtRepository.findAll()` for the bills page
gave 1 + N queries: one per bill and per apartment, because `@ManyToOne` was EAGER
by default (now all to-one relations are LAZY — see *FetchType*).
`findAllWithBillAndApartment` uses `join fetch d.bill`, `join fetch b.category`,
`join fetch d.apartment` and loads everything in one query. Spot it by counting the
SELECTs in the SQL log for one page load (see *SQL logging*).

### Persistence context (first-level cache)
**What:** Within one transaction (one `EntityManager`), each entity is loaded at
most once, and every reference to the same row is the **same Java object**.
**Why it matters / when I used it:** Explains why `Bill` works as a `groupingBy` key
without `equals`/`hashCode`: all debts of a bill point to the same `Bill` instance.
**One context per transaction:** with OSIV off, each repository call made
**outside** a transaction gets its own short-lived context, so the same entity can
be loaded again by the next call. Grouping the calls in one `@Transactional`
service method (`ResidentAccountService.loadAccount`) shares one context across
them.

### Open Session in View (spring.jpa.open-in-view)
**SETTING.** **What:** Keeps the Hibernate session (and a DB connection) open for the
whole web request, including view rendering. Enabled by default; Spring Boot logs a
warning at startup when it is left at the default.
**Why it matters / when I used it:** Disabled in `application.properties`
(`spring.jpa.open-in-view=false`), because it:
- **hides N+1** — lazy loading happens silently while Thymeleaf renders;
- **holds a pooled connection** during rendering, longer than the queries need;
- **blurs the data-access boundary** — templates end up triggering queries.

Now each page loads what it needs in a repository/service; a missed relation fails
loudly (*LazyInitializationException*) instead of running a hidden query.

### LazyInitializationException
**What:** A lazy relation (a proxy or a lazy collection) is accessed **after** the
session that loaded its owner was closed — e.g. in a template with OSIV off.
**Why it matters / when I used it:** `my/account.html` read `${user.apartment.name}`
while `findByEmail` hadn't fetched the apartment. The fix is to **fetch in the
repository** (`join fetch` / `@EntityGraph`) or to hand the view the values it needs
(see *View-model record*). Not fixes: `@Transactional` on controllers or
`hibernate.enable_lazy_load_no_trans` — both just bring hidden lazy queries back.

### Lazy loading inside a transaction
**What:** Inside a transaction the session is open, so touching a lazy relation
simply runs a query and works.
**Why it matters / when I used it:** Observed in `BillService.registerBill`:
iterating `category.getApartments()` (lazy) loads them there without error. It works
— but each access is still a query, so lists should be fetched up front.

### Isolation level READ_COMMITTED
**What:** PostgreSQL's default isolation level: each **statement** sees the data
committed before it started. Two queries in the same transaction can therefore see
different data if another transaction commits in between. `REPEATABLE_READ` would
give the whole transaction one snapshot.
**Why it matters / when I used it:** `ResidentAccountService.loadAccount` runs four
queries in one read-only transaction. That shares the persistence context, but it
does **not** guarantee that the balance, debts and payments come from the same
moment. Acceptable here (a payment committed between two queries is very unlikely
and harmless on a read page).

### Aggregation in the database
**What:** Sums, counts and grouping (`SUM`, `COUNT`, `GROUP BY`) run in SQL; the DB
returns one small row per result instead of every detail row.
**Why it matters / when I used it:** `ApartmentRepository.findAllBalances` returns
one row per apartment with its totals. Loading every debt and payment into Java
just to sum them with streams moves far more data and gets slower as history grows.

### Filtering in the database vs in Java
**What:** Put the condition in the query (`WHERE`) so the DB returns only the rows
needed, instead of loading many rows and filtering with a stream.
**Why it matters / when I used it:** `ApartmentRepository.findBalance` used to call
`findAllBalances()` and `stream().filter(…)` for one apartment; it is now its own
query with `where a.id = :apartmentId`. Less data over the wire, the DB can use the
primary-key index, and the method does what its name says.

### Join fan-out
**What:** Joining one parent to **two** child tables produces every combination of
their rows: an apartment with 10 debts and 5 payments gives 50 rows, so each debt is
counted 5 times and each payment 10 times in a `SUM`.
**Why it matters / when I used it:** `findAllBalances` avoids it with one scalar
subquery per sum — `(select sum(d.amount) from Debt d where d.apartment = a)` and
the same for `Payment` — instead of `join` + `sum` over both tables.

### Constructor expression (select new …)
**What:** In JPQL, `select new com.example.Dto(a.id, …)` builds one DTO per result
row by calling its constructor.
**Why it matters / when I used it:** `findAllBalances` returns
`List<ApartmentBalance>` directly. The class needs its **fully qualified** name
(`gr.fotistsou.koinoxrista.dto.ApartmentBalance`): JPQL is a string parsed by
Hibernate and doesn't see Java imports. Entities are known by their simple name
(`Debt`, `Payment`) because Hibernate registered them.

### SUM returns NULL on zero rows
**What:** In SQL/JPQL, `SUM` over no rows returns `NULL`, not `0`.
**Why it matters / when I used it:** An apartment with no payments yet gets
`totalPayments = null`; `null.subtract(…)` would throw a `NullPointerException`.
The `ApartmentBalance` compact constructor replaces `null` with `BigDecimal.ZERO`
(SQL alternative: `coalesce(sum(…), 0)`).

### NOT EXISTS subquery
**What:** `where not exists (select … where …)` keeps the rows for which the
subquery finds **nothing** — "parents without a child".
**Why it matters / when I used it:** `ApartmentRepository.findWithoutUser()`
(`where not exists (select u from AppUser u where u.apartment = a)`) fills the
apartment dropdown on the new-resident form, so the admin can only pick an apartment
that has no user yet.

### Hibernate-generated constraints
**What:** With `ddl-auto`, Hibernate creates the constraints itself and names them:
unique constraints get generated names like `uk1j9d9a06…`; an
`@Enumerated(STRING)` column gets a **CHECK** constraint listing the allowed values
(`app_user_role_check: role in ('ADMIN','RESIDENT')`).
**Why it matters / when I used it:** Visible with `\d app_user` (see RUNBOOK.md).
Limitation: `ddl-auto=update` does **not** alter existing constraints. Adding a new
`Role` value would leave the old CHECK in place and inserts with the new value would
fail — it needs a migration (or a DB reset in dev). One reason to move to Flyway
later.

### @EntityGraph(attributePaths = …)
**What:** Tells Spring Data to load the listed (lazy) associations in the same
query, without writing JPQL.
**Why it matters / when I used it:** `CategoryRepository.findAllByOrderByNameAsc()`
with `@EntityGraph(attributePaths = "apartments")` loads every category with its
apartments in one query — without it, the categories page would run one extra query
per category to show its apartments (N+1). `findWithApartmentsById` does the same
for the edit form. In a derived query name, the text between `find` and `By`
(`WithApartments`) is ignored, so it can be used to describe the variant.

### @OrderBy on a collection
**What:** `@OrderBy("id")` on a collection mapping makes Hibernate load it sorted
(and keep that order in the `Set`, using a `LinkedHashSet`).
**Why it matters / when I used it:** `Category.apartments` — the categories page
lists the apartments as Ισόγειο, 1ος, 2ος instead of a random `HashSet` order.

### exists… derived queries
**What:** `existsBy…` derived queries return a `boolean` and stop at the first
match, cheaper than loading the entity.
**Why it matters / when I used it:** Category name uniqueness:
`existsByName(name)` on create, `existsByNameAndIdNot(name, id)` on edit, so a
category doesn't conflict with **itself** when saved unchanged. The DB unique
constraint stays as the final safety net.

### Managed entities and dirty checking
**What:** Inside a transaction, an entity loaded by `findById` is **managed**:
Hibernate remembers its state and writes any changes automatically at commit
(dirty checking). `save()` is still needed for **new** entities (to persist them).
**Why it matters / when I used it:** `CategoryService.save` changes the name, rule
and apartments of a loaded category; the UPDATE happens at commit even without
`save()`. (The method calls `save()` for both cases, which is harmless for a managed
entity, and needed for a new one.) Dirty checking compares against the state taken
at load time and writes **only what changed**: saving a category unchanged produces
no UPDATE, and for a collection only the changed rows are written.

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

### View name resolution
**What:** A controller returns a **view name**; Thymeleaf turns it into a file path:
`templates/` + view name + `.html`.
**Why it matters / when I used it:** `return "admin/users"` renders
`templates/admin/users.html`; `"admin/user-form"` → `templates/admin/user-form.html`.
The string and the file location must match — renaming a template means changing the
controller too (done when the user templates moved from `admin/users/list.html` /
`new.html` to the flat `admin/` convention).

### View-model record
**What:** A record built for **one page**, holding exactly what the template needs,
instead of handing it an entity graph full of lazy proxies.
**Why it matters / when I used it:** `ResidentAccount(firstName, balance, debts,
payments)` is built by `ResidentAccountService.loadAccount` inside one read-only
transaction. The template reads `firstName` and `balance.apartmentName` (plain
values) instead of `user.apartment.name` (a lazy proxy); the debt list comes from a
`join fetch` query, so everything the page touches is loaded before the session
closes.

### Thymeleaf (server-side rendering)
**What:** A template engine that produces the final HTML on the server before
sending it to the browser.
**Why it matters / when I used it:** Renders `apartments.html` with real data. Unlike
a client-side SPA, the page arrives complete — closest analogue to PHP echoing HTML,
which eases the transition.

### DTO / form object
**What:** A plain class shaped like the form (Data Transfer Object), separate from
the entity. The form binds to it; the controller then builds the entity.
**Why it matters / when I used it:** Forms bind to `BillForm` / `GasBillForm`
(package `dto`), not to `Bill`:
- Form shape ≠ entity shape: the form sends a `categoryId`, the entity holds a
  `Category`.
- Mass-assignment protection: binding to the entity would let a crafted POST set
  any field (`id`, `splitRule`, …); the DTO has only the fields the form offers.
- Input rules (validation, messages) live with the input, not on the entity.

A class with getters/setters, not a `record`: the GET handler needs an **empty**
instance for the blank form, and Spring fills it field by field through setters.

### Bean Validation
**What:** Declarative input rules as annotations. Jakarta Bean Validation is the
spec; Hibernate Validator implements it; `spring-boot-starter-validation` brings it
in.
**Why it matters / when I used it:** On the form DTOs:
- `@NotNull` on every field.
- `@Digits(integer = 8, fraction = 2)` on money — matches `precision = 10,
  scale = 2` in the DB.
- `@Positive` on `meterTotal` (it is a divisor), `@PositiveOrZero` on `meterFloor`.
- **No** `@Positive` on money: credit bills are negative.

Validation expresses domain rules, so each annotation needs a domain reason.

### @Valid + BindingResult
**What:** `@Valid` triggers validation of the bound object; the errors land in the
`BindingResult` instead of throwing.
**Why it matters / when I used it:** In both admin controllers. Rules:
`BindingResult` must come **directly after** the `@Valid` parameter (otherwise
Spring throws instead). On errors, **return the view** (not a redirect), so the
entered values and the messages stay on the page.
`bindingResult.reject("billError", message)` adds a global (not field) error — used
when `BillService` throws `IllegalArgumentException`.

### Post/Redirect/Get (PRG)
**What:** After a successful POST, answer with a redirect; the browser then does a
GET.
**Why it matters / when I used it:** Both forms return `redirect:…` on success. F5
then repeats the harmless GET, not the POST — the same bill cannot be submitted
twice. Same idea as idempotency.

### Flash attributes
**What:** Model attributes that survive exactly **one** redirect (stored in the
session, removed after the next request).
**Why it matters / when I used it:**
`redirectAttributes.addFlashAttribute("successMessage", …)` shows "Καταχωρίστηκε…"
on the page after the redirect, and it disappears on refresh.
`AdminUserController` uses the same mechanism to show a new resident's **temporary
password exactly once**: after the redirect the value is gone from the session, and
it never appears in a URL. A query parameter (`?password=…`) would end up in browser
history, server logs and `Referer` headers.

### @ModelAttribute on a method
**What:** A controller method annotated with `@ModelAttribute("name")` runs before
**every** handler of that controller and adds its return value to the model.
**Why it matters / when I used it:** `AdminGasBillController` adds the
`categories` and `apartments` dropdown data once this way, so the GET, the error
paths and the POST all have it without repeating `model.addAttribute`.
(`AdminBillController` still adds `categories` by hand in each path.)

### @RequestMapping on the class
**What:** A common URL prefix for all handlers of a controller; method mappings are
relative to it.
**Why it matters / when I used it:** `AdminBillController` is
`@RequestMapping("/admin/bills")`: `@GetMapping` → `/admin/bills`,
`@GetMapping("/new")` → `/admin/bills/new`, `@PostMapping` → POST `/admin/bills`.

### Thymeleaf forms
**What:** The form-related Thymeleaf syntax used in the admin templates:
- `th:object="${billForm}"` + `*{…}` — selection expressions relative to the form
  object.
- `th:field="*{amount}"` — sets the input's `name`, `id` and `value` in one go
  (for a `<select>`, it also marks the bound option as `selected`). The templates
  still write `id` explicitly — on `<select>`s too — because the IDE only sees the
  raw template and needs a literal `id` to resolve `<label for="…">`.
- `#fields.hasErrors('amount')` / `th:errors="*{amount}"` — field errors;
  `#fields.globalErrors()` — errors from `bindingResult.reject`.
- `@{/admin/bills}` — context-relative URLs. `th:action="@{…}"` also inserts the
  CSRF token as a hidden field (see *CSRF*).
- `#temporals.format(date, 'dd/MM/yyyy')`, `#numbers.formatDecimal(…)` — date and
  number formatting.
- `th:with="bill=${entry.key}"` — a local variable.
- Iterating a `Map`: `th:each="entry : ${billsWithDebts}"` gives `entry.key` and
  `entry.value`.

**Why it matters / when I used it:** `bill-form.html`, `gas-bill-form.html`,
`bills.html`.

### @RequestParam and query strings
**What:** `@RequestParam` binds a query-string parameter (`?apartmentId=2`) to a
handler argument; `required = false` makes it optional (`null` when absent).
Thymeleaf builds such links with `@{/path(name=${value})}`.
**Why it matters / when I used it:** The balances page links each row to
`@{/admin/payments/new(apartmentId=${row.apartmentId()})}`;
`AdminPaymentController.showForm(@RequestParam(required = false) Long apartmentId, …)`
uses it to prefill the form, and still works when opened without it.

### Prefilling a form
**What:** Whatever is set on the form object before the view renders shows up in
the inputs, because `th:field` reads the values from it.
**Why it matters / when I used it:** `showForm` sets today's date, the apartment and
(only if it owes) the current balance on a new `PaymentForm` — the admin usually
just clicks "Καταχώριση".

### @Positive / @PastOrPresent as domain rules
**What:** Bean Validation constraints: `@Positive` = greater than 0;
`@PastOrPresent` = a date today or earlier.
**Why it matters / when I used it:** On `PaymentForm`: a payment is money received,
so its amount is positive (unlike bill amounts, which may be negative); and a
payment can't have happened in the future.

### @PathVariable vs @RequestParam
**What:** `@PathVariable` reads a part of the path (`/admin/categories/3/edit` →
`id = 3`); `@RequestParam` reads the query string (`?apartmentId=1`).
**Why it matters / when I used it:** The path **identifies the resource** (which
category is edited); the query string carries **optional input** (which apartment
to prefill on the payment form).

### Id of the edited record comes from the URL
**What:** On edit, the record's id is taken from the URL (`POST
/admin/categories/{id}`), never from a hidden form field.
**Why it matters / when I used it:** `CategoryForm` has no `id` field. A hidden id
is just more user input that could be changed to edit another record; the URL makes
the target explicit, and is where authorization checks will attach later.

### One template and one form for create and edit
**What:** Create and edit share `category-form.html` and `CategoryForm`; only the
POST target differs: `POST /admin/categories` (create) vs
`POST /admin/categories/{id}` (update).
**Why it matters / when I used it:** The template switches title and `th:action`
on whether `categoryId` is in the model. Both endpoints call one private `save`
method in the controller, so validation and redirect logic exist once.

### ResponseStatusException for a missing record
**What:** `throw new ResponseStatusException(HttpStatus.NOT_FOUND)` makes Spring
answer with that HTTP status.
**Why it matters / when I used it:** `/admin/categories/999/edit` returns **404**
(the resource doesn't exist), not a 500 (server error) from an unhandled exception.

### rejectValue vs reject
**What:** `bindingResult.rejectValue("name", code, message)` attaches an error to a
**field** (shown next to it with `th:errors="*{name}"`);
`bindingResult.reject(code, message)` adds a **global** error for the whole form.
**Why it matters / when I used it:** The duplicate category name is a field error
(`rejectValue("name", "duplicate", …)`); service failures on the bill forms are
global errors (`reject("billError", …)`).

### Cross-field validation, @NotBlank, normalizing input
**What:**
- Rules that involve two fields go on a `boolean isXxx()` method annotated with
  `@AssertTrue`; Bean Validation treats it as a property (`xxx`).
- `@NotBlank` (strings: not null, not empty, not only whitespace) vs `@NotNull`
  (any type: just not null).
- Input can be normalized in the setter: `this.name = name == null ? null :
  name.strip();`.

**Why it matters / when I used it:** `CategoryForm.isMeterBasedWithTwoApartments()`
checks "METER_BASED ⇒ exactly 2 apartments"; its error is shown with
`th:errors="*{meterBasedWithTwoApartments}"`. The name is `@NotBlank` and stripped,
so `" Απολύμανση "` can't create a near-duplicate.

### Thymeleaf: checkboxes, enum options, path templates, projection
**What:**
- Checkboxes bound to a `Set<Long>`: `th:field="*{apartmentIds}"` +
  `th:value="${apartment.id}"` checks the boxes whose id is in the set. `th:field`
  also renders a hidden `_apartmentIds` marker, so "none selected" arrives as an
  empty set instead of no value. `#ids.prev('apartmentIds')` gives the id generated
  for the previous checkbox, for its `<label th:for>`.
- Enum options: `th:value="${rule}"` (the constant name, bound back to the enum) and
  `th:text="${rule.label}"`.
- Path templates: `@{/admin/categories/{id}/edit(id=${category.id})}` fills `{id}`.
- SpEL projection `category.apartments.![name]` turns a collection into a list of
  one field; `#strings.listJoin(list, ', ')` joins it.

**Why it matters / when I used it:** `category-form.html` and `categories.html`.

---

## 4. Configuration & Tooling

### @Configuration and @Bean
**What:** A `@Configuration` class holds `@Bean` methods; Spring calls each once and
registers the return value as a bean. Used for objects we don't write ourselves
(library classes), so we can't put `@Component` on them.
**Why it matters / when I used it:** `SecurityConfig` declares the
`SecurityFilterChain` (the access rules) and the `PasswordEncoder`
(`BCryptPasswordEncoder`) as beans; `UserService` and `AdminSeeder` then get the
encoder injected.

### @ConfigurationProperties
**SETTING.** **What:** Binds a group of properties with a common prefix to a typed
object. `@ConfigurationPropertiesScan` on the main class registers every such class.
Relaxed binding: `app.admin.email` in a `.properties` file and the environment
variable `APP_ADMIN_EMAIL` fill the same field.
**Why it matters / when I used it:** `AdminProperties` is a record with
`@ConfigurationProperties(prefix = "app.admin")` (`email`, `password`); `AdminSeeder`
gets it injected instead of reading strings with `@Value`. Locally the values live
in the git-ignored `application-local.properties`; in production they can come from
environment variables without changing code.

### ddl-auto
**SETTING.** **What:** Hibernate's schema-management mode
(`spring.jpa.hibernate.ddl-auto`): `none`, `validate`, `update`, `create`,
`create-drop`.
**Why it matters / when I used it:** Set to `update` locally so the schema follows
entity changes without manual SQL. Pitfall: `update` only **adds** missing
tables/columns; it never alters or drops existing ones (changed nullability, type,
renamed/removed fields stay as they were). After such a change, reset the DB in dev
(see RUNBOOK.md); in prod, write a migration. The same goes for constraints (see
*Hibernate-generated constraints*). Interview/fintech caveat: never `update` in
production — use `validate` plus real migrations (Flyway/Liquibase) so schema changes
are reviewed and versioned.

### Spring Profiles
**SETTING.** **What:** Named sets of configuration (`local`, `prod`, …) activated via
`spring.profiles.active`; `application-<profile>.properties` overrides the base file.
**Why it matters / when I used it:** `local` is active by default and holds the DB
connection in `application-local.properties`, keeping env-specific values out of the
shared config.
- `application.properties` — applies to **every** environment (app name, encoding,
  `open-in-view=false`, the active profile).
- `application-local.properties` — **development only**, git-ignored: DB
  connection, `ddl-auto=update`, SQL logging, the seeded admin. Its committed
  template is `application-local.properties.example`.

Pending: `spring.profiles.active=local` sits in `application.properties`, i.e. inside
the jar; profile selection should move out of it (run configuration / env var)
before deploying.

### SQL logging
**SETTING.** **What:** Two ways to see the SQL Hibernate runs:
`spring.jpa.show-sql=true` prints straight to **stdout** (no timestamp, no thread,
can't be filtered); `logging.level.org.hibernate.SQL=debug` sends it through the
**logger**, with timestamp and thread name, and can be set per profile.
`spring.jpa.properties.hibernate.format_sql=true` pretty-prints it on several lines.
**Why it matters / when I used it:** Using both printed every query twice, so
`show-sql` was removed; `application-local.properties` keeps the logger setting and
`format_sql`. Production simply doesn't set it.

### Reading thread names in logs
**TOOLING.** **What:** Each log line shows the thread that wrote it; Tomcat serves each
request on a worker thread named like `http-nio-8080-exec-3`.
**Why it matters / when I used it:** All SQL lines with the same `exec-N` (close in
time) belong to the same request, so you can count the queries of one page load and
spot an N+1. Startup work (seeders) runs on `main`.

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
`<argLine>-javaagent:${org.mockito:mockito-core:jar} -Xshare:off</argLine>`.
IntelliJ picks the setting up after a Maven sync.
`-Xshare:off` turns off **CDS** (Class Data Sharing, a JVM startup cache of core
classes). A `-javaagent` appends to the bootstrap classpath, which CDS can't share,
so the JVM printed "Sharing is only supported for boot loader classes…". Disabling
CDS for the test JVM silences it; the cost is a slightly slower test startup.

### IDE false positives
**TOOLING.** **What:** An IDE inspection that reports an error the compiler or the
framework does not.
**Why it matters / when I used it:** IntelliJ's "Query language checks" mis-typed
the `SUM` subqueries in `findAllBalances`. Hibernate validates every `@Query` at
startup, so a broken query would stop the app anyway; the inspection was lowered to
**Warning** instead of disabled, so it still helps elsewhere. Rule: when the IDE and
the running app disagree, the running app wins.

### CI / GitHub Actions
**TOOLING.** **What:** Continuous Integration = every change is built and tested
automatically on a clean machine. In GitHub Actions a **workflow** (a YAML file in
`.github/workflows/`) contains **jobs**; each job runs a list of **steps** on a fresh
runner.
**Why it matters / when I used it:** `.github/workflows/ci.yml` runs on every push and
pull request to `master`: check out → set up JDK 21 → `./mvnw -B verify` (unit +
integration tests). "Works on my machine" stops counting — a missing file, an
uncommitted change or a test that depends on local state fails in CI.

### Runner
**TOOLING.** **What:** The machine a job runs on. `runs-on: ubuntu-latest` gives a
**throwaway** Linux VM, created for the job and destroyed afterwards.
**Why it matters / when I used it:** It has **Docker preinstalled**, so Testcontainers
starts PostgreSQL there with no extra setup. Nothing survives between runs, so every
run starts from the committed code only (no `application-local.properties`, no local
DB).

### setup-java with cache: maven
**TOOLING.** **What:** The `actions/setup-java` step installs a JDK and sets
`JAVA_HOME`. `cache: maven` saves `~/.m2/repository` after the run and restores it
next time; the cache key is a hash of `pom.xml`.
**Why it matters / when I used it:** `distribution: temurin`, `java-version: '21'` —
the same JDK as locally. With the cache, dependencies are downloaded only when
`pom.xml` changes, so runs are faster.

### Maven wrapper and -B (batch mode)
**TOOLING.** **What:** `./mvnw` downloads and runs the Maven version pinned in
`.mvn/wrapper/maven-wrapper.properties`, so every machine uses the same Maven. `-B`
(batch mode) turns off interactive prompts, download progress bars and colors.
**Why it matters / when I used it:** CI runs `./mvnw -B verify`: no Maven install
needed on the runner, the same version as locally, and logs without thousands of
progress lines.

### Least-privilege workflow permissions
**TOOLING.** **What:** Each workflow gets a `GITHUB_TOKEN`; `permissions:` limits what
it may do with the repository.
**Why it matters / when I used it:** `ci.yml` sets `permissions: contents: read` — the
build only needs to read the code. If a step or a third-party action were
compromised, it couldn't push code or change the repo.

### concurrency, cancel-in-progress and timeout-minutes
**TOOLING.** **What:** `concurrency.group` puts runs in a group (here
`ci-${{ github.ref }}`, one per branch); with `cancel-in-progress: true`, a new run
cancels the older one still running in the same group. `timeout-minutes` kills a job
that runs too long.
**Why it matters / when I used it:** Two quick pushes don't build both — only the
latest matters. `timeout-minutes: 15` stops a hanging run (e.g. a stuck container)
instead of using up the default 6 hours of runner time.

### Executable bit in git
**TOOLING.** **What:** Git stores a file mode per file: `100644` (normal) or `100755`
(executable). Windows has no executable bit, so with `core.fileMode=false` git ignores
mode changes locally — but it still stores and checks out the recorded mode on Linux.
**Why it matters / when I used it:** `mvnw` must be `100755`, or `./mvnw` fails on the
Linux runner with "Permission denied". Fixed once with
`git update-index --chmod=+x mvnw` (check with `git ls-files -s mvnw`).

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
integration test runs the **whole app wired together** — Spring context, security
filter chain, real database, template rendering. The test pyramid: many fast unit
tests at the bottom, fewer slower integration tests above.
**Why it matters / when I used it:** The calculator, `BillService` and `UserService`
tests are unit tests. `SecurityIT` and `ResidentAccountIT` are integration tests:
they catch what mocks can't — wrong security rules, broken SQL/JPQL, a missing
`join fetch` (`LazyInitializationException` in a template), template errors, bad
configuration. Limit of unit tests: with mocked repositories they cannot prove that a
rollback happened — there is no real transaction.

### Arrange – Act – Assert, @Test, @BeforeEach
**What:** Each test has three parts: set up the input (Arrange), call the code
(Act), check the result (Assert). `@Test` marks a test method; `@BeforeEach` runs
before **every** test, so each test starts from fresh objects (test isolation — no
test depends on another's leftovers).
**Why it matters / when I used it:** All test classes build their calculator and
apartments in `@BeforeEach`. Test classes and methods are package-private (JUnit 5
doesn't need `public`), and test names describe behavior, e.g.
`givesLeftoverCentToLowestIdApartment` or `createResident_rejectsDuplicateEmail`.

### One test per decision path
**What:** Each branch of a method (each `if` that returns, throws or saves
differently) gets its own test, so a failing test points at exactly one rule.
**Why it matters / when I used it:** `UserServiceTest` has one test for the success
path (normalized email, encoded password, role, apartment) and one per rejection
(duplicate email, apartment already taken). The rejection tests also check
`verify(userRepository, never()).save(any())` — the important behavior is that
nothing was stored.

### AssertJ
**What:** A fluent assertion library: `assertThat(actual).…`.
**Why it matters / when I used it:**
- `assertThatThrownBy(...)` — check the exception type **and** its message, so the
  test fails if the code throws for the wrong reason.
- `isEqualByComparingTo("40.00")` for `BigDecimal` — `equals()` also compares the
  scale, so `40.0` ≠ `40.00`; `compareTo` compares only the value.
- `extracting(Debt::getApartment)`, `allSatisfy(...)` — check a property across a
  whole list.
- `isSameAs(...)` vs `isEqualTo(...)` — `isSameAs` checks the **same instance**
  (`==`), `isEqualTo` checks `equals()`. Entities here have no `equals()`, so for
  them `isEqualTo` also falls back to identity; `isSameAs` states the intent: "the
  service saved exactly the apartment it loaded" (`UserServiceTest`).

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
`UnnecessaryStubbingException`, which keeps tests free of dead setup. E.g. in
`UserServiceTest.createResident_rejectsDuplicateEmail` the code throws before
loading the apartment, so stubbing `apartmentRepository.findById` there would fail
the test — the test only stubs what that path really uses. Unstubbed methods return
defaults (`false`, `null`, empty `Optional`), which is why the success test needs no
stub for `existsByEmail`.

### ArgumentCaptor / @Captor
**What:** Captures an argument passed to a mock so the test can inspect it.
**Why it matters / when I used it:** The `List<Debt>` is built **inside**
`registerBill` and only handed to `debtRepository.saveAll`; the captor is the only
way to see it. `@Captor` (instead of `ArgumentCaptor.forClass`) keeps the generic
type `ArgumentCaptor<List<Debt>>` without an unchecked cast. `UserServiceTest` uses
`ArgumentCaptor.forClass(AppUser.class)` (no generics needed) to inspect the user
passed to `save` — the only way to check that the stored password is the hash, not
the raw value.

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

### Testcontainers
**What:** A library that starts real services in throwaway Docker containers for the
duration of a test run, and removes them afterwards.
**Why it matters / when I used it:** `TestcontainersConfig` starts **PostgreSQL 17**
(the same major version as `docker-compose.yml`) for the integration tests. Why not
an in-memory H2: H2 speaks a different SQL dialect and enforces constraints
differently, so a test could pass on H2 and fail on PostgreSQL. With the real engine,
JPQL, the `SUM` subqueries, CHECK and unique constraints behave exactly as in
production. Needs Docker running.

### @ServiceConnection
**What:** On a container `@Bean`, it tells Spring Boot to read the connection details
(JDBC URL, username, password) **from the container** and configure the datasource
with them.
**Why it matters / when I used it:** `TestcontainersConfig.postgres()` is
`@Bean @ServiceConnection PostgreSQLContainer`. No `@DynamicPropertySource` and no
`spring.datasource.*` in the test properties: the container picks a random port, and
Boot wires it automatically.

### Spring test context caching
**What:** Spring caches the application context between test classes. Classes with
**identical** configuration (same annotations, profiles, imports, mocked beans)
reuse one context; any difference creates a new one.
**Why it matters / when I used it:** Starting a context and a PostgreSQL container
takes seconds. `IntegrationTestBase` puts the whole configuration (`@SpringBootTest`,
`@AutoConfigureMockMvc`, `@ActiveProfiles("test")`, `@Import(TestcontainersConfig)`)
in one place, so every `*IT` class shares **one context and one container per run**.
A per-class annotation (e.g. a `@MockitoBean`) would break the cache.

### MockMvcTester
**What:** The AssertJ-style API for MockMvc: `assertThat(mvc.get().uri("/my"))
.hasStatusOk().bodyText().contains(…)`. MockMvc calls the app **without a real
server**, but the request still goes through the real filter chain (security, CSRF),
controller, service, DB and Thymeleaf rendering.
**Why it matters / when I used it:** `IntegrationTestBase` injects it (enabled by
`@AutoConfigureMockMvc`). `SecurityIT` checks redirects/403s; `ResidentAccountIT`
checks the rendered page text — e.g. `"103,45 €"` and `"(οφειλή)"`.

### @WithMockUser vs @WithUserDetails
**What:** Both put an authenticated user in the `SecurityContext` for a test.
`@WithMockUser` creates a **fake** principal (default user `user`, `ROLE_USER`; or
`roles = "ADMIN"`) without touching the DB. `@WithUserDetails("email")` loads a
**real** user through the app's `UserDetailsService`.
**Why it matters / when I used it:** `SecurityIT` uses `@WithMockUser` for pure
access rules. `ResidentAccountIT` uses `@WithUserDetails` because `/my` looks the user
up by email in the DB. `setupBefore = TestExecutionEvent.TEST_EXECUTION` is needed:
by default the user is loaded **before** `@BeforeEach`, i.e. before the test has
created it.

### Surefire vs Failsafe
**SETTING.** **What:** Two Maven plugins that run tests. **Surefire** runs `*Test`
classes in the `test` phase (`./mvnw test`). **Failsafe** runs `*IT` classes in the
`integration-test` / `verify` phases (`./mvnw verify`), and fails the build only
after cleanup.
**Why it matters / when I used it:** Unit tests stay fast and need no Docker; the
slower `*IT` tests run on `verify`. Both plugins get the same Mockito-agent `argLine`
in `pom.xml`. Note: `./mvnw package` stops before `integration-test`, so it runs only
the unit tests.

### No @Transactional on integration tests
**What:** `@Transactional` on a test wraps each test in a transaction that is rolled
back at the end — a popular way to clean up.
**Why it matters / when I used it:** Not used here. With MockMvc the request runs on
the test's thread, so the test transaction would stay **open through the controller
and the template rendering**: lazy relations would load silently and a missing
`join fetch` would never throw `LazyInitializationException` — re-creating Open
Session in View in tests only. Instead, `IntegrationTestBase` cleans up in
`@BeforeEach` with `TRUNCATE apartment, category CASCADE` (CASCADE also empties every
table that references them), and each test builds its own data.

---

## 7. Security

### Authentication vs Authorization
**What:** Authentication = **who you are** (prove identity: email + password).
Authorization = **what you may do** (which pages/actions that identity can reach).
**Why it matters / when I used it:** The login form authenticates; the rule
`requestMatchers("/admin/**").hasRole("ADMIN")` in `SecurityConfig` authorizes.
Not logged in → redirect to `/login` (401-type failure); logged in as a resident and
opening `/admin/…` → 403 Forbidden.

### Security filter chain
**What:** Every request passes through an ordered chain of servlet filters (CSRF
check, session → user, login/logout handling, authorization) **before** it reaches
a controller. Any filter can stop the request.
**Why it matters / when I used it:** The `SecurityFilterChain` bean in
`SecurityConfig` configures it: `authorizeHttpRequests` (rules), `formLogin` (the
default login page), `logout`. Controllers never check access themselves — a
request to `/admin/bills` that isn't from an ADMIN never reaches
`AdminBillController`.

### CSRF (Cross-Site Request Forgery)
**What:** Another site makes the victim's browser send a request to our app; the
browser attaches our session cookie automatically, so it looks legitimate. Defense:
every state-changing request (POST) must carry a secret token that only our pages
know.
**Why it matters / when I used it:** Spring Security enables CSRF protection by
default. Forms rendered with `th:action="@{…}"` get a hidden `_csrf` input
automatically. A form with a plain `action="/admin/bills"` has no token → the POST
is rejected with **403**.

### BCrypt and salt
**What:** BCrypt is a password-hashing function that is **deliberately slow** and
**adaptive** (a cost factor makes it slower as hardware gets faster). A random
**salt** is generated per password and stored inside the hash string
(`$2a$10$<salt><hash>`).
**Why it matters / when I used it:** `BCryptPasswordEncoder` stores only the hash
in `app_user.password`; login re-hashes the typed password with the stored salt and
compares. Slow = brute-forcing stolen hashes is expensive; salt = two users with the
same password get different hashes, and precomputed (rainbow) tables are useless.
Passwords are never stored in plain text and can't be "decrypted".

### UserDetailsService
**What:** The Spring Security interface that loads a user by username
(`loadUserByUsername`) and returns a `UserDetails` (username, password hash,
authorities).
**Why it matters / when I used it:** `AppUserDetailsService` is the bridge between
our `app_user` table and Spring Security: the **email** is the username; it looks up
the normalized email and builds a `User` with the BCrypt hash and the role. Spring
Security finds the bean (plus the `PasswordEncoder` bean) and does the password
check itself.

### ROLE_ prefix
**What:** Spring Security works with **authorities** (strings). `.roles("ADMIN")`
stores the authority `ROLE_ADMIN`; `hasRole("ADMIN")` checks for `ROLE_ADMIN`
(adds the prefix); `hasAuthority("ROLE_ADMIN")` checks the exact string.
**Why it matters / when I used it:** `AppUserDetailsService` uses
`.roles(user.getRole().name())`, `SecurityConfig` uses `hasRole("ADMIN")`, and
`HomeController` compares `getAuthority()` to `"ROLE_ADMIN"` — with the prefix,
because there it reads the raw authority string.

### SecurityContext
**What:** Holds the `Authentication` (the logged-in user and its authorities) for the
current request. With session-based login it is stored in the HTTP session (the
`JSESSIONID` cookie identifies it) and restored on every request.
**Why it matters / when I used it:** `HomeController` receives the `Authentication`
as a method parameter and redirects by role: ADMIN → `/admin/apartments`, otherwise
→ `/my`.

### @AuthenticationPrincipal
**What:** Injects the logged-in user (the principal from the `SecurityContext`) into
a controller method parameter.
**Why it matters / when I used it:** `ResidentController.myAccount(@AuthenticationPrincipal
UserDetails principal, …)` gets the email via `principal.getUsername()` and loads the
`AppUser` from it — no session code, no parameter from the request.

### IDOR (Insecure Direct Object Reference)
**What:** A user reaches someone else's data by changing an id in the URL or form
(`/my?apartmentId=3`) because the server trusts it.
**Why it matters / when I used it:** `/my` takes **no** apartment id from the
request: the apartment comes from the authenticated user
(`user.getApartment().getId()`), so a resident can only ever see their own debts,
payments and balance.

### Logout via POST
**What:** Logout is a state-changing action, so it is a POST with a CSRF token, not
a GET link.
**Why it matters / when I used it:** A GET `/logout` could be triggered by any other
site (a link or `<img src=".../logout">`) and log the user out. The templates use a
small `<form th:action="@{/logout}" method="post">` with a button; Spring Security's
default logout only accepts POST when CSRF is on.

### User enumeration
**What:** An attacker learns which accounts exist from different error messages
("no such user" vs "wrong password").
**Why it matters / when I used it:** Spring Security's login shows the same generic
error for an unknown email and a wrong password, so the login page doesn't reveal
which emails are registered. (The "email already in use" message on
`/admin/users/new` is fine: only the admin sees it.)

### Mass assignment
**What:** Binding a request straight onto an object lets an attacker set fields the
form never showed (`role=ADMIN`, `id=…`).
**Why it matters / when I used it:** `ResidentForm` has only first name, last name,
email and apartment — **no `role`**. `UserService.createResident` always sets
`Role.RESIDENT`, so no POST can create an admin. See *DTO / form object*.

### SecureRandom vs Random
**What:** `java.util.Random` is a fast, **predictable** generator: from a few
outputs its future values can be computed. `java.security.SecureRandom` is
cryptographically strong.
**Why it matters / when I used it:** `UserService.generatePassword()` uses
`SecureRandom` for the temporary passwords. Rule: anything security-related
(passwords, tokens, reset links) uses `SecureRandom`.

### TOCTOU and unique constraints
**What:** Time-Of-Check to Time-Of-Use: a check and the action that depends on it
are separate steps, so the state can change in between. "Check, then save" is not
atomic.
**Why it matters / when I used it:** `UserService.createResident` checks
`existsByEmail` and `existsByApartmentId`, then saves. Two simultaneous requests
could both pass the checks. The DB unique constraints on `app_user.email` and
`app_user.apartment_id` are the last line of defense: the second insert fails. The
checks are there for a friendly error message; the constraint is what guarantees the
rule.
