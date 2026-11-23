# RUNBOOK

Practical commands for running and operating this app locally.

## Local run

Build:
```bash
./mvnw clean package
```

Start the app:
```bash
./mvnw spring-boot:run
```

Run the packaged jar instead (after `package`):
```bash
java -jar target/koinoxrista-0.0.1-SNAPSHOT.jar
```
The name is `<artifactId>-<version>.jar` from `pom.xml`. Use the exact name:
PowerShell passes `target/*.jar` to `java` literally instead of expanding it.

The app listens on `http://localhost:8080` by default.

## Running tests

From IntelliJ: click the green arrow next to a test class or method.

Unit tests only (`*Test`, Surefire) — fast, **no Docker needed**:
```bash
./mvnw test
```

Unit + integration tests (`*IT`, Failsafe):
```bash
./mvnw verify
```
Needs:
- **Docker running** — Testcontainers starts a throwaway PostgreSQL 17 container.
- **`JAVA_HOME` set.** On Windows, set it as a **user** environment variable (pointing
  to the JDK 21 folder) and restart the IDE/terminal so it picks it up.

The integration tests use **their own container database** (profile `test`,
schema `create-drop`, seeders disabled). They never touch the `docker-compose`
database, so local data is safe.

`./mvnw clean package` runs only the unit tests (it stops before the
integration-test phase).

Run one class (the quotes are needed in PowerShell):
```bash
./mvnw test "-Dtest=BillServiceTest"
./mvnw verify "-Dit.test=SecurityIT"
```

The JVM warning `Sharing is only supported for boot loader classes because
bootstrap classpath has been appended` is a side effect of loading the Mockito agent.
It is silenced by `-Xshare:off` in the surefire `argLine` in `pom.xml`; if it still
shows up in IntelliJ, reload the Maven project. It is harmless either way.

## CI

GitHub Actions runs `.github/workflows/ci.yml` (`./mvnw -B verify`: unit +
integration tests) on every push and pull request to `master`.

- **See runs:** the repository's **Actions** tab on GitHub → workflow "CI". Each
  commit also shows a ✓ / ✗ next to it.
- **Re-run:** open the run → **Re-run jobs** (top right) → "Re-run failed jobs" or
  "Re-run all jobs". Useful for a flaky failure (e.g. a Docker pull timeout).
- **On failure:** the Surefire and Failsafe reports (`target/surefire-reports/`,
  `target/failsafe-reports/`) are uploaded as the **`test-reports`** artifact,
  listed at the bottom of the run's summary page. Download it to see the failing
  test's full output. Artifacts are kept for **7 days**.
- A run is cancelled if a newer push to the same branch starts, and is killed after
  15 minutes.
- `./mvnw: Permission denied` in CI means `mvnw` lost its executable bit:
  ```bash
  git update-index --chmod=+x mvnw
  ```
  then commit (see GLOSSARY *Executable bit in git*).

## Database

PostgreSQL runs as a Docker container defined in `docker-compose.yml`. Its
credentials come from `.env` (copy from `.env.example`).

Start / stop the database:
```bash
docker compose up -d
docker compose down          # add -v to also wipe the data volume
```

App connection settings live in `src/main/resources/application-local.properties`
(git-ignored; copy it from `application-local.properties.example`)
(`spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`),
activated by the `local` profile (`spring.profiles.active=local` in
`application.properties`). The values there must match the `POSTGRES_*` values in
`.env`.

Connect with psql (via the running container):
```bash
docker exec -it koinoxrista-db psql -U <username> -d <database>
```

Check the container is up:
```bash
docker ps --filter name=koinoxrista-db
```

## Seed data

`ApartmentSeeder`, `CategorySeeder` and `AdminSeeder` (in that order) insert the
initial apartments, categories and the admin user on startup, but only when their
table is empty (`count() == 0`). Editing a seeder later does **not** change rows
that already exist.

To re-seed, delete the rows from that table (or wipe the whole database), then
restart the app:
```bash
docker compose down -v       # wipes the whole DB (data volume)
docker compose up -d
./mvnw spring-boot:run
```

## Users and login

### Admin account

