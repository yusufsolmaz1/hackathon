package com.hackathon.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

// =============================================================
// 3) Order detail
//    Mevcut endpoint'e tek alan eklendi: paymentInfo.splitId
//    Geriye dönük uyumlu: nullable + default null
// =============================================================

@Serializable
data class OrderDetailPaymentInfoDto(
    val cardImageUrl: String? = null,
    val paymentDescription: String? = null,
    val totalPrice: String,
    val paymentItems: List<JsonElement> = emptyList(),
    val paymentType: String,
    val cobrandedRewardInfo: JsonElement? = null,
    val isCargoBundleUsed: Boolean = false,
    val umicoInfoItems: List<JsonElement> = emptyList(),
    /**
     * Split kapsamında oluşturulmuş siparişlerde dolu döner.
     * Client → ty://?Page=OrtakOdemeTakip&SplitId=<id>&OrderId=<orderId>
     * deeplink'i ile chip render eder.
     */
    val splitId: String? = null,
)

@Serializable
data class OrderDetailDto(
    val id: String,
    val paymentInfo: OrderDetailPaymentInfoDto,
)

// =============================================================
// DB row
// =============================================================

@Serializable
data class OrderRow(
    val id: String,
    @SerialName("user_id")      val userId: String,
    @SerialName("payment_info") val paymentInfo: OrderDetailPaymentInfoDto,
    @SerialName("split_id")     val splitId: String? = null,
)
