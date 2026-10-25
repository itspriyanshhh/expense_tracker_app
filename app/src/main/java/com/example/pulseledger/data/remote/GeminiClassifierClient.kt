package com.example.pulseledger.data.remote

import com.example.pulseledger.data.security.KeyStorageProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.Calendar
import java.util.concurrent.TimeUnit

@Serializable
data class GeminiClassificationResult(
    val category: String,
    val cleanMerchantName: String,
    val isP2pPersonName: Boolean,
    val confidence: Double
)

@Serializable
data class ScrubbedPayload(
    val rawMerchant: String,
    val upiVpaPrefix: String?,
    val hourOfDay: Int,
    val amount: Double
)

/**
 * Tier 3 Gemini REST Structured JSON Classifier.
 *
 * Privacy Guardrail (Strict):
 * NEVER transmits raw SMS bodies, bank account suffixes (A/c XX1234), or closing balances.
 * Strictly transmits scrubbed primitives: { rawMerchant, upiVpaPrefix, hourOfDay, amount }.
 */
open class GeminiClassifierClient(
    private val keyStorage: KeyStorageProvider,
    private val modelId: String = DEFAULT_MODEL_ID,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(8, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        const val DEFAULT_MODEL_ID = "gemini-flash-lite-latest"
        val FALLBACK_MODELS = listOf(
            "gemini-flash-lite-latest",
            "gemini-3.1-flash-lite",
            "gemini-3.5-flash-lite"
        )
        private const val TAG = "GeminiClassifier"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Classifies a scrubbed transaction.
     * Returns null if API key is not configured or network call fails.
     */
    open suspend fun classify(
        rawMerchant: String,
        upiVpa: String?,
        amount: Double,
        timestamp: Long = System.currentTimeMillis()
    ): GeminiClassificationResult? = withContext(Dispatchers.IO) {
        val apiKey = keyStorage.getGeminiApiKey()
        if (apiKey.isNullOrBlank()) {
            android.util.Log.d(TAG, "No Gemini API key available in KeyStorage/BuildConfig")
            return@withContext null
        }
        android.util.Log.d(TAG, "Using API key prefix: ${apiKey.take(10)}... (length=${apiKey.length})")

        // Calculate hour of day from timestamp
        val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)
        val vpaPrefix = upiVpa?.substringBefore('@')?.takeIf { it.isNotBlank() }

        // Scrubbed payload: strictly privacy compliant
        val scrubbed = ScrubbedPayload(
            rawMerchant = rawMerchant.trim(),
            upiVpaPrefix = vpaPrefix,
            hourOfDay = hourOfDay,
            amount = amount
        )

        val requestBodyJson = buildRequestBody(scrubbed)

        val modelsToTry = if (modelId == DEFAULT_MODEL_ID) {
            FALLBACK_MODELS
        } else {
            listOf(modelId) + FALLBACK_MODELS.filter { it != modelId }
        }

        for (candidateModel in modelsToTry) {
            val url = "https://generativelanguage.googleapis.com/v1beta/models/$candidateModel:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .post(requestBodyJson.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .build()

            try {
                httpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        val errorSnippet = response.body?.string()?.take(200) ?: "no body"
                        android.util.Log.w(TAG, "Model $candidateModel returned HTTP ${response.code}: $errorSnippet")
                        if (response.code == 404) {
                            return@use // try next fallback model
                        }
                        return@withContext null
                    }

                    val responseBody = response.body?.string() ?: return@withContext null
                    val result = parseGeminiStructuredResponse(responseBody)
                    if (result != null) {
                        android.util.Log.d(TAG, "Classified '$rawMerchant' via $candidateModel into ${result.category}")
                        return@withContext result
                    }
                }
            } catch (e: IOException) {
                android.util.Log.w(TAG, "Network error on model $candidateModel: ${e.message}")
            } catch (e: Exception) {
                android.util.Log.w(TAG, "Classification error on model $candidateModel: ${e.message}")
            }
        }

        null
    }

    private fun buildRequestBody(payload: ScrubbedPayload): String {
        val promptText = """
            You are a privacy-first Indian fintech transaction classifier.
            Given this scrubbed transaction data:
            - Raw Merchant: "${payload.rawMerchant}"
            - UPI VPA Prefix: "${payload.upiVpaPrefix ?: "N/A"}"
            - Amount: INR ${payload.amount}
            - Hour of Day: ${payload.hourOfDay}

            STRICT P2P RULE:
            If rawMerchant looks like an individual person's human name (e.g., "Suresh Yadav", "Mohd Irfan", "Priya Sharma", "Ramesh Kumar") without any commercial suffixes ("Store", "Medicos", "Enterprises", "Foods", "Mart", "Auto", "Traders", "Cafe", "Services"), you MUST set:
            "isP2pPersonName": true,
            "category": "P2P_Unknown",
            "cleanMerchantName": "${payload.rawMerchant}",
            "confidence": 1.0

            Otherwise, classify into one of the allowed categories:
            Food & Dining, Groceries, Transit & Travel, Shopping, Bills & Utilities, Entertainment, Medical, Education, Investments, Home & Rent, Personal Care, Insurance.
            Provide canonical cleanMerchantName, set isP2pPersonName to false, and assign a confidence between 0.0 and 1.0.
        """.trimIndent()

        val jsonObject = buildJsonObject {
            putJsonArray("contents") {
                add(buildJsonObject {
                    putJsonArray("parts") {
                        add(buildJsonObject {
                            put("text", promptText)
                        })
                    }
                })
            }
            putJsonObject("generationConfig") {
                put("responseMimeType", "application/json")
                putJsonObject("responseSchema") {
                    put("type", "OBJECT")
                    putJsonObject("properties") {
                        putJsonObject("category") {
                            put("type", "STRING")
                            putJsonArray("enum") {
                                add(kotlinx.serialization.json.JsonPrimitive("Food & Dining"))
                                add(kotlinx.serialization.json.JsonPrimitive("Groceries"))
                                add(kotlinx.serialization.json.JsonPrimitive("Transit & Travel"))
                                add(kotlinx.serialization.json.JsonPrimitive("Shopping"))
                                add(kotlinx.serialization.json.JsonPrimitive("Bills & Utilities"))
                                add(kotlinx.serialization.json.JsonPrimitive("Entertainment"))
                                add(kotlinx.serialization.json.JsonPrimitive("Medical"))
                                add(kotlinx.serialization.json.JsonPrimitive("Education"))
                                add(kotlinx.serialization.json.JsonPrimitive("Investments"))
                                add(kotlinx.serialization.json.JsonPrimitive("Home & Rent"))
                                add(kotlinx.serialization.json.JsonPrimitive("Personal Care"))
                                add(kotlinx.serialization.json.JsonPrimitive("Insurance"))
                                add(kotlinx.serialization.json.JsonPrimitive("P2P_Unknown"))
                            }
                        }
                        putJsonObject("cleanMerchantName") {
                            put("type", "STRING")
                        }
                        putJsonObject("isP2pPersonName") {
                            put("type", "BOOLEAN")
                        }
                        putJsonObject("confidence") {
                            put("type", "NUMBER")
                        }
                    }
                    putJsonArray("required") {
                        add(kotlinx.serialization.json.JsonPrimitive("category"))
                        add(kotlinx.serialization.json.JsonPrimitive("cleanMerchantName"))
                        add(kotlinx.serialization.json.JsonPrimitive("isP2pPersonName"))
                        add(kotlinx.serialization.json.JsonPrimitive("confidence"))
                    }
                }
            }
        }

        return jsonObject.toString()
    }

    internal fun parseGeminiStructuredResponse(responseBody: String): GeminiClassificationResult? {
        return try {
            val root = json.parseToJsonElement(responseBody)
            val candidates = root.asJsonObjectOrNull()?.get("candidates")?.asJsonArrayOrNull()
            val firstCandidate = candidates?.firstOrNull()?.asJsonObjectOrNull()
            val parts = firstCandidate?.get("content")?.asJsonObjectOrNull()?.get("parts")?.asJsonArrayOrNull()
            val textJson = parts?.firstOrNull()?.asJsonObjectOrNull()?.get("text")?.asJsonPrimitiveOrNull()?.content
                ?: return null

            json.decodeFromString<GeminiClassificationResult>(textJson)
        } catch (e: Exception) {
            null
        }
    }

    private fun kotlinx.serialization.json.JsonElement.asJsonObjectOrNull() =
        this as? kotlinx.serialization.json.JsonObject

    private fun kotlinx.serialization.json.JsonElement.asJsonArrayOrNull() =
        this as? kotlinx.serialization.json.JsonArray

    private fun kotlinx.serialization.json.JsonElement.asJsonPrimitiveOrNull() =
        this as? kotlinx.serialization.json.JsonPrimitive
}