`AdminSeeder` creates the admin on startup when `app_user` is empty, linked to the
apartment 1ος, with the password stored as a BCrypt hash. Credentials come from
`application-local.properties` (see `application-local.properties.example`):
```properties
app.admin.email=<admin-email>
app.admin.password=<admin-password>
```
or from the environment variables `APP_ADMIN_EMAIL` / `APP_ADMIN_PASSWORD`. If
either is missing, startup fails with "app.admin.email and app.admin.password must
be set".

> ⚠️ Write the admin email in **lowercase**. Login looks up the lowercased email,
> and `AdminSeeder` stores the value as given — an admin seeded as `Admin@…` cannot
> log in (fix below).

Changing these properties later does not change an existing admin (the seeder only
runs on an empty table); reset the DB or update the row.

### Resident accounts

1. Log in as the admin and open `/admin/users/new`.
2. Enter first name, last name, email and the apartment (only apartments without a
   user are listed).
3. After saving, `/admin/users` shows the **temporary password once**. Copy it and
   hand it to the resident — it is stored only as a hash and cannot be shown again.
   If it is lost, the account has to be recreated (there is no password reset).

### Inspect users and constraints

```bash
docker compose exec db psql -U <db-user> -d <db-name> -c "\d app_user"
docker compose exec db psql -U <db-user> -d <db-name> -c "select id, email, role from app_user"
```
`\d app_user` lists the unique constraints (`email`, `apartment_id`) and the CHECK
constraint on `role`.

### Fix an admin stored with an uppercase email

```bash
docker compose exec db psql -U <db-user> -d <db-name> -c "update app_user set email = lower(email) where role = 'ADMIN'"
```

## See the executed SQL

In `application-local.properties` (already in the `.example`):
```properties
logging.level.org.hibernate.SQL=debug
spring.jpa.properties.hibernate.format_sql=true
```
Don't also set `spring.jpa.show-sql=true` — every query would be printed twice.

Each SQL line carries the thread name. Tomcat handles each request on a thread like
`http-nio-8080-exec-3`, so the lines with the same `exec-N` (and close timestamps)
are the queries of **one** page load:
```text
... DEBUG 12345 --- [nio-8080-exec-3] org.hibernate.SQL : select ...
```
Open the page once and count them: more than a handful for a list page usually means
an N+1 (a relation that isn't fetched in the query). Seeders run on `main`.

## Reset the database

When an entity change alters an **existing** column (nullability, type, rename),
`ddl-auto=update` will not apply it — it only adds missing tables/columns. Reset
the database, then restart the app:
```bash
docker compose down -v
docker compose up -d
./mvnw spring-boot:run
```
All tables are recreated from the entities and the seeders run again.

> ⚠️ `down -v` deletes the data volume: **all data is lost**.

## Troubleshooting

App won't start — check these in order:

- **Database not running.** Start the Docker container and retry:
  ```bash
  docker compose up -d
  ```
  Confirm it's up with `docker ps --filter name=koinoxrista-db`.

- **Wrong credentials or database name.** The `spring.datasource.*` values in
  `application-local.properties` must match the `POSTGRES_*` values in `.env`. A
  `PSQLException` / `password authentication failed` in the startup log points
  here.

- **Port already in use.** If port 8080 is taken (`Web server failed to start.
  Port 8080 was already in use`), find and stop the process:
  ```powershell
  Get-NetTCPConnection -LocalPort 8080 | Select-Object OwningProcess
  Stop-Process -Id <pid>
  ```
  Or run on a different port:
  ```bash
  ./mvnw spring-boot:run -Dspring-boot.run.arguments=--server.port=8081
  ```

A page fails with `LazyInitializationException` (`could not initialize proxy … no
Session`):

- **Meaning.** A template (or code after the transaction) read a property through
  a lazy relation that the query didn't load. `open-in-view` is off and all to-one
  relations are LAZY, so there is no session left to load it.
- **Find it.** The stack trace names the template and expression (e.g.
  `user.apartment.name`); find the controller that renders that view and the
  repository method that loaded the root object.
- **Fix it there:** add `join fetch` to the `@Query` or `@EntityGraph(attributePaths
  = …)` to the repository method, or pass the template the plain value it needs
  (a view-model record built in a service). Don't turn `open-in-view` back on, put
  `@Transactional` on the controller, or enable
  `hibernate.enable_lazy_load_no_trans`.
