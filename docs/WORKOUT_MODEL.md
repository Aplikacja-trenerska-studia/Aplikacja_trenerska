# Model ćwiczeń i planów — S2-05 / S2-06

Migracja `V3__exercises_and_workout_plans.sql` dodaje cztery tabele. Model JPA i repozytoria są w `workout/entity` oraz `workout/repository`. Schemat powstaje przez Flyway; Hibernate sprawdza zgodność przez `ddl-auto=validate`.

| Tabela / encja | Dane i relacje |
|---|---|
| `exercises` / `Exercise` | Nazwa (160 znaków), główna grupa mięśniowa, typ `STRENGTH` / `HOME`, opcjonalny opis (2000 znaków), opcjonalny autor `created_by` → `users`. Brak autora oznacza wpis katalogowy bez przypisania do konta. |
| `workout_plans` / `WorkoutPlan` | Wymagany właściciel `owner_id` → `users`, nazwa (120 znaków), opcjonalny opis (2000 znaków), `is_shared` (domyślnie `false`), czas utworzenia. Pole udostępniania przygotowuje schemat pod S4; obecnie nie nadaje dostępu do planu. |
| `plan_days` / `PlanDay` | Wymagany plan, nazwa (120 znaków), `day_index` — pozycja dnia w planie, liczona od zera. To kolejność sesji, nie dzień tygodnia ani data kalendarzowa. |
| `plan_exercises` / `PlanExercise` | Wymagany dzień i ćwiczenie z katalogu, serie, powtórzenia, ciężar w kg i `order_index` — pozycja ćwiczenia w dniu, liczona od zera. |

Główne grupy mięśniowe zapisuje enum `MuscleGroup`: `CHEST`, `BACK`, `SHOULDERS`, `BICEPS`, `TRICEPS`, `FOREARMS`, `QUADRICEPS`, `HAMSTRINGS`, `GLUTES`, `CALVES`, `CORE`, `FULL_BODY`. Przy dodawaniu wartości enumów trzeba rozszerzyć ograniczenia bazy nową migracją.

## Katalog startowy — S2-06

Migracja `V4__seed_exercise_catalog.sql` dodaje **48 ćwiczeń** z polskimi nazwami, krótkimi opisami ruchu, typem i główną grupą mięśniową. Każda z 12 grup ma po 2 wpisy `STRENGTH` i 2 wpisy `HOME`, czyli łącznie 24 ćwiczenia każdego typu.

`STRENGTH` obejmuje warianty z typowym sprzętem siłowym: sztangą, hantlami, maszynami, wyciągiem lub kettlebell. `HOME` obejmuje warianty z masą ciała i prostym wyposażeniem; wymagane gumy, butelki, ręcznik lub drążek są wymienione w opisach. Jest to umowny podział wariantów według sprzętu. Grupa mięśniowa wskazuje główny obszar, a ćwiczenie może angażować również inne mięśnie. Wpisy opisują ruchy rozliczane powtórzeniami, zgodnie z obecnym modelem planu.

Katalog jest wspólny, z `created_by = NULL`, i nie wymaga konta demonstracyjnego. Identyfikatory generuje baza; aplikacja nie powinna zakładać konkretnych numerów ID. Flyway wykonuje V4 raz na bazę, więc ponowne uruchomienie migracji/aplikacji nie powiela wpisów. Migracja dodaje dane do istniejącego katalogu, zachowując dotychczasowe ćwiczenia i odwołania w planach. Kolejne zmiany danych należy wprowadzać nowymi migracjami.

## Integralność i kolejność

- Serie i powtórzenia muszą być dodatnie. Ciężar to `BigDecimal` / `NUMERIC(7,2)`, od `0` do `99999.99` kg; `0` oznacza brak dodatkowego obciążenia. Przyszłe DTO muszą odrzucać więcej niż dwa miejsca dziesiętne, zamiast polegać na zaokrągleniu przez bazę.
- Nazwy nie mogą być puste ani składać się ze zwykłych spacji. Właściciel planu i wszystkie relacje plan → dzień → wpis ćwiczenia są wymagane.
- Pozycje są nieujemne i unikalne w obrębie rodzica: `(plan_id, day_index)` oraz `(plan_day_id, order_index)`. Mogą mieć przerwy. Repozytoria zwracają dni i ćwiczenia w tej kolejności.
- To samo ćwiczenie można umieścić kilka razy w jednym dniu, na różnych pozycjach.
- Przy zmianie kolejności należy unikać chwilowych kolizji pozycji. Można najpierw przenieść elementy na wolne pozycje, wykonać flush, a następnie zapisać pozycje docelowe w tej samej transakcji.
- Indeksy wspierają odczyt planów właściciela, uporządkowanych dzieci oraz wyszukiwanie powiązań z katalogiem ćwiczeń.

