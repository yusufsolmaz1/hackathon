package com.hackathon.repository

import com.hackathon.model.FavoriteRow
import com.hackathon.model.LikeRow
import com.hackathon.model.ProductRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

open class ProductRepository(private val supabase: SupabaseClient) {
    private val products = "products"
    private val favorites = "product_favorites"
    private val likes = "product_likes"

    open suspend fun listAll(): List<ProductRow> =
        supabase.from(products).select { order("id", Order.ASCENDING) }.decodeList()

    open suspend fun findById(id: String): ProductRow? =
        supabase.from(products).select { filter { eq("id", id) } }.decodeSingleOrNull()

    open suspend fun search(query: String): List<ProductRow> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        val pattern = "%${q.replace("%", "").replace("_", "")}%"
        return supabase.from(products).select {
            filter {
                or {
                    ilike("name", pattern)
                    ilike("brand", pattern)
                }
            }
            order("id", Order.ASCENDING)
        }.decodeList()
    }

    open suspend fun listFavoriteProductIds(userId: String): Set<String> =
        supabase.from(favorites).select { filter { eq("user_id", userId) } }
            .decodeList<FavoriteRow>().map { it.productId }.toSet()

    open suspend fun listFavoriteProducts(userId: String): List<ProductRow> {
        val ids = listFavoriteProductIds(userId)
        if (ids.isEmpty()) return emptyList()
        return supabase.from(products).select {
            filter { isIn("id", ids.toList()) }
            order("id", Order.ASCENDING)
        }.decodeList()
    }

    /** Returns the new favorite state. */
    open suspend fun toggleFavorite(userId: String, productId: String): Boolean {
        val existing = supabase.from(favorites).select {
            filter { eq("user_id", userId); eq("product_id", productId) }
        }.decodeSingleOrNull<FavoriteRow>()
        return if (existing == null) {
            supabase.from(favorites).insert(FavoriteRow(userId = userId, productId = productId))
            true
        } else {
            supabase.from(favorites).delete {
                filter { eq("user_id", userId); eq("product_id", productId) }
            }
            false
        }
    }

    open suspend fun getLikeStatus(userId: String, productId: String): String? =
        supabase.from(likes).select {
            filter { eq("user_id", userId); eq("product_id", productId) }
        }.decodeSingleOrNull<LikeRow>()?.status

    open suspend fun setLikeStatus(userId: String, productId: String, status: String) {
        // Delete existing then insert (status is composite PK part-free; we use upsert semantics)
        supabase.from(likes).delete {
            filter { eq("user_id", userId); eq("product_id", productId) }
        }
        supabase.from(likes).insert(LikeRow(userId = userId, productId = productId, status = status))
    }

    open suspend fun clearLikeStatus(userId: String, productId: String) {
        supabase.from(likes).delete {
            filter { eq("user_id", userId); eq("product_id", productId) }
        }
    }

    open suspend fun updateLikeCounts(productId: String, likeCount: Int, dislikeCount: Int) {
        val patch = buildJsonObject {
            put("like_count", JsonPrimitive(likeCount))
            put("dislike_count", JsonPrimitive(dislikeCount))
        }
        supabase.from(products).update(patch) { filter { eq("id", productId) } }
    }

    open suspend fun countLikes(productId: String): Pair<Int, Int> {
        val rows = supabase.from(likes).select { filter { eq("product_id", productId) } }
            .decodeList<LikeRow>()
        val liked = rows.count { it.status == "liked" }
        val disliked = rows.count { it.status == "disliked" }
        return liked to disliked
    }
}
