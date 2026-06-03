# Forge Team 3 Repository Atlas

> **Audience:** New developer who has never seen this codebase.  
> **Source of truth:** Forge PES v2.0 PDF specification.  
> **Last verified:** 2026-06-03 — all file paths, class names, and endpoints confirmed from actual source code.

---

## 1. Project Identity

### What is Forge?

Forge is Grid Dynamics' internal workforce intelligence platform. It automates the entire hiring lifecycle — from a business unit deciding "we need an engineer" all the way through to a hired employee's first day. Think of it as a private LinkedIn + ATS (Applicant Tracking System) built by Grid Dynamics engineers, for Grid Dynamics.

### What is this repository?

This repository is **Team 3's module** of Forge: the **Market Presence — Core Portal**. It is the public-facing careers website plus the backend system that powers it. Candidates browse live job postings and submit applications here. Recruiters manage those postings through a REST API and an admin UI.

### Which team owns this?

**Bangalore Team 3** — one of six teams building the Forge PES (Personnel Engineering System) v2.0.

### Why does this project exist?

Without this system, HR would:
- Manually copy-paste job descriptions to LinkedIn, Indeed, and the company website
- Receive applications via email with no tracking
- Manually forward application data to the candidate pipeline team

With this system:
- Job postings are created from demand data automatically (or manually by HR)
- Candidates browse a polished careers portal at `/jobs` or `/careers`
- They apply through a validated multi-step form with resume upload
- Applications are automatically forwarded to Team 2's pipeline service
- Jobs are distributed to external job boards (LinkedIn, Indeed)
- Analytics data flows to Team 4's reporting system

### What problem does Team 3 solve?

Team 3 sits in the middle of the Forge ecosystem. It **receives** hiring demand from Team 1 (Chennai) and **sends** application records to Team 2. It is the only team that is directly visible to external job applicants — the public-facing careers portal is owned entirely by Team 3.

### How this fits into the larger Forge platform

```
Team 1 (Chennai)     →  demand.opened Kafka event
                              ↓
              ┌─────────────────────────────────┐
              │  TEAM 3 (Bangalore) — THIS REPO  │
              │  Market Presence — Core Portal    │
              │  - Job Posting CRUD               │
              │  - Public Careers Portal          │
              │  - Application Intake             │
              │  - Channel Publishing             │
              │  - Analytics Events               │
              └─────────────────────────────────┘
                     ↓                   ↓
              Team 2 (?)          Team 4 (Analytics)
              Talent Acquisition  Reporting / BI
```

### What does "Market Presence — Core Portal" mean?

"Market Presence" means making Forge AI visible in the job market — putting real job postings on LinkedIn, Indeed, and a branded careers website. "Core Portal" is the canonical name in the PES spec for Team 3's deliverable: the system that creates, manages, publishes, and tracks job postings.

### What is in scope for Team 3?

- Job posting CRUD (create, read, update, delete)
- Demand → job posting auto-creation (REQ-JP-01)
- Public careers portal UI (React, port 5173)
- Candidate application intake with resume upload
- Publishing to CAREERS_PORTAL, LinkedIn (fallback), Indeed (XML feed)
- Analytics event collection (views, clicks, apply starts, completions)
- Application handoff to Team 2 pipeline
- SEO (slugs, meta tags, JSON-LD)
- Employer branding (About Us, culture, benefits)
- Employee referral links

### What is out of scope for Team 3?

- Demand creation and approval workflow (Team 1, Chennai)
- Candidate screening, interviews, offers (Team 2)
- Real-time analytics dashboards and BI reports (Team 4)
- HR profile management beyond what serves the portal
- AI/ML semantic job matching (separate Forge module)

---

## 2. Official Specification Context

### The Forge PES PDF is the source of truth

All requirements come from the **Forge PES v2.0 specification PDF**. Team 3 owns requirements REQ-JP-01 through REQ-JP-11.

### REQ-JP-01 — Demand-to-Job-Posting Auto-Creation

**Plain English:** When Chennai Team 1 marks a hiring demand as "open for external hiring" (status = OPEN_EXTERNAL), Team 3 must automatically create a DRAFT job posting pre-filled with the demand data (title, seniority, skills, location, department).

**Priority:** Must

**How it works in code:** Team 1 publishes a Kafka event on topic `demand-opened`. `DemandEventConsumer.java` listens on that topic. In dev/demo, call `POST /api/job-postings/from-demand` directly with a `DemandSnapshot` JSON body — the same code runs both ways.

### REQ-JP-02 — Job Posting Management (Admin CRUD)

**Plain English:** HR recruiters must be able to create, edit, view, and delete job postings through a UI or API. Every job posting must have a rich set of fields: title, description, responsibilities, requirements, benefits, seniority, location, department, salary, work mode, deadline.

**Priority:** Must

**How it works in code:** `JobPostingController.java` exposes full CRUD at `/api/job-postings`. The frontend provides `AdminJobsPage.tsx` (list/manage) and `AdminJobEditorPage.tsx` (create/edit form).

### REQ-JP-03 — Channel Publishing and Unpublishing

**Plain English:** When a job is published it should be broadcast to the careers portal, LinkedIn, and Indeed. When the job is closed or its application deadline passes, it must be automatically removed from all those channels.

**Priority:** Must

**How it works in code:** `JobPostingChannelController.java` handles publish/unpublish. `JobPostingChannelService.java` manages the logic: CAREERS_PORTAL → always LIVE; LinkedIn → LIVE if API configured, else PENDING with copy-URL message; Indeed → LIVE via XML feed. `ChannelExpiryScheduler.java` runs hourly and auto-closes expired jobs.

### REQ-JP-04 — Automatic Careers Portal Publication

**Plain English:** When a job's status changes to PUBLISHED, it must automatically appear on the careers portal immediately — no extra publish step needed.

**Priority:** Must

**How it works in code:** `JobPostingService.updateStatus()` calls `channelService.upsertCareersPortalChannel()` when status transitions to PUBLISHED. The CAREERS_PORTAL channel is set to LIVE automatically. `DataSeeder.seedCareersPortalChannels()` also ensures all existing PUBLISHED jobs have a channel record on startup.

### REQ-JP-05 — Analytics Events

**Plain English:** Every time a candidate views a job detail, clicks "Apply Now", opens the application form, or completes an application — that event must be recorded and available via an API so Team 4 can build analytics dashboards.

**Priority:** Must

**How it works in code:** `AnalyticsService.java` records events to the `job_posting_analytic_events` table. Events: VIEW, CLICK, APPLY_START, APPLY_COMPLETE. `JobAnalyticsController.java` exposes aggregated counts at `/api/analytics/job-postings`. The frontend fires CLICK/APPLY_START events via `analytics.api.ts`.

### REQ-JP-06 — Accessible Public Careers Portal

**Plain English:** The careers portal must be accessible to all users, including those using screen readers or keyboard-only navigation. It must pass WCAG 2.1 AA automated accessibility checks with zero critical violations.

**Priority:** Must

**How it works in code:** The React UI uses semantic HTML and TailwindCSS. The `qa/accessibility/` folder contains `axe-scan.mjs` — a Playwright + axe-core script that scans `/jobs`, `/careers/:slug`, and `/careers/:slug/apply`. Scan results are saved to `qa/accessibility/scan-results/`. As of 2026-06-03, zero critical violations are confirmed.

### REQ-JP-07 — Candidate Application with Resume Upload

**Plain English:** Candidates must be able to apply for a job through a multi-step form. The form must collect personal info, experience, education, skills, certifications, projects, screening answers, documents, and GDPR consent. A resume file (PDF or DOCX, max 10 MB) must be uploadable. The system must detect and block duplicate applications.

**Priority:** Must

**How it works in code:** `ApplicationIntakeController.java` accepts `multipart/form-data` at `POST /api/public/jobs/{slug}/apply`. `ApplicationIntakeService.java` validates, stores the file via `FileStorageService.java`, detects duplicates by (job_posting_id + candidate_email), and returns 409 on duplicate. The frontend `ApplicationPage.tsx` renders a 10-step stepper via `ApplicationStepper.tsx`.

### REQ-JP-08 — Application Handoff to Team 2

**Plain English:** Every application submitted through the careers portal must be forwarded to Chennai Team 2's Talent Acquisition Engine within 30 seconds. If Team 2 is unavailable, the handoff must be stored and retried.

**Priority:** Must

**How it works in code:** `ApplicationHandoffService.java` creates a `HandoffRecord` with status=PENDING immediately after each application save. `HttpTeam2Client.java` posts to `{app.team2.api-base-url}/applications/intake`. The stub URL `http://localhost:8086/api/stub/team2` is active by default — `Team2StubController.java` receives it locally. The admin endpoint `POST /api/admin/handoffs/{id}/retry` allows manual retry. `POST /api/admin/handoffs/retry-pending` bulk-retries all PENDING records.

### REQ-JP-09 — SEO-Friendly Careers Portal

**Plain English:** The careers portal must have clean, human-readable URLs (`/careers/react-frontend-engineer-bangalore`), proper HTML meta tags (title, description), and JSON-LD structured data so search engines can index job postings correctly.

**Priority:** Should

**How it works in code:** Every job has a unique `slug` field (e.g. `react-frontend-engineer-bangalore`). The backend stores `metaTitle` and `metaDescription`. `useDocumentMeta.ts` sets `document.title`, injects `<meta name="description">`, and injects `<script type="application/ld+json">` for each job detail page. Routes `/careers` and `/careers/:slug` are the canonical PES URLs (alongside the legacy `/jobs` routes).

### REQ-JP-10 — Employer Branding

**Plain English:** The careers portal must show company information: a mission statement, culture values, employee benefits, and employee testimonial stories. This content must be editable without code changes.

**Priority:** Should

**How it works in code:** `branding.ts` is the single source of truth for all branding content. It exports a `BrandingConfig` object with company name, tagline, mission, culture values, benefits, and employee stories. Currently this is a static TypeScript file. The spec requires it to be editable via admin UI — that backend endpoint (`GET /api/admin/branding`) is marked Missing.

### REQ-JP-11 — Employee Referral Links

**Plain English:** Employees must be able to generate a shareable referral URL for any job posting. When a candidate applies through a referral URL, their application must be tagged with `source=REFERRAL` instead of `source=CAREERS_PORTAL`.

**Priority:** Should

**How it works in code:** `JobPostingReferralController.java` at `POST /api/job-postings/{id}/referrals` generates a unique 10-character code and returns a shareable URL (`/careers/{slug}?ref={code}`). When a candidate applies and passes a `referralCode` field, `JobReferralService.resolveSource()` verifies the code and sets `source=REFERRAL`. `JobReferralService.markApplied()` updates the referral record status to APPLIED.

### Must vs Should

The PES spec uses two priority levels:
- **Must** = non-negotiable for acceptance. REQ-JP-01 through REQ-JP-08 are Must.
- **Should** = expected for a complete submission but the spec allows documented trade-offs. REQ-JP-09 through REQ-JP-11 are Should.

### Compliance Summary (detailed matrix in Section 14)

| ID | Status |
|----|--------|
| REQ-JP-01 | Done — HTTP fallback + Kafka consumer wired |
| REQ-JP-02 | Done — full CRUD + admin UI |
| REQ-JP-03 | Done — publish/unpublish + expiry scheduler |
| REQ-JP-04 | Done — auto-publish on status change |
| REQ-JP-05 | Done — event recording + analytics API |
| REQ-JP-06 | Done — axe-core zero critical violations confirmed |
| REQ-JP-07 | Done — multipart form + resume upload + duplicate block |
| REQ-JP-08 | Demo fallback — local stub active; replace URL for real Team 2 |
| REQ-JP-09 | Done — slugs + meta tags + JSON-LD |
| REQ-JP-10 | Demo fallback — static branding.ts; admin editor endpoint missing |
| REQ-JP-11 | Done — referral link generation + source tracking |

---

## 3. Current Repository Structure

```
market-presence-fullstack/               ← repo root
├── README.md                            ← project overview and quick-start guide
├── .idea/                               ← IntelliJ IDEA project config (ignore)
│
├── backend/                             ← ACTIVE Spring Boot 4 backend (port 8086)
│   ├── pom.xml                          ← Maven build file, Java 21, Spring Boot 4.0.6
│   ├── mvnw / mvnw.cmd                  ← Maven wrapper scripts (no separate Maven install needed)
│   ├── src/main/java/...                ← application source code
│   ├── src/main/resources/
│   │   ├── application.yml              ← all configuration (DB, port, email, Kafka, JWT)
│   │   └── db/migration/               ← Flyway SQL migrations V1, V2, V3
│   └── src/test/                        ← JUnit 5 + Mockito tests
│
├── frontend/                            ← frontend workspace root
│   ├── frontend/                        ← ACTIVE React 19 + Vite frontend (port 5173)
│   │   ├── package.json                 ← npm dependencies
│   │   ├── vite.config.ts               ← Vite bundler config
│   │   ├── .env.local                   ← VITE_API_URL=http://localhost:8086/api
│   │   ├── src/                         ← application source code
│   │   ├── dist/                        ← compiled production build output
│   │   ├── nginx.conf                   ← Nginx config for Docker deployment
│   │   └── Dockerfile                   ← Docker image for the frontend
│   ├── docker-compose.yml               ← Docker Compose for containerised dev
│   └── Makefile                         ← helper commands for Docker workflow
│
├── docs/                                ← project documentation
│   ├── TEAM3_PROJECT_EXPLANATION.md     ← earlier beginner-friendly overview
│   └── FORGE_TEAM3_REPOSITORY_ATLAS.md ← THIS FILE (the complete atlas)
│
└── qa/                                  ← quality assurance scripts
    └── accessibility/
        ├── axe-scan.mjs                 ← Playwright + axe-core accessibility scan (REQ-JP-06)
        ├── package.json                 ← @playwright/test, @axe-core/playwright
        └── scan-results/               ← JSON output from axe-core scan runs
            ├── jobs-prefix.json
            ├── jobs-postfix.json
            ├── careers-detail-prefix.json
            ├── careers-detail-postfix.json
            ├── careers-apply-prefix.json
            └── careers-apply-postfix.json
```

> **Note on archive folder:** The instructions reference `./archive/unused-career-backend`. As of 2026-06-03, no `archive/` directory exists in this repository. If an old backend existed previously, it has been removed. Do not look for it.

### Folder-by-folder guide

| Folder | Active? | Purpose | Should you modify it? |
|--------|---------|---------|----------------------|
| `backend/` | YES | Spring Boot API server, all business logic | Yes — this is where new features go |
| `frontend/frontend/` | YES | React UI, all candidate and admin pages | Yes — for UI changes |
| `frontend/` (root) | YES (config only) | Docker Compose, Makefile for containerised dev | Only for Docker setup |
| `docs/` | YES | Documentation files | Yes — keep docs updated |
| `qa/accessibility/` | YES | Axe-core scan for REQ-JP-06 | Yes — run before demos |
| `frontend/frontend/dist/` | NO | Build output, git-ignored | Never modify manually |

---

## 4. Is This Microservices?

**Short answer: No — not yet. It is a modular full-stack application that is microservice-ready.**

### Honest assessment

The current codebase is a **single Spring Boot application** that handles all of Team 3's responsibilities in one process. There is one deployed backend at port 8086 and one React SPA at port 5173. They communicate over HTTP/JSON. There is no service mesh, no API gateway, no container orchestration.

### What makes it microservice-ready

The code is designed with microservice separation in mind:
- `Team2Client` interface + `HttpTeam2Client` implementation: Team 2 is called over HTTP. Swapping the base URL is a one-line config change.
- `DemandEventConsumer`: the Kafka `@KafkaListener` is wired but the broker is not running locally. The HTTP fallback (`POST /api/job-postings/from-demand`) makes the same code path testable.
- `KafkaEvents.java`: event shapes for ApplicationSubmitted, StatusUpdated, JobViewed are defined and ready to publish.
- `OutboxEvent` entity and `outbox_events` table: the outbox pattern is in the schema (V3 migration), ready for a transactional outbox publisher.
- Clean package separation: config, controller, service, repository, entity, dto, event, exception, util.

### What is still local / demo fallback

| Concern | Current state | What is needed for real microservices |
|---------|--------------|--------------------------------------|
| Team 1 Kafka event | `DemandEventConsumer` listens but no broker runs locally | Kafka broker (local: docker run apache/kafka) or shared dev cluster |
| Team 2 handoff | Local stub at `/api/stub/team2` | Real Team 2 URL in `app.team2.api-base-url` |
| Email delivery | Logs to console (`LoggingEmailService`) | Set `app.email.smtp.enabled=true` + SMTP/SendGrid credentials |
| LinkedIn publish | Sets status=PENDING | LinkedIn Jobs API credentials in `app.channels.linkedin.api-configured=true` |
| Independent deployment | One JVM process | Docker image per service, Kubernetes deployment, CI/CD pipeline |
| API Gateway | None | Nginx or Spring Cloud Gateway in front of all PES services |
| Service contracts | Informal | OpenAPI spec files committed to a shared schema registry |

---

## 5. High-Level Architecture

### Flow diagram

