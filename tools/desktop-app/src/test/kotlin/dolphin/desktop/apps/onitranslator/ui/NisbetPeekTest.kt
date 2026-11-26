package dolphin.desktop.apps.onitranslator.ui

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertIs

class NisbetPeekTest {

    @Test
    fun testShouldPeek() {
        assertFalse("Hello world".shouldPeek())
        assertTrue("Hello\\nworld".shouldPeek())
        assertTrue("Select <color=#ffff00>Blueprint</color>".shouldPeek())
        assertTrue("Press {Hotkey/Pan} to move".shouldPeek())
    }

    @Test
    fun testToOniTokens_Basic() {
        val text = "Hello <b>World</b>"
        val tokens = text.toOniTokens()
        
        // Tokens: "Hello ", <b>, "World", </b>
        assertEquals(4, tokens.size) 
        assertIs<OniToken.Plain>(tokens[0])
        assertEquals("Hello ", (tokens[0] as OniToken.Plain).text)
        
        assertIs<OniToken.OpeningTag>(tokens[1])
        assertEquals("b", (tokens[1] as OniToken.OpeningTag).name)
        
        assertIs<OniToken.Plain>(tokens[2])
        assertEquals("World", (tokens[2] as OniToken.Plain).text)
        
        assertIs<OniToken.ClosingTag>(tokens[3])
        assertEquals("b", (tokens[3] as OniToken.ClosingTag).name)
    }

    @Test
    fun testToOniTokens_Unrecognized() {
        val text = "<unknown>Text</unknown>"
        val tokens = text.toOniTokens()
        
        assertEquals(3, tokens.size) // <unknown>, "Text", </unknown>
        assertIs<OniToken.OpeningTag>(tokens[0])
        assertEquals("unknown", (tokens[0] as OniToken.OpeningTag).name)
    }

    @Test
    fun testToOniTokens_Dynamic() {
        val text = "Press {Hotkey/Action}"
        val tokens = text.toOniTokens()
        
        assertEquals(2, tokens.size) // "Press ", {Hotkey/Action}
        assertIs<OniToken.Dynamic>(tokens[1])
        assertEquals("{Hotkey/Action}", (tokens[1] as OniToken.Dynamic).raw)
    }

    @Test
    fun testToOniTokens_Unclosed() {
        val text = "Hello <b>World"
        val tokens = text.toOniTokens()
        
        // Tokens: "Hello ", <b>, "World"
        assertEquals(3, tokens.size)
        assertEquals("b", (tokens[1] as OniToken.OpeningTag).name)
    }

    @Test
    fun testToOniTokens_BuggyKlei() {
        // Sample of what might happen in real data
        val text = "Click <color=#ff00ff>here</link>"
        val tokens = text.toOniTokens()
        
        // Tokens: "Click ", <color>, "here", </link>
        assertEquals(4, tokens.size)
        assertEquals("color", (tokens[1] as OniToken.OpeningTag).name)
        assertEquals("link", (tokens[3] as OniToken.ClosingTag).name)
    }

    @Test
    fun testExtractLinkedEntities() {
        val standard = """We need a <link="PLANTERBOX">Planter Box</link> for farming."""
        assertEquals(listOf("Planter Box" to "PLANTERBOX"), extractLinkedEntities(standard))

        val compound = """Construct a <link="MANUALGENERATOR"><b>Manual Generator</b></link> now."""
        assertEquals(listOf("Manual Generator" to "MANUALGENERATOR"), extractLinkedEntities(compound))

        val quoteless = """See <link=DOOR>Pneumatic Door</link> here."""
        assertEquals(listOf("Pneumatic Door" to "DOOR"), extractLinkedEntities(quoteless))

        val escapedQuotes = """Sample <link=\"METALORE\">Metal</link> from PO file."""
        assertEquals(listOf("Metal" to "METALORE"), extractLinkedEntities(escapedQuotes))

        val doubleEscaped = """Sample <link=\\"OXYGEN\\">Oxygen</link> from serialized string."""
        assertEquals(listOf("Oxygen" to "OXYGEN"), extractLinkedEntities(doubleEscaped))
    }

    @Test
    fun testHasOniSyntaxError_TmpEmailHeadersAndCrossedNesting() {
        val emailHeader = "<smallcaps>To: <b>Dr. M. Skłodowska</b><size=12><alpha=#AA> <msklodowska@gravitas.nova></size></color>\n" +
            "From: <b>Admin</b><size=12><alpha=#AA> <admin@gravitas.nova></color></size></smallcaps>"
        assertFalse(emailHeader.hasOniSyntaxError(), "Klei email headers with <alpha=#AA>...</color> and crossed nesting must pass")

        val crossedItalicSmallcaps = "<i><smallcaps>Proactive solutions</i>\nOpen 24/7</smallcaps>"
        assertFalse(crossedItalicSmallcaps.hasOniSyntaxError(), "TMP crossed nesting must pass")

        val stateOverrideAlpha = "Normal <alpha=#55>Faded Text <alpha=#FF>Normal again"
        assertFalse(stateOverrideAlpha.hasOniSyntaxError(), "TMP state-override <alpha> without closing tag must pass")
    }

    @Test
    fun testHasOniSyntaxError_RejectsInvalidAndUnclosedTags() {
        assertTrue("<alpha=#AA>Faded</alpha>".hasOniSyntaxError(), "TMP has no </alpha> closing tag")
        assertTrue("Extra </color>".hasOniSyntaxError(), "Unmatched </color> must fail")
        assertTrue("<color=#ffffff>Unclosed".hasOniSyntaxError(), "Unclosed <color> must fail")
        assertTrue("<b>Unclosed bold".hasOniSyntaxError(), "Unclosed <b> must fail")
        assertTrue("Click <color=#ff00ff>here</link>".hasOniSyntaxError(), "Mismatched closing tag must fail")
        assertTrue("<unknown>Text</unknown>".hasOniSyntaxError(), "Unknown tag must fail")
    }
}
