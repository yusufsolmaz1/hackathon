package com.hackathon

import com.hackathon.config.buildSupabaseClient
import com.hackathon.config.configureErrorHandling
import com.hackathon.config.configureSerialization
import com.hackathon.repository.CartRepository
import com.hackathon.repository.CollectionRepository
import com.hackathon.repository.FriendRepository
import com.hackathon.repository.OrderRepository
import com.hackathon.repository.ProductRepository
import com.hackathon.repository.UserRepository
import com.hackathon.routes.authRoutes
import com.hackathon.routes.cartRoutes
import com.hackathon.routes.collectionRoutes
import com.hackathon.routes.friendRoutes
import com.hackathon.routes.orderRoutes
import com.hackathon.routes.productRoutes
import com.hackathon.service.AuthService
import com.hackathon.service.CartService
import com.hackathon.service.CollectionService
import com.hackathon.service.FriendService
import com.hackathon.service.OrderService
import com.hackathon.service.ProductService
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.netty.EngineMain
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun main(args: Array<String>) = EngineMain.main(args)

fun Application.module() {
    install(CallLogging)
    configureSerialization()
    configureErrorHandling()

    val supabase = buildSupabaseClient()

    // Repositories
    val userRepo = UserRepository(supabase)
    val productRepo = ProductRepository(supabase)
    val friendRepo = FriendRepository(supabase)
    val collectionRepo = CollectionRepository(supabase)
    val cartRepo = CartRepository(supabase)
    val orderRepo = OrderRepository(supabase)

    // Services
    val authService = AuthService(userRepo)
    val productService = ProductService(productRepo)
    val friendService = FriendService(friendRepo, userRepo)
    val collectionService = CollectionService(collectionRepo, productRepo, userRepo, productService)
    val cartService = CartService(cartRepo, productRepo)
    val orderService = OrderService(orderRepo, cartRepo, friendRepo, userRepo)

    routing {
        get("/health") {
            call.respond(mapOf("status" to "ok"))
        }
        authRoutes(authService, userRepo)
        productRoutes(productService, userRepo)
        friendRoutes(friendService, userRepo)
        collectionRoutes(collectionService, userRepo)
        cartRoutes(cartService, userRepo)
        orderRoutes(orderService, userRepo)
    }
}
