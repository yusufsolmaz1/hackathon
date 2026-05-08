package com.hackathon.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ───── Response DTOs ─────

@Serializable
data class OrderItemDto(
    val name: String,
    val brand: String,
    val quantity: Int,
    val price: Double,
    @SerialName("icon_name") val iconName: String,
)

@Serializable
data class OrderSummaryDto(
    val id: String,
    @SerialName("order_number") val orderNumber: String,
    @SerialName("total_price") val totalPrice: Double,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("estimated_delivery") val estimatedDelivery: String? = null,
    @SerialName("is_split_payment") val isSplitPayment: Boolean,
    @SerialName("item_count") val itemCount: Int,
)

@Serializable
data class SplitParticipantDto(
    @SerialName("friend_id") val friendId: String,
    val name: String,
    val amount: Double,
    @SerialName("has_paid") val hasPaid: Boolean,
)

@Serializable
data class OrderDetailDto(
    val id: String,
    @SerialName("order_number") val orderNumber: String,
    @SerialName("total_price") val totalPrice: Double,
    val status: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("estimated_delivery") val estimatedDelivery: String? = null,
    @SerialName("is_split_payment") val isSplitPayment: Boolean,
    val items: List<OrderItemDto>,
    @SerialName("split_participants") val splitParticipants: List<SplitParticipantDto> = emptyList(),
)

@Serializable
data class SplitStatusDto(
    @SerialName("order_id") val orderId: String,
    @SerialName("total_price") val totalPrice: Double,
    val participants: List<SplitParticipantDto>,
    @SerialName("paid_amount") val paidAmount: Double,
    @SerialName("remaining_amount") val remainingAmount: Double,
    @SerialName("all_paid") val allPaid: Boolean,
)

// ───── Request DTOs ─────

@Serializable
data class CreateOrderRequest(
    @SerialName("cart_item_ids") val cartItemIds: List<String> = emptyList(),
)

@Serializable
data class CreateSplitOrderRequest(
    @SerialName("cart_item_ids") val cartItemIds: List<String> = emptyList(),
    @SerialName("friend_ids") val friendIds: List<String>,
)

// ───── DB Rows ─────

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class OrderRow(
    val id: String,
    @SerialName("user_id") val userId: String,
    @SerialName("order_number") val orderNumber: String,
    @SerialName("total_price") val totalPrice: Double,
    val status: String,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("estimated_delivery") val estimatedDelivery: String? = null,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("is_split_payment") val isSplitPayment: Boolean = false,
)

@Serializable
data class OrderItemRow(
    @SerialName("order_id") val orderId: String,
    val name: String,
    val brand: String,
    val quantity: Int,
    val price: Double,
    @SerialName("icon_name") val iconName: String,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class OrderSplitParticipantRow(
    @SerialName("order_id") val orderId: String,
    @SerialName("friend_id") val friendId: String,
    val name: String,
    val amount: Double,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("has_paid") val hasPaid: Boolean = false,
)
