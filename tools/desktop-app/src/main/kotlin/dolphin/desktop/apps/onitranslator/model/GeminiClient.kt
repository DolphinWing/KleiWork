package dolphin.desktop.apps.onitranslator.model

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.CompletionException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Single scored localization candidate returned by Gemini in Brigette format.
 */
data class GeminiOption(
    val label: String = "",
    val score: Int = 0,
    val text: String = "",
    val rationale: String = "",
)

/**
 * Structured JSON payload matching `brigette_system_prompt.md`.
 */
data class GeminiPayload(
    val matchedTerms: List<String> = emptyList(),
    val analysis: String = "",
    val options: List<GeminiOption> = emptyList(),
    val recommendedIndex: Int = 0,
    val conclusion: String = "",
)

/**
 * Token and latency telemetry recorded for a Gemini API invocation.
 */
data class GeminiUsage(
    val promptTokens: Int = 0,
    val candidateTokens: Int = 0,
    val thoughtsTokens: Int = 0,
    val totalTokens: Int = 0,
    val elapsedMs: Long = 0L,
    val modelName: String = "",
)

/**
 * Successful Gemini translation result containing decoded options and usage telemetry.
 */
data class GeminiTranslationResult(
    val payload: GeminiPayload,
    val usage: GeminiUsage,
    val rawJson: String,
)

/**
 * Typed error hierarchy for Gemini API calls.
 */
sealed class GeminiError(
    val message: String,
    val httpStatus: Int? = null,
) {
    class MissingApiKey(message: String = "Gemini API key is not configured.") : GeminiError(message)
    class Unauthorized(message: String, httpStatus: Int) : GeminiError(message, httpStatus)
    class RateLimited(message: String, httpStatus: Int = 429) : GeminiError(message, httpStatus)
    class ModelNotFound(message: String, httpStatus: Int = 404) : GeminiError(message, httpStatus)
    class ServiceUnavailable(message: String, httpStatus: Int = 503) : GeminiError(message, httpStatus)
    class ApiError(message: String, httpStatus: Int) : GeminiError(message, httpStatus)
    class NetworkError(message: String) : GeminiError(message)
    class ParseError(message: String, val rawBody: String = "") : GeminiError(message)
    class Cancelled(message: String = "Request was cancelled.") : GeminiError(message)
}

/**
 * Result wrapper returned by [GeminiClient.generateTranslation].
 */
sealed interface GeminiResponse {
    data class Success(val result: GeminiTranslationResult) : GeminiResponse
    data class Failure(val error: GeminiError) : GeminiResponse
}

/**
 * Lightweight, reflection-free REST client for Google AI Studio (Gemini Developer API).
 *
 * Uses JDK [HttpClient] and compile-time `kotlinx.serialization.json` so release ProGuard
 * obfuscation requires zero reflection keep rules. API keys are sent exclusively via the
 * `x-goog-api-key` HTTP header and redacted from all error outputs.
 */
