# ═══════════════════════════════════════════════════════════════
# Forge Careers Platform — Makefile
# ═══════════════════════════════════════════════════════════════

.PHONY: help up down build logs clean dev-front dev-back

help:
	@echo ""
	@echo "  Forge Careers Platform — Commands"
	@echo "  ─────────────────────────────────────────────────────"
	@echo "  make up          Start entire stack (detached)"
	@echo "  make down        Stop and remove containers"
	@echo "  make build       Rebuild all Docker images"
	@echo "  make logs        Tail all container logs"
	@echo "  make logs-back   Tail backend logs only"
	@echo "  make clean       Remove containers + volumes (⚠ data loss)"
	@echo "  make dev-front   Run frontend in dev mode (requires Node)"
	@echo "  make dev-back    Run backend locally (requires Java 21 + Maven)"
	@echo "  make infra       Start only infra (postgres, redis, kafka)"
	@echo ""

up:
	docker compose up -d

down:
	docker compose down

build:
	docker compose build --no-cache

logs:
	docker compose logs -f

logs-back:
	docker compose logs -f backend

logs-front:
	docker compose logs -f frontend

clean:
	docker compose down -v --remove-orphans

infra:
	docker compose up -d postgres redis zookeeper kafka kafka-ui
	@echo "Infrastructure ready:"
	@echo "  PostgreSQL → localhost:5432"
	@echo "  Redis      → localhost:6379"
	@echo "  Kafka      → localhost:9092"
	@echo "  Kafka UI   → http://localhost:8090"

dev-front:
	cd frontend && npm install && npm run dev

dev-back:
	cd backend && mvn spring-boot:run

status:
	docker compose ps
