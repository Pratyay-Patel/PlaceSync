# Implementation Plan
# PlaceSync — SaaS Placement Management Platform

**Version:** 1.0.0
**Last updated:** 2026-06-23
**Author:** Pratyay Patel

---

## How to use this document

This file is the single source of truth for the phased build-out of PlaceSync. Update it as each phase is completed — mark the status, record what was built, and check off the acceptance criteria. It also serves as the changelog for version control: each phase maps to one or more Git branches and one merged PR.

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
| 3 | Jobs, Applications, Interviews, Resumes + Redis caching | 🔄 In progress | `feat/job-application-layer` |
| 4 | Notifications + Apache Kafka | ⬜ Not started | `feat/notification-kafka` |
| 5 | Analytics + AWS S3 + Email delivery | ⬜ Not started | `feat/analytics-s3-email` |
| 6 | CI/CD + Nginx + Production hardening | ⬜ Not started | `feat/cicd-production` |

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
| `src/main/resources/application-dev.yml` | Dev profile: local PostgreSQL, verbose SQL logging |
| `src/main/resources/application-prod.yml` | Prod profile: env-var-driven, HikariCP tuned |
| `src/test/resources/application.yml` | Test profile: H2 in-memory, Flyway disabled, ddl-auto=none |

#### Flyway migration
- `V001__create_enum_types.sql` — All 12 PostgreSQL custom ENUM types + `set_updated_at()` trigger function

ENUMs: `user_role`, `verification_status`, `company_status`, `job_status`, `job_location_type`, `job_type`, `application_status`, `interview_type`, `interview_status`, `notification_type`, `gender_type`, `audit_action`

#### Docker
| File | Purpose |
|---|---|
| `Dockerfile` | Multi-stage build (Maven compile → lean JRE runtime, non-root user) |
| `docker-compose.yml` | Phase 1 stack: PostgreSQL 16 + Spring Boot API |
| `.dockerignore` | Excludes target/, .git/, .env from build context |
| `.env.example` | Environment variable template (all phases documented) |

#### Test
- `PlaceSyncApplicationTests.java` — `@SpringBootTest` context load test using H2

---

## Phase 2 — Database Layer + Auth/User/Recruiter/Company Modules

**Status:** ✅ Complete
**Branch:** `feat/database-layer` (in progress — not yet merged)

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

#### JPA entities
| Entity | Package | Notes |
|---|---|---|
| `User` | `user.entity` | Central identity table |
| `UserRole` | `user.entity` | Enum: ROLE_STUDENT, ROLE_RECRUITER, ROLE_ADMIN |
| `StudentProfile` | `user.entity` | 1:1 with User |
| `StudentSkill` | `user.entity` | Child collection |
| `StudentEducation` | `user.entity` | Child collection |
| `StudentExperience` | `user.entity` | Child collection |
| `GenderType` | `user.entity` | Enum |
| `Resume` | `user.entity` | S3 metadata (bytes stored on S3, not here) |
| `RefreshToken` | `auth.entity` | JWT refresh token with family_id |
| `EmailVerificationToken` | `auth.entity` | Single-use, time-limited |
| `PasswordResetToken` | `auth.entity` | Single-use, 1-hour expiry |
| `Company` | `company.entity` | Soft-deletable |
| `CompanyStatus` | `company.entity` | Enum |
| `RecruiterProfile` | `recruiter.entity` | 1:1 with User |
| `VerificationStatus` | `recruiter.entity` | Enum |
| `Job` | `job.entity` | Soft-deletable, approval workflow |
| `JobStatus` | `job.entity` | Enum |
| `JobLocationType` | `job.entity` | Enum |
| `JobType` | `job.entity` | Enum |
| `JobRequiredSkill` | `job.entity` | Child collection |
| `JobEligibleDepartment` | `job.entity` | Child collection |
| `Application` | `application.entity` | Status lifecycle |
| `ApplicationStatus` | `application.entity` | Enum |
| `Interview` | `interview.entity` | Multi-round scheduling |
| `InterviewStatus` | `interview.entity` | Enum |
| `InterviewType` | `interview.entity` | Enum |
| `Notification` | `notification.entity` | Append-only inbox |
| `NotificationType` | `notification.entity` | Enum |
| `AuditLog` | `common.audit` | Immutable audit trail |
| `AuditAction` | `common.audit` | Enum |

