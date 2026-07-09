package com.example.api

import android.util.Log
import com.example.BuildConfig
import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// --- Gemini Request / Response Models ---
data class GeminiRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

data class Content(
    val parts: List<Part>
)

data class Part(
    val text: String
)

data class GenerationConfig(
    val temperature: Float = 0.4f,
    val responseMimeType: String? = null
)

data class GeminiResponse(
    val candidates: List<Candidate>?
)

data class Candidate(
    val content: ContentResponse?
)

data class ContentResponse(
    val parts: List<PartResponse>?
)

data class PartResponse(
    val text: String?
)

// --- Retrofit Endpoint ---
interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") key: String,
        @Body request: GeminiRequest
    ): GeminiResponse
}

// --- Domain Response Models ---
data class FoodRecognitionResult(
    val name: String,
    val category: String,
    val quantity: String,
    val estExpiryDays: Int,
    val confidence: Double,
    val environmentalImpact: String,
    val isSimulated: Boolean = false
)

data class ExpiryPredictionResult(
    val shelfLifeDays: Int,
    val storageTip: String,
    val alertThresholdDays: Int,
    val isSimulated: Boolean = false
)

data class RecipeSuggestion(
    val title: String,
    val prepTime: String,
    val difficulty: String,
    val ingredients: List<String>,
    val instructions: List<String>,
    val co2PreventedKg: Double
)

data class WastePredictionResult(
    val surplusRisk: String, // High, Medium, Low
    val recommendedStockAdjustment: String,
    val predictedWasteKg: Double,
    val actionableSavingTip: String,
    val isSimulated: Boolean = false
)

object GeminiClient {
    private const val TAG = "GeminiClient"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"

