# Administrator aplikacji

Panel `/admin` jest dostępny dla aktywnego, niezablokowanego konta z rolą `ADMIN`. Backend sprawdza aktualną rolę i stan konta przy każdym żądaniu; sam link lub rola zapisana w przeglądarce nie nadają uprawnień.

Rozszerzenie **S3-12** przygotowano, sprawdzono lokalnie i opublikowano 19.11.2026. Oba projekty Vercela osiągnęły `READY` dla kodu scalonego do `main` przez [`ecacd1d`](https://github.com/Aplikacja-trenerska-studia/Aplikacja_trenerska/commit/ecacd1dd448c21d71e52f26c8ecf1037eeb1c730). Logi potwierdzają migrację Supabase do V8 i nadanie pierwszych uprawnień administratora. Publikację oraz pełny odbiór rozlicza [checklista sprintu](SPRINTY.md#rozszerzenie-s3-12--administrator-19112026).

## Zakres panelu

- Podsumowanie: liczba kont, aktywnych użytkowników, kont trenerów i administratorów, planów, zapisanych serii oraz grup.
- Lista użytkowników z wyszukiwaniem nazwy/e-maila, filtrami roli i stanu oraz stronicowaniem.
- Nadawanie i odbieranie ról `USER`, `TRAINER`, `ADMIN` po potwierdzeniu zmiany.
- Blokowanie i odblokowywanie dostępu po potwierdzeniu. Blokada zachowuje dane konta.

Podsumowanie pokazuje liczbę **serii**, a nie liczbę całych treningów. Liczby trenerów i administratorów obejmują konta z daną rolą; liczba aktywnych kont jest raportowana osobno. Panel nie udostępnia cudzych haseł, tokenów, planów ani historii pomiarów.

## Pierwszy administrator

Pierwsze uprawnienie jest nadawane istniejącemu kontu przez jednorazową konfigurację backendu. Nie ma domyślnego hasła administratora, automatycznej rejestracji administratora ani publicznego endpointu inicjalizacji.

1. Utwórz zwykłe konto, potwierdź jego e-mail i sprawdź logowanie. Konto musi być niezablokowane.
2. W projekcie **backendu** Vercela ustaw zmienną **Production**:

   ```dotenv
   APP_BOOTSTRAP_ADMIN_EMAIL=<e-mail istniejącego, aktywnego konta>
   ```

3. Wdróż backend zawierający migrację V8. Przy starcie Flyway doda pole `blocked` i tabelę sterującą inicjalizacją. Inicjalizator podniesie wyłącznie wskazane konto do `ADMIN` i unieważni jego wcześniejsze sesje.
4. Usuń zmienną `APP_BOOTSTRAP_ADMIN_EMAIL` z Vercela i wykonaj ponowne wdrożenie. Inicjalizacja jest zapisana w bazie jako wykonana; usunięcie zmiennej nie odbiera nadanej roli.
5. Zaloguj się ponownie i otwórz [panel administratora](https://aplikacja-trenerska.vercel.app/admin). Kolejne role zmieniaj już z panelu.

Domyślnie zmienna jest pusta i inicjalizacja niczego nie zmienia. Nie wpisuj rzeczywistego adresu konta do kodu, migracji, plików przykładowych ani repozytorium.

Inicjalizacja działa tylko raz. Jeśli w bazie istnieje już jakiekolwiek konto `ADMIN`, również oczekujące na aktywację lub zablokowane, nie nadaje roli kolejnemu kontu i oznacza inicjalizację jako zakończoną. Pozostawienie lub późniejsza zmiana zmiennej nie służy do ponownego nadawania ról. Brak wskazanego konta, niepotwierdzony e-mail lub blokada zatrzymują start zamiast utworzyć konto, potwierdzić jego e-mail albo zmienić hasło.

Lokalnie ustaw tę samą zmienną w środowisku procesu backendu lub konfiguracji Compose, używając lokalnego aktywnego konta. Pierwsza inicjalizacja dotyczy danej bazy, a nie komputera czy pojedynczego kontenera.

## Role i stan konta

`enabled` oznacza potwierdzenie e-maila, natomiast `blocked` jest osobną blokadą administracyjną:

| Stan | Warunek | Logowanie |
|---|---|---|
| Aktywne | `enabled = true`, `blocked = false` | Dozwolone |
| Oczekujące | `enabled = false`, `blocked = false` | Po potwierdzeniu e-maila |
| Zablokowane | `blocked = true` | Niedozwolone |

Odblokowanie nie potwierdza e-maila. Potwierdzenie e-maila i reset hasła nie zdejmują blokady administracyjnej. Blokada oraz faktyczna zmiana roli zwiększają `token_version`, więc wcześniejsze JWT przestają działać; po odblokowaniu lub zmianie roli użytkownik loguje się ponownie. Powtórzenie identycznej wartości jest bezpieczne i nie unieważnia kolejny raz sesji.

Rolę można zmieniać kontu z potwierdzonym e-mailem. Nadanie `ADMIN` wymaga dodatkowo braku blokady. Administrator nie może zmienić własnej roli ani zablokować własnego konta. Aplikacja chroni także ostatnie aktywne konto `ADMIN`, w tym przed usunięciem przez jego własny link potwierdzający. Przed odebraniem ostatniego dostępu trzeba mieć inne aktywne konto administratora.

Operacje administracyjne, inicjalizacja i potwierdzenie usunięcia konta korzystają ze wspólnej blokady transakcyjnej. Dwie równoległe operacje nie mogą usunąć ostatnich uprawnień administratora.

`ADMIN` nie otrzymuje automatycznie roli `TRAINER`. Może zarządzać kontami w panelu administracyjnym i korzystać z własnych planów oraz progresu. Prowadzenie grup pozostaje uprawnieniem konta `TRAINER`; to odrębna rola.

## API administratora

Wszystkie endpointy wymagają JWT aktualnego, aktywnego administratora. Brak sesji daje `401`, niewłaściwa rola `403`, brak konta `404`, a niedozwolona zmiana `409`.

| Metoda | Endpoint | Działanie |
|---|---|---|
| GET | `/api/admin/overview` | `{users, activeUsers, trainers, admins, plans, results, groups}` |
| GET | `/api/admin/users?q=&role=&status=&page=0&size=20` | Wyszukiwana i stronicowana lista kont |
| PATCH | `/api/admin/users/{id}/role` | JSON `{ "role": "TRAINER" }` |
| PATCH | `/api/admin/users/{id}/block` | JSON `{ "blocked": true }` lub `false` |

Wynik listy: `{content, page, size, totalElements, totalPages}`. Każdy użytkownik zawiera `id`, `email`, `displayName`, `role`, `enabled`, `blocked`, `createdAt`. Zmiana zwraca ten sam format pojedynczego użytkownika. Pola haseł i tokenów nie są zwracane.

Filtry łączą się warunkiem AND. `q` szuka fragmentu e-maila lub nazwy bez rozróżniania wielkości liter, a znaki `%` i `_` są traktowane dosłownie. `role` przyjmuje `USER`, `TRAINER`, `ADMIN`, `status` — `ACTIVE`, `PENDING`, `BLOCKED`. Strony liczone są od zera, maksymalny rozmiar strony wynosi 100.

## Scenariusz sprawdzenia

1. Zainicjalizuj pierwszego administratora i zaloguj się ponownie. Konto `USER` i `TRAINER` nie powinno mieć dostępu do `/api/admin/**`.
2. Znajdź inne aktywne konto po nazwie lub e-mailu; nadaj `TRAINER`. Sprawdź, że jego dotychczasowy JWT przestał działać, a po ponownym logowaniu jest dostępny panel trenera.
3. Zablokuj konto. Sprawdź odmowę logowania i dostępu starym JWT. Odblokuj je i potwierdź, że wcześniejsze dane nadal istnieją.
4. Sprawdź konto oczekujące na aktywację: odblokowanie nie pozwala ominąć potwierdzenia e-maila; potwierdzenie nie usuwa trwającej blokady.
5. Sprawdź wyszukiwanie, filtry, strony listy oraz potwierdzenia i anulowanie zmian.

`AdminApiIntegrationTests` obejmuje API panelu, uprawnienia, listę i zmiany ról. `AdminLifecycleIntegrationTests` sprawdza wpływ blokad na aktywację/reset/sesję, jednorazową inicjalizację, ochronę ostatniego administratora i równoległe potwierdzanie usunięcia kont. Zakres wykonanych testów i publikacji należy raportować osobno od samej implementacji.

## Potwierdzone sprawdzenia — 19.11.2026

- Pełny zestaw **262 testów backendu** przeszedł bez błędów na PostgreSQL 17 oraz TimescaleDB/PostgreSQL 16. Na H2 zaliczono 260 testów, a dwa testy wymagające rzeczywistego PostgreSQL zostały celowo pominięte.
- `AdminMigrationIntegrationTests` potwierdza aktualizację V7 → V8 z zachowaniem istniejącego konta, skrótu hasła, wersji sesji, wyników, pomiarów i grup. Sama migracja nie nadaje roli `ADMIN`; ponowny start nie wykonuje V8 ponownie.
- **33 testy frontendu**, build i lint przeszły. Pełny test UI sprawdził panel, wyszukiwanie i filtry, anulowanie zmiany, błąd zapisu `503` i ponowienie, przejścia `USER` → `TRAINER` → `ADMIN` → `USER`, blokadę/odblokowanie i unieważnianie JWT.
- Widoki **1280 × 900** oraz **375 × 812** nie mają poziomego przewijania ani błędów JavaScript. Zrzuty sprawdzono wizualnie.
- Izolowany test profilu `prod` przeszedł z migracjami V1–V8, schematem `training_app`, SMTP, portem hostingu i CORS. Test skryptu operatora potwierdził także odmowę zmiany roli zablokowanego konta.

## Publikacja i odbiór online — 19.11.2026

- `feat/admin-panel` wypchnięto z commitem `78f9918` i scalono do `main` przez `ecacd1d`. Oba produkcyjne wdrożenia Vercela dla tego samego kodu mają `READY`, a backend zwraca `UP`.
- Logi Flyway o 18:08 UTC (19:08 w Warszawie) potwierdzają wykonanie jednej migracji V7 → V8 w Supabase w 0,110 s. Kolejny start rozpoznał wersję 8 bez ponawiania migracji.
- Log inicjalizatora potwierdził nadanie pierwszej roli `ADMIN`. Po potwierdzeniu usunięto `APP_BOOTSTRAP_ADMIN_EMAIL` z konfiguracji Production. Adres konta i jego identyfikator nie są częścią dokumentacji ani repozytorium.
- Strona `/admin` zwraca `200`, a paczka frontendu zawiera panel administratora. `/api/admin/users` i `/api/admin/overview` bez JWT zwracają `401`.

Właściciel konta musi ponownie się zalogować, ponieważ nadanie roli unieważniło wcześniejsze sesje. Potwierdzenie nadania uprawnień pochodzi z logów inicjalizatora; nie logowano się hasłem użytkownika. Pełne sprawdzenie listy i operacji w panelu online na zalogowanym koncie, zespołowe review i test na rzeczywistym telefonie pozostają do potwierdzenia.

## Powtarzalne testy

Testy backendu nie wymagają dostępu do produkcyjnego Supabase ani kont Vercela:

```powershell
cd backend
.\mvnw.cmd test
cd ..
pwsh -NoProfile -File tests/test-sprint3-databases.ps1 -Database postgres
pwsh -NoProfile -File tests/test-sprint3-databases.ps1 -Database timescale
pwsh -NoProfile -File tests/smoke-deployment.ps1
pwsh -NoProfile -File tests/smoke-roles.ps1
```

Nazwa `test-sprint3-databases.ps1` jest zachowana; skrypt uruchamia cały obecny zestaw, w tym testy administratora i migracji V8.

Frontend sprawdzisz z katalogu repozytorium:

```powershell
cd frontend
npm ci
npm test
npm run build
npm run lint
cd ..
```

Test przeglądarkowy `tests/smoke-admin-ui.mjs` wymaga Playwright oraz Chromium. Playwright można udostępnić przez `NODE_PATH`, a inną lokalną instalację Chromium przez `UI_CHROME_PATH`. Skrypt przyjmuje wyłącznie lokalne adresy HTTP i projekt Compose o nazwie `admin-*`; używa lokalnego konta `dev@example.test` oraz własnego jednorazowego konta, które sprząta standardowym przepływem potwierdzonym wiadomością w Mailpit.

Przykład z oddzielnym projektem testowym:

```powershell
$env:APP_BOOTSTRAP_ADMIN_EMAIL = 'dev@example.test'
$env:COMPOSE_PROJECT_NAME = 'admin-validation'
docker compose -p admin-validation up -d --build
# Poczekaj na UP pod http://localhost:3000/api/health.
node tests/smoke-admin-ui.mjs
docker compose -p admin-validation down -v
Remove-Item Env:APP_BOOTSTRAP_ADMIN_EMAIL
Remove-Item Env:COMPOSE_PROJECT_NAME
```

Polecenie `down -v` w tym przykładzie usuwa wyłącznie dane oddzielnego projektu `admin-validation`. Test nie korzysta z istniejącej bazy aplikacji ani z produkcyjnych kont. Konto `dev` jest tworzone tylko w lokalnym profilu `dev`; konfiguracja produkcyjna nie tworzy takiego konta.
