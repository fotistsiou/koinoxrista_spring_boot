# koinoxrista

A Spring Boot application for managing shared building expenses (κοινόχρηστα). It
replaces the Excel sheets a building owner used to split expense categories —
electricity, gas, boiler maintenance, disinfection, and any others the admin adds —
among apartments, track who owes what, and email each resident their monthly
balance. Built for personal use (the owner runs it for their own building), not as a
product for third parties.

## Features

- Bills split per category (equal split or gas meters), debts per apartment,
  payments and live balances
- Session-based login (Spring Security, BCrypt passwords)
- Roles: ADMIN manages everything under `/admin/**`; RESIDENT sees only their own
  apartment
- Admin user management: create resident accounts with a one-time temporary password
- Resident account page (`/my`): own balance, debts and payments

## Tech stack

- Java 21
- Spring Boot 4.1
- PostgreSQL
- Spring Data JPA
- Spring Security
- Thymeleaf
- Maven
- Docker / Docker Compose (local PostgreSQL)

## Setup and run locally

1. Start PostgreSQL via Docker Compose. Copy `.env.example` to `.env` and set the
   same DB name / user / password that `src/main/resources/application-local.properties`
   expects, then:
   ```bash
   docker compose up -d
   ```
2. Build:
   ```bash
   ./mvnw clean package
   ```
3. Run the Spring Boot app (the `local` profile is active by default):
   ```bash
   ./mvnw spring-boot:run
   ```

The app starts on `http://localhost:8080` by default. The apartment listing is at
`/apartments`.

Admin pages:
- `/admin/apartments` — balance per apartment (debts, payments, balance)
- `/admin/categories` — categories with their split rule and apartments (create / edit)
- `/admin/bills` — all bills with their debts per apartment
- `/admin/bills/new` — register a plain bill (equal split)
- `/admin/gas-bills/new` — register the monthly gas bill (meter-based split)
- `/admin/payments/new` — register a payment
- `/admin/users` — user accounts; `/admin/users/new` creates a resident

Every page requires login; `/` redirects the admin to `/admin/apartments` and a
resident to `/my`. See [RUNBOOK.md](RUNBOOK.md#users-and-login) for the admin
account setup.

## Project structure

Standard Maven/Spring Boot layout. Application code lives under `src/main/java`,
Thymeleaf templates and configuration under `src/main/resources`. DB connection
settings live in `application-local.properties` (activated by the `local` profile).
See [RUNBOOK.md](RUNBOOK.md) for operational commands.

## Documentation

- [DOMAIN.md](DOMAIN.md) — entities, relationships, split rules, locked principles
- [GLOSSARY.md](GLOSSARY.md) — programming concepts learned while building the app
- [RUNBOOK.md](RUNBOOK.md) — local run, database, seed data, troubleshooting
