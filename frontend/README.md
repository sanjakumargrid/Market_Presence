# 🔷 Forge Careers Platform

A production-grade full-stack **AI-Native Careers Portal** built with React + Spring Boot + Apache Kafka + Docker.

---

## 🏗 Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Docker Network                           │
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌──────────────────┐  │
│  │   Frontend   │    │   Backend    │    │   PostgreSQL 16  │  │
│  │  React + Vite│───▶│  Spring Boot │───▶│   (Primary DB)   │  │
│  │  Nginx :80   │    │  Java 21 :8080│   └──────────────────┘  │
│  └──────────────┘    │              │                           │
│      :3000           │              │    ┌──────────────────┐  │
│                      │              │───▶│    Redis 7       │  │
│                      │              │    │  (Cache Layer)   │  │
│                      │              │    └──────────────────┘  │
│                      │              │                           │
│                      │              │    ┌──────────────────┐  │
│                      │              │───▶│  Apache Kafka    │  │
│                      └──────────────┘    │  (Event Stream)  │  │
│                                          └──────────────────┘  │
│                                                │                │
│                                          ┌──────────────────┐  │
│                                          │   Kafka UI       │  │
│                                          │  (Management)    │  │
│                                          │     :8090        │  │
│                                          └──────────────────┘  │
└─────────────────────────────────────────────────────────────────┘
```

---

## 🚀 Quick Start

### Prerequisites
- Docker Desktop 4.x+
- Docker Compose v2+

### 1. Start Everything

```bash
cd forge-platform
docker compose up -d
```

That's it. Docker will:
1. Start PostgreSQL, Redis, Zookeeper, Kafka
2. Build and launch the Spring Boot backend (auto-runs DB migrations via Flyway)
3. Build and launch the React frontend via Nginx
4. Sync jobs from the external API on first boot

### 2. Access the App

| Service     | URL                          |
|-------------|------------------------------|
| 🖥 Frontend  | http://localhost:3000        |
| 🔌 API       | http://localhost:8080/api    |
| 📊 Kafka UI  | http://localhost:8090        |
| 🐘 PostgreSQL| localhost:5432               |

### 3. Default Accounts

| Role      | Email                  | Password   |
|-----------|------------------------|------------|
| Admin     | admin@forge.ai         | Admin@123  |
| Recruiter | recruiter@forge.ai     | Admin@123  |

---

## 📁 Project Structure

```
forge-platform/
├── docker-compose.yml          # Full stack orchestration
├── Makefile                    # Helper commands
│
├── frontend/                   # React 18 + TypeScript + Vite
│   ├── Dockerfile
│   ├── nginx.conf              # SPA routing + API proxy
│   └── src/
│       ├── api/                # axios, auth, jobs, applications, profile
│       ├── components/         # Navbar, Sidebar, JobCard, ApplicationStepper...
│       ├── features/jobs/      # hooks, services, types
│       ├── pages/              # Home, Jobs, JobDetails, Apply, Applications, Profile
│       ├── routes/             # AppRoutes with protected routes
│       └── store/              # Zustand auth store + localStorage persistence
│
└── backend/                    # Spring Boot 3 + Java 21
    ├── Dockerfile
    └── src/main/java/com/forge/careers/
        ├── config/             # Security, Kafka, Redis/Cache configs
        ├── controller/         # Auth, Jobs, Applications, Profile REST controllers
        ├── dto/                # Request/Response DTOs with Jackson mapping
        ├── entity/             # JPA entities: User, Job, CandidateProfile, Application
        ├── event/              # Kafka event POJOs
        ├── exception/          # GlobalExceptionHandler + custom exceptions
        ├── kafka/
        │   ├── producer/       # CareerEventProducer
        │   └── consumer/       # CareerEventConsumer
        ├── repository/         # Spring Data JPA repositories
        ├── service/            # AuthService, JobService, ApplicationService, ProfileService
        └── util/               # JwtUtil
