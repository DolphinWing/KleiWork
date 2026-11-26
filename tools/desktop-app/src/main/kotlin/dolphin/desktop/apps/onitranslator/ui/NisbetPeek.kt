package dolphin.desktop.apps.onitranslator.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import dolphin.desktop.apps.onitranslator.model.SnackbarManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetAnticipate
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetHigh
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetSorry
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetThinking
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetWhistle
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.button_close
import dolphin.desktop.apps.onitranslator.generated.resources.ic_close
import dolphin.desktop.apps.onitranslator.generated.resources.ic_link
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_1
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_2
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_3
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_peek_close
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_4
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_5
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_error_1
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_error_2
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_error_3
import dolphin.desktop.apps.onitranslator.generated.resources.peek_title
import dolphin.desktop.apps.onitranslator.generated.resources.tag_sensor_mismatch
import dolphin.desktop.apps.onitranslator.generated.resources.tag_sensor_source_error
import dolphin.desktop.apps.onitranslator.generated.resources.tag_sensor_target_error
import dolphin.desktop.apps.onitranslator.generated.resources.tag_sensor_title
import dolphin.desktop.apps.onitranslator.model.TagDiagnostic
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/**
 * NisbetPeek: The cute WYSIWYG preview for ONI strings.
 * 
 * [ONI Syntax Samples]
 * 
 * [Supported]
 * - Color:   "Select <color=#ffff00>Blueprint</color>"
 * - Link:    "<link=\"PLANTS\">Plants</link> can grow..."
 * - Bold:    "Press <b>[P]</b> to focus."
 * - Italic:  "<i>Italic text</i>"
 * - Style:   "<style=\"KKeyword\">Skill</style>"
 * - Newline: "First line.\\nSecond line."
 * - Sub/Sup: "(O<sub>2</sub>)", "Relic<sup>TM</sup>"
 * - Dynamic: "Press {Hotkey/Pan} to move"
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
    val Link = Color(0xFFF48FB1)    // Pink/Magenta
    val Warning = Color(0xFFFF5252) // Red
    val Highlight = Color(0xFFFFB74D) // Orange/Yellow
    val Success = Color(0xFF81C784) // Green
    val Metadata = Color(0xFF9E9E9E) // Gray
    val Dynamic = Color(0xFF81D4FA)  // Light Blue
    val Keyword = Color(0xFF26C6DA)  // Cyan/Teal for Klei KKeyword
    val Lore = Color(0xFFB0BEC5)     // Blue-Gray for flavor_text / lore
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
        if (value.startsWith("{") && value.endsWith("}")) {
            tokens.add(OniToken.Dynamic(value))
        } else if (value.startsWith("</") && value.endsWith(">")) {
            tokens.add(OniToken.ClosingTag(value.substring(2, value.length - 1).lowercase()))
        } else if (value.startsWith("<") && value.endsWith(">")) {
            if (value.contains("@")) { // High probability of being an email address
                tokens.add(OniToken.Plain(match.value))
            } else {
                val content = value.substring(1, value.length - 1)
                val tagName = content.split("=")[0].split(" ")[0].lowercase()
                tokens.add(OniToken.OpeningTag(tagName, value))
            }
        } else {
            tokens.add(OniToken.Plain(value))
        }
        currentIndex = match.range.last + 1
    }

    if (currentIndex < raw.length) {
        tokens.add(OniToken.Plain(raw.substring(currentIndex)))
    }

    return tokens
}

private val KNOWN_ONI_TAGS = setOf(
    "b",
    "i",
    "link",
    "sub",
    "sup",
    "style",
    "color",
    "alpha",
    "size",
    "smallcaps",
    "indent",
)

private val ALPHA_HEX_REGEX = Regex("""#([A-Fa-f0-9]{2})""")

/**
 * The magic function that turns tokens into Nisbet's vision.
 */
