package com.hackathon.repository

import com.hackathon.model.FriendEdgeRow
import com.hackathon.model.UserRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

open class FriendRepository(private val supabase: SupabaseClient) {
    private val friends = "friends"
    private val users = "users"

    open suspend fun listFriendIds(userId: String): List<String> =
        supabase.from(friends).select { filter { eq("user_id", userId) } }
            .decodeList<FriendEdgeRow>().map { it.friendId }

    open suspend fun listFriendUsers(userId: String): List<UserRow> {
        val ids = listFriendIds(userId)
        if (ids.isEmpty()) return emptyList()
        return supabase.from(users).select { filter { isIn("id", ids) } }.decodeList()
    }

    open suspend fun exists(userId: String, friendId: String): Boolean =
        supabase.from(friends).select {
            filter { eq("user_id", userId); eq("friend_id", friendId) }
        }.decodeSingleOrNull<FriendEdgeRow>() != null

    open suspend fun addBidirectional(userId: String, friendId: String) {
        // Insert both directions; ignore primary-key conflicts via existence check
        if (!exists(userId, friendId)) {
            supabase.from(friends).insert(FriendEdgeRow(userId = userId, friendId = friendId))
        }
        if (!exists(friendId, userId)) {
            supabase.from(friends).insert(FriendEdgeRow(userId = friendId, friendId = userId))
        }
    }

    open suspend fun removeBidirectional(userId: String, friendId: String) {
        supabase.from(friends).delete {
            filter { eq("user_id", userId); eq("friend_id", friendId) }
        }
        supabase.from(friends).delete {
            filter { eq("user_id", friendId); eq("friend_id", userId) }
        }
    }
}
