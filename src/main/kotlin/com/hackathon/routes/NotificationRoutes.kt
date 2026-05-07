package com.hackathon.routes

import com.hackathon.config.TrendException
import com.hackathon.config.requireUserId
import com.hackathon.model.MessageResponse
import com.hackathon.repository.UserRepository
import com.hackathon.service.NotificationService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.notificationRoutes(service: NotificationService, userRepo: UserRepository) {
    route("/notifications") {
        get {
            val userId = call.requireUserId(userRepo)
            val after = call.request.queryParameters["after"]?.takeIf { it.isNotBlank() }
            call.respond(service.list(userId, after))
        }
        put("/{id}/read") {
            val userId = call.requireUserId(userRepo)
            val id = call.parameters["id"] ?: throw TrendException.badRequest("'id' gereklidir.")
            val msg = service.markRead(userId, id)
            call.respond(MessageResponse(msg))
        }
    }
}