class GeminiClient(
    private val httpClient: HttpClient = defaultHttpClient(),
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val requestTimeout: Duration = Duration.ofSeconds(60),
) {
    companion object {
        const val DEFAULT_BASE_URL = "https://generativelanguage.googleapis.com/v1beta"
        const val DEFAULT_MODEL = "gemini-2.5-flash"

        private val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
            coerceInputValues = true
        }

        fun defaultHttpClient(): HttpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build()

        private fun sanitize(text: String, apiKey: String): String {
            if (apiKey.isBlank() || text.isBlank()) return text
            return text.replace(apiKey, "[REDACTED]")
        }
    }

    /**
     * Sends [brief] to Gemini `generateContent` with a strict Structured Output JSON schema.
     * Supports structured concurrency cancellation via [suspendCancellableCoroutine].
     */
    suspend fun generateTranslation(
        brief: TranslationBrief,
        apiKey: String,
        model: String = DEFAULT_MODEL,
        temperature: Double = 0.3,
    ): GeminiResponse {
        val cleanKey = apiKey.trim()
        if (cleanKey.isBlank()) {
            return GeminiResponse.Failure(GeminiError.MissingApiKey())
        }

        val cleanModel = model.trim().removePrefix("models/")
        if (cleanModel.isBlank()) {
            return GeminiResponse.Failure(GeminiError.ModelNotFound("Model name cannot be empty.", 404))
        }

        val encodedModel = URLEncoder.encode(cleanModel, StandardCharsets.UTF_8)
        val endpoint = "${baseUrl.trimEnd('/')}/models/$encodedModel:generateContent"
        val requestBody = buildRequestBody(brief, temperature)

        val request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .timeout(requestTimeout)
            .header("Content-Type", "application/json; charset=UTF-8")
            .header("x-goog-api-key", cleanKey)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
            .build()

        val startNs = System.nanoTime()
        return try {
            val response = sendAsyncCancellable(request)
            val elapsedMs = ((System.nanoTime() - startNs) / 1_000_000L).coerceAtLeast(0L)
            parseResponse(
                httpStatus = response.statusCode(),
                responseBody = response.body().orEmpty(),
                elapsedMs = elapsedMs,
                modelName = cleanModel,
                apiKeyToRedact = cleanKey,
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            val unwrapped = (e as? CompletionException)?.cause ?: e
            val rawMsg = unwrapped.message ?: unwrapped::class.simpleName ?: "Network failure"
            GeminiResponse.Failure(GeminiError.NetworkError(sanitize(rawMsg, cleanKey)))
        }
    }

    private suspend fun sendAsyncCancellable(request: HttpRequest): HttpResponse<String> {
        return suspendCancellableCoroutine { cont ->
            val future = httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
            cont.invokeOnCancellation {
                future.cancel(true)
            }
            future.whenComplete { resp, err ->
                if (!cont.isActive) return@whenComplete
                if (err != null) {
                    cont.resumeWithException(err)
                } else {
                    cont.resume(resp)
                }
            }
        }
    }

    internal fun buildRequestBody(brief: TranslationBrief, temperature: Double = 0.3): String {
        val body = buildJsonObject {
            if (brief.systemInstruction.isNotBlank()) {
                put("system_instruction", buildJsonObject {
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", brief.systemInstruction) })
                    })
                })
            }

            put("contents", buildJsonArray {
                add(buildJsonObject {
                    put("role", "user")
                    put("parts", buildJsonArray {
                        add(buildJsonObject { put("text", brief.userPrompt) })
                    })
                })
            })

            put("generationConfig", buildJsonObject {
                put("temperature", temperature)
                put("responseMimeType", "application/json")
                put("responseSchema", buildResponseSchema())
            })
        }
        return body.toString()
    }

    private fun buildResponseSchema(): JsonObject = buildJsonObject {
        put("type", "OBJECT")
        put("properties", buildJsonObject {
            put("matchedTerms", buildJsonObject {
                put("type", "ARRAY")
                put("items", buildJsonObject { put("type", "STRING") })
            })
            put("analysis", buildJsonObject {
                put("type", "STRING")
            })
            put("options", buildJsonObject {
                put("type", "ARRAY")
                put("items", buildJsonObject {
                    put("type", "OBJECT")
                    put("properties", buildJsonObject {
                        put("label", buildJsonObject { put("type", "STRING") })
                        put("score", buildJsonObject { put("type", "INTEGER") })
                        put("text", buildJsonObject { put("type", "STRING") })
                        put("rationale", buildJsonObject { put("type", "STRING") })
                    })
                    put("required", JsonArray(listOf("label", "score", "text", "rationale").map { JsonPrimitive(it) }))
                })
            })
            put("recommendedIndex", buildJsonObject {
                put("type", "INTEGER")
            })
            put("conclusion", buildJsonObject {
                put("type", "STRING")
            })
        })
        put(
            "required",
            JsonArray(listOf("matchedTerms", "analysis", "options", "recommendedIndex", "conclusion").map { JsonPrimitive(it) })
        )
    }

    internal fun parseResponse(
        httpStatus: Int,
        responseBody: String,
        elapsedMs: Long,
        modelName: String,
        apiKeyToRedact: String = "",
    ): GeminiResponse {
        if (httpStatus !in 200..299) {
            return GeminiResponse.Failure(mapHttpError(httpStatus, responseBody, apiKeyToRedact))
        }

        val root = try {
            json.parseToJsonElement(responseBody).jsonObject
        } catch (e: Exception) {
            return GeminiResponse.Failure(
                GeminiError.ParseError(
                    message = sanitize("Invalid JSON envelope from Gemini: ${e.message}", apiKeyToRedact),
                    rawBody = sanitize(responseBody, apiKeyToRedact),
                )
            )
        }

        val usage = extractUsage(root, elapsedMs, modelName)
        val candidateText = extractCandidateText(root)
            ?: return GeminiResponse.Failure(
                GeminiError.ParseError(
                    message = buildMissingCandidateMessage(root),
                    rawBody = sanitize(responseBody, apiKeyToRedact),
                )
            )

        val cleanedJsonText = unwrapCodeFenceIfNeeded(candidateText)
        val payload = try {
            decodePayload(cleanedJsonText)
        } catch (e: Exception) {
            return GeminiResponse.Failure(
                GeminiError.ParseError(
                    message = sanitize("Failed to decode Gemini structured payload: ${e.message}", apiKeyToRedact),
                    rawBody = sanitize(candidateText, apiKeyToRedact),
                )
            )
        }

        if (payload.options.isEmpty()) {
            return GeminiResponse.Failure(
                GeminiError.ParseError(
                    message = "Gemini returned no translation options.",
                    rawBody = sanitize(candidateText, apiKeyToRedact),
                )
            )
        }

        val normalizedPayload = payload.copy(
            recommendedIndex = payload.recommendedIndex.coerceIn(0, payload.options.lastIndex)
        )
        return GeminiResponse.Success(
            GeminiTranslationResult(
                payload = normalizedPayload,
                usage = usage,
                rawJson = cleanedJsonText,
            )
        )
    }

    private fun decodePayload(rawJsonText: String): GeminiPayload {
        val obj = json.parseToJsonElement(rawJsonText).jsonObject
        val matchedTerms = (obj["matchedTerms"] as? JsonArray)
            ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            ?: emptyList()
        val analysis = (obj["analysis"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        val options = (obj["options"] as? JsonArray)?.mapNotNull { elem ->
            val o = elem as? JsonObject ?: return@mapNotNull null
            GeminiOption(
                label = (o["label"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                score = (o["score"] as? JsonPrimitive)?.let { p -> p.intOrNull ?: p.doubleOrNull?.toInt() } ?: 0,
                text = (o["text"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
                rationale = (o["rationale"] as? JsonPrimitive)?.contentOrNull.orEmpty(),
            )
        } ?: emptyList()
        val recommendedIndex = (obj["recommendedIndex"] as? JsonPrimitive)?.intOrNull ?: 0
        val conclusion = (obj["conclusion"] as? JsonPrimitive)?.contentOrNull.orEmpty()
        return GeminiPayload(
            matchedTerms = matchedTerms,
            analysis = analysis,
            options = options,
            recommendedIndex = recommendedIndex,
            conclusion = conclusion,
        )
    }

    private fun extractUsage(root: JsonObject, elapsedMs: Long, modelName: String): GeminiUsage {
        val meta = root["usageMetadata"] as? JsonObject ?: return GeminiUsage(elapsedMs = elapsedMs, modelName = modelName)
        return GeminiUsage(
            promptTokens = meta["promptTokenCount"]?.jsonPrimitive?.intOrNull ?: 0,
            candidateTokens = meta["candidatesTokenCount"]?.jsonPrimitive?.intOrNull ?: 0,
            thoughtsTokens = meta["thoughtsTokenCount"]?.jsonPrimitive?.intOrNull ?: 0,
            totalTokens = meta["totalTokenCount"]?.jsonPrimitive?.intOrNull ?: 0,
            elapsedMs = elapsedMs,
            modelName = modelName,
        )
    }

    private fun extractCandidateText(root: JsonObject): String? {
        val candidates = root["candidates"] as? JsonArray ?: return null
        val firstCandidate = candidates.firstOrNull() as? JsonObject ?: return null
        val content = firstCandidate["content"] as? JsonObject ?: return null
        val parts = content["parts"] as? JsonArray ?: return null

        val answerPart = parts.mapNotNull { it as? JsonObject }
            .firstOrNull { part ->
                val isThought = part["thought"]?.jsonPrimitive?.booleanOrNull == true
                val text = part["text"]?.jsonPrimitive?.content
                !isThought && !text.isNullOrBlank()
            }
            ?: parts.mapNotNull { it as? JsonObject }
                .firstOrNull { !it["text"]?.jsonPrimitive?.content.isNullOrBlank() }

        return answerPart?.get("text")?.jsonPrimitive?.content
    }

    private fun buildMissingCandidateMessage(root: JsonObject): String {
        val blockReason = (root["promptFeedback"] as? JsonObject)
            ?.get("blockReason")?.jsonPrimitive?.content
        val finishReason = ((root["candidates"] as? JsonArray)?.firstOrNull() as? JsonObject)
            ?.get("finishReason")?.jsonPrimitive?.content
        return buildString {
            append("Gemini response contained no candidate text")
            if (!finishReason.isNullOrBlank()) append(" (finishReason=$finishReason)")
            if (!blockReason.isNullOrBlank()) append(" (blockReason=$blockReason)")
            append(".")
        }
    }

    private fun unwrapCodeFenceIfNeeded(text: String): String {
        val trimmed = text.trim()
        if (!trimmed.startsWith("```")) return trimmed
        val lines = trimmed.lines()
        if (lines.size >= 3 && lines.last().trim() == "```") {
            return lines.subList(1, lines.lastIndex).joinToString("\n").trim()
        }
        return trimmed
    }

    private fun mapHttpError(httpStatus: Int, responseBody: String, apiKeyToRedact: String): GeminiError {
        val detail = extractErrorMessage(responseBody) ?: responseBody.take(240).ifBlank { "HTTP $httpStatus" }
        val safeDetail = sanitize(detail, apiKeyToRedact)
        return when (httpStatus) {
            401, 403 -> GeminiError.Unauthorized(safeDetail, httpStatus)
            404 -> GeminiError.ModelNotFound(safeDetail, httpStatus)
            429 -> GeminiError.RateLimited(safeDetail, httpStatus)
            503 -> GeminiError.ServiceUnavailable(safeDetail, httpStatus)
            else -> GeminiError.ApiError(safeDetail, httpStatus)
        }
    }

    private fun extractErrorMessage(responseBody: String): String? {
        if (responseBody.isBlank()) return null
        return try {
            val errObj = json.parseToJsonElement(responseBody).jsonObject["error"]?.jsonObject ?: return null
            val status = errObj["status"]?.jsonPrimitive?.content
            val msg = errObj["message"]?.jsonPrimitive?.content
            when {
                !status.isNullOrBlank() && !msg.isNullOrBlank() -> "$status: $msg"
                !msg.isNullOrBlank() -> msg
                else -> status
            }
        } catch (_: Exception) {
            null
        }
    }
}
