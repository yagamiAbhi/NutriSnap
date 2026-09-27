# NutriSnap

NutriSnap is a local-first Android nutrition tracker for a single user. It lets users log meals, review AI-estimated nutrition, edit the result, and keep all historical data on-device with SQLite.

## Stack

- Kotlin
- Jetpack Compose
- Room + SQLite
- Material 3
- Android CameraX + Photo Picker
- Gemini REST API via an HTTP client (Retrofit or OkHttp)
- Local-only storage, no backend or auth

## Architecture summary

The app follows the requested local-first flow:

Camera / manual entry -> Gemini analysis -> review/edit -> SQLite -> daily/7-day/30-day dashboards

This keeps nutrition records private and persistent on the user’s device while still enabling AI-assisted food recognition.

## Project structure

```text
app/
  src/main/
    java/com/nutrisnap/app/
      data/
        db/
        models/
        repository/
      gemini/
      ui/
        navigation/
        screens/
        theme/
    res/
      drawable/
      values/
AndroidManifest.xml
build.gradle.kts
settings.gradle.kts
build.gradle.kts
gradle.properties
.gitignore
```

## SQLite schema

The app uses Room entities to represent the following logical tables.

### Meal
- id: Long
- date: LocalDate (stored as ISO string or epoch day)
- mealType: TEXT (BREAKFAST, LUNCH, DINNER, SNACK)
- snackIndex: INTEGER nullable
- createdAt: Long
- updatedAt: Long

### FoodEntry
- id: Long
- mealId: Long foreign key to Meal
- name: String
- quantity: Double
- unit: String
- weightGrams: Double nullable
- source: TEXT (GEMINI, MANUAL, SAVED_FOOD)
- createdAt: Long
- updatedAt: Long

### Nutrition
- id: Long
- foodEntryId: Long foreign key to FoodEntry
- caloriesKcal: Double nullable
- proteinG: Double nullable
- carbohydratesG: Double nullable
- fatG: Double nullable
- fiberG: Double nullable
- sugarG: Double nullable
- saturatedFatG: Double nullable
- calciumMg: Double nullable
- ironMg: Double nullable
- magnesiumMg: Double nullable
- phosphorusMg: Double nullable
- potassiumMg: Double nullable
- sodiumMg: Double nullable
- zincMg: Double nullable
- copperMg: Double nullable
- manganeseMg: Double nullable
- seleniumUg: Double nullable
- vitaminAUg: Double nullable
- vitaminCMg: Double nullable
- vitaminDUg: Double nullable
- vitaminEMg: Double nullable
- vitaminKUg: Double nullable
- vitaminB1Mg: Double nullable
- vitaminB2Mg: Double nullable
- vitaminB3Mg: Double nullable
- vitaminB5Mg: Double nullable
- vitaminB6Mg: Double nullable
- vitaminB7Ug: Double nullable
- vitaminB9Ug: Double nullable
- vitaminB12Ug: Double nullable

Every nutrient column is nullable, as required by the app’s unknown-data policy.

### SavedFood
- id: Long
- name: String
- defaultQuantity: Double
- defaultUnit: String
- defaultWeightGrams: Double nullable
- nutritionSnapshot: stored as nutrition fields or as a link to a Nutrition row, depending on the chosen ORM design
- createdAt: Long
- updatedAt: Long

### NutritionTarget
- nutrient: String
- targetValue: Double
- unit: String

## Null/unknown nutrient handling

The app follows these rules:

- null means the value is unknown or not reasonably estimable
- 0 means the amount is known to be effectively zero
- nulls are never converted to zero during aggregation
- unknown values are displayed as placeholders such as “—”
- totals and daily summaries show completeness separately from known totals

## Gemini request/response contract

Gemini is only used for image and food analysis. The request sends a photo and a strongly defined JSON schema, and the response is validated before it is accepted.

```json
{
  "meal_description": "Roti with paneer curry and salad",
  "foods": [
    {
      "name": "Roti",
      "quantity": 3,
      "unit": "pieces",
      "estimated_weight_g": 120,
      "nutrition": {
        "calories_kcal": 300,
        "protein_g": 9,
        "carbohydrates_g": 60,
        "fat_g": 6,
        "fiber_g": 9,
        "sugar_g": null,
        "saturated_fat_g": null,
        "calcium_mg": 45,
        "iron_mg": 3,
        "magnesium_mg": 60,
        "phosphorus_mg": 150,
        "potassium_mg": 240,
        "sodium_mg": null,
        "zinc_mg": null,
        "copper_mg": null,
        "manganese_mg": null,
        "selenium_ug": null,
        "vitamin_a_ug": null,
        "vitamin_c_mg": null,
        "vitamin_d_ug": null,
        "vitamin_e_mg": null,
        "vitamin_k_ug": null,
        "vitamin_b1_mg": null,
        "vitamin_b2_mg": null,
        "vitamin_b3_mg": null,
        "vitamin_b5_mg": null,
        "vitamin_b6_mg": null,
        "vitamin_b7_ug": null,
        "vitamin_b9_ug": null,
        "vitamin_b12_ug": null
      }
    }
  ]
}
```

The Gemini prompt includes explicit instructions that null is valid for nutrients with insufficient evidence and that the app requires review before saving.

## Navigation and screens

The app uses bottom navigation:

- HOME
- HISTORY
- ADD FOOD
- ANALYTICS
- SETTINGS

The initial MVP includes:

- Home dashboard for today’s nutrient totals
- Meal list with expandable foods
- Add Food flow with manual and saved-food entry
- Review screen for AI-estimated foods
- Night/standard Material theme and settings

## Maintainability decisions

- One central nutrient definition list is used so labels, units, and validation stay consistent.
- Database logic is isolated under Room entities and repositories, not scattered through UI code.
- Gemini API code lives in a dedicated service layer so the UI never directly calls network logic.
- A review/edit step is required before any AI output is saved to the local database.
- Data retention is kept local and capped to the last 30 days to satisfy the product’s rolling storage requirement.

## MVP implementation status

This repository contains the initial project skeleton and the core architecture required for the first development milestone. It intentionally keeps the app local-first, modular, and ready for incremental feature work.

## Important security note

Do not commit an API key. Development should use a local `local.properties` or a secrets file excluded by `.gitignore`.

Example approach:

```properties
GEMINI_API_KEY=your_local_key_here
```

This key should be loaded from a local, untracked config file and never committed to Git.
