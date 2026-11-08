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