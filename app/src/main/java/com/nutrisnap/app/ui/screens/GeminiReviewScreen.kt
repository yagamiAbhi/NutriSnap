package com.nutrisnap.app.ui.screens

import android.net.Uri
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.nutrisnap.app.data.gemini.GeminiFoodEstimate
import com.nutrisnap.app.data.gemini.TemporaryMealPhoto
import com.nutrisnap.app.data.models.MealType
import com.nutrisnap.app.data.models.NUTRIENT_DEFINITIONS
import com.nutrisnap.app.data.models.NutrientCategory
import com.nutrisnap.app.data.models.NutritionEntity
import com.nutrisnap.app.data.models.FoodSource
import com.nutrisnap.app.data.models.withValue
import com.nutrisnap.app.ui.NutritionViewModel

private data class FoodReviewDraft(
    val name: String,
    val quantity: String,
    val unit: String,
    val weight: String,
    val nutrition: NutritionEntity,
    val source: FoodSource,
    val expandedMicros: Boolean = false
)

@Composable
fun GeminiReviewScreen(
    photoUri: String,
    viewModel: NutritionViewModel,
    onManualEntry: () -> Unit,
    onBack: () -> Unit,
    onSaved: () -> Unit
) {
    val context = LocalContext.current
    val analysis by viewModel.photoAnalysis.collectAsStateWithLifecycle()
    val estimate = analysis.estimate
    val foods = remember(photoUri) { mutableStateListOf<FoodReviewDraft>() }
    var mealType by remember { mutableStateOf(MealType.LUNCH) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    LaunchedEffect(photoUri) { viewModel.analyzePhoto(Uri.parse(photoUri)) }
    LaunchedEffect(analysis.estimate) {
        analysis.estimate?.let { estimate ->
            foods.clear()
            foods.addAll(estimate.foods.map { food ->
                FoodReviewDraft(food.name, food.quantity.toString(), food.unit, food.estimatedWeightGrams?.toString().orEmpty(), food.nutrition, food.source)
            })
        }
    }
    DisposableEffect(photoUri) {
        onDispose { TemporaryMealPhoto.deleteIfTemporary(context, Uri.parse(photoUri)) }
    }

    if (analysis.isLoading) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(Modifier.height(18.dp))
            Text("Analyzing meal photo…", style = MaterialTheme.typography.titleMedium)
            Text("Estimates are shown for review before anything is saved.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    if (analysis.estimate == null) {
        Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Couldn’t analyze this photo", style = MaterialTheme.typography.headlineSmall)
            Text(analysis.error ?: "Gemini analysis failed. Retry or enter foods manually.", color = MaterialTheme.colorScheme.error)
            AsyncImage(model = Uri.parse(photoUri), contentDescription = "Selected meal photo", modifier = Modifier.fillMaxWidth().weight(1f))
            Button(onClick = { viewModel.analyzePhoto(Uri.parse(photoUri)) }, modifier = Modifier.fillMaxWidth()) { Text("Retry analysis") }
            OutlinedButton(onClick = onManualEntry, modifier = Modifier.fillMaxWidth()) { Text("Enter foods manually") }
            OutlinedButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Choose another photo") }
        }
        return
    }

    Column(Modifier.fillMaxSize().imePadding().padding(horizontal = 18.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Review meal", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text(estimate?.mealDescription.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("AI estimates can be uncertain. Correct each item before saving.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
            listOf(MealType.BREAKFAST, MealType.LUNCH, MealType.DINNER, MealType.SNACK).forEach { type ->
                FilterChip(selected = mealType == type, onClick = { mealType = type }, label = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) })
            }
        }
        LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            itemsIndexed(foods) { index, food ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Food ${index + 1} · ${food.source.name.replace('_', ' ')}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        OutlinedTextField(food.name, { updateFood(foods, index) { copy(name = it) }; error = null }, label = { Text("Food name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(food.quantity, { updateFood(foods, index) { copy(quantity = it) }; error = null }, label = { Text("Quantity") }, modifier = Modifier.weight(1f), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                            OutlinedTextField(food.unit, { updateFood(foods, index) { copy(unit = it) }; error = null }, label = { Text("Unit") }, modifier = Modifier.weight(1f), singleLine = true)
                        }
                        OutlinedTextField(food.weight, { updateFood(foods, index) { copy(weight = it) }; error = null }, label = { Text("Estimated weight (g)") }, supportingText = { Text("Leave blank when uncertain") }, modifier = Modifier.fillMaxWidth(), singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
                        Text("Nutrition", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        NUTRIENT_DEFINITIONS.filter { it.category == NutrientCategory.MACRO }.forEach { nutrient ->
                            OutlinedTextField(
                                value = food.nutrition.valueForKey(nutrient.key)?.let(::formatAmount).orEmpty(),
                                onValueChange = { text -> updateFood(foods, index) { copy(nutrition = nutrition.withValue(nutrient.key, text.toDoubleOrNull())) }; error = null },
                                label = { Text("${nutrient.label} (${nutrient.unit})") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                            )
                        }
                        androidx.compose.material3.TextButton(onClick = { updateFood(foods, index) { copy(expandedMicros = !expandedMicros) } }) {
                            Text(if (food.expandedMicros) "Hide micronutrients" else "Edit micronutrients")
                        }
                        if (food.expandedMicros) {
                            NUTRIENT_DEFINITIONS.filter { it.category != NutrientCategory.MACRO }.forEach { nutrient ->
                                OutlinedTextField(
                                    value = food.nutrition.valueForKey(nutrient.key)?.let(::formatAmount).orEmpty(),
                                    onValueChange = { text -> updateFood(foods, index) { copy(nutrition = nutrition.withValue(nutrient.key, text.toDoubleOrNull())) }; error = null },
                                    label = { Text("${nutrient.label} (${nutrient.unit})") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                                )
                            }
                        }
                            OutlinedButton(onClick = { foods.removeAt(index) }, modifier = Modifier.fillMaxWidth()) { Text("Remove food") }
                    }
                }
            }
        }
            OutlinedButton(
                onClick = {
                    foods.add(
                        FoodReviewDraft(
                            name = "",
                            quantity = "1",
                            unit = "serving",
                            weight = "",
                            nutrition = NutritionEntity(foodEntryId = 0),
                            source = FoodSource.MANUAL
                        )
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("+ Add another food") }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        Button(
            onClick = {
                val validationError = validateFoods(foods)
                if (validationError != null) {
                    error = validationError
                } else {
                    saving = true
                    val reviewed = foods.map { draft ->
                        GeminiFoodEstimate(
                            name = draft.name.trim(),
                            quantity = draft.quantity.toDouble(),
                            unit = draft.unit.trim(),
                            estimatedWeightGrams = draft.weight.takeIf { it.isNotBlank() }?.toDouble(),
                            nutrition = draft.nutrition,
                            source = draft.source
                        )
                    }
                    viewModel.saveGeminiFoods(reviewed, mealType) { result ->
                        saving = false
                        result.onSuccess { onSaved() }.onFailure { error = "Could not save meal: ${it.message ?: "database error"}" }
                    }
                }
            },
            enabled = !saving && foods.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(52.dp)
        ) { Text(if (saving) "Saving…" else "Save reviewed meal") }
    }
}

private fun updateFood(foods: MutableList<FoodReviewDraft>, index: Int, transform: FoodReviewDraft.() -> FoodReviewDraft) {
    foods[index] = foods[index].transform()
}

private fun NutritionEntity.valueForKey(key: String): Double? = when (key) {
    "caloriesKcal" -> caloriesKcal
    "proteinG" -> proteinG
    "carbohydratesG" -> carbohydratesG
    "fatG" -> fatG
    "fiberG" -> fiberG
    "sugarG" -> sugarG
    "saturatedFatG" -> saturatedFatG
    "calciumMg" -> calciumMg
    "ironMg" -> ironMg
    "magnesiumMg" -> magnesiumMg
    "phosphorusMg" -> phosphorusMg
    "potassiumMg" -> potassiumMg
    "sodiumMg" -> sodiumMg
    "zincMg" -> zincMg
    "copperMg" -> copperMg
    "manganeseMg" -> manganeseMg
    "seleniumUg" -> seleniumUg
    "vitaminAUg" -> vitaminAUg
    "vitaminCMg" -> vitaminCMg
    "vitaminDUg" -> vitaminDUg
    "vitaminEMg" -> vitaminEMg
    "vitaminKUg" -> vitaminKUg
    "vitaminB1Mg" -> vitaminB1Mg
    "vitaminB2Mg" -> vitaminB2Mg
    "vitaminB3Mg" -> vitaminB3Mg
    "vitaminB5Mg" -> vitaminB5Mg
    "vitaminB6Mg" -> vitaminB6Mg
    "vitaminB7Ug" -> vitaminB7Ug
    "vitaminB9Ug" -> vitaminB9Ug
    "vitaminB12Ug" -> vitaminB12Ug
    else -> null
}

private fun validateFoods(foods: List<FoodReviewDraft>): String? {
    if (foods.isEmpty()) return "Add at least one food before saving."
    foods.forEachIndexed { index, food ->
        val quantity = food.quantity.toDoubleOrNull()
        if (food.name.isBlank()) return "Food ${index + 1}: enter a name."
        if (quantity == null || !quantity.isFinite() || quantity <= 0.0) return "Food ${index + 1}: quantity must be greater than zero."
        if (food.unit.isBlank()) return "Food ${index + 1}: enter a unit."
        if (food.weight.isNotBlank()) {
            val weight = food.weight.toDoubleOrNull()
            if (weight == null || !weight.isFinite() || weight <= 0.0) return "Food ${index + 1}: weight must be greater than zero, or leave it blank."
        }
        if (NUTRIENT_DEFINITIONS.any { spec ->
                val value = food.nutrition.valueForKey(spec.key)
                value != null && (!value.isFinite() || value < 0.0)
            }) return "Food ${index + 1}: nutrition values cannot be negative."
    }
    return null
}
