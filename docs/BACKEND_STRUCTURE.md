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
