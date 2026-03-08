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
}
