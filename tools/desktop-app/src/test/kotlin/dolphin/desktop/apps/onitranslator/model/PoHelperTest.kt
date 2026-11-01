package dolphin.desktop.apps.onitranslator.model

import java.io.BufferedReader
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PoHelperTest {
    private val mockConfigs = Configs(
        oniAssetsDir = "",
        oniWorkshopDir = "",
        dataBankPath = ""
    )
    private val mockRefinery = TextRefinery(emptyMap())
    private val helper = PoHelper(mockConfigs, mockRefinery)

    @Test
    fun testEscapeQuotes() {
        val original = """He said "Hello" and <link="METALORE">Metal</link>."""
        val escaped = helper.escapePoQuote(original)
        assertEquals("""He said \"Hello\" and <link=\"METALORE\">Metal</link>.""", escaped)

        // Test mixed: should not double-escape already escaped backslashes
        val mixed = """He said \"Hello\" and <link="METALORE">Metal</link>."""
        val escapedMixed = helper.escapePoQuote(mixed)
        assertEquals("""He said \"Hello\" and <link=\"METALORE\">Metal</link>.""", escapedMixed)
    }

    @Test
    fun testSanitizeCorruptedQuotes() {
        // Corrupted sequence with double backslashes \\" should be sanitized to single backslash \"
        val corrupted = """He said \\"Hello\\" and <link=\\"METALORE\\">Metal</link>."""
        val sanitized = helper.sanitizeLoadedQuote(corrupted)
        assertEquals("""He said \"Hello\" and <link=\"METALORE\">Metal</link>.""", sanitized)
    }

    @Test
    fun testParseMultiLinePoFile() {
        val poContent = """
            #. STRINGS.BUILDINGS.PREFABS.BASE_OUTHOUSE.NAME
            msgctxt "STRINGS.BUILDINGS.PREFABS.BASE_OUTHOUSE.NAME"
            msgid ""
            "(Zn) Zinc Ore is a malleable raw <link=\"METALORE\">Metal</link>.\n"
            "\n"
            "It is suitable for building <link=\"POWER\">Power</link> systems."
            msgstr ""
            "(Zn) 锌矿是一种易塑的<link=\"METALORE\">金属</link>原料。\n"
            "\n"
            "它可用于建造<link=\"POWER\">电力</link>系统。"
        """.trimIndent()

        val reader = BufferedReader(StringReader(poContent))
        val entries = helper.parsePoFile(reader)

        assertEquals(1, entries.size)
        val entry = entries[0]

        assertEquals("STRINGS.BUILDINGS.PREFABS.BASE_OUTHOUSE.NAME", entry.key)
        assertEquals("STRINGS.BUILDINGS.PREFABS.BASE_OUTHOUSE.NAME", entry.text)
        
        // Verify msgid: should not have extra quotes, and \n should remain as \n literal character (\\n)
        val expectedId = "(Zn) Zinc Ore is a malleable raw <link=\\\"METALORE\\\">Metal</link>.\\n\\nIt is suitable for building <link=\\\"POWER\\\">Power</link> systems."
        assertEquals(expectedId, entry.id)
        
        // Verify msgstr: should not have extra quotes, and \n should remain as \n literal character (\\n)
        val expectedStr = "(Zn) 锌矿是一种易塑的<link=\\\"METALORE\\\">金属</link>原料。\\n\\n它可用于建造<link=\\\"POWER\\\">电力</link>系统。"
        assertEquals(expectedStr, entry.str)
    }

    @Test
    fun testParseSingleLineWithQuotes() {
        val poContent = """
            #. STRINGS.UI.FRONTEND.TITLE
            msgctxt "STRINGS.UI.FRONTEND.TITLE"
            msgid "Oxygen Not Included"
            msgstr "缺氧"
        """.trimIndent()

        val reader = BufferedReader(StringReader(poContent))
        val entries = helper.parsePoFile(reader)

        assertEquals(1, entries.size)
        val entry = entries[0]
        assertEquals("STRINGS.UI.FRONTEND.TITLE", entry.key)
        assertEquals("STRINGS.UI.FRONTEND.TITLE", entry.text)
        assertEquals("Oxygen Not Included", entry.id)
        assertEquals("缺氧", entry.str)
    }

    @Test
    fun testParseWithIndentationSpaces() {
        val poContent = """
            #. STRINGS.UI.FRONTEND.HELP
            msgctxt "STRINGS.UI.FRONTEND.HELP"
            msgid "    • Help English"
            msgstr "    • 幫助繁體"
        """.trimIndent()

        val reader = BufferedReader(StringReader(poContent))
        val entries = helper.parsePoFile(reader)

        assertEquals(1, entries.size)
        val entry = entries[0]
        assertEquals("    • Help English", entry.id)
        assertEquals("    • 幫助繁體", entry.str)
    }

    @Test
    fun testParseSingleSpace() {
        val poContent = """
            #. STRINGS.UI.FRONTEND.SPACE
            msgctxt "STRINGS.UI.FRONTEND.SPACE"
            msgid " "
            msgstr " "
        """.trimIndent()

        val reader = BufferedReader(StringReader(poContent))
        val entries = helper.parsePoFile(reader)

        assertEquals(1, entries.size)
        val entry = entries[0]
        assertEquals(" ", entry.id)
        assertEquals(" ", entry.str)
    }

    @Test
    fun testGlossaryBankFiltersFacadesAndStatusItems() {
        val bank = GlossaryBank()
        val tempTsv = java.io.File.createTempFile("test_glossary", ".tsv")
        tempTsv.deleteOnExit()
        tempTsv.writeText(
            """
            Category	English	Chinese	Path
            BUILDINGS.PREFABS	Cot	簡易床	STRINGS.BUILDINGS.PREFABS.BED.NAME
            BUILDINGS.PREFABS	Cot	小床	STRINGS.BUILDINGS.PREFABS.BED.FACADES.DEFAULT_BED.NAME
            BUILDING.STATUSITEMS	Broken	壞掉的	STRINGS.BUILDING.STATUSITEMS.BROKEN.NAME
            UI.SPACEDESTINATIONS	Metallic Asteroid	金屬小行星	STRINGS.UI.SPACEDESTINATIONS.METALLIC_ASTEROID.NAME
            """.trimIndent()
        )

        val loaded = bank.loadFromTsv(tempTsv)
        assertTrue(loaded)
        assertEquals(2, bank.terms.size)
        assertTrue(bank.terms.any { it.english == "Cot" && it.canonicalCht == "簡易床" })
        assertTrue(bank.terms.any { it.english == "Metallic Asteroid" && it.canonicalCht == "金屬小行星" })
        // Verify blacklisted items are rejected
        assertTrue(bank.terms.none { it.key.contains(".FACADES.") })
        assertTrue(bank.terms.none { it.key.contains("STATUSITEMS") })
    }

    @Test
    fun testGlossaryBankFallbackIncludesLevel1Categories() {
        val bank = GlossaryBank()
        val entries = listOf(
            PoEntry(key = "STRINGS.BUILDINGS.PREFABS.BED.FACADES.DEFAULT_BED.NAME", text = "", id = "Cot", str = "小床"),
            PoEntry(key = "STRINGS.BUILDINGS.PREFABS.BED.NAME", text = "", id = "Cot", str = "簡易床"),
            PoEntry(key = "STRINGS.BUILDING.STATUSITEMS.BROKEN.NAME", text = "", id = "Broken", str = "壞掉的"),
            PoEntry(key = "STRINGS.UI.SPACEDESTINATIONS.METALLIC.NAME", text = "", id = "Metallic Asteroid", str = "金屬小行星"),
            PoEntry(key = "STRINGS.DUPLICANTS.ROLES.ARTIST.NAME", text = "", id = "Master Artist", str = "藝術大師"),
            PoEntry(key = "STRINGS.CREATURES.PLANT_MUTATIONS.BLOOMS.NAME", text = "", id = "Blooming", str = "盛開"),
            PoEntry(key = "STRINGS.DUPLICANTS.ATTRIBUTES.ATHLETICS.NAME", text = "", id = "Athletics", str = "體能"),
        )

        bank.fallbackFromPoEntries(entries)
        // Verify facades and statusitems skipped, Cot takes BED.NAME, and level 1 categories extracted
        assertEquals(5, bank.terms.size)
        val cotTerm = bank.findByEnglish("Cot")
        assertEquals("簡易床", cotTerm?.canonicalCht)
        assertEquals("STRINGS.BUILDINGS.PREFABS.BED.NAME", cotTerm?.key)
        assertTrue(bank.terms.any { it.english == "Metallic Asteroid" })
        assertTrue(bank.terms.any { it.english == "Master Artist" })
        assertTrue(bank.terms.any { it.english == "Blooming" })
        assertTrue(bank.terms.any { it.english == "Athletics" })
    }
}