#### Spring Data repositories
All repositories in their respective `repository/` sub-packages:
`UserRepository`, `StudentProfileRepository`, `StudentSkillRepository`, `StudentEducationRepository`, `StudentExperienceRepository`, `ResumeRepository`, `RefreshTokenRepository`, `EmailVerificationTokenRepository`, `PasswordResetTokenRepository`, `CompanyRepository`, `RecruiterProfileRepository`, `JobRepository`, `JobRequiredSkillRepository`, `JobEligibleDepartmentRepository`, `ApplicationRepository`, `InterviewRepository`, `NotificationRepository`, `AuditLogRepository`

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
| File | Purpose |
|---|---|
| `user/dto/StudentProfileResponse.java` | Profile read response |
| `user/dto/UpdateStudentProfileRequest.java` | Profile update request |
| `user/dto/StudentSkillRequest.java` | Add skill |
| `user/dto/StudentEducationRequest.java` | Add/update education |
| `user/dto/StudentExperienceRequest.java` | Add/update experience |
| `user/service/UserService.java` | Profile + skills + education + experience CRUD |
| `user/controller/StudentProfileController.java` | 13 endpoints under `/api/v1/students/` |

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
| File | Purpose |
|---|---|
| `recruiter/dto/RecruiterProfileResponse.java` | Profile read response |
| `recruiter/dto/UpdateRecruiterProfileRequest.java` | Profile update request |
| `recruiter/dto/RecruiterVerificationRequest.java` | Admin approve/reject |
| `recruiter/service/RecruiterService.java` | Profile CRUD + admin verification workflow |
| `recruiter/controller/RecruiterController.java` | 4 endpoints |

**Recruiter endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/recruiters/profile` | RECRUITER | Get own profile |
| PUT | `/api/v1/recruiters/profile` | RECRUITER | Update profile |
| GET | `/api/v1/admin/recruiters/pending` | ADMIN | List pending verifications |
| PATCH | `/api/v1/admin/recruiters/{id}/verify` | ADMIN | Approve or reject recruiter |

#### Company module
| File | Purpose |
|---|---|
| `company/dto/CompanyResponse.java` | Company read response |
| `company/dto/CreateCompanyRequest.java` | Create request |
| `company/dto/UpdateCompanyRequest.java` | Update request |
| `company/dto/CompanyVerificationRequest.java` | Admin approve/reject |
| `company/service/CompanyService.java` | Full CRUD + admin approval workflow |
| `company/controller/CompanyController.java` | 7 endpoints |

**Company endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/companies` | Any | List verified companies (paginated) |
| GET | `/api/v1/companies/{id}` | Any | Get company by ID |
| POST | `/api/v1/companies` | RECRUITER | Create company |
| PUT | `/api/v1/companies/{id}` | RECRUITER | Update company (creator only) |
| DELETE | `/api/v1/companies/{id}` | RECRUITER | Soft-delete company (creator only) |
| GET | `/api/v1/admin/companies/pending` | ADMIN | List pending approvals |
| PATCH | `/api/v1/admin/companies/{id}/verify` | ADMIN | Approve or reject company |

### Phase 2 acceptance criteria
- [x] Spring context loads with full security configuration
- [x] `mvn clean test` passes (1 test, BUILD SUCCESS)
- [x] All 19 Flyway migrations accounted for
- [x] Every entity has a repository
- [x] JWT access token issued on login, validated on protected routes
- [x] Refresh token rotation with family-based reuse detection
- [x] Student profile fully manageable (skills, education, experience)
- [x] Recruiter verification workflow operable by ADMIN
- [x] Company approval workflow operable by ADMIN
- [ ] Verify all endpoints manually against a running PostgreSQL instance
- [ ] Merge to `main` via PR

---

## Phase 3 — Jobs, Applications, Interviews + Resume Metadata + Redis Caching

**Status:** 🔄 In progress
**Branch:** `feat/job-application-layer`

### Scope

This phase builds the core placement workflow: posting jobs, applying to them, and scheduling interviews. Redis caching is introduced here for the hot read paths (job listings, individual job detail). Resume metadata management is also added so students can attach a resume to an application — actual S3 upload is deferred to Phase 5.

