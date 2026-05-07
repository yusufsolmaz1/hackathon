package com.hackathon.routes

import com.hackathon.config.TrendException
import com.hackathon.config.requireUserId
import com.hackathon.model.LikeStatusRequest
import com.hackathon.repository.UserRepository
import com.hackathon.service.ProductService
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.productRoutes(service: ProductService, userRepo: UserRepository) {
    route("/products") {
        get {
            val userId = call.requireUserId(userRepo)
            call.respond(service.listAll(userId))
        }
        get("/search") {
            val userId = call.requireUserId(userRepo)
            val q = call.request.queryParameters["q"]
                ?: throw TrendException.badRequest("'q' query parametresi gereklidir.")
            call.respond(service.search(userId, q))
        }
        get("/favorites") {
            val userId = call.requireUserId(userRepo)
            call.respond(service.listFavorites(userId))
        }
        get("/{id}") {
            val userId = call.requireUserId(userRepo)
            val id = call.parameters["id"] ?: throw TrendException.badRequest("'id' gereklidir.")
            call.respond(service.getById(userId, id))
        }
        put("/{id}/favorite") {
            val userId = call.requireUserId(userRepo)
            val id = call.parameters["id"] ?: throw TrendException.badRequest("'id' gereklidir.")
            call.respond(service.toggleFavorite(userId, id))
        }
        put("/{id}/like") {
            val userId = call.requireUserId(userRepo)
            val id = call.parameters["id"] ?: throw TrendException.badRequest("'id' gereklidir.")
            val req = call.receive<LikeStatusRequest>()
            call.respond(service.setLike(userId, id, req))
        }
    }
}
