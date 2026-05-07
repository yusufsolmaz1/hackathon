package com.hackathon.repository

import com.hackathon.model.Item
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from

open class ItemRepository(private val supabase: SupabaseClient) {

    private val table = "items"

    open suspend fun listAll(): List<Item> =
        supabase.from(table).select().decodeList()

    open suspend fun findById(id: String): Item? =
        supabase.from(table)
            .select { filter { eq("id", id) } }
            .decodeSingleOrNull()

    open suspend fun create(item: Item): Item =
        supabase.from(table)
            .insert(item) { select() }
            .decodeSingle()

    open suspend fun update(id: String, item: Item): Item? =
        supabase.from(table)
            .update(item) {
                select()
                filter { eq("id", id) }
            }
            .decodeSingleOrNull()

    open suspend fun delete(id: String): Boolean {
        val deleted = supabase.from(table)
            .delete {
                select()
                filter { eq("id", id) }
            }
            .decodeList<Item>()
        return deleted.isNotEmpty()
    }
}