@Composable
@ReadOnlyComposable
fun String.peek(): AnnotatedString {
    val tokens = this.toOniTokens()
    val defaultAlpha = 0.6f
    val tagStack = mutableListOf<Pair<String, SpanStyle>>()
    val linkStarts = mutableListOf<Pair<Int, String>>()

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
                    if (token.name == "link" && linkStarts.isNotEmpty()) {
                        val (start, id) = linkStarts.removeAt(linkStarts.size - 1)
                        if (length > start) {
                            addStringAnnotation(tag = "link", annotation = id, start = start, end = length)
                        }
                    }
                    // Unity TMP has no </alpha> closing tag; </color> can close either <color> or <alpha>
                    val matchIdx = if (token.name == "alpha") {
                        -1
                    } else {
                        tagStack.indexOfLast { (name, _) ->
                            if (token.name == "color") (name == "color" || name == "alpha") else name == token.name
                        }
                    }
                    if (matchIdx >= 0) {
                        val popCount = tagStack.size - matchIdx
                        val reopen = tagStack.subList(matchIdx + 1, tagStack.size).toList()
                        repeat(popCount) { pop() }
                        while (tagStack.size > matchIdx) {
                            tagStack.removeAt(tagStack.size - 1)
                        }
                        reopen.forEach { (name, style) ->
                            tagStack.add(name to style)
                            pushStyle(style)
                        }
                    } else {
                        pushStyle(SpanStyle(color = OniColor.Warning.copy(alpha = 0.5f), fontSize = 8.sp))
                        append("</${token.name}>")
                        pop()
                    }
                }

                is OniToken.OpeningTag -> {
                    if (token.name == "link") {
                        val rawId = token.raw.substringAfter("=").substringBefore(">").trim(' ', '"', '\\')
                        linkStarts.add(length to rawId)
                    }
                    val style = when (token.name) {
                        "b" -> SpanStyle(fontWeight = FontWeight.Bold)
                        "i" -> SpanStyle(fontStyle = FontStyle.Italic)
                        "link" -> SpanStyle(
                            color = OniColor.Link,
                            textDecoration = TextDecoration.Underline,
                            background = OniColor.Link.copy(alpha = 0.12f),
                            fontWeight = FontWeight.Medium,
                        )
                        "sub" -> SpanStyle(baselineShift = BaselineShift.Subscript, fontSize = 10.sp)
                        "sup" -> SpanStyle(baselineShift = BaselineShift.Superscript, fontSize = 10.sp)
                        "style" -> {
                            val rawLower = token.raw.lowercase()
                            when {
                                rawLower.contains("logic_on") -> SpanStyle(color = OniColor.Success, fontWeight = FontWeight.Bold)
                                rawLower.contains("logic_off") -> SpanStyle(color = OniColor.Warning, fontWeight = FontWeight.Bold)
                                rawLower.contains("kkeyword") -> SpanStyle(color = OniColor.Keyword, fontWeight = FontWeight.SemiBold)
                                rawLower.contains("flavor_text") || rawLower.contains("lore") -> SpanStyle(color = OniColor.Lore, fontStyle = FontStyle.Italic)
                                else -> SpanStyle(color = OniColor.Highlight)
                            }
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

                        "alpha" -> {
                            val hexAlpha = ALPHA_HEX_REGEX.find(token.raw)?.groupValues?.get(1)?.toIntOrNull(16)
                            val resolvedAlpha = if (hexAlpha != null) (hexAlpha / 255f).coerceIn(0f, 1f) else defaultAlpha
                            SpanStyle(color = MaterialTheme.colorScheme.onSurface.copy(alpha = resolvedAlpha))
                        }
                        "size" -> SpanStyle(fontSize = 12.sp)
                        "smallcaps" -> SpanStyle(fontFeatureSettings = "smcp")
                        "indent" -> SpanStyle(baselineShift = BaselineShift.None) // Indent is hard to simulate in SpanStyle, but we keep it safe
                        else -> {
                            SpanStyle(
                                color = OniColor.Warning,
                                fontSize = 9.sp,
                                textDecoration = TextDecoration.Underline
                            )
                        }
                    }

                    if (token.name in KNOWN_ONI_TAGS) {
                        tagStack.add(token.name to style)
                        pushStyle(style)
                    } else {
                        pushStyle(style)
                        append(token.raw)
                        pop()
                    }
                }
            }
        }
        linkStarts.forEach { (start, id) ->
            if (length > start) {
                addStringAnnotation(tag = "link", annotation = id, start = start, end = length)
            }
        }
        repeat(tagStack.size) { pop() }
    }
}

