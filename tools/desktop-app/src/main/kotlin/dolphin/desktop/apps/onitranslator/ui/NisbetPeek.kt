package dolphin.desktop.apps.onitranslator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.peek_title
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.compose.resources.StringResource

/**
 * NisbetPeek: The cute WYSIWYG preview for ONI strings.
 * 
 * [ONI Syntax Samples]
 * 
 * [Supported]
 * - Color:   "Select <color=#ffff00>Blueprint</color>"
 * - Link:    "<link=\"PLANTS\">Plants</link> can grow..."
 * - Bold:    "Press <b>[P]</b> to focus."
 * - Newline: "First line.\\nSecond line."
 * 
 * [Pending/Analysis]
 * - <b><style=\"logic_off\">Red Signal</style></b>: Set signal path to <b>up</b> position
 * - This Duplicant receives a free <style=\"KKeyword\">Skill</style>
 * - I can use {Hotkey/AnalogCamera} to pan my view.
 * - (O<sub>2</sub>) Polluted Oxygen is dirty, unfiltered air.\n\nIt is breathable.
 * - RelicAAAA<sup>AAAGHH</sup>
 * - <smallcaps>「風化小行星」有頻繁的流星雨，且富含風化層，這是一種極為有用的過濾材料。</smallcaps>
 * - <smallcaps>To: <b>Harold P. Moreson, PhD</b><alpha=#AA><size=12> <hmoreson@gravitas.nova></size></color>
 * - <b>Your VIP package includes:</b><indent=5%>\n\n- An exclusive set of bespoke survival-supporting technology!
 * - <size=11><i>*Discount applies to new memberships only. Standard joiner fees apply.</size></i>
 * 
 * [Analysis from Screenshots]
 * - 0001.PNG: Lore/Email metadata uses gray text.
 * - 0002.PNG: Item links use a distinct pink/magenta color.
 * - 0003.PNG: Shortcuts and focus items use red or orange highlights.
 * - 0004.PNG: Logic signals use green/red.
 */

// ONI Game UI Color Palette
object OniColor {
    val Link = Color(0xFFF48FB1)    // Pink/Magenta for items/links
    val Warning = Color(0xFFFF5252) // Red for shortcuts and alerts
    val Highlight = Color(0xFFFFB74D) // Orange/Yellow for names
    val Success = Color(0xFF81C784) // Green for logic signals
    val Metadata = Color(0xFF9E9E9E) // Gray for email headers
}

/**
 * The magic function that turns raw code into Nisbet's vision.
 * Usage: text.peek()
 */
fun String.peek(): AnnotatedString {
    val raw = this.replace("\\n", "\n")
    
    return buildAnnotatedString {
        var currentIndex = 0
        
        // Regex to find: <color=#xxxxxx>...</color>, <link="id">...</link>, <b>...</b>
        val tagRegex = Regex("""<(color|link|b)[^>]*>(.*?)</\1>""", RegexOption.DOT_MATCHES_ALL)
        val matches = tagRegex.findAll(raw)
        
        for (match in matches) {
            append(raw.substring(currentIndex, match.range.first))
            
            val tagName = match.groups[1]?.value
            val tagContent = match.groups[2]?.value ?: ""
            val fullMatch = match.value
            
            val style = when {
                tagName == "b" -> SpanStyle(fontWeight = FontWeight.Bold)
                tagName == "link" -> SpanStyle(
                    color = OniColor.Link, 
                    textDecoration = TextDecoration.Underline
                )
                fullMatch.contains("color=#") -> {
                    val hex = Regex("""color=#([A-Fa-f0-9]{6})""").find(fullMatch)?.groups?.get(1)?.value
                    if (hex != null) {
                        try {
                            val colorLong = hex.toLong(16) or 0xFF000000
                            SpanStyle(color = Color(colorLong))
                        } catch (e: Exception) {
                            SpanStyle(color = OniColor.Highlight)
                        }
                    } else {
                        SpanStyle(color = OniColor.Highlight)
                    }
                }
                else -> SpanStyle()
            }
            
            pushStyle(style)
            append(tagContent)
            pop()
            
            currentIndex = match.range.last + 1
        }
        
        if (currentIndex < raw.length) {
            append(raw.substring(currentIndex))
        }
    }
}

/**
 * The smart sensor that decides if an entry is interesting enough for Nisbet to peek.
 */
fun String.shouldPeek(): Boolean {
    if (this.isBlank()) return false
    val hasNewline = this.contains("\\n")
    val hasTags = this.contains("<color") || this.contains("<link") || this.contains("<b>")
    val isLong = this.length > 120
    return hasNewline || hasTags || isLong
}

@Composable
fun NisbetPeekDrawer(
    visible: Boolean,
    text: String,
    avatar: Painter?,
    quote: StringResource?,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut(),
        modifier = modifier.fillMaxHeight().width(320.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp,
            shadowElevation = 16.dp,
        ) {
            Column(modifier = Modifier.padding(16.dp).verticalScroll(rememberScrollState())) {
                Text(
                    stringResource(Res.string.peek_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.secondary,
                )

                Spacer(Modifier.height(24.dp))

                NisbetPeekCard(text = text)

                Spacer(Modifier.height(24.dp))

                // Chat-style Interaction
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    avatar?.let {
                        Image(
                            painter = it,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).padding(end = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp
                        ),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f)
                    ) {
                        quote?.let {
                            Text(
                                stringResource(it),
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NisbetPeekCard(
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(modifier = Modifier.padding(12.dp)) {
            Text(
                text = text.peek(),
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp, letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
