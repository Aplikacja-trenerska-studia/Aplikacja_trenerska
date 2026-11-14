# Sprinty — aplikacja trenerska

> Uzupełnienie do `PLAN.md`. Sprint = 1 tydzień (pon–nd). Termin oddania projektu: **19.12.2026**.

> **Stan na 14.11.2026:** kalendarzowo trwa S1 (9–15.11). Funkcje kont przewidziane na S2 (16–22.11) wykonano częściowo z wyprzedzeniem. Poniższe statusy opisują aktualny kod i wykonane sprawdzenia.

**Legenda:** ✅ / `[x]` — technicznie wykonane i sprawdzone w opisanym zakresie; 🟡 — częściowo; ☐ / `[ ]` — pozostaje do wykonania albo potwierdzenia. Status techniczny nie potwierdza zespołowego review, merge do `main` ani całej Definition of Done.

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
| **S3** | 23–29.11   | Progres (TimescaleDB) i moduł trenera                           | M3, M4         |
| **S4** | 30.11–6.12 | Kalkulatory, kalendarz, statystyki, udostępnianie planów        | M5, M6         |
| **S5** | 7–13.12    | Stabilizacja, algorytm oceny planów, testy mobile, dokumentacja | —              |
| Bufor  | 14–19.12   | Tylko poprawki i oddanie, **bez nowych funkcji**                | —              |


> Weekend 7–8.11 (przed S1): wybierz framework frontendowy i załóż repozytorium. Dzięki temu S1 startuje bez opóźnień.

---



## Sprint 1 (9–15.11) — Fundament i logowanie

**Cel:** `docker compose up` uruchamia bazę, backend i frontend; da się zarejestrować i zalogować.


| ID    | Zadanie                                                                                                       | Tor     | Status |
| ----- | ------------------------------------------------------------------------------------------------------------- | ------- | ------ |
| S1-01 | Repozytorium, branche (`main` + `feature/*`), PR-y, `README.md` z instrukcją uruchomienia                     | OPS     | 🟡 repozytorium i README istnieją; konwencja branchy, PR i review — do potwierdzenia w zespole |
| S1-02 | **Decyzja: framework frontendowy** (do wtorku, wszyscy) → **React** + Vite                                  | wszyscy | ✅ |
| S1-03 | `docker-compose.yml` z TimescaleDB + sprawdzenie połączenia z bazą                                            | OPS     | ✅ |
| S1-04 | Inicjalizacja Spring Boot (Web, Security, JPA, Validation, Flyway, PostgreSQL) + endpoint `GET /api/health`   | BE-1    | ✅ |
| S1-05 | Migracja V1: `users` (+ `verification_tokens`)                                                                | BE-1    | ✅ |
| S1-06 | Rejestracja i logowanie: `POST /api/auth/register`, `POST /api/auth/login`, JWT, konfiguracja Spring Security | BE-1    | ✅ |
| S1-07 | Inicjalizacja frontendu: routing, layout **mobile-first**, Dockerfile                                         | FE-1    | ✅ |
| S1-08 | Strony `Logowanie` i `Rejestracja` podpięte pod API, przechowywanie tokenu                                    | FE-1    | ✅ |
| S1-09 | Dockerfile backendu + frontendu, uruchomienie usług w jednym Compose                                          | OPS     | ✅ sprawdzony build i start; obecnie 4 kontenery z Mailpit |
| S1-10 | Szkic modelu danych na kartce/diagramie (uzgodniony w zespole)                                                | BE-2    | 🟡 szkic w `PLAN.md` + `docs/BACKEND_STRUCTURE.md`; diagram zespołowy — do uzgodnienia |


**Demo na koniec sprintu:** `docker compose up --build` → rejestracja → potwierdzenie linkiem z Mailpit → logowanie → profil i wylogowanie.
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
- [x] **Demo** — rejestracja → potwierdzenie w Mailpit → logowanie → profil + wylogowanie

### Częściowo

- [ ] **S1-01** — potwierdzić konwencję branchy i PR-ów oraz przeprowadzić zespołowe review zmian; repozytorium Git i `README.md` są gotowe
- [ ] **S1-10** — dokumentacja struktury i szkic tabel w `PLAN.md`; brak formalnego diagramu uzgodnionego na spotkaniu

### Nie zrobione / przeniesione

- Potwierdzenie e-mail i reset hasła — **zakres Sprintu 2** (S2-01, S2-02), już wykonane z wyprzedzeniem
- Domknięcie obsługi ról `USER` / `TRAINER` i testów uprawnień — **Sprint 2** (S2-04); enum, rola w JWT i `@PreAuthorize` dla kont są już w kodzie

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