/**
 * The smart sensor that decides if an entry is interesting enough for Nisbet to peek.
 */
fun String.shouldPeek(diagnostic: TagDiagnostic? = null): Boolean {
    if (this.isBlank()) return false
    if (diagnostic?.hasIssue == true) return true // Always peek if there are tag issues
    val hasNewline = this.contains("\\n") || this.contains("\n")
    val hasTags = this.contains("<") || this.contains("{")
    val isLong = this.length > 100
    return hasNewline || hasTags || isLong
}

/**
 * Detects if the string has ONI / Unity TextMeshPro rich-text syntax errors.
 *
 * Follows Unity TextMeshPro semantics rather than strict XML LIFO nesting:
 * - Angle-bracketed emails containing `@` (e.g. `<msklodowska@gravitas.nova>`) are already treated as
 *   [OniToken.Plain] by [toOniTokens].
 * - `<alpha>` is a TMP color-channel state tag with no `</alpha>` closing tag: `</alpha>` is always rejected,
 *   while `</color>` may close either `<color>` or `<alpha>` (one-way compatibility) and `<alpha=#AA>...<alpha=#FF>`
 *   state overrides do not require a closing tag.
 * - Independent per-tag depth tracking allows legal TMP crossed nesting (e.g. `<size=12><alpha=#AA>...</size></color>`,
 *   `<i><smallcaps>...</i>...</smallcaps>`), while still flagging unknown tags, unmatched closings, and unclosed tags.
 */
fun String.hasOniSyntaxError(): Boolean {
    val tokens = this.toOniTokens()
    var colorDepth = 0
    var alphaDepth = 0
    val tagDepths = mutableMapOf<String, Int>()

    for (token in tokens) {
        when (token) {
            is OniToken.OpeningTag -> {
                if (token.name !in KNOWN_ONI_TAGS) return true
                when (token.name) {
                    "color" -> colorDepth++
                    "alpha" -> alphaDepth++
                    else -> tagDepths[token.name] = (tagDepths[token.name] ?: 0) + 1
                }
            }

            is OniToken.ClosingTag -> {
                when (token.name) {
                    "alpha" -> return true
                    "color" -> {
                        if (colorDepth > 0) {
                            colorDepth--
                        } else if (alphaDepth > 0) {
                            alphaDepth--
                        } else {
                            return true
                        }
                    }

                    else -> {
                        if (token.name !in KNOWN_ONI_TAGS) return true
                        val current = tagDepths[token.name] ?: 0
                        if (current <= 0) return true
                        tagDepths[token.name] = current - 1
                    }
                }
            }

            else -> {}
        }
    }
    return colorDepth > 0 || tagDepths.values.any { it > 0 }
}

