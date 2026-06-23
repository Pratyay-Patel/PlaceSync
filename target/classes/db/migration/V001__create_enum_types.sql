-- ─────────────────────────────────────────────────────────────────────────────
-- V001 — Create custom ENUM types and shared trigger function
--
-- This is the foundation migration. Every subsequent migration that creates
-- tables depends on these types existing first.
--
-- Flyway applies migrations in version order (V001, V002, ...) on application
-- startup. Never modify this file after it has been applied to any environment.
-- ─────────────────────────────────────────────────────────────────────────────

-- User roles — maps to Spring Security GrantedAuthority values
CREATE TYPE user_role AS ENUM (
    'ROLE_STUDENT',
    'ROLE_RECRUITER',
    'ROLE_ADMIN'
);

-- Recruiter verification workflow states
CREATE TYPE verification_status AS ENUM (
    'PENDING_VERIFICATION',
    'VERIFIED',
    'REJECTED',
    'REVOKED'
);

-- Company approval workflow states
CREATE TYPE company_status AS ENUM (
    'PENDING_VERIFICATION',
    'VERIFIED',
    'REJECTED',
    'DEACTIVATED'
);

-- Job posting lifecycle states
-- Valid transitions: DRAFT → PENDING_APPROVAL → OPEN → CLOSED | EXPIRED | REJECTED
CREATE TYPE job_status AS ENUM (
    'DRAFT',
    'PENDING_APPROVAL',
    'OPEN',
    'CLOSED',
    'REJECTED',
    'EXPIRED'
);

-- Job work arrangement
CREATE TYPE job_location_type AS ENUM (
    'REMOTE',
    'ONSITE',
    'HYBRID'
);

-- Job employment category
CREATE TYPE job_type AS ENUM (
    'FULL_TIME',
    'INTERNSHIP',
    'CONTRACT'
);

-- Application lifecycle states (see SRS APP-FR-003 for valid transitions)
-- APPLIED → UNDER_REVIEW → SHORTLISTED | REJECTED
-- SHORTLISTED → INTERVIEW_SCHEDULED | REJECTED
-- INTERVIEW_SCHEDULED → OFFERED | REJECTED
CREATE TYPE application_status AS ENUM (
    'APPLIED',
    'UNDER_REVIEW',
    'SHORTLISTED',
    'INTERVIEW_SCHEDULED',
    'OFFERED',
    'REJECTED'
);

-- Interview format
CREATE TYPE interview_type AS ENUM (
    'ONLINE',
    'OFFLINE'
);

-- Interview scheduling lifecycle
CREATE TYPE interview_status AS ENUM (
    'SCHEDULED',
    'RESCHEDULED',
    'CANCELLED',
    'COMPLETED'
);

-- In-app notification categories (drives frontend icon and deep-link routing)
CREATE TYPE notification_type AS ENUM (
    'APPLICATION_SUBMITTED',
    'APPLICATION_STATUS_CHANGED',
    'INTERVIEW_SCHEDULED',
    'INTERVIEW_RESCHEDULED',
    'INTERVIEW_CANCELLED',
    'RECRUITER_VERIFIED',
    'RECRUITER_REJECTED',
    'OFFER_RELEASED'
);

-- Gender options for student profile
CREATE TYPE gender_type AS ENUM (
    'MALE',
    'FEMALE',
    'OTHER',
    'PREFER_NOT_TO_SAY'
);

-- Audit event action categories (used by audit_log.action column)
CREATE TYPE audit_action AS ENUM (
    'CREATE',
    'UPDATE',
    'DELETE',
    'SOFT_DELETE',
    'LOGIN_SUCCESS',
    'LOGIN_FAILURE',
    'LOGOUT',
    'PASSWORD_CHANGE',
    'PASSWORD_RESET',
    'EMAIL_VERIFIED',
    'ACCOUNT_LOCKED',
    'ACCOUNT_UNLOCKED'
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Shared trigger function: automatically sets updated_at = NOW() on every UPDATE.
-- Applied to each table that has an updated_at column in subsequent migrations.
-- This removes the dependency on the application layer remembering to set this field.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE FUNCTION set_updated_at()
    RETURNS TRIGGER
    LANGUAGE plpgsql
AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$;