| ID | Zadanie | Tor | Kto | Status na 14.11 |
|---|---|---|---|---|
| S2-01 | Potwierdzenie konta (token w e-mailu; w dev: Mailpit w Compose) | BE-1 | | ✅ aktywacja i ponowna wysyłka; SMTP sprawdzone w Dockerze |
| S2-02 | Reset hasła (`forgot-password`, `reset-password`) | BE-1 | | ✅ token jednorazowy, unieważnienie sesji i testy |
| S2-03 | Usunięcie konta: prośba z aktualnym hasłem i potwierdzenie tokenem z e-maila | BE-1 | | ✅ pełny przepływ i sprzątanie danych konta sprawdzone |
| S2-04 | Role `USER` / `TRAINER`, zabezpieczenie endpointów (`@PreAuthorize`) | BE-1 | | 🟡 role w modelu i JWT, ochrona kont; nadawanie TRAINER i testy uprawnień do domknięcia |
| S2-05 | Migracje: `exercises`, `workout_plans`, `plan_days`, `plan_exercises` | BE-2 | | ☐ brak modułu planów i jego tabel |
| S2-06 | **Seed bazy ćwiczeń** (ok. 40–60: siłowe + domowe, z grupą mięśniową) | BE-2 | | ☐ do wykonania |
| S2-07 | API: `GET /api/exercises` (filtry), CRUD `/api/plans` | BE-2 | | ☐ do wykonania |
| S2-08 | Frontend: lista ćwiczeń, lista planów | FE-1 | | ☐ do wykonania |
| S2-09 | Frontend: **kreator planu** (dni → ćwiczenia → serie/powtórzenia/ciężar) | FE-1 | | ☐ do wykonania |
| S2-10 | Frontend: ochrona tras (guard), strona profilu z usunięciem konta, ekrany potwierdzenia/resetu hasła | FE-2 | | ✅ sprawdzony profil, ekrany i przekierowanie do logowania |
| S2-11 | Pierwsze testy backendu (auth + plany, happy path) | BE-2 | | 🟡 testy kont gotowe; testy planów po implementacji |


**Demo:** pełna ścieżka: rejestracja → potwierdzenie → login → utworzenie planu z ćwiczeniami.

### Postęp Sprintu 2 — stan na 14.11.2026

Zrealizowano technicznie **4 z 11 zadań**, 2 są częściowe, a 5 pozostaje otwartych. Poniższy raport jest aktualizacją postępu przed rozpoczęciem kalendarzowego S2; końcowy raport i review pozostają po stronie zespołu.

### Zrobione

- [x] **S2-01** — rejestracja wymaga aktywacji mailem; można ponowić wysyłkę linku.
- [x] **S2-02** — reset hasła przez jednorazowy link; stare hasło i wcześniejsze JWT tracą dostęp.
- [x] **S2-03** — usunięcie konta wymaga aktualnego hasła i jawnego potwierdzenia z wiadomości; samo otwarcie linku nie usuwa danych.
- [x] **S2-10** — profil, guard sesji, ekrany potwierdzenia/resetu, wylogowanie i synchronizacja sesji między kartami.
- [x] **Weryfikacja kont** — 15 testów backendu (14 integracyjnych i kontekst), 4 testy pomocniczych funkcji frontendu, build i lint.
- [x] **Docker i SMTP** — cztery kontenery, migracje V1/V2 na PostgreSQL, wiadomości w Mailpit, przepływy przez nginx; test równoległego resetu dopuszcza tylko jedno użycie tokenu. Powtarzalny test: `node tests/smoke-accounts.mjs` z katalogu głównego repozytorium.
- [x] **UI** — profil i ekrany kont sprawdzone na komputerze oraz w emulowanym widoku mobilnym. Użytkownik potwierdził, że wpisanie `mat` w Brave po poprawce nie powoduje crasha.

### Pozostało do zamknięcia S2

- [ ] **S2-04** — ustalić i wdrożyć bezpieczny sposób nadawania roli TRAINER; dodać testy dostępu zgodnego z rolami. Rejestracja obecnie zawsze tworzy USER.
- [ ] **S2-05** — dodać migracje i model ćwiczeń, planów, dni treningowych oraz ćwiczeń w planie.
- [ ] **S2-06** — przygotować około 40–60 ćwiczeń siłowych/domowych z grupami mięśniowymi.
- [ ] **S2-07** — zaimplementować listę ćwiczeń z filtrami i CRUD planów. Zapewnić, że użytkownik ma dostęp wyłącznie do swoich planów.
- [ ] **S2-08** — podłączyć listę ćwiczeń i listę planów na frontendzie.
- [ ] **S2-09** — dodać kreator i obsługę edycji planu: dni, ćwiczenia, serie, powtórzenia, ciężar oraz usuwanie planów.
- [ ] **S2-11** — uzupełnić testy planów: tworzenie, odczyt, edycja, usunięcie, walidacja i odmowa dostępu do cudzych danych.
- [ ] **Demo S2** — sprawdzić w Dockerze pełną ścieżkę: rejestracja → potwierdzenie → logowanie → utworzenie planu z ćwiczeniami; sprawdzić nowe widoki na komputerze i w układzie mobilnym.
- [ ] **Domknięcie zespołowe** — przypisać właścicieli zadań, wykonać PR/review i merge oraz potwierdzić pozostałe kryteria Definition of Done, w tym sprawdzenie na rzeczywistym telefonie.

