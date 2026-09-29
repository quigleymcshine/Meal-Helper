package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.QuickAddItem
import kotlinx.coroutines.flow.Flow

@Dao
interface QuickAddDao {
    @Query("SELECT * FROM quick_add_items ORDER BY usageCount DESC, lastUsedTimestamp DESC")
    fun getAllQuickAddItems(): Flow<List<QuickAddItem>>

    @Query("SELECT * FROM quick_add_items WHERE LOWER(TRIM(name)) = LOWER(TRIM(:name)) LIMIT 1")
    suspend fun findQuickAddItemByName(name: String): QuickAddItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickAddItem(item: QuickAddItem): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuickAddItems(items: List<QuickAddItem>)

    @Update
    suspend fun updateQuickAddItem(item: QuickAddItem)

    @Delete
    suspend fun deleteQuickAddItem(item: QuickAddItem)

    @Query("DELETE FROM quick_add_items WHERE id = :id")
    suspend fun deleteQuickAddItemById(id: Long)

    @Query("UPDATE quick_add_items SET usageCount = usageCount + 1, lastUsedTimestamp = :timestamp WHERE id = :id")
    suspend fun incrementUsage(id: Long, timestamp: Long)
}