```

---

## 🔌 Backend API Reference

### Authentication
```
POST   /api/auth/register          Register new candidate
POST   /api/auth/login             Login → returns JWT
```

### Jobs (Public)
```
GET    /api/jobs                   All published jobs (array — matches external API)
GET    /api/jobs/search            Paginated search with filters
GET    /api/jobs/departments       List of departments
GET    /api/jobs/stats             { published: N }
GET    /api/jobs/:slug             Single job by slug
```

### Applications (JWT required)
```
POST   /api/applications/jobs/:slug/apply    Submit application (multipart)
GET    /api/applications/mine                My applications
GET    /api/applications/mine/stats          Dashboard stats
GET    /api/applications/jobs/:slug/check    Already applied?
PATCH  /api/applications/:id/status         Update status (recruiter/admin)
```

### Profile (JWT required)
```
GET    /api/profile               Get my profile
PUT    /api/profile               Update profile
POST   /api/profile/resume        Upload resume file
```

---

## 📨 Kafka Topics

| Topic                       | Trigger                        | Consumer Action             |
|-----------------------------|--------------------------------|-----------------------------|
| `application-submitted`     | New application submitted      | Email confirmation, analytics |
| `application-status-updated`| Status changed by recruiter    | Candidate notification email |
| `job-viewed`                | Job detail page visited        | View count, analytics        |
| `email-notification`        | Any email needed               | SendGrid / SES integration   |

Inspect topics live at **http://localhost:8090** (Kafka UI).

---

## 🛠 Development

### Run infrastructure only, develop locally:

```bash
# Terminal 1 — start infra
make infra

# Terminal 2 — backend
cd backend
mvn spring-boot:run

# Terminal 3 — frontend
cd frontend
npm install
npm run dev          # http://localhost:5173
```

### Environment variables (frontend)
Create `frontend/.env.local`:
```env
VITE_API_URL=http://localhost:8080/api
```

### Environment variables (backend)
Set in `docker-compose.yml` or your shell:
```env
DB_HOST=localhost
DB_PORT=5432
DB_NAME=forge_careers
DB_USER=forge
DB_PASSWORD=forge123
REDIS_HOST=localhost
KAFKA_BROKERS=localhost:9092
JWT_SECRET=your-secret-key
CORS_ORIGINS=http://localhost:5173
```

---

## 🔐 Security

- JWT Bearer token authentication (HS256, 24h expiry)
- BCrypt password hashing (cost factor 12)
- Spring Security method-level authorization (`@PreAuthorize`)
- CORS configured per environment
- Non-root Docker containers
- File upload validation (type + size)

---

## 📦 Tech Stack

| Layer      | Technology                                        |
|------------|---------------------------------------------------|
| Frontend   | React 18, TypeScript, Vite, Tailwind CSS          |
| State      | TanStack Query, Zustand, React Hook Form + Zod    |
| Backend    | Spring Boot 3.2, Java 21, Spring Security         |
| Database   | PostgreSQL 16, Flyway migrations                  |
| Cache      | Redis 7, Spring Cache                             |
| Messaging  | Apache Kafka 3.6, Spring Kafka                    |
| Auth       | JWT (jjwt), BCrypt                                |
| Container  | Docker, Docker Compose, Nginx                     |
| ORM        | Spring Data JPA, Hibernate                        |

---

## 🧪 Useful Commands

```bash
make up            # Start full stack
make down          # Stop
make logs          # All logs
make logs-back     # Backend logs only
make infra         # Only databases + Kafka
make clean         # Remove everything including volumes
make status        # Show container status
```

---

## 📝 Notes

- On first boot, the backend automatically **syncs jobs** from `https://mock-api-pido.onrender.com/api/jobs` and stores them in PostgreSQL. This runs every 30 minutes.
- The `GET /api/jobs` endpoint returns data in the **same shape** as the external API, so the frontend works identically with both.
- File uploads are stored in a Docker volume at `/uploads/resumes`.
- The frontend gracefully **falls back to localStorage** if the backend is unreachable (useful for demos without a running backend).
# careerPortal
