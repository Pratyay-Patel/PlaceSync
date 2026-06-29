# Implementation Plan
# PlaceSync — SaaS Placement Management Platform

**Version:** 2.0.0
**Last updated:** 2026-06-28
**Author:** Pratyay Patel

---

## How to use this document

This file is the single source of truth for the phased build-out of PlaceSync V1. It is authoritative over all other planning documents. Update it as each phase is completed — mark the status, record what was built, and check off the acceptance criteria.

**Status legend**

| Symbol | Meaning |
|---|---|
| ✅ | Complete |
| 🔄 | In progress |
| ⬜ | Not started |

---

## Phase map (summary)

| Phase | Scope | Status | Branch |
|---|---|---|---|
| 1 | Project bootstrap & infrastructure | ✅ Complete | `feat/backend` |
| 2 | Database layer + Auth/User/Recruiter/Company modules | ✅ Complete | `feat/database-layer` |
| 3 | Jobs, Applications, Interviews, Resumes + Redis caching | ✅ Complete | `feat/job-application-layer` |
| 4 | Infrastructure hardening + Notifications + Kafka + Admin module | ⬜ Not started | `feat/notification-kafka` |
| 5 | Analytics + AWS S3 + Email delivery | ⬜ Not started | `feat/analytics-s3-email` |
| 6 | Testing suite + CI/CD + Nginx + Production hardening | ⬜ Not started | `feat/cicd-production` |
| 7 | Frontend — React + TypeScript + Vite | ⬜ Not started | `feat/frontend` |

---

## Database environment strategy

PlaceSync uses two PostgreSQL environments throughout its lifecycle. The application is provider-agnostic — it connects via `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD` only, so switching environments requires no code changes, only configuration changes.

| Environment | Provider | Used for |
|---|---|---|
| Development / CI | Docker Compose (PostgreSQL 16 container) | Local development, feature implementation, integration testing, GitHub Actions CI, local Flyway migrations |
| Production | Supabase (managed PostgreSQL) | Staging, production deployment, VPS deployment, long-term data persistence |

**Docker PostgreSQL is the permanent development database.** It is not a temporary stand-in to be replaced — it continues to be used for all local and CI work even after Supabase is connected in Phase 5. The two environments run in parallel throughout the project's lifetime.

**Why this split:**
- Fast local development and instant database resets during testing
- Full offline development capability with no external dependency
- Reproducible, reliable CI builds using a known-good container image
- Safe isolation between development data and production data
- Supabase provides managed backups, high availability, and connection pooling (PgBouncer) for production without operational overhead

Connecting to Supabase for the first time is a dedicated task in Phase 5 (subphase 5.5). Until then, all work targets the Docker PostgreSQL instance.

---

## Development workflow

Every numbered subphase (e.g., 4.0, 4.1, 4.2 …) is an independent implementation milestone. After completing a subphase:

1. Run all relevant verification steps (build, tests, manual smoke test where applicable).
2. Update this file — mark the subphase complete and check off its acceptance criteria.
3. Summarize what was implemented: files created, files modified, dependencies added, endpoints added, acceptance criteria satisfied.
4. Suggest a concise Conventional Commit message.
5. State the branch status and confirm this is the recommended point to commit.
6. **Wait for confirmation before starting the next subphase.**

---

## Phase 1 — Project Bootstrap & Infrastructure

**Status:** ✅ Complete
**Branch:** `feat/backend` → merged to `main` via PR #1
**Commit:** `feat: Phase 1 — Spring Boot 3 project bootstrap and infrastructure`

### What was built

#### Maven project (`pom.xml`)
Spring Boot 3.3.6, Java 21. Dependencies:

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-web` | REST API (embedded Tomcat) |
| `spring-boot-starter-data-jpa` | Hibernate ORM + Spring Data |
| `postgresql` | PostgreSQL JDBC driver |
| `flyway-core` + `flyway-database-postgresql` | Schema migrations |
| `spring-boot-starter-validation` | Jakarta Bean Validation |
| `spring-boot-starter-actuator` | Health and info endpoints |
| `lombok` | Boilerplate reduction |
| `spring-boot-starter-test` + `h2` | Tests with in-memory DB |

#### Application entry point
- `PlaceSyncApplication.java` — `@SpringBootApplication` main class

#### Package structure (13 packages)
All packages created with `package-info.java`:
```
com.placesync
├── auth/
├── user/
├── recruiter/
├── company/
├── job/
├── application/
├── interview/
├── notification/
├── analytics/
└── common/
    ├── config/
    ├── exception/
    ├── util/
    └── audit/
```

#### Configuration files
| File | Purpose |
|---|---|
| `src/main/resources/application.yml` | Base config: JPA (ddl-auto=none), Flyway, Actuator |
| `src/main/resources/application-dev.yml` | Dev profile: env-var-driven datasource, verbose SQL logging |
| `src/main/resources/application-prod.yml` | Prod profile: env-var-driven, HikariCP tuned |
| `src/test/resources/application.yml` | Test profile: H2 in-memory, Flyway disabled, ddl-auto=none |

#### Flyway migration
- `V001__create_enum_types.sql` — All 12 PostgreSQL custom ENUM types + `set_updated_at()` trigger function

ENUMs: `user_role`, `verification_status`, `company_status`, `job_status`, `job_location_type`, `job_type`, `application_status`, `interview_type`, `interview_status`, `notification_type`, `gender_type`, `audit_action`

#### Docker
| File | Purpose |
|---|---|
| `Dockerfile` | Multi-stage build (Maven compile → lean JRE runtime, non-root user) |
| `docker-compose.yml` | Dev stack: PostgreSQL 16 + Redis 7 + Spring Boot API |
| `.dockerignore` | Excludes target/, .git/, .env from build context |
| `.env.example` | Environment variable template (all phases documented) |

#### Test
- `PlaceSyncApplicationTests.java` — `@SpringBootTest` context load test using H2

---

## Phase 2 — Database Layer + Auth/User/Recruiter/Company Modules

**Status:** ✅ Complete
**Branch:** `feat/database-layer` → merged to `main`

### What was built

#### Flyway migrations (V002–V019)
All 18 tables created with full constraints, indexes, and triggers:

| Script | Table(s) |
|---|---|
| `V002__create_users_table.sql` | `users` + `trg_users_updated_at` |
| `V003__create_refresh_tokens_table.sql` | `refresh_tokens` |
| `V004__create_email_verification_tokens_table.sql` | `email_verification_tokens` |
| `V005__create_password_reset_tokens_table.sql` | `password_reset_tokens` |
| `V006__create_companies_table.sql` | `companies` + trigger |
| `V007__create_student_profiles_table.sql` | `student_profiles` + trigger |
| `V008__create_student_skills_table.sql` | `student_skills` |
| `V009__create_student_educations_table.sql` | `student_educations` + trigger |
| `V010__create_student_experiences_table.sql` | `student_experiences` + trigger |
| `V011__create_recruiter_profiles_table.sql` | `recruiter_profiles` + trigger |
| `V012__create_resumes_table.sql` | `resumes` + partial unique index |
| `V013__create_jobs_table.sql` | `jobs` + trigger |
| `V014__create_job_required_skills_table.sql` | `job_required_skills` |
| `V015__create_job_eligible_departments_table.sql` | `job_eligible_departments` |
| `V016__create_applications_table.sql` | `applications` + trigger |
| `V017__create_interviews_table.sql` | `interviews` + trigger |
| `V018__create_notifications_table.sql` | `notifications` |
| `V019__create_audit_log_table.sql` | `audit_log` |

#### JPA entities (all with `@JdbcType(PostgreSQLEnumJdbcType.class)` on enum fields — fix applied in Phase 3)
`User`, `StudentProfile`, `StudentSkill`, `StudentEducation`, `StudentExperience`, `GenderType`, `Resume`, `RefreshToken`, `EmailVerificationToken`, `PasswordResetToken`, `Company`, `CompanyStatus`, `RecruiterProfile`, `VerificationStatus`, `Job`, `JobStatus`, `JobLocationType`, `JobType`, `JobRequiredSkill`, `JobEligibleDepartment`, `Application`, `ApplicationStatus`, `Interview`, `InterviewStatus`, `InterviewType`, `Notification`, `NotificationType`, `AuditLog`, `AuditAction`

#### New dependencies added to `pom.xml`
| Dependency | Purpose |
|---|---|
| `spring-boot-starter-security` | Spring Security 6 |
| `jjwt-api/impl/jackson 0.12.6` | JWT generation and validation |
| `springdoc-openapi-starter-webmvc-ui 2.6.0` | Swagger UI + OpenAPI 3.1 |
| `spring-security-test` | Security test support |

#### Common infrastructure
| File | Purpose |
|---|---|
| `common/config/JwtProperties.java` | `@ConfigurationProperties` for `app.jwt.*` |
| `common/config/AppProperties.java` | `@ConfigurationProperties` for `app.cors.*`, `app.base-url` |
| `common/config/SecurityConfig.java` | Stateless JWT filter chain, CORS, method security |
| `common/config/OpenApiConfig.java` | Swagger UI with Bearer auth scheme |
| `common/security/UserPrincipal.java` | `UserDetails` impl — built from JWT claims or User entity |
| `common/security/JwtTokenProvider.java` | HMAC-SHA256 JWT generation and validation |
| `common/security/JwtAuthenticationFilter.java` | `OncePerRequestFilter` — extracts + validates Bearer token |
| `common/security/UserDetailsServiceImpl.java` | Loads `UserPrincipal` from DB by email |
| `common/exception/GlobalExceptionHandler.java` | `@RestControllerAdvice` — maps all exceptions to standard error response |
| `common/exception/ApiErrorResponse.java` | Standard error response body |
| `common/exception/ResourceNotFoundException.java` | 404 |
| `common/exception/ConflictException.java` | 409 |
| `common/exception/UnauthorizedException.java` | 401 |
| `common/util/PagedResponse.java` | Wrapper for paginated list responses |

#### Auth module
| File | Purpose |
|---|---|
| `auth/dto/RegisterRequest.java` | email, password, role, firstName, lastName, (student fields) |
| `auth/dto/LoginRequest.java` | email, password |
| `auth/dto/AuthResponse.java` | accessToken, refreshToken, expiresIn, userId, email, role |
| `auth/dto/RefreshTokenRequest.java` | refreshToken |
| `auth/dto/ForgotPasswordRequest.java` | email |
| `auth/dto/ResetPasswordRequest.java` | token, newPassword |
| `auth/dto/ChangePasswordRequest.java` | currentPassword, newPassword |
| `auth/service/EmailService.java` | Stub — logs tokens; replaced by real SMTP in Phase 5 |
| `auth/service/AuthService.java` | Full auth business logic |
| `auth/controller/AuthController.java` | 9 endpoints under `/api/v1/auth/` |

**Auth endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/auth/register` | None | Register student or recruiter |
| POST | `/api/v1/auth/login` | None | Login, returns JWT pair |
| POST | `/api/v1/auth/refresh` | None | Rotate refresh token |
| POST | `/api/v1/auth/logout` | None | Revoke refresh token |
| GET | `/api/v1/auth/verify-email?token=` | None | Verify email address |
| POST | `/api/v1/auth/forgot-password` | None | Request password reset email |
| POST | `/api/v1/auth/reset-password` | None | Reset password via token |
| POST | `/api/v1/auth/change-password` | JWT | Change password (authenticated) |
| GET | `/api/v1/auth/me` | JWT | Current user identity |

