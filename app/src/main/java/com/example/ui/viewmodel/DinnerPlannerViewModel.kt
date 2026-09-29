package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Meal
import com.example.data.model.MealIngredient
import com.example.data.model.MealWithIngredients
import com.example.data.model.PantryItem
import com.example.data.model.PantryReadyMeal
import com.example.data.model.QuickAddItem
import com.example.data.model.ShoppingItem
import com.example.data.model.WeeklyPlanItem
import com.example.data.repository.AddMealPlanResult
import com.example.data.repository.DinnerPlannerRepository
import com.example.util.DateUtils
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AppTab(val title: String) {
    PLAN("Weekly Plan"),
    SHOPPING("Groceries"),
    PANTRY("Pantry"),
    MEALS("Meals")
}

enum class MealFilter {
    ALL,
    PANTRY_READY,
    PREVIOUSLY_COOKED
}

class DinnerPlannerViewModel(
    application: Application,
    private val repository: DinnerPlannerRepository
) : AndroidViewModel(application) {

    // Active bottom navigation tab
    private val _selectedTab = MutableStateFlow(AppTab.PLAN)
    val selectedTab: StateFlow<AppTab> = _selectedTab.asStateFlow()

    // Week offset from current week (0 = current week, 1 = next week, -1 = last week)
    private val _weekOffset = MutableStateFlow(0)
    val weekOffset: StateFlow<Int> = _weekOffset.asStateFlow()

    // Meals tab filter
    private val _mealFilter = MutableStateFlow(MealFilter.ALL)
    val mealFilter: StateFlow<MealFilter> = _mealFilter.asStateFlow()

    // Search query for meals
    private val _mealSearchQuery = MutableStateFlow("")
    val mealSearchQuery: StateFlow<String> = _mealSearchQuery.asStateFlow()

    // Search query for pantry
    private val _pantrySearchQuery = MutableStateFlow("")
    val pantrySearchQuery: StateFlow<String> = _pantrySearchQuery.asStateFlow()

    // User feedback events (e.g., SnackBar messages)
    private val _snackbarMessage = MutableSharedFlow<String>()
    val snackbarMessage: SharedFlow<String> = _snackbarMessage.asSharedFlow()

    // All meals
    val allMeals: StateFlow<List<MealWithIngredients>> = repository.allMeals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Current week plan items
    val currentWeekPlan: StateFlow<List<WeeklyPlanItem>> = _weekOffset
        .flatMapLatest { offset ->
            repository.getPlanForWeek(DateUtils.getWeekStartDate(offset))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Shopping items
    val shoppingItems: StateFlow<List<ShoppingItem>> = repository.allShoppingItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Pantry items
    val allPantryItems: StateFlow<List<PantryItem>> = repository.allPantryItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Quick add items (independent items saved for quick re-add)
    val quickAddItems: StateFlow<List<QuickAddItem>> = repository.allQuickAddItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Pantry ready meals computation
    val pantryReadyMeals: StateFlow<List<PantryReadyMeal>> = repository.pantryReadyMeals
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Filtered Meals for Meals tab
    val filteredMeals: StateFlow<List<PantryReadyMeal>> = combine(
        pantryReadyMeals,
        _mealFilter,
        _mealSearchQuery
    ) { meals, filter, query ->
        meals.filter { item ->
            val matchesQuery = query.isBlank() ||
                item.mealWithIngredients.meal.name.contains(query, ignoreCase = true) ||
                item.mealWithIngredients.meal.category.contains(query, ignoreCase = true)

            val matchesFilter = when (filter) {
                MealFilter.ALL -> true
                MealFilter.PANTRY_READY -> item.isFullyReady
                MealFilter.PREVIOUSLY_COOKED -> item.isPreviouslyUsed
            }

            matchesQuery && matchesFilter
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered Pantry Items
    val filteredPantryItems: StateFlow<List<PantryItem>> = combine(
        allPantryItems,
        _pantrySearchQuery
    ) { items, query ->
        if (query.isBlank()) items
        else items.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.category.contains(query, ignoreCase = true)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun selectTab(tab: AppTab) {
        _selectedTab.value = tab
    }

    fun nextWeek() {
        _weekOffset.value += 1
    }

    fun previousWeek() {
        _weekOffset.value -= 1
    }

    fun resetToCurrentWeek() {
        _weekOffset.value = 0
    }

    fun setMealFilter(filter: MealFilter) {
        _mealFilter.value = filter
    }

    fun setMealSearchQuery(query: String) {
        _mealSearchQuery.value = query
    }

    fun setPantrySearchQuery(query: String) {
        _pantrySearchQuery.value = query
    }

    /**
     * Add a meal to the weekly plan.
     * Skips ingredients in pantry, adds missing ones to shopping list.
     */
    fun addMealToPlan(mealId: Long, dayOfWeek: String) {
        viewModelScope.launch {
            val weekStartDate = DateUtils.getWeekStartDate(_weekOffset.value)
            val result = repository.addMealToPlan(
                mealId = mealId,
                dayOfWeek = dayOfWeek,
                weekStartDate = weekStartDate
            )
            val msg = buildString {
                append("Planned '${result.mealName}' for ${result.dayOfWeek}!")
                if (result.addedCount > 0) {
                    append(" Added ${result.addedCount} items to shopping list.")
                }
                if (result.skippedInPantryCount > 0) {
                    append(" (${result.skippedInPantryCount} skipped - already in pantry)")
                }
            }
            _snackbarMessage.emit(msg)
        }
    }

    fun removePlanItem(planItemId: Long) {
        viewModelScope.launch {
            repository.deleteWeeklyPlanItem(planItemId)
            _snackbarMessage.emit("Meal removed from plan")
        }
    }

    /**
     * Removes an item from the shopping list.
     * Per requirement: item is considered in the pantry!
     */
    fun removeShoppingItem(item: ShoppingItem, addToPantry: Boolean = true) {
        viewModelScope.launch {
            repository.removeShoppingItem(item, addToPantry = addToPantry)
            if (addToPantry) {
                _snackbarMessage.emit("Moved '${item.name}' to Pantry")
            } else {
                _snackbarMessage.emit("Removed '${item.name}'")
            }
        }
    }

    fun toggleShoppingItemChecked(item: ShoppingItem) {
        viewModelScope.launch {
            repository.toggleShoppingItemChecked(item)
        }
    }

    fun clearCheckedShoppingItems() {
        viewModelScope.launch {
            repository.clearCheckedShoppingItems()
            _snackbarMessage.emit("Cleared checked items")
        }
    }

    /**
     * Adds an independent shopping item and saves it for quick re-add in the future.
     */
    fun addCustomShoppingItem(name: String, quantity: String, category: String) {
        viewModelScope.launch {
            repository.addCustomShoppingItem(name, quantity, category)
            _snackbarMessage.emit("Added '$name' to groceries & saved for quick re-add")
        }
    }

    /**
     * Quickly re-adds a saved item to the grocery list.
     */
    fun quickReaddItem(quickItem: QuickAddItem) {
        viewModelScope.launch {
            repository.quickReaddShoppingItem(quickItem)
            _snackbarMessage.emit("Re-added '${quickItem.name}' to shopping list")
        }
    }

    fun deleteQuickAddItem(quickItem: QuickAddItem) {
        viewModelScope.launch {
            repository.deleteQuickAddItem(quickItem)
            _snackbarMessage.emit("Removed from quick-add suggestions")
        }
    }

    // Pantry operations
    fun addPantryItem(name: String, quantity: String, category: String) {
        viewModelScope.launch {
            repository.addPantryItem(name, quantity, category)
            _snackbarMessage.emit("Added '$name' to Pantry")
        }
    }

    fun removePantryItem(item: PantryItem) {
        viewModelScope.launch {
            repository.removePantryItem(item)
            _snackbarMessage.emit("Removed '${item.name}' from Pantry")
        }
    }

    // Meal operations
    fun saveMeal(meal: Meal, ingredients: List<MealIngredient>) {
        viewModelScope.launch {
            repository.saveMeal(meal, ingredients)
            _snackbarMessage.emit("Saved meal '${meal.name}'")
        }
    }

    fun deleteMeal(meal: Meal) {
        viewModelScope.launch {
            repository.deleteMeal(meal)
            _snackbarMessage.emit("Deleted meal '${meal.name}'")
        }
    }

    companion object {
        fun provideFactory(application: Application): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val db = AppDatabase.getDatabase(
                        application,
                        kotlinx.coroutines.GlobalScope
                    )
                    val repo = DinnerPlannerRepository(
                        mealDao = db.mealDao(),
                        weeklyPlanDao = db.weeklyPlanDao(),
                        shoppingListDao = db.shoppingListDao(),
                        pantryDao = db.pantryDao(),
                        quickAddDao = db.quickAddDao()
                    )
                    return DinnerPlannerViewModel(application, repo) as T
                }
            }
    }
}
