package com.nutrisnap.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrisnap.app.data.models.MealType
import com.nutrisnap.app.ui.NutritionViewModel

@Composable
fun SavedFoodsScreen(viewModel: NutritionViewModel, onAdded: () -> Unit) {
    val foods by viewModel.savedFoods.collectAsStateWithLifecycle()
    var mealType by remember { mutableStateOf(MealType.BREAKFAST) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 18.dp)) {
        Text("Saved foods", style = MaterialTheme.typography.headlineMedium)
        Text("Choose a food to add a copy to today’s log.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 12.dp)) {
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK).forEach { type ->
                FilterChip(selected = mealType == type, onClick = { mealType = type }, label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) })
            }
        }
        message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (foods.isEmpty()) {
            Text("No saved foods yet. Add one from Enter manually.", modifier = Modifier.padding(top = 24.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(foods, key = { it.id }) { food ->
                    Card(Modifier.fillMaxWidth()) {
                        Row(Modifier.fillMaxWidth().padding(14.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(Modifier.weight(1f)) {
                                Text(food.name, style = MaterialTheme.typography.titleMedium)
                                Text("${food.defaultQuantity} ${food.defaultUnit}${food.defaultWeightGrams?.let { " · ${it} g" }.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Button(onClick = {
                                viewModel.addSavedFood(food.id, mealType) { result ->
                                    result.onSuccess { onAdded() }.onFailure { message = "Could not add food: ${it.message ?: "database error"}" }
                                }
                            }) { Text("Add") }
                        }
                    }
                }
            }
        }
    }
}
