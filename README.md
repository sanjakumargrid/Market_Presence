
# Forge AI Careers — Full-Stack Market Presence Portal

A job-posting and candidate-application platform built for **Grid Dynamics Forge**.  
Recruiters manage job postings through a REST API; candidates browse live jobs and submit detailed applications through a polished React UI.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Architecture](#2-architecture)
3. [What Is Implemented](#3-what-is-implemented)
4. [What Is Remaining](#4-what-is-remaining)
5. [Prerequisites](#5-prerequisites)
6. [PostgreSQL Setup](#6-postgresql-setup)
7. [Backend Setup](#7-backend-setup)
8. [Frontend Setup](#8-frontend-setup)
9. [Run Commands (Quick Reference)](#9-run-commands-quick-reference)
10. [API List](#10-api-list)
11. [Demo Flow](#11-demo-flow)
12. [Project Structure](#12-project-structure)
13. [Troubleshooting](#13-troubleshooting)

---

## 1. Project Overview

**What does this app do?**

- Recruiters/admins can **create, update, publish, and delete job postings** via a REST API.
- Published jobs appear on the **candidate-facing portal** where applicants can browse, filter, and apply.
- Candidates fill in a **10-step application form** (personal info, experience, education, skills, certifications, projects, screening questions, documents, and GDPR consent).
- Applications are saved to PostgreSQL and tracked locally in the browser.
- Job postings can be "published" to external channels (LinkedIn, Indeed) — mocked for now.
- Internal employee **referrals** can be recorded against any job.

**Tech stack summary:**

| Layer | Stack |
|-------|-------|
| Backend | Java 21 · Spring Boot 4 · Spring Data JPA · Spring Security · PostgreSQL |
| Frontend | React 19 · TypeScript · Vite · TailwindCSS · Zustand · React Query |
| API Docs | SpringDoc OpenAPI 3 (Swagger UI) |
| Testing | JUnit 5 · Mockito · MockMvc (27 tests) |

---

## 2. Architecture

```
Browser (port 5173)
      │
      │  HTTP / JSON
      ▼
React 19 Frontend  ──────────────────────────────────────────────
  pages/            Vite dev server serves the SPA
  components/       React Query caches API calls
  store/            Zustand holds auth state (localStorage)
  api/              Axios calls backend at http://localhost:8086
      │
      │  HTTP / JSON
      ▼
Spring Boot 4 Backend  (port 8086) ──────────────────────────────
  controller/        REST endpoints (5 controllers)
  service/           Business logic
  repository/        Spring Data JPA (PostgreSQL)
  entity/            JPA entities → 4 tables
  config/            SecurityConfig (CORS + permitAll), DataSeeder
      │
      │  JDBC
      ▼
PostgreSQL  (port 5432)
  forge_market_presence database
  ├── job_postings          (30 columns)
  ├── application_intakes   (candidate applications)
  ├── job_posting_channels  (LinkedIn, Indeed, Naukri)
  └── job_referrals         (internal referrals)
```

**Key design decisions:**

- Hibernate `ddl-auto: update` means you never write SQL DDL — tables are created/updated automatically on startup.
- `DataSeeder` runs on every startup and inserts 8 demo jobs idempotently (safe to restart repeatedly).
- Security is `permitAll` in development — no tokens are needed to call any endpoint.
- CORS allows `localhost:3000` and `localhost:5173` (Vite default).
- The frontend reads `VITE_API_URL` from `.env.local` so the backend URL is easy to change.

---

## 3. What Is Implemented

### Backend

| Area | Detail |
|------|--------|
| Job Postings CRUD | Create, read (list + by ID + by slug), update, patch status (DRAFT/PUBLISHED/CLOSED), delete |
| Pagination & filtering | `?status=`, `?location=`, `?seniority=`, `?title=` with `page`, `size`, `sortBy`, `sortDir` |
| Public job API | `GET /api/public/jobs` and `GET /api/public/jobs/{slug}` — no auth required |
| Application intake | `POST /api/public/jobs/{slug}/apply` saves candidate application; duplicate check returns 409 |
| Channel publishing | Mock `POST /api/job-postings/{id}/publish/linkedin` and `/publish/indeed`; list via `GET /channels` |
| Job referrals | Create a referral with a unique code; look up by code |
| Seed data | 8 jobs (6 PUBLISHED, 1 DRAFT, 1 CLOSED) with sample applications, channels, referrals |
| Swagger UI | All endpoints documented at `/swagger-ui/index.html` |
| Exception handling | Global handler returns `{ status, error, message }` JSON for 404 and 409 |
| Tests | 27 tests across 4 test classes: controller (MockMvc), service (Mockito), context load |

### Frontend

| Area | Detail |
|------|--------|
| Home page | Hero section with featured jobs pulled from the API |
| Jobs list page | Cards with department badge, work-mode pill, location, salary, seniority |
| Search & filter | Real-time title search; filter by work mode, seniority, department; sort by date/salary/name |
| Job detail page | Full description, responsibilities, requirements (tags), benefits, salary tiles |
| 10-step application form | Resume upload, personal info, experience, education, skills, certifications, projects, screening, documents, GDPR review |
| AI resume parsing (simulated) | Uploading a resume triggers a 1.5 s delay then auto-fills all form fields with realistic demo data |
| Duplicate application guard | Checks localStorage before opening form; shows modal if already applied |
| Application tracking | "My Applications" page lists submitted apps with status and next step (stored in localStorage) |
| Auth store | Zustand store persists `isAuthenticated`, token, and user profile to localStorage |
| Protected routes | Apply, Applications, and Profile pages redirect to `/login` if not authenticated |
| Login + Register pages | UI built (`LoginPage.tsx`, `RegisterPage.tsx`) — auth API calls wired |

---

## 4. What Is Remaining

| Feature | Status | Notes |
|---------|--------|-------|
| Auth backend | Not built | `LoginPage` and `RegisterPage` UI exist and call `/api/auth/login` and `/api/auth/register`, but no backend controller or JWT implementation exists yet |
| Login/Register routes | Not wired | The pages exist but are not added to `AppRoutes.tsx` — add them to unblock login |
| Profile page | Stub | `ProfilePage.tsx` renders but contains no real content |
| Applications from backend | Partial | Applications are saved to PostgreSQL on submit, but "My Applications" page only reads from localStorage — no `GET /api/applications` endpoint exists |
| Real file uploads | Not built | Backend stores `resumeUrl` as a plain string; no file storage (S3, local disk) is wired up |
| Real AI resume parsing | Simulated | The "AI parse" is a `setTimeout` that fills hardcoded demo data; no actual parser is called |
| Admin UI | Not built | Job management (create/publish/close) is API-only; no browser dashboard for recruiters |
| Email notifications | Not built | No confirmation emails on application submission |
| Channel publishing | Mock only | LinkedIn/Indeed publishing records a DB row but makes no real external API call |
| Pagination UI | Not built | Backend supports pagination; frontend loads all jobs in one request |
| CLOSED job guard | Not built | A candidate can navigate to `/jobs/senior-java-developer-pune/apply` on a CLOSED job |

---

## 5. Prerequisites

Install these before starting:

| Tool | Version | How to get it |
|------|---------|---------------|
| Java | 21 | https://adoptium.net |
| Maven | 3.9+ | Included as `./mvnw` — no separate install needed |
| Node.js | 18+ | https://nodejs.org |
| npm | 9+ | Bundled with Node.js |
| PostgreSQL | 14+ | https://www.postgresql.org/download/ |

Check your versions:

```bash
java -version        # expect: openjdk 21.x.x
node -v              # expect: v18.x.x or higher
psql --version       # expect: psql (PostgreSQL) 14.x or higher
```

---

## 6. PostgreSQL Setup

You only need to do this once.

### Step 1 — Start PostgreSQL

```bash
# macOS (Homebrew)
brew services start postgresql@16

# Linux (systemd)
sudo systemctl start postgresql

# Windows
# Open Services (services.msc) and start "postgresql-x64-16"
# or use pgAdmin
```

### Step 2 — Create the database and user

```bash
# Open a psql session as the postgres superuser
psql -U postgres
```

Then run these SQL commands inside psql:

```sql
-- Create the application user
CREATE USER sanjay WITH PASSWORD '1234';

-- Create the database
CREATE DATABASE forge_market_presence OWNER sanjay;

-- Grant full access
GRANT ALL PRIVILEGES ON DATABASE forge_market_presence TO sanjay;

-- Exit
\q
```

### Step 3 — Verify the connection

```bash
PGPASSWORD=1234 psql -U sanjay -d forge_market_presence -h localhost -c "SELECT 1;"
```

Expected output: `?column? = 1`

> **Note:** You never need to create tables manually. Hibernate creates and updates the schema automatically when the backend starts for the first time.

---

## 7. Backend Setup

### Directory

```
market-presence-fullstack/backend/
```

### Configuration

All settings are in `src/main/resources/application.yml`:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/forge_market_presence
    username: sanjay
    password: 1234
  jpa:
    hibernate:
      ddl-auto: update   # auto-creates/migrates tables on startup
    show-sql: true       # prints SQL to console — set false to reduce noise

server:
  port: 8086
```

Change the username/password here if your PostgreSQL setup differs.

### Start the backend

```bash
# Navigate to the backend folder
cd market-presence-fullstack/backend

# First run — downloads all Maven dependencies (~60 seconds)
./mvnw spring-boot:run

# Subsequent runs — fast (dependencies are cached)
./mvnw spring-boot:run -q
```

> On Windows use `mvnw.cmd spring-boot:run` instead of `./mvnw`.

### What happens on first start

1. Hibernate creates four tables: `job_postings`, `application_intakes`, `job_posting_channels`, `job_referrals`.
2. `DataSeeder` inserts 8 demo job postings, sample applications, channel records, and referrals.
3. Seeding is **idempotent** — restarting the backend never creates duplicate rows.

### Verify it is running

```bash
curl http://localhost:8086/actuator/health
# Expected: {"status":"UP"}
```

### Swagger UI

Open in your browser: **http://localhost:8086/swagger-ui/index.html**

All endpoints are listed, documented, and executable directly from the browser.

---

## 8. Frontend Setup

### Directory

```
market-presence-fullstack/frontend/frontend/
```

### Step 1 — Install dependencies (first time only)

```bash
cd market-presence-fullstack/frontend/frontend
npm install
```

This installs React, Vite, TailwindCSS, React Query, Zustand, and all other packages (~60 seconds on first run).

### Step 2 — Check the environment file

A `.env.local` file is already present with the correct URL:

```
VITE_API_URL=http://localhost:8086/api
```

If you change the backend port in `application.yml`, update this value to match.

### Step 3 — Start the dev server

```bash
cd market-presence-fullstack/frontend/frontend
npm run dev
```

Open in your browser: **http://localhost:5173**

> The backend must be running first, otherwise the jobs list shows an error state.

### Production build (optional)

```bash
npm run build      # compiles TypeScript and bundles to dist/
npm run preview    # serves the built files locally to test before deploying
```

---

## 9. Run Commands (Quick Reference)

Open two terminal windows side by side:

**Terminal 1 — Backend**

```bash
cd market-presence-fullstack/backend
./mvnw spring-boot:run
```

**Terminal 2 — Frontend**

```bash
cd market-presence-fullstack/frontend/frontend
npm run dev
```

**App URLs**

| Service | URL |
|---------|-----|
| Frontend (React) | http://localhost:5173 |
| Backend (Spring Boot) | http://localhost:8086 |
| Swagger UI | http://localhost:8086/swagger-ui/index.html |
| Health check | http://localhost:8086/actuator/health |

**Run backend tests**

```bash
cd market-presence-fullstack/backend
./mvnw test
# Expected: Tests run: 27, Failures: 0, Errors: 0
```

**TypeScript type-check (frontend)**

```bash
cd market-presence-fullstack/frontend/frontend
npx tsc -p tsconfig.app.json --noEmit
# No output means no errors
```

---

## 10. API List

Base URL: `http://localhost:8086/api`

All endpoints are open (no authentication required in development).

### Public Endpoints — Candidate Facing

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/public/jobs` | List all PUBLISHED job postings |
| `GET` | `/public/jobs/{slug}` | Get one published job by URL slug |
| `POST` | `/public/jobs/{slug}/apply` | Submit a candidate application |

#### Example — list published jobs

```bash
curl http://localhost:8086/api/public/jobs
```

#### Example — apply for a job

```bash
curl -X POST http://localhost:8086/api/public/jobs/react-frontend-engineer-bangalore/apply \
  -H "Content-Type: application/json" \
  -d '{
    "candidateName": "Priya Sharma",
    "candidateEmail": "priya.sharma@example.com",
    "resumeUrl": "resume_priya.pdf",
    "coverLetter": "4 years React experience. Eager to join Forge AI."
  }'
```

**Success (201 Created):**
```json
{
  "id": 5,
  "jobPostingId": 1,
  "candidateEmail": "priya.sharma@example.com",
  "status": "SUBMITTED",
  "appliedAt": "2026-06-02T10:00:00Z"
}
```

**Duplicate (409 Conflict):**
```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Application already submitted for this job by: priya.sharma@example.com"
}
```

---

### Admin Endpoints — Job Postings

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/job-postings` | Create a new job posting (starts as DRAFT) |
| `GET` | `/job-postings` | List all jobs with optional filters and pagination |
| `GET` | `/job-postings/{id}` | Get one job by database ID |
| `GET` | `/job-postings/slug/{slug}` | Get one job by slug |
| `PUT` | `/job-postings/{id}` | Update all fields of a job |
| `PATCH` | `/job-postings/{id}/status` | Change status only (DRAFT → PUBLISHED → CLOSED) |
| `DELETE` | `/job-postings/{id}` | Delete a job posting |

#### Pagination & filter query parameters for `GET /job-postings`

| Param | Default | Example |
|-------|---------|---------|
| `status` | (all) | `PUBLISHED` |
| `location` | (all) | `Bangalore` (partial match) |
| `seniority` | (all) | `SENIOR` |
| `title` | (all) | `engineer` (partial match) |
| `page` | `0` | `1` |
| `size` | `20` | `5` |
| `sortBy` | `createdAt` | `title` |
| `sortDir` | `desc` | `asc` |

```bash
# Get page 0, 5 results, only PUBLISHED jobs, sorted by title A–Z
curl "http://localhost:8086/api/job-postings?status=PUBLISHED&size=5&sortBy=title&sortDir=asc"
```

#### Example — create a job

```bash
curl -X POST http://localhost:8086/api/job-postings \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Platform Engineer",
    "slug": "platform-engineer-pune",
    "location": "Pune, MH",
    "locationCity": "Pune",
    "locationState": "MH",
    "locationCountry": "IN",
    "seniority": "MID",
    "department": "Infrastructure",
    "employmentType": "FULL_TIME",
    "workMode": "HYBRID",
    "salaryMin": 1400000,
    "salaryMax": 1900000,
    "currency": "INR",
    "showSalary": true,
    "applicationDeadline": "2027-06-30"
  }'
```

#### Example — publish a job (DRAFT → PUBLISHED)

```bash
curl -X PATCH http://localhost:8086/api/job-postings/11/status \
  -H "Content-Type: application/json" \
  -d '{"status": "PUBLISHED"}'
```

---

### Admin Endpoints — Channels & Referrals

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/job-postings/{id}/publish/linkedin` | Publish job to LinkedIn (mock) |
| `POST` | `/job-postings/{id}/publish/indeed` | Publish job to Indeed (mock) |
| `GET` | `/job-postings/{id}/channels` | List all channels this job is published on |
| `POST` | `/referrals` | Create a referral for a job |
| `GET` | `/referrals/{referralCode}` | Look up a referral by its code |

---

## 11. Demo Flow

Follow these steps to see the full app in action.

### Step 1 — Start both servers

```bash
# Terminal 1
cd backend && ./mvnw spring-boot:run

# Terminal 2
cd frontend/frontend && npm run dev
```

### Step 2 — Open the app

Go to **http://localhost:5173**. The home page shows featured published jobs.

### Step 3 — Browse jobs

Click **Browse Jobs** or navigate to `/jobs`.

You will see 6 job cards (PUBLISHED only). Each card shows:
- Coloured department badge (e.g. "EN" for Engineering)
- Work mode pill: Remote / Hybrid / On-site
- Location (city · state)
- Salary range (₹ for INR, $ for USD)
- Seniority level
- "Apply Now" button

### Step 4 — Filter and search

Try the following:
- Type `engineer` in the search box → cards filter in real time
- Select **Work Mode: Remote** → only remote roles remain
- Select **Seniority: Senior** → only senior roles
- Use the **Sort** dropdown: Latest / Highest Salary / Lowest Salary / A–Z

### Step 5 — View a job detail

Click any card. The detail page shows full description, responsibilities listed as cards, requirements as tags, and benefits.

### Step 6 — Log in (or simulate login)

The apply flow requires authentication. To simulate login, you can call the Zustand store directly in the browser console:

```js
// Open DevTools console at http://localhost:5173
window.__zustand?.setState({ isAuthenticated: true, user: { email: 'test@forge.ai', name: 'Test User' } })
```

> Alternatively, register/login routes will work once the backend auth controller is implemented (see Section 4).

### Step 7 — Apply for a job

Click **Apply Now** on any job card. Walk through the 10 steps:

| Step | What you fill in |
|------|-----------------|
| 0 — Resume | Upload any file (PDF, DOCX). After 1.5 s the form auto-fills with demo data |
| 1 — My Info | Name, email, phone, location, LinkedIn, GitHub, portfolio |
| 2 — Experience | Add work history entries |
| 3 — Education | Add degree entries |
| 4 — Skills | Skill name, proficiency, years |
| 5 — Certifications | Cert name, issuer, dates |
| 6 — Projects | Project name, tech stack, URL, description |
| 7 — Screening | Visa sponsorship, notice period, CTC, relocation willingness |
| 8 — Documents | Optional: cover letter, transcripts, portfolio, certs |
| 9 — Review | Check GDPR consent box → Submit |

On success, a confirmation page appears.

### Step 8 — Confirm in the database

```bash
PGPASSWORD=1234 psql -U sanjay -d forge_market_presence -h localhost \
  -c "SELECT candidate_name, candidate_email, status, applied_at
      FROM application_intakes
      ORDER BY applied_at DESC
      LIMIT 5;"
```

### Step 9 — Try the Swagger UI

Open **http://localhost:8086/swagger-ui/index.html** and try:
- `GET /api/public/jobs` → see all published jobs
- `PATCH /api/job-postings/{id}/status` → change status to `CLOSED`
- `POST /api/referrals` → create a referral

---

## 12. Project Structure

```
market-presence-fullstack/
├── README.md
│
├── backend/                                   Spring Boot 4 application
│   └── src/
│       ├── main/java/.../
│       │   ├── config/
│       │   │   ├── DataSeeder.java            Seeds 8 demo jobs on startup (idempotent)
│       │   │   └── SecurityConfig.java        CORS (5173/3000) + permitAll
│       │   ├── controller/
│       │   │   ├── JobPostingController       /api/job-postings  (admin CRUD)
│       │   │   ├── PublicJobController        /api/public/jobs   (candidate read)
│       │   │   ├── ApplicationIntakeController /api/public/jobs/{slug}/apply
│       │   │   ├── JobPostingChannelController /api/job-postings/{id}/publish/*
│       │   │   └── JobReferralController      /api/referrals
│       │   ├── dto/                           Request/response objects (Lombok)
│       │   ├── entity/                        JPA entities → 4 DB tables
│       │   ├── exception/                     GlobalExceptionHandler (404, 409)
│       │   ├── repository/                    Spring Data JPA repositories
│       │   └── service/                       Business logic layer
│       ├── main/resources/
│       │   └── application.yml                DB url, port 8086
│       └── test/                              27 unit tests (MockMvc + Mockito)
│
└── frontend/
    └── frontend/                              React 19 + Vite application
        ├── .env.local                         VITE_API_URL=http://localhost:8086/api
        └── src/
            ├── api/
            │   ├── axios.ts                   Axios instance, JWT interceptor hook
            │   ├── jobs.api.ts                GET /public/jobs, GET /public/jobs/{slug}
            │   ├── applications.api.ts        POST /public/jobs/{slug}/apply
            │   ├── auth.api.ts                POST /auth/register, /auth/login (backend pending)
            │   └── profile.api.ts             Profile endpoints (backend pending)
            ├── features/jobs/
            │   ├── hooks/useJobs.ts           React Query list hook
            │   ├── hooks/useJob.ts            React Query single-job hook
            │   └── types/job.types.ts         TypeScript interface for Job (27 fields)
            ├── components/
            │   ├── common/                    Navbar, Sidebar, Skeleton loader
            │   ├── jobs/                      JobCard, JobFilters
            │   └── application/              ApplicationStepper, ResumeUploader, SuccessModal, AlreadyAppliedModal
            ├── pages/
            │   ├── HomePage.tsx               Hero + featured jobs
            │   ├── JobsPage.tsx               List + search + filter
            │   ├── JobDetailsPage.tsx         Full detail view
            │   ├── ApplicationPage.tsx        10-step application form
            │   ├── ApplicationsPage.tsx       My Applications (localStorage)
            │   ├── LoginPage.tsx              Login UI (route not yet wired)
            │   ├── RegisterPage.tsx           Register UI (route not yet wired)
            │   └── ProfilePage.tsx            Profile UI (stub)
            ├── routes/AppRoutes.tsx           React Router v7 route definitions
            ├── store/
            │   ├── authStore.ts               Zustand: isAuthenticated, token, user
            │   └── localStorage.ts            Application tracking helpers
            └── layouts/CandidateLayout.tsx    Sidebar + Navbar shell
```

---

## 13. Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| `Connection refused` at port 8086 | Backend not started | Run `./mvnw spring-boot:run` in the `backend/` folder |
| `Failed to load jobs` in browser | Backend not running or wrong port | Check `.env.local` has `VITE_API_URL=http://localhost:8086/api` |
| `FATAL: role "sanjay" does not exist` | DB user not created | Follow PostgreSQL setup in Section 6 |
| `FATAL: database "forge_market_presence" does not exist` | DB not created | Run `CREATE DATABASE forge_market_presence OWNER sanjay;` in psql |
| Job cards show empty location/salary | Pre-seed rows have null new-column values | Restart the backend — `DataSeeder.patchLegacyJobs()` fills nulls automatically |
| `lower(bytea) does not exist` | Hibernate passes untyped null to JPQL filter | Already fixed in repository with `CAST(:param AS String)` |
| Port 5173 already in use | Another Vite dev server running | `lsof -ti:5173 \| xargs kill -9` (macOS/Linux) |
| Port 8086 already in use | Another Spring Boot instance running | `lsof -ti:8086 \| xargs kill -9` (macOS/Linux) |
| Apply button redirects to `/login` | `isAuthenticated` is false in Zustand store | Log in once the auth backend is built, or temporarily set auth state in DevTools console |
| `npm: command not found` | Node.js not installed | Install from https://nodejs.org |
| `java: command not found` | Java 21 not installed or not on PATH | Install from https://adoptium.net and set `JAVA_HOME` |
| Maven build fails with `Unsupported class file major version` | Wrong Java version | Run `java -version` — must be 21; update `JAVA_HOME` if needed |
| Swagger UI shows `No operations defined` | Backend crashed on startup | Check the console for errors — most common cause is the database not running |

### Useful database inspection commands

```bash
# Connect to the database
PGPASSWORD=1234 psql -U sanjay -d forge_market_presence -h localhost

# Inside psql:
\dt                                           -- list all tables
SELECT id, title, status, slug FROM job_postings;
SELECT candidate_name, status, applied_at FROM application_intakes;
SELECT * FROM job_posting_channels;
SELECT referral_code, referred_candidate_name FROM job_referrals;
\q                                            -- exit
```

---

## Seed Data Reference

`DataSeeder` inserts these records on first start (idempotent):

| # | Title | Status | City | Level | Salary |
|---|-------|--------|------|-------|--------|
| 1 | React Frontend Engineer | PUBLISHED | Bangalore | Senior | ₹18–25 L |
| 2 | DevOps Engineer | PUBLISHED | Hyderabad | Mid | ₹15–20 L |
| 3 | QA Automation Engineer | PUBLISHED | Pune | Junior | ₹8–12 L |
| 4 | Data Engineer | PUBLISHED | Bangalore | Senior | ₹20–28 L |
| 5 | Product Manager | PUBLISHED | Chennai | Lead | ₹30–40 L |
| 6 | Business Analyst | **DRAFT** | Hyderabad | Mid | ₹10–15 L |
| 7 | Full Stack Engineer | PUBLISHED | Remote | Mid | $80–120k |
| 8 | Senior Java Developer | **CLOSED** | Pune | Senior | ₹20–30 L |

`GET /api/public/jobs` returns only the 6 PUBLISHED rows.
