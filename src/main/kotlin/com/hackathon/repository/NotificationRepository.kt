package com.hackathon.repository

import com.hackathon.model.NotificationRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

open class NotificationRepository(private val supabase: SupabaseClient) {
    private val table = "notifications"

    open suspend fun listForUser(userId: String, after: String?): List<NotificationRow> =
        supabase.from(table).select {
            filter {
                eq("user_id", userId)
                if (after != null) gt("created_at", after)
            }
            order("created_at", Order.DESCENDING)
        }.decodeList()

    open suspend fun findById(id: String): NotificationRow? =
        supabase.from(table).select { filter { eq("id", id) } }.decodeSingleOrNull()

    open suspend fun insert(row: NotificationRow): NotificationRow =
        supabase.from(table).insert(row) { select() }.decodeSingle()

    open suspend fun markRead(id: String) {
        val patch = buildJsonObject { put("is_read", JsonPrimitive(true)) }
        supabase.from(table).update(patch) { filter { eq("id", id) } }
    }
}