```
┌─────────────────────────────────────────────────────────────────┐
│  Browser  (http://localhost:5173)                                │
│                                                                  │
│  React 19 + TypeScript + Vite + TailwindCSS                     │
│  ├── pages/       ← one file per page route                     │
│  ├── components/  ← reusable UI building blocks                 │
│  ├── api/         ← Axios functions that call the backend       │
│  ├── features/    ← hooks + types + services for jobs           │
│  ├── store/       ← Zustand auth state persisted to localStorage│
│  └── routes/      ← React Router v7 route definitions          │
└──────────────┬──────────────────────────────────────────────────┘
               │  HTTP/JSON  (VITE_API_URL = http://localhost:8086/api)
               │  Authorization: Bearer <JWT token>
               ▼
┌─────────────────────────────────────────────────────────────────┐
│  Spring Boot 4  (http://localhost:8086)                         │
│                                                                  │
│  ├── SecurityConfig     ← JWT filter, CORS, route permissions   │
│  ├── controller/        ← REST endpoints (13 controllers)       │
│  │   ├── JobPostingController        /api/job-postings          │
│  │   ├── PublicJobController         /api/public/jobs           │
│  │   ├── ApplicationIntakeController /api/public/jobs/{slug}/apply│
│  │   ├── JobAnalyticsController      /api/analytics + /api/public/jobs/{slug}/events│
│  │   ├── JobPostingChannelController /api/job-postings/{id}/channels│
│  │   ├── IndeedFeedController        /api/public/jobs/feed.xml  │
│  │   ├── JobPostingReferralController/api/job-postings/{id}/referrals│
│  │   ├── JobReferralController       /api/referrals             │
│  │   ├── HandoffStatusController     /api/admin/handoffs        │
│  │   ├── AuthController              /api/auth                  │
│  │   ├── ProfileController           /api/profile               │
│  │   ├── ApplicationController       /api/applications          │
│  │   └── Team2StubController         /api/stub/team2            │
│  ├── service/           ← all business logic                    │
│  ├── repository/        ← Spring Data JPA (SQL queries)         │
│  ├── entity/            ← JPA entity classes → DB tables        │
│  └── config/            ← DataSeeder, security, Kafka setup     │
└──────────────┬──────────────────────────────────────────────────┘
               │  JDBC / Hibernate
               ▼
┌─────────────────────────────────────────────────────────────────┐
│  PostgreSQL  (localhost:5432)                                    │
│  Database: forge_market_presence   User: sanjay                 │
│                                                                  │
│  ├── job_postings              ← all job postings               │
│  ├── application_intakes       ← candidate applications         │
│  ├── job_posting_channels      ← channel publish records        │
│  ├── job_referrals             ← referral links                 │
│  ├── job_posting_analytic_events ← view/click/apply events      │
│  ├── application_handoffs      ← Team 2 handoff records         │
│  ├── users                     ← registered candidates/admins   │
│  ├── candidate_profiles        ← candidate profile data         │
│  ├── external_candidates       ← external applicant records     │
│  ├── candidate_applications    ← linked application records     │
│  ├── notification_logs         ← email send log                 │
│  └── outbox_events             ← transactional outbox (ready)   │
└─────────────────────────────────────────────────────────────────┘
               │  (future)
               ▼
┌───────────────────────────────────┐    ┌──────────────────────────┐
│  Team 2 — Talent Acq Engine       │    │  Team 4 — Analytics / BI  │
│  POST /applications/intake        │    │  GET /api/analytics/...   │
│  (stub active locally)            │    │  (API is live)            │
└───────────────────────────────────┘    └──────────────────────────┘
```

### Candidate flow (public, no login required)

1. Candidate opens `http://localhost:5173/jobs`
2. `useJobs.ts` calls `GET /api/public/jobs` → returns all PUBLISHED jobs
3. Candidate clicks a job card → `GET /api/public/jobs/{slug}` → VIEW event recorded
4. Candidate clicks "Apply Now" → `analytics.api.ts` fires `CLICK` event → redirects to `/careers/{slug}/apply`
5. `ApplicationPage.tsx` fires `APPLY_START` event → shows 10-step form
6. Candidate uploads resume, fills form, submits → `POST /api/public/jobs/{slug}/apply` (multipart)
7. Backend: validates → checks duplicate → stores resume file → saves `ApplicationIntake` → sends email → marks referral if present → triggers handoff to Team 2 → records `APPLY_COMPLETE` event
8. Frontend: shows `SuccessModal.tsx`

### Admin / recruiter flow (requires JWT login)

1. Admin logs in at `/login` → `POST /api/auth/login` → JWT token stored in localStorage
2. Admin navigates to `/admin/jobs` → `GET /api/job-postings` (requires ROLE_ADMIN or ROLE_RECRUITER)
3. Admin clicks "New Job" → `/admin/jobs/new` → fills `AdminJobEditorPage.tsx` → `POST /api/job-postings`
4. Admin clicks "Publish" → `PATCH /api/job-postings/{id}/status` with `{"status":"PUBLISHED"}`
5. Backend auto-creates CAREERS_PORTAL channel record with status=LIVE
6. Admin manually publishes to LinkedIn/Indeed via `POST /api/job-postings/{id}/channels/{channel}/publish`

### Analytics flow

The frontend fires events at key moments using `analytics.api.ts`. The backend records each event to `job_posting_analytic_events`. Team 4 queries `GET /api/analytics/job-postings` or `GET /api/analytics/job-postings/{id}` to get per-channel aggregated counts.

### Referral flow

Employee calls `POST /api/job-postings/{id}/referrals` → gets a shareable URL like `http://localhost:5173/careers/react-frontend-engineer-bangalore?ref=ABC123456`. Candidate visits that URL and applies. The `referralCode` is included in the apply request. Backend checks the code, sets `source=REFERRAL`, marks the referral record as APPLIED.

### Demand-to-job fallback

Team 1 is supposed to publish a Kafka event on topic `demand-opened`. `DemandEventConsumer` listens for it. In dev/demo (no Kafka running), send `POST /api/job-postings/from-demand` with a `DemandSnapshot` JSON body — the same `JobPostingService.createFromDemand()` method is called either way.

---

## 6. Backend Overview

### Path

```
backend/
└── src/main/java/com/griddynamics/forge/market_presence_service/
```

### Main application class

`MarketPresenceServiceApplication.java` — the `@SpringBootApplication` entry point. Run this to start the server.

### Technology stack

| Technology | Version | Role |
|-----------|---------|------|
| Java | 21 | Language |
| Spring Boot | 4.0.6 | Framework |
| Spring Security | included | JWT auth, CORS |
| Spring Data JPA | included | Database access |
| Hibernate | included | ORM |
| PostgreSQL driver | included | DB connectivity |
| Flyway | included | DB schema migrations |
| SpringDoc OpenAPI | 3.0.2 | Swagger UI |
| JJWT | 0.11.5 | JWT generation/validation |
| Spring Kafka | included | Kafka consumer |
| Spring Mail | included | Email (SMTP) |
| Lombok | included | Boilerplate reduction |
| Spring Actuator | included | Health endpoint |
| Maven | wrapper | Build tool |

### PostgreSQL connection (application.yml)

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/forge_market_presence
    username: sanjay
    password: 1234
```

### Server port: 8086

### Key application.yml sections

```yaml
server:
  port: 8086

app:
  jwt:
    secret: "-----------------"
    expiration-ms: 86400000           # 24 hours

  upload:
    dir: uploads/resumes              # local disk storage for resume files

  team2:
    api-base-url: "http://localhost:8086/api/stub/team2"   # points to local stub
    timeout-seconds: 10

  email:
    smtp:
      enabled: false                  # false = LoggingEmailService (log only)
      from: noreply@forge-ai.com

  channels:
    linkedin:
      api-configured: false           # false = PENDING status, no real API call
    careers-portal:
      base-url: "http://localhost:5173"
```

### Swagger UI

URL: `http://localhost:8086/swagger-ui/index.html`  
All 13 controllers are documented here. You can execute requests directly from the browser. For authenticated endpoints, use the "Authorize" button and paste a JWT token.

### Actuator health

URL: `http://localhost:8086/actuator/health`  
Returns `{"status":"UP"}` when the backend is running and the database is connected.

### Flyway migrations

Three SQL files in `src/main/resources/db/migration/`:
- `V1__init_market_presence.sql` — core tables: job_postings, application_intakes, job_posting_channels, job_referrals, application_handoffs
- `V2__careers_auth_migration.sql` — users, candidate_profiles, candidate_skills, external_candidates, candidate_applications
- `V3__infrastructure_migration.sql` — notification_logs, outbox_events

Flyway runs automatically on startup. Hibernate `ddl-auto: update` also keeps the schema in sync with entity classes.

---

### config package

#### ApplicationConfig.java

Sets up the Spring Security beans needed for JWT auth:
- `UserDetailsService` — loads a `User` entity by email from the database
- `AuthenticationProvider` — `DaoAuthenticationProvider` using the UserDetailsService + BCrypt
- `AuthenticationManager` — wired into `AuthController` for login
- `PasswordEncoder` — `BCryptPasswordEncoder(12)` (12 rounds, production-grade)
- `ObjectMapper` — registers `JavaTimeModule` so `Instant` and `LocalDate` serialise correctly

#### DataSeeder.java

Implements `CommandLineRunner` — runs every time the application starts.

What it does (all idempotent — safe to restart):
1. `patchLegacyJobs()` — fills null columns on pre-existing rows (department, workMode, salaries etc.)
2. `patchLegacyChannelNames()` — normalises "LinkedIn" → "LINKEDIN", "Indeed" → "INDEED"
3. `seedJobPostings()` — inserts 8 demo jobs if their slugs don't already exist
4. `seedApplicationIntakes()` — inserts 4 sample applications for demo jobs
5. `seedChannels()` — inserts sample channel records for demo jobs
6. `seedReferrals()` — inserts 2 sample referral records
7. `seedCareersPortalChannels()` — ensures every PUBLISHED job has a CAREERS_PORTAL channel with status=LIVE

#### KafkaConfig.java

Configures the Kafka consumer. The `@KafkaListener` in `DemandEventConsumer` references the container factory created here. In local dev without a Kafka broker, the listener is registered but never receives messages — it simply waits. No error is thrown.

#### SecurityConfig.java

Configures Spring Security:

```
Public (no token needed):
  OPTIONS /**                         ← CORS preflight
  /api/auth/**                        ← login + register
  GET /api/public/**                  ← public jobs, apply, analytics events, XML feed
  /actuator/**                        ← health check

Protected (JWT required):
  /api/job-postings/**                ← ROLE_ADMIN or ROLE_RECRUITER
  /api/admin/**                       ← ROLE_ADMIN only
  everything else                     ← any authenticated user
```

The `JwtAuthFilter` (inner class) reads the `Authorization: Bearer <token>` header, validates the JWT, and sets Spring Security's `SecurityContextHolder` for the request lifetime.

**Important for new developers:** The admin endpoints (`/api/job-postings/**`) require a JWT token from a user with role ADMIN or RECRUITER. The `DataSeeder` does not create admin users. To use admin endpoints, register a user via `POST /api/auth/register`, then manually update their role in the database:
```sql
UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';
```

---

### controller package

#### JobPostingController.java

**Path:** `/api/job-postings`  
**Requirement:** REQ-JP-01, REQ-JP-02  
**Calls:** `JobPostingService`

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/job-postings` | Create a new job posting (starts as DRAFT) |
| POST | `/api/job-postings/from-demand` | Pre-fill DRAFT from DemandSnapshot (REQ-JP-01) |
| GET | `/api/job-postings` | List all with filters and pagination |
| GET | `/api/job-postings/{id}` | Get one by database ID |
| GET | `/api/job-postings/slug/{slug}` | Get one by URL slug |
| PUT | `/api/job-postings/{id}` | Replace all fields |
| PATCH | `/api/job-postings/{id}/status` | Change status only |
| DELETE | `/api/job-postings/{id}` | Delete permanently |

Pagination query params for GET list: `status`, `location`, `seniority`, `title`, `page`, `size`, `sortBy`, `sortDir`.

#### PublicJobController.java

**Path:** `/api/public/jobs`  
**Requirement:** REQ-JP-06, REQ-JP-05  
**Calls:** `JobPostingService`, `AnalyticsService`  
**Auth:** None required (permitAll)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/public/jobs` | List all PUBLISHED jobs |
| GET | `/api/public/jobs/{slug}` | Get one PUBLISHED job + record VIEW event |

The `GET /{slug}` endpoint records a VIEW analytic event automatically. The `?channel` query param (default: `CAREERS_PORTAL`) allows LinkedIn/Indeed crawlers to identify themselves.

#### ApplicationIntakeController.java

**Path:** `/api/public/jobs`  
**Requirement:** REQ-JP-07  
**Calls:** `ApplicationIntakeService`  
**Auth:** None required (permitAll — candidates apply without an account)

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/public/jobs/{slug}/apply` | Submit candidate application (multipart/form-data) |

Request format: `multipart/form-data` with two parts:
- `application` — JSON: `{candidateName, candidateEmail, candidatePhone, coverLetter, referralCode}`
- `resume` — optional PDF or DOCX file (max 10 MB)

Returns 201 on success. Returns 409 with a friendly message on duplicate. Returns 400 if resume is not PDF/DOCX or if required fields are missing.

#### JobAnalyticsController.java

**Requirement:** REQ-JP-05  
**Calls:** `AnalyticsService`, `JobPostingRepository`  
**Auth:** None required for event recording; analytics read is public

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/public/jobs/{slug}/events` | Record CLICK or APPLY_START event (always 204) |
| GET | `/api/analytics/job-postings` | Aggregate analytics for all jobs |
| GET | `/api/analytics/job-postings/{id}` | Aggregate analytics for one job |

Event recording is fire-and-forget — unknown slugs are silently ignored. Analytics never throws — it never breaks the user's flow.

#### JobPostingChannelController.java

**Path:** `/api/job-postings/{id}`  
**Requirement:** REQ-JP-03, REQ-JP-04  
**Calls:** `JobPostingChannelService`

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/job-postings/{id}/channels/{channel}/publish` | Publish to CAREERS_PORTAL, LINKEDIN, or INDEED |
| POST | `/api/job-postings/{id}/channels/{channel}/unpublish` | Unpublish from a channel |
| GET | `/api/job-postings/{id}/channels` | List all channel records |
| POST | `/api/job-postings/{id}/publish/linkedin` | Legacy endpoint (delegates to `/channels/LINKEDIN/publish`) |
| POST | `/api/job-postings/{id}/publish/indeed` | Legacy endpoint (delegates to `/channels/INDEED/publish`) |

#### IndeedFeedController.java

**Path:** `/api/public/jobs/feed.xml`  
**Requirement:** REQ-JP-03 (Indeed fallback)  
**Auth:** None required  
**Produces:** `application/xml`

Returns all PUBLISHED jobs in Indeed's XML job feed format. Submit this URL to Indeed's Job Distributor portal to activate automatic synchronisation. No Indeed API key required — Indeed pulls the XML.

#### JobPostingReferralController.java

**Path:** `/api/job-postings/{id}`  
**Requirement:** REQ-JP-11  
**Calls:** `JobReferralService`

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/job-postings/{id}/referrals` | Generate shareable referral URL |
| GET | `/api/job-postings/{id}/referrals` | List all referrals for a job |

