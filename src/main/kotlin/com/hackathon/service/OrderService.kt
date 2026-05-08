package com.hackathon.service

import com.hackathon.config.TrendException
import com.hackathon.config.newOrderId
import com.hackathon.config.newOrderNumber
import com.hackathon.model.CartItemRow
import com.hackathon.model.CreateOrderRequest
import com.hackathon.model.CreateSplitOrderRequest
import com.hackathon.model.OrderDetailDto
import com.hackathon.model.OrderItemDto
import com.hackathon.model.OrderItemRow
import com.hackathon.model.OrderRow
import com.hackathon.model.OrderSplitParticipantRow
import com.hackathon.model.OrderSummaryDto
import com.hackathon.model.SplitParticipantDto
import com.hackathon.model.SplitStatusDto
import com.hackathon.repository.CartRepository
import com.hackathon.repository.FriendRepository
import com.hackathon.repository.OrderRepository
import com.hackathon.repository.UserRepository
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

class OrderService(
    private val orderRepo: OrderRepository,
    private val cartRepo: CartRepository,
    private val friendRepo: FriendRepository,
    private val userRepo: UserRepository,
    private val notificationService: NotificationService,
) {

    suspend fun list(userId: String): List<OrderSummaryDto> {
        val rows = orderRepo.listForUser(userId)
        return rows.map { row ->
            val items = orderRepo.listItems(row.id)
            row.toSummary(items.sumOf { it.quantity })
        }
    }

    suspend fun create(userId: String, req: CreateOrderRequest): OrderDetailDto =
        createInternal(userId, req.cartItemIds, isSplit = false, friendIds = emptyList())

    suspend fun createSplit(userId: String, req: CreateSplitOrderRequest): OrderDetailDto {
        if (req.friendIds.isEmpty()) throw TrendException.badRequest("En az bir arkadas secilmelidir.")
        val validFriendIds = req.friendIds.distinct().filter { it != userId }
        // Verify each friend exists and is a friend
        val myFriends = friendRepo.listFriendIds(userId).toSet()
        for (fid in validFriendIds) {
            if (fid !in myFriends) throw TrendException.badRequest("$fid arkadasiniz degil.")
        }
        return createInternal(userId, req.cartItemIds, isSplit = true, friendIds = validFriendIds)
    }

    suspend fun getDetail(userId: String, orderId: String): OrderDetailDto {
        val row = orderRepo.findById(orderId) ?: throw TrendException.notFound("Siparis bulunamadi.")
        if (row.userId != userId) throw TrendException.forbidden()
        val items = orderRepo.listItems(orderId)
        val splits = if (row.isSplitPayment) orderRepo.listSplitParticipants(orderId) else emptyList()
        return row.toDetail(items, splits)
    }

    suspend fun getSplitStatus(userId: String, orderId: String): SplitStatusDto {
        val row = orderRepo.findById(orderId) ?: throw TrendException.notFound("Siparis bulunamadi.")
        if (row.userId != userId) throw TrendException.forbidden()
        if (!row.isSplitPayment) throw TrendException.badRequest("Bu siparis ortak odeme degil.")
        val splits = orderRepo.listSplitParticipants(orderId)
        val paid = splits.filter { it.hasPaid }.sumOf { it.amount }
        val remaining = (row.totalPrice - paid).coerceAtLeast(0.0)
        return SplitStatusDto(
            orderId = row.id,
            totalPrice = row.totalPrice,
            participants = splits.map { it.toDto() },
            paidAmount = paid,
            remainingAmount = remaining,
            allPaid = splits.isNotEmpty() && splits.all { it.hasPaid },
        )
    }

    private suspend fun createInternal(
        userId: String,
        cartItemIds: List<String>,
        isSplit: Boolean,
        friendIds: List<String>,
    ): OrderDetailDto {
        val cart = cartRepo.list(userId)
        if (cart.isEmpty()) throw TrendException.badRequest("Sepet bos.")
        val selected = if (cartItemIds.isEmpty()) cart else {
            val idSet = cartItemIds.toSet()
            cart.filter { it.id in idSet }
        }
        if (selected.isEmpty()) throw TrendException.badRequest("Secilen sepet kalemleri bulunamadi.")
        val total = selected.sumOf { it.price * it.quantity }
        val now = OffsetDateTime.now(ZoneOffset.UTC)
        val eta = now.plusDays(3)
        val orderId = newOrderId()
        val orderRow = OrderRow(
            id = orderId,
            userId = userId,
            orderNumber = newOrderNumber(),
            totalPrice = total,
            status = if (isSplit) "payment_pending" else "confirmed",
            createdAt = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            estimatedDelivery = eta.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
            isSplitPayment = isSplit,
        )
        val savedOrder = orderRepo.insert(orderRow)
        val itemRows = selected.map { it.toOrderItemRow(orderId) }
        orderRepo.insertItems(itemRows)

        val splitRows = if (isSplit) {
            // Owner pays equal share too
            val participants = friendIds + userId
            val share = total / participants.size
            participants.map { pid ->
                val name = if (pid == userId) "Ben" else (userRepo.findById(pid)?.name ?: pid)
                OrderSplitParticipantRow(
                    orderId = orderId,
                    friendId = pid,
                    name = name,
                    amount = share,
                    hasPaid = false,
                )
            }
        } else emptyList()
        orderRepo.insertSplitParticipants(splitRows)

        // Clear consumed cart items
        for (item in selected) cartRepo.delete(item.id)

        // Notifications (fail-soft)
        if (isSplit) {
            val owner = userRepo.findById(userId)
            val ownerName = owner?.name ?: "Bir arkadasin"
            for (split in splitRows) {
                if (split.friendId == userId) continue
                notificationService.notifySplitPaymentReceived(
                    recipientUserId = split.friendId,
                    ownerName = ownerName,
                    orderNumber = savedOrder.orderNumber,
                    amount = split.amount,
                )
            }
        } else {
            notificationService.notifyOrderConfirmed(userId, savedOrder.orderNumber)
        }

        return savedOrder.toDetail(itemRows, splitRows)
    }

    private fun OrderRow.toSummary(itemCount: Int) = OrderSummaryDto(
        id = id,
        orderNumber = orderNumber,
        totalPrice = totalPrice,
        status = status,
        createdAt = createdAt ?: "",
        estimatedDelivery = estimatedDelivery,
        isSplitPayment = isSplitPayment,
        itemCount = itemCount,
    )

    private fun OrderRow.toDetail(items: List<OrderItemRow>, splits: List<OrderSplitParticipantRow>) =
        OrderDetailDto(
            id = id,
            orderNumber = orderNumber,
            totalPrice = totalPrice,
            status = status,
            createdAt = createdAt ?: "",
            estimatedDelivery = estimatedDelivery,
            isSplitPayment = isSplitPayment,
            items = items.map { it.toDto() },
            splitParticipants = splits.map { it.toDto() },
        )

    private fun OrderItemRow.toDto() = OrderItemDto(
        name = name, brand = brand, quantity = quantity,
        price = price, iconName = iconName,
    )

    private fun OrderSplitParticipantRow.toDto() = SplitParticipantDto(
        friendId = friendId, name = name, amount = amount, hasPaid = hasPaid,
    )

    private fun CartItemRow.toOrderItemRow(orderId: String) = OrderItemRow(
        orderId = orderId,
        name = name,
        brand = brand,
        quantity = quantity,
        price = price,
        iconName = iconName,
    )
}
