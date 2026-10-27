package com.nutrisnap.app.data.gemini

import android.content.ContentResolver
import android.graphics.ImageDecoder
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.util.Base64
import com.nutrisnap.app.data.models.NUTRIENT_DEFINITIONS
import com.nutrisnap.app.data.models.NutritionEntity
import com.nutrisnap.app.data.models.withValue
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class GeminiFoodAnalyzer(
    private val apiKey: String,
    private val model: String,
    private val fallbackModel: String,
    private val contentResolver: ContentResolver
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(75, TimeUnit.SECONDS)
        .callTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun analyze(imageUri: Uri): GeminiMealEstimate = withContext(Dispatchers.IO) {
        if (apiKey.isBlank() || apiKey == "PASTE_YOUR_GEMINI_API_KEY_HERE") {
            throw GeminiAnalysisException("Add your Gemini API key to the local secrets.properties file, then rebuild the app.")
        }

        val photo = readCompressedImage(imageUri)
        val requestJson = createRequest(photo.first, photo.second)
        try {
            generateWithRetry(model, requestJson)
        } catch (error: GeminiAnalysisException) {
            if (error.statusCode !in FALLBACK_STATUS_CODES || fallbackModel.isBlank() || fallbackModel == model) {
                throw error
            }
            try {
                generateWithRetry(fallbackModel, requestJson)
            } catch (fallbackError: GeminiAnalysisException) {
                throw GeminiAnalysisException(
                    "Gemini is temporarily unavailable on both $model and $fallbackModel. ${fallbackError.message}",
                    fallbackError,
                    fallbackError.statusCode
                )
            }
        }
    }

    private suspend fun generateWithRetry(modelName: String, requestJson: JSONObject): GeminiMealEstimate {
        var attempt = 0
        while (true) {
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent")
                .header("x-goog-api-key", apiKey)
                .post(requestJson.toString().toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            val response = try {
                client.newCall(request).execute()
            } catch (error: IOException) {
                if (attempt >= MAX_RETRY_ATTEMPTS) {
                    throw GeminiAnalysisException("Could not reach Gemini after retries. Check your internet connection and retry.", error)
                }
                waitBeforeRetry(attempt++)
                continue
            }

            val (statusCode, successful, responseText) = response.use {
                Triple(it.code, it.isSuccessful, it.body?.string().orEmpty())
            }
            if (successful) return parseResponse(responseText)

            val message = runCatching {
                JSONObject(responseText).optJSONObject("error")?.optString("message")
            }.getOrNull().orEmpty()
            if (statusCode in RETRY_STATUS_CODES && attempt < MAX_RETRY_ATTEMPTS) {
                waitBeforeRetry(attempt++)
                continue
            }
            val detail = message.takeIf { it.isNotBlank() } ?: "Gemini request failed (HTTP $statusCode)."
            throw GeminiAnalysisException(detail, statusCode = statusCode)
        }
    }

    private suspend fun waitBeforeRetry(attempt: Int) {
        val backoffMillis = INITIAL_RETRY_DELAY_MS * (1L shl attempt)
        delay(backoffMillis + Random.nextLong(0, RETRY_JITTER_MAX_MS))
    }

    private fun readCompressedImage(uri: Uri): Pair<String, String> {
        val bitmap = decodeImage(uri)
        return try {
            val resized = if (bitmap.width > MAX_IMAGE_EDGE || bitmap.height > MAX_IMAGE_EDGE) {
                val scale = minOf(MAX_IMAGE_EDGE.toFloat() / bitmap.width, MAX_IMAGE_EDGE.toFloat() / bitmap.height)
                Bitmap.createScaledBitmap(bitmap, (bitmap.width * scale).toInt(), (bitmap.height * scale).toInt(), true)
            } else bitmap
            val output = ByteArrayOutputStream()
            if (!resized.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)) {
                throw GeminiAnalysisException("Could not prepare the photo for analysis.")
            }
            if (resized !== bitmap) resized.recycle()
            "image/jpeg" to Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
        } finally {
            bitmap.recycle()
        }
    }

    private fun decodeImage(uri: Uri): Bitmap {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            return try {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(contentResolver, uri)) { decoder, info, _ ->
                    val scale = minOf(
                        1f,
                        MAX_IMAGE_EDGE.toFloat() / maxOf(info.size.width, info.size.height)
                    )
                    decoder.setTargetSize(
                        (info.size.width * scale).toInt().coerceAtLeast(1),
                        (info.size.height * scale).toInt().coerceAtLeast(1)
                    )
                    decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                }
            } catch (error: Exception) {
                throw GeminiAnalysisException("Android couldn’t decode this photo. Try choosing a JPEG or PNG image.", error)
            }
        }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        val boundsStream = contentResolver.openInputStream(uri)
            ?: throw GeminiAnalysisException("Could not open the selected photo. Choose it again and retry.")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw GeminiAnalysisException("This image format is not supported on this Android version. Choose a JPEG or PNG photo.")
        }

        var sampleSize = 1
        while (bounds.outWidth / sampleSize > MAX_IMAGE_EDGE || bounds.outHeight / sampleSize > MAX_IMAGE_EDGE) {
            sampleSize *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val imageStream = contentResolver.openInputStream(uri)
            ?: throw GeminiAnalysisException("Could not reopen the selected photo. Choose it again and retry.")
        return imageStream.use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw GeminiAnalysisException("This image format could not be decoded. Choose a JPEG or PNG photo.")
    }

    private fun createRequest(mimeType: String, base64Image: String): JSONObject {
        val parts = JSONArray()
            .put(JSONObject().put("text", ANALYSIS_PROMPT))
            .put(JSONObject().put("inlineData", JSONObject().put("mimeType", mimeType).put("data", base64Image)))
        val content = JSONArray().put(JSONObject().put("role", "user").put("parts", parts))
        val textFormat = JSONObject()
            .put("mimeType", "APPLICATION_JSON")
            .put("schema", responseSchema())
        return JSONObject()
            .put("contents", content)
            .put("generationConfig", JSONObject().put("responseFormat", JSONObject().put("text", textFormat)))
    }

    private fun responseSchema(): JSONObject {
        val nutrientProperties = JSONObject()
        val nutrientKeys = JSONArray()
        NUTRIENT_DEFINITIONS.forEach { nutrient ->
            val key = nutrient.key.toSnakeCase()
            nutrientProperties.put(
                key,
                JSONObject()
                    .put("type", "number")
                    .put("nullable", true)
                    .put("description", "Estimated ${nutrient.label} in ${nutrient.unit}; null when not reasonably estimable from the image.")
            )
            nutrientKeys.put(key)
        }
        val nutritionSchema = JSONObject()
            .put("type", "object")
            .put("properties", nutrientProperties)
            .put("required", nutrientKeys)
        val foodProperties = JSONObject()
            .put("name", JSONObject().put("type", "string"))
            .put("quantity", JSONObject().put("type", "number"))
            .put("unit", JSONObject().put("type", "string"))
            .put("estimated_weight_g", JSONObject().put("type", "number").put("nullable", true))
            .put("nutrition", nutritionSchema)
        val foodSchema = JSONObject()
            .put("type", "object")
            .put("properties", foodProperties)
            .put("required", JSONArray(listOf("name", "quantity", "unit", "estimated_weight_g", "nutrition")))
        return JSONObject()
            .put("type", "object")
            .put(
                "properties",
                JSONObject()
                    .put("meal_description", JSONObject().put("type", "string"))
                    .put("foods", JSONObject().put("type", "array").put("items", foodSchema))
            )
            .put("required", JSONArray(listOf("meal_description", "foods")))
    }

    private fun parseResponse(body: String): GeminiMealEstimate {
        val candidateText = try {
            val response = JSONObject(body)
            val candidates = response.optJSONArray("candidates")
                ?: throw GeminiAnalysisException("Gemini returned no analysis. Try another photo or enter foods manually.")
            val first = candidates.optJSONObject(0)
                ?: throw GeminiAnalysisException("Gemini could not identify food in this photo.")
            val parts = first.optJSONObject("content")?.optJSONArray("parts")
                ?: throw GeminiAnalysisException("Gemini returned an incomplete response. Please retry.")
            (0 until parts.length()).mapNotNull { parts.optJSONObject(it)?.optString("text") }.joinToString("")
        } catch (error: GeminiAnalysisException) {
            throw error
        } catch (error: Exception) {
            throw GeminiAnalysisException("Gemini returned invalid response data. Please retry or enter foods manually.", error)
        }

        val result = try {
            JSONObject(candidateText)
        } catch (error: Exception) {
            throw GeminiAnalysisException("Gemini’s response was not valid JSON. Please retry or enter foods manually.", error)
        }
        val foodsJson = result.optJSONArray("foods")
            ?: throw GeminiAnalysisException("Gemini’s response is missing the foods list. Please retry.")
        if (foodsJson.length() == 0) throw GeminiAnalysisException("No food could be identified confidently. Try another photo or enter foods manually.")
        if (foodsJson.length() > MAX_FOODS) throw GeminiAnalysisException("Gemini returned too many food items. Please retry with a clearer photo.")

        val foods = (0 until foodsJson.length()).map { index ->
            val food = foodsJson.optJSONObject(index)
                ?: throw GeminiAnalysisException("Gemini returned an invalid food item. Please retry.")
            val name = food.optString("name").trim()
            val quantity = food.optDouble("quantity", Double.NaN)
            val unit = food.optString("unit").trim()
            if (name.isBlank() || unit.isBlank() || !quantity.isFinite() || quantity <= 0.0) {
                throw GeminiAnalysisException("Gemini returned incomplete food details. Please retry or edit manually.")
            }
            val weight = nullableNumber(food, "estimated_weight_g")
            if (weight != null && (!weight.isFinite() || weight <= 0.0)) {
                throw GeminiAnalysisException("Gemini returned an invalid estimated weight. Please retry.")
            }
            val nutritionJson = food.optJSONObject("nutrition")
            var nutrition = NutritionEntity(foodEntryId = 0)
            NUTRIENT_DEFINITIONS.forEach { nutrient ->
                val raw = nutritionJson?.let { nullableNumber(it, nutrient.key.toSnakeCase()) }
                if (raw != null && (!raw.isFinite() || raw < 0.0)) {
                    throw GeminiAnalysisException("Gemini returned an invalid ${nutrient.label} estimate. Please retry.")
                }
                nutrition = nutrition.withValue(nutrient.key, raw)
            }
            GeminiFoodEstimate(name, quantity, unit, weight, nutrition)
        }
        return GeminiMealEstimate(result.optString("meal_description").takeIf { it.isNotBlank() } ?: "Meal estimate", foods)
    }

    private fun nullableNumber(json: JSONObject, key: String): Double? {
        if (!json.has(key) || json.isNull(key)) return null
        val value = json.optDouble(key, Double.NaN)
        return value.takeIf { it.isFinite() }
            ?: throw GeminiAnalysisException("Gemini returned a malformed value for $key.")
    }

    private fun String.toSnakeCase(): String = replace(Regex("([a-z0-9])([A-Z])"), "$1_$2").lowercase()

    companion object {
        private const val MAX_IMAGE_EDGE = 1600
        private const val JPEG_QUALITY = 84
        private const val MAX_FOODS = 20
        private const val MAX_RETRY_ATTEMPTS = 2
        private const val INITIAL_RETRY_DELAY_MS = 900L
        private const val RETRY_JITTER_MAX_MS = 400L
        private val RETRY_STATUS_CODES = setOf(408, 429, 500, 502, 503, 504)
        private val FALLBACK_STATUS_CODES = setOf(429, 500, 502, 503, 504)
        private val ANALYSIS_PROMPT = """
            Identify the distinct foods visible in this meal photo and estimate each item separately.
            This is nutrition tracking, not medical advice. Image-based portion sizes and recipe ingredients are uncertain.
            Do not claim exactness. Use a reasonable visible quantity and unit; use estimated_weight_g=null if weight cannot be reasonably estimated.
            For every nutrient, return null when the image and ordinary food knowledge do not support a reasonable estimate.
            Never invent values to fill the schema. Null means unknown; zero means confidently negligible.
            Estimate values for the visible portion only, not per 100 g. Consider likely cooking oils and ingredients only when visually evident; otherwise leave uncertain nutrients null.
            If no food can be identified confidently, return an empty foods array rather than guessing.
            Return only the required structured JSON. The user will review and edit every item before it can be saved.
        """.trimIndent()
    }
}

class GeminiAnalysisException(
    message: String,
    cause: Throwable? = null,
    val statusCode: Int? = null
) : Exception(message, cause)
