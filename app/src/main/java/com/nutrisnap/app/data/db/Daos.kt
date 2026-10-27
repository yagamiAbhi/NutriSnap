package com.nutrisnap.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import com.nutrisnap.app.data.models.FoodEntryEntity
import com.nutrisnap.app.data.models.FoodSource
import com.nutrisnap.app.data.models.HistoryDaySummary
import com.nutrisnap.app.data.models.MealEntity
import com.nutrisnap.app.data.models.MealFoodNutritionRow
import com.nutrisnap.app.data.models.NutritionEntity
import com.nutrisnap.app.data.models.NutritionTargetEntity
import com.nutrisnap.app.data.models.SavedFoodEntity
import com.nutrisnap.app.data.models.SavedFoodNutritionEntity
import java.time.LocalDate

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeal(meal: MealEntity): Long

    @Query("SELECT * FROM meals WHERE date = :date ORDER BY mealType, snackIndex")
    suspend fun getMealsForDate(date: LocalDate): List<MealEntity>

    @Query("SELECT COALESCE(MAX(snackIndex), 0) + 1 FROM meals WHERE date = :date AND mealType = 'SNACK'")
    suspend fun nextSnackIndex(date: LocalDate): Int

    @Query("SELECT id FROM meals WHERE date = :date AND mealType = :mealType AND snackIndex IS :snackIndex LIMIT 1")
    suspend fun findMealId(date: LocalDate, mealType: com.nutrisnap.app.data.models.MealType, snackIndex: Int?): Long?
}

@Dao
interface FoodEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(foodEntry: FoodEntryEntity): Long

    @Query("SELECT * FROM food_entries WHERE mealId = :mealId ORDER BY id")
    suspend fun getFoodsForMeal(mealId: Long): List<FoodEntryEntity>

    @Query("SELECT m.id AS mealId, m.date AS mealDate, m.mealType AS mealType, m.snackIndex AS snackIndex, f.id AS foodId, f.name AS foodName, f.quantity AS quantity, f.unit AS unit, f.weightGrams AS weightGrams, f.source AS source, n.caloriesKcal AS caloriesKcal, n.proteinG AS proteinG, n.carbohydratesG AS carbohydratesG, n.fatG AS fatG, n.fiberG AS fiberG, n.sugarG AS sugarG, n.saturatedFatG AS saturatedFatG, n.calciumMg AS calciumMg, n.ironMg AS ironMg, n.magnesiumMg AS magnesiumMg, n.phosphorusMg AS phosphorusMg, n.potassiumMg AS potassiumMg, n.sodiumMg AS sodiumMg, n.zincMg AS zincMg, n.copperMg AS copperMg, n.manganeseMg AS manganeseMg, n.seleniumUg AS seleniumUg, n.vitaminAUg AS vitaminAUg, n.vitaminCMg AS vitaminCMg, n.vitaminDUg AS vitaminDUg, n.vitaminEMg AS vitaminEMg, n.vitaminKUg AS vitaminKUg, n.vitaminB1Mg AS vitaminB1Mg, n.vitaminB2Mg AS vitaminB2Mg, n.vitaminB3Mg AS vitaminB3Mg, n.vitaminB5Mg AS vitaminB5Mg, n.vitaminB6Mg AS vitaminB6Mg, n.vitaminB7Ug AS vitaminB7Ug, n.vitaminB9Ug AS vitaminB9Ug, n.vitaminB12Ug AS vitaminB12Ug FROM meals m LEFT JOIN food_entries f ON f.mealId = m.id LEFT JOIN nutrition n ON n.foodEntryId = f.id WHERE m.date = :date ORDER BY m.mealType, m.snackIndex, f.id")
    fun observeMealLogs(date: LocalDate): Flow<List<MealFoodNutritionRow>>

    @Query("SELECT m.id AS mealId, m.date AS mealDate, m.mealType AS mealType, m.snackIndex AS snackIndex, f.id AS foodId, f.name AS foodName, f.quantity AS quantity, f.unit AS unit, f.weightGrams AS weightGrams, f.source AS source, n.caloriesKcal AS caloriesKcal, n.proteinG AS proteinG, n.carbohydratesG AS carbohydratesG, n.fatG AS fatG, n.fiberG AS fiberG, n.sugarG AS sugarG, n.saturatedFatG AS saturatedFatG, n.calciumMg AS calciumMg, n.ironMg AS ironMg, n.magnesiumMg AS magnesiumMg, n.phosphorusMg AS phosphorusMg, n.potassiumMg AS potassiumMg, n.sodiumMg AS sodiumMg, n.zincMg AS zincMg, n.copperMg AS copperMg, n.manganeseMg AS manganeseMg, n.seleniumUg AS seleniumUg, n.vitaminAUg AS vitaminAUg, n.vitaminCMg AS vitaminCMg, n.vitaminDUg AS vitaminDUg, n.vitaminEMg AS vitaminEMg, n.vitaminKUg AS vitaminKUg, n.vitaminB1Mg AS vitaminB1Mg, n.vitaminB2Mg AS vitaminB2Mg, n.vitaminB3Mg AS vitaminB3Mg, n.vitaminB5Mg AS vitaminB5Mg, n.vitaminB6Mg AS vitaminB6Mg, n.vitaminB7Ug AS vitaminB7Ug, n.vitaminB9Ug AS vitaminB9Ug, n.vitaminB12Ug AS vitaminB12Ug FROM meals m LEFT JOIN food_entries f ON f.mealId = m.id LEFT JOIN nutrition n ON n.foodEntryId = f.id WHERE m.date BETWEEN :startDate AND :endDate ORDER BY m.date, m.mealType, m.snackIndex, f.id")
    fun observeMealLogsBetween(startDate: LocalDate, endDate: LocalDate): Flow<List<MealFoodNutritionRow>>

    @Query("SELECT m.date AS date, COUNT(f.id) AS foodCount, SUM(n.caloriesKcal) AS caloriesKcal, SUM(n.proteinG) AS proteinG FROM meals m INNER JOIN food_entries f ON f.mealId = m.id LEFT JOIN nutrition n ON n.foodEntryId = f.id WHERE m.date >= :startDate GROUP BY m.date ORDER BY m.date DESC")
    fun observeRecentHistory(startDate: LocalDate): Flow<List<HistoryDaySummary>>
}

