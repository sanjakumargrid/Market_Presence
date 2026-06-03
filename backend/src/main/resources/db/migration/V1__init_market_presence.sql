-- V1__init_market_presence.sql

CREATE TABLE IF NOT EXISTS job_postings (
    id BIGSERIAL PRIMARY KEY,
    demand_id BIGINT,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    requirements TEXT,
    responsibilities TEXT,
    benefits TEXT,
    employment_type VARCHAR(255),
    work_mode VARCHAR(255),
    location VARCHAR(255),
    location_city VARCHAR(255),
    location_state VARCHAR(255),
    location_country VARCHAR(255),
    seniority VARCHAR(255),
    department VARCHAR(255),
    job_category VARCHAR(255),
    salary_min INTEGER,
    salary_max INTEGER,
    currency VARCHAR(255),
    show_salary BOOLEAN,
    meta_title VARCHAR(255),
    meta_description TEXT,
    status VARCHAR(255),
    slug VARCHAR(255) UNIQUE,
    application_deadline DATE,
    applications_count INTEGER DEFAULT 0 NOT NULL,
    published_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS application_intakes (
    id BIGSERIAL PRIMARY KEY,
    job_posting_id BIGINT,
    candidate_name VARCHAR(255),
    candidate_email VARCHAR(255),
    candidate_phone VARCHAR(255),
    resume_url VARCHAR(255),
    source VARCHAR(255),
    cover_letter TEXT,
    status VARCHAR(255),
    notes TEXT,
    applied_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_posting_channels (
    id BIGSERIAL PRIMARY KEY,
    job_posting_id BIGINT,
    channel_name VARCHAR(255),
    channel_url VARCHAR(255),
    status VARCHAR(255),
    error_message VARCHAR(255),
    posted_at TIMESTAMP,
    unpublished_at TIMESTAMP,
    expires_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS job_referrals (
    id BIGSERIAL PRIMARY KEY,
    job_posting_id BIGINT,
    referrer_id BIGINT,
    referred_candidate_name VARCHAR(255),
    referred_candidate_email VARCHAR(255),
    referral_code VARCHAR(255) UNIQUE,
    status VARCHAR(255),
    notes TEXT,
    referred_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE IF NOT EXISTS application_handoffs (
    id BIGSERIAL PRIMARY KEY,
    application_intake_id BIGINT,
    candidate_email VARCHAR(255),
    candidate_phone VARCHAR(255),
    job_slug VARCHAR(255),
    job_title VARCHAR(255),
    source VARCHAR(255),
    status VARCHAR(255),
    error_message TEXT,
    team2_response_id VARCHAR(255),
    attempted_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP
);
