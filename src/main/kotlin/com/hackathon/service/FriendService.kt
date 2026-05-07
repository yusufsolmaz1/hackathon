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
) {

    suspend fun list(userId: String): List<FriendDto> =
        friendRepo.listFriendUsers(userId).map { it.toDto() }

    suspend fun add(userId: String, req: AddFriendRequest): FriendDto {
        val friendId = req.friendId.trim()
        if (friendId.isBlank()) throw TrendException.badRequest("'friend_id' gereklidir.")
        if (friendId == userId) throw TrendException.badRequest("Kendinizi arkadas ekleyemezsiniz.")
        val target = userRepo.findById(friendId) ?: throw TrendException.userNotFound()
        friendRepo.addBidirectional(userId, friendId)
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
            }
        }
        return SyncFriendsResponse(added = added, notFound = notFound)
    }

    private fun UserRow.toDto() = FriendDto(
        id = id, name = name, avatarUrl = avatarUrl, avatarColorName = avatarColorName,
    )
}
