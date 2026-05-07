package com.hackathon.service

import com.hackathon.config.TrendException
import com.hackathon.config.newNotificationId
import com.hackathon.model.NotificationDto
import com.hackathon.model.NotificationRow
import com.hackathon.repository.NotificationRepository
import org.slf4j.LoggerFactory
import java.util.Locale

/**
 * Notifications module — spec'e birebir uyar:
 *   GET /notifications?after=<iso>     → array of NotificationDto
 *   PUT /notifications/{id}/read       → { "message": "..." }
 *
 * Tetikleyici helper'lar (notify*) diger servislerden cagrilir; insert basarisiz
 * olursa parent operasyon (siparis, arkadas ekleme vb.) bozulmasin diye loglanir
 * ve yutulur.
 */
class NotificationService(private val repo: NotificationRepository) {

    private val logger = LoggerFactory.getLogger(NotificationService::class.java)

    suspend fun list(userId: String, after: String?): List<NotificationDto> =
        repo.listForUser(userId, after).map { it.toDto() }

    suspend fun markRead(userId: String, notificationId: String): String {
        val row = repo.findById(notificationId) ?: throw TrendException.notFound("Bildirim bulunamadi.")
        // Baskasinin bildirimini de NOT_FOUND olarak gizle
        if (row.userId != userId) throw TrendException.notFound("Bildirim bulunamadi.")
        repo.markRead(notificationId)
        return "Bildirim okundu olarak isaretlendi."
    }

    // ─────────────────────────────────────────────────────
    // Trigger helpers — fail-soft
    // ─────────────────────────────────────────────────────

    suspend fun notifyOrderConfirmed(userId: String, orderNumber: String) {
        insertSafe(
            userId = userId,
            type = "order_confirmed",
            title = "Siparis Onaylandi",
            body = "$orderNumber numarali siparisiniz onaylandi.",
        )
    }

    suspend fun notifySplitPaymentReceived(
        recipientUserId: String,
        ownerName: String,
        orderNumber: String,
        amount: Double,
    ) {
        insertSafe(
            userId = recipientUserId,
            type = "split_payment_received",
            title = "Ortak Odeme",
            body = "$ownerName seni $orderNumber siparisine ekledi. ${formatPrice(amount)} odemen var.",
        )
    }

    suspend fun notifyFriendRequest(recipientUserId: String, requesterName: String) {
        insertSafe(
            userId = recipientUserId,
            type = "friend_request",
            title = "Yeni Arkadas",
            body = "$requesterName sizi arkadas olarak ekledi.",
        )
    }

    suspend fun notifyCollectionShared(
        recipientUserId: String,
        ownerName: String,
        collectionName: String,
    ) {
        insertSafe(
            userId = recipientUserId,
            type = "collection_shared",
            title = "Koleksiyon Paylasildi",
            body = "$ownerName '$collectionName' koleksiyonunu seninle paylasti.",
        )
    }

    private suspend fun insertSafe(userId: String, type: String, title: String, body: String) {
        try {
            repo.insert(
                NotificationRow(
                    id = newNotificationId(),
                    userId = userId,
                    type = type,
                    title = title,
                    body = body,
                )
            )
        } catch (e: Exception) {
            logger.warn("notification insert failed: type=$type user=$userId", e)
        }
    }

    private fun NotificationRow.toDto() = NotificationDto(
        id = id,
        title = title,
        body = body,
        type = type,
        createdAt = createdAt ?: "",
        isRead = isRead,
    )

    private fun formatPrice(value: Double): String {
        val formatted = String.format(Locale.US, "%.2f", value).replace('.', ',')
        return "$formatted TL"
    }
}