### New dependencies to add

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-data-redis` | Redis repository and template |
| `spring-boot-starter-cache` | Spring Cache abstraction (`@Cacheable`, `@CacheEvict`) |

### New configuration

In `application.yml`:
```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: ${REDIS_HOST:localhost}
      port: ${REDIS_PORT:6379}
      timeout: 2000ms
  cache:
    redis:
      time-to-live: 300000
      key-prefix: "placesync::"
      use-key-prefix: true
      cache-null-values: false
```

In `docker-compose.yml`: add the `redis` service (Redis 7 Alpine).

In `common/config/`: add `CacheConfig.java` to define per-cache TTLs.

### Files to create

#### Resume module (within `user/`)
| File | Notes |
|---|---|
| `user/dto/ResumeResponse.java` | Metadata response (no S3 URL yet — added in Phase 5) |
| `user/dto/CreateResumeRequest.java` | label, originalFilename, fileSizeBytes, isDefault |
| `user/service/ResumeService.java` | List, create metadata, set default, soft-delete |
| `user/controller/ResumeController.java` | 5 endpoints under `/api/v1/students/resumes/` |

**Resume endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/students/resumes` | STUDENT | List own resumes |
| POST | `/api/v1/students/resumes` | STUDENT | Upload resume metadata (S3 upload added in Phase 5) |
| PATCH | `/api/v1/students/resumes/{id}/default` | STUDENT | Set as default resume |
| DELETE | `/api/v1/students/resumes/{id}` | STUDENT | Soft-delete resume |
| GET | `/api/v1/students/resumes/{id}/url` | STUDENT/RECRUITER | Get pre-signed download URL (Phase 5) |

#### Job module (`job/`)
| File | Notes |
|---|---|
| `job/dto/JobResponse.java` | Full job detail |
| `job/dto/JobSummaryResponse.java` | Lightweight card for listings page |
| `job/dto/CreateJobRequest.java` | title, description, locationType, jobType, etc. |
| `job/dto/UpdateJobRequest.java` | Same fields, allowed only in DRAFT/PENDING_APPROVAL |
| `job/dto/JobApprovalRequest.java` | Admin: APPROVE or REJECT |
| `job/service/JobService.java` | CRUD, admin approval, status transitions, `@Cacheable` |
| `job/controller/JobController.java` | Endpoints for students, recruiters, admins |

**Job endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/jobs` | Any JWT | List open jobs (paginated, filtered) — cached |
| GET | `/api/v1/jobs/{id}` | Any JWT | Get job detail — cached |
| POST | `/api/v1/jobs` | RECRUITER | Create job (starts as PENDING_APPROVAL) |
| PUT | `/api/v1/jobs/{id}` | RECRUITER | Update job (DRAFT or PENDING_APPROVAL only) |
| DELETE | `/api/v1/jobs/{id}` | RECRUITER | Soft-delete job |
| PATCH | `/api/v1/jobs/{id}/close` | RECRUITER | Close job (set status = CLOSED) |
| GET | `/api/v1/recruiters/jobs` | RECRUITER | List own job postings |
| GET | `/api/v1/admin/jobs/pending` | ADMIN | List jobs pending approval |
| PATCH | `/api/v1/admin/jobs/{id}/approve` | ADMIN | Approve or reject job |

**Cache design:**
- `job-listings` — key by page/size/filter hash, TTL 5 min, evict on any job create/update/close
- `job-detail` — key by jobId, TTL 10 min, evict on job update/close

#### Application module (`application/`)
| File | Notes |
|---|---|
| `application/dto/ApplicationResponse.java` | Full application detail |
| `application/dto/ApplyRequest.java` | jobId, resumeId |
| `application/dto/UpdateApplicationStatusRequest.java` | Recruiter: new status + optional note |
| `application/service/ApplicationService.java` | Apply, list, status transitions |
| `application/controller/ApplicationController.java` | Endpoints for students and recruiters |

**Application endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/v1/applications` | STUDENT | Apply to a job |
| GET | `/api/v1/students/applications` | STUDENT | List own applications (paginated) |
| GET | `/api/v1/students/applications/{id}` | STUDENT | Get own application detail |
| GET | `/api/v1/recruiters/jobs/{jobId}/applications` | RECRUITER | List applicants for a job |
| PATCH | `/api/v1/recruiters/applications/{id}/status` | RECRUITER | Update application status |

**Business rules to enforce:**
- One application per student per job (DB UNIQUE constraint enforces this; return 409 on duplicate)
- Student must have an active resume to apply
- Job must be in `OPEN` status
- Student department must be in `job_eligible_departments` if that list is non-empty
- Student CGPA must be ≥ `job.min_cgpa` if set

