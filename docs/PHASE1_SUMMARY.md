# Phase 1 Summary — Backend Foundation
# PlaceSync

**Phase:** 1 — Project Bootstrap & Infrastructure
**Date:** 2026-06-23
**Status:** Complete

---

## What Was Created

### 1. Maven Project (`pom.xml`)

Spring Boot 3.3.6 with Java 21. Dependencies added:

| Dependency | Scope | Purpose |
|---|---|---|
| `spring-boot-starter-web` | compile | REST API layer (Tomcat embedded) |
| `spring-boot-starter-data-jpa` | compile | Hibernate ORM + Spring Data repositories |
| `postgresql` | runtime | JDBC driver for PostgreSQL |
| `flyway-core` | compile | Database migration engine |
| `flyway-database-postgresql` | compile | Flyway 10+ PostgreSQL support module |
| `spring-boot-starter-validation` | compile | Jakarta Bean Validation (input validation) |
| `spring-boot-starter-actuator` | compile | Health and info endpoints |
| `lombok` | compile (optional) | Boilerplate reduction (excluded from final JAR) |
| `spring-boot-starter-test` | test | JUnit 5, Mockito, AssertJ |
| `h2` | test | In-memory DB for context load tests (no PostgreSQL needed in CI) |

### 2. Application Entry Point

`src/main/java/com/placesync/PlaceSyncApplication.java` — `@SpringBootApplication` main class.

### 3. Package Structure (13 packages)

All packages are established with `package-info.java` documentation files.

```
com.placesync
├── auth/           — JWT, refresh tokens, OAuth2, password management (Phase 2)
├── user/           — User lifecycle, admin management (Phase 2)
├── recruiter/      — Recruiter profiles, verification workflow (Phase 2)
├── company/        — Company profiles, admin approval (Phase 2)
├── job/            — Job postings, eligibility, search (Phase 3)
├── application/    — Application tracking, status lifecycle (Phase 3)
├── interview/      — Interview scheduling, rounds (Phase 3)
├── notification/   — In-app inbox, Kafka consumer (Phase 4)
├── analytics/      — Dashboard statistics, placement rate (Phase 5)
└── common/
    ├── config/     — Spring configuration beans (Phase 2+)
    ├── exception/  — Global error handling, custom exceptions (Phase 2)
    ├── util/       — Shared utilities (Phase 2+)
    └── audit/      — AOP audit logging, audit_log table (Phase 3)
```

### 4. Configuration Files

| File | Purpose |
|---|---|
| `src/main/resources/application.yml` | Base config: JPA, Flyway, Actuator, app info |
| `src/main/resources/application-dev.yml` | Dev profile: local DB defaults, verbose SQL logging |
| `src/main/resources/application-prod.yml` | Prod profile: required env vars, HikariCP tuned, minimal logging |
| `src/test/resources/application.yml` | Test profile: H2 in-memory, Flyway disabled |

**Key configuration decisions:**
- `spring.jpa.hibernate.ddl-auto=none` — DDL is owned entirely by Flyway
- `spring.jpa.open-in-view=false` — Prevents lazy-loading antipattern
- `spring.flyway.validate-on-migrate=true` — Detects local migration tampering
- Dev profile uses fallback defaults so the app starts without any `.env` on a fresh machine

### 5. Flyway Migration

`src/main/resources/db/migration/V001__create_enum_types.sql`

Creates all 11 custom PostgreSQL ENUM types and the shared `set_updated_at()` trigger function. No tables are created in V001 — they are created in V002–V019 in subsequent phases.

ENUMs defined: `user_role`, `verification_status`, `company_status`, `job_status`, `job_location_type`, `job_type`, `application_status`, `interview_type`, `interview_status`, `notification_type`, `gender_type`, `audit_action`.

### 6. Test

`src/test/java/com/placesync/PlaceSyncApplicationTests.java`

`@SpringBootTest` + `@ActiveProfiles("test")` — verifies the Spring application context loads successfully. Uses H2 in-memory with Flyway disabled, so no database is required in the CI pipeline at this stage.

### 7. Docker Support

| File | Purpose |
|---|---|
| `Dockerfile` | Multi-stage build: Maven compile stage + lean JRE runtime stage with non-root user |
| `.dockerignore` | Excludes `target/`, `.git/`, `.env`, `src/test/` from Docker build context |
| `docker-compose.yml` | Phase 1 stack: PostgreSQL 16 + Spring Boot API |
| `.env.example` | Environment variable template (all phases documented, future ones commented out) |

