package com.hackathon.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ───── Response DTO (API) ─────

@Serializable
data class ProductDto(
    val id: String,
    val brand: String,
    val name: String,
    val rating: Double,
    @SerialName("review_count") val reviewCount: Int,
    val price: Double,
    @SerialName("image_name") val imageName: String,
    @SerialName("is_favorite") val isFavorite: Boolean,
)

// ───── Like ─────

@Serializable
data class LikeStatusRequest(val status: String)   // liked | disliked | none

@Serializable
data class LikeState(
    val status: String,
    @SerialName("like_count") val likeCount: Int,
    @SerialName("dislike_count") val dislikeCount: Int,
)

@Serializable
data class FavoriteToggleResponse(@SerialName("is_favorite") val isFavorite: Boolean)

// ───── DB Rows ─────

@Serializable
data class ProductRow(
    val id: String,
    val brand: String,
    val name: String,
    val rating: Double,
    @SerialName("review_count") val reviewCount: Int,
    val price: Double,
    @SerialName("image_name") val imageName: String,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("dislike_count") val dislikeCount: Int = 0,
)

@Serializable
data class FavoriteRow(
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: String,
)

@Serializable
data class LikeRow(
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: String,
    val status: String,
)
