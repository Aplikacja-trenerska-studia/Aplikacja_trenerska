# Ćwiczenia i plany na frontendzie — S2-08 / S2-09

## Widoki

| Adres | Zakres |
|---|---|
| `/cwiczenia` | Katalog z polskimi nazwami grup i typów, opisami, wyszukiwaniem nazwy oraz filtrami grupy mięśniowej i typu. |
| `/plany` | Własne plany od najnowszego, z nazwą, opisem i datą utworzenia; przejście do szczegółów. |
| `/plany/:id` | Podgląd planu: uporządkowane dni i ćwiczenia, serie, powtórzenia i ciężar w polskim formacie liczbowym. |
| `/plany/nowy` | Kreator planu z dniami i wyborem ćwiczeń z katalogu. |
| `/plany/:id/edytuj` | Edycja całego własnego planu z odtworzonymi wartościami formularza. |

Widoki są chronione istniejącym `ProtectedRoute` i korzystają z JWT przez `apiFetch`. Backend nadal sprawdza właściciela planu. Nowe zakładki są dostępne po zalogowaniu, a strona wejściowa prowadzi do planów i katalogu.

Po wejściu bez sesji użytkownik trafia na logowanie i wraca do wybranej listy lub planu, z zachowaniem parametrów wyszukiwania. Formularz logowania i `GuestRoute` używają tego samego, ograniczonego do obsługiwanych lokalnych tras adresu powrotu.

## Filtry i stany

Filtry katalogu są zatwierdzane przyciskiem „Filtruj” i zapisują się w URL jako `q`, `muscleGroup`, `type`. Działają łącznie. Wstecz/Dalej i odświeżenie strony odtwarzają wartości filtrów; „Wyczyść filtry” wraca do pełnej listy. Zapytania kodują znaki polskie oraz metaznaki URL. Klient korzysta z filtrowania API, bez własnej kopii logiki wyszukiwania.

Listy i szczegóły obsługują ładowanie, błędy z ponowieniem i brak danych. Brak planów prowadzi do utworzenia pierwszego planu lub przeglądania ćwiczeń, a brak wyników pozwala wyczyścić filtry. Nieistniejący/cudzy plan wyświetla komunikat API i odnośnik do własnych planów. Odpowiedź `401` usuwa sesję i przekierowuje do logowania.

`useApiResource` anuluje zapytania przy zmianie adresu lub opuszczeniu widoku, ignoruje spóźnione odpowiedzi i nie pokazuje wyników poprzedniego filtra/ID podczas nowego odczytu. Wspólne typy odpowiedzi i polskie etykiety są w `frontend/src/types/workout.ts`.

Na małym ekranie formularz i katalog układają się w jedną kolumnę. Na większym ekranie katalog ma dwie kolumny. Podgląd planu pokazuje parametry każdego ćwiczenia bez szerokiej tabeli.

## Kreator, edycja i usuwanie — S2-09

Przycisk „Nowy plan” otwiera formularz nazwy, opcjonalnego opisu i dni treningowych. Każdy dzień ma własną nazwę oraz listę wpisów ćwiczeń. Wybór ćwiczeń korzysta z załadowanego katalogu i lokalnych filtrów nazwy, grupy oraz typu. Można wielokrotnie dodać to samo ćwiczenie, usunąć wpis lub dzień oraz zmienić ich kolejność strzałkami. Ostatniego dnia nie można usunąć; co najmniej jedno ćwiczenie w każdym dniu jest wymagane do zapisu.

Każdy wpis ma serie, powtórzenia i ciężar. Ciężar przyjmuje przecinek lub kropkę i jest wysyłany jako liczba, z maksymalnie dwoma miejscami dziesiętnymi. Walidacja odpowiada ograniczeniom [API planów](WORKOUT_PLANS_API.md), w tym 31 dniom i 100 wpisom na dzień. Błędy są przypisane do pól, a podsumowanie błędów otrzymuje fokus. Wewnętrzne klucze formularza pozostają stabilne podczas zmiany kolejności, więc wpisane liczby przemieszczają się razem z właściwym ćwiczeniem.

