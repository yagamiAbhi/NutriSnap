package com.nutrisnap.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nutrisnap.app.data.models.NUTRIENT_DEFINITIONS
import com.nutrisnap.app.ui.NutritionViewModel

@Composable
fun SettingsScreen(viewModel: NutritionViewModel) {
    val dashboard by viewModel.dashboard.collectAsStateWithLifecycle()
    val goals = NUTRIENT_DEFINITIONS.filter { it.isMacro }
    val values = remember { mutableStateMapOf<String, String>() }
    var message by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Settings", style = MaterialTheme.typography.headlineMedium)
        Text("Daily nutrition goals", style = MaterialTheme.typography.titleLarge)
        Text("Personalize your targets. These are guidance values, not medical advice.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        goals.forEach { spec ->
            val current = dashboard.targets[spec.key]?.targetValue?.let(::formatAmount)
                ?: spec.defaultTarget?.let(::formatAmount).orEmpty()
            OutlinedTextField(
                value = values[spec.key] ?: current,
                onValueChange = { values[spec.key] = it; message = null },
                label = { Text("${spec.label} (${spec.unit})") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
        message?.let { Text(it, color = if (it.startsWith("Saved")) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error) }
        Button(onClick = {
            val parsed = goals.mapNotNull { spec ->
                val value = (values[spec.key] ?: dashboard.targets[spec.key]?.targetValue?.toString() ?: spec.defaultTarget?.toString()).orEmpty().toDoubleOrNull()
                if (value == null || value <= 0.0) null else Triple(spec, value, values[spec.key])
            }
            if (parsed.size != goals.size) {
                message = "Enter a target greater than zero for each nutrient."
            } else {
                var remaining = parsed.size
                parsed.forEach { (spec, value, _) ->
                    viewModel.saveTarget(spec.key, value, spec.unit) { result ->
                        result.onFailure { message = "Could not save targets: ${it.message ?: "database error"}" }
                        remaining -= 1
                        if (remaining == 0 && message == null) message = "Targets saved."
                    }
                }
            }
        }, modifier = Modifier.fillMaxWidth()) { Text("Save targets") }
    }
}