Body for POST: `{"referrerId": 1001}` (optional — referrer's employee ID).

#### JobReferralController.java

**Path:** `/api/referrals`  
**Note:** Legacy controller kept for backward compatibility. Prefer `/api/job-postings/{id}/referrals`.

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/referrals` | Create a referral (requires full `JobReferralRequest` body) |
| GET | `/api/referrals/{referralCode}` | Look up a referral by its unique code |

#### HandoffStatusController.java

**Path:** `/api/admin/handoffs`  
**Requirement:** REQ-JP-08  
**Calls:** `ApplicationHandoffService`  
**Auth:** Requires ROLE_ADMIN

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/admin/handoffs` | List handoff records (filter by `?status=PENDING|SENT|FAILED`) |
| GET | `/api/admin/handoffs/{id}` | Get one handoff record |
| POST | `/api/admin/handoffs/{id}/retry` | Retry one PENDING or FAILED handoff |
| POST | `/api/admin/handoffs/retry-pending` | Retry all PENDING handoffs at once |

#### AuthController.java

**Path:** `/api/auth`  
**Auth:** None required (permitAll)

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/auth/register` | Register a new user account (returns JWT) |
| POST | `/api/auth/login` | Login and receive JWT |

Both return `ApiResponse<AuthDto.AuthResponse>` with `{token, email, role, userId, name}`.

#### ProfileController.java

**Path:** `/api/profile`  
**Auth:** Any authenticated user (JWT required)

| Method | Path | Purpose |
|--------|------|---------|
| GET | `/api/profile` | Get current user's candidate profile |
| PUT | `/api/profile` | Update profile details |
| POST | `/api/profile/resume` | Upload/replace resume file |

Uses `@AuthenticationPrincipal User currentUser` — Spring Security injects the authenticated `User` entity automatically.

#### ApplicationController.java

**Path:** `/api/applications`  
**Auth:** Any authenticated user (JWT required)

| Method | Path | Purpose |
|--------|------|---------|
| POST | `/api/applications/jobs/{slug}/apply` | Submit a full multi-document application (authenticated flow) |
| GET | `/api/applications/mine` | Get all applications for the authenticated user |

This controller handles the richer authenticated apply flow (accepts resume, coverLetter, certifications, transcripts, portfolio as separate file parts). It uses `ApplicationService`, not `ApplicationIntakeService`.

#### Team2StubController.java

**Path:** `/api/stub/team2`  
**Purpose:** Demo stub for Chennai Team 2's intake endpoint.

When `app.team2.api-base-url` points to `http://localhost:8086/api/stub/team2`, the handoff loop works locally without Team 2 actually running. Accepts `POST /api/stub/team2/applications/intake` and returns a generated demo application ID. Replace `app.team2.api-base-url` with Team 2's real URL to bypass this stub.

---

### service package

#### JobPostingService.java

Core business logic for job posting management.

- `create(JobPostingRequest)` — validates salary range (min must not exceed max), auto-generates slug from title+city, handles slug collisions by appending `-2`, `-3` etc., saves to DB
- `createFromDemand(DemandSnapshot)` — maps DemandSnapshot fields to JobPosting, joins `skills[]` into a comma-separated requirements string, normalises level aliases (STAFF → LEAD), always creates as DRAFT
- `updateStatus(id, StatusUpdateRequest)` — validates status transition (DRAFT → PUBLISHED → CLOSED), sets `publishedAt` when transitioning to PUBLISHED, calls `channelService.upsertCareersPortalChannel()` on PUBLISHED, calls `channelService.unpublishAllLiveChannels()` on CLOSED
- `getPublishedJobsPublic()` — queries only PUBLISHED jobs, maps to `PublicJobResponse`
- `getAll(...)` — paginated query with optional status/location/seniority/title filters
- `delete(id)` — throws 404 if not found, then deletes

#### ApplicationIntakeService.java

Orchestrates the public apply flow (REQ-JP-07, REQ-JP-08):
1. Finds job by slug or throws 404
2. Checks duplicate by (jobPostingId + candidateEmail) — throws 409 if duplicate
3. Stores resume file via `FileStorageService` if provided
4. Builds and saves `ApplicationIntake` entity
5. Increments `job.applicationsCount`
6. Sends confirmation email via `EmailService` (never throws)
7. Resolves referral source via `JobReferralService.resolveSource()`, marks referral as APPLIED if source=REFERRAL
8. Triggers handoff to Team 2 via `ApplicationHandoffService.createAndAttempt()` (never throws)
9. Records `APPLY_COMPLETE` analytics event (never throws)

#### AnalyticsService.java

Records and aggregates analytics events.
- `record(jobPostingId, eventType, channelName)` — validates event type (VIEW, CLICK, APPLY_START, APPLY_COMPLETE) and channel name, saves to DB. **Never throws** — exceptions are logged and swallowed.
- `getAnalytics(id)` — queries counts by job + event type + channel from the repository, returns `JobPostingAnalyticsResponse` with per-channel breakdown
- `getAllAnalytics()` — same but for every job posting

#### JobPostingChannelService.java

Manages channel publish/unpublish state (REQ-JP-03, REQ-JP-04):
- `publishChannel(id, channel)` — CAREERS_PORTAL → always LIVE; LINKEDIN → LIVE if `linkedInApiConfigured=true`, else PENDING with copy-URL message; INDEED → LIVE with feed URL instruction
- `unpublishChannel(id, channel)` — sets status=UNPUBLISHED, stamps `unpublishedAt`
- `upsertCareersPortalChannel(id, slug)` — called automatically by `JobPostingService` on PUBLISHED status change
- `unpublishAllLiveChannels(id)` — called automatically when a job is CLOSED or expires

#### ApplicationHandoffService.java

Orchestrates Team 2 handoff (REQ-JP-08):
- `createAndAttempt(intake, job)` — creates PENDING record, immediately attempts HTTP call if `team2BaseUrl` is configured. Never throws — errors are caught and the record status is updated to FAILED.
- `retry(id)` — re-attempts a single handoff by ID
- `retryAllPending()` — bulk-retries all PENDING records (useful when Team 2 URL is configured after applications were submitted)
- Status flow: PENDING → SENT (HTTP 2xx) or FAILED (HTTP error)

#### JobReferralService.java

Manages referral link lifecycle (REQ-JP-11):
- `generateLink(jobPostingId, referrerId)` — creates a unique 10-char code (UUID-derived), returns shareable URL `/careers/{slug}?ref={code}`
- `create(request)` — legacy endpoint path (requires full candidate info up-front)
- `resolveSource(code, jobPostingId)` — returns "REFERRAL" or "CAREERS_PORTAL"; validates code exists and belongs to the correct job
- `markApplied(code, name, email)` — updates referral record status to APPLIED and fills in candidate details

#### EmailService.java (interface) / LoggingEmailService.java / SmtpEmailService.java

`EmailService` is an interface with one method: `sendApplicationConfirmation(toEmail, candidateName, jobTitle)`.

- **`LoggingEmailService`** is active when `app.email.smtp.enabled=false` (the default). It writes the email content to the application log. No SMTP server needed.
- **`SmtpEmailService`** is active when `app.email.smtp.enabled=true`. It uses `spring.mail.*` settings to send a real email via SMTP (or SendGrid). To test locally: `docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog` then set `enabled: true`.

#### FileStorageService.java

Saves uploaded resume files to local disk at the path configured by `app.upload.dir` (`uploads/resumes` by default). Validates the file extension — only PDF and DOCX are accepted; throws `IllegalArgumentException` for anything else. Returns the relative file path as the `resumeUrl` stored in `ApplicationIntake`.

#### HttpTeam2Client.java (implements Team2Client)

Uses Spring's `RestClient` to POST a `Team2ApplicationPayload` to `{app.team2.api-base-url}/applications/intake`. Configured with a 10-second connect and read timeout. Returns `Optional<String>` — the application ID returned by Team 2's response `{"id": "T2-XXXXXXXX"}`.

#### AuthService.java

Handles user registration and login:
- `register()` — checks for duplicate email, saves `User` entity with BCrypt-hashed password, creates linked `CandidateProfile`, generates and returns JWT
- `login()` — authenticates via `AuthenticationManager`, generates JWT, retrieves profile name

#### ProfileService.java

CRUD for `CandidateProfile` linked to a `User`. Handles profile retrieval, update, and resume file upload for authenticated users.

#### ApplicationService.java

Handles the authenticated apply flow (richer than `ApplicationIntakeService`). Accepts multiple file uploads (resume, coverLetter, certifications, transcripts, portfolio), stores them, and saves a `CandidateApplication` record. Linked to `ApplicationController`.

#### ChannelExpiryScheduler.java

A `@Scheduled(cron = "0 0 * * * *")` component — runs every hour. Finds PUBLISHED jobs whose `applicationDeadline` is before today, sets their status to CLOSED, and calls `channelService.unpublishAllLiveChannels()` for each. This is the automatic REQ-JP-03 expiry mechanism.

---

### repository package

All repositories extend `JpaRepository<Entity, Long>` and inherit standard CRUD operations from Spring Data JPA.

| Repository | Entity | Key custom methods |
|-----------|--------|--------------------|
| `JobPostingRepository` | `JobPosting` | `findBySlug(slug)`, `existsBySlug(slug)`, `findByFilters(status, location, seniority, title, pageable)` (JPQL) |
| `ApplicationIntakeRepository` | `ApplicationIntake` | `existsByJobPostingIdAndCandidateEmail(jobId, email)` (duplicate check) |
| `JobPostingChannelRepository` | `JobPostingChannel` | `findByJobPostingId(id)`, `findByJobPostingIdAndChannelName(id, name)` |
| `JobReferralRepository` | `JobReferral` | `findByReferralCode(code)`, `existsByReferralCode(code)`, `findByJobPostingId(id)` |
| `JobPostingAnalyticEventRepository` | `JobPostingAnalyticEvent` | `countByJobPostingIdAndEventTypeAndChannelName(id, type, channel)` |
| `HandoffRecordRepository` | `HandoffRecord` | `findAllByOrderByCreatedAtDesc(pageable)`, `findByStatus(status, pageable)`, `findByStatus(status)` (for bulk retry) |
| `UserRepository` | `User` | `findByEmail(email)`, `existsByEmail(email)` |
| `CandidateProfileRepository` | `CandidateProfile` | `findByUser(user)` |
| `ExternalCandidateRepository` | `ExternalCandidate` | `findByEmail(email)` |
| `CandidateApplicationRepository` | `CandidateApplication` | `findByCandidateId(id)` |
| `OutboxEventRepository` | `OutboxEvent` | Standard JPA only |

---

### entity package

#### JobPosting.java → table: `job_postings`

The central entity. One row = one job posting.

| Column | Type | Notes |
|--------|------|-------|
| id | BIGSERIAL PK | Auto-increment |
| demand_id | BIGINT | Links to Team 1's demand record |
| title | VARCHAR NOT NULL | Job title |
| description | TEXT | Full job description (Markdown supported) |
| requirements | TEXT | Required skills/experience |
| responsibilities | TEXT | What the role involves |
| benefits | TEXT | Perks and compensation |
| employment_type | VARCHAR | FULL_TIME, PART_TIME, CONTRACT, INTERNSHIP |
| work_mode | VARCHAR | REMOTE, HYBRID, ONSITE |
| location | VARCHAR | Display string e.g. "Bangalore, KA" |
| location_city | VARCHAR | City component |
| location_state | VARCHAR | State/region code |
| location_country | VARCHAR | ISO country code |
| seniority | VARCHAR | JUNIOR, MID, SENIOR, LEAD, EXECUTIVE |
| department | VARCHAR | Engineering, Product, Data etc. |
| job_category | VARCHAR | Sub-category e.g. "Backend Development" |
| salary_min | INTEGER | Minimum salary (raw number) |
| salary_max | INTEGER | Maximum salary (raw number) |
| currency | VARCHAR | INR or USD |
| show_salary | BOOLEAN | Whether to display salary publicly |
| meta_title | VARCHAR | SEO page title |
| meta_description | TEXT | SEO meta description |
| status | VARCHAR | DRAFT, PUBLISHED, CLOSED |
| slug | VARCHAR UNIQUE | URL-friendly identifier |
| application_deadline | DATE | Auto-close date |
| applications_count | INTEGER DEFAULT 0 | Counter incremented on each apply |
| published_at | TIMESTAMP | When status became PUBLISHED |
| created_at | TIMESTAMP | Set by `@PrePersist` |
| updated_at | TIMESTAMP | Set by `@PreUpdate` |

#### ApplicationIntake.java → table: `application_intakes`

One row = one candidate application submitted via the public apply flow.

Unique constraint: `(job_posting_id, candidate_email)` — prevents duplicate applications.

| Column | Notes |
|--------|-------|
| id | Auto-increment PK |
| job_posting_id | FK to job_postings.id |
| candidate_name | Full name |
| candidate_email | Email (part of unique constraint) |
| candidate_phone | Optional phone number |
| resume_url | Local file path (e.g. `uploads/resumes/...`) |
| source | CAREERS_PORTAL or REFERRAL |
| cover_letter | TEXT — optional |
| status | SUBMITTED (default) |
| notes | Admin notes |
| applied_at | When the application was submitted |

#### JobPostingChannel.java → table: `job_posting_channels`

One row = one channel record for a job posting. A single job can have records for CAREERS_PORTAL, LINKEDIN, and INDEED.

Unique constraint: `(job_posting_id, channel_name)`.

Status values: `DRAFT`, `PENDING`, `LIVE`, `FAILED`, `UNPUBLISHED`  
Channel names: `CAREERS_PORTAL`, `LINKEDIN`, `INDEED`

#### JobReferral.java → table: `job_referrals`

One row = one referral record. Has a `referral_code` (unique) that becomes the `?ref=` query parameter.

Status values: `PENDING` (link generated, candidate hasn't applied yet), `APPLIED` (candidate used the link to apply).

#### JobPostingAnalyticEvent.java → table: `job_posting_analytic_events`

One row = one event occurrence (append-only, never updated).

Event types: `VIEW`, `CLICK`, `APPLY_START`, `APPLY_COMPLETE`  
Channel names: `CAREERS_PORTAL`, `LINKEDIN`, `INDEED`

Indexed on `(job_posting_id)` and `(job_posting_id, event_type, channel_name)` for fast count queries.

#### HandoffRecord.java → table: `application_handoffs`

One row = one Team 2 handoff attempt.

| Column | Notes |
|--------|-------|
| application_intake_id | FK to application_intakes.id |
| candidate_email | Denormalised for retry without joining |
| job_slug | Denormalised for retry |
| source | CAREERS_PORTAL or REFERRAL |
| status | PENDING, SENT, FAILED |
| error_message | Populated on FAILED status |
| team2_response_id | ID returned by Team 2 (e.g. "T2-ABC12345") |
| attempted_at | Timestamp of last attempt |

#### User.java → table: `users`

Registered users. Role can be: `CANDIDATE`, `ADMIN`, `RECRUITER`.  
Password is BCrypt-hashed (12 rounds).  
The `User` entity implements Spring Security's `UserDetails` so it can be injected with `@AuthenticationPrincipal`.

#### CandidateProfile.java → table: `candidate_profiles`

One-to-one with `User`. Stores the candidate's display name, phone, bio, professional title, resume file path, salary expectation, work mode preference, and notification settings.

#### CandidateApplication.java → table: `candidate_applications`

The richer application record for authenticated users. Links `ExternalCandidate` → `JobPosting`. Has unique constraint `(job_id, candidate_id)`.

#### ExternalCandidate.java → table: `external_candidates`

Stores the personal details of a candidate who has applied (name, email, phone, LinkedIn, GitHub, portfolio, gender, GDPR consent). One `ExternalCandidate` can apply to multiple jobs — they are deduplicated by email.

#### OutboxEvent.java → table: `outbox_events`

Schema-level support for the transactional outbox pattern. Fields: `aggregate_id`, `aggregate_type`, `event_type`, `payload` (JSONB), `status` (PENDING/SENT). No publisher is currently wired — the table is ready for a background processor that publishes events to Kafka.

---

### dto package

| DTO | Type | Used by | Key fields |
|-----|------|---------|-----------|
| `JobPostingRequest` | Request | `POST /api/job-postings` | title (required), seniority (required), applicationDeadline (required, future), all rich fields optional |
| `JobPostingUpdateRequest` | Request | `PUT /api/job-postings/{id}` | Same fields as request |
| `StatusUpdateRequest` | Request | `PATCH /api/job-postings/{id}/status` | `status` (DRAFT/PUBLISHED/CLOSED) |
| `DemandSnapshot` | Request | `POST /api/job-postings/from-demand` | demandId, title (required), level, skills[], locationCity, locationState, locationCountry, department, targetDate (required, future) |
| `JobPostingResponse` | Response | All admin job APIs | 29-field record: id, demandId, title, slug, status, description, requirements, responsibilities, benefits, employmentType, workMode, seniority, location, locationCity, locationState, locationCountry, department, jobCategory, salaryMin, salaryMax, currency, showSalary, metaTitle, metaDescription, applicationDeadline, applicationsCount, publishedAt, createdAt, updatedAt |
| `PublicJobResponse` | Response | Public job API | Same fields as above — used to avoid leaking admin-only fields |
| `PagedResponse<T>` | Response | Paginated list APIs | content[], page, size, totalElements, totalPages, last |
| `ApplicationIntakeRequest` | Request | `POST /apply` | candidateName (required), candidateEmail (required), candidatePhone, coverLetter, referralCode |
| `ApplicationIntakeResponse` | Response | Apply confirmation | id, jobPostingId, candidateName, candidateEmail, candidatePhone, resumeUrl, status, source, appliedAt |
| `JobPostingChannelResponse` | Response | Channel APIs | id, jobPostingId, channelName, channelUrl, status, errorMessage, publishedAt, unpublishedAt, lastUpdatedAt, expiresAt |
| `JobPostingAnalyticsResponse` | Response | Analytics API | id, title, slug, channels[] (per-channel counts), totalViews, totalClicks, totalApplyStarts, totalApplyCompletions |
| `ChannelAnalyticsDto` | Embedded | Analytics response | channel, views, clicks, applyStarts, applyCompletions |
| `JobReferralRequest` | Request | `POST /api/referrals` | jobPostingId, referrerId, referredCandidateName, referredCandidateEmail, referralCode (optional), notes |
| `JobReferralResponse` | Response | Referral APIs | id, jobPostingId, referrerId, referredCandidateName, referredCandidateEmail, referralCode, status, notes, referredAt, referralUrl |
| `HandoffStatusResponse` | Response | Handoff admin API | All HandoffRecord fields as a record |
| `Team2ApplicationPayload` | Internal | Sent to Team 2 | firstName, lastName, email, phone, source, resumeUrl, jobSlug, jobTitle, appliedAt, applicationIntakeId |
| `AuthDto` | Nested | Auth endpoints | `RegisterRequest` (name, email, password), `LoginRequest` (email, password), `AuthResponse` (token, email, role, userId, name) |
| `ProfileDto` | Request/Response | Profile endpoints | fullName, phone, bio, professionalTitle, skills[], preferredLocations, salaryExpectation, workMode, notification preferences |
| `ApiResponse<T>` | Response wrapper | Auth + profile + application controllers | status ("ok"/"error"), message, data |

---

### exception package

#### ResourceNotFoundException.java

Thrown when a requested entity does not exist (job by ID/slug, referral by code, handoff by ID).  
HTTP response: **404 Not Found**

#### ConflictException.java

Thrown when a unique constraint is violated (duplicate application, duplicate email on register).  
HTTP response: **409 Conflict**

#### GlobalExceptionHandler.java

`@RestControllerAdvice` — catches exceptions globally and returns structured JSON responses.

| Exception | HTTP Status | Response |
|-----------|-------------|---------|
| `ResourceNotFoundException` | 404 | `{status, error: "Not Found", message}` |
| `ConflictException` | 409 | `{status, error: "Conflict", message}` |
| `MethodArgumentNotValidException` | 400 | `{status, error: "Validation Failed", message}` |
| `IllegalArgumentException` | 400 | `{status, error: "Bad Request", message}` |
| `Exception` (catch-all) | 500 | `{status, error: "Internal Server Error", message}` |

#### ErrorResponse.java

The record returned by `GlobalExceptionHandler`: `{int status, String error, String message}`.

---

### event package

#### DemandEventConsumer.java

`@KafkaListener(topics = "demand-opened", groupId = "market-presence-group")` — listens for Team 1's demand opened events. On receiving a `DemandSnapshot` payload, calls `JobPostingService.createFromDemand()`.

**Current state:** The `@KafkaListener` is registered and functional but no Kafka broker runs locally by default. To test it: start a Kafka broker (`docker run --rm -p 9092:9092 apache/kafka:latest`), publish a message to topic `demand-opened`, and the consumer will pick it up.

#### KafkaEvents.java

Defines four event shapes as inner static Lombok classes:
- `ApplicationSubmittedEvent` — ready to publish when an application is saved (not currently published by any service)
- `ApplicationStatusUpdatedEvent` — ready for status change notifications
- `JobViewedEvent` — ready for view events
- `EmailNotificationEvent` — ready for email queue

These are the event contracts Team 3 would publish to Kafka for other teams to consume. They are defined but not yet wired to a publisher.

---

### util package

#### JwtUtil.java

Generates and validates JWT tokens using HMAC-SHA256 and the secret key from `app.jwt.secret`.

- `generateToken(userDetails)` — creates a signed token with subject=email, expiry=24h
- `isTokenValid(token, userDetails)` — validates signature and expiry
- `extractUsername(token)` — decodes the subject claim (email)

**Note on secret key:** The dev secret `"404E635266..."` is committed to `application.yml`. For production, inject via environment variable:
```bash
export APP_JWT_SECRET="your-production-secret-minimum-32-chars"
```

---

## 7. Frontend Overview

### Path

```
frontend/frontend/
```

### Technology stack

| Technology | Version | Role |
|-----------|---------|------|
| React | 19.2.6 | UI framework |
| TypeScript | 6.0.2 | Type safety |
| Vite | 8.0.12 | Build tool + dev server |
| TailwindCSS | 3.4.19 | Utility-first CSS |
| React Router DOM | 7.16.0 | Client-side routing |
| TanStack React Query | 5.100.14 | Server state + caching |
| Zustand | 5.0.14 | Client state (auth) |
| Axios | 1.16.1 | HTTP client |
| React Hook Form | 7.77.0 | Form management |
| Zod | 4.4.3 | Schema validation |
| Lucide React | 1.17.0 | Icon library |

### Ports

- **Dev server:** `http://localhost:5173` (run `npm run dev`)
- **Preview (built):** `http://localhost:4173` (run `npm run preview`) — used by the axe-core accessibility scan

### Environment file: `.env.local`

```
VITE_API_URL=http://localhost:8086/api
```

This is the only required env variable. All Axios calls use this as the base URL. If you change the backend port in `application.yml`, update this value to match.

### How frontend talks to backend

`src/api/axios.ts` creates a single Axios instance with:
- `baseURL = VITE_API_URL` (from `.env.local`)
- `timeout = 15000` (15 seconds)
- Request interceptor: reads `forge_token` from localStorage and attaches `Authorization: Bearer <token>` header
- Response interceptor: on 401, clears localStorage and fires `auth:logout` event

---

### src/api

#### axios.ts

The shared Axios instance. Import this in all other API files — never create a new `axios.create()` anywhere else.

#### jobs.api.ts

Public job listing (no auth required):
- `getJobs()` → `GET /public/jobs` → `Job[]`
- `getJobBySlug(slug, channel?)` → `GET /public/jobs/{slug}` → `Job`
- `searchJobsPaginated(filters)` → calls `getJobs()` then filters/paginates in memory (client-side)
- `getDepartments()` → derives unique departments from the full job list
- `filterJobs(jobs, filters)` → pure function for client-side filtering by search text, work_mode, experience_level, employment_type, location_city, department, and sort order

#### admin.api.ts

Admin job management (requires ADMIN/RECRUITER JWT):
- `adminListJobs(params?)` → `GET /job-postings` with pagination params
- `adminGetJob(id)` → `GET /job-postings/{id}`
- `adminCreateJob(payload)` → `POST /job-postings`
- `adminUpdateJob(id, payload)` → `PUT /job-postings/{id}`
- `adminUpdateStatus(id, status)` → `PATCH /job-postings/{id}/status`
- `adminDeleteJob(id)` → `DELETE /job-postings/{id}`
- `adminGetChannels(jobId)` → `GET /job-postings/{jobId}/channels`
- `adminPublishChannel(jobId, channel)` → `POST /job-postings/{jobId}/channels/{channel}/publish`
- `adminUnpublishChannel(jobId, channel)` → `POST /job-postings/{jobId}/channels/{channel}/unpublish`
- `adminGetHandoffs(params?)` → `GET /admin/handoffs`
- `adminRetryHandoff(id)` → `POST /admin/handoffs/{id}/retry`
- `adminRetryAllPending()` → `POST /admin/handoffs/retry-pending`

#### analytics.api.ts

Fire-and-forget analytics events (REQ-JP-05):
- `recordEvent(slug, eventType, channel?)` → `POST /public/jobs/{slug}/events`
- `eventType` is `'CLICK'` or `'APPLY_START'` — the TypeScript type enforces this
- All errors are swallowed — analytics never blocks the user

#### auth.api.ts

Authentication:
- `register(data)` → `POST /api/auth/register`
- `login(data)` → `POST /api/auth/login`
- Both return `ApiResponse<AuthDto.AuthResponse>` with token, email, role, userId, name

#### applications.api.ts

Application submission:
- `submitApplication(slug, formData)` → `POST /public/jobs/{slug}/apply` as multipart/form-data
- Returns `ApplicationIntakeResponse`

#### profile.api.ts

Candidate profile (requires auth):
- `getProfile()` → `GET /api/profile`
- `updateProfile(data)` → `PUT /api/profile`
- `uploadResume(file)` → `POST /api/profile/resume`

---

### src/pages

#### HomePage.tsx

**Route:** `/`  
Shows the branded hero section (using `branding.ts`) and a grid of featured published jobs pulled from `GET /public/jobs`. Includes "Browse All Jobs" CTA.

#### JobsPage.tsx

**Route:** `/jobs`  
The main job listing page. Fetches all published jobs via `useJobs()`. Passes jobs to `JobFilters.tsx` for real-time client-side filtering. Renders a grid of `JobCard.tsx` components. Shows skeleton placeholders while loading.

#### JobDetailsPage.tsx

**Route:** `/jobs/:slug` and `/careers/:slug`  
Fetches a single job via `useJob(slug)`. Uses `useDocumentMeta()` to set the page title, meta description, and inject JSON-LD structured data for SEO. Shows full job detail: description, responsibilities, requirements (rendered as tags), benefits, salary, and an "Apply Now" button. Fires a CLICK analytics event when "Apply Now" is clicked.

#### ApplicationPage.tsx

**Route:** `/jobs/:slug/apply` and `/careers/:slug/apply`  
The 10-step application form rendered via `ApplicationStepper.tsx`. Step 0 accepts a resume file upload — after selection, demo data auto-fills the remaining fields. On final step, `gdprConsent` checkbox is required before submission. On submit, calls `POST /public/jobs/{slug}/apply` as multipart/form-data. Shows `SuccessModal` on success or `AlreadyAppliedModal` if already applied.

#### ApplicationsPage.tsx

**Route:** `/applications`  
Shows "My Applications" — reads application records stored in **localStorage** (not from the backend). Each entry shows the job title, status, and next step. The backend `GET /api/applications/mine` endpoint exists but is not called by this page yet.

#### ProfilePage.tsx

**Route:** `/profile`  
Candidate profile page. Calls `GET /api/profile` when authenticated. Allows profile editing and resume upload. Displays skills, preferred locations, salary expectation, notification preferences.

#### LoginPage.tsx

**Route:** `/login`  
Login form. Calls `POST /api/auth/login`. On success, stores JWT via `authStore.setUser()` and navigates to home. On failure, shows error message.

#### RegisterPage.tsx

**Route:** `/register`  
Registration form. Calls `POST /api/auth/register`. On success, auto-logs in by storing the returned JWT.

#### AdminJobsPage.tsx

**Route:** `/admin/jobs`  
Lists all job postings (any status) via `adminListJobs()`. Shows a table with title, status, location, seniority, applications count, and action buttons (Edit, Publish, Close, Delete). Includes a "Create New Job" button.

#### AdminJobEditorPage.tsx

**Route:** `/admin/jobs/new` and `/admin/jobs/:id/edit`  
Rich form for creating or editing a job posting. Includes all 20+ fields. Calls `adminCreateJob()` or `adminUpdateJob()`. Also embeds `ChannelPanel.tsx` to show and manage channel publish status for the current job.

#### AdminHandoffsPage.tsx

**Route:** `/admin/handoffs`  
Lists Team 2 handoff records via `adminGetHandoffs()`. Shows status (PENDING/SENT/FAILED), candidate email, job title, and timestamp. Provides "Retry" button for individual records and "Retry All Pending" button.

---

### src/components

#### JobCard.tsx

Displays one job posting in the listings grid. Shows: coloured department badge, work mode pill (Remote/Hybrid/On-site), title, location, salary range (formatted with currency symbol), seniority level, and "Apply Now" button. The "Apply Now" button fires a CLICK analytics event before navigating.

#### JobFilters.tsx

The filter sidebar/panel for the jobs list. Controls: text search, work mode dropdown, seniority (experience level) dropdown, department dropdown. All filtering is client-side via `filterJobs()` in `jobs.api.ts`. This is a controlled component — it lifts filter state to `JobsPage`.

#### ResumeUploader.tsx

Step 0 of the application form. A drag-and-drop file input that accepts PDF and DOCX files. After a file is selected, triggers a 1.5-second simulated "AI parse" delay and auto-fills form fields with demo data. The actual resume file is collected and sent as a multipart upload on form submission.

#### ApplicationStepper.tsx

The 10-step application wizard. Manages step navigation (prev/next), collects form data across all steps, assembles the multipart FormData payload, and submits. Steps: Resume Upload, Personal Info, Experience, Education, Skills, Certifications, Projects, Screening Questions, Documents, Review + GDPR Consent.

#### SuccessModal.tsx

Confirmation modal shown after a successful application submission. Shows a success message with the job title and a "View My Applications" link.

#### AlreadyAppliedModal.tsx

Modal shown when a candidate tries to apply to a job they've already applied to (409 response from backend, or localStorage check). Shows a friendly message and a "View My Applications" link.

#### Navbar.tsx

Top navigation bar. Shows the Forge AI logo, navigation links, and auth state (Login/Register buttons when unauthenticated; user avatar + Logout when authenticated). Responds to `auth:logout` event from the Axios interceptor.

#### Sidebar.tsx

Left navigation sidebar used in the `CandidateLayout`. Contains links to Jobs, Applications, Profile, and Admin sections.

#### Skeleton.tsx

CSS skeleton placeholder components used while data is loading. Prevents layout shift during React Query's `isLoading` state.

#### ChannelPanel.tsx

Admin component embedded in `AdminJobEditorPage`. Shows the current status of all three channels (CAREERS_PORTAL, LINKEDIN, INDEED) for a job posting and provides Publish/Unpublish buttons. Uses `adminGetChannels()`, `adminPublishChannel()`, `adminUnpublishChannel()`.

#### MarkdownToolbar.tsx

Toolbar with buttons for common Markdown formatting (Bold, Italic, Bullet list, Code block). Used in the job description textarea in `AdminJobEditorPage`.

---

### src/features/jobs

#### hooks/useJobs.ts

`useQuery` hook that calls `getJobs()` and caches the result with React Query. Key: `['jobs']`. Used by `JobsPage` and `HomePage`.

#### hooks/useJob.ts

`useQuery` hook that calls `getJobBySlug(slug)` for a single job. Key: `['job', slug]`. Used by `JobDetailsPage` and `ApplicationPage`.

#### services/jobs.service.ts

Thin service layer wrapping `jobs.api.ts` functions. Exists as an architectural separation point — add caching logic or mock adapters here without touching the API layer.

#### types/job.types.ts

TypeScript interfaces for the entire frontend:
- `Job` — 27 fields mapping to `PublicJobResponse` JSON (snake_case field names matching backend response)
- `JobFilters` — search, work_mode, experience_level, employment_type, location_city, department, sort
- `Application` — for the localStorage-based applications list
- `ApplicationForm` — all 30+ fields of the multi-step application form
- `ExperienceForm`, `EducationForm`, `SkillForm`, `CertificationForm`, `ProjectForm` — sub-form types
- `CandidateProfile` — profile page type

---

### src/routes/AppRoutes.tsx

Defines all React Router v7 routes:

| Route | Component | Auth? |
|-------|-----------|-------|
| `/` | `HomePage` | No |
| `/jobs` | `JobsPage` | No |
| `/jobs/:slug` | `JobDetailsPage` | No |
| `/jobs/:slug/apply` | `ApplicationPage` | Wrapped in `ProtectedRoute` |
| `/careers` | Redirects to `/jobs` | No |
| `/careers/:slug` | `JobDetailsPage` | No |
| `/careers/:slug/apply` | `ApplicationPage` | Wrapped in `ProtectedRoute` |
| `/applications` | `ApplicationsPage` | Wrapped in `ProtectedRoute` |
| `/profile` | `ProfilePage` | Wrapped in `ProtectedRoute` |
| `/admin` | Redirects to `/admin/jobs` | No |
| `/admin/jobs` | `AdminJobsPage` | No |
| `/admin/jobs/new` | `AdminJobEditorPage` | No |
| `/admin/jobs/:id/edit` | `AdminJobEditorPage` | No |
| `/admin/handoffs` | `AdminHandoffsPage` | No |
| `/login` | `LoginPage` | No |
| `/register` | `RegisterPage` | No |
| `*` | Redirects to `/` | No |

**Note:** `ProtectedRoute` currently renders children directly without checking auth (`return <>{children}</>`). The redirect-to-login logic is therefore disabled. The apply button on job cards also navigates without checking auth first. For a production deployment, add an auth check inside `ProtectedRoute`.

---

### src/store

#### authStore.ts

Zustand store managing authentication state. State: `user: AuthUser | null`, `isAuthenticated: boolean`.

- `setUser(user)` — saves token to `localStorage.forge_token`, user JSON to `localStorage.forge_user`, sets `isAuthenticated: true`
- `logout()` — clears localStorage keys, sets `isAuthenticated: false`
- `loadFromStorage()` — restores auth state from localStorage on app load (called in `main.tsx` or `App.tsx`)

The JWT token in localStorage is automatically attached to every Axios request by the interceptor in `axios.ts`.

#### localStorage.ts

Helpers for the applications tracking list stored in localStorage (key: `forge_applications`). Functions: `getStoredApplications()`, `saveApplication(application)`, `hasApplied(jobId)`. This is used by `ApplicationsPage` and the duplicate-check logic in `ApplicationPage`.

---

### src/data/branding.ts

Static employer branding configuration (REQ-JP-10). Exports a single `branding: BrandingConfig` object with:
- `companyName`: "Forge AI"
- `tagline`: "Engineering the talent supply chain — intelligently."
- `missionStatement`: company mission text
- `aboutUs`: paragraph about Forge AI
- `cultureValues`: four values (Engineering First, Radical Transparency, Global Perspective, Peer-Led Growth)
- `benefits`: six items (compensation, learning budget, flexible work, health, equity, certifications)
- `employeeStories`: three testimonial cards

**Why it's static:** The PES spec requires admin/HR to be able to edit this without a developer. The current implementation is a TypeScript file. A production version would call `GET /api/admin/branding` and allow editing via an admin UI. The types are already defined in this file — swapping the data source is a one-file change.

---

### src/lib

#### formatDate.ts

Utility function that formats an ISO date string (`2026-06-02T10:00:00Z`) to a human-readable string (`Jun 2, 2026`) using `Intl.DateTimeFormat`.

#### formatSalary.ts

Formats salary for display. For INR: converts raw number (e.g. 1800000) to "₹18 L" (lakhs). For USD: formats as "$80k". Respects the `currency` field from the job posting.

#### useDocumentMeta.ts

React hook for per-page SEO metadata (REQ-JP-09). On mount:
- Sets `document.title` to the provided title string
- Creates or updates `<meta name="description">` with the provided description
- Injects `<script type="application/ld+json">` with the provided JSON-LD object

On unmount: restores the previous title, removes the injected tags. Used by `JobDetailsPage.tsx` to inject job-specific structured data.

---

## 8. End-to-End Workflows

### Workflow A: Demand to Job Posting (REQ-JP-01)

**Scenario:** Chennai Team 1 approves a hiring demand and marks it as OPEN_EXTERNAL.

**Steps:**
1. Team 1 publishes a Kafka message to topic `demand-opened` with a `DemandSnapshot` payload.
2. `DemandEventConsumer.onDemandOpened()` receives the message and calls `JobPostingService.createFromDemand(snapshot)`.
3. The service maps fields: `snapshot.level` → `seniority`, `snapshot.skills[]` joined by ", " → `requirements`, `snapshot.targetDate` → `applicationDeadline`, title + city → auto-generated slug.
4. A `JobPosting` is saved with `status=DRAFT` and `demandId` populated.
5. HR logs into `/admin/jobs`, finds the DRAFT, edits the description/responsibilities/benefits, and clicks Publish.

**Demo fallback (no Kafka running):**
```bash
curl -X POST http://localhost:8086/api/job-postings/from-demand \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_JWT>" \
  -d '{
    "demandId": 42,
    "title": "Senior Java Developer",
    "level": "SENIOR",
    "skills": ["Java 17", "Spring Boot", "Kafka"],
    "locationCity": "Bangalore",
    "locationState": "KA",
    "locationCountry": "IN",
    "department": "Engineering",
    "targetDate": "2027-06-30"
  }'
```

---

### Workflow B: Create / Edit Job Posting (REQ-JP-02)

**Scenario:** HR manually creates a new job posting.

**Steps:**
1. Admin navigates to `http://localhost:5173/admin/jobs/new`.
2. `AdminJobEditorPage.tsx` renders the full form.
3. On submit, `adminCreateJob(payload)` calls `POST /api/job-postings`.
4. `JobPostingController.create()` calls `JobPostingService.create(request)`.
5. Service validates: title required, deadline must be future, salaryMin ≤ salaryMax.
6. Service generates a URL slug: "React Frontend Engineer" + "Bangalore" → `react-frontend-engineer-bangalore`. If slug exists, appends `-2`, `-3` etc.
7. `JobPosting` is saved with `status=DRAFT`.
8. Admin edits the posting at `/admin/jobs/{id}/edit` → `adminUpdateJob()` → `PUT /api/job-postings/{id}`.
9. Admin publishes: `adminUpdateStatus(id, 'PUBLISHED')` → `PATCH /api/job-postings/{id}/status`.

---

### Workflow C: Publish Job to Channels (REQ-JP-03, REQ-JP-04)

**Scenario:** Admin publishes a job and broadcasts it to all channels.

**Steps:**
1. `PATCH /api/job-postings/{id}/status` with `{"status":"PUBLISHED"}`.
2. `JobPostingService.updateStatus()` sets `publishedAt=now`, status=PUBLISHED.
3. Automatically calls `channelService.upsertCareersPortalChannel(id, slug)` → creates/updates CAREERS_PORTAL channel to LIVE at URL `http://localhost:5173/jobs/{slug}`.
4. Admin clicks "Publish to LinkedIn" in `ChannelPanel.tsx` → `POST /api/job-postings/{id}/channels/LINKEDIN/publish`.
5. Since `app.channels.linkedin.api-configured=false`: channel status=PENDING, errorMessage contains the copyable job URL for manual LinkedIn posting.
6. Admin clicks "Publish to Indeed" → `POST /api/job-postings/{id}/channels/INDEED/publish`.
7. Indeed channel status=LIVE immediately (the XML feed at `/api/public/jobs/feed.xml` always includes published jobs).
8. **Auto-unpublish:** `ChannelExpiryScheduler` runs hourly. If `applicationDeadline` is before today: sets status=CLOSED, calls `unpublishAllLiveChannels()` → all LIVE channels → UNPUBLISHED.

---

### Workflow D: Public Careers Portal Browsing (REQ-JP-06)

**Scenario:** A candidate browses jobs from their browser.

**Steps:**
1. Candidate opens `http://localhost:5173/jobs`.
2. `useJobs()` fires React Query → `GET /api/public/jobs` → returns all PUBLISHED jobs as `PublicJobResponse[]`.
3. Jobs are cached by React Query (staleTime defaults).
4. `JobFilters.tsx` updates filter state on each keystroke/selection.
5. `filterJobs(jobs, filters)` runs client-side: filters by search text (matches title, department, city, category, description, requirements), work_mode, experience_level, employment_type, location_city, department. Sorts by latest/salary_high/salary_low/az.
6. Filtered jobs render as `JobCard.tsx` components.
7. Candidate searches "engineer" → only jobs containing "engineer" in any text field remain visible — instant, no API call.

---

### Workflow E: Job Details and SEO (REQ-JP-09)

**Scenario:** Candidate opens a job detail page. Google indexes it.

**Steps:**
1. Candidate navigates to `/careers/react-frontend-engineer-bangalore` (canonical PES URL) or `/jobs/react-frontend-engineer-bangalore` (legacy).
2. `JobDetailsPage.tsx` extracts slug from URL params, calls `useJob(slug)`.
3. `GET /api/public/jobs/react-frontend-engineer-bangalore` → returns `PublicJobResponse` + records VIEW event.
4. `useDocumentMeta()` sets:
   - `document.title = "React Frontend Engineer | Forge AI Careers"`
   - `<meta name="description" content="..." />`
   - `<script type="application/ld+json">{ "@context": "https://schema.org", "@type": "JobPosting", ... }</script>`
5. Full job detail renders: description, responsibilities, requirements (as tag chips), benefits, salary, work mode, deadline.
6. When Google crawls this URL it finds the JSON-LD structured data and can display the job in Google Jobs search results.

---

### Workflow F: Apply Flow (REQ-JP-07)

**Scenario:** Candidate applies for a job.

**Steps:**
1. Candidate clicks "Apply Now" on `JobDetailsPage`. Frontend fires `recordEvent(slug, 'CLICK')` → `POST /public/jobs/{slug}/events`.
2. Navigates to `/careers/{slug}/apply`. Frontend fires `recordEvent(slug, 'APPLY_START')`.
3. `ApplicationPage.tsx` checks localStorage — if already applied, shows `AlreadyAppliedModal`.
4. `ApplicationStepper.tsx` renders Step 0 (Resume Upload).
5. Candidate uploads a PDF → `ResumeUploader.tsx` → 1.5s delay → auto-fills demo data in remaining steps.
6. Candidate fills all 10 steps. On Step 9 (Review), checks GDPR consent checkbox.
7. On submit: `ApplicationStepper` builds a `FormData` object:
   - Part `application`: JSON `{candidateName, candidateEmail, candidatePhone, coverLetter, referralCode}`
   - Part `resume`: the PDF file
8. `POST /api/public/jobs/{slug}/apply` (multipart/form-data).
9. Backend: finds job by slug → duplicate check → stores resume file → saves `ApplicationIntake` → increments applicationsCount → sends email (log only) → resolves referral → triggers handoff → records APPLY_COMPLETE.
10. Frontend: saves application to localStorage → shows `SuccessModal`.

**Duplicate block:**
- Backend: checks `existsByJobPostingIdAndCandidateEmail(jobId, email)` → returns 409 with message.
- Frontend: also checks localStorage before opening the form — shows `AlreadyAppliedModal` immediately without making a request.

---

### Workflow G: Analytics (REQ-JP-05)

**Scenario:** Team 4 wants to see how many candidates viewed and applied for each job.

**Event firing flow:**
1. `GET /api/public/jobs/{slug}` → backend records VIEW automatically in `PublicJobController`.
2. Candidate clicks "Apply Now" → frontend calls `POST /api/public/jobs/{slug}/events` with `{eventType:"CLICK"}`.
3. Candidate lands on apply page → frontend calls same endpoint with `{eventType:"APPLY_START"}`.
4. Candidate submits application → backend records APPLY_COMPLETE in `ApplicationIntakeService`.

**Query flow (Team 4):**
```bash
# All jobs summary
curl http://localhost:8086/api/analytics/job-postings

# One job detail
curl http://localhost:8086/api/analytics/job-postings/1
```

Response includes per-channel breakdown (CAREERS_PORTAL, LINKEDIN, INDEED) and totals.

---

### Workflow H: Referral Flow (REQ-JP-11)

**Scenario:** An employee shares a referral link with a friend.

**Steps:**
1. Employee calls `POST /api/job-postings/{id}/referrals` with `{"referrerId": 1001}`.
2. `JobReferralService.generateLink()` creates a unique 10-char code (UUID-derived, uppercase).
3. Returns `referralUrl`: `http://localhost:5173/careers/react-frontend-engineer-bangalore?ref=ABC123DEF5`.
4. Employee shares this URL via email/WhatsApp.
5. Candidate opens the URL → `JobDetailsPage` renders normally (the `?ref=` param is preserved in URL).
6. Candidate clicks "Apply Now" → `ApplicationPage` collects the `ref` param from URL.
7. On submit: `referralCode: "ABC123DEF5"` is included in the `application` JSON part.
8. `ApplicationIntakeService.apply()` calls `referralService.resolveSource("ABC123DEF5", jobId)`.
9. Service verifies code exists and belongs to this job → returns "REFERRAL".
10. `ApplicationIntake.source = "REFERRAL"` is saved.
11. `referralService.markApplied()` updates the referral record: status=APPLIED, fills in candidate name/email.

**Gap:** No outcome tracking after APPLIED status. Hire/reject outcome would need a new status field and a backend endpoint.

---

### Workflow I: Team 2 Handoff (REQ-JP-08)

**Scenario:** An application is submitted and must be forwarded to Team 2.

**Steps:**
1. `ApplicationIntakeService.apply()` saves the `ApplicationIntake` record.
2. Calls `handoffService.createAndAttempt(intake, job)` at the end (after DB save succeeds).
3. `ApplicationHandoffService` builds a `HandoffRecord` with status=PENDING and saves it.
4. Checks if `app.team2.api-base-url` is set.
   - **With stub URL configured (default):** `HttpTeam2Client.send()` posts `Team2ApplicationPayload` to `http://localhost:8086/api/stub/team2/applications/intake`. `Team2StubController` responds with `{"id":"T2-XXXXXXXX","status":"RECEIVED"}`. Record status → SENT.
   - **With empty URL:** Logs a warning. Record remains PENDING. Can be retried later.
   - **With real Team 2 URL:** Same flow. Record status → SENT with real `team2ResponseId`.
5. All exceptions are caught — if Team 2 is down, the apply flow is unaffected. Record status → FAILED, `errorMessage` populated.

**Admin retry:**
```bash
# View pending handoffs
curl http://localhost:8086/api/admin/handoffs?status=PENDING \
  -H "Authorization: Bearer <ADMIN_JWT>"

# Retry one
curl -X POST http://localhost:8086/api/admin/handoffs/1/retry \
  -H "Authorization: Bearer <ADMIN_JWT>"

# Retry all pending
curl -X POST http://localhost:8086/api/admin/handoffs/retry-pending \
  -H "Authorization: Bearer <ADMIN_JWT>"
```

---

## 9. API Reference

Base URL: `http://localhost:8086/api`

Authentication: `Authorization: Bearer <JWT>` for protected endpoints.

---

### POST /api/job-postings

**Purpose:** Create a new job posting (starts as DRAFT)  
**Auth:** ROLE_ADMIN or ROLE_RECRUITER  
**Requirement:** REQ-JP-02

```bash
curl -X POST http://localhost:8086/api/job-postings \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "title": "Platform Engineer",
    "description": "Build and maintain the CI/CD pipeline...",
    "seniority": "SENIOR",
    "locationCity": "Bangalore",
    "locationState": "KA",
    "locationCountry": "IN",
    "department": "Infrastructure",
    "employmentType": "FULL_TIME",
    "workMode": "HYBRID",
    "salaryMin": 2000000,
    "salaryMax": 2800000,
    "currency": "INR",
    "showSalary": true,
    "applicationDeadline": "2027-06-30"
  }'
```

**Success 201:**
```json
{
  "id": 9,
  "demandId": null,
  "title": "Platform Engineer",
  "slug": "platform-engineer-bangalore",
  "status": "DRAFT",
  "seniority": "SENIOR",
  "locationCity": "Bangalore",
  "salaryMin": 2000000,
  "salaryMax": 2800000,
  "currency": "INR",
  "applicationsCount": 0,
  "createdAt": "2026-06-03T10:00:00Z"
}
```

**Error 400:** title blank, deadline in past, salaryMin > salaryMax

---

### POST /api/job-postings/from-demand

**Purpose:** Pre-fill a DRAFT posting from demand data (REQ-JP-01 fallback)  
**Auth:** ROLE_ADMIN or ROLE_RECRUITER

```bash
curl -X POST http://localhost:8086/api/job-postings/from-demand \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{
    "demandId": 42,
    "title": "Senior Java Developer",
    "level": "SENIOR",
    "skills": ["Java 17", "Spring Boot", "Kafka", "PostgreSQL"],
    "locationCity": "Bangalore",
    "locationState": "KA",
    "locationCountry": "IN",
    "department": "Engineering",
    "targetDate": "2027-06-30"
  }'
```

**Success 201:** Same as above. `requirements` = "Java 17, Spring Boot, Kafka, PostgreSQL". `status` = "DRAFT".

---

### GET /api/job-postings

**Purpose:** List all job postings with filters and pagination  
**Auth:** ROLE_ADMIN or ROLE_RECRUITER

```bash
# All jobs
curl http://localhost:8086/api/job-postings \
  -H "Authorization: Bearer <TOKEN>"

# Filters + pagination
curl "http://localhost:8086/api/job-postings?status=PUBLISHED&seniority=SENIOR&page=0&size=5&sortBy=title&sortDir=asc" \
  -H "Authorization: Bearer <TOKEN>"
```

**Success 200:**
```json
{
  "content": [...],
  "page": 0,
  "size": 20,
  "totalElements": 8,
  "totalPages": 1,
  "last": true
}
```

---

### GET /api/job-postings/{id}

**Purpose:** Get one job posting by database ID  
**Auth:** ROLE_ADMIN or ROLE_RECRUITER

```bash
curl http://localhost:8086/api/job-postings/1 -H "Authorization: Bearer <TOKEN>"
```

**Error 404:** `{"status":404,"error":"Not Found","message":"Job posting not found with id: 1"}`

---

### PUT /api/job-postings/{id}

**Purpose:** Replace all fields of a job posting  
**Auth:** ROLE_ADMIN or ROLE_RECRUITER

Body: same as `JobPostingRequest`. All optional fields can be null.

---

### PATCH /api/job-postings/{id}/status

**Purpose:** Change status only (DRAFT → PUBLISHED → CLOSED)  
**Auth:** ROLE_ADMIN or ROLE_RECRUITER  
**Requirement:** REQ-JP-03, REQ-JP-04

```bash
# Publish
curl -X PATCH http://localhost:8086/api/job-postings/1/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"status": "PUBLISHED"}'

# Close
curl -X PATCH http://localhost:8086/api/job-postings/1/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"status": "CLOSED"}'
```

Publishing automatically creates a CAREERS_PORTAL channel record (LIVE). Closing automatically unpublishes all LIVE channels.

---

### DELETE /api/job-postings/{id}

**Purpose:** Delete permanently  
**Auth:** ROLE_ADMIN or ROLE_RECRUITER

```bash
curl -X DELETE http://localhost:8086/api/job-postings/1 -H "Authorization: Bearer <TOKEN>"
```

**Success:** 204 No Content. **Error:** 404 if not found.

---

### GET /api/public/jobs

**Purpose:** List all PUBLISHED jobs (public, no auth)  
**Requirement:** REQ-JP-06

```bash
curl http://localhost:8086/api/public/jobs
```

Returns `PublicJobResponse[]` — all PUBLISHED jobs with full details.

---

### GET /api/public/jobs/{slug}

**Purpose:** Get one PUBLISHED job by slug; auto-records VIEW event  
**Auth:** None  
**Requirement:** REQ-JP-05, REQ-JP-09

```bash
curl "http://localhost:8086/api/public/jobs/react-frontend-engineer-bangalore"
curl "http://localhost:8086/api/public/jobs/react-frontend-engineer-bangalore?channel=LINKEDIN"
```

---

### POST /api/public/jobs/{slug}/apply

**Purpose:** Submit candidate application with optional resume file  
**Auth:** None (public)  
**Requirement:** REQ-JP-07  
**Content-Type:** multipart/form-data

```bash
curl -X POST http://localhost:8086/api/public/jobs/react-frontend-engineer-bangalore/apply \
  -F 'application={"candidateName":"Priya Sharma","candidateEmail":"priya@example.com","candidatePhone":"+91 98765 43210","coverLetter":"4 years React experience."};type=application/json' \
  -F 'resume=@/path/to/resume.pdf;type=application/pdf'
```

**Success 201:**
```json
{
  "id": 5,
  "jobPostingId": 1,
  "candidateName": "Priya Sharma",
  "candidateEmail": "priya@example.com",
  "candidatePhone": "+91 98765 43210",
  "resumeUrl": "uploads/resumes/react-frontend-engineer-bangalore_1234567890.pdf",
  "status": "SUBMITTED",
  "source": "CAREERS_PORTAL",
  "appliedAt": "2026-06-03T10:00:00Z"
}
```

**Duplicate 409:**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "You have already applied for this position. Check your application status in My Applications."
}
```

**Invalid file 400:** `{"status":400,"error":"Bad Request","message":"Only PDF and DOCX resumes are accepted"}`

---

### POST /api/public/jobs/{slug}/events

**Purpose:** Record CLICK or APPLY_START event from the frontend  
**Auth:** None  
**Requirement:** REQ-JP-05  
**Always returns:** 204 No Content (unknown slugs silently ignored)

```bash
curl -X POST http://localhost:8086/api/public/jobs/react-frontend-engineer-bangalore/events \
  -H "Content-Type: application/json" \
  -d '{"eventType": "CLICK", "channel": "CAREERS_PORTAL"}'
```

---

### GET /api/analytics/job-postings

**Purpose:** Aggregated analytics for all job postings (Team 4 API)  
**Auth:** None  
**Requirement:** REQ-JP-05

```bash
curl http://localhost:8086/api/analytics/job-postings
```

**Success 200:**
```json
[
  {
    "id": 1,
    "title": "React Frontend Engineer",
    "slug": "react-frontend-engineer-bangalore",
    "channels": [
      {"channel":"CAREERS_PORTAL","views":12,"clicks":5,"applyStarts":3,"applyCompletions":2},
      {"channel":"LINKEDIN","views":0,"clicks":0,"applyStarts":0,"applyCompletions":0},
      {"channel":"INDEED","views":0,"clicks":0,"applyStarts":0,"applyCompletions":0}
    ],
    "totalViews": 12,
    "totalClicks": 5,
    "totalApplyStarts": 3,
    "totalApplyCompletions": 2
  }
]
```

---

### GET /api/analytics/job-postings/{id}

Same as above but for a single job. `404` if job not found.

```bash
curl http://localhost:8086/api/analytics/job-postings/1
```

---

### POST /api/job-postings/{id}/channels/{channel}/publish

**Purpose:** Publish to CAREERS_PORTAL, LINKEDIN, or INDEED  
**Auth:** ROLE_ADMIN or ROLE_RECRUITER  
**Requirement:** REQ-JP-03

```bash
# Publish to careers portal
curl -X POST http://localhost:8086/api/job-postings/1/channels/CAREERS_PORTAL/publish \
  -H "Authorization: Bearer <TOKEN>"

# Publish to LinkedIn (returns PENDING since API not configured)
curl -X POST http://localhost:8086/api/job-postings/1/channels/LINKEDIN/publish \
  -H "Authorization: Bearer <TOKEN>"

# Publish to Indeed (returns LIVE — XML feed always available)
curl -X POST http://localhost:8086/api/job-postings/1/channels/INDEED/publish \
  -H "Authorization: Bearer <TOKEN>"
```

**Success 201:**
```json
{
  "id": 3,
  "jobPostingId": 1,
  "channelName": "LINKEDIN",
  "channelUrl": "http://localhost:5173/jobs/react-frontend-engineer-bangalore",
  "status": "PENDING",
  "errorMessage": "LinkedIn API is not configured. To post manually: copy this URL and use it in LinkedIn's 'Create a Job Post' flow: http://localhost:5173/jobs/react-frontend-engineer-bangalore",
  "publishedAt": null
}
```

---

### POST /api/job-postings/{id}/channels/{channel}/unpublish

```bash
curl -X POST http://localhost:8086/api/job-postings/1/channels/LINKEDIN/unpublish \
  -H "Authorization: Bearer <TOKEN>"
```

Returns channel record with `status: "UNPUBLISHED"` and `unpublishedAt` timestamp.

---

### GET /api/job-postings/{id}/channels

```bash
curl http://localhost:8086/api/job-postings/1/channels \
  -H "Authorization: Bearer <TOKEN>"
```

Returns array of all channel records for the job.

---

### GET /api/public/jobs/feed.xml

**Purpose:** Indeed-compatible XML job feed (REQ-JP-03 fallback)  
**Auth:** None  
**Produces:** application/xml

```bash
curl http://localhost:8086/api/public/jobs/feed.xml
```

Submit this URL to Indeed's Job Distributor portal. Indeed polls it periodically to sync jobs.

---

### POST /api/job-postings/{id}/referrals

**Purpose:** Generate a shareable referral URL  
**Auth:** Any authenticated user  
**Requirement:** REQ-JP-11

```bash
curl -X POST http://localhost:8086/api/job-postings/1/referrals \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"referrerId": 1001}'
```

**Success 201:**
```json
{
  "id": 3,
  "jobPostingId": 1,
  "referrerId": 1001,
  "referredCandidateName": "PENDING",
  "referredCandidateEmail": "pending@referral.tbd",
  "referralCode": "ABC123DEF5",
  "status": "PENDING",
  "notes": "Generated link — no candidate yet",
  "referralUrl": "http://localhost:5173/careers/react-frontend-engineer-bangalore?ref=ABC123DEF5"
}
```

---

### GET /api/job-postings/{id}/referrals

```bash
curl http://localhost:8086/api/job-postings/1/referrals \
  -H "Authorization: Bearer <TOKEN>"
```

---

### GET /api/referrals/{code}

```bash
curl http://localhost:8086/api/referrals/REF-FORGE-REACT-001
```

---

### GET /api/admin/handoffs

```bash
# All handoffs
curl http://localhost:8086/api/admin/handoffs \
  -H "Authorization: Bearer <ADMIN_JWT>"

# Only pending
curl "http://localhost:8086/api/admin/handoffs?status=PENDING" \
  -H "Authorization: Bearer <ADMIN_JWT>"
```

---

### POST /api/admin/handoffs/{id}/retry

```bash
curl -X POST http://localhost:8086/api/admin/handoffs/1/retry \
  -H "Authorization: Bearer <ADMIN_JWT>"
```

---

### POST /api/auth/register

```bash
curl -X POST http://localhost:8086/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"name":"Priya Sharma","email":"priya@forge.ai","password":"securepassword123"}'
```

**Success 200:**
```json
{
  "status": "ok",
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiJ9...",
    "email": "priya@forge.ai",
    "role": "CANDIDATE",
    "userId": "uuid-here",
    "name": "Priya Sharma"
  }
}
```

---

### POST /api/auth/login

```bash
curl -X POST http://localhost:8086/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"priya@forge.ai","password":"securepassword123"}'
```

---

## 10. Database Guide

### Connection

```bash
PGPASSWORD=1234 psql -U sanjay -d forge_market_presence -h localhost
```

### Tables at a glance

| Table | Purpose |
|-------|---------|
| `job_postings` | All job postings (DRAFT/PUBLISHED/CLOSED) |
| `application_intakes` | Candidate applications from public apply flow |
| `job_posting_channels` | Channel publish/unpublish records |
| `job_referrals` | Referral links and their status |
| `job_posting_analytic_events` | View/click/apply event log (append-only) |
| `application_handoffs` | Team 2 handoff attempts |
| `users` | Registered user accounts |
| `candidate_profiles` | Candidate profile data |
| `candidate_skills` | Skills linked to a candidate profile |
| `external_candidates` | External applicant personal info |
| `candidate_applications` | Richer application records (authenticated flow) |
| `notification_logs` | Email send log |
| `outbox_events` | Transactional outbox (ready, not yet published) |
| `flyway_schema_history` | Flyway migration history (auto-managed) |

### Important enums / status values

```
job_postings.status:           DRAFT, PUBLISHED, CLOSED
application_intakes.status:    SUBMITTED
application_intakes.source:    CAREERS_PORTAL, REFERRAL
job_posting_channels.status:   DRAFT, PENDING, LIVE, FAILED, UNPUBLISHED
job_posting_channels.channel_name: CAREERS_PORTAL, LINKEDIN, INDEED
job_referrals.status:          PENDING, APPLIED
application_handoffs.status:   PENDING, SENT, FAILED
users.role:                    CANDIDATE, ADMIN, RECRUITER
job_posting_analytic_events.event_type: VIEW, CLICK, APPLY_START, APPLY_COMPLETE
```

### Useful SQL queries

```sql
-- List all jobs
SELECT id, title, status, slug, seniority, location_city, applications_count
FROM job_postings
ORDER BY created_at DESC;

