package com.hackathon

import com.hackathon.config.buildSupabaseClient
import com.hackathon.config.configureErrorHandling
import com.hackathon.config.configureSerialization
import com.hackathon.repository.ItemRepository
import com.hackathon.routes.itemRoutes
import com.hackathon.service.ItemService
import io.ktor.server.application.Application
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging
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
