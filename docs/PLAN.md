# Aplikacja trenerska — plan projektu

> Webowa aplikacja do planowania treningów i śledzenia progresu.
> Projekt programistyczny, Uniwersytet Ekonomiczny w Katowicach, rok akademicki 2026/2027.

| | |
|---|---|
| **Zespół** | Hubert Muszalski, Marcin Szeja, Mateusz Dudek, Mateusz Trzaska |
| **Termin oddania** | **19 grudnia 2026** |
| **Platforma** | Aplikacja webowa, responsywna, głównie pod telefony (mobile-first) |

> **Legenda:** sekcje oznaczone 📄 pochodzą wprost z formularza SMART. Sekcje oznaczone 💡 to propozycje techniczne (do uzgodnienia w zespole).

> **Stan checklist: 16.11.2026.** Trwa Sprint 2 (16–22.11); konta i plany są technicznie gotowe. Wdrożenie na Vercel i Supabase należy do zadania **S3-11 w Sprincie 3 (23–29.11)** i zostało wykonane z wyprzedzeniem **16.11.2026**. Kod produkcyjny jest w `main`. `[x]` potwierdza opisany zakres techniczny; zespołowe review i Definition of Done całego projektu nadal wymagają osobnego potwierdzenia. Szczegóły: [SPRINTY.md](SPRINTY.md).

---

## 1. Cel projektu 📄

Stworzyć webową aplikację trenerską, która pomaga użytkownikowi:

- tworzyć **plany treningowe**,
- kontrolować **progres siłowy i sylwetkowy**,
- mieć **kontakt z trenerem** (opcjonalnie) i znajomymi.

Projekt odpowiada na realną potrzebę osób trenujących: w jednym miejscu planować treningi, monitorować postępy i kontaktować się z trenerem oraz znajomymi.
Dla zespołu to także nauka pełnego cyklu tworzenia aplikacji: backend + frontend + baza danych (w tym szeregi czasowe w TimescaleDB) + zabezpieczenia (autoryzacja i autentykacja) + wdrożenie w Dockerze.

---

## 2. Stack technologiczny 📄

| Warstwa | Technologia | Status |
|---|---|---|
| Frontend | React + TypeScript + Vite, Tailwind CSS | ✅ ustalone |
| Backend | Java + **Spring Boot** | ✅ ustalone |
| Baza danych | **PostgreSQL**; lokalnie TimescaleDB, online Supabase PostgreSQL 17 | ✅ obecne moduły; technologia pomiarów online do decyzji przed S3 |
| Wdrożenie | **Docker Compose** lokalnie; **Vercel + Supabase** online | ✅ działa; produkcja z `main`, poczta w trybie demo |

### Decyzja dotycząca frontendu

W sprincie 1 wybrano **React + Vite**. Frontend używa TypeScriptu, React Routera i Tailwind CSS. Dalsze ekrany rozwijamy w tym stosie.

---

## 3. Architektura 💡

```
┌──────────────┐     HTTPS/REST (JSON)     ┌───────────────────┐      JDBC      ┌──────────────────────────┐
│  Frontend    │ ────────────────────────► │  Backend          │ ─────────────► │  PostgreSQL + Timescale  │
│  (SPA, PWA)  │ ◄──────────────────────── │  Spring Boot      │ ◄───────────── │  (dane relacyjne +       │
│  mobile-first│       JWT w nagłówku      │  Spring Security  │                │   hypertables: pomiary)  │
└──────────────┘                           └───────────────────┘                └──────────────────────────┘
        └──────────────── każdy element w osobnym kontenerze Docker ───────────────────┘
```

### Proponowana struktura repozytorium

```
/
├── backend/                 # Spring Boot (Maven/Gradle)
│   └── src/main/java/.../
│       ├── auth/            # rejestracja, logowanie, JWT, reset hasła
│       ├── user/            # konta, profile, role
│       ├── trainer/         # grupy trenerskie, przypisywanie planów
│       ├── workout/         # plany treningowe, ćwiczenia
│       ├── progress/        # wyniki ćwiczeń, waga, obwody ciała
│       ├── calc/            # kalkulatory: kalorie, makro, 1RM
│       ├── calendar/        # kalendarz, przypomnienia
│       ├── social/          # wiadomości, DM, znajomi
│       └── events/          # wydarzenia sportowe
├── frontend/                # SPA (framework do wyboru)
├── db/
│   └── migrations/          # Flyway / Liquibase
├── docker-compose.yml
├── .env.example
├── PLAN.md                  # ten plik
└── README.md
```

