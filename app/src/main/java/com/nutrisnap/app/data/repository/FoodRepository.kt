package com.nutrisnap.app.data.repository

import com.nutrisnap.app.data.db.NutriSnapDatabase
import com.nutrisnap.app.data.models.FoodEntryEntity
import com.nutrisnap.app.data.models.FoodSource
import com.nutrisnap.app.data.models.MealEntity
import com.nutrisnap.app.data.models.MealType
import com.nutrisnap.app.data.models.NutritionEntity
import com.nutrisnap.app.data.models.NutritionTargetEntity
import com.nutrisnap.app.data.models.SavedFoodEntity
import com.nutrisnap.app.data.models.SavedFoodNutritionEntity
import java.time.LocalDate

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
            database.nutritionDao().insertNutrition(
                nutrition.copy(foodEntryId = foodId)
            )
        }

        return foodId
    }

    suspend fun getMealsForDate(date: LocalDate): List<MealEntity> = database.mealDao().getMealsForDate(date)

    suspend fun getFoodsForMeal(mealId: Long): List<FoodEntryEntity> = database.foodEntryDao().getFoodsForMeal(mealId)

    suspend fun getNutritionForFood(foodEntryId: Long): NutritionEntity? = database.nutritionDao().getNutritionForFood(foodEntryId)

    suspend fun saveFoodTemplate(
        name: String,
        quantity: Double,
        unit: String,
        weightGrams: Double? = null,
        nutrition: SavedFoodNutritionEntity? = null
    ) {
        val savedFoodId = database.savedFoodDao().insertSavedFood(
            SavedFoodEntity(
                name = name,
                defaultQuantity = quantity,
                defaultUnit = unit,
                defaultWeightGrams = weightGrams
            )
        )

        if (nutrition != null) {
            database.savedFoodNutritionDao().insertNutrition(
                nutrition.copy(savedFoodId = savedFoodId)
            )
        }
    }

    suspend fun getSavedFoods(): List<SavedFoodEntity> = database.savedFoodDao().getSavedFoods()

    suspend fun getSavedFoodNutrition(savedFoodId: Long): SavedFoodNutritionEntity? =
        database.savedFoodNutritionDao().getNutritionForSavedFood(savedFoodId)

    suspend fun upsertTarget(target: NutritionTargetEntity) = database.nutritionTargetDao().upsertTarget(target)

    suspend fun getTargets(): List<NutritionTargetEntity> = database.nutritionTargetDao().getAllTargets()
}