#### Interview module (`interview/`)
| File | Notes |
|---|---|
| `interview/dto/InterviewResponse.java` | Interview detail |
| `interview/dto/ScheduleInterviewRequest.java` | applicationId, roundNumber, type, scheduledAt, duration, meetingLink/venue |
| `interview/dto/UpdateInterviewRequest.java` | Reschedule: new scheduledAt, meetingLink/venue |
| `interview/dto/CancelInterviewRequest.java` | cancellationReason |
| `interview/service/InterviewService.java` | Schedule, reschedule, cancel, complete |
| `interview/controller/InterviewController.java` | Endpoints for recruiters and students |

**Interview endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/students/interviews` | STUDENT | List own upcoming interviews |
| GET | `/api/v1/recruiters/applications/{id}/interviews` | RECRUITER | List interviews for an application |
| POST | `/api/v1/recruiters/applications/{id}/interviews` | RECRUITER | Schedule an interview round |
| PUT | `/api/v1/recruiters/interviews/{id}` | RECRUITER | Reschedule interview |
| PATCH | `/api/v1/recruiters/interviews/{id}/cancel` | RECRUITER | Cancel interview |
| PATCH | `/api/v1/recruiters/interviews/{id}/complete` | RECRUITER | Mark interview as completed |

### Phase 3 acceptance criteria
- [ ] `mvn clean test` passes
- [ ] `docker-compose up` brings up PostgreSQL + Redis + API
- [ ] Student can list open jobs, view job detail (responses served from Redis cache on second hit)
- [ ] Student can apply to an open job with a valid resume
- [ ] Duplicate application returns 409
- [ ] Recruiter can update application status through full lifecycle
- [ ] Recruiter can schedule, reschedule, and cancel interview rounds
- [ ] Cache eviction verified: job listing cache clears when a recruiter closes a job
- [ ] Merge to `main` via PR

---

## Phase 4 — Notifications + Apache Kafka

**Status:** ⬜ Not started
**Planned branch:** `feat/notification-kafka`
**Depends on:** Phase 3 (ApplicationService and InterviewService must exist to produce events)

### Scope

In-app notification inbox for students and recruiters. Events that trigger notifications (application submitted, status changed, interview scheduled) are published to Kafka topics by the existing service layer. A consumer in the `notification` module processes those events and writes `Notification` rows. A Spring Application Events fallback handles the case where Kafka is unavailable.

### New dependencies to add

| Dependency | Purpose |
|---|---|
| `spring-kafka` | Kafka producer + consumer |

### New configuration

In `application.yml`:
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

In `docker-compose.yml`: add the `kafka` service (Confluent KRaft mode, no ZooKeeper).

### Files to create

#### Kafka events (`common/event/` or `common/kafka/`)
| File | Notes |
|---|---|
| `common/event/ApplicationSubmittedEvent.java` | eventId, applicationId, studentId, jobId, jobTitle, companyName |
| `common/event/ApplicationStatusChangedEvent.java` | applicationId, studentId, oldStatus, newStatus |
| `common/event/InterviewScheduledEvent.java` | interviewId, applicationId, studentId, round, scheduledAt |
| `common/event/InterviewRescheduledEvent.java` | interviewId, studentId, oldScheduledAt, newScheduledAt |
| `common/event/InterviewCancelledEvent.java` | interviewId, studentId, cancellationReason |
| `common/event/OfferReleasedEvent.java` | applicationId, studentId, jobTitle, companyName |

#### Kafka producer
Producers live in the service layer of each module (ApplicationService, InterviewService). They use `KafkaTemplate` and fall back to `ApplicationEventPublisher` on failure.

| File | Notes |
|---|---|
| `common/kafka/KafkaEventPublisher.java` | Wrapper around `KafkaTemplate` with fallback to `ApplicationEventPublisher` |

#### Notification module (`notification/`)
| File | Notes |
|---|---|
| `notification/dto/NotificationResponse.java` | Notification read response |
| `notification/service/NotificationService.java` | Create notification, mark as read, count unread |
| `notification/consumer/NotificationConsumer.java` | `@KafkaListener` for all event topics |
| `notification/consumer/NotificationFallbackListener.java` | `@EventListener` + `@Async` fallback |
| `notification/controller/NotificationController.java` | Inbox endpoints for authenticated users |

**Notification endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/notifications` | Any JWT | List notifications (paginated, optional unread filter) |
| GET | `/api/v1/notifications/unread-count` | Any JWT | Count unread notifications |
| PATCH | `/api/v1/notifications/{id}/read` | Any JWT | Mark one notification as read |
| PATCH | `/api/v1/notifications/read-all` | Any JWT | Mark all notifications as read |

