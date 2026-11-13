package dolphin.desktop.apps.onitranslator.model

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GlossarySensorTest {

    private val sampleTerms = listOf(
        GlossaryTerm("GEYSERS", "Cool Steam Vent", "低溫蒸氣噴孔", "STRINGS.BUILDINGS.PREFABS.COOLSTEAMVENT.NAME"),
        GlossaryTerm("ELEMENTS", "Steam", "蒸氣", "STRINGS.ELEMENTS.STEAM.NAME"),
        GlossaryTerm("ELEMENTS", "Water", "水", "STRINGS.ELEMENTS.WATER.NAME"),
        GlossaryTerm("DUPLICANTS.CHOREGROUPS", "Building", "建造", "STRINGS.DUPLICANTS.CHOREGROUPS.BUILD.NAME"),
        GlossaryTerm("UI.TOOLS", "All", "全部", "STRINGS.UI.TOOLS.FILTERLAYERS.ALL.NAME"),
    ).sortedByDescending { it.english.length }

    @Test
    fun testSingleWordTermsIgnoredInPlainText() {
        // Plain text containing single words like "All", "Building", "Water"
        // Should NOT trigger conflicts because single words have high polysemy / ambiguity
        val source = "All duplicants can enter this building and drink water."
        val target = "所有複製人皆可進入此建築並喝水。"

        val diag = GlossarySensor.diagnose(source, target, sampleTerms)
        assertFalse(diag.hasConflict, "Single words in unlinked plain text must not trigger false conflicts")
        assertEquals(0, diag.conflicts.size)
    }

    @Test
    fun testSingleWordTermsEnforcedInLinkTags() {
        // When explicitly wrapped in an ONI <link> tag, single words MUST be strictly diagnosed
        val source = "Requires <link=\"STRINGS.ELEMENTS.WATER.NAME\">Water</link> to operate."
        val targetWrong = "需要礦物質液體以運作。"

        val diag = GlossarySensor.diagnose(source, targetWrong, sampleTerms)
        assertTrue(diag.hasConflict)
        assertEquals(1, diag.conflicts.size)
        assertEquals("Water", diag.conflicts[0].term.english)
        assertEquals("水", diag.conflicts[0].canonicalTranslation)
    }

    @Test
    fun testMultiWordCompoundTermsEnforcedInPlainText() {
        // Compound terms (2+ words) have high specificity and are safely checked in unlinked plain text
        val source = "A Cool Steam Vent periodically emits boiling liquid."
        val targetWrong = "一個冷蒸氣孔會週期性噴出沸騰液體。"

        val diag = GlossarySensor.diagnose(source, targetWrong, sampleTerms)
        assertTrue(diag.hasConflict)
        assertEquals(1, diag.conflicts.size)
        assertEquals("Cool Steam Vent", diag.conflicts[0].term.english)
    }

    @Test
    fun testLongestMatchFirstAvoidsSubtermClash() {
        val source = "A Cool Steam Vent periodically emits boiling liquid."
        // Translation used a wrong/new term for Cool Steam Vent, but not for Steam
        val targetWrong = "一個冷蒸氣孔會週期性噴出沸騰液體。"

        val diag = GlossarySensor.diagnose(source, targetWrong, sampleTerms)
        assertTrue(diag.hasConflict)
        assertTrue(diag.isUnreviewed)
        // Should only match "Cool Steam Vent", NOT also flag "Steam" as a separate duplicate conflict
        assertEquals(1, diag.conflicts.size)
        assertEquals("Cool Steam Vent", diag.conflicts[0].term.english)
        assertEquals("低溫蒸氣噴孔", diag.conflicts[0].canonicalTranslation)
    }

    @Test
    fun testPassWhenCanonicalTermIsUsed() {
        val source = "A Cool Steam Vent emits Water and Steam."
        val targetCorrect = "一個低溫蒸氣噴孔會噴出水與蒸氣。"

        val diag = GlossarySensor.diagnose(source, targetCorrect, sampleTerms)
        assertFalse(diag.hasConflict)
        assertEquals(0, diag.conflicts.size)
    }

    @Test
    fun testTagFirstMatching() {
        val source = "Harvesting from <link=\"STRINGS.BUILDINGS.PREFABS.COOLSTEAMVENT.NAME\">Cool Steam Vent</link> is vital."
        val targetMissing = "從冷蒸氣孔收集資源至關重要。"

        val diag = GlossarySensor.diagnose(source, targetMissing, sampleTerms)
        assertTrue(diag.hasConflict)
        assertEquals(1, diag.conflicts.size)
        assertEquals("Cool Steam Vent", diag.conflicts[0].term.english)

        // Test one-click apply
        val applied = GlossarySensor.applyCanonical(
            "從<link=\"STRINGS.BUILDINGS.PREFABS.COOLSTEAMVENT.NAME\">冷蒸氣孔</link>收集資源至關重要。",
            diag.conflicts[0]
        )
        assertTrue(applied.contains("低溫蒸氣噴孔"))

        // Test one-click apply with ONI short ID tag: <link="COOLSTEAMVENT">
        val appliedShort = GlossarySensor.applyCanonical(
            "從<link=\"COOLSTEAMVENT\">冷蒸氣孔</link>收集資源至關重要。",
            diag.conflicts[0]
        )
        assertTrue(appliedShort.contains("低溫蒸氣噴孔"))
    }

    @Test
    fun testApplyCanonicalInPlainText() {
        val conflict = GlossaryConflict(
            term = GlossaryTerm("GEYSERS", "Cool Steam Vent", "低溫蒸氣噴孔", "STRINGS.BUILDINGS.PREFABS.COOLSTEAMVENT.NAME"),
            canonicalTranslation = "低溫蒸氣噴孔",
            matchedSourceText = "Cool Steam Vent"
        )

        // 1. Direct English in translation
        val engTarget = "這是一個 Cool Steam Vent。"
        assertEquals("這是一個 低溫蒸氣噴孔。", GlossarySensor.applyCanonical(engTarget, conflict))

        // 2. Fuzzy substring replacement (cold vent -> low temp vent)
        val fuzzyTarget = "這是一個冷蒸氣噴孔。"
        assertEquals("這是一個低溫蒸氣噴孔。", GlossarySensor.applyCanonical(fuzzyTarget, conflict))

        // 3. Character swap replacement (氣 <-> 汽)
        val steamConflict = GlossaryConflict(
            term = GlossaryTerm("ELEMENTS", "Steam", "蒸氣", "STRINGS.ELEMENTS.STEAM.NAME"),
            canonicalTranslation = "蒸氣",
            matchedSourceText = "Steam"
        )
        val swapTarget = "排出大量蒸汽。"
        assertEquals("排出大量蒸氣。", GlossarySensor.applyCanonical(swapTarget, steamConflict))
    }

    @Test
    fun testAcceptedNewTermState() {
        val source = "A Cool Steam Vent is here."
        val targetNew = "這裡有一個冷蒸氣噴孔。"

        val diag = GlossarySensor.diagnose(source, targetNew, sampleTerms, isAccepted = true)
        assertTrue(diag.hasConflict)
        assertFalse(diag.isUnreviewed) // Marked as accepted
        assertTrue(diag.isAccepted)
    }

    @Test
    fun testGlossaryBankTsvParsing() {
        val tempTsv = File.createTempFile("test_glossary", ".tsv").apply {
            writeText(
                """
                Category	English	Chinese	Path
                BUILDINGS	Cool Steam Vent	低溫蒸氣噴孔	STRINGS.BUILDINGS.PREFABS.COOLSTEAMVENT.NAME
                ELEMENTS	Steam	蒸氣	STRINGS.ELEMENTS.STEAM.NAME
                """.trimIndent()
            )
            deleteOnExit()
        }

        val bank = GlossaryBank()
        val loaded = bank.loadFromTsv(tempTsv)
        assertTrue(loaded)
        assertEquals(2, bank.terms.size)
        // Verify terms are sorted descending by length
        assertEquals("Cool Steam Vent", bank.terms[0].english)
        assertEquals("Steam", bank.terms[1].english)
    }

    @Test
    fun testApplyCanonicalSafelyLeavesUnmatchedTextIntact() {
        val conflict = GlossaryConflict(
            term = GlossaryTerm("GEYSERS", "Cool Steam Vent", "低溫蒸氣噴孔", "STRINGS.BUILDINGS.PREFABS.COOLSTEAMVENT.NAME"),
            canonicalTranslation = "低溫蒸氣噴孔",
            matchedSourceText = "Cool Steam Vent"
        )
        // Paraphrased text with no shared noun characters
        val unmatchedTarget = "這是一個完全意譯的熱泉噴湧設備。"
        val result = GlossarySensor.applyCanonical(unmatchedTarget, conflict)
        // Must safely return original string untouched without corrupting characters
        assertEquals(unmatchedTarget, result)
    }

    @Test
    fun testApplyCanonicalDoesNotExpandGenericNounWithoutPrefix() {
        val conflict = GlossaryConflict(
            term = GlossaryTerm("BUILDINGS", "Natural Gas Generator", "天然氣發電機", "STRINGS.BUILDINGS.PREFABS.GENERATOR.NAME"),
            canonicalTranslation = "天然氣發電機",
            matchedSourceText = "Natural Gas Generator"
        )
        // Target text has only generic base noun "發電機" with no prefix characters
        val genericTarget = "這是一個發電機。"
        val result = GlossarySensor.applyCanonical(genericTarget, conflict)
        // Must NOT falsely prepend "天然氣" when prefix was not present
        assertEquals(genericTarget, result)
    }

    @Test
    fun testFallbackFromPoEntries() {
        val bank = GlossaryBank()
        val entries = listOf(
            PoEntry(key = "STRINGS.BUILDINGS.PREFABS.COOLSTEAMVENT.NAME", text = "Cool Steam Vent", id = "Cool Steam Vent", str = "低溫蒸氣噴孔"),
            PoEntry(key = "STRINGS.ELEMENTS.STEAM.NAME", text = "Steam", id = "Steam", str = "蒸氣"),
            PoEntry(key = "STRINGS.UI.FRONTEND.BUTTON.NAME", text = "Play", id = "Play", str = "開始遊戲"),
            PoEntry(key = "STRINGS.UI.TOOLS.FILTERLAYERS.ALL.NAME", text = "All", id = "All", str = "全部"),
            PoEntry(key = "STRINGS.DUPLICANTS.CHOREGROUPS.BUILD.NAME", text = "Building", id = "Building", str = "建造"),
        )

        bank.fallbackFromPoEntries(entries)
        // Only the whitelisted BUILDINGS.PREFABS and ELEMENTS should be extracted
        assertEquals(2, bank.terms.size)
        assertEquals("Cool Steam Vent", bank.terms[0].english)
        assertEquals("Steam", bank.terms[1].english)
    }

    @Test
    fun testDiagnosePerformanceWithLargeEntrySet() {
        val largeTerms = (1..1500).map { i ->
            GlossaryTerm("CATEGORY", "Term Number $i", "標號 $i", "STRINGS.TERM_$i.NAME")
        }
        val source = "This text references Term Number 42 in plain text."
        val target = "這段文字提到錯誤的翻譯。"

        val start = System.currentTimeMillis()
        var conflictCount = 0
        repeat(5000) {
            val diag = GlossarySensor.diagnose(source, target, largeTerms)
            if (diag.hasConflict) conflictCount++
        }
        val elapsed = System.currentTimeMillis() - start
        assertEquals(5000, conflictCount)
        // 5000 diagnostics against 1500 terms must easily finish in under 2000ms
        assertTrue(elapsed < 2000, "5000 diagnostics should complete in < 2000ms, took ${elapsed}ms")
    }

    @Test
    fun testMultipleOccurrencesCandidateSpansAndRtlBatchReplacement() {
        val gasTerm = GlossaryTerm("BUILDINGS", "Natural Gas Generator", "天然氣發電機", "STRINGS.BUILDINGS.PREFABS.GENERATOR.NAME")
        val conflict = GlossaryConflict(
            term = gasTerm,
            canonicalTranslation = "天然氣發電機",
            matchedSourceText = "Natural Gas Generator"
        )
        val target = "這是一台瓦斯發電機。請保持瓦斯發電機低溫。"

        val spans = GlossarySensor.findCandidateSpans(target, conflict)
        assertEquals(2, spans.size, "Should locate both occurrences of 瓦斯發電機")
        assertEquals(4..8, spans[0].range)
        assertEquals("瓦斯發電機", spans[0].text)
        assertEquals("天然氣發電機", spans[0].replacement)

        assertEquals(13..17, spans[1].range)
        assertEquals("瓦斯發電機", spans[1].text)
        assertEquals("天然氣發電機", spans[1].replacement)

        // Test single replacement
        val singleResult = GlossarySensor.applySingleReplacement(target, spans[0])
        assertEquals("這是一台天然氣發電機。請保持瓦斯發電機低溫。", singleResult)

        // Test RTL batch replacement (both replaced at once with zero index drift)
        val batchResult = GlossarySensor.applyBatchReplacement(target, spans)
        assertEquals("這是一台天然氣發電機。請保持天然氣發電機低溫。", batchResult)
    }

    @Test
    fun testDiagnoseKeepsConflictWhenOneOccurrenceIsFixedAndOneRemains() {
        val gasTerm = GlossaryTerm("BUILDINGS", "Natural Gas Generator", "天然氣發電機", "STRINGS.BUILDINGS.PREFABS.GENERATOR.NAME")
        val terms = listOf(gasTerm)
        val source = "A Natural Gas Generator burns Gas. Keep the Natural Gas Generator cool."
        // One is fixed to canonical, but one still uses obsolete "瓦斯發電機"
        val targetPartial = "這是一台天然氣發電機。請保持瓦斯發電機低溫。"

        val diag = GlossarySensor.diagnose(source, targetPartial, terms)
        assertTrue(diag.hasConflict, "Conflict banner should NOT disappear if another non-canonical occurrence remains")
        assertEquals(1, diag.conflicts.size)

        // When all are fixed:
        val targetAllFixed = "這是一台天然氣發電機。請保持天然氣發電機低溫。"
        val diagFixed = GlossarySensor.diagnose(source, targetAllFixed, terms)
        assertFalse(diagFixed.hasConflict, "Conflict should clear when all occurrences are canonical")
    }

    @Test
    fun testMultipleLinkTagsCandidateSpans() {
        val powerTerm = GlossaryTerm("POWER", "Power", "電網", "STRINGS.UI.POWER.NAME")
        val conflict = GlossaryConflict(
            term = powerTerm,
            canonicalTranslation = "電網",
            matchedSourceText = "Power",
            linkTarget = "POWER"
        )
        val target = "需要 <link=\"POWER\">電力</link> 和 <link=\"POWER\">電力</link> 供應。"

        val spans = GlossarySensor.findCandidateSpans(target, conflict)
        assertEquals(2, spans.size)
        val replaced = GlossarySensor.applyBatchReplacement(target, spans)
        assertEquals("需要 <link=\"POWER\">電網</link> 和 <link=\"POWER\">電網</link> 供應。", replaced)
    }

    @Test
    fun testLinguisticRulesLoadingAndVariants() {
        assertTrue(LinguisticRules.characterVariants.isNotEmpty(), "Character variants should be loaded")
        assertTrue(LinguisticRules.boundaryChars.isNotEmpty(), "Boundary delimiters should be loaded")
        assertTrue(LinguisticRules.isDelim('，'), "Fullwidth comma must be recognized as delimiter")
        assertTrue(LinguisticRules.isDelim('保'), "Grammar particle 保 must be recognized as boundary")
        assertTrue(LinguisticRules.isDelim('<'), "< must be recognized as delimiter")
        assertTrue(LinguisticRules.isDelim('>'), "> must be recognized as delimiter")

        val variants = LinguisticRules.getVariants("蒸氣噴孔")
        assertTrue(variants.contains("蒸氣噴孔"))
        assertTrue(variants.contains("蒸汽噴孔"))
    }

    @Test
    fun testFuzzyExpansionNeverSwallowsTagDelimiters() {
        // Jet Suit case: canonical is 噴射飛行衣 (5 chars), but target text has <link="JET_SUIT">飛行衣</link>
        // Substring expansion must NOT swallow the '<' of '</link>' or '>' of '<link>'
        val jetSuitTerm = GlossaryTerm("EQUIPMENT", "Jet Suit", "噴射飛行衣", "STRINGS.EQUIPMENT.PREFABS.JET_SUIT.NAME")
        val conflict = GlossaryConflict(
            term = jetSuitTerm,
            canonicalTranslation = "噴射飛行衣",
            matchedSourceText = "Jet Suits",
            linkTarget = "OTHER_TAG", // non-matching linkTarget so it falls through to fuzzy matching
        )
        val target = "穿上你的<link=\"OTHER_TAG\">飛行衣</link>準備出發。"

        val spans = GlossarySensor.findCandidateSpans(target, conflict)
        for (span in spans) {
            assertFalse(span.text.contains('<'), "Candidate span text '${span.text}' must never swallow '<'")
            assertFalse(span.text.contains('>'), "Candidate span text '${span.text}' must never swallow '>'")
        }
    }

    @Test
    fun testSharedPrefixTermsDoNotConflictWhenBothAreCanonical() {
        val railTerm = GlossaryTerm("BUILDINGS", "Conveyor Rail", "輸送帶軌道", "STRINGS.BUILDINGS.PREFABS.SOLIDCONDUIT.NAME")
        val receptacleTerm = GlossaryTerm("BUILDINGS", "Conveyor Receptacle", "輸送帶存儲箱", "STRINGS.BUILDINGS.PREFABS.SOLIDCONDUITOUTBOX.NAME")
        val terms = listOf(receptacleTerm, railTerm)

        val source = "<style=\"KKeyword\">Conveyor Rail</style> cannot hold more materials\n\nRemove materials from <style=\"KKeyword\">Conveyor Receptacle</style> to make room"
        val target = "<style=\"KKeyword\">輸送帶軌道</style>無法再承載更多材料\n\n從<style=\"KKeyword\">輸送帶存儲箱</style>移除材料，以騰出更多空間"

        val diag = GlossarySensor.diagnose(source, target, terms)
        assertFalse(diag.hasConflict, "Satisfaction count must recognize both terms as fully satisfied and produce NO conflict")
        assertEquals(0, diag.conflicts.size)

        val spans = GlossarySensor.findAllCandidateSpans(target, diag.conflicts)
        assertTrue(spans.isEmpty(), "No candidate spans should be produced when all terms are canonical")
    }

    @Test
    fun testKnownCanonicalProtectionPreventsFuzzyCannibalization() {
        val railTerm = GlossaryTerm("BUILDINGS", "Conveyor Rail", "輸送帶軌道", "STRINGS.BUILDINGS.PREFABS.SOLIDCONDUIT.NAME")
        val receptacleTerm = GlossaryTerm("BUILDINGS", "Conveyor Receptacle", "輸送帶存儲箱", "STRINGS.BUILDINGS.PREFABS.SOLIDCONDUITOUTBOX.NAME")
        val terms = listOf(receptacleTerm, railTerm)

        // Conveyor Rail is correctly translated to 輸送帶軌道, but Conveyor Receptacle is obsolete 輸送箱
        val source = "<style=\"KKeyword\">Conveyor Rail</style> cannot hold more materials\n\nRemove materials from <style=\"KKeyword\">Conveyor Receptacle</style> to make room"
        val target = "<style=\"KKeyword\">輸送帶軌道</style>無法再承載更多材料\n\n從<style=\"KKeyword\">輸送箱</style>移除材料，以騰出更多空間"

        val diag = GlossarySensor.diagnose(source, target, terms)
        assertTrue(diag.hasConflict, "Must diagnose conflict for the missing Conveyor Receptacle")
        assertEquals(1, diag.conflicts.size)
        assertEquals("Conveyor Receptacle", diag.conflicts[0].term.english)

        // Ensure findCandidateSpans for Conveyor Receptacle does NOT eat the already-canonical 輸送帶軌道
        val knownCanonicals = terms.map { it.canonicalCht }.toSet()
        val spans = GlossarySensor.findCandidateSpans(target, diag.conflicts[0], knownCanonicals = knownCanonicals)
        for (span in spans) {
            assertFalse(
                span.text.contains("輸送帶軌道"),
                "Candidate span for Conveyor Receptacle must never cannibalize the valid canonical term '輸送帶軌道'"
            )
        }
    }
}
