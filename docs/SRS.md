# Software Requirements Specification (SRS)
# PlaceSync — SaaS Placement Management Platform

**Version:** 1.0.0
**Status:** Draft
**Date:** 2026-06-23
**Author:** Pratyay Patel

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Introduction](#2-introduction)
   - 2.1 [Purpose](#21-purpose)
   - 2.2 [Scope](#22-scope)
   - 2.3 [Definitions, Acronyms, and Abbreviations](#23-definitions-acronyms-and-abbreviations)
   - 2.4 [Document Conventions](#24-document-conventions)
   - 2.5 [Intended Audience](#25-intended-audience)
   - 2.6 [References](#26-references)
3. [Product Vision](#3-product-vision)
4. [User Personas](#4-user-personas)
5. [Functional Requirements](#5-functional-requirements)
   - 5.1 [Authentication & Authorization](#51-authentication--authorization)
   - 5.2 [User Management](#52-user-management)
   - 5.3 [Student Profile Management](#53-student-profile-management)
   - 5.4 [Recruiter Management](#54-recruiter-management)
   - 5.5 [Company Management](#55-company-management)
   - 5.6 [Resume Management](#56-resume-management)
   - 5.7 [Job Management](#57-job-management)
   - 5.8 [Application Tracking](#58-application-tracking)
   - 5.9 [Interview Scheduling](#59-interview-scheduling)
   - 5.10 [Notification System](#510-notification-system)
   - 5.11 [Email System](#511-email-system)
   - 5.12 [Analytics Dashboard](#512-analytics-dashboard)
   - 5.13 [Audit Logging](#513-audit-logging)
   - 5.14 [Administrative Controls](#514-administrative-controls)
6. [Non-Functional Requirements](#6-non-functional-requirements)
   - 6.1 [Performance](#61-performance)
   - 6.2 [Scalability](#62-scalability)
   - 6.3 [Security](#63-security)
   - 6.4 [Availability & Reliability](#64-availability--reliability)
   - 6.5 [Maintainability](#65-maintainability)
   - 6.6 [Usability](#66-usability)
   - 6.7 [Compliance](#67-compliance)
7. [Constraints](#7-constraints)
8. [Assumptions](#8-assumptions)
9. [Risks & Mitigations](#9-risks--mitigations)
10. [Future Enhancements](#10-future-enhancements)
11. [Success Metrics](#11-success-metrics)
12. [Acceptance Criteria](#12-acceptance-criteria)

---

## 1. Executive Summary

PlaceSync is a production-grade, cloud-native SaaS platform designed to modernize campus placement and recruitment operations. It eliminates the fragmented, manual workflows that plague traditional placement cells — spreadsheets, email chains, and disconnected systems — by providing a unified, role-aware digital environment for students, recruiters, and placement administrators.

The platform's core value proposition is threefold:

- **For Students:** A single interface to build profiles, manage resumes, discover jobs, track applications, and receive interview updates.
- **For Recruiters:** Streamlined hiring workflows from job creation to candidate shortlisting and interview scheduling.
- **For Placement Administrators:** Real-time visibility, centralized control, and actionable analytics across the entire recruitment lifecycle.

PlaceSync is built with a Modular Monolith backend architecture using Java 21 and Spring Boot 3, a React/TypeScript frontend, PostgreSQL for persistence, Redis for caching, Apache Kafka for event-driven processing, and AWS S3 for file storage. It is designed to be deployable on a VPS via Docker with Nginx as the reverse proxy, with a GitHub Actions CI/CD pipeline. The system prioritizes security-first design, clean architecture, and production readiness from day one.

This document defines the software requirements for Version 1 (V1) of PlaceSync.

---

## 2. Introduction

### 2.1 Purpose

This Software Requirements Specification (SRS) defines the complete functional and non-functional requirements for the PlaceSync platform, Version 1. It serves as the authoritative contract between the product vision and the engineering implementation. All feature development, testing, and acceptance decisions in V1 shall be grounded in this document.

### 2.2 Scope

PlaceSync V1 covers:

- A React/TypeScript single-page application (SPA) consumed by three user roles: Student, Recruiter, and Placement Administrator.
- A Spring Boot 3 modular monolith backend exposing RESTful APIs, documented via OpenAPI/Swagger.
- JWT-based authentication with refresh tokens and Google OAuth2 login.
- PostgreSQL persistence with Flyway-managed schema migrations.
- Redis caching for performance-critical read paths.
- Apache Kafka for asynchronous event processing, with Spring Application Events as a fallback.
- AWS S3 for durable file storage (resumes and profile pictures).
- Gmail SMTP-based transactional email delivery.
- Full Docker-based deployment on an Ubuntu VPS with Nginx and HTTPS.
- GitHub Actions-based CI/CD pipeline.

Out of scope for V1:

- Microservices decomposition.
- Mobile native applications.
- Multi-institution / multi-tenant support.
- Payment processing.
- Video interview integration.
- Amazon SES migration.

### 2.3 Definitions, Acronyms, and Abbreviations

| Term | Definition |
|---|---|
| SRS | Software Requirements Specification |
| SaaS | Software as a Service |
| SPA | Single Page Application |
| JWT | JSON Web Token |
| OAuth2 | Open Authorization 2.0 |
| API | Application Programming Interface |
| REST | Representational State Transfer |
| JPA | Java Persistence API |
| ORM | Object Relational Mapper |
| CI/CD | Continuous Integration / Continuous Delivery |
| VPS | Virtual Private Server |
| S3 | Amazon Simple Storage Service |
| SMTP | Simple Mail Transfer Protocol |
| SES | Amazon Simple Email Service |
| MFA | Multi-Factor Authentication |
| RBAC | Role-Based Access Control |
| DTO | Data Transfer Object |
| FR | Functional Requirement |
| NFR | Non-Functional Requirement |
| V1 | Version 1 — initial production release |

### 2.4 Document Conventions

- Requirement identifiers follow the pattern `[MODULE]-FR-XXX` for functional requirements and `NFR-XXX` for non-functional requirements, where `XXX` is a zero-padded three-digit integer.
- Priority levels: **P0** (Critical / Must Have), **P1** (High / Should Have), **P2** (Medium / Could Have), **P3** (Low / Won't Have in V1).
- All timestamps are stored and processed in UTC.

### 2.5 Intended Audience

| Audience | Purpose |
|---|---|
| Developer / Architect | Implementation guidance and scope boundary |
| QA Engineer | Test case derivation and acceptance validation |
| Product Owner | Feature completeness verification |
| DevOps Engineer | Infrastructure and deployment requirements |
| Technical Reviewer | Architecture and design review |

### 2.6 References

- Spring Boot 3 Documentation — https://docs.spring.io/spring-boot/docs/3.x/reference/html/
- React Documentation — https://react.dev
- OpenAPI Specification 3.1 — https://spec.openapis.org/oas/v3.1.0
- OWASP Top 10 — https://owasp.org/www-project-top-ten/
- RFC 6749 — OAuth 2.0 Authorization Framework

---

## 3. Product Vision

### Vision Statement

> PlaceSync exists to eliminate friction in campus placement. Every student deserves a seamless path from profile to placement, and every recruiter deserves a modern, efficient way to identify and engage talent.

### Product Goals (V1)

1. **Digitize the Placement Lifecycle** — Replace ad-hoc spreadsheets and email threads with a structured, trackable workflow.
2. **Empower Students** — Give students ownership over their career data: profiles, resumes, applications, and outcomes.
3. **Accelerate Recruiter Workflows** — Reduce recruiter overhead from job creation to candidate selection.
4. **Enable Data-Driven Decisions** — Provide placement administrators with real-time analytics and audit trails.
5. **Demonstrate Production-Grade Engineering** — Serve as a reference implementation for modern SaaS backend development with Java and Spring Boot.

### Product Positioning

PlaceSync targets college and university placement cells that currently manage recruitment through disconnected tools. V1 focuses on a single-institution deployment with an admin-controlled workflow. Future versions may expand to multi-institution SaaS with self-service onboarding.

---

## 4. User Personas

### 4.1 Student — "Arjun"

**Background:** Final-year engineering student preparing for campus placements. Technically literate; uses mobile and desktop. Anxious about missing opportunities or making profile errors.

**Goals:**
- Maintain an up-to-date, professional profile.
- Apply to relevant jobs quickly without repetitive data entry.
- Track application status in real time to reduce uncertainty.
- Receive timely notifications about interviews or rejections.

**Pain Points:**
- Missing application deadlines due to poor communication.
- Uncertainty about whether an application was received or reviewed.
- Managing multiple resume versions across different job targets.
- Manually checking portals repeatedly for status updates.

**Key Workflows:**
- Register → Complete Profile → Upload Resume → Browse Jobs → Apply → Track Applications → Attend Interviews.

---

### 4.2 Recruiter — "Priya"

**Background:** HR manager at a mid-sized technology company. Attends multiple campus drives per year. Manages a shortlisting and interview pipeline across candidates.

**Goals:**
- Post jobs with structured eligibility criteria.
- Efficiently review and filter applicant pools.
- Schedule interviews without email back-and-forth.
- Communicate decisions to candidates.

**Pain Points:**
- High volume of unfiltered applicants with no structured filtering.
- Coordinating interview schedules across multiple stakeholders.
- Lack of a unified view of candidate history and status.

**Key Workflows:**
- Create Company Profile → Post Job → Review Applications → Shortlist/Reject → Schedule Interviews → Close Job.

---

### 4.3 Placement Administrator — "Dr. Meera"

**Background:** Placement coordinator at a university. Responsible for the overall placement cell operations: managing students, verifying recruiters, ensuring compliance, and reporting to department heads.

**Goals:**
- Maintain a verified roster of students and recruiters.
- Monitor placement progress across all open drives.
- Generate reports for institutional stakeholders.
- Ensure the platform is not misused.

**Pain Points:**
- Lack of real-time visibility into placement activity.
- Manual reconciliation of student outcomes at the end of each cycle.
- Difficulty detecting fraudulent or unverified recruiter activity.

**Key Workflows:**
- Manage Students → Verify Recruiters → Monitor Drives → View Analytics → Generate Reports → Manage Audit Logs.

---

## 5. Functional Requirements

### 5.1 Authentication & Authorization

#### 5.1.1 Registration

| ID | Requirement | Priority |
|---|---|---|
| AUTH-FR-001 | The system shall allow students to self-register with first name, last name, email, and password. | P0 |
| AUTH-FR-002 | The system shall validate that email addresses conform to standard email format. | P0 |
| AUTH-FR-003 | The system shall enforce a password strength policy: minimum 8 characters, at least one uppercase letter, one lowercase letter, one digit, and one special character. | P0 |
| AUTH-FR-004 | The system shall hash all passwords using BCrypt before persistence. Plain-text passwords must never be stored. | P0 |
| AUTH-FR-005 | The system shall send an email verification link to newly registered students. Unverified accounts shall not be able to log in. | P0 |
| AUTH-FR-006 | The system shall allow admins to create recruiter accounts directly or invite recruiters via email. | P0 |

#### 5.1.2 Login

| ID | Requirement | Priority |
|---|---|---|
| AUTH-FR-010 | The system shall authenticate users via email and password. | P0 |
| AUTH-FR-011 | On successful authentication, the system shall issue a signed JWT access token (short-lived: 15 minutes) and a refresh token (long-lived: 7 days). | P0 |
| AUTH-FR-012 | The system shall return a 401 Unauthorized response for invalid credentials. The error message must not distinguish between an invalid email and an invalid password. | P0 |
| AUTH-FR-013 | The system shall allow users to log in with Google OAuth2. New users authenticating via OAuth2 shall be auto-provisioned with the ROLE_STUDENT role by default. | P1 |
| AUTH-FR-014 | The system shall lock an account after five consecutive failed login attempts and notify the user by email. | P1 |

#### 5.1.3 Token Management

| ID | Requirement | Priority |
|---|---|---|
| AUTH-FR-020 | The system shall allow clients to exchange a valid refresh token for a new access token and a new refresh token (token rotation). | P0 |
| AUTH-FR-021 | The system shall invalidate refresh tokens on logout. Subsequent use of an invalidated refresh token shall return a 401. | P0 |
| AUTH-FR-022 | The system shall store refresh tokens securely and associate them with the owning user. | P0 |
| AUTH-FR-023 | The system shall allow a user to revoke all active sessions (logout from all devices). | P1 |

#### 5.1.4 Password Management

| ID | Requirement | Priority |
|---|---|---|
| AUTH-FR-030 | The system shall allow users to request a password reset via email. A time-limited reset token (valid for 1 hour) shall be sent. | P0 |
| AUTH-FR-031 | The system shall invalidate the reset token after a single use or upon expiration. | P0 |
| AUTH-FR-032 | The system shall allow authenticated users to change their password by providing their current password and a new password. | P1 |

#### 5.1.5 Authorization

| ID | Requirement | Priority |
|---|---|---|
| AUTH-FR-040 | The system shall implement Role-Based Access Control (RBAC) with three roles: ROLE_STUDENT, ROLE_RECRUITER, and ROLE_ADMIN. | P0 |
| AUTH-FR-041 | Each API endpoint shall be protected and accessible only to the authorized role(s). Unauthorized access attempts shall return 403 Forbidden. | P0 |
| AUTH-FR-042 | Users shall only be able to read or modify their own resources. Cross-user data access (e.g., Student A reading Student B's applications) shall be rejected with 403. | P0 |

---

### 5.2 User Management

| ID | Requirement | Priority |
|---|---|---|
| USER-FR-001 | The system shall maintain a central user entity with fields: id, email, role, isEmailVerified, isActive, createdAt, updatedAt. | P0 |
| USER-FR-002 | The system shall allow admins to deactivate any user account. Deactivated users shall not be able to log in. | P0 |
| USER-FR-003 | The system shall allow admins to reactivate a previously deactivated user. | P1 |
| USER-FR-004 | The system shall allow admins to search users by name, email, role, and status. | P1 |
| USER-FR-005 | The system shall soft-delete user records. Physical deletion shall not occur in V1. | P0 |

---

### 5.3 Student Profile Management

| ID | Requirement | Priority |
|---|---|---|
| STU-FR-001 | The system shall allow a student to create and maintain a profile including: full name, phone number, date of birth, gender, current institution, department, graduation year, CGPA, and a short bio. | P0 |
| STU-FR-002 | The system shall allow a student to upload a profile picture. The file shall be stored on AWS S3. Accepted formats: JPEG, PNG. Maximum size: 5 MB. | P0 |
| STU-FR-003 | The system shall allow a student to add, edit, and remove skills from their profile. | P0 |
| STU-FR-004 | The system shall allow a student to add, edit, and remove academic qualifications (degree, institution, year, marks). | P0 |
| STU-FR-005 | The system shall allow a student to add, edit, and remove work/internship experience entries. | P1 |
| STU-FR-006 | The system shall allow a student to mark their profile as publicly visible to recruiters or hidden. | P1 |
| STU-FR-007 | The system shall display a profile completeness percentage based on filled fields. | P2 |
| STU-FR-008 | The system shall enforce CGPA values between 0.0 and 10.0. | P0 |
| STU-FR-009 | Recruiters shall be able to search and view student profiles of students who have applied to their jobs. | P0 |

---

### 5.4 Recruiter Management

| ID | Requirement | Priority |
|---|---|---|
| REC-FR-001 | The system shall allow a recruiter to maintain a profile: full name, job title, contact email, and phone. | P0 |
| REC-FR-002 | New recruiter accounts shall have a status of PENDING_VERIFICATION until approved by an administrator. | P0 |
| REC-FR-003 | An admin shall be able to approve or reject a recruiter's verification request. | P0 |
| REC-FR-004 | Unverified recruiters shall not be able to post jobs or access student data. | P0 |
| REC-FR-005 | The system shall send the recruiter an email notification upon approval or rejection of their verification. | P1 |
| REC-FR-006 | An admin shall be able to revoke a recruiter's verified status. | P1 |

---

### 5.5 Company Management

| ID | Requirement | Priority |
|---|---|---|
| COM-FR-001 | The system shall allow a verified recruiter to create and manage a company profile: name, description, website, industry, headquarters, and logo. | P0 |
| COM-FR-002 | A company logo shall be stored on AWS S3. Accepted formats: JPEG, PNG. Maximum size: 2 MB. | P1 |
| COM-FR-003 | An admin shall be able to verify, reject, or deactivate company profiles. | P0 |
| COM-FR-004 | A recruiter may be associated with exactly one company in V1. | P0 |
| COM-FR-005 | Students shall be able to view company profiles when browsing jobs. | P0 |

---

### 5.6 Resume Management

| ID | Requirement | Priority |
|---|---|---|
| RES-FR-001 | The system shall allow a student to upload one or more resume files in PDF format. | P0 |
| RES-FR-002 | Uploaded resumes shall be stored on AWS S3. Maximum file size per resume: 10 MB. | P0 |
| RES-FR-003 | The system shall allow a student to maintain multiple resume versions and label each (e.g., "Software Engineer Resume", "Data Science Resume"). | P1 |
| RES-FR-004 | The system shall allow a student to designate one resume as the default. | P1 |
| RES-FR-005 | The system shall allow a student to delete a resume version, provided it is not attached to any active application. | P1 |
| RES-FR-006 | The system shall generate a pre-signed S3 URL for secure, time-limited resume download. | P0 |
| RES-FR-007 | Recruiters shall be able to download resumes of applicants who have applied to their jobs. Direct access to other students' resumes is not permitted. | P0 |
| RES-FR-008 | The system shall store resume metadata: filename, label, size, uploadedAt, and S3 key. | P0 |

---

### 5.7 Job Management

| ID | Requirement | Priority |
|---|---|---|
| JOB-FR-001 | A verified recruiter shall be able to create a job posting with: title, description, location (remote/onsite/hybrid), job type (full-time/internship/contract), CTC or stipend, application deadline, required skills, eligibility criteria (min CGPA, eligible departments). | P0 |
| JOB-FR-002 | The system shall enforce that application deadlines are set in the future at the time of creation. | P0 |
| JOB-FR-003 | A recruiter shall be able to edit an existing job posting. Edits to deadline and eligibility shall trigger re-notification to eligible students. | P1 |
| JOB-FR-004 | A recruiter shall be able to close a job posting. Closed jobs shall not accept new applications. | P0 |
| JOB-FR-005 | The system shall automatically close a job once its application deadline passes. | P0 |
| JOB-FR-006 | An admin shall be able to approve, reject, or remove job postings. | P0 |
| JOB-FR-007 | Students shall be able to browse all open, approved job postings. | P0 |
| JOB-FR-008 | Students shall be able to search and filter jobs by: title keyword, company name, location type, job type, required skills, and deadline. | P0 |
| JOB-FR-009 | The system shall cache the job listing results in Redis with an appropriate TTL. | P0 |
| JOB-FR-010 | The system shall paginate job listings. Default page size: 20. | P0 |
| JOB-FR-011 | Students who do not meet the eligibility criteria for a job shall not be able to apply. | P1 |

---

### 5.8 Application Tracking

| ID | Requirement | Priority |
|---|---|---|
| APP-FR-001 | A student shall be able to apply to an open job by selecting a resume from their uploaded resumes. | P0 |
| APP-FR-002 | The system shall prevent duplicate applications (one student, one job). Attempting to apply again shall return an appropriate error. | P0 |
| APP-FR-003 | An application shall have a status with the following lifecycle: APPLIED → UNDER_REVIEW → SHORTLISTED / REJECTED → INTERVIEW_SCHEDULED → OFFERED / REJECTED. | P0 |
| APP-FR-004 | A recruiter shall be able to update the status of an application. | P0 |
| APP-FR-005 | Status transitions shall follow the defined lifecycle. Invalid transitions shall be rejected with 400 Bad Request. | P0 |
| APP-FR-006 | The system shall send a notification to the student whenever their application status changes. | P0 |
| APP-FR-007 | A student shall be able to view all their submitted applications and their current status. | P0 |
| APP-FR-008 | A recruiter shall be able to view all applications for their job postings, with the ability to filter by status. | P0 |
| APP-FR-009 | An application event (ApplicationSubmitted) shall be published to Kafka when a student applies. | P0 |
| APP-FR-010 | An admin shall be able to view all applications across all jobs. | P1 |
| APP-FR-011 | A student shall not be able to withdraw a submitted application in V1. | P0 |

---

### 5.9 Interview Scheduling

| ID | Requirement | Priority |
|---|---|---|
| INT-FR-001 | A recruiter shall be able to schedule an interview for a shortlisted applicant by specifying: interview round number, interview type (online/offline), scheduled date and time, duration, and meeting link or venue. | P0 |
| INT-FR-002 | The system shall send an email and in-app notification to the student upon interview scheduling. | P0 |
| INT-FR-003 | The system shall publish an InterviewScheduled event to Kafka when an interview is created. | P0 |
| INT-FR-004 | A recruiter shall be able to reschedule an interview. The student shall be notified of any rescheduling. | P1 |
| INT-FR-005 | A recruiter shall be able to cancel an interview with a cancellation reason. The student shall be notified. | P1 |
| INT-FR-006 | A student shall be able to view all upcoming and past interview schedules. | P0 |
| INT-FR-007 | The system shall enforce that scheduled interview dates are in the future. | P0 |

---

### 5.10 Notification System

| ID | Requirement | Priority |
|---|---|---|
| NOTIF-FR-001 | The system shall maintain an in-app notification inbox for each user. | P0 |
| NOTIF-FR-002 | Notifications shall be created for the following events: application submitted, application status changed, interview scheduled, interview rescheduled, interview cancelled, recruiter verified/rejected, offer released. | P0 |
| NOTIF-FR-003 | A user shall be able to view all their notifications, paginated and sorted by recency. | P0 |
| NOTIF-FR-004 | A user shall be able to mark individual notifications as read. | P0 |
| NOTIF-FR-005 | A user shall be able to mark all notifications as read in a single action. | P1 |
| NOTIF-FR-006 | The system shall display an unread notification count badge in the frontend. | P1 |
| NOTIF-FR-007 | Notifications shall be consumed from Kafka topics and persisted to the database by the notification consumer. | P0 |
| NOTIF-FR-008 | If Kafka is unavailable, notifications shall be created synchronously via Spring Application Events. | P0 |

---

### 5.11 Email System

| ID | Requirement | Priority |
|---|---|---|
| EMAIL-FR-001 | The system shall send transactional emails using Gmail SMTP via Spring Mail in V1. | P0 |
| EMAIL-FR-002 | The system shall send emails for the following triggers: email verification, password reset, account locked, recruiter verification approved/rejected, interview scheduled, interview rescheduled, interview cancelled, offer released. | P0 |
| EMAIL-FR-003 | Emails shall use HTML templates with the PlaceSync branding. | P1 |
| EMAIL-FR-004 | Email sending shall be performed asynchronously to avoid blocking the request thread. | P0 |
| EMAIL-FR-005 | Failed email deliveries shall be logged. The system shall not fail a user-facing operation solely because an email could not be sent. | P0 |

---

### 5.12 Analytics Dashboard

| ID | Requirement | Priority |
|---|---|---|
| ANL-FR-001 | The admin dashboard shall display: total registered students, total verified recruiters, total active companies, total open job postings, total applications submitted, total offers made. | P0 |
| ANL-FR-002 | The admin dashboard shall display a placement rate: (students with at least one OFFERED application) / (total registered students). | P0 |
| ANL-FR-003 | The admin dashboard shall display top hiring companies by offer count. | P1 |
| ANL-FR-004 | The admin dashboard shall display top hired departments. | P1 |
| ANL-FR-005 | Analytics data shall be cached in Redis with a configurable TTL (default: 10 minutes). | P0 |
| ANL-FR-006 | The system shall invalidate the analytics cache when a new offer is recorded. | P0 |
| ANL-FR-007 | A recruiter shall be able to view analytics scoped to their own job postings: views, applications, shortlisted count, offer count. | P1 |

---

### 5.13 Audit Logging

| ID | Requirement | Priority |
|---|---|---|
| AUD-FR-001 | The system shall log all write operations (create, update, delete) to an audit log table. | P0 |
| AUD-FR-002 | Each audit log entry shall contain: entity type, entity ID, action, actor user ID, actor role, timestamp, and a before/after JSON snapshot of key fields. | P0 |
| AUD-FR-003 | An admin shall be able to query the audit log by entity type, actor, and time range. | P1 |
| AUD-FR-004 | Audit log entries shall be immutable. No update or delete operations shall be permitted on audit records. | P0 |
| AUD-FR-005 | The audit log shall record authentication events: login success, login failure, logout, password change, password reset. | P1 |

---

### 5.14 Administrative Controls

| ID | Requirement | Priority |
|---|---|---|
| ADM-FR-001 | An admin shall be able to view, search, and manage all students, recruiters, and companies. | P0 |
| ADM-FR-002 | An admin shall be able to activate or deactivate any user account. | P0 |
| ADM-FR-003 | An admin shall be able to approve or reject recruiter verification requests. | P0 |
| ADM-FR-004 | An admin shall be able to approve, reject, or remove job postings. | P0 |
| ADM-FR-005 | An admin shall be able to view all applications across all jobs. | P1 |
| ADM-FR-006 | An admin shall be able to view all interview schedules across all recruiters. | P1 |
| ADM-FR-007 | An admin shall have access to the analytics dashboard and audit logs. | P0 |
| ADM-FR-008 | The admin UI shall restrict access to users with the ROLE_ADMIN role only. Attempting to access admin routes with other roles shall redirect to 403. | P0 |

---

## 6. Non-Functional Requirements

### 6.1 Performance

| ID | Requirement |
|---|---|
| NFR-001 | The API p95 response time for read operations (job listing, student profile, applications) shall not exceed 300 ms under normal load. |
| NFR-002 | The API p95 response time for write operations (application submission, profile update) shall not exceed 500 ms. |
| NFR-003 | The system shall serve cached responses (Redis hits) within 50 ms. |
| NFR-004 | File uploads (resume, profile picture) shall complete within 10 seconds under normal network conditions. |
| NFR-005 | The frontend initial page load (SPA shell) shall complete within 3 seconds on a standard broadband connection. |

### 6.2 Scalability

| ID | Requirement |
|---|---|
| NFR-010 | The V1 architecture shall support at least 500 concurrent users without service degradation. |
| NFR-011 | The system shall be horizontally scalable at the backend layer by running multiple instances behind Nginx. |
| NFR-012 | The database connection pool shall be sized appropriately for the expected concurrency (configurable via application properties). |
| NFR-013 | The Kafka consumer group shall support scaling by adding additional consumer instances in the future. |

### 6.3 Security

| ID | Requirement |
|---|---|
| NFR-020 | All inter-service and client-server communication shall use HTTPS (TLS 1.2+). |
| NFR-021 | JWT access tokens shall expire within 15 minutes. |
| NFR-022 | Refresh tokens shall be stored securely and rotated on each use. |
| NFR-023 | All user passwords shall be stored using BCrypt with a cost factor of 12 or higher. |
| NFR-024 | The application shall implement security headers: Content-Security-Policy, X-Frame-Options, X-Content-Type-Options, Strict-Transport-Security. |
| NFR-025 | API rate limiting shall be implemented at the Nginx layer to prevent brute-force attacks (configurable threshold: e.g., 100 requests/minute per IP). |
| NFR-026 | Input validation shall be performed on all API inputs at the controller layer using Spring Validation. |
| NFR-027 | The application shall guard against OWASP Top 10 vulnerabilities (SQL injection, XSS, CSRF, etc.). |
| NFR-028 | S3 buckets shall not be publicly accessible. All file access shall use pre-signed URLs with a TTL. |
| NFR-029 | Database credentials, API keys, and secrets shall not be committed to version control. All secrets shall be injected via environment variables. |

### 6.4 Availability & Reliability

| ID | Requirement |
|---|---|
| NFR-030 | The system shall target 99% uptime on the VPS deployment in V1. |
| NFR-031 | The application shall implement graceful shutdown to allow in-flight requests to complete before process termination. |
| NFR-032 | Kafka consumer failures shall not cause data loss; unprocessed events shall be retried with exponential backoff. |
| NFR-033 | The system shall remain partially functional if Kafka is unavailable, by falling back to Spring Application Events for notification and email processing. |
| NFR-034 | The application shall implement health check endpoints (`/actuator/health`) for load balancer and monitoring integration. |

### 6.5 Maintainability

| ID | Requirement |
|---|---|
| NFR-040 | The backend codebase shall follow the defined modular monolith structure. No cross-module direct calls to internal repositories; all inter-module communication shall go through service layer interfaces. |
| NFR-041 | The codebase shall maintain a minimum SonarQube quality gate of: 0 critical bugs, 0 security vulnerabilities, <5% code duplication, >70% unit test coverage on service layer. |
| NFR-042 | Database schema changes shall be managed exclusively via Flyway migration scripts. Manual DDL changes to the production database are prohibited. |
| NFR-043 | The API contract shall be version-prefixed (`/api/v1/...`) to allow non-breaking evolution. |
| NFR-044 | All application configuration shall be externalized via `application.properties` or environment variables. No hardcoded environment-specific values in source code. |

### 6.6 Usability

| ID | Requirement |
|---|---|
| NFR-050 | The frontend shall be responsive and usable on screen widths from 375 px (mobile) to 1920 px (desktop). |
| NFR-051 | API error responses shall follow a consistent JSON error schema: `{ "status": <HTTP code>, "error": <error type>, "message": <human-readable message>, "timestamp": <ISO 8601 timestamp> }`. |
| NFR-052 | The system shall display meaningful validation error messages for all form inputs. |
| NFR-053 | The Swagger UI (`/swagger-ui.html`) shall be accessible in non-production environments for developer exploration. |

### 6.7 Compliance

| ID | Requirement |
|---|---|
| NFR-060 | The system shall not store sensitive personal data beyond what is necessary for placement operations. |
| NFR-061 | Users shall be able to export or request deletion of their personal data (foundational for future GDPR compliance). |
| NFR-062 | Audit logs shall be retained for a minimum of 12 months. |

---

## 7. Constraints

| # | Constraint |
|---|---|
| C-001 | The backend MUST be implemented in Java 21 with Spring Boot 3. No other backend language or framework is permitted in V1. |
| C-002 | The database MUST be PostgreSQL. No other relational or document database is permitted in V1. |
| C-003 | The frontend MUST use React with TypeScript and Vite. |
| C-004 | The architecture MUST be a Modular Monolith. Microservices decomposition is explicitly out of scope for V1. |
| C-005 | All infrastructure provisioning must be compatible with a single Ubuntu VPS. No Kubernetes or managed cloud orchestration in V1. |
| C-006 | The developer performs all Git operations manually. Automated CI pipelines must not commit, push, or merge code. |
| C-007 | All costs must be minimized for a personal learning project. Free tiers of Supabase, GitHub Actions, and SonarCloud should be utilized where possible. |
| C-008 | Maven is the required build tool. Gradle is not permitted. |

---

## 8. Assumptions

| # | Assumption |
|---|---|
| A-001 | A single institution is served by the V1 deployment. Multi-tenancy is not required. |
| A-002 | All users have stable internet connectivity sufficient for file uploads. |
| A-003 | The VPS has a minimum of 2 vCPUs and 4 GB RAM to run the full Docker Compose stack. |
| A-004 | AWS S3 credentials are available and the developer has the necessary IAM permissions to create and configure buckets. |
| A-005 | A Gmail account is available and an App Password is configured for SMTP usage. |
| A-006 | Google OAuth2 credentials (Client ID and Client Secret) are provisioned in Google Cloud Console. |
| A-007 | A domain name is available and DNS is configured to point to the VPS IP for HTTPS provisioning. |
| A-008 | Kafka is deployed as a single-broker instance in Docker Compose for V1. Multi-broker Kafka clustering is a future concern. |
| A-009 | Redis is deployed as a single-instance, non-clustered setup in Docker Compose for V1. |
| A-010 | The Supabase PostgreSQL instance is accessible from the VPS deployment environment. |

---

## 9. Risks & Mitigations

| # | Risk | Probability | Impact | Mitigation |
|---|---|---|---|---|
| R-001 | Kafka downtime causes notification and email loss | Medium | High | Implement Spring Application Events fallback. Log all failed Kafka publishes for replay. |
| R-002 | AWS S3 misconfiguration exposes student files | Low | Critical | Use private bucket policy. Enforce pre-signed URL access. Audit IAM policies. |
| R-003 | JWT secret key compromise | Low | Critical | Rotate secret keys via environment variable; all existing tokens become invalid on rotation. |
| R-004 | Gmail SMTP rate limits block email delivery | Medium | Medium | Implement async email sending with retry. Log delivery failures. Plan Amazon SES migration. |
| R-005 | Flyway migration failure on production database | Low | High | Always run migrations in a staging environment first. Keep migrations backward-compatible and reversible. |
| R-006 | SonarQube quality gate failure blocks CI | Medium | Low | Address code smells iteratively. Do not block the build on first integration. |
| R-007 | VPS resource exhaustion under load | Medium | Medium | Monitor CPU/RAM with Docker stats and OS-level metrics. Implement connection pool limits. |
| R-008 | Refresh token theft leads to session hijacking | Low | High | Implement refresh token rotation and family invalidation on reuse detection. |

---

## 10. Future Enhancements

The following capabilities are explicitly deferred to versions beyond V1:

| # | Enhancement |
|---|---|
| FE-001 | Multi-institution / multi-tenant support with isolated data namespacing. |
| FE-002 | Microservices decomposition of high-load modules (notifications, analytics). |
| FE-003 | Mobile native applications (iOS, Android) using React Native. |
| FE-004 | Video interview integration with a third-party provider (Zoom, Google Meet API). |
| FE-005 | Amazon SES migration to replace Gmail SMTP at scale. |
| FE-006 | Multi-Factor Authentication (MFA / TOTP). |
| FE-007 | Real-time notifications via WebSocket or Server-Sent Events. |
| FE-008 | AI-powered resume scoring and job recommendation. |
| FE-009 | GDPR-compliant data export and right-to-erasure workflows. |
| FE-010 | Placement calendar with iCal export for interview schedules. |
| FE-011 | Multi-broker Kafka cluster for high availability event streaming. |
| FE-012 | Redis Sentinel or Redis Cluster for cache high availability. |
| FE-013 | Kubernetes-based deployment with Helm charts. |
| FE-014 | Admin-configurable email templates. |
| FE-015 | Recruiter-side bulk resume download. |

---

## 11. Success Metrics

| # | Metric | Target (V1) |
|---|---|---|
| SM-001 | All P0 functional requirements implemented and tested | 100% |
| SM-002 | All P1 functional requirements implemented and tested | ≥ 90% |
| SM-003 | API p95 response time (read operations) | < 300 ms |
| SM-004 | SonarQube quality gate | Passed (0 critical bugs, 0 security vulnerabilities) |
| SM-005 | Unit test coverage (service layer) | ≥ 70% |
| SM-006 | CI/CD pipeline (build + test) completion time | < 10 minutes |
| SM-007 | Zero critical security vulnerabilities in OWASP scan | Pass |
| SM-008 | Full end-to-end student placement workflow operational | Pass |
| SM-009 | Full end-to-end recruiter hiring workflow operational | Pass |
| SM-010 | Docker Compose stack starts cleanly from a fresh clone | Pass |

---

## 12. Acceptance Criteria

### 12.1 Authentication

- [ ] A new student can register, receive an email verification link, verify their email, and log in.
- [ ] A logged-in user receives a JWT access token and a refresh token.
- [ ] Accessing a protected endpoint with an expired access token returns 401.
- [ ] A valid refresh token exchanges for a new access/refresh token pair.
- [ ] A student cannot access recruiter-only or admin-only endpoints (returns 403).
- [ ] Google OAuth2 login creates a new student account if one does not exist.
- [ ] Password reset flow works end-to-end via email link.

### 12.2 Student Workflow

- [ ] A student can complete their full profile including education, skills, and bio.
- [ ] A student can upload a profile picture and see it reflected on their profile.
- [ ] A student can upload a PDF resume, label it, and set it as default.
- [ ] A student can browse open jobs and apply using their uploaded resume.
- [ ] A student cannot apply to the same job twice.
- [ ] A student receives an in-app notification when their application status changes.
- [ ] A student can view their full application history with current statuses.
- [ ] A student can view their scheduled interviews.

### 12.3 Recruiter Workflow

- [ ] A newly registered recruiter account requires admin approval before posting jobs.
- [ ] A verified recruiter can create a company profile.
- [ ] A verified recruiter can create, edit, and close job postings.
- [ ] A recruiter can view all applications for their jobs.
- [ ] A recruiter can move an applicant through the status lifecycle.
- [ ] A recruiter can schedule an interview for a shortlisted candidate, and the student receives a notification and email.

### 12.4 Admin Workflow

- [ ] An admin can log in and access the admin dashboard.
- [ ] An admin can approve or reject recruiter verification requests.
- [ ] An admin can activate or deactivate any user.
- [ ] An admin can view platform-wide analytics.
- [ ] An admin can search the audit log by entity type and time range.

### 12.5 Infrastructure

- [ ] The full application stack starts with a single `docker compose up` command.
- [ ] The application is accessible via HTTPS on the configured domain.
- [ ] The GitHub Actions CI pipeline runs on every push to `main` and `develop` and produces a clear pass/fail result.
- [ ] Flyway migrations run automatically on application startup.
- [ ] Health check endpoint (`/actuator/health`) returns 200 OK in a healthy state.

---

*End of Software Requirements Specification — PlaceSync V1.0*
