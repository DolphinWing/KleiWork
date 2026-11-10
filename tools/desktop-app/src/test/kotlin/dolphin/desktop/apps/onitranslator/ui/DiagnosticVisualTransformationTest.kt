package dolphin.desktop.apps.onitranslator.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class DiagnosticVisualTransformationTest {

    @Test
    fun testEmptySpansIdentity() {
        val raw = AnnotatedString("這是一台瓦斯發電機。")
        val vt = DiagnosticVisualTransformation(emptyList())
        val transformed = vt.filter(raw)

        assertEquals(raw.text, transformed.text.text)
        assertEquals(0, transformed.text.spanStyles.size)
        assertEquals(OffsetMapping.Identity, transformed.offsetMapping)
    }

    @Test
    fun testSingleSpanHighlighting() {
        val raw = AnnotatedString("這是一台瓦斯發電機。")
        val highlightColor = Color(0x40EF5350)
        val span = HighlightSpan(range = 4..8, backgroundColor = highlightColor)

        val vt = DiagnosticVisualTransformation(listOf(span))
        val transformed = vt.filter(raw)

        assertEquals(raw.text, transformed.text.text)
        assertEquals(1, transformed.text.spanStyles.size)
        val applied = transformed.text.spanStyles[0]
        assertEquals(4, applied.start)
        assertEquals(9, applied.end) // end is exclusive in AnnotatedString (range 4..8 -> 4 until 9)
        assertEquals(highlightColor, applied.item.background)
        assertEquals(FontWeight.Normal, applied.item.fontWeight)
    }

    @Test
    fun testMultipleSpansHighlighting() {
        val raw = AnnotatedString("這是一台瓦斯發電機。請保持瓦斯發電機低溫。")
        val color1 = Color(0x40EF5350)
        val color2 = Color(0x40AB47BC)

        val spans = listOf(
            HighlightSpan(range = 4..8, backgroundColor = color1),
            HighlightSpan(range = 13..17, backgroundColor = color2, isHovered = true)
        )

        val vt = DiagnosticVisualTransformation(spans)
        val transformed = vt.filter(raw)

        assertEquals(2, transformed.text.spanStyles.size)

        val style1 = transformed.text.spanStyles[0]
        assertEquals(4, style1.start)
        assertEquals(9, style1.end)
        assertEquals(color1, style1.item.background)
        assertEquals(FontWeight.Normal, style1.item.fontWeight)

        val style2 = transformed.text.spanStyles[1]
        assertEquals(13, style2.start)
        assertEquals(18, style2.end)
        assertEquals(color2, style2.item.background)
        assertEquals(FontWeight.Bold, style2.item.fontWeight)
    }

    @Test
    fun testOutOfBoundsDefensiveClamping() {
        // Simulates rapid typing or deletion where span indices temporarily exceed string length
        val shortText = AnnotatedString("Short")
        val staleSpan = HighlightSpan(range = 10..30, backgroundColor = Color.Red)
        val partiallyOverSpan = HighlightSpan(range = 2..20, backgroundColor = Color.Blue)

        val vt = DiagnosticVisualTransformation(listOf(staleSpan, partiallyOverSpan))
        // Must not throw IndexOutOfBoundsException!
        val transformed = vt.filter(shortText)

        assertNotNull(transformed)
        assertEquals(shortText.text, transformed.text.text)
        // staleSpan (10..30 clamped to 5..5) produces empty span start==end so it's skipped.
        // partiallyOverSpan (2..20 clamped to 2..5) applies safely.
        assertEquals(1, transformed.text.spanStyles.size)
        assertEquals(2, transformed.text.spanStyles[0].start)
        assertEquals(5, transformed.text.spanStyles[0].end)
    }

    @Test
    fun testIdentityOffsetMappingGuarantees() {
        val raw = AnnotatedString("Hello World")
        val vt = DiagnosticVisualTransformation(listOf(HighlightSpan(0..4, Color.Yellow)))
        val transformed = vt.filter(raw)

        for (i in 0..raw.length) {
            assertEquals(i, transformed.offsetMapping.originalToTransformed(i))
            assertEquals(i, transformed.offsetMapping.transformedToOriginal(i))
        }
    }
}
