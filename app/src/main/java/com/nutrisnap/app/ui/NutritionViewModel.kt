package com.nutrisnap.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.nutrisnap.app.BuildConfig
import com.nutrisnap.app.data.db.NutriSnapDatabase
import com.nutrisnap.app.data.gemini.GeminiFoodAnalyzer
import com.nutrisnap.app.data.gemini.GeminiFoodEstimate
import com.nutrisnap.app.data.gemini.GeminiMealEstimate
import com.nutrisnap.app.data.models.FoodSource
import com.nutrisnap.app.data.models.FoodLogDraft
import com.nutrisnap.app.data.models.HistoryDaySummary
import com.nutrisnap.app.data.models.MealFoodNutritionRow
import com.nutrisnap.app.data.models.MealType
import com.nutrisnap.app.data.models.NUTRIENT_DEFINITIONS
import com.nutrisnap.app.data.models.NutritionEntity
import com.nutrisnap.app.data.models.NutritionTargetEntity
import com.nutrisnap.app.data.models.SavedFoodEntity
import com.nutrisnap.app.data.models.SavedFoodNutritionEntity
import com.nutrisnap.app.data.models.toSavedFoodNutrition
import com.nutrisnap.app.data.repository.FoodRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class NutrientCoverage(
    val knownTotal: Double?,
    val knownFoodCount: Int,
    val foodCount: Int
)

data class FoodLogItem(
    val id: Long,
    val name: String,
    val quantity: Double,
    val unit: String,
    val nutrition: NutritionEntity
)

data class MealSection(
    val mealType: MealType,
    val snackIndex: Int? = null,
    val foods: List<FoodLogItem> = emptyList()
) {
    val title: String
        get() = if (mealType == MealType.SNACK) "Snack ${snackIndex ?: 1}" else mealType.name.lowercase().replaceFirstChar { it.uppercase() }
}

data class DashboardState(
    val date: LocalDate = LocalDate.now(),
    val meals: List<MealSection> = emptyList(),
    val totals: Map<String, NutrientCoverage> = emptyMap(),
    val targets: Map<String, NutritionTargetEntity> = emptyMap()
) {
    val foodCount: Int get() = meals.sumOf { it.foods.size }
}

data class AnalyticsState(
    val periodDays: Int = 7,
    val daysWithLogs: Int = 0,
    val averages: Map<String, NutrientCoverage> = emptyMap()
)

