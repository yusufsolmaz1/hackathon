# Hackathon — Ktor + Supabase Backend Skeleton

**Date:** 2026-05-07
**Owner:** yusuf.solmaz1
**Status:** Approved

## 1. Purpose

A minimal, production-style Ktor backend skeleton wired to Supabase (Postgrest only) so that during a hackathon the team can rapidly add domain features without spending time on boilerplate. The skeleton ships with one example resource (`items`) demonstrating the full request → service → repository → Supabase round-trip.

## 2. Scope

**In scope:**
- Ktor 3.x JVM server with Netty
- supabase-kt SDK with Postgrest module
- Layered architecture: routes → service → repository → model
- One example CRUD resource (`items`)
- Health check endpoint
- Centralized error handling via `StatusPages`
- Environment-variable-based configuration
- Smoke tests with Ktor `testApplication`
- README with run instructions and Supabase table SQL

**Out of scope (YAGNI):**
- Supabase Auth, Storage, Realtime modules
- Dependency injection framework (Koin)
- Dockerfile / containerization
- Database migration tooling
- CI/CD configuration
- Production observability (metrics, tracing)

## 3. Project Layout

```
/Users/yusuf.solmaz1/hackathon/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── .env.example
├── .gitignore
├── README.md
└── src/
    ├── main/
    │   ├── kotlin/com/hackathon/
    │   │   ├── Application.kt
    │   │   ├── config/
    │   │   │   ├── SupabaseClient.kt
    │   │   │   └── Serialization.kt
    │   │   ├── routes/
    │   │   │   └── ItemRoutes.kt
    │   │   ├── service/
    │   │   │   └── ItemService.kt
    │   │   ├── repository/
    │   │   │   └── ItemRepository.kt
    │   │   └── model/
    │   │       └── Item.kt
    │   └── resources/
    │       ├── application.conf
    │       └── logback.xml
    └── test/
        └── kotlin/com/hackathon/
            └── ItemRoutesTest.kt
```

## 4. Stack

| Layer | Choice | Version |
|---|---|---|
| Language | Kotlin | 2.0.21 |
| JVM target | JDK | 17 |
| Server framework | Ktor | 3.0.x |
| Engine | Netty | (Ktor default) |
| Supabase client | supabase-kt | BOM 3.x |
| Postgrest module | supabase-kt postgrest-kt | (via BOM) |
| HTTP engine for supabase-kt | Ktor CIO | 3.0.x |
| Serialization | kotlinx.serialization JSON | 1.7.x |
| Logging | Logback Classic | 1.5.x |
| Test framework | JUnit 5 + Ktor testApplication | latest stable |
| Build | Gradle Kotlin DSL | 8.x |

## 5. Data Model

```kotlin
@Serializable
data class Item(
    val id: String? = null,        // Supabase generates UUID
    val name: String,
    val description: String? = null
)
```

**Supabase table SQL (in README):**
```sql
create table items (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  description text,
  created_at timestamptz default now()
);
```

## 6. Endpoints

| Method | Path | Body | Response | Notes |
|---|---|---|---|---|
| GET | `/health` | — | `{"status":"ok"}` | Liveness check |
| GET | `/items` | — | `Item[]` | List all |
| GET | `/items/{id}` | — | `Item` | 404 if not found |
| POST | `/items` | `Item` (no id) | `Item` (with id) | 201 Created |
| PUT | `/items/{id}` | `Item` | `Item` | Full update |
| DELETE | `/items/{id}` | — | 204 No Content | |

## 7. Configuration

- Environment variables read via `System.getenv`:
  - `SUPABASE_URL` — project URL (e.g. `https://xxx.supabase.co`)
  - `SUPABASE_KEY` — anon key for skeleton (service role only if backend bypasses RLS)
  - `PORT` — optional, defaults to 8080
- `.env.example` is committed; `.env` is git-ignored
- `application.conf` (HOCON) maps env vars and Ktor server settings
- App fails fast at startup if required env vars are missing

## 8. Architecture & Layers

**Request flow:**
```
HTTP request
   → Routes (parse path/body, return HTTP)
   → Service (orchestration, validation)
   → Repository (supabase-kt postgrest calls)
   → Supabase
```

**Wiring:** Manual constructor injection in `Application.kt`. No DI framework. Each layer takes its dependencies as constructor parameters; routes receive the service instance.

**Why layered (not flat):** Repository can be swapped/mocked in tests. Service is the place to add business logic during the hackathon without touching routes or repository. Cost is ~4 extra files for the example resource.

## 9. Error Handling

- `StatusPages` plugin installed once in `Application.kt`
- Domain exceptions defined in service layer:
  - `NotFoundException` → HTTP 404
  - `ValidationException` → HTTP 400
- Repository wraps `RestException` (from supabase-kt) into domain exceptions
- Unhandled exceptions → HTTP 500 with generic JSON body, full stack trace logged via Logback
- All error responses use uniform JSON shape: `{"error": "<message>", "code": "<code>"}`

## 10. Testing Strategy

- `ItemRoutesTest.kt` uses `testApplication` to test:
  - GET `/health` returns 200 with expected body
  - GET `/items/{id}` with non-existent id returns 404
- Repository is **not** unit-tested in the skeleton (would require a Supabase test instance or HTTP mock); a `// TODO` comment marks where to add integration tests
- Test goal: prove the wiring works end-to-end inside the JVM, not full coverage

## 11. README Content

The README must include:
1. Prerequisites (JDK 17, Supabase project)
2. Supabase table creation SQL
3. `.env` setup instructions
4. `./gradlew run` command
5. Sample `curl` commands for each endpoint
6. Where to add new resources (which files to copy from `Item*` pattern)

## 12. Acceptance Criteria

- [ ] `./gradlew build` succeeds with no warnings beyond Kotlin/Gradle defaults
- [ ] `./gradlew run` starts the server on port 8080
- [ ] `GET /health` returns `200 {"status":"ok"}`
- [ ] With valid Supabase credentials and `items` table created, full CRUD round-trip works against real Supabase
- [ ] `./gradlew test` passes
- [ ] README is sufficient for a new contributor to run the project from scratch

## 13. Open Questions

None — all resolved during brainstorming:
- Project type: skeleton only
- Supabase modules: Postgrest only
- Architecture: layered
- Supabase client: supabase-kt SDK
- Path: `/Users/yusuf.solmaz1/hackathon/`
- Package: `com.hackathon`
