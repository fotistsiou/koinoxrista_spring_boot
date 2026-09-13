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

Connection settings live in `src/main/resources/application.properties`
(`spring.datasource.url`, `spring.datasource.username`, `spring.datasource.password`).

Connect to local PostgreSQL with psql:
```bash
psql -h localhost -p 5432 -U <username> -d <database>
```

Check that PostgreSQL is running (Windows service):
```powershell
Get-Service -Name postgresql*
```

## Troubleshooting

App won't start — check these in order:

- **Database not running.** Start the PostgreSQL service and retry:
  ```powershell
  Start-Service postgresql*
  ```
  Confirm you can connect with the `psql` command above.

- **Wrong credentials or database name.** Compare `spring.datasource.*` in
  `application.properties` against the real database. A `PSQLException` /
  `password authentication failed` in the startup log points here.

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
