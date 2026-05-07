package com.hackathon.routes

import com.hackathon.config.TrendException
import com.hackathon.config.requireUserId
import com.hackathon.model.AddFriendRequest
import com.hackathon.model.SyncFriendsRequest
import com.hackathon.repository.UserRepository
import com.hackathon.service.FriendService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

fun Route.friendRoutes(service: FriendService, userRepo: UserRepository) {
    route("/friends") {
        get {
            val userId = call.requireUserId(userRepo)
            call.respond(service.list(userId))
        }
        post {
            val userId = call.requireUserId(userRepo)
            val req = call.receive<AddFriendRequest>()
            call.respond(HttpStatusCode.Created, service.add(userId, req))
        }
        delete("/{id}") {
            val userId = call.requireUserId(userRepo)
            val id = call.parameters["id"] ?: throw TrendException.badRequest("'id' gereklidir.")
            service.remove(userId, id)
            call.respond(HttpStatusCode.NoContent)
        }
        post("/sync") {
            val userId = call.requireUserId(userRepo)
            val req = call.receive<SyncFriendsRequest>()
            call.respond(service.sync(userId, req))
        }
    }
}
