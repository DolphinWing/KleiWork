package dolphin.desktop.apps.onitranslator.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Represents a text range inside the editor to be visually highlighted.
 *
 * @param range Character index range in the target text.
 * @param backgroundColor Background tint color for highlighting.
 * @param textColor Optional text foreground color.
 * @param isHovered Whether this span is currently hovered by a Quick-Fix chip.
 */
data class HighlightSpan(
    val range: IntRange,
    val backgroundColor: Color,
    val textColor: Color = Color.Unspecified,
    val isHovered: Boolean = false,
)

/**
 * VisualTransformation that decorates raw editor text with diagnostic background colors
 * and styles, without altering character content or offset mapping (1:1 identity).
 *
 * Features:
 * 1. Zero cursor jitter: Uses strict 1:1 [OffsetMapping.Identity].
 * 2. Typing safety: Clamps all span ranges defensively to avoid [IndexOutOfBoundsException]
 *    during high-speed keystrokes or text deletion frames.
 * 3. Hover synchronization: Highlights focused spans with bolder font weight.
 */
class DiagnosticVisualTransformation(
    private val highlightSpans: List<HighlightSpan>,
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        if (highlightSpans.isEmpty() || text.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val builder = AnnotatedString.Builder(text)
        val len = text.length

        for (span in highlightSpans) {
            val start = span.range.first.coerceIn(0, len)
            val end = (span.range.last + 1).coerceIn(start, len)

            if (start < end) {
                builder.addStyle(
                    SpanStyle(
                        background = span.backgroundColor,
                        color = span.textColor,
                        fontWeight = if (span.isHovered) FontWeight.Bold else FontWeight.Normal,
                    ),
                    start = start,
                    end = end,
                )
            }
        }

        return TransformedText(builder.toAnnotatedString(), OffsetMapping.Identity)
    }
}