@Dao
interface NutritionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutrition(nutrition: NutritionEntity): Long

    @Query("SELECT * FROM nutrition WHERE foodEntryId = :foodEntryId")
    suspend fun getNutritionForFood(foodEntryId: Long): NutritionEntity?
}

@Dao
interface SavedFoodDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavedFood(savedFood: SavedFoodEntity): Long

    @Query("SELECT * FROM saved_foods ORDER BY name")
    suspend fun getSavedFoods(): List<SavedFoodEntity>

    @Query("SELECT * FROM saved_foods ORDER BY name")
    fun observeSavedFoods(): Flow<List<SavedFoodEntity>>

    @Query("SELECT * FROM saved_foods WHERE id = :id LIMIT 1")
    suspend fun getSavedFood(id: Long): SavedFoodEntity?

    @Query("SELECT * FROM saved_foods WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): SavedFoodEntity?

    @androidx.room.Update
    suspend fun update(savedFood: SavedFoodEntity)
}

@Dao
interface SavedFoodNutritionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutrition(nutrition: SavedFoodNutritionEntity): Long

    @Query("SELECT * FROM saved_food_nutrition WHERE savedFoodId = :savedFoodId")
    suspend fun getNutritionForSavedFood(savedFoodId: Long): SavedFoodNutritionEntity?
}

@Dao
interface NutritionTargetDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTarget(target: NutritionTargetEntity)

    @Query("SELECT * FROM nutrition_targets")
    suspend fun getAllTargets(): List<NutritionTargetEntity>

    @Query("SELECT * FROM nutrition_targets")
    fun observeTargets(): Flow<List<NutritionTargetEntity>>
}
