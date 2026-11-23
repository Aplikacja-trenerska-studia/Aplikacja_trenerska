# Aplikacja trenerska — plan projektu

> Webowa aplikacja do planowania treningów i śledzenia progresu.
> Projekt programistyczny, Uniwersytet Ekonomiczny w Katowicach, rok akademicki 2026/2027.

| | |
|---|---|
| **Zespół** | Hubert Muszalski, Marcin Szeja, Mateusz Dudek, Mateusz Trzaska |
| **Termin oddania** | **19 grudnia 2026** |
| **Platforma** | Aplikacja webowa, responsywna, głównie pod telefony (mobile-first) |

> **Legenda:** sekcje oznaczone 📄 pochodzą wprost z formularza SMART. Sekcje oznaczone 💡 to propozycje techniczne (do uzgodnienia w zespole).

> **Stan checklist: 19.11.2026.** Konta i plany ze Sprintu 2 oraz funkcje progresu i trenera ze Sprintu 3 są technicznie gotowe i sprawdzone lokalnie. `feat/sprint-3` wypchnięto i scalono do `main` przez commit `f40d008`; wdrożenia obu projektów Vercela oraz wykonanie migracji Supabase do V7 potwierdzono 19.11. Publikację i pełny odbiór funkcji rozliczamy osobno w [checkliście S3](SPRINTY.md#publikacja-i-odbiór-s3). Harmonogram **S3: 23–29.11** pozostaje bez zmian. Pierwsze wdrożenie Vercel/Supabase (**S3-11**) wykonano **16.11.2026**. `[x]` potwierdza opisany zakres techniczny; zespołowe review, test na rzeczywistym telefonie i pełny pozytywny przepływ online wymagają osobnego potwierdzenia. Szczegóły: [SPRINTY.md](SPRINTY.md), [SPRINT3.md](SPRINT3.md).

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
| Baza danych | **PostgreSQL**; lokalnie TimescaleDB, online Supabase PostgreSQL 17 | ✅ oba warianty sprawdzone lokalnie; wykonanie V5–V7 w produkcji potwierdzone logami Flyway 19.11 |
| Wdrożenie | **Docker Compose** lokalnie; **Vercel + Supabase** online | ✅ S3 wdrożony 19.11 z `main` (`f40d008`), oba projekty READY; transport poczty `log`, bez wysyłki wiadomości |

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

Lokalnie V7 tworzy hypertabele wyników i pomiarów, jeśli serwer udostępnia rozszerzenie TimescaleDB. W Supabase PostgreSQL 17 te same tabele pozostają zwykłymi tabelami z indeksami czasu. API i interfejs użytkownika są wspólne dla obu wariantów; aplikacja nie wymaga funkcji SQL dostępnych wyłącznie w TimescaleDB.

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
- [x] V5: wyniki i pomiary, V6: grupy i przypisania, V7: warunkowe włączenie hypertabel; aktualizacja V4 → V7 zachowuje istniejące konto i plan, ponowny start nie wykonuje migracji drugi raz
- [x] Aplikacja uruchamia się jedną komendą: `docker compose up --build`
- [x] Layout responsywny (mobile-first) obecnych ekranów kont, sprawdzony na komputerze i w emulowanym widoku mobilnym
- [x] Frontend i backend na Vercelu, baza Supabase w schemacie `training_app`, HTTPS i CORS (S3-11: pierwsze wdrożenie 16.11, publikacja funkcji S3 19.11)
- [x] Migracje V1–V4 i 48 ćwiczeń w Supabase od 16.11; przeniesienie jedynego lokalnego konta z kopiami zapasowymi poza GitHubem
- [x] Publikacja `feat/sprint-3` z commitem `10769cb` i scalenie `--no-ff` do `main` przez `f40d008`, wypchnięte na GitHuba 19.11
- [x] Wdrożenia produkcyjne obu projektów Vercela READY dla `f40d008`; migracje V5–V7 wykonane w Supabase PostgreSQL 17.12, potwierdzone logami Flyway i prawidłowym startem backendu
- [x] Automatyczne wdrożenia obu projektów z `main`; `feat/deployment` scalony i usunięty
- [x] Backend obsługuje opcjonalny transport `APP_MAIL_MODE=log`, który zapisuje linki kont w prywatnych logach bez wysyłania wiadomości
- [x] Sprawdzenia online: strony SPA, API `UP`, odczyt bazy, ochrona tras i CORS

Adres aplikacji: [aplikacja-trenerska.vercel.app](https://aplikacja-trenerska.vercel.app). Konfiguracja i zakres potwierdzonych testów: [DEPLOYMENT.md](DEPLOYMENT.md).

### M1. Konta użytkowników i autoryzacja (P1)

- [x] Rejestracja z potwierdzeniem konta przez e-mail
- [x] Logowanie / wylogowanie (JWT + Spring Security)
- [x] Role `USER`, `TRAINER`, `ADMIN` w modelu i obsłudze sesji; rejestracja tworzy konto `USER`
- [x] Sposób nadawania roli `TRAINER` przez operatora (`tools/set-user-role.sql`), unieważnienie sesji po zmianie roli i testy rozróżniające uprawnienia ról (S2-04, 15.11.2026)
- [x] Panel administratora `/admin`: wyszukiwanie i stronicowanie kont, zmiana ról `USER`/`TRAINER`/`ADMIN`, potwierdzenia zmian i zbiorcze podsumowanie (S3-12, rozszerzenie uzgodnione 19.11)
- [x] Osobna blokada administracyjna `blocked`, unieważnianie JWT po zmianie roli/stanu i ochrona przed obejściem blokady przez aktywację lub reset hasła (V8)
- [x] Jednorazowa inicjalizacja istniejącego konta administratora i ochrona ostatniego aktywnego administratora, również przy równoległym usuwaniu kont
- [x] Przypomnienie hasła (reset przez e-mail/token)
- [x] Usunięcie konta po ponownym podaniu hasła i potwierdzeniu przez e-mail
- [x] Ochrona obecnych endpointów użytkownika przez JWT i `@PreAuthorize`, ochrona trasy `/profil`
- [x] Autoryzacja katalogu i CRUD planów według ról i właściciela z sesji; testy odmowy dostępu do cudzych planów
- [x] Autoryzacja endpointów progresu i trenera według właściciela, roli i zaakceptowanego członkostwa; testy odmowy dostępu i odebrania zgody po opuszczeniu grupy
- [ ] Autoryzacja kolejnych modułów podczas ich wdrażania

Implementacja kont i kontrakt API: [ACCOUNTS.md](ACCOUNTS.md). Reset hasła unieważnia wcześniejsze JWT; jednorazowe tokeny mają przeznaczenie, termin ważności i skrót w bazie. Zaliczone sprawdzenia backendu na H2 i PostgreSQL 16: 111 testów (15 kont, 7 ról, 25 modelu planów, 3 katalogu, 19 API ćwiczeń, 41 API planów i uruchomienie kontekstu), w tym CRUD i izolacja własnych planów, walidacja oraz usuwanie konta z planem przy zachowaniu katalogu. Sprawdzono także aktualizacje V2 → V3 → V4 z istniejącymi danymi. Wcześniej sprawdzono 4 testy pomocniczych funkcji frontendu, build i lint frontendu oraz przepływy kont przez nginx z PostgreSQL i rzeczywistą wysyłką SMTP do Mailpit (`tests/smoke-accounts.mjs`). Potwierdzono również obejście crasha Brave podczas wpisywania nicku (`autocomplete="off"`).

Mailpit przechwytuje wiadomości lokalnie. Produkcja korzysta z `APP_MAIL_MODE=log`: linki trafiają do prywatnych logów API, wiadomości nie są wysyłane. Rzeczywista wysyłka wymaga zewnętrznego SMTP; wybór transportu nie zmienia interfejsu użytkownika. Pełne przepływy kont, planów i S3 sprawdzono lokalnie; ich powtórzenie online na koncie testowym pozostaje na checkliście odbioru S3-11. Weryfikacja z 19.11: po 216 testów backendu bez błędów na PostgreSQL 17 i TimescaleDB/PostgreSQL 16; na H2 215 zaliczonych i 1 celowo pominięty test wymagający PostgreSQL. Frontend: 27 testów, build i lint zaliczone. Szczegóły i powtarzalne polecenia: [SPRINT3.md](SPRINT3.md).

Dodatkowe rozszerzenie **S3-12 — administrator** przygotowano, sprawdzono lokalnie i opublikowano 19.11. Aktualny zestaw po tym rozszerzeniu obejmuje **262 testy backendu** zaliczone na PostgreSQL 17 i TimescaleDB/PostgreSQL 16 (H2: 260 zaliczonych i 2 celowo pominięte) oraz **33 testy frontendu**, build i lint. Aktualizacja V7 → V8 zachowuje konta, wersje sesji i dane treningowe, a pełny test UI sprawdził zmiany ról, blokady i zachowanie sesji na komputerze oraz w widoku mobilnym. Kod scalono do `main` przez `ecacd1d`; oba projekty Vercela mają `READY`, API zwraca `UP`, a logi potwierdzają wykonanie V8 w Supabase i nadanie pierwszych uprawnień `ADMIN`. Zmienną inicjalizacji usunięto z konfiguracji Production. Pełny odbiór panelu po ponownym zalogowaniu właściciela konta, review i rzeczywisty telefon są nadal rozliczane osobno w [checkliście S3-12](SPRINTY.md#rozszerzenie-s3-12--administrator-19112026). Instrukcja panelu i jednorazowej inicjalizacji: [ADMIN.md](ADMIN.md).

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

- [x] Zapis treningu z wybranego planu i dnia: serie, powtórzenia, ciężar i data; cały zapis w jednej transakcji
- [x] Zapis wagi ciała
- [x] Zapis obwodów: klatka, talia, biodra, ramię i udo
- [x] Historia z filtrami dat i ćwiczenia, usuwanie błędnych wpisów oraz wykresy ciężaru, wagi i obwodów
- [x] Hypertabele TimescaleDB lokalnie i zgodny wariant zwykłych tabel na Supabase PostgreSQL 17 (V5/V7)

Zakres M3 zaimplementowano i sprawdzono na `feat/sprint-3` 19.11.2026. Wyniki zachowują nazwę planu, dnia i ćwiczenia z chwili zapisu, także po edycji lub usunięciu planu. Odczyt jest ograniczony do własnego konta; uprawniony trener otrzymuje dostęp dopiero po akceptacji zaproszenia przez podopiecznego. Zapis treningu, pomiarów i odczyt wykresów przeszedł w pełnym stacku Docker przez UI; potwierdzono dwie rzeczywiste hypertabele na TimescaleDB. Publikację i odbiór dokumentuje osobna [checklista S3](SPRINTY.md#publikacja-i-odbiór-s3).

### M4. Moduł trenera (P1/P2)

- [x] Rola trenera nadawana istniejącemu kontu przez operatora (S2-04)
- [x] Tworzenie i usuwanie własnych **grup podopiecznych**
- [x] Zaproszenie po e-mailu, akceptacja w aplikacji, odrzucenie zaproszenia i opuszczenie grupy
- [x] Przypisywanie własnego planu osobie lub zaakceptowanym członkom grupy
- [x] Lista przypisanych planów u podopiecznego i możliwość wykonania treningu z otrzymanego planu
- [x] Podgląd wyników, pomiarów i wykresów zaakceptowanego podopiecznego; odebranie dostępu po opuszczeniu grupy

Nowe funkcje M4 zaimplementowano i sprawdzono na `feat/sprint-3` 19.11.2026. Przypisanie tworzy niezależną kopię planu na koncie podopiecznego. Zmiany planu źródłowego i usunięcie konta trenera nie usuwają cudzej kopii. Usunięcie konta obejmuje własny progres, pomiary, grupy i członkostwa oraz powiązane przypisania; dane pozostałych kont pozostają. Test pełnego przepływu potwierdził zaproszenie, akceptację, przypisanie kopii, wykonanie z niej treningu i odebranie podglądu po opuszczeniu grupy. Publikacja kodu i odbiór produkcyjny są rozliczane niezależnie od tych testów.

### M5. Udostępnianie i ocena planów (P2)

- [x] Udostępnianie planów linkiem: włączanie i wyłączanie przez właściciela, podgląd dla zalogowanych, niezależna kopia na własnym koncie (S4-06, V9)
- [ ] **Algorytm oceny planów** — *do zdefiniowania w zespole* (np. pokrycie grup mięśniowych, objętość tygodniowa, balans push/pull)

### M6. Moduły dodatkowe (P2/P3)

| Moduł | Opis | Priorytet |
|---|---|---|
| **Kalkulator kalorii/makro** | Mini-moduł w stylu Fitatu: średnie zapotrzebowanie kaloryczne (np. Mifflin-St Jeor), podział makroskładników | P2 — 🟡 zaimplementowany (S4-02) |
| **Kalkulator 1RM** | Maksymalny ciężar dla ćwiczeń (np. wzór Epleya) | P2 — 🟡 zaimplementowany (S4-01) |
| **Kalendarz i przypomnienia** | Planowanie treningów w kalendarzu, powiadomienia | P2 — 🟡 kalendarz i lista „dziś do zrobienia” (S4-03, S4-04); bez e-maili |
| **Statystyki** | Podsumowania treningów, objętość, rekordy | P2 — 🟡 zaimplementowane (S4-05) |
| **Social media** | Tablica, wiadomości, DM | P3 |
| **Wydarzenia sportowe** | Lista wydarzeń, np. Silesia Maraton | P3 |

Moduły S4 przygotowano 23.11.2026 wraz z testami i dashboardem po zalogowaniu (S4-07). Testy backendu, build frontendu, review i test na telefonie pozostają do wykonania. Kontrakt API i zasady obliczeń: [SPRINT4.md](SPRINT4.md).

---

## 5. Model danych (szkic) 💡

```sql
-- Użytkownicy i role
users            (id, email, password_hash, display_name, role, enabled, created_at)
verification_tokens (id, user_id, token, type, expires_at)   -- potwierdzenie konta / reset hasła

-- Trener i grupy
training_groups  (id, trainer_id → users, name, created_at)
group_members    (group_id → training_groups, user_id → users, status, invited_at, accepted_at)

-- Ćwiczenia i plany
exercises        (id, name, muscle_group, type, description, created_by)
workout_plans    (id, owner_id → users, name, description, is_shared, share_token UNIQUE, created_at)
plan_days        (id, plan_id → workout_plans, day_index, name)
plan_exercises   (id, plan_day_id → plan_days, exercise_id, sets, reps, weight, order_index)
plan_assignments (id, group_id, user_id, source_plan_id NULL, copied_plan_id NULL,
                  assigned_by, plan_name, assigned_at)

-- Szeregi czasowe; klucz główny obu tabel: (id UUID, time TIMESTAMPTZ)
exercise_results (id, time, user_id, exercise_id, exercise_name, plan_name, day_name, set_no, reps, weight)
body_metrics     (id, time, user_id, weight_kg, chest_cm, waist_cm, hips_cm, arm_cm, thigh_cm)

-- Kalendarz (V9)
scheduled_workouts (id, user_id → users, plan_id → workout_plans NULL, day_index, plan_name, day_name,
                    scheduled_date, note, completed_at, created_at)

-- V7 wywołuje create_hypertable tylko, gdy serwer udostępnia TimescaleDB.
-- Supabase PostgreSQL 17 zachowuje zwykłe tabele z indeksami użytkownika i czasu.
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
| POST/DELETE | `/api/plans/{id}/share` | Włączenie / wyłączenie udostępniania własnego planu — S4 | USER / TRAINER / ADMIN |
| GET | `/api/shared-plans/{token}` | Podgląd udostępnionego planu — S4 | USER / TRAINER / ADMIN |
| POST | `/api/shared-plans/{token}/copy` | Kopia udostępnionego planu na własnym koncie — S4 | USER / TRAINER / ADMIN |
| POST | `/api/results` | Atomowy zapis serii wybranego dnia własnego planu — S3 | USER / TRAINER / ADMIN |
| GET | `/api/results?exerciseId=&from=&to=` | Własna historia wyników, najnowsze maks. 2000 wpisów — S3 | USER / TRAINER / ADMIN |
| DELETE | `/api/results/{id}?time=` | Usunięcie własnego wyniku — S3 | USER / TRAINER / ADMIN |
| POST/GET | `/api/body-metrics` | Zapis / historia wagi i obwodów, najnowsze maks. 500 wpisów — S3 | USER / TRAINER / ADMIN |
| DELETE | `/api/body-metrics/{id}?time=` | Usunięcie własnego pomiaru — S3 | USER / TRAINER / ADMIN |
| GET/POST | `/api/groups` | Lista / tworzenie własnych grup — S3 | TRAINER |
| GET/DELETE | `/api/groups/{id}` | Szczegóły / usunięcie własnej grupy — S3 | TRAINER |
| POST | `/api/groups/{id}/members` | Zaproszenie podopiecznego po e-mailu — S3 | TRAINER |
| DELETE | `/api/groups/{id}/members/{userId}` | Usunięcie członkostwa — S3 | TRAINER |
| POST | `/api/groups/{id}/assign-plan` | Kopia własnego planu dla zaakceptowanej osoby lub grupy — S3 | TRAINER |
| GET | `/api/groups/{id}/members/{userId}/results` | Progres zaakceptowanego podopiecznego — S3 | TRAINER |
| GET | `/api/groups/{id}/members/{userId}/body-metrics` | Pomiary zaakceptowanego podopiecznego — S3 | TRAINER |
| GET | `/api/my-invitations` | Własne zaproszenia i członkostwa — S3 | USER / TRAINER / ADMIN |
| POST | `/api/my-invitations/{groupId}/accept` | Akceptacja zaproszenia — S3 | USER / TRAINER / ADMIN |
| DELETE | `/api/my-invitations/{groupId}` | Odrzucenie zaproszenia / opuszczenie grupy — S3 | USER / TRAINER / ADMIN |
| GET | `/api/my-assignments` | Własne przypisania planów — S3 | USER / TRAINER / ADMIN |
| GET | `/api/calc/1rm?weight=&reps=` | Kalkulator 1RM — S4 | USER / TRAINER / ADMIN |
| GET | `/api/calc/calories?sex=&age=&heightCm=&weightKg=&activity=&goal=` | Zapotrzebowanie kaloryczne i makro — S4 | USER / TRAINER / ADMIN |
| GET/POST | `/api/schedule` | Kalendarz w zakresie dat / zaplanowanie treningu — S4 | USER / TRAINER / ADMIN |
| PUT/DELETE | `/api/schedule/{id}` | Edycja / usunięcie wpisu — S4 | USER / TRAINER / ADMIN |
| POST/DELETE | `/api/schedule/{id}/complete` | Oznaczenie / cofnięcie wykonania — S4 | USER / TRAINER / ADMIN |
| GET | `/api/schedule/agenda?date=` | Dziś, zaległe i najbliższe treningi — S4 | USER / TRAINER / ADMIN |
| GET | `/api/stats/summary?weeks=&zone=` | Objętość tygodniowa, podsumowanie i rekordy — S4 | USER / TRAINER / ADMIN |
| GET | `/api/stats/sessions?limit=` | Ostatnie treningi — S4 | USER / TRAINER / ADMIN |

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
- [x] **Działające, przetestowane przepływy kluczowe lokalnie:**
  - [x] rejestracja, potwierdzenie e-maila i logowanie; dodatkowo reset hasła i usuwanie konta
  - [x] utworzenie planu treningowego przez kreator, edycja i usunięcie z potwierdzeniem
  - [x] zapis wyniku treningu, pomiarów i odczyt historii oraz wykresów
  - [x] utworzenie grupy przez trenera, akceptacja zaproszenia, przypisanie kopii planu, wykonanie treningu z kopii i odebranie dostępu po opuszczeniu grupy
- [ ] Aplikacja **poprawnie wyświetla się i działa na komputerach i urządzeniach mobilnych** — nowe ekrany sprawdzono przy 1280 × 900 i 375 × 812 bez poziomego przewijania i błędów JavaScript; test na prawdziwym telefonie pozostaje do wykonania
- [x] Obecna aplikacja **uruchamia się w kontenerach Docker** (backend, baza danych, frontend, Mailpit); migracje V1–V7 oraz pełny przepływ rejestracja → plan → wynik → współpraca z trenerem sprawdzone lokalnie
- [x] Aplikacja z funkcjami S3 jest dostępna online na Vercelu z bazą Supabase i wdrożeniami produkcyjnymi z `main`; 19.11 potwierdzono READY dla obu projektów, migracje do V7, dostępność, odczyt bazy, CORS i ochronę tras. Pełny pozytywny przepływ online jest osobnym punktem odbioru
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
| **S5** | 7–13.12 | Stabilizacja | Algorytm oceny planów, testy mobile, poprawki, dokumentacja i dane przykładowe |
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
| Dwa warianty bazy pomiarów | Średni | Hypertabele TimescaleDB w Dockerze oraz zwykłe tabele na Supabase PostgreSQL 17. V7 rozpoznaje rozszerzenie; oba warianty i aktualizację V4 → V7 sprawdzono lokalnie, wykonanie migracji produkcyjnych potwierdzono logami Flyway 19.11. Kolejne zmiany wymagają sprawdzenia obu wariantów ([szczegóły](DEPLOYMENT.md#preview-i-kolejne-migracje)) |
| Dostęp trenera do danych podopiecznych | Wysoki | Wymagana akceptacja zaproszenia; opuszczenie grupy odbiera podgląd. Testy izolacji kont, grup i usuwania relacji są częścią S3 |
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
| M1 Konta i autoryzacja | | ✅ przepływy kont, operatorowe nadawanie roli trenera i nowe reguły dostępu S3 sprawdzone lokalnie |
| M2 Plany treningowe | | ✅ model, katalog, API, listy, kreator, edycja i usuwanie gotowe; pełny przepływ UI sprawdzony w Dockerze |
| M3 Progres | | ✅ technicznie gotowe i sprawdzone lokalnie 19.11; publikacja i odbiór w osobnej checkliście S3 |
| M4 Moduł trenera | | ✅ technicznie gotowe i sprawdzone lokalnie: zaproszenia z akceptacją i niezależne kopie planów; publikacja i odbiór w osobnej checkliście S3 |
| M5 Udostępnianie i ocena planów | | 🟡 udostępnianie i kopiowanie zaimplementowane (S4-06); algorytm oceny w S5 |
| M6 Moduły dodatkowe | | 🟡 kalkulatory, kalendarz z przypomnieniami i statystyki zaimplementowane (S4); testy do uruchomienia, review i telefon |
| Frontend (UI/UX, mobile) | | 🟡 konta, plany oraz nowe ekrany treningu, progresu i trenera sprawdzone na desktopie i w mobilnym widoku przeglądarki; rzeczywisty telefon pozostaje do sprawdzenia |

---

## 14. Zgodność z kryteriami SMART

| Kryterium | Jak jest spełnione |
|---|---|
| **S**pecific | Webowa aplikacja trenerska z jasnym zakresem funkcji i stackiem (rozdz. 1, 2, 4) |
| **M**easurable | Lista wskaźników: ≥ 6 modułów, przetestowane przepływy, działanie mobile, Docker (rozdz. 8) |
| **A**chievable | Etapowa realizacja, znane technologie, priorytety P1–P3 (rozdz. 4, 10, 11) |
| **R**elevant | Realna potrzeba użytkowników + rozwój kompetencji full-stack (rozdz. 1) |
| **T**ime-bound | Termin: **19.12.2026** (rozdz. 9) |
