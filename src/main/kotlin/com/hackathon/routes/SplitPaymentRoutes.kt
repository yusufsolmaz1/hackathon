package com.hackathon.routes

import com.hackathon.config.ValidationException
import com.hackathon.model.InitiateSplitRequestDto
import com.hackathon.model.InitiatorPayRequestDto
import com.hackathon.service.SplitPaymentService
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route

/**
 * Base path: /split-payment
 *
 * Endpoint haritası:
 *   POST   /initiate
 *   POST   /initiator-pay/{splitId}
 *   GET    /request/{splitId}/{participantId}
 *   POST   /reject/{splitId}                  (X-User-Id header zorunlu)
 *   GET    /{splitId}                         (X-User-Id header zorunlu — viewer)
 *   POST   /{splitId}/remind/{participantId}
 *   POST   /{splitId}/cancel
 */
fun Route.splitPaymentRoutes(service: SplitPaymentService) {
    route("/split-payment") {

        // 1.1
        post("/initiate") {
            val body = call.receive<InitiateSplitRequestDto>()
            call.respond(HttpStatusCode.Created, service.initiate(body))
        }

        // 1.2
        post("/initiator-pay/{splitId}") {
            val splitId = call.parameters["splitId"] ?: throw ValidationException("splitId required")
            val body = call.receive<InitiatorPayRequestDto>()
            call.respond(service.initiatorPay(splitId, body.orderId))
        }

        // 1.3
        get("/request/{splitId}/{participantId}") {
            val splitId = call.parameters["splitId"] ?: throw ValidationException("splitId required")
            val participantId = call.parameters["participantId"] ?: throw ValidationException("participantId required")
            call.respond(service.request(splitId, participantId))
        }

        // 1.4
        post("/reject/{splitId}") {
            val splitId = call.parameters["splitId"] ?: throw ValidationException("splitId required")
            val viewerId = call.request.headers["X-User-Id"]
                ?: throw ValidationException("X-User-Id header required")
            service.reject(splitId, viewerId)
            call.respond(HttpStatusCode.NoContent)
        }

        // 1.6 — daha spesifik path; 1.5'ten önce tanımla
        post("/{splitId}/remind/{participantId}") {
            val splitId = call.parameters["splitId"] ?: throw ValidationException("splitId required")
            val participantId = call.parameters["participantId"] ?: throw ValidationException("participantId required")
            service.remind(splitId, participantId)
            call.respond(HttpStatusCode.NoContent)
        }

        // 1.7
        post("/{splitId}/cancel") {
            val splitId = call.parameters["splitId"] ?: throw ValidationException("splitId required")
            service.cancel(splitId)
            call.respond(HttpStatusCode.NoContent)
        }

        // 1.5 — en jenerik path en sonda
        get("/{splitId}") {
            val splitId = call.parameters["splitId"] ?: throw ValidationException("splitId required")
            val viewerId = call.request.headers["X-User-Id"]
                ?: throw ValidationException("X-User-Id header required")
            call.respond(service.tracking(splitId, viewerId))
        }
    }
}
