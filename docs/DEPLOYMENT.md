# Wdrożenie: Vercel i Supabase

## Stan wdrożenia — 16 listopada 2026

Wdrożenie działa publicznie. Zadanie **S3-11** należy do **Sprintu 3 (23–29 listopada)** i zostało wykonane z wyprzedzeniem **16 listopada 2026**. Oba projekty Vercel śledzą produkcyjny branch `main`; sprawdzono wersję po scaleniu `ee2fa1e`.

| Część | Działające wdrożenie |
|---|---|
| Frontend | [aplikacja-trenerska.vercel.app](https://aplikacja-trenerska.vercel.app), projekt `aplikacja-trenerska-frontend` |
| Backend | [aplikacja-trenerska-api.vercel.app](https://aplikacja-trenerska-api.vercel.app), Java 21 / Container Runtime, region `dub1` |
| Baza | Supabase PostgreSQL 17, `eu-west-1`, prywatny schemat `training_app`, migracje V1–V4 i 48 ćwiczeń |

Poczta działa w trybie demonstracyjnym: backend ma `APP_MAIL_MODE=log`, a frontend `VITE_MAIL_MODE=log`. **Wiadomości nie trafiają do skrzynek e-mail.** Linki aktywacji, resetu i usunięcia konta odczytuje administrator z prywatnych logów backendu.

Z lokalnej bazy przeniesiono jedno istniejące aktywne konto `USER`, zachowując wszystkie osiem pól, w tym ID, skrót hasła i wersję sesji. Katalog ćwiczeń i historia migracji w Supabase pozostały bez zmian. Kopie danych są poza Git oraz w osobnym prywatnym schemacie kopii w Supabase; repozytorium nie zawiera danych konta ani zawartości tych kopii.

Zakres wykonanych sprawdzeń i pozostałe testy online zawiera [checklista wdrożenia](#5-sprawdź-wdrożenie). Poniższe sekcje opisują konfigurację i kolejne wdrożenia.

Konfiguracja zakłada dwa projekty Vercel z tego samego repozytorium: statyczny frontend React/Vite oraz backend Java 21 jako kontener. Supabase udostępnia PostgreSQL. Wiadomości można wysyłać przez zewnętrzny SMTP albo zastąpić je linkami w prywatnych logach na potrzeby demonstracji studenckiej. Aplikacja zachowuje własne konta i JWT; nie korzysta z Supabase Auth.

Gałęzią produkcyjną obu projektów Vercel jest `main`. Zmiany rozwijaj na osobnych branchach i scalaj do `main` przed wdrożeniem produkcyjnym. Samo przygotowanie plików lokalnie nie publikuje aplikacji.

Vercel Container Runtime jest obecnie w becie. Ustawienia hostingu opisuje [dokumentacja kontenerów](https://vercel.com/docs/functions/runtimes/container), a frontend Vite — [dokumentacja Vite na Vercel](https://vercel.com/docs/frameworks/frontend/vite).

## 1. Przygotuj Supabase

1. Utwórz nowy projekt i zapisz hasło bazy. Wybierz region zbliżony do regionu backendu.
2. W panelu **Connect** skopiuj osobno połączenia **Transaction pooler** i **Session pooler**. Host i nazwa użytkownika zależą od projektu — skopiuj je z panelu.
3. Wyłącz **Enable Data API** w ustawieniach integracji Data API. Nasz frontend komunikuje się z backendem, a nie z automatycznym REST/GraphQL Supabase. [Instrukcja Supabase](https://supabase.com/docs/guides/api/securing-your-api#disable-the-data-api).
4. Nie twórz ręcznie tabel ani nie wklejaj migracji V1–V4 w SQL Editor. Flyway utworzy prywatny schemat `training_app`, historię migracji, tabele i katalog 48 ćwiczeń. Nie dodawaj `training_app` do schematów udostępnianych przez Data API. W Table Editor wybierz ten schemat, aby zobaczyć tabele aplikacji.

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
| `APP_MAIL_MODE` | `smtp` (domyślnie) albo `log` dla demonstracji bez wysyłki wiadomości |
| `APP_MAIL_FROM` | zweryfikowany u dostawcy adres nadawcy; wymagany w trybie `smtp` |
| `SPRING_MAIL_HOST` | host zewnętrznego SMTP |
| `SPRING_MAIL_PORT` | port SMTP, np. `587` dla STARTTLS |
| `SPRING_MAIL_USERNAME` | login SMTP |
| `SPRING_MAIL_PASSWORD` | hasło lub klucz SMTP |
| `SMTP_AUTH` | zwykle `true`, zgodnie z dostawcą |
| `SMTP_STARTTLS_ENABLE` | `true` dla serwera korzystającego z STARTTLS |
| `SMTP_SSL_ENABLE` | `false` przy STARTTLS na 587; `true` dla implicit TLS na 465 |

Profil `prod` ustawia domyślnie jedną konekcję Hikari na instancję, bez utrzymywania bezczynnych połączeń. Flyway i Hibernate korzystają ze schematu `training_app`; Swagger jest wyłączony. Brak wymaganych ustawień zatrzymuje uruchomienie zamiast użyć lokalnych danych. Nie łącz profilu `prod` z `dev` ani `docker`.

### Demonstracja bez dostawcy poczty

Na potrzeby projektu studenckiego ustaw w backendzie `APP_MAIL_MODE=log`, a we frontendzie `VITE_MAIL_MODE=log`. W tym wariancie nie trzeba zakładać skrzynki, kupować domeny ani podawać `APP_MAIL_FROM`, zmiennych `SPRING_MAIL_*` i `SMTP_*`. Pozostałe ustawienia bazy, JWT, HTTPS i `APP_FRONTEND_URL` są nadal wymagane. Pozostaw `SPRING_PROFILES_ACTIVE=prod`.

1. Po wdrożeniu zarejestruj fikcyjne konto, np. `student@example.test`.
2. W projekcie **backendu** w Vercel otwórz **Logs** i znajdź wpis `[DEMO MAIL]` dla operacji `EMAIL_CONFIRMATION`.
3. Otwórz pełny adres z tego wpisu w przeglądarce i potwierdź konto na stronie frontendu. Następnie możesz się zalogować.
4. Reset hasła i usunięcie konta działają tak samo: wyślij prośbę w aplikacji i otwórz nowy link z wpisu odpowiednio `PASSWORD_RESET` lub `ACCOUNT_DELETION`. Samo otwarcie strony usunięcia nie usuwa konta — operację trzeba potwierdzić.

W planie Hobby logi są przechowywane przez godzinę. Najwygodniej otworzyć **Logs** i włączyć **Live** przed demonstracją. Jeśli wpis już zniknął, zamów nowy link w aplikacji (kolejna prośba jest dostępna po minucie). [Logi i limity Vercela](https://vercel.com/docs/logs/runtime).

Tryb `log` zachowuje jednorazowość i ważność tokenów oraz sprawdzanie hasła przy usuwaniu konta. Nie udostępnia linków przez publiczne API. Frontend informuje, że link przekazuje administrator demonstracji. **Pełne linki dają uprawnienie do aktywacji, resetu hasła lub potwierdzenia usunięcia konta: nie publikuj logów ani ich zrzutów. Używaj wyłącznie fikcyjnych kont i danych.**

Przy przejściu na rzeczywistą wysyłkę ustaw `APP_MAIL_MODE=smtp`, uzupełnij `APP_MAIL_FROM` oraz wszystkie wymagane dane SMTP, a we frontendzie ustaw `VITE_MAIL_MODE=smtp` lub usuń tę zmienną. Wykonaj ponowne wdrożenie obu projektów. Tryb `smtp` jest domyślny i nie zapisuje linków do logów.

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
# Tylko dla demonstracji z backendowym APP_MAIL_MODE=log:
VITE_MAIL_MODE=log
```

Podaj wyłącznie origin backendu: bez końcowego `/api`, ścieżki, parametrów i fragmentu. To publiczny adres API, a nie sekret. Na Vercel wymagane jest HTTPS. Build sprawdza ustawienie i zgłasza błąd, jeżeli jest niepoprawne lub go brakuje.

Zmienne są zapisywane w paczce podczas budowania frontendu. Po zmianie `VITE_API_URL` lub `VITE_MAIL_MODE` wykonaj **Redeploy**. Nie wystarczy odświeżyć kartę przeglądarki. `VITE_MAIL_MODE` steruje jedynie komunikatami interfejsu; sposób dostarczenia linków ustawia backendowe `APP_MAIL_MODE`.

`frontend/vercel.json` zapewnia fallback SPA dla bezpośrednich wejść na `/profil`, `/plany` i strony potwierdzeń. API jest wywoływane bezpośrednio pod domeną z `VITE_API_URL`, więc nie trzeba ręcznie wpisywać domeny backendu do reguł rewrite ani ustawiać kluczy Supabase we frontendzie. Dla tego wariantu CORS backendu musi zawierać origin frontendu.

Lokalne Vite i Docker zachowują dotychczasowe działanie: przy pustym `VITE_API_URL` korzystają ze względnego `/api` i lokalnego proxy.

## 5. Sprawdź wdrożenie

- [x] Oba projekty działają pod publicznymi adresami HTTPS i wdrażają `main`.
- [x] Backend działa z profilem `prod`; `/api/health` zwraca `UP`, migracje V1–V4 przeszły i nie utworzono konta developerskiego.
- [x] Zapytanie logowania losowym nieistniejącym kontem zwraca `401` po odczycie bazy Supabase.
- [x] Bez JWT endpointy profilu, ćwiczeń i planów zwracają `401`.
- [x] CORS dopuszcza adres frontendu, a żądanie z obcego originu odrzuca kodem `403`.
- [x] Frontend i sześć sprawdzonych tras SPA zwracają `200`; publiczna paczka wskazuje właściwe API i tryb demonstracyjnej poczty.
- [x] W bazie jest 48 ćwiczeń oraz przeniesione konto `USER`; kopie danych pozostają prywatne.
- [ ] Na osobnym fikcyjnym koncie online: rejestracja zapisuje link w prywatnych logach, potwierdzenie aktywuje konto i można się zalogować.
- [ ] Po zalogowaniu online: katalog pokazuje ćwiczenia i można utworzyć, odczytać, edytować oraz usunąć własny plan.
- [ ] Online: reset hasła działa, a wcześniejsza sesja traci dostęp.
- [ ] Online: usunięcie konta wymaga hasła i potwierdzenia przez jednorazowy link.
- [ ] Podczas powyższych testów w przeglądarce nie występują błędy CORS ani próby połączenia z `localhost`.

Sprawdzenia oznaczone jako wykonane dotyczą wdrożenia z 16 listopada 2026. Pełne scenariusze rejestracji, resetu hasła, usunięcia konta i CRUD planów sprawdzono lokalnie; nie wykonywano ich na przeniesionym koncie online. Weryfikacja lokalna obejmowała 157 testów backendu, 21 testów frontendu oraz build i lint frontendu. Pozycje online pozostają do ręcznego odbioru na osobnym fikcyjnym koncie.

Weryfikację linków i usuwania wykonuj wyłącznie na własnym koncie testowym, po jednoznacznym przypisaniu linku do tego konta. Lokalne skrypty smoke są przeznaczone do lokalnego stacku i Mailpit; nie uruchamiaj ich przeciw produkcji.

## Zmiany bazy w zespole

1. Dodaj kolejny plik w `backend/src/main/resources/db/migration`, np. `V5__add_training_results.sql`. Uzgodnij numer z zespołem, aby dwa PR-y nie dodały różnych migracji V5. Wykonanych V1–V4 nie edytuj — poprawka dostaje nowy numer. Flyway zapisuje wykonane wersje i ich sumy kontrolne. [Migracje wersjonowane](https://documentation.red-gate.com/fd/versioned-migrations-273973333.html).
2. W migracji dodaj `CREATE TABLE`, `ALTER TABLE` lub indeksy oraz odpowiednie zmiany encji i kodu. Nie wpisuj na sztywno schematu `public`: produkcyjny schemat `training_app` ustawia konfiguracja. Hibernate ma `ddl-auto=validate`, więc sam nie tworzy tabel.
3. Sprawdź zmianę lokalnie w Dockerze i testach, również na bazie z istniejącymi danymi. Zmiana schematu i korzystający z niej kod powinny znaleźć się w tym samym PR.
4. Po scaleniu do `main` Vercel wdraża backend. Przy jego starcie Flyway wykonuje brakujące migracje w Supabase, przez połączenie sesyjne na porcie `5432`. Nie trzeba ręcznie tworzyć tabel w panelu. Po wdrożeniu sprawdź logi Flyway i `/api/health`; błąd migracji zatrzymuje start aplikacji.

Do zwykłej pracy nad kodem i migracjami członkowie zespołu nie potrzebują dostępu do Supabase ani hasła produkcyjnej bazy. Każdy korzysta z własnego lokalnego Dockera, a dane połączenia produkcyjnego są zapisane w Vercelu. Dostęp do panelu Supabase można nadać osobom zajmującym się podglądem danych, kopiami i administracją; służą do tego osobne zaproszenia i role, a nie wspólne konto. [Dostęp zespołu w Supabase](https://supabase.com/docs/guides/platform/access-control).

Migracje powinny być krótkie i zgodne z poprzednią wersją działającego backendu. Najpierw dodawaj nowe struktury, a usuwanie starych lub długie przeliczenia danych planuj osobno. Cofnięcie wersji kodu w Vercelu nie cofa automatycznie zmian w bazie; zmiany usuwające dane wymagają wcześniejszej kopii i osobnego planu.

## Preview i kolejne migracje

Preview powinno mieć osobną bazę lub projekt Supabase, osobny sekret JWT i adres frontendu w `APP_FRONTEND_URL`/CORS. Nie podłączaj przypadkowych preview z migracjami i operacjami usuwania do produkcyjnej bazy. Dopuszczaj konkretne domeny preview zamiast wszystkich `*.vercel.app`.

Mała pula połączeń jest ustawiona w profilu produkcyjnym, ponieważ Vercel może uruchamiać kilka instancji backendu. Flyway używa osobnego połączenia sesyjnego; nie kieruj jego migracji przez transaction pooler. Przy większym ruchu warto wydzielić migracje do pojedynczego kroku wdrożeniowego, a w runtime zostawić walidację schematu.

Zapytania operatora, np. zmiana roli trenera, muszą używać właściwego schematu. Przy pracy w SQL Editor ustaw `SET search_path TO training_app, public;`, a przy uruchamianiu skryptu `tools/set-user-role.sql` ustaw taki sam `search_path` dla połączenia operatora.

Aktualne migracje wymagają zwykłego PostgreSQL. Planowany moduł progresu z TimescaleDB wymaga osobnej decyzji: rozszerzenie jest wycofane dla projektów Supabase na PostgreSQL 17. [Informacja Supabase o TimescaleDB](https://supabase.com/docs/guides/database/extensions/timescaledb).

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

Skrypt buduje backend i tworzy własne tymczasowe kontenery PostgreSQL oraz Mailpit. Sprawdza port wskazany przez hosting, schemat `training_app`, migracje V1–V4, katalog, CORS, brak konta developerskiego, wyłączenie Swaggera i przepływ konta/planu przez SMTP. Po zakończeniu usuwa własne kontenery, sieć i obraz. Nie korzysta z istniejącej bazy Compose ani ze zdalnych kont. Połączenia JDBC/SMTP w tym izolowanym teście są celowo lokalne i bez TLS; test nie potwierdza połączenia TLS z rzeczywistym Supabase lub dostawcą SMTP.
