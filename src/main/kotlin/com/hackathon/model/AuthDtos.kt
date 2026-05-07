package com.hackathon.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ───── Request DTOs ─────

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val name: String, val email: String, val password: String)

@Serializable
data class UpdateProfileRequest(
    val name: String? = null,
    val email: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

// ───── Response DTOs ─────

@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
)

@Serializable
data class AuthResponse(val token: String, val user: UserProfile)

// ───── DB Rows ─────

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class UserRow(
    val id: String,
    val email: String,
    @SerialName("password_hash") val passwordHash: String,
    val name: String,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("avatar_color_name") val avatarColorName: String = "blue",
)

@Serializable
data class SessionRow(
    val token: String,
    @SerialName("user_id") val userId: String,
)
