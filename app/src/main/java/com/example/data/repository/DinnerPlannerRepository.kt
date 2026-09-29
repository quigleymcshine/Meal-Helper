package com.example.data.repository

import com.example.data.dao.MealDao
import com.example.data.dao.PantryDao
import com.example.data.dao.QuickAddDao
import com.example.data.dao.ShoppingListDao
import com.example.data.dao.WeeklyPlanDao
import com.example.data.model.Meal
import com.example.data.model.MealIngredient
import com.example.data.model.MealWithIngredients
import com.example.data.model.PantryItem
import com.example.data.model.PantryReadyMeal
import com.example.data.model.QuickAddItem
import com.example.data.model.ShoppingItem
import com.example.data.model.WeeklyPlanItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class AddMealPlanResult(
    val mealName: String,
    val dayOfWeek: String,
    val addedCount: Int,
    val skippedInPantryCount: Int,
    val skippedIngredientNames: List<String>
)

class DinnerPlannerRepository(
    private val mealDao: MealDao,
    private val weeklyPlanDao: WeeklyPlanDao,
    private val shoppingListDao: ShoppingListDao,
    private val pantryDao: PantryDao,
    private val quickAddDao: QuickAddDao
) {
    val allMeals: Flow<List<MealWithIngredients>> = mealDao.getAllMealsWithIngredients()
    val allShoppingItems: Flow<List<ShoppingItem>> = shoppingListDao.getAllShoppingItems()
    val allPantryItems: Flow<List<PantryItem>> = pantryDao.getAllPantryItems()
    val allQuickAddItems: Flow<List<QuickAddItem>> = quickAddDao.getAllQuickAddItems()

    fun getPlanForWeek(weekStartDate: String): Flow<List<WeeklyPlanItem>> {
        return weeklyPlanDao.getPlanForWeek(weekStartDate)
    }

    /**
     * Flow combining all meals and pantry items to assess ingredient availability.
     * Computes matching status, highlighting previously used meals with 100% pantry ingredients.
     */
    val pantryReadyMeals: Flow<List<PantryReadyMeal>> = combine(
        allMeals,
        allPantryItems
    ) { meals, pantryItems ->
        val pantryNamesNormalized = pantryItems.map { it.name.trim().lowercase() }

        meals.map { mealWithIngredients ->
            val ingredients = mealWithIngredients.ingredients
            val totalCount = ingredients.size
            val inPantry = mutableListOf<String>()
            val missing = mutableListOf<String>()

            for (ingredient in ingredients) {
                val ingName = ingredient.name.trim()
                val ingNorm = ingName.lowercase()
                val matchesPantry = pantryNamesNormalized.any { pName ->
                    pName == ingNorm ||
                    pName.contains(ingNorm) ||
                    ingNorm.contains(pName)
                }

                if (matchesPantry) {
                    inPantry.add(ingName)
                } else {
                    missing.add(ingName)
                }
            }

            PantryReadyMeal(
                mealWithIngredients = mealWithIngredients,
                inPantryCount = inPantry.size,
                totalCount = totalCount,
                inPantryIngredients = inPantry,
                missingIngredients = missing,
                isFullyReady = missing.isEmpty() && totalCount > 0,
                isPreviouslyUsed = mealWithIngredients.meal.timesPlanned > 0
            )
        }
    }

    /**
     * Adds a meal to the weekly plan for a specific day.
     * Automatically inspects ingredients against current pantry inventory:
     * - Ingredients already in pantry are NOT added to the shopping list.
     * - Ingredients not in pantry are added to the shopping list.
     * - Tracks timesPlanned on the meal.
     */
    suspend fun addMealToPlan(
        mealId: Long,
        dayOfWeek: String,
        weekStartDate: String,
        notes: String = ""
    ): AddMealPlanResult {
        // 1. Add plan item
        weeklyPlanDao.insertPlanItem(
            WeeklyPlanItem(
                mealId = mealId,
                dayOfWeek = dayOfWeek,
                weekStartDate = weekStartDate,
                notes = notes
            )
        )

        // 2. Increment times planned for this meal
        mealDao.incrementTimesPlanned(mealId)

        // 3. Check ingredients against pantry
        val mealWithIngredients = mealDao.getMealWithIngredientsById(mealId)
            ?: return AddMealPlanResult("Meal", dayOfWeek, 0, 0, emptyList())

        val pantryList = pantryDao.getPantryItemsList()
        val pantryNamesNormalized = pantryList.map { it.name.trim().lowercase() }

        val itemsToAddToShopping = mutableListOf<ShoppingItem>()
        val skippedIngredients = mutableListOf<String>()

        for (ingredient in mealWithIngredients.ingredients) {
            val ingName = ingredient.name.trim()
            val ingNorm = ingName.lowercase()

            val isInPantry = pantryNamesNormalized.any { pName ->
                pName == ingNorm || pName.contains(ingNorm) || ingNorm.contains(pName)
            }

            if (isInPantry) {
                skippedIngredients.add(ingName)
            } else {
                itemsToAddToShopping.add(
                    ShoppingItem(
                        name = ingName,
                        quantity = ingredient.quantity,
                        category = determineCategory(ingName),
                        sourceMealId = mealWithIngredients.meal.id,
                        sourceMealName = mealWithIngredients.meal.name
                    )
                )
            }
        }

        if (itemsToAddToShopping.isNotEmpty()) {
            shoppingListDao.insertShoppingItems(itemsToAddToShopping)
        }

        return AddMealPlanResult(
            mealName = mealWithIngredients.meal.name,
            dayOfWeek = dayOfWeek,
            addedCount = itemsToAddToShopping.size,
            skippedInPantryCount = skippedIngredients.size,
            skippedIngredientNames = skippedIngredients
        )
    }

    /**
     * When an item is removed from the shopping list, it is considered in the pantry
     * (as required by: "If a meal is added to the weekly plan and then an ingredient is removed
     * from the shopping list that item should be considered in the pantry.")
     */
    suspend fun removeShoppingItem(item: ShoppingItem, addToPantry: Boolean = true) {
        if (addToPantry) {
            val existingPantry = pantryDao.findPantryItemByName(item.name)
            if (existingPantry == null) {
                pantryDao.insertPantryItem(
                    PantryItem(
                        name = item.name.trim(),
                        quantity = if (item.quantity.isNotBlank()) item.quantity else "1",
                        category = item.category
                    )
                )
            }
        }
        shoppingListDao.deleteShoppingItem(item)
    }

    suspend fun toggleShoppingItemChecked(item: ShoppingItem) {
        val updated = item.copy(isChecked = !item.isChecked)
        shoppingListDao.updateShoppingItem(updated)
    }

    suspend fun clearCheckedShoppingItems(addToPantry: Boolean = true) {
        // Find checked items and optionally add to pantry before clearing
        // Note: For explicit individual item remove, removeShoppingItem already does this.
        shoppingListDao.deleteCheckedItems()
    }

    /**
     * Add independent item to shopping list (not tied to a meal).
     * Saves the item to the Quick Add catalog so it can be quickly re-added in the future.
     */
    suspend fun addCustomShoppingItem(
        name: String,
        quantity: String = "",
        category: String = "Groceries"
    ) {
        val trimmedName = name.trim()
        if (trimmedName.isBlank()) return

        // 1. Add to shopping list
        shoppingListDao.insertShoppingItem(
            ShoppingItem(
                name = trimmedName,
                quantity = quantity.trim(),
                category = category,
                sourceMealId = null,
                sourceMealName = null
            )
        )

        // 2. Save / update in Quick Add catalog
        val existing = quickAddDao.findQuickAddItemByName(trimmedName)
        if (existing != null) {
            quickAddDao.incrementUsage(existing.id, System.currentTimeMillis())
        } else {
            quickAddDao.insertQuickAddItem(
                QuickAddItem(
                    name = trimmedName,
                    category = category,
                    defaultQuantity = if (quantity.isNotBlank()) quantity.trim() else "1",
                    usageCount = 1,
                    lastUsedTimestamp = System.currentTimeMillis()
                )
            )
        }
    }

    /**
     * Quickly re-add a saved independent item to the shopping list.
     */
    suspend fun quickReaddShoppingItem(quickItem: QuickAddItem) {
        shoppingListDao.insertShoppingItem(
            ShoppingItem(
                name = quickItem.name,
                quantity = quickItem.defaultQuantity,
                category = quickItem.category,
                sourceMealId = null,
                sourceMealName = null
            )
        )
        quickAddDao.incrementUsage(quickItem.id, System.currentTimeMillis())
    }

    // Pantry Management
    suspend fun addPantryItem(name: String, quantity: String = "", category: String = "Pantry") {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        val existing = pantryDao.findPantryItemByName(trimmed)
        if (existing != null) {
            pantryDao.updatePantryItem(
                existing.copy(
                    quantity = if (quantity.isNotBlank()) quantity.trim() else existing.quantity,
                    category = category
                )
            )
        } else {
            pantryDao.insertPantryItem(
                PantryItem(
                    name = trimmed,
                    quantity = quantity.trim(),
                    category = category
                )
            )
        }
    }

    suspend fun removePantryItem(item: PantryItem) {
        pantryDao.deletePantryItem(item)
    }

    suspend fun deletePantryItemById(id: Long) {
        pantryDao.deletePantryItemById(id)
    }

    // Meal Management
    suspend fun saveMeal(
        meal: Meal,
        ingredients: List<MealIngredient>
    ): Long {
        val mealId = if (meal.id == 0L) {
            mealDao.insertMeal(meal)
        } else {
            mealDao.updateMeal(meal)
            mealDao.deleteIngredientsForMeal(meal.id)
            meal.id
        }

        val ingredientsWithMealId = ingredients.map {
            it.copy(mealId = mealId, name = it.name.trim())
        }
        mealDao.insertIngredients(ingredientsWithMealId)
        return mealId
    }

    suspend fun deleteMeal(meal: Meal) {
        mealDao.deleteMeal(meal)
    }

    // Weekly Plan Management
    suspend fun deleteWeeklyPlanItem(planItemId: Long) {
        weeklyPlanDao.deletePlanItemById(planItemId)
    }

    suspend fun clearWeek(weekStartDate: String) {
        weeklyPlanDao.clearWeek(weekStartDate)
    }

    suspend fun deleteQuickAddItem(item: QuickAddItem) {
        quickAddDao.deleteQuickAddItem(item)
    }

    private fun determineCategory(name: String): String {
        val lower = name.lowercase()
        return when {
            lower.contains("beef") || lower.contains("chicken") || lower.contains("pork") ||
            lower.contains("salmon") || lower.contains("fish") || lower.contains("shrimp") ||
            lower.contains("meat") || lower.contains("bacon") || lower.contains("pancetta") -> "Meat & Seafood"

            lower.contains("onion") || lower.contains("garlic") || lower.contains("tomato") ||
            lower.contains("lemon") || lower.contains("lime") || lower.contains("asparagus") ||
            lower.contains("lettuce") || lower.contains("herb") || lower.contains("basil") ||
            lower.contains("cilantro") || lower.contains("ginger") || lower.contains("mushroom") ||
            lower.contains("carrot") || lower.contains("apple") || lower.contains("banana") -> "Produce"

            lower.contains("milk") || lower.contains("cheese") || lower.contains("cream") ||
            lower.contains("butter") || lower.contains("yogurt") || lower.contains("cheddar") ||
            lower.contains("parmesan") || lower.contains("feta") -> "Dairy"

            lower.contains("pasta") || lower.contains("spaghetti") || lower.contains("rice") ||
            lower.contains("noodle") || lower.contains("tortilla") || lower.contains("bread") ||
            lower.contains("flour") || lower.contains("fettuccine") -> "Bakery & Grains"

            lower.contains("pepper") || lower.contains("salt") || lower.contains("sauce") ||
            lower.contains("oil") || lower.contains("seasoning") || lower.contains("vinegar") ||
            lower.contains("broth") || lower.contains("salsa") -> "Pantry & Spices"

            else -> "Groceries"
        }
    }
}