**Kafka topics:**
| Topic | Producer | Consumer |
|---|---|---|
| `application-events` | `ApplicationService` | `NotificationConsumer` |
| `interview-events` | `InterviewService` | `NotificationConsumer` |
| `offer-events` | `ApplicationService` | `NotificationConsumer` |

### Phase 4 acceptance criteria
- [ ] `mvn clean test` passes
- [ ] `docker-compose up` brings up PostgreSQL + Redis + Kafka + API
- [ ] Student receives a notification when they apply to a job
- [ ] Student receives a notification when their application status changes
- [ ] Student receives a notification when an interview is scheduled
- [ ] Notifications appear in the inbox endpoint
- [ ] Unread count decrements when notifications are marked as read
- [ ] Kafka fallback: shutting down Kafka does not prevent notifications from being created (Spring Events path verified)
- [ ] Merge to `main` via PR

---

## Phase 5 — Analytics + AWS S3 + Email Delivery

**Status:** ⬜ Not started
**Planned branch:** `feat/analytics-s3-email`
**Depends on:** Phase 4 (Kafka events power offer analytics)

### Scope

Three independent sub-systems that can be built in parallel and merged together:

1. **Analytics** — placement rate, offer count, company breakdown dashboards
2. **AWS S3** — actual file upload for resumes and profile pictures (replaces the Phase 3 metadata stubs)
3. **Email delivery** — replace the `EmailService` stub with real Thymeleaf templates + Gmail SMTP

### New dependencies to add

| Dependency | Purpose |
|---|---|
| `spring-boot-starter-mail` | JavaMailSender for SMTP |
| `spring-boot-starter-thymeleaf` | HTML email templates |
| `aws-java-sdk-s3` or `software.amazon.awssdk:s3` | AWS S3 SDK |

### Files to create

#### Analytics module (`analytics/`)
| File | Notes |
|---|---|
| `analytics/dto/PlacementStatsResponse.java` | totalStudents, placed, placementRate, avgPackage |
| `analytics/dto/CompanyStatsResponse.java` | Per-company offer count, job count |
| `analytics/service/AnalyticsService.java` | Aggregation queries — results cached with 10 min TTL |
| `analytics/controller/AnalyticsController.java` | Dashboard endpoints |

**Analytics endpoints:**
| Method | Path | Auth | Description |
|---|---|---|---|
| GET | `/api/v1/analytics/placement-stats` | ADMIN | Global placement statistics |
| GET | `/api/v1/analytics/company-breakdown` | ADMIN | Per-company offer counts |
| GET | `/api/v1/analytics/recruiter-stats` | RECRUITER | Own jobs/applications/offers |

**Cache design:**
- `analytics-dashboard` — key `global`, TTL 10 min, evict when a new offer is recorded
- `recruiter-analytics` — key by recruiterId, TTL 10 min, evict on application status change

#### AWS S3 integration
| File | Notes |
|---|---|
| `common/config/S3Config.java` | AWS credentials + S3Client bean |
| `common/storage/S3StorageService.java` | `uploadFile()`, `generatePresignedUrl()`, `deleteFile()` |

**Resume upload flow update:** Replace the Phase 3 metadata-only `ResumeController` with a multipart upload endpoint that streams the file to S3 and stores the S3 key in the `resumes` table.

**Profile picture upload:** Add `PATCH /api/v1/students/profile/picture` endpoint.

#### Email delivery
| File | Notes |
|---|---|
| `auth/service/EmailService.java` | Replace stub with real `JavaMailSender` implementation |
| `src/main/resources/templates/email-verification.html` | Thymeleaf template |
| `src/main/resources/templates/password-reset.html` | Thymeleaf template |
| `src/main/resources/templates/application-confirmation.html` | Thymeleaf template |
| `src/main/resources/templates/application-status-update.html` | Thymeleaf template |
| `src/main/resources/templates/interview-scheduled.html` | Thymeleaf template |
| `src/main/resources/templates/interview-rescheduled.html` | Thymeleaf template |
| `src/main/resources/templates/interview-cancelled.html` | Thymeleaf template |
| `src/main/resources/templates/recruiter-approved.html` | Thymeleaf template |
| `src/main/resources/templates/recruiter-rejected.html` | Thymeleaf template |

