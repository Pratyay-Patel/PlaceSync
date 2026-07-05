# PlaceSync — Deployment Guide

## Database environments

PlaceSync uses two PostgreSQL environments that run in parallel throughout the project lifecycle. No code changes are required when switching between them — only configuration (environment variables) changes.

| Environment | Provider | Used for |
|---|---|---|
| Development / CI | Docker Compose (PostgreSQL 16 container) | Local development, feature implementation, integration testing, GitHub Actions CI |
| Production | Supabase (managed PostgreSQL) | Staging, production deployment, long-term data persistence |

---

## Supabase connection strings

Supabase exposes two distinct connection endpoints. **You must use the correct one for each use case** — using the wrong one will cause failures.

### Direct connection (port 5432)

**Use for: Flyway migrations only.**

Flyway requires a persistent, stateful connection because it holds locks on the schema history table during migration runs. PgBouncer (the pooler) operates in transaction mode, which breaks Flyway's advisory locks.

```
Host:     db.<project-ref>.supabase.co
Port:     5432
User:     postgres
Database: postgres
```

JDBC URL format:
```
jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require
```

### Transaction pooler (port 6543)

**Use for: the running Spring Boot application.**

PgBouncer's transaction pooling multiplexes many application connections over a small number of real database connections. This is essential for production — direct connections are limited on Supabase's free tier.

```
Host:     aws-1-<region>.pooler.supabase.com
Port:     6543
User:     postgres.<project-ref>
Database: postgres
```

JDBC URL format:
```
jdbc:postgresql://aws-1-<region>.pooler.supabase.com:6543/postgres
```

> Note: The transaction pooler username includes the project reference suffix: `postgres.<project-ref>`. This is different from the direct connection username (`postgres`).

---

## Environment variable mapping

### `.env.prod` (VPS / production server)

```properties
DATABASE_URL=jdbc:postgresql://aws-1-<region>.pooler.supabase.com:6543/postgres
DATABASE_USERNAME=postgres.<project-ref>
DATABASE_PASSWORD=<supabase-db-password>
```

### Flyway migration (one-time / on each deploy with new migrations)

Pass the direct connection string via Maven command-line properties — do not put it in `.env.prod`:

```powershell
$env:JAVA_HOME = "C:/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
mvn flyway:migrate `
  "-Dflyway.url=jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require" `
  "-Dflyway.user=postgres" `
  "-Dflyway.password=<supabase-db-password>"
```

**Never run `flyway:migrate` via the pooler (port 6543).** Flyway will fail with a lock error or silently corrupt migration history.

---

## Running Flyway migrations against Supabase

### First-time setup (fresh Supabase project)

1. Start Docker Compose so Redis and Kafka are available locally.
2. Run `mvn flyway:migrate` with the **direct** connection string (see command above).
3. Verify all migrations applied cleanly in the Supabase SQL Editor:

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

All rows must have `success = true`. The current baseline is **V001–V020** (20 migrations).

### On each subsequent deploy

If new Flyway migration files (e.g., `V021__...sql`) have been added since the last deploy:

```powershell
mvn flyway:migrate `
  "-Dflyway.url=jdbc:postgresql://db.<project-ref>.supabase.co:5432/postgres?sslmode=require" `
  "-Dflyway.user=postgres" `
  "-Dflyway.password=<supabase-db-password>"
```

Flyway detects which migrations have already been applied (via CRC32 checksum) and only runs new ones. Do not edit existing migration files after they have been applied — Flyway will throw a checksum mismatch error on next startup.

---

## Schema validation

After running migrations, verify the schema in the Supabase SQL Editor:

```sql
-- All tables in the public schema
SELECT table_name
FROM information_schema.tables
WHERE table_schema = 'public'
ORDER BY table_name;

-- All custom enum types (PlaceSync types only, excluding Supabase internals)
SELECT typname
FROM pg_type
WHERE typtype = 'e'
  AND typnamespace = (SELECT oid FROM pg_namespace WHERE nspname = 'public')
ORDER BY typname;
```

Expected: 18 application tables + `flyway_schema_history`, and 12 custom enum types.

---

## Local smoke test against Supabase

To verify the application connects to Supabase before a production deploy:

1. Ensure Docker Compose is running (Redis + Kafka must be up).
2. Run the following command, replacing placeholders with real values:

```powershell
$env:JAVA_HOME = "C:/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"
mvn spring-boot:run `
  "-Dspring-boot.run.profiles=dev" `
  "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:postgresql://aws-1-<region>.pooler.supabase.com:6543/postgres --spring.datasource.username=postgres.<project-ref> --spring.datasource.password=<supabase-db-password> --spring.mail.username=<gmail-address> --spring.mail.password=<gmail-app-password> --spring.kafka.bootstrap-servers=localhost:9092"
```

3. Confirm the application started: `GET http://localhost:8080/actuator/health`
   - `db` component must be `UP`
   - `redis` component must be `UP`
   - `mail` will be `UP` only if real Gmail credentials were provided

4. Register a test user: `POST http://localhost:8080/api/v1/auth/register`

5. Verify the user row appears in Supabase → **Table Editor** → `users`.

---

## Security notes

- `.env.prod` is git-ignored. **Never commit it.**
- `.env.example` contains only placeholders and is safe to commit.
- The direct connection string (port 5432) grants unrestricted database access — use it only for migration runs and never expose it in application configuration.
- The transaction pooler enforces row-level security policies set in Supabase; the direct connection bypasses them. All application queries must go through the pooler.
