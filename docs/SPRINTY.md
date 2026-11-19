# Sprinty — aplikacja trenerska

> Uzupełnienie do `PLAN.md`. Sprint = 1 tydzień (pon–nd). Termin oddania projektu: **19.12.2026**.

> **Stan na 19.11.2026:** konta i plany ze Sprintu 2 oraz progres i trener (S3-01–S3-10) są technicznie gotowe i sprawdzone lokalnie. Implementację S3 przygotowano 19.11 na `feat/sprint-3`; publikację, wdrożenie i odbiór rozliczamy osobno w [checkliście poniżej](#publikacja-i-odbiór-s3). Terminu Sprintu 3 (**23–29.11**) nie zmieniamy. Pierwsze wdrożenie Vercel/Supabase (S3-11) wykonano wcześniej, 16.11. Szczegóły: [SPRINT3.md](SPRINT3.md), [DEPLOYMENT.md](DEPLOYMENT.md).

**Legenda:** ✅ / `[x]` — technicznie wykonane i sprawdzone w opisanym zakresie; 🟡 — częściowo; ☐ / `[ ]` — pozostaje do wykonania albo potwierdzenia. Status funkcji nie zastępuje zespołowego review ani całej Definition of Done; scalenie i wdrożenie są raportowane osobno.

## Założenia

- Zespół: 4 osoby, każda ma ok. **6–8 h tygodniowo** (to są studia, są inne przedmioty) → ok. 25–30 h pracy zespołu na sprint.
- **4 sprinty to realny plan.** Po sprincie 4 mają być gotowe wszystkie moduły P1 i P2 (≥ 6 modułów). Sprint 5 i bufor to stabilizacja, a nie nowe funkcje.
- Zadania są małe (maks. ok. 1 dzień pracy jednej osoby), żeby łatwo było napisać w raporcie, co jest zrobione.
- Każdy task ma ID (np. `S2-04`), które można wpisywać do raportu i do commitów.



### Tory pracy (propozycja podziału na 4 osoby)


| Tor            | Zakres                                                       |
| -------------- | ------------------------------------------------------------ |
| **BE-1**       | Backend: auth, bezpieczeństwo, konta, role, trener           |
| **BE-2**       | Backend: ćwiczenia, plany, progres, statystyki, kalkulatory  |
| **FE-1**       | Frontend: auth, layout, plany, kreator planu                 |
| **FE-2 / OPS** | Frontend: progres, wykresy, panel trenera + Docker, baza, CI |


Kolumna `Kto` w tabelach jest do uzupełnienia na początku sprintu.

---



## Harmonogram


| Sprint | Daty       | Cel w jednym zdaniu                                             | Moduły         |
| ------ | ---------- | --------------------------------------------------------------- | -------------- |
| **S1** | 9–15.11    | Szkielet działa w Dockerze, rejestracja i logowanie end-to-end  | M0, M1 (część) |
| **S2** | 16–22.11   | Pełne konta, baza ćwiczeń, plany treningowe                     | M1, M2         |
| **S3** | 23–29.11   | Progres, moduł trenera i wdrożenie Vercel/Supabase              | M0, M3, M4     |
| **S4** | 30.11–6.12 | Kalkulatory, kalendarz, statystyki, udostępnianie planów        | M5, M6         |
| **S5** | 7–13.12    | Stabilizacja, algorytm oceny planów, testy mobile, dokumentacja | —              |
| Bufor  | 14–19.12   | Tylko poprawki i oddanie, **bez nowych funkcji**                | —              |


> Weekend 7–8.11 (przed S1): wybierz framework frontendowy i załóż repozytorium. Dzięki temu S1 startuje bez opóźnień.

---



## Sprint 1 (9–15.11) — Fundament i logowanie

**Cel:** `docker compose up` uruchamia bazę, backend i frontend; da się zarejestrować i zalogować.


| ID    | Zadanie                                                                                                       | Tor     | Status |
| ----- | ------------------------------------------------------------------------------------------------------------- | ------- | ------ |
| S1-01 | Repozytorium, branche (`main` + `feat/*`, `fix/*`, `docs/*`), PR-y, `README.md` z instrukcją uruchomienia       | OPS     | 🟡 repozytorium, README, branche i Conventional Commits ustalone; zespołowe PR/review — do potwierdzenia |
| S1-02 | **Decyzja: framework frontendowy** (do wtorku, wszyscy) → **React** + Vite                                  | wszyscy | ✅ |
| S1-03 | `docker-compose.yml` z TimescaleDB + sprawdzenie połączenia z bazą                                            | OPS     | ✅ |
| S1-04 | Inicjalizacja Spring Boot (Web, Security, JPA, Validation, Flyway, PostgreSQL) + endpoint `GET /api/health`   | BE-1    | ✅ |
| S1-05 | Migracja V1: `users` (+ `verification_tokens`)                                                                | BE-1    | ✅ |
| S1-06 | Rejestracja i logowanie: `POST /api/auth/register`, `POST /api/auth/login`, JWT, konfiguracja Spring Security | BE-1    | ✅ |
| S1-07 | Inicjalizacja frontendu: routing, layout **mobile-first**, Dockerfile                                         | FE-1    | ✅ |
| S1-08 | Strony `Logowanie` i `Rejestracja` podpięte pod API, przechowywanie tokenu                                    | FE-1    | ✅ |
| S1-09 | Dockerfile backendu + frontendu, uruchomienie usług w jednym Compose                                          | OPS     | ✅ sprawdzony build i start; obecnie 4 kontenery z Mailpit |
| S1-10 | Szkic modelu danych na kartce/diagramie (uzgodniony w zespole)                                                | BE-2    | 🟡 szkic w `PLAN.md` + `docs/BACKEND_STRUCTURE.md`; diagram zespołowy — do uzgodnienia |


**Scenariusz na koniec sprintu:** `docker compose up --build` → rejestracja → potwierdzenie linkiem z Mailpit → logowanie → profil i wylogowanie.
**Poza pierwotnym zakresem S1:** potwierdzenie e-mail i reset hasła należą do S2; są już wdrożone z wyprzedzeniem.

### Raport — Sprint 1

## Raport: Sprint 1 (9–15.11)

### Zrobione

- [x] **S1-02** — React + Vite, Tailwind, layout mobile-first
- [x] **S1-03** — `docker-compose.yml` (db TimescaleDB, backend, frontend)
- [x] **S1-04** — Spring Boot, `GET /api/health`, Swagger (springdoc)
- [x] **S1-05** — Flyway `V1`: tabele `users`, `verification_tokens`
- [x] **S1-06** — rejestracja i logowanie (JWT, Spring Security), `GET /api/users/me`
- [x] **S1-07** — routing (`/`, `/logowanie`, `/rejestracja`), `AppLayout`, Dockerfile FE
- [x] **S1-08** — formularze PL, token w `localStorage`, ochrona tras (obecnie `/profil`; `/` jest publiczną stroną wejściową)
- [x] **S1-09** — obrazy Docker, uruchomienie jedną komendą `docker compose up --build`
- [x] **Scenariusz przepływu** — rejestracja → potwierdzenie w Mailpit → logowanie → profil + wylogowanie

### Częściowo

- [ ] **S1-01** — potwierdzić zespołowy proces PR/review; repozytorium, README i konwencja branchy/commitów są gotowe, a produkcja korzysta z `main`.
- [ ] **S1-10** — dokumentacja struktury i szkic tabel w `PLAN.md`; brak formalnego diagramu uzgodnionego na spotkaniu

### Nie zrobione / przeniesione

- Potwierdzenie e-mail i reset hasła — **zakres Sprintu 2** (S2-01, S2-02), już wykonane z wyprzedzeniem
- Obsługa ról `USER` / `TRAINER` i testy uprawnień — **Sprint 2** (S2-04); domknięte technicznie 15.11, szczegóły w raporcie S2

### Problemy i ryzyka

- Pierwszy start Dockera: brak `spring-boot-starter-flyway` blokował migracje (naprawione); po zmianach wymagany `docker compose up --build`
- Nagłówek UI nie reagował na logowanie bez odświeżenia strony (naprawione: synchronizacja sesji w layoucie)

### Zakres następnego tygodnia (Sprint 2; aktualny postęp poniżej)

- Potwierdzenie konta e-mail, reset hasła, usunięcie konta
- Role USER / TRAINER, zabezpieczenie endpointów
- Baza ćwiczeń (seed) i CRUD planów treningowych
- Frontend: lista ćwiczeń, kreator planu

---



## Sprint 2 (16–22.11) — Konta i plany treningowe

**Cel:** komplet funkcji kont (M1) oraz tworzenie własnych planów (M2).


| ID | Zadanie | Tor | Kto | Status na 16.11 |
|---|---|---|---|---|
| S2-01 | Potwierdzenie konta (token w e-mailu; w dev: Mailpit w Compose) | BE-1 | | ✅ aktywacja i ponowna wysyłka; SMTP sprawdzone w Dockerze |
| S2-02 | Reset hasła (`forgot-password`, `reset-password`) | BE-1 | | ✅ token jednorazowy, unieważnienie sesji i testy |
| S2-03 | Usunięcie konta: prośba z aktualnym hasłem i potwierdzenie tokenem z e-maila | BE-1 | | ✅ pełny przepływ i sprzątanie danych konta sprawdzone |
| S2-04 | Role `USER` / `TRAINER`, zabezpieczenie endpointów (`@PreAuthorize`) | BE-1 | | ✅ skrypt operatora USER ↔ TRAINER, unieważnianie sesji, ochrona ścieżek trenera i testy uprawnień |
| S2-05 | Migracje: `exercises`, `workout_plans`, `plan_days`, `plan_exercises` | BE-2 | | ✅ V3, encje i repozytoria; właściciel, kolejność, ograniczenia i kaskady usuwania sprawdzone |
| S2-06 | **Seed bazy ćwiczeń** (ok. 40–60: siłowe + domowe, z grupą mięśniową) | BE-2 | | ✅ V4: 48 ćwiczeń (24 siłowe, 24 domowe), polskie nazwy i opisy, wszystkie 12 grup mięśniowych |
| S2-07 | API: `GET /api/exercises` (filtry), CRUD `/api/plans` | BE-2 | | ✅ katalog z filtrami i pełny CRUD własnych planów; walidacja i dostęp według właściciela |
| S2-08 | Frontend: lista ćwiczeń, lista planów | FE-1 | | ✅ chronione listy z API, filtry w URL, szczegóły planu, stany ładowania/błędów/puste; sprawdzony desktop i widok mobilny |
| S2-09 | Frontend: **kreator planu** (dni → ćwiczenia → serie/powtórzenia/ciężar) | FE-1 | | ✅ tworzenie, edycja, kolejność dni/ćwiczeń, walidacja i usuwanie z potwierdzeniem; pełny przepływ UI sprawdzony w Dockerze |
| S2-10 | Frontend: ochrona tras (guard), strona profilu z usunięciem konta, ekrany potwierdzenia/resetu hasła | FE-2 | | ✅ sprawdzony profil, ekrany i przekierowanie do logowania |
| S2-11 | Pierwsze testy backendu (auth + plany, happy path) | BE-2 | | ✅ testy kont, modelu, katalogu i HTTP planów; CRUD, walidacja, odmowa dostępu do cudzych danych |


**Scenariusz:** pełna ścieżka: rejestracja → potwierdzenie → login → utworzenie planu z ćwiczeniami.

### Postęp Sprintu 2 — stan na 16.11.2026

Zrealizowano technicznie **11 z 11 zadań S2**. Cały przepływ S2 sprawdzono lokalnie w Docker Compose. Kod S2 jest scalony do `main`. Zespołowe review, raport i test na rzeczywistym telefonie pozostają do wykonania; status techniczny nie zamyka całej Definition of Done. Wdrożenie online i jego odbiór są raportowane w S3-11.

### Zrobione

- [x] **S2-01** — rejestracja wymaga aktywacji mailem; można ponowić wysyłkę linku.
- [x] **S2-02** — reset hasła przez jednorazowy link; stare hasło i wcześniejsze JWT tracą dostęp.
- [x] **S2-03** — usunięcie konta wymaga aktualnego hasła i jawnego potwierdzenia z wiadomości; samo otwarcie linku nie usuwa danych.
- [x] **S2-04 (15.11)** — operator nadaje/odbiera TRAINER przez `tools/set-user-role.sql`; tylko aktywne konta USER/TRAINER, zmiana unieważnia JWT. Rejestracja pozostaje USER. Zabezpieczono przestrzenie `/api/trainer/**` i `/api/groups/**` (także ścieżki bazowe); funkcje trenera pozostają zakresem S3. Dodano 7 testów integracyjnych ról (JWT, 401/403, `@PreAuthorize`, zmiana i odebranie uprawnień) oraz test skryptu na jednorazowym PostgreSQL 16: `pwsh -NoProfile -File tests/smoke-roles.ps1`. Instrukcja operatora: `docs/ACCOUNTS.md`.
- [x] **S2-10** — profil, guard sesji, ekrany potwierdzenia/resetu, wylogowanie i synchronizacja sesji między kartami.
- [x] **S2-05 (15.11)** — migracja V3, modele i repozytoria ćwiczeń, planów, dni i wpisów ćwiczeń. Plan ma właściciela; indeksy określają kolejność; baza wymusza dodatnie serie/powtórzenia, nieujemny ciężar i unikalne pozycje. Usunięcie konta usuwa jego plany, dni i wpisy, zachowując katalog oraz cudze plany. Pełne 48 testów backendu przeszło na PostgreSQL 16 (w tym 25 testów modelu i nowy test usunięcia konta z planem); dodanie V3 do V2 zachowuje istniejące konta/tokeny. Powtarzalny test: `pwsh -NoProfile -File tests/test-workout-postgres.ps1`. Szczegóły: `docs/WORKOUT_MODEL.md`.
- [x] **Weryfikacja kont** — 15 testów backendu (14 integracyjnych i kontekst), 4 testy pomocniczych funkcji frontendu, build i lint.
- [x] **S2-06 (15.11)** — V4 dodaje 48 wspólnych ćwiczeń bez przypisania do konta: po 2 siłowe i 2 domowe dla każdej z 12 grup mięśniowych. Każdy wpis ma polską nazwę i opis z wymaganym sprzętem. ID generuje baza, a Flyway zapobiega ponownemu wykonaniu migracji. Trzy nowe testy katalogu sprawdzają kompletność, unikalność, polskie znaki, typy/grupy i brak duplikacji przy ponownym uruchomieniu Flyway. Pełny zestaw 51 testów backendu przeszedł na H2 i PostgreSQL 16. Test aktualizacji V3 → V4 na PostgreSQL 16 zachowuje istniejące ćwiczenie i plan. Powtarzalna weryfikacja PostgreSQL: `pwsh -NoProfile -File tests/test-workout-postgres.ps1`.
- [x] **Docker i SMTP** — cztery kontenery, migracje V1/V2 na PostgreSQL, wiadomości w Mailpit, przepływy przez nginx; test równoległego resetu dopuszcza tylko jedno użycie tokenu. Powtarzalny test: `node tests/smoke-accounts.mjs` z katalogu głównego repozytorium.
- [x] **UI** — profil i ekrany kont sprawdzone na komputerze oraz w emulowanym widoku mobilnym. Użytkownik potwierdził, że wpisanie `mat` w Brave po poprawce nie powoduje crasha.

- [x] **S2-07 — katalog ćwiczeń:** `GET /api/exercises` wymaga aktywnej sesji JWT i roli USER/TRAINER/ADMIN. Filtry `muscleGroup`, `type` i `q` łączą się warunkiem AND; wyszukiwanie nazwy ignoruje wielkość liter i traktuje metaznaki LIKE dosłownie. Odpowiedź jest uporządkowaną tablicą DTO bez danych autora. Dodano 19 testów HTTP. Kontrakt: [EXERCISES_API.md](EXERCISES_API.md).
- [x] **S2-07 — plany:** POST/GET `/api/plans`, GET/PUT/DELETE `/api/plans/{id}`; właściciel z sesji, cudzy i nieistniejący plan zwracają `404`, także dla TRAINER/ADMIN. Pełny PUT zapisuje kolejność z tablic, zastępując dzieci w transakcji; usuwanie zachowuje katalog. Wymuszono walidację nazw, kolekcji, istniejących ćwiczeń, serii/powtórzeń i ciężaru. Kontrakt: [WORKOUT_PLANS_API.md](WORKOUT_PLANS_API.md).
- [x] **S2-11 — testy HTTP planów:** 41 nowych przypadków wykonuje żądania bez nadrzędnej transakcji testowej: CRUD trzech ról, wielokrotna edycja, walidacja, limity, kaskady, nieaktywne/unieważnione sesje, próba podmiany właściciela i odmowa dostępu do cudzych danych. Pełny zestaw 111 testów backendu przeszedł na H2 i PostgreSQL 16. Skrypt testów PostgreSQL czeka na gotowość serwera TCP, unikając przejściowego serwera inicjalizacji kontenera.

- [x] **S2-08:** dodano `/cwiczenia`, `/plany`, `/plany/:id`, nawigację i powrót po logowaniu z zachowaniem filtrów. Build, lint i 9 testów frontendu przeszły. Test `tests/smoke-workout-ui.mjs` przeszedł na rzeczywistym stacku Compose: filtry/historia, ładowanie, puste listy, plan i szczegóły, błędy/ponowienie, 404/401 oraz widoki 1280 × 900 i 375 × 812 bez poziomego przewijania i błędów JavaScript. Zrzuty sprawdzono wizualnie. Szczegóły: [WORKOUT_FRONTEND.md](WORKOUT_FRONTEND.md).

- [x] **S2-09:** `/plany/nowy` i `/plany/:id/edytuj` zapisują pełny plan przez POST/PUT. Wybór z katalogu ma filtry; dni i wpisy można dodawać, usuwać i przestawiać. Walidacja zachowuje szkic po błędzie, obsługuje ciężar z przecinkiem i ograniczenia API. DELETE wymaga osobnego potwierdzenia w dialogu; anulowanie i błąd nie usuwają planu. Build, lint bez ostrzeżeń i 16 testów frontendu przeszły.
- [x] **Scenariusz S2:** `tests/smoke-workout-ui.mjs` przeszedł na Compose przez nginx z rzeczywistym API i Mailpit: rejestracja w UI → aktywacja w UI z linku Mailpit → logowanie → utworzenie planu w kreatorze → edycja i trwały odczyt → usunięcie z potwierdzeniem. Sprawdzono też walidację, kolejność, zachowanie szkicu przy błędach POST/PUT, anulowanie i ponawianie DELETE oraz desktop 1280 × 900 i widok mobilny 375 × 812. Brak poziomego przewijania i błędów JavaScript; zrzuty sprawdzono wizualnie. Konto i plany testowe usunięto po teście. Szczegóły: [WORKOUT_FRONTEND.md](WORKOUT_FRONTEND.md).

### Pozostało do zamknięcia S2

- [ ] **Domknięcie zespołowe** — przypisać właścicieli zadań, przeprowadzić review i przygotować raport sprintu.
- [ ] Sprawdzić aplikację na rzeczywistym telefonie i potwierdzić pozostałe kryteria Definition of Done.

### Problemy i ryzyka

- Wrapper Maven miał CRLF, co blokowało build kontenera Linux. Naprawiono końce linii i regułę `.gitattributes`; ponowny build i start przeszły.
- Brave crashował przy wpisaniu trzech liter w pole nazwy. Zastosowano `autocomplete="off"` wyłącznie dla nazwy wyświetlanej; użytkownik potwierdził skuteczność po Ctrl+F5.
- Lokalnie pocztę przechwytuje Mailpit. Wdrożenie i sposób obsługi linków online należą do S3-11.
- Usuwanie konta obejmuje model planów (V3). W S3 rozszerzono i sprawdzono kaskady wyników, pomiarów, grup, członkostw i przypisań (V5/V6). Niezależne kopie planów należące do innych kont pozostają.

Kontrakt endpointów kont i scenariusze weryfikacji: [ACCOUNTS.md](ACCOUNTS.md).

### Następny zakres po S2 (Sprint 3)

- Wdrożenie Vercel/Supabase (S3-11, wykonane z wyprzedzeniem) i odbiór pełnych przepływów online
- Zapis wyników treningów, waga, obwody ciała (hypertabele TimescaleDB w Dockerze, zgodne zwykłe tabele w Supabase PostgreSQL 17)
- Historia i wykresy progresu
- Moduł trenera: grupy podopiecznych, zaproszenia z akceptacją, przypisywanie niezależnych kopii planów
- Test przepływu: rejestracja → plan → wynik treningu

---



## Sprint 3 (23–29.11) — Progres, trener i wdrożenie

**Cel:** zamknięcie wszystkich funkcji P1: śledzenie postępów (M3), moduł trenera (M4) i wdrożenie online na Vercel i Supabase.

> Zakres S3-01–S3-10 wykonano i sprawdzono technicznie z wyprzedzeniem 19.11.2026, łącznie z podglądem progresu podopiecznych (S3-09). Potwierdzone testy nie oznaczają scalenia, wdrożenia ani zamknięcia zespołowej Definition of Done.


| ID | Zadanie | Tor | Kto | Status na 19.11 |
|---|---|---|---|---|
| S3-01 | V5: `exercise_results`, `body_metrics`; V7: warunkowe `create_hypertable` | BE-2 | | ✅ migracje V4 → V7 sprawdzone na PostgreSQL 17 i TimescaleDB/PostgreSQL 16; dwie rzeczywiste hypertabele |
| S3-02 | API zapisu, historii i usuwania własnych wyników oraz pomiarów | BE-2 | | ✅ walidacja, własność, atomowość, filtry i kaskady sprawdzone |
| S3-03 | Frontend: wykonanie treningu z wybranego planu i dnia, wpisanie serii | FE-1 | | ✅ zapis z własnego i przypisanego planu sprawdzony przez UI |
| S3-04 | Frontend: historia i wykresy ciężaru, wagi oraz obwodów z filtrami dat | FE-2 | | ✅ pomiary, historia i wykresy sprawdzone; czytelność osi poprawiona i zweryfikowana |
| S3-05 | V6: `training_groups`, `group_members`, `plan_assignments` | BE-1 | | ✅ integralność, niezależne kopie i kaskady sprawdzone |
| S3-06 | API trenera: własne grupy, zaproszenia z akceptacją, przypisywanie planów | BE-1 | | ✅ role, własność grup i zgoda podopiecznego sprawdzone |
| S3-07 | Frontend: panel trenera, grupy, członkowie i przypisanie planu | FE-2 | | ✅ pełny przepływ panelu trenera sprawdzony przez UI |
| S3-08 | Frontend: zaproszenia, ich akceptacja, opuszczenie grupy i przypisane plany | FE-1 | | ✅ akceptacja, otrzymana kopia i trening z kopii sprawdzone |
| S3-09 | Trener: historia, pomiary i wykresy zaakceptowanego podopiecznego | BE-1 / FE-2 | | ✅ podgląd i odebranie dostępu po opuszczeniu grupy sprawdzone |
| S3-10 | Test przepływu: rejestracja → plan → wynik → akceptacja zaproszenia → przypisanie planu | BE-2 | | ✅ `tests/smoke-sprint3-ui.mjs` przeszedł na pełnym stacku Docker |
| S3-11 | Vercel/Supabase, produkcja z `main`, migracja konta i transport poczty `log` | OPS | | ✅ infrastruktura uruchomiona 16.11; publikacja i odbiór nowych funkcji według checklisty poniżej |


**Scenariusz:** użytkownik zapisuje trening i widzi wykres; trener tworzy grupę, podopieczny akceptuje zaproszenie, a trener przypisuje mu plan.

### Postęp Sprintu 3 — implementacja S3-01–S3-10 (19.11.2026)

Implementację przygotowano na `feat/sprint-3` 19.11.2026. Testy backendu, frontendu, migracji i pełnego przepływu przeszły w opisanym poniżej zakresie. Publikację, wdrożenie produkcyjne i zespołowy odbiór rozliczamy osobno w checkliście; same wyniki testów nie potwierdzają ich wykonania. Kontrakt API i instrukcja sprawdzenia: [SPRINT3.md](SPRINT3.md).

- [x] **S3-01/S3-02:** wyniki serii i pomiary ciała, walidacja zakresów, własność danych, filtry czasu i usuwanie błędnych wpisów. Zapis serii treningu jest atomowy. Historia zachowuje nazwy planu, dnia i ćwiczenia także po zmianie lub usunięciu planu.
- [x] **S3-03/S3-04:** wykonanie treningu z własnego lub przypisanego planu, historia i wykresy ciężaru, wagi oraz obwodów. API zwraca do 2000 najnowszych wyników i 500 pomiarów w wybranym zakresie; filtry dat umożliwiają odczyt starszych wpisów.
- [x] **S3-05–S3-08:** grupy należą do trenera. Zaproszenie po e-mailu wymaga akceptacji w aplikacji; użytkownik może je odrzucić lub opuścić grupę. Przypisanie tworzy niezależną kopię planu dla osoby albo zaakceptowanych członków grupy.
- [x] **S3-09:** trener widzi progres wyłącznie zaakceptowanego podopiecznego we własnej grupie. Opuszczenie grupy lub usunięcie członkostwa odbiera dostęp. Powtórne przypisanie tego samego planu nie tworzy kolejnej kopii.
- [x] **Integralność:** usunięcie konta usuwa jego progres, pomiary, grupy, członkostwa i powiązane przypisania. Cudze konta, katalog i niezależne kopie planów podopiecznych pozostają.
- [x] **S3-10:** `tests/smoke-sprint3-ui.mjs` przeszedł na pełnym stacku Docker: rejestracja z SMTP do Mailpit → aktywacja → plan w UI → zapis treningu → pomiary i wykresy → zaproszenie trenera → akceptacja → przypisanie niezależnej kopii → trening z kopii → opuszczenie grupy i odebranie dostępu.

**Uzgodniona baza:** TimescaleDB w lokalnym Dockerze; zwykłe tabele PostgreSQL z indeksami czasu w Supabase PostgreSQL 17. V5 tworzy wspólny schemat, a V7 rozpoznaje dostępność TimescaleDB i tylko wtedy tworzy hypertabele. Brak rozszerzenia w Supabase nie blokuje startu ani funkcji progresu. Zmiana hostingu bazy nie jest potrzebna.

### Wyniki sprawdzeń S3 — 19.11.2026

- [x] **Backend:** po 216 testów bez niepowodzeń i błędów na PostgreSQL 17 oraz TimescaleDB/PostgreSQL 16, w odrębnym prywatnym schemacie `training_app`. H2: 215 zaliczonych i 1 celowo pominięty test wymagający PostgreSQL, 216 łącznie.
- [x] **Migracje:** aktualizacja V4 → V7 zachowuje istniejące konto i plan; ponowne uruchomienie wykonuje 0 migracji. Na TimescaleDB potwierdzono dwie rzeczywiste hypertabele. Powtarzalny test: `tests/test-sprint3-databases.ps1`.
- [x] **Frontend:** 27 testów, build i lint zaliczone. Pełny przepływ UI przeszedł bez błędów JavaScript i poziomego przewijania przy 1280 × 900 oraz 375 × 812. Zrzuty obejrzano; poprawiono i ponownie sprawdzono czytelność podpisów osi wykresów (12 px).
- [x] **Profil produkcyjny lokalnie:** izolowany `tests/smoke-deployment.ps1` przeszedł z prywatnym schematem, migracjami V1–V7, SMTP, `PORT`, CORS i ochroną tras. Nie oznacza to zastosowania zmian w produkcyjnej bazie Supabase.

### Publikacja i odbiór S3

- [ ] **Publikacja brancha:** utworzyć commit i wypchnąć `feat/sprint-3` na GitHuba.
- [ ] **Scalenie:** połączyć `feat/sprint-3` z `main` przez commit scalający (`--no-ff`) i wypchnąć `main`.
- [ ] **Wdrożenie:** potwierdzić gotowe wdrożenia obu projektów Vercela z `main`, zdrowie API, migracje V1–V7 w Supabase, ochronę tras i CORS.
- [ ] **Review zespołowe:** przeprowadzić review z udziałem co najmniej jednej osoby z zespołu.
- [ ] **Pełny odbiór online:** na osobnym koncie testowym wykonać rejestrację, aktywację, plan, trening, pomiary, współpracę z trenerem oraz przepływy resetu hasła i usunięcia konta. Uwzględnić rzeczywiście skonfigurowany transport poczty.
- [ ] **Rzeczywisty telefon:** sprawdzić nowe ekrany na fizycznym urządzeniu.
- [ ] **Domknięcie zespołowe:** uzupełnić raport sprintu i właścicieli zadań.

### Postęp Sprintu 3 — wdrożenie S3-11 (16.11.2026)

Pierwsze wdrożenie wykonano przed planowanym terminem Sprintu 3. Opisany tu historyczny stan z 16.11 dotyczy kont i planów z S2. Status publikacji kolejnych funkcji jest rozliczany w checkliście „Publikacja i odbiór S3”.

- [x] Frontend: [aplikacja-trenerska.vercel.app](https://aplikacja-trenerska.vercel.app); backend: [API health](https://aplikacja-trenerska-api.vercel.app/api/health). Backend działa jako kontener Java 21 w regionie `dub1`.
- [x] Supabase PostgreSQL 17: prywatny schemat `training_app`, migracje V1–V4 i 48 ćwiczeń. Przeniesiono jedyne lokalne konto z zachowaniem danych i hasła; kopie zapasowe pozostają poza GitHubem. Lokalna baza nie zawierała planów.
- [x] Backend skonfigurowano bez dostawcy SMTP, z `APP_MAIL_MODE=log`; linki do operacji na koncie zapisywały się w prywatnych logach API. Nie potwierdza to dostarczenia wiadomości do użytkownika.
- [x] Scalenie do `main` (`ee2fa1e`), wdrożenie tego commita w obu projektach i ustawienie `main` jako Production Branch. Tymczasowy `feat/deployment` usunięto lokalnie i na GitHubie.
- [x] Sprawdzenia online: backend `UP`, zapytanie do bazy przy logowaniu nieistniejącym kontem (`401`), ochrona profilu/katalogu/planów bez JWT (`401`), CORS dla frontendu i odrzucenie obcego originu (`403`), trasy SPA (`200`) oraz poprawny adres API w paczce frontendu.
- [x] Sprawdzenia lokalne konfiguracji: 157 testów backendu, 21 testów frontendu, build i lint. Test integracyjny profilu `prod` obejmuje aktywację, reset i usunięcie konta bez SMTP; nie jest to potwierdzenie wykonania tych operacji na żywej bazie.

### Pozostałe sprawdzenia wdrożenia — S3-11

- [ ] Na osobnym koncie testowym wykonać pełny przepływ online: rejestracja → aktywacja → logowanie → CRUD planu → reset hasła → potwierdzone usunięcie konta. Uwzględnić rzeczywiście skonfigurowany transport poczty.
- [ ] Potwierdzić brak błędów przeglądarki podczas pełnych przepływów online zgodnie z [checklistą wdrożenia](DEPLOYMENT.md#5-sprawdź-wdrożenie).

### Ryzyka wdrożenia

- Ostatnio potwierdzona konfiguracja online używa `log`, bez wysyłki wiadomości. Linki pozwalają wykonać operacje na koncie, dlatego logi pozostają prywatne. Dostarczanie wiadomości do użytkowników wymaga skonfigurowanego SMTP.
- W Supabase PostgreSQL 17 nie ma TimescaleDB. Uzgodnione V5/V7 zachowują zgodność zwykłych tabel online i hypertabel lokalnych; oba warianty oraz aktualizację istniejącej bazy sprawdzono lokalnie. Po wdrożeniu należy potwierdzić wykonanie nowych migracji i przepływy na produkcji. Szczegóły: [DEPLOYMENT.md](DEPLOYMENT.md#preview-i-kolejne-migracje).

### Raport — Sprint 3

```markdown
## Raport: Sprint 3 (23–29.11)
### Zrobione
- [x] S3-01–S3-10 — implementacja przygotowana 19.11 na `feat/sprint-3` i sprawdzona technicznie; wyniki testów opisano powyżej
- [x] S3-11 — pierwsze wdrożenie Vercel/Supabase wykonane 16.11, produkcja z `main`
### Publikacja i odbiór
- Status commita, pushowania, scalenia i wdrożenia: checklista „Publikacja i odbiór S3”
- Review zespołowe, test na rzeczywistym telefonie i pełny odbiór online są potwierdzane niezależnie
### Problemy i ryzyka
-
### Plan na następny tydzień (Sprint 4)
- Kalkulatory: 1RM oraz kalorie/makro
- Kalendarz treningów i proste przypomnienia
- Statystyki (objętość tygodniowa, rekordy)
- Udostępnianie planów
```

---



## Sprint 4 (30.11–6.12) — Moduły dodatkowe

**Cel:** komplet modułów P2. Po tym sprincie spełniamy kryterium **≥ 6 modułów**.


| ID    | Zadanie                                                                                                         | Tor         | Kto |
| ----- | --------------------------------------------------------------------------------------------------------------- | ----------- | --- |
| S4-01 | **Kalkulator 1RM** (np. wzór Epleya): endpoint + widok                                                          | BE-2 / FE-1 |     |
| S4-02 | **Kalkulator kalorii i makro** (np. Mifflin-St Jeor): endpoint + widok                                          | BE-2 / FE-1 |     |
| S4-03 | **Kalendarz**: tabela `scheduled_workouts`, planowanie treningu na dany dzień, widok kalendarza                 | BE-1 / FE-2 |     |
| S4-04 | **Przypomnienia** (wersja prosta: lista „dziś do zrobienia" na dashboardzie; opcjonalnie `@Scheduled` + e-mail) | BE-1        |     |
| S4-05 | **Statystyki**: objętość tygodniowa, rekordy i widok; wspólne zapytania PostgreSQL (`date_trunc`, opcjonalnie `time_bucket` lokalnie) | BE-2 / FE-2 |     |
| S4-06 | **Udostępnianie planów** (link / `is_shared`, kopiowanie planu do swojego konta)                                | BE-2 / FE-1 |     |
| S4-07 | Dashboard (strona główna po zalogowaniu: najbliższy trening, ostatnie wyniki)                                   | FE-1        |     |
| S4-08 | Przegląd UI na telefonie, lista błędów do S5                                                                    | wszyscy     |     |


**Scenariusz:** wszystkie moduły widoczne w aplikacji.

### Raport — Sprint 4

```markdown
## Raport: Sprint 4 (30.11–6.12)
### Zrobione
- [ ] S4-01 ...
### Nie zrobione / przeniesione
-
### Problemy i ryzyka
-
### Plan na następny tydzień (Sprint 5)
- Algorytm oceny planów treningowych
- Poprawki UI na urządzeniach mobilnych
- Naprawa błędów, testy
- Dokumentacja (README, API) i dane przykładowe
- Test uruchomienia od zera w Dockerze
```

---



## Sprint 5 (7–13.12) — Stabilizacja i dokumentacja

**Cel:** aplikacja działa stabilnie i da się ją uruchomić jedną komendą na czystej maszynie.


| ID    | Zadanie                                                                                                      | Tor         | Kto |
| ----- | ------------------------------------------------------------------------------------------------------------ | ----------- | --- |
| S5-01 | **Algorytm oceny planów** (prosty scoring: pokrycie grup mięśniowych, objętość tygodniowa, balans push/pull) | BE-2        |     |
| S5-02 | Poprawki UI: test na prawdziwym telefonie i na komputerze                                                    | FE-1 / FE-2 |     |
| S5-03 | Bugfixy z listy z S4-08                                                                                      | wszyscy     |     |
| S5-04 | Dokumentacja: `README`, instrukcja uruchomienia, opis API (springdoc / Swagger)                              | BE-1        |     |
| S5-05 | Dane przykładowe (użytkownik, trener, przykładowe plany i wyniki)                                            | BE-2        |     |
| S5-06 | Test „od zera": czysty clone → `docker compose up` → działa                                                  | OPS         |     |
| S5-07 | *(opcjonalnie, tylko gdy wszystko wyżej gotowe)* mały moduł social/DM lub lista wydarzeń sportowych          | —           |     |




### Raport — Sprint 5

```markdown
## Raport: Sprint 5 (7–13.12)
### Zrobione
- [ ] S5-01 ...
### Nie zrobione / przeniesione
-
### Plan na ostatni tydzień (14–19.12)
- Tylko poprawki błędów i dokumentacja (bez nowych funkcji)
- Finalny test uruchomienia w Dockerze
- Przygotowanie do oddania projektu
```

---



## Bufor (14–19.12)

Bez nowych funkcji. Tylko: poprawki błędów, finalne `docker compose up` na czystym środowisku, uzupełnienie dokumentacji, przygotowanie do oddania.

---



## Co ciąć, jeśli sprint się nie wyrabia

Obcinamy w tej kolejności (od pierwszego do ostatniego):

1. Wydarzenia sportowe, social media / DM (P3, i tak opcjonalne)
2. Przypomnienia (zostaje sam kalendarz)
3. Algorytm oceny planów (wersja uproszczona lub pominięta)
4. Kalkulator kalorii/makro (zostaje 1RM)
5. Udostępnianie planów

**Nigdy nie tniemy:** konta i autoryzacja, plany, progres, moduł trenera, Docker.

## Zasady pracy w sprincie

- Na początku tygodnia (pon) podziel zadania w kolumnie `Kto`, w środę krótki check, czy ktoś nie jest zablokowany.
- Zadanie jest zrobione dopiero po spełnieniu Definition of Done z `PLAN.md` (PR zmergowany, działa w Dockerze, działa na telefonie).
- To, czego nie zrobicie, przenosicie do następnego sprintu w sekcji „Nie zrobione / przeniesione".
- Raport kopiujesz z szablonu powyżej: sekcja „Plan na następny tydzień" jest już wstępnie wypełniona, więc wystarczy uzupełnić „Zrobione".

