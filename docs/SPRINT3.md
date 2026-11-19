# Progres i współpraca z trenerem — Sprint 3

Implementacja przygotowana 19.11.2026 na `feat/sprint-3`. Planowane daty sprintu pozostają 23–29.11. Status testów i publikacji jest w [SPRINTY.md](SPRINTY.md).

## Korzystanie z aplikacji

- **Wykonaj trening** (`/trening`): wybierz plan i dzień, wpisz rzeczywiste powtórzenia i ciężary, odznacz pominięte serie i zapisz. Można wejść bezpośrednio ze szczegółów planu. Nieudany zapis zachowuje formularz.
- **Progres** (`/progres`): dodawaj wagę lub wybrane obwody, filtruj historię po ćwiczeniu i datach, przeglądaj wykresy. Usunięcie błędnego wpisu wymaga potwierdzenia.
- **Panel trenera** (`/trener`): trener tworzy grupę, zaprasza aktywne konto użytkownika po e-mailu, przypisuje własny plan osobie lub całej zaakceptowanej grupie i odczytuje progres podopiecznych.
- **Współpraca** (`/wspolpraca`): użytkownik przyjmuje lub odrzuca zaproszenie, widzi plany od trenera i może zakończyć współpracę. Zaproszenie pojawia się w aplikacji i nie wymaga dodatkowego e-maila.