#### User module (student profiles)
**Student endpoints:**
| Method | Path | Description |
|---|---|---|
| GET | `/api/v1/students/profile` | Get own profile |
| PUT | `/api/v1/students/profile` | Update profile |
| GET | `/api/v1/students/profile/skills` | List skills |
| POST | `/api/v1/students/profile/skills` | Add skill |
| DELETE | `/api/v1/students/profile/skills/{skillId}` | Remove skill |
| GET | `/api/v1/students/profile/education` | List education |
| POST | `/api/v1/students/profile/education` | Add education |
| PUT | `/api/v1/students/profile/education/{id}` | Update education |
| DELETE | `/api/v1/students/profile/education/{id}` | Delete education |
| GET | `/api/v1/students/profile/experience` | List experience |
| POST | `/api/v1/students/profile/experience` | Add experience |
| PUT | `/api/v1/students/profile/experience/{id}` | Update experience |
| DELETE | `/api/v1/students/profile/experience/{id}` | Delete experience |

#### Recruiter module
**Recruiter endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/recruiters/profile` | RECRUITER | Get own profile |
| PUT | `/api/v1/recruiters/profile` | RECRUITER | Update profile |
| GET | `/api/v1/admin/recruiters/pending` | ADMIN | List pending verifications |
| PATCH | `/api/v1/admin/recruiters/{id}/verify` | ADMIN | Approve or reject recruiter |

#### Company module
**Company endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/companies` | Any JWT | List verified companies (paginated) |
| GET | `/api/v1/companies/{id}` | Any JWT | Get company by ID |
| POST | `/api/v1/companies` | RECRUITER | Create company |
| PUT | `/api/v1/companies/{id}` | RECRUITER | Update company (creator only) |
| DELETE | `/api/v1/companies/{id}` | RECRUITER | Soft-delete company (creator only) |
| GET | `/api/v1/admin/companies/pending` | ADMIN | List pending approvals |
| PATCH | `/api/v1/admin/companies/{id}/verify` | ADMIN | Approve or reject company |

### Phase 2 acceptance criteria
- [x] Spring context loads with full security configuration
- [x] `mvn clean test` passes (context load test)
- [x] All 19 Flyway migrations applied cleanly
- [x] Every entity has a repository
- [x] JWT access token issued on login, validated on protected routes
- [x] Refresh token rotation with family-based reuse detection
- [x] Student profile fully manageable (skills, education, experience)
- [x] Recruiter verification workflow operable by ADMIN
- [x] Company approval workflow operable by ADMIN
- [x] All endpoints verified manually against a running PostgreSQL instance
- [x] Merged to `main` via PR

---

## Phase 3 — Jobs, Applications, Interviews + Resume Metadata + Redis Caching

**Status:** ✅ Complete
**Branch:** `feat/job-application-layer` → merged to `main`

### What was built

#### New dependencies added
| Dependency | Purpose |
|---|---|
| `spring-boot-starter-data-redis` | Redis repository and template |
| `spring-boot-starter-cache` | Spring Cache abstraction (`@Cacheable`, `@CacheEvict`) |

#### Docker Compose update
- Redis 7 Alpine service added with health check

#### Resume module (`user/`)
**Resume endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/students/resumes` | STUDENT | List own resumes |
| POST | `/api/v1/students/resumes` | STUDENT | Register resume metadata (S3 upload in Phase 5) |
| PATCH | `/api/v1/students/resumes/{id}/default` | STUDENT | Set as default resume |
| DELETE | `/api/v1/students/resumes/{id}` | STUDENT | Soft-delete resume |
| GET | `/api/v1/students/resumes/{id}/url` | STUDENT/RECRUITER | Pre-signed download URL (returns 501 — Phase 5) |

#### Job module (`job/`)
**Job endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/jobs` | Any JWT | List open jobs (paginated) — Redis cached |
| GET | `/api/v1/jobs/{id}` | Any JWT | Get job detail — Redis cached |
| POST | `/api/v1/jobs` | RECRUITER | Create job (starts as PENDING_APPROVAL) |
| PUT | `/api/v1/jobs/{id}` | RECRUITER | Update job |
| DELETE | `/api/v1/jobs/{id}` | RECRUITER | Soft-delete job |
| PATCH | `/api/v1/jobs/{id}/close` | RECRUITER | Close job |
| GET | `/api/v1/recruiters/jobs` | RECRUITER | List own job postings |
| GET | `/api/v1/admin/jobs/pending` | ADMIN | List jobs pending approval |
| PATCH | `/api/v1/admin/jobs/{id}/approve` | ADMIN | Approve or reject job |

#### Application module (`application/`)
**Application endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/applications` | STUDENT | Apply to a job |
| GET | `/api/v1/students/applications` | STUDENT | List own applications (paginated) |
| GET | `/api/v1/students/applications/{id}` | STUDENT | Get own application detail |
| GET | `/api/v1/recruiters/jobs/{jobId}/applications` | RECRUITER | List applicants for a job |
| PATCH | `/api/v1/recruiters/applications/{id}/status` | RECRUITER | Update application status |

#### Interview module (`interview/`)
**Interview endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/students/interviews` | STUDENT | List own upcoming interviews |
| GET | `/api/v1/recruiters/applications/{id}/interviews` | RECRUITER | List interviews for an application |
| POST | `/api/v1/recruiters/applications/{id}/interviews` | RECRUITER | Schedule an interview round |
| PUT | `/api/v1/recruiters/interviews/{id}` | RECRUITER | Reschedule interview |
| PATCH | `/api/v1/recruiters/interviews/{id}/cancel` | RECRUITER | Cancel interview |
| PATCH | `/api/v1/recruiters/interviews/{id}/complete` | RECRUITER | Mark interview as completed |

### Bugs fixed in this phase
| Bug | Root Cause | Fix Applied |
|---|---|---|
| `column 'role' is of type user_role but expression is of type character varying` | Hibernate 6 sends enums as `character varying`; PostgreSQL rejects implicit cast to custom enum type | `@JdbcType(PostgreSQLEnumJdbcType.class)` added to all 12 enum fields across 9 entity files |
| `Connection to localhost:5432 refused` (API inside Docker) | `application-dev.yml` hardcoded `localhost` — inside Docker, `localhost` is the container itself | Changed to `${DATABASE_URL:jdbc:postgresql://localhost:5432/placesync_dev}` so docker-compose env var takes effect |
| Swagger UI sends `sort=["string"]` for paginated endpoints → 500 | SpringDoc renders `Pageable` as a complex object instead of individual fields | `@ParameterObject` added to all 8 `Pageable` parameters across 4 controllers |
| `HttpRequestMethodNotSupportedException` returns 500 | `GlobalExceptionHandler` had no handler for 405 | Added handlers for 405, 400 (malformed JSON), unhandled exception logging |
| Docker PostgreSQL port conflict with local PostgreSQL 17 | Both bound to host port 5432 | Docker PostgreSQL remapped to host port 5433 in `docker-compose.yml` |

### What is deferred to later phases
- Actual S3 file upload for resumes — Phase 5 (`/resumes/{id}/url` returns 501)
- AOP audit logging — Phase 4 (table exists, no aspect yet)
- MapStruct mapper layer — Phase 4 (manual mapping in service/response classes)
- Formal unit and integration tests — Phase 6

### Phase 3 acceptance criteria
- [x] `docker-compose up` brings up PostgreSQL + Redis + API
- [x] Student can list open jobs and view job detail (Redis cache verified on second hit)
- [x] Student can apply to an open job with a valid resume
- [x] Duplicate application returns 409
- [x] Recruiter can update application status through full lifecycle
- [x] Recruiter can schedule, reschedule, cancel, and complete interview rounds
- [x] All endpoints verified manually via Swagger UI and DB queries in pgAdmin
- [x] Merged to `main` via PR

---

## Phase 4 — Infrastructure Hardening + Notifications + Kafka + Admin Module

**Status:** ⬜ Not started
**Planned branch:** `feat/notification-kafka`
**Depends on:** Phase 3

### Scope

This is the largest phase. It delivers the Kafka event pipeline, in-app notification inbox, and admin user-management module — and simultaneously addresses all the cross-cutting infrastructure gaps carried over from earlier phases: MapStruct mapper layer, AOP audit logging, structured request logging, common pagination/filtering/sorting infrastructure, enhanced Bean Validation, SonarLint integration, and a consistent API success response format.

Build order within this phase:
0. Basic CI pipeline (GitHub Actions — build + unit tests on every push)
1. SonarLint + code quality setup
2. MapStruct mapper layer (backfill all existing modules + new ones)
3. Structured logging + request/response filter + MDC correlation IDs
4. Common pagination/filtering/sorting infrastructure
5. Enhanced Bean Validation + custom validators
6. AOP audit logging
7. Consistent API success/error response format
8. Admin module — user management + global views
9. Kafka infrastructure + events
10. Notification module (Kafka consumer + Spring Events fallback)
11. Unit tests for Phases 2, 3, and 4 service layers (Mockito)

---

### 4.0 Basic CI Pipeline

**Why here:** Every subphase that follows produces tested, mergeable code. A minimal CI run — build + unit tests — should catch regressions immediately on push. Advanced features (SonarCloud gate, Testcontainers integration tests, coverage reporting) are added in Phase 6 once the full test suite exists.

#### File to create

`.github/workflows/ci.yml`

```yaml
name: CI

on:
  push:
    branches: ["**"]
  pull_request:
    branches: [main]

jobs:
  build-and-test:
    runs-on: ubuntu-latest

    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up JDK 21
        uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
          cache: maven

      - name: Build and run unit tests
        run: mvn -B clean verify -Dspring.profiles.active=test
```

- Runs on every push to any branch and every PR targeting `main`
- Uses the `test` Spring profile (H2 in-memory, Flyway disabled, fast)
- Maven dependency cache keyed by `pom.xml` — subsequent runs resolve dependencies from cache

#### Subphase 4.0 acceptance criteria
- [x] `.github/workflows/ci.yml` committed to the branch
- [x] Pipeline triggers on push and completes green (`mvn clean verify` passes)
- [x] Context load test (`PlaceSyncApplicationTests`) passes in CI

---

### 4.1 SonarLint & Code Quality Setup ✅

**Why now:** The SRS (NFR-041) requires a SonarQube quality gate of 0 critical bugs, 0 security vulnerabilities, <5% code duplication. Setting this up early catches issues while the codebase is still small rather than deferring to Phase 6 when the backlog of fixes would be large.

#### What was built
- Installed SonarQube (SonarLint) plugin in IntelliJ IDEA
- Added `jacoco-maven-plugin` (0.8.12) to `pom.xml` — generates `target/site/jacoco/jacoco.xml` on every `mvn verify`
- Added `sonar-maven-plugin` (3.11.0.3922) to `pom.xml`
- Added `sonar.host.url`, `sonar.organization`, `sonar.projectKey`, `sonar.coverage.jacoco.xmlReportPaths` to `pom.xml` properties so the Maven plugin connects to SonarCloud without command-line flags
- Created `sonar-project.properties` at project root (SonarCloud metadata; token passed at runtime only)
- Fixed all 9 high-severity findings: extracted 8 duplicate string literals into class-level constants (`STUDENT_PROFILE`, `RECRUITER_PROFILE`, `COMPANY`, `MESSAGE_KEY`) across 8 service/controller classes; marked CSRF hotspot in `SecurityConfig` as "Safe" (intentional — stateless JWT API)
- SonarCloud quality gate: **PASSED** — 0 blocker, 0 high issues

