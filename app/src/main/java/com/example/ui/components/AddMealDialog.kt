package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.Meal
import com.example.data.model.MealIngredient

data class EditableIngredient(
    var name: String = "",
    var quantity: String = ""
)

@Composable
fun AddMealDialog(
    initialMeal: Meal? = null,
    initialIngredients: List<MealIngredient> = emptyList(),
    onSaveMeal: (Meal, List<MealIngredient>) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(initialMeal?.name ?: "") }
    var category by remember { mutableStateOf(initialMeal?.category ?: "Dinner") }
    var prepTime by remember { mutableStateOf(initialMeal?.prepTimeMinutes?.toString() ?: "30") }
    var servings by remember { mutableStateOf(initialMeal?.servings?.toString() ?: "4") }
    var instructions by remember { mutableStateOf(initialMeal?.instructions ?: "") }

    val ingredientsList = remember {
        mutableStateListOf<EditableIngredient>().apply {
            if (initialIngredients.isNotEmpty()) {
                addAll(initialIngredients.map { EditableIngredient(it.name, it.quantity) })
            } else {
                add(EditableIngredient())
                add(EditableIngredient())
            }
        }
    }

    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (initialMeal == null) "Create New Dinner" else "Edit Meal",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            errorMessage = null
                        },
                        label = { Text("Meal Name *") },
                        placeholder = { Text("e.g. Homemade Margherita Pizza") },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("meal_name_input")
                    )
                }

                item {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = category,
                            onValueChange = { category = it },
                            label = { Text("Category") },
                            placeholder = { Text("Italian, Quick...") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("meal_category_input")
                        )
                        OutlinedTextField(
                            value = prepTime,
                            onValueChange = { prepTime = it },
                            label = { Text("Prep (mins)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .width(110.dp)
                                .testTag("meal_preptime_input")
                        )
                    }
                }

                item {
                    OutlinedTextField(
                        value = instructions,
                        onValueChange = { instructions = it },
                        label = { Text("Cooking Notes / Recipe") },
                        placeholder = { Text("Optional steps or recipe notes...") },
                        maxLines = 3,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("meal_instructions_input")
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ingredients",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(
                            onClick = { ingredientsList.add(EditableIngredient()) },
                            modifier = Modifier.testTag("add_ingredient_row_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Item")
                        }
                    }
                }

                itemsIndexed(ingredientsList) { index, itemState ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        OutlinedTextField(
                            value = itemState.name,
                            onValueChange = {
                                ingredientsList[index] = itemState.copy(name = it)
                            },
                            label = { Text("Ingredient") },
                            placeholder = { Text("e.g. Olive Oil") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(1.4f)
                                .testTag("ingredient_name_$index")
                        )

                        OutlinedTextField(
                            value = itemState.quantity,
                            onValueChange = {
                                ingredientsList[index] = itemState.copy(quantity = it)
                            },
                            label = { Text("Qty") },
                            placeholder = { Text("2 tbsp") },
                            singleLine = true,
                            modifier = Modifier
                                .weight(0.9f)
                                .testTag("ingredient_qty_$index")
                        )

                        IconButton(
                            onClick = {
                                if (ingredientsList.size > 1) {
                                    ingredientsList.removeAt(index)
                                }
                            },
                            enabled = ingredientsList.size > 1
                        ) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Remove Ingredient",
                                tint = if (ingredientsList.size > 1) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    item {
                        Text(
                            text = errorMessage ?: "",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter a meal name"
                        return@Button
                    }
                    val validIngredients = ingredientsList
                        .filter { it.name.isNotBlank() }
                        .map {
                            MealIngredient(
                                mealId = initialMeal?.id ?: 0L,
                                name = it.name.trim(),
                                quantity = it.quantity.trim()
                            )
                        }

                    if (validIngredients.isEmpty()) {
                        errorMessage = "Please add at least one ingredient"
                        return@Button
                    }

                    val meal = Meal(
                        id = initialMeal?.id ?: 0L,
                        name = name.trim(),
                        category = category.trim().ifBlank { "Dinner" },
                        prepTimeMinutes = prepTime.toIntOrNull() ?: 30,
                        servings = servings.toIntOrNull() ?: 4,
                        instructions = instructions.trim(),
                        timesPlanned = initialMeal?.timesPlanned ?: 0
                    )
                    onSaveMeal(meal, validIngredients)
                },
                modifier = Modifier.testTag("save_meal_button")
            ) {
                Text("Save Meal")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
