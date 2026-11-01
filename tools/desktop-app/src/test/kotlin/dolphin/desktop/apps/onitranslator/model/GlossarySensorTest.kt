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
}
