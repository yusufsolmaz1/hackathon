package com.hackathon.repository

import com.hackathon.model.CollectionParticipantRow
import com.hackathon.model.CollectionProductLikeRow
import com.hackathon.model.CollectionProductRow
import com.hackathon.model.CollectionRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

open class CollectionRepository(private val supabase: SupabaseClient) {
    private val collections = "collections"
    private val collectionProducts = "collection_products"
    private val collectionParticipants = "collection_participants"
    private val collectionProductLikes = "collection_product_likes"

    open suspend fun listOwnedOrParticipating(userId: String): List<CollectionRow> {
        // Owned
        val owned = supabase.from(collections).select {
            filter { eq("owner_id", userId) }
            order("id", Order.ASCENDING)
        }.decodeList<CollectionRow>()
        // Participating (others)
        val partRows = supabase.from(collectionParticipants).select {
            filter { eq("user_id", userId) }
        }.decodeList<CollectionParticipantRow>()
        val participatingIds = partRows.map { it.collectionId } - owned.map { it.id }.toSet()
        val participating = if (participatingIds.isEmpty()) emptyList() else
            supabase.from(collections).select {
                filter { isIn("id", participatingIds) }
                order("id", Order.ASCENDING)
            }.decodeList<CollectionRow>()
        return owned + participating
    }

    open suspend fun findById(id: String): CollectionRow? =
        supabase.from(collections).select { filter { eq("id", id) } }.decodeSingleOrNull()

    open suspend fun create(row: CollectionRow): CollectionRow =
        supabase.from(collections).insert(row) { select() }.decodeSingle()

    open suspend fun listProductIds(collectionId: String): List<String> =
        listProducts(collectionId).map { it.productId }

    open suspend fun listProducts(collectionId: String): List<CollectionProductRow> =
        supabase.from(collectionProducts).select { filter { eq("collection_id", collectionId) } }
            .decodeList<CollectionProductRow>()

    open suspend fun addProducts(collectionId: String, productIds: List<String>, addedBy: String? = null) {
        if (productIds.isEmpty()) return
        val rows = productIds.distinct().map { pid ->
            buildJsonObject {
                put("collection_id", JsonPrimitive(collectionId))
                put("product_id", JsonPrimitive(pid))
                if (addedBy != null) put("added_by", JsonPrimitive(addedBy))
            }
        }
        supabase.from(collectionProducts).insert(rows)
    }

    open suspend fun listParticipantIds(collectionId: String): List<String> =
        supabase.from(collectionParticipants).select { filter { eq("collection_id", collectionId) } }
            .decodeList<CollectionParticipantRow>().map { it.userId }

    open suspend fun addParticipants(collectionId: String, userIds: List<String>) {
        if (userIds.isEmpty()) return
        // Filter out duplicates
        val existing = listParticipantIds(collectionId).toSet()
        val toAdd = userIds.distinct().filter { it !in existing }
            .map { CollectionParticipantRow(collectionId, it) }
        if (toAdd.isNotEmpty()) supabase.from(collectionParticipants).insert(toAdd)
    }

    open suspend fun setShared(collectionId: String, isShared: Boolean) {
        val patch = buildJsonObject { put("is_shared", JsonPrimitive(isShared)) }
        supabase.from(collections).update(patch) { filter { eq("id", collectionId) } }
    }

    // ── per-collection product likes ────────────────────────────────

    open suspend fun listProductLikes(collectionId: String): List<CollectionProductLikeRow> =
        supabase.from(collectionProductLikes).select { filter { eq("collection_id", collectionId) } }
            .decodeList<CollectionProductLikeRow>()

    open suspend fun setProductLikeStatus(
        collectionId: String,
        productId: String,
        userId: String,
        status: String,
    ) {
        // Try update first; if no row, insert.
        val existing = supabase.from(collectionProductLikes).select {
            filter {
                eq("collection_id", collectionId)
                eq("product_id", productId)
                eq("user_id", userId)
            }
        }.decodeList<CollectionProductLikeRow>()
        if (existing.isNotEmpty()) {
            val patch = buildJsonObject { put("status", JsonPrimitive(status)) }
            supabase.from(collectionProductLikes).update(patch) {
                filter {
                    eq("collection_id", collectionId)
                    eq("product_id", productId)
                    eq("user_id", userId)
                }
            }
        } else {
            val row = buildJsonObject {
                put("collection_id", JsonPrimitive(collectionId))
                put("product_id", JsonPrimitive(productId))
                put("user_id", JsonPrimitive(userId))
                put("status", JsonPrimitive(status))
            }
            supabase.from(collectionProductLikes).insert(row)
        }
    }

    open suspend fun clearProductLike(collectionId: String, productId: String, userId: String) {
        supabase.from(collectionProductLikes).delete {
            filter {
                eq("collection_id", collectionId)
                eq("product_id", productId)
                eq("user_id", userId)
            }
        }
    }
}