@Composable
fun NisbetPeekDrawer(
    visible: Boolean,
    text: String,
    emotion: NisbetEmotion?,
    diagnostic: TagDiagnostic? = null,
    onDismiss: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { it } + fadeIn(),
        exit = slideOutHorizontally { it } + fadeOut(),
        modifier = modifier.fillMaxHeight().width(420.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surfaceVariant,
            tonalElevation = 4.dp,
            shadowElevation = 8.dp,
            // border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Fixed Header: Title and top-right close button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(Res.string.peek_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(
                            painterResource(Res.drawable.ic_close),
                            contentDescription = stringResource(Res.string.button_close),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Scrollable Content
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {

                NisbetPeekCard(text = text)

                val linkedEntities = remember(text) { extractLinkedEntities(text) }
                if (linkedEntities.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    LinkedEntitiesCard(linkedEntities)
                }

                // Tag Sensor Report
                diagnostic?.let { diag ->
                    if (diag.hasIssue) {
                        Spacer(Modifier.height(16.dp))
                        DiagnosticReport(diag)
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Chat-style Interaction: More lightweight and conversational
                Row(
                    verticalAlignment = Alignment.Bottom, // Align to bottom for a grounded feel
                    modifier = Modifier.fillMaxWidth()
                ) {
                    emotion?.let {
                        Image(
                            painter = painterResource(it.icon),
                            contentDescription = null,
                            modifier = Modifier
                                .size(80.dp) // Slightly larger
                                .padding(bottom = 4.dp)
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 4.dp,
                            bottomEnd = 16.dp
                        ),
                        color = MaterialTheme.colorScheme.surface, // Cleaner background
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .weight(1f)
                            .padding(bottom = 16.dp, end = 12.dp)
                    ) {
                        emotion?.let {
                            Text(
                                stringResource(it.quote),
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    // fontStyle = FontStyle.Italic // "Murmur" vibe
                                ),
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Sticky Bottom Action: Full-width button matching bottom-right opening ergonomic zone
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(Res.drawable.ic_close),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(Res.string.tooltip_peek_close))
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticReport(diagnostic: TagDiagnostic) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                stringResource(Res.string.tag_sensor_title),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            if (diagnostic.sourceError) {
                Text(
                    stringResource(Res.string.tag_sensor_source_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OniColor.Highlight
                )
            }

            if (diagnostic.targetError) {
                Text(
                    stringResource(Res.string.tag_sensor_target_error),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OniColor.Warning
                )
            }

            val report = diagnostic.getDiffReport()
            report.forEach { (tag, diff) ->
                Text(
                    stringResource(Res.string.tag_sensor_mismatch, tag, diff.first, diff.second),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (diff.first == diff.second) MaterialTheme.colorScheme.onSurface
                    else OniColor.Warning
                )
            }

        }
    }
}

@Composable
fun NisbetPeekCard(
    text: String,
    modifier: Modifier = Modifier
) {
    val annotatedString = text.peek()
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(2.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Box(
            modifier = Modifier
                .padding(12.dp)
                .pointerInput(annotatedString) {
                    detectTapGestures { pos ->
                        val layout = layoutResult ?: return@detectTapGestures
                        val offset = layout.getOffsetForPosition(pos)
                        val annotations = annotatedString.getStringAnnotations(tag = "link", start = offset, end = offset)
                        annotations.firstOrNull()?.let { annotation ->
                            val linkId = annotation.item
                            try {
                                val stringSelection = java.awt.datatransfer.StringSelection(linkId)
                                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(stringSelection, null)
                            } catch (_: Exception) {}
                            SnackbarManager.showMessage(linkId)
                        }
                    }
                }
        ) {
            Text(
                text = annotatedString,
                onTextLayout = { layoutResult = it },
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 20.sp, letterSpacing = 0.5.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/**
 * Extracts distinct linked entities (<link="ID">Text</link>) from text for preview inspection.
 * Strips nested inner tags (e.g. <b>) and handles escaped quotes (\") and unquoted IDs cleanly.
 */
fun extractLinkedEntities(text: String): List<Pair<String, String>> {
    if (!text.contains("<link", ignoreCase = true)) return emptyList()
    val regex = Regex("""<link\s*=\s*\\*"?([^\\">]+)\\*"?>\s*(.*?)\s*</link>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
    val tagRegex = Regex("""<[^>]+>""")
    return regex.findAll(text).map { match ->
        val rawInner = match.groupValues[2]
        val cleanText = rawInner.replace(tagRegex, "").trim()
        val linkId = match.groupValues[1].trim()
        (if (cleanText.isNotEmpty()) cleanText else linkId) to linkId
    }.distinct().toList()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinkedEntitiesCard(entities: List<Pair<String, String>>) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    painterResource(Res.drawable.ic_link),
                    contentDescription = null,
                    tint = OniColor.Link,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "連結目標實體 (Link Target IDs)",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                entities.forEach { (linkText, linkId) ->
                    Surface(
                        onClick = {
                            try {
                                val stringSelection = java.awt.datatransfer.StringSelection(linkId)
                                java.awt.Toolkit.getDefaultToolkit().systemClipboard.setContents(stringSelection, null)
                            } catch (_: Exception) {}
                            SnackbarManager.showMessage(linkId)
                        },
                        shape = RoundedCornerShape(4.dp),
                        color = OniColor.Link.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, OniColor.Link.copy(alpha = 0.35f)),
                        modifier = Modifier.pointerHoverIcon(PointerIcon.Hand)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = linkText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "➜",
                                style = MaterialTheme.typography.labelSmall,
                                color = OniColor.Link
                            )
                            Text(
                                text = linkId,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
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