-- Only published jobs
SELECT id, title, slug, published_at
FROM job_postings
WHERE status = 'PUBLISHED'
ORDER BY published_at DESC;

-- All applications
SELECT ai.id, jp.title, ai.candidate_name, ai.candidate_email, ai.status, ai.source, ai.applied_at
FROM application_intakes ai
JOIN job_postings jp ON jp.id = ai.job_posting_id
ORDER BY ai.applied_at DESC;

-- Check duplicate: has this email applied for this job?
SELECT COUNT(*) FROM application_intakes
WHERE job_posting_id = 1 AND candidate_email = 'priya@example.com';

-- Channel status for all jobs
SELECT jp.title, ch.channel_name, ch.status, ch.published_at
FROM job_posting_channels ch
JOIN job_postings jp ON jp.id = ch.job_posting_id
ORDER BY jp.id, ch.channel_name;

-- Analytics summary
SELECT jp.title, ae.event_type, ae.channel_name, COUNT(*) as count
FROM job_posting_analytic_events ae
JOIN job_postings jp ON jp.id = ae.job_posting_id
GROUP BY jp.title, ae.event_type, ae.channel_name
ORDER BY jp.title, ae.event_type;

-- Referral records
SELECT jr.referral_code, jp.title, jr.referred_candidate_name, jr.status, jr.referred_at
FROM job_referrals jr
JOIN job_postings jp ON jp.id = jr.job_posting_id;