    // Check if key is configured
    val isApiKeyAvailable: Boolean
        get() {
            val key = BuildConfig.GEMINI_API_KEY
            return key.isNotBlank() && key != "MY_GEMINI_API_KEY"
        }

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val api: GeminiApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    // AI Food Recognition from image / prompt
    suspend fun recognizeFood(description: String): FoodRecognitionResult = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable) {
            // High quality deterministic mock based on input
            return@withContext getSimulatedRecognition(description)
        }

        val prompt = """
            You are a Food Specialist AI. The user says: "$description".
            Analyze this description or simulated image context. Provide details about the food item in valid JSON format ONLY with the following exact keys:
            {
              "name": "string (name of food)",
              "category": "string (one of: Veggies, Fruits, Dairy, Bakery, Meat, Pantry, Grains)",
              "quantity": "string (estimated quantity with units, e.g. 1.2 kg or 500g)",
              "estExpiryDays": integer (days before it expires),
              "confidence": float (between 0.0 and 1.0),
              "environmentalImpact": "string (short description of the CO2 or environmental footprint saved by rescuing this)"
            }
            Do not include markdown or backticks in the response, just the raw JSON block.
        """.trimIndent()

        try {
            val response = api.generateContent(
                key = BuildConfig.GEMINI_API_KEY,
                request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(responseMimeType = "application/json")
                )
            )
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanedJson = jsonText.replace("```json", "").replace("```", "").trim()
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(cleanedJson) ?: throw Exception("JSON Parse failed")

            FoodRecognitionResult(
                name = (map["name"] as? String) ?: "Fresh Food Item",
                category = (map["category"] as? String) ?: "Pantry",
                quantity = (map["quantity"] as? String) ?: "1 unit",
                estExpiryDays = ((map["estExpiryDays"] as? Double)?.toInt()) ?: 3,
                confidence = (map["confidence"] as? Double) ?: 0.95,
                environmentalImpact = (map["environmentalImpact"] as? String) ?: "Prevents landfill methane emissions.",
                isSimulated = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error in recognizeFood: ${e.message}", e)
            getSimulatedRecognition(description)
        }
    }

    // AI Expiry prediction
    suspend fun predictExpiry(foodName: String, category: String): ExpiryPredictionResult = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable) {
            return@withContext getSimulatedExpiry(foodName, category)
        }

        val prompt = """
            You are an AI Food Preservation expert. 
            For the food item: "$foodName" under category: "$category", predict the optimal remaining shelf life.
            Respond ONLY in valid JSON format with the following exact keys:
            {
              "shelfLifeDays": integer (estimated optimal remaining shelf life),
              "storageTip": "string (expert storage tip to maximize freshness)",
              "alertThresholdDays": integer (alert threshold in days, e.g. notify when 1 or 2 days left)
            }
            Do not include markdown or backticks.
        """.trimIndent()

        try {
            val response = api.generateContent(
                key = BuildConfig.GEMINI_API_KEY,
                request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(responseMimeType = "application/json")
                )
            )
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanedJson = jsonText.replace("```json", "").replace("```", "").trim()
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(cleanedJson) ?: throw Exception("JSON Parse failed")

            ExpiryPredictionResult(
                shelfLifeDays = ((map["shelfLifeDays"] as? Double)?.toInt()) ?: 4,
                storageTip = (map["storageTip"] as? String) ?: "Store in airtight containers.",
                alertThresholdDays = ((map["alertThresholdDays"] as? Double)?.toInt()) ?: 2,
                isSimulated = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error in predictExpiry: ${e.message}", e)
            getSimulatedExpiry(foodName, category)
        }
    }

    // AI Recipe suggestions
    suspend fun getRecipeSuggestions(ingredients: List<String>): List<RecipeSuggestion> = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable) {
            return@withContext getSimulatedRecipes(ingredients)
        }

        val prompt = """
            You are a Gourmet Eco-Chef AI. Given the following ingredients that are expiring soon: ${ingredients.joinToString(", ")}.
            Generate 2 delicious creative zero-waste recipes.
            Respond in valid JSON format ONLY, as a list of recipes.
            JSON Schema:
            [
              {
                "title": "Recipe Title",
                "prepTime": "Prep time (e.g. 15 mins)",
                "difficulty": "Easy, Medium or Hard",
                "ingredients": ["list of strings"],
                "instructions": ["step 1", "step 2"],
                "co2PreventedKg": float (estimated CO2 prevented in kg by reusing these items, e.g. 1.8)
              }
            ]
            Do not include markdown or backticks.
        """.trimIndent()

        try {
            val response = api.generateContent(
                key = BuildConfig.GEMINI_API_KEY,
                request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(responseMimeType = "application/json")
                )
            )
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanedJson = jsonText.replace("```json", "").replace("```", "").trim()
            
            val type = com.squareup.moshi.Types.newParameterizedType(List::class.java, Map::class.java)
            val adapter = moshi.adapter<List<Map<String, Any>>>(type)
            val list = adapter.fromJson(cleanedJson) ?: throw Exception("Parse failed")

            list.map { map ->
                RecipeSuggestion(
                    title = (map["title"] as? String) ?: "Zero Waste Meal",
                    prepTime = (map["prepTime"] as? String) ?: "20 mins",
                    difficulty = (map["difficulty"] as? String) ?: "Easy",
                    ingredients = (map["ingredients"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                    instructions = (map["instructions"] as? List<*>)?.mapNotNull { it?.toString() } ?: emptyList(),
                    co2PreventedKg = (map["co2PreventedKg"] as? Double) ?: 1.5
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error in getRecipeSuggestions: ${e.message}", e)
            getSimulatedRecipes(ingredients)
        }
    }

    // AI Waste and Demand prediction for restaurants
    suspend fun predictWasteDemand(foodName: String, inventoryCount: Int): WastePredictionResult = withContext(Dispatchers.IO) {
        if (!isApiKeyAvailable) {
            return@withContext getSimulatedWastePrediction(foodName)
        }

        val prompt = """
            You are a Commercial Food Waste auditor.
            For restaurant item: "$foodName" with current stock quantity: $inventoryCount units, predict the waste risk and seasonal demand adjustments.
            Respond ONLY in valid JSON format with the following exact keys:
            {
              "surplusRisk": "string (High, Medium or Low)",
              "recommendedStockAdjustment": "string (e.g. Decrease orders by 15% next week)",
              "predictedWasteKg": float (expected waste if unmanaged in kg),
              "actionableSavingTip": "string (highly tactical saving advice for this specific item)"
            }
            Do not include markdown or backticks.
        """.trimIndent()

        try {
            val response = api.generateContent(
                key = BuildConfig.GEMINI_API_KEY,
                request = GeminiRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    generationConfig = GenerationConfig(responseMimeType = "application/json")
                )
            )
            val jsonText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
            val cleanedJson = jsonText.replace("```json", "").replace("```", "").trim()
            val adapter = moshi.adapter(Map::class.java)
            val map = adapter.fromJson(cleanedJson) ?: throw Exception("JSON Parse failed")

            WastePredictionResult(
                surplusRisk = (map["surplusRisk"] as? String) ?: "Medium",
                recommendedStockAdjustment = (map["recommendedStockAdjustment"] as? String) ?: "Optimize restock timeline",
                predictedWasteKg = (map["predictedWasteKg"] as? Double) ?: 2.4,
                actionableSavingTip = (map["actionableSavingTip"] as? String) ?: "Repurpose surplus into soups.",
                isSimulated = false
            )
        } catch (e: Exception) {
            Log.e(TAG, "Gemini API error in predictWasteDemand: ${e.message}", e)
            getSimulatedWastePrediction(foodName)
        }
    }

    // --- High-Quality Simulation Helpers ---

    private fun getSimulatedRecognition(desc: String): FoodRecognitionResult {
        val lower = desc.lowercase()
        return when {
            "banana" in lower || "fruit" in lower -> FoodRecognitionResult(
                name = "Fresh Bananas",
                category = "Fruits",
                quantity = "5 Units",
                estExpiryDays = 3,
                confidence = 0.98,
                environmentalImpact = "Rescuing this avoids 1.5kg of CO2 equivalent landfill emissions.",
                isSimulated = true
            )
            "milk" in lower || "yogurt" in lower || "dairy" in lower -> FoodRecognitionResult(
                name = "Low-fat Milk",
                category = "Dairy",
                quantity = "1 Litre",
                estExpiryDays = 4,
                confidence = 0.95,
                environmentalImpact = "Dairy has high carbon water impact. Saving it preserves 2,000L of water.",
                isSimulated = true
            )
            "bread" in lower || "bakery" in lower || "croissant" in lower -> FoodRecognitionResult(
                name = "Sourdough Bread",
                category = "Bakery",
                quantity = "1 Loaf",
                estExpiryDays = 2,
                confidence = 0.92,
                environmentalImpact = "Bakery waste is highly preventable. Prevents 1.1kg of CO2 release.",
                isSimulated = true
            )
            else -> FoodRecognitionResult(
                name = desc.ifBlank { "Organic Produce" },
                category = "Veggies",
                quantity = "1.0 kg",
                estExpiryDays = 5,
                confidence = 0.85,
                environmentalImpact = "Keeps organic food in the human loop, avoiding waste decomposition.",
                isSimulated = true
            )
        }
    }

    private fun getSimulatedExpiry(foodName: String, category: String): ExpiryPredictionResult {
        val lower = foodName.lowercase()
        return when {
            "spinach" in lower || "lettuce" in lower || "salad" in lower -> ExpiryPredictionResult(
                shelfLifeDays = 2,
                storageTip = "Wrap loosely in a dry paper towel and place inside a sealed bag to absorb excess humidity.",
                alertThresholdDays = 1,
                isSimulated = true
            )
            "tomato" in lower || "cucumber" in lower -> ExpiryPredictionResult(
                shelfLifeDays = 4,
                storageTip = "Keep outside the fridge at room temperature, away from direct sunlight, stem-side down.",
                alertThresholdDays = 2,
                isSimulated = true
            )
            "milk" in lower || "yogurt" in lower -> ExpiryPredictionResult(
                shelfLifeDays = 5,
                storageTip = "Store in the coldest back shelf of the refrigerator, never in the door compartments.",
                alertThresholdDays = 2,
                isSimulated = true
            )
            else -> ExpiryPredictionResult(
                shelfLifeDays = 6,
                storageTip = "Keep in a cool, dark, dry drawer with proper ventilation.",
                alertThresholdDays = 2,
                isSimulated = true
            )
        }
    }

    private fun getSimulatedRecipes(ingredients: List<String>): List<RecipeSuggestion> {
        val ingList = if (ingredients.isEmpty()) listOf("Tomatoes", "Bread") else ingredients
        return listOf(
            RecipeSuggestion(
                title = "Rustic Zero-Waste Panzenella Salad",
                prepTime = "15 mins",
                difficulty = "Easy",
                ingredients = ingList + listOf("Olive oil", "Garlic", "Fresh basil"),
                instructions = listOf(
                    "Tear stale or expiring bread into bite-sized cubes and toast in a skillet with olive oil and garlic.",
                    "Dice soft tomatoes and toss with the toasted bread cubes and shredded basil.",
                    "Let sit for 10 minutes so bread absorbs the rich tomato juices, then enjoy!"
                ),
                co2PreventedKg = 1.9
            ),
            RecipeSuggestion(
                title = "Savory Eco-Fridge Scramble",
                prepTime = "10 mins",
                difficulty = "Easy",
                ingredients = ingList + listOf("2 Eggs", "Pinch of cheese", "Black pepper"),
                instructions = listOf(
                    "Sauté chopped vegetables (like spinach, tomatoes, or onions) in a warm oiled skillet.",
                    "Whisk eggs with a splash of water, then pour over the vegetables.",
                    "Scramble gently until firm, top with cheese, and serve immediately with toasted bread."
                ),
                co2PreventedKg = 1.2
            )
        )
    }

    private fun getSimulatedWastePrediction(foodName: String): WastePredictionResult {
        val lower = foodName.lowercase()
        return when {
            "salad" in lower || "lettuce" in lower -> WastePredictionResult(
                surplusRisk = "High",
                recommendedStockAdjustment = "Reduce order volume by 25% for upcoming mid-week deliveries",
                predictedWasteKg = 8.5,
                actionableSavingTip = "Veggies have high perishability. Serve in smaller portion sizes with free refills.",
                isSimulated = true
            )
            "beef" in lower || "chicken" in lower || "meat" in lower -> WastePredictionResult(
                surplusRisk = "Medium",
                recommendedStockAdjustment = "Freeze or vacuum seal uncooked cuts 2 days before menu rotation",
                predictedWasteKg = 4.2,
                actionableSavingTip = "Pre-portion and store in blast freezer immediately to extend viability up to 3 months.",
                isSimulated = true
            )
            else -> WastePredictionResult(
                surplusRisk = "Low",
                recommendedStockAdjustment = "Maintain regular par levels; verify stock rotation daily",
                predictedWasteKg = 1.8,
                actionableSavingTip = "Practice strict FIFO (First In, First Out) on shelves.",
                isSimulated = true
            )
        }
    }
}
