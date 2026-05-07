package com.hackathon.repository

import com.hackathon.model.OrderItemRow
import com.hackathon.model.OrderRow
import com.hackathon.model.OrderSplitParticipantRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

open class OrderRepository(private val supabase: SupabaseClient) {
    private val orders = "orders"
    private val orderItems = "order_items"
    private val orderSplit = "order_split_participants"

    open suspend fun listForUser(userId: String): List<OrderRow> =
        supabase.from(orders).select {
            filter { eq("user_id", userId) }
            order("created_at", Order.DESCENDING)
        }.decodeList()

    open suspend fun findById(orderId: String): OrderRow? =
        supabase.from(orders).select { filter { eq("id", orderId) } }.decodeSingleOrNull()

    open suspend fun insert(row: OrderRow): OrderRow =
        supabase.from(orders).insert(row) { select() }.decodeSingle()

    open suspend fun insertItems(items: List<OrderItemRow>) {
        if (items.isEmpty()) return
        supabase.from(orderItems).insert(items)
    }

    open suspend fun listItems(orderId: String): List<OrderItemRow> =
        supabase.from(orderItems).select { filter { eq("order_id", orderId) } }.decodeList()

    open suspend fun insertSplitParticipants(rows: List<OrderSplitParticipantRow>) {
        if (rows.isEmpty()) return
        supabase.from(orderSplit).insert(rows)
    }

    open suspend fun listSplitParticipants(orderId: String): List<OrderSplitParticipantRow> =
        supabase.from(orderSplit).select { filter { eq("order_id", orderId) } }.decodeList()

    open suspend fun setStatus(orderId: String, status: String) {
        val patch = buildJsonObject { put("status", JsonPrimitive(status)) }
        supabase.from(orders).update(patch) { filter { eq("id", orderId) } }
    }
}
