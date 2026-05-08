package com.hackathon.service

import com.hackathon.config.TrendException
import com.hackathon.model.FavoriteToggleResponse
import com.hackathon.model.LikeState
import com.hackathon.model.LikeStatusRequest
import com.hackathon.model.ProductDto
import com.hackathon.model.ProductRow
import com.hackathon.repository.ProductRepository

class ProductService(private val productRepo: ProductRepository) {

    suspend fun listAll(userId: String): List<ProductDto> {
        val rows = productRepo.listAll()
        val favorites = productRepo.listFavoriteProductIds(userId)
        return rows.map { it.toDto(favorites.contains(it.id)) }
    }

    suspend fun getById(userId: String, id: String): ProductDto {
        val row = productRepo.findById(id) ?: throw TrendException.productNotFound()
        val favorites = productRepo.listFavoriteProductIds(userId)
        return row.toDto(favorites.contains(row.id))
    }

    suspend fun search(userId: String, query: String): List<ProductDto> {
        if (query.isBlank()) throw TrendException.badRequest("Arama kelimesi gereklidir.")
        val rows = productRepo.search(query)
        val favorites = productRepo.listFavoriteProductIds(userId)
        return rows.map { it.toDto(favorites.contains(it.id)) }
    }

    suspend fun listFavorites(userId: String): List<ProductDto> {
        val rows = productRepo.listFavoriteProducts(userId)
        return rows.map { it.toDto(isFavorite = true) }
    }

    suspend fun toggleFavorite(userId: String, productId: String): FavoriteToggleResponse {
        productRepo.findById(productId) ?: throw TrendException.productNotFound()
        val isFavorite = productRepo.toggleFavorite(userId, productId)
        return FavoriteToggleResponse(isFavorite = isFavorite)
    }

    suspend fun setLike(userId: String, productId: String, req: LikeStatusRequest): LikeState {
        productRepo.findById(productId) ?: throw TrendException.productNotFound()
        val target = req.status.lowercase()
        if (target !in listOf("liked", "disliked", "none")) {
            throw TrendException.badRequest("Status 'liked', 'disliked' veya 'none' olmalidir.")
        }
        if (target == "none") {
            productRepo.clearLikeStatus(userId, productId)
        } else {
            productRepo.setLikeStatus(userId, productId, target)
        }
        val (liked, disliked) = productRepo.countLikes(productId)
        productRepo.updateLikeCounts(productId, liked, disliked)
        val current = productRepo.getLikeStatus(userId, productId) ?: "none"
        return LikeState(status = current, likeCount = liked, dislikeCount = disliked)
    }

    private fun ProductRow.toDto(isFavorite: Boolean): ProductDto = ProductDto(
        id = id,
        brand = brand,
        name = name,
        rating = rating,
        reviewCount = reviewCount,
        price = price,
        imageName = imageName,
        isFavorite = isFavorite,
    )
}
