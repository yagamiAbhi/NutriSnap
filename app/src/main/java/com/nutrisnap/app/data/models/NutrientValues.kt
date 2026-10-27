package com.nutrisnap.app.data.models

fun NutritionEntity.valueFor(key: String): Double? = when (key) {
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

fun NutritionEntity.withValue(key: String, value: Double?): NutritionEntity = when (key) {
    "caloriesKcal" -> copy(caloriesKcal = value)
    "proteinG" -> copy(proteinG = value)
    "carbohydratesG" -> copy(carbohydratesG = value)
    "fatG" -> copy(fatG = value)
    "fiberG" -> copy(fiberG = value)
    "sugarG" -> copy(sugarG = value)
    "saturatedFatG" -> copy(saturatedFatG = value)
    "calciumMg" -> copy(calciumMg = value)
    "ironMg" -> copy(ironMg = value)
    "magnesiumMg" -> copy(magnesiumMg = value)
    "phosphorusMg" -> copy(phosphorusMg = value)
    "potassiumMg" -> copy(potassiumMg = value)
    "sodiumMg" -> copy(sodiumMg = value)
    "zincMg" -> copy(zincMg = value)
    "copperMg" -> copy(copperMg = value)
    "manganeseMg" -> copy(manganeseMg = value)
    "seleniumUg" -> copy(seleniumUg = value)
    "vitaminAUg" -> copy(vitaminAUg = value)
    "vitaminCMg" -> copy(vitaminCMg = value)
    "vitaminDUg" -> copy(vitaminDUg = value)
    "vitaminEMg" -> copy(vitaminEMg = value)
    "vitaminKUg" -> copy(vitaminKUg = value)
    "vitaminB1Mg" -> copy(vitaminB1Mg = value)
    "vitaminB2Mg" -> copy(vitaminB2Mg = value)
    "vitaminB3Mg" -> copy(vitaminB3Mg = value)
    "vitaminB5Mg" -> copy(vitaminB5Mg = value)
    "vitaminB6Mg" -> copy(vitaminB6Mg = value)
    "vitaminB7Ug" -> copy(vitaminB7Ug = value)
    "vitaminB9Ug" -> copy(vitaminB9Ug = value)
    "vitaminB12Ug" -> copy(vitaminB12Ug = value)
    else -> this
}

fun NutritionEntity.toSavedFoodNutrition(savedFoodId: Long = 0): SavedFoodNutritionEntity =
    SavedFoodNutritionEntity(
        savedFoodId = savedFoodId,
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

fun SavedFoodNutritionEntity.toNutritionEntity(foodEntryId: Long = 0): NutritionEntity =
    NutritionEntity(
        foodEntryId = foodEntryId,
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
