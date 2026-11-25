package dolphin.desktop.apps.onitranslator.model

import kotlinx.coroutines.runBlocking
import java.io.BufferedReader
import java.io.File
import java.io.StringReader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
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

    @Test
    fun testEntryMapIndexAndTemplateOrder() = runBlocking {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "po_helper_index_test_${System.currentTimeMillis()}")
        val assetsDir = File(tempDir, "assets").apply { mkdirs() }
        val workshopDir = File(tempDir, "workshop").apply { mkdirs() }
        try {
            File(assetsDir, "strings_template.pot").writeText(
                """
                #. STRINGS.Z_SECOND
                msgctxt "STRINGS.Z_SECOND"
                msgid "Second"
                msgstr ""

                #. STRINGS.A_FIRST
                msgctxt "STRINGS.A_FIRST"
                msgid "First"
                msgstr ""
                """.trimIndent()
            )
            val testHelper = PoHelper(
                Configs(
                    oniAssetsDir = assetsDir.absolutePath,
                    oniWorkshopDir = workshopDir.absolutePath,
                    dataBankPath = ""
                ),
                mockRefinery
            )
            testHelper.runTranslationProcess()

            val entries = testHelper.allEntries()
            assertEquals(listOf("STRINGS.Z_SECOND", "STRINGS.A_FIRST"), entries.map { it.key })

            val entry = testHelper.entry("STRINGS.A_FIRST")
            assertNotNull(entry)
            assertEquals("First", entry.id)
            assertNull(testHelper.entry("STRINGS.NON_EXISTENT"))

            testHelper.update("STRINGS.A_FIRST", "第一")
            assertEquals("第一", testHelper.entry("STRINGS.A_FIRST")?.str)
            assertTrue(testHelper.entry("STRINGS.A_FIRST")?.changed ?: 0L > 0L)

            // allEntries() returns an unmodifiable view
            val mutableView = entries as MutableList<PoEntry>
            assertFailsWith<UnsupportedOperationException> {
                mutableView.clear()
            }
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testCommitFormalSaveSyncsBaselineAndClearsPending(): Unit = runBlocking {
        val tempDir = File(System.getProperty("java.io.tmpdir"), "po_helper_commit_test_${System.currentTimeMillis()}")
        val assetsDir = File(tempDir, "assets").apply { mkdirs() }
        val workshopDir = File(tempDir, "workshop").apply { mkdirs() }
        try {
            File(assetsDir, "strings_template.pot").writeText(
                """
                #. STRINGS.EXISTING
                msgctxt "STRINGS.EXISTING"
                msgid "New English"
                msgstr ""

                #. STRINGS.NEW_ITEM
                msgctxt "STRINGS.NEW_ITEM"
                msgid "Brand New"
                msgstr ""
                """.trimIndent()
            )
            File(workshopDir, "strings.po").writeText(
                """
                #. STRINGS.EXISTING
                msgctxt "STRINGS.EXISTING"
                msgid "Old English"
                msgstr "舊翻譯"
                """.trimIndent()
            )
            val testHelper = PoHelper(
                Configs(
                    oniAssetsDir = assetsDir.absolutePath,
                    oniWorkshopDir = workshopDir.absolutePath,
                    dataBankPath = ""
                ),
                mockRefinery
            )
            testHelper.runTranslationProcess()

            assertEquals(true, testHelper.entry("STRINGS.EXISTING")?.msgidChanged)
            assertEquals(true, testHelper.entry("STRINGS.NEW_ITEM")?.newly)
            assertEquals(2, testHelper.buildChangeList().size)

            testHelper.update("STRINGS.EXISTING", "新翻譯")
            testHelper.commitFormalSave()

            assertEquals("新翻譯", testHelper.poEntry("STRINGS.EXISTING")?.str)
            assertEquals("New English", testHelper.poEntry("STRINGS.EXISTING")?.id)
            assertNotNull(testHelper.poEntry("STRINGS.NEW_ITEM"))
            assertNull(testHelper.draftEntry("STRINGS.EXISTING"))

            val existing = testHelper.entry("STRINGS.EXISTING")
            assertNotNull(existing)
            assertEquals(0L, existing.changed)
            assertFalse(existing.newly)
            assertFalse(existing.msgidChanged)

            val newItem = testHelper.entry("STRINGS.NEW_ITEM")
            assertNotNull(newItem)
            assertEquals(0L, newItem.changed)
            assertFalse(newItem.newly)
            assertFalse(newItem.msgidChanged)

            assertTrue(testHelper.buildChangeList().isEmpty())

            // Ensure translatedEntries stores an independent snapshot from entryList
            testHelper.update("STRINGS.EXISTING", "二次修改")
            assertEquals("二次修改", testHelper.entry("STRINGS.EXISTING")?.str)
            assertEquals("新翻譯", testHelper.poEntry("STRINGS.EXISTING")?.str)
        } finally {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testEscapePoValueNormalizesRawNewlines() {
        val raw = "第一行\r\n第二行\n\n含\"引號\"與既有\\n換行"
        val escaped = helper.escapePoValue(raw)
        assertEquals("""第一行\n第二行\n\n含\"引號\"與既有\n換行""", escaped)
    }
}
