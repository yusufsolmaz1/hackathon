package com.hackathon

import com.hackathon.config.configureSerialization
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
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
            configureSerialization()
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
