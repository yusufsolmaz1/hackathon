package com.hackathon.repository

import com.hackathon.model.CartItemRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

open class CartRepository(private val supabase: SupabaseClient) {
    private val cart = "cart_items"

    open suspend fun list(userId: String): List<CartItemRow> =
        supabase.from(cart).select {
            filter { eq("user_id", userId) }
            order("id", Order.ASCENDING)
        }.decodeList()

    open suspend fun findById(itemId: String): CartItemRow? =
        supabase.from(cart).select { filter { eq("id", itemId) } }.decodeSingleOrNull()

    open suspend fun findByProductAndSize(userId: String, productId: String, size: String): CartItemRow? =
        supabase.from(cart).select {
            filter {
                eq("user_id", userId); eq("product_id", productId); eq("size", size)
            }
        }.decodeSingleOrNull()

    open suspend fun insert(row: CartItemRow): CartItemRow =
        supabase.from(cart).insert(row) { select() }.decodeSingle()

    open suspend fun updateQuantity(itemId: String, quantity: Int): CartItemRow {
        val patch = buildJsonObject { put("quantity", JsonPrimitive(quantity)) }
        return supabase.from(cart).update(patch) {
            select(); filter { eq("id", itemId) }
        }.decodeSingle()
    }

    open suspend fun delete(itemId: String) {
        supabase.from(cart).delete { filter { eq("id", itemId) } }
    }

    open suspend fun deleteAllForUser(userId: String) {
        supabase.from(cart).delete { filter { eq("user_id", userId) } }
    }
}
