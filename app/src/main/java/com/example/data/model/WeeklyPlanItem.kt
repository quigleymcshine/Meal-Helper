package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "weekly_plan_items",
    foreignKeys = [
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mealId"])]
)
data class WeeklyPlanItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mealId: Long,
    val dayOfWeek: String, // e.g. "Monday", "Tuesday", etc.
    val weekStartDate: String, // e.g. "2026-09-28"
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)