POST tworzy plan, a PUT zastępuje cały istniejący plan. Po sukcesie następuje przejście do szczegółów z komunikatem. Podczas zapisu formularz i przyciski są zablokowane, aby uniknąć wielokrotnego wysłania. Błąd API pozostawia pełny szkic w formularzu i pozwala ponowić zapis. „Anuluj” pyta o odrzucenie zmienionego szkicu; odświeżenie lub zamknięcie strony z niezapisanymi zmianami wywołuje ostrzeżenie przeglądarki. Szkic jest przechowywany w pamięci bieżącego formularza.

W szczegółach są akcje „Edytuj plan” i „Usuń plan”. Usuwanie otwiera natywny modalny `dialog` z nazwą planu, anulowaniem i osobnym potwierdzeniem. Samo otwarcie lub anulowanie nie wysyła DELETE. Błąd usuwania pozostawia dialog otwarty i pozwala ponowić operację; sukces wraca do odświeżonej listy. Dialog blokuje interakcję z tłem i można go zamknąć klawiszem Escape, gdy operacja nie trwa.

## Weryfikacja

Build, lint bez ostrzeżeń i 16 testów pomocniczych funkcji frontendu przeszły. Zakres obejmuje kodowanie/odtwarzanie filtrów, bezpieczny adres powrotu, konwersję szkicu do żądania API, walidację liczb i limitów, polski przecinek, odtworzenie edycji oraz zachowanie wartości/kluczy podczas zmiany kolejności. Polecenia w `frontend`: `npm run build`, `npm run lint`, `npm test` (Node 22+).

Test przeglądarkowy `tests/smoke-workout-ui.mjs` wykonano na uruchomionym Docker Compose przez nginx z rzeczywistym API i Mailpit. Sprawdza:

- rejestrację przez UI, aktywację przez stronę otwartą z wiadomości Mailpit, ochronę tras i powrót po logowaniu z filtrami;
- połączone filtry, historię nawigacji, czyszczenie i puste wyniki;
- pustą listę planów oraz utworzenie planu przez kreator: dwa dni, dodawanie/usuwanie ćwiczeń, zmiana kolejności dni i wpisów oraz liczby z przecinkiem;
- nieprawidłowy szkic bez zapisu, zachowanie szkicu po błędzie POST i jedno wysłanie podczas oczekiwania na odpowiedź;
- anulowanie edycji, usunięcie dnia, zmianę kolejności i parametrów, błąd PUT z ponowieniem i trwały odczyt zmian po odświeżeniu;
- anulowanie usunięcia, błąd DELETE z ponowieniem i faktyczne usunięcie potwierdzone przez API;
- ładowanie, błędy obu list i ponowienie, brak planu (`404`), utratę sesji (`401`);
- układ 1280 × 900 oraz mobilny 375 × 812, także kreator i dialog usuwania, bez poziomego przewijania i błędów JavaScript.

Zrzuty sprawdzono wizualnie. Znajdują się w ignorowanym katalogu `backend/target/ui-screenshots`. To kontrola widoku mobilnego w Chrome, nie test na rzeczywistym telefonie; taki test nadal pozostaje w Definition of Done.

Powtórzenie testu z katalogu głównego wymaga działającego Compose, Node i Playwright. Przykład PowerShell z lokalnym Chrome:

```powershell
docker compose up -d --build
npm install --prefix backend/target/ui-runner --no-save playwright
$env:NODE_PATH = (Resolve-Path backend/target/ui-runner/node_modules).Path
$env:UI_CHROME_PATH = 'C:\Program Files\Google\Chrome\Application\chrome.exe'
node tests/smoke-workout-ui.mjs
```

Playwright można również udostępnić z istniejącego środowiska Node przez `NODE_PATH`. Bez `UI_CHROME_PATH` używana jest przeglądarka zainstalowana dla Playwright. Test dopuszcza wyłącznie lokalne adresy HTTP, tworzy jednorazowe konto, aktywuje je przez Mailpit, a w bloku sprzątania usuwa konto i jego plan. Wiadomości pozostają w Mailpit. Przypadki błędów i opóźnienia API są symulowane wyłącznie w testowej przeglądarce.
