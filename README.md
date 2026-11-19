# Aplikacja trenerska

Projekt studencki (UE Katowice). Stack: Spring Boot, PostgreSQL/TimescaleDB, React, Docker.

Wersja online: **[aplikacja-trenerska.vercel.app](https://aplikacja-trenerska.vercel.app)**.

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

### Domyślne konto do lokalnych testów

Lokalny Compose domyślnie uruchamia profile `docker,dev` i tworzy aktywne konto:

- E-mail: **`dev@example.test`**
- Hasło: **`devdevdev`**

Nie wymaga rejestracji ani potwierdzenia wiadomości. Zapisane plany pozostają w bazie. Kolejny start nie zmienia istniejącego konta, hasła, roli ani danych; po usunięciu konta zostanie ono utworzone przy następnym starcie z profilem `dev`. Przy lokalnym uruchomieniu Maven można włączyć je przez `-Dspring-boot.run.profiles=dev`.

Tworzenie tego konta działa wyłącznie w profilu `dev`. Poza lokalnymi testami ustaw `SPRING_PROFILES_ACTIVE=docker` (bez `dev`); wyłączenie profilu nie usuwa konta już zapisanego w dotychczasowej bazie.

Po zalogowaniu dostępne są zakładki **Ćwiczenia** (`/cwiczenia`) i **Moje plany** (`/plany`). Katalog obsługuje filtry nazwy, partii i typu. Przycisk **Nowy plan** otwiera kreator dni i ćwiczeń z seriami, powtórzeniami i ciężarem. W szczegółach można edytować lub usunąć plan po potwierdzeniu; strzałki w formularzu zmieniają kolejność dni i ćwiczeń.

**Trening** (`/trening`) zapisuje wykonane serie wybranego dnia planu. **Progres** (`/progres`) zawiera wagę, obwody, historię i wykresy. W **Współpracy** (`/wspolpraca`) przyjmujesz zaproszenia i otrzymujesz plany. Osoby z rolą `TRAINER` mają też **Panel trenera** (`/trener`): grupy, zaproszenia, przypisywanie planów i podgląd progresu po zgodzie podopiecznego. Przypisany plan jest niezależną kopią na koncie odbiorcy.

Lokalna TimescaleDB przechowuje wyniki i pomiary w prawdziwych hypertabelach. Supabase PostgreSQL 17 używa zgodnych zwykłych tabel z indeksami; API działa tak samo. Migracje V5–V7 wykonuje automatycznie Flyway. Instrukcja i kontrakty nowych modułów: [SPRINT3.md](docs/SPRINT3.md).

## Uruchomienie lokalne (dev)

1. Baza i poczta: `docker compose up db mailpit` albo lokalny PostgreSQL i serwer SMTP.
2. Backend: `cd backend` → `mvnw spring-boot:run`
3. Frontend: `cd frontend` → `npm install` → `npm run dev` (proxy `/api` → port 8080)

Lokalny frontend działa na http://localhost:5173, a Mailpit na http://localhost:8025. Backend domyślnie łączy się z SMTP `localhost:1025`.

## Konfiguracja kont i poczty

| Zmienna środowiskowa | Domyślna wartość lokalna | Zastosowanie |
|---|---|---|
| `APP_FRONTEND_URL` | `http://localhost:5173` | Adres aplikacji używany w linkach e-mail; w Compose `http://localhost:3000` |
| `APP_MAIL_MODE` | `smtp` | Transport poczty: SMTP lub techniczny `log` bez wysyłki |
| `APP_MAIL_FROM` | `no-reply@aplikacja.local` | Nadawca wiadomości |
| `SPRING_MAIL_HOST` | `localhost` | Host SMTP; w Compose `mailpit` |
| `SPRING_MAIL_PORT` | `1025` | Port SMTP |
| `SPRING_MAIL_USERNAME` / `SPRING_MAIL_PASSWORD` | puste | Dane logowania SMTP |
| `SMTP_AUTH` | `false` | Uwierzytelnienie SMTP |
| `SMTP_STARTTLS_ENABLE` | `false` | STARTTLS; ustaw zgodnie z wymaganiami dostawcy SMTP |
| `APP_ACCOUNT_EMAIL_CONFIRMATION_TTL` | `24h` | Ważność linku aktywacyjnego |
| `APP_ACCOUNT_PASSWORD_RESET_TTL` | `30m` | Ważność linku resetu hasła |
| `APP_ACCOUNT_ACCOUNT_DELETION_TTL` | `30m` | Ważność linku usunięcia konta |

Wiadomości lokalne odbiera Mailpit. Dla wdrożenia ustaw adres frontendu z HTTPS, własny sekret JWT (`APP_JWT_SECRET`) oraz dane SMTP. Przy pracy lokalnej na innym porcie lub przez zewnętrzny adres zmień `APP_FRONTEND_URL`.

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

Schemat ćwiczeń i planów (V3), katalog 48 ćwiczeń (V4), kaskady usuwania i model JPA: [WORKOUT_MODEL.md](docs/WORKOUT_MODEL.md). Pełne testy backendu na jednorazowym PostgreSQL 16 oraz sprawdzenie aktualizacji V2 → V3 → V4: `pwsh -NoProfile -File tests/test-workout-postgres.ps1` (wymaga Dockera, używa JDK 21 w kontenerze). Katalog dodaje się automatycznie przy starcie backendu przez Flyway.

Test pełnego stacku Docker (z katalogu głównego repozytorium):

```powershell
docker compose up -d --build
node tests/smoke-accounts.mjs
```

Poczekaj, aż `http://localhost:3000/api/health` zwróci `UP`. Skrypt sprawdza API przez nginx, prawdziwą wysyłkę SMTP do Mailpit oraz jednorazowość tokenów przy równoległym resecie na PostgreSQL. Używa losowego konta testowego, które usuwa po sukcesie. Wiadomości testowe pozostają w Mailpit. Przy błędzie wypisuje etap i adres konta do diagnostyki; może wtedy pozostać konto testowe.

Wrapper `backend/mvnw` musi mieć końce linii LF, zgodnie z `.gitattributes`, aby działał w kontenerze Linux.

## Wdrożenie na Vercel i Supabase

Stan na 19 listopada 2026: frontend działa pod adresem podanym wyżej, a backend pod [aplikacja-trenerska-api.vercel.app](https://aplikacja-trenerska-api.vercel.app). Oba projekty Vercel wdrażają `main`; opublikowano także progres i moduł trenera ze Sprintu 3. Backend Java 21 używa Container Runtime w Dublinie (`dub1`), a Supabase PostgreSQL 17 w `eu-west-1` przechowuje prywatny schemat `training_app` z migracjami V1–V7 i katalogiem 48 ćwiczeń. Flyway potwierdził aktualizację V4 → V7, API zwraca `UP`, a nowe trasy i ochrona endpointów przeszły sprawdzenie online. Przy pierwszym wdrożeniu przeniesiono jedno istniejące konto `USER`; dane konta i kopie zapasowe pozostają poza repozytorium.

Profil backendu `prod` oddziela ustawienia wdrożenia od lokalnych profili `docker,dev`. Frontend na Vercel wymaga publicznego adresu backendu w `VITE_API_URL`; lokalny pusty adres zachowuje proxy `/api`.

Pocztę konfiguruje backend. `APP_MAIL_MODE=smtp` wysyła wiadomości przez SMTP, lokalnie do Mailpit. Techniczny transport `log` zapisuje poufne linki jako `[MAIL LINK]` w prywatnych logach i nie dostarcza e-maili. W ostatnim sprawdzonym wdrożeniu używano `log`; wysyłka online wymaga konfiguracji SMTP. Frontend korzysta z jednego standardowego przepływu konta.

Konfiguracja i [checklista wdrożenia](docs/DEPLOYMENT.md#5-sprawdź-wdrożenie) rozróżniają wykonane sprawdzenia online od lokalnych testów pełnych przepływów konta i planów. Ręczny odbiór tych przepływów online na osobnym fikcyjnym koncie pozostaje do wykonania. Dokładne zmienne, SMTP, migracje, CORS i ustawienia paneli: [DEPLOYMENT.md](docs/DEPLOYMENT.md).

## Dokumentacja

- [PLAN.md](docs/PLAN.md) — zakres projektu  
- [SPRINTY.md](docs/SPRINTY.md) — harmonogram  
- [BACKEND_STRUCTURE.md](docs/BACKEND_STRUCTURE.md) — układ pakietów  
- [ACCOUNTS.md](docs/ACCOUNTS.md) — konta, ochrona sesji, API i obsługa wiadomości
- [WORKOUT_MODEL.md](docs/WORKOUT_MODEL.md) — model ćwiczeń i planów, kolejność, integralność i testy
- [EXERCISES_API.md](docs/EXERCISES_API.md) — odczyt katalogu ćwiczeń z filtrami, format odpowiedzi i autoryzacja
- [WORKOUT_PLANS_API.md](docs/WORKOUT_PLANS_API.md) — CRUD własnych planów, dni i ćwiczeń, walidacja i testy uprawnień
- [WORKOUT_FRONTEND.md](docs/WORKOUT_FRONTEND.md) — listy, kreator, edycja i usuwanie planów, walidacja i pełny test przeglądarkowy
- [SPRINT3.md](docs/SPRINT3.md) — wyniki, pomiary, wykresy, grupy, zaproszenia i przypisywanie planów; API i testy
- [DEPLOYMENT.md](docs/DEPLOYMENT.md) — Vercel, Supabase, SMTP, zmienne środowiskowe i sprawdzenie wdrożenia
