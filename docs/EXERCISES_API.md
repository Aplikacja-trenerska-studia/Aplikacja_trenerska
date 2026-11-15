# API katalogu ćwiczeń — część S2-07

`GET /api/exercises` zwraca wspólny katalog ćwiczeń dla zalogowanych kont `USER`, `TRAINER` i `ADMIN`. Wymagany nagłówek: `Authorization: Bearer <JWT>`. Dostęp wymaga aktywnego konta i aktualnej sesji; chroni go filtr JWT oraz `@PreAuthorize`.

## Filtry

Wszystkie parametry są opcjonalne i łączą się warunkiem AND.

| Parametr | Wartość | Działanie |
|---|---|---|
| `muscleGroup` | Wartość enum `MuscleGroup`, np. `CHEST`, `BACK`, `CORE` | Dokładne dopasowanie głównej grupy mięśniowej. Pełna lista: [WORKOUT_MODEL.md](WORKOUT_MODEL.md). |
| `type` | `STRENGTH` albo `HOME` | Dokładne dopasowanie typu. |
| `q` | Fragment nazwy, maks. 160 znaków po usunięciu białych znaków z początku i końca | Szuka tylko w nazwie, bez rozróżniania wielkości liter. Polskie znaki są zachowane i istotne dla dopasowania. `%`, `_` i `\` są traktowane dosłownie. |

Brak parametru lub pusta wartość oznacza brak danego filtra. Wartości enumów wymagają wielkich liter; nieznana wartość daje `400`. Puste lub złożone wyłącznie z białych znaków `q` nie ogranicza wyników.

Przykłady:

```text
GET /api/exercises
GET /api/exercises?muscleGroup=CHEST
GET /api/exercises?type=HOME
GET /api/exercises?muscleGroup=CHEST&type=HOME&q=pompki
```

Przy przesyłaniu zapytania klient powinien kodować parametry URL, np. przez `URLSearchParams`.

## Odpowiedź

`200 OK`, tablica JSON. Poniższy ID jest przykładem — aplikacja korzysta z identyfikatorów zwracanych przez API, a nie ze stałych numerów katalogu.

```json
[
  {
    "id": 123,
    "name": "Pompki klasyczne",
    "muscleGroup": "CHEST",
    "type": "HOME",
    "description": "W podporze na dłoniach i stopach opuszczaj tułów przez ugięcie łokci, następnie wracaj do pozycji wyjściowej. Bez dodatkowego sprzętu."
  }
]
```

Zwracane pola to `id`, `name`, `muscleGroup`, `type`, `description`; opis może być `null` dla wpisów bez opisu. Odpowiedź nie zawiera relacji do autora ani danych konta. Brak dopasowań daje `[]` ze statusem `200`.

Lista jest sortowana rosnąco po nazwie bez rozróżniania wielkości liter, z ID jako rozstrzygnięciem remisu. Kolejność znaków zależy od ustawień porównywania tekstu w bazie. Endpoint zwraca wszystkie dopasowania; obecny katalog startowy ma 48 wpisów i nie używa paginacji.

## Błędy

| Status | Znaczenie |
|---|---|
| `401` | Brak JWT, błędny/wygasły token, nieaktywne lub usunięte konto albo unieważniona sesja. |
| `403` | Konto nie ma roli dopuszczonej przez kontroler. |
| `400` | Nieprawidłowy `muscleGroup` / `type` lub zbyt długie `q`. |

Błędy zawierają `message`. Nieprawidłowy parametr enum zawiera także `errors`, np.:

```json
{
  "message": "Nieprawidłowa wartość parametru: type",
  "errors": { "type": "Nieprawidłowa wartość" }
}
```

## Implementacja i weryfikacja

Przepływ: `ExerciseController` → `ExerciseService` → `ExerciseRepository`, mapowanie przez `ExerciseMapper` do `ExerciseResponse`. Filtry budują warunki zapytania do bazy; wyszukiwanie przekazuje wartości jako parametry i cytuje metaznaki LIKE. Serwis działa w transakcji tylko do odczytu. Endpoint nie modyfikuje katalogu.

`ExerciseApiIntegrationTests` sprawdzają JWT i dostęp wszystkich trzech ról, kształt DTO, pojedyncze i połączone filtry, polskie znaki, puste wyniki, znaki specjalne, błędy parametrów i kolejność wyników. Uruchomienie: standardowe `./mvnw.cmd test` w `backend` z JDK 21 albo `pwsh -NoProfile -File tests/test-workout-postgres.ps1` z katalogu głównego (Docker i pełny zestaw na PostgreSQL 16).

CRUD własnych planów i testy uprawnień do tych operacji są opisane w [WORKOUT_PLANS_API.md](WORKOUT_PLANS_API.md). Widok katalogu z filtrami jest wdrożony w S2-08: [WORKOUT_FRONTEND.md](WORKOUT_FRONTEND.md).
