package com.hackathon.model

import kotlinx.serialization.Serializable

@Serializable
data class Item(
    val id: String? = null,
    val name: String,
    val description: String? = null,
)
