# Architecture Document
# PlaceSync — SaaS Placement Management Platform

**Version:** 1.0.0
**Status:** Draft
**Date:** 2026-06-23
**Author:** Pratyay Patel

---

## Table of Contents

1. [Architectural Overview](#1-architectural-overview)
2. [Architectural Goals](#2-architectural-goals)
3. [Architectural Style](#3-architectural-style)
4. [Modular Monolith Justification](#4-modular-monolith-justification)
5. [Frontend Architecture](#5-frontend-architecture)
6. [Backend Architecture](#6-backend-architecture)
7. [Database Architecture](#7-database-architecture)
8. [Security Architecture](#8-security-architecture)
9. [JWT Authentication Flow](#9-jwt-authentication-flow)
10. [OAuth2 Authentication Flow](#10-oauth2-authentication-flow)
11. [Redis Architecture](#11-redis-architecture)
12. [Kafka Architecture](#12-kafka-architecture)
13. [Kafka Fallback Architecture](#13-kafka-fallback-architecture)
14. [AWS S3 Architecture](#14-aws-s3-architecture)
15. [Email Architecture](#15-email-architecture)
16. [CI/CD Architecture](#16-cicd-architecture)
17. [Docker Architecture](#17-docker-architecture)
18. [Deployment Architecture](#18-deployment-architecture)
19. [Nginx Reverse Proxy Architecture](#19-nginx-reverse-proxy-architecture)
20. [VPS Architecture](#20-vps-architecture)
21. [Scalability Considerations](#21-scalability-considerations)
22. [Observability Considerations](#22-observability-considerations)
23. [Future Evolution Strategy](#23-future-evolution-strategy)

---

## 1. Architectural Overview

PlaceSync is a full-stack SaaS application built around a **Modular Monolith** backend and a **React SPA** frontend. The two are completely decoupled and communicate exclusively via a versioned REST API.

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              CLIENT LAYER                                   │
│                                                                             │
│   ┌───────────────────────────────────────────────────────────┐             │
│   │             React SPA  (Vite + TypeScript)                │             │
│   │     React Router  │  TanStack Query  │  Material UI       │             │
│   └───────────────────────────────┬───────────────────────────┘             │
│                                   │ HTTPS REST (JSON)                       │
└───────────────────────────────────┼─────────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼─────────────────────────────────────────┐
│                           REVERSE PROXY                                     │
│                                   │                                         │
│              ┌────────────────────▼──────────────────┐                      │
│              │         Nginx (Ubuntu VPS)            │                      │
│              │   TLS Termination │ Rate Limiting     │                      │
│              │   Static File Serving │ Proxy Pass    │                      │
│              └────────────────────┬──────────────────┘                      │
└───────────────────────────────────┼─────────────────────────────────────────┘
                                    │
┌───────────────────────────────────┼─────────────────────────────────────────┐
│                        APPLICATION LAYER                                    │
│                                   │                                         │
│              ┌────────────────────▼──────────────────┐                      │
│              │   Spring Boot 3 Modular Monolith      │                      │
│              │           (Java 21)                   │                      │
│              │                                       │                      │
│              │  auth │ users │ students │ recruiters │                      │
│              │  companies │ resumes │ jobs           │                      │
│              │  applications │ interviews            │                      │
│              │  notifications │ analytics │ audit    │                      │
│              └──┬─────────┬──────────┬──────────────┘                      │
│                 │         │          │                                       │
└─────────────────┼─────────┼──────────┼─────────────────────────────────────┘
                  │         │          │
       ┌──────────▼─┐  ┌───▼──────┐  ┌▼──────────┐
       │ PostgreSQL │  │  Redis   │  │  Kafka    │
       │ (Supabase) │  │ (Cache)  │  │ (Events)  │
       └────────────┘  └──────────┘  └───────────┘
                                           │
                                  ┌────────▼────────┐
                                  │  Notification   │
                                  │  Consumer       │
                                  │  (in-process)   │
                                  └─────────────────┘

External Services:
  AWS S3      — Resume and profile picture storage
  Gmail SMTP  — Transactional email delivery
  Google      — OAuth2 authentication provider
```

---

## 2. Architectural Goals

| Goal | Design Decision |
|---|---|
| **Clean Architecture** | Strict layering: Controller → Service → Repository. No skipping layers. |
| **Separation of Concerns** | Backend split into bounded modules. Frontend split into pages, components, hooks, and services. |
| **Security First** | JWT + refresh tokens, BCrypt, HTTPS-only, pre-signed S3 URLs, no secrets in code. |
| **Testability** | Service layer is framework-agnostic Java. Unit tests use Mockito; integration tests use Spring Boot Test with real DB. |
| **Maintainability** | One module per domain. Flyway for migrations. OpenAPI for API contracts. |
| **Scalability** | Redis cache reduces DB load. Kafka decouples heavy async operations. Stateless backend allows horizontal scaling. |
| **Developer Experience** | Docker Compose brings the full stack up locally. Swagger UI for API exploration. GitHub Actions for automated checks. |
| **Production Readiness** | Health checks, structured logging, graceful shutdown, audit logging, connection pooling. |

---

## 3. Architectural Style

PlaceSync uses a **Layered + Event-Driven hybrid** within a **Modular Monolith** deployment unit.

### Layered Architecture (per module)

```
  ┌─────────────────────────────────────────┐
  │          Controller Layer               │  ← HTTP, request/response mapping,
  │   (@RestController, @RequestMapping)    │    input validation, auth enforcement
  ├─────────────────────────────────────────┤
  │           Service Layer                 │  ← Business logic, orchestration,
  │          (@Service, @Transactional)     │    event publishing
  ├─────────────────────────────────────────┤
  │         Repository Layer                │  ← Data access, JPA queries
  │    (JpaRepository, @Repository)         │
  ├─────────────────────────────────────────┤
  │    Entity / DTO / Mapper Layer          │  ← Domain model, data contracts,
  │  (@Entity, record DTOs, MapStruct)      │    transformation
  └─────────────────────────────────────────┘
```

### Event-Driven Layer (cross-cutting)

Kafka producers in the service layer publish domain events. Consumers in the notification module process those events asynchronously. This decouples the request thread from downstream side effects (email, in-app notifications).

---

## 4. Modular Monolith Justification

### What is a Modular Monolith?

A modular monolith is a single deployable unit whose internal codebase is partitioned into well-bounded modules with explicit, enforced interfaces. Each module owns its domain logic and data access. Modules communicate through service interfaces, not direct repository calls or shared database tables.

### Why Not Microservices?

| Concern | Microservices | Modular Monolith (PlaceSync V1) |
|---|---|---|
| **Operational Complexity** | High — requires container orchestration, service discovery, distributed tracing, API gateway | Low — single Docker Compose file, one JVM process |
| **Network Latency** | Cross-service calls introduce network overhead and failure modes | In-process calls are zero-latency and transactional |
| **Data Consistency** | Distributed transactions (Saga, 2PC) are complex | ACID transactions within a single PostgreSQL database |
| **Team Size** | Justified when multiple independent teams own separate services | Solo/small team — module boundaries are sufficient |
| **Debugging** | Distributed logs, correlation IDs across services | Single log stream, single stack trace |
| **Deployment** | Each service has its own CI/CD pipeline | One build, one deploy |
| **Evolution** | Hard to re-draw service boundaries retroactively | Module boundaries can evolve cheaply before extraction |

### Why Modular Monolith Works Here

1. **Learning objective:** Demonstrates domain-driven design and clean architecture without the noise of distributed systems.
2. **Bounded modules are pre-positioned for extraction:** If a specific module (e.g., `notifications`) needs to scale independently in V2, it can be extracted into a microservice because it already has a clean interface boundary.
3. **ACID transactions across modules:** Placing an application and triggering an audit log entry happen atomically in one database transaction — impossible across service boundaries without a saga.
4. **Single deployment unit reduces VPS cost:** One JVM instead of five to eight services.

### Module Structure

```
com.placesync
├── auth/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── mapper/
├── users/
│   ├── controller/
│   ├── service/
│   ├── repository/
│   ├── entity/
│   ├── dto/
│   └── mapper/
├── students/
├── recruiters/
├── companies/
├── resumes/
├── jobs/
├── applications/
├── interviews/
├── notifications/
├── analytics/
├── audit/
├── common/          ← shared utilities, base classes, exceptions, pagination
└── config/          ← Spring configuration, security config, Kafka config, Redis config
```

### Module Interface Rule

No module may import a `repository` or `entity` class from another module. Cross-module access is permitted only through the other module's `service` interface. This is enforced by code review. In V2, ArchUnit tests can enforce this constraint automatically.

---

## 5. Frontend Architecture

### Technology Stack

| Technology | Role |
|---|---|
| React 18 | Component-based UI framework |
| TypeScript | Static typing for reliability and DX |
| Vite | Build tool — fast HMR in development, optimized bundle in production |
| React Router v6 | Client-side routing with nested layouts |
| TanStack Query (React Query) | Server state management, caching, background refetching |
| Axios | HTTP client with interceptors for auth token injection |
| Material UI (MUI) v5 | Component library for consistent, accessible UI |

### Folder Structure

```
src/
├── api/              ← Axios instance + API function wrappers per module
├── components/       ← Reusable UI components (shared across pages)
│   ├── common/       ← Button, Modal, DataTable, etc.
│   └── layout/       ← Navbar, Sidebar, PageWrapper
├── pages/            ← Route-level components (one per route)
│   ├── auth/
│   ├── student/
│   ├── recruiter/
│   └── admin/
├── hooks/            ← Custom React hooks (useAuth, useProfile, useJobs, etc.)
├── context/          ← React Context for auth state
├── types/            ← TypeScript interfaces and type definitions
├── utils/            ← Date formatting, validation helpers, constants
└── routes/           ← Route definitions, protected route wrappers
```

### Authentication State Management

Auth state (access token, user role) is held in React Context and persisted to `localStorage`. TanStack Query handles all server data fetching. Axios interceptors automatically attach the `Authorization: Bearer <token>` header to every request and handle 401 responses by attempting a silent token refresh.

```
Axios Interceptor Flow:

Request → Attach access token → Server
Response 401 → Call /auth/refresh → Get new tokens → Retry original request
Response 401 (refresh failed) → Clear auth state → Redirect to /login
```

### Routing

Routes are protected by a `PrivateRoute` wrapper component that checks the user's role and redirects to the login page or a 403 page as appropriate.

```
/                        → Public landing
/login                   → Auth page
/register                → Auth page
/auth/callback           → OAuth2 callback handler
/student/*               → PrivateRoute (ROLE_STUDENT)
/recruiter/*             → PrivateRoute (ROLE_RECRUITER)
/admin/*                 → PrivateRoute (ROLE_ADMIN)
```

---

## 6. Backend Architecture

### Technology Stack

| Technology | Role |
|---|---|
| Java 21 | Runtime — virtual threads (Project Loom) available if needed |
| Spring Boot 3 | Application framework, auto-configuration |
| Spring Security 6 | Authentication, authorization, JWT filter chain |
| Spring Data JPA | Repository abstraction over Hibernate |
| Spring Validation | Bean validation with Jakarta Validation annotations |
| Spring Mail | Email sending abstraction |
| Spring Cache | Declarative caching with Redis backend |
| Spring Kafka | Kafka producer/consumer integration |
| MapStruct | Compile-time DTO ↔ Entity mapping |
| Flyway | Database schema version management |
| Springdoc OpenAPI | Swagger UI + OpenAPI 3.1 spec generation |
| Spring Boot Actuator | Health, metrics, info endpoints |

### Request Lifecycle

```
Client Request (HTTPS)
       │
       ▼
  Nginx (TLS termination, rate limiting)
       │
       ▼
  Spring Security Filter Chain
  ├── JwtAuthenticationFilter  → validates access token, sets SecurityContext
  ├── CorsFilter               → enforces CORS policy
  └── (other filters)
       │
       ▼
  @RestController (Controller Layer)
  ├── @Valid input validation
  ├── Role enforcement (@PreAuthorize / method security)
  └── Delegates to Service
       │
       ▼
  @Service (Business Logic Layer)
  ├── Business rules
  ├── @Transactional boundary
  ├── Kafka event publishing
  └── Cache interactions
       │
       ▼
  @Repository (Data Access Layer)
  ├── JPA query execution
  └── Hibernate → PostgreSQL
       │
       ▼
  JSON Response → Client
```

### API Design Conventions

- Base path: `/api/v1/`
- Resource naming: plural nouns — `/api/v1/jobs`, `/api/v1/applications`
- HTTP methods: GET (read), POST (create), PUT (full update), PATCH (partial update), DELETE (delete/deactivate)
- Pagination: `?page=0&size=20&sort=createdAt,desc`
- Standard error response:

```json
{
  "status": 404,
  "error": "NOT_FOUND",
  "message": "Job with id '42' not found",
  "timestamp": "2026-06-23T10:15:30Z",
  "path": "/api/v1/jobs/42"
}
```

### Cross-Cutting Concerns

| Concern | Implementation |
|---|---|
| Logging | SLF4J + Logback. Structured log format in production (JSON). |
| Exception Handling | `@RestControllerAdvice` — maps all exceptions to standard error response. |
| Auditing | AOP `@Aspect` intercepts write operations and persists to `audit_log` table. |
| Validation | Jakarta Bean Validation on DTOs. `@Valid` at controller method parameters. |
| Transactions | `@Transactional` at service layer. Read-only transactions for query methods. |
| Caching | `@Cacheable`, `@CacheEvict`, `@CachePut` annotations backed by Redis. |

---

## 7. Database Architecture

### Technology

- **Database:** PostgreSQL 15+ (hosted on Supabase for V1)
- **ORM:** Hibernate 6 (via Spring Data JPA)
- **Migration Tool:** Flyway

### Schema Management

All DDL changes go through Flyway migration scripts in `src/main/resources/db/migration/`. Scripts are named `V{number}__{description}.sql` (e.g., `V001__create_users_table.sql`). Production migration is automated — Flyway runs on application startup.

**Rule:** No manual DDL against the production database. Every schema change goes through a Flyway script, committed and reviewed before deployment.

### Core Entity Relationships

```
users (id, email, password_hash, role, is_email_verified, is_active, created_at)
  │
  ├──< student_profiles (user_id FK, full_name, phone, cgpa, graduation_year, ...)
  │     │
  │     ├──< resumes (id, student_id FK, label, s3_key, filename, size, is_default)
  │     └──< applications (id, student_id FK, job_id FK, resume_id FK, status, applied_at)
  │               │
  │               └──< interviews (id, application_id FK, round, type, scheduled_at, ...)
  │
  ├──< recruiter_profiles (user_id FK, full_name, company_id FK, verification_status)
  │     │
  │     └──< jobs (id, company_id FK, recruiter_id FK, title, status, deadline, ...)
  │
  └──< companies (id, name, website, industry, logo_s3_key, is_verified)

notifications (id, user_id FK, type, title, body, is_read, created_at)
audit_log (id, entity_type, entity_id, action, actor_id, actor_role, timestamp, payload JSONB)
refresh_tokens (id, user_id FK, token_hash, expires_at, revoked, created_at)
```

### Connection Pooling

HikariCP (Spring Boot default) is configured with pool sizes tuned for the expected load:

```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      connection-timeout: 30000
      idle-timeout: 600000
      max-lifetime: 1800000
```

### Indexing Strategy

Key indexes beyond primary keys:

| Table | Indexed Columns | Reason |
|---|---|---|
| `users` | `email` (unique) | Login lookup |
| `jobs` | `status`, `deadline`, `company_id` | Job listing filters |
| `applications` | `student_id`, `job_id`, `status` | Application queries |
| `notifications` | `user_id`, `is_read`, `created_at` | Inbox queries |
| `audit_log` | `entity_type`, `actor_id`, `timestamp` | Audit search |
| `refresh_tokens` | `token_hash` (unique), `user_id` | Token validation |

---

## 8. Security Architecture

### Defense Layers

```
Layer 1 — Network:
  Nginx rate limiting (100 req/min per IP by default)
  UFW firewall (only ports 80, 443, 22 open)
  HTTPS enforced (HTTP redirects to HTTPS)

Layer 2 — Transport:
  TLS 1.2+ (Let's Encrypt certificate, auto-renewed)
  HSTS header (Strict-Transport-Security)

Layer 3 — Application:
  Spring Security filter chain
  JWT validation (signature, expiry, issuer claim)
  RBAC enforcement (@PreAuthorize)
  CORS policy (only frontend origin allowed)
  Input validation (Jakarta Bean Validation)
  CSRF protection (disabled for stateless JWT API; re-evaluate if cookies are used)

Layer 4 — Data:
  BCrypt password hashing (cost factor 12)
  Refresh token rotation (detect reuse)
  Pre-signed S3 URLs (no public S3 access)
  Sensitive fields never returned in API responses (password_hash, token_hash)
  Audit log for all write operations

Layer 5 — Secrets:
  No credentials in source code
  All secrets via environment variables
  Docker secrets or .env file (never committed)
```

### Security Headers

Set by Nginx and Spring Security:

```
Content-Security-Policy: default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline'
X-Frame-Options: DENY
X-Content-Type-Options: nosniff
Referrer-Policy: strict-origin-when-cross-origin
Strict-Transport-Security: max-age=31536000; includeSubDomains
Permissions-Policy: camera=(), microphone=(), geolocation=()
```

---

## 9. JWT Authentication Flow

### Token Design

| Token | Expiry | Storage | Purpose |
|---|---|---|---|
| Access Token | 15 minutes | Memory (JS variable or React state) | API authentication |
| Refresh Token | 7 days | HttpOnly cookie or localStorage | Obtain new access token |

### Standard Login Flow

```
Client                        API Server                     Database
  │                               │                               │
  ├─── POST /auth/login ─────────►│                               │
  │    { email, password }        │                               │
  │                               ├── Find user by email ────────►│
  │                               │◄── user record ───────────────┤
  │                               │                               │
  │                               ├── BCrypt.verify(password,     │
  │                               │   user.passwordHash)          │
  │                               │                               │
  │                               ├── Generate signed JWT ────────┤
  │                               │   (sub: userId, role: X,      │
  │                               │    iat: now, exp: now+15m)    │
  │                               │                               │
  │                               ├── Generate Refresh Token ─────┤
  │                               ├── Hash refresh token ─────────►│
  │                               │   store in refresh_tokens     │
  │                               │                               │
  │◄── 200 OK ────────────────────┤                               │
  │    { accessToken, refreshToken,│                              │
  │      expiresIn, role }        │                               │

Subsequent Requests:
  │                               │
  ├─── GET /api/v1/jobs ─────────►│
  │    Authorization: Bearer <AT> │
  │                               │
  │                     JwtAuthenticationFilter:
  │                     ├── Extract token from header
  │                     ├── Validate signature (HMAC-SHA256 with secret)
  │                     ├── Check expiry
  │                     ├── Extract userId + role → set SecurityContext
  │                               │
  │◄── 200 OK (jobs list) ────────┤

Token Refresh Flow:
  │                               │
  ├─── POST /auth/refresh ───────►│
  │    { refreshToken }           │
  │                               ├── Hash token → lookup in DB
  │                               ├── Validate: not revoked, not expired
  │                               ├── Revoke old refresh token (rotation)
  │                               ├── Issue new access token + refresh token
  │◄── 200 OK ────────────────────┤
  │    { accessToken, refreshToken }│

Logout Flow:
  │                               │
  ├─── POST /auth/logout ────────►│
  │    { refreshToken }           │
  │                               ├── Hash token → mark revoked in DB
  │◄── 204 No Content ────────────┤
  │    (client clears tokens)     │
```

### JWT Token Structure

```json
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "student@example.com",
  "role": "ROLE_STUDENT",
  "iat": 1719129600,
  "exp": 1719130500,
  "iss": "placesync"
}
```

### Refresh Token Rotation

On each refresh, the old token is invalidated and a new one is issued. If a refresh token is used twice (indicating theft), the entire token family for that user is invalidated (all sessions are logged out). This limits the blast radius of a stolen refresh token.

---

## 10. OAuth2 Authentication Flow

### Provider: Google OAuth2 (Authorization Code Flow)

```
Browser                    API Server                 Google
  │                             │                         │
  ├── Click "Login with Google" │                         │
  │                             │                         │
  │◄── Redirect to Google ──────┤                         │
  │    /oauth2/authorization/google                        │
  │                             │                         │
  ├─────────────────────────────────────────────────────►│
  │    Google Login Page         │                        │
  │◄─────────────────────────────────────────────────────┤
  │    User grants consent       │                        │
  │                             │                         │
  ├───────────────────────────►│◄───────────────────────►│
  │  Redirect to /auth/callback │  Authorization Code     │
  │  ?code=<auth_code>          │  ↔  access_token        │
  │                             │  ↔  id_token (JWT)      │
  │                             │                         │
  │                       Extract from id_token:
  │                       - email
  │                       - name
  │                       - picture URL
  │                             │
  │                       Check if user exists in DB:
  │                       - If yes: load existing user
  │                       - If no: auto-provision new user
  │                             │ (role: ROLE_STUDENT)
  │                             │
  │                       Issue PlaceSync JWT + refresh token
  │◄── Redirect to frontend ────┤
  │    /auth/callback?token=... │
  │                             │
  ├── Store tokens in client ───┘
```

Spring Security 6 handles the OAuth2 dance via `OAuth2LoginSuccessHandler`. The handler extracts user attributes from the `OAuth2User` object and delegates to `AuthService.provisionOAuthUser()`.

---

## 11. Redis Architecture

### Deployment (V1)

Single Redis instance running in Docker Compose, accessible only within the Docker network. No persistence configuration (cache loss on restart is acceptable; the database is the source of truth).

### Cache Namespace Design

| Cache Name | Key Pattern | TTL | Eviction Trigger |
|---|---|---|---|
| `user-profiles` | `user-profiles::{userId}` | 30 min | Profile update |
| `job-listings` | `job-listings::page-{n}-size-{s}-filters-{hash}` | 5 min | Job create/update/close |
| `job-detail` | `job-detail::{jobId}` | 10 min | Job update/close |
| `analytics-dashboard` | `analytics-dashboard::global` | 10 min | New offer recorded |
| `recruiter-analytics` | `recruiter-analytics::{recruiterId}` | 10 min | Application status change |

### Spring Cache Integration

```java
// Service method: read-through cache
@Cacheable(value = "job-listings", key = "#pageable.pageNumber + '-' + #pageable.pageSize")
public Page<JobSummaryDto> getOpenJobs(Pageable pageable) { ... }

// Evict on update
@CacheEvict(value = "job-listings", allEntries = true)
@Transactional
public JobDto updateJob(UUID jobId, UpdateJobRequest request) { ... }

// Evict specific entry
@CacheEvict(value = "user-profiles", key = "#userId")
@Transactional
public StudentProfileDto updateProfile(UUID userId, UpdateProfileRequest request) { ... }
```

### Cache Invalidation Strategy

PlaceSync uses a **write-through invalidation** approach:
- On any write operation that affects cached data, the relevant cache entry is evicted (`@CacheEvict`).
- The next read will produce a cache miss, reload from the database, and re-populate the cache.
- This trades slightly higher DB read load on the first post-invalidation request for strong consistency.
- For analytics, where staleness is more tolerable, TTL-based expiry (10 minutes) is the primary invalidation mechanism.

### Redis Configuration

```yaml
spring:
  cache:
    type: redis
  data:
    redis:
      host: ${REDIS_HOST:redis}
      port: ${REDIS_PORT:6379}
      timeout: 2000ms
  cache:
    redis:
      time-to-live: 300000   # default 5 min; overridden per cache
      key-prefix: "placesync::"
      use-key-prefix: true
      cache-null-values: false
```

---

## 12. Kafka Architecture

### Deployment (V1)

Single-broker Kafka running in Docker Compose with KRaft mode (no ZooKeeper dependency from Kafka 3.x+).

### Topic Design

| Topic | Producers | Consumers | Partitions | Retention |
|---|---|---|---|---|
| `application-events` | ApplicationService | NotificationConsumer | 3 | 7 days |
| `interview-events` | InterviewService | NotificationConsumer | 3 | 7 days |
| `notification-events` | Multiple | NotificationConsumer | 3 | 7 days |
| `offer-events` | ApplicationService | NotificationConsumer, AnalyticsConsumer | 3 | 7 days |

### Event Schema Examples

**ApplicationSubmittedEvent**
```json
{
  "eventId": "uuid",
  "eventType": "APPLICATION_SUBMITTED",
  "timestamp": "2026-06-23T10:00:00Z",
  "applicationId": "uuid",
  "studentId": "uuid",
  "jobId": "uuid",
  "jobTitle": "Software Engineer",
  "companyName": "Acme Corp",
  "studentEmail": "student@example.com",
  "studentName": "Arjun Sharma"
}
```

**InterviewScheduledEvent**
```json
{
  "eventId": "uuid",
  "eventType": "INTERVIEW_SCHEDULED",
  "timestamp": "2026-06-23T10:00:00Z",
  "interviewId": "uuid",
  "applicationId": "uuid",
  "studentId": "uuid",
  "studentEmail": "student@example.com",
  "studentName": "Arjun Sharma",
  "round": 1,
  "interviewType": "ONLINE",
  "scheduledAt": "2026-07-01T14:00:00Z",
  "durationMinutes": 60,
  "meetingLink": "https://meet.example.com/abc123"
}
```

### Producer Design

Producers live in the respective domain service. They use `KafkaTemplate` and publish asynchronously with a callback to handle delivery failures:

```java
@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ApplicationEventPublisher springEventPublisher;

    public ApplicationDto applyToJob(UUID studentId, UUID jobId, ApplyRequest request) {
        // ... business logic, persist application ...

        ApplicationSubmittedEvent event = buildEvent(application);
        publishEvent("application-events", event);

        return mapper.toDto(application);
    }

    private void publishEvent(String topic, Object event) {
        try {
            kafkaTemplate.send(topic, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.warn("Kafka publish failed, falling back to Spring event", ex);
                        springEventPublisher.publishEvent(event);
                    }
                });
        } catch (Exception ex) {
            log.warn("Kafka unavailable, falling back to Spring event", ex);
            springEventPublisher.publishEvent(event);
        }
    }
}
```

### Consumer Design

The `NotificationConsumer` in the `notifications` module listens to all relevant topics:

```java
@Component
@RequiredArgsConstructor
public class NotificationConsumer {

    private final NotificationService notificationService;
    private final EmailService emailService;

    @KafkaListener(topics = "application-events", groupId = "notification-group")
    public void handleApplicationEvent(ApplicationSubmittedEvent event) {
        notificationService.createNotification(event.getStudentId(),
            "Application Submitted",
            "Your application for " + event.getJobTitle() + " at " + event.getCompanyName() + " was received.");
        emailService.sendApplicationConfirmation(event);
    }

    @KafkaListener(topics = "interview-events", groupId = "notification-group")
    public void handleInterviewEvent(InterviewScheduledEvent event) {
        notificationService.createNotification(event.getStudentId(),
            "Interview Scheduled",
            "Round " + event.getRound() + " interview scheduled for " + event.getScheduledAt());
        emailService.sendInterviewScheduledEmail(event);
    }
}
```

### Event Flow Diagram

```
Student submits application
         │
         ▼
  ApplicationService.applyToJob()
  ├── Persist Application entity (status: APPLIED)
  ├── Persist Audit log entry
  └── Publish ApplicationSubmittedEvent to Kafka
               │
               ▼
      Kafka Topic: application-events
               │
               ▼
      NotificationConsumer.handleApplicationEvent()
      ├── NotificationService.createNotification() → persists to DB
      └── EmailService.sendApplicationConfirmation() → Gmail SMTP
```

---

## 13. Kafka Fallback Architecture

### Problem

If Kafka is unavailable (startup failure, network partition, broker crash), events published via `KafkaTemplate` will fail. Without a fallback, this would silently drop notifications and emails — a poor user experience.

### Solution: Spring Application Events

Spring Framework's built-in `ApplicationEventPublisher` and `@EventListener` mechanism provides an in-process, synchronous (or asynchronous) fallback that requires zero external infrastructure.

### Fallback Design

```
Normal Operation:
  Service ──► KafkaTemplate.send() ──► Kafka Broker ──► Consumer

Kafka Unavailable:
  Service ──► KafkaTemplate.send() throws / completes exceptionally
           └─► ApplicationEventPublisher.publishEvent() ──► @EventListener (same JVM)
                                                        └─► NotificationService (direct call)
                                                        └─► EmailService (direct call)
```

### Implementation

```java
// Fallback listener — active always, but only receives events when Kafka fails
@Component
@RequiredArgsConstructor
public class NotificationFallbackListener {

    private final NotificationService notificationService;
    private final EmailService emailService;

    @Async
    @EventListener
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        log.info("Processing ApplicationSubmittedEvent via Spring fallback");
        notificationService.createNotification(...);
        emailService.sendApplicationConfirmation(event);
    }

    @Async
    @EventListener
    public void onInterviewScheduled(InterviewScheduledEvent event) {
        log.info("Processing InterviewScheduledEvent via Spring fallback");
        notificationService.createNotification(...);
        emailService.sendInterviewScheduledEmail(event);
    }
}
```

The `@Async` annotation ensures the fallback listener runs on a separate thread pool, preventing it from blocking the request thread even in fallback mode.

### Limitations of the Fallback

| Limitation | Impact |
|---|---|
| No durability | If the JVM crashes between publishing the Spring event and the listener executing, the event is lost. |
| No fan-out | Spring events are in-process only; a second consumer (e.g., analytics consumer) would not receive them unless explicitly registered as a listener. |
| No replay | Events are not stored; there is no mechanism to replay missed events after Kafka recovers. |

The fallback is a **best-effort degraded mode**. It is suitable for V1 where Kafka is a non-critical enhancement. When Kafka is healthy, all processing goes through Kafka.

---

## 14. AWS S3 Architecture

### Bucket Design

| Bucket | Contents | Access |
|---|---|---|
| `placesync-resumes` | Student PDF resumes | Private (pre-signed URL only) |
| `placesync-profile-pictures` | Student and company images | Private (pre-signed URL only) |

Both buckets are **private** — no public access policies. All access is mediated through pre-signed URLs generated by the backend.

### S3 Key Naming Convention

```
resumes/{studentId}/{resumeId}/{originalFilename}
profile-pictures/{userId}/{timestamp}-{randomUUID}.jpg
company-logos/{companyId}/{timestamp}-{randomUUID}.jpg
```

### Resume Upload Flow

```
Client (Browser)                    API Server                         AWS S3
       │                                │                                  │
       ├── POST /api/v1/resumes ───────►│                                  │
       │   Content-Type: multipart/form-data                               │
       │   file: resume.pdf             │                                  │
       │   label: "Software Engineer"   │                                  │
       │                                │                                  │
       │                         Validate:
       │                         ├── File type == PDF
       │                         ├── File size <= 10 MB
       │                         └── Student owns this profile
       │                                │                                  │
       │                         Build S3 key:                             │
       │                         resumes/{studentId}/{resumeId}/resume.pdf │
       │                                │                                  │
       │                                ├── PutObjectRequest ─────────────►│
       │                                │   (via AmazonS3 SDK)             │
       │                                │◄── 200 OK ───────────────────────┤
       │                                │                                  │
       │                         Persist Resume entity:
       │                         ├── s3_key, filename, size, label, uploadedAt
       │◄── 201 Created ─────────────── │
       │    { resumeId, label, ...}      │

Resume Download Flow:
       │                                │                                  │
       ├── GET /api/v1/resumes/{id}/url►│                                  │
       │                                │                                  │
       │                         Verify requester is authorized:
       │                         ├── Student: owns the resume
       │                         └── Recruiter: student applied to their job
       │                                │                                  │
       │                         GeneratePresignedUrlRequest:
       │                         ├── Key: resume.s3Key
       │                         ├── Expiry: 15 minutes
       │                         └── Method: GET
       │                                ├── GeneratePresignedUrl ─────────►│
       │                                │◄── pre-signed URL ───────────────┤
       │◄── 200 OK ──────────────────── │
       │    { downloadUrl, expiresAt }   │
       │                                │                                  │
       ├── GET {pre-signed URL} ──────────────────────────────────────────►│
       │◄────────────────────────────────────────────────────── resume.pdf ┤
```

### IAM Configuration

The backend uses an IAM user (not instance role in V1) with a scoped policy:

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:GeneratePresignedUrl"
      ],
      "Resource": [
        "arn:aws:s3:::placesync-resumes/*",
        "arn:aws:s3:::placesync-profile-pictures/*"
      ]
    }
  ]
}
```

IAM credentials are injected via environment variables (`AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_REGION`). They are never committed to version control.

---

## 15. Email Architecture

### V1: Gmail SMTP

Spring Mail is configured to use Gmail's SMTP relay. A dedicated Gmail account (e.g., `noreply.placesync@gmail.com`) with an App Password is used.

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

### Email Sending Architecture

All email sending is asynchronous using a dedicated thread pool (`@Async`):

```
Event (Kafka Consumer or Spring Fallback Listener)
       │
       ▼
  EmailService.send*()
  ├── Build email from Thymeleaf HTML template
  ├── Populate model (user name, link, etc.)
  ├── MimeMessageHelper.setTo(), setSubject(), setText(html=true)
  └── JavaMailSender.send()  ← @Async — non-blocking
         │
         ▼ (separate thread pool)
  Gmail SMTP → Recipient Inbox
  (failure: log warning, do not propagate exception to caller)
```

### Transactional Email Triggers

| Trigger | Template |
|---|---|
| User registration | `email-verification.html` |
| Password reset | `password-reset.html` |
| Account locked | `account-locked.html` |
| Recruiter approved | `recruiter-approved.html` |
| Recruiter rejected | `recruiter-rejected.html` |
| Application confirmed | `application-confirmation.html` |
| Application status changed | `application-status-update.html` |
| Interview scheduled | `interview-scheduled.html` |
| Interview rescheduled | `interview-rescheduled.html` |
| Interview cancelled | `interview-cancelled.html` |

### Future V2: Amazon SES

Gmail SMTP has a daily sending limit (~500/day). When PlaceSync grows beyond this:

1. Provision an SES identity and verify the sending domain.
2. Request production SES access (exit sandbox).
3. Replace the Spring Mail SMTP config with the SES SMTP endpoint (SES supports SMTP relay — no SDK change required).
4. Alternatively, use the AWS SES Java SDK v2 for programmatic sending with better bounce/complaint handling.

The email sending architecture (service layer, templates, async execution) does not change — only the transport configuration changes.

---

## 16. CI/CD Architecture

### Philosophy

- **CI is automated:** Every push triggers a build, test, and quality analysis pipeline.
- **CD is manual in V1:** Deployment to the VPS is triggered manually by the developer after CI passes. Future V2 will automate deployment.
- **The developer owns all Git operations:** CI never commits, pushes, or merges.

### GitHub Actions Pipeline

#### Trigger

```yaml
on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main, develop]
```

#### CI Pipeline Stages

```
Push to main/develop
        │
        ▼
┌─────────────────────────────────────────────────┐
│ Job: build-and-test                             │
│                                                 │
│ 1. Checkout code                                │
│ 2. Set up JDK 21 (Temurin)                      │
│ 3. Cache Maven dependencies (~/.m2)             │
│ 4. Start test services (PostgreSQL, Redis)      │
│    via docker-compose.test.yml or               │
│    GitHub Actions service containers            │
│ 5. Run: mvn verify                              │
│    ├── Compile                                  │
│    ├── Run unit tests (JUnit 5 + Mockito)        │
│    └── Run integration tests (Spring Boot Test) │
│ 6. Publish test results                         │
│ 7. Upload coverage report (JaCoCo)              │
└────────────────────┬────────────────────────────┘
                     │ (on success)
                     ▼
┌─────────────────────────────────────────────────┐
│ Job: sonar-analysis                             │
│                                                 │
│ 1. Run: mvn sonar:sonar                         │
│    -Dsonar.projectKey=placesync                 │
│    -Dsonar.host.url=https://sonarcloud.io       │
│    -Dsonar.token=${{ secrets.SONAR_TOKEN }}     │
│ 2. SonarCloud evaluates:                        │
│    ├── Code coverage (JaCoCo report)            │
│    ├── Code smells                              │
│    ├── Security vulnerabilities                 │
│    ├── Bugs                                     │
│    └── Duplications                             │
│ 3. Quality Gate enforced:                       │
│    └── Fails CI if gate not passed              │
└─────────────────────────────────────────────────┘
```

#### Secrets Management in GitHub Actions

| Secret | Usage |
|---|---|
| `SONAR_TOKEN` | SonarCloud analysis authentication |
| `TEST_DB_URL` | PostgreSQL connection string for integration tests |
| `TEST_REDIS_URL` | Redis connection for integration tests |

Production secrets (AWS, Gmail, JWT secret) are never exposed to CI. They are managed directly on the VPS via environment variables.

#### Pipeline File Structure

```
.github/
└── workflows/
    ├── ci.yml            ← Main CI pipeline (build + test + Sonar)
    └── pr-checks.yml     ← Lightweight compile check on PRs (fast feedback)
```

#### Future V2: CD Pipeline

```
After CI passes on main:
        │
        ▼
┌─────────────────────────────────────────────────┐
│ Job: build-docker-image                         │
│ 1. Build Docker image: mvn spring-boot:build-image│
│    or: docker build -t placesync-api .          │
│ 2. Push to Docker Hub or GitHub Container Registry│
└────────────────────┬────────────────────────────┘
                     │
                     ▼
┌─────────────────────────────────────────────────┐
│ Job: deploy-to-vps                              │
│ 1. SSH into VPS (using stored SSH key secret)   │
│ 2. docker compose pull                          │
│ 3. docker compose up -d --no-deps api           │
│ 4. Health check: curl /actuator/health          │
└─────────────────────────────────────────────────┘
```

---

## 17. Docker Architecture

### Docker Compose — Local Development

```yaml
# docker-compose.yml
services:
  api:
    build: .
    image: placesync-api:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_DATASOURCE_URL=${DATABASE_URL}
      - SPRING_DATA_REDIS_HOST=redis
      - SPRING_KAFKA_BOOTSTRAP_SERVERS=kafka:9092
      - JWT_SECRET=${JWT_SECRET}
      - AWS_ACCESS_KEY_ID=${AWS_ACCESS_KEY_ID}
      - AWS_SECRET_ACCESS_KEY=${AWS_SECRET_ACCESS_KEY}
      - MAIL_USERNAME=${MAIL_USERNAME}
      - MAIL_APP_PASSWORD=${MAIL_APP_PASSWORD}
    depends_on:
      redis:
        condition: service_healthy
      kafka:
        condition: service_healthy
    networks:
      - placesync-net

  redis:
    image: redis:7-alpine
    ports:
      - "6379:6379"
    healthcheck:
      test: ["CMD", "redis-cli", "ping"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - placesync-net

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

  nginx:
    image: nginx:alpine
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./frontend/dist:/usr/share/nginx/html:ro
      - /etc/letsencrypt:/etc/letsencrypt:ro
    depends_on:
      - api
    networks:
      - placesync-net

networks:
  placesync-net:
    driver: bridge
```

### Dockerfile (Backend)

```dockerfile
# Multi-stage build
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn package -DskipTests -B

FROM eclipse-temurin:21-jre-alpine AS runtime
WORKDIR /app
RUN addgroup -S placesync && adduser -S placesync -G placesync
COPY --from=build /app/target/*.jar app.jar
RUN chown placesync:placesync app.jar
USER placesync
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

Key practices:
- Multi-stage build: Maven in the build stage, lean JRE in the runtime stage.
- Non-root user for the application process.
- Maven dependency cache layer for faster rebuilds.

---

## 18. Deployment Architecture

### Production Stack (V1)

```
User Browser
     │ HTTPS (port 443)
     ▼
Ubuntu VPS (22.04 LTS)
  ├── Nginx (reverse proxy, TLS termination)
  │     ├── Serves React SPA static files (from /dist)
  │     ├── Proxies /api/* → Spring Boot :8080
  │     └── Rate limiting, security headers
  │
  ├── Docker Compose Stack:
  │     ├── placesync-api     (Spring Boot, :8080)
  │     ├── redis             (Cache, :6379 — internal only)
  │     └── kafka             (Event bus, :9092 — internal only)
  │
  └── External Services:
        ├── Supabase PostgreSQL  (managed, external)
        ├── AWS S3              (file storage, external)
        └── Gmail SMTP          (email delivery, external)
```

### Environment Variables (.env — never committed)

```bash
# Database
DATABASE_URL=jdbc:postgresql://<supabase-host>:5432/postgres
DATABASE_USERNAME=postgres
DATABASE_PASSWORD=<secret>

# Redis
REDIS_HOST=redis
REDIS_PORT=6379

# JWT
JWT_SECRET=<256-bit-random-secret>
JWT_ACCESS_TOKEN_EXPIRY_MS=900000
JWT_REFRESH_TOKEN_EXPIRY_DAYS=7

# AWS
AWS_ACCESS_KEY_ID=<key>
AWS_SECRET_ACCESS_KEY=<secret>
AWS_REGION=ap-south-1
AWS_S3_BUCKET_RESUMES=placesync-resumes
AWS_S3_BUCKET_PICTURES=placesync-profile-pictures

# Email
MAIL_USERNAME=noreply.placesync@gmail.com
MAIL_APP_PASSWORD=<gmail-app-password>

# Google OAuth2
GOOGLE_CLIENT_ID=<client-id>
GOOGLE_CLIENT_SECRET=<client-secret>

# App
FRONTEND_URL=https://placesync.yourdomain.com
APP_BASE_URL=https://api.placesync.yourdomain.com
```

---

## 19. Nginx Reverse Proxy Architecture

### Responsibilities

1. **TLS Termination** — Decrypt HTTPS traffic; backend communicates over HTTP internally.
2. **Static File Serving** — Serve the React SPA `dist/` bundle directly from the filesystem (no API roundtrip for static assets).
3. **Reverse Proxy** — Forward `/api/*` requests to the Spring Boot application on port 8080.
4. **Rate Limiting** — Limit request rates per client IP to prevent brute-force and DDoS.
5. **Security Headers** — Set all HTTP security headers before the response reaches the client.
6. **HTTP → HTTPS Redirect** — All HTTP traffic (port 80) is permanently redirected to HTTPS (port 443).

### Nginx Configuration

```nginx
# /etc/nginx/nginx.conf (simplified)

worker_processes auto;
events { worker_connections 1024; }

http {
    include       mime.types;
    default_type  application/octet-stream;
    sendfile      on;
    gzip          on;

    # Rate limiting zone — 100 req/min per IP
    limit_req_zone $binary_remote_addr zone=api_limit:10m rate=100r/m;

    # HTTP → HTTPS redirect
    server {
        listen 80;
        server_name placesync.yourdomain.com;
        return 301 https://$server_name$request_uri;
    }

    server {
        listen 443 ssl http2;
        server_name placesync.yourdomain.com;

        ssl_certificate     /etc/letsencrypt/live/placesync.yourdomain.com/fullchain.pem;
        ssl_certificate_key /etc/letsencrypt/live/placesync.yourdomain.com/privkey.pem;
        ssl_protocols       TLSv1.2 TLSv1.3;
        ssl_prefer_server_ciphers on;

        # Security headers
        add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
        add_header X-Frame-Options "DENY" always;
        add_header X-Content-Type-Options "nosniff" always;
        add_header Referrer-Policy "strict-origin-when-cross-origin" always;
        add_header Content-Security-Policy "default-src 'self'; script-src 'self'; style-src 'self' 'unsafe-inline';" always;

        # React SPA — serve static files, fallback to index.html for client-side routing
        root /usr/share/nginx/html;
        index index.html;

        location / {
            try_files $uri $uri/ /index.html;
        }

        # API reverse proxy
        location /api/ {
            limit_req zone=api_limit burst=20 nodelay;
            proxy_pass http://api:8080;
            proxy_http_version 1.1;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
            proxy_connect_timeout 10s;
            proxy_read_timeout 30s;
        }

        # Swagger UI (non-production only — disable in production or restrict by IP)
        location /swagger-ui/ {
            proxy_pass http://api:8080/swagger-ui/;
            proxy_set_header Host $host;
        }

        # Actuator — restrict to localhost or admin IP
        location /actuator/ {
            allow 127.0.0.1;
            deny all;
            proxy_pass http://api:8080/actuator/;
        }
    }
}
```

---

## 20. VPS Architecture

### Server Specification (Minimum)

| Resource | Minimum | Recommended |
|---|---|---|
| vCPUs | 2 | 4 |
| RAM | 4 GB | 8 GB |
| Storage | 40 GB SSD | 80 GB SSD |
| OS | Ubuntu 22.04 LTS | Ubuntu 22.04 LTS |
| Bandwidth | 1 TB/month | 3 TB/month |

### Security Hardening

```bash
# UFW Firewall — only allow essential ports
ufw allow 22/tcp    # SSH
ufw allow 80/tcp    # HTTP (redirects to HTTPS)
ufw allow 443/tcp   # HTTPS
ufw deny 8080       # Spring Boot — internal only (not public)
ufw deny 6379       # Redis — internal only
ufw deny 9092       # Kafka — internal only
ufw enable
```

- SSH access with key-based authentication only (password auth disabled).
- Fail2ban installed to block IPs after repeated SSH failures.
- Docker network used for internal container-to-container communication (Redis, Kafka ports not exposed to the host unless needed for debugging).

### SSL Certificate

Let's Encrypt with Certbot:

```bash
certbot --nginx -d placesync.yourdomain.com
# Auto-renewal via cron or systemd timer (certbot renew)
```

### Process Management

Docker Compose is started as a systemd service to ensure containers restart on VPS reboot:

```ini
# /etc/systemd/system/placesync.service
[Unit]
Description=PlaceSync Docker Compose Stack
After=docker.service
Requires=docker.service

[Service]
WorkingDirectory=/opt/placesync
ExecStart=/usr/bin/docker compose up
ExecStop=/usr/bin/docker compose down
Restart=always
RestartSec=10

[Install]
WantedBy=multi-user.target
```

---

## 21. Scalability Considerations

### Current V1 Bottlenecks and Mitigations

| Bottleneck | Mitigation |
|---|---|
| Database reads at scale | Redis cache on hot paths (job listings, user profiles, analytics). Read-only transactions. |
| File upload throughput | AWS S3 handles storage. Backend is stateless regarding files. |
| Notification fan-out | Kafka decouples notification creation from the request thread. |
| Single JVM process | Stateless backend — can run N instances behind Nginx with sticky-session-free design (JWT is stateless). |

### Horizontal Scaling Path (V2+)

```
Current (V1):                    Future (V2+):

  Nginx                            Nginx (load balancer)
    │                              ├── api-instance-1
    ▼                              ├── api-instance-2
  api (1 instance)                 └── api-instance-3
    │                                    │
    ├── Redis                     Redis Sentinel (HA) or Redis Cluster
    ├── Kafka (1 broker)          Kafka (3 broker cluster, 3 partitions)
    └── PostgreSQL                PostgreSQL read replica for analytics queries
```

For horizontal scaling to work without code changes:
- Sessions must be stateless (JWT — already implemented).
- No local file system writes in the API (files go to S3 — already implemented).
- Cache invalidation must work across instances (Redis is shared — already implemented).

### Database Scaling

Read-heavy queries (job listings, analytics) can be offloaded to a PostgreSQL read replica in V2. Spring Data JPA supports routing read-only transactions to a replica via `AbstractRoutingDataSource`.

---

## 22. Observability Considerations

### V1 Observability

| Signal | V1 Implementation |
|---|---|
| **Logs** | SLF4J + Logback. JSON format in production for log aggregation tools. Docker Compose log driver captures stdout. |
| **Health** | Spring Boot Actuator `/actuator/health` — Nginx and manual monitoring. |
| **Metrics** | `/actuator/metrics` available but not yet connected to a metrics backend in V1. |
| **Tracing** | Not implemented in V1. |
| **Alerting** | Manual VPS monitoring via `docker stats`, `htop`, disk usage alerts. |

### V2 Observability Stack (planned)

```
Application Metrics:
  Spring Boot Actuator → Micrometer → Prometheus → Grafana

Logs:
  Application stdout → Promtail → Loki → Grafana

Tracing:
  Spring Boot → Micrometer Tracing (Brave/OTLP) → Tempo → Grafana

Alerts:
  Grafana Alerting → Email / Slack webhook
```

### Key Metrics to Track (V1 manual / V2 automated)

- API error rate (4xx, 5xx per endpoint)
- API p95 latency
- JVM heap usage
- Database connection pool saturation
- Redis cache hit rate
- Kafka consumer lag
- Active user sessions

---

## 23. Future Evolution Strategy

### Module Extraction Readiness

Each module in the modular monolith is designed to be extractable into an independent microservice. The preconditions for extraction are:

1. The module's service interface is well-defined and stable.
2. The module communicates with others only through Kafka events or service interfaces (no direct repository calls).
3. The module's database tables are logically isolated (even if physically in the same PostgreSQL instance).

**Most likely extraction candidates:**

| Module | Reason for future extraction |
|---|---|
| `notifications` | High write volume, independent scaling from core CRUD operations |
| `analytics` | Heavy aggregation queries — can be offloaded to read replica or separate DB |
| `resumes` | Potential future OCR/AI processing — compute-intensive workloads suit separate scaling |

### Technology Evolution Path

| V1 | V2+ |
|---|---|
| Single VPS | Kubernetes (GKE, EKS, or self-hosted k3s) |
| Docker Compose | Helm Charts |
| Single Kafka broker | Multi-broker Kafka cluster |
| Single Redis | Redis Sentinel / Redis Cluster |
| Gmail SMTP | Amazon SES |
| Manual CD | Fully automated CD with GitOps (ArgoCD) |
| Spring Application Events fallback | Outbox pattern (transactional outbox for reliable event publishing) |
| JaCoCo + SonarCloud | Full quality gate with architecture enforcement (ArchUnit) |

### API Versioning Strategy

The API is versioned with a URL prefix (`/api/v1/`). When breaking changes are required:

1. Deploy the new version under `/api/v2/`.
2. Support both versions simultaneously for a deprecation period.
3. Communicate the sunset date to API consumers.
4. Remove the old version once traffic drops to zero.

The modular structure makes it easy to add a `v2` controller alongside the `v1` controller within the same module, reusing the service layer.

---

*End of Architecture Document — PlaceSync V1.0*
