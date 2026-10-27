package com.nutrisnap.app.data.models

data class FoodLogDraft(
    val name: String,
    val quantity: Double,
    val unit: String,
    val weightGrams: Double?,
    val nutrition: NutritionEntity,
    val source: FoodSource = FoodSource.MANUAL
)
