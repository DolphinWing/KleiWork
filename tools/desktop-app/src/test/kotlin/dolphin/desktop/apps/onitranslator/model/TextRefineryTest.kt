package dolphin.desktop.apps.onitranslator.model

import kotlin.test.Test
import kotlin.test.assertEquals

class TextRefineryTest {
    private val refinery = TextRefinery(emptyMap())

    @Test
    fun testRefineQuotesPairedEscaped() {
        val original = """這是一個 \"小動物\"，聽說它很 \"可愛\"。"""
        val refined = refinery.refineQuotes(original)
        assertEquals("""這是一個 「小動物」，聽說它很 「可愛」。""", refined)
    }

    @Test
    fun testRefineQuotesPairedRaw() {
        val original = """這是一個 "小動物"，聽說它很 "可愛"。"""
        val refined = refinery.refineQuotes(original)
        assertEquals("""這是一個 「小動物」，聽說它很 「可愛」。""", refined)
    }

    @Test
    fun testRefineQuotesDefensiveOddCount() {
        val original = """這是一個 \"小動物 且 \"可愛\"。""" // 3 quotes, odd count
        val refined = refinery.refineQuotes(original)
        assertEquals(original, refined) // Should not change due to defensive rule
    }

    @Test
    fun testRefineQuotesKeepTagQuotes() {
        val original = """請點擊 <link="XYZ">連結</link> 來查看。"""
        val refined = refinery.refineQuotes(original)
        assertEquals(original, refined) // Tag attribute quotes should be preserved
    }

    @Test
    fun testRefineQuotesMixTagsAndContent() {
        val original = """這是一個 \"小動物\" 且 <link=\"XYZ\">它很 \"可愛\"</link>。"""
        val refined = refinery.refineQuotes(original)
        val expected = """這是一個 「小動物」 且 <link=\"XYZ\">它很 「可愛」</link>。"""
        assertEquals(expected, refined)
    }
}
