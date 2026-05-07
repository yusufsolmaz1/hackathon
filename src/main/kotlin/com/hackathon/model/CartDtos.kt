package com.hackathon.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ───── Response DTO ─────

@Serializable
data class CartItemDto(
    val id: String,
    @SerialName("product_id") val productId: String,
    val brand: String,
    val name: String,
    val size: String,
    val price: String,                 // formatted "X,YY TL"
    val quantity: Int,
    @SerialName("icon_name") val iconName: String,
)

@Serializable
data class CartResponse(
    val items: List<CartItemDto>,
    @SerialName("total_price") val totalPrice: String,
    @SerialName("item_count") val itemCount: Int,
)

// ───── Request DTOs ─────

@Serializable
data class AddCartItemRequest(
    @SerialName("product_id") val productId: String,
    val size: String,
    val quantity: Int = 1,
)

@Serializable
data class UpdateCartItemRequest(val quantity: Int)

// ───── DB Row ─────

@Serializable
data class CartItemRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("product_id") val productId: String,
    val brand: String,
    val name: String,
    val size: String,
    val price: Double,
    val quantity: Int,
    @SerialName("icon_name") val iconName: String,
)
