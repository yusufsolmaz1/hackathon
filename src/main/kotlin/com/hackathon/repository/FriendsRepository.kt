package com.hackathon.repository

import com.hackathon.model.FriendRow
import com.hackathon.model.UserRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

open class FriendsRepository(private val supabase: SupabaseClient) {

    private val friendsTable = "friends"
    private val usersTable = "users"

    // ---------- users (lookup helpers) ----------

    open suspend fun findUser(userId: String): UserRow? =
        supabase.from(usersTable)
            .select { filter { eq("id", userId) } }
            .decodeSingleOrNull()

    open suspend fun findUserByEmail(email: String): UserRow? =
        supabase.from(usersTable)
            .select { filter { eq("email", email) } }
            .decodeSingleOrNull()

    open suspend fun findUsers(ids: Collection<String>): List<UserRow> {
        if (ids.isEmpty()) return emptyList()
        return supabase.from(usersTable)
            .select { filter { isIn("id", ids.toList()) } }
            .decodeList()
    }

    // ---------- friend graph ----------

    open suspend fun listFriends(userId: String): List<UserRow> {
        val edges = supabase.from(friendsTable)
            .select { filter { eq("user_id", userId) } }
            .decodeList<FriendRow>()
        return findUsers(edges.map { it.friendId })
    }

    open suspend fun isFriend(userId: String, friendId: String): Boolean =
        supabase.from(friendsTable)
            .select {
                filter {
                    eq("user_id", userId)
                    eq("friend_id", friendId)
                }
            }
            .decodeList<FriendRow>()
            .isNotEmpty()

    /** Bidirectional add: her iki yön ayrı satır. */
    open suspend fun addFriend(userId: String, friendId: String) {
        supabase.from(friendsTable).insert(
            listOf(
                FriendRow(userId, friendId),
                FriendRow(friendId, userId),
            ),
        )
    }

    /** Bidirectional delete. */
    open suspend fun deleteFriend(userId: String, friendId: String): Boolean {
        val deleted = supabase.from(friendsTable)
            .delete {
                select()
                filter {
                    or {
                        and {
                            eq("user_id", userId)
                            eq("friend_id", friendId)
                        }
                        and {
                            eq("user_id", friendId)
                            eq("friend_id", userId)
                        }
                    }
                }
            }
            .decodeList<FriendRow>()
        return deleted.isNotEmpty()
    }
}
