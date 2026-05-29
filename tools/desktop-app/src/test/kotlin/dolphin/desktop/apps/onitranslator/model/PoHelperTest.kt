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
}
