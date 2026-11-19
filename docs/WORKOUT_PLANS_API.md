# API własnych planów — S2-07 / S2-11

Wszystkie operacje wymagają `Authorization: Bearer <JWT>`. Role `USER`, `TRAINER` i `ADMIN` mogą zarządzać swoimi planami. Backend wybiera właściciela z uwierzytelnionej sesji. Rola trenera ani administratora nie daje dostępu do cudzych planów przez te endpointy.

## Endpointy

| Metoda | Endpoint | Sukces |
|---|---|---|
| GET | `/api/plans` | `200`, tablica podsumowań własnych planów, od najnowszego; remisy rozstrzyga malejące ID. Brak planów daje `[]`. |
| POST | `/api/plans` | `201`, szczegóły utworzonego planu oraz nagłówek `Location: /api/plans/{id}`. |
| GET | `/api/plans/{id}` | `200`, szczegóły własnego planu. |
| PUT | `/api/plans/{id}` | `200`, szczegóły po pełnym zastąpieniu nazwy, opisu, dni i wpisów ćwiczeń. |
| DELETE | `/api/plans/{id}` | `204`, bez treści; usuwa plan oraz jego dni i wpisy ćwiczeń. Katalog pozostaje. |

Brak planu i próba dostępu do cudzego planu dają ten sam `404` z komunikatem `Nie znaleziono planu`. Dotyczy to również cudzego planu z `is_shared=true` w bazie; udostępnianie jest zakresem S4. Kolejne usunięcie już usuniętego planu daje `404`.

## Treść POST / PUT

Żądanie używa `Content-Type: application/json`. Przykładowe ID ćwiczenia należy zastąpić ID otrzymanym z [GET /api/exercises](EXERCISES_API.md).

```json
{
  "name": "Trening domowy A",
  "description": "Dwa treningi tygodniowo",
  "days": [
    {
      "name": "Dzień A",
      "exercises": [
        { "exerciseId": 123, "sets": 3, "reps": 10, "weight": 0 }
      ]
    }
  ]
}
```

| Pole | Reguła |
|---|---|
| `name` planu i dnia | Wymagane, niepuste, do 120 znaków. Białe znaki na początku i końcu są usuwane przy zapisie. |
| `description` | Opcjonalny opis planu, do 2000 znaków; brak pola w PUT usuwa dotychczasowy opis. |
| `days` | Wymagana tablica 1–31 dni, bez elementów `null`. |
| `days[].exercises` | Wymagana tablica 1–100 wpisów w każdym dniu, bez elementów `null`. |
| `exerciseId` | Wymagane, dodatnie ID istniejącego ćwiczenia z katalogu. To samo ćwiczenie może wystąpić kilka razy. |
| `sets`, `reps` | Wymagane dodatnie liczby całkowite. |
| `weight` | Wymagana liczba od `0` do `99999.99`, do dwóch miejsc dziesiętnych; `0` oznacza brak dodatkowego obciążenia. |

Pozycje dni i ćwiczeń wynikają z kolejności tablic. Backend nadaje `dayIndex` i `orderIndex` od zera. Pola właściciela, udostępniania i identyfikatory dzieci nie są częścią DTO wejściowego; przesłanie takich dodatkowych pól nie zmienia właściciela, udostępniania ani sposobu przypisania dzieci.

PUT wymaga kompletnej zawartości planu. Plan zachowuje ID, właściciela i czas utworzenia. Dni i wpisy ćwiczeń są zastępowane nowymi rekordami, więc ich ID mogą się zmienić; klient powinien użyć nowej odpowiedzi. Operacja usuwa stare pozycje przed wstawieniem nowych, aby uniknąć konfliktu unikalnych indeksów.

## Odpowiedzi

Lista zwraca podsumowania: `id`, `name`, `description`, `createdAt`.

POST, GET szczegółów i PUT zwracają dodatkowo `days`. Każdy dzień ma `id`, `name`, `dayIndex` i `exercises`. Wpis ćwiczenia ma `id`, `orderIndex`, `sets`, `reps`, `weight` oraz `exercise` z polami `id`, `name`, `muscleGroup`, `type`, `description`. Odpowiedzi nie zawierają encji ani danych konta właściciela/autora.

Mapowanie DTO odbywa się w transakcji serwisu. Dni i ćwiczenia są zwracane w kolejności indeksów również bezpośrednio po zapisie.

## Bezpieczeństwo, błędy i transakcje

- `401`: brak poprawnej aktywnej sesji, wyłączone lub usunięte konto albo unieważniony JWT.
- `403`: brak roli wymaganej przez kontroler.
- `404`: nieistniejący lub cudzy plan.
- `400`: błędny JSON, brak wymaganych pól, błędny typ ID w ścieżce, nieprawidłowe wartości lub nieistniejące ćwiczenie.

Błędy zawierają `message`. Błędy walidacji zawierają także `errors` z nazwami pól, np. `days[0].exercises[0].sets`.

Tworzenie, edycja i usuwanie są pojedynczymi transakcjami. Nieudane żądanie nie pozostawia częściowo zapisanego planu. Przed zastąpieniem zawartości sprawdzane są wszystkie ID ćwiczeń. Zapisy blokują wiersz właściciela, zgodnie z kolejnością blokad przy usuwaniu konta; zapisy tego samego użytkownika wykonują się kolejno. Ostatni udany PUT ustala zawartość planu. API nie ma jeszcze wersjonowania konfliktów edycji.

## Testy

`WorkoutPlanApiIntegrationTests` wykonują żądania HTTP bez nadrzędnej transakcji testowej, dzięki czemu kolejne żądania sprawdzają rzeczywiście zatwierdzone dane. Zakres: pełny CRUD każdej roli, wielokrotna edycja i kolejność, usuwanie dzieci, brak dostępu do cudzych danych, próba podmiany właściciela/udostępniania, nieaktywne i unieważnione sesje, walidacja zagnieżdżonych danych, limity kolekcji, nieznane ćwiczenia i zachowanie planu po nieudanej edycji.

Wszystkie testy: `./mvnw.cmd test` w `backend` (JDK 21) lub `pwsh -NoProfile -File tests/test-workout-postgres.ps1` w katalogu głównym (Docker, tymczasowy PostgreSQL 16). Testy nie łączą się z bazą Compose.

API planów i katalogu zamyka zakres S2-07, a testy HTTP planów — S2-11. Listy, kreator, edycja i usuwanie S2-08/09 są wdrożone: [WORKOUT_FRONTEND.md](WORKOUT_FRONTEND.md). Pełny scenariusz tworzenia planu przez UI przeszedł na lokalnym Docker Compose. Review i test na rzeczywistym telefonie pozostają w domknięciu zespołowym sprintu. Kopie planów przypisane przez trenera należą do odbiorcy i są obsługiwane przez to samo API; szczegóły: [SPRINT3.md](SPRINT3.md).
