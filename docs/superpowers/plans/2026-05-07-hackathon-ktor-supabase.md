# Hackathon Ktor + Supabase Skeleton Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Scaffold a runnable Ktor 3.x backend wired to Supabase via supabase-kt, with one example `items` CRUD resource demonstrating the full route → service → repository pipeline.

**Architecture:** Layered (routes → service → repository → model), manual constructor injection in `Application.kt`, no DI framework. Single example resource `items` proves the round-trip; copy-paste pattern for new resources during the hackathon.

**Tech Stack:** Kotlin 2.0.21, JDK 17, Ktor 3.0.3 (Netty), supabase-kt 3.0.2 (Postgrest), Ktor CIO HTTP engine, kotlinx.serialization 1.7.3, Logback 1.5.12, JUnit 5, Gradle 8.10 Kotlin DSL.

**Working directory:** `/Users/yusuf.solmaz1/hackathon` (git repo already exists with the design spec committed). All paths below are relative to this directory.

---

## File Structure

| File | Responsibility |
|---|---|
| `settings.gradle.kts` | Project name |
| `build.gradle.kts` | Plugins, deps, application config |
| `gradle.properties` | Kotlin/Ktor versions |
| `.gitignore` | Ignore build, IDE, env files |
| `.env.example` | Template for SUPABASE_URL / SUPABASE_KEY |
| `README.md` | Setup, run, sample curls |
| `src/main/resources/application.conf` | Ktor server config (HOCON) |
| `src/main/resources/logback.xml` | Logback config |
| `src/main/kotlin/com/hackathon/Application.kt` | Entry point, manual wiring |
| `src/main/kotlin/com/hackathon/config/Serialization.kt` | ContentNegotiation install |
| `src/main/kotlin/com/hackathon/config/SupabaseClient.kt` | supabase-kt client factory |
| `src/main/kotlin/com/hackathon/config/ErrorHandling.kt` | StatusPages + domain exceptions |
| `src/main/kotlin/com/hackathon/model/Item.kt` | `Item` data class |
| `src/main/kotlin/com/hackathon/repository/ItemRepository.kt` | Postgrest calls |
| `src/main/kotlin/com/hackathon/service/ItemService.kt` | Business logic |
| `src/main/kotlin/com/hackathon/routes/ItemRoutes.kt` | HTTP routes for `/items` |
| `src/test/kotlin/com/hackathon/HealthRouteTest.kt` | Health smoke test |
| `src/test/kotlin/com/hackathon/ItemRoutesTest.kt` | Item routes smoke test |

---

## Task 1: Gradle build configuration

**Files:**
- Create: `settings.gradle.kts`
- Create: `gradle.properties`
- Create: `build.gradle.kts`
- Create: `.gitignore`

- [ ] **Step 1: Create `settings.gradle.kts`**

```kotlin
rootProject.name = "hackathon"
```

- [ ] **Step 2: Create `gradle.properties`**

```properties
kotlin.code.style=official
kotlin_version=2.0.21
ktor_version=3.0.3
supabase_version=3.0.2
logback_version=1.5.12
serialization_version=1.7.3
```

- [ ] **Step 3: Create `build.gradle.kts`**

```kotlin
val kotlin_version: String by project
val ktor_version: String by project
val supabase_version: String by project
val logback_version: String by project
val serialization_version: String by project

plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    id("io.ktor.plugin") version "3.0.3"
}

group = "com.hackathon"
version = "0.0.1"

application {
    mainClass.set("com.hackathon.ApplicationKt")
}

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // Ktor server
    implementation("io.ktor:ktor-server-core:$ktor_version")
    implementation("io.ktor:ktor-server-netty:$ktor_version")
    implementation("io.ktor:ktor-server-content-negotiation:$ktor_version")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktor_version")
    implementation("io.ktor:ktor-server-status-pages:$ktor_version")
    implementation("io.ktor:ktor-server-call-logging:$ktor_version")
    implementation("io.ktor:ktor-server-config-yaml:$ktor_version")

    // Supabase
    implementation(platform("io.github.jan-tennert.supabase:bom:$supabase_version"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.ktor:ktor-client-cio:$ktor_version")

    // kotlinx.serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serialization_version")

    // Logging
    implementation("ch.qos.logback:logback-classic:$logback_version")

    // Testing
    testImplementation("io.ktor:ktor-server-test-host:$ktor_version")
    testImplementation("io.ktor:ktor-client-content-negotiation:$ktor_version")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:$kotlin_version")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
```

