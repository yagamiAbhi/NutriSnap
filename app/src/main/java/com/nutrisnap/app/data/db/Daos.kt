package com.nutrisnap.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.nutrisnap.app.data.models.FoodEntryEntity
import com.nutrisnap.app.data.models.MealEntity
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
}

@Dao
interface FoodEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFood(foodEntry: FoodEntryEntity): Long

    @Query("SELECT * FROM food_entries WHERE mealId = :mealId ORDER BY id")
    suspend fun getFoodsForMeal(mealId: Long): List<FoodEntryEntity>
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
}
