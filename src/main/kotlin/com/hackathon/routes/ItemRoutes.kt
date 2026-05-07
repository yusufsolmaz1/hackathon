package com.hackathon.routes

import com.hackathon.config.ValidationException
import com.hackathon.model.Item
import com.hackathon.service.ItemService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.delete
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.itemRoutes(service: ItemService) {
    route("/items") {
        get {
            call.respond(service.listAll())
        }

        get("/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("id required")
            call.respond(service.getById(id))
        }

        post {
            val item = call.receive<Item>()
            call.respond(HttpStatusCode.Created, service.create(item))
        }

        put("/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("id required")
            val item = call.receive<Item>()
            call.respond(service.update(id, item))
        }

        delete("/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("id required")
            service.delete(id)
            call.respond(HttpStatusCode.NoContent)
        }
    }
}