-- Handoff status
SELECT ah.id, ah.status, ah.candidate_email, ah.job_title, ah.source, ah.team2_response_id, ah.attempted_at
FROM application_handoffs ah
ORDER BY ah.created_at DESC;

-- Inspect a specific candidate's application
SELECT ai.*, jp.title as job_title
FROM application_intakes ai
JOIN job_postings jp ON jp.id = ai.job_posting_id
WHERE ai.candidate_email = 'priya@example.com';

-- Seed data jobs (what DataSeeder inserted)
SELECT id, slug, status FROM job_postings WHERE demand_id IS NOT NULL ORDER BY id;
```

---

## 11. Testing Guide

### Backend tests

**Location:** `backend/src/test/java/com/griddynamics/forge/market_presence_service/`

**Test classes and what they test:**

| Class | Type | Covers |
|-------|------|--------|
| `MarketPresenceServiceApplicationTests` | Context load | Spring context loads without error |
| `JobPostingControllerTest` | MockMvc | All `/api/job-postings` endpoints, HTTP status codes, JSON response fields, validation errors |
| `PublicJobControllerTest` | MockMvc | `/api/public/jobs` endpoints |
| `ApplicationIntakeControllerTest` | MockMvc | `/api/public/jobs/{slug}/apply` multipart endpoint |
| `JobAnalyticsControllerTest` | MockMvc | `/api/public/jobs/{slug}/events` and `/api/analytics/job-postings` |
| `IndeedFeedControllerTest` | MockMvc | `/api/public/jobs/feed.xml` XML output |
| `HandoffStatusControllerTest` | MockMvc | `/api/admin/handoffs` endpoints |
| `JobPostingServiceTest` | Mockito | `create()`, `createFromDemand()`, `getById()`, `getBySlug()`, `updateStatus()`, `delete()`, slug collision, salary validation |
| `AnalyticsServiceTest` | Mockito | `record()`, `getAnalytics()`, `getAllAnalytics()` |
| `ApplicationHandoffServiceTest` | Mockito | PENDING → SENT → FAILED transitions, retry logic |
| `JobPostingChannelServiceTest` | Mockito | Channel publish/unpublish state transitions |
| `JobReferralServiceTest` | Mockito | Link generation, source resolution, markApplied |

**Run all backend tests:**
```bash
cd market-presence-fullstack/backend
./mvnw test
# Expected: all tests pass, no failures or errors
```

**Debug failing test:**
```bash
# Show full stack traces
./mvnw test -pl . 2>&1 | less

