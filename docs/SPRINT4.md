# Moduły dodatkowe — Sprint 4

Sprint 4 (30.11–6.12) dodaje moduły P2: kalkulatory, kalendarz z przypomnieniami, statystyki, udostępnianie planów i dashboard. Status zadań i raport: [SPRINTY.md](SPRINTY.md#sprint-4-3011612--moduły-dodatkowe).

## Korzystanie z aplikacji

- **Strona główna po zalogowaniu** (`/`): dashboard z listą „Dziś do zrobienia”, zaległymi treningami z ostatnich 14 dni, najbliższym treningiem, podsumowaniem bieżącego tygodnia i trzema ostatnimi treningami.
- **Kalendarz** (`/kalendarz?miesiac=2026-12&dzien=2026-12-03`): widok miesiąca od poniedziałku, planowanie dnia z własnego planu, notatka, edycja, oznaczanie wykonania i usuwanie. „Rozpocznij” otwiera formularz treningu z wybranym planem i dniem. Po zapisaniu wyników wpis w kalendarzu zostaje automatycznie oznaczony jako wykonany. Przycisk „Zaplanuj” w szczegółach planu otwiera kalendarz z wybranym planem.
- **Statystyki** (`/statystyki?tygodnie=12`): treningi, serie, powtórzenia i objętość w zakresie 4, 12, 26 lub 52 tygodni, wykres objętości tygodniowej z tabelą danych oraz rekordy w ćwiczeniach z całej historii.
- **Kalkulatory** (`/kalkulatory`): szacowane 1RM z tabelą ciężarów procentowych oraz zapotrzebowanie kaloryczne z podziałem na makroskładniki.
- **Udostępnianie**: w szczegółach planu przycisk „Udostępnij link” tworzy link `/udostepnione/{token}`. Zalogowany odbiorca widzi plan i może zapisać jego niezależną kopię. Wyłączenie udostępniania unieważnia link; ponowne udostępnienie tworzy nowy. Na liście planów udostępnione plany mają oznaczenie.

Przypomnienia działają w wersji prostej (S4-04): lista na dashboardzie. Wysyłka e-maili przez `@Scheduled` jest opcjonalna i nie została wdrożona; backend na Vercelu nie działa stale, więc zadanie cykliczne nie byłoby niezawodne.

## API

Wszystkie endpointy wymagają aktywnej sesji JWT (rola `USER`, `TRAINER` lub `ADMIN`). Właściciel danych wynika z sesji. Cudze i nieistniejące zasoby zwracają `404`.

### Kalkulatory

| Metoda | Endpoint | Działanie |
|---|---|---|
| GET | `/api/calc/1rm?weight=100&reps=5` | Epley (wynik główny), Brzycki, Lombardi i tabela 100–50% |
| GET | `/api/calc/calories?sex=&age=&heightCm=&weightKg=&activity=&goal=` | PPM, CPM, cel kaloryczny i makroskładniki |

- 1RM: ciężar `>0` i `≤2000` kg, maksymalnie dwa miejsca po przecinku; powtórzenia 1–20. Dla jednego powtórzenia wszystkie wzory zwracają podany ciężar. Szacunki są zaokrąglane do 0,1 kg, ciężary w tabeli do 0,5 kg; liczba powtórzeń w tabeli wynika z odwróconego wzoru Epleya.
- Kalorie: `sex` = `MALE`/`FEMALE`, wiek 15–100, wzrost 100–250 cm, masa 30–300 kg. `activity`: `SEDENTARY` (1,2), `LIGHT` (1,375), `MODERATE` (1,55), `ACTIVE` (1,725), `VERY_ACTIVE` (1,9). `goal`: `LOSE` (−15% CPM), `MAINTAIN`, `GAIN` (+10% CPM).
- PPM wg Mifflina-St Jeora: `10·masa + 6,25·wzrost − 5·wiek + 5` (mężczyźni) lub `− 161` (kobiety). Białko: 2,0 g/kg przy redukcji, 1,6 g/kg przy utrzymaniu, 1,8 g/kg przy budowie masy; tłuszcze 25% kalorii; reszta to węglowodany.

Odpowiedź 1RM: `weight`, `reps`, `epley`, `brzycki`, `lombardi`, `percentages[] {percent, weight, reps}`. Odpowiedź kalorii: `bmr`, `tdee`, `targetCalories`, `calorieChange`, `activityFactor`, `macros {proteinG, fatG, carbsG, proteinKcal, fatKcal, carbsKcal}`.

### Kalendarz i przypomnienia

| Metoda | Endpoint | Działanie |
|---|---|---|
| GET | `/api/schedule?from=2026-12-01&to=2026-12-31` | Wpisy w zakresie (włącznie), maks. 92 dni |
| GET | `/api/schedule/agenda?date=2026-12-10` | `today`, `overdue` (14 dni wstecz, niewykonane), `upcoming` (7 dni, maks. 10) |
| POST | `/api/schedule` | `{date, planId, planDayId, note?}` → `201` + `Location` |
| PUT | `/api/schedule/{id}` | Zmiana daty, planu, dnia lub notatki; stan wykonania zostaje |
| POST / DELETE | `/api/schedule/{id}/complete` | Oznaczenie wykonania (idempotentne) / cofnięcie |
| DELETE | `/api/schedule/{id}` | Usunięcie wpisu → `204` |

Odpowiedź: `id`, `date`, `planId`, `planDayId`, `planName`, `dayName`, `note`, `completed`, `completedAt`. Daty są dniami kalendarzowymi bez strefy czasowej; frontend przekazuje lokalną datę użytkownika. Dozwolony zakres 1.01.2000–31.12.2100, maks. 10 wpisów na dzień (`409`), notatka do 500 znaków.

Wpis zapamiętuje plan, **pozycję dnia** w planie oraz nazwy z chwili zapisu. Edycja planu zastępuje rekordy dni, dlatego odpowiedź wskazuje aktualny dzień o tej samej pozycji i jego bieżącą nazwę. Po usunięciu planu `planId` i `planDayId` mają `null`, a nazwy pozostają.

### Statystyki

| Metoda | Endpoint | Działanie |
|---|---|---|
| GET | `/api/stats/summary?weeks=12&zone=Europe/Warsaw` | Podsumowanie, tygodnie i rekordy; `weeks` 1–52 |
| GET | `/api/stats/sessions?limit=5` | Ostatnie treningi, od najnowszych; `limit` 1–20 |

- **Trening (sesja)** to jeden zapis z `POST /api/results`; wszystkie jego serie mają ten sam czas.
- **Objętość** to suma `powtórzenia × ciężar`. Ćwiczenia z ciężarem 0 kg zwiększają liczbę serii i powtórzeń, ale nie objętość.
- **Tygodnie** trwają od poniedziałku do niedzieli w strefie `zone` (domyślnie `Europe/Warsaw`, frontend wysyła strefę przeglądarki). Ostatni element to bieżący tydzień; tygodnie bez treningów mają zera.
- Zapytanie grupuje serie po sesji w bazie (agregaty JPQL), a przypisanie sesji do tygodnia odbywa się w serwisie w strefie użytkownika. Ten sam kod działa w H2, Supabase PostgreSQL 17 i lokalnej TimescaleDB; nie korzystamy z `time_bucket`, bo `date_trunc` po stronie bazy liczyłby granice tygodnia w UTC.
- **Rekordy** obejmują całą historię: największy ciężar (przy remisie więcej powtórzeń) z datą pierwszego osiągnięcia, szacowane 1RM wg Epleya, największą liczbę powtórzeń w serii, liczbę serii i datę ostatniego wykonania. Dla ćwiczeń bez obciążenia ciężar i 1RM mają `null`.

Odpowiedź `summary`: `from`, `to`, `zone`, `summary {sessions, sets, reps, volume, activeWeeks, averageSessionsPerWeek}`, `weeks[] {weekStart, sessions, sets, reps, volume}`, `records[]`. Odpowiedź `sessions[]`: `time`, `planName`, `dayName`, `exercises`, `sets`, `reps`, `volume`.

### Udostępnianie planów

| Metoda | Endpoint | Działanie |
|---|---|---|
| POST | `/api/plans/{id}/share` | Włącza udostępnianie właściciela; ponowne wywołanie zwraca ten sam link |
| DELETE | `/api/plans/{id}/share` | Wyłącza udostępnianie i unieważnia link → `204` |
| GET | `/api/shared-plans/{token}` | Podgląd: nazwa, opis, autor, dni i ćwiczenia, `own` |
| POST | `/api/shared-plans/{token}/copy` | Niezależna kopia na koncie wywołującego → `201` + `Location` |

- Token ma 43 znaki base64url (32 losowe bajty z `SecureRandom`) i jest unikalny. Link działa tylko, gdy `is_shared = true` i token się zgadza; ustawiony sam znacznik `is_shared` niczego nie udostępnia.
- Podgląd nie zawiera identyfikatora planu, tokenu ani identyfikatora autora. Plan zablokowanego lub niepotwierdzonego autora jest niedostępny.
- Podgląd pokazuje bieżącą wersję planu. Kopia jest migawką: późniejsza edycja, wyłączenie udostępniania, usunięcie planu źródłowego lub konta autora jej nie zmienia. Kopia nie jest udostępniona.
- `GET /api/plans` i `GET /api/plans/{id}` zwracają dodatkowo `shared`; szczegóły planu także `shareToken` (tylko właścicielowi). Edycja planu (`PUT`) zachowuje stan udostępniania i token.

## Baza i migracje

**V9** (`V9__plan_sharing_and_schedule.sql`):

- `workout_plans.share_token VARCHAR(43)` z ograniczeniem `UNIQUE`; istniejące plany pozostają prywatne.
- `scheduled_workouts`: `user_id` (kaskada przy usunięciu konta), `plan_id` (`SET NULL` przy usunięciu planu), `day_index`, migawki nazw planu i dnia, `scheduled_date`, `note`, `completed_at`, `created_at`, indeks `(user_id, scheduled_date, id)`.

Następna wolna wersja migracji to **V10**. Flyway wykonuje V9 przy starcie backendu; w Supabase nie uruchamiamy jej ręcznie.

## Testy

| Plik | Zakres |
|---|---|
| `CalculatorApiIntegrationTests` | Wzory i zaokrąglenia, wszystkie role, walidacja, brak parametrów, sesja |
| `ScheduleApiIntegrationTests` | CRUD, agenda, wykonanie, limity, dopasowanie po pozycji dnia, usunięcie planu i konta, izolacja kont |
| `StatsApiIntegrationTests` | Grupowanie tygodni w strefie czasowej, podsumowanie, rekordy z całej historii, ostatnie sesje, walidacja |
| `PlanSharingApiIntegrationTests` | Udostępnianie, podgląd, kopia, niezależność kopii, unieważnienie linku, nieaktywny autor, sesja |
| `Sprint4MigrationIntegrationTests` | Aktualizacja V8 → V9 na PostgreSQL z zachowaniem danych i kaskadami (pomijany na H2) |
| `frontend/tests/sprint4.test.mjs` | Walidacja formularzy kalkulatorów, siatka miesiąca, daty lokalne, linki do treningu, pomocnicze funkcje statystyk i linków |

```powershell
cd backend; .\mvnw.cmd test
cd ..\frontend; npm test; npm run lint; npm run build
pwsh -NoProfile -File tests/test-sprint3-databases.ps1 -Database postgres
```

`tests/smoke-deployment.ps1` oczekuje teraz dziewięciu migracji (V1–V9).