---

## 4. Zakres funkcjonalny 📄 + priorytety 💡

Priorytety (zgodnie z założeniem etapowej realizacji z formularza):

- **P1** — rdzeń, musi być na termin,
- **P2** — w drugiej kolejności,
- **P3** — tylko jeśli zostanie czas.

### M0. Infrastruktura (P1)

- [x] Repozytorium Git i README z instrukcją uruchomienia
- [x] Konwencja branchy `feat/*`, `fix/*`, `docs/*` i Conventional Commits; produkcja wdrażana z `main`
- [ ] Potwierdzenie zespołowego procesu PR i review zmian
- [x] `docker-compose.yml`: `db` (TimescaleDB), `backend`, `frontend`, lokalny SMTP Mailpit
- [x] Flyway: migracje V1/V2 kont i tokenów, V3 ćwiczeń i planów, V4 katalogu ćwiczeń oraz `.env.example`
- [x] Aplikacja uruchamia się jedną komendą: `docker compose up --build`
- [x] Layout responsywny (mobile-first) obecnych ekranów kont, sprawdzony na komputerze i w emulowanym widoku mobilnym
- [x] Frontend i backend na Vercelu, baza Supabase w schemacie `training_app`, HTTPS i CORS (S3-11, wykonano z wyprzedzeniem 16.11.2026)
- [x] Migracje V1–V4 i 48 ćwiczeń w Supabase; przeniesienie jedynego lokalnego konta z kopiami zapasowymi poza GitHubem
- [x] Automatyczne wdrożenia obu projektów z `main`; `feat/deployment` scalony i usunięty
- [x] Demonstracja bez SMTP: linki kont w prywatnych logach backendu, odpowiednie komunikaty frontendu
- [x] Sprawdzenia online: strony SPA, API `UP`, odczyt bazy, ochrona tras i CORS

