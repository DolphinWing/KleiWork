package dolphin.desktop.apps.onitranslator.model

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assumptions.assumeTrue
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class GeminiClientTest {
    private val client = GeminiClient()

    @Test
    fun testBuildRequestBodyContainsSystemPromptAndSchema() {
        val brief = TranslationBrief(
            systemInstruction = "Brigette system rules",
            userPrompt = "Translate Oxygen",
            snapshot = PromptContextSnapshot(),
        )
        val rawBody = client.buildRequestBody(brief, temperature = 0.25)
        val root = Json.parseToJsonElement(rawBody).jsonObject

        val sysParts = root["system_instruction"]!!.jsonObject["parts"]!!.jsonArray
        assertEquals("Brigette system rules", sysParts[0].jsonObject["text"]!!.jsonPrimitive.content)

        val userParts = root["contents"]!!.jsonArray[0].jsonObject["parts"]!!.jsonArray
        assertEquals("Translate Oxygen", userParts[0].jsonObject["text"]!!.jsonPrimitive.content)

        val genConfig = root["generationConfig"]!!.jsonObject
        assertEquals("application/json", genConfig["responseMimeType"]!!.jsonPrimitive.content)

        val schemaProps = genConfig["responseSchema"]!!.jsonObject["properties"]!!.jsonObject
        assertTrue("matchedTerms" in schemaProps)
        assertTrue("analysis" in schemaProps)
        assertTrue("options" in schemaProps)
        assertTrue("recommendedIndex" in schemaProps)
        assertTrue("conclusion" in schemaProps)
    }

    @Test
    fun testParseSuccessResponseWithUsageAndThoughts() {
        val payloadJson = """
            {
              "matchedTerms": ["Oxygen => 氧氣"],
              "analysis": "元素名詞說明。",
              "options": [
                {"label": "選項 A（鎖定術語）", "score": 10, "text": "提供<link=\"OXYGEN\">氧氣</link>。", "rationale": "嚴格鎖定術語。"},
                {"label": "選項 B（口語）", "score": 8, "text": "供應<link=\"OXYGEN\">氧氣</link>。", "rationale": "語氣較口語。"}
              ],
              "recommendedIndex": 0,
              "conclusion": "建議採用選項 A，術語與標籤皆精準。"
            }
        """.trimIndent()
        val encodedPayload = JsonPrimitive(payloadJson).toString()

        val apiResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "role": "model",
                    "parts": [
                      {"thought": true, "text": "internal thinking text"},
                      {"text": $encodedPayload}
                    ]
                  },
                  "finishReason": "STOP"
                }
              ],
              "usageMetadata": {
                "promptTokenCount": 420,
                "candidatesTokenCount": 135,
                "thoughtsTokenCount": 64,
                "totalTokenCount": 619
              }
            }
        """.trimIndent()

        val response = client.parseResponse(
            httpStatus = 200,
            responseBody = apiResponse,
            elapsedMs = 1234L,
            modelName = "gemini-2.5-flash",
        )

        val success = assertIs<GeminiResponse.Success>(response)
        assertEquals(2, success.result.payload.options.size)
        assertEquals("提供<link=\"OXYGEN\">氧氣</link>。", success.result.payload.options[0].text)
        assertEquals(10, success.result.payload.options[0].score)
        assertEquals(0, success.result.payload.recommendedIndex)
        assertEquals(420, success.result.usage.promptTokens)
        assertEquals(135, success.result.usage.candidateTokens)
        assertEquals(64, success.result.usage.thoughtsTokens)
        assertEquals(619, success.result.usage.totalTokens)
        assertEquals(1234L, success.result.usage.elapsedMs)
        assertEquals("gemini-2.5-flash", success.result.usage.modelName)
    }

    @Test
    fun testParseResponseClampsOutOfRangeRecommendedIndex() {
        val payloadJson = """
            {
              "matchedTerms": [],
              "analysis": "測試邊界。",
              "options": [
                {"label": "A", "score": 9, "text": "甲", "rationale": "理由甲"},
                {"label": "B", "score": 8, "text": "乙", "rationale": "理由乙"}
              ],
              "recommendedIndex": 99,
              "conclusion": "完成。"
            }
        """.trimIndent().replace("\"", "\\\"").replace("\n", "")

        val apiResponse = """
            {"candidates":[{"content":{"parts":[{"text":"$payloadJson"}]}}]}
        """.trimIndent()

        val success = assertIs<GeminiResponse.Success>(
            client.parseResponse(200, apiResponse, 100L, "gemini-2.5-flash")
        )
        assertEquals(1, success.result.payload.recommendedIndex)
    }

    @Test
    fun testParseResponseNormalizesJsonNewlinesToPoLiteral() {
        val apiResponse = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\"matchedTerms\":[],\"analysis\":\"分析段落\\n保留換行。\",\"options\":[{\"label\":\"單換行\",\"score\":10,\"text\":\"第一行\\n第二行\",\"rationale\":\"ok\"},{\"label\":\"雙換行\",\"score\":9,\"text\":\"第一段\\n\\n第二段\",\"rationale\":\"ok\"},{\"label\":\"已雙重跳脫\",\"score\":9,\"text\":\"甲\\\\n\\\\n乙\",\"rationale\":\"ok\"},{\"label\":\"CRLF\",\"score\":8,\"text\":\"上行\\r\\n下行\",\"rationale\":\"ok\"}],\"recommendedIndex\":0,\"conclusion\":\"完成。\"}"
                      }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()

        val success = assertIs<GeminiResponse.Success>(
            client.parseResponse(200, apiResponse, 120L, "gemini-2.5-flash")
        )
        val options = success.result.payload.options
        assertEquals("第一行\\n第二行", options[0].text)
        assertEquals("第一段\\n\\n第二段", options[1].text)
        assertEquals("甲\\n\\n乙", options[2].text)
        assertEquals("上行\\n下行", options[3].text)
        // Analysis prose preserves real newlines for UI readability
        assertEquals("分析段落\n保留換行。", success.result.payload.analysis)
    }

    @Test
    fun testParseErrorResponsesAndApiKeyRedaction() {
        val secretKey = "AIzaSySecretTestKey12345"

        val unauth = assertIs<GeminiResponse.Failure>(
            client.parseResponse(
                401,
                """{"error":{"status":"UNAUTHENTICATED","message":"Invalid key $secretKey"}}""",
                10L,
                "gemini-2.5-flash",
                secretKey,
            )
        )
        val unauthErr = assertIs<GeminiError.Unauthorized>(unauth.error)
        assertFalse(unauthErr.message.contains(secretKey))
        assertTrue(unauthErr.message.contains("[REDACTED]"))

        val notFound = assertIs<GeminiResponse.Failure>(
            client.parseResponse(404, """{"error":{"status":"NOT_FOUND","message":"Unknown model"}}""", 10L, "bad-model")
        )
        assertIs<GeminiError.ModelNotFound>(notFound.error)

        val rateLimited = assertIs<GeminiResponse.Failure>(
            client.parseResponse(429, """{"error":{"status":"RESOURCE_EXHAUSTED","message":"Quota exceeded"}}""", 10L, "gemini-2.5-flash")
        )
        assertIs<GeminiError.RateLimited>(rateLimited.error)

        val unavailable = assertIs<GeminiResponse.Failure>(
            client.parseResponse(503, """{"error":{"status":"UNAVAILABLE","message":"Overloaded"}}""", 10L, "gemini-2.5-flash")
        )
        assertIs<GeminiError.ServiceUnavailable>(unavailable.error)

        val serverErr = assertIs<GeminiResponse.Failure>(
            client.parseResponse(500, "Internal error $secretKey", 10L, "gemini-2.5-flash", secretKey)
        )
        val apiErr = assertIs<GeminiError.ApiError>(serverErr.error)
        assertFalse(apiErr.message.contains(secretKey))
    }

    @Test
    fun testParseMalformedOrEmptyPayloadReturnsParseError() {
        val invalidJson = assertIs<GeminiResponse.Failure>(
            client.parseResponse(200, "not-json", 10L, "gemini-2.5-flash")
        )
        assertIs<GeminiError.ParseError>(invalidJson.error)

        val emptyOptions = assertIs<GeminiResponse.Failure>(
            client.parseResponse(
                200,
                """{"candidates":[{"content":{"parts":[{"text":"{\"matchedTerms\":[],\"analysis\":\"x\",\"options\":[],\"recommendedIndex\":0,\"conclusion\":\"done\"}"}]}}]}""",
                10L,
                "gemini-2.5-flash",
            )
        )
        assertIs<GeminiError.ParseError>(emptyOptions.error)
    }

    @Test
    fun testParseRateLimitedExtractsRetryDelayAndQuotaId() {
        val dailyQuotaBody = """
            {
              "error": {
                "code": 429,
                "message": "You exceeded your current quota.",
                "status": "RESOURCE_EXHAUSTED",
                "details": [
                  {
                    "@type": "type.googleapis.com/google.rpc.QuotaFailure",
                    "violations": [
                      {
                        "quotaMetric": "generativelanguage.googleapis.com/generate_content_free_tier_requests",
                        "quotaId": "GenerateRequestsPerDayPerProjectPerModel-FreeTier"
                      }
                    ]
                  },
                  {
                    "@type": "type.googleapis.com/google.rpc.RetryInfo",
                    "retryDelay": "18.2s"
                  }
                ]
              }
            }
        """.trimIndent()

        val dailyFailure = assertIs<GeminiResponse.Failure>(
            client.parseResponse(429, dailyQuotaBody, 10L, "gemini-flash")
        )
        val dailyErr = assertIs<GeminiError.RateLimited>(dailyFailure.error)
        assertEquals(19, dailyErr.retryDelaySeconds)
        assertEquals("GenerateRequestsPerDayPerProjectPerModel-FreeTier", dailyErr.quotaId)
        assertTrue(dailyErr.isDailyQuota)
        assertFalse(dailyErr.isMinuteQuota)

        val minuteQuotaBody = """
            {
              "error": {
                "status": "RESOURCE_EXHAUSTED",
                "message": "RPM exceeded",
                "details": [
                  {
                    "@type": "type.googleapis.com/google.rpc.QuotaFailure",
                    "violations": [
                      {"quotaId": "GenerateRequestsPerMinutePerProjectPerModel-FreeTier"}
                    ]
                  }
                ]
              }
            }
        """.trimIndent()
        val minuteFailure = assertIs<GeminiResponse.Failure>(
            client.parseResponse(429, minuteQuotaBody, 10L, "gemini-flash", retryAfterHeader = "25")
        )
        val minuteErr = assertIs<GeminiError.RateLimited>(minuteFailure.error)
        assertEquals(25, minuteErr.retryDelaySeconds)
        assertTrue(minuteErr.isMinuteQuota)
        assertFalse(minuteErr.isDailyQuota)
    }

    @Test
    fun testParseModelListResponseFiltersAndSorts() {
        val secretKey = "AIzaSySecretListKey"
        val body = """
            {
              "models": [
                {
                  "name": "models/text-embedding-004",
                  "displayName": "Text Embedding",
                  "supportedGenerationMethods": ["embedContent"]
                },
                {
                  "name": "models/gemma-3-27b-it",
                  "displayName": "Gemma 3 27B",
                  "supportedGenerationMethods": ["generateContent"]
                },
                {
                  "name": "models/gemini-nano-banana-2.1",
                  "displayName": "Nano Banana 2.1",
                  "supportedGenerationMethods": ["generateContent"]
                },
                {
                  "name": "models/gemini-2.5-flash-image",
                  "displayName": "Nano Banana",
                  "supportedGenerationMethods": ["generateContent"]
                },
                {
                  "name": "models/gemini-2.5-flash-preview-tts",
                  "displayName": "Gemini 2.5 Flash Preview TTS",
                  "supportedGenerationMethods": ["generateContent"]
                },
                {
                  "name": "models/gemini-robotics-er-1.5-preview",
                  "displayName": "Gemini Robotics-ER 1.5 Preview",
                  "supportedGenerationMethods": ["generateContent"]
                },
                {
                  "name": "models/gemini-2.5-computer-use-preview",
                  "displayName": "Gemini 2.5 Computer Use Preview",
                  "supportedGenerationMethods": ["generateContent"]
                },
                {
                  "name": "models/gemini-3.5-flash-lite",
                  "displayName": "Gemini 3.5 Flash-Lite",
                  "inputTokenLimit": 1000000,
                  "outputTokenLimit": 8192,
                  "supportedGenerationMethods": ["generateContent", "countTokens"]
                },
                {
                  "name": "models/gemini-3.8-flash",
                  "displayName": "Gemini 3.8 Flash",
                  "inputTokenLimit": 1048576,
                  "outputTokenLimit": 65536,
                  "supportedGenerationMethods": ["generateContent"]
                }
              ]
            }
        """.trimIndent()

        val success = assertIs<GeminiModelListResponse.Success>(
            client.parseModelListResponse(200, body)
        )
        assertEquals(listOf("gemini-3.8-flash", "gemini-3.5-flash-lite"), success.models.map { it.id })
        assertEquals("Gemini 3.8 Flash", success.models.first().displayName)
        assertEquals(1048576, success.models.first().inputTokenLimit)

        val errResp = assertIs<GeminiModelListResponse.Failure>(
            client.parseModelListResponse(403, """{"error":{"message":"Denied $secretKey"}}""", secretKey)
        )
        val unauth = assertIs<GeminiError.Unauthorized>(errResp.error)
        assertFalse(unauth.message.contains(secretKey))
        assertTrue(unauth.message.contains("[REDACTED]"))
    }

    @Test
    fun testMissingApiKeyFailsFastWithoutNetwork(): Unit = runBlocking {
        val brief = TranslationBrief("sys", "user", PromptContextSnapshot())
        val failure = assertIs<GeminiResponse.Failure>(
            client.generateTranslation(brief = brief, apiKey = "   ")
        )
        assertIs<GeminiError.MissingApiKey>(failure.error)
    }

    /**
     * Opt-in Live Smoke Test against Google AI Studio.
     * Skipped automatically unless GEMINI_LIVE_TEST=true and GEMINI_API_KEY is set.
     */
    @Test
    fun testLiveSmokeAgainstAiStudio(): Unit = runBlocking {
        val enabled = System.getenv("GEMINI_LIVE_TEST")?.equals("true", ignoreCase = true) == true
        val apiKey = System.getenv("GEMINI_API_KEY").orEmpty()
        assumeTrue(enabled && apiKey.isNotBlank(), "Skipped: set GEMINI_LIVE_TEST=true and GEMINI_API_KEY to run live smoke test.")

        val model = System.getenv("GEMINI_MODEL")?.takeIf { it.isNotBlank() } ?: GeminiClient.DEFAULT_MODEL
        val systemPrompt = File("src/main/composeResources/files/brigette_system_prompt.md").readText(Charsets.UTF_8)
        val terms = listOf(
            GlossaryTerm("ELEMENTS", "Oxygen", "氧氣", "STRINGS.ELEMENTS.OXYGEN.NAME"),
            GlossaryTerm("BUILDINGS.PREFABS", "Gas Pipe", "氣體管道", "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.NAME"),
        )
        val descKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.DESC"
        val nameKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.NAME"
        val entry = EditorData(
            target = PoEntry(
                key = descKey,
                text = descKey,
                id = "Carries <link=\"OXYGEN\">Oxygen</link> between <style=\"KKeyword\">Gas Pipe</style> intakes and outputs.",
                str = "Carries <link=\"OXYGEN\">Oxygen</link> between <style=\"KKeyword\">Gas Pipe</style> intakes and outputs.",
            ),
            sourceText = "Carries <link=\"OXYGEN\">Oxygen</link> between <style=\"KKeyword\">Gas Pipe</style> intakes and outputs.",
            chsReference = "在<style=\"KKeyword\">气体管道</style>输入口和输出口之间输送<link=\"OXYGEN\">氧气</link>。",
        )
        val brief = PromptComposer.compose(
            entry = entry,
            systemInstruction = systemPrompt,
            glossaryTerms = terms,
            allEntries = listOf(PoEntry(nameKey, nameKey, "Gas Pipe", "氣體管道"), entry.target),
        )

        val response = client.generateTranslation(brief = brief, apiKey = apiKey, model = model)
        val success = assertIs<GeminiResponse.Success>(response)
        assertTrue(success.result.payload.options.isNotEmpty())
        assertTrue(success.result.usage.totalTokens > 0)

        println("=== LIVE SMOKE TEST RESULT ($model) ===")
        println("Elapsed: ${success.result.usage.elapsedMs} ms | Tokens: prompt=${success.result.usage.promptTokens}, out=${success.result.usage.candidateTokens}, thoughts=${success.result.usage.thoughtsTokens}, total=${success.result.usage.totalTokens}")
        println("Analysis: ${success.result.payload.analysis}")
        success.result.payload.options.forEachIndexed { idx, opt ->
            val rec = if (idx == success.result.payload.recommendedIndex) " [RECOMMENDED]" else ""
            println("[$idx] (${opt.score}/10)$rec ${opt.label}: ${opt.text} -- ${opt.rationale}")
        }
        println("Conclusion: ${success.result.payload.conclusion}")
        println("=== END LIVE SMOKE TEST ===")
    }
}
