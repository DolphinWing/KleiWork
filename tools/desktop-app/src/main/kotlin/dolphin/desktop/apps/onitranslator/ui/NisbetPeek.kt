package dolphin.desktop.apps.onitranslator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetAnticipate
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetHigh
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetSorry
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetThinking
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetWhistle
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_1
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_2
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_3
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_4
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_5
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_error_1
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_error_2
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_error_3
import dolphin.desktop.apps.onitranslator.generated.resources.peek_title
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * NisbetPeek: The cute WYSIWYG preview for ONI strings.
 */

// ONI Game UI Color Palette
object OniColor {
    val Link = Color(0xFFF48FB1)    // Pink/Magenta
    val Warning = Color(0xFFFF5252) // Red
    val Highlight = Color(0xFFFFB74D) // Orange/Yellow
    val Success = Color(0xFF81C784) // Green
    val Metadata = Color(0xFF9E9E9E) // Gray
    val Dynamic = Color(0xFF81D4FA)  // Light Blue
    val Unrecognized = Color(0x4D000000) // Faded black for unknown tags
}

sealed class OniToken {
    data class Plain(val text: String) : OniToken()
    data class OpeningTag(val name: String, val raw: String) : OniToken()
    data class ClosingTag(val name: String) : OniToken()
    data class Dynamic(val raw: String) : OniToken()
}

data class NisbetEmotion(
    val icon: DrawableResource,
    val quote: StringResource
) {
    companion object {
        fun random(text: String): NisbetEmotion {
            return if (text.hasOniSyntaxError()) {
                val quote = listOf(
                    Res.string.peek_quote_error_1,
                    Res.string.peek_quote_error_2,
                    Res.string.peek_quote_error_3
                ).random()
                NisbetEmotion(Res.drawable.NisbetSorry, quote)
            } else {
                val quote = listOf(
                    Res.string.peek_quote_1,
                    Res.string.peek_quote_2,
                    Res.string.peek_quote_3,
                    Res.string.peek_quote_4,
                    Res.string.peek_quote_5
                ).random()
                val icon = listOf(
                    Res.drawable.NisbetAnticipate,
                    Res.drawable.NisbetHigh,
                    Res.drawable.NisbetThinking,
                    Res.drawable.NisbetWhistle
                ).random()
                NisbetEmotion(icon, quote)
            }
        }
    }
}

/**
 * Pure logic: Parse raw string into tokens. No Compose dependencies.
 */
fun String.toOniTokens(): List<OniToken> {
    val raw = this.replace("\\n", "\n")
    val tokens = mutableListOf<OniToken>()
    val regex = Regex("""(<[^>]+>|\{[^}]+\})""")
    var currentIndex = 0

    regex.findAll(raw).forEach { match ->
        if (match.range.first > currentIndex) {
            tokens.add(OniToken.Plain(raw.substring(currentIndex, match.range.first)))
        }

        val value = match.value
        when {
            value.startsWith("{") -> tokens.add(OniToken.Dynamic(value))
            value.startsWith("</") -> tokens.add(OniToken.ClosingTag(value.substring(2, value.length - 1).lowercase()))
            else -> {
                val tagName = value.substring(1, value.length - 1).split("=")[0].split(" ")[0].lowercase()
                tokens.add(OniToken.OpeningTag(tagName, value))
            }
        }
        currentIndex = match.range.last + 1
    }

    if (currentIndex < raw.length) {
        tokens.add(OniToken.Plain(raw.substring(currentIndex)))
    }

    return tokens
}

/**
 * The magic function that turns tokens into Nisbet's vision.
 */
