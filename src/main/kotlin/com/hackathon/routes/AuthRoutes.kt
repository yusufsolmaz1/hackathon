package com.hackathon.routes

import com.hackathon.config.requireUserId
import com.hackathon.model.LoginRequest
import com.hackathon.model.RegisterRequest
import com.hackathon.model.UpdateProfileRequest
import com.hackathon.repository.UserRepository
import com.hackathon.service.AuthService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.authRoutes(service: AuthService, userRepo: UserRepository) {
    route("/auth") {
        post("/login") {
            val req = call.receive<LoginRequest>()
            call.respond(service.login(req))
        }
        post("/register") {
            val req = call.receive<RegisterRequest>()
            call.respond(HttpStatusCode.Created, service.register(req))
        }
        get("/profile") {
            val userId = call.requireUserId(userRepo)
            call.respond(service.getProfile(userId))
        }
        put("/profile") {
            val userId = call.requireUserId(userRepo)
            val req = call.receive<UpdateProfileRequest>()
            call.respond(service.updateProfile(userId, req))
        }
    }
}
