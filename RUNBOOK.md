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
java -jar target/*.jar
```

The app listens on `http://localhost:8080` by default.

## Database

PostgreSQL runs as a Docker container defined in `docker-compose.yml`. Its
credentials come from `.env` (copy from `.env.example`).

Start / stop the database:
```bash
docker compose up -d
docker compose down          # add -v to also wipe the data volume
```

App connection settings live in `src/main/resources/application-local.properties`
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
