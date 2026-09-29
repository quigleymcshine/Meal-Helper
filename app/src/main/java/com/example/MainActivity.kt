package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Kitchen
import androidx.compose.material.icons.outlined.RestaurantMenu
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.PantryReadyMeal
import com.example.ui.components.MealDetailDialog
import com.example.ui.components.PlanMealSelectDayDialog
import com.example.ui.screens.MealsScreen
import com.example.ui.screens.PantryScreen
import com.example.ui.screens.ShoppingListScreen
import com.example.ui.screens.WeeklyPlanScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AppTab
import com.example.ui.viewmodel.DinnerPlannerViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val app = LocalContext.current.applicationContext as android.app.Application
                val viewModel: DinnerPlannerViewModel = viewModel(
                    factory = DinnerPlannerViewModel.provideFactory(app)
                )
                DinnerPlannerApp(viewModel = viewModel)
            }
        }
    }
}

@Composable
fun DinnerPlannerApp(viewModel: DinnerPlannerViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val weekOffset by viewModel.weekOffset.collectAsStateWithLifecycle()
    val currentWeekPlan by viewModel.currentWeekPlan.collectAsStateWithLifecycle()
    val allMeals by viewModel.allMeals.collectAsStateWithLifecycle()
    val shoppingItems by viewModel.shoppingItems.collectAsStateWithLifecycle()
    val allPantryItems by viewModel.allPantryItems.collectAsStateWithLifecycle()
    val filteredPantryItems by viewModel.filteredPantryItems.collectAsStateWithLifecycle()
    val quickAddItems by viewModel.quickAddItems.collectAsStateWithLifecycle()
    val pantryReadyMeals by viewModel.pantryReadyMeals.collectAsStateWithLifecycle()
    val filteredMeals by viewModel.filteredMeals.collectAsStateWithLifecycle()
    val mealFilter by viewModel.mealFilter.collectAsStateWithLifecycle()
    val mealSearchQuery by viewModel.mealSearchQuery.collectAsStateWithLifecycle()
    val pantrySearchQuery by viewModel.pantrySearchQuery.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    var selectedMealForDetail by remember { mutableStateOf<PantryReadyMeal?>(null) }
    var mealToPlanForDay by remember { mutableStateOf<PantryReadyMeal?>(null) }

    // Listen for snackbar notifications
    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    // Handle back button: return to Weekly Plan if on secondary tab
    BackHandler(enabled = selectedTab != AppTab.PLAN) {
        viewModel.selectTab(AppTab.PLAN)
    }

    val activeGroceriesCount = remember(shoppingItems) {
        shoppingItems.count { !it.isChecked }
    }
    val readyMealsCount = remember(pantryReadyMeals) {
        pantryReadyMeals.count { it.isFullyReady }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                modifier = Modifier.testTag("bottom_nav_bar")
            ) {
                // Weekly Plan
                NavigationBarItem(
                    selected = selectedTab == AppTab.PLAN,
                    onClick = { viewModel.selectTab(AppTab.PLAN) },
                    icon = {
                        Icon(
                            if (selectedTab == AppTab.PLAN) Icons.Filled.DateRange else Icons.Outlined.DateRange,
                            contentDescription = "Weekly Plan"
                        )
                    },
                    label = { Text("Weekly Plan") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_weekly_plan")
                )

                // Groceries / Shopping List
                NavigationBarItem(
                    selected = selectedTab == AppTab.SHOPPING,
                    onClick = { viewModel.selectTab(AppTab.SHOPPING) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeGroceriesCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.secondary,
                                        contentColor = MaterialTheme.colorScheme.onSecondary
                                    ) {
                                        Text("$activeGroceriesCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (selectedTab == AppTab.SHOPPING) Icons.Filled.ShoppingBag else Icons.Outlined.ShoppingBag,
                                contentDescription = "Groceries"
                            )
                        }
                    },
                    label = { Text("Groceries") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_groceries")
                )

                // Pantry
                NavigationBarItem(
                    selected = selectedTab == AppTab.PANTRY,
                    onClick = { viewModel.selectTab(AppTab.PANTRY) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (allPantryItems.isNotEmpty()) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ) {
                                        Text("${allPantryItems.size}")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (selectedTab == AppTab.PANTRY) Icons.Filled.Kitchen else Icons.Outlined.Kitchen,
                                contentDescription = "Pantry"
                            )
                        }
                    },
                    label = { Text("Pantry") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_pantry")
                )

                // Meals Catalog & Ready to Cook
                NavigationBarItem(
                    selected = selectedTab == AppTab.MEALS,
                    onClick = { viewModel.selectTab(AppTab.MEALS) },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (readyMealsCount > 0) {
                                    Badge(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    ) {
                                        Text("$readyMealsCount")
                                    }
                                }
                            }
                        ) {
                            Icon(
                                if (selectedTab == AppTab.MEALS) Icons.Filled.RestaurantMenu else Icons.Outlined.RestaurantMenu,
                                contentDescription = "Meals"
                            )
                        }
                    },
                    label = { Text("Meals") },
                    colors = NavigationBarItemDefaults.colors(
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("nav_meals")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                AppTab.PLAN -> WeeklyPlanScreen(
                    weekOffset = weekOffset,
                    weeklyPlanItems = currentWeekPlan,
                    allMeals = allMeals,
                    pantryReadyMeals = pantryReadyMeals,
                    onPreviousWeek = { viewModel.previousWeek() },
                    onNextWeek = { viewModel.nextWeek() },
                    onResetToCurrentWeek = { viewModel.resetToCurrentWeek() },
                    onAddMealToPlan = { mealId, dayOfWeek ->
                        viewModel.addMealToPlan(mealId, dayOfWeek)
                    },
                    onRemovePlanItem = { planItemId ->
                        viewModel.removePlanItem(planItemId)
                    },
                    onMealClick = { readyMeal ->
                        selectedMealForDetail = readyMeal
                    }
                )

                AppTab.SHOPPING -> ShoppingListScreen(
                    shoppingItems = shoppingItems,
                    quickAddItems = quickAddItems,
                    onToggleChecked = { item ->
                        viewModel.toggleShoppingItemChecked(item)
                    },
                    onRemoveItem = { item, addToPantry ->
                        viewModel.removeShoppingItem(item, addToPantry)
                    },
                    onClearChecked = {
                        viewModel.clearCheckedShoppingItems()
                    },
                    onAddCustomItem = { name, quantity, category ->
                        viewModel.addCustomShoppingItem(name, quantity, category)
                    },
                    onQuickReadd = { quickItem ->
                        viewModel.quickReaddItem(quickItem)
                    }
                )

                AppTab.PANTRY -> PantryScreen(
                    pantryItems = filteredPantryItems,
                    searchQuery = pantrySearchQuery,
                    onSearchQueryChange = { viewModel.setPantrySearchQuery(it) },
                    onAddItem = { name, quantity, category ->
                        viewModel.addPantryItem(name, quantity, category)
                    },
                    onRemoveItem = { item ->
                        viewModel.removePantryItem(item)
                    }
                )

                AppTab.MEALS -> MealsScreen(
                    meals = filteredMeals,
                    activeFilter = mealFilter,
                    searchQuery = mealSearchQuery,
                    onFilterChange = { viewModel.setMealFilter(it) },
                    onSearchQueryChange = { viewModel.setMealSearchQuery(it) },
                    onPlanMealForDay = { mealId, dayOfWeek ->
                        viewModel.addMealToPlan(mealId, dayOfWeek)
                    },
                    onSaveMeal = { meal, ingredients ->
                        viewModel.saveMeal(meal, ingredients)
                    },
                    onDeleteMeal = { meal ->
                        viewModel.deleteMeal(meal)
                    }
                )
            }
        }
    }

    // Detail dialog when tapped from Weekly Plan
    selectedMealForDetail?.let { readyMeal ->
        MealDetailDialog(
            pantryReadyMeal = readyMeal,
            onPlanMeal = {
                mealToPlanForDay = readyMeal
                selectedMealForDetail = null
            },
            onEditMeal = {
                selectedMealForDetail = null
                viewModel.selectTab(AppTab.MEALS)
            },
            onDismiss = { selectedMealForDetail = null }
        )
    }

    mealToPlanForDay?.let { readyMeal ->
        PlanMealSelectDayDialog(
            mealName = readyMeal.mealWithIngredients.meal.name,
            onDaySelected = { day ->
                viewModel.addMealToPlan(readyMeal.mealWithIngredients.meal.id, day)
                mealToPlanForDay = null
            },
            onDismiss = { mealToPlanForDay = null }
        )
    }
}
