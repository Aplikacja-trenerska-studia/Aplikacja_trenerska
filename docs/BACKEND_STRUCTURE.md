# Struktura backendu

Katalog `backend/src/main/java/com/studia/aplikacja_trenerska/`:

```
├── AplikacjaTrenerskaApplication.java
├── ServletInitializer.java
├── config/                 # Security, JWT, wspólna konfiguracja
├── common/
│   ├── controller/         # np. health
│   └── exception/
├── auth/
│   ├── controller/
│   ├── service/
│   ├── dto/
│   ├── entity/
│   ├── repository/
│   └── security/           # filtr JWT
├── user/
│   ├── controller/
│   ├── dto/
│   ├── entity/
│   ├── mapper/
│   └── repository/
├── workout/
│   ├── controller/         # katalog ćwiczeń i CRUD /api/plans
│   ├── service/            # filtry katalogu, własność i transakcje planów
│   ├── dto/                # odpowiedzi katalogu, żądania i odpowiedzi planów
│   ├── mapper/             # encje → odpowiedzi API
│   ├── entity/             # ćwiczenia, plany, dni, wpisy ćwiczeń, enumy
│   └── repository/         # zapis/odczyt, właściciel planu, kolejność dzieci
├── progress/
├── trainer/
└── …
```

**Konwencja modułu:** `controller` → `service` → `repository` / `entity`, DTO na granicy API, `mapper` do mapowania entity ↔ DTO.

Migracje Flyway: `backend/src/main/resources/db/migration/`.

## Obsługa kont

- `auth/service` — rejestracja, logowanie, tokeny jednorazowe, reset hasła, potwierdzenie usunięcia i wysyłka e-maili.
- `auth/entity` i `auth/repository` — tokeny z przeznaczeniem, datą ważności i powiązaniem z użytkownikiem; przechowywany jest skrót tokenu.
- `auth/security` — weryfikacja JWT wraz z aktywnością konta, identyfikatorem użytkownika i wersją sesji.
- `user/controller` — profil i żądanie wysłania potwierdzenia usunięcia konta.
- `config` — adres frontendu, nadawca wiadomości i czasy ważności tokenów, obok konfiguracji JWT i zabezpieczeń.

Reset hasła zmienia wersję sesji użytkownika, aby wcześniej wydane JWT przestały działać. Zmiana danych i zużycie tokenu odbywają się w transakcji. Szczegóły przepływów i API: [ACCOUNTS.md](ACCOUNTS.md).

## Ćwiczenia i plany — fundament S2-05

Migracja V3 dodaje `exercises`, `workout_plans`, `plan_days`, `plan_exercises`. Model obejmuje właściciela planu, kolejność dni/ćwiczeń, serie, powtórzenia i ciężar `BigDecimal`. Kolekcje dni i wpisów ćwiczeń są zarządzane jako części planu; relacje do kont i katalogu nie kaskadują usunięcia w górę. Klucze obce usuwają dane własnych planów razem z kontem, zachowując katalog i cudze plany.

Migracja V4 (S2-06) wypełnia katalog 48 ćwiczeniami z polskimi nazwami i opisami, grupami mięśniowymi i typami `STRENGTH` / `HOME`. Dane nie są przypisane do konta, a ID generuje baza. Ponowny start aplikacji nie powiela wpisów dzięki historii Flyway.

Szczegóły pól, ograniczeń, katalogu, pracy z kolekcjami i testów: [WORKOUT_MODEL.md](WORKOUT_MODEL.md). API katalogu: [EXERCISES_API.md](EXERCISES_API.md). CRUD własnych planów: [WORKOUT_PLANS_API.md](WORKOUT_PLANS_API.md). Serwisy planów sprawdzają właściciela z sesji, zapisują całą strukturę w jednej transakcji i blokują wiersz użytkownika przed zapisem. Kontrolery zwracają DTO, a nowe plany otrzymują `201` i `Location`; usunięcie zwraca `204`.
