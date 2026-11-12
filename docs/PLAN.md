# Aplikacja trenerska — plan projektu

> Webowa aplikacja do planowania treningów i śledzenia progresu.
> Projekt programistyczny, Uniwersytet Ekonomiczny w Katowicach, rok akademicki 2026/2027.

| | |
|---|---|
| **Zespół** | Hubert Muszalski, Marcin Szeja, Mateusz Dudek, Mateusz Trzaska |
| **Termin oddania** | **19 grudnia 2026** |
| **Platforma** | Aplikacja webowa, responsywna, głównie pod telefony (mobile-first) |

> **Legenda:** sekcje oznaczone 📄 pochodzą wprost z formularza SMART. Sekcje oznaczone 💡 to propozycje techniczne (do uzgodnienia w zespole).

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
| Frontend | Framework do wyboru (React / Vue / Angular / Svelte) | ⏳ **do decyzji** |
| Backend | Java + **Spring Boot** | ✅ ustalone |
| Baza danych | **PostgreSQL** + **TimescaleDB** | ✅ ustalone |
| Wdrożenie | **Docker** (docker compose) | ✅ ustalone |

### 💡 Rekomendacja wyboru frontendu

Zespół ma doświadczenie w Javie i PHP, ale **nie we frameworkach frontendowych** — to największe ryzyko projektu. Wybierz framework **w pierwszym tygodniu** i nie zmieniaj go. Kryteria: ilość materiałów do nauki, gotowe biblioteki UI (mobile-first), łatwa integracja z REST API. Praktyczny wybór: **React + Vite** lub **Vue 3 + Vite**.

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

- [ ] Repozytorium Git, konwencja branchy i PR-ów
- [ ] `docker-compose.yml`: `db` (TimescaleDB), `backend`, `frontend`
- [ ] Migracje bazy (Flyway/Liquibase), `.env.example`
- [ ] Aplikacja uruchamia się jedną komendą: `docker compose up`
- [ ] Layout responsywny (mobile-first)

### M1. Konta użytkowników i autoryzacja (P1)

- [ ] Rejestracja (z potwierdzeniem konta, np. e-mail)
- [ ] Logowanie / wylogowanie (JWT + Spring Security)
- [ ] Role: `USER`, `TRAINER` (`ADMIN` opcjonalnie)
- [ ] Przypomnienie hasła (reset przez e-mail/token)
- [ ] Usunięcie konta
- [ ] Zabezpieczenie endpointów wg ról

### M2. Moduł użytkownika — plany treningowe (P1)

- [ ] **Baza ćwiczeń** (nazwa, grupa mięśniowa, opis, typ: siłowe / domowe)
- [ ] Tworzenie własnych planów treningowych (siłowe i domowe)
- [ ] Plan → dni treningowe → ćwiczenia (serie, powtórzenia, ciężar)
- [ ] Edycja i usuwanie planów

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
| GET | `/api/auth/confirm?token=` | Potwierdzenie konta | — |
| POST | `/api/auth/login` | Logowanie → JWT | — |
| POST | `/api/auth/forgot-password` | Prośba o reset hasła | — |
| POST | `/api/auth/reset-password` | Ustawienie nowego hasła | — |
| DELETE | `/api/users/me` | Usunięcie konta | USER |
| GET | `/api/exercises` | Baza ćwiczeń | USER |
| GET/POST | `/api/plans` | Lista / tworzenie planów | USER |
| GET/PUT/DELETE | `/api/plans/{id}` | Szczegóły / edycja / usunięcie | USER |
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
  - [ ] rejestracja i logowanie
  - [ ] utworzenie planu treningowego
  - [ ] zapis wyniku treningu
  - [ ] utworzenie grupy przez trenera i przypisanie planu podopiecznemu
- [ ] Aplikacja **poprawnie wyświetla się i działa na komputerach i urządzeniach mobilnych**
- [ ] Aplikacja **uruchamia się w kontenerach Docker** (backend, baza danych, frontend)
- [ ] **Cotygodniowe podsumowanie postępów** w zespole, z odniesieniem do listy zadań

---

## 9. Harmonogram 💡

Termin końcowy: **sobota 19.12.2026**. Zostało ok. **6 tygodni**.

