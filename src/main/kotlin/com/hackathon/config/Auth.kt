package com.hackathon.config

import com.hackathon.repository.UserRepository
import io.ktor.http.HttpHeaders
import io.ktor.server.application.ApplicationCall
import io.ktor.server.request.header
import java.security.MessageDigest

/**
 * Bearer token'dan userId çıkarır. Token bulunamazsa 401 atar.
 *
 * Kullanım: route handler'ın ilk satırında `val userId = call.requireUserId(userRepo)`
 */
suspend fun ApplicationCall.requireUserId(userRepo: UserRepository): String {
    val header = request.header(HttpHeaders.Authorization)
        ?: throw TrendException.unauthorized("Authorization header eksik.")
    if (!header.startsWith("Bearer ", ignoreCase = true)) {
        throw TrendException.unauthorized("Authorization header gecersiz format.")
    }
    val token = header.substring(7).trim()
    if (token.isEmpty()) throw TrendException.unauthorized("Token bos.")

    return userRepo.findUserIdByToken(token)
        ?: throw TrendException.unauthorized()
}

/** Plain text password → SHA-256 hex (lowercase). */
fun sha256(input: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
    return digest.joinToString("") { "%02x".format(it) }
}
