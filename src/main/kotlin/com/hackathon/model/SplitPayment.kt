package com.hackathon.model

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// =============================================================
// Enums (string-encoded; DB ile aynı set)
// =============================================================

@Serializable
enum class SplitMethod { EQUAL, CUSTOM }

@Serializable
enum class SplitParticipantStatus { PENDING, PAID, REJECTED, EXPIRED }

@Serializable
enum class SplitOverallStatus { WAITING, COMPLETED, EXPIRED, CANCELLED }

@Serializable
enum class SplitRole { INITIATOR, PARTICIPANT }

// =============================================================
// 1.1  POST /initiate
// =============================================================

@Serializable
data class ParticipantInputDto(
    val participantId: String,
    val shareKurus: Long,
    val isInitiator: Boolean = false,
)

@Serializable
data class InitiateSplitRequestDto(
    val totalKurus: Long,
    val splitMethod: SplitMethod,
    val participants: List<ParticipantInputDto>,
)

@Serializable
data class InitiateSplitResponseDto(
    val splitId: String,
)

// =============================================================
// 1.2  POST /initiator-pay/{splitId}
// =============================================================

@Serializable
data class InitiatorPayRequestDto(
    val orderId: String,
)

@Serializable
data class InitiatorPayResponseDto(
    val trackingDeeplink: String,
)

// =============================================================
// 1.3  GET /request/{splitId}/{participantId}
// =============================================================

@Serializable
data class SplitProductDto(
    val productId: String,
    val name: String,
    val imageUrl: String? = null,
    val priceKurus: Long,
)

@Serializable
data class SplitPaymentRequestResponseDto(
    val splitId: String,
    val participantId: String,
    val initiatorName: String,
    val shareKurus: Long,
    val totalKurus: Long,
    val splitMethod: SplitMethod,
    val status: SplitParticipantStatus,
    val products: List<SplitProductDto>,
)

// =============================================================
// 1.5  GET /{splitId}  (tracking)
// =============================================================

@Serializable
data class TrackingInitiatorDto(
    val name: String,
    val initials: String,
)

@Serializable
data class TrackingParticipantDto(
    val id: String,
    val name: String,
    val initials: String,
    val status: SplitParticipantStatus,
    val amountKurus: Long,
)

@Serializable
data class SplitPaymentTrackingDto(
    val splitId: String,
    val role: SplitRole,
    val overallStatus: SplitOverallStatus,
    val expiresAtMillis: Long,
    val totalKurus: Long,
    val yourShareKurus: Long,
    val initiator: TrackingInitiatorDto,
    val participants: List<TrackingParticipantDto>,
    val products: List<SplitProductDto>,
)

// =============================================================
// DB row representations (snake_case → @SerialName)
// =============================================================

@Serializable
data class SplitRow(
    val id: String? = null,
    @SerialName("initiator_id")        val initiatorId: String,
    @SerialName("initiator_order_id")  val initiatorOrderId: String? = null,
    @SerialName("total_kurus")         val totalKurus: Long,
    @SerialName("split_method")        val splitMethod: String,
    val status: String = SplitOverallStatus.WAITING.name,
    val products: List<SplitProductDto> = emptyList(),
    @SerialName("expires_at_millis")   val expiresAtMillis: Long,
)

@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class SplitParticipantRow(
    @SerialName("split_id")        val splitId: String,
    @SerialName("participant_id")  val participantId: String,
    @SerialName("share_kurus")     val shareKurus: Long,
    // Bulk insert'te default değerler JSON'dan düşmesin diye ALWAYS encode.
    // Postgrest column union'u ilk satırdan çıkarıyor; eksik kalan satıra null yazıyor.
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    @SerialName("is_initiator")    val isInitiator: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.ALWAYS)
    val status: String = SplitParticipantStatus.PENDING.name,
    @SerialName("paid_order_id")   val paidOrderId: String? = null,
)
