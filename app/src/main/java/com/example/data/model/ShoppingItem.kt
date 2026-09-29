package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shopping_items")
data class ShoppingItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val quantity: String = "",
    val category: String = "Produce",
    val isChecked: Boolean = false,
    val sourceMealId: Long? = null,
    val sourceMealName: String? = null,
    val addedTimestamp: Long = System.currentTimeMillis()
)
