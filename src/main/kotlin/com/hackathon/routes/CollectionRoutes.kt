package com.hackathon.routes

import com.hackathon.config.TrendException
import com.hackathon.config.requireUserId
import com.hackathon.model.AddProductsToCollectionRequest
import com.hackathon.model.CreateCollectionRequest
import com.hackathon.model.LikeStatusRequest
import com.hackathon.model.ShareCollectionRequest
import com.hackathon.repository.UserRepository
import com.hackathon.service.CollectionService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.route

fun Route.collectionRoutes(service: CollectionService, userRepo: UserRepository) {
    route("/collections") {
        get {
            val userId = call.requireUserId(userRepo)
            call.respond(service.list(userId))
        }
        post {
            val userId = call.requireUserId(userRepo)
            val req = call.receive<CreateCollectionRequest>()
            call.respond(HttpStatusCode.Created, service.create(userId, req))
        }
        get("/{id}") {
            val userId = call.requireUserId(userRepo)
            val id = call.parameters["id"] ?: throw TrendException.badRequest("'id' gereklidir.")
            call.respond(service.getDetail(userId, id))
        }
        post("/{id}/share") {
            val userId = call.requireUserId(userRepo)
            val id = call.parameters["id"] ?: throw TrendException.badRequest("'id' gereklidir.")
            val req = call.receive<ShareCollectionRequest>()
            call.respond(service.share(userId, id, req))
        }
        post("/{id}/products") {
            val userId = call.requireUserId(userRepo)
            val id = call.parameters["id"] ?: throw TrendException.badRequest("'id' gereklidir.")
            val req = call.receive<AddProductsToCollectionRequest>()
            call.respond(service.addProducts(userId, id, req.productIds))
        }
        put("/{id}/products/{pid}/like") {
            val userId = call.requireUserId(userRepo)
            val id = call.parameters["id"] ?: throw TrendException.badRequest("'id' gereklidir.")
            val pid = call.parameters["pid"] ?: throw TrendException.badRequest("'pid' gereklidir.")
            val req = call.receive<LikeStatusRequest>()
            call.respond(service.setProductLike(userId, id, pid, req))
        }
    }
}