Adres aplikacji: [aplikacja-trenerska.vercel.app](https://aplikacja-trenerska.vercel.app). Konfiguracja i zakres potwierdzonych testów: [DEPLOYMENT.md](DEPLOYMENT.md).

### M1. Konta użytkowników i autoryzacja (P1)

- [x] Rejestracja z potwierdzeniem konta przez e-mail
- [x] Logowanie / wylogowanie (JWT + Spring Security)
- [x] Role `USER`, `TRAINER`, `ADMIN` w modelu i obsłudze sesji; rejestracja tworzy konto `USER`
- [x] Sposób nadawania roli `TRAINER` przez operatora (`tools/set-user-role.sql`), unieważnienie sesji po zmianie roli i testy rozróżniające uprawnienia ról (S2-04, 15.11.2026)
- [x] Przypomnienie hasła (reset przez e-mail/token)
- [x] Usunięcie konta po ponownym podaniu hasła i potwierdzeniu przez e-mail
- [x] Ochrona obecnych endpointów użytkownika przez JWT i `@PreAuthorize`, ochrona trasy `/profil`
- [x] Autoryzacja katalogu i CRUD planów według ról i właściciela z sesji; testy odmowy dostępu do cudzych planów
- [ ] Autoryzacja przyszłych endpointów modułu trenera i kolejnych modułów podczas ich wdrażania

Implementacja kont i kontrakt API: [ACCOUNTS.md](ACCOUNTS.md). Reset hasła unieważnia wcześniejsze JWT; jednorazowe tokeny mają przeznaczenie, termin ważności i skrót w bazie. Zaliczone sprawdzenia backendu na H2 i PostgreSQL 16: 111 testów (15 kont, 7 ról, 25 modelu planów, 3 katalogu, 19 API ćwiczeń, 41 API planów i uruchomienie kontekstu), w tym CRUD i izolacja własnych planów, walidacja oraz usuwanie konta z planem przy zachowaniu katalogu. Sprawdzono także aktualizacje V2 → V3 → V4 z istniejącymi danymi. Wcześniej sprawdzono 4 testy pomocniczych funkcji frontendu, build i lint frontendu oraz przepływy kont przez nginx z PostgreSQL i rzeczywistą wysyłką SMTP do Mailpit (`tests/smoke-accounts.mjs`). Potwierdzono również obejście crasha Brave podczas wpisywania nicku (`autocomplete="off"`).

Mailpit przechwytuje wiadomości lokalnie. Wdrożenie demonstracyjne korzysta z `APP_MAIL_MODE=log`: linki trafiają do prywatnych logów API, a frontend pokazuje tryb demo. Rzeczywista wysyłka wymaga zewnętrznego SMTP. Pełne przepływy kont i planów są sprawdzone lokalnie; ich powtórzenie online na koncie testowym pozostaje na checkliście odbioru wdrożenia w Sprincie 3 (S3-11). Najnowsze sprawdzenie lokalne konfiguracji obejmuje 157 testów backendu, 21 testów frontendu, build i lint.

### M2. Moduł użytkownika — plany treningowe (P1)

- [x] Fundament modelu: migracja V3, encje i repozytoria ćwiczeń, planów, dni i wpisów ćwiczeń; właściciel, kolejność i kaskady usuwania (S2-05)
- [x] **Dane bazy ćwiczeń** — 48 wpisów z polską nazwą, grupą mięśniową, opisem i typem siłowe / domowe (V4, S2-06); widok katalogu gotowy w S2-08
- [x] API odczytu katalogu: `GET /api/exercises` z filtrami grupy mięśniowej, typu i fragmentu nazwy; dostęp przez JWT (część S2-07)
- [x] API CRUD własnych planów: tworzenie, lista, szczegóły, pełna edycja i usuwanie wraz z testami HTTP (S2-07, S2-11)
- [x] Frontend listy ćwiczeń z filtrami i listy własnych planów oraz podgląd szczegółów, chronione trasami sesji (S2-08)
- [x] Tworzenie własnych planów treningowych przez frontend (S2-09)
- [x] Kreator: plan → dni treningowe → ćwiczenia (serie, powtórzenia, ciężar), zmiana kolejności i walidacja zgodna z API
- [x] Edycja i usuwanie planów przez frontend z potwierdzeniem usuwania i zachowaniem szkicu przy błędzie zapisu

Schemat, katalog i zasady integralności: [WORKOUT_MODEL.md](WORKOUT_MODEL.md). Odczyt katalogu: [EXERCISES_API.md](EXERCISES_API.md). CRUD planów: [WORKOUT_PLANS_API.md](WORKOUT_PLANS_API.md). Listy, kreator, edycja i usuwanie: [WORKOUT_FRONTEND.md](WORKOUT_FRONTEND.md). Build, lint bez ostrzeżeń i 16 testów frontendu przeszły. Pełny przepływ rejestracja → aktywacja → logowanie → utworzenie planu przez UI, a następnie edycja i usuwanie, sprawdzono w Dockerze testem `tests/smoke-workout-ui.mjs`; desktop i mobilny viewport bez błędów JavaScript. Test na rzeczywistym telefonie pozostaje do wykonania.

### M3. Śledzenie postępów (P1)

- [ ] Zapis wyników ćwiczeń (ciężar, powtórzenia, data)
- [ ] Zapis wagi ciała
- [ ] Zapis obwodów ciała
- [ ] Historia i wykresy progresu (dane w hypertables TimescaleDB)

### M4. Moduł trenera (P1/P2)

- [ ] Konto trenera
- [ ] Tworzenie **grup podopiecznych**
- [ ] Przypisywanie planów treningowych do grupy / osoby
- [ ] Podgląd progresu podopiecznych

### M5. Udostępnianie i ocena planów (P2)

- [ ] Udostępnianie planów (link / inni użytkownicy)
- [ ] **Algorytm oceny planów** — *do zdefiniowania w zespole* (np. pokrycie grup mięśniowych, objętość tygodniowa, balans push/pull)

### M6. Moduły dodatkowe (P2/P3)

| Moduł | Opis | Priorytet |
|---|---|---|
| **Kalkulator kalorii/makro** | Mini-moduł w stylu Fitatu: średnie zapotrzebowanie kaloryczne (np. Mifflin-St Jeor), podział makroskładników | P2 |
| **Kalkulator 1RM** | Maksymalny ciężar dla ćwiczeń (np. wzór Epleya) | P2 |
| **Kalendarz i przypomnienia** | Planowanie treningów w kalendarzu, powiadomienia | P2 |
| **Statystyki** | Podsumowania treningów, objętość, rekordy | P2 |
| **Social media** | Tablica, wiadomości, DM | P3 |
| **Wydarzenia sportowe** | Lista wydarzeń, np. Silesia Maraton | P3 |

---

## 5. Model danych (szkic) 💡

```sql
-- Użytkownicy i role
users            (id, email, password_hash, display_name, role, enabled, created_at)
verification_tokens (id, user_id, token, type, expires_at)   -- potwierdzenie konta / reset hasła

-- Trener i grupy
training_groups  (id, trainer_id → users, name, created_at)
group_members    (group_id → training_groups, user_id → users)

-- Ćwiczenia i plany
exercises        (id, name, muscle_group, type, description, created_by)
workout_plans    (id, owner_id → users, name, description, is_shared, created_at)
plan_days        (id, plan_id → workout_plans, day_index, name)
plan_exercises   (id, plan_day_id → plan_days, exercise_id, sets, reps, weight, order_index)
plan_assignments (id, plan_id, user_id NULL, group_id NULL, assigned_by, assigned_at)

-- Szeregi czasowe → TimescaleDB hypertables
exercise_results (time TIMESTAMPTZ, user_id, exercise_id, set_no, reps, weight)
body_metrics     (time TIMESTAMPTZ, user_id, weight_kg, chest_cm, waist_cm, hips_cm, arm_cm, thigh_cm)

SELECT create_hypertable('exercise_results', 'time');
SELECT create_hypertable('body_metrics', 'time');
```

---

## 6. API REST (szkic) 💡

| Metoda | Endpoint | Opis | Rola |
|---|---|---|---|
| POST | `/api/auth/register` | Rejestracja | — |
| POST | `/api/auth/confirm` | Potwierdzenie konta (`token` w JSON) | — |
| POST | `/api/auth/resend-confirmation` | Ponowienie wiadomości aktywacyjnej | — |
| POST | `/api/auth/login` | Logowanie → JWT | — |
| POST | `/api/auth/forgot-password` | Prośba o reset hasła | — |
| POST | `/api/auth/reset-password` | Ustawienie nowego hasła | — |
| GET | `/api/users/me` | Dane własnego konta | Zalogowany |
| POST | `/api/users/me/deletion-request` | Prośba o usunięcie konta (`password` w JSON), wysyłka e-maila | Zalogowany |
| POST | `/api/auth/confirm-deletion` | Usunięcie konta po potwierdzeniu (`token` w JSON) | — |
| GET | `/api/exercises?muscleGroup=&type=&q=` | Katalog z filtrami grupy, typu i nazwy — wdrożony | USER / TRAINER / ADMIN |
| GET/POST | `/api/plans` | Lista / tworzenie własnych planów — wdrożone | USER / TRAINER / ADMIN |
| GET/PUT/DELETE | `/api/plans/{id}` | Szczegóły / edycja / usunięcie własnego planu — wdrożone | USER / TRAINER / ADMIN |
| POST | `/api/plans/{id}/share` | Udostępnienie planu | USER |
| POST | `/api/results` | Zapis wyniku ćwiczenia | USER |
| GET | `/api/results?exerciseId=&from=&to=` | Historia wyników | USER |
| POST/GET | `/api/body-metrics` | Waga / obwody | USER |
| POST/GET | `/api/groups` | Grupy trenera | TRAINER |
| POST | `/api/groups/{id}/members` | Dodanie podopiecznego | TRAINER |
| POST | `/api/groups/{id}/assign-plan` | Przypisanie planu | TRAINER |
| GET | `/api/calc/1rm?weight=&reps=` | Kalkulator 1RM | USER |
| GET | `/api/calc/calories` | Zapotrzebowanie kaloryczne | USER |

---

## 7. Docker — szkic `docker-compose.yml` 💡

```yaml
services:
  db:
    image: timescale/timescaledb:latest-pg16
    environment:
      POSTGRES_DB: ${DB_NAME}
      POSTGRES_USER: ${DB_USER}
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    volumes:
      - db_data:/var/lib/postgresql/data
    ports:
      - "5432:5432"

  backend:
    build: ./backend
    depends_on:
      - db
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/${DB_NAME}
      SPRING_DATASOURCE_USERNAME: ${DB_USER}
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
    ports:
      - "8080:8080"

  frontend:
    build: ./frontend
    depends_on:
      - backend
    ports:
      - "3000:80"

volumes:
  db_data:
```

---

## 8. Kryteria sukcesu (Measurable) 📄

Postęp oceniamy za pomocą następujących wskaźników:

- [ ] **≥ 6 modułów** zrealizowanych (w stosunku do zaplanowanych w zakresie projektu)
- [ ] **Działające, przetestowane przepływy kluczowe:**
  - [x] rejestracja, potwierdzenie e-maila i logowanie; dodatkowo reset hasła i usuwanie konta
  - [x] utworzenie planu treningowego przez kreator, edycja i usunięcie z potwierdzeniem
  - [ ] zapis wyniku treningu
  - [ ] utworzenie grupy przez trenera i przypisanie planu podopiecznemu
- [ ] Aplikacja **poprawnie wyświetla się i działa na komputerach i urządzeniach mobilnych** — ekrany kont sprawdzone na komputerze i w emulacji; pozostałe moduły i test na prawdziwym telefonie jeszcze przed zespołem
- [x] Obecna aplikacja **uruchamia się w kontenerach Docker** (backend, baza danych, frontend, Mailpit); migracje V1–V4 oraz przepływy kont i planów sprawdzone
- [x] Obecna aplikacja jest dostępna online na Vercelu z bazą Supabase i wdrożeniami produkcyjnymi z `main`; sprawdzono dostępność, odczyt bazy, CORS i ochronę tras (S3-11, wykonano 16.11.2026)
- [ ] **Cotygodniowe podsumowanie postępów** w zespole, z odniesieniem do listy zadań

---

## 9. Harmonogram 💡

Termin końcowy: **sobota 19.12.2026**. Zostało ok. **6 tygodni**.

Harmonogram jest zgodny z operacyjnym podziałem zadań w [SPRINTY.md](SPRINTY.md); tam znajdują się identyfikatory zadań, statusy i raporty.

| Sprint | Daty | Cel | Zakres |
|---|---|---|---|
| **S1** | 9–15.11 | Fundament i logowanie | Repo, Docker Compose, migracje, React + Vite, rejestracja i logowanie (M0, część M1) |
| **S2** | 16–22.11 | Konta i plany | Potwierdzenie e-maila, reset hasła, usuwanie konta, role (M1); baza ćwiczeń, CRUD planów i widoki mobile (M2) |
| **S3** | 23–29.11 | Progres, trener i wdrożenie | Wyniki, waga, obwody, wykresy (M3); grupy trenera i przypisywanie planów (M4); Vercel i Supabase (S3-11, wykonano z wyprzedzeniem 16.11) |
| **S4** | 30.11–6.12 | Dodatki | Udostępnianie planów, kalkulatory, kalendarz, statystyki (M5, M6) |
| **S5** | 7–13.12 | Stabilizacja | Algorytm oceny planów, testy mobile, poprawki, dokumentacja i dane demo |
| **Bufor** | 14–19.12 | Oddanie | Tylko poprawki, dokumentacja, finalny `docker compose up` i oddanie |

> **Zasada:** po Sprincie 4 zamrażamy nowe moduły. Sprint 5 i bufor służą stabilizacji; priorytety P1 muszą być ukończone przed dodatkami.

---

## 10. Ograniczenia i ryzyka 📄 + 💡

| Ryzyko | Wpływ | Działanie |
|---|---|---|
| Stały termin oddania (19.12) | Wysoki | Priorytety P1/P2/P3; moduły dodatkowe mogą zostać pominięte (np. moduł Fitatu) |
| Brak doświadczenia z frameworkiem frontendowym | Wysoki | Szybki wybór w T1, nauka na małych zadaniach, gotowa biblioteka UI |
| Szeroki zakres funkcjonalny | Średni | Realizacja etapami: **1)** web + konta + plany trenera/użytkownika, **2)** kalendarz, kalkulatory, statystyki, udostępnianie, **3)** social, mini-Fitatu, wydarzenia |
| Nowa technologia: TimescaleDB | Średni | Standardowy PostgreSQL + `create_hypertable` tylko dla tabel pomiarów |
| TimescaleDB w hostingu Supabase | Wysoki dla S3 | Obecne wdrożenie używa PostgreSQL 17 bez TimescaleDB; przed modułem progresu uzgodnić rozwiązanie pomiarów lub inny hosting bazy ([szczegóły](DEPLOYMENT.md#preview-i-kolejne-migracje)) |
| Konfiguracja Dockera | Średni | Docker Compose uruchomiony już w T1, nie na koniec |

---

## 11. Zasoby i kompetencje 📄

- **Znane technologie:** Java, Spring Boot, PostgreSQL, Docker
- **Doświadczenie zespołu:** Java i PHP, konteneryzacja, bazy danych
- **Do nauczenia:** wybrany framework frontendowy, projektowanie stron responsywnych, TimescaleDB

---

## 12. Definition of Done 💡

To kryteria do sprawdzenia dla każdego zadania przed jego zamknięciem; poniższe pola nie są zbiorczym raportem wykonania projektu. Status implementacji jest w sekcji 4 i w [SPRINTY.md](SPRINTY.md).

Zadanie uznajemy za skończone, gdy:

- [ ] kod jest w `main` przez Pull Request z review co najmniej jednej osoby,
- [ ] funkcja działa w Dockerze (`docker compose up`),
- [ ] działa poprawnie na telefonie i na komputerze,
- [ ] endpointy mają podstawowe testy (min. happy path),
- [ ] zmiany w bazie są w migracjach,
- [ ] README / dokumentacja zaktualizowane, jeśli coś się zmieniło.

---

## 13. Pierwsze kroki (Sprint 0) 💡

1. [x] Założyć repozytorium i dodać plan w `docs/PLAN.md`
2. [x] Wybrać framework frontendowy — React + Vite
3. [x] Zainicjować projekt Spring Boot (Spring Web, Security, Data JPA, PostgreSQL Driver, Validation, Flyway)
4. [x] Zainicjować projekt frontendowy
5. [x] Napisać `docker-compose.yml` z bazą TimescaleDB i sprawdzić połączenie
6. [ ] Podzielić moduły M1–M6 między członków zespołu (kolumna „Osoba" poniżej)
7. [ ] Ustalić stały termin cotygodniowego spotkania

### Podział pracy (do uzupełnienia)

| Moduł | Osoba | Status |
|---|---|---|
| M0 Infrastruktura / Docker | | 🟡 Docker i Vercel/Supabase działają, produkcja z `main`; proces PR i zespołowe review do potwierdzenia |
| M1 Konta i autoryzacja | | ✅ przepływy kont, operatorowe nadawanie roli trenera i testy obecnych uprawnień gotowe; nowe endpointy wymagają zabezpieczenia podczas implementacji |
| M2 Plany treningowe | | ✅ model, katalog, API, listy, kreator, edycja i usuwanie gotowe; pełny przepływ UI sprawdzony w Dockerze |
| M3 Progres | | ☐ do wykonania |
| M4 Moduł trenera | | ☐ do wykonania |
| M5 Udostępnianie i ocena planów | | ☐ do wykonania |
| M6 Moduły dodatkowe | | ☐ do wykonania |
| Frontend (UI/UX, mobile) | | 🟡 konta i cały zakres UI planów S2 gotowe; kolejne moduły do wykonania; widoki sprawdzone w desktopie i emulowanym układzie mobilnym, rzeczywisty telefon pozostaje do sprawdzenia |

---

## 14. Zgodność z kryteriami SMART

| Kryterium | Jak jest spełnione |
|---|---|
| **S**pecific | Webowa aplikacja trenerska z jasnym zakresem funkcji i stackiem (rozdz. 1, 2, 4) |
| **M**easurable | Lista wskaźników: ≥ 6 modułów, przetestowane przepływy, działanie mobile, Docker (rozdz. 8) |
| **A**chievable | Etapowa realizacja, znane technologie, priorytety P1–P3 (rozdz. 4, 10, 11) |
| **R**elevant | Realna potrzeba użytkowników + rozwój kompetencji full-stack (rozdz. 1) |
| **T**ime-bound | Termin: **19.12.2026** (rozdz. 9) |