- [ ] **Step 4: Create `.gitignore`**

```gitignore
# Build
.gradle/
build/
out/

# IDE
.idea/
*.iml
*.iws
*.ipr
.vscode/

# OS
.DS_Store

# Env
.env
.env.local

# Logs
*.log
```

- [ ] **Step 5: Verify Gradle wrapper resolves dependencies**

Run: `gradle wrapper --gradle-version 8.10 && ./gradlew dependencies --configuration runtimeClasspath -q | head -30`
Expected: Lists Ktor, Supabase, kotlinx-serialization in the resolved classpath. No errors.

- [ ] **Step 6: Commit**

```bash
git add settings.gradle.kts gradle.properties build.gradle.kts .gitignore gradle/ gradlew gradlew.bat
git commit -m "build: add Gradle config with Ktor 3 and supabase-kt"
```

---

## Task 2: Logback and HOCON config

**Files:**
- Create: `src/main/resources/logback.xml`
- Create: `src/main/resources/application.conf`
- Create: `.env.example`

- [ ] **Step 1: Create `src/main/resources/logback.xml`**

```xml
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>%d{HH:mm:ss.SSS} [%thread] %-5level %logger{36} - %msg%n</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="STDOUT"/>
    </root>

    <logger name="io.ktor" level="INFO"/>
    <logger name="com.hackathon" level="DEBUG"/>
</configuration>
```

- [ ] **Step 2: Create `src/main/resources/application.conf`**

```hocon
ktor {
    deployment {
        port = 8080
        port = ${?PORT}
    }
    application {
        modules = [ com.hackathon.ApplicationKt.module ]
    }
}

supabase {
    url = ${?SUPABASE_URL}
    key = ${?SUPABASE_KEY}
}
```

- [ ] **Step 3: Create `.env.example`**

```bash
# Copy to .env and fill in
SUPABASE_URL=https://your-project.supabase.co
SUPABASE_KEY=your-anon-key
PORT=8080
```

- [ ] **Step 4: Commit**

```bash
git add src/main/resources/logback.xml src/main/resources/application.conf .env.example
git commit -m "config: add Logback, HOCON, and env template"
```

---

## Task 3: Domain model

**Files:**
- Create: `src/main/kotlin/com/hackathon/model/Item.kt`

- [ ] **Step 1: Create `src/main/kotlin/com/hackathon/model/Item.kt`**

```kotlin
package com.hackathon.model

import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val id: String? = null,
    val name: String,
    val description: String? = null,
)
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin -q`
Expected: BUILD SUCCESSFUL with no errors.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/hackathon/model/Item.kt
git commit -m "model: add Item data class"
```

---

## Task 4: Error handling and domain exceptions

**Files:**
- Create: `src/main/kotlin/com/hackathon/config/ErrorHandling.kt`

- [ ] **Step 1: Create `src/main/kotlin/com/hackathon/config/ErrorHandling.kt`**

```kotlin
package com.hackathon.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

class NotFoundException(message: String) : RuntimeException(message)
class ValidationException(message: String) : RuntimeException(message)

@Serializable
data class ErrorResponse(val error: String, val code: String)

fun Application.configureErrorHandling() {
    val logger = LoggerFactory.getLogger("ErrorHandling")
    install(StatusPages) {
        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(cause.message ?: "Not found", "NOT_FOUND"),
            )
        }
        exception<ValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(cause.message ?: "Bad request", "VALIDATION"),
            )
        }
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Internal server error", "INTERNAL"),
            )
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/hackathon/config/ErrorHandling.kt
git commit -m "config: add StatusPages error handling and domain exceptions"
```

---

## Task 5: Serialization config

**Files:**
- Create: `src/main/kotlin/com/hackathon/config/Serialization.kt`

- [ ] **Step 1: Create `src/main/kotlin/com/hackathon/config/Serialization.kt`**

```kotlin
package com.hackathon.config

import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import kotlinx.serialization.json.Json

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json(
            Json {
                prettyPrint = true
                isLenient = true
                ignoreUnknownKeys = true
            },
        )
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/hackathon/config/Serialization.kt
git commit -m "config: add JSON content negotiation"
```

---

## Task 6: Supabase client factory

**Files:**
- Create: `src/main/kotlin/com/hackathon/config/SupabaseClient.kt`

- [ ] **Step 1: Create `src/main/kotlin/com/hackathon/config/SupabaseClient.kt`**

```kotlin
package com.hackathon.config

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest

fun buildSupabaseClient(): SupabaseClient {
    val url = System.getenv("SUPABASE_URL")
        ?: error("SUPABASE_URL environment variable is required")
    val key = System.getenv("SUPABASE_KEY")
        ?: error("SUPABASE_KEY environment variable is required")

    return createSupabaseClient(supabaseUrl = url, supabaseKey = key) {
        install(Postgrest)
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/hackathon/config/SupabaseClient.kt
git commit -m "config: add Supabase client factory"
```

---

## Task 7: Item repository

**Files:**
- Create: `src/main/kotlin/com/hackathon/repository/ItemRepository.kt`

- [ ] **Step 1: Create `src/main/kotlin/com/hackathon/repository/ItemRepository.kt`**

```kotlin
package com.hackathon.repository

import com.hackathon.model.Item
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

class ItemRepository(private val supabase: SupabaseClient) {

    private val table = "items"

    suspend fun listAll(): List<Item> =
        supabase.from(table).select().decodeList()

    suspend fun findById(id: String): Item? =
        supabase.from(table)
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull()

    suspend fun create(item: Item): Item =
        supabase.from(table)
            .insert(item) { select() }
            .decodeSingle()

    suspend fun update(id: String, item: Item): Item? =
        supabase.from(table)
            .update(item) {
                select()
                filter { eq("id", id) }
            }
            .decodeSingleOrNull()

    suspend fun delete(id: String): Boolean {
        val deleted = supabase.from(table)
            .delete {
                select()
                filter { eq("id", id) }
            }
            .decodeList<Item>()
        return deleted.isNotEmpty()
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/hackathon/repository/ItemRepository.kt
git commit -m "repo: add ItemRepository with Postgrest CRUD"
```

---

## Task 8: Item service

**Files:**
- Create: `src/main/kotlin/com/hackathon/service/ItemService.kt`

- [ ] **Step 1: Create `src/main/kotlin/com/hackathon/service/ItemService.kt`**

```kotlin
package com.hackathon.service

import com.hackathon.config.NotFoundException
import com.hackathon.config.ValidationException
import com.hackathon.model.Item
import com.hackathon.repository.ItemRepository

class ItemService(private val repository: ItemRepository) {

    suspend fun listAll(): List<Item> = repository.listAll()

    suspend fun getById(id: String): Item =
        repository.findById(id) ?: throw NotFoundException("Item $id not found")

    suspend fun create(item: Item): Item {
        if (item.name.isBlank()) throw ValidationException("name must not be blank")
        return repository.create(item.copy(id = null))
    }

    suspend fun update(id: String, item: Item): Item =
        repository.update(id, item.copy(id = null))
            ?: throw NotFoundException("Item $id not found")

    suspend fun delete(id: String) {
        val ok = repository.delete(id)
        if (!ok) throw NotFoundException("Item $id not found")
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/hackathon/service/ItemService.kt
git commit -m "service: add ItemService with validation and not-found handling"
```

---

## Task 9: Item routes

**Files:**
- Create: `src/main/kotlin/com/hackathon/routes/ItemRoutes.kt`

- [ ] **Step 1: Create `src/main/kotlin/com/hackathon/routes/ItemRoutes.kt`**

```kotlin
package com.hackathon.routes

import com.hackathon.config.ValidationException
import com.hackathon.model.Item
import com.hackathon.service.ItemService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.itemRoutes(service: ItemService) {
    route("/items") {
        get {
            call.respond(service.listAll())
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("id required")
            call.respond(service.getById(id))
        }

        post {
            val item = call.receive<Item>()
            call.respond(HttpStatusCode.Created, service.create(item))
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("id required")
            val item = call.receive<Item>()
            call.respond(service.update(id, item))
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("id required")
            service.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/hackathon/routes/ItemRoutes.kt
git commit -m "routes: add /items CRUD endpoints"
```

---

## Task 10: Application entry point and wiring

**Files:**
- Create: `src/main/kotlin/com/hackathon/Application.kt`

- [ ] **Step 1: Create `src/main/kotlin/com/hackathon/Application.kt`**

```kotlin
package com.hackathon

import com.hackathon.config.buildSupabaseClient
import com.hackathon.config.configureErrorHandling
import com.hackathon.config.configureSerialization
import com.hackathon.repository.ItemRepository
import com.hackathon.routes.itemRoutes
import com.hackathon.service.ItemService
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(CallLogging)
    configureSerialization()
    configureErrorHandling()

    val supabase = buildSupabaseClient()
    val itemRepository = ItemRepository(supabase)
    val itemService = ItemService(itemRepository)

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        itemRoutes(itemService)
    }
}
```

- [ ] **Step 2: Verify compilation**

Run: `./gradlew compileKotlin -q`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add src/main/kotlin/com/hackathon/Application.kt
git commit -m "app: add entry point with manual wiring"
```

---

## Task 11: Health route smoke test

**Files:**
- Create: `src/test/kotlin/com/hackathon/HealthRouteTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.hackathon

import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.install
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class HealthRouteTest {

    @Test
    fun `GET health returns 200 with status ok`() = testApplication {
        application {
            routing {
                get("/health") {
                    call.respond(mapOf("status" to "ok"))
                }
            }
        }

        val response = client.get("/health")
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains("\"status\""))
        assertTrue(response.bodyAsText().contains("\"ok\""))
    }
}
```

- [ ] **Step 2: Run test to verify it passes (this test isolates the route logic)**

Run: `./gradlew test --tests com.hackathon.HealthRouteTest -q`
Expected: BUILD SUCCESSFUL, 1 test passing.

- [ ] **Step 3: Commit**

```bash
git add src/test/kotlin/com/hackathon/HealthRouteTest.kt
git commit -m "test: add health route smoke test"
```

---

## Task 12: Item routes smoke test

**Files:**
- Create: `src/test/kotlin/com/hackathon/ItemRoutesTest.kt`

- [ ] **Step 1: Write the test (uses a fake repository to avoid hitting Supabase)**

```kotlin
package com.hackathon

import com.hackathon.config.ErrorResponse
import com.hackathon.config.configureErrorHandling
import com.hackathon.config.configureSerialization
import com.hackathon.model.Item
import com.hackathon.repository.ItemRepository
import com.hackathon.routes.itemRoutes
import com.hackathon.service.ItemService
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Uses a hand-rolled fake repository by subclassing.
 * Avoids hitting real Supabase during unit tests.
 *
 * TODO: Add integration tests against a real Supabase test project.
 */
private class FakeItemRepository : ItemRepository(
    createSupabaseClient("https://stub.invalid", "stub") { install(Postgrest) },
) {
    private val store = mutableMapOf<String, Item>()
    private var counter = 0

    override suspend fun listAll(): List<Item> = store.values.toList()
    override suspend fun findById(id: String): Item? = store[id]
    override suspend fun create(item: Item): Item {
        val id = (++counter).toString()
        val saved = item.copy(id = id)
        store[id] = saved
        return saved
    }
    override suspend fun update(id: String, item: Item): Item? {
        if (id !in store) return null
        val updated = item.copy(id = id)
        store[id] = updated
        return updated
    }
    override suspend fun delete(id: String): Boolean = store.remove(id) != null
}

class ItemRoutesTest {

    @Test
    fun `GET non-existent item returns 404`() = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            val service = ItemService(FakeItemRepository())
            routing { itemRoutes(service) }
        }

        val response = client.get("/items/does-not-exist")
        assertEquals(HttpStatusCode.NotFound, response.status)
    }

    @Test
    fun `POST then GET round-trips an item`() = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            val service = ItemService(FakeItemRepository())
            routing { itemRoutes(service) }
        }

        val httpClient = createClient {
            install(ContentNegotiation) { json() }
        }

        val created: Item = httpClient.post("/items") {
            contentType(ContentType.Application.Json)
            setBody(Item(name = "test", description = "hello"))
        }.body()

        assertEquals("test", created.name)
        assertEquals("hello", created.description)

        val fetched: Item = httpClient.get("/items/${created.id}").body()
        assertEquals(created, fetched)
    }

    @Test
    fun `POST with blank name returns 400`() = testApplication {
        application {
            configureSerialization()
            configureErrorHandling()
            val service = ItemService(FakeItemRepository())
            routing { itemRoutes(service) }
        }

        val httpClient = createClient {
            install(ContentNegotiation) { json() }
        }

        val response = httpClient.post("/items") {
            contentType(ContentType.Application.Json)
            setBody(Item(name = "", description = null))
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        val err: ErrorResponse = response.body()
        assertEquals("VALIDATION", err.code)
    }
}
```

- [ ] **Step 2: Make `ItemRepository` methods open so the fake can override them**

Modify `src/main/kotlin/com/hackathon/repository/ItemRepository.kt` line 1: change `class ItemRepository` to `open class ItemRepository`, and prepend `open` to each suspend function (`open suspend fun listAll`, `open suspend fun findById`, `open suspend fun create`, `open suspend fun update`, `open suspend fun delete`).

The full file becomes:

```kotlin
package com.hackathon.repository

import com.hackathon.model.Item
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

open class ItemRepository(private val supabase: SupabaseClient) {

    private val table = "items"

    open suspend fun listAll(): List<Item> =
        supabase.from(table).select().decodeList()

    open suspend fun findById(id: String): Item? =
        supabase.from(table)
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull()

    open suspend fun create(item: Item): Item =
        supabase.from(table)
            .insert(item) { select() }
            .decodeSingle()

    open suspend fun update(id: String, item: Item): Item? =
        supabase.from(table)
            .update(item) {
                select()
                filter { eq("id", id) }
            }
            .decodeSingleOrNull()

    open suspend fun delete(id: String): Boolean {
        val deleted = supabase.from(table)
            .delete {
                select()
                filter { eq("id", id) }
            }
            .decodeList<Item>()
        return deleted.isNotEmpty()
    }
}
```

- [ ] **Step 3: Run tests**

Run: `./gradlew test --tests com.hackathon.ItemRoutesTest -q`
Expected: BUILD SUCCESSFUL, 3 tests passing.

- [ ] **Step 4: Commit**

```bash
git add src/test/kotlin/com/hackathon/ItemRoutesTest.kt src/main/kotlin/com/hackathon/repository/ItemRepository.kt
git commit -m "test: add Item routes smoke tests with fake repository"
```

---

## Task 13: README

**Files:**
- Create: `README.md`

- [ ] **Step 1: Create `README.md`**

````markdown
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
````

- [ ] **Step 2: Commit**

```bash
git add README.md
git commit -m "docs: add README with setup, API, and conventions"
```

---

## Task 14: Final build verification

- [ ] **Step 1: Run full build**

Run: `./gradlew build -q`
Expected: BUILD SUCCESSFUL. All tests pass. No warnings beyond Kotlin/Gradle defaults.

- [ ] **Step 2: Verify project tree**

Run: `find . -type f -not -path './.git/*' -not -path './.gradle/*' -not -path './build/*' -not -path './gradle/wrapper/*' | sort`
Expected: All files from the File Structure table are present.

- [ ] **Step 3: Final commit (if any leftover changes)**

```bash
git status
# If clean, no commit needed. Otherwise:
# git add . && git commit -m "chore: final cleanup"
```

---

## Self-Review Notes

**Spec coverage:**
- Project layout (spec §3) → Tasks 1-10 + file structure table ✓
- Stack (spec §4) → Task 1 build.gradle.kts ✓
- Data model (spec §5) → Task 3 ✓
- All 6 endpoints (spec §6) → Tasks 9 + 10 ✓
- Env-var config (spec §7) → Tasks 2 + 6 ✓
- Layered architecture + manual wiring (spec §8) → Tasks 7-10 ✓
- StatusPages + uniform error JSON (spec §9) → Task 4 ✓
- testApplication smoke tests (spec §10) → Tasks 11-12 ✓
- README content (spec §11) → Task 13 ✓
- Acceptance criteria (spec §12) → Task 14 ✓

**Type consistency check:**
- `Item` signature consistent across model, repo, service, routes, tests ✓
- `ItemRepository` made `open` in Task 12 to support test fake — caught and inlined ✓
- `NotFoundException` / `ValidationException` defined in `config/ErrorHandling.kt`, imported by service and routes ✓
- `ErrorResponse` defined in `config/ErrorHandling.kt`, used in test ✓
- `configureSerialization` / `configureErrorHandling` / `buildSupabaseClient` / `itemRoutes` names consistent ✓

**Placeholder scan:** No TBD/TODO in plan steps. One `TODO` comment intentionally placed in test file as per spec §10. ✓

**Open issue:** None.
