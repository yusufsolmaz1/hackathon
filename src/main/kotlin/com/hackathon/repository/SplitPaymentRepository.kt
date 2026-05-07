package com.hackathon.repository

import com.hackathon.model.SplitParticipantRow
import com.hackathon.model.SplitRow
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

open class SplitPaymentRepository(private val supabase: SupabaseClient) {

    private val splits = "splits"
    private val participants = "split_participants"

    // ---------- splits ----------

    open suspend fun insertSplit(row: SplitRow): SplitRow =
        supabase.from(splits).insert(row) { select() }.decodeSingle()

    open suspend fun findSplit(splitId: String): SplitRow? =
        supabase.from(splits)
            .select { filter { eq("id", splitId) } }
            .decodeSingleOrNull()

    open suspend fun updateSplitStatus(splitId: String, status: String): SplitRow? =
        supabase.from(splits)
            .update(buildJsonObject { put("status", JsonPrimitive(status)) }) {
                select()
                filter { eq("id", splitId) }
            }
            .decodeSingleOrNull()

    open suspend fun setInitiatorOrderId(splitId: String, orderId: String): SplitRow? =
        supabase.from(splits)
            .update(buildJsonObject { put("initiator_order_id", JsonPrimitive(orderId)) }) {
                select()
                filter { eq("id", splitId) }
            }
            .decodeSingleOrNull()

    // ---------- participants ----------

    open suspend fun insertParticipants(rows: List<SplitParticipantRow>): List<SplitParticipantRow> =
        supabase.from(participants).insert(rows) { select() }.decodeList()

    open suspend fun listParticipants(splitId: String): List<SplitParticipantRow> =
        supabase.from(participants)
            .select { filter { eq("split_id", splitId) } }
            .decodeList()

    open suspend fun findParticipant(splitId: String, participantId: String): SplitParticipantRow? =
        supabase.from(participants)
            .select {
                filter {
                    eq("split_id", splitId)
                    eq("participant_id", participantId)
                }
            }
            .decodeSingleOrNull()

    open suspend fun updateParticipantStatus(
        splitId: String,
        participantId: String,
        status: String,
        paidOrderId: String? = null,
    ): SplitParticipantRow? {
        val patch = buildJsonObject {
            put("status", JsonPrimitive(status))
            if (paidOrderId != null) put("paid_order_id", JsonPrimitive(paidOrderId))
        }
        return supabase.from(participants)
            .update(patch) {
                select()
                filter {
                    eq("split_id", splitId)
                    eq("participant_id", participantId)
                }
            }
            .decodeSingleOrNull()
    }
}
