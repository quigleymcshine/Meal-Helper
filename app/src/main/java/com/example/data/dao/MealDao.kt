package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.data.model.Meal
import com.example.data.model.MealIngredient
import com.example.data.model.MealWithIngredients
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Transaction
    @Query("SELECT * FROM meals ORDER BY name ASC")
    fun getAllMealsWithIngredients(): Flow<List<MealWithIngredients>>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :mealId")
    suspend fun getMealWithIngredientsById(mealId: Long): MealWithIngredients?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: Meal): Long

    @Update
    suspend fun updateMeal(meal: Meal)

    @Delete
    suspend fun deleteMeal(meal: Meal)

    @Query("DELETE FROM meals WHERE id = :mealId")
    suspend fun deleteMealById(mealId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIngredients(ingredients: List<MealIngredient>)

    @Query("DELETE FROM meal_ingredients WHERE mealId = :mealId")
    suspend fun deleteIngredientsForMeal(mealId: Long)

    @Query("UPDATE meals SET timesPlanned = timesPlanned + 1 WHERE id = :mealId")
    suspend fun incrementTimesPlanned(mealId: Long)
}
