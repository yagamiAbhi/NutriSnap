package com.nutrisnap.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrisnap.app.ui.NutritionViewModel
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HistoryScreen(viewModel: NutritionViewModel) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val historyDays by viewModel.historyDays.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("History", style = MaterialTheme.typography.headlineMedium)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            IconButton(onClick = { viewModel.showDate(dashboard.date.minusDays(1)) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Previous day")
            }
            Button(onClick = { viewModel.showDate(LocalDate.now()) }) { Text("Today") }
            IconButton(onClick = { viewModel.showDate(dashboard.date.plusDays(1)) }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Next day")
            }
        }
        Text(dashboard.date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d")), style = MaterialTheme.typography.titleLarge)
        val calories = dashboard.totals["caloriesKcal"]
        Text(
            calories?.knownTotal?.let { "${formatAmount(it)} kcal known intake" } ?: "No logged nutrition for this date",
            style = MaterialTheme.typography.bodyLarge
        )
        val protein = dashboard.totals["proteinG"]?.knownTotal
        Text(
            "${dashboard.foodCount} foods${protein?.let { " · ${formatAmount(it)} g protein" }.orEmpty()}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (dashboard.foodCount == 0) {
            Text("No foods logged on this date.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        dashboard.meals.forEach { meal ->
            if (meal.foods.isNotEmpty()) {
                SurfaceSection {
                    Text(meal.title, style = MaterialTheme.typography.titleMedium)
                    meal.foods.forEach { food ->
                        Text("${food.name} · ${formatAmount(food.quantity)} ${food.unit}")
                        Text(food.nutrition.caloriesKcal?.let { "${formatAmount(it)} kcal" } ?: "Calories unknown", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        SectionTitle("Recent logged days", "Last 30 days")
        if (historyDays.isEmpty()) {
            Text("Your logged days will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            historyDays.forEach { day ->
                val selected = day.date == dashboard.date
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.showDate(day.date) },
                    colors = androidx.compose.material3.CardDefaults.cardColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
                    )
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                if (day.date == LocalDate.now()) "Today" else day.date.format(DateTimeFormatter.ofPattern("EEE, MMM d")),
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                "${day.foodCount} ${if (day.foodCount == 1) "food" else "foods"}" +
                                    (day.proteinG?.let { " · ${formatAmount(it)} g protein" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(day.caloriesKcal?.let { "${formatAmount(it)} kcal" } ?: "— kcal", style = MaterialTheme.typography.titleSmall)
                    }
                }
            }
        }
    }
}
