package com.hackathon.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class NotificationRow(
    val id: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("user_id") val userId: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val type: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val title: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val body: String,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null,
)

@Serializable
data class NotificationDto(
    val id: String,
    val title: String,
    val body: String,
    val type: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("is_read") val isRead: Boolean,
)

@Serializable
data class MessageResponse(val message: String)