# Run only one test class
./mvnw test -Dtest=JobPostingServiceTest

# Run one specific test method
./mvnw test -Dtest=JobPostingServiceTest#create_savesJobAndReturnsResponse
```

**Surefire report:** After `./mvnw test`, HTML reports are at:
```
backend/target/surefire-reports/
```

### Frontend type-check (not unit tests)

```bash
cd market-presence-fullstack/frontend/frontend
npm run build
# Runs TypeScript compiler + Vite bundler
# Zero errors expected
```

```bash
# Type-check only (no build output)
npx tsc -p tsconfig.app.json --noEmit
```

### Accessibility scan (REQ-JP-06)

```bash
# 1. Build and start preview server (port 4173)
cd market-presence-fullstack/frontend/frontend
npm run build
npm run preview -- --port 4173 &

# 2. Run the scan
cd ../../qa/accessibility
npm install  # first time only
node axe-scan.mjs postfix

# 3. Stop preview server
pkill -f "vite preview"
```

Results are written to `qa/accessibility/scan-results/`. The script exits with code 1 if any critical violations are found.

---

## 12. How to Run Locally From Zero

### Prerequisites

Install all of these before starting:

| Tool | Required version | Install |
|------|-----------------|---------|
| Java | 21 | https://adoptium.net → Temurin 21 |
| Maven | Not needed separately | Included as `./mvnw` |
| Node.js | 18 or higher | https://nodejs.org |
| npm | 9 or higher | Bundled with Node |
| PostgreSQL | 14 or higher | https://www.postgresql.org/download/ |

Verify:
```bash
java -version       # should show: openjdk 21.x.x
node -v             # should show: v18.x.x or higher
psql --version      # should show: psql (PostgreSQL) 14.x or higher
```

### Step 1 — Set JAVA_HOME

```bash
# macOS (Homebrew Temurin)
export JAVA_HOME=$(/usr/libexec/java_home -v 21)
echo $JAVA_HOME    # should print: /Library/Java/JavaVirtualMachines/temurin-21.jdk/Contents/Home

# Linux
export JAVA_HOME=/usr/lib/jvm/java-21-openjdk-amd64
```

Add this line to `~/.zshrc` or `~/.bashrc` to make it permanent.

### Step 2 — Start PostgreSQL

```bash
# macOS (Homebrew)
brew services start postgresql@16

# Linux
sudo systemctl start postgresql

# Verify it's running
psql --version
```

### Step 3 — Create the database

```bash
# Open psql as the postgres superuser
psql -U postgres

# Inside psql, run these four commands:
CREATE USER sanjay WITH PASSWORD '1234';
CREATE DATABASE forge_market_presence OWNER sanjay;
GRANT ALL PRIVILEGES ON DATABASE forge_market_presence TO sanjay;
\q
```

**Verify connection:**
```bash
PGPASSWORD=1234 psql -U sanjay -d forge_market_presence -h localhost -c "SELECT 1;"
# Expected output: ?column? = 1
```

### Step 4 — Run backend tests

```bash
cd market-presence-fullstack/backend
./mvnw test
# Expected: all tests pass
```

### Step 5 — Start the backend

```bash
cd market-presence-fullstack/backend
./mvnw spring-boot:run
```

First run downloads all Maven dependencies (~60 seconds). Subsequent runs take ~15 seconds.

What happens on first start:
1. Flyway runs V1, V2, V3 migrations and creates all tables
2. Hibernate validates the schema
3. `DataSeeder` inserts 8 demo jobs, 4 applications, channel records, and referrals
4. Server starts listening on port 8086

**Verify the backend is running:**
```bash
curl http://localhost:8086/actuator/health
# Expected: {"status":"UP"}
```

**Open Swagger:**
Navigate to `http://localhost:8086/swagger-ui/index.html`

### Step 6 — Check frontend .env.local

```bash
cat market-presence-fullstack/frontend/frontend/.env.local
# Should contain: VITE_API_URL=http://localhost:8086/api
```

If the file doesn't exist, create it:
```bash
echo 'VITE_API_URL=http://localhost:8086/api' > market-presence-fullstack/frontend/frontend/.env.local
```

### Step 7 — Install frontend dependencies

```bash
cd market-presence-fullstack/frontend/frontend
npm install
# First time: ~60 seconds, installs to node_modules/
```

### Step 8 — Build frontend (optional TypeScript check)

```bash
npm run build
# Compiles TypeScript + bundles to dist/
# Zero errors expected
```

### Step 9 — Start the frontend dev server

```bash
cd market-presence-fullstack/frontend/frontend
npm run dev
```

Open `http://localhost:5173` — you should see the Forge AI careers portal with job cards.

### Quick-reference run commands

Open **two terminal windows side by side**:

**Terminal 1 — Backend:**
```bash
cd market-presence-fullstack/backend
./mvnw spring-boot:run
```

**Terminal 2 — Frontend:**
```bash
cd market-presence-fullstack/frontend/frontend
npm run dev
```

| URL | What it shows |
|-----|--------------|
| `http://localhost:5173` | Careers portal home page |
| `http://localhost:5173/jobs` | All published jobs |
| `http://localhost:5173/admin/jobs` | Admin job management |
| `http://localhost:8086/swagger-ui/index.html` | All API endpoints |
| `http://localhost:8086/actuator/health` | Backend health check |

---

## 13. Troubleshooting

### "release version 21 not supported" when running mvnw

**Cause:** The wrong Java version is being used.  
**Fix:**
```bash
java -version   # must say openjdk 21.x.x
export JAVA_HOME=$(/usr/libexec/java_home -v 21)   # macOS
./mvnw spring-boot:run
```

### "psql: command not found"

**Cause:** PostgreSQL not installed or not on PATH.  
**Fix:**
```bash
# macOS
brew install postgresql@16
export PATH="/opt/homebrew/opt/postgresql@16/bin:$PATH"

# Linux
sudo apt install postgresql-client
```

### "FATAL: password authentication failed for user sanjay"

**Cause:** User `sanjay` does not exist or has wrong password.  
**Fix:**
```bash
psql -U postgres -c "CREATE USER sanjay WITH PASSWORD '1234';"
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE forge_market_presence TO sanjay;"
```

### "FATAL: database forge_market_presence does not exist"

**Fix:**
```bash
psql -U postgres -c "CREATE DATABASE forge_market_presence OWNER sanjay;"
```

### "missing table" or Hibernate error on startup

**Cause:** Flyway migration failed or database is empty.  
**Fix:** Check the backend console for Flyway errors. Usually means the database user lacks CREATE permission:
```bash
psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE forge_market_presence TO sanjay;"
```
Then restart the backend.

### Port 8086 already in use

```bash
# Find and kill the process
lsof -ti:8086 | xargs kill -9    # macOS/Linux
```

### Port 5173 already in use

```bash
lsof -ti:5173 | xargs kill -9    # macOS/Linux
```

### Frontend calls port 8080 instead of 8086

**Cause:** `.env.local` is missing or has wrong URL.  
**Fix:** Ensure `frontend/frontend/.env.local` contains:
```
VITE_API_URL=http://localhost:8086/api
```
Restart the Vite dev server after changing `.env.local`.

### 403 Forbidden on /api/public/jobs/events (analytics endpoint)

**Cause:** Spring Security is blocking a public endpoint.  
**Check:** `SecurityConfig.java` has:
```java
.requestMatchers(HttpMethod.GET, "/api/public/**").permitAll()
```
Note that only `GET` is explicitly permitted. The events endpoint uses `POST`. Verify the security config permits `POST /api/public/**` or the specific events path:
```java
.requestMatchers("/api/public/**").permitAll()
```
If still blocked after verifying security config, check that the request is not missing a `Content-Type: application/json` header.

### 500 Internal Server Error on apply

**Cause:** Usually a database constraint violation or missing file.  
**Fix:** Check the backend console logs (not the browser). Common sub-causes:
- `job_posting_id` does not exist in `job_postings` table
- `uploads/resumes/` directory does not exist (created automatically on first file save, but check write permissions)
- `candidate_email` validation failed

### Running curl inside psql

`curl` is a shell command, not a SQL command. If you see an error like `syntax error at or near "curl"`, you are running it in the wrong terminal. Open a separate shell tab and run curl there.

### Using YOUR-SLUG-HERE instead of a real slug

All API examples with `{slug}` require a real slug from the database. Get valid slugs:
```bash
PGPASSWORD=1234 psql -U sanjay -d forge_market_presence -h localhost \
  -c "SELECT slug, status FROM job_postings WHERE status='PUBLISHED';"
```

### Duplicate application gives 409

This is expected behaviour (REQ-JP-07). You are trying to apply with an email that already has a record in `application_intakes` for that job. Use a different email address for testing.

### Empty analytics table

Analytics events are recorded lazily as candidates interact with the portal. To generate some quickly:
```bash
# This records a VIEW event
curl http://localhost:8086/api/public/jobs/react-frontend-engineer-bangalore

# This records a CLICK event
curl -X POST http://localhost:8086/api/public/jobs/react-frontend-engineer-bangalore/events \
  -H "Content-Type: application/json" \
  -d '{"eventType":"CLICK","channel":"CAREERS_PORTAL"}'
```

### Swagger UI shows empty or no operations

**Cause:** Backend crashed during startup.  
**Fix:** Check console output for errors. Most common: PostgreSQL not running or wrong credentials in `application.yml`.

### Admin UI shows 401/403

The admin pages (`/admin/jobs`, `/admin/handoffs`) call endpoints that require `ROLE_ADMIN` or `ROLE_RECRUITER`. Register a user via `POST /api/auth/register`, then manually grant the role:
```sql
PGPASSWORD=1234 psql -U sanjay -d forge_market_presence -h localhost \
  -c "UPDATE users SET role = 'ADMIN' WHERE email = 'your@email.com';"
```
Then log in again at `/login` to get a fresh token with the new role.

---

## 14. Compliance Matrix

