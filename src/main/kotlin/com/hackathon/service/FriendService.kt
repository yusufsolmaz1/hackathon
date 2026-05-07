package com.hackathon.service

import com.hackathon.config.TrendException
import com.hackathon.model.AddFriendRequest
import com.hackathon.model.FriendDto
import com.hackathon.model.SyncFriendsRequest
import com.hackathon.model.SyncFriendsResponse
import com.hackathon.model.UserRow
import com.hackathon.repository.FriendRepository
import com.hackathon.repository.UserRepository

class FriendService(
    private val friendRepo: FriendRepository,
    private val userRepo: UserRepository,
    private val notificationService: NotificationService,
) {

    suspend fun list(userId: String): List<FriendDto> =
        friendRepo.listFriendUsers(userId).map { it.toDto() }

    suspend fun add(userId: String, req: AddFriendRequest): FriendDto {
        val emailIn = req.email?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        val friendIdIn = req.friendId?.trim()?.takeIf { it.isNotEmpty() }
        if (emailIn == null && friendIdIn == null) {
            throw TrendException.badRequest("'email' veya 'friend_id' gereklidir.")
        }
        val target = when {
            emailIn != null -> userRepo.findByEmail(emailIn) ?: throw TrendException.userNotFound()
            else -> userRepo.findById(friendIdIn!!) ?: throw TrendException.userNotFound()
        }
        if (target.id == userId) throw TrendException.badRequest("Kendinizi arkadas ekleyemezsiniz.")
        friendRepo.addBidirectional(userId, target.id)
        // Notify the added friend
        val requester = userRepo.findById(userId)
        notificationService.notifyFriendRequest(
            recipientUserId = target.id,
            requesterName = requester?.name ?: "Bir kullanici",
        )
        return target.toDto()
    }

    suspend fun remove(userId: String, friendId: String) {
        if (friendId.isBlank()) throw TrendException.badRequest("'id' gereklidir.")
        friendRepo.removeBidirectional(userId, friendId)
    }

    suspend fun sync(userId: String, req: SyncFriendsRequest): SyncFriendsResponse {
        val emails = req.contactEmails.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.distinct()
        val added = mutableListOf<FriendDto>()
        val notFound = mutableListOf<String>()
        for (email in emails) {
            val user = userRepo.findByEmail(email)
            if (user == null) {
                notFound.add(email)
                continue
            }
            if (user.id == userId) continue
            if (!friendRepo.exists(userId, user.id)) {
                friendRepo.addBidirectional(userId, user.id)
                added.add(user.toDto())
                val requester = userRepo.findById(userId)
                notificationService.notifyFriendRequest(
                    recipientUserId = user.id,
                    requesterName = requester?.name ?: "Bir kullanici",
                )
            }
        }
        return SyncFriendsResponse(added = added, notFound = notFound)
    }

    private fun UserRow.toDto() = FriendDto(
        id = id, name = name, avatarUrl = avatarUrl, avatarColorName = avatarColorName,
    )
}
