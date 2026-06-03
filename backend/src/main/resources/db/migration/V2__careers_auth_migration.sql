-- V2__careers_auth_migration.sql

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS candidate_profiles (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    full_name VARCHAR(255),
    phone VARCHAR(50),
    bio TEXT,
    professional_title VARCHAR(255),
    resume_file_name VARCHAR(500),
    resume_file_path VARCHAR(500),
    salary_expectation VARCHAR(100),
    work_mode_preference VARCHAR(50) DEFAULT 'REMOTE',
    smart_job_alerts BOOLEAN DEFAULT TRUE,
    app_status_updates BOOLEAN DEFAULT TRUE,
    employer_messaging BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS candidate_skills (
    id BIGSERIAL PRIMARY KEY,
    candidate_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    skill VARCHAR(100) NOT NULL,
    UNIQUE(candidate_id, skill)
);

-- ============================================================
-- CANDIDATE APPLICATIONS
-- ============================================================
CREATE TABLE IF NOT EXISTS external_candidates (
    id UUID PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    first_name VARCHAR(255),
    middle_name VARCHAR(255),
    last_name VARCHAR(255),
    phone VARCHAR(50),
    address TEXT,
    linkedin_url VARCHAR(255),
    github_url VARCHAR(255),
    portfolio_url VARCHAR(255),
    gender VARCHAR(50),
    consent_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    consent_accepted_at TIMESTAMPTZ,
    created_by BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS candidate_applications (
    application_id UUID PRIMARY KEY,
    job_id BIGINT NOT NULL REFERENCES job_postings(id) ON DELETE CASCADE,
    candidate_id UUID NOT NULL REFERENCES external_candidates(id) ON DELETE CASCADE,
    status VARCHAR(100) NOT NULL DEFAULT 'APPLIED',
    source VARCHAR(100) DEFAULT 'CAREER_PORTAL',
    applied_date TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(job_id, candidate_id)
);