This table tracks Team 3's compliance with the Forge PES v2.0 specification.

Status definitions:
- **Done** — fully implemented and passing
- **Partial** — core logic exists but missing one or more expected capabilities
- **Demo fallback** — the feature works locally but uses a stub/mock instead of real integration
- **Missing** — not yet implemented

| Req ID | PDF Expectation | Priority | Status | Backend Evidence | Frontend Evidence | API Evidence | Database Evidence | Test Evidence | Demo Command | Remaining Gap |
|--------|----------------|----------|--------|-----------------|------------------|-------------|------------------|--------------|-------------|--------------|
| REQ-JP-01 | Auto-create DRAFT job posting when demand transitions to OPEN_EXTERNAL via Kafka | Must | Demo fallback | `DemandEventConsumer.java` (Kafka listener wired), `JobPostingService.createFromDemand()` | `AdminJobEditorPage` shows pre-filled DRAFT | `POST /api/job-postings/from-demand` | `job_postings.demand_id` populated | `JobPostingServiceTest.createFromDemand_*` (4 tests) | `curl -X POST .../from-demand -d '{...}'` | No local Kafka broker. Real integration requires a running Kafka cluster and Team 1 producing to `demand-opened` topic. |
| REQ-JP-02 | HR can create, edit, and manage job postings with all required fields | Must | Done | `JobPostingController`, `JobPostingService`, `JobPosting` entity (28 fields) | `AdminJobsPage.tsx`, `AdminJobEditorPage.tsx` | `POST /api/job-postings`, `PUT /api/job-postings/{id}`, `PATCH /api/job-postings/{id}/status` | `job_postings` table | `JobPostingControllerTest` (12 tests), `JobPostingServiceTest` (14 tests) | Open `http://localhost:5173/admin/jobs` | Admin UI requires ROLE_ADMIN JWT. DataSeeder does not create admin users. |
| REQ-JP-03 | Automatic channel unpublish when job is closed or deadline passes | Must | Done | `JobPostingChannelService.unpublishAllLiveChannels()`, `ChannelExpiryScheduler` (hourly cron) | `ChannelPanel.tsx` shows UNPUBLISHED status | `POST /api/job-postings/{id}/channels/{channel}/unpublish` | `job_posting_channels.status`, `unpublished_at` | `JobPostingChannelServiceTest` | `PATCH .../status {"status":"CLOSED"}` | LinkedIn is PENDING not LIVE (no real API). Indeed is via XML feed only. |
| REQ-JP-04 | Job auto-publishes to careers portal when status = PUBLISHED | Must | Done | `JobPostingService.updateStatus()` → `channelService.upsertCareersPortalChannel()`, `DataSeeder.seedCareersPortalChannels()` | Job appears at `/jobs` after status update | `GET /api/public/jobs` | `job_posting_channels` with `channel_name=CAREERS_PORTAL, status=LIVE` | `JobPostingChannelServiceTest` | `PATCH .../status {"status":"PUBLISHED"}` then `GET /api/public/jobs` | None — fully functional. |
| REQ-JP-05 | Analytics events (views, clicks, apply starts, completions) per posting per channel, exposed to Team 4 | Must | Done | `AnalyticsService`, `JobAnalyticsController`, `JobPostingAnalyticEvent` entity | `analytics.api.ts` fires CLICK + APPLY_START | `GET /api/analytics/job-postings`, `POST /api/public/jobs/{slug}/events` | `job_posting_analytic_events` table with indexes | `AnalyticsServiceTest`, `JobAnalyticsControllerTest` | Browse a job then `curl .../analytics/job-postings` | VIEW recorded on slug GET, APPLY_COMPLETE recorded in service. CLICK/APPLY_START require frontend to fire them (already wired). |
| REQ-JP-06 | Accessible public careers portal, WCAG 2.1 AA, zero critical violations | Must | Done | N/A | `qa/accessibility/axe-scan.mjs` passes, semantic HTML, TailwindCSS | `/jobs`, `/careers/:slug`, `/careers/:slug/apply` | N/A | `qa/accessibility/scan-results/*.json` — zero critical violations | `cd qa/accessibility && node axe-scan.mjs postfix` | Full report in `scan-results/`. Needs re-run after any UI changes. |
| REQ-JP-07 | Multi-step application form, resume upload, duplicate detection, confirmation | Must | Done | `ApplicationIntakeController`, `ApplicationIntakeService`, `FileStorageService`, `LoggingEmailService`, unique constraint on `(job_posting_id, candidate_email)` | `ApplicationPage.tsx`, `ApplicationStepper.tsx`, `ResumeUploader.tsx`, `SuccessModal.tsx`, `AlreadyAppliedModal.tsx` | `POST /api/public/jobs/{slug}/apply` (multipart) | `application_intakes` table | `ApplicationIntakeControllerTest` | Apply via UI or `curl -F ...` | Email is logged (not sent) by default. Set `app.email.smtp.enabled=true` for real email. |
| REQ-JP-08 | Forward applications to Team 2 within 30 seconds, with retry on failure | Must | Demo fallback | `ApplicationHandoffService`, `HttpTeam2Client`, `Team2StubController`, `HandoffRecord` entity | `AdminHandoffsPage.tsx` (retry UI) | `GET /api/admin/handoffs`, `POST .../retry`, `POST .../retry-pending` | `application_handoffs` table | `ApplicationHandoffServiceTest` | Apply then `curl .../admin/handoffs` | Real Team 2 service not running. Stub at `api/stub/team2` is active. Replace `app.team2.api-base-url` with real Team 2 URL. |
| REQ-JP-09 | SEO-friendly URLs, meta tags, JSON-LD structured data | Should | Done | `JobPosting.metaTitle`, `JobPosting.metaDescription`, `JobPosting.slug` | `useDocumentMeta.ts`, JSON-LD injection in `JobDetailsPage`, `/careers/:slug` canonical route | `GET /api/public/jobs/{slug}` returns metaTitle, metaDescription | `meta_title`, `meta_description` columns | N/A | Open `/careers/react-frontend-engineer-bangalore`, view page source | None — fully functional. |
| REQ-JP-10 | Employer branding: company info, culture, benefits, employee stories | Should | Demo fallback | N/A | `branding.ts` static config file, rendered on `HomePage` | No admin branding API endpoint exists | N/A | N/A | Open `http://localhost:5173` | `GET /api/admin/branding` endpoint missing. Admin/HR cannot edit branding without a code change. |
| REQ-JP-11 | Employee referral links with source tracking | Should | Done | `JobReferralService`, `JobPostingReferralController`, `ApplicationIntakeService.resolveSource()`, `markApplied()` | Referral code passed in application form (URL param capture) | `POST /api/job-postings/{id}/referrals`, `GET /api/referrals/{code}` | `job_referrals` table with `referral_code UNIQUE` | `JobReferralServiceTest` | `curl -X POST .../referrals -d '{"referrerId":1001}'` | No hire/reject outcome tracking after APPLIED status. |


---

## 15. What We Have Completed

The following features are fully implemented, tested, and working in this repository as of 2026-06-03.

### Infrastructure
- Java 21 + Spring Boot 4 backend compiling and running at port 8086
- React 19 + TypeScript + Vite frontend running at port 5173
- PostgreSQL database `forge_market_presence` with three Flyway migrations
- `application.yml` with all configurable settings documented inline
- `DataSeeder` that inserts 8 demo jobs on every startup (idempotent)
- Swagger UI at `/swagger-ui/index.html`
- Actuator health at `/actuator/health`

### Authentication (JWT)
- `POST /api/auth/register` — creates user + candidate profile, returns JWT
- `POST /api/auth/login` — validates credentials, returns JWT
- `JwtUtil` with HS256 signing, 24-hour expiry
- `JwtAuthFilter` that validates token on every request
- Role-based access: CANDIDATE, ADMIN, RECRUITER

### Job Posting CRUD (REQ-JP-02)
- Create, read (list + by ID + by slug), update all fields, patch status, delete
- Auto-slug generation with collision handling
- Rich filtering: status, location, seniority, title (all partial match)
- Pagination + sorting with `PagedResponse<T>`
- Full validation: title required, deadline must be future, salaryMin ≤ salaryMax

### Demand-to-Job Fallback (REQ-JP-01)
- `POST /api/job-postings/from-demand` accepts `DemandSnapshot`
- Kafka `@KafkaListener` on topic `demand-opened` wired and ready
- Skill list → requirements text, level alias normalisation (STAFF → LEAD)

### Public Careers Portal (REQ-JP-06)
- `/jobs` page with grid of job cards
- `/careers` → redirects to `/jobs`
- `/careers/:slug` → job detail with full description, requirements, responsibilities, benefits
- Real-time client-side filtering: search text, work mode, seniority, employment type, location, department
- Sorting: latest, highest salary, lowest salary, A–Z

### Candidate Application (REQ-JP-07)
- 10-step application form in `ApplicationStepper.tsx`
- Resume upload (PDF/DOCX, max 10 MB), validated server-side
- `FileStorageService` stores files to `uploads/resumes/` on local disk
- Duplicate detection: 409 on duplicate (backend) + localStorage check (frontend)
- `SuccessModal` on submit success, `AlreadyAppliedModal` on duplicate
- `LoggingEmailService` logs confirmation email to console

### Channel Publishing (REQ-JP-03, REQ-JP-04)
- `JobPostingChannelService` manages CAREERS_PORTAL, LINKEDIN, INDEED channels
- Auto-publish to CAREERS_PORTAL on PUBLISHED status change
- LinkedIn: PENDING status with copy-URL fallback when API not configured
- Indeed: LIVE immediately via XML feed at `/api/public/jobs/feed.xml`
- `ChannelExpiryScheduler` runs hourly, auto-closes and unpublishes expired jobs
- Manual unpublish via `POST /api/job-postings/{id}/channels/{channel}/unpublish`
- `ChannelPanel.tsx` admin UI for channel management

### Indeed XML Feed (REQ-JP-03)
- `GET /api/public/jobs/feed.xml` returns all PUBLISHED jobs in Indeed XML spec format
- Includes title, date, reference number, URL, company, city, state, country, remote type, job type, salary, description

### Analytics (REQ-JP-05)
- VIEW recorded automatically on `GET /api/public/jobs/{slug}`
- CLICK and APPLY_START recorded via `POST /api/public/jobs/{slug}/events`
- APPLY_COMPLETE recorded in `ApplicationIntakeService`
- `GET /api/analytics/job-postings` and `GET /api/analytics/job-postings/{id}` for Team 4
- Per-channel breakdown (CAREERS_PORTAL, LINKEDIN, INDEED) + totals

### Referral Links (REQ-JP-11)
- `POST /api/job-postings/{id}/referrals` generates unique 10-char code
- Shareable URL: `/careers/{slug}?ref={code}`
- `resolveSource()` tags applications with CAREERS_PORTAL or REFERRAL
- `markApplied()` updates referral status to APPLIED with candidate details

### Team 2 Handoff (REQ-JP-08)
- `HandoffRecord` created with PENDING status on every application
- `HttpTeam2Client` posts `Team2ApplicationPayload` to configured URL
- `Team2StubController` at `/api/stub/team2` acts as local Team 2 replacement
- Status transitions: PENDING → SENT (success) or FAILED (error)
- `POST /api/admin/handoffs/{id}/retry` — manual retry
- `POST /api/admin/handoffs/retry-pending` — bulk retry all PENDING
- `AdminHandoffsPage.tsx` shows handoff status dashboard

### SEO (REQ-JP-09)
- `slug` field on every job posting (unique, URL-friendly)
- `metaTitle` and `metaDescription` stored on job posting and returned in API
- `useDocumentMeta.ts` injects `<title>`, `<meta name="description">`, `<script type="application/ld+json">`
- `/careers/:slug` canonical route (alongside legacy `/jobs/:slug`)

### Employer Branding (REQ-JP-10)
- `branding.ts` exports full `BrandingConfig` with company info, culture values, benefits, employee stories
- Rendered on `HomePage.tsx`

### Accessibility (REQ-JP-06)
- `qa/accessibility/axe-scan.mjs` — Playwright + axe-core WCAG 2.1 AA scan
- Pages scanned: `/jobs`, `/careers/:slug`, `/careers/:slug/apply`
- Scan results saved to `qa/accessibility/scan-results/`
- Zero critical violations confirmed (2026-06-03)

### Admin Pages
- `AdminJobsPage.tsx` — list all jobs, status badges, action buttons
- `AdminJobEditorPage.tsx` — create/edit form with all 20+ fields + MarkdownToolbar + ChannelPanel
- `AdminHandoffsPage.tsx` — handoff status + retry UI

### Testing
- 10+ test classes covering all major controllers and services
- MockMvc for HTTP layer, Mockito for service layer
- Context load test (`MarketPresenceServiceApplicationTests`)

---

## 16. What Is Still Behind

Be honest with project reviewers about the following gaps.