#### Acceptance criteria
- [x] SonarQube plugin installed in IntelliJ IDEA
- [x] `jacoco-maven-plugin` generates coverage report on `mvn verify`
- [x] `sonar-maven-plugin` connects to SonarCloud via `pom.xml` properties
- [x] `sonar-project.properties` created and committed (no secrets)
- [x] `mvn sonar:sonar` runs successfully — quality gate PASSED
- [x] Zero high/blocker findings remaining

---

### 4.2 MapStruct Mapper Layer ✅

**Why:** All existing modules perform Entity → DTO conversion manually inside service methods or response constructors. This couples the service layer to the presentation layer, makes testing harder, and produces duplication. MapStruct generates type-safe, compile-time mappers.

#### What was built
- Added `mapstruct` (1.5.5.Final) dependency and `maven-compiler-plugin` with `annotationProcessorPaths` for `mapstruct-processor`, `lombok`, and `lombok-mapstruct-binding` (0.2.0)
- Created 7 mapper interfaces (skipped `AuthMapper` — `AuthResponse` requires injected JWT services, not entity data; skipped `notification`/`analytics` — modules are stubs):
  - `user/mapper/StudentProfileMapper` — `StudentProfile → StudentProfileResponse` (nested `user.id`, Boolean `isProfilePublic` expression)
  - `user/mapper/ResumeMapper` — `Resume → ResumeResponse`
  - `recruiter/mapper/RecruiterMapper` — `RecruiterProfile → RecruiterProfileResponse` (nullable `company.id`/`company.name`)
  - `company/mapper/CompanyMapper` — `Company → CompanyResponse` (nested `createdBy.id`)
  - `job/mapper/JobMapper` — `Job → JobResponse` and `Job → JobSummaryResponse` (collection → `List<String>` via `@Named` default methods)
  - `application/mapper/ApplicationMapper` — `Application → ApplicationResponse` (3-level nesting: `job.company.name`)
  - `interview/mapper/InterviewMapper` — `Interview → InterviewResponse` (4-level nesting: `application.job.company.name`)
- Refactored all 7 service classes to inject mappers via constructor injection and call `mapper.toResponse()` / `mapper::toResponse`
- Removed all `from()` static methods and entity imports from 8 response DTOs

#### Acceptance criteria
- [x] MapStruct dependency and annotation processor paths added to `pom.xml`
- [x] 7 mapper interfaces created in module `mapper` sub-packages
- [x] All `static from()` methods removed from response DTOs
- [x] All service classes use injected mappers — no manual mapping code remains
- [x] `mvn clean verify` passes — BUILD SUCCESS, 134 source files compiled

---

### 4.3 Structured Logging + Request/Response Filter + Correlation IDs ✅

**Why:** Without structured logging, debugging production issues requires grep-based log mining. Correlation IDs link all log lines from a single HTTP request together. The SRS (ARCH section 22) specifies JSON structured logging in production.

#### What was built
- `common/logging/MdcLoggingFilter.java` — reads or generates `X-Correlation-ID` UUID per request, writes to MDC and response header, clears MDC in `finally` block
- `common/logging/RequestResponseLoggingFilter.java` — logs `METHOD path -> status (Xms)` at INFO for every request
- `common/config/LoggingConfig.java` — registers both filters via `FilterRegistrationBean` with order 1 (MDC) then 2 (request/response)
- `src/main/resources/logback-spring.xml` — dev profile: human-readable console with `[%X{correlationId}]`; prod profile: `LogstashEncoder` JSON with `correlationId` MDC field
- Added `logstash-logback-encoder:7.4` dependency
- Added SLF4J `Logger` + `log.info()` on all write operations to: `UserService`, `ResumeService`, `RecruiterService`, `CompanyService`, `JobService`, `ApplicationService`, `InterviewService`

#### Acceptance criteria
- [x] `MdcLoggingFilter` sets `correlationId` in MDC and echoes it in response header
- [x] `RequestResponseLoggingFilter` logs method, path, status, and duration for every request
- [x] Dev profile uses human-readable pattern with `[correlationId]`
- [x] Prod profile uses JSON encoder (Logstash)
- [x] All 7 service classes have `private static final Logger log` and `log.info()` on write operations
- [x] `mvn clean verify` passes — BUILD SUCCESS, 137 source files compiled

---

### 4.4 Common Pagination, Filtering, Sorting, and Search Infrastructure ✅

**Why:** Each controller currently re-implements filtering independently. The SRS (JOB-FR-008) requires filtering by keyword, company, location type, job type, skills, and deadline. Rather than repeating this in every controller, a reusable specification layer handles it.

#### What was built
- `common/spec/JobSpecification.java` — static factory `withFilters(JobFilterRequest)` composing predicates: `status=OPEN`, `deletedAt IS NULL`, keyword LIKE on title/description, companyId, locationType, jobType, skill LIKE with LEFT JOIN on requiredSkills, deadlineAfter; null filters are no-ops
- `common/spec/ApplicationSpecification.java` — `withFilters(jobId, studentId, status)` — infrastructure for admin module (subphase 4.7)
- `common/spec/UserSearchSpecification.java` — `withFilters(email, role, isActive)` — infrastructure for admin module (subphase 4.7)
- `job/dto/JobFilterRequest.java` — `@Getter @Setter` bean with all 6 filter fields + `cacheKey()` method for stable cache key generation
- `JobRepository`, `ApplicationRepository`, `UserRepository` — each extended with `JpaSpecificationExecutor<T>`
- `JobService.getOpenJobs` — updated signature to `(JobFilterRequest filter, Pageable pageable)`; uses `JobSpecification.withFilters(filter)`; cache key now includes `filter.cacheKey()` so different filter combinations cache independently
- `JobController.getOpenJobs` — now accepts `@ParameterObject JobFilterRequest filter` so all 6 filters appear as Swagger query params
- `PagedResponse.java` — reviewed, no changes needed

#### Endpoints updated
| Method | Path | Change |
|---|---|---|
| `GET` | `/api/v1/jobs` | Now accepts optional `keyword`, `companyId`, `locationType`, `jobType`, `skill`, `deadlineAfter` query params |

#### Acceptance criteria
- [x] `JobSpecification` builds correct predicates for all 6 filter dimensions
- [x] `ApplicationSpecification` and `UserSearchSpecification` created as infrastructure
- [x] All three repositories extend `JpaSpecificationExecutor`
- [x] `GET /api/v1/jobs` accepts all 6 optional filter params
- [x] Redis cache key includes filter combination — different filters cache independently
- [x] `mvn clean verify` passes — BUILD SUCCESS, 141 source files compiled

---

### 4.5 Enhanced Bean Validation + Custom Validators ✅

**Why:** Current validation uses only standard Jakarta annotations (`@NotBlank`, `@Size`, `@NotNull`). Several business rules require custom validators.

#### What was built
- `common/validation/ValidPassword` + `PasswordValidator` — regex `^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[^a-zA-Z0-9]).{8,}$` enforcing SRS AUTH-FR-003; replaces `@Size(min=8)` on both `RegisterRequest` and `ResetPasswordRequest`
- `common/validation/FutureDate` + `FutureDateValidator` — `value.isAfter(OffsetDateTime.now())` with a descriptive message; replaces `@Future` on `CreateJobRequest.applicationDeadline` and `ScheduleInterviewRequest.scheduledAt`
- `common/validation/ValidCgpa` + `CgpaValidator` — `BigDecimal` range check [0.0, 10.0] inclusive (SRS STU-FR-008); replaces `@DecimalMin @DecimalMax` on `UpdateStudentProfileRequest.cgpa`
- `@ValidFileSize` deferred to Phase 5 (resume upload not yet implemented)
- `ApplicationService.updateStatus` — added `VALID_TRANSITIONS` map enforcing the state machine; invalid transitions throw `IllegalArgumentException` → 400 with `"Invalid status transition from X to Y"`

#### Status transition matrix
```
APPLIED → UNDER_REVIEW → SHORTLISTED → INTERVIEW_SCHEDULED → OFFERED
        ↘ REJECTED      ↘ REJECTED    ↘ REJECTED            ↘ REJECTED
```
OFFERED and REJECTED are terminal states (no further transitions allowed).

#### Acceptance criteria
- [x] `@ValidPassword` applied to `RegisterRequest.password` and `ResetPasswordRequest.newPassword`
- [x] `@FutureDate` applied to `CreateJobRequest.applicationDeadline` and `ScheduleInterviewRequest.scheduledAt`
- [x] `@ValidCgpa` applied to `UpdateStudentProfileRequest.cgpa`
- [x] Invalid status transitions return 400 with descriptive message
- [x] `mvn clean verify` passes — BUILD SUCCESS

---

### 4.6 AOP Audit Logging ✅ COMPLETE

**Why:** The `audit_log` table exists from Phase 1 but nothing writes to it. ARS-FR-001 requires logging all write operations. The correct implementation is an AOP aspect that intercepts service methods — not manual `auditLogRepository.save()` calls scattered across every service.

**Post-implementation fixes (verified working):**
- `AuditLog.action` required `@ColumnTransformer(write = "?::audit_action")` — PostgreSQL JDBC does not implicitly cast `character varying` to custom enum types
- `AuditLog.ipAddress` required `@ColumnTransformer(write = "?::inet")` — same issue with the `inet` column type
- Removed `@Transactional` from `AuditLogService.saveAsync` — the outer transaction was being marked rollback-only after a flush failure, causing `UnexpectedRollbackException` to escape the try/catch; Spring Data's own `@Transactional` on `save()` is sufficient

#### Files to create
| File | Purpose |
|---|---|
| `common/audit/Auditable.java` | Annotation: `@Auditable(action = AuditAction.CREATE, entityType = "Job")` |
| `common/audit/AuditAspect.java` | `@Around` aspect — captures before/after state, resolves actor from `SecurityContextHolder`, persists `AuditLog` |
| `common/audit/AuditContext.java` | Thread-local holder for the entity snapshot before the intercepted method runs |
| `common/audit/AuthEventAuditListener.java` | `@EventListener` for Spring Security `AbstractAuthenticationEvent` — logs login success, login failure, logout |

#### `AuditAspect` design
```java
@Around("@annotation(auditable)")
public Object audit(ProceedingJoinPoint pjp, Auditable auditable) throws Throwable {
    // 1. Capture actor from SecurityContextHolder
    // 2. Capture "before" snapshot of the entity (if update/delete)
    // 3. Proceed with the method
    // 4. Capture "after" snapshot from the return value
    // 5. Persist AuditLog asynchronously (@Async) — must not fail the business tx
    return result;
}
```

The audit write is `@Async` so a failure to write the audit log does not roll back the business transaction.

#### Authentication event auditing
| Event | Audit Action |
|---|---|
| `AuthenticationSuccessEvent` | `LOGIN_SUCCESS` — record userId, IP, userAgent |
| `AbstractAuthenticationFailureEvent` | `LOGIN_FAILURE` — record attempted email, IP |
| Logout (`POST /auth/logout`) | `LOGOUT` — called explicitly from `AuthService` |
| Password change | `PASSWORD_CHANGED` |
| Password reset | `PASSWORD_RESET` |

#### Audit log admin search API
Add to `common/audit/`:
| File | Purpose |
|---|---|
| `audit/dto/AuditLogResponse.java` | Read DTO |
| `audit/dto/AuditSearchRequest.java` | Filter params: entityType, actorId, action, from, to |
| `audit/service/AuditLogService.java` | Paginated search — read-only, no modification methods |
| `audit/controller/AuditController.java` | Admin-only endpoint |

