package com.hackathon.service

import com.hackathon.config.TrendException
import com.hackathon.config.newCollectionId
import com.hackathon.model.CollectionDetailDto
import com.hackathon.model.CollectionProductDto
import com.hackathon.model.CollectionRow
import com.hackathon.model.CollectionSummaryDto
import com.hackathon.model.CreateCollectionRequest
import com.hackathon.model.FriendDto
import com.hackathon.model.ProductDto
import com.hackathon.model.ShareCollectionRequest
import com.hackathon.model.ShareCollectionResponse
import com.hackathon.model.UserRow
import com.hackathon.repository.CollectionRepository
import com.hackathon.repository.ProductRepository
import com.hackathon.repository.UserRepository

class CollectionService(
    private val repo: CollectionRepository,
    private val productRepo: ProductRepository,
    private val userRepo: UserRepository,
    private val productService: ProductService,
    private val notificationService: NotificationService,
) {

    suspend fun list(userId: String): List<CollectionSummaryDto> {
        val rows = repo.listOwnedOrParticipating(userId)
        return rows.map { row ->
            CollectionSummaryDto(
                id = row.id,
                name = row.name,
                description = row.description,
                isShared = row.isShared,
                imageName = row.imageName,
                productCount = repo.listProductIds(row.id).size,
                participantCount = repo.listParticipantIds(row.id).size,
            )
        }
    }

    suspend fun create(userId: String, req: CreateCollectionRequest): CollectionDetailDto {
        val name = req.name.trim()
        if (name.length < 2) throw TrendException.badRequest("Koleksiyon adi en az 2 karakter olmali.")
        val row = CollectionRow(
            id = newCollectionId(),
            ownerId = userId,
            name = name,
            description = req.description?.trim()?.takeIf { it.isNotBlank() },
            isShared = req.isShared,
            imageName = req.imageName?.takeIf { it.isNotBlank() } ?: "collection_default",
        )
        val saved = repo.create(row)
        if (req.productIds.isNotEmpty()) {
            // Validate each id exists; ignore unknown rather than failing the whole call
            val valid = req.productIds.distinct().filter { productRepo.findById(it) != null }
            repo.addProducts(saved.id, valid, addedBy = userId)
        }
        // Owner is implicitly a participant in shared collections; record it for clarity
        if (saved.isShared) {
            repo.addParticipants(saved.id, listOf(userId))
        }
        return getDetail(userId, saved.id)
    }

    suspend fun getDetail(userId: String, collectionId: String): CollectionDetailDto {
        val row = repo.findById(collectionId) ?: throw TrendException.notFound("Koleksiyon bulunamadi.")
        if (row.ownerId != userId && userId !in repo.listParticipantIds(row.id)) {
            throw TrendException.forbidden("Bu koleksiyona erisiminiz yok.")
        }
        val productRows = repo.listProducts(row.id)
        // Batch lookup: unique adder ids → name map (avoid N+1 lookups)
        val adderIds = productRows.mapNotNull { it.addedBy }.distinct()
        val adderNames: Map<String, String> = adderIds.mapNotNull { uid ->
            userRepo.findById(uid)?.let { uid to it.name }
        }.toMap()
        val products: List<CollectionProductDto> = productRows.mapNotNull { cpRow ->
            val product: ProductDto =
                runCatching { productService.getById(userId, cpRow.productId) }.getOrNull()
                    ?: return@mapNotNull null
            CollectionProductDto(
                id = product.id,
                brand = product.brand,
                name = product.name,
                rating = product.rating,
                reviewCount = product.reviewCount,
                price = product.price,
                imageName = product.imageName,
                isFavorite = product.isFavorite,
                addedById = cpRow.addedBy,
                addedByName = cpRow.addedBy?.let { adderNames[it] },
                addedAt = cpRow.addedAt,
            )
        }
        val participantIds = repo.listParticipantIds(row.id)
        val participantUsers = if (participantIds.isEmpty()) emptyList() else
            participantIds.mapNotNull { uid -> userRepo.findById(uid) }
        return CollectionDetailDto(
            id = row.id,
            name = row.name,
            description = row.description,
            isShared = row.isShared,
            imageName = row.imageName,
            products = products,
            participants = participantUsers.map { it.toFriendDto() },
        )
    }

    suspend fun addProducts(userId: String, collectionId: String, productIds: List<String>): CollectionDetailDto {
        val row = repo.findById(collectionId) ?: throw TrendException.notFound("Koleksiyon bulunamadi.")
        val isOwner = row.ownerId == userId
        val isParticipant = userId in repo.listParticipantIds(row.id)
        if (!isOwner && !isParticipant) {
            throw TrendException.forbidden("Bu koleksiyona urun ekleyemezsiniz.")
        }
        if (productIds.isEmpty()) throw TrendException.badRequest("'product_ids' bos olamaz.")
        val existing = repo.listProductIds(collectionId).toSet()
        val valid = productIds.distinct()
            .filter { it !in existing }
            .filter { productRepo.findById(it) != null }
        if (valid.isNotEmpty()) {
            repo.addProducts(collectionId, valid, addedBy = userId)
        }
        return getDetail(userId, collectionId)
    }

    suspend fun share(userId: String, collectionId: String, req: ShareCollectionRequest): ShareCollectionResponse {
        val row = repo.findById(collectionId) ?: throw TrendException.notFound("Koleksiyon bulunamadi.")
        if (row.ownerId != userId) throw TrendException.forbidden("Sadece sahibi paylasabilir.")
        val existingParticipants = repo.listParticipantIds(collectionId).toSet()
        val validIds = req.friendIds.distinct().filter { userRepo.findById(it) != null && it != userId }
        repo.addParticipants(collectionId, validIds + userId)
        repo.setShared(collectionId, true)
        // Notify only newly added participants
        val owner = userRepo.findById(userId)
        val ownerName = owner?.name ?: "Bir arkadasin"
        for (fid in validIds) {
            if (fid in existingParticipants) continue
            notificationService.notifyCollectionShared(
                recipientUserId = fid,
                ownerName = ownerName,
                collectionName = row.name,
            )
        }
        val count = repo.listParticipantIds(collectionId).size
        return ShareCollectionResponse(isShared = true, participantCount = count)
    }

    private fun UserRow.toFriendDto() = FriendDto(
        id = id, name = name, avatarUrl = avatarUrl, avatarColorName = avatarColorName,
    )
}
