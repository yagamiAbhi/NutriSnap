package com.nutrisnap.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrisnap.app.data.models.NUTRIENT_DEFINITIONS
import com.nutrisnap.app.ui.NutritionViewModel
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(viewModel: NutritionViewModel, onAddFood: () -> Unit) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val macroSpecs = NUTRIENT_DEFINITIONS.filter { it.isMacro }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Text("NutriSnap", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(
            if (dashboard.date == java.time.LocalDate.now()) "Today" else "Daily log",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            dashboard.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(Modifier.padding(18.dp)) {
                Text("Calories", style = MaterialTheme.typography.titleMedium)
                val calorieSpec = macroSpecs.first { it.key == "caloriesKcal" }
                NutritionProgressRow(
                    spec = calorieSpec,
                    coverage = dashboard.totals[calorieSpec.key],
                    target = dashboard.targets[calorieSpec.key]?.targetValue ?: calorieSpec.defaultTarget
                )
            }
        }

        SectionTitle("Nutrients", "${dashboard.foodCount} foods logged")
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                macroSpecs.filterNot { it.key == "caloriesKcal" }.forEachIndexed { index, spec ->
                    NutritionProgressRow(
                        spec = spec,
                        coverage = dashboard.totals[spec.key],
                        target = dashboard.targets[spec.key]?.targetValue ?: spec.defaultTarget
                    )
                    if (index != macroSpecs.size - 2) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        SectionTitle("Meals")
        dashboard.meals.forEach { meal ->
            var expanded by remember(meal.mealType, meal.snackIndex, meal.foods.size) { mutableStateOf(meal.foods.isNotEmpty()) }
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { expanded = !expanded },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(meal.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text(
                                if (meal.foods.isEmpty()) "Nothing logged yet" else "${meal.foods.size} ${if (meal.foods.size == 1) "food" else "foods"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        val knownCalories = meal.foods.mapNotNull { it.nutrition.caloriesKcal }
                        Text(
                            if (knownCalories.isEmpty()) "— kcal" else "${formatAmount(knownCalories.sum())} kcal",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (expanded && meal.foods.isNotEmpty()) {
                        HorizontalDivider(Modifier.padding(vertical = 10.dp), color = MaterialTheme.colorScheme.outlineVariant)
                        meal.foods.forEach { food ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.SpaceBetween) {
                                Text("${food.name} · ${formatAmount(food.quantity)} ${food.unit}", modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                                Text(food.nutrition.caloriesKcal?.let { "${formatAmount(it)} kcal" } ?: "— kcal", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    }
                }
            }
        }
        OutlinedButton(onClick = onAddFood, modifier = Modifier.fillMaxWidth().height(52.dp)) {
            Icon(Icons.Filled.Add, contentDescription = null)
            Text("  Add food")
        }
        Spacer(Modifier.height(16.dp))
    }
}