### Problemy i ryzyka

- Wrapper Maven miał CRLF, co blokowało build kontenera Linux. Naprawiono końce linii i regułę `.gitattributes`; ponowny build i start przeszły.
- Brave crashował przy wpisaniu trzech liter w pole nazwy. Zastosowano `autocomplete="off"` wyłącznie dla nazwy wyświetlanej; użytkownik potwierdził skuteczność po Ctrl+F5.
- Mailpit przechwytuje wiadomości lokalnie — nie trafiają do rzeczywistych skrzynek. Zewnętrzny SMTP jest konfiguracją wdrożeniową, a lokalny Mailpit spełnia założenia testów S2.
- Przy dodaniu planów trzeba rozszerzyć usuwanie konta o nowe dane i sprawdzić integralność relacji.

Kontrakt endpointów kont i scenariusze weryfikacji: [ACCOUNTS.md](ACCOUNTS.md).

### Następny zakres po S2 (Sprint 3)

- Zapis wyników treningów, waga, obwody ciała (TimescaleDB hypertables)
- Historia i wykresy progresu
- Moduł trenera: grupy podopiecznych, przypisywanie planów
- Test przepływu: rejestracja → plan → wynik treningu

---



## Sprint 3 (23–29.11) — Progres i trener

**Cel:** zamknięcie wszystkich funkcji P1: śledzenie postępów (M3) i moduł trenera (M4).

> To najcięższy sprint. Jeśli zabraknie czasu, **S3-09 (podgląd progresu podopiecznych) przenosimy do S4**.


| ID    | Zadanie                                                                               | Tor         | Kto |
| ----- | ------------------------------------------------------------------------------------- | ----------- | --- |
| S3-01 | Migracje: `exercise_results`, `body_metrics` + `create_hypertable`                    | BE-2        |     |
| S3-02 | API: `POST/GET /api/results`, `POST/GET /api/body-metrics`                            | BE-2        |     |
| S3-03 | Frontend: ekran „wykonaj trening" (wybór planu → wpisanie serii)                      | FE-1        |     |
| S3-04 | Frontend: historia + **wykresy** (ciężar w czasie, waga ciała, obwody)                | FE-2        |     |
| S3-05 | Migracje: `training_groups`, `group_members`, `plan_assignments`                      | BE-1        |     |
| S3-06 | API trenera: tworzenie grup, dodawanie podopiecznych, `assign-plan`                   | BE-1        |     |
| S3-07 | Frontend: panel trenera (grupy, podopieczni, przypisanie planu)                       | FE-2        |     |
| S3-08 | Frontend: użytkownik widzi plany przypisane przez trenera                             | FE-1        |     |
| S3-09 | Trener: podgląd progresu podopiecznego (API + widok)                                  | BE-1 / FE-2 |     |
| S3-10 | Test przepływu kluczowego: rejestracja → plan → zapis wyniku → trener przypisuje plan | BE-2        |     |


**Demo:** użytkownik zapisuje trening i widzi wykres; trener tworzy grupę i przypisuje plan.

### Raport — Sprint 3

```markdown
## Raport: Sprint 3 (23–29.11)
### Zrobione
- [ ] S3-01 ...
### Nie zrobione / przeniesione
-
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
| S4-05 | **Statystyki**: objętość tygodniowa, rekordy (zapytania z `time_bucket`) + widok                                | BE-2 / FE-2 |     |
| S4-06 | **Udostępnianie planów** (link / `is_shared`, kopiowanie planu do swojego konta)                                | BE-2 / FE-1 |     |
| S4-07 | Dashboard (strona główna po zalogowaniu: najbliższy trening, ostatnie wyniki)                                   | FE-1        |     |
| S4-08 | Przegląd UI na telefonie, lista błędów do S5                                                                    | wszyscy     |     |


**Demo:** wszystkie moduły widoczne w aplikacji.

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
- Dokumentacja (README, API) i dane demonstracyjne
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
| S5-05 | Dane demo (użytkownik, trener, przykładowe plany i wyniki)                                                   | BE-2        |     |
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
- Przygotowanie prezentacji / oddanie projektu
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

