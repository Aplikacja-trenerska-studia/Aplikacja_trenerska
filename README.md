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
- Skrzynka testowa Mailpit: http://localhost:8025 (przechwytuje wiadomości, nie wysyła ich na zewnętrzne adresy)

Po rejestracji otwórz wiadomość w Mailpit i potwierdź konto, zanim się zalogujesz. Linki w wiadomościach z konfiguracji Docker prowadzą na port 3000.

## Uruchomienie lokalne (dev)

1. Baza i poczta: `docker compose up db mailpit` albo lokalny PostgreSQL i serwer SMTP.
2. Backend: `cd backend` → `mvnw spring-boot:run`
3. Frontend: `cd frontend` → `npm install` → `npm run dev` (proxy `/api` → port 8080)

Lokalny frontend działa na http://localhost:5173, a Mailpit na http://localhost:8025. Backend domyślnie łączy się z SMTP `localhost:1025`.

## Konfiguracja kont i poczty

| Zmienna środowiskowa | Domyślna wartość lokalna | Zastosowanie |
|---|---|---|
| `APP_FRONTEND_URL` | `http://localhost:5173` | Adres aplikacji używany w linkach e-mail; w Compose `http://localhost:3000` |
| `APP_MAIL_FROM` | `no-reply@aplikacja.local` | Nadawca wiadomości |
| `SPRING_MAIL_HOST` | `localhost` | Host SMTP; w Compose `mailpit` |
| `SPRING_MAIL_PORT` | `1025` | Port SMTP |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | puste | Dane logowania SMTP |
| `SMTP_AUTH` | `false` | Uwierzytelnienie SMTP |
| `SMTP_STARTTLS_ENABLE` | `false` | STARTTLS; ustaw zgodnie z wymaganiami dostawcy SMTP |
| `APP_ACCOUNT_EMAIL_CONFIRMATION_TTL` | `24h` | Ważność linku aktywacyjnego |
| `APP_ACCOUNT_PASSWORD_RESET_TTL` | `30m` | Ważność linku resetu hasła |
| `APP_ACCOUNT_ACCOUNT_DELETION_TTL` | `30m` | Ważność linku usunięcia konta |

Konfiguracja lokalna z Mailpit nie dostarcza wiadomości do rzeczywistych skrzynek. Dla wdrożenia ustaw produkcyjny SMTP, adres frontendu z HTTPS i własny sekret JWT (`APP_JWT_SECRET`). Przy pracy lokalnej na innym porcie lub przez zewnętrzny adres zmień `APP_FRONTEND_URL`.

## Weryfikacja zmian

```powershell
cd backend
.\mvnw.cmd test
cd ../frontend
npm run build
npm run lint
npm test
```

Opis API i scenariusze sprawdzenia aktywacji, resetu hasła i usuwania konta: [Konta użytkowników](docs/ACCOUNTS.md).

Nadawanie i odbieranie roli trenera: [Role USER / TRAINER](docs/ACCOUNTS.md#role-user--trainer). Test skryptu operatora na jednorazowej bazie PostgreSQL: `pwsh -NoProfile -File tests/smoke-roles.ps1` (wymaga Dockera).

Test pełnego stacku Docker (z katalogu głównego repozytorium):

```powershell
docker compose up -d --build
node tests/smoke-accounts.mjs
```

Poczekaj, aż `http://localhost:3000/api/health` zwróci `UP`. Skrypt sprawdza API przez nginx, prawdziwą wysyłkę SMTP do Mailpit oraz jednorazowość tokenów przy równoległym resecie na PostgreSQL. Używa losowego konta testowego, które usuwa po sukcesie. Wiadomości testowe pozostają w Mailpit. Przy błędzie wypisuje etap i adres konta do diagnostyki; może wtedy pozostać konto testowe.

Wrapper `backend/mvnw` musi mieć końce linii LF, zgodnie z `.gitattributes`, aby działał w kontenerze Linux.

## Dokumentacja

- [PLAN.md](docs/PLAN.md) — zakres projektu  
- [SPRINTY.md](docs/SPRINTY.md) — harmonogram  
- [BACKEND_STRUCTURE.md](docs/BACKEND_STRUCTURE.md) — układ pakietów  
- [ACCOUNTS.md](docs/ACCOUNTS.md) — konta, ochrona sesji, API i obsługa wiadomości
