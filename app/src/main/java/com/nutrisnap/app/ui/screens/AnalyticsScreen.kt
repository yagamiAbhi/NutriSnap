package com.nutrisnap.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrisnap.app.data.models.NUTRIENT_DEFINITIONS
import com.nutrisnap.app.ui.NutritionViewModel

@Composable
fun AnalyticsScreen(viewModel: NutritionViewModel) {
    val analytics by viewModel.analytics.collectAsStateWithLifecycle()
    val specs = NUTRIENT_DEFINITIONS.filter { it.isMacro }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Trends", style = MaterialTheme.typography.headlineMedium)
        Text("Average intake across days with logged foods.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FilterChip(selected = analytics.periodDays == 7, onClick = { viewModel.setAnalyticsPeriod(7) }, label = { Text("7 days") })
            FilterChip(selected = analytics.periodDays == 30, onClick = { viewModel.setAnalyticsPeriod(30) }, label = { Text("30 days") })
        }
        Text("${analytics.daysWithLogs} of ${analytics.periodDays} days logged", style = MaterialTheme.typography.titleMedium)
        if (analytics.daysWithLogs == 0) {
            Text("Log meals to see your nutrition trends.", modifier = Modifier.padding(top = 12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            specs.forEach { spec ->
                val average = analytics.averages[spec.key]
                SurfaceSection {
                    Text(spec.label, style = MaterialTheme.typography.titleMedium)
                    Text(
                        average?.knownTotal?.let { "${formatAmount(it)} ${spec.unit} per logged day" } ?: "— ${spec.unit}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (average != null && average.knownFoodCount < average.foodCount) {
                        Text("Known on ${average.knownFoodCount} of ${average.foodCount} logged days", color = MaterialTheme.colorScheme.tertiary)
                    }
                }
            }
        }
    }
}
