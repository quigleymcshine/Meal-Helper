package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.Meal
import com.example.data.model.MealIngredient
import com.example.data.model.PantryReadyMeal
import com.example.ui.components.AddMealDialog
import com.example.ui.components.MealDetailDialog
import com.example.ui.components.PlanMealSelectDayDialog
import com.example.ui.viewmodel.MealFilter

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MealsScreen(
    meals: List<PantryReadyMeal>,
    activeFilter: MealFilter,
    searchQuery: String,
    onFilterChange: (MealFilter) -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onPlanMealForDay: (mealId: Long, dayOfWeek: String) -> Unit,
    onSaveMeal: (Meal, List<MealIngredient>) -> Unit,
    onDeleteMeal: (Meal) -> Unit
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var mealToEdit by remember { mutableStateOf<PantryReadyMeal?>(null) }
    var mealToPlan by remember { mutableStateOf<PantryReadyMeal?>(null) }
    var selectedMealForDetail by remember { mutableStateOf<PantryReadyMeal?>(null) }

    val readyCount = remember(meals) { meals.count { it.isFullyReady } }
    val previouslyUsedReadyCount = remember(meals) {
        meals.count { it.isFullyReady && it.isPreviouslyUsed }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Meals & Recipes",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Track dinner recipes & see pantry-ready meals",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            FilledTonalButton(
                                onClick = { showCreateDialog = true },
                                modifier = Modifier.testTag("add_new_meal_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("New Meal")
                            }
                        }

                        // Pantry Ready Banner
                        if (readyCount > 0) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onFilterChange(MealFilter.PANTRY_READY) }
                                    .testTag("pantry_ready_alert_banner"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "$readyCount meals ready to cook right now!",
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            text = if (previouslyUsedReadyCount > 0)
                                                "$previouslyUsedReadyCount previously cooked favorites have 100% ingredients in pantry"
                                            else
                                                "All ingredients are currently available in your pantry",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Search box
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onSearchQueryChange,
                            label = { Text("Search meals or category") },
                            leadingIcon = {
                                Icon(Icons.Default.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSearchQueryChange("") }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("meals_search_input")
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Filter chips
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            contentPadding = PaddingValues(vertical = 2.dp)
                        ) {
                            item {
                                FilterChip(
                                    selected = activeFilter == MealFilter.ALL,
                                    onClick = { onFilterChange(MealFilter.ALL) },
                                    label = { Text("All Meals (${meals.size})") },
                                    modifier = Modifier.testTag("filter_all_meals")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = activeFilter == MealFilter.PANTRY_READY,
                                    onClick = { onFilterChange(MealFilter.PANTRY_READY) },
                                    leadingIcon = {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text("Pantry Ready ($readyCount)") },
                                    modifier = Modifier.testTag("filter_pantry_ready")
                                )
                            }
                            item {
                                FilterChip(
                                    selected = activeFilter == MealFilter.PREVIOUSLY_COOKED,
                                    onClick = { onFilterChange(MealFilter.PREVIOUSLY_COOKED) },
                                    leadingIcon = {
                                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, modifier = Modifier.size(16.dp))
                                    },
                                    label = { Text("Previously Cooked") },
                                    modifier = Modifier.testTag("filter_previously_cooked")
                                )
                            }
                        }
                    }
                }
            }

            // Meals list
            if (meals.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.RestaurantMenu,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No meals found",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (activeFilter == MealFilter.PANTRY_READY)
                                    "No meals currently have 100% of ingredients in your pantry. Check the Pantry tab or All Meals!"
                                else
                                    "Tap '+ New Meal' to add your favorite dinner recipes and ingredients.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            )
                        }
                    }
                }
            } else {
                items(meals, key = { it.mealWithIngredients.meal.id }) { item ->
                    val meal = item.mealWithIngredients.meal
                    val isPantryReady = item.isFullyReady

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedMealForDetail = item }
                            .testTag("meal_card_${meal.id}"),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isPantryReady)
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                            else
                                MaterialTheme.colorScheme.surface
                        ),
                        border = if (isPantryReady)
                            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.6f))
                        else null
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = meal.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        SuggestionChip(
                                            onClick = {},
                                            label = { Text(meal.category) },
                                            modifier = Modifier.height(24.dp)
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text("${meal.prepTimeMinutes}m", style = MaterialTheme.typography.bodySmall)
                                        }
                                        if (meal.timesPlanned > 0) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    Icons.Default.LocalFireDepartment,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.secondary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(2.dp))
                                                Text(
                                                    "${meal.timesPlanned}x planned",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.secondary,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }

                                Row {
                                    IconButton(
                                        onClick = { mealToEdit = item },
                                        modifier = Modifier.testTag("edit_meal_${meal.id}")
                                    ) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Meal", modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(
                                        onClick = { onDeleteMeal(meal) },
                                        modifier = Modifier.testTag("delete_meal_${meal.id}")
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete Meal",
                                            tint = MaterialTheme.colorScheme.error,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Pantry Status Badge
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isPantryReady)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.surfaceVariant
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (isPantryReady) Icons.Default.CheckCircle else Icons.Default.RestaurantMenu,
                                        contentDescription = null,
                                        tint = if (isPantryReady) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPantryReady)
                                            "100% IN PANTRY • Ready to Cook!"
                                        else
                                            "${item.inPantryCount} of ${item.totalCount} in pantry (${item.missingIngredients.size} to buy)",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isPantryReady) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Ingredients chips preview
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                item.mealWithIngredients.ingredients.take(6).forEach { ing ->
                                    val inPantry = item.inPantryIngredients.contains(ing.name.trim())
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = if (inPantry)
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = if (inPantry) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                                                contentDescription = null,
                                                modifier = Modifier.size(10.dp),
                                                tint = if (inPantry) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = ing.name,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = if (inPantry) FontWeight.SemiBold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                                if (item.mealWithIngredients.ingredients.size > 6) {
                                    Text(
                                        text = "+${item.mealWithIngredients.ingredients.size - 6} more",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Action: Plan for this week
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End
                            ) {
                                Button(
                                    onClick = { mealToPlan = item },
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("plan_meal_button_${meal.id}"),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Plan for Dinner", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(84.dp))
            }
        }

        // Floating Action Button for New Meal
        FloatingActionButton(
            onClick = { showCreateDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 76.dp, end = 20.dp)
                .testTag("new_meal_fab"),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ) {
            Icon(Icons.Default.Add, contentDescription = "Create Meal")
        }
    }

    if (showCreateDialog) {
        AddMealDialog(
            onSaveMeal = { meal, ingredients ->
                onSaveMeal(meal, ingredients)
                showCreateDialog = false
            },
            onDismiss = { showCreateDialog = false }
        )
    }

    mealToEdit?.let { readyMeal ->
        AddMealDialog(
            initialMeal = readyMeal.mealWithIngredients.meal,
            initialIngredients = readyMeal.mealWithIngredients.ingredients,
            onSaveMeal = { meal, ingredients ->
                onSaveMeal(meal, ingredients)
                mealToEdit = null
            },
            onDismiss = { mealToEdit = null }
        )
    }

    mealToPlan?.let { readyMeal ->
        PlanMealSelectDayDialog(
            mealName = readyMeal.mealWithIngredients.meal.name,
            onDaySelected = { day ->
                onPlanMealForDay(readyMeal.mealWithIngredients.meal.id, day)
                mealToPlan = null
            },
            onDismiss = { mealToPlan = null }
        )
    }

    selectedMealForDetail?.let { readyMeal ->
        MealDetailDialog(
            pantryReadyMeal = readyMeal,
            onPlanMeal = {
                mealToPlan = readyMeal
                selectedMealForDetail = null
            },
            onEditMeal = {
                mealToEdit = readyMeal
                selectedMealForDetail = null
            },
            onDismiss = { selectedMealForDetail = null }
        )
    }
}
