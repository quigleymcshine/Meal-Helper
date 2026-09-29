package com.example.data.model

data class PantryReadyMeal(
    val mealWithIngredients: MealWithIngredients,
    val inPantryCount: Int,
    val totalCount: Int,
    val inPantryIngredients: List<String>,
    val missingIngredients: List<String>,
    val isFullyReady: Boolean,
    val isPreviouslyUsed: Boolean
)