Relacje do właściciela i katalogu to `ManyToOne` z `LAZY`, bez kaskadowania zapisu ani usunięcia w stronę konta/katalogu. Plan ma uporządkowaną kolekcję dni, a dzień — wpisów ćwiczeń, z `cascade=ALL`, `orphanRemoval` i `@OnDelete(CASCADE)`. Metody `addDay` / `removeDay` i `addExercise` / `removeExercise` synchronizują obie strony relacji; należy ich używać przy budowaniu i edycji planu. Odczyt kolejności przez repozytoria lub ponowne wczytanie kolekcji uwzględnia indeksy; samo dodawanie do kolekcji w pamięci zachowuje kolejność dodawania. Klucze obce bazy zapewniają kaskady także dla operacji SQL i usuwania konta. Po bezpośrednim usunięciu przez SQL wcześniej załadowane encje mogą być nieaktualne — nie należy ponownie ich używać bez odświeżenia/wyczyszczenia kontekstu JPA.

## Usuwanie danych

- Usunięcie konta usuwa jego plany, dni i wpisy ćwiczeń przez `ON DELETE CASCADE`.
- Usunięcie planu usuwa jego dni i wpisy; usunięcie dnia usuwa tylko jego wpisy.
- Katalog ćwiczeń pozostaje. Jeżeli usuwane konto było autorem ćwiczenia, `created_by` staje się `NULL`; ćwiczenie może nadal występować w planach innych osób.
- Usunięcie ćwiczenia używanego w planie jest blokowane przez klucz obcy `RESTRICT`.

Dotychczasowa ścieżka potwierdzania usunięcia konta korzysta z tych reguł w tej samej transakcji. Dodanie wyników treningów lub relacji trenera w S3 wymaga rozszerzenia zasad usuwania i testów o kolejne tabele.

## Weryfikacja

`WorkoutModelIntegrationTests` sprawdzają mapowanie JPA, kolejność, dokładność ciężaru, odczyt planów według właściciela, kaskady i ograniczenia bazy. `AccountSecurityIntegrationTests` dodatkowo sprawdzają potwierdzenie usunięcia konta posiadającego plan oraz zachowanie cudzego planu ze wspólnym ćwiczeniem.

`ExerciseCatalogIntegrationTests` sprawdzają kompletność i unikalność 48 nazw, odczyt polskich znaków, opisy, autorstwo, pokrycie wszystkich grup i obu typów oraz brak duplikacji przy kolejnym uruchomieniu Flyway. Test usunięcia konta zachowuje identyfikatory katalogu startowego. Testy modelu izolują własne dane transakcją wycofywaną po każdym scenariuszu.

Standardowy zestaw na H2: `cd backend` → `./mvnw.cmd test` (JDK 21).

Pełny zestaw na PostgreSQL 16 oraz sprawdzenie dodania V3 do bazy V2 z istniejącym kontem i tokenem, a następnie V4 do V3 z własnym ćwiczeniem i planem, z katalogu głównego repozytorium:

```powershell
pwsh -NoProfile -File tests/test-workout-postgres.ps1
```

Skrypt wymaga Dockera, pobiera obrazy PostgreSQL 16 i JDK 21 oraz zależności Maven. Tworzy tymczasowy kontener bazy bez publikowania portów i usuwa go po zakończeniu. Nie łączy się z bazą Compose. Raporty Maven zapisuje w `backend/target/surefire-reports`, a zależności przechowuje w wolumenie `aplikacja-trenerska-maven-cache`.

## Kolejne zadania

S2-05 obejmuje schemat, modele i repozytoria, a S2-06 — dane katalogu. API S2-07 jest wdrożone: [EXERCISES_API.md](EXERCISES_API.md) i [WORKOUT_PLANS_API.md](WORKOUT_PLANS_API.md). Serwis planów pobiera właściciela z uwierzytelnionej sesji i używa go przy odczycie, zapisie oraz usuwaniu; PUT zastępuje dzieci w jednej transakcji. Listy, szczegóły, kreator, edycja i usuwanie S2-08/09 są gotowe: [WORKOUT_FRONTEND.md](WORKOUT_FRONTEND.md). Kolejne wyniki treningów i moduł trenera pozostają w S3.
