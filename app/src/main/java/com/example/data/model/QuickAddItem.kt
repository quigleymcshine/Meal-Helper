package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_add_items")
data class QuickAddItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val category: String = "Pantry",
    val defaultQuantity: String = "1",
    val usageCount: Int = 1,
    val lastUsedTimestamp: Long = System.currentTimeMillis()
)
