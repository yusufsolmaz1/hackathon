package com.hackathon.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ───── Response DTO ─────

@Serializable
data class FriendDto(
    val id: String,                        // friend's user id
    val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("avatar_color_name") val avatarColorName: String,
)

// ───── Request DTOs ─────

@Serializable
data class AddFriendRequest(@SerialName("friend_id") val friendId: String)

@Serializable
data class SyncFriendsRequest(@SerialName("contact_emails") val contactEmails: List<String>)

@Serializable
data class SyncFriendsResponse(val added: List<FriendDto>, @SerialName("not_found") val notFound: List<String>)

// ───── DB Row ─────

@Serializable
data class FriendEdgeRow(
    @SerialName("user_id") val userId: String,
    @SerialName("friend_id") val friendId: String,
)
