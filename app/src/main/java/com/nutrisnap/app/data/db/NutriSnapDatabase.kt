package com.nutrisnap.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.nutrisnap.app.data.models.FoodEntryEntity
import com.nutrisnap.app.data.models.MealEntity
import com.nutrisnap.app.data.models.NutritionEntity
import com.nutrisnap.app.data.models.NutritionTargetEntity
import com.nutrisnap.app.data.models.SavedFoodEntity
import com.nutrisnap.app.data.models.SavedFoodNutritionEntity

@Database(
    entities = [
        MealEntity::class,
        FoodEntryEntity::class,
        NutritionEntity::class,
        SavedFoodEntity::class,
        SavedFoodNutritionEntity::class,
        NutritionTargetEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(RoomConverters::class)
abstract class NutriSnapDatabase : RoomDatabase() {
    abstract fun mealDao(): MealDao
    abstract fun foodEntryDao(): FoodEntryDao
    abstract fun nutritionDao(): NutritionDao
    abstract fun savedFoodDao(): SavedFoodDao
    abstract fun savedFoodNutritionDao(): SavedFoodNutritionDao
    abstract fun nutritionTargetDao(): NutritionTargetDao

    companion object {
        @Volatile
        private var INSTANCE: NutriSnapDatabase? = null

        fun getInstance(context: Context): NutriSnapDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    NutriSnapDatabase::class.java,
                    "nutrisnap.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
