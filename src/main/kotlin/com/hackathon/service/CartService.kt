package com.hackathon.service

import com.hackathon.config.TrendException
import com.hackathon.config.newCartItemId
import com.hackathon.model.AddCartItemRequest
import com.hackathon.model.CartItemDto
import com.hackathon.model.CartItemRow
import com.hackathon.model.CartResponse
import com.hackathon.model.UpdateCartItemRequest
import com.hackathon.repository.CartRepository
import com.hackathon.repository.ProductRepository

class CartService(
    private val cartRepo: CartRepository,
    private val productRepo: ProductRepository,
) {

    suspend fun list(userId: String): CartResponse {
        val rows = cartRepo.list(userId)
        return rows.toResponse()
    }

    suspend fun add(userId: String, req: AddCartItemRequest): CartItemDto {
        if (req.size.isBlank()) throw TrendException.badRequest("'size' gereklidir.")
        if (req.quantity <= 0) throw TrendException.badRequest("'quantity' 0'dan buyuk olmalidir.")
        val product = productRepo.findById(req.productId) ?: throw TrendException.productNotFound()
        // Same product + size → bump quantity
        val existing = cartRepo.findByProductAndSize(userId, product.id, req.size)
        val saved = if (existing != null) {
            cartRepo.updateQuantity(existing.id, existing.quantity + req.quantity)
        } else {
            cartRepo.insert(
                CartItemRow(
                    id = newCartItemId(),
                    userId = userId,
                    productId = product.id,
                    brand = product.brand,
                    name = product.name,
                    size = req.size.trim(),
                    price = product.price,
                    quantity = req.quantity,
                    iconName = product.imageName,
                )
            )
        }
        return saved.toDto()
    }

    suspend fun updateQuantity(userId: String, itemId: String, req: UpdateCartItemRequest): CartItemDto {
        val existing = cartRepo.findById(itemId) ?: throw TrendException.notFound("Sepet kalemi bulunamadi.")
        if (existing.userId != userId) throw TrendException.forbidden()
        if (req.quantity <= 0) throw TrendException.badRequest("'quantity' 0'dan buyuk olmalidir.")
        val updated = cartRepo.updateQuantity(itemId, req.quantity)
        return updated.toDto()
    }

    suspend fun remove(userId: String, itemId: String) {
        val existing = cartRepo.findById(itemId) ?: throw TrendException.notFound("Sepet kalemi bulunamadi.")
        if (existing.userId != userId) throw TrendException.forbidden()
        cartRepo.delete(itemId)
    }

    private fun List<CartItemRow>.toResponse(): CartResponse {
        val total = sumOf { it.price * it.quantity }
        val items = map { it.toDto() }
        return CartResponse(items = items, totalPrice = formatPrice(total), itemCount = sumOf { it.quantity })
    }

    private fun CartItemRow.toDto() = CartItemDto(
        id = id,
        productId = productId,
        brand = brand,
        name = name,
        size = size,
        price = formatPrice(price),
        quantity = quantity,
        iconName = iconName,
    )

    private fun formatPrice(value: Double): String {
        val formatted = String.format(java.util.Locale.US, "%.2f", value).replace('.', ',')
        return "$formatted TL"
    }
}
