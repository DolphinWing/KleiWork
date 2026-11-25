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
