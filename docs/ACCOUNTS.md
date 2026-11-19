# Konta użytkowników

Moduł `auth` obsługuje rejestrację, potwierdzenie e-maila, logowanie, odzyskanie hasła i potwierdzenie usunięcia konta. Moduł `user` udostępnia dane zalogowanego użytkownika i przyjmuje jego prośbę o usunięcie konta.

## Dostarczanie linków

Domyślny transport `APP_MAIL_MODE=smtp` wysyła wiadomości przez skonfigurowany serwer SMTP. Lokalnie wiadomości przechwytuje Mailpit; dostarczanie na rzeczywiste adresy wymaga zewnętrznego serwera SMTP i poprawnych danych nadawcy.

Opcjonalny transport `APP_MAIL_MODE=log` nie wysyła wiadomości: pełne linki zapisuje pod znacznikiem `[MAIL LINK]` w prywatnych logach backendu. Nie wymaga danych SMTP ani `APP_MAIL_FROM`. Służy do technicznego sprawdzania przepływów bez dostawcy poczty; jego wybór nie zmienia interfejsu użytkownika. Linków nie zwraca publiczne API. Do testów używaj fikcyjnego konta i nie publikuj logów ani linków.

Ostatni potwierdzony stan wersji [online](https://aplikacja-trenerska.vercel.app), z 16 listopada 2026, używał `APP_MAIL_MODE=log`. Nie oznacza to dostarczenia wiadomości do skrzynki użytkownika. Przed uruchomieniem rzeczywistej wysyłki należy skonfigurować SMTP zgodnie z [instrukcją wdrożenia](DEPLOYMENT.md).

## Przepływy

### Lokalne konto deweloperskie

Profil `dev` tworzy przy starcie aktywne konto `dev@example.test` z hasłem `devdevdev`, nazwą `Dev` i rolą `USER`. Hasło jest zapisywane przez istniejący encoder BCrypt. Lokalny Compose domyślnie włącza `docker,dev`, a samo konto nie wymaga Mailpit ani aktywacji. Pozostałe rejestracje nadal wymagają potwierdzenia adresu.

Jeżeli konto o tym adresie już istnieje (niezależnie od wielkości liter), inicjalizator niczego nie zmienia: nie resetuje hasła, nie aktywuje konta i nie zmienia jego roli ani wersji sesji. Usunięte konto zostanie odtworzone przy kolejnym starcie z profilem `dev`. Profil bez `dev` nie uruchamia inicjalizatora i nie usuwa wcześniej zapisanych danych. Dla uruchomienia bez konta domyślnego ustaw `SPRING_PROFILES_ACTIVE=docker`; dla Maven lokalnie profil można włączyć przez `-Dspring-boot.run.profiles=dev`.

### Rejestracja i potwierdzenie e-maila

1. Użytkownik rejestruje konto na `/rejestracja`.
2. Backend tworzy nieaktywne konto i wysyła wiadomość z linkiem `/confirm-email?token=…`. Rejestracja zwraca komunikat; nie loguje użytkownika.
3. Otwarcie linku wyświetla stronę potwierdzenia. Użytkownik naciska przycisk, który wysyła `POST /api/auth/confirm`.
4. Po aktywacji użytkownik loguje się na `/logowanie`.

Gdy wiadomość nie dotarła lub link wygasł, można ponowić wysyłkę przez `POST /api/auth/resend-confirmation`. Odpowiedź nie ujawnia, czy podany adres jest przypisany do nieaktywnego konta.

Konta utworzone przed dodaniem potwierdzenia pozostają aktywne. Potwierdzenie dotyczy nowych rejestracji.

### Reset hasła

1. Na stronie logowania użytkownik wybiera odzyskanie hasła i podaje e-mail.
2. `POST /api/auth/forgot-password` zwraca taki sam komunikat dla istniejącego i nieistniejącego konta. Wiadomość jest wysyłana tylko dla uprawnionego konta. Również w razie awarii SMTP odpowiedź pozostaje `200`, aby nie ujawniać istnienia konta; jeśli wiadomość nie dociera, należy ponowić prośbę po chwili. Tak samo zachowuje się ponowienie aktywacji konta.
3. Link `/reset-password?token=…` prowadzi do formularza nowego hasła z powtórzeniem hasła.
4. Po udanym `POST /api/auth/reset-password` użytkownik loguje się nowym hasłem. Dotychczasowe sesje JWT tracą ważność.

### Usunięcie konta

1. Zalogowany użytkownik otwiera profil i podaje obecne hasło w sekcji usuwania konta.
2. `POST /api/users/me/deletion-request` weryfikuje hasło i wysyła wiadomość na adres przypisany do konta. Samo żądanie nie usuwa konta.
3. Link `/confirm-deletion?token=…` wyświetla opis skutków operacji i wymaga naciśnięcia przycisku potwierdzenia.
4. `POST /api/auth/confirm-deletion` usuwa konto, tokeny, własne plany z ich dniami i ćwiczeniami, wyniki treningów oraz pomiary ciała. Usuwa także grupy prowadzone przez tę osobę, jej członkostwa w grupach i powiązane przypisania planów. Istniejące sesje przestają działać. Katalog ćwiczeń i cudze plany pozostają; autorstwo ćwiczeń usuwanego konta jest czyszczone.

Przypisanie przez trenera tworzy osobną kopię planu należącą do podopiecznego. Usunięcie konta trenera lub grupy nie usuwa kopii zapisanej już na cudzym koncie. Usunięcie konta podopiecznego usuwa jego kopie, ale zachowuje źródłowe plany trenera i dane pozostałych podopiecznych.

Potwierdzenie z e-maila jest dostępne także w innej przeglądarce, bez aktywnej sesji. Otwarcie linku metodą GET nie zmienia danych; podgląd wiadomości przez klienta pocztowego nie może sam usunąć konta.

## API

Wszystkie żądania z treścią używają `Content-Type: application/json`. Chronione endpointy wymagają nagłówka `Authorization: Bearer <JWT>`.

| Metoda | Endpoint | Treść żądania | Dostęp / odpowiedź |
|---|---|---|---|
| POST | `/api/auth/register` | `{ "email", "password", "displayName" }` | Publiczny, `201`, `{ "message" }` |
| POST | `/api/auth/login` | `{ "email", "password" }` | Publiczny, JWT i dane użytkownika |
| POST | `/api/auth/confirm` | `{ "token" }` | Publiczny, `{ "message" }` |
| POST | `/api/auth/resend-confirmation` | `{ "email" }` | Publiczny, `{ "message" }` |
| POST | `/api/auth/forgot-password` | `{ "email" }` | Publiczny, `{ "message" }` |
| POST | `/api/auth/reset-password` | `{ "token", "password" }` | Publiczny, `{ "message" }` |
| GET | `/api/users/me` | — | Zalogowany użytkownik, `{ "id", "email", "displayName", "role" }` |
| POST | `/api/users/me/deletion-request` | `{ "password" }` | Zalogowany użytkownik, `{ "message" }` |
| POST | `/api/auth/confirm-deletion` | `{ "token" }` | Publiczny, `{ "message" }` |

Zapisy w nawiasach klamrowych w tabeli oznaczają nazwy pól, a nie gotowy JSON do wysłania. Błędy API zawierają `message`, a błędy walidacji także `errors` z komunikatami przypisanymi do pól.

## Role USER / TRAINER

Rejestracja zawsze tworzy `USER`; pole `role` przesłane przez klienta nie nadaje uprawnień. Obie role mogą korzystać z profilu i usuwania własnego konta. Ścieżki `/api/trainer`, `/api/trainer/**`, `/api/groups` i `/api/groups/**` są zarezerwowane dla `TRAINER`; kontroler grup dodatkowo stosuje `@PreAuthorize("hasRole('TRAINER')")`. Trener zarządza wyłącznie własnymi grupami, a dostęp do progresu podopiecznego wymaga zaakceptowanego członkostwa. Odrzucenie zaproszenia, opuszczenie grupy lub usunięcie członkostwa odbiera ten dostęp. Brak prawidłowej sesji daje `401`, brak wymaganej roli — `403`.

Rolę nadaje lub odbiera operator mający dostęp do bazy, po sprawdzeniu tożsamości i uprawnienia danej osoby do pełnienia funkcji trenera. Nie ma publicznego endpointu zmiany roli ani możliwości samodzielnego awansu z profilu. Z katalogu głównego repozytorium, przy uruchomionej bazie Compose:

```powershell
# Nadaj TRAINER istniejącemu, aktywowanemu kontu.
Get-Content -Raw tools/set-user-role.sql | docker compose exec -T db psql -X -U aplikacja -d aplikacja_trenerska -v ON_ERROR_STOP=1 -v 'email=trener@example.com' -v role=TRAINER -f -
# Sprawdź $LASTEXITCODE: 0 oznacza sukces; inny kod oznacza błąd i rollback.

# Odbierz rolę trenera.
Get-Content -Raw tools/set-user-role.sql | docker compose exec -T db psql -X -U aplikacja -d aplikacja_trenerska -v ON_ERROR_STOP=1 -v 'email=trener@example.com' -v role=USER -f -
```

Skrypt blokuje rekord na czas transakcji, dopuszcza wyłącznie przejścia `USER` ↔ `TRAINER` i odmawia zmiany nieistniejących lub nieaktywnych kont oraz kont `ADMIN`. Każda faktyczna zmiana zwiększa `token_version`: wcześniejsze JWT przestają działać i użytkownik musi ponownie się zalogować. Powtórzenie tej samej roli niczego nie zmienia. Wynik pokazuje ID, e-mail, rolę i wersję sesji do sprawdzenia przez operatora. Parametry są cytowane przez psql; skrypt należy uruchamiać z `-X`, `ON_ERROR_STOP=1` i `-f -`, jak w przykładzie.

Rola w JWT służy jako informacja dla klienta. Backend ustala uprawnienia na podstawie aktualnej roli z bazy przy każdym żądaniu. Istniejący `ADMIN` nie dziedziczy automatycznie uprawnień trenera; panel administracyjny pozostaje poza S2.

Testy ról: `RoleSecurityIntegrationTests` sprawdzają prawdziwe JWT, reguły URL i `@PreAuthorize` na kontrolerze istniejącym wyłącznie w testach. Skrypt operatora można sprawdzić poleceniem `pwsh -NoProfile -File tests/smoke-roles.ps1`. Test wymaga Dockera, uruchamia jednorazową bazę PostgreSQL 16 bez publikowania portów i usuwa kontener po zakończeniu; nie korzysta z bazy aplikacji.

## Zabezpieczenia

- Tokeny z wiadomości mają określone przeznaczenie i termin ważności. Nie można użyć tokenu aktywacji do zmiany hasła lub usunięcia konta.
- Domyślnie aktywacja jest ważna 24 godziny, reset hasła 30 minut, a potwierdzenie usunięcia 30 minut.
- Token można zrealizować tylko raz. Wiadomości tego samego rodzaju do danego konta można wysyłać co 60 sekund; żądania w tym czasie nie wysyłają kolejnej wiadomości. Po tym czasie nowy token zastępuje poprzedni token tego samego rodzaju.
- Baza przechowuje skróty tokenów. W trybie `smtp` surowe tokeny trafiają wyłącznie do wiadomości i nie są logowane. Jawnie włączony transport `log` zapisuje pełne linki w prywatnych logach backendu; dostęp do tych logów pozwala wykonać operacje potwierdzane linkiem.
- Reset hasła unieważnia wcześniejsze JWT. Weryfikacja sesji uwzględnia także aktywność i tożsamość konta, więc usunięcie i ponowna rejestracja na ten sam adres nie przywraca starej sesji.
- Migracja V2 dodaje wersję sesji i usuwa wcześniejsze tokeny weryfikacyjne zapisane bez skrótu. JWT wydane przed tą zmianą nie mają wersji sesji i wymagają ponownego logowania.
- Frontend weryfikuje sesję przez API przed pokazaniem chronionej strony i obsługuje wygaśnięcie sesji. Autoryzacja backendu obowiązuje niezależnie od zabezpieczenia tras Reacta.
- Wdrożenie publiczne wymaga własnego sekretu JWT, profilu `prod` i adresu frontendu z HTTPS. Dostarczanie wiadomości użytkownikom zapewnia produkcyjny SMTP. Opcjonalny `APP_MAIL_MODE=log` zapisuje linki wyłącznie w logach i nie zastępuje skonfigurowanej wysyłki. Domyślne dane lokalnej konfiguracji nie są ustawieniami produkcyjnymi. Szczegóły: [DEPLOYMENT.md](DEPLOYMENT.md).

Hasło podczas rejestracji i resetu musi mieć co najmniej 8 znaków oraz maksymalnie 72 bajty po zakodowaniu w UTF-8 (ograniczenie BCrypt). Znaki spoza ASCII mogą zajmować więcej niż jeden bajt. Zmiana hasła usuwa także oczekujące tokeny usunięcia konta.

## Scenariusz sprawdzenia

Pełny scenariusz sprawdzono lokalnie, również dla profilu `prod` z transportem `log`. Po wdrożeniu zweryfikowano ochronę tras i odczyt bazy przy odmowie logowania; scenariuszy rejestracji, resetu i usunięcia online nie wykonywano na przeniesionym koncie. Pozostałe testy online wykonaj na osobnym fikcyjnym koncie zgodnie z [checklistą wdrożenia](DEPLOYMENT.md#5-sprawdź-wdrożenie).

1. Uruchom aplikację i lokalną skrzynkę Mailpit zgodnie z README.
2. Zarejestruj konto; sprawdź, że przed potwierdzeniem nie można się zalogować.
3. Otwórz link z wiadomości i potwierdź konto. Zaloguj się i otwórz profil.
4. Wyloguj się, poproś o reset hasła i ustaw inne hasło przez link z nowej wiadomości. Sprawdź, że stare hasło i stary JWT nie działają.
5. W profilu wpisz błędne hasło przy usuwaniu konta — wiadomość nie powinna zostać wysłana. Wpisz poprawne hasło i otwórz link z wiadomości.
6. Sprawdź, że samo otwarcie strony niczego nie usuwa. Dopiero przycisk potwierdzenia usuwa konto.
7. Sprawdź odmowę ponownego użycia tokenu, tokenu zmienionego lub wygasłego oraz przekierowanie niezalogowanego użytkownika z profilu na logowanie.

Obecny model danych obejmuje konta, tokeny, ćwiczenia i strukturę planów (V1–V4), progres (V5) oraz grupy, członkostwa i przypisania (V6). Kaskady kluczy obcych usuwają dane konta bez usuwania wspólnego katalogu i niezależnych planów innych użytkowników. `ProgressApiIntegrationTests` sprawdzają także usunięcie własnych wyników i pomiarów przez potwierdzenie konta; `TrainerApiIntegrationTests` obejmują integralność relacji i kopii planów. Model planów opisuje [WORKOUT_MODEL.md](WORKOUT_MODEL.md), a status sprawdzeń sprintu — [SPRINTY.md](SPRINTY.md).
