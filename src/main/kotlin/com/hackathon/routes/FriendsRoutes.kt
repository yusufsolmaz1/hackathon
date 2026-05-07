package com.hackathon.routes

import com.hackathon.config.ValidationException
import com.hackathon.model.AddFriendRequestDto
import com.hackathon.service.FriendsService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * Base path: /friends
 *
 *   GET    /friends
 *   POST   /friends/add
 *   DELETE /friends/{friendId}
 *
 * Tüm endpoint'ler X-User-Id header'ı ister (hackathon auth shim).
 */
fun Route.friendsRoutes(service: FriendsService) {
    route("/friends") {

        get {
            val userId = call.request.headers["X-User-Id"]
                ?: throw ValidationException("X-User-Id header required")
            call.respond(service.list(userId))
        }

        post("/add") {
            val userId = call.request.headers["X-User-Id"]
                ?: throw ValidationException("X-User-Id header required")
            val body = call.receive<AddFriendRequestDto>()
            call.respond(service.add(userId, body.email))
        }

        delete("/{friendId}") {
            val userId = call.request.headers["X-User-Id"]
                ?: throw ValidationException("X-User-Id header required")
            val friendId = call.parameters["friendId"] ?: throw ValidationException("friendId required")
            service.delete(userId, friendId)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
