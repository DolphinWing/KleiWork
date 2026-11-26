package dolphin.desktop.apps.onitranslator.model

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PromptComposerTest {
    private val sampleTerms = listOf(
        GlossaryTerm(
            category = "ELEMENTS",
            english = "Cool Steam Vent",
            canonicalCht = "低溫蒸氣噴孔",
            key = "STRINGS.CREATURES.SPECIES.GEYSER.STEAM.NAME",
        ),
        GlossaryTerm(
            category = "ELEMENTS",
            english = "Steam",
            canonicalCht = "蒸氣",
            key = "STRINGS.ELEMENTS.STEAM.NAME",
        ),
        GlossaryTerm(
            category = "ELEMENTS",
            english = "Oxygen",
            canonicalCht = "氧氣",
            key = "STRINGS.ELEMENTS.OXYGEN.NAME",
        ),
        GlossaryTerm(
            category = "ELEMENTS",
            english = "Polluted Oxygen",
            canonicalCht = "汙氧",
            key = "STRINGS.ELEMENTS.CONTAMINATEDOXYGEN.NAME",
        ),
        GlossaryTerm(
            category = "BUILDINGS.PREFABS",
            english = "Gas Pipe",
            canonicalCht = "氣體管道",
            key = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.NAME",
        ),
        GlossaryTerm(
            category = "UI",
            english = "KKeyword",
            canonicalCht = "不該被標籤屬性匹配",
            key = "STRINGS.UI.FAKE_KKEYWORD.NAME",
        ),
        GlossaryTerm(
            category = "UI",
            english = "style",
            canonicalCht = "不該被標籤名稱匹配",
            key = "STRINGS.UI.FAKE_STYLE.NAME",
        ),
    )

    @Test
    fun testExtractRelevantGlossaryTagFirstWithEscapedQuotes() {
        val source = """Converts <link=\"CONTAMINATEDOXYGEN\">Polluted Oxygen</link> into <link="OXYGEN">Oxygen</link>."""
        val matched = PromptComposer.extractRelevantGlossary(source, sampleTerms)

        assertEquals(
            listOf("Polluted Oxygen", "Oxygen"),
            matched.map { it.english },
        )
    }

    @Test
    fun testExtractRelevantGlossaryLongestMatchAndTagProtection() {
        val source = """A <style="KKeyword">Cool Steam Vent</style> periodically emits hot <link="GASCONDUIT">Gas Pipe</link> pressure."""
        val matched = PromptComposer.extractRelevantGlossary(source, sampleTerms)

        val matchedEnglish = matched.map { it.english }
        assertTrue("Gas Pipe" in matchedEnglish)
        assertTrue("Cool Steam Vent" in matchedEnglish)
        // "Steam" is already covered inside "Cool Steam Vent" span, so it should not match redundantly
        assertFalse("Steam" in matchedEnglish)
        // Tag markup attributes/names must never be matched as glossary terms
        assertFalse("KKeyword" in matchedEnglish)
        assertFalse("style" in matchedEnglish)
    }

    @Test
    fun testExtractRelevantGlossaryDeduplicatesAndRespectsLimit() {
        val source = "Cool Steam Vent outputs Oxygen and Polluted Oxygen through Gas Pipe."
        val matched = PromptComposer.extractRelevantGlossary(source, sampleTerms, maxTerms = 2)

        assertEquals(2, matched.size)
        assertEquals(matched.distinctBy { it.key }.size, matched.size)
    }

    @Test
    fun testFindSiblingEntriesForwardAndReverseOrder() {
        val nameKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.NAME"
        val descKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.DESC"
        val effectKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.EFFECT"
        val nestedChildKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.EXTRA.NOTE"
        val shallowKey = "STRINGS.BUILDINGS.OTHER"

        val entries = listOf(
            PoEntry(nameKey, nameKey, "Gas Pipe", "氣體管道"),
            PoEntry(descKey, descKey, "Carries Gases between intakes and outputs.", "Carries Gases between intakes and outputs."),
            PoEntry(effectKey, effectKey, "Transports <link=\"ELEMENTS_GAS\">Gas</link>.", "Transports <link=\"ELEMENTS_GAS\">Gas</link>."),
            PoEntry(nestedChildKey, nestedChildKey, "Nested note", "Nested note"),
            PoEntry(shallowKey, shallowKey, "Other", "其他"),
        )
        val chsMap = mapOf(
            descKey to "在输入口和输出口之间输送气体。",
            effectKey to "输送<link=\"ELEMENTS_GAS\">气体</link>。",
        )

        // Forward: editing .DESC should pull translated .NAME first, followed by .EFFECT
        val fromDesc = PromptComposer.findSiblingEntries(
            currentKey = descKey,
            allEntries = entries,
            chsLookup = { chsMap[it] },
        )
        assertEquals(listOf("NAME", "EFFECT"), fromDesc.map { it.suffix })
        assertEquals("氣體管道", fromDesc.first().currentTranslation)
        assertEquals("輸送" + "…" , fromDesc[1].chsReference?.let { "輸送…" }) // chsRef preserved for untranslated sibling
        assertEquals("输送<link=\"ELEMENTS_GAS\">气体</link>。", fromDesc[1].chsReference)

        // Reverse: editing .NAME should still pull .DESC and .EFFECT in priority order
        val fromName = PromptComposer.findSiblingEntries(
            currentKey = nameKey,
            allEntries = entries,
            chsLookup = { chsMap[it] },
        )
        assertEquals(listOf("DESC", "EFFECT"), fromName.map { it.suffix })
        assertEquals(null, fromName.first().currentTranslation)
    }

    @Test
    fun testFindSiblingEntriesIgnoresShallowPrefixAndRespectsMax() {
        val entries = listOf(
            PoEntry("STRINGS.UI.SAVE", "STRINGS.UI.SAVE", "Save", "儲存"),
            PoEntry("STRINGS.UI.LOAD", "STRINGS.UI.LOAD", "Load", "載入"),
        )
        // Parent prefix "STRINGS.UI" only has 1 dot -> too broad, must return empty
        assertTrue(PromptComposer.findSiblingEntries("STRINGS.UI.SAVE", entries).isEmpty())

        val deepEntries = (1..6).map { idx ->
            val key = "STRINGS.BUILDINGS.PREFABS.TEST.FIELD_$idx"
            PoEntry(key, key, "Source $idx", "Source $idx")
        }
        val capped = PromptComposer.findSiblingEntries("STRINGS.BUILDINGS.PREFABS.TEST.FIELD_1", deepEntries, maxSiblings = 3)
        assertEquals(3, capped.size)
        assertFalse(capped.any { it.key.endsWith("FIELD_1") })
    }

    @Test
    fun testComposeSectionsTogglesAndWhitespacePreservation() {
        val targetKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.DESC"
        val nameKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.NAME"
        val indentedSource = "    • Transports <link=\"OXYGEN\">Oxygen</link>\\nSafely."
        val entry = EditorData(
            target = PoEntry(
                key = targetKey,
                text = targetKey,
                id = indentedSource,
                str = "    • 輸送氧氣。",
                msgidChanged = true,
            ),
            sourceText = indentedSource,
            oldSourceText = "    • Transports Oxygen.",
            chsReference = "    • 安全地输送<link=\"OXYGEN\">氧气</link>。",
            poText = "    • 輸送氧氣。",
        )
        val allEntries = listOf(
            PoEntry(nameKey, nameKey, "Gas Pipe", "氣體管道"),
            entry.target,
        )
        val recent = listOf(
            RecentTranslationContext(targetKey, indentedSource, "Self should be filtered"),
            RecentTranslationContext("STRINGS.UI.TIP_1", "Keep pipes connected.", "保持管道連通。"),
        )

        val brief = PromptComposer.compose(
            entry = entry,
            systemInstruction = "SYSTEM_PROMPT",
            glossaryTerms = sampleTerms,
            allEntries = allEntries,
            recentTranslations = recent,
            options = PromptContextOptions(includeSiblings = true, includeRecent = true),
        )

        // Exact leading indentation and literal \n preserved
        assertTrue(brief.userPrompt.contains("msgid \"    • Transports <link=\"OXYGEN\">Oxygen</link>\\nSafely.\""))
        assertTrue(brief.userPrompt.contains("## Source Change Diff (msgidChanged)"))
        assertTrue(brief.userPrompt.contains("## Official Simplified Chinese Reference"))
        assertTrue(brief.userPrompt.contains("## Locked Glossary Terms"))
        assertTrue(brief.userPrompt.contains("Oxygen => 氧氣"))
        assertTrue(brief.userPrompt.contains("## Related Sibling Entries"))
        assertTrue(brief.userPrompt.contains("氣體管道"))
        assertTrue(brief.userPrompt.contains("## Recent Session Translations"))
        assertTrue(brief.userPrompt.contains("STRINGS.UI.TIP_1"))
        assertFalse(brief.userPrompt.contains("Self should be filtered"))

        // Toggles off siblings and recent
        val minimal = PromptComposer.compose(
            entry = entry,
            systemInstruction = "SYSTEM_PROMPT",
            glossaryTerms = sampleTerms,
            allEntries = allEntries,
            recentTranslations = recent,
            options = PromptContextOptions(includeSiblings = false, includeRecent = false),
        )
        assertFalse(minimal.userPrompt.contains("## Related Sibling Entries"))
        assertFalse(minimal.userPrompt.contains("## Recent Session Translations"))
        assertTrue(minimal.snapshot.siblings.isEmpty())
        assertTrue(minimal.snapshot.recent.isEmpty())
    }

    @Test
    fun testSystemPromptResourceExistsAndClean() {
        val promptFile = File("src/main/composeResources/files/brigette_system_prompt.md")
        assertTrue(promptFile.exists(), "Expected system prompt file at ${promptFile.absolutePath}")
        val content = promptFile.readText(Charsets.UTF_8)

        assertTrue(content.contains("Brigette Aurora"))
        assertTrue(content.contains("Terminology Lock"))
        assertTrue(content.contains("Tag Restoration & Preservation"))
        assertTrue(content.contains("options"))
        assertFalse(content.contains("drive:read_file_content"))
        assertFalse(content.contains("skills:get_skill_resources"))
        assertFalse(content.contains("references/glossary.txt"))
    }

    @Test
    fun testPrintSampleComposedPromptsForReview() {
        val descKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.DESC"
        val nameKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.NAME"
        val entry = EditorData(
            target = PoEntry(
                key = descKey,
                text = descKey,
                id = "Carries <link=\\\"OXYGEN\\\">Oxygen</link> and other gases between <style=\\\"KKeyword\\\">Gas Pipe</style> vents.\\nDo not overheat.",
                str = "在<style=\\\"KKeyword\\\">氣體管道</style>通風口之間輸送<link=\\\"OXYGEN\\\">氧氣</link>與其他氣體。\\n請勿過熱。",
                msgidChanged = true,
            ),
            sourceText = "Carries <link=\\\"OXYGEN\\\">Oxygen</link> and other gases between <style=\\\"KKeyword\\\">Gas Pipe</style> vents.\\nDo not overheat.",
            oldSourceText = "Carries <link=\\\"OXYGEN\\\">Oxygen</link> between <style=\\\"KKeyword\\\">Gas Pipe</style> vents.",
            chsReference = "在<style=\\\"KKeyword\\\">气体管道</style>通风口之间输送<link=\\\"OXYGEN\\\">氧气</link>和其他气体。\\n请勿过热。",
            poText = "在<style=\\\"KKeyword\\\">氣體管道</style>通風口之間輸送<link=\\\"OXYGEN\\\">氧氣</link>。",
        )
        val allEntries = listOf(
            PoEntry(nameKey, nameKey, "Gas Pipe", "氣體管道"),
            entry.target,
        )
        val brief = PromptComposer.compose(
            entry = entry,
            systemInstruction = File("src/main/composeResources/files/brigette_system_prompt.md").readText(Charsets.UTF_8),
            glossaryTerms = sampleTerms,
            allEntries = allEntries,
        )
        println("=== SAMPLE COMPOSED USER PROMPT ===")
        println(brief.userPrompt)
        println("=== END SAMPLE ===")
        assertTrue(brief.userPrompt.isNotBlank())
    }

    @Test
    fun testFormatHandoffMarkdownIncludesRoundsSelectionAndDraft() {
        val round1Result = GeminiTranslationResult(
            payload = GeminiPayload(
                analysis = "第一輪結構分析。",
                options = listOf(
                    GeminiOption("選項 A", 9, "氣體管道", "符合術語"),
                    GeminiOption("選項 B", 8, "輸送氣管", "偏口語"),
                ),
                recommendedIndex = 0,
                conclusion = "建議選項 A。",
            ),
            usage = GeminiUsage(modelName = "gemini-2.5-flash"),
            rawJson = "{}",
        )
        val round2Result = GeminiTranslationResult(
            payload = GeminiPayload(
                analysis = "依要求再精簡。",
                options = listOf(
                    GeminiOption("選項 A2", 10, "氣管", "更短"),
                ),
                recommendedIndex = 0,
                conclusion = "可改採簡稱。",
            ),
            usage = GeminiUsage(modelName = "gemini-2.5-flash"),
            rawJson = "{}",
        )
        val markdown = PromptComposer.formatHandoffMarkdown(
            entryKey = "STRINGS.BUILDINGS.PREFABS.GASCONDUIT.NAME",
            initialPrompt = "## Target Entry\nmsgid \"Gas Pipe\"",
            rounds = listOf(
                BrigetteRound(userPrompt = "## Target Entry\nmsgid \"Gas Pipe\"", isFollowUp = false, result = round1Result),
                BrigetteRound(userPrompt = "有沒有更精簡的說法", isFollowUp = true, result = round2Result),
            ),
            selectedRoundIndex = 0,
            selectedOptionIndex = 0,
            draftFollowUp = "擔心與玩家習慣衝突",
        )

        assertTrue(markdown.contains("不需再輸出 JSON 格式"))
        assertTrue(markdown.contains("STRINGS.BUILDINGS.PREFABS.GASCONDUIT.NAME"))
        assertTrue(markdown.contains("Round 1（初始分析）"))
        assertTrue(markdown.contains("Round 2（追問）"))
        assertTrue(markdown.contains("有沒有更精簡的說法"))
        assertTrue(markdown.contains("★推薦 [目前暫選] **選項 A**"))
        assertTrue(markdown.contains("**目前暫選譯文**（Round 1 · 選項 A）：`氣體管道`"))
        assertTrue(markdown.contains("**我想接著討論**：擔心與玩家習慣衝突"))
    }
}