**Audit endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/admin/audit-log` | ADMIN | Search audit log (paginated, filtered) |
| GET | `/api/v1/admin/audit-log/{id}` | ADMIN | Get single audit log entry |

Query parameters for `GET /api/v1/admin/audit-log`:
`entityType`, `actorId`, `action`, `from` (ISO 8601), `to` (ISO 8601), `page`, `size`

---

### 4.7 Consistent API Response Format ✅ COMPLETE

**Why:** NFR-051 mandates a consistent error schema. Success responses currently return raw DTOs with no envelope. Adding an optional `ApiResponse<T>` wrapper makes it easier to include metadata (request ID, timestamp) without breaking the individual DTO shapes.

#### Decision
Use a **lightweight envelope only for non-trivial success responses** (not for simple CRUD returns where the DTO is already self-evident). The envelope is:

```json
{
  "success": true,
  "data": { ... },
  "timestamp": "2026-06-27T10:00:00Z",
  "correlationId": "uuid"
}
```

Apply to new endpoints added in Phase 4+. Existing Phase 2/3 endpoints keep their current shape to avoid breaking changes — they will be homogenized in Phase 6 if needed.

#### Files to create
| File | Purpose |
|---|---|
| `common/util/ApiResponse.java` | Generic `record ApiResponse<T>(boolean success, T data, OffsetDateTime timestamp, String correlationId)` |
| `common/util/ApiResponseFactory.java` | Static factory: `ApiResponseFactory.ok(data, correlationId)` |

---

### 4.8 Admin Module — User Management + Global Views ✅ COMPLETE

**Why:** Several SRS requirements (USER-FR-002, USER-FR-004, ADM-FR-001 through ADM-FR-008) specify admin capabilities that have no endpoints yet: searching/deactivating users, viewing all applications, viewing all interviews globally.

#### Files to create
| File | Purpose |
|---|---|
| `common/admin/dto/UserSummaryResponse.java` | id, email, role, isActive, isEmailVerified, createdAt |
| `common/admin/dto/UpdateUserStatusRequest.java` | `isActive: boolean` |
| `common/admin/service/AdminUserService.java` | Search users, activate, deactivate |
| `common/admin/controller/AdminUserController.java` | User management endpoints |

**Admin user management endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/admin/users` | ADMIN | Search users — filter by name, email, role, status (paginated) |
| GET | `/api/v1/admin/users/{userId}` | ADMIN | Get user detail |
| PATCH | `/api/v1/admin/users/{userId}/status` | ADMIN | Activate or deactivate a user |

**Admin global views endpoints (add to existing controllers):**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/admin/applications` | ADMIN | View all applications across all jobs (paginated, filterable by status) |
| GET | `/api/v1/admin/interviews` | ADMIN | View all interview schedules across all recruiters (paginated) |

These read from `ApplicationRepository` and `InterviewRepository` without a user-scope filter (admin sees everything).

---

### 4.9 Kafka Infrastructure + Events ✅ COMPLETE

#### What was built
- `common/event/DomainEvent.java` — interface: `eventId`, `eventType`, `timestamp`
- `common/event/ApplicationSubmittedEvent.java`, `ApplicationStatusChangedEvent.java`, `OfferReleasedEvent.java` — application domain events
- `common/event/InterviewScheduledEvent.java`, `InterviewRescheduledEvent.java`, `InterviewCancelledEvent.java` — interview domain events
- `common/event/RecruiterVerifiedEvent.java` — recruiter verification event
- `common/kafka/KafkaEventPublisher.java` — `publish()` routes through Spring `ApplicationEventPublisher`; `@TransactionalEventListener(AFTER_COMMIT)` + `@Async` delivers to Kafka after DB commit; `KafkaTemplate` is `@Autowired(required = false)` for test safety
- `common/config/KafkaConfig.java` — `application-events`, `interview-events`, `offer-events` topics (3 partitions, replication 1); `DefaultErrorHandler` + `DeadLetterPublishingRecoverer` (3 retries, 1 s back-off); `@ConditionalOnBean(KafkaTemplate.class)` on error handler bean
- `docker-compose.yml` — Confluent KRaft Kafka 7.6.0 added; `api` now depends on `kafka: service_healthy`
- `application-dev.yml` — Kafka bootstrap-servers, consumer group, JSON de/serialization config
- `src/test/resources/application.yml` — `KafkaAutoConfiguration` excluded so tests run without a broker
- `ApplicationService` — injects `KafkaEventPublisher`; publishes `ApplicationSubmittedEvent` after `apply()`, `ApplicationStatusChangedEvent` + `OfferReleasedEvent` after `updateStatus()`
- `InterviewService` — injects `KafkaEventPublisher`; publishes `InterviewScheduledEvent`, `InterviewRescheduledEvent`, `InterviewCancelledEvent`
- `RecruiterService` — injects `KafkaEventPublisher`; publishes `RecruiterVerifiedEvent` after `processVerification()`

#### Acceptance criteria
- [x] `spring-kafka` dependency added
- [x] Kafka KRaft service in docker-compose.yml with health check
- [x] `DomainEvent` interface and 7 event records created
- [x] `KafkaEventPublisher` publishes via `@TransactionalEventListener(AFTER_COMMIT)` — Kafka send only after DB transaction commits
- [x] `KafkaConfig` creates 3 topics (3 partitions each) and `DefaultErrorHandler` with `DeadLetterPublishingRecoverer`
- [x] Events wired in `ApplicationService`, `InterviewService`, `RecruiterService`
- [x] `mvn clean verify` passes

#### New dependency
```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

#### Docker Compose update
Add Confluent KRaft-mode Kafka service (no ZooKeeper):
```yaml
kafka:
  image: confluentinc/cp-kafka:7.6.0
  environment:
    KAFKA_NODE_ID: 1
    KAFKA_PROCESS_ROLES: broker,controller
    KAFKA_LISTENERS: PLAINTEXT://0.0.0.0:9092,CONTROLLER://0.0.0.0:9093
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092
    KAFKA_CONTROLLER_QUORUM_VOTERS: 1@kafka:9093
    KAFKA_CONTROLLER_LISTENER_NAMES: CONTROLLER
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
  ports:
    - "9092:9092"
  healthcheck:
    test: ["CMD", "kafka-topics", "--bootstrap-server", "localhost:9092", "--list"]
    interval: 30s
    timeout: 10s
    retries: 5
  networks:
    - placesync-net
```

#### Kafka configuration
```yaml
spring:
  kafka:
    bootstrap-servers: ${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
    consumer:
      group-id: notification-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.placesync.*"
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
```

#### Domain events (`common/event/`)
| Event class | Trigger | Key fields |
|---|---|---|
| `ApplicationSubmittedEvent` | Student applies | applicationId, studentId, jobId, jobTitle, companyName, studentEmail |
| `ApplicationStatusChangedEvent` | Recruiter updates status | applicationId, studentId, oldStatus, newStatus |
| `InterviewScheduledEvent` | Recruiter schedules interview | interviewId, applicationId, studentId, round, scheduledAt, meetingLink |
| `InterviewRescheduledEvent` | Recruiter reschedules | interviewId, studentId, oldScheduledAt, newScheduledAt |
| `InterviewCancelledEvent` | Recruiter cancels | interviewId, studentId, cancellationReason |
| `RecruiterVerifiedEvent` | Admin approves recruiter | recruiterId, userId, decision |
| `OfferReleasedEvent` | Recruiter sets status = OFFERED | applicationId, studentId, jobTitle, companyName |

All event classes are plain Java records implementing a common `DomainEvent` interface (`eventId: UUID`, `eventType: String`, `timestamp: OffsetDateTime`).

#### Kafka event publisher (`common/kafka/`)
| File | Purpose |
|---|---|
| `common/kafka/KafkaEventPublisher.java` | Wraps `KafkaTemplate` — publishes event; on failure falls back to `ApplicationEventPublisher` and logs a `WARN` |
| `common/config/KafkaConfig.java` | Topic beans, consumer error handler with `DeadLetterPublishingRecoverer` |

Inject `KafkaEventPublisher` into `ApplicationService` and `InterviewService`. Publish events after successful DB commit (use `@TransactionalEventListener(phase = AFTER_COMMIT)` to guarantee Kafka publish happens only after the transaction commits).

#### Kafka topics
| Topic | Partitions | Retention | Producer | Consumer |
|---|---|---|---|---|
| `application-events` | 3 | 7 days | `ApplicationService` | `NotificationConsumer` |
| `interview-events` | 3 | 7 days | `InterviewService` | `NotificationConsumer` |
| `offer-events` | 3 | 7 days | `ApplicationService` | `NotificationConsumer` |

---

### 4.10 Notification Module

#### Files to create
| File | Purpose |
|---|---|
| `notification/dto/NotificationResponse.java` | id, type, title, body, isRead, createdAt |
| `notification/service/NotificationService.java` | createNotification, markAsRead, markAllRead, countUnread, listPaginated |
| `notification/consumer/NotificationConsumer.java` | `@KafkaListener` on all 3 topics |
| `notification/consumer/NotificationFallbackListener.java` | `@Async @EventListener` — fires when Kafka publish fails |
| `notification/controller/NotificationController.java` | Inbox endpoints |

