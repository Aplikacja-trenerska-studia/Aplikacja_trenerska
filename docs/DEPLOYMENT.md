# Wdrożenie: Vercel i Supabase

Konfiguracja zakłada dwa projekty Vercel z tego samego repozytorium: statyczny frontend React/Vite oraz backend Java 21 jako kontener. Supabase udostępnia PostgreSQL. Wiadomości można wysyłać przez zewnętrzny SMTP albo zastąpić je linkami w prywatnych logach na potrzeby demonstracji studenckiej. Aplikacja zachowuje własne konta i JWT; nie korzysta z Supabase Auth.

Pliki przygotowano na branchu `feat/deployment`. Samo przygotowanie ich lokalnie nie publikuje brancha ani aplikacji. Przed importem tej wersji do Vercel branch musi być wypchnięty do GitHub albo zmiany muszą zostać scalone do wybranego brancha wdrożeniowego.

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
| Git / Production Branch | branch zawierający konfigurację | ten sam branch |

Do pierwszego wdrożenia tej gałęzi wybierz `feat/deployment` po jej wypchnięciu; po scaleniu można ustawić `main`. Sam push brancha nie zmienia automatycznie ustawienia Production Branch w istniejących projektach.

Jeśli import początkowo użył `main`, docelowy branch ustawisz w **Settings → Environments → Production → Branch Tracking**. Jeśli panel zgłasza „No deployments found”, najpierw uzupełnij poniższe zmienne Production i wykonaj pierwsze wdrożenie z lokalnego brancha `feat/deployment` przez CLI. Następnie ponów ustawienie Branch Tracking. Ponowne wdrożenie starego commita z `main` nie pobierze konfiguracji z innego brancha. [Wybór brancha i ręczne wdrożenia Vercela](https://vercel.com/docs/git).

Przy wdrażaniu przez Vercel CLI uruchamiaj polecenia z katalogu głównego repozytorium i wybieraj istniejący projekt przez `--project`. Ustawienia projektu zachowują właściwy Root Directory (`backend` albo `frontend`). Repozytorium zawiera `.vercelignore`, który wyklucza lokalne pliki `.env*`, zależności, wyniki kompilacji i logi z wysyłanego źródła; same reguły `.gitignore` nie zastępują tej konfiguracji. Przed pierwszym wysłaniem sprawdź listę plików poleceniem `vercel deploy --project <NAZWA_PROJEKTU> --prod --dry --json`. Manifest powinien zawierać kod, migracje i konfigurację budowania, a w backendzie także `Dockerfile`, `.dockerignore`, `pom.xml`, `mvnw` oraz `.mvn/wrapper/maven-wrapper.properties`. Nie powinien zawierać rzeczywistych plików środowiska ani katalogów `target`, `dist` i `node_modules`. [Wykluczenia plików Vercel](https://vercel.com/docs/deployments/vercel-ignore), [podgląd manifestu przed wdrożeniem](https://vercel.com/changelog/dry-run-deployments-with-vercel-cli).

Po zalogowaniu przez `vercel login`, sprawdzeniu aktywnego lokalnego brancha oraz uzupełnieniu zmiennych obu projektów wykonaj z katalogu głównego repozytorium (podstaw własne nazwy istniejących projektów):

```powershell
vercel deploy --project aplikacja-trenerska-api --prod --meta githubDeployment=1 --meta githubCommitRef=feat/deployment
vercel deploy --project aplikacja-trenerska --prod --meta githubDeployment=1 --meta githubCommitRef=feat/deployment
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

- [ ] Backend uruchamia się z profilem `prod`, migracje V1–V4 przeszły i nie utworzono konta developerskiego.
- [ ] Rejestracja dostarcza wiadomość do rzeczywistej skrzynki (`smtp`) albo zapisuje link w prywatnych logach backendu (`log`); link prowadzi do domeny frontendu.
- [ ] Potwierdzenie konta i logowanie działają; katalog pokazuje 48 ćwiczeń.
- [ ] Można utworzyć, odczytać, edytować i usunąć własny plan.
- [ ] Bezpośrednie otwarcie i odświeżenie `/profil` oraz `/plany` nie zwracają 404 hostingu.
- [ ] Reset hasła działa, a wcześniejsza sesja traci dostęp.
- [ ] Usunięcie konta wymaga hasła i potwierdzenia przez jednorazowy link z wiadomości lub prywatnych logów demonstracji.
- [ ] W konsoli przeglądarki nie ma błędów CORS ani prób połączenia z `localhost`.

Weryfikację wiadomości i operacji usunięcia wykonuj na własnym koncie przeznaczonym do testów. Lokalne skrypty smoke są przeznaczone do lokalnego stacku i Mailpit; nie uruchamiaj ich przeciw produkcji.

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
