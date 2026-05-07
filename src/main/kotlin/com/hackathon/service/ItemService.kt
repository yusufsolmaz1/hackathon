package com.hackathon.service

import com.hackathon.config.NotFoundException
import com.hackathon.config.ValidationException
import com.hackathon.model.Item
import com.hackathon.repository.ItemRepository

class ItemService(private val repository: ItemRepository) {

    suspend fun listAll(): List<Item> = repository.listAll()

    suspend fun getById(id: String): Item =
        repository.findById(id) ?: throw NotFoundException("Item $id not found")

    suspend fun create(item: Item): Item {
        if (item.name.isBlank()) throw ValidationException("name must not be blank")
        return repository.create(item.copy(id = null))
    }

    suspend fun update(id: String, item: Item): Item =
        repository.update(id, item.copy(id = null))
            ?: throw NotFoundException("Item $id not found")

    suspend fun delete(id: String) {
        val ok = repository.delete(id)
        if (!ok) throw NotFoundException("Item $id not found")
    }
}
