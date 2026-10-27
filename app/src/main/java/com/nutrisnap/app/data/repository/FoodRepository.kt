package com.nutrisnap.app.data.repository

import com.nutrisnap.app.data.db.NutriSnapDatabase
import androidx.room.withTransaction
import com.nutrisnap.app.data.models.FoodEntryEntity
import com.nutrisnap.app.data.models.FoodLogDraft
import com.nutrisnap.app.data.models.FoodSource
import com.nutrisnap.app.data.models.HistoryDaySummary
import com.nutrisnap.app.data.models.MealEntity
import com.nutrisnap.app.data.models.MealType
import com.nutrisnap.app.data.models.MealFoodNutritionRow
import com.nutrisnap.app.data.models.NutritionEntity
import com.nutrisnap.app.data.models.NutritionTargetEntity
import com.nutrisnap.app.data.models.SavedFoodEntity
import com.nutrisnap.app.data.models.SavedFoodNutritionEntity
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class FoodRepository(private val database: NutriSnapDatabase) {
    suspend fun insertMeal(date: LocalDate, mealType: MealType, snackIndex: Int? = null): Long {
        return database.mealDao().insertMeal(
            MealEntity(
                date = date,
                mealType = mealType,
                snackIndex = snackIndex
            )
        )
    }

    suspend fun insertFood(
        mealId: Long,
        name: String,
        quantity: Double,
        unit: String,
        weightGrams: Double? = null,
        source: FoodSource = FoodSource.MANUAL,
        nutrition: NutritionEntity? = null
    ): Long {
        return database.withTransaction {
            val foodId = database.foodEntryDao().insertFood(
                FoodEntryEntity(
                    mealId = mealId,
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    weightGrams = weightGrams,
                    source = source
                )
            )

            if (nutrition != null) {
                database.nutritionDao().insertNutrition(nutrition.copy(id = 0, foodEntryId = foodId))
            }

            foodId
        }
    }

    suspend fun addFoodToMeal(
        date: LocalDate,
        mealType: MealType,
        snackIndex: Int?,
        name: String,
        quantity: Double,
        unit: String,
        weightGrams: Double?,
        source: FoodSource,
        nutrition: NutritionEntity
    ): Long = database.withTransaction {
        val mealId = database.mealDao().findMealId(date, mealType, snackIndex)
            ?: database.mealDao().insertMeal(MealEntity(date = date, mealType = mealType, snackIndex = snackIndex))
        val foodId = database.foodEntryDao().insertFood(
            FoodEntryEntity(
                mealId = mealId,
                name = name.trim(),
                quantity = quantity,
                unit = unit.trim(),
                weightGrams = weightGrams,
                source = source
            )
        )
        database.nutritionDao().insertNutrition(nutrition.copy(id = 0, foodEntryId = foodId))
        foodId
    }

    suspend fun addFoodsToMeal(
        date: LocalDate,
        mealType: MealType,
        snackIndex: Int?,
        foods: List<FoodLogDraft>
    ) = database.withTransaction {
        require(foods.isNotEmpty()) { "Add at least one food before saving." }
        val mealId = database.mealDao().findMealId(date, mealType, snackIndex)
            ?: database.mealDao().insertMeal(MealEntity(date = date, mealType = mealType, snackIndex = snackIndex))
        foods.forEach { food ->
            require(food.name.isNotBlank() && food.quantity > 0.0 && food.unit.isNotBlank()) {
                "Every food needs a name, positive quantity, and unit."
            }
            val foodId = database.foodEntryDao().insertFood(
                FoodEntryEntity(
                    mealId = mealId,
                    name = food.name.trim(),
                    quantity = food.quantity,
                    unit = food.unit.trim(),
                    weightGrams = food.weightGrams,
                    source = food.source
                )
            )
            database.nutritionDao().insertNutrition(food.nutrition.copy(id = 0, foodEntryId = foodId))
        }
    }

    suspend fun nextSnackIndex(date: LocalDate): Int = database.mealDao().nextSnackIndex(date)

    fun observeMealLogs(date: LocalDate): Flow<List<MealFoodNutritionRow>> =
        database.foodEntryDao().observeMealLogs(date)

    fun observeMealLogsBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<MealFoodNutritionRow>> =
        database.foodEntryDao().observeMealLogsBetween(startDate, endDate)

    fun observeRecentHistory(startDate: LocalDate): Flow<List<HistoryDaySummary>> =
        database.foodEntryDao().observeRecentHistory(startDate)

    suspend fun getMealsForDate(date: LocalDate): List<MealEntity> = database.mealDao().getMealsForDate(date)

    suspend fun getFoodsForMeal(mealId: Long): List<FoodEntryEntity> = database.foodEntryDao().getFoodsForMeal(mealId)

    suspend fun getNutritionForFood(foodEntryId: Long): NutritionEntity? = database.nutritionDao().getNutritionForFood(foodEntryId)

    suspend fun saveFoodTemplate(
        name: String,
        quantity: Double,
        unit: String,
        weightGrams: Double? = null,
        nutrition: SavedFoodNutritionEntity? = null
    ) = database.withTransaction {
        val existing = database.savedFoodDao().findByName(name.trim())
        val savedFoodId = if (existing == null) {
            database.savedFoodDao().insertSavedFood(
                SavedFoodEntity(
                    name = name.trim(),
                    defaultQuantity = quantity,
                    defaultUnit = unit.trim(),
                    defaultWeightGrams = weightGrams
                )
            )
        } else {
            database.savedFoodDao().update(
                existing.copy(
                    defaultQuantity = quantity,
                    defaultUnit = unit.trim(),
                    defaultWeightGrams = weightGrams,
                    updatedAtEpochMillis = System.currentTimeMillis()
                )
            )
            existing.id
        }

        if (nutrition != null) {
            database.savedFoodNutritionDao().insertNutrition(
                nutrition.copy(id = 0, savedFoodId = savedFoodId)
            )
        }
    }

    suspend fun getSavedFoods(): List<SavedFoodEntity> = database.savedFoodDao().getSavedFoods()

    suspend fun getSavedFoodNutrition(savedFoodId: Long): SavedFoodNutritionEntity? =
        database.savedFoodNutritionDao().getNutritionForSavedFood(savedFoodId)

    fun observeSavedFoods(): Flow<List<SavedFoodEntity>> = database.savedFoodDao().observeSavedFoods()

    suspend fun addSavedFoodToMeal(savedFoodId: Long, date: LocalDate, mealType: MealType, snackIndex: Int?) {
        database.withTransaction {
            val saved = database.savedFoodDao().getSavedFood(savedFoodId) ?: return@withTransaction
            val mealId = database.mealDao().findMealId(date, mealType, snackIndex)
                ?: database.mealDao().insertMeal(MealEntity(date = date, mealType = mealType, snackIndex = snackIndex))
            val foodId = database.foodEntryDao().insertFood(
                FoodEntryEntity(
                    mealId = mealId,
                    name = saved.name,
                    quantity = saved.defaultQuantity,
                    unit = saved.defaultUnit,
                    weightGrams = saved.defaultWeightGrams,
                    source = FoodSource.SAVED_FOOD
                )
            )
            database.savedFoodNutritionDao().getNutritionForSavedFood(savedFoodId)?.let { template ->
                database.nutritionDao().insertNutrition(
                    NutritionEntity(
                        foodEntryId = foodId,
                        caloriesKcal = template.caloriesKcal,
                        proteinG = template.proteinG,
                        carbohydratesG = template.carbohydratesG,
                        fatG = template.fatG,
                        fiberG = template.fiberG,
                        sugarG = template.sugarG,
                        saturatedFatG = template.saturatedFatG,
                        calciumMg = template.calciumMg,
                        ironMg = template.ironMg,
                        magnesiumMg = template.magnesiumMg,
                        phosphorusMg = template.phosphorusMg,
                        potassiumMg = template.potassiumMg,
                        sodiumMg = template.sodiumMg,
                        zincMg = template.zincMg,
                        copperMg = template.copperMg,
                        manganeseMg = template.manganeseMg,
                        seleniumUg = template.seleniumUg,
                        vitaminAUg = template.vitaminAUg,
                        vitaminCMg = template.vitaminCMg,
                        vitaminDUg = template.vitaminDUg,
                        vitaminEMg = template.vitaminEMg,
                        vitaminKUg = template.vitaminKUg,
                        vitaminB1Mg = template.vitaminB1Mg,
                        vitaminB2Mg = template.vitaminB2Mg,
                        vitaminB3Mg = template.vitaminB3Mg,
                        vitaminB5Mg = template.vitaminB5Mg,
                        vitaminB6Mg = template.vitaminB6Mg,
                        vitaminB7Ug = template.vitaminB7Ug,
                        vitaminB9Ug = template.vitaminB9Ug,
                        vitaminB12Ug = template.vitaminB12Ug
                    )
                )
            }
        }
    }

    suspend fun upsertTarget(target: NutritionTargetEntity) = database.nutritionTargetDao().upsertTarget(target)

    suspend fun getTargets(): List<NutritionTargetEntity> = database.nutritionTargetDao().getAllTargets()

    fun observeTargets(): Flow<List<NutritionTargetEntity>> = database.nutritionTargetDao().observeTargets()
}
