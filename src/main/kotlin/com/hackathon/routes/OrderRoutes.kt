package com.hackathon.routes

import com.hackathon.config.ValidationException
import com.hackathon.service.OrderService
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.route

/**
 * Mevcut sipariş detayı.
 *   GET /orders/{id}
 *
 * Yeni alan: paymentInfo.splitId (opsiyonel, default null).
 * Split kapsamındaki siparişlerde dolu döner; client chip render eder.
 */
fun Route.orderRoutes(service: OrderService) {
    route("/orders") {
        get("/{id}") {
            val id = call.parameters["id"] ?: throw ValidationException("id required")
            call.respond(service.getDetail(id))
        }
    }
}
