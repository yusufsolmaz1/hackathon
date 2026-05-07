package com.hackathon.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ───── Response DTOs ─────

@Serializable
data class CollectionSummaryDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("is_shared") val isShared: Boolean,
    @SerialName("image_name") val imageName: String,
    @SerialName("product_count") val productCount: Int,
    @SerialName("participant_count") val participantCount: Int,
)

@Serializable
data class CollectionDetailDto(
    val id: String,
    val name: String,
    val description: String? = null,
    @SerialName("is_shared") val isShared: Boolean,
    @SerialName("image_name") val imageName: String,
    val products: List<ProductDto>,
    val participants: List<FriendDto>,
)

// ───── Request DTOs ─────

@Serializable
data class CreateCollectionRequest(
    val name: String,
    val description: String? = null,
    @SerialName("image_name") val imageName: String? = null,
    @SerialName("product_ids") val productIds: List<String> = emptyList(),
    @SerialName("is_shared") val isShared: Boolean = false,
)

@Serializable
data class ShareCollectionRequest(
    @SerialName("friend_ids") val friendIds: List<String>,
)

@Serializable
data class ShareCollectionResponse(
    @SerialName("is_shared") val isShared: Boolean,
    @SerialName("participant_count") val participantCount: Int,
)

// ───── DB Rows ─────

@Serializable
data class CollectionRow(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val name: String,
    val description: String? = null,
    @SerialName("is_shared") val isShared: Boolean = false,
    @SerialName("image_name") val imageName: String = "collection_default",
)

@Serializable
data class CollectionProductRow(
    @SerialName("collection_id") val collectionId: String,
    @SerialName("product_id") val productId: String,
)

@Serializable
data class CollectionParticipantRow(
    @SerialName("collection_id") val collectionId: String,
    @SerialName("user_id") val userId: String,
)
