# Hackathon — Ktor + Supabase Backend Skeleton

A minimal Ktor 3 backend wired to Supabase via [supabase-kt](https://github.com/supabase-community/supabase-kt). Ships with one example resource (`items`) demonstrating the route → service → repository pattern.

## Prerequisites

- JDK 17
- A Supabase project ([create one](https://supabase.com))

## Setup

### 1. Create the `items` table in Supabase

In Supabase SQL Editor, run:

```sql
create table items (
  id uuid primary key default gen_random_uuid(),
  name text not null,
  description text,
  created_at timestamptz default now()
);

-- For the skeleton, allow the anon key to read/write.
-- Lock this down before going to production.
alter table items enable row level security;
create policy "anon read"  on items for select using (true);
create policy "anon write" on items for insert with check (true);
create policy "anon update" on items for update using (true);
create policy "anon delete" on items for delete using (true);
```

### 2. Configure environment

```bash
cp .env.example .env
# Edit .env and fill in SUPABASE_URL and SUPABASE_KEY (anon key)
```

Then export them in your shell before running:

```bash
export $(cat .env | xargs)
```

### 3. Run

```bash
./gradlew run
```

Server listens on `http://localhost:8080`.

## API

### Health

```bash
curl localhost:8080/health
# {"status":"ok"}
```

### Items

```bash
# List
curl localhost:8080/items

# Get one
curl localhost:8080/items/<uuid>

# Create
curl -X POST localhost:8080/items \
  -H 'Content-Type: application/json' \
  -d '{"name":"first item","description":"hello"}'

# Update
curl -X PUT localhost:8080/items/<uuid> \
  -H 'Content-Type: application/json' \
  -d '{"name":"updated","description":"new"}'

# Delete
curl -X DELETE localhost:8080/items/<uuid>
```

## Tests

```bash
./gradlew test
```

## Adding a new resource

Copy the `Item*` pattern:

1. `model/Foo.kt` — data class
2. `repository/FooRepository.kt` — Postgrest CRUD
3. `service/FooService.kt` — business logic
4. `routes/FooRoutes.kt` — HTTP routes
5. Wire it in `Application.kt`

## Project layout

```
src/main/kotlin/com/hackathon/
├── Application.kt           # entry point + wiring
├── config/                  # Supabase client, JSON, error handling
├── model/                   # data classes
├── repository/              # Supabase calls
├── service/                 # business logic
└── routes/                  # HTTP routes
```
