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
├── progress/              # controller, service, dto, entity, repository: serie i pomiary
├── trainer/               # controller, service, dto, entity, repository: grupy i zgoda
├── admin/                 # controller, service, dto, repository: konta, role, blokady, liczniki
└── …
```

**Konwencja modułu:** `controller` → `service` → `repository` / `entity`, DTO na granicy API, `mapper` do mapowania entity ↔ DTO.

Migracje SQL Flyway: `backend/src/main/resources/db/migration/`. Migracja Java V7 (warunkowe włączenie hypertabel) jest w `backend/src/main/java/db/migration/`.

## Obsługa kont

- `auth/service` — rejestracja, logowanie, tokeny jednorazowe, reset hasła, potwierdzenie usunięcia i wysyłka e-maili.
- `auth/entity` i `auth/repository` — tokeny z przeznaczeniem, datą ważności i powiązaniem z użytkownikiem; przechowywany jest skrót tokenu.
- `auth/security` — weryfikacja JWT wraz z aktywnością konta, identyfikatorem użytkownika i wersją sesji.
- `user/entity/User` — osobne znaczenia potwierdzenia e-maila (`enabled`) i blokady administracyjnej (`blocked`); aktywne konto wymaga obu warunków: potwierdzone i niezablokowane.
- `user/controller` — profil i żądanie wysłania potwierdzenia usunięcia konta.
- `config` — adres frontendu, nadawca wiadomości i czasy ważności tokenów, obok konfiguracji JWT i zabezpieczeń.

Reset hasła zmienia wersję sesji użytkownika, aby wcześniej wydane JWT przestały działać. Zmiana danych i zużycie tokenu odbywają się w transakcji. Szczegóły przepływów i API: [ACCOUNTS.md](ACCOUNTS.md).

## Ćwiczenia i plany — fundament S2-05

Migracja V3 dodaje `exercises`, `workout_plans`, `plan_days`, `plan_exercises`. Model obejmuje właściciela planu, kolejność dni/ćwiczeń, serie, powtórzenia i ciężar `BigDecimal`. Kolekcje dni i wpisów ćwiczeń są zarządzane jako części planu; relacje do kont i katalogu nie kaskadują usunięcia w górę. Klucze obce usuwają dane własnych planów razem z kontem, zachowując katalog i cudze plany.

Migracja V4 (S2-06) wypełnia katalog 48 ćwiczeniami z polskimi nazwami i opisami, grupami mięśniowymi i typami `STRENGTH` / `HOME`. Dane nie są przypisane do konta, a ID generuje baza. Ponowny start aplikacji nie powiela wpisów dzięki historii Flyway.

Szczegóły pól, ograniczeń, katalogu, pracy z kolekcjami i testów: [WORKOUT_MODEL.md](WORKOUT_MODEL.md). API katalogu: [EXERCISES_API.md](EXERCISES_API.md). CRUD własnych planów: [WORKOUT_PLANS_API.md](WORKOUT_PLANS_API.md). Serwisy planów sprawdzają właściciela z sesji, zapisują całą strukturę w jednej transakcji i blokują wiersz użytkownika przed zapisem. Kontrolery zwracają DTO, a nowe plany otrzymują `201` i `Location`; usunięcie zwraca `204`.

## Progres i trener — Sprint 3

`ProgressService` zapisuje serie jednego treningu atomowo, sprawdza właściciela planu i przynależność dnia oraz ćwiczeń. Wyniki i pomiary mają UUID oraz czas jako złożony klucz; nazwy planu, dnia i ćwiczenia są kopiowane do historii. Odczyt i usuwanie są ograniczone do właściciela.

`TrainerService` sprawdza rolę i własność grup, zarządza zaproszeniami oraz zgodą podopiecznego. Przypisanie głęboko kopiuje plan do konta odbiorcy. Trener wywołuje odczyt progresu dopiero po sprawdzeniu zaakceptowanego członkostwa. Blokady kont i członkostw serializują zapisy oraz współpracują z kaskadami usuwania konta.

V5 dodaje wyniki i pomiary, V6 grupy/członkostwa/przypisania, a V7 konwertuje pomiary do hypertabel tylko w bazie udostępniającej TimescaleDB. Szczegóły API, bezpieczeństwa, limitów i scenariuszy w [SPRINT3.md](SPRINT3.md).

## Administrator — rozszerzenie S3-12

`AdminController` i reguły URL chronią `/api/admin/**` rolą `ADMIN`. `AdminService` ponownie sprawdza aktualną rolę i stan operatora, zwraca stronicowane DTO kont i zbiorcze liczniki oraz zmienia role i blokady. Wyszukiwanie traktuje metaznaki LIKE dosłownie. Odpowiedzi nie zawierają haseł ani tokenów i nie udostępniają cudzych planów lub historii pomiarów.

`AdminAccessGuard` blokuje pojedynczy wiersz `admin_control` przed blokadami kont. Ten sam porządek obejmuje zmiany ról/blokad, pierwszą inicjalizację i `AccountService.confirmDeletion`, aby równoległe operacje nie odebrały ostatniego aktywnego dostępu administracyjnego. Faktyczna zmiana roli lub blokady unieważnia wcześniejsze JWT przez `token_version`. `AdminSql` kwalifikuje tabele zgodnie z `spring.jpa.properties.hibernate.default_schema`, zachowując prywatny schemat produkcyjny.

`config/AdminBootstrapInitializer` jest uruchamiany przy starcie po migracjach. Opcjonalne `APP_BOOTSTRAP_ADMIN_EMAIL` może jednokrotnie podnieść istniejące, potwierdzone i niezablokowane konto do `ADMIN`; nie tworzy kont ani haseł. Stan wykonania jest zapisany w bazie, a kolejne starty i zmiany ustawienia nie nadają ról ponownie.

V8 dodaje `users.blocked`, indeksy listy administracyjnej i singleton `admin_control`, zachowując dotychczasowe dane. `AdminMigrationIntegrationTests` sprawdza aktualizację V7 → V8, `AdminApiIntegrationTests` API i uprawnienia, a `AdminLifecycleIntegrationTests` przepływy kont, blokady, inicjalizację i ochronę ostatniego administratora. Opis panelu, konfiguracji i wyników testów: [ADMIN.md](ADMIN.md).