@Composable
@ReadOnlyComposable
fun String.peek(): AnnotatedString {
    val tokens = this.toOniTokens()
    val defaultAlpha = 0.6f
    val tagStack = mutableListOf<String>()

    return buildAnnotatedString {
        tokens.forEach { token ->
            when (token) {
                is OniToken.Plain -> append(token.text)
                is OniToken.Dynamic -> {
                    pushStyle(SpanStyle(color = OniColor.Dynamic, fontWeight = FontWeight.Bold))
                    append(token.raw)
                    pop()
                }

                is OniToken.ClosingTag -> {
                    if (tagStack.isNotEmpty() && tagStack.last() == token.name) {
                        tagStack.removeAt(tagStack.size - 1)
                        pop()
                    } else {
                        pushStyle(SpanStyle(color = OniColor.Warning.copy(alpha = 0.5f), fontSize = 8.sp))
                        append("</${token.name}>")
                        pop()
                    }
                }

                is OniToken.OpeningTag -> {
                    val style = when (token.name) {
                        "b" -> SpanStyle(fontWeight = FontWeight.Bold)
                        "i" -> SpanStyle(fontStyle = FontStyle.Italic)
                        "link" -> SpanStyle(color = OniColor.Link, textDecoration = TextDecoration.Underline)
                        "sub" -> SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 10.sp)
                        "sup" -> SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 10.sp)
                        "style" -> {
                            if (token.raw.contains("logic_off")) SpanStyle(color = OniColor.Warning)
                            else SpanStyle(color = OniColor.Highlight)
                        }

                        "color" -> {
                            val hex = Regex("""#([A-Fa-f0-9]{6})""").find(token.raw)?.groups?.get(1)?.value
                            if (hex != null) {
                                try {
                                    val colorLong = hex.toLong(16) or 0xFF000000
                                    SpanStyle(color = Color(colorLong))
                                } catch (_: Exception) {
                                    SpanStyle(color = OniColor.Highlight)
                                }
                            } else SpanStyle(color = OniColor.Highlight)
                        }

                        "alpha" -> SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = defaultAlpha))
                        "size" -> SpanStyle(fontSize = 12.sp)
                        else -> {
                            SpanStyle(
                                color = OniColor.Warning,
                                fontSize = 9.sp,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }

                    if (token.name in listOf("b", "i", "link", "sub", "sup", "style", "color", "alpha", "size")) {
                        tagStack.add(token.name)
                        pushStyle(style)
                    } else {
                        pushStyle(style)
                        append(token.raw)
                        pop()
                    }
                }
            }
        }
        repeat(tagStack.size) { pop() }
    }
}

/**
 * The smart sensor that decides if an entry is interesting enough for Nisbet to peek.
 */
fun String.shouldPeek(): Boolean {
    if (this.isBlank()) return false
    val hasNewline = this.contains("\\n")
    val hasTags = this.contains("<") || this.contains("{")
    val isLong = this.length > 100
    return hasNewline || hasTags || isLong
}

/**
 * Detects if the string has ONI syntax errors.
 */
fun String.hasOniSyntaxError(): Boolean {
    val tokens = this.toOniTokens()
    val tagStack = mutableListOf<String>()
    val knownTags = listOf("b", "i", "link", "sub", "sup", "style", "color", "alpha", "size")

    tokens.forEach { token ->
        when (token) {
            is OniToken.OpeningTag -> {
                if (token.name !in knownTags) return true
                tagStack.add(token.name)
            }

            is OniToken.ClosingTag -> {
                if (tagStack.isEmpty() || tagStack.last() != token.name) return true
                tagStack.removeAt(tagStack.size - 1)
            }

            else -> {}
        }
    }
    return tagStack.isNotEmpty()
}

@Composable
fun NisbetPeekDrawer(
    visible: Boolean,
    text: String,
    emotion: NisbetEmotion?,
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
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                NisbetPeekCard(text = text)

                Spacer(Modifier.height(24.dp))

                // Chat-style Interaction
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    emotion?.let {
                        Image(
                            painter = painterResource(it.icon),
                            contentDescription = null,
                            modifier = Modifier.size(64.dp).padding(end = 12.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 0.dp, topEnd = 12.dp, bottomStart = 12.dp, bottomEnd = 12.dp
                        ),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        modifier = Modifier.weight(1f)
                    ) {
                        emotion?.let {
                            Text(
                                stringResource(it.quote),
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

@Preview
@Composable
private fun NisbetPeekPreview() {
    val samples = listOf(
        "Select <color=#ffff00>Blueprint</color>",
        "Press <b>[P]</b> to focus.",
        "(O<sub>2</sub>) Polluted Oxygen is dirty, unfiltered air.\\n\\nIt is breathable.",
        "RelicAAAA<sup>AAAGHH</sup>",
        "<b><style=\"logic_off\">Red Signal</style></b>: Set signal path to <b>up</b> position",
        "I can use {Hotkey/AnalogCamera} to pan my view.",
        "<foo>Unrecognized tag</foo> and <color=#ff00ff>unclosed tag"
    )

    OniTranslatorTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                samples.forEach { text ->
                    NisbetPeekCard(text = text)
                }
            }
        }
    }
}

@Preview
@Composable
private fun NisbetPeekDrawerPreview() {
    OniTranslatorTheme {
        Surface {
            NisbetPeekDrawer(
                visible = true,
                text = "這段翻譯看起來不錯！\\n我們來試試看<link=\\\"PLANTERBOX\\\">種植箱</link>的效果。\\n\\n以及強大的<b><color=#ff0000>紅色訊號</color></b>！",
                emotion = NisbetEmotion.random("這段翻譯看起來不錯！"),
                modifier = Modifier.height(600.dp)
            )
        }
    }
}