Rola `TRAINER` nadal jest nadawana przez operatora zgodnie z [ACCOUNTS.md](ACCOUNTS.md#role-user--trainer). Rejestracja nie pozwala samodzielnie uzyskać tej roli.

## Wyniki i pomiary — API

Wszystkie endpointy wymagają aktywnej sesji. Właściciel wynika z JWT; klient nie wybiera `userId`.

| Metoda | Endpoint | Działanie |
|---|---|---|
| POST | `/api/results` | Atomowy zapis wykonanych serii jednego dnia planu |
| GET | `/api/results?exerciseId=&from=&to=` | Własna historia, od najnowszych, maks. 2000 serii |
| DELETE | `/api/results/{id}?time=...` | Usunięcie własnej serii |
| POST | `/api/body-metrics` | Zapis wagi lub co najmniej jednego obwodu |
| GET | `/api/body-metrics?from=&to=` | Własne pomiary, od najnowszych, maks. 500 |
| DELETE | `/api/body-metrics/{id}?time=...` | Usunięcie własnego pomiaru |

Przykładowy zapis treningu (identyfikatory pobierz z własnego planu):

```json
{
  "time": "2026-11-19T15:00:00Z",
  "planId": 12,
  "planDayId": 34,
  "sets": [
    {"exerciseId": 5, "setNo": 1, "reps": 10, "weight": 42.5},
    {"exerciseId": 5, "setNo": 2, "reps": 8, "weight": 45}
  ]
}
```

Odpowiedź zawiera tablicę zapisanych serii: `id` (UUID), `time`, `exerciseId`, `exerciseName`, `setNo`, `reps`, `weight`, `planName`, `dayName`. Dzień musi należeć do własnego planu, a ćwiczenia do wybranego dnia. Można zapisać część planowanych serii. Para ćwiczenie/numer serii nie może powtarzać się w jednym żądaniu. Nieprawidłowa seria odrzuca cały zapis.

Pomiary mają `time` i dowolne z pól `weightKg`, `chestCm`, `waistCm`, `hipsCm`, `armCm`, `thighCm`; odpowiedź dodaje UUID `id`. Niewypełnione pola mogą mieć `null`. Wymagany jest przynajmniej jeden pomiar.

Limity: 500 serii na zapis, numery serii 1–100 na ćwiczenie, powtórzenia 1–1000, ciężar 0–2000 kg. Masa ciała 10–500 kg, klatka/talia/biodra 10–300 cm, ramię 5–150 cm, udo 5–200 cm. Liczby mają najwyżej dwa miejsca po przecinku. Formularz treningu pokazuje ograniczenie dużego planu; pojedynczy wpis ćwiczenia obejmuje do 50 planowanych serii.

Czas zapisu jest normalizowany do mikrosekund; daty pomiarów zaczynają się od 2000 r. i nie mogą wyprzedzać bieżącego czasu poza niewielką tolerancją zegara. Filtry `from`/`to` są opcjonalnymi chwilami ISO 8601, włącznie z oboma końcami. Interfejs zamienia lokalne dni na odpowiedni zakres UTC i wyświetla daty lokalnie. Przy usuwaniu przekaż dokładny `time` zwrócony przez API, zakodowany jako parametr URL.

## Znaczenie wykresów

Wykres ćwiczenia pokazuje **największy ciężar zapisanej serii w każdym lokalnym dniu**, osobno dla wybranego ćwiczenia. Nie jest wykresem objętości ani szacowanego 1RM. Waga i obwody pokazują rzeczywiste pomiary w czasie, pomijając brakujące wartości. Pojedynczy punkt i pusta historia mają osobne stany. Pod wykresem można rozwinąć tabelę z dokładnymi danymi.

Historia pokazuje limit odpowiedzi oraz pozwala zawęzić daty, aby odczytać starsze wpisy. Nazwy planu, dnia i ćwiczenia zapisane przy wyniku są zachowywane mimo późniejszej edycji lub usunięcia planu.

## Grupy i przypisania — API

| Metoda | Endpoint | Dostęp |
|---|---|---|
| GET / POST | `/api/groups` | Własne grupy trenera; POST `{name}` |
| GET / DELETE | `/api/groups/{id}` | Trener będący właścicielem |
| POST | `/api/groups/{id}/members` | Zaproszenie `{email}` do własnej grupy |
| DELETE | `/api/groups/{id}/members/{userId}` | Usunięcie podopiecznego przez właściciela |
| POST | `/api/groups/{id}/assign-plan` | `{planId,userId?}`; brak `userId` oznacza zaakceptowaną grupę |
| GET | `/api/groups/{id}/members/{userId}/results` | Właściciel grupy, zaakceptowane członkostwo; filtry jak dla wyników |
| GET | `/api/groups/{id}/members/{userId}/body-metrics` | Jak wyżej; filtry dat |
| GET | `/api/my-invitations` | Własne oczekujące i przyjęte zaproszenia |
| POST | `/api/my-invitations/{groupId}/accept` | Akceptacja własnego zaproszenia |
| DELETE | `/api/my-invitations/{groupId}` | Odrzucenie lub zakończenie własnego członkostwa |
| GET | `/api/my-assignments` | Własne przypisania |

Grupa ma `id`, `name`, `createdAt`, `members`; członek: `userId`, `displayName`, `email`, `status` (`PENDING`/`ACCEPTED`), `invitedAt`, `acceptedAt`. Zaproszenie użytkownika zawiera `groupId`, `groupName`, `trainerName`, status i daty. Przypisanie zawiera `id`, `planId` (kopia lub `null`, jeśli usunięta), `planName`, `trainerName`, `groupName`, `userId`, `displayName`, `assignedAt`.

Trener nie widzi progresu ani nie przypisuje planu przed akceptacją. Usunięcie członkostwa odbiera dostęp przez daną grupę; jeśli użytkownik zaakceptował również inną grupę tego samego trenera, tamta relacja pozostaje aktywna. Cudze grupy i niedostępni podopieczni zwracają `404`. Odebranie roli trenera blokuje jego endpointy oraz przyjmowanie nieaktualnych zaproszeń.

Przypisanie tworzy **pełną, niezależną kopię planu należącą do podopiecznego**. Jest widoczna także w `/api/plans` i może służyć do zapisu treningu. Edycja źródła nie zmienia kopii. Powtórzenie przypisania tego samego źródła tej samej osobie w tej samej grupie zwraca istniejącą kopię. Jeżeli podopieczny ją usunie, kolejne przypisanie utworzy nową z aktualnego źródła.

Usunięcie grupy lub członkostwa usuwa metadane przypisania, zachowując kopię i wyniki użytkownika. Usunięcie konta usuwa jego własne plany, wyniki, pomiary, grupy, członkostwa i przypisania. Kopie planów już należące do innych użytkowników pozostają zachowane. Wspólny katalog ćwiczeń pozostaje w bazie.

## Baza i migracje

- **V5**: `exercise_results`, `body_metrics`, złożone klucze `(id,time)`, indeksy użytkownika/ćwiczenia/czasu, ograniczenia i kaskady.
- **V6**: `training_groups`, `group_members`, `plan_assignments`, status zgody, indeksy i kaskady.
- **V7** (migracja Java w `backend/src/main/java/db/migration`): automatyczna konwersja tabel pomiarów do hypertabel, jeśli PostgreSQL udostępnia TimescaleDB. W H2 i Supabase PG17 pomija konwersję. W obu wariantach funkcje aplikacji są takie same.

Flyway wykonuje migracje przy starcie backendu. Nie uruchamiaj ich ręcznie w Supabase i nie zmieniaj już zastosowanych wersji. Nowe tabele wymagają następnego numeru **V8**. Zmiany V5–V7 nie usuwają istniejących kont ani planów.

## Weryfikacja

Sprawdzone 19.11.2026:

- 216 testów backendu: 215 przeszło na H2, a test aktualizacji PostgreSQL został tam celowo pominięty; komplet 216 przeszedł w zestawach uruchomionych dla PostgreSQL 17 oraz TimescaleDB/PostgreSQL 16. Osobny test konfiguracji `prod` ma izolowaną bazę H2 także podczas tych przebiegów.
- Aktualizacja V4 → V7 zachowała istniejące konto, wersję sesji i plan z ćwiczeniem; ponowne uruchomienie Flyway nie wykonało migracji ponownie. W obu testach użyto prywatnego schematu; TimescaleDB ma dwie prawdziwe hypertabele.
- 27 testów frontendu, build i lint bez błędów.
- Pełny test UI opisany niżej przeszedł na Compose z prawdziwym API i SMTP do Mailpit. Sprawdzono komputer 1280 × 900 oraz widok 375 × 812, zachowanie formularza po błędzie i odebranie dostępu po zakończeniu współpracy. Brak poziomego przewijania oraz błędów JavaScript; zrzuty sprawdzono wizualnie. Osie wykresów dodatkowo sprawdzono po poprawce: etykiety 12 px przy obu szerokościach.
- `tests/smoke-deployment.ps1`: profil `prod`, port 8097, schemat `training_app`, V1–V7, CORS, ochrona tras, SMTP oraz przepływ konta i planu.

Pozostają zespołowe review, test na rzeczywistym telefonie oraz scalenie/publikacja i odbiór nowych funkcji online. Sprawdzenia lokalne nie oznaczają wykonania nowych migracji na produkcyjnym Supabase.

```powershell
# Backend: zwykły PostgreSQL 17 w prywatnym schemacie training_app + aktualizacja V4 -> V7
pwsh -NoProfile -File tests/test-sprint3-databases.ps1 -Database postgres
# Te same testy i kontrola dwóch prawdziwych hypertabel
pwsh -NoProfile -File tests/test-sprint3-databases.ps1 -Database timescale
```

Test przeglądarkowy `tests/smoke-sprint3-ui.mjs` wymaga Playwright, Chromium i oddzielnego Compose o nazwie zaczynającej się od `sprint3-`. Celowo odmawia pracy na zdalnych adresach i istniejącej bazie aplikacji. Rejestruje konta testowe, odbiera wiadomości z Mailpit, tworzy plan w UI i zapisuje trening, sprawdza pomiary i wykresy, zaproszenie/akceptację/przypisanie oraz odebranie zgody. Rolę trenera nadaje operatorem wyłącznie w tej odizolowanej bazie. Konta usuwa przez standardowy przepływ potwierdzony wiadomością. Zrzuty zapisuje w ignorowanym `backend/target/sprint3-ui`.

```powershell
docker compose -p sprint3-validation up -d --build
# Po instalacji Playwright ustaw NODE_PATH na katalog node_modules, jeśli jest poza repo.
$env:COMPOSE_PROJECT_NAME = 'sprint3-validation'
node tests/smoke-sprint3-ui.mjs
docker compose -p sprint3-validation down -v
```