### Real LinkedIn API integration
LinkedIn channel status is PENDING when `app.channels.linkedin.api-configured=false`. A real LinkedIn Jobs API (OAuth, API key, job submission via LinkedIn's Job Posting API) is not implemented. **Impact:** REQ-JP-03 LinkedIn channel shows as PENDING in demos.

### Real Indeed direct submission
Indeed is served via the XML feed at `/api/public/jobs/feed.xml`. The recruiter must manually submit this feed URL to Indeed's Job Distributor portal. There is no automated push to Indeed's API. **Impact:** Partial REQ-JP-03 — the feed exists but submission is manual.

### Real SMTP email delivery
`LoggingEmailService` is active by default — emails are written to the application log, not actually sent. To activate real delivery: set `app.email.smtp.enabled=true` and configure an SMTP server (Mailhog for local dev, SendGrid/AWS SES for production). **Impact:** REQ-JP-07 confirmation email is not delivered to candidates in current state.

### Real Team 1 Kafka event
`DemandEventConsumer` is wired but there is no local Kafka broker. The `POST /api/job-postings/from-demand` HTTP fallback works for demos, but a production deployment requires a running Kafka cluster and Team 1 publishing to topic `demand-opened`. **Impact:** REQ-JP-01 works as demo fallback only.

### Real Team 2 service
`Team2StubController` handles handoffs locally. The `app.team2.api-base-url` must be replaced with Team 2's real URL for the handoff to reach the actual Talent Acquisition Engine. **Impact:** REQ-JP-08 handoff is confirmed end-to-end locally but not with the real Team 2 service.

### Admin / HR branding editor
`branding.ts` is a static TypeScript file. There is no `GET/PUT /api/admin/branding` endpoint. HR cannot edit company info, culture values, or benefits without a code change. **Impact:** REQ-JP-10 is static demo fallback.

### ProtectedRoute auth enforcement
`ProtectedRoute` in `AppRoutes.tsx` currently renders children unconditionally (`return <>{children}</>`). The apply page, applications page, and profile page are accessible without a valid token in the browser. **Impact:** Security is handled at the API level (JWT validation), but the frontend allows unauthenticated navigation to protected pages.

### DataSeeder does not create admin users
New developers cannot access admin endpoints until they manually set a user's role to ADMIN in the database. There is no default admin credential. **Impact:** Admin UI demos require an extra setup step.

### JaCoCo coverage report
No JaCoCo Maven plugin is configured. There is no automated code coverage gate or report. The PES spec mentions a 60% coverage target. **Impact:** Coverage compliance is not verified automatically.

### OpenAPI spec files not committed
SpringDoc generates the OpenAPI spec at runtime (`/v3/api-docs`). No static `openapi.yaml` or `openapi.json` file is committed to the repository. A shared schema registry or contract-first API design workflow would require this. **Impact:** Cross-team contract verification is manual.

### Docker / Kubernetes / CI/CD
No `docker-compose.yml` exists at the repository root for the full stack (backend + frontend + PostgreSQL together). The frontend has a `Dockerfile` and a `docker-compose.yml` in `frontend/` but they are for the frontend only. No GitHub Actions CI pipeline is configured in this repository. **Impact:** No automated test-on-push or container deployment.

### OutboxEvent publisher not wired
The `outbox_events` table and `OutboxEvent` entity exist (V3 migration). The `KafkaEvents` event shapes are defined. But no service currently writes to the outbox or publishes its contents to Kafka. **Impact:** Kafka-based inter-service communication is schema-ready but not functional.

---

## 17. Suggested Next Implementation Plan

This is a practical roadmap ordered by impact on PES compliance.

### Priority 1 — Close Must requirement gaps

**1. Fix admin access for demos (1 hour)**
Add a default admin seed user in `DataSeeder.java` using `BCryptPasswordEncoder`. This removes the manual SQL step for every new developer.

**2. Wire ProtectedRoute to auth check (30 minutes)**
In `AppRoutes.tsx`, update `ProtectedRoute` to read from `useAuthStore()` and redirect to `/login` if not authenticated. This enforces role-based navigation on the frontend to match the backend security rules.

**3. Activate real SMTP email for REQ-JP-07 (2 hours)**
Run Mailhog locally (`docker run -d -p 1025:1025 -p 8025:8025 mailhog/mailhog`), set `app.email.smtp.enabled=true` in `application.yml`, verify confirmation emails appear at `http://localhost:8025`. Document the Mailhog setup in the README.

### Priority 2 — Strengthen existing features

**4. Complete REQ-JP-10 branding editor (4 hours)**
Add `GET /api/admin/branding` and `PUT /api/admin/branding` endpoints backed by a `BrandingConfig` table or a JSON file stored in the database. Add an admin UI form at `/admin/branding`. Change `branding.ts` to call the API instead of importing static data.

**5. Real LinkedIn publish proof for demos (2 hours)**
Since a real LinkedIn API key is unlikely before the demo, document the PENDING status as the intended fallback. Add a clear UI affordance in `ChannelPanel.tsx` showing the copyable URL and a "How to manually post to LinkedIn" tooltip. This makes the demo story clear.

**6. Indeed manual submission documentation (30 minutes)**
Add a step in the demo script and admin UI showing how to submit `/api/public/jobs/feed.xml` to Indeed's Job Distributor. Add the feed URL visibly in the admin channel panel for INDEED channel.

### Priority 3 — Demo hardening

**7. Add Kafka demo instructions (1 hour)**
Add a section to the README showing how to start a local Kafka broker with Docker and test the `demand-opened` consumer. This demonstrates REQ-JP-01 properly.

**8. Add JaCoCo plugin (1 hour)**
Add the `jacoco-maven-plugin` to `pom.xml` with `mvn verify` generating a coverage report. Set a fail-threshold of 60% to meet the PES target.

**9. Add Docker Compose for full stack (2 hours)**
Create a `docker-compose.yml` at the repo root that starts PostgreSQL + backend + frontend together. This simplifies onboarding to a single `docker-compose up` command.

**10. Commit OpenAPI spec (30 minutes)**
Run `curl http://localhost:8086/v3/api-docs -o openapi.json` and commit it. Add a GitHub Actions step that regenerates it on each push for contract tracking.

### Priority 4 — Production readiness

**11. Externalize secrets (1 hour)**
Move JWT secret and DB password from `application.yml` to environment variables. Add `.env.example` with placeholder values and update the README.

**12. CI/CD pipeline (2 hours)**
Add `.github/workflows/ci.yml` that runs `./mvnw test` on every push to main and PR. Add the frontend `npm run build` check as a second job.

---

## 18. Demo Script

This script walks through the complete REQ-JP-01 to REQ-JP-11 feature set in approximately 15 minutes.

### Setup (do before the demo)

```bash
# Terminal 1 — Backend
cd market-presence-fullstack/backend
./mvnw spring-boot:run

# Terminal 2 — Frontend
cd market-presence-fullstack/frontend/frontend
npm run dev
```

Wait for "Started MarketPresenceServiceApplication" in Terminal 1.

### Step 1 — Health check and Swagger

```bash
curl http://localhost:8086/actuator/health
# Show: {"status":"UP"}
```

Open `http://localhost:8086/swagger-ui/index.html`  
Say: "All 13 controllers are documented here. We can execute requests directly."

### Step 2 — Public careers portal (REQ-JP-06)

Open `http://localhost:5173/jobs`  
Show: 6 job cards with department badges, work mode pills, salary ranges

Search "engineer" in the search box — cards filter instantly.  
Select "Work Mode: Remote" — only remote roles remain.  
Select "Seniority: Senior" — only senior roles.  
Use Sort dropdown: "Highest Salary" — reordered instantly.  
Say: "All filtering is client-side — zero extra API calls after initial load."

### Step 3 — Job detail and SEO (REQ-JP-09)

Click any job card (e.g. React Frontend Engineer).  
Navigate to `/careers/react-frontend-engineer-bangalore` (show the clean URL).  
Right-click → View Page Source → find `<script type="application/ld+json">` → show the structured data.  
Say: "Google Jobs can index this directly from the JSON-LD."

### Step 4 — Demand to job posting (REQ-JP-01)

```bash
curl -X POST http://localhost:8086/api/job-postings/from-demand \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <ADMIN_JWT>" \
  -d '{
    "demandId": 999,
    "title": "Staff Platform Engineer",
    "level": "STAFF",
    "skills": ["Kubernetes", "Terraform", "AWS"],
    "locationCity": "Bangalore",
    "locationState": "KA",
    "locationCountry": "IN",
    "department": "Infrastructure",
    "targetDate": "2027-12-31"
  }'
```

Show response: `status: "DRAFT"`, `seniority: "LEAD"` (STAFF normalised), `requirements: "Kubernetes, Terraform, AWS"`, `demandId: 999`.  
Say: "In production this is triggered by a Kafka event from Team 1. The HTTP endpoint is the local fallback."

### Step 5 — Admin job editor (REQ-JP-02)

Open `http://localhost:5173/admin/jobs`  
Click "Edit" on any job — show the full form with all fields.  
Click "New Job" → fill minimal fields → save → show new DRAFT in the list.

### Step 6 — Channel publishing (REQ-JP-03, REQ-JP-04)

Open the new job's edit page — show `ChannelPanel.tsx` at the bottom.

```bash
# Auto-publish to careers portal (happens automatically on PUBLISHED)
curl -X PATCH http://localhost:8086/api/job-postings/1/status \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"status":"PUBLISHED"}'

# Show channels
curl http://localhost:8086/api/job-postings/1/channels \
  -H "Authorization: Bearer <TOKEN>"
```

Show CAREERS_PORTAL → LIVE, then publish to LINKEDIN → PENDING (with copy-URL message), then INDEED → LIVE (XML feed).

### Step 7 — Indeed XML feed

```bash
curl http://localhost:8086/api/public/jobs/feed.xml
```

Show the XML output with job elements. Say: "Recruiters submit this URL to Indeed's Job Distributor portal."

### Step 8 — Referral link (REQ-JP-11)

```bash
curl -X POST http://localhost:8086/api/job-postings/1/referrals \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <TOKEN>" \
  -d '{"referrerId": 1001}'
```

Show `referralUrl`: `http://localhost:5173/careers/react-frontend-engineer-bangalore?ref=ABC123DEF5`  
Say: "An employee shares this URL. When someone applies through it, the application is tagged REFERRAL."

### Step 9 — Apply flow (REQ-JP-07)

Open `http://localhost:5173/jobs/react-frontend-engineer-bangalore`  
Click "Apply Now" — show the analytics event firing in Network tab.  
Walk through all 10 steps of the form.  
Upload a PDF — show the auto-fill animation.  
Check the GDPR consent box.  
Click Submit.  
Show `SuccessModal`.

```bash
# Verify in database
PGPASSWORD=1234 psql -U sanjay -d forge_market_presence -h localhost \
  -c "SELECT candidate_name, candidate_email, source, status FROM application_intakes ORDER BY applied_at DESC LIMIT 3;"
```

### Step 10 — Duplicate block (REQ-JP-07)

Try applying again with the same email.  
Show: `AlreadyAppliedModal` before the form even opens.  
Or in curl: show the 409 response.

### Step 11 — Team 2 handoff (REQ-JP-08)

```bash
curl http://localhost:8086/api/admin/handoffs \
  -H "Authorization: Bearer <ADMIN_JWT>"
```

Show records with `status: "SENT"` and `team2ResponseId: "T2-XXXXXXXX"` from the local stub.  
Open `http://localhost:5173/admin/handoffs` — show the UI.  
Say: "Replace `app.team2.api-base-url` with Team 2's real URL and all pending handoffs can be bulk-retried."

### Step 12 — Analytics (REQ-JP-05)

```bash
curl http://localhost:8086/api/analytics/job-postings/1
```

Show the per-channel counts. Say: "Team 4 calls this API to build their analytics dashboards."

### Step 13 — Database confirmation

```bash
PGPASSWORD=1234 psql -U sanjay -d forge_market_presence -h localhost << 'SQL'
SELECT 'job_postings' as table_name, COUNT(*) FROM job_postings
UNION ALL SELECT 'applications', COUNT(*) FROM application_intakes
UNION ALL SELECT 'channels', COUNT(*) FROM job_posting_channels
UNION ALL SELECT 'referrals', COUNT(*) FROM job_referrals
UNION ALL SELECT 'analytic_events', COUNT(*) FROM job_posting_analytic_events
UNION ALL SELECT 'handoffs', COUNT(*) FROM application_handoffs;
SQL
```

### Step 14 — Acknowledge known fallbacks

State clearly:
- LinkedIn: PENDING (no API key — recruiter posts manually using the copy URL)
- Email: logged to console (no SMTP — set `enabled=true` + Mailhog for real delivery)
- Team 2: local stub (replace URL for production integration)
- Kafka: HTTP fallback (DemandEventConsumer is wired, needs a broker)
- Branding: static file (admin editor endpoint is the next implementation item)

---

## 19. New Developer Onboarding Checklist

Complete these steps in order when joining the project.

- [ ] **Clone the repository**
  ```bash
  git clone <repo-url>
  cd market-presence-fullstack
  ```

- [ ] **Install Java 21** from https://adoptium.net  
  Verify: `java -version` shows `openjdk 21.x.x`

- [ ] **Set JAVA_HOME** in your shell profile (`~/.zshrc` or `~/.bashrc`)

- [ ] **Install Node.js 18+** from https://nodejs.org  
  Verify: `node -v` shows `v18.x.x` or higher

- [ ] **Install PostgreSQL 14+** and start the service

- [ ] **Create the database user and database**
  ```bash
  psql -U postgres -c "CREATE USER sanjay WITH PASSWORD '1234';"
  psql -U postgres -c "CREATE DATABASE forge_market_presence OWNER sanjay;"
  psql -U postgres -c "GRANT ALL PRIVILEGES ON DATABASE forge_market_presence TO sanjay;"
  ```

- [ ] **Verify database connection**
  ```bash
  PGPASSWORD=1234 psql -U sanjay -d forge_market_presence -h localhost -c "SELECT 1;"
  ```

- [ ] **Check that .env.local exists** at `frontend/frontend/.env.local` with:
  ```
  VITE_API_URL=http://localhost:8086/api
  ```

- [ ] **Run backend tests**
  ```bash
  cd backend && ./mvnw test
  ```
  All tests should pass.

- [ ] **Run the frontend build**
  ```bash
  cd frontend/frontend && npm install && npm run build
  ```
  Zero TypeScript errors expected.

- [ ] **Start the backend** and verify health:
  ```bash
  cd backend && ./mvnw spring-boot:run
  curl http://localhost:8086/actuator/health   # → {"status":"UP"}
  ```

- [ ] **Start the frontend** and verify jobs load:
  ```bash
  cd frontend/frontend && npm run dev
  # Open http://localhost:5173/jobs — should show 6 job cards
  ```

- [ ] **Open Swagger** at `http://localhost:8086/swagger-ui/index.html`  
  Confirm all controller groups are visible.

- [ ] **Register an admin user and grant admin role**
  ```bash
  curl -X POST http://localhost:8086/api/auth/register \
    -H "Content-Type: application/json" \
    -d '{"name":"Your Name","email":"you@forge.ai","password":"yourpassword"}'
  # Then grant admin role:
  PGPASSWORD=1234 psql -U sanjay -d forge_market_presence -h localhost \
    -c "UPDATE users SET role = 'ADMIN' WHERE email = 'you@forge.ai';"
  ```

- [ ] **Test the full apply flow**  
  Open any job at `http://localhost:5173/jobs`, click Apply Now, complete all 10 steps, submit.  
  Verify the application appears in the database.

- [ ] **Read the compliance matrix** (Section 14 above)  
  Understand which requirements are Done vs Demo fallback.

- [ ] **Pick your first issue** from the gap list in Section 16.

---

## 20. Glossary

**REST API**  
A way for two programs to talk to each other over HTTP. The frontend sends requests (GET, POST, PUT, PATCH, DELETE) and the backend sends back responses (usually JSON). Like a web form but for programs.

**Controller**  
A Java class in Spring Boot that receives HTTP requests and returns HTTP responses. Example: `JobPostingController.java` receives `GET /api/job-postings` and returns a list of jobs.

**Service**  
A Java class that contains the business logic. Controllers call services. Services do not know about HTTP — they just know about data and rules. Example: `ApplicationIntakeService.apply()` checks for duplicates, saves the application, sends email, triggers handoff.

**Repository**  
A Java interface that Spring Data JPA uses to generate database queries automatically. You define method names like `findBySlug(String slug)` and Spring generates the SQL. Example: `JobPostingRepository.findBySlug()`.

**Entity**  
A Java class annotated with `@Entity` that maps to a database table. Each field maps to a column. Example: `JobPosting.java` maps to the `job_postings` table.

**DTO (Data Transfer Object)**  
A simple class (or Java `record`) that carries data between layers. Request DTOs carry input from the HTTP request. Response DTOs carry output back to the caller. They are not stored in the database. Example: `JobPostingRequest` is the DTO for creating a job; `JobPostingResponse` is the DTO returned after creation.

**JPA (Java Persistence API)**  
The Java standard for mapping Java objects (entities) to relational database tables. Hibernate is the implementation used here. With JPA you write Java, and Hibernate generates and runs the SQL.

**PostgreSQL**  
The open-source relational database used by this project. Stores all job postings, applications, channels, referrals, and analytics events. Database name: `forge_market_presence`, user: `sanjay`, port: 5432.

**Swagger (OpenAPI)**  
Auto-generated API documentation. SpringDoc scans all `@RestController` classes and generates an interactive API browser at `/swagger-ui/index.html`. You can read, test, and execute API endpoints directly from the browser.

**Actuator**  
A Spring Boot feature that exposes management endpoints. The `/actuator/health` endpoint is used to check if the server is running. Used by load balancers in production to know when to route traffic.

**React**  
A JavaScript framework for building user interfaces. The frontend of this project is a React application — all the pages, forms, and job cards are React components.

**Vite**  
The build tool and development server for the React frontend. Running `npm run dev` starts Vite, which serves the React app at `http://localhost:5173` with hot-module reload.

**Axios**  
An HTTP client library for JavaScript. The frontend uses Axios to make API calls to the backend. All calls go through the shared instance in `api/axios.ts` which attaches the JWT token automatically.

**React Query (TanStack Query)**  
A library that manages server state in React. It handles loading states, caching, and refetching. `useJobs()` and `useJob(slug)` are React Query hooks — they call the backend API and cache the results.

**Slug**  
A URL-friendly version of a title. Example: "React Frontend Engineer" in Bangalore becomes `react-frontend-engineer-bangalore`. Used in the job posting URL: `/careers/react-frontend-engineer-bangalore`. Slugs are unique and stored in the database.

**SEO (Search Engine Optimisation)**  
Making web pages discoverable by search engines like Google. For job postings this means: clean URLs (slugs), descriptive `<title>` tags, `<meta name="description">` tags, and JSON-LD structured data that Google can parse to display jobs in "Google Jobs" search results.

**JSON-LD**  
A format for embedding structured data in web pages as a `<script type="application/ld+json">` tag. Google reads it to understand page content. For job postings it contains: title, description, salary, location, company, and application URL in a machine-readable format.

**Analytics event**  
A record of something a user did. Events in this system: VIEW (opened a job detail page), CLICK (clicked "Apply Now"), APPLY_START (opened the application form), APPLY_COMPLETE (submitted the form successfully). Stored in `job_posting_analytic_events`, aggregated for Team 4.

**Channel**  
Where a job posting is published. Three channels exist: CAREERS_PORTAL (this portal), LINKEDIN, INDEED. Each has a status: DRAFT, PENDING, LIVE, FAILED, UNPUBLISHED. Stored in `job_posting_channels`.

**Referral code**  
A unique 10-character string (e.g. `ABC123DEF5`) embedded in a URL (`?ref=ABC123DEF5`) that identifies who shared the job link. When a candidate applies through a referral URL, their `ApplicationIntake.source` is set to REFERRAL instead of CAREERS_PORTAL.

**Handoff**  
Forwarding a candidate application from Team 3 (careers portal) to Team 2 (talent acquisition engine). Each application triggers a `HandoffRecord` with status PENDING → SENT (success) or FAILED (error). Stored in `application_handoffs`.

**Fallback**  
When a real integration is not available, a fallback provides similar behaviour locally. Examples: `LoggingEmailService` logs emails instead of sending them; `Team2StubController` simulates Team 2's intake endpoint; `POST /api/job-postings/from-demand` simulates the Kafka event from Team 1.

**Microservice**  
An architectural style where each system is a separate, independently deployable service. This project is a single Spring Boot application (not a true microservice) but is designed to integrate with other services over HTTP and Kafka.

**Monolith**  
A single application that handles all responsibilities in one process. This project is currently a monolith — one Spring Boot server handles all Team 3 concerns. The code is structured for eventual decomposition.

**Kafka**  
A distributed event streaming platform. Team 1 is supposed to publish a `demand.opened` event to a Kafka topic when a hiring demand is approved. `DemandEventConsumer` in this project is wired to listen for it. Kafka is not running locally — the HTTP fallback is used for dev/demo.

**CI/CD (Continuous Integration / Continuous Deployment)**  
Automated pipelines that run tests on every code push and deploy the application automatically. Not yet configured for this repository. A GitHub Actions workflow would run `./mvnw test` and `npm run build` on every pull request.

**Docker**  
A tool for packaging applications into containers that run the same way everywhere. The frontend has a `Dockerfile`. A full-stack `docker-compose.yml` combining backend + frontend + PostgreSQL is identified as a next implementation step.

**Kubernetes (K8s)**  
A container orchestration platform that manages Docker containers across multiple servers. Not currently used — a future production deployment concern.

**Zustand**  
A lightweight React state management library. Used in this project for auth state (`authStore.ts`). Stores the authenticated user's JWT token and profile in memory (and persists to localStorage so it survives page refresh).

**Flyway**  
A database migration tool. It runs SQL scripts in order (V1, V2, V3...) to create and evolve the database schema. Runs automatically on backend startup. Never modify existing Flyway scripts — always add a new numbered script.

**BCrypt**  
A one-way password hashing algorithm. User passwords are never stored in plain text — they are hashed with BCrypt (12 rounds) before being saved to the `users` table. Spring Security's `BCryptPasswordEncoder` handles this.

**JWT (JSON Web Token)**  
A compact, signed token that proves identity. After login, the backend issues a JWT that the frontend stores in localStorage. Every subsequent API request includes `Authorization: Bearer <token>`. The backend validates the signature to identify the user without a database lookup.

**Spring Boot**  
An opinionated Java framework that makes it easy to build production-ready web applications. It handles dependency injection, web server, database connection, security, and more with minimal configuration.

**TailwindCSS**  
A utility-first CSS framework. Instead of writing custom CSS, you apply small utility classes directly in HTML/JSX (`flex`, `gap-4`, `text-primary-700`). The entire frontend styling uses Tailwind.

---

*This document was generated from direct inspection of the repository source code on 2026-06-03.*  
*Every class name, endpoint, file path, and SQL query has been verified against the actual codebase.*  
*If something marked "needs verification" is found, read the referenced source file directly.*
