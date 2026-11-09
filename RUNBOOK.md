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

## Tests

From IntelliJ: click the green arrow next to a test class or method.

Run all tests:
```bash
./mvnw test
```
This includes `KoinoxristaApplicationTests` (`@SpringBootTest`), which starts the
full context and needs the Docker DB up (`docker compose up -d`). The same applies
to `./mvnw clean package`, which runs the tests too.

Run one class (the quotes are needed in PowerShell):
```bash
./mvnw test "-Dtest=BillServiceTest"
```

The JVM warning `Sharing is only supported for boot loader classes because
bootstrap classpath has been appended` is a side effect of loading the Mockito agent.
It is silenced by `-Xshare:off` in the surefire `argLine` in `pom.xml`; if it still
shows up in IntelliJ, reload the Maven project. It is harmless either way.

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
   If it is lost, the account has to be recreated (no password reset yet).

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
