package dolphin.desktop.apps.onitranslator.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BrigetteConfigAndStatsTest {
    @Test
    fun testEffectiveGeminiApiKeyPrefersConfigThenEnv() {
        val fromConfig = Configs(geminiApiKey = "  config-key  ")
        assertEquals("config-key", fromConfig.effectiveGeminiApiKey { "env-key" })

        val fromEnv = Configs(geminiApiKey = "   ")
        assertEquals("env-key", fromEnv.effectiveGeminiApiKey { "  env-key  " })

        val missing = Configs(geminiApiKey = "")
        assertEquals("", missing.effectiveGeminiApiKey { null })
    }

    @Test
    fun testGeminiSessionStatsAccumulatesUsage() {
        var stats = GeminiSessionStats()
        stats = stats.record(
            GeminiUsage(
                promptTokens = 400,
                candidateTokens = 120,
                thoughtsTokens = 50,
                totalTokens = 570,
                elapsedMs = 950L,
                modelName = "gemini-3.6-flash",
            )
        )
        stats = stats.record(
            GeminiUsage(
                promptTokens = 600,
                candidateTokens = 180,
                thoughtsTokens = 0,
                totalTokens = 780,
                elapsedMs = 1100L,
                modelName = "gemini-3.6-flash",
            )
        )

        assertEquals(2, stats.requestCount)
        assertEquals(1000, stats.totalPromptTokens)
        assertEquals(300, stats.totalCandidateTokens)
        assertEquals(50, stats.totalThoughtsTokens)
        assertEquals(1350, stats.totalTokens)
        assertEquals(2050L, stats.totalElapsedMs)
    }

    @Test
    fun testBrigetteDeskStateDefaultDynamicContextOptions() {
        val state = BrigetteDeskState()
        assertFalse(state.visible)
        assertTrue(state.contextOptions.includeSiblings)
        assertFalse(state.contextOptions.includeRecent)
    }
}
