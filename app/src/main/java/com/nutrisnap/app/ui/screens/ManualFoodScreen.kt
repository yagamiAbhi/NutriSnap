package com.nutrisnap.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nutrisnap.app.data.models.MealType
import com.nutrisnap.app.data.models.NUTRIENT_DEFINITIONS
import com.nutrisnap.app.data.models.NutritionEntity
import com.nutrisnap.app.data.models.withValue
import com.nutrisnap.app.ui.NutritionViewModel

@Composable
fun ManualFoodScreen(viewModel: NutritionViewModel, onSaved: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("serving") }
    var weight by remember { mutableStateOf("") }
    var mealType by remember { mutableStateOf(MealType.BREAKFAST) }
    var saveAsTemplate by remember { mutableStateOf(false) }
    var expandedNutrients by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    val nutrientInputs = remember { mutableStateMapOf<String, String>() }

    Column(
        modifier = Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Enter food", style = MaterialTheme.typography.headlineMedium)
        Text("Only fill in values you know. Leave unknown nutrients blank.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        OutlinedTextField(name, { name = it; error = null }, label = { Text("Food name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(unit, { unit = it }, label = { Text("Unit") }, modifier = Modifier.weight(1f), singleLine = true)
        }
        OutlinedTextField(weight, { weight = it }, label = { Text("Weight (g), optional") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))

        Text("Meal", style = MaterialTheme.typography.titleMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK).forEach { type ->
                FilterChip(selected = mealType == type, onClick = { mealType = type }, label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) })
            }
        }

        Text("Nutrition (optional)", style = MaterialTheme.typography.titleMedium)
        NUTRIENT_DEFINITIONS.filter { it.isMacro }.forEach { spec ->
            OutlinedTextField(
                value = nutrientInputs[spec.key].orEmpty(),
                onValueChange = { nutrientInputs[spec.key] = it },
                label = { Text("${spec.label} (${spec.unit})") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
        }
        androidx.compose.material3.TextButton(onClick = { expandedNutrients = !expandedNutrients }) {
            Text(if (expandedNutrients) "Hide micronutrients" else "Add micronutrients")
        }
        if (expandedNutrients) {
            NUTRIENT_DEFINITIONS.filter { !it.isMacro }.forEach { spec ->
                OutlinedTextField(
                    value = nutrientInputs[spec.key].orEmpty(),
                    onValueChange = { nutrientInputs[spec.key] = it },
                    label = { Text("${spec.label} (${spec.unit})") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Checkbox(checked = saveAsTemplate, onCheckedChange = { saveAsTemplate = it })
            Text("Save as a Saved Food")
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                val parsedQuantity = quantity.toDoubleOrNull()
                val parsedWeight = weight.takeIf { it.isNotBlank() }?.toDoubleOrNull()
                when {
                    name.isBlank() -> error = "Enter a food name."
                    parsedQuantity == null || parsedQuantity <= 0.0 -> error = "Quantity must be greater than zero."
                    unit.isBlank() -> error = "Enter a unit, such as g, cup, or piece."
                    weight.isNotBlank() && (parsedWeight == null || parsedWeight <= 0.0) -> error = "Weight must be greater than zero."
                    nutrientInputs.values.any { it.isNotBlank() && (it.toDoubleOrNull() == null || it.toDouble() < 0.0) } -> error = "Nutrition values must be zero or greater."
                    else -> {
                        var nutrition = NutritionEntity(foodEntryId = 0)
                        nutrientInputs.forEach { (key, value) ->
                            if (value.isNotBlank()) nutrition = nutrition.withValue(key, value.toDouble())
                        }
                        saving = true
                        viewModel.saveFood(name, parsedQuantity, unit, parsedWeight, mealType, nutrition, saveAsTemplate) { result ->
                            saving = false
                            result.onSuccess { onSaved() }.onFailure { error = "Could not save food: ${it.message ?: "database error"}" }
                        }
                    }
                }
            },
            enabled = !saving,
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (saving) "Saving…" else "Save food") }
        Spacer(Modifier.height(12.dp))
    }
}
