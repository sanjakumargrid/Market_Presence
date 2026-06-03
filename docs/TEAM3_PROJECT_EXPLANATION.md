
# Forge PES v2.0 — Team 3: Market Presence Core Portal
## Complete Beginner-Friendly Technical Documentation

**Team:** Bangalore Team 3  
**Module:** Market Presence — Core Portal  
**Backend:** http://localhost:8086  
**Frontend:** http://localhost:5173  
**Database:** PostgreSQL — `forge_market_presence`  
**Requirements covered:** REQ-JP-01 through REQ-JP-11

---

# 1. Project Overview

## What is this project?

This project is the **careers portal** for Forge AI — an internal workforce intelligence platform built by Grid Dynamics. Think of it like LinkedIn Jobs or Indeed, but built specifically for Forge AI's hiring process.

When a business unit inside Forge AI decides they need to hire someone, that hiring need is called a **demand**. This project takes that demand and turns it into a publicly visible job posting that candidates can browse and apply to.

## What problem does it solve?

Without this system:
- HR would manually post jobs to LinkedIn, Indeed, and the company website
- Applications would arrive via email with no tracking
- There would be no way to automatically tell other systems (like candidate pipelines) when someone applies

With this system:
- Job postings are created automatically from demand data
- Candidates browse a real careers portal at `/careers`
- They apply through a multi-step form with resume upload
- Applications are automatically forwarded to Team 2's candidate pipeline system
- Jobs are automatically distributed to LinkedIn and Indeed

## How does it relate to Forge PES?

Forge PES (Personnel Engineering System) is a 6-team system:

| Team | Location | Responsibility |
|------|----------|---------------|
| Team 1 | Chennai | Demand management, Kafka events |
| **Team 3** | **Bangalore** | **This project — careers portal, job postings, applications** |
| Team 2 | ? | Candidate pipeline (receives applications from Team 3) |
| Teams 4-6 | Various | Other PES modules |

Team 3 sits in the middle: it **receives** demand events from Team 1, and **sends** application data to Team 2.

## What do REQ-JP-01 to REQ-JP-11 mean?

| Requirement | Plain English |
|-------------|--------------|
| REQ-JP-01 | When a demand becomes "open for external hiring", auto-create a job posting draft from the demand data |
| REQ-JP-02 | Allow HR to create, edit, and manage job postings manually |
| REQ-JP-03 | When a job is closed or expires, automatically unpublish it from all channels (LinkedIn, Indeed, careers portal) |
| REQ-JP-04 | When a job is published, automatically publish it to the careers portal channel |
| REQ-JP-05 | Job posting analytics: views, clicks, apply-starts, apply-completions per posting per channel — exposed to Team 4 analytics API |
| REQ-JP-06 | Build a public careers portal that candidates can browse without logging in |
| REQ-JP-07 | Allow candidates to submit a multi-step application with resume upload |
| REQ-JP-08 | Forward applications to Team 2's candidate pipeline with fallback if Team 2 is offline |
| REQ-JP-09 | Make the careers portal SEO-friendly with proper URLs, meta tags, and JSON-LD |
| REQ-JP-10 | Show employer branding on the careers portal (About Us, culture, benefits, employee stories) |
| REQ-JP-11 | Allow employees to share referral links that track the source of applications |

## What is implemented vs demo fallback?

| Feature | Status |
|---------|--------|
| Job posting CRUD | **Fully implemented** |
| Demand → job posting creation (REQ-JP-01) | **Fully implemented** |
| Public careers portal with filters | **Fully implemented** |
| 10-step apply form with resume | **Fully implemented** |
| Duplicate application blocking | **Fully implemented** |
| Team 2 handoff with outbox pattern | **Implemented — demo fallback** (URL empty, stays PENDING) |
| Careers portal channel auto-publish | **Fully implemented** |
| LinkedIn channel | **Demo fallback** (sets PENDING, gives copy-URL) |
| Indeed XML feed | **Fully implemented** |
| SEO meta tags + JSON-LD | **Fully implemented** |
| Employer branding | **Fully implemented** (static config) |
| Referral links + source tracking | **Fully implemented** |
| Authentication | **Demo mode** (hardcoded logged-in user) |
| Email confirmation | **Demo fallback** (logs to console) |
| Real LinkedIn API | **Not implemented** (would need API credentials) |

---

# 2. Architecture Overview

## System diagram

```
Browser (http://localhost:5173)
        │
        │  HTTP/JSON + Multipart
        ▼
React 19 + TypeScript + Vite
  ├── Axios (api layer)
  ├── React Query (server-state cache)
  ├── Zustand (auth state)
  └── React Router v6 (URL routing)
        │
        │  HTTP REST calls
        ▼
Spring Boot 4 + Java 21 (http://localhost:8086)
  ├── Controllers (REST endpoints)
  ├── Services (business logic)
  ├── Repositories (database access)
  └── Entities (database table models)
        │
        │  JDBC / JPA / Hibernate
        ▼
PostgreSQL (localhost:5432 / forge_market_presence)
```

## Is this microservices or monolith?

**Currently: a single monolith.** The backend is one Spring Boot application that handles everything — job postings, applications, channels, referrals, handoffs, the Indeed feed.

**How it can become microservices later:**
- Split `JobPostingService` into a standalone Job Management Service
- Split `ApplicationIntakeService` into a standalone Apply Service  
- Use Kafka (already owned by Chennai Team 1) to communicate between services
- Each service gets its own database schema

## What parts are demo substitutes?

| Real production system | Demo substitute used |
|------------------------|---------------------|
| Kafka event from Team 1 | `POST /api/job-postings/from-demand` REST endpoint |
| Team 2's candidate pipeline | HTTP REST call; if URL empty → PENDING record |
| SMTP email server | `LoggingEmailService` logs to console |
| LinkedIn Jobs API | Sets channel PENDING + gives copy-URL |
| Indeed XML submission | Returns feed URL at `/api/public/jobs/feed.xml` |
| JWT auth backend | `isAuthenticated: true` hardcoded in Zustand store |

---

# 3. Complete Folder Structure

## Backend folder structure

```
backend/
├── src/
│   ├── main/
│   │   ├── java/com/griddynamics/forge/market_presence_service/
│   │   │   ├── MarketPresenceServiceApplication.java   ← entry point
│   │   │   ├── config/
│   │   │   │   ├── SecurityConfig.java                 ← CORS + security rules
│   │   │   │   └── DataSeeder.java                     ← seeds demo data on startup
│   │   │   ├── controller/
│   │   │   │   ├── JobPostingController.java            ← admin CRUD endpoints
│   │   │   │   ├── PublicJobController.java             ← public browse endpoints
│   │   │   │   ├── ApplicationIntakeController.java     ← apply endpoint
│   │   │   │   ├── JobPostingChannelController.java     ← publish/unpublish channels
│   │   │   │   ├── JobPostingReferralController.java    ← generate referral links
│   │   │   │   ├── JobReferralController.java           ← legacy referral lookup
│   │   │   │   ├── HandoffStatusController.java         ← admin handoff inspector
│   │   │   │   └── IndeedFeedController.java            ← Indeed XML feed
│   │   │   ├── service/
│   │   │   │   ├── JobPostingService.java               ← job creation/status logic
│   │   │   │   ├── ApplicationIntakeService.java        ← full apply flow
│   │   │   │   ├── JobPostingChannelService.java        ← channel publish logic
│   │   │   │   ├── ApplicationHandoffService.java       ← Team 2 handoff + outbox
│   │   │   │   ├── JobReferralService.java              ← referral link generation
│   │   │   │   ├── ChannelExpiryScheduler.java          ← hourly expiry check
│   │   │   │   ├── EmailService.java                    ← interface
│   │   │   │   ├── LoggingEmailService.java             ← demo impl (logs only)
│   │   │   │   ├── FileStorageService.java              ← resume file storage
│   │   │   │   ├── Team2Client.java                     ← interface
│   │   │   │   └── HttpTeam2Client.java                 ← REST impl of Team2Client
│   │   │   ├── repository/
│   │   │   │   ├── JobPostingRepository.java
│   │   │   │   ├── ApplicationIntakeRepository.java
│   │   │   │   ├── JobPostingChannelRepository.java
│   │   │   │   ├── JobReferralRepository.java
│   │   │   │   └── HandoffRecordRepository.java
│   │   │   ├── entity/
│   │   │   │   ├── JobPosting.java                      ← job_postings table
│   │   │   │   ├── ApplicationIntake.java               ← application_intakes table
│   │   │   │   ├── JobPostingChannel.java               ← job_posting_channels table
│   │   │   │   ├── JobReferral.java                     ← job_referrals table
│   │   │   │   └── HandoffRecord.java                   ← application_handoffs table
│   │   │   ├── dto/
│   │   │   │   ├── JobPostingRequest.java
│   │   │   │   ├── JobPostingUpdateRequest.java
│   │   │   │   ├── JobPostingResponse.java
│   │   │   │   ├── PublicJobResponse.java
│   │   │   │   ├── DemandSnapshot.java
│   │   │   │   ├── ApplicationIntakeRequest.java
│   │   │   │   ├── ApplicationIntakeResponse.java
│   │   │   │   ├── JobPostingChannelResponse.java
│   │   │   │   ├── JobReferralRequest.java
│   │   │   │   ├── JobReferralResponse.java
│   │   │   │   ├── HandoffStatusResponse.java
│   │   │   │   ├── StatusUpdateRequest.java
│   │   │   │   ├── Team2ApplicationPayload.java
│   │   │   │   └── PagedResponse.java
│   │   │   └── exception/
│   │   │       ├── GlobalExceptionHandler.java
│   │   │       ├── ResourceNotFoundException.java
│   │   │       ├── ConflictException.java
│   │   │       └── ErrorResponse.java
│   │   └── resources/
│   │       └── application.yml                          ← all config (port, DB, channels)
│   └── test/
│       └── java/.../
│           ├── MarketPresenceServiceApplicationTests.java
│           ├── controller/
│           │   ├── JobPostingControllerTest.java        ← 13 tests
│           │   ├── ApplicationIntakeControllerTest.java ← 8 tests
│           │   ├── HandoffStatusControllerTest.java     ← 6 tests
│           │   ├── PublicJobControllerTest.java         ← 3 tests
│           │   └── IndeedFeedControllerTest.java        ← 3 tests
│           └── service/
│               ├── JobPostingServiceTest.java           ← 20 tests
│               ├── ApplicationHandoffServiceTest.java   ← 5 tests
│               ├── JobPostingChannelServiceTest.java    ← 10 tests
│               └── JobReferralServiceTest.java          ← 9 tests
└── pom.xml
```

## Frontend folder structure

```
frontend/frontend/
├── src/
│   ├── main.tsx                        ← React app entry point
│   ├── App.tsx                         ← QueryClient + Router setup
│   ├── index.css                       ← global Tailwind styles
│   ├── api/
│   │   ├── axios.ts                    ← Axios instance + JWT interceptor
│   │   ├── jobs.api.ts                 ← getJobs, getJobBySlug, filterJobs
│   │   ├── applications.api.ts         ← submitApplication, getMyApplications
│   │   ├── auth.api.ts                 ← login, register (calls port 8080)
│   │   └── profile.api.ts             ← getProfile, updateProfile (calls port 8080)
│   ├── pages/
│   │   ├── HomePage.tsx                ← landing + branding
│   │   ├── JobsPage.tsx                ← browse + filter jobs
│   │   ├── JobDetailsPage.tsx          ← single job + SEO + referral
│   │   ├── ApplicationPage.tsx         ← 10-step apply form
│   │   ├── ApplicationsPage.tsx        ← candidate's submitted applications
│   │   ├── ProfilePage.tsx             ← candidate profile editor
│   │   ├── LoginPage.tsx               ← login form
│   │   └── RegisterPage.tsx            ← register form
│   ├── components/
│   │   ├── application/
│   │   │   ├── ApplicationStepper.tsx  ← the 10-step form UI
│   │   │   ├── AlreadyAppliedModal.tsx ← friendly duplicate error modal
│   │   │   ├── ResumeUploader.tsx      ← PDF/DOCX file upload component
│   │   │   └── SuccessModal.tsx        ← post-apply confirmation modal
│   │   ├── common/
│   │   │   ├── Navbar.tsx              ← top navigation bar
│   │   │   ├── Sidebar.tsx             ← left sidebar navigation
│   │   │   └── Skeleton.tsx            ← loading placeholder cards
│   │   └── jobs/
│   │       └── JobFilters.tsx          ← filter panel sidebar
│   ├── features/jobs/
│   │   ├── hooks/
│   │   │   ├── useJobs.ts              ← React Query hook for job list
│   │   │   └── useJob.ts               ← React Query hook for single job
│   │   ├── services/
│   │   │   └── jobs.service.ts         ← re-exports from jobs.api.ts
│   │   └── types/
│   │       └── job.types.ts            ← TypeScript interfaces for all data shapes
│   ├── layouts/
│   │   └── CandidateLayout.tsx         ← wraps pages with Sidebar + Navbar
│   ├── routes/
│   │   └── AppRoutes.tsx               ← all URL → page mappings
│   ├── store/
│   │   ├── authStore.ts                ← Zustand auth state (demo: always logged in)
│   │   └── localStorage.ts             ← applied_jobs + candidate_profile persistence
│   ├── data/
│   │   └── branding.ts                 ← employer branding content (REQ-JP-10)
│   └── lib/
│       ├── useDocumentMeta.ts          ← SEO hook: title, meta, JSON-LD
│       ├── formatDate.ts               ← date formatting util
│       └── formatSalary.ts             ← salary range formatting util
├── .env.local                          ← VITE_API_URL=http://localhost:8086/api
├── package.json
├── vite.config.ts
└── tsconfig.json
```

