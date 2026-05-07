package com.hackathon.repository

import com.hackathon.model.OrderRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

open class OrderRepository(private val supabase: SupabaseClient) {

    private val table = "orders"

    open suspend fun findById(id: String): OrderRow? =
        supabase.from(table)
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull()

    open suspend fun setSplitId(orderId: String, splitId: String): OrderRow? =
        supabase.from(table)
            .update(buildJsonObject { put("split_id", JsonPrimitive(splitId)) }) {
                select()
                filter { eq("id", orderId) }
            }
            .decodeSingleOrNull()
}