data class PhotoAnalysisState(
    val isLoading: Boolean = false,
    val estimate: GeminiMealEstimate? = null,
    val error: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = FoodRepository(NutriSnapDatabase.getInstance(application))
    private val analyzer = GeminiFoodAnalyzer(
        apiKey = BuildConfig.GEMINI_API_KEY,
        model = BuildConfig.GEMINI_MODEL,
        fallbackModel = BuildConfig.GEMINI_FALLBACK_MODEL,
        contentResolver = application.contentResolver
    )
    private val selectedDate = MutableStateFlow(LocalDate.now())
    private val analyticsPeriodDays = MutableStateFlow(7)
    private val _photoAnalysis = MutableStateFlow(PhotoAnalysisState())

    val photoAnalysis: StateFlow<PhotoAnalysisState> = _photoAnalysis

    val dashboard: StateFlow<DashboardState> = selectedDate.flatMapLatest { date ->
        combine(repository.observeMealLogs(date), repository.observeTargets()) { rows, targets ->
            buildDashboard(date, rows, targets)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardState())

    val savedFoods: StateFlow<List<SavedFoodEntity>> = repository.observeSavedFoods()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val historyDays: StateFlow<List<HistoryDaySummary>> = repository
        .observeRecentHistory(LocalDate.now().minusDays(29))
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val analytics: StateFlow<AnalyticsState> = combine(selectedDate, analyticsPeriodDays) { date, days -> date to days }
        .flatMapLatest { (endDate, days) ->
            repository.observeMealLogsBetween(endDate.minusDays(days.toLong() - 1), endDate).map { rows ->
                buildAnalytics(days, rows)
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AnalyticsState())

    fun showDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun setAnalyticsPeriod(days: Int) {
        analyticsPeriodDays.value = days.coerceIn(7, 30)
    }

    fun analyzePhoto(uri: Uri) {
        viewModelScope.launch {
            _photoAnalysis.value = PhotoAnalysisState(isLoading = true)
            runCatching { analyzer.analyze(uri) }
                .onSuccess { _photoAnalysis.value = PhotoAnalysisState(estimate = it) }
                .onFailure {
                    _photoAnalysis.value = PhotoAnalysisState(
                        error = it.message ?: "Gemini analysis failed. Retry or enter the food manually."
                    )
                }
        }
    }

    fun saveGeminiFoods(
        foods: List<GeminiFoodEstimate>,
        mealType: MealType,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                val date = selectedDate.value
                val snackIndex = if (mealType == MealType.SNACK) repository.nextSnackIndex(date) else null
                repository.addFoodsToMeal(
                    date = date,
                    mealType = mealType,
                    snackIndex = snackIndex,
                    foods = foods.map {
                        FoodLogDraft(it.name, it.quantity, it.unit, it.estimatedWeightGrams, it.nutrition, it.source)
                    }
                )
            }.onSuccess {
                _photoAnalysis.value = PhotoAnalysisState()
                onResult(Result.success(Unit))
            }.onFailure { onResult(Result.failure(it)) }
        }
    }

    fun saveFood(
        name: String,
        quantity: Double,
        unit: String,
        weightGrams: Double?,
        mealType: MealType,
        nutrition: NutritionEntity,
        saveAsTemplate: Boolean,
        onResult: (Result<Unit>) -> Unit
    ) {
        viewModelScope.launch {
            runCatching {
                val date = selectedDate.value
                val snackIndex = if (mealType == MealType.SNACK) repository.nextSnackIndex(date) else null
                repository.addFoodToMeal(
                    date = date,
                    mealType = mealType,
                    snackIndex = snackIndex,
                    name = name,
                    quantity = quantity,
                    unit = unit,
                    weightGrams = weightGrams,
                    source = FoodSource.MANUAL,
                    nutrition = nutrition
                )
                if (saveAsTemplate) {
                    repository.saveFoodTemplate(
                        name = name,
                        quantity = quantity,
                        unit = unit,
                        weightGrams = weightGrams,
                        nutrition = nutrition.toSavedFoodNutrition()
                    )
                }
            }.onSuccess { onResult(Result.success(Unit)) }
                .onFailure { onResult(Result.failure(it)) }
        }
    }

    fun addSavedFood(savedFoodId: Long, mealType: MealType, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val date = selectedDate.value
                val snackIndex = if (mealType == MealType.SNACK) repository.nextSnackIndex(date) else null
                repository.addSavedFoodToMeal(savedFoodId, date, mealType, snackIndex)
            }.onSuccess { onResult(Result.success(Unit)) }
                .onFailure { onResult(Result.failure(it)) }
        }
    }

    fun saveTarget(key: String, value: Double, unit: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.upsertTarget(NutritionTargetEntity(key, value, unit))
            }.onSuccess { onResult(Result.success(Unit)) }
                .onFailure { onResult(Result.failure(it)) }
        }
    }

    private fun buildDashboard(
        date: LocalDate,
        rows: List<MealFoodNutritionRow>,
        targetList: List<NutritionTargetEntity>
    ): DashboardState {
        val targetMap = targetList.associateBy { it.nutrientKey }
        val foodsByMealId = rows.filter { it.foodId != null }.groupBy { it.mealId }
        val loggedMeals = rows.distinctBy { it.mealId }.map { meal ->
            MealSection(
                mealType = meal.mealType,
                snackIndex = meal.snackIndex,
                foods = foodsByMealId[meal.mealId].orEmpty().mapNotNull { row ->
                    row.foodId?.let { id ->
                        FoodLogItem(
                            id = id,
                            name = row.foodName.orEmpty(),
                            quantity = row.quantity ?: 1.0,
                            unit = row.unit.orEmpty(),
                            nutrition = row.toNutritionEntity()
                        )
                    }
                }
            )
        }
        val baseMeals = listOf(MealSection(MealType.BREAKFAST), MealSection(MealType.LUNCH), MealSection(MealType.DINNER))
        val allMeals = (loggedMeals + baseMeals)
            .distinctBy { if (it.mealType == MealType.SNACK) "SNACK-${it.snackIndex}" else it.mealType.name }
            .sortedWith(compareBy<MealSection> { it.mealType.ordinal }.thenBy { it.snackIndex ?: 0 })
        val foodRows = rows.filter { it.foodId != null }
        val totals = NUTRIENT_DEFINITIONS.associate { definition ->
            val knownValues = foodRows.mapNotNull { it.valueFor(definition.key) }
            definition.key to NutrientCoverage(
                knownTotal = knownValues.takeIf { it.isNotEmpty() }?.sum(),
                knownFoodCount = knownValues.size,
                foodCount = foodRows.size
            )
        }
        return DashboardState(date, allMeals, totals, targetMap)
    }

    private fun buildAnalytics(days: Int, rows: List<MealFoodNutritionRow>): AnalyticsState {
        val foodRows = rows.filter { it.foodId != null }
        val dailyRows = foodRows.groupBy { it.mealDate }
        val averages = NUTRIENT_DEFINITIONS.associate { definition ->
            val dailyKnown = dailyRows.values.mapNotNull { day ->
                val values = day.mapNotNull { it.valueFor(definition.key) }
                values.takeIf { it.isNotEmpty() }?.sum()
            }
            definition.key to NutrientCoverage(
                knownTotal = dailyKnown.takeIf { it.isNotEmpty() }?.average(),
                knownFoodCount = dailyKnown.size,
                foodCount = dailyRows.size
            )
        }
        return AnalyticsState(days, dailyRows.size, averages)
    }
}

private fun MealFoodNutritionRow.valueFor(key: String): Double? = when (key) {
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

private fun MealFoodNutritionRow.toNutritionEntity() = NutritionEntity(
    foodEntryId = foodId ?: 0,
    caloriesKcal = caloriesKcal,
    proteinG = proteinG,
    carbohydratesG = carbohydratesG,
    fatG = fatG,
    fiberG = fiberG,
    sugarG = sugarG,
    saturatedFatG = saturatedFatG,
    calciumMg = calciumMg,
    ironMg = ironMg,
    magnesiumMg = magnesiumMg,
    phosphorusMg = phosphorusMg,
    potassiumMg = potassiumMg,
    sodiumMg = sodiumMg,
    zincMg = zincMg,
    copperMg = copperMg,
    manganeseMg = manganeseMg,
    seleniumUg = seleniumUg,
    vitaminAUg = vitaminAUg,
    vitaminCMg = vitaminCMg,
    vitaminDUg = vitaminDUg,
    vitaminEMg = vitaminEMg,
    vitaminKUg = vitaminKUg,
    vitaminB1Mg = vitaminB1Mg,
    vitaminB2Mg = vitaminB2Mg,
    vitaminB3Mg = vitaminB3Mg,
    vitaminB5Mg = vitaminB5Mg,
    vitaminB6Mg = vitaminB6Mg,
    vitaminB7Ug = vitaminB7Ug,
    vitaminB9Ug = vitaminB9Ug,
    vitaminB12Ug = vitaminB12Ug
)