---

# 4. Backend Deep Explanation

## controller package

### JobPostingController.java
**Purpose:** Admin CRUD operations for job postings. Only HR/admin users would call this in production.  
**Base URL:** `POST/GET/PUT/PATCH/DELETE /api/job-postings`  
**Supports:** REQ-JP-01, REQ-JP-02, REQ-JP-03, REQ-JP-04

| Method | URL | What it does |
|--------|-----|-------------|
| POST | `/api/job-postings` | Create a new job posting manually |
| POST | `/api/job-postings/from-demand` | Create job posting from demand snapshot (REQ-JP-01) |
| GET | `/api/job-postings` | List all job postings with optional filters |
| GET | `/api/job-postings/{id}` | Get one job posting by ID |
| GET | `/api/job-postings/slug/{slug}` | Get one job posting by slug |
| PUT | `/api/job-postings/{id}` | Full update of a job posting |
| PATCH | `/api/job-postings/{id}/status` | Change status (DRAFT→PUBLISHED→CLOSED) |
| DELETE | `/api/job-postings/{id}` | Delete a job posting |

**Request flow:** Request body → `@Valid` validation → `JobPostingService` → response DTO returned as JSON.

---

### PublicJobController.java
**Purpose:** Public-facing endpoints that candidates use without authentication.  
**Base URL:** `GET /api/public/jobs`  
**Supports:** REQ-JP-06

| Method | URL | What it does |
|--------|-----|-------------|
| GET | `/api/public/jobs` | List all PUBLISHED jobs (returns `PublicJobResponse` with snake_case fields) |
| GET | `/api/public/jobs/{slug}` | Get one published job by its URL slug |

**Why separate from `JobPostingController`?** The public controller returns `PublicJobResponse` (snake_case, safe fields only). The admin controller returns `JobPostingResponse` (camelCase, all admin fields). They use different DTOs for security and API contract clarity.

---

### ApplicationIntakeController.java
**Purpose:** Receives job applications as multipart form data.  
**Base URL:** `POST /api/public/jobs`  
**Supports:** REQ-JP-07

| Method | URL | What it does |
|--------|-----|-------------|
| POST | `/api/public/jobs/{slug}/apply` | Submit a job application with optional resume file |

**Important technical detail:** This endpoint uses `consumes = MediaType.MULTIPART_FORM_DATA_VALUE`. The request contains two parts:
- `application` — a JSON blob describing the candidate (name, email, phone, cover letter, referral code)
- `resume` — an optional PDF or DOCX file

---

### JobPostingChannelController.java
**Purpose:** Publish or unpublish a job to specific distribution channels (CAREERS_PORTAL, LINKEDIN, INDEED).  
**Base URL:** `/api/job-postings/{id}`  
**Supports:** REQ-JP-03, REQ-JP-04

| Method | URL | What it does |
|--------|-----|-------------|
| POST | `/api/job-postings/{id}/channels/{channel}/publish` | Publish to a specific channel |
| POST | `/api/job-postings/{id}/channels/{channel}/unpublish` | Unpublish from a specific channel |
| GET | `/api/job-postings/{id}/channels` | List all channel statuses for a job |

---

### JobPostingReferralController.java
**Purpose:** Generate referral links for a specific job posting.  
**Base URL:** `/api/job-postings/{id}`  
**Supports:** REQ-JP-11

| Method | URL | What it does |
|--------|-----|-------------|
| POST | `/api/job-postings/{id}/referrals` | Generate a new referral link. Body: `{ "referrerId": 123 }` |
| GET | `/api/job-postings/{id}/referrals` | List all referrals for this job |

---

### HandoffStatusController.java
**Purpose:** Admin endpoint to inspect and retry failed Team 2 handoffs.  
**Base URL:** `/api/admin/handoffs`  
**Supports:** REQ-JP-08

| Method | URL | What it does |
|--------|-----|-------------|
| GET | `/api/admin/handoffs` | List all handoff records (optional `?status=PENDING`) |
| GET | `/api/admin/handoffs/{id}` | Get details of one handoff record |
| POST | `/api/admin/handoffs/{id}/retry` | Manually retry a PENDING or FAILED handoff |

---

### IndeedFeedController.java
**Purpose:** Generates an XML job feed that can be submitted to Indeed Job Distributor.  
**Base URL:** `/api/public/jobs`  
**Supports:** REQ-JP-04 (Indeed channel)

| Method | URL | What it does |
|--------|-----|-------------|
| GET | `/api/public/jobs/feed.xml` | Returns Indeed-format XML of all PUBLISHED jobs |

---

### JobReferralController.java
**Purpose:** Legacy referral endpoint for looking up a referral by code.  
**Base URL:** `/api/referrals`

| Method | URL | What it does |
|--------|-----|-------------|
| POST | `/api/referrals` | Legacy create referral with candidate info |
| GET | `/api/referrals/{referralCode}` | Look up a referral record by its code |

---

## service package

### JobPostingService.java
**Purpose:** Core business logic for creating and managing job postings.  
**Depends on:** `JobPostingRepository`, `JobPostingChannelService`

**Key methods:**

`createFromDemand(DemandSnapshot snapshot)` — REQ-JP-01. Takes demand data and creates a draft job posting. Calls `normaliseSeniority()` to map demand level (e.g. "L4") to "SENIOR". Calls `buildRequirementsFromSkills()` to turn a list of skills into a formatted requirements string. Calls `resolveLocation()` to normalize location fields. Saves with status=DRAFT.

`updateStatus(id, newStatus)` — Contains the two most important business rules:
- If newStatus = PUBLISHED → calls `channelService.upsertCareersPortalChannel()` (REQ-JP-04)
- If newStatus = CLOSED → calls `channelService.unpublishAllLiveChannels()` (REQ-JP-03)

`validateSalaryRange(min, max)` — Throws `IllegalArgumentException` if max < min or if either is negative.

---

### ApplicationIntakeService.java
**Purpose:** Orchestrates the entire job application flow. The most complex service.  
**Depends on:** `ApplicationIntakeRepository`, `JobPostingRepository`, `FileStorageService`, `EmailService`, `ApplicationHandoffService`, `JobReferralService`

**The `apply(slug, request, resume)` method does these steps in order:**

1. Look up the job posting by slug → throw 404 if not found
2. Check for duplicate: `existsByJobPostingIdAndCandidateEmail` → throw `ConflictException` (409) if found
3. Store the resume file via `fileStorageService.store()` → get back a `resumeUrl`
4. Determine application source via `referralService.resolveSource(referralCode, jobId)` → either "REFERRAL" or "CAREERS_PORTAL"
5. Save the `ApplicationIntake` entity to the database
6. Increment `applicationsCount` on the `JobPosting` entity (REQ-JP-05)
7. Send confirmation email via `emailService.sendApplicationConfirmation()` (logs in demo)
8. If source is REFERRAL, call `referralService.markApplied(code, name, email)`
9. Call `handoffService.createAndAttempt(intake, job)` to forward to Team 2 (REQ-JP-08)

---

### JobPostingChannelService.java
**Purpose:** Manages the publish/unpublish lifecycle for all distribution channels.  
**Depends on:** `JobPostingChannelRepository`, `JobPostingRepository`

**Key methods:**

`publishChannel(jobId, channelName)` — Routes to channel-specific logic:
- **CAREERS_PORTAL:** Always succeeds. Sets status=LIVE, stores URL `{careersPortalBaseUrl}/jobs/{slug}`
- **LINKEDIN:** If `app.channels.linkedin.api-configured: false` → sets PENDING, stores copy-URL message. If true → would call LinkedIn API (not implemented)
- **INDEED:** Always sets LIVE, stores the feed URL `http://localhost:8086/api/public/jobs/feed.xml`

`unpublishAllLiveChannels(jobId)` — Finds all channels with status LIVE (or legacy "ACTIVE"/"PUBLISHED") and sets them to UNPUBLISHED. Called when job is CLOSED or expired.

`upsertCareersPortalChannel(jobPosting)` — Creates or updates the CAREERS_PORTAL channel record to LIVE status. Called automatically when a job is published (REQ-JP-04).

---

### ApplicationHandoffService.java
**Purpose:** Implements the outbox/fallback pattern to forward applications to Team 2.  
**Depends on:** `HandoffRecordRepository`, `Team2Client`

**The `createAndAttempt(intake, job)` method:**
1. Creates a `HandoffRecord` with status=PENDING and saves it immediately
2. Checks if `app.team2.api-base-url` is configured
3. If yes: calls `team2Client.send(payload)` → on success: updates status=SENT; on exception: updates status=FAILED
4. If no: logs a warning, leaves record as PENDING (retriable via admin endpoint)
5. **Never throws** — the apply flow must complete regardless of Team 2's availability

---

### JobReferralService.java
**Purpose:** Generates referral links and resolves referral source during application.  
**Depends on:** `JobReferralRepository`, `JobPostingRepository`

**Key methods:**

`generateLink(jobPostingId, referrerId)` — Creates a `JobReferral` record with a random UUID as the referral code. The `referralUrl` is built as `{careersPortalBaseUrl}/careers/{slug}?ref={code}`. Status is explicitly set to "PENDING" (cannot rely on `@PrePersist` in test environments).

`resolveSource(referralCode, jobPostingId)` — Returns "REFERRAL" if the code exists and belongs to this job. Returns "CAREERS_PORTAL" otherwise.

`markApplied(code, name, email)` — Updates the referral record with the candidate's name and email once they submit an application.

---

### ChannelExpiryScheduler.java
**Purpose:** Automatically closes expired jobs and unpublishes their channels.  
**Annotation:** `@Scheduled(cron = "0 0 * * * *")` — runs at the top of every hour

**Logic:** Queries all PUBLISHED jobs where `applicationDeadline` is before today. For each: sets status=CLOSED, calls `channelService.unpublishAllLiveChannels()`.

---

### EmailService.java + LoggingEmailService.java
**Purpose:** Abstracts email sending so a real SMTP implementation can be plugged in later.  
**Interface:** `sendApplicationConfirmation(toEmail, candidateName, jobTitle)`  
**Demo implementation:** `LoggingEmailService` writes to SLF4J INFO log instead of sending an email.

---

### FileStorageService.java
**Purpose:** Validates and stores resume files uploaded during application.

**Validation rules:**
- File must be PDF or DOCX (checks both MIME type and file extension)
- Throws `IllegalArgumentException` for unsupported types

**Storage:** Files are saved at `uploads/resumes/{jobSlug}/{originalFilename}`. The relative path is returned as `resumeUrl` and stored in the `ApplicationIntake` record.

---

