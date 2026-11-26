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
        assertFalse(state.isPromptEdited)
    }

    @Test
    fun testBrigetteDeskStateTracksEditablePromptDiff() {
        val initial = BrigetteDeskState(
            visible = true,
            builtPromptPreview = "Original prompt",
            editablePrompt = "Original prompt",
        )
        assertFalse(initial.isPromptEdited)

        val edited = initial.copy(editablePrompt = "Original prompt\nExtra note")
        assertTrue(edited.isPromptEdited)

        val reset = edited.copy(editablePrompt = edited.builtPromptPreview)
        assertFalse(reset.isPromptEdited)
    }

    @Test
    fun testBrigetteDeskStateMultiRoundSelectionAndLatestResult() {
        val r1 = GeminiTranslationResult(
            payload = GeminiPayload(
                options = listOf(
                    GeminiOption("A", 9, "譯文一", "r1"),
                    GeminiOption("B", 8, "譯文二", "r1b"),
                ),
                recommendedIndex = 0,
            ),
            usage = GeminiUsage(),
            rawJson = "{\"r\":1}",
        )
        val r2 = GeminiTranslationResult(
            payload = GeminiPayload(
                options = listOf(
                    GeminiOption("A2", 10, "修訂譯文", "r2"),
                ),
                recommendedIndex = 0,
            ),
            usage = GeminiUsage(),
            rawJson = "{\"r\":2}",
        )

        val state = BrigetteDeskState(
            visible = true,
            rounds = listOf(
                BrigetteRound("initial", isFollowUp = false, result = r1),
                BrigetteRound("shorter please", isFollowUp = true, result = r2),
            ),
            selectedRoundIndex = 1,
            selectedOptionIndex = 0,
        )
        assertEquals(r2, state.result)
        assertEquals("修訂譯文", state.selectedOption?.text)

        val fallbackToRound1 = state.copy(selectedRoundIndex = 0, selectedOptionIndex = 1)
        assertEquals("譯文二", fallbackToRound1.selectedOption?.text)
    }
}