**Phase 1 Docker Compose services:**
- `postgres` — PostgreSQL 16 Alpine with health check
- `api` — Spring Boot app, waits for postgres health check before starting

**Not yet included** (added in later phases): Redis, Kafka, Nginx.

### 8. Project Housekeeping

| File | Purpose |
|---|---|
| `.gitignore` | Excludes `target/`, `.idea/`, `.env`, logs |
| `.env.example` | Documents all environment variables across all phases |

---

## File Tree

```
PlaceSync/
├── docs/
│   ├── SRS.md
│   ├── ARCHITECTURE.md
│   ├── DATABASE_DESIGN.md
│   └── PHASE1_SUMMARY.md               ← this file
├── src/
│   ├── main/
│   │   ├── java/com/placesync/
│   │   │   ├── PlaceSyncApplication.java
│   │   │   ├── auth/package-info.java
│   │   │   ├── user/package-info.java
│   │   │   ├── recruiter/package-info.java
│   │   │   ├── company/package-info.java
│   │   │   ├── job/package-info.java
│   │   │   ├── application/package-info.java
│   │   │   ├── interview/package-info.java
│   │   │   ├── notification/package-info.java
│   │   │   ├── analytics/package-info.java
│   │   │   └── common/
│   │   │       ├── config/package-info.java
│   │   │       ├── exception/package-info.java
│   │   │       ├── util/package-info.java
│   │   │       └── audit/package-info.java
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       ├── application-prod.yml
│   │       └── db/migration/
│   │           └── V001__create_enum_types.sql
│   └── test/
│       ├── java/com/placesync/
│       │   └── PlaceSyncApplicationTests.java
│       └── resources/
│           └── application.yml
├── .dockerignore
├── .env.example
├── .gitignore
├── docker-compose.yml
├── Dockerfile
├── pom.xml
└── README.md
```

---

## What Is NOT Yet Implemented

Everything listed below is intentionally deferred to later phases:

- Spring Security (Phase 2)
- JWT / refresh tokens (Phase 2)
- Google OAuth2 (Phase 2)
- All JPA entities (Phase 2+)
- All Spring Data repositories (Phase 2+)
- All REST controllers (Phase 2+)
- All service classes (Phase 2+)
- Redis caching (Phase 3)
- Apache Kafka (Phase 4)
- AWS S3 integration (Phase 5)
- Spring Mail / email delivery (Phase 5)
- Nginx reverse proxy configuration (Phase 6)
- GitHub Actions CI/CD pipeline (Phase 6)

---

## Suggested Commit Message

```
feat: Phase 1 — Spring Boot 3 project bootstrap and infrastructure

- pom.xml: Spring Boot 3.3.6 / Java 21 with Web, JPA, PostgreSQL,
  Flyway, Validation, Actuator, Lombok
- Package structure: 9 domain modules + 4 common sub-packages
- Configuration: application.yml (base), dev and prod profiles, test profile
- Flyway V001: PostgreSQL ENUM types and set_updated_at() trigger function
- Docker: multi-stage Dockerfile, docker-compose.yml (postgres + api),
  .dockerignore, .env.example
- Test: context load test using H2 in-memory (no real DB required)
```

---

## Verification Commands

Run these to confirm the Phase 1 setup is correct.

### Compile and run tests
```bash
# Must produce: BUILD SUCCESS
mvn clean verify
```

### Run tests only
```bash
mvn test
```

### Build the JAR (skip tests)
```bash
mvn clean package -DskipTests
```

### Start with a local PostgreSQL (dev profile)
```bash
# Requires PostgreSQL running on localhost:5432
# with database 'placesync_dev' and user 'postgres'
java -jar target/placesync-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

### Verify Actuator health endpoint
```bash
# After starting the app:
curl http://localhost:8080/actuator/health
# Expected: {"status":"UP","components":{"db":{"status":"UP"},...}}

curl http://localhost:8080/actuator/info
# Expected: {"app":{"name":"PlaceSync","version":"1.0.0",...}}
```

### Start with Docker Compose
```bash
cp .env.example .env
# Edit .env if needed (defaults work for local dev)
docker compose up
# App will be available at http://localhost:8080
# Health: http://localhost:8080/actuator/health
```

### Verify Flyway ran V001
```bash
# Connect to the database and check:
# SELECT version, description, success FROM flyway_schema_history;
# Expected row: version=1, description=create enum types, success=true

# Also verify the ENUM type exists:
# SELECT typname FROM pg_type WHERE typname = 'user_role';
```
