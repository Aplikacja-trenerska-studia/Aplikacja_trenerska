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
├── workout/                # Sprint 2+
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
