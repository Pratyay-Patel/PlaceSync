<div align="center">

# PlaceSync

### A production-grade, cloud-native SaaS platform for campus placement and recruitment

Replacing spreadsheets and email chains with a unified, role-aware system for students, recruiters, and placement administrators.

[![CI](https://github.com/Pratyay-Patel/PlaceSync/actions/workflows/ci.yml/badge.svg)](https://github.com/Pratyay-Patel/PlaceSync/actions/workflows/ci.yml)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=Pratyay-Patel_PlaceSync&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=Pratyay-Patel_PlaceSync)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=Pratyay-Patel_PlaceSync&metric=coverage)](https://sonarcloud.io/summary/new_code?id=Pratyay-Patel_PlaceSync)
![Java](https://img.shields.io/badge/Java-21-ED8B00?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.3.6-6DB33F?logo=springboot&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

[Features](#features) · [Tech Stack](#tech-stack) · [Architecture](#architecture) · [Quick Start](#quick-start) · [API Docs](#api-documentation) · [Roadmap](#roadmap)

</div>

---

## Overview

PlaceSync modernises campus placement operations for colleges and universities. It provides a single, structured platform where **students** build profiles and apply to jobs, **recruiters** post opportunities and manage the full hiring pipeline, and **placement administrators** oversee the entire process with real-time analytics, audit trails, and role management.

Built with a production mindset — event-driven notifications, S3 file storage, Redis caching, structured logging, and a CI pipeline with quality gates — this is not a demo project.

---

## Features

### For Students
- Complete profile management: skills, education, experience, profile picture
- Resume upload and management (PDF, stored in AWS S3)
- Browse and filter open job postings (keyword, location type, job type, skills, deadline)
- One-click application with resume selection and eligibility display
- Real-time application status tracking through the full hiring lifecycle
- Interview schedule view (upcoming and past rounds)
- In-app notification inbox with unread badge

### For Recruiters
- Company profile creation with logo upload
- Job posting with rich eligibility criteria (CGPA, departments, skills)
- Full applicant pipeline management with status transitions
- Interview scheduling, rescheduling, and cancellation
- Resume download via pre-signed S3 URLs
- Per-recruiter analytics (jobs posted, applications, shortlisted, offers)

### For Placement Administrators
- Recruiter and company verification workflow (approve / reject)
- Job approval before public listing
- Global view of all applications and interviews across the platform
- User management — search, activate, deactivate accounts
- Placement analytics dashboard (global stats, top companies, department-wise placement rates)
- Searchable audit log with before/after state diffs for every write operation

### Platform-wide
- JWT authentication with access + refresh token rotation and reuse detection
- Google OAuth2 sign-in
- Automated email notifications (Thymeleaf HTML templates) for all key events
- Kafka event pipeline with transactional fallback — notifications delivered even if Kafka is unreachable
- Correlation ID tracing across every request (logged in MDC, echoed in `X-Correlation-ID` header)
- Security headers on every response (HSTS, X-Frame-Options, X-Content-Type-Options, Referrer-Policy)

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 3.3.6 (modular monolith) |
| Security | Spring Security 6, JWT (jjwt 0.12), OAuth2 (Google) |
| ORM | Hibernate 6 / Spring Data JPA, MapStruct |
| Database | PostgreSQL 16 (Docker locally, Supabase in production) |
| Migrations | Flyway |
| Caching | Redis 7 |
| Messaging | Apache Kafka (KRaft mode, no ZooKeeper) |
| Object Storage | AWS S3 (SDK v2) — resumes, profile pictures, company logos |
| Email | Spring Mail + Thymeleaf HTML templates (Gmail SMTP) |
| API Docs | SpringDoc OpenAPI 3.1 / Swagger UI |
| Frontend | React 18, TypeScript 5, Vite 5, Material UI v5 |
| State / Data | Zustand, TanStack Query v5, Axios |
| Containerisation | Docker, Docker Compose |
| Reverse Proxy | Nginx |
| CI/CD | GitHub Actions |
| Code Quality | SonarCloud (quality gate enforced on every merge to `main`) |
| Testing | JUnit 5, Mockito (160+ unit tests), Testcontainers |

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                          Client Layer                               │
│          React 18 SPA (TypeScript · MUI · TanStack Query)          │
└────────────────────────────┬────────────────────────────────────────┘
                             │  HTTPS
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        Nginx (Reverse Proxy)                        │
│           TLS termination · Rate limiting · Security headers        │
└────────────────────────────┬────────────────────────────────────────┘
                             │
                             ▼
┌─────────────────────────────────────────────────────────────────────┐
│                    Spring Boot API (Port 8080)                      │
│                                                                     │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐   │
│  │   auth   │  │  student │  │recruiter │  │     analytics    │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────────┘   │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────────────┐   │
│  │  company │  │   job    │  │   app    │  │   notification   │   │
│  └──────────┘  └──────────┘  └──────────┘  └──────────────────┘   │
│                                                                     │
│  common: security · kafka · storage · audit · logging · exception  │
└────┬──────────────┬─────────────┬────────────────┬─────────────────┘
     │              │             │                │
     ▼              ▼             ▼                ▼
┌─────────┐  ┌───────────┐  ┌────────┐     ┌──────────┐
│PostgreSQL│  │   Redis   │  │ Kafka  │     │  AWS S3  │
│(Supabase)│  │  Cache +  │  │3 topics│     │ Resumes  │
│  16     │  │  Sessions │  │  DLQ   │     │ Pictures │
└─────────┘  └───────────┘  └────────┘     └──────────┘
```

**Event flow:** Service layer publishes domain events → `KafkaEventPublisher` delivers after DB commit via `@TransactionalEventListener(AFTER_COMMIT)` → `NotificationConsumer` creates in-app notifications + fires emails. If Kafka is unreachable, `NotificationFallbackListener` handles delivery directly via Spring `ApplicationEvents`.

---

## Quick Start

### Prerequisites

- Docker Desktop
- Java 21 (only needed if running outside Docker)
- An `.env` file — copy from `.env.example` and fill in values

### Run with Docker Compose

```bash
# 1. Clone the repository
git clone https://github.com/Pratyay-Patel/PlaceSync.git
cd PlaceSync

# 2. Set up environment variables
cp .env.example .env
# Edit .env — at minimum set DATABASE_PASSWORD, JWT_SECRET, MAIL_USERNAME, MAIL_APP_PASSWORD,
# AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY

# 3. Start all services (PostgreSQL + Redis + Kafka + API)
docker compose up --build -d

# 4. Check health
curl http://localhost:8080/actuator/health
```

The API is live at `http://localhost:8080`. Swagger UI is at `http://localhost:8080/swagger-ui/index.html`.

### Run locally (outside Docker)

Requires Docker Compose running for PostgreSQL, Redis, and Kafka.

```bash
# Windows (PowerShell) — set JAVA_HOME to JDK 21 if needed
$env:JAVA_HOME = "C:/Program Files/Eclipse Adoptium/jdk-21.0.11.10-hotspot"

# Load environment variables from .env
Get-Content .env | ForEach-Object {
    if ($_ -match '^([A-Z0-9_]+)=(.+)$') { Set-Item "env:$($matches[1])" $matches[2] }
}

mvn spring-boot:run "-Dspring-boot.run.profiles=dev"
```

---

## API Documentation

Interactive Swagger UI is available at `/swagger-ui/index.html` when the application is running.

All endpoints require a Bearer token in the `Authorization` header unless marked as public.

### Endpoint Summary

| Module | Prefix | Roles |
|---|---|---|
| Auth | `/api/v1/auth` | Public |
| Student Profile | `/api/v1/students` | STUDENT |
| Recruiter Profile | `/api/v1/recruiters` | RECRUITER |
| Companies | `/api/v1/companies` | Any / RECRUITER |
| Jobs | `/api/v1/jobs` | Any / RECRUITER |
| Applications | `/api/v1/applications`, `/api/v1/students/applications`, `/api/v1/recruiters/jobs/:id/applications` | STUDENT / RECRUITER |
| Interviews | `/api/v1/students/interviews`, `/api/v1/recruiters/interviews` | STUDENT / RECRUITER |
| Notifications | `/api/v1/notifications` | Any |
| Analytics | `/api/v1/analytics` | ADMIN / RECRUITER |
| Admin | `/api/v1/admin` | ADMIN |

---

## Project Structure

```
src/main/java/com/placesync/
├── auth/               # Registration, login, JWT, refresh tokens, password reset
├── user/               # Student profile, skills, education, experience, resumes
├── recruiter/          # Recruiter profile, verification
├── company/            # Company CRUD, logo upload, admin approval
├── job/                # Job posting, filtering, admin approval, Redis caching
├── application/        # Application lifecycle, status state machine
├── interview/          # Interview scheduling and management
├── notification/       # In-app inbox (Kafka consumer + fallback listener)
├── analytics/          # Placement stats, company and department breakdowns
└── common/
    ├── audit/          # AOP @Auditable aspect, audit_log table, admin search API
    ├── config/         # Security, Kafka, S3, Redis, Async, OpenAPI
    ├── event/          # Domain events (ApplicationSubmitted, InterviewScheduled, etc.)
    ├── exception/      # GlobalExceptionHandler, typed exceptions
    ├── kafka/          # KafkaEventPublisher, dead-letter handling
    ├── logging/        # MDC correlation ID filter, request/response logging
    ├── security/       # JwtTokenProvider, JwtAuthenticationFilter, UserPrincipal
    ├── storage/        # S3StorageService, FileValidationService (magic-byte check)
    └── util/           # ApiResponse, PagedResponse, ApiResponseFactory
```

---

## Environment Variables

| Variable | Description | Required |
|---|---|---|
| `DATABASE_URL` | PostgreSQL JDBC URL | Yes |
| `DATABASE_USERNAME` | Database user | Yes |
| `DATABASE_PASSWORD` | Database password | Yes |
| `JWT_SECRET` | HMAC-SHA256 signing key (min 32 chars) | Yes |
| `REDIS_HOST` | Redis hostname | Yes |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka bootstrap address | Yes |
| `AWS_ACCESS_KEY_ID` | AWS IAM key | Yes |
| `AWS_SECRET_ACCESS_KEY` | AWS IAM secret | Yes |
| `AWS_REGION` | AWS region (e.g. `ap-south-1`) | Yes |
| `AWS_S3_BUCKET_RESUMES` | S3 bucket for resumes | Yes |
| `AWS_S3_BUCKET_PICTURES` | S3 bucket for profile pictures | Yes |
| `MAIL_USERNAME` | Gmail address for SMTP | Yes |
| `MAIL_APP_PASSWORD` | Gmail App Password | Yes |
| `GOOGLE_CLIENT_ID` | Google OAuth2 client ID | Phase 6 |
| `GOOGLE_CLIENT_SECRET` | Google OAuth2 client secret | Phase 6 |

See `.env.example` for the full reference with defaults.

---

## Roadmap

| Phase | Scope | Status |
|---|---|---|
| 1 | Project bootstrap, Docker, Flyway, CI | ✅ Complete |
| 2 | Auth, JWT, student/recruiter/company modules | ✅ Complete |
| 3 | Jobs, applications, interviews, Redis caching | ✅ Complete |
| 4 | Kafka, notifications, AOP audit logging, admin module, unit tests | ✅ Complete |
| 5 | Analytics, AWS S3, email delivery, Supabase production integration | ✅ Complete |
| 6 | Integration tests, Testcontainers, Nginx, Render deployment, Google OAuth2, scheduled jobs | 🔄 In progress |
| 7 | React 18 + TypeScript frontend — student, recruiter, and admin dashboards | ⬜ Planned |

---

## License

This project is licensed under the MIT License.

---

<div align="center">
  Built by <a href="https://github.com/Pratyay-Patel">Pratyay Patel</a>
</div>