**Notification endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/notifications` | Any JWT | List own notifications (paginated, optional `?unreadOnly=true`) |
| GET | `/api/v1/notifications/unread-count` | Any JWT | Count unread notifications |
| PATCH | `/api/v1/notifications/{id}/read` | Any JWT | Mark one notification as read |
| PATCH | `/api/v1/notifications/read-all` | Any JWT | Mark all notifications as read |

**Notification creation mapping:**

| Event | Notification type | Title | Body |
|---|---|---|---|
| `ApplicationSubmittedEvent` | `APPLICATION_UPDATE` | "Application submitted" | "Your application for {jobTitle} at {companyName} was received." |
| `ApplicationStatusChangedEvent` | `APPLICATION_UPDATE` | "Application status updated" | "Your {jobTitle} application is now {newStatus}." |
| `InterviewScheduledEvent` | `INTERVIEW_SCHEDULED` | "Interview scheduled" | "Round {round} interview scheduled for {scheduledAt}." |
| `InterviewRescheduledEvent` | `INTERVIEW_SCHEDULED` | "Interview rescheduled" | "Your interview has been moved to {newScheduledAt}." |
| `InterviewCancelledEvent` | `INTERVIEW_SCHEDULED` | "Interview cancelled" | "Your interview was cancelled: {reason}." |
| `RecruiterVerifiedEvent` (approved) | `SYSTEM` | "Verification approved" | "Your recruiter profile has been verified." |
| `RecruiterVerifiedEvent` (rejected) | `SYSTEM` | "Verification rejected" | "Your recruiter verification was rejected." |
| `OfferReleasedEvent` | `OFFER_RELEASED` | "Offer received!" | "Congratulations! You have received an offer from {companyName} for {jobTitle}." |

---

### 4.11 Unit Testing — Phase 2, 3, and 4 Service Layers

**Why now:** All business logic exists. Writing unit tests before Phase 5 adds features ensures a regression safety net. These are pure Mockito tests — no Spring context, no DB, fast.

#### Test files to create (one per service class)
| Test class | Service under test | Key scenarios |
|---|---|---|
| `AuthServiceTest` | `AuthService` | Register success, duplicate email → 409, login success, invalid credentials → 401, refresh token rotation, logout |
| `UserServiceTest` | `UserService` | Get profile, update profile, add/remove skill, add/remove education, add/remove experience |
| `RecruiterServiceTest` | `RecruiterService` | Get profile, update profile, admin approve → VERIFIED, admin reject → REJECTED, already-verified → conflict |
| `CompanyServiceTest` | `CompanyService` | Create company, update (creator only), admin verify, admin reject, non-creator update → 403 |
| `ResumeServiceTest` | `ResumeService` | Create metadata, set default clears others, soft-delete, delete resume in active application → conflict |
| `JobServiceTest` | `JobService` | Create job, update job (pending only), approve → OPEN, close job, cache eviction |
| `ApplicationServiceTest` | `ApplicationService` | Apply, duplicate apply → 409, job not OPEN → conflict, CGPA check, department eligibility, status transitions, invalid transition → 400 |
| `InterviewServiceTest` | `InterviewService` | Schedule interview, reschedule, cancel, complete, schedule in past → 400 |
| `NotificationServiceTest` | `NotificationService` | Create notification, mark as read, mark all read, unread count |
| `AdminUserServiceTest` | `AdminUserService` | Search users, deactivate, reactivate |

**Test conventions:**
- Use `@ExtendWith(MockitoExtension.class)` — no Spring context
- Mock all repository and external service dependencies with `@Mock`
- Use `@InjectMocks` for the service under test
- Name test methods: `methodName_scenario_expectedBehavior()` (e.g., `apply_duplicateApplication_throwsConflictException`)
- Assert both happy path and all documented error paths
- Target ≥ 70% line coverage on service layer (SRS NFR-041)

---

### Phase 4 acceptance criteria
- [ ] SonarLint enabled in IDE; no blocker/critical findings in new code
- [ ] `mvn verify` passes including all new unit tests
- [ ] MapStruct mappers compile cleanly; no manual mapping code remains in service classes
- [ ] Request/response log lines include `correlationId` field
- [ ] `GET /api/v1/jobs?keyword=backend&locationType=REMOTE` returns filtered results
- [ ] Invalid application status transition returns 400 with descriptive message
- [ ] `@Auditable`-annotated methods produce rows in `audit_log` with `old_values` and `new_values`
- [ ] Login success/failure recorded in `audit_log`
- [ ] `GET /api/v1/admin/audit-log?entityType=Job&from=2026-01-01` returns paginated results
- [ ] Admin can deactivate a user; deactivated user's login returns 401
- [ ] Admin can search users by email partial match
- [ ] Admin can view all applications and all interviews across all users
- [ ] `docker-compose up` brings up PostgreSQL + Redis + Kafka + API (all healthy)
- [ ] Student receives in-app notification after applying to a job
- [ ] Student receives in-app notification when application status changes
- [ ] Student receives in-app notification when interview is scheduled
- [ ] Notifications visible at `GET /api/v1/notifications`
- [ ] Unread count decrements on `PATCH /api/v1/notifications/{id}/read`
- [ ] Kafka fallback verified: shutting down Kafka container does not prevent notifications from being created
- [ ] Service layer unit test suite runs in < 30 seconds
- [ ] Merged to `main` via PR

---

## Phase 5 — Analytics + AWS S3 + Email Delivery

**Status:** ⬜ Not started
**Planned branch:** `feat/analytics-s3-email`
**Depends on:** Phase 4 (Kafka events feed offer analytics)

### Scope

Four sub-systems built and merged together:

1. **Analytics** — global placement dashboard (admin) + recruiter-scoped analytics
2. **AWS S3** — real file upload for resumes and profile pictures; company logos
3. **Email delivery** — replace the `EmailService` stub with HTML Thymeleaf templates over Gmail SMTP
4. **Supabase production integration** — first-time connection of the application to the managed production database

Plus production-facing security and file safety concerns: Spring-level security headers, file upload validation (MIME type + magic bytes), and request size limits.

All development and CI work in this phase continues to target Docker PostgreSQL. Supabase is introduced at the end of the phase (subphase 5.5) after all features are verified locally.

---

### 5.1 Analytics Module

#### New dependency
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>
<!-- already present from Phase 3 -->
```

#### Files to create
| File | Purpose |
|---|---|
| `analytics/dto/PlacementStatsResponse.java` | totalStudents, totalRecruiters, totalCompanies, openJobs, totalApplications, totalOffers, placementRate |
| `analytics/dto/CompanyStatsResponse.java` | companyId, companyName, offerCount, jobCount, applicationCount |
| `analytics/dto/DepartmentStatsResponse.java` | department, placedCount, totalStudents, placementRate |
| `analytics/dto/RecruiterStatsResponse.java` | jobsPosted, totalApplications, shortlisted, offers |
| `analytics/service/AnalyticsService.java` | Aggregation queries; all results `@Cacheable` |
| `analytics/controller/AnalyticsController.java` | Dashboard endpoints |

**Analytics endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/analytics/placement-stats` | ADMIN | Global placement statistics |
| GET | `/api/v1/analytics/company-breakdown` | ADMIN | Per-company offer counts (top 10) |
| GET | `/api/v1/analytics/department-breakdown` | ADMIN | Per-department placement rates |
| GET | `/api/v1/analytics/recruiter-stats` | RECRUITER | Own jobs/applications/offers |

**Cache design:**
| Cache | Key | TTL | Eviction trigger |
|---|---|---|---|
| `analytics-dashboard` | `global` | 10 min | New offer recorded (listen for `OfferReleasedEvent`) |
| `recruiter-analytics` | `{recruiterId}` | 10 min | Application status change for this recruiter's jobs |

---

### 5.2 AWS S3 Integration

#### New dependency
```xml
<dependency>
    <groupId>software.amazon.awssdk</groupId>
    <artifactId>s3</artifactId>
    <version>2.25.0</version>
</dependency>
```

#### Files to create
| File | Purpose |
|---|---|
| `common/config/S3Config.java` | `S3Client` bean — reads AWS credentials from env vars |
| `common/storage/S3StorageService.java` | `uploadFile(key, inputStream, contentType, size)`, `generatePresignedUrl(key, expiryMinutes)`, `deleteFile(key)` |
| `common/storage/FileValidationService.java` | Validates MIME type by reading magic bytes (not trusting `Content-Type` header), enforces size limits |

#### Resume upload — replace Phase 3 stub
Update `POST /api/v1/students/resumes` from metadata-only to a multipart upload:

| Field | Type | Constraint |
|---|---|---|
| `file` | `MultipartFile` | PDF only (validated by magic bytes `%PDF`), max 10 MB |
| `label` | `String` | 1–255 chars |
| `isDefault` | `boolean` | Optional, default false |

S3 key: `resumes/{studentId}/{resumeId}/{sanitizedFilename}.pdf`

Implement `GET /api/v1/students/resumes/{id}/url` (currently returns 501):
- Student: must own the resume
- Recruiter: the student must have applied to one of their jobs
- Returns `{ downloadUrl, expiresAt }` — pre-signed URL valid for 15 minutes

#### Profile picture upload (new endpoint)
`PATCH /api/v1/students/profile/picture` — STUDENT only
- Accepts JPEG or PNG; max 5 MB; validated by magic bytes
- Stores at `profile-pictures/{userId}/{timestamp}-{uuid}.jpg`
- Returns updated `StudentProfileResponse` with `profilePictureUrl` (pre-signed, 60 min TTL)

#### Company logo upload (new endpoint)
`PATCH /api/v1/companies/{companyId}/logo` — RECRUITER (creator only)
- Accepts JPEG or PNG; max 2 MB
- Stores at `company-logos/{companyId}/{timestamp}-{uuid}.jpg`

#### File upload validation (security concern)
| Check | Implementation |
|---|---|
| File extension whitelist | Reject filenames not ending in `.pdf`, `.jpg`, `.jpeg`, `.png` |
| MIME type by magic bytes | Read first 8 bytes; PDF must start with `%PDF`, JPEG with `FFD8FF`, PNG with `89504E47` |
| Max file size | Spring MVC: `spring.servlet.multipart.max-file-size=10MB`, `max-request-size=11MB`; validated in `FileValidationService` before S3 upload |
| S3 bucket privacy | Buckets remain private; all access via pre-signed URLs — no public `GetObject` policy |

#### New env vars
```
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_REGION=ap-south-1
AWS_S3_BUCKET_RESUMES=placesync-resumes
AWS_S3_BUCKET_PICTURES=placesync-profile-pictures
```

---

### 5.3 Email Delivery

#### New dependencies
```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-mail</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-thymeleaf</artifactId>
</dependency>
```

#### Configuration
```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${MAIL_USERNAME}
    password: ${MAIL_APP_PASSWORD}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.smtp.starttls.required: true
```

#### Replace `EmailService` stub
| File | Purpose |
|---|---|
| `auth/service/EmailService.java` | Replace log-stub with real `JavaMailSender` + Thymeleaf rendering; all sends are `@Async` |
| `common/config/AsyncConfig.java` | `@EnableAsync` + `ThreadPoolTaskExecutor` for email threads (core=2, max=5, queue=50) |

All email sends must be fire-and-forget: failures are logged at WARN level but never propagate exceptions to the caller.

#### Email templates (`src/main/resources/templates/email/`)
| Template | Trigger |
|---|---|
| `email-verification.html` | Registration |
| `password-reset.html` | Forgot password |
| `account-locked.html` | 5 consecutive failed logins |
| `recruiter-approved.html` | Admin approves recruiter |
| `recruiter-rejected.html` | Admin rejects recruiter |
| `application-confirmation.html` | Student applies |
| `application-status-update.html` | Recruiter updates application status |
| `interview-scheduled.html` | Interview scheduled |
| `interview-rescheduled.html` | Interview rescheduled |
| `interview-cancelled.html` | Interview cancelled |

All templates use consistent PlaceSync branding (header, footer, colors). Variables are injected via Thymeleaf `th:text`, `th:href`.

---

### 5.4 Spring-Level Security Headers

**Why:** Security headers are set at Nginx in production, but the application itself should also set them so they apply in non-Nginx environments (local dev, CI integration tests) and provide defense-in-depth.

Update `SecurityConfig.java`:
```java
http.headers(headers -> headers
    .frameOptions(HeadersConfigurer.FrameOptionsConfig::deny)
    .contentTypeOptions(Customizer.withDefaults())
    .httpStrictTransportSecurity(hsts -> hsts
        .includeSubDomains(true)
        .maxAgeInSeconds(31536000))
    .referrerPolicy(referrer -> referrer
        .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN))
);
```

---

### 5.5 Supabase Production Integration

**Why here:** All Phase 5 features are now verified against Docker PostgreSQL. Before this branch is merged, the application must be proven to start cleanly against the managed production database. This subphase has no code changes — it is purely configuration, connection verification, and schema migration.

**Docker PostgreSQL continues unchanged** for local development and CI after this subphase. Supabase is an additional target environment, not a replacement.

#### Prerequisites
- Phase 5 features (5.1 – 5.4) are complete and `mvn verify` passes against Docker PostgreSQL
- A Supabase account and project exist (free tier is sufficient for V1)

#### Tasks

**1. Create the Supabase project**
- Create a new project at [supabase.com](https://supabase.com) in the region closest to the VPS (e.g., `ap-south-1`)
- Note the project URL and service role key from the Supabase dashboard
- Retrieve the direct connection string (not the pooler) for Flyway migrations:
  `jdbc:postgresql://<host>:5432/postgres`
- Retrieve the transaction pooler connection string (PgBouncer) for the running application:
  `jdbc:postgresql://<host>:6543/postgres?pgbouncer=true`