### Team2Client.java + HttpTeam2Client.java
**Purpose:** Sends application data to Team 2's REST API.  
**Interface method:** `send(Team2ApplicationPayload)` returns `Optional<String>` (Team 2's assigned application ID)  
**Implementation:** Uses Spring `RestClient` to POST to `{app.team2.api-base-url}/applications/intake`

---

## repository package

All repositories extend `JpaRepository<Entity, Long>` which automatically provides: `save()`, `findById()`, `findAll()`, `deleteById()`, `existsById()`, `count()`.

### JobPostingRepository.java
**Entity:** `JobPosting`  
**Custom methods:**
- `findBySlug(String slug)` → `Optional<JobPosting>`
- `findByStatus(String status)` → `List<JobPosting>`
- `findByStatusAndApplicationDeadlineBefore(String status, LocalDate date)` → for expiry scheduler
- `existsBySlug(String slug)` → for uniqueness check

### ApplicationIntakeRepository.java
**Entity:** `ApplicationIntake`  
**Custom methods:**
- `existsByJobPostingIdAndCandidateEmail(Long jobId, String email)` → duplicate check
- `findByJobPostingId(Long jobId)` → list all applications for a job

### JobPostingChannelRepository.java
**Entity:** `JobPostingChannel`  
**Custom methods:**
- `findByJobPostingId(Long jobId)` → list all channels for a job
- `findByJobPostingIdAndChannelName(Long jobId, String channelName)` → find specific channel

### JobReferralRepository.java
**Entity:** `JobReferral`  
**Custom methods:**
- `findByReferralCode(String code)` → `Optional<JobReferral>`
- `findByJobPostingId(Long jobId)` → list all referrals for a job

### HandoffRecordRepository.java
**Entity:** `HandoffRecord`  
**Custom methods:**
- `findByStatus(String status)` → filter by PENDING/SENT/FAILED

---

## entity/model package

### JobPosting.java — Table: `job_postings`

| Column | Type | Meaning |
|--------|------|---------|
| id | BIGINT PK | Auto-generated ID |
| demand_id | BIGINT | Links to the demand that triggered this job (REQ-JP-01) |
| title | VARCHAR | Job title e.g. "Senior Java Engineer" |
| slug | VARCHAR UNIQUE | URL-friendly ID e.g. "senior-java-engineer-bangalore-2024" |
| status | VARCHAR | DRAFT / PUBLISHED / CLOSED |
| description | TEXT | Full job description |
| requirements | TEXT | Skills and qualifications needed |
| responsibilities | TEXT | What the role entails |
| benefits | TEXT | What the company offers |
| employment_type | VARCHAR | FULL_TIME / PART_TIME / CONTRACT |
| work_mode | VARCHAR | REMOTE / HYBRID / ON_SITE |
| location | VARCHAR | Free-text location summary |
| location_city | VARCHAR | City e.g. "Bangalore" |
| location_state | VARCHAR | State e.g. "Karnataka" |
| location_country | VARCHAR | Country e.g. "India" |
| seniority | VARCHAR | JUNIOR / MID / SENIOR / LEAD / PRINCIPAL |
| department | VARCHAR | Department e.g. "Engineering" |
| job_category | VARCHAR | Role type e.g. "Backend Development" |
| salary_min | DECIMAL | Minimum salary |
| salary_max | DECIMAL | Maximum salary |
| currency | VARCHAR | INR / USD |
| show_salary | BOOLEAN | Whether to display salary on portal |
| meta_title | VARCHAR | SEO page title (REQ-JP-09) |
| meta_description | VARCHAR | SEO meta description (REQ-JP-09) |
| application_deadline | DATE | Expiry date for applications (REQ-JP-03) |
| applications_count | INT | Running total of applications (REQ-JP-05) |
| published_at | TIMESTAMP | When the job was first published |
| created_at | TIMESTAMP | Record creation time |
| updated_at | TIMESTAMP | Last modification time |

`@PrePersist` sets default values for `applicationsCount=0`, `currency="INR"`, `showSalary=false`, and timestamps.

---

### ApplicationIntake.java — Table: `application_intakes`

| Column | Type | Meaning |
|--------|------|---------|
| id | BIGINT PK | Auto-generated ID |
| job_posting_id | BIGINT FK | References `job_postings.id` |
| candidate_name | VARCHAR | Full name |
| candidate_email | VARCHAR | Email — part of uniqueness constraint |
| candidate_phone | VARCHAR | Phone number |
| resume_url | VARCHAR | Path to stored resume file |
| source | VARCHAR | "CAREERS_PORTAL" or "REFERRAL" (REQ-JP-11) |
| cover_letter | TEXT | Candidate's cover letter + extra form data |
| status | VARCHAR | Default "SUBMITTED" |
| notes | TEXT | Internal HR notes |
| applied_at | TIMESTAMP | When application was submitted |
| created_at | TIMESTAMP | Record creation time |
| updated_at | TIMESTAMP | Last modification time |

**Unique constraint:** `(job_posting_id, candidate_email)` — one email per job.

---

### JobPostingChannel.java — Table: `job_posting_channels`

| Column | Type | Meaning |
|--------|------|---------|
| id | BIGINT PK | Auto-generated |
| job_posting_id | BIGINT FK | Which job this channel belongs to |
| channel_name | VARCHAR | "CAREERS_PORTAL" / "LINKEDIN" / "INDEED" |
| channel_url | VARCHAR | Where the job was published |
| status | VARCHAR | DRAFT / PENDING / LIVE / FAILED / UNPUBLISHED |
| error_message | VARCHAR | Why it failed, or copy-URL message for LINKEDIN/INDEED |
| posted_at | TIMESTAMP | When it went LIVE |
| unpublished_at | TIMESTAMP | When it was unpublished |
| expires_at | TIMESTAMP | Optional channel-level expiry |
| created_at | TIMESTAMP | Record creation time |
| updated_at | TIMESTAMP | Last modification time |

**Unique constraint:** `(job_posting_id, channel_name)` — one record per channel per job.

---

### JobReferral.java — Table: `job_referrals`

| Column | Type | Meaning |
|--------|------|---------|
| id | BIGINT PK | Auto-generated |
| job_posting_id | BIGINT FK | Which job the referral is for |
| referrer_id | BIGINT | Employee who made the referral |
| referred_candidate_name | VARCHAR | Candidate name (filled after they apply) |
| referred_candidate_email | VARCHAR | Candidate email |
| referral_code | VARCHAR UNIQUE | Random UUID used in share URL |
| status | VARCHAR | PENDING / APPLIED / HIRED |
| notes | TEXT | Optional notes |
| referred_at | TIMESTAMP | When referral was created |
| created_at | TIMESTAMP | Record creation |
| updated_at | TIMESTAMP | Last modification |

---

### HandoffRecord.java — Table: `application_handoffs`

| Column | Type | Meaning |
|--------|------|---------|
| id | BIGINT PK | Auto-generated |
| application_intake_id | BIGINT | Which application this is for |
| candidate_email | VARCHAR | Denormalized for quick lookup |
| candidate_phone | VARCHAR | Denormalized |
| job_slug | VARCHAR | Denormalized |
| job_title | VARCHAR | Denormalized |
| source | VARCHAR | CAREERS_PORTAL or REFERRAL |
| status | VARCHAR | PENDING / SENT / FAILED |
| error_message | TEXT | What went wrong if FAILED |
| team2_response_id | VARCHAR | ID that Team 2 returned on success |
| attempted_at | TIMESTAMP | When the HTTP attempt was made |
| created_at | TIMESTAMP | Record creation |
| updated_at | TIMESTAMP | Last modification |

---

## dto package

### DemandSnapshot.java (Request)
Used by `POST /api/job-postings/from-demand` to simulate Team 1 sending demand data.

| Field | Validation | Meaning |
|-------|-----------|---------|
| demandId | — | ID from Team 1's demand system |
| title | `@NotBlank` | Job title |
| level | — | e.g. "L4", "L5" → normalized to seniority |
| skills | `List<String>` | Required skills → built into requirements text |
| location | — | Location string |
| locationCity/State/Country | — | Structured location |
| department | — | Department name |
| targetDate | `@Future` | Must be a future date |

### ApplicationIntakeRequest.java (Request)
The JSON part of the multipart apply request.

| Field | Validation | Meaning |
|-------|-----------|---------|
| candidateName | `@NotBlank` | Full name |
| candidateEmail | `@NotBlank @Email` | Email — used for duplicate check |
| candidatePhone | `@Pattern(...)` | Phone number format validation |
| coverLetter | — | Optional cover letter text |
| referralCode | — | Optional referral code (REQ-JP-11) |

### PublicJobResponse.java (Response)
Returned by `GET /api/public/jobs` and `GET /api/public/jobs/{slug}`.  
**Uses `@JsonNaming(SnakeCaseStrategy.class)`** so all fields are snake_case — matching what the frontend's `job.types.ts` `Job` interface expects.

Key field mappings:
- Entity `seniority` → response field `experience_level`
- Entity `status` → response field `posting_status`
- Entity `applicationDeadline` → response field `expires_at`

### Team2ApplicationPayload.java (Internal)
Used internally to build what gets sent to Team 2. Contains: `firstName`, `lastName`, `email`, `phone`, `source`, `resumeUrl`, `jobSlug`, `jobTitle`, `appliedAt`, `applicationIntakeId`.

### PagedResponse.java (Generic)
A generic record `PagedResponse<T>` with fields: `content`, `page`, `size`, `totalElements`, `totalPages`, `last`.

---

## exception package

### GlobalExceptionHandler.java
Annotated with `@RestControllerAdvice` — intercepts all exceptions thrown by any controller and converts them to structured JSON error responses.

| Exception | HTTP Status | When it happens |
|-----------|-------------|----------------|
| `ResourceNotFoundException` | 404 | Entity not found by ID or slug |
| `ConflictException` | 409 | Duplicate application, duplicate slug |
| `IllegalArgumentException` | 400 | Bad input (invalid salary, bad file type) |
| `MethodArgumentNotValidException` | 400 | `@Valid` annotation failures |
| `Exception` (catch-all) | 500 | Unexpected server errors |

**Error response format:**
```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Unsupported file type: text/plain",
  "details": []
}
```

**Why this matters for frontend:** The frontend's `applications.api.ts` specifically checks for status 409 to show the `AlreadyAppliedModal` instead of a generic error.

---

## config package

### SecurityConfig.java
- Disables CSRF (safe for REST APIs)
- Sets all endpoints to `permitAll()` — no authentication required (demo mode)
- Configures CORS to allow requests from `http://localhost:3000` and `http://localhost:5173`

### MarketPresenceServiceApplication.java
- Entry point annotated with `@SpringBootApplication`
- Also has `@EnableScheduling` which enables the `@Scheduled` annotation in `ChannelExpiryScheduler`

---

## seeder/bootstrap package

### DataSeeder.java
Implements `CommandLineRunner` — runs once on every application startup.

**What it seeds:**
1. `patchLegacyJobs()` — fixes any jobs with null status by setting them to DRAFT
2. `patchLegacyChannelNames()` — normalizes old channel names like "LinkedIn" to "LINKEDIN"
3. **8 job postings** across different departments, seniorities, and work modes (e.g. "Senior Java Engineer", "React Frontend Engineer", "DevOps Engineer")
4. **Application intakes** for some of those jobs (demo applicants)
5. **Channel records** for those jobs
6. **Referral records** for some jobs
7. `seedCareersPortalChannels()` — ensures every PUBLISHED job has a CAREERS_PORTAL channel with status=LIVE

**Why is seeding needed?** The frontend careers portal needs real data to show. Without seed data, the job listing would be empty. The seeder prevents duplicate inserts by checking `existsBySlug()` before creating each record.

---

# 5. Frontend Deep Explanation

## api folder

### axios.ts
The shared Axios instance used by all API functions.

```
Base URL: VITE_API_URL env variable (defaults to http://localhost:8086/api via .env.local)
Timeout: 15 seconds
Default Content-Type: application/json
```

**Request interceptor:** Before every request, reads `forge_token` from localStorage and adds it as `Authorization: Bearer {token}`. In demo mode this is `"mock-token"` — the backend ignores it because `permitAll()`.

**Response interceptor:** If any response returns 401, clears localStorage and fires `window.dispatchEvent(new Event('auth:logout'))`. The `App.tsx` `useEffect` listens for this event and calls Zustand's `logout()`.

---

### jobs.api.ts
API functions for browsing jobs. All call the **backend at port 8086**.

`getJobs()` → `GET /api/public/jobs` → returns `Job[]`

`getJobBySlug(slug)` → `GET /api/public/jobs/{slug}` → returns single `Job`

`filterJobs(jobs, filters)` — **client-side** filtering function. Takes the full jobs array and applies filters for: search text (searches title, department, city, category, description, requirements), work_mode, experience_level, employment_type, location_city, department. Then sorts by latest/salary_high/salary_low/a-z.

`searchJobsPaginated(filters)` — calls `getJobs()` then `filterJobs()` then slices for pagination.

`getDepartments()` — calls `getJobs()` and extracts unique department values.

---

### applications.api.ts
Handles submitting and viewing applications.

**`submitApplication(jobSlug, formData, files, referralCode?)`** — The most complex API function:
1. Combines first/middle/last name into `candidateName`
2. Builds a plain-text cover letter by combining all form fields the backend doesn't have dedicated columns for (location, LinkedIn URL, GitHub URL, skills list, latest experience, screening answers)
3. Creates a `FormData` object with two parts:
   - `application` — JSON blob with `candidateName`, `candidateEmail`, `candidatePhone`, `coverLetter`, optional `referralCode`
   - `resume` — the actual file if provided
4. Sets `Content-Type: undefined` so the browser sets the multipart boundary automatically
5. On 409 response → throws `Error('already applied')` (triggers `AlreadyAppliedModal`)
6. On success → returns an `Application` object and saves to localStorage

**`getMyApplications()`** → `GET /api/applications/mine` — calls the **frontend/backend service at port 8080** (not port 8086). Falls back gracefully if not available.

---

### auth.api.ts
`login()` → `POST /api/auth/login`  
`register()` → `POST /api/auth/register`

These call the **frontend/backend service at port 8080**, not the Team 3 backend. In demo mode, these calls will fail (port 8080 is not running), but since `authStore` hardcodes `isAuthenticated: true`, the UI works anyway.

---

### profile.api.ts
`getProfile()` → `GET /api/profile`  
`updateProfile(profile)` → `PUT /api/profile`  
`uploadResume(file)` → `POST /api/profile/resume`

Also calls port 8080. Falls back to localStorage in `ProfilePage.tsx`.

---

## pages folder

### HomePage.tsx
**Route:** `/`  
**Purpose:** Landing page showcasing employer branding (REQ-JP-10)  
**Content:** Company tagline, mission statement, About Us section, culture values (4 cards), benefits (6 items), employee stories (3 testimonial cards), featured jobs section with quick-apply links  
**Data source:** `src/data/branding.ts` (static) + `useJobs()` hook for featured jobs  
**No authentication required**

---

### JobsPage.tsx
**Route:** `/jobs` (also `/careers` redirects here)  
**Purpose:** Browse and filter all published job postings (REQ-JP-06)  
**Components used:** `JobFilters` sidebar, `Skeleton` loading cards, job listing grid  
**API calls:** `useJobs()` hook → `getJobs()` → `GET /api/public/jobs`  
**Client-side filtering:** `filterJobs()` from `jobs.api.ts`  
**Features:** Search box, work mode filter, experience level filter, department filter, location filter, sort options, pagination

---

### JobDetailsPage.tsx
**Route:** `/jobs/:slug` and `/careers/:slug`  
**Purpose:** Shows full details of one job posting. The canonical SEO URL is `/careers/:slug`.  
**Supports:** REQ-JP-06, REQ-JP-09, REQ-JP-11

**Key behaviors:**
1. Reads the `?ref=` query parameter from the URL. If present, shows a referral banner.
2. Calls `useDocumentMeta()` to set:
   - `document.title` to the job's `meta_title`
   - `<meta name="description">` to the job's `meta_description`
   - `<script type="application/ld+json">` with a `JobPosting` schema
3. The "Share" button copies a URL in the format `/careers/{slug}?ref={YOUR_REFERRAL_CODE}` to clipboard
4. The "Apply Now" button navigates to `/careers/{slug}/apply` passing `{ state: { referralCode } }` via React Router's location state

---

### ApplicationPage.tsx
**Route:** `/jobs/:slug/apply` and `/careers/:slug/apply`  
**Purpose:** 10-step application form (REQ-JP-07)  
**Authentication:** `ProtectedRoute` wraps it, but in demo mode all routes pass through

**10 steps:**
1. Resume upload (PDF/DOCX)
2. Personal information (name, email, phone, location, LinkedIn, GitHub, Portfolio)
3. Work experience (multiple entries)
4. Education (multiple entries)
5. Skills (multiple with proficiency level)
6. Certifications
7. Projects
8. Screening questions (visa status, notice period, expected CTC, relocation)
9. Equal opportunity documents (gender, veteran status, disability)
10. Review and GDPR consent

**Duplicate check:** On mount, calls `hasApplied(slug)` from localStorage. If true → shows `AlreadyAppliedModal` immediately before the form opens.

**On submit:** Calls `submitApplication(slug, formData, files, referralCode)`. On success → shows `SuccessModal` and saves application to localStorage via `addApplication()`.

---

### ApplicationsPage.tsx
**Route:** `/applications`  
**Purpose:** Shows the candidate's submitted applications  
**Data strategy:** Tries `getMyApplications()` (port 8080) first. If that fails, falls back to `getAppliedJobs()` from localStorage.

---

### ProfilePage.tsx
**Route:** `/profile`  
**Purpose:** View and edit candidate profile (name, phone, bio, skills, preferences)  
**Data strategy:** Tries `getProfile()` from backend (port 8080). Falls back to `getProfile()` from localStorage. Save action updates both localStorage and attempts backend save.

---

## components folder

### ApplicationStepper.tsx
The multi-step form UI. Renders whichever step is currently active, shows a progress indicator, handles "Next" and "Back" navigation, and exposes a submit handler on the final step.

### AlreadyAppliedModal.tsx
Shown when the backend returns 409 or when localStorage indicates the candidate already applied. Displays a friendly message instead of a raw error.

### ResumeUploader.tsx
Drag-and-drop + click-to-browse file picker. Validates file type (PDF/DOCX only) client-side before sending. Shows file name and size after selection.

### SuccessModal.tsx
Shown after a successful application submission. Displays the job title, application reference, and next steps. Links to the Applications page.

### Navbar.tsx
Top navigation bar. Shows the Forge AI logo, navigation links, and user avatar. Reads auth state from `useAuthStore()`.

### Sidebar.tsx
Left sidebar with main navigation links (Home, Jobs, Applications, Profile). Highlights the active route using React Router's `useLocation()`. Has `aria-current="page"` on the active link for WCAG 2.1 AA compliance.

### JobFilters.tsx
Filter panel displayed on the left side of `JobsPage`. Contains: search input, work mode select, experience level select, department select, sort dropdown. Calls parent's filter update handler on change.

### Skeleton.tsx
Loading placeholder cards shown while jobs are being fetched. Animated pulse effect to indicate loading state.

---

## features folder

### features/jobs/hooks/useJobs.ts
React Query hook: `useQuery({ queryKey: ['jobs'], queryFn: getJobs, staleTime: 5 * 60 * 1000 })`. The 5-minute stale time means fetched jobs are cached and not re-fetched on every page visit.

### features/jobs/hooks/useJob.ts
React Query hook: `useQuery({ queryKey: ['job', slug], queryFn: () => getJobBySlug(slug) })`. Used by `JobDetailsPage`.

### features/jobs/services/jobs.service.ts
Simple re-export: `export { getJobs, getJobBySlug, filterJobs } from '../../../api/jobs.api'`. Exists so pages import from the feature layer, not directly from the api layer.

### features/jobs/types/job.types.ts
All TypeScript interfaces used throughout the frontend:
- `Job` — the public job object with snake_case fields matching the backend's `PublicJobResponse`
- `JobFilters` — filter parameters
- `Application` — a submitted application record
- `ApplicationForm` — the 10-step form's data shape
- `CandidateProfile` — profile page data
- `ExperienceForm`, `EducationForm`, `SkillForm`, `CertificationForm`, `ProjectForm` — sub-forms within the application

---

## routes folder

### AppRoutes.tsx
All URL-to-component mappings. Routes inside `<CandidateLayout>` get the sidebar + navbar wrapper.

| URL | Component | Notes |
|-----|-----------|-------|
| `/` | `HomePage` | Landing page |
| `/jobs` | `JobsPage` | Browse jobs |
| `/jobs/:slug` | `JobDetailsPage` | Job detail |
| `/jobs/:slug/apply` | `ApplicationPage` | Apply form |
| `/careers` | Redirect → `/jobs` | REQ-JP-09 canonical URL |
| `/careers/:slug` | `JobDetailsPage` | REQ-JP-09 canonical detail |
| `/careers/:slug/apply` | `ApplicationPage` | REQ-JP-09 canonical apply |
| `/applications` | `ApplicationsPage` | My applications |
| `/profile` | `ProfilePage` | Edit profile |
| `/login` | `LoginPage` | Login form (calls port 8080) |
| `/register` | `RegisterPage` | Register form (calls port 8080) |
| `*` | Redirect → `/` | Catch-all |

`ProtectedRoute` is a passthrough component — it renders children without checking auth. In a real production system it would check `isAuthenticated` and redirect to `/login`.

---

## store and localStorage

### authStore.ts (Zustand)
**CRITICAL DEMO NOTE:** `isAuthenticated` is hardcoded to `true`. The mock user is:
```
userId: '11111111-1111-1111-1111-111111111111'
email:  'candidate@forge.ai'
name:   'Guest Candidate'
role:   'CANDIDATE'
token:  'mock-token'
```
This allows the entire apply flow to work in a demo without running a real authentication backend.

### localStorage.ts
Two keys stored in the browser's localStorage:
- `applied_jobs` — array of `Application` objects. Used by `hasApplied(slug)` for client-side duplicate checking and by `ApplicationsPage` as fallback data source.
- `candidate_profile` — `CandidateProfile` object. Used by `ProfilePage` as fallback.

**What should move to backend in production:** Everything. localStorage is a demo convenience. In production, applied jobs and profile data would come from an authenticated backend.

---

# 6. End-to-End User Flows

## Flow 1: Create Job Posting

**Trigger:** HR user calls `POST /api/job-postings` with job details, OR  
**Trigger:** Team 1 demand system calls `POST /api/job-postings/from-demand` (REQ-JP-01)

**Steps:**
1. Request arrives at `JobPostingController.create()` or `createFromDemand()`
2. `@Valid` validates required fields (title must not be blank, salary range must be valid)
3. `JobPostingService.create()` or `createFromDemand()` is called
4. For demand: `normaliseSeniority("L4") → "SENIOR"`, `buildRequirementsFromSkills(["Java","Spring"]) → "Requirements:\n- Java\n- Spring"`
5. `JobPosting` entity is populated and saved via `jobPostingRepository.save()`
6. `@PrePersist` sets `applicationsCount=0`, `status=DRAFT`, timestamps
7. Slug is generated from title + location + year, guaranteed unique
8. `JobPostingResponse` DTO is returned as 201 Created

**Tables updated:** `job_postings` (new row, status=DRAFT)

---

## Flow 2: Publish Job

**Trigger:** `PATCH /api/job-postings/{id}/status` with body `{ "status": "PUBLISHED" }`

**Steps:**
1. `JobPostingController.updateStatus()` calls `JobPostingService.updateStatus(id, "PUBLISHED")`
2. Service sets `job.setStatus("PUBLISHED")`, `job.setPublishedAt(now())`
3. **REQ-JP-04 hook:** `channelService.upsertCareersPortalChannel(job)` is called
4. Channel service creates or updates a `JobPostingChannel` record: `channelName="CAREERS_PORTAL"`, `status="LIVE"`, `channelUrl="http://localhost:5173/jobs/{slug}"`
5. Job is saved, channel is saved

**Tables updated:** `job_postings` (status=PUBLISHED), `job_posting_channels` (new CAREERS_PORTAL LIVE record)

**To publish to LinkedIn:** `POST /api/job-postings/{id}/channels/LINKEDIN/publish`
- Since `app.channels.linkedin.api-configured: false`, status becomes PENDING
- `errorMessage` = "LinkedIn post pending. Copy and share this URL manually: http://localhost:5173/careers/{slug}"

**To publish to Indeed:** `POST /api/job-postings/{id}/channels/INDEED/publish`
- Status becomes LIVE
- `errorMessage` = "Submit this XML feed URL to Indeed Job Distributor: http://localhost:8086/api/public/jobs/feed.xml"

---

## Flow 3: View Careers Portal Jobs

```
Browser navigates to http://localhost:5173/jobs
→ JobsPage renders
→ useJobs() hook fires
→ React Query checks cache (staleTime: 5 min)
→ If cache miss: getJobs() calls GET http://localhost:8086/api/public/jobs
→ PublicJobController.listPublished()
→ JobPostingService.findAllPublished()
→ jobPostingRepository.findByStatus("PUBLISHED")
→ PostgreSQL: SELECT * FROM job_postings WHERE status = 'PUBLISHED'
→ Returns List<JobPosting>
→ Each mapped to PublicJobResponse (snake_case fields)
→ JSON array returned to browser
→ filterJobs() applies any active filters
→ Paginated slice rendered as job cards
```

---

## Flow 4: View Job Details

```
Browser navigates to /careers/senior-java-engineer-bangalore-2024
→ JobDetailsPage renders, reads slug from URL params
→ useJob(slug) hook fires
→ getJobBySlug("senior-java-engineer-bangalore-2024")
→ GET http://localhost:8086/api/public/jobs/senior-java-engineer-bangalore-2024
→ PublicJobController.getBySlug()
→ jobPostingRepository.findBySlug(slug)
→ PostgreSQL: SELECT * FROM job_postings WHERE slug = '...'
→ Maps to PublicJobResponse
→ Returns to browser
→ useDocumentMeta() sets document.title, meta description, JSON-LD script
→ Page renders job title, description, salary, requirements, apply button
→ If ?ref= param present, referral banner is shown
```

---

## Flow 5: Apply for Job

```
Candidate clicks "Apply Now" on JobDetailsPage
→ Navigates to /careers/{slug}/apply with { state: { referralCode } }
→ ApplicationPage mounts
→ hasApplied(slug) checked from localStorage → if true: AlreadyAppliedModal shown
→ If not applied: 10-step ApplicationStepper renders

Candidate completes all 10 steps and clicks Submit:
→ submitApplication(slug, formData, files, referralCode) called
→ Builds JSON payload: { candidateName, candidateEmail, candidatePhone, coverLetter, referralCode }
→ Builds FormData with "application" JSON blob + "resume" file
→ POST http://localhost:8086/api/public/jobs/{slug}/apply

Backend (ApplicationIntakeController → ApplicationIntakeService):
1. Look up job by slug → 404 if not found
2. existsByJobPostingIdAndCandidateEmail → 409 if duplicate
3. fileStorageService.store(resume) → saves file, returns path
4. referralService.resolveSource(code, jobId) → "REFERRAL" or "CAREERS_PORTAL"
5. Save ApplicationIntake to application_intakes table
6. INCREMENT job.applicationsCount
7. emailService.sendApplicationConfirmation() → logs to console in demo
8. If REFERRAL: referralService.markApplied(code, name, email)
9. handoffService.createAndAttempt() → saves HandoffRecord PENDING, attempts Team 2 HTTP call

Backend returns 201 with ApplicationIntakeResponse

Frontend:
→ addApplication(app) saves to localStorage["applied_jobs"]
→ SuccessModal shown with reference ID
```

---

## Flow 6: Duplicate Application

```
Candidate visits /careers/{slug}/apply for a job they already applied to

Check 1 — Frontend (before form):
→ hasApplied(slug) reads localStorage["applied_jobs"]
→ If found: AlreadyAppliedModal shown immediately
→ Candidate never sees the form

Check 2 — Backend (if localStorage was cleared):
→ POST /api/public/jobs/{slug}/apply submitted normally
→ ApplicationIntakeService calls existsByJobPostingIdAndCandidateEmail(jobId, email)
→ SELECT EXISTS(... WHERE job_posting_id=? AND candidate_email=?) → true
→ throw new ConflictException("You have already applied for this position")
→ GlobalExceptionHandler catches ConflictException
→ Returns HTTP 409 Conflict with JSON error body

Frontend:
→ applications.api.ts catches err.response.status === 409
→ throws new Error('already applied')
→ ApplicationPage catches this specific message
→ AlreadyAppliedModal shown (friendly UI instead of error toast)
```

---

## Flow 7: Referral Link

```
Employee wants to refer someone for a job:
→ POST /api/job-postings/{jobId}/referrals
   Body: { "referrerId": 456 }
→ JobPostingReferralController → JobReferralService.generateLink()
→ Creates JobReferral: referralCode = UUID.randomUUID().toString()
→ Saves to job_referrals table with status="PENDING"
→ Returns referralUrl = "http://localhost:5173/careers/{slug}?ref={code}"

Employee shares that URL with the candidate.

Candidate clicks the URL:
→ JobDetailsPage renders at /careers/{slug}?ref={code}
→ useSearchParams() reads "ref" → referralCode = "{code}"
→ Referral banner shown: "You were referred! Apply to get priority consideration."
→ "Apply Now" navigates to /careers/{slug}/apply passing { state: { referralCode } }

Candidate submits application:
→ submitApplication(slug, formData, files, "{code}")
→ applicationPayload.referralCode = "{code}"
→ Backend: referralService.resolveSource("{code}", jobId) returns "REFERRAL"
→ ApplicationIntake saved with source="REFERRAL"
→ referralService.markApplied("{code}", candidateName, candidateEmail)
→ job_referrals updated: referred_candidate_name, referred_candidate_email, status="APPLIED"
```

---

# 7. Requirement Mapping REQ-JP-01 to REQ-JP-11

| Req ID | Simple Meaning | Backend Files | Frontend Files | Database Tables | API Endpoints | Status |
|--------|---------------|--------------|----------------|-----------------|---------------|--------|
| REQ-JP-01 | Auto-create job draft from demand | `JobPostingController`, `JobPostingService`, `DemandSnapshot` | None (admin/API) | `job_postings` | `POST /api/job-postings/from-demand` | **Done** |
| REQ-JP-02 | HR manually create/edit job postings | `JobPostingController`, `JobPostingService` | Admin UI (not built — use Swagger) | `job_postings` | `POST/PUT/PATCH /api/job-postings` | **Done (API)** |
| REQ-JP-03 | Auto-unpublish when job closes/expires | `ChannelExpiryScheduler`, `JobPostingChannelService` | None | `job_postings`, `job_posting_channels` | `PATCH /api/job-postings/{id}/status` | **Done** |
| REQ-JP-04 | Auto-publish to CAREERS_PORTAL on publish | `JobPostingService`, `JobPostingChannelService` | None | `job_posting_channels` | `PATCH /status` triggers it | **Done** |
| REQ-JP-05 | Analytics: views, clicks, apply-starts, apply-completions per posting per channel, exposed to Team 4 | `AnalyticsService`, `JobAnalyticsController`, `ApplicationIntakeService`, `PublicJobController` | `analytics.api.ts`, `JobDetailsPage` (CLICK), `ApplicationPage` (APPLY_START) | `job_posting_analytic_events` | `POST /api/public/jobs/{slug}/events`, `GET /api/analytics/job-postings`, `GET /api/analytics/job-postings/{id}` | **Done** |
| REQ-JP-06 | Public careers portal with search/filter | `PublicJobController`, `JobPostingService` | `JobsPage`, `JobDetailsPage`, `useJobs`, `JobFilters` | `job_postings` | `GET /api/public/jobs` | **Done** |
| REQ-JP-07 | Apply Now flow with resume + duplicate block | `ApplicationIntakeController`, `ApplicationIntakeService`, `FileStorageService` | `ApplicationPage`, `ApplicationStepper`, `AlreadyAppliedModal` | `application_intakes` | `POST /api/public/jobs/{slug}/apply` | **Done** |
| REQ-JP-08 | Forward application to Team 2 with fallback | `ApplicationHandoffService`, `HttpTeam2Client`, `HandoffStatusController` | None (admin API) | `application_handoffs` | `GET/POST /api/admin/handoffs` | **Done (demo fallback)** |
| REQ-JP-09 | SEO: /careers/{slug}, meta tags, JSON-LD | `PublicJobController` | `AppRoutes`, `JobDetailsPage`, `useDocumentMeta` | `job_postings.meta_title`, `meta_description` | `GET /api/public/jobs/{slug}` | **Done** |
| REQ-JP-10 | Employer branding on careers portal | None (static) | `HomePage`, `branding.ts` | None | None | **Done (static)** |
| REQ-JP-11 | Referral links with source tracking | `JobReferralService`, `JobPostingReferralController` | `JobDetailsPage`, `ApplicationPage`, `applications.api.ts` | `job_referrals`, `application_intakes.source` | `POST /api/job-postings/{id}/referrals` | **Done** |

---

# 8. API Documentation

## Job Posting Admin API

### Create job posting
```bash
curl -X POST http://localhost:8086/api/job-postings \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Senior Java Engineer",
    "description": "We are hiring a senior Java engineer...",
    "requirements": "5+ years Java, Spring Boot",
    "responsibilities": "Design and build microservices",
    "benefits": "Health insurance, flexible work",
    "employmentType": "FULL_TIME",
    "workMode": "HYBRID",
    "locationCity": "Bangalore",
    "locationState": "Karnataka",
    "locationCountry": "India",
    "seniority": "SENIOR",
    "department": "Engineering",
    "jobCategory": "Backend Development",
    "salaryMin": 2000000,
    "salaryMax": 3500000,
    "currency": "INR",
    "showSalary": true,
    "metaTitle": "Senior Java Engineer | Forge AI Careers",
    "metaDescription": "Join our backend team building the next generation...",
    "applicationDeadline": "2026-12-31"
  }'
```
**Response:** 201 Created with `JobPostingResponse` (all fields, camelCase)  
**Error:** 400 if title blank or salary invalid

---

### Create from demand (REQ-JP-01)
```bash
curl -X POST http://localhost:8086/api/job-postings/from-demand \
  -H "Content-Type: application/json" \
  -d '{
    "demandId": 101,
    "title": "Java Backend Engineer",
    "level": "L4",
    "skills": ["Java", "Spring Boot", "PostgreSQL"],
    "location": "Bangalore",
    "locationCity": "Bangalore",
    "locationState": "Karnataka",
    "locationCountry": "India",
    "department": "Engineering",
    "targetDate": "2026-12-31"
  }'
```
**Response:** 201 Created with auto-populated draft  
**Error:** 400 if title blank or targetDate is in the past

---

### List all job postings
```bash
curl http://localhost:8086/api/job-postings
```
**Response:** `PagedResponse<JobPostingResponse>`

---

### Get job posting by ID
```bash
curl http://localhost:8086/api/job-postings/1
```
**Response:** `JobPostingResponse` or 404

---

### Update job status
```bash
curl -X PATCH http://localhost:8086/api/job-postings/1/status \
  -H "Content-Type: application/json" \
  -d '{ "status": "PUBLISHED" }'
```
**Side effects:**
- `PUBLISHED` → auto-creates CAREERS_PORTAL channel as LIVE (REQ-JP-04)
- `CLOSED` → unpublishes all LIVE channels (REQ-JP-03)

---

### Delete job posting
```bash
curl -X DELETE http://localhost:8086/api/job-postings/1
```
**Response:** 204 No Content

---

## Public Jobs API

### List all published jobs
```bash
curl http://localhost:8086/api/public/jobs
```
**Response:** `Job[]` (snake_case fields — matches frontend `Job` interface)
```json
[
  {
    "id": 1,
    "title": "Senior Java Engineer",
    "slug": "senior-java-engineer-bangalore-2024",
    "posting_status": "PUBLISHED",
    "experience_level": "SENIOR",
    "work_mode": "HYBRID",
    "location_city": "Bangalore",
    "salary_min": 2000000,
    "salary_max": 3500000,
    "currency": "INR",
    "show_salary": true,
    "expires_at": "2026-12-31",
    ...
  }
]
```

---

### Get job by slug
```bash
curl http://localhost:8086/api/public/jobs/senior-java-engineer-bangalore-2024
```
**Response:** Single `Job` (snake_case) or 404

---

### Indeed XML feed
```bash
curl http://localhost:8086/api/public/jobs/feed.xml
```
**Response:** `Content-Type: application/xml`
```xml
<?xml version="1.0" encoding="UTF-8"?>
<source>
  <publisherurl>http://localhost:5173</publisherurl>
  <job>
    <title><![CDATA[Senior Java Engineer]]></title>
    <url><![CDATA[http://localhost:5173/careers/senior-java-engineer-bangalore-2024]]></url>
    <referencenumber>1</referencenumber>
    <location><![CDATA[Bangalore, Karnataka, India]]></location>
    <jobtype>fulltime</jobtype>
    ...
  </job>
</source>
```

---

## Apply API

### Submit application (multipart)
```bash
curl -X POST http://localhost:8086/api/public/jobs/senior-java-engineer-bangalore-2024/apply \
  -F 'application={"candidateName":"Priya Sharma","candidateEmail":"priya@example.com","candidatePhone":"+91-9876543210","coverLetter":"I am excited to apply...","referralCode":""};type=application/json' \
  -F 'resume=@/path/to/resume.pdf'
```
**Response:** 201 Created
```json
{
  "id": 42,
  "jobPostingId": 1,
  "candidateName": "Priya Sharma",
  "candidateEmail": "priya@example.com",
  "status": "SUBMITTED",
  "source": "CAREERS_PORTAL"
}
```
**Errors:**
- 400 — blank name, invalid email, unsupported file type
- 404 — job not found
- 409 — already applied with this email

---

## Channel API

### Publish to a channel
```bash
curl -X POST http://localhost:8086/api/job-postings/1/channels/LINKEDIN/publish
```
**Response:** `JobPostingChannelResponse`
```json
{
  "channelName": "LINKEDIN",
  "status": "PENDING",
  "channelUrl": null,
  "errorMessage": "LinkedIn post pending. Copy and share this URL manually: http://localhost:5173/careers/senior-java-engineer-bangalore-2024"
}
```

### Unpublish from a channel
```bash
curl -X POST http://localhost:8086/api/job-postings/1/channels/LINKEDIN/unpublish
```

### List channel statuses
```bash
curl http://localhost:8086/api/job-postings/1/channels
```
**Response:** `JobPostingChannelResponse[]`

---

## Referral API

### Generate referral link
```bash
curl -X POST http://localhost:8086/api/job-postings/1/referrals \
  -H "Content-Type: application/json" \
  -d '{ "referrerId": 456 }'
```
**Response:** `JobReferralResponse`
```json
{
  "id": 7,
  "jobPostingId": 1,
  "referrerId": 456,
  "referralCode": "a1b2c3d4-e5f6-...",
  "status": "PENDING",
  "referralUrl": "http://localhost:5173/careers/senior-java-engineer-bangalore-2024?ref=a1b2c3d4-e5f6-..."
}
```

---

## Handoff Admin API

### List all handoff records
```bash
curl http://localhost:8086/api/admin/handoffs
curl http://localhost:8086/api/admin/handoffs?status=PENDING
```

### Get one handoff
```bash
curl http://localhost:8086/api/admin/handoffs/1
```

### Retry a failed/pending handoff
```bash
curl -X POST http://localhost:8086/api/admin/handoffs/1/retry
```

---

# 9. Database Documentation

## Database name
`forge_market_presence`

## Connection details (local)
```
Host:     localhost
Port:     5432
Database: forge_market_presence
Username: sanjay
Password: 1234
```

## Schema management
Hibernate `ddl-auto: update` is used. This means Hibernate automatically creates or alters tables to match the Java entity classes on every startup. There are no SQL migration files (no Flyway or Liquibase). This is fine for development but in production you would use Flyway with versioned migrations.

---

## Table: job_postings

The central table. Every job posting lives here.

```sql
SELECT id, title, slug, status, seniority, department,
       work_mode, location_city, salary_min, salary_max,
       applications_count, published_at, application_deadline
FROM job_postings
ORDER BY created_at DESC;
```

**Status values:** `DRAFT`, `PUBLISHED`, `CLOSED`

**Key constraints:**
- `slug` is UNIQUE — no two jobs can have the same URL slug
- `application_deadline` drives the hourly expiry scheduler

---

## Table: application_intakes

Every job application is stored here.

```sql
-- All applications for a specific job
SELECT id, candidate_name, candidate_email, source, status, applied_at
FROM application_intakes
WHERE job_posting_id = 1
ORDER BY applied_at DESC;

-- Find a duplicate application
SELECT EXISTS(
  SELECT 1 FROM application_intakes
  WHERE job_posting_id = 1
  AND candidate_email = 'priya@example.com'
);
```

**Status values:** `SUBMITTED` (default)  
**Source values:** `CAREERS_PORTAL`, `REFERRAL`

**Key constraint:** UNIQUE on `(job_posting_id, candidate_email)` — one email per job.

---

## Table: job_posting_channels

Tracks where each job has been published.

```sql
-- See all channel statuses for all jobs
SELECT jp.title, jpc.channel_name, jpc.status, jpc.channel_url, jpc.error_message
FROM job_posting_channels jpc
JOIN job_postings jp ON jpc.job_posting_id = jp.id
ORDER BY jp.title, jpc.channel_name;

-- Check if a job is live on careers portal
SELECT * FROM job_posting_channels
WHERE job_posting_id = 1 AND channel_name = 'CAREERS_PORTAL';
```

**Channel names:** `CAREERS_PORTAL`, `LINKEDIN`, `INDEED`  
**Status values:** `DRAFT`, `PENDING`, `LIVE`, `FAILED`, `UNPUBLISHED`

**Key constraint:** UNIQUE on `(job_posting_id, channel_name)` — one record per channel per job.

---

## Table: application_handoffs

The outbox table for Team 2 integration.

```sql
-- Check handoff status for an application
SELECT id, candidate_email, job_slug, status, error_message, attempted_at
FROM application_handoffs
WHERE status = 'PENDING';

-- Check if handoff reached Team 2
SELECT id, status, team2_response_id, attempted_at
FROM application_handoffs
WHERE application_intake_id = 42;
```

**Status values:** `PENDING`, `SENT`, `FAILED`

---

## Table: job_referrals

Tracks referral links generated by employees.

```sql
-- List all referrals for a job
SELECT referral_code, referrer_id, referred_candidate_name,
       referred_candidate_email, status
FROM job_referrals
WHERE job_posting_id = 1;

-- Look up a referral by code
SELECT * FROM job_referrals
WHERE referral_code = 'a1b2c3d4-e5f6-...';
```

**Status values:** `PENDING`, `APPLIED`, `HIRED`

---

## Example SQL queries for demonstration

```sql
-- List all published jobs
SELECT title, slug, seniority, work_mode, location_city, applications_count
FROM job_postings
WHERE status = 'PUBLISHED'
ORDER BY published_at DESC;

-- Count applications per job
SELECT jp.title, COUNT(ai.id) as application_count
FROM job_postings jp
LEFT JOIN application_intakes ai ON ai.job_posting_id = jp.id
GROUP BY jp.id, jp.title
ORDER BY application_count DESC;

-- Jobs expiring soon (for scheduler testing)
SELECT id, title, status, application_deadline
FROM job_postings
WHERE status = 'PUBLISHED'
  AND application_deadline < CURRENT_DATE;

-- All PENDING handoffs (Team 2 offline)
SELECT id, candidate_email, job_title, status, created_at
FROM application_handoffs
WHERE status = 'PENDING'
ORDER BY created_at;

-- Applications by source (CAREERS_PORTAL vs REFERRAL)
SELECT source, COUNT(*) as count
FROM application_intakes
GROUP BY source;
```

---

# 10. Testing Explanation

## Overview
**78 tests total, 0 failures.**  
All backend tests use MockMvc (controller tests) or Mockito (service tests) — no real Spring context or real database is needed for most tests.

## Test files and counts

| Test File | Tests | What it verifies |
|-----------|-------|-----------------|
| `JobPostingControllerTest` | 13 | Create job (201), negative salary (400), blank title (400), fromDemand happy path, fromDemand past date (400) |
| `ApplicationIntakeControllerTest` | 8 | Apply with PDF (201), DOCX (201), no resume (201), duplicate (409), wrong file type (400), blank name (400), bad email (400), job not found (404) |
| `HandoffStatusControllerTest` | 6 | List all handoffs, filter by status, get one, retry |
| `PublicJobControllerTest` | 3 | List published jobs, get by slug, slug not found (404) |
| `IndeedFeedControllerTest` | 3 | Feed returns XML, contains job title, contains careers URL |
| `MarketPresenceServiceApplicationTests` | 1 | Spring context loads without error |
| `JobPostingServiceTest` | 20 | Create job, fromDemand skill normalization, seniority mapping, salary validation, status update hooks |
| `ApplicationHandoffServiceTest` | 5 | No URL → PENDING, URL + success → SENT, URL + exception → FAILED, payload has source, never throws |
| `JobPostingChannelServiceTest` | 10 | CAREERS_PORTAL always LIVE, LINKEDIN pending when not configured, INDEED always LIVE, unpublish sets UNPUBLISHED |
| `JobReferralServiceTest` | 9 | Generate link creates record, resolveSource returns REFERRAL when code matches, returns CAREERS_PORTAL when code is null, markApplied updates record |

## How to run tests
```bash
cd backend
mvn test
```
Output: `Tests run: 78, Failures: 0, Errors: 0, Skipped: 0`

## Frontend type checking
```bash
cd frontend/frontend
npm run build
```
This runs the TypeScript compiler via Vite. If there are any type errors, the build fails. This acts as a lightweight test for frontend correctness.

## Manual testing per requirement

| Requirement | How to test manually |
|-------------|---------------------|
| REQ-JP-01 | `POST /api/job-postings/from-demand` via Swagger → check job appears in DB with status=DRAFT |
| REQ-JP-02 | `POST /api/job-postings` via Swagger → `PUT /api/job-postings/{id}` to edit |
| REQ-JP-03 | Set a job's `applicationDeadline` to yesterday → wait for scheduler or call `PATCH /status → CLOSED` → check channels become UNPUBLISHED |
| REQ-JP-04 | `PATCH /api/job-postings/{id}/status` → `{ status: "PUBLISHED" }` → check `job_posting_channels` for CAREERS_PORTAL LIVE |
| REQ-JP-05 | Open a job detail page → check `job_posting_analytic_events` for a VIEW row. Click Apply Now → check for CLICK row. Open the apply form → check for APPLY_START row. Submit → check for APPLY_COMPLETE row. Call `GET /api/analytics/job-postings/{id}` to see per-channel totals. |
| REQ-JP-06 | Open `http://localhost:5173/jobs` → see job cards, use filters, search |
| REQ-JP-07 | Click Apply on any job → complete 10-step form → check `application_intakes` in DB |
| REQ-JP-08 | Submit application → check `application_handoffs` table → status=PENDING (URL empty in config) |
| REQ-JP-09 | Open `/careers/{slug}` → inspect browser `<title>` and `<meta name="description">` → view page source for JSON-LD |
| REQ-JP-10 | Open `/` (home page) → scroll through About Us, culture values, benefits, employee stories |
| REQ-JP-11 | `POST /api/job-postings/{id}/referrals` → get `referralUrl` → open that URL → apply → check `application_intakes.source = 'REFERRAL'` |

## Tests that should be added later
- `ApplicationIntakeServiceTest` (service-level unit test — currently only controller test exists)
- `ChannelExpirySchedulerTest` (verify scheduler triggers unpublish)
- `FileStorageServiceTest` (verify MIME type rejection)
- Frontend component tests (Vitest + React Testing Library)
- Integration test with real PostgreSQL (e.g., using Testcontainers)

---

# 11. How to Run the Project

## Prerequisites

| Tool | Required version | Check with |
|------|----------------|-----------|
| Java JDK | 21 | `java -version` |
| Maven | 3.9+ | `mvn -version` |
| PostgreSQL | 14+ | `psql --version` |
| Node.js | 18+ | `node -version` |
| npm | 9+ | `npm -version` |

## Step 1: Start PostgreSQL

```bash
# macOS (Homebrew)
brew services start postgresql@14

# Ubuntu/Debian
sudo service postgresql start

# Verify it's running
psql -U postgres -c "SELECT version();"
```

## Step 2: Create the database

```bash
psql -U postgres
CREATE DATABASE forge_market_presence;
CREATE USER sanjay WITH PASSWORD '1234';
GRANT ALL PRIVILEGES ON DATABASE forge_market_presence TO sanjay;
\q
```

## Step 3: Start the backend

```bash
cd /Users/sanjakumar/Desktop/javapractise/market-presence-fullstack/backend
mvn spring-boot:run
```

**What you'll see:**
- Hibernate creates all tables automatically
- `DataSeeder` runs and creates 8 demo job postings
- "Started MarketPresenceServiceApplication" log line
- Spring is now running at **http://localhost:8086**

**Verify:**
```bash
curl http://localhost:8086/api/public/jobs
# Should return a JSON array of jobs
```

**Swagger UI** (if springdoc-openapi is on the classpath):
```
http://localhost:8086/swagger-ui.html
```

**Spring Actuator health** (if actuator is on the classpath):
```
http://localhost:8086/actuator/health
```

## Step 4: Start the frontend

```bash
cd /Users/sanjakumar/Desktop/javapractise/market-presence-fullstack/frontend/frontend
npm install
npm run dev
```

**Verify:**
- Terminal shows `VITE v5.x ready in ...ms`
- Open **http://localhost:5173** in browser
- Home page loads with Forge AI branding
- Navigate to `/jobs` — job cards appear

## Important config files

**Backend — `backend/src/main/resources/application.yml`:**
- Change `username`/`password` if your PostgreSQL credentials differ
- Set `app.team2.api-base-url` to Team 2's URL if they are online
- Set `app.channels.linkedin.api-configured: true` if LinkedIn API is ready

**Frontend — `frontend/frontend/.env.local`:**
```
VITE_API_URL=http://localhost:8086/api
```
This points the frontend to the Team 3 backend. If you change the backend port, update this.

---

# 12. Demo Script

Use this script to demonstrate the project to Grid Dynamics reviewers or in an interview.

### Step 1 — Start backend
```bash
cd backend && mvn spring-boot:run
```
Say: "This is our Spring Boot 4 backend running on Java 21. It starts with auto-seeded demo data."

### Step 2 — Start frontend
```bash
cd frontend/frontend && npm run dev
```
Say: "This is our React 19 + TypeScript + Vite frontend."

### Step 3 — Show backend health
```
Open: http://localhost:8086/api/public/jobs
```
Say: "The public API requires no authentication — it serves published jobs to any visitor."

### Step 4 — Show careers portal
```
Open: http://localhost:5173/jobs
```
Say: "This is our public careers portal. Candidates browse available positions without needing an account."

### Step 5 — Show job filtering
Type "Java" in the search box. Select "REMOTE" in work mode. Select "SENIOR" in experience level.  
Say: "Candidates can search and filter by title, skills, work mode, experience level, department, and city. Filtering is instant and client-side — no extra API call per filter."

### Step 6 — Show employer branding (REQ-JP-10)
```
Navigate to: http://localhost:5173/
Scroll through the page
```
Say: "The home page showcases our employer brand — About Us, culture values, benefits, and real employee stories. In production this would be served from an admin CMS. Today it's a typed config file we can swap without changing any JSX."

### Step 7 — Open a job detail (REQ-JP-09)
Click any job card. Point to the browser URL bar.  
Say: "Notice the URL is /careers/{slug}. This is our SEO-friendly canonical URL structure per REQ-JP-09."

Open browser dev tools → Elements → `<head>`.  
Say: "The page title, meta description, and JSON-LD JobPosting schema are all dynamically set per job. Search engines can index this correctly."

### Step 8 — Show referral banner (REQ-JP-11)
```
In the browser address bar, add: ?ref=some-code
Press Enter
```
Say: "When a candidate arrives via a referral link, they see this banner. The referral code is tracked through the entire apply flow. If valid, the application is tagged source=REFERRAL."

### Step 9 — Apply for a job (REQ-JP-07)
Click "Apply Now". Walk through 2–3 steps of the form.  
Say: "This is our 10-step application form built with Zod + react-hook-form. It collects experience, education, skills, certifications, projects, and screening questions. Validation happens in real time."

Upload a PDF resume on step 1.  
Say: "Resume upload validates MIME type and extension. Only PDF and DOCX are accepted."

### Step 10 — Show successful application
Complete the form and submit.  
Say: "On success, we show this confirmation modal. The application is saved to our database, the job's application count is incremented, an email confirmation is triggered (demo: logged to console), and the application is forwarded to Team 2's pipeline."

### Step 11 — Show duplicate application handling (REQ-JP-07)
Try to apply for the same job again.  
Say: "Our frontend checks localStorage first — the AlreadyApplied modal appears immediately. Even if localStorage is cleared, the backend enforces a unique constraint on (job_id, email) and returns HTTP 409, which we map to this same friendly modal."

### Step 12 — Show channel management (REQ-JP-03, REQ-JP-04)
```bash
curl http://localhost:8086/api/job-postings/1/channels
```
Say: "Each job has channel records. When published, CAREERS_PORTAL automatically goes LIVE. LINKEDIN goes PENDING with a copy-URL for manual posting — we don't have LinkedIn API credentials but the system is designed to swap the flag. INDEED is LIVE and the recruiter submits our XML feed URL to Indeed Job Distributor."

### Step 13 — Show Team 2 handoff fallback (REQ-JP-08)
```bash
curl http://localhost:8086/api/admin/handoffs
```
Say: "Every application creates a HandoffRecord. Since Team 2's URL is not configured locally, the status is PENDING. When Team 2 comes online, an admin calls this retry endpoint. The design ensures the apply flow never fails because of Team 2's availability — classic outbox pattern."

### Step 14 — Show accessibility
Open browser accessibility inspector. Navigate to `/jobs`.  
Say: "We meet WCAG 2.1 AA. Skip navigation link, aria-label on icon-only buttons, aria-current on active nav link, label htmlFor on all form inputs, aria-hidden on decorative icons."

### Step 15 — Show database
```bash
psql -U sanjay -d forge_market_presence -c "SELECT title, status, applications_count FROM job_postings;"
psql -U sanjay -d forge_market_presence -c "SELECT candidate_name, source, status FROM application_intakes;"
psql -U sanjay -d forge_market_presence -c "SELECT channel_name, status FROM job_posting_channels;"
```
Say: "All data is persisted in PostgreSQL. The job application just submitted appears in application_intakes with source CAREERS_PORTAL. If a referral code was used, source would be REFERRAL."

---

# 13. Remaining Gaps / Future Improvements

### Real LinkedIn API integration
**Current:** Status set to PENDING, recruiter gets a copy-URL message.  
**What's needed:** LinkedIn Jobs API credentials, OAuth 2.0 flow, replace the `api-configured: false` flag with real API call in `JobPostingChannelService.publishLinkedIn()`.

### Real Indeed XML submission
**Current:** XML feed is generated at `/api/public/jobs/feed.xml`. Recruiter submits URL to Indeed Job Distributor manually.  
**What's needed:** Register with Indeed's API, submit the feed URL programmatically, handle Indeed's callback for indexing confirmation.

### Real Team 2 service integration
**Current:** `app.team2.api-base-url` is empty. All handoffs stay PENDING.  
**What's needed:** Team 2's actual service URL, verify the `Team2ApplicationPayload` matches their API contract, enable retry automation (e.g., a scheduled retry of all PENDING records).

### Kafka event integration (REQ-JP-01)
**Current:** Team 1's demand events are simulated via `POST /api/job-postings/from-demand`.  
**What's needed:** A Kafka consumer that listens to Team 1's `demand.opened` topic, deserializes the event into `DemandSnapshot`, and calls the same service logic.

### Real email/SMS
**Current:** `LoggingEmailService` logs confirmation emails to the console.  
**What's needed:** Replace `LoggingEmailService` with an SMTP implementation (e.g., Spring Mail + SendGrid/SES). No other code changes needed — the interface is already in place.

### Production authentication
**Current:** `authStore.ts` hardcodes `isAuthenticated: true`. `ProtectedRoute` is a passthrough.  
**What's needed:** Real JWT auth backend (already partially built in `frontend/backend/` at port 8080), real `ProtectedRoute` that redirects to `/login`, real login/register flow.

### Admin UI for job management
**Current:** HR manages jobs via Swagger or raw HTTP calls.  
**What's needed:** An admin-facing React pages for creating/editing/publishing jobs with a rich text editor for description fields.

### Docker / container deployment
**Current:** Runs on developer's local machine.  
**What's needed:** `Dockerfile` for backend, `Dockerfile` for frontend (Nginx serving built assets), `docker-compose.yml` to run backend + frontend + PostgreSQL together.

### CI/CD pipeline
**Current:** No automated build/test pipeline.  
**What's needed:** GitHub Actions workflow: on PR → run `mvn test` + `npm run build`, on merge to main → build Docker images + push to registry.

### More backend tests
Missing tests:
- `ApplicationIntakeServiceTest` — unit test for the full apply flow
- `FileStorageServiceTest` — test MIME validation rejection
- `ChannelExpirySchedulerTest` — verify scheduler calls unpublish
- Integration tests with real DB via Testcontainers

### Production file storage
**Current:** Resumes stored at `uploads/resumes/` on the local filesystem.  
**What's needed:** Cloud object storage (AWS S3 or GCP GCS) for resumes. The `FileStorageService` interface is the right abstraction point — add an `S3FileStorageService` implementation.

---

# 14. Beginner Glossary

**Controller**  
The class that receives HTTP requests (like a receptionist). It reads what the client sent, asks the Service to do the work, then sends back a response. In Spring Boot, marked with `@RestController`.

**Service**  
The class that contains business logic (like a manager). It decides what is valid, what rules to apply, and calls the Repository to read/write data. Marked with `@Service`.

**Repository**  
The class that talks to the database (like a filing clerk). It runs SQL queries. With Spring Data JPA, you just define an interface extending `JpaRepository` — Spring writes the SQL for you.

**Entity**  
A Java class that represents a database table (like a row template). Each field is a column. Marked with `@Entity`. JPA (Hibernate) maps between the Java object and the database row automatically.

**DTO (Data Transfer Object)**  
A plain class used to carry data into or out of an API. Request DTOs hold what the client sends. Response DTOs hold what the server returns. Different from Entities — DTOs don't map to database tables.

**API (Application Programming Interface)**  
A set of rules for how two pieces of software communicate. In this project, the frontend and backend communicate via a REST API over HTTP.

**REST**  
A style of API where each URL represents a resource (like `/api/job-postings/1`) and HTTP verbs (GET, POST, PUT, PATCH, DELETE) describe the action.

**PostgreSQL**  
An open-source relational database. Think of it as Excel — data is stored in tables with rows and columns. SQL is the language used to query it.

**JPA (Java Persistence API)**  
A Java standard for working with databases using objects instead of raw SQL. Hibernate is the most popular implementation. In this project, you write Java entity classes and JPA handles the SQL.

**Axios**  
A JavaScript library for making HTTP requests from the browser. The frontend uses it to call the backend API.

**React Query (TanStack Query)**  
A library for managing "server state" in React — data that comes from an API. It automatically caches results, handles loading/error states, and knows when to refetch stale data.

**Route**  
A mapping from a URL path to a React page component. For example, `/jobs` → `JobsPage`, `/careers/:slug` → `JobDetailsPage`.

**Slug**  
A URL-friendly identifier derived from a title. For example, the title "Senior Java Engineer — Bangalore" becomes the slug `senior-java-engineer-bangalore-2024`. Slugs are readable and SEO-friendly.

**CORS (Cross-Origin Resource Sharing)**  
A browser security rule that blocks requests to a different domain/port unless the server explicitly allows it. Our backend allows requests from `localhost:5173` (the frontend).

**HTTP Status Codes**
- 200 OK — request succeeded
- 201 Created — new resource created
- 400 Bad Request — client sent invalid data
- 404 Not Found — resource doesn't exist
- 409 Conflict — request conflicts with existing data (e.g., duplicate application)
- 500 Internal Server Error — something went wrong on the server

**Seed data**  
Pre-loaded demo data that is inserted into the database when the application starts. Used in development and demos so the UI has something real to show.

**Fallback**  
An alternative behavior used when the real system is unavailable. For example, when Team 2's URL is not configured, the handoff record stays PENDING (the fallback) instead of failing the entire apply request.

**Microservice**  
A style of architecture where each feature is a separate, independently deployable service. The opposite is a **Monolith**, where everything is one application. This project is currently a monolith but designed to be split.

**Multipart form data**  
An HTTP content type used when uploading files. A single request contains multiple "parts" — in this project, one part is the JSON application data and another part is the resume file.

**Outbox pattern**  
A reliability pattern where you save an intent-to-send record (the "outbox") to your own database first, then attempt the external call. If the external call fails, the record stays in the database for retry. Prevents data loss when external services are down.

**JSON-LD**  
"JSON Linked Data" — a format for embedding structured data in web pages so search engines understand the content. This project embeds a `JobPosting` schema so Google can show rich results for jobs.

**Zustand**  
A lightweight state management library for React. Used here to store the authenticated user across all pages without passing it as props.

---

# 15. Code Walkthrough File-by-File

## backend/src/main/java/.../MarketPresenceServiceApplication.java

The entry point of the entire backend. Contains `main()` which launches the Spring Boot application. The `@EnableScheduling` annotation here is what activates the `@Scheduled` annotation in `ChannelExpiryScheduler` — without it, the hourly expiry check would never run.

**Calls:** Nothing directly. Spring Boot starts and wires everything automatically.  
**Required by:** REQ-JP-03 (scheduling), everything else.

---

## .../config/SecurityConfig.java

Configures Spring Security. The key decision here was to `permitAll()` everything — no authentication is enforced by the backend. CSRF is disabled (safe for stateless REST APIs). CORS is configured to allow the frontend origin.

**Why no auth?** Team 3's scope is the careers portal, not auth infrastructure. Auth lives in `frontend/backend/` (port 8080). Adding auth to port 8086 was out of scope and would break the demo-ready design.

---

## .../config/DataSeeder.java

Runs at startup. Creates 8 varied demo job postings with different titles, departments, seniorities, and work modes so the careers portal looks populated. Uses `existsBySlug()` check to avoid re-inserting on every restart. Also runs patch methods to normalize any legacy data left from earlier development.

**Why it matters:** Without seed data, a reviewer opening the frontend would see an empty jobs page. The seeder ensures a believable demo state immediately.

---

## .../entity/JobPosting.java

The most important entity. Maps to `job_postings`. The `@PrePersist` method sets `applicationsCount=0`, `currency="INR"`, `showSalary=false`, and both timestamps before any new record is saved — ensuring defaults are always consistent.

The `slug` field is unique. If two jobs with the same title were created, the service adds a suffix to guarantee uniqueness.

---

## .../entity/ApplicationIntake.java

Maps to `application_intakes`. The unique constraint on `(job_posting_id, candidate_email)` is the database-level duplicate guard — even if the application code had a bug, the database would reject a second application from the same email for the same job.

The `source` field defaults to `"CAREERS_PORTAL"` in Java (not in the database `@Column` — the column is nullable to avoid DDL errors on existing rows with null values, but new rows always get the Java default).

---

## .../entity/JobPostingChannel.java

Maps to `job_posting_channels`. The `publishedAt` field maps to a database column named `posted_at` using `@Column(name="posted_at")` — this was an early naming decision that was kept for backward compatibility with seed data.

The `errorMessage` field is overloaded — it stores actual error messages for FAILED status AND informational messages (copy-URL for LINKEDIN, feed URL for INDEED) for PENDING/LIVE status.

---

## .../service/JobPostingService.java

The core service for managing job postings. The most important behavioral hook is in `updateStatus()` — two lines that connect the job status lifecycle to the channel system (REQ-JP-04 and REQ-JP-03). Any time status changes to PUBLISHED, the careers portal channel is automatically set live. Any time status changes to CLOSED, all live channels are automatically unpublished.

The `normaliseSeniority()` helper maps Team 1's level codes (L3, L4, L5, L6, L7) to human-readable values (JUNIOR, MID, SENIOR, LEAD, PRINCIPAL).

---

## .../service/ApplicationIntakeService.java

Orchestrates the entire "apply" transaction. The key design decision is that this service coordinates multiple side effects (file storage, email, referral tracking, handoff) but treats each as best-effort except the actual database save. If the email fails or the handoff fails, the application is still recorded — this prevents a broken email service from blocking a candidate's application.

---

## .../service/ApplicationHandoffService.java

Implements the outbox pattern. The record is always saved as PENDING first. The HTTP call to Team 2 is attempted second. The exception handler around the HTTP call ensures that `createAndAttempt()` never propagates an exception upward — this is the "never blocks" guarantee that makes the apply flow resilient to Team 2 outages.

---

## .../service/JobPostingChannelService.java

The "routing" service for channel publishing. Contains all the fallback logic:
- CAREERS_PORTAL: always works, always LIVE
- LINKEDIN: flag-controlled, always PENDING in local dev
- INDEED: always LIVE, gives feed URL

The `unpublishAllLiveChannels()` method defensively matches not just "LIVE" but also "ACTIVE" and "PUBLISHED" — legacy values that may exist in older data.

---

## .../service/ChannelExpiryScheduler.java

A single `@Scheduled` method that runs every hour. It finds all PUBLISHED jobs whose `applicationDeadline` is in the past, marks them CLOSED, and unpublishes their channels. The scheduler is what makes REQ-JP-03 (auto-unpublish on deadline) work without any manual intervention.

The cron expression `"0 0 * * * *"` means: at second 0 of minute 0 of every hour.

---

## .../service/FileStorageService.java

Validates that uploaded files are PDF or DOCX by checking both the MIME type (from the `Content-Type` header) and the file extension. The dual check prevents tricks like renaming an executable file `.pdf`. Files are stored under `uploads/resumes/{slug}/` relative to the working directory.

**Edge case:** If the same filename is uploaded twice for the same job, the new file overwrites the old one. This is acceptable for the demo but production would need unique filenames (e.g., UUID prefix).

---

## .../controller/ApplicationIntakeController.java

Uses `consumes = MediaType.MULTIPART_FORM_DATA_VALUE` and `@RequestPart` annotations instead of `@RequestBody`. The `application` part is bound to `ApplicationIntakeRequest` (Spring uses Jackson to deserialize the JSON blob). The `resume` part is bound to `MultipartFile`. The `required=false` on resume means the endpoint accepts applications without a file.

---

## .../controller/IndeedFeedController.java

Produces `application/xml` (not the usual `application/json`). Manually builds the XML string by iterating published jobs and formatting each as an `<job>` element. This is a simplified XML generation — in production you would use JAXB or a proper XML serialization library.

---

## .../exception/GlobalExceptionHandler.java

Annotated `@RestControllerAdvice` — Spring automatically applies this to all controllers. The handler for `MethodArgumentNotValidException` (triggered by `@Valid`) extracts all field-level validation errors into a list of strings, giving the frontend precise information about which fields failed and why.

---

## frontend/frontend/src/api/axios.ts

The shared Axios instance. The request interceptor adds the JWT token. The response interceptor handles 401 by firing a DOM event. `App.tsx` listens for `auth:logout` and calls Zustand's `logout()`. This decouples the HTTP layer from the React component tree — Axios doesn't need to import Zustand.

---

## frontend/frontend/src/api/applications.api.ts

The most complex frontend file. The `submitApplication()` function converts the rich 10-step form data into what the backend actually needs: a `candidateName` (joined first+middle+last), a `coverLetter` (assembled from all the extra form fields the backend has no dedicated columns for), and the binary resume file.

The `Content-Type: undefined` trick on line 173 is critical — it forces the browser to set the multipart boundary header automatically. If you set `Content-Type: multipart/form-data` manually, the boundary parameter is missing and the backend cannot parse the parts.

---

## frontend/frontend/src/store/authStore.ts

The Zustand store that makes the demo work. `isAuthenticated: true` and the mock user are hardcoded. The `loadFromStorage()` function can override this if a real token is found in localStorage (from a real login), but in demo mode it never finds one so the mock user persists.

**Important:** In production, remove the hardcoded `user` and `isAuthenticated: true` initial state. Let `loadFromStorage()` be the only way to restore a session.

---

## frontend/frontend/src/store/localStorage.ts

The `hasApplied(slug)` function is the frontend's first line of defense against duplicate applications. It is checked before the form opens, giving instant feedback. The `addApplication()` function is called after a successful backend response, so localStorage and the database stay in sync.

---

## frontend/frontend/src/lib/useDocumentMeta.ts

Three `useEffect` hooks managing `document.title`, `<meta name="description">`, and `<script type="application/ld+json">`. Each cleanup function restores the previous state when the component unmounts. The JSON-LD effect uses `JSON.stringify(jsonLd)` as its dependency — without this, the effect would only run once (on mount) and never update if the job data changed.

This hook replaces `react-helmet` without adding a dependency. React 19 natively hoists `<title>` and `<meta>` from JSX, but JSON-LD `<script>` tags still need imperative DOM access, which is what this hook handles.

---

## frontend/frontend/src/data/branding.ts

Static typed configuration for all employer branding content. The comment at the top is intentional: "For a production deployment, replace this static export with an API call." The component interfaces (`CultureValue`, `Benefit`, `EmployeeStory`, `BrandingConfig`) are already defined — a future API response just needs to match these shapes.

---

## frontend/frontend/src/pages/JobDetailsPage.tsx

Reads `?ref=` from URL search params. If present, shows a referral banner and remembers the code. When "Apply Now" is clicked, passes the code via React Router's `location.state` to `ApplicationPage`. The SEO setup (`useDocumentMeta`) and the referral banner are the two features unique to this page beyond displaying job details.

---

## frontend/frontend/src/pages/ApplicationPage.tsx

Reads `referralCode` from `location.state` (passed by `JobDetailsPage`). Checks `hasApplied(slug)` immediately on mount. Renders `ApplicationStepper` for the 10-step form. On submit, calls `submitApplication()`, handles the 'already applied' error specifically to show `AlreadyAppliedModal`, and handles all other errors as a generic error message.

---

## frontend/frontend/src/routes/AppRoutes.tsx

Two notable points:
1. `/careers` redirects to `/jobs` — the canonical URL structure is `/careers` but the legacy `/jobs` routes are kept for backward compatibility
2. `ProtectedRoute` is a no-op component that renders `{children}` directly. In production it would check `useAuthStore().isAuthenticated` and redirect to `/login` if false.

---

# 16. Final Summary

## What is completed

All 11 requirements (REQ-JP-01 through REQ-JP-11) are implemented in some form. The backend has 78 passing tests. The frontend builds without TypeScript errors. The full apply flow — from browsing jobs to submitting an application with a resume — works end to end.

## What is demo-ready

| Feature | Demo-ready? |
|---------|------------|
| Browse/search/filter published jobs | Yes |
| View job details with SEO meta and JSON-LD | Yes |
| Apply with resume + 10-step form | Yes |
| Duplicate application blocking (frontend + backend) | Yes |
| CAREERS_PORTAL channel auto-publish | Yes |
| Indeed XML feed | Yes |
| Referral links with source tracking | Yes |
| Employer branding home page | Yes |
| Team 2 handoff (PENDING fallback) | Yes — shows pattern, Team 2 not live |
| LinkedIn channel (PENDING fallback) | Yes — shows pattern, no API key |
| Hourly expiry scheduler | Yes — code runs, set a past deadline to trigger |
| Auth-protected routes | Yes — demo mode hardcodes logged-in user |

## What is production-ready

The following components are production-quality with no further work:
- All entity models and database schema
- Global exception handler with structured error responses
- File storage service (MIME + extension validation)
- Outbox/retry pattern for Team 2 handoff
- Channel status lifecycle
- SEO implementation (meta tags, JSON-LD)
- WCAG 2.1 AA accessibility
- 78-test suite

## What is still local/demo fallback

| Component | What to replace for production |
|-----------|-------------------------------|
| `LoggingEmailService` | SMTP implementation (Spring Mail + SES/SendGrid) |
| `app.team2.api-base-url: ""` | Team 2's real service URL |
| `app.channels.linkedin.api-configured: false` | LinkedIn Jobs API credentials |
| Auth hardcoded in `authStore.ts` | Real JWT flow with `frontend/backend/` |
| localStorage for applied jobs | Backend `/api/applications/mine` |
| Local filesystem for resumes | AWS S3 or GCP GCS |
| Demand events via REST | Kafka consumer on Team 1's topic |

## What to say in presentation / interview

1. **Architecture decision:** "We built a single Spring Boot service that owns the entire Market Presence domain — job postings, applications, channels, referrals, and handoffs. We used interfaces (EmailService, Team2Client, FileStorageService) as seams so each production replacement is a one-class swap."

2. **Resilience design:** "The apply flow never fails because of an external system. Team 2 handoffs use an outbox pattern — saved as PENDING first, HTTP call attempted second. If Team 2 is down, an admin can retry via our handoff inspector endpoint."

3. **REQ coverage:** "All 11 requirements are implemented. Some use production-grade code (channels, SEO, referrals, duplicate blocking). Others use demo-friendly fallbacks that are explicit and easy to upgrade — flag-controlled LinkedIn, logged email, empty Team 2 URL."

4. **Testing:** "78 tests, zero failures. Controller tests use MockMvc with standalone setup — no Spring context, fast execution. Service tests use Mockito. We tested all happy paths and critical error paths: 409 duplicate, 404 not found, 400 invalid file type, handoff failure isolation."

5. **Frontend approach:** "React 19 with TypeScript and React Query. Server state is cached with 5-minute stale time. Client-side filtering for instant UX without extra API calls. The auth store is in demo mode — isAuthenticated hardcoded true so the full apply flow works in any demo environment."
