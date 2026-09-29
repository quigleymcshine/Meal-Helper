package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.WeeklyPlanItem
import kotlinx.coroutines.flow.Flow

@Dao
interface WeeklyPlanDao {
    @Query("SELECT * FROM weekly_plan_items WHERE weekStartDate = :weekStartDate ORDER BY timestamp ASC")
    fun getPlanForWeek(weekStartDate: String): Flow<List<WeeklyPlanItem>>

    @Query("SELECT * FROM weekly_plan_items ORDER BY timestamp DESC")
    fun getAllPlanItems(): Flow<List<WeeklyPlanItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanItem(item: WeeklyPlanItem): Long

    @Delete
    suspend fun deletePlanItem(item: WeeklyPlanItem)

    @Query("DELETE FROM weekly_plan_items WHERE id = :id")
    suspend fun deletePlanItemById(id: Long)

    @Query("DELETE FROM weekly_plan_items WHERE weekStartDate = :weekStartDate")
    suspend fun clearWeek(weekStartDate: String)
}
