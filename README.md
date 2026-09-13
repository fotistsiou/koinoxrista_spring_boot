# koinoxrista

A Spring Boot application for managing shared building expenses (κοινόχρηστα). It
replaces the Excel sheets a building owner used to split three expense categories
— electricity, gas, and disinfection — among apartments, track who owes what, and
email each resident their monthly balance. Built for personal use (the owner runs
it for their own building), not as a product for third parties.

## Tech stack

- Java 21
- Spring Boot 4.1
- PostgreSQL
- Spring Data JPA
- Thymeleaf
- Maven

## Setup and run locally

1. Have a local PostgreSQL instance running and create the database (connection
   settings live in `src/main/resources/application.properties`).
2. Build:
   ```bash
   ./mvnw clean package
   ```
3. Run the Spring Boot app:
   ```bash
   ./mvnw spring-boot:run
   ```

The app starts on `http://localhost:8080` by default.

## Project structure

Standard Maven/Spring Boot layout. Application code lives under `src/main/java`,
Thymeleaf templates and configuration under `src/main/resources`. See
[RUNBOOK.md](RUNBOOK.md) for operational commands.
