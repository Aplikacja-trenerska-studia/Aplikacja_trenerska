# Aplikacja trenerska

Projekt studencki (UE Katowice). Stack: Spring Boot, PostgreSQL/TimescaleDB, React, Docker.

## Wymagania

- Java 21, Maven (wrapper w `backend/`)
- Node.js 22+ (frontend)
- Docker Desktop (opcjonalnie — pełny stack)

## Uruchomienie (Docker)

```bash
docker compose up --build
```

- Frontend: http://localhost:3000  
- Backend API: http://localhost:8080/api/health  
- Baza: `localhost:5432` (użytkownik/hasło/db jak w `backend/src/main/resources/application.properties`)

## Uruchomienie lokalne (dev)

1. Baza: `docker compose up db` albo lokalny PostgreSQL z tą samą nazwą bazy i użytkownikiem co w `application.properties`.
2. Backend: `cd backend` → `mvnw spring-boot:run`
3. Frontend: `cd frontend` → `npm install` → `npm run dev` (proxy `/api` → port 8080)

## Dokumentacja

- [PLAN.md](docs/PLAN.md) — zakres projektu  
- [SPRINTY.md](docs/SPRINTY.md) — harmonogram  
- [BACKEND_STRUCTURE.md](docs/BACKEND_STRUCTURE.md) — układ pakietów  
