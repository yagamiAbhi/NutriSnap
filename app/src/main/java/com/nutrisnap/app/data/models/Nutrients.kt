package com.nutrisnap.app.data.models

data class NutrientSpec(
    val key: String,
    val label: String,
    val unit: String,
    val category: NutrientCategory,
    val isMacro: Boolean = false
)

enum class NutrientCategory {
    MACRO,
    MINERAL,
    VITAMIN
}

val NUTRIENT_DEFINITIONS: List<NutrientSpec> = listOf(
    NutrientSpec("caloriesKcal", "Calories", "kcal", NutrientCategory.MACRO, true),
    NutrientSpec("proteinG", "Protein", "g", NutrientCategory.MACRO, true),
    NutrientSpec("carbohydratesG", "Carbohydrates", "g", NutrientCategory.MACRO, true),
    NutrientSpec("fatG", "Fat", "g", NutrientCategory.MACRO, true),
    NutrientSpec("fiberG", "Fiber", "g", NutrientCategory.MACRO, true),
    NutrientSpec("sugarG", "Sugar", "g", NutrientCategory.MACRO),
    NutrientSpec("saturatedFatG", "Saturated Fat", "g", NutrientCategory.MACRO),
    NutrientSpec("calciumMg", "Calcium", "mg", NutrientCategory.MINERAL),
    NutrientSpec("ironMg", "Iron", "mg", NutrientCategory.MINERAL),
    NutrientSpec("magnesiumMg", "Magnesium", "mg", NutrientCategory.MINERAL),
    NutrientSpec("phosphorusMg", "Phosphorus", "mg", NutrientCategory.MINERAL),
    NutrientSpec("potassiumMg", "Potassium", "mg", NutrientCategory.MINERAL),
    NutrientSpec("sodiumMg", "Sodium", "mg", NutrientCategory.MINERAL),
    NutrientSpec("zincMg", "Zinc", "mg", NutrientCategory.MINERAL),
    NutrientSpec("copperMg", "Copper", "mg", NutrientCategory.MINERAL),
    NutrientSpec("manganeseMg", "Manganese", "mg", NutrientCategory.MINERAL),
    NutrientSpec("seleniumUg", "Selenium", "µg", NutrientCategory.MINERAL),
    NutrientSpec("vitaminAUg", "Vitamin A", "µg", NutrientCategory.VITAMIN),
    NutrientSpec("vitaminCMg", "Vitamin C", "mg", NutrientCategory.VITAMIN),
    NutrientSpec("vitaminDUg", "Vitamin D", "µg", NutrientCategory.VITAMIN),
    NutrientSpec("vitaminEMg", "Vitamin E", "mg", NutrientCategory.VITAMIN),
    NutrientSpec("vitaminKUg", "Vitamin K", "µg", NutrientCategory.VITAMIN),
    NutrientSpec("vitaminB1Mg", "Vitamin B1", "mg", NutrientCategory.VITAMIN),
    NutrientSpec("vitaminB2Mg", "Vitamin B2", "mg", NutrientCategory.VITAMIN),
    NutrientSpec("vitaminB3Mg", "Vitamin B3", "mg", NutrientCategory.VITAMIN),
    NutrientSpec("vitaminB5Mg", "Vitamin B5", "mg", NutrientCategory.VITAMIN),
    NutrientSpec("vitaminB6Mg", "Vitamin B6", "mg", NutrientCategory.VITAMIN),
    NutrientSpec("vitaminB7Ug", "Vitamin B7", "µg", NutrientCategory.VITAMIN),
    NutrientSpec("vitaminB9Ug", "Vitamin B9", "µg", NutrientCategory.VITAMIN),
    NutrientSpec("vitaminB12Ug", "Vitamin B12", "µg", NutrientCategory.VITAMIN)
)

fun nutrientSpecFor(key: String): NutrientSpec? = NUTRIENT_DEFINITIONS.firstOrNull { it.key == key }
