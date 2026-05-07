package com.hackathon.service

import com.hackathon.config.NotFoundException
import com.hackathon.config.ValidationException
import com.hackathon.model.InitiateSplitRequestDto
import com.hackathon.model.InitiateSplitResponseDto
import com.hackathon.model.InitiatorPayResponseDto
import com.hackathon.model.SplitMethod
import com.hackathon.model.SplitOverallStatus
import com.hackathon.model.SplitParticipantRow
import com.hackathon.model.SplitParticipantStatus
import com.hackathon.model.SplitPaymentRequestResponseDto
import com.hackathon.model.SplitPaymentTrackingDto
import com.hackathon.model.SplitProductDto
import com.hackathon.model.SplitRole
import com.hackathon.model.SplitRow
import com.hackathon.model.TrackingInitiatorDto
import com.hackathon.model.TrackingParticipantDto
import com.hackathon.repository.FriendsRepository
import com.hackathon.repository.OrderRepository
import com.hackathon.repository.SplitPaymentRepository
import java.util.concurrent.TimeUnit
import kotlin.random.Random

class SplitPaymentService(
    private val splits: SplitPaymentRepository,
    private val users: FriendsRepository,
    private val orders: OrderRepository,
) {

    /** Tüm split intent'leri 24 saat içinde expire olur. */
    private val ttlMillis = TimeUnit.HOURS.toMillis(24)

    // ---------------------------------------------------------------
    // 1.1  initiate
    // ---------------------------------------------------------------
    suspend fun initiate(req: InitiateSplitRequestDto): InitiateSplitResponseDto {
        validateInitiate(req)

        val initiator = req.participants.first { it.isInitiator }
        val splitId = "sp_" + Random.nextBytes(4).joinToString("") { "%02x".format(it) }

        splits.insertSplit(
            SplitRow(
                id = splitId,
                initiatorId = initiator.participantId,
                totalKurus = req.totalKurus,
                splitMethod = req.splitMethod.name,
                status = SplitOverallStatus.WAITING.name,
                products = emptyList(),                       // initiate'te products yok
                expiresAtMillis = System.currentTimeMillis() + ttlMillis,
            ),
        )

        splits.insertParticipants(
            req.participants.map {
                SplitParticipantRow(
                    splitId = splitId,
                    participantId = it.participantId,
                    shareKurus = it.shareKurus,
                    isInitiator = it.isInitiator,
                    status = SplitParticipantStatus.PENDING.name,
                )
            },
        )

        return InitiateSplitResponseDto(splitId)
    }

    private fun validateInitiate(req: InitiateSplitRequestDto) {
        if (req.totalKurus <= 0) throw ValidationException("totalKurus must be positive")
        if (req.participants.isEmpty()) throw ValidationException("participants required")
        val initiators = req.participants.count { it.isInitiator }
        if (initiators != 1) throw ValidationException("exactly one initiator required")
        val sum = req.participants.sumOf { it.shareKurus }
        if (req.splitMethod == SplitMethod.CUSTOM && sum != req.totalKurus) {
            throw ValidationException("CUSTOM split: shareKurus toplamı totalKurus'a eşit olmalı")
        }
        if (req.splitMethod == SplitMethod.EQUAL && sum != req.totalKurus) {
            throw ValidationException("EQUAL split: shareKurus toplamı totalKurus'a eşit olmalı")
        }
        val ids = req.participants.map { it.participantId }
        if (ids.size != ids.toSet().size) throw ValidationException("duplicate participantId")
    }

    // ---------------------------------------------------------------
    // 1.2  initiator-pay
    // ---------------------------------------------------------------
    suspend fun initiatorPay(splitId: String, orderId: String): InitiatorPayResponseDto {
        val split = splits.findSplit(splitId) ?: throw NotFoundException("split $splitId not found")
        if (split.status != SplitOverallStatus.WAITING.name) {
            throw ValidationException("split is ${split.status}, cannot pay")
        }
        splits.setInitiatorOrderId(splitId, orderId)
        splits.updateParticipantStatus(
            splitId = splitId,
            participantId = split.initiatorId,
            status = SplitParticipantStatus.PAID.name,
            paidOrderId = orderId,
        ) ?: throw NotFoundException("initiator participant not found")

        // Order'a split_id'yi denormalize et (chip için)
        runCatching { orders.setSplitId(orderId, splitId) }

        // (Hackathon scope: gerçek push/sms yok)

        recomputeOverallStatus(splitId)

        return InitiatorPayResponseDto(
            trackingDeeplink = buildTrackingDeeplink(splitId),
        )
    }

    private fun buildTrackingDeeplink(splitId: String): String =
        "ty://?Page=OrtakOdemeTakip&SplitId=$splitId"

    // ---------------------------------------------------------------
    // 1.3  request
    // ---------------------------------------------------------------
    suspend fun request(splitId: String, participantId: String): SplitPaymentRequestResponseDto {
        val split = splits.findSplit(splitId) ?: throw NotFoundException("split $splitId not found")
        val part = splits.findParticipant(splitId, participantId)
            ?: throw NotFoundException("participant $participantId not in split $splitId")
        val initiator = users.findUser(split.initiatorId)
            ?: throw NotFoundException("initiator user not found")

        return SplitPaymentRequestResponseDto(
            splitId = splitId,
            participantId = participantId,
            initiatorName = initiator.name,
            shareKurus = part.shareKurus,
            totalKurus = split.totalKurus,
            splitMethod = SplitMethod.valueOf(split.splitMethod),
            status = SplitParticipantStatus.valueOf(part.status),
            products = split.products,
        )
    }

    // ---------------------------------------------------------------
    // 1.4  reject
    // ---------------------------------------------------------------
    suspend fun reject(splitId: String, participantId: String) {
        val part = splits.findParticipant(splitId, participantId)
            ?: throw NotFoundException("participant $participantId not in split $splitId")
        if (part.status == SplitParticipantStatus.PAID.name) {
            throw ValidationException("paid participant cannot reject")
        }
        splits.updateParticipantStatus(splitId, participantId, SplitParticipantStatus.REJECTED.name)
        recomputeOverallStatus(splitId)
    }

    // ---------------------------------------------------------------
    // 1.5  tracking
    // ---------------------------------------------------------------
    suspend fun tracking(splitId: String, viewerId: String): SplitPaymentTrackingDto {
        val split = splits.findSplit(splitId) ?: throw NotFoundException("split $splitId not found")
        val parts = splits.listParticipants(splitId)
        val viewerPart = parts.firstOrNull { it.participantId == viewerId }
            ?: throw NotFoundException("viewer $viewerId not in split $splitId")

        val userMap = users.findUsers(parts.map { it.participantId }).associateBy { it.id }
        val initiatorUser = userMap[split.initiatorId]
            ?: throw NotFoundException("initiator user missing")

        return SplitPaymentTrackingDto(
            splitId = splitId,
            role = if (viewerPart.isInitiator) SplitRole.INITIATOR else SplitRole.PARTICIPANT,
            overallStatus = SplitOverallStatus.valueOf(split.status),
            expiresAtMillis = split.expiresAtMillis,
            totalKurus = split.totalKurus,
            yourShareKurus = viewerPart.shareKurus,
            initiator = TrackingInitiatorDto(
                name = initiatorUser.name,
                initials = initiatorUser.initials,
            ),
            participants = parts
                .filter { !it.isInitiator }
                .map { p ->
                    val u = userMap[p.participantId]
                    TrackingParticipantDto(
                        id = p.participantId,
                        name = u?.name ?: p.participantId,
                        initials = u?.initials ?: "?",
                        status = SplitParticipantStatus.valueOf(p.status),
                        amountKurus = p.shareKurus,
                    )
                },
            products = split.products,
        )
    }

    // ---------------------------------------------------------------
    // 1.6  remind
    // ---------------------------------------------------------------
    suspend fun remind(splitId: String, participantId: String) {
        val split = splits.findSplit(splitId) ?: throw NotFoundException("split $splitId not found")
        val part = splits.findParticipant(splitId, participantId)
            ?: throw NotFoundException("participant $participantId not in split $splitId")
        if (part.status != SplitParticipantStatus.PENDING.name) {
            throw ValidationException("only PENDING participants can be reminded")
        }
        // (Hackathon scope: gerçek push yok — log seviyesinde no-op)
        @Suppress("UNUSED_VARIABLE") val _split = split
    }

    // ---------------------------------------------------------------
    // 1.7  cancel
    // ---------------------------------------------------------------
    suspend fun cancel(splitId: String) {
        val split = splits.findSplit(splitId) ?: throw NotFoundException("split $splitId not found")
        if (split.status != SplitOverallStatus.WAITING.name) {
            throw ValidationException("split already ${split.status}")
        }
        splits.updateSplitStatus(splitId, SplitOverallStatus.CANCELLED.name)
    }

    // ---------------------------------------------------------------
    // status recompute
    // ---------------------------------------------------------------
    private suspend fun recomputeOverallStatus(splitId: String) {
        val parts = splits.listParticipants(splitId)
        if (parts.isEmpty()) return
        val allPaid = parts.all { it.status == SplitParticipantStatus.PAID.name }
        if (allPaid) splits.updateSplitStatus(splitId, SplitOverallStatus.COMPLETED.name)
    }
}

/** Helper: SplitProductDto listesini placeholder olarak kullan (initiate'de products yok). */
@Suppress("unused")
private val emptyProducts: List<SplitProductDto> = emptyList()
