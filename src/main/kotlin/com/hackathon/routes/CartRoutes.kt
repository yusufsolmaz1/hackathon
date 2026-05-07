package com.hackathon.routes

import com.hackathon.config.TrendException
import com.hackathon.config.requireUserId
import com.hackathon.model.AddCartItemRequest
import com.hackathon.model.UpdateCartItemRequest
import com.hackathon.repository.UserRepository
import com.hackathon.service.CartService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.cartRoutes(service: CartService, userRepo: UserRepository) {
    route("/cart") {
        get {
            val userId = call.requireUserId(userRepo)
            call.respond(service.list(userId))
        }
        route("/items") {
            post {
                val userId = call.requireUserId(userRepo)
                val req = call.receive<AddCartItemRequest>()
                call.respond(HttpStatusCode.Created, service.add(userId, req))
            }
            put("/{id}") {
                val userId = call.requireUserId(userRepo)
                val id = call.parameters["id"] ?: throw TrendException.badRequest("'id' gereklidir.")
                val req = call.receive<UpdateCartItemRequest>()
                call.respond(service.updateQuantity(userId, id, req))
            }
            delete("/{id}") {
                val userId = call.requireUserId(userRepo)
                val id = call.parameters["id"] ?: throw TrendException.badRequest("'id' gereklidir.")
                service.remove(userId, id)
                call.respond(HttpStatusCode.NoContent)
            }
        }
    }
}
