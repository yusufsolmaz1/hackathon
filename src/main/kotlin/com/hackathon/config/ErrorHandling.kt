package com.hackathon.config

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import kotlinx.serialization.Serializable
import org.slf4j.LoggerFactory

/**
 * TrendSocial API'sinin tek tip exception'ı.
 * httpStatus → respond status, code → ErrorResponse.error.code, message → ErrorResponse.error.message
 *
 * Standart kodlar (swagger ile uyumlu):
 *   UNAUTHORIZED, FORBIDDEN, NOT_FOUND, BAD_REQUEST,
 *   EMAIL_EXISTS, USER_NOT_FOUND, PRODUCT_NOT_FOUND, SERVER_ERROR
 */
class TrendException(
    val httpStatus: HttpStatusCode,
    val code: String,
    override val message: String,
) : RuntimeException(message) {
    companion object {
        fun unauthorized(msg: String = "Oturum sureniz doldu. Lutfen tekrar giris yapin.") =
            TrendException(HttpStatusCode.Unauthorized, "UNAUTHORIZED", msg)

        fun notFound(msg: String = "Istenen icerik bulunamadi.") =
            TrendException(HttpStatusCode.NotFound, "NOT_FOUND", msg)

        fun badRequest(msg: String = "Gecersiz veri gonderildi. Lutfen alanlari kontrol edin.") =
            TrendException(HttpStatusCode.BadRequest, "BAD_REQUEST", msg)

        fun forbidden(msg: String = "Bu islem icin yetkiniz bulunmuyor.") =
            TrendException(HttpStatusCode.Forbidden, "FORBIDDEN", msg)

        fun emailExists(msg: String = "Bu e-posta adresi zaten kayitli.") =
            TrendException(HttpStatusCode.BadRequest, "EMAIL_EXISTS", msg)

        fun userNotFound(msg: String = "Bu e-posta adresine ait kullanici bulunamadi.") =
            TrendException(HttpStatusCode.BadRequest, "USER_NOT_FOUND", msg)

        fun productNotFound(msg: String = "Urun bulunamadi.") =
            TrendException(HttpStatusCode.NotFound, "PRODUCT_NOT_FOUND", msg)

        fun invalidDate(msg: String = "Dogum gunu ISO 8601 formatinda olmalidir.") =
            TrendException(HttpStatusCode.BadRequest, "INVALID_DATE", msg)
    }
}

@Serializable
data class ErrorBody(val code: String, val message: String)

@Serializable
data class ErrorResponse(val error: ErrorBody)

fun Application.configureErrorHandling() {
    val logger = LoggerFactory.getLogger("ErrorHandling")
    install(StatusPages) {
        exception<TrendException> { call, cause ->
            call.respond(cause.httpStatus, ErrorResponse(ErrorBody(cause.code, cause.message)))
        }
        exception<Throwable> { call, cause ->
            logger.error("Unhandled exception", cause)
            call.respond(
                HttpStatusCode.InternalServerError,
                ErrorResponse(ErrorBody("SERVER_ERROR", "Sunucu hatasi. Lutfen daha sonra tekrar deneyin.")),
            )
        }
    }
}
