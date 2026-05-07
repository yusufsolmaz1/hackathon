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
