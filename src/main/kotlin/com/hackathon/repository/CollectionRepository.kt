package com.hackathon.repository

import com.hackathon.model.CollectionParticipantRow
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
        supabase.from(collectionProducts).select { filter { eq("collection_id", collectionId) } }
            .decodeList<CollectionProductRow>().map { it.productId }

    open suspend fun addProducts(collectionId: String, productIds: List<String>) {
        if (productIds.isEmpty()) return
        val rows = productIds.distinct().map { CollectionProductRow(collectionId, it) }
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
}