**2. Configure environment variables for production**

Add to `.env.example` (do not commit real values):
```
# Production database (Supabase)
PROD_DATABASE_URL=jdbc:postgresql://<supabase-host>:6543/postgres?pgbouncer=true
PROD_DATABASE_USERNAME=postgres
PROD_DATABASE_PASSWORD=<supabase-db-password>
```

Create a local `.env.prod` file (git-ignored) for production smoke testing:
```
DATABASE_URL=jdbc:postgresql://<supabase-host>:6543/postgres?pgbouncer=true
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<supabase-db-password>
```

Verify `.env.prod` is in `.gitignore` before proceeding.

**3. Configure SSL**

Supabase requires SSL. Add `?sslmode=require` to the connection URL if not already implied by the `pgbouncer=true` parameter. Verify the connection is encrypted by checking the Supabase dashboard connection logs.

**4. Run Flyway migrations against Supabase**

Run migrations using the direct connection (not the pooler — Flyway requires a persistent connection):
```bash
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot" \
  mvn flyway:migrate \
  -Dflyway.url="jdbc:postgresql://<supabase-host>:5432/postgres?sslmode=require" \
  -Dflyway.user=postgres \
  -Dflyway.password=<password>
```

Verify all 19 migrations (V001–V019) applied cleanly in the Supabase SQL editor: `SELECT * FROM flyway_schema_history ORDER BY installed_rank;`

**5. Validate schema consistency**

Spot-check key tables and enum types in the Supabase SQL editor:
```sql
SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY table_name;
SELECT typname FROM pg_type WHERE typtype = 'e' ORDER BY typname;
```

Confirm all 18 tables and all 12 custom enum types are present.

**6. Smoke test the application against Supabase**

Start the application locally pointing at Supabase (Redis and Kafka still from Docker Compose):
```bash
JAVA_HOME="C:/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot" \
  mvn spring-boot:run \
  -Dspring.datasource.url="${PROD_DATABASE_URL}" \
  -Dspring.datasource.username="${PROD_DATABASE_USERNAME}" \
  -Dspring.datasource.password="${PROD_DATABASE_PASSWORD}"
```

Verify:
- Application starts without errors
- `GET /actuator/health` returns `{"status":"UP"}`
- `POST /api/v1/auth/register` creates a user row in the Supabase `users` table
- `POST /api/v1/auth/login` returns a valid JWT

**7. Update `docs/DEPLOYMENT.md`**

Add a section documenting the two-environment database setup:
- How to obtain Supabase connection strings (direct vs. pooler)
- When to use each connection string (Flyway = direct, application = pooler)
- Environment variable mapping for VPS `.env` file
- How to verify Flyway migration status on Supabase

#### No code changes required

This subphase produces no Java source changes. The application is already provider-agnostic. If any code change is needed to make the application work with Supabase, that is a bug in the application's configuration isolation and must be fixed before this subphase is marked complete.

#### Subphase 5.5 acceptance criteria
- [ ] Supabase project created and connection strings retrieved
- [ ] `flyway:migrate` runs cleanly against Supabase — all 19 migrations applied, `flyway_schema_history` shows no errors
- [ ] All 18 tables and 12 custom enum types present in Supabase schema
- [ ] Application starts successfully when `DATABASE_URL` points to Supabase
- [ ] `GET /actuator/health` returns `{"status":"UP"}` against Supabase
- [ ] Register + login smoke test succeeds and data is visible in Supabase dashboard
- [ ] `.env.prod` is git-ignored and no production credentials are committed
- [ ] `docs/DEPLOYMENT.md` updated with Supabase connection and migration instructions
- [ ] Docker PostgreSQL is still used for all `mvn verify` runs — no changes to dev or CI configuration

---

### Phase 5 acceptance criteria
- [ ] `mvn verify` passes
- [ ] Admin analytics dashboard returns accurate placement rate, top companies, and per-department stats
- [ ] Analytics response is served from Redis cache on second call (verified via Redis monitor or logs)
- [ ] Student can upload a PDF resume (file stored in S3; metadata in DB with `s3_key`)
- [ ] Uploading a non-PDF file returns 400 with message "Only PDF files are accepted"
- [ ] Uploading a file exceeding 10 MB returns 400 with message "File size exceeds the 10 MB limit"
- [ ] Student can retrieve a pre-signed download URL for their resume (15-min expiry)
- [ ] Recruiter can retrieve a pre-signed URL only for resumes of students who applied to their jobs
- [ ] Student can upload a profile picture (JPEG/PNG, ≤ 5 MB); URL visible in profile response
- [ ] Company logo upload works; logo URL visible in company response
- [ ] Email verification link sent on registration opens a valid page
- [ ] Password reset email contains a working link
- [ ] Interview-scheduled email received by student when recruiter schedules an interview
- [ ] Email failures do not return an error to the caller — logged as WARN
- [ ] Security headers (`X-Frame-Options`, `X-Content-Type-Options`, `Strict-Transport-Security`) present in all API responses
- [ ] Merged to `main` via PR

---

## Phase 6 — Testing Suite + CI/CD + Nginx + Production Hardening

**Status:** ⬜ Not started
**Planned branch:** `feat/cicd-production`
**Depends on:** Phase 5 (all features complete before CI enforces quality gates)

### Scope

1. **Comprehensive test suite** — repository tests, controller integration tests, security tests, Testcontainers
2. **GitHub Actions CI pipeline** — build, test, SonarCloud quality gate
3. **Nginx reverse proxy** — TLS termination, rate limiting, static SPA serving, security headers
4. **Production hardening** — structured JSON logging, Micrometer metrics, scheduled maintenance jobs, scheduled job expiry, Google OAuth2, VPS hardening documentation, request size limits, rate limiting

---

### 6.1 Comprehensive Testing Suite

#### Repository tests (Spring Data slice tests)
Use `@DataJpaTest` with an embedded H2 or a real PostgreSQL via Testcontainers.

**Testcontainers dependency:**
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>kafka</artifactId>
    <scope>test</scope>
</dependency>
```

**Repository tests to create:**

| Test class | Repository under test | Key scenarios |
|---|---|---|
| `UserRepositoryTest` | `UserRepository` | Find by email, find active by email, unique email constraint |
| `JobRepositoryTest` | `JobRepository` | Find open jobs paginated, find by recruiter, deleted jobs excluded |
| `ApplicationRepositoryTest` | `ApplicationRepository` | Unique constraint (student + job), find by student, find by job |
| `NotificationRepositoryTest` | `NotificationRepository` | Find unread by user, count unread, find paginated by user |
| `AuditLogRepositoryTest` | `AuditLogRepository` | Search by entity type and time range |
| `RefreshTokenRepositoryTest` | `RefreshTokenRepository` | Find by token hash, revoke by user, family invalidation |

All repository tests run against a real PostgreSQL via `@Testcontainers` + `@Container PostgreSQLContainer` to validate SQL, constraints, and indexes — not against H2 which does not support PostgreSQL-specific features (custom ENUMs, `JSONB`, `INET`).

#### Controller integration tests
Use `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `@Testcontainers` for PostgreSQL and Redis.

**Integration test classes:**

| Test class | Controller under test | Key scenarios |
|---|---|---|
| `AuthControllerIT` | `AuthController` | Full register→login→refresh→logout cycle, invalid credentials, expired token |
| `JobControllerIT` | `JobController` | Create job (as recruiter), admin approve, student list open jobs (cache hit), unauthorized access |
| `ApplicationControllerIT` | `ApplicationController` | Full apply→shortlist→interview cycle, duplicate apply → 409, cross-user access → 403 |
| `NotificationControllerIT` | `NotificationController` | Apply triggers notification, mark as read, unread count |
| `AdminControllerIT` | Admin controllers | Verify recruiter, approve company, user deactivation, audit log search |
| `ResumeControllerIT` | `ResumeController` | Upload metadata, set default, delete, access control |

**Integration test conventions:**
- Each test class is `@Transactional` with `@Rollback(true)` OR uses `@Sql("/test-data/reset.sql")` before each test to ensure isolation
- Use `TestRestTemplate` or `MockMvc` (prefer `MockMvc` for speed)
- Test authentication by obtaining a real JWT via `POST /api/v1/auth/login` in a `@BeforeEach`
- Do not mock repositories or services in integration tests — let the real stack run

#### Security tests
| Test class | Scenarios |
|---|---|
| `SecurityFilterChainTest` | Public endpoints accessible without token, protected endpoints return 401 without token |
| `RbacTest` | Student token rejected on recruiter endpoints (403), recruiter token rejected on admin endpoints (403), admin can access all |
| `JwtValidationTest` | Expired token → 401, tampered signature → 401, missing `Authorization` header → 401 |
| `CrossUserAccessTest` | Student A cannot read Student B's applications, Student A cannot read Student B's resumes |
| `RefreshTokenSecurityTest` | Used refresh token rejected on second use, entire family invalidated on reuse detection |

#### Micrometer + metrics
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

Expose `/actuator/prometheus` (restricted to internal network via Nginx). Key custom metrics:
| Metric | Type | Description |
|---|---|---|
| `placesync.jobs.applied.total` | Counter | Total applications submitted |
| `placesync.interviews.scheduled.total` | Counter | Total interviews scheduled |
| `placesync.cache.hit.ratio` | Gauge | Redis hit/miss ratio (from Spring Cache metrics) |
| `placesync.kafka.publish.failures.total` | Counter | Kafka publish failures that fell back to Spring Events |
| `placesync.email.send.failures.total` | Counter | Email send failures |

---

### 6.2 GitHub Actions CI Pipeline — Advanced

The basic workflow (checkout, Java 21 setup, Maven cache, `mvn clean verify`) was established in subphase 4.0. This subphase **expands** `.github/workflows/ci.yml` in place — do not create a new file, edit the existing one.

#### Changes to `.github/workflows/ci.yml`

Add two new steps after the unit-test step:

**Step 1 — PostgreSQL + Redis service containers** (required by Testcontainers integration tests):
```yaml
services:
  postgres:
    image: postgres:16-alpine
    env:
      POSTGRES_DB: placesync_test
      POSTGRES_USER: placesync
      POSTGRES_PASSWORD: test_secret
    ports:
      - "5432:5432"
    options: >-
      --health-cmd pg_isready
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    options: >-
      --health-cmd "redis-cli ping"
      --health-interval 10s
      --health-timeout 5s
      --health-retries 5
```

**Step 2 — JaCoCo coverage report** (after `mvn verify`):
```yaml
- name: Publish coverage report
  uses: actions/upload-artifact@v4
  with:
    name: jacoco-report
    path: target/site/jacoco/
```

**Step 3 — SonarCloud analysis** (runs only on push to `main`, not on every PR):
```yaml
- name: SonarCloud analysis
  if: github.ref == 'refs/heads/main'
  env:
    SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
  run: mvn -B sonar:sonar
```

**SonarCloud quality gate (enforced on `main` pushes):**
- 0 blocker bugs
- 0 critical security vulnerabilities
- ≥ 70% line coverage on service layer
- < 5% code duplication

#### New file: `.github/workflows/pr-checks.yml`

Fast compile-only check on every PR (< 2 min feedback, no service containers):
```yaml
name: PR checks
on:
  pull_request:
    branches: [main]
jobs:
  compile:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with:
          java-version: "21"
          distribution: "temurin"
          cache: maven
      - run: mvn -B compile -q
```