### Phase 5 acceptance criteria
- [ ] `mvn clean test` passes
- [ ] Student can upload a PDF resume (file stored in S3, metadata in DB)
- [ ] Student can download their own resume via pre-signed URL (15-min expiry)
- [ ] Recruiter can download a resume only if the student applied to their job
- [ ] Email verification link sent on registration is a real working link
- [ ] Password reset email contains a working link
- [ ] Interview scheduled email sent to student
- [ ] Admin analytics dashboard returns accurate counts
- [ ] Merge to `main` via PR

---

## Phase 6 — CI/CD + Nginx + Production Hardening

**Status:** ⬜ Not started
**Planned branch:** `feat/cicd-production`
**Depends on:** Phase 5 (all features must be complete before setting up CI/CD)

### Scope

GitHub Actions CI pipeline, full Docker Compose production stack, Nginx reverse proxy configuration, and final production hardening items.

### Files to create

#### GitHub Actions
| File | Notes |
|---|---|
| `.github/workflows/ci.yml` | Compile, test, SonarCloud analysis on push/PR to main |
| `.github/workflows/pr-checks.yml` | Fast compile check on pull requests |

**CI pipeline stages:**
1. Checkout + JDK 21 setup + Maven cache
2. Start PostgreSQL + Redis via GitHub Actions service containers
3. `mvn verify` (compile + unit tests + integration tests)
4. Publish test results + JaCoCo coverage report
5. SonarCloud analysis (on success)

#### Docker Compose update
Update `docker-compose.yml` to add `redis`, `kafka`, and `nginx` services for the full local development stack. The current Phase 1 file only has `postgres` + `api`.

#### Nginx
| File | Notes |
|---|---|
| `nginx/nginx.conf` | TLS termination, `/api/` proxy to Spring Boot, static SPA serving, rate limiting |

**Nginx responsibilities:**
- HTTP → HTTPS redirect (port 80 → 443)
- TLS termination (Let's Encrypt)
- `/api/*` → `http://api:8080`
- `/` → React SPA static files (`/usr/share/nginx/html`)
- Rate limiting: 100 req/min per IP
- Security headers: HSTS, X-Frame-Options, CSP, etc.
- `/actuator/` — restricted to localhost

#### Production hardening
- [ ] Enable structured JSON logging in `application-prod.yml` (Logback JSON encoder)
- [ ] Verify HikariCP pool sizes are tuned for prod load
- [ ] Add `@Scheduled` cleanup job for expired tokens (`refresh_tokens`, `email_verification_tokens`, `password_reset_tokens`)
- [ ] Add `@Scheduled` job to expire jobs whose `application_deadline` has passed (set status = EXPIRED)
- [ ] Add Google OAuth2 (`spring-boot-starter-oauth2-client`) — deferred from Phase 2
- [ ] VPS setup documentation (UFW, fail2ban, certbot, systemd service)

### Phase 6 acceptance criteria
- [ ] `git push` to `main` triggers CI — pipeline passes
- [ ] `docker-compose up` brings up the full stack (PostgreSQL, Redis, Kafka, API, Nginx)
- [ ] Nginx serves the API at `http://localhost/api/v1/auth/me`
- [ ] SonarCloud quality gate passes
- [ ] Expired token cleanup job runs without errors
- [ ] Merge to `main` via PR — project is production-deployable

---

## Appendix A — Branch and commit conventions

### Branch naming
```
feat/<scope>     — new feature or phase
fix/<scope>      — bug fix
chore/<scope>    — build, config, tooling changes
docs/<scope>     — documentation only
```

### Commit message format
```
<type>: <short summary>

[optional body]

Co-Authored-By: ...
```

**Types:** `feat`, `fix`, `chore`, `docs`, `test`, `refactor`

### PR per phase
Each phase should be one PR from its feature branch into `main`. Squash the commits down to logical units before merging.

---

## Appendix B — Environment variables reference

All variables used across phases. Set these in `.env` (never committed; use `.env.example` as template).

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

*End of Implementation Plan — PlaceSync V1.0*
