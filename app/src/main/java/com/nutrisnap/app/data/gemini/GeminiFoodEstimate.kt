package com.nutrisnap.app.data.gemini

import com.nutrisnap.app.data.models.NutritionEntity
import com.nutrisnap.app.data.models.FoodSource

data class GeminiFoodEstimate(
    val name: String,
    val quantity: Double,
    val unit: String,
    val estimatedWeightGrams: Double?,
    val nutrition: NutritionEntity,
    val source: FoodSource = FoodSource.GEMINI
)

data class GeminiMealEstimate(
    val mealDescription: String,
    val foods: List<GeminiFoodEstimate>
)
