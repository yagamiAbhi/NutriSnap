package com.nutrisnap.app.data.db

import androidx.room.TypeConverter
import com.nutrisnap.app.data.models.FoodSource
import com.nutrisnap.app.data.models.MealType
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class RoomConverters {
    private val formatter = DateTimeFormatter.ISO_LOCAL_DATE

    @TypeConverter
    fun fromLocalDate(value: LocalDate?): String? = value?.format(formatter)

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it, formatter) }

    @TypeConverter
    fun fromMealType(value: MealType?): String? = value?.name

    @TypeConverter
    fun toMealType(value: String?): MealType? = value?.let { MealType.valueOf(it) }

    @TypeConverter
    fun fromFoodSource(value: FoodSource?): String? = value?.name

    @TypeConverter
    fun toFoodSource(value: String?): FoodSource? = value?.let { FoodSource.valueOf(it) }
}