#### GitHub Actions secrets required
| Secret | Purpose |
|---|---|
| `SONAR_TOKEN` | SonarCloud authentication |
| `TEST_JWT_SECRET` | Used by integration tests only |

Production secrets (AWS, Gmail, JWT) are never stored in CI.

---

### 6.3 Nginx Reverse Proxy

#### Files to create
| File | Purpose |
|---|---|
| `nginx/nginx.conf` | Full production Nginx config |
| `nginx/conf.d/placesync.conf` | Server block — TLS, proxy, static, rate limiting |

**Nginx responsibilities:**
- HTTP → HTTPS redirect (301)
- TLS termination (Let's Encrypt / Certbot)
- `/api/*` → `http://api:8080` with proxy headers
- `/` → React SPA static files at `/usr/share/nginx/html`; `try_files $uri /index.html` for client-side routing
- Rate limiting: `limit_req_zone` — 100 req/min per IP on `/api/`
- Security headers: HSTS, `X-Frame-Options: DENY`, `X-Content-Type-Options: nosniff`, `Referrer-Policy`, CSP
- `/actuator/` — restricted to `127.0.0.1` only
- `/swagger-ui/` — disabled or restricted by IP in production

Add `nginx` service to `docker-compose.yml` (production variant).

---

### 6.4 Production Hardening

#### Structured JSON logging (production profile)
Update `application-prod.yml`:
```yaml
logging:
  level:
    root: WARN
    com.placesync: INFO
```

Update `logback-spring.xml` (prod profile): JSON appender using `logstash-logback-encoder`. Each line: `timestamp`, `level`, `logger`, `correlationId`, `message`, `exception`.

#### Scheduled maintenance jobs
Create `common/scheduler/MaintenanceScheduler.java` with `@EnableScheduling`:

| Job | Schedule | Action |
|---|---|---|
| Expired refresh token cleanup | Daily at 02:00 UTC | `DELETE FROM refresh_tokens WHERE expires_at < NOW() OR is_revoked = true` |
| Expired email verification token cleanup | Daily at 02:15 UTC | `DELETE FROM email_verification_tokens WHERE expires_at < NOW()` |
| Expired password reset token cleanup | Daily at 02:30 UTC | `DELETE FROM password_reset_tokens WHERE expires_at < NOW()` |
| Job deadline expiry | Every hour | `UPDATE jobs SET status = 'EXPIRED' WHERE application_deadline < NOW() AND status = 'OPEN'` (SRS JOB-FR-005) |

#### Google OAuth2 (deferred from Phase 2)
Add `spring-boot-starter-oauth2-client`. Implement `OAuth2LoginSuccessHandler`:
- If user exists: load and issue PlaceSync JWT pair
- If user does not exist: auto-provision with `ROLE_STUDENT`, issue JWT pair
- New endpoint: `GET /api/v1/auth/oauth2/callback` — frontend receives token here

New env vars: `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`

#### Request size limits
```yaml
spring:
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 11MB
```

Add `MaxUploadSizeExceededException` handler to `GlobalExceptionHandler` → 400 with clear message.

#### Swagger UI — production gating
Disable Swagger UI in production profile:
```yaml
# application-prod.yml
springdoc:
  swagger-ui:
    enabled: false
  api-docs:
    enabled: false
```

#### Comprehensive OpenAPI / Swagger annotations
For all existing controllers, add:
- `@Tag(name, description)` on each controller
- `@Operation(summary, description)` on each endpoint
- `@ApiResponse` annotations for all returned status codes (200, 201, 400, 401, 403, 404, 409, 500)
- `@Schema(description, example)` on all DTO fields
- Request/response body examples in `@Content`

This ensures Swagger UI is a complete, accurate API reference.

#### VPS setup documentation
Expand `docs/DEPLOYMENT.md` (started in subphase 5.5) to cover full VPS deployment:
- UFW firewall rules (22, 80, 443 only)
- Docker + Docker Compose installation
- Certbot / Let's Encrypt SSL setup
- systemd service for Docker Compose auto-restart on reboot
- Fail2ban for SSH brute-force protection
- `.env` file setup on VPS — including `DATABASE_URL` pointing to Supabase (not a local PostgreSQL container; the VPS runs no database service of its own)
- Running `flyway:migrate` against Supabase before first deploy

---

### Phase 6 acceptance criteria
- [ ] `mvn verify` passes — all unit + integration + security tests pass
- [ ] Repository tests run against real PostgreSQL via Testcontainers
- [ ] `GET /api/v1/admin/applications` (admin token) → 200; `GET /api/v1/admin/applications` (student token) → 403
- [ ] Used refresh token rejected on second use; full family invalidated
- [ ] Service layer unit test coverage ≥ 70% (JaCoCo report)
- [ ] `git push` to `main` triggers the expanded CI pipeline (integration tests + SonarCloud) — all stages pass
- [ ] SonarCloud quality gate passes (0 critical bugs, 0 security vulnerabilities, ≥ 70% coverage)
- [ ] Nginx serves `GET /api/v1/auth/me` correctly at `http://localhost/api/v1/auth/me`
- [ ] Nginx serves the React SPA static files at `http://localhost/`
- [ ] `/actuator/` returns 403 when accessed from outside localhost via Nginx
- [ ] Swagger UI returns 404 in production profile
- [ ] Scheduled job expiry runs — jobs past deadline are set to EXPIRED
- [ ] Token cleanup job removes expired rows from `refresh_tokens`
- [ ] `GET /actuator/prometheus` returns Prometheus-formatted metrics
- [ ] Custom metric `placesync.jobs.applied.total` increments on each application
- [ ] Google OAuth2 login provisions a new student account if the email is not known
- [ ] All API responses include `X-Correlation-ID` header matching the request log
- [ ] Merged to `main` via PR — backend is production-deployable

---

## Phase 7 — Frontend: React + TypeScript + Vite

**Status:** ⬜ Not started
**Planned branch:** `feat/frontend`
**Depends on:** Phase 6 (all backend endpoints must be stable before frontend integration)

### Scope

A React 18 SPA with TypeScript and Vite, consuming the PlaceSync REST API. Three distinct role-aware dashboards: Student, Recruiter, and Admin. Serves static files via Nginx in production.

### Technology stack

| Technology | Role |
|---|---|
| React 18 | Component framework |
| TypeScript 5 | Static typing |
| Vite 5 | Build tool (fast HMR in dev, optimized bundle in production) |
| React Router v6 | Client-side routing with nested layouts |
| TanStack Query (React Query v5) | Server state — fetching, caching, background refetch |
| Axios | HTTP client with interceptors for auth token injection and 401 handling |
| Material UI (MUI) v5 | Component library — consistent, accessible, responsive UI |
| React Hook Form + Zod | Form state management + schema-driven validation |
| Zustand | Lightweight client state management for auth context |
| Vitest + React Testing Library | Component and hook unit tests |

---

### 7.1 Project Setup

#### Scaffold
```bash
npm create vite@latest placesync-frontend -- --template react-ts
cd placesync-frontend
npm install @mui/material @emotion/react @emotion/styled
npm install @tanstack/react-query axios react-router-dom
npm install react-hook-form zod @hookform/resolvers
npm install zustand
npm install -D vitest @testing-library/react @testing-library/user-event jsdom
```

#### Folder structure
```
src/
├── api/              ← Axios instance + module-level API functions
│   ├── axiosClient.ts
│   ├── authApi.ts
│   ├── jobApi.ts
│   ├── applicationApi.ts
│   ├── interviewApi.ts
│   ├── notificationApi.ts
│   ├── resumeApi.ts
│   ├── adminApi.ts
│   └── analyticsApi.ts
├── components/
│   ├── common/       ← Button, Modal, DataTable, LoadingSpinner, ConfirmDialog
│   └── layout/       ← Navbar, Sidebar, PageWrapper, RoleBadge
├── pages/
│   ├── auth/         ← LoginPage, RegisterPage, ForgotPasswordPage, ResetPasswordPage
│   ├── student/      ← StudentDashboard, ProfilePage, JobsPage, JobDetailPage, ApplicationsPage, InterviewsPage, ResumesPage
│   ├── recruiter/    ← RecruiterDashboard, ProfilePage, JobsPage, CreateJobPage, ApplicationsPage, ScheduleInterviewPage
│   └── admin/        ← AdminDashboard, UsersPage, UserDetailPage, RecruitersPage, CompaniesPage, JobsPage, AuditLogPage, AnalyticsPage
├── hooks/            ← useAuth, useProfile, useJobs, useApplications, useNotifications, useResumes
├── store/            ← authStore.ts (Zustand — stores accessToken, user role, userId)
├── types/            ← TypeScript interfaces mirroring backend DTOs
├── utils/            ← dateFormatter, errorHandler, fileSizeFormatter
└── routes/           ← router.tsx, PrivateRoute.tsx, RoleRoute.tsx
```

---

### 7.2 Authentication & Token Management

#### Axios interceptor (`src/api/axiosClient.ts`)
```
Request → Attach Authorization: Bearer <accessToken>
Response 401 → POST /api/v1/auth/refresh → Get new tokens → Retry original request
Response 401 (refresh failed) → Clear auth store → Redirect to /login
```

- Access token stored in Zustand (in-memory — not localStorage to avoid XSS risk)
- Refresh token stored in `localStorage` (acceptable for V1; HttpOnly cookie preferred in V2)
- On page refresh: check localStorage for refresh token → silently refresh → restore session

#### Auth store (`src/store/authStore.ts`)
```typescript
interface AuthState {
  accessToken: string | null;
  userId: string | null;
  email: string | null;
  role: 'ROLE_STUDENT' | 'ROLE_RECRUITER' | 'ROLE_ADMIN' | null;
  isAuthenticated: boolean;
  login: (authResponse: AuthResponse) => void;
  logout: () => void;
}
```

#### Route guards (`src/routes/`)
- `PrivateRoute` — redirects to `/login` if not authenticated
- `RoleRoute` — accepts `allowedRoles: string[]`; redirects to `/403` if role doesn't match

#### Route map
```
/                       → PublicLanding (or redirect to dashboard if authenticated)
/login                  → LoginPage
/register               → RegisterPage
/forgot-password        → ForgotPasswordPage
/reset-password?token=  → ResetPasswordPage
/verify-email?token=    → EmailVerificationPage
/403                    → ForbiddenPage

/student/*              → RoleRoute (ROLE_STUDENT)
  /student/dashboard
  /student/profile
  /student/resumes
  /student/jobs
  /student/jobs/:jobId
  /student/applications
  /student/interviews

/recruiter/*            → RoleRoute (ROLE_RECRUITER)
  /recruiter/dashboard
  /recruiter/profile
  /recruiter/jobs
  /recruiter/jobs/create
  /recruiter/jobs/:jobId/edit
  /recruiter/jobs/:jobId/applications
  /recruiter/jobs/:jobId/applications/:applicationId

/admin/*                → RoleRoute (ROLE_ADMIN)
  /admin/dashboard
  /admin/users
  /admin/users/:userId
  /admin/recruiters/pending
  /admin/companies/pending
  /admin/jobs/pending
  /admin/applications
  /admin/interviews
  /admin/analytics
  /admin/audit-log
```

---

### 7.3 Student Dashboard & Features

| Page | Key components | API calls |
|---|---|---|
| `StudentDashboard` | Stats cards (applications submitted, interviews upcoming, offers), recent activity feed | `GET /api/v1/students/applications`, `GET /api/v1/students/interviews` |
| `ProfilePage` | Form with all profile fields; skills chip list; education and experience accordions; profile picture upload | `GET/PUT /api/v1/students/profile`, `POST /api/v1/students/profile/skills`, profile picture PATCH |
| `ResumesPage` | Resume cards with label, size, upload date; default badge; upload button; delete | `GET/POST/PATCH/DELETE /api/v1/students/resumes` |
| `JobsPage` | Paginated job card grid; filter sidebar (locationType, jobType, keyword); deadline badge | `GET /api/v1/jobs` with filter params |
| `JobDetailPage` | Full job info; eligibility check display; Apply button with resume selector | `GET /api/v1/jobs/:jobId`, `POST /api/v1/applications` |
| `ApplicationsPage` | Application table with status chip and timeline; status history | `GET /api/v1/students/applications` |
| `InterviewsPage` | Interview cards with round, type, date/time, meeting link; upcoming vs past tabs | `GET /api/v1/students/interviews` |

**Notification badge:** Navbar fetches `GET /api/v1/notifications/unread-count` every 60 seconds (polling — WebSocket is V2). Bell icon shows count. Clicking opens a notification drawer.

---

### 7.4 Recruiter Dashboard & Features

| Page | Key components | API calls |
|---|---|---|
| `RecruiterDashboard` | Stats: open jobs, total applications, shortlisted, offers; recent applicants | `GET /api/v1/recruiters/jobs`, `GET /api/v1/analytics/recruiter-stats` |
| `ProfilePage` | Name, title, contact, company selector (dropdown of verified companies); verification status badge | `GET/PUT /api/v1/recruiters/profile`, `GET /api/v1/companies` |
| `JobsPage` | Jobs table with status, application count, deadline; Create Job button | `GET /api/v1/recruiters/jobs` |
| `CreateJobPage` | Multi-section form: basic info, eligibility (CGPA, departments), skills chip input, deadline picker | `POST /api/v1/jobs` |
| `ApplicationsPage` (per job) | Applicant table with status selector dropdown; view student profile button; download resume button | `GET /api/v1/recruiters/jobs/:jobId/applications`, `PATCH /api/v1/recruiters/applications/:id/status` |
| `ScheduleInterviewPage` | Form: round, type, date/time, duration, meeting link; reschedule and cancel actions | `POST/PUT/PATCH /api/v1/recruiters/applications/:id/interviews` |

---

### 7.5 Admin Dashboard & Features

| Page | Key components | API calls |
|---|---|---|
| `AdminDashboard` | Platform-wide analytics: total students, recruiters, companies, jobs, applications, placement rate | `GET /api/v1/analytics/placement-stats` |
| `AnalyticsPage` | Bar charts (top companies, top departments), placement rate gauge | `GET /api/v1/analytics/company-breakdown`, `GET /api/v1/analytics/department-breakdown` |
| `UsersPage` | Searchable user table (email, role, status); activate/deactivate toggle | `GET /api/v1/admin/users`, `PATCH /api/v1/admin/users/:id/status` |
| `RecruitersPage` | Pending recruiters list with profile detail; Approve / Reject buttons | `GET /api/v1/admin/recruiters/pending`, `PATCH /api/v1/admin/recruiters/:id/verify` |
| `CompaniesPage` | Pending companies list; Approve / Reject buttons | `GET /api/v1/admin/companies/pending`, `PATCH /api/v1/admin/companies/:id/verify` |
| `JobsPage` (admin) | Pending jobs list; Approve / Reject buttons | `GET /api/v1/admin/jobs/pending`, `PATCH /api/v1/admin/jobs/:id/approve` |
| `ApplicationsPage` (admin) | All applications across all jobs; filter by status | `GET /api/v1/admin/applications` |
| `InterviewsPage` (admin) | All interviews across all recruiters | `GET /api/v1/admin/interviews` |
| `AuditLogPage` | Searchable table: entity type, actor, action, timestamp, before/after diff viewer | `GET /api/v1/admin/audit-log` |

---

### 7.6 Shared UI Components

| Component | Purpose |
|---|---|
| `DataTable<T>` | Generic paginated table with sortable columns, per-page size selector |
| `StatusChip` | Maps `ApplicationStatus`, `JobStatus`, `VerificationStatus` to coloured MUI `Chip` |
| `ConfirmDialog` | Reusable confirmation modal for destructive actions (deactivate user, cancel interview) |
| `FileUploadButton` | Validates file type and size client-side before calling upload API |
| `NotificationDrawer` | Slide-out panel listing notifications; "Mark all read" button |
| `ProfileCompletenessBar` | Linear progress bar showing student profile completeness % (SRS STU-FR-007) |
| `LoadingSpinner` | Full-page and inline loading indicators |
| `ErrorBoundary` | Wraps all route components; renders friendly error page on unhandled JS error |

---

### 7.7 Frontend Testing

Use Vitest + React Testing Library. All tests run headlessly in CI.

| Test file | What is tested |
|---|---|
| `authStore.test.ts` | Login sets tokens, logout clears state, role is readable |
| `axiosClient.test.ts` | 401 response triggers token refresh; refresh failure clears auth and redirects |
| `PrivateRoute.test.tsx` | Unauthenticated user is redirected to `/login` |
| `RoleRoute.test.tsx` | Wrong-role user is redirected to `/403` |
| `LoginPage.test.tsx` | Form validation shown on empty submit; successful login redirects by role |
| `JobsPage.test.tsx` | Jobs render from mocked API; keyword filter updates query |
| `ApplicationsPage.test.tsx` | Applications render; status chip matches status enum |
| `NotificationDrawer.test.tsx` | Unread count badge shows; marking read decrements count |

Mock Axios calls using `vi.mock` or MSW (Mock Service Worker) for more realistic network mocking.

---

### 7.8 Docker Build & Nginx Static Serving

#### Dockerfile for frontend (multi-stage)
```dockerfile
FROM node:20-alpine AS build
WORKDIR /app
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build          # outputs to /app/dist

FROM nginx:alpine AS runtime
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx/spa.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
```

`nginx/spa.conf` — serves React SPA with fallback to `index.html`:
```nginx
location / {
    root   /usr/share/nginx/html;
    index  index.html;
    try_files $uri $uri/ /index.html;
}
```

#### Integration into `docker-compose.yml`
The `nginx` service (already planned in Phase 6) mounts `/usr/share/nginx/html` from the frontend build stage, serving the SPA while proxying `/api/*` to the Spring Boot API container.

#### Vite proxy (development only)
```typescript
// vite.config.ts
export default defineConfig({
  server: {
    proxy: {
      '/api': 'http://localhost:8080'
    }
  }
})
```

This lets `npm run dev` hit the local Spring Boot API without CORS issues during development.

---

### Phase 7 acceptance criteria
- [ ] `npm run build` completes without TypeScript errors
- [ ] `npm test` passes all Vitest tests
- [ ] Unauthenticated user visiting `/student/dashboard` is redirected to `/login`
- [ ] Student can register, verify email (link from console log in dev), and log in
- [ ] Student can browse and filter open jobs; apply to a job using a resume
- [ ] Student sees their application with the current status on the Applications page
- [ ] Student sees a notification when their application status changes
- [ ] Recruiter can create and manage job postings
- [ ] Recruiter can view applicants and move them through the status lifecycle
- [ ] Recruiter can schedule an interview; student sees it on their Interviews page
- [ ] Admin can approve a pending recruiter and pending company
- [ ] Admin dashboard shows accurate placement stats
- [ ] Admin can search audit log by entity type
- [ ] Profile completeness bar reflects actual filled fields
- [ ] App is responsive at 375 px (mobile) and 1920 px (desktop) — SRS NFR-050
- [ ] `docker-compose up` serves the SPA at `http://localhost/` via Nginx
- [ ] Merged to `main` via PR — PlaceSync V1 is functionally complete

---

## Appendix A — Branch and commit conventions

### Branch naming
```
feat/<scope>     — new feature or phase
fix/<scope>      — bug fix
chore/<scope>    — build, config, tooling changes
docs/<scope>     — documentation only
test/<scope>     — tests only
refactor/<scope> — refactoring without behaviour change
```

### Commit message format
```
<type>: <short summary>

[optional body]

Co-Authored-By: ...
```

**Types:** `feat`, `fix`, `chore`, `docs`, `test`, `refactor`

### PR per phase
Each phase should be one PR from its feature branch into `main`.

---

## Appendix B — Environment variables reference

| Variable | Phase introduced | Required in prod |
|---|---|---|
| `DATABASE_URL` | 1 | ✅ |
| `DATABASE_USERNAME` | 1 | ✅ |
| `DATABASE_PASSWORD` | 1 | ✅ |
| `JWT_SECRET` | 2 | ✅ |
| `JWT_ACCESS_TOKEN_EXPIRY_MS` | 2 | Optional (default 900000) |
| `JWT_REFRESH_TOKEN_EXPIRY_DAYS` | 2 | Optional (default 7) |
| `FRONTEND_URL` | 2 | ✅ |
| `APP_BASE_URL` | 2 | ✅ |
| `REDIS_HOST` | 3 | ✅ |
| `REDIS_PORT` | 3 | Optional (default 6379) |
| `KAFKA_BOOTSTRAP_SERVERS` | 4 | ✅ |
| `AWS_ACCESS_KEY_ID` | 5 | ✅ |
| `AWS_SECRET_ACCESS_KEY` | 5 | ✅ |
| `AWS_REGION` | 5 | ✅ |
| `AWS_S3_BUCKET_RESUMES` | 5 | ✅ |
| `AWS_S3_BUCKET_PICTURES` | 5 | ✅ |
| `MAIL_USERNAME` | 5 | ✅ |
| `MAIL_APP_PASSWORD` | 5 | ✅ |
| `GOOGLE_CLIENT_ID` | 6 | ✅ |
| `GOOGLE_CLIENT_SECRET` | 6 | ✅ |

---

## Appendix C — SRS requirement coverage map

| SRS Module | Requirements | Covered in Phase |
|---|---|---|
| AUTH | FR-001–042 | 2 (core), 6 (OAuth2, account lock email) |
| USER | FR-001–005 | 2 (profile), 4 (admin activate/deactivate/search) |
| STU | FR-001–009 | 2 (profile CRUD), 5 (profile picture, S3), 7 (frontend completeness bar) |
| REC | FR-001–006 | 2 (profile), 4 (email notification on verify) |
| COM | FR-001–005 | 2 (CRUD), 5 (logo S3) |
| RES | FR-001–008 | 3 (metadata), 5 (S3 upload + pre-signed URL) |
| JOB | FR-001–011 | 3 (CRUD, approval), 4 (filtering/search), 6 (deadline expiry scheduler) |
| APP | FR-001–011 | 3 (lifecycle), 4 (Kafka event, notification, status transition validation), 6 (admin global view) |
| INT | FR-001–007 | 3 (schedule/reschedule/cancel/complete), 4 (Kafka event, notification) |
| NOTIF | FR-001–008 | 4 (Kafka consumer, fallback, inbox endpoints) |
| EMAIL | FR-001–005 | 5 (Thymeleaf templates, Gmail SMTP, async) |
| ANL | FR-001–007 | 5 (aggregation, Redis cache) |
| AUD | FR-001–005 | 4 (AOP aspect, auth events, admin search API) |
| ADM | FR-001–008 | 4 (user mgmt, global views), 5 (analytics), 7 (admin frontend) |
| NFR | All NFRs | Progressively across phases 1–7 |

---

*End of Implementation Plan — PlaceSync V1.0*
