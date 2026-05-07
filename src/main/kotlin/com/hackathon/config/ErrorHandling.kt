package com.hackathon.config

import com.hackathon.model.FriendsErrorCode
import com.hackathon.model.FriendsErrorResponseDto
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

class NotFoundException(message: String) : RuntimeException(message)
class ValidationException(message: String) : RuntimeException(message)

/**
 * Friends BFF'ye özel error contract'ı taşır.
 * Status: 4xx (USER_NOT_FOUND için 404, diğerleri 409/400).
 */
class FriendsException(
    val errorCode: FriendsErrorCode,
    message: String,
) : RuntimeException(message)

@Serializable
data class ErrorResponse(val error: String, val code: String)

fun Application.configureErrorHandling() {
    val logger = LoggerFactory.getLogger("ErrorHandling")
    install(StatusPages) {
        exception<NotFoundException> { call, cause ->
            call.respond(
                HttpStatusCode.NotFound,
                ErrorResponse(cause.message ?: "Not found", "NOT_FOUND"),
            )
        }
        exception<ValidationException> { call, cause ->
            call.respond(
                HttpStatusCode.BadRequest,
                ErrorResponse(cause.message ?: "Bad request", "VALIDATION"),
            )
        }
        exception<FriendsException> { call, cause ->
            val status = when (cause.errorCode) {
                FriendsErrorCode.USER_NOT_FOUND -> HttpStatusCode.NotFound
                FriendsErrorCode.ALREADY_FRIEND -> HttpStatusCode.Conflict
                FriendsErrorCode.SELF_ADD,
                FriendsErrorCode.INVALID_EMAIL  -> HttpStatusCode.BadRequest
            }
            call.respond(
                status,
                FriendsErrorResponseDto(
                    errorCode = cause.errorCode.name,
                    message = cause.message ?: cause.errorCode.name,
                ),
            )
        }
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse("Internal server error", "INTERNAL"),
            )
        }
    }
}
