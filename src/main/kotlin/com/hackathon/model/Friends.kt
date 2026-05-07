package com.hackathon.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================
// 2.x  Friends BFF
// =============================================================

@Serializable
data class FriendDto(
    val id: String,
    val name: String,
    val email: String,
    val initials: String,
    val avatarUrl: String? = null,
)

@Serializable
data class FriendsListResponseDto(
    val friends: List<FriendDto>,
)

@Serializable
data class AddFriendRequestDto(
    val email: String,
)

@Serializable
data class AddFriendResponseDto(
    val friend: FriendDto,
)

/**
 * 4xx error payload — client tarafı sealed FriendsError'a maple ediyor.
 *
 * errorCode set:
 *  - USER_NOT_FOUND
 *  - SELF_ADD
 *  - ALREADY_FRIEND
 *  - INVALID_EMAIL
 */
@Serializable
data class FriendsErrorResponseDto(
    val errorCode: String,
    val message: String,
)

enum class FriendsErrorCode {
    USER_NOT_FOUND,
    SELF_ADD,
    ALREADY_FRIEND,
    INVALID_EMAIL,
}

// =============================================================
// DB rows
// =============================================================

@Serializable
data class UserRow(
    val id: String,
    val name: String,
    val email: String? = null,
    val initials: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class FriendRow(
    @SerialName("user_id")   val userId: String,
    @SerialName("friend_id") val friendId: String,
)
