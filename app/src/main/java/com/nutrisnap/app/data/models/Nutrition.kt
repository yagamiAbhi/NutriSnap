package com.nutrisnap.app.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class MealType {
    BREAKFAST,
    LUNCH,
    DINNER,
    SNACK
}

enum class FoodSource {
    GEMINI,
    MANUAL,
    SAVED_FOOD
}

@Entity(
    tableName = "meals",
    indices = [Index(value = ["date", "mealType", "snackIndex"])]
)
data class MealEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val mealType: MealType,
    val snackIndex: Int? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis
)

@Entity(
    tableName = "food_entries",
    foreignKeys = [
        ForeignKey(
            entity = MealEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["mealId"])]
)
data class FoodEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mealId: Long,
    val name: String,
    val quantity: Double,
    val unit: String,
    val weightGrams: Double? = null,
    val source: FoodSource,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis
)

@Entity(
    tableName = "nutrition",
    foreignKeys = [
        ForeignKey(
            entity = FoodEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodEntryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["foodEntryId"], unique = true)]
)
data class NutritionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val foodEntryId: Long,
    val caloriesKcal: Double? = null,
    val proteinG: Double? = null,
    val carbohydratesG: Double? = null,
    val fatG: Double? = null,
    val fiberG: Double? = null,
    val sugarG: Double? = null,
    val saturatedFatG: Double? = null,
    val calciumMg: Double? = null,
    val ironMg: Double? = null,
    val magnesiumMg: Double? = null,
    val phosphorusMg: Double? = null,
    val potassiumMg: Double? = null,
    val sodiumMg: Double? = null,
    val zincMg: Double? = null,
    val copperMg: Double? = null,
    val manganeseMg: Double? = null,
    val seleniumUg: Double? = null,
    val vitaminAUg: Double? = null,
    val vitaminCMg: Double? = null,
    val vitaminDUg: Double? = null,
    val vitaminEMg: Double? = null,
    val vitaminKUg: Double? = null,
    val vitaminB1Mg: Double? = null,
    val vitaminB2Mg: Double? = null,
    val vitaminB3Mg: Double? = null,
    val vitaminB5Mg: Double? = null,
    val vitaminB6Mg: Double? = null,
    val vitaminB7Ug: Double? = null,
    val vitaminB9Ug: Double? = null,
    val vitaminB12Ug: Double? = null
)

@Entity(
    tableName = "saved_foods",
    indices = [Index(value = ["name"], unique = true)]
)
data class SavedFoodEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val defaultQuantity: Double,
    val defaultUnit: String,
    val defaultWeightGrams: Double? = null,
    val createdAtEpochMillis: Long = System.currentTimeMillis(),
    val updatedAtEpochMillis: Long = createdAtEpochMillis
)

@Entity(
    tableName = "saved_food_nutrition",
    foreignKeys = [
        ForeignKey(
            entity = SavedFoodEntity::class,
            parentColumns = ["id"],
            childColumns = ["savedFoodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["savedFoodId"], unique = true)]
)
data class SavedFoodNutritionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val savedFoodId: Long,
    val caloriesKcal: Double? = null,
    val proteinG: Double? = null,
    val carbohydratesG: Double? = null,
    val fatG: Double? = null,
    val fiberG: Double? = null,
    val sugarG: Double? = null,
    val saturatedFatG: Double? = null,
    val calciumMg: Double? = null,
    val ironMg: Double? = null,
    val magnesiumMg: Double? = null,
    val phosphorusMg: Double? = null,
    val potassiumMg: Double? = null,
    val sodiumMg: Double? = null,
    val zincMg: Double? = null,
    val copperMg: Double? = null,
    val manganeseMg: Double? = null,
    val seleniumUg: Double? = null,
    val vitaminAUg: Double? = null,
    val vitaminCMg: Double? = null,
    val vitaminDUg: Double? = null,
    val vitaminEMg: Double? = null,
    val vitaminKUg: Double? = null,
    val vitaminB1Mg: Double? = null,
    val vitaminB2Mg: Double? = null,
    val vitaminB3Mg: Double? = null,
    val vitaminB5Mg: Double? = null,
    val vitaminB6Mg: Double? = null,
    val vitaminB7Ug: Double? = null,
    val vitaminB9Ug: Double? = null,
    val vitaminB12Ug: Double? = null
)

@Entity(
    tableName = "nutrition_targets",
    primaryKeys = ["nutrientKey"]
)
data class NutritionTargetEntity(
    val nutrientKey: String,
    val targetValue: Double,
    val unit: String
)
