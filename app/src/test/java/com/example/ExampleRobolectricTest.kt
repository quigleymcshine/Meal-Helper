package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Meal
import com.example.data.model.MealIngredient
import com.example.data.model.PantryItem
import com.example.data.repository.DinnerPlannerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: DinnerPlannerRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = DinnerPlannerRepository(
            mealDao = db.mealDao(),
            weeklyPlanDao = db.weeklyPlanDao(),
            shoppingListDao = db.shoppingListDao(),
            pantryDao = db.pantryDao(),
            quickAddDao = db.quickAddDao()
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun testAppName() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Dinner Planner", appName)
    }

    @Test
    fun testPlanMealSkipsPantryIngredientsAndAddsMissing() = runBlocking {
        // 1. Create a meal with 3 ingredients: Olive Oil, Garlic, Salmon Fillets
        val mealId = repository.saveMeal(
            Meal(name = "Pan-Seared Salmon", category = "Seafood", prepTimeMinutes = 20),
            listOf(
                MealIngredient(mealId = 0, name = "Olive Oil", quantity = "2 tbsp"),
                MealIngredient(mealId = 0, name = "Garlic", quantity = "3 cloves"),
                MealIngredient(mealId = 0, name = "Salmon Fillets", quantity = "2 pieces")
            )
        )

        // 2. Add Olive Oil and Garlic to the pantry
        repository.addPantryItem("Olive Oil", "1 bottle", "Oils")
        repository.addPantryItem("Garlic", "4 heads", "Produce")

        // 3. Add meal to weekly plan
        val result = repository.addMealToPlan(
            mealId = mealId,
            dayOfWeek = "Tuesday",
            weekStartDate = "2026-09-28"
        )

        assertEquals("Pan-Seared Salmon", result.mealName)
        assertEquals(1, result.addedCount) // Only Salmon Fillets
        assertEquals(2, result.skippedInPantryCount) // Olive Oil & Garlic skipped!

        val shoppingList = repository.allShoppingItems.first()
        assertEquals(1, shoppingList.size)
        assertEquals("Salmon Fillets", shoppingList[0].name)
    }

    @Test
    fun testRemovingShoppingListItemAddsToPantry() = runBlocking {
        // Add a shopping item
        repository.addCustomShoppingItem("Avocado", "3", "Produce")
        val shoppingBefore = repository.allShoppingItems.first()
        assertEquals(1, shoppingBefore.size)

        // Remove item from shopping list
        repository.removeShoppingItem(shoppingBefore[0], addToPantry = true)

        val shoppingAfter = repository.allShoppingItems.first()
        assertEquals(0, shoppingAfter.size)

        // Verify it was moved to pantry
        val pantryList = repository.allPantryItems.first()
        val foundInPantry = pantryList.find { it.name.equals("Avocado", ignoreCase = true) }
        assertNotNull(foundInPantry)
    }

    @Test
    fun testIndependentShoppingItemsSavedForQuickReadd() = runBlocking {
        // Add an independent item
        repository.addCustomShoppingItem("Oat Milk", "1 carton", "Dairy")

        // Verify it is in quick add catalog
        val quickAddList = repository.allQuickAddItems.first()
        val found = quickAddList.find { it.name.equals("Oat Milk", ignoreCase = true) }
        assertNotNull(found)

        // Quick re-add it
        repository.quickReaddShoppingItem(found!!)
        val shoppingList = repository.allShoppingItems.first()
        assertEquals(2, shoppingList.size) // 1 from initial add + 1 from quick re-add
    }

    @Test
    fun testPantryReadyMealsIdentified() = runBlocking {
        // Create meal where all ingredients are in pantry
        val mealId = repository.saveMeal(
            Meal(name = "Garlic Toast", category = "Quick", prepTimeMinutes = 10, timesPlanned = 1),
            listOf(
                MealIngredient(mealId = 0, name = "Bread", quantity = "4 slices"),
                MealIngredient(mealId = 0, name = "Butter", quantity = "2 tbsp"),
                MealIngredient(mealId = 0, name = "Garlic", quantity = "2 cloves")
            )
        )

        repository.addPantryItem("Bread", "1 loaf", "Bakery")
        repository.addPantryItem("Butter", "1 stick", "Dairy")
        repository.addPantryItem("Garlic", "3 cloves", "Produce")

        val readyMeals = repository.pantryReadyMeals.first()
        val toast = readyMeals.find { it.mealWithIngredients.meal.id == mealId }

        assertNotNull(toast)
        assertTrue(toast!!.isFullyReady)
        assertTrue(toast.isPreviouslyUsed)
        assertEquals(3, toast.inPantryCount)
        assertEquals(0, toast.missingIngredients.size)
    }
}