| Tydzień | Daty | Cel | Zakres |
|---|---|---|---|
| **T1** | 9–15.11 | Fundament | Repo, Docker Compose, migracje, **wybór frameworka frontendu**, szkielet backendu i frontendu |
| **T2** | 16–22.11 | Konta | Rejestracja, logowanie, JWT, role, reset hasła, usunięcie konta (M1) |
| **T3** | 23–29.11 | Plany | Baza ćwiczeń, CRUD planów, widoki mobile (M2) |
| **T4** | 30.11–6.12 | Progres + trener | Wyniki, waga, obwody, wykresy (M3); grupy trenera i przypisywanie planów (M4) |
| **T5** | 7–13.12 | Dodatki | Udostępnianie planów, kalkulatory, kalendarz, statystyki (M5, M6) |
| **T6** | 14–19.12 | Stabilizacja | Testy, poprawki, dokumentacja, finalny `docker compose up`, oddanie |

> **Zasada:** w T5 zamrażamy nowe funkcje, jeśli P1 nie jest w pełni gotowe. W T6 nie dodajemy funkcji — tylko poprawki i dokumentacja.

---

## 10. Ograniczenia i ryzyka 📄 + 💡

| Ryzyko | Wpływ | Działanie |
|---|---|---|
| Stały termin oddania (19.12) | Wysoki | Priorytety P1/P2/P3; moduły dodatkowe mogą zostać pominięte (np. moduł Fitatu) |
| Brak doświadczenia z frameworkiem frontendowym | Wysoki | Szybki wybór w T1, nauka na małych zadaniach, gotowa biblioteka UI |
| Szeroki zakres funkcjonalny | Średni | Realizacja etapami: **1)** web + konta + plany trenera/użytkownika, **2)** kalendarz, kalkulatory, statystyki, udostępnianie, **3)** social, mini-Fitatu, wydarzenia |
| Nowa technologia: TimescaleDB | Średni | Standardowy PostgreSQL + `create_hypertable` tylko dla tabel pomiarów |
| Konfiguracja Dockera | Średni | Docker Compose uruchomiony już w T1, nie na koniec |

---

## 11. Zasoby i kompetencje 📄

- **Znane technologie:** Java, Spring Boot, PostgreSQL, Docker
- **Doświadczenie zespołu:** Java i PHP, konteneryzacja, bazy danych
- **Do nauczenia:** wybrany framework frontendowy, projektowanie stron responsywnych, TimescaleDB

---

## 12. Definition of Done 💡

Zadanie uznajemy za skończone, gdy:

- [ ] kod jest w `main` przez Pull Request z review co najmniej jednej osoby,
- [ ] funkcja działa w Dockerze (`docker compose up`),
- [ ] działa poprawnie na telefonie i na komputerze,
- [ ] endpointy mają podstawowe testy (min. happy path),
- [ ] zmiany w bazie są w migracjach,
- [ ] README / dokumentacja zaktualizowane, jeśli coś się zmieniło.

---

## 13. Pierwsze kroki (Sprint 0) 💡

1. [ ] Założyć repozytorium i dodać ten plik jako `PLAN.md`
2. [ ] Wybrać framework frontendowy (decyzja zespołu)
3. [ ] Zainicjować projekt Spring Boot (Spring Web, Security, Data JPA, PostgreSQL Driver, Validation, Flyway)
4. [ ] Zainicjować projekt frontendowy
5. [ ] Napisać `docker-compose.yml` z bazą TimescaleDB i sprawdzić połączenie
6. [ ] Podzielić moduły M1–M6 między członków zespołu (kolumna „Osoba" poniżej)
7. [ ] Ustalić stały termin cotygodniowego spotkania

### Podział pracy (do uzupełnienia)

| Moduł | Osoba | Status |
|---|---|---|
| M0 Infrastruktura / Docker | | ☐ |
| M1 Konta i autoryzacja | | ☐ |
| M2 Plany treningowe | | ☐ |
| M3 Progres | | ☐ |
| M4 Moduł trenera | | ☐ |
| M5 Udostępnianie i ocena planów | | ☐ |
| M6 Moduły dodatkowe | | ☐ |
| Frontend (UI/UX, mobile) | | ☐ |

---

## 14. Zgodność z kryteriami SMART

| Kryterium | Jak jest spełnione |
|---|---|
| **S**pecific | Webowa aplikacja trenerska z jasnym zakresem funkcji i stackiem (rozdz. 1, 2, 4) |
| **M**easurable | Lista wskaźników: ≥ 6 modułów, przetestowane przepływy, działanie mobile, Docker (rozdz. 8) |
| **A**chievable | Etapowa realizacja, znane technologie, priorytety P1–P3 (rozdz. 4, 10, 11) |
| **R**elevant | Realna potrzeba użytkowników + rozwój kompetencji full-stack (rozdz. 1) |
| **T**ime-bound | Termin: **19.12.2026** (rozdz. 9) |
