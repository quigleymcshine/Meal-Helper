package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.MealDao
import com.example.data.dao.PantryDao
import com.example.data.dao.QuickAddDao
import com.example.data.dao.ShoppingListDao
import com.example.data.dao.WeeklyPlanDao
import com.example.data.model.Meal
import com.example.data.model.MealIngredient
import com.example.data.model.PantryItem
import com.example.data.model.QuickAddItem
import com.example.data.model.ShoppingItem
import com.example.data.model.WeeklyPlanItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Meal::class,
        MealIngredient::class,
        WeeklyPlanItem::class,
        ShoppingItem::class,
        PantryItem::class,
        QuickAddItem::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun weeklyPlanDao(): WeeklyPlanDao
    abstract fun shoppingListDao(): ShoppingListDao
    abstract fun pantryDao(): PantryDao
    abstract fun quickAddDao(): QuickAddDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dinner_planner.db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database)
                    }
                }
            }

            private suspend fun populateInitialData(database: AppDatabase) {
                val mealDao = database.mealDao()
                val pantryDao = database.pantryDao()
                val quickAddDao = database.quickAddDao()

                // 1. Initial Pantry Staples
                val initialPantry = listOf(
                    PantryItem(name = "Olive Oil", quantity = "1 bottle", category = "Oils & Vinegars"),
                    PantryItem(name = "Salt", quantity = "1 container", category = "Spices & Seasoning"),
                    PantryItem(name = "Black Pepper", quantity = "1 grinder", category = "Spices & Seasoning"),
                    PantryItem(name = "Garlic", quantity = "4 heads", category = "Produce"),
                    PantryItem(name = "Butter", quantity = "2 sticks", category = "Dairy"),
                    PantryItem(name = "Spaghetti", quantity = "1 lb", category = "Grains & Pasta"),
                    PantryItem(name = "Soy Sauce", quantity = "1 bottle", category = "Condiments"),
                    PantryItem(name = "Parmesan Cheese", quantity = "1 wedge", category = "Dairy")
                )
                pantryDao.insertPantryItems(initialPantry)

                // 2. Initial Quick Add Independent Items
                val initialQuickAdd = listOf(
                    QuickAddItem(name = "Whole Milk", category = "Dairy", defaultQuantity = "1 gallon", usageCount = 5),
                    QuickAddItem(name = "Sourdough Bread", category = "Bakery", defaultQuantity = "1 loaf", usageCount = 4),
                    QuickAddItem(name = "Large Eggs", category = "Dairy", defaultQuantity = "1 dozen", usageCount = 6),
                    QuickAddItem(name = "Bananas", category = "Produce", defaultQuantity = "1 bunch", usageCount = 4),
                    QuickAddItem(name = "Dark Roast Coffee", category = "Beverages", defaultQuantity = "1 bag", usageCount = 3),
                    QuickAddItem(name = "Greek Yogurt", category = "Dairy", defaultQuantity = "32 oz", usageCount = 2),
                    QuickAddItem(name = "Paper Towels", category = "Household", defaultQuantity = "2 rolls", usageCount = 3)
                )
                quickAddDao.insertQuickAddItems(initialQuickAdd)

                // 3. Pre-populated Meals with rich ingredients
                // Meal 1: Aglio e Olio - completely in pantry! (previously planned)
                val m1 = mealDao.insertMeal(
                    Meal(
                        name = "Garlic Olive Oil Pasta (Aglio e Olio)",
                        category = "Italian",
                        prepTimeMinutes = 20,
                        servings = 4,
                        instructions = "Boil pasta in salted water. Sauté thinly sliced garlic in olive oil until golden. Toss pasta with garlic oil, black pepper, and grated parmesan.",
                        timesPlanned = 2
                    )
                )
                mealDao.insertIngredients(
                    listOf(
                        MealIngredient(mealId = m1, name = "Spaghetti", quantity = "400g"),
                        MealIngredient(mealId = m1, name = "Olive Oil", quantity = "1/3 cup"),
                        MealIngredient(mealId = m1, name = "Garlic", quantity = "6 cloves"),
                        MealIngredient(mealId = m1, name = "Black Pepper", quantity = "1 tsp"),
                        MealIngredient(mealId = m1, name = "Salt", quantity = "1 tbsp"),
                        MealIngredient(mealId = m1, name = "Parmesan Cheese", quantity = "1/2 cup")
                    )
                )

                // Meal 2: Garlic Butter Salmon
                val m2 = mealDao.insertMeal(
                    Meal(
                        name = "Garlic Butter Pan-Seared Salmon",
                        category = "Seafood",
                        prepTimeMinutes = 25,
                        servings = 3,
                        instructions = "Season salmon with salt and pepper. Sear in butter with minced garlic until flaky. Squeeze fresh lemon over fillets and serve with roasted asparagus.",
                        timesPlanned = 1
                    )
                )
                mealDao.insertIngredients(
                    listOf(
                        MealIngredient(mealId = m2, name = "Salmon Fillets", quantity = "3 fillets"),
                        MealIngredient(mealId = m2, name = "Butter", quantity = "3 tbsp"),
                        MealIngredient(mealId = m2, name = "Garlic", quantity = "4 cloves"),
                        MealIngredient(mealId = m2, name = "Fresh Lemon", quantity = "1"),
                        MealIngredient(mealId = m2, name = "Asparagus", quantity = "1 bunch"),
                        MealIngredient(mealId = m2, name = "Salt", quantity = "to taste"),
                        MealIngredient(mealId = m2, name = "Black Pepper", quantity = "to taste")
                    )
                )

                // Meal 3: Classic Beef Tacos
                val m3 = mealDao.insertMeal(
                    Meal(
                        name = "Street Style Beef Tacos",
                        category = "Mexican",
                        prepTimeMinutes = 25,
                        servings = 4,
                        instructions = "Brown ground beef with taco seasoning. Warm corn tortillas and assemble with fresh salsa, shredded cheddar cheese, diced cilantro, and lime.",
                        timesPlanned = 3
                    )
                )
                mealDao.insertIngredients(
                    listOf(
                        MealIngredient(mealId = m3, name = "Ground Beef", quantity = "1 lb"),
                        MealIngredient(mealId = m3, name = "Taco Seasoning", quantity = "1 packet"),
                        MealIngredient(mealId = m3, name = "Corn Tortillas", quantity = "8"),
                        MealIngredient(mealId = m3, name = "Cheddar Cheese", quantity = "1 cup"),
                        MealIngredient(mealId = m3, name = "Fresh Salsa", quantity = "1 jar"),
                        MealIngredient(mealId = m3, name = "Lime", quantity = "2")
                    )
                )

                // Meal 4: Chicken Alfredo
                val m4 = mealDao.insertMeal(
                    Meal(
                        name = "Creamy Chicken Fettuccine Alfredo",
                        category = "Italian",
                        prepTimeMinutes = 35,
                        servings = 4,
                        instructions = "Cook fettuccine. Sauté chicken breast strips in butter and garlic. Simmer heavy cream and parmesan until thick sauce forms. Toss together.",
                        timesPlanned = 0
                    )
                )
                mealDao.insertIngredients(
                    listOf(
                        MealIngredient(mealId = m4, name = "Fettuccine Pasta", quantity = "1 lb"),
                        MealIngredient(mealId = m4, name = "Chicken Breast", quantity = "1.5 lbs"),
                        MealIngredient(mealId = m4, name = "Heavy Cream", quantity = "1 cup"),
                        MealIngredient(mealId = m4, name = "Butter", quantity = "4 tbsp"),
                        MealIngredient(mealId = m4, name = "Parmesan Cheese", quantity = "1 cup"),
                        MealIngredient(mealId = m4, name = "Garlic", quantity = "3 cloves")
                    )
                )

                // Meal 5: Veggie Fried Rice
                val m5 = mealDao.insertMeal(
                    Meal(
                        name = "Quick Savory Fried Rice",
                        category = "Asian",
                        prepTimeMinutes = 20,
                        servings = 4,
                        instructions = "Scramble eggs in hot wok. Add chilled jasmine rice, garlic, soy sauce, and frozen peas & carrots. Drizzle sesame oil and toss thoroughly.",
                        timesPlanned = 1
                    )
                )
                mealDao.insertIngredients(
                    listOf(
                        MealIngredient(mealId = m5, name = "Cooked Jasmine Rice", quantity = "3 cups"),
                        MealIngredient(mealId = m5, name = "Large Eggs", quantity = "3"),
                        MealIngredient(mealId = m5, name = "Soy Sauce", quantity = "3 tbsp"),
                        MealIngredient(mealId = m5, name = "Sesame Oil", quantity = "1 tbsp"),
                        MealIngredient(mealId = m5, name = "Frozen Peas & Carrots", quantity = "1 cup"),
                        MealIngredient(mealId = m5, name = "Garlic", quantity = "2 cloves"),
                        MealIngredient(mealId = m5, name = "Green Onions", quantity = "3 stalks")
                    )
                )
            }
        }
    }
}
