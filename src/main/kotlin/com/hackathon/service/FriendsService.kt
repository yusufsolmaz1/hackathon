package com.hackathon.service

import com.hackathon.config.FriendsException
import com.hackathon.config.NotFoundException
import com.hackathon.model.AddFriendResponseDto
import com.hackathon.model.FriendDto
import com.hackathon.model.FriendsErrorCode
import com.hackathon.model.FriendsListResponseDto
import com.hackathon.model.UserRow
import com.hackathon.repository.FriendsRepository

class FriendsService(private val repo: FriendsRepository) {

    private val emailRegex = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    // 2.1
    suspend fun list(userId: String): FriendsListResponseDto =
        FriendsListResponseDto(repo.listFriends(userId).map { it.toDto() })

    // 2.2
    suspend fun add(userId: String, email: String): AddFriendResponseDto {
        val trimmed = email.trim()
        if (!emailRegex.matches(trimmed)) {
            throw FriendsException(FriendsErrorCode.INVALID_EMAIL, "Geçersiz e-posta.")
        }
        val target = repo.findUserByEmail(trimmed)
            ?: throw FriendsException(FriendsErrorCode.USER_NOT_FOUND, "Kullanıcı bulunamadı.")
        if (target.id == userId) {
            throw FriendsException(FriendsErrorCode.SELF_ADD, "Kendinizi arkadaş ekleyemezsiniz.")
        }
        if (repo.isFriend(userId, target.id)) {
            throw FriendsException(FriendsErrorCode.ALREADY_FRIEND, "Bu kullanıcı zaten arkadaşınız.")
        }
        repo.addFriend(userId, target.id)
        return AddFriendResponseDto(target.toDto())
    }

    // 2.3
    suspend fun delete(userId: String, friendId: String) {
        val ok = repo.deleteFriend(userId, friendId)
        if (!ok) throw NotFoundException("friend $friendId not found")
    }

    private fun UserRow.toDto() = FriendDto(
        id = id,
        name = name,
        email = email ?: "",
        initials = initials,
        avatarUrl = avatarUrl,
    )
}
