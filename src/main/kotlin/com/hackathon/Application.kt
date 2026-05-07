package com.hackathon

import com.hackathon.config.buildSupabaseClient
import com.hackathon.config.configureErrorHandling
import com.hackathon.config.configureSerialization
import com.hackathon.repository.FriendsRepository
import com.hackathon.repository.ItemRepository
import com.hackathon.repository.OrderRepository
import com.hackathon.repository.SplitPaymentRepository
import com.hackathon.routes.friendsRoutes
import com.hackathon.routes.itemRoutes
import com.hackathon.routes.orderRoutes
import com.hackathon.routes.splitPaymentRoutes
import com.hackathon.service.FriendsService
import com.hackathon.service.ItemService
import com.hackathon.service.OrderService
import com.hackathon.service.SplitPaymentService
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

    // Items (mevcut)
    val itemRepository = ItemRepository(supabase)
    val itemService = ItemService(itemRepository)

    // Friends
    val friendsRepository = FriendsRepository(supabase)
    val friendsService = FriendsService(friendsRepository)

    // Orders
    val orderRepository = OrderRepository(supabase)
    val orderService = OrderService(orderRepository)

    // Split Payment
    val splitRepository = SplitPaymentRepository(supabase)
    val splitService = SplitPaymentService(splitRepository, friendsRepository, orderRepository)

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        itemRoutes(itemService)
        friendsRoutes(friendsService)
        orderRoutes(orderService)
        splitPaymentRoutes(splitService)
    }
}
