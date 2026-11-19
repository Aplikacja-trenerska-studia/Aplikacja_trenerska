# Wdrożenie: Vercel i Supabase

## Stan wdrożenia — 19 listopada 2026

Wdrożenie działa publicznie. Zadanie **S3-11** należy do **Sprintu 3 (23–29 listopada)**; pierwsze wdrożenie wykonano z wyprzedzeniem **16 listopada 2026**, a progres i moduł trenera opublikowano **19 listopada**. Oba projekty Vercel śledzą produkcyjny branch `main`; sprawdzono kod po scaleniu [`f40d008`](https://github.com/Aplikacja-trenerska-studia/Aplikacja_trenerska/commit/f40d008a4639c8658cf8b181dc8e1ab6eed6d1ce).

**Aktualizacja z 19.11.2026:** `feat/sprint-3` wypchnięto i scalono commitem merge. Frontend i backend Vercela osiągnęły `READY` dla tego samego kodu. Logi Flyway potwierdzają aktualizację Supabase ze schematu V4 do V7: trzy migracje wykonano poprawnie o 18:38 czasu polskiego (0,472 s), a kolejny start rozpoznał wersję 7 bez ponawiania migracji. Na Supabase PostgreSQL 17 V7 pozostawia zwykłe tabele z indeksami; hypertabele działają w lokalnym TimescaleDB. Publikację rozlicza [checklista S3](SPRINTY.md#publikacja-i-odbiór-s3); pełny opis funkcji: [SPRINT3.md](SPRINT3.md).

Sprawdzone wdrożenia kodu: API `dpl_FZ3j8aACrkDVQ6dJqVdHUXAS37RP`, frontend `dpl_ZhuuVuPRipQkPZcqjwQkTqnwzYrR`. Potwierdzenie migracji pochodzi z Runtime Logs backendu połączonego z Supabase PostgreSQL 17.12 i jego poprawnego startu z walidacją Hibernate; nie wykonywano dodatkowego SQL w panelu Supabase.

| Część | Działające wdrożenie |
|---|---|
| Frontend | [aplikacja-trenerska.vercel.app](https://aplikacja-trenerska.vercel.app), projekt `aplikacja-trenerska-frontend` |
| Backend | [aplikacja-trenerska-api.vercel.app](https://aplikacja-trenerska-api.vercel.app), Java 21 / Container Runtime, region `dub1` |
| Baza | Supabase PostgreSQL 17, `eu-west-1`, prywatny schemat `training_app`, migracje V1–V7 i katalog 48 ćwiczeń |

Konfiguracja poczty w ostatnim sprawdzonym wdrożeniu używa `APP_MAIL_MODE=log`. Ten transport zapisuje linki w prywatnych logach zamiast dostarczać wiadomości. Docelowa wysyłka do skrzynek wymaga ustawień SMTP opisanych poniżej. Interfejs kont jest wspólny dla wszystkich transportów poczty.

Z lokalnej bazy przeniesiono jedno istniejące aktywne konto `USER`, zachowując wszystkie osiem pól, w tym ID, skrót hasła i wersję sesji. Katalog ćwiczeń i historia migracji w Supabase pozostały bez zmian. Kopie danych są poza Git oraz w osobnym prywatnym schemacie kopii w Supabase; repozytorium nie zawiera danych konta ani zawartości tych kopii.

Zakres wykonanych sprawdzeń i pozostałe testy online zawiera [checklista wdrożenia](#5-sprawdź-wdrożenie). Poniższe sekcje opisują konfigurację i kolejne wdrożenia.

Konfiguracja zakłada dwa projekty Vercel z tego samego repozytorium: statyczny frontend React/Vite oraz backend Java 21 jako kontener. Supabase udostępnia PostgreSQL. Wiadomości wysyła backend przez SMTP; lokalnie odbiera je Mailpit. Aplikacja zachowuje własne konta i JWT; nie korzysta z Supabase Auth.

Gałęzią produkcyjną obu projektów Vercel jest `main`. Zmiany rozwijaj na osobnych branchach i scalaj do `main` przed wdrożeniem produkcyjnym. Samo przygotowanie plików lokalnie nie publikuje aplikacji.

Vercel Container Runtime jest obecnie w becie. Ustawienia hostingu opisuje [dokumentacja kontenerów](https://vercel.com/docs/functions/runtimes/container), a frontend Vite — [dokumentacja Vite na Vercel](https://vercel.com/docs/frameworks/frontend/vite).

## 1. Przygotuj Supabase

1. Utwórz nowy projekt i zapisz hasło bazy. Wybierz region zbliżony do regionu backendu.
2. W panelu **Connect** skopiuj osobno połączenia **Transaction pooler** i **Session pooler**. Host i nazwa użytkownika zależą od projektu — skopiuj je z panelu.
3. Wyłącz **Enable Data API** w ustawieniach integracji Data API. Nasz frontend komunikuje się z backendem, a nie z automatycznym REST/GraphQL Supabase. [Instrukcja Supabase](https://supabase.com/docs/guides/api/securing-your-api#disable-the-data-api).
4. Nie twórz ręcznie tabel ani nie wklejaj migracji w SQL Editor. Flyway utworzy prywatny schemat `training_app`, historię migracji, tabele i katalog 48 ćwiczeń. Nie dodawaj `training_app` do schematów udostępnianych przez Data API. W Table Editor wybierz ten schemat, aby zobaczyć tabele aplikacji.

Backend używa transaction poolera na porcie `6543`; migracje Flyway korzystają z session poolera na `5432`. Ustawienie `prepareThreshold=0` w JDBC wyłącza przygotowane instrukcje nieobsługiwane przez współdzielony transaction pooler. SSL musi pozostać włączony. [Połączenia i ograniczenia Supabase](https://supabase.com/docs/guides/database/connecting-to-postgres).

`backend/vercel.json` wybiera region `dub1` (Dublin / `eu-west-1`), zgodny z regionem bazy użytej w tym wdrożeniu. Utrzymywanie backendu blisko bazy ogranicza opóźnienia podczas migracji i startu Hibernate. Przy bazie w innym regionie dostosuj to ustawienie. [Regiony Vercela](https://vercel.com/docs/regions).

## 2. Zarezerwuj projekty i adresy Vercel

Utwórz dwa projekty połączone z repozytorium GitHub. Ich stabilne domeny produkcyjne będą potrzebne w zmiennych środowiskowych, przykładowo:

- frontend: `https://trening-app.vercel.app`;
- backend: `https://trening-api.vercel.app`.

Wartości poniżej są przykładami — użyj własnych adresów. Jeżeli najpierw utworzysz projekty bez kompletnych zmiennych i pierwszy build nie przejdzie, uzupełnij je i wykonaj **Redeploy**.

| Ustawienie projektu | Frontend | Backend |
|---|---|---|
| Root Directory | `frontend` | `backend` |
| Framework Preset | Vite | Container |
| Node.js Version | 22.x | nie dotyczy; JDK/JRE 21 w obrazie |
| Install Command | `npm ci` | obsługiwane przez Dockerfile |
| Build Command | `npm run build` | obsługiwane przez Dockerfile |
| Output Directory | `dist` | nie dotyczy |
| Plik kontenera | nie używać Dockerfile frontendu | Dockerfile backendu |
| Git / Production Branch | `main` | `main` |

W obu projektach wybierz `main` jako Production Branch. Sam push lub scalenie brancha nie zmienia automatycznie tego ustawienia w istniejących projektach.

Docelowy branch ustawisz w **Settings → Environments → Production → Branch Tracking**. Jeśli panel zgłasza „No deployments found”, najpierw uzupełnij poniższe zmienne Production i wykonaj pierwsze wdrożenie aktualnego lokalnego `main` przez CLI. Następnie ponów ustawienie Branch Tracking. Przed wdrożeniem sprawdź, czy wymagane zmiany są już scalone i wypchnięte do `main`; ponowne wdrożenie starszego commita nie pobiera nowego kodu. [Wybór brancha i ręczne wdrożenia Vercela](https://vercel.com/docs/git).

Przy wdrażaniu przez Vercel CLI uruchamiaj polecenia z katalogu głównego repozytorium i wybieraj istniejący projekt przez `--project`. Ustawienia projektu zachowują właściwy Root Directory (`backend` albo `frontend`). Repozytorium zawiera `.vercelignore`, który wyklucza lokalne pliki `.env*`, zależności, wyniki kompilacji i logi z wysyłanego źródła; same reguły `.gitignore` nie zastępują tej konfiguracji. Przed pierwszym wysłaniem sprawdź listę plików poleceniem `vercel deploy --project <NAZWA_PROJEKTU> --prod --dry --json`. Manifest powinien zawierać kod, migracje i konfigurację budowania, a w backendzie także `Dockerfile`, `.dockerignore`, `pom.xml`, `mvnw` oraz `.mvn/wrapper/maven-wrapper.properties`. Nie powinien zawierać rzeczywistych plików środowiska ani katalogów `target`, `dist` i `node_modules`. [Wykluczenia plików Vercel](https://vercel.com/docs/deployments/vercel-ignore), [podgląd manifestu przed wdrożeniem](https://vercel.com/changelog/dry-run-deployments-with-vercel-cli).

Po zalogowaniu przez `vercel login`, sprawdzeniu aktywnego lokalnego brancha oraz uzupełnieniu zmiennych obu projektów wykonaj z katalogu głównego repozytorium (podstaw własne nazwy istniejących projektów):

```powershell
git switch main
vercel deploy --project aplikacja-trenerska-api --prod --meta githubDeployment=1 --meta githubCommitRef=main
vercel deploy --project aplikacja-trenerska-frontend --prod --meta githubDeployment=1 --meta githubCommitRef=main
```

CLI wysyła bieżące pliki lokalne; parametr `githubCommitRef` opisuje branch i nie przełącza kodu. Opcja `--prod` używa zmiennych Production, również gdy branch nie jest jeszcze przypięty w panelu. Jeżeli konto ma kilka zespołów, wskaż właściwy przez `--scope <SLUG_ZESPOLU>`. [Wdrożenia produkcyjne i powiązanie z Git](https://vercel.com/docs/cli/deploy).

## 3. Ustaw backend w Vercel

W **Settings → Environment Variables** projektu backendowego dodaj zmienne dla środowiska **Production**. W panelu wpisuje się wartości bez zewnętrznych cudzysłowów. Wzór ze wszystkimi ustawieniami znajduje się w [backend/.env.production.example](../backend/.env.production.example); plik jest przykładem i nie jest automatycznie importowany do panelu.

| Zmienna | Wartość / znaczenie |
|---|---|
| `PORT` | `8080` |
| `SPRING_PROFILES_ACTIVE` | `prod` — bez `dev` i `docker` |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://<TRANSACTION_POOLER_HOST>:6543/postgres?sslmode=require&prepareThreshold=0&currentSchema=training_app` |
| `SPRING_DATASOURCE_USERNAME` | użytkownik z Transaction poolera, zwykle `postgres.<PROJECT_REF>` |
| `SPRING_DATASOURCE_PASSWORD` | hasło bazy |
| `SPRING_FLYWAY_URL` | `jdbc:postgresql://<SESSION_POOLER_HOST>:5432/postgres?sslmode=require&currentSchema=training_app` |
| `SPRING_FLYWAY_USER` | użytkownik z Session poolera |
| `SPRING_FLYWAY_PASSWORD` | hasło użytkownika migracji |
| `APP_JWT_SECRET` | własny losowy sekret, co najmniej 32 bajty; ten sam dla wszystkich instancji backendu |
| `APP_FRONTEND_URL` | dokładny adres frontendu HTTPS, np. `https://trening-app.vercel.app` |
| `APP_CORS_ALLOWED_ORIGINS` | opcjonalnie dodatkowe dokładne originy rozdzielone przecinkami; bez `*` i bez ścieżek |
| `APP_MAIL_MODE` | `smtp` (domyślnie); `log` to techniczny transport zapisujący linki w logach bez wysyłki |
| `APP_MAIL_FROM` | zweryfikowany u dostawcy adres nadawcy; wymagany w trybie `smtp` |
| `SPRING_MAIL_HOST` | host zewnętrznego SMTP |
| `SPRING_MAIL_PORT` | port SMTP, np. `587` dla STARTTLS |
| `SPRING_MAIL_USERNAME` | login SMTP |
| `SPRING_MAIL_PASSWORD` | hasło lub klucz SMTP |
| `SMTP_AUTH` | zwykle `true`, zgodnie z dostawcą |
| `SMTP_STARTTLS_ENABLE` | `true` dla serwera korzystającego z STARTTLS |
| `SMTP_SSL_ENABLE` | `false` przy STARTTLS na 587; `true` dla implicit TLS na 465 |

Profil `prod` ustawia domyślnie jedną konekcję Hikari na instancję, bez utrzymywania bezczynnych połączeń. Flyway i Hibernate korzystają ze schematu `training_app`; Swagger jest wyłączony. Brak wymaganych ustawień zatrzymuje uruchomienie zamiast użyć lokalnych danych. Nie łącz profilu `prod` z `dev` ani `docker`.

### Transport poczty

Lokalny Docker używa SMTP i Mailpit. Rejestracja, reset hasła i usunięcie konta działają przez wiadomość oraz jednorazowe potwierdzenie; typ skrzynki nie zmienia przepływu aplikacji.

Backend obsługuje również techniczny transport `APP_MAIL_MODE=log`. Wtedy nie łączy się z SMTP, a link zapisuje jako `[MAIL LINK]` w prywatnych logach. Link jest poufny i nie jest zwracany przez API. Nie jest to wysyłka e-mail. Aby dostarczać wiadomości użytkownikom, ustaw `APP_MAIL_MODE=smtp`, uzupełnij nadawcę i dane SMTP oraz wdroż backend ponownie. Frontend nie wymaga zmiennej wybierającej transport poczty.

### Wysyłka przez SMTP

Zmienne `APP_MAIL_FROM`, `SPRING_MAIL_*` i `SMTP_*` z tabeli dotyczą wariantu `APP_MAIL_MODE=smtp`.

Dla SMTP z szyfrowaniem od początku połączenia, zwykle na porcie `465`, ustaw zamiast STARTTLS `SMTP_SSL_ENABLE=true` i `SMTP_STARTTLS_ENABLE=false`, zgodnie z wymaganiami dostawcy. Przy standardowym wariancie portu `587` pozostaw STARTTLS i uwierzytelnienie włączone.

Nie kopiuj lokalnego `.env.example` do panelu bez zmian. Lokalna konfiguracja używa Mailpit i profilu `dev`, który tworzy znane konto `dev@example.test`. Profil `prod` nie tworzy tego konta. Wyłączenie `dev` nie usuwa konta utworzonego wcześniej, dlatego korzystaj z nowej bazy wdrożeniowej.

Nie dodawaj sekretów do GitHub ani do zmiennych z prefiksem `VITE_`. Dostęp do danych JDBC oraz wysyłka wiadomości są zadaniem backendu. Ustawienia SMTP w Supabase Auth nie konfigurują Spring Mail. `APP_FRONTEND_URL` jest automatycznie dozwolonym originem CORS; dodatkową listę ustawiaj tylko dla innych konkretnych domen aplikacji.

Po zapisaniu zmiennych wdroż backend i sprawdź:

```text
https://<BACKEND>.vercel.app/api/health
```

Oczekiwane: JSON z `status: "UP"`. Następnie sprawdź logi startu i migracji. Endpoint health potwierdza działanie procesu; rzeczywisty odczyt danych należy sprawdzić po zalogowaniu przez katalog ćwiczeń.

Używaj stabilnego adresu produkcyjnego backendu. Jeżeli Vercel Deployment Protection wymaga logowania do Vercel przed dostępem do API, trzeba odpowiednio skonfigurować dostęp do produkcyjnej domeny backendu; odpowiedź logowania Vercel nie jest odpowiedzią naszego API.

## 4. Ustaw frontend w Vercel

W projekcie frontendowym ustaw dla **Production**:

```dotenv
VITE_API_URL=https://<BACKEND>.vercel.app
```

Podaj wyłącznie origin backendu: bez końcowego `/api`, ścieżki, parametrów i fragmentu. To publiczny adres API, a nie sekret. Na Vercel wymagane jest HTTPS. Build sprawdza ustawienie i zgłasza błąd, jeżeli jest niepoprawne lub go brakuje.

Zmienne są zapisywane w paczce podczas budowania frontendu. Po zmianie `VITE_API_URL` wykonaj **Redeploy**. Nie wystarczy odświeżyć kartę przeglądarki. Sposób dostarczenia wiadomości konfiguruje wyłącznie backend.

`frontend/vercel.json` zapewnia fallback SPA dla bezpośrednich wejść na `/profil`, `/plany` i strony potwierdzeń. API jest wywoływane bezpośrednio pod domeną z `VITE_API_URL`, więc nie trzeba ręcznie wpisywać domeny backendu do reguł rewrite ani ustawiać kluczy Supabase we frontendzie. Dla tego wariantu CORS backendu musi zawierać origin frontendu.

Lokalne Vite i Docker zachowują dotychczasowe działanie: przy pustym `VITE_API_URL` korzystają ze względnego `/api` i lokalnego proxy.

## 5. Sprawdź wdrożenie

- [x] Oba projekty działają pod publicznymi adresami HTTPS i wdrażają `main`.
- [x] Backend działa z profilem `prod`; `/api/health` zwraca `UP`. V5–V7 wykonano 19.11, a kolejny start potwierdził aktualny schemat V7. Profil produkcyjny nie tworzy konta developerskiego.
- [x] Zapytanie logowania losowym nieistniejącym kontem zwraca `401` po odczycie bazy Supabase.
- [x] Bez JWT endpointy profilu, ćwiczeń i planów zwracają `401`. Po aktualizacji 19.11 sprawdzono również wyniki, pomiary, grupy, zaproszenia i przypisania (`401`).
- [x] CORS dopuszcza adres frontendu, a żądanie z obcego originu odrzuca kodem `403`.
- [x] Frontend zwraca `200` dla `/`, `/trening`, `/progres`, `/trener`, `/wspolpraca`, `/rejestracja`. Paczka zawiera moduły S3 i używa standardowych komunikatów kont.
- [x] W bazie jest 48 ćwiczeń oraz przeniesione konto `USER`; kopie danych pozostają prywatne.
- [ ] Na osobnym fikcyjnym koncie online: rejestracja zapisuje link w prywatnych logach, potwierdzenie aktywuje konto i można się zalogować.
- [ ] Po zalogowaniu online: katalog pokazuje ćwiczenia i można utworzyć, odczytać, edytować oraz usunąć własny plan.
- [ ] Online: reset hasła działa, a wcześniejsza sesja traci dostęp.
- [ ] Online: usunięcie konta wymaga hasła i potwierdzenia przez jednorazowy link.
- [ ] Podczas powyższych testów w przeglądarce nie występują błędy CORS ani próby połączenia z `localhost`.

Sprawdzenia oznaczone jako wykonane dotyczą wdrożenia z 16 listopada 2026. Pełne scenariusze rejestracji, resetu hasła, usunięcia konta i CRUD planów sprawdzono lokalnie; nie wykonywano ich na przeniesionym koncie online. Weryfikacja lokalna obejmowała 157 testów backendu, 21 testów frontendu oraz build i lint frontendu. Pozycje online pozostają do ręcznego odbioru na osobnym fikcyjnym koncie.

Weryfikację linków i usuwania wykonuj wyłącznie na własnym koncie testowym, po jednoznacznym przypisaniu linku do tego konta. Lokalne skrypty smoke są przeznaczone do lokalnego stacku i Mailpit; nie uruchamiaj ich przeciw produkcji.

## Zmiany bazy w zespole

1. Dodaj kolejny plik w `backend/src/main/resources/db/migration`, np. `V8__add_calendar.sql`. Uzgodnij numer z zespołem, aby dwa PR-y nie dodały różnych migracji V8. Zastosowanych migracji SQL i Java nie edytuj — poprawka dostaje nowy numer. V5–V7 są już zajęte przez Sprint 3. Flyway zapisuje wykonane wersje i ich sumy kontrolne. [Migracje wersjonowane](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html).
2. W migracji dodaj `CREATE TABLE`, `ALTER TABLE` lub indeksy oraz odpowiednie zmiany encji i kodu. Nie wpisuj na sztywno schematu `public`: produkcyjny schemat `training_app` ustawia konfiguracja. Hibernate ma `ddl-auto=validate`, więc sam nie tworzy tabel.
3. Sprawdź zmianę lokalnie w Dockerze i testach, również na bazie z istniejącymi danymi. Zmiana schematu i korzystający z niej kod powinny znaleźć się w tym samym PR.
4. Po scaleniu do `main` Vercel wdraża backend. Przy jego starcie Flyway wykonuje brakujące migracje w Supabase, przez połączenie sesyjne na porcie `5432`. Nie trzeba ręcznie tworzyć tabel w panelu. Po wdrożeniu sprawdź logi Flyway i `/api/health`; błąd migracji zatrzymuje start aplikacji.

Do zwykłej pracy nad kodem i migracjami członkowie zespołu nie potrzebują dostępu do Supabase ani hasła produkcyjnej bazy. Każdy korzysta z własnego lokalnego Dockera, a dane połączenia produkcyjnego są zapisane w Vercelu. Dostęp do panelu Supabase można nadać osobom zajmującym się podglądem danych, kopiami i administracją; służą do tego osobne zaproszenia i role, a nie wspólne konto. [Dostęp zespołu w Supabase](https://supabase.com/docs/guides/platform/access-control).

Migracje powinny być krótkie i zgodne z poprzednią wersją działającego backendu. Najpierw dodawaj nowe struktury, a usuwanie starych lub długie przeliczenia danych planuj osobno. Cofnięcie wersji kodu w Vercelu nie cofa automatycznie zmian w bazie; zmiany usuwające dane wymagają wcześniejszej kopii i osobnego planu.

## Preview i kolejne migracje

Preview powinno mieć osobną bazę lub projekt Supabase, osobny sekret JWT i adres frontendu w `APP_FRONTEND_URL`/CORS. Nie podłączaj przypadkowych preview z migracjami i operacjami usuwania do produkcyjnej bazy. Dopuszczaj konkretne domeny preview zamiast wszystkich `*.vercel.app`.

Mała pula połączeń jest ustawiona w profilu produkcyjnym, ponieważ Vercel może uruchamiać kilka instancji backendu. Flyway używa osobnego połączenia sesyjnego; nie kieruj jego migracji przez transaction pooler. Przy większym ruchu warto wydzielić migracje do pojedynczego kroku wdrożeniowego, a w runtime zostawić walidację schematu.

Zapytania operatora, np. zmiana roli trenera, muszą używać właściwego schematu. Przy pracy w SQL Editor ustaw `SET search_path TO training_app, public;`, a przy uruchamianiu skryptu `tools/set-user-role.sql` ustaw taki sam `search_path` dla połączenia operatora.

Migracje V5–V7 dodają progres i grupy trenera. V5/V6 są zgodne ze zwykłym PostgreSQL. Migracja Java V7 sprawdza dostępność TimescaleDB i tworzy prawdziwe hypertabele `exercise_results` oraz `body_metrics` tam, gdzie rozszerzenie jest dostępne (lokalny Compose). Na Supabase PostgreSQL 17 pozostawia zwykłe tabele z indeksami. API i funkcje są takie same w obu wariantach; nie trzeba zmieniać panelu Supabase. Jest to wariant uzgodniony 19.11.2026. [Ograniczenie Supabase](https://supabase.com/docs/guides/database/extensions/timescaledb).

V7 jest migracją wersjonowaną: przeniesienie później istniejącej bazy bez rozszerzenia do hostingu TimescaleDB wymaga nowej migracji konwertującej tabele. Ponowny start nie uruchomi wykonanej V7 drugi raz.

## Weryfikacja lokalna konfiguracji

```powershell
cd backend
.\mvnw.cmd test
cd ../frontend
npm ci
npm run build
npm run lint
npm test
```

Testy nie wymagają produkcyjnego Supabase ani SMTP. Dostęp do kont Vercel/Supabase i pierwsze zdalne wdrożenie wykonuje się osobno po uzupełnieniu paneli.

Test profilu produkcyjnego z katalogu głównego repozytorium (PowerShell 7 i uruchomiony Docker):

```powershell
pwsh -NoProfile -File tests/smoke-deployment.ps1
```

Skrypt buduje backend i tworzy własne tymczasowe kontenery PostgreSQL oraz Mailpit. Sprawdza port wskazany przez hosting, schemat `training_app`, migracje V1–V7, katalog, CORS, brak konta developerskiego, wyłączenie Swaggera i przepływ konta/planu przez SMTP. Po zakończeniu usuwa własne kontenery, sieć i obraz. Nie korzysta z istniejącej bazy Compose ani ze zdalnych kont. Połączenia JDBC/SMTP w tym izolowanym teście są celowo lokalne i bez TLS; test nie potwierdza połączenia TLS z rzeczywistym Supabase lub dostawcą SMTP. Nową wersję sprawdzono tym skryptem 19.11.2026; testy progresu i trenera opisuje [SPRINT3.md](SPRINT3.md).
