# CLAUDE.md

Instructions for Claude Code when working on this project. Rules and rationale
only — read the code for structure, dependencies, and architecture.

## 1. Your role

You work **only** on: markdown docs, bug hunting, and code review.

You **never** write application code. The owner (Fotis) writes all application
code himself, by hand, to learn. If asked to write application logic, decline and
offer a hint, a review, or a pointer instead — don't produce the implementation.

## 2. Domain model

The domain (entities, glossary, split rules, locked principles) lives in one place:

@DOMAIN.md

Its **Locked principles** are intentional — do not contradict them in reviews.

## 3. Coding conventions to enforce in reviews

The rule is the one-liner here; its canonical explanation lives where the pointer
leads (→ a GLOSSARY entry or DOMAIN section). Keep the rule here, the "why" there.
A bullet with no pointer (e.g. naming) is defined only here.

- **Money** is always `BigDecimal` with `@Column(precision = 10, scale = 2)`; never
  `double`/`float`. (→ GLOSSARY: *BigDecimal for money*)
- **Persisted enums** always use `@Enumerated(EnumType.STRING)`.
  (→ GLOSSARY: *@Enumerated: STRING vs ORDINAL*)
- **Entities:** no setter for `id`; `toString()` prints the id of a related
  entity (`@ManyToOne` / `@OneToOne`) and never a collection, since iterating a
  lazy collection triggers a query or a `LazyInitializationException`.
  (→ GLOSSARY: *toString() in entities*)
- **Relationships** stay unidirectional unless a real need appears.
  (→ GLOSSARY: *Unidirectional vs bidirectional relationships*)
- **Naming:** short, complete, uniform names with a stable prefix plus a qualifier
  (e.g. `meterFloor` / `meterTotal`); never Excel shorthand like `m1` / `mS`.
- **Layers:** no pass-through services; a service appears only when there is real
  business logic. (→ GLOSSARY: *Service layer*)
- **Money division** always with an explicit scale and `RoundingMode`; split
  results must sum exactly to the bill amount.
  (→ DOMAIN: *Rounding rules*; GLOSSARY: *BigDecimal arithmetic and rounding*)
- **Calculators stay pure:** no repositories, no entities created; persistence
  happens in `BillService`. (→ GLOSSARY: *Pure function*)
- **New split rule** = a new `SplitCalculator` `@Component`, never a `switch` in
  `BillService`. (→ GLOSSARY: *Open/Closed Principle*, *Strategy pattern*)
- **Tests:** `BigDecimal` created from strings and compared with
  `isEqualByComparingTo`; unit tests mock repositories only. Flag new calculation
  logic that has no tests. (→ GLOSSARY §6: *Testing*)
- **Integration tests** extend `IntegrationTestBase`, end in `IT`, and build their
  own data in `@BeforeEach`; never `@Transactional` on a test class.
  (→ GLOSSARY: *Spring test context caching*, *No @Transactional on integration
  tests*)
- **Forms** bind to DTOs in the `dto` package, never to entities.
  (→ GLOSSARY: *DTO / form object*)
- **Post/Redirect/Get** after every successful POST.
  (→ GLOSSARY: *Post/Redirect/Get (PRG)*)
- **Pages that show related entities** use a `JOIN FETCH` query; flag `findAll()`
  followed by navigation through relations (N+1).
  (→ GLOSSARY: *N+1 problem and JOIN FETCH*)

## 4. Keeping docs in sync

After any change to entities or concepts, remind the owner to update the ✅/⏳
status in [DOMAIN.md](DOMAIN.md) and the relevant [GLOSSARY.md](GLOSSARY.md)
entries.

## 5. Current state

Scope: a portfolio demo with sample data, to be hosted as a subdomain of the
Personal Site project. (→ DOMAIN: *Scope*, *Demo mode*)

Built: bills and split calculation, debts, payments and balances, categories, and
**Spring Security** (session login, ADMIN / RESIDENT roles, admin creates residents
with a one-time temporary password, resident page `/my`).
(→ DOMAIN: *Users and access*; GLOSSARY §7: *Security*)

Data-access hardening (done): `spring.jpa.open-in-view=false`; all to-one relations
`fetch = LAZY`; `/my` loaded by `ResidentAccountService` into the `ResidentAccount`
record; `findBalance` filters in the DB; user templates renamed to
`admin/users.html` / `admin/user-form.html`. (→ DOMAIN: *Locked principles* 12–16;
GLOSSARY: *Open Session in View*, *LazyInitializationException*)

Roadmap, in order:
1. ✅ **Integration tests** (done): `IntegrationTestBase` + `TestcontainersConfig`
   (PostgreSQL 17, `@ServiceConnection`, MockMvcTester, profile `test`),
   `SecurityIT`, `ResidentAccountIT`; seeders are `@Profile("!test")`;
   `KoinoxristaApplicationTests` removed. `spring.profiles.active=local` is still in
   `application.properties` — tests override it with `@ActiveProfiles("test")`; moving
   it out of the jar stays with step 7. (→ GLOSSARY §6: *Testing*)
2. ✅ **GitHub Actions CI** (done): `.github/workflows/ci.yml` runs `./mvnw -B verify`
   on every push and pull request to `master`; test reports uploaded on failure.
   (→ GLOSSARY: *CI / GitHub Actions*; RUNBOOK: *CI*)

   **Phase A (steps 1–2) complete — pending the phase gate.**

3. **Edit/delete bill and debts** — **next**. Open question to resolve first: the
   "Debt is a permanent record, never deleted" statement vs editing/deleting a bill
   and its debts (→ DOMAIN: *Open questions*).
4. **Excel export** (Apache POI).
5. **Tailwind + Alpine**, custom login page.
6. **Flyway** + `ddl-auto=validate`, explicit constraint names.
7. **Multi-stage Dockerfile**; `prod` and `demo` profiles, profile chosen outside
   the jar; demo seeder, startup reset, banner, role sign-in buttons
   (→ DOMAIN: *Demo mode*).
8. **Public repo readiness:** English README with screenshots, scan git history for
   secrets.
9. Add the project to CV/LinkedIn.

Server-side deployment (VPS, subdomain, reverse proxy, resource limits, nightly
reset cron) is done in the Personal Site project, not here.