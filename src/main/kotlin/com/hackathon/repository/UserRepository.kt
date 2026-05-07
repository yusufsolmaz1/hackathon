package com.hackathon.repository

import com.hackathon.model.SessionRow
import com.hackathon.model.UserRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

open class UserRepository(private val supabase: SupabaseClient) {
    private val users = "users"
    private val sessions = "sessions"

    open suspend fun findById(id: String): UserRow? =
        supabase.from(users).select { filter { eq("id", id) } }.decodeSingleOrNull()

    open suspend fun findByEmail(email: String): UserRow? =
        supabase.from(users).select { filter { eq("email", email) } }.decodeSingleOrNull()

    open suspend fun create(user: UserRow): UserRow =
        supabase.from(users).insert(user) { select() }.decodeSingle()

    open suspend fun updateProfile(
        id: String,
        name: String?,
        email: String?,
        avatarUrl: String?,
    ): UserRow {
        val patch = buildJsonObject {
            if (name != null) put("name", JsonPrimitive(name))
            if (email != null) put("email", JsonPrimitive(email))
            if (avatarUrl != null) put("avatar_url", JsonPrimitive(avatarUrl))
        }
        return supabase.from(users).update(patch) {
            select()
            filter { eq("id", id) }
        }.decodeSingle()
    }

    // ───── Sessions (fake JWT) ─────

    open suspend fun createSession(token: String, userId: String) {
        supabase.from(sessions).insert(SessionRow(token = token, userId = userId))
    }

    open suspend fun findUserIdByToken(token: String): String? =
        supabase.from(sessions).select { filter { eq("token", token) } }
            .decodeSingleOrNull<SessionRow>()?.userId
}
