package com.hackathon.service

import com.hackathon.config.NotFoundException
import com.hackathon.model.OrderDetailDto
import com.hackathon.model.OrderRow
import com.hackathon.repository.OrderRepository

class OrderService(private val repo: OrderRepository) {

    suspend fun getDetail(id: String): OrderDetailDto {
        val row = repo.findById(id) ?: throw NotFoundException("order $id not found")
        return row.toDetailDto()
    }

    private fun OrderRow.toDetailDto(): OrderDetailDto =
        OrderDetailDto(
            id = id,
            // splitId tablodaki denormalized değer, payload zaten içeriyor olabilir.
            // Tek doğru kaynak orders.split_id → response payment_info'ya patchle.
            paymentInfo = paymentInfo.copy(splitId = paymentInfo.splitId ?: splitId),
        )
}
