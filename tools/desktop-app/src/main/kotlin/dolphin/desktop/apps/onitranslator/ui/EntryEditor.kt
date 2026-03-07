package dolphin.desktop.apps.onitranslator.ui

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.SettingsBackupRestore
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dolphin.desktop.apps.onitranslator.app.AppEvent
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetAnticipate
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetHigh
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetSorry
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetThinking
import dolphin.desktop.apps.onitranslator.generated.resources.NisbetWhistle
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.button_apply
import dolphin.desktop.apps.onitranslator.generated.resources.button_cancel
import dolphin.desktop.apps.onitranslator.generated.resources.label_translated_text
import dolphin.desktop.apps.onitranslator.generated.resources.nisbet_ponder
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_1
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_2
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_3
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_4
import dolphin.desktop.apps.onitranslator.generated.resources.peek_quote_5
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_copy_this_text
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_show_link
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_smart_paste
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_toggle_simplified
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_toggle_translated
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_undo_paste
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_use_this_text
import dolphin.desktop.apps.onitranslator.model.EditorData
import dolphin.desktop.apps.onitranslator.model.EntryTagType
import dolphin.desktop.apps.onitranslator.model.PoEntry
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme
import dolphin.desktop.apps.onitranslator.widget.TooltipIconButton
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable

@Composable
fun EntryEditor(
    entry: EditorData?,
    onEvent: (AppEvent) -> Unit,
    modifier: Modifier = Modifier,
    onConvert: (String) -> String = { it },
) {
    if (entry == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Image(
                painterResource(Res.drawable.nisbet_ponder),
                contentDescription = null,
                modifier = Modifier.size(240.dp),
                alpha = 0.5f
            )
        }
    } else {
        key(entry) { // reset all states
            EntryEditorSection(entry, onEvent, onConvert, modifier)
        }
    }
}

@Preview
@Composable
private fun EntryEditorPreviewEmpty() {
    Column {
        arrayOf(false, true).forEach { darkTheme ->
            OniTranslatorTheme(darkTheme) {
                Surface {
                    EntryEditor(entry = null, modifier = Modifier.height(320.dp), onEvent = {})
                }
            }
        }
    }
}

@Preview
@Composable
private fun EntryEditorPreviewLight() {
    val sampleEntry = PoEntry(
        key = "STRINGS.key",
        id = "This is sample text",
    )
    val data = EditorData(sampleEntry, "sample text", "simplified text", "draftText text")

    OniTranslatorTheme(darkTheme = false) {
        Surface {
            EntryEditor(entry = data, onEvent = {})
        }
    }
}

@Preview
@Composable
private fun EntryEditorPreviewDark() {
    val sampleEntry = PoEntry(
        key = "STRINGS.key",
        text = "STRING.key",
        id = "This is sample text",
        str = "This is translated text",
    )
    val data = EditorData(sampleEntry, "sample text", "reference text")

    OniTranslatorTheme(darkTheme = true) {
        Surface {
            EntryEditor(entry = data, onEvent = {})
        }
    }
}

@Composable
private fun EntryEditorSection(
    entry: EditorData,
    onEvent: (AppEvent) -> Unit,
    onConvert: (String) -> String,
    modifier: Modifier = Modifier,
) {
    var editedText by remember { mutableStateOf(entry.target.msgStr()) }
    var backupText by remember { mutableStateOf<String?>(null) }
    var showRefs by remember { mutableStateOf(true) }
    var showDraft by remember { mutableStateOf(true) }
    var isPeeking by remember { mutableStateOf(false) }
    val isChanged = editedText != (entry.target.str.trim())
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // Random quote and emotion for NisbetPeek
    val (peekQuote, peekAvatar) = remember(isPeeking) {
        if (isPeeking) {
            val quote = listOf(
                Res.string.peek_quote_1,
                Res.string.peek_quote_2,
                Res.string.peek_quote_3,
                Res.string.peek_quote_4,
                Res.string.peek_quote_5
            ).random()

            val avatar = listOf(
                Res.drawable.NisbetAnticipate,
                Res.drawable.NisbetHigh,
                Res.drawable.NisbetThinking,
                Res.drawable.NisbetSorry,
                Res.drawable.NisbetWhistle,
            ).random()

            quote to avatar
        } else null to null
    }

    LaunchedEffect(entry.target.key) {
        editedText = entry.target.msgStr()
        backupText = null // Reset backup when switching entries
        isPeeking = false // Reset preview when switching entries
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Main Editor Layer
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                entry.target.key,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.fillMaxWidth()
            )

            ReferenceSection(
                entry,
                onCopyToClipboard = { onEvent(AppEvent.Ui.CopyToClipboard(it)) },
                onReplace = { editedText = onConvert(it) },
                refsVisible = showRefs,
                draftVisible = showDraft,
            )

            OutlinedTextField(
                value = editedText,
                onValueChange = { editedText = it },
                label = { Text(stringResource(Res.string.label_translated_text)) },
                modifier = Modifier.fillMaxWidth().weight(1f),
                minLines = 5,
                textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                shape = MaterialTheme.shapes.medium,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                )
            )

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                entry.referenceText?.let {
                    SimpleSwitch(
                        checked = showRefs,
                        onCheckedChange = { showRefs = it },
                        type = EntryTagType.Simplified
                    )
                }
                Spacer(Modifier.width(8.dp))
                entry.draftText?.let {
                    SimpleSwitch(
                        checked = showDraft,
                        onCheckedChange = { showDraft = it },
                        type = EntryTagType.Translated
                    )
                }

                Spacer(Modifier.width(16.dp))

                // NisbetPeek Toggle: Shown only when entry has tags or long text
                if (editedText.shouldPeek()) {
                    TooltipIconButton(
                        icon = if (isPeeking) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        tooltip = if (isPeeking) "Close NisbetPeek" else "Let Nisbet peek",
                        onClick = { isPeeking = !isPeeking }
                    )
                }

                // Smart Paste from Clipboard (Gemini Gems format) - Auto Apply
                TooltipIconButton(
                    icon = Icons.Rounded.AutoAwesome,
                    tooltip = stringResource(Res.string.tooltip_smart_paste),
                    onClick = {
                        scope.launch {
                            // Using the modern LocalClipboard API
                            // Ref: https://medium.com/@yamin.khan.mahdi/reading-clipboard-text-across-all-platforms-in-compose-multiplatform-cmp-7474ffc03f09
                            try {
                                clipboard.getClipEntry()?.let { clipEntry ->
                                    // On Desktop, ClipEntry wraps java.awt.datatransfer.Transferable
                                    // The property is accessible as nativeClipEntry in recent CMP versions
                                    val transferable = clipEntry.nativeClipEntry as? Transferable
                                    if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                                        val clipboardText =
                                            transferable.getTransferData(DataFlavor.stringFlavor) as? String
                                        if (clipboardText != null) {
                                            extractMsgStr(clipboardText)?.let { parsedText ->
                                                backupText = editedText // Save current manual text for undo
                                                editedText = parsedText
                                                // Auto Apply: trigger save event immediately
                                                onEvent(AppEvent.Editor.Save(entry.target, parsedText))
                                            }
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                // Silent fail for clipboard access issues
                            }
                        }
                    }
                )

                // Undo Smart Paste - Auto Apply back
                if (backupText != null) {
                    TooltipIconButton(
                        icon = Icons.Rounded.SettingsBackupRestore,
                        tooltip = stringResource(Res.string.tooltip_undo_paste),
                        onClick = {
                            val textToRestore = backupText ?: ""
                            editedText = textToRestore
                            // Auto Apply: trigger save event to revert back
                            onEvent(AppEvent.Editor.Save(entry.target, textToRestore))
                            backupText = null // Clear backup after restore
                        }
                    )
                }

                Spacer(Modifier.weight(1f))
                TextButton(onClick = { onEvent(AppEvent.Editor.Select(null)) }) {
                    Text(stringResource(Res.string.button_cancel))
                }
                TextButton(
                    onClick = { onEvent(AppEvent.Editor.Save(entry.target, editedText)) },
                    enabled = isChanged,
                ) {
                    Text(stringResource(Res.string.button_apply))
                }
            }
        }

        // NisbetPeek: Side Drawer Component
        NisbetPeekDrawer(
            visible = isPeeking && editedText.shouldPeek(),
            text = editedText,
            avatar = peekAvatar?.let { painterResource(it) },
            quote = peekQuote,
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

/**
 * Extracts msgstr content from a PO-formatted string.
 * Supports multi-line format and preserves escape characters like \n.
 */
private fun extractMsgStr(text: String): String? {
    // Look for msgstr "..." pattern. 
    // This regex matches the content inside the first msgstr found.
    val regex = Regex("""msgstr\s+"((?:[^"\\]|\\.)*)"""")
    val match = regex.find(text)
    return match?.groupValues?.get(1) // Return the raw captured content, preserving \n
}

@Composable
private fun ReferenceSection(
    entry: EditorData,
    onCopyToClipboard: (String) -> Unit,
    onReplace: ((String) -> Unit),
    refsVisible: Boolean = true,
    draftVisible: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReferenceView(
            entry.templateText, type = EntryTagType.Original,
            onCopyToClipboard = { text ->
                val textToCopy = text.ifBlank {
                    var t = "msgctxt \"${entry.target.key()}\"\n"
                    t += "msgid \"${entry.templateText}\"\n"
                    t += "msgstr \"${entry.target.msgStr()}\""
                    t
                }
                onCopyToClipboard.invoke(textToCopy)
            }
        )
        entry.referenceText?.let { text ->
            if (text.isNotBlank() && refsVisible) {
                ReferenceView(
                    text,
                    type = EntryTagType.Simplified,
                    onReplace = { onReplace.invoke(text) },
                )
            }
        }
        entry.draftText?.let { text ->
            if (text.isNotBlank() && draftVisible) {
                ReferenceView(
                    text,
                    type = EntryTagType.Translated,
                    onReplace = { onReplace.invoke(text) },
                )
            }
        }
    }
}

@Composable
private fun ReferenceView(
    text: String,
    type: EntryTagType,
    onReplace: (() -> Unit)? = null,
    onCopyToClipboard: ((String) -> Unit)? = null,
) {
    var linkSelector by remember { mutableStateOf(false) }
    // use regex to find link content
    // sample: <link=\"DATABANK\">Data Banks</link>
    val regex = Regex("<link=([^>]+)>([^<]+)</link>")
    val links = regex.findAll(text)

    if (linkSelector) {
        Dialog(onDismissRequest = { linkSelector = false }) {
            LinkSelectorContent(links, onSelected = {
                onCopyToClipboard?.invoke(it)
                linkSelector = false
            })
        }
    }

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = type.containerColor(MaterialTheme.colorScheme),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = type.contentColor(MaterialTheme.colorScheme),
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
            )

            onCopyToClipboard?.let { listener ->
                TooltipIconButton(
                    icon = Icons.Rounded.ContentCopy,
                    tooltip = stringResource(Res.string.tooltip_copy_this_text),
                ) {
                    listener.invoke("")
                }
                if (links.count() > 0) {
                    TooltipIconButton(
                        icon = Icons.Rounded.Link,
                        tooltip = stringResource(Res.string.tooltip_show_link),
                    ) {
                        linkSelector = !linkSelector
                    }
                }
            }
            onReplace?.let { listener ->
                TooltipIconButton(
                    icon = Icons.Rounded.ContentPaste,
                    tooltip = stringResource(Res.string.tooltip_use_this_text),
                    onClick = listener,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ReferenceViewPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        arrayOf(false, true).forEach { darkTheme ->
            OniTranslatorTheme(darkTheme) {
                Surface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReferenceView(
                            "template text <link=\\\"DATABANK\\\">Data Banks</link>",
                            EntryTagType.Original,
                            onCopyToClipboard = {})
                        ReferenceView("simplified text", EntryTagType.Simplified, onReplace = {})
                        ReferenceView("draft text", EntryTagType.Translated, onReplace = {})
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    type: EntryTagType,
    modifier: Modifier = Modifier
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above),
        tooltip = {
            val tooltipText = when (type) {
                EntryTagType.Simplified -> stringResource(Res.string.tooltip_toggle_simplified)
                EntryTagType.Translated -> stringResource(Res.string.tooltip_toggle_translated)
                else -> stringResource(type.label)
            }
            Surface(
                color = MaterialTheme.colorScheme.inverseSurface,
                contentColor = MaterialTheme.colorScheme.inverseOnSurface,
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.extraSmall
            ) {
                Text(
                    tooltipText,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        },
        state = rememberTooltipState(),
    ) {
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                // When checked: dot = content color, track = container color
                checkedThumbColor = type.contentColor(MaterialTheme.colorScheme).copy(alpha = 0.4f),
                checkedTrackColor = type.containerColor(MaterialTheme.colorScheme),
                checkedBorderColor = type.contentColor(MaterialTheme.colorScheme).copy(alpha = 0.4f),

                // When unchecked: use standard outline/surface variant colors
                uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                uncheckedTrackColor = type.containerColor(MaterialTheme.colorScheme).copy(alpha = 0.6f),
                uncheckedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.8f),
            ),
            modifier = modifier,
        )
    }
}

@Preview
@Composable
private fun SimpleSwitchPreview() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        arrayOf(false, true).forEach { darkTheme ->
            OniTranslatorTheme(darkTheme) {
                EntryTagType.entries.forEach { type ->
                    Surface {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(stringResource(type.label))
                            SimpleSwitch(true, {}, type)
                            SimpleSwitch(false, {}, type)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LinkSelectorContent(links: Sequence<MatchResult>, onSelected: (String) -> Unit) {
    Column(
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surface, MaterialTheme.shapes.medium)
            .padding(16.dp)
    ) {
        links.forEach {
            val link = it.groupValues[1].substring(2, it.groupValues[1].length - 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onSelected(link) }) {
                    Text(link)
                }
                IconButton(onClick = { onSelected("<link=\\\"${link}\\\"></link>") }) {
                    Icon(Icons.Rounded.Link, contentDescription = null)
                }
            }
        }
    }
}

@Preview
@Composable
private fun LinkSelectorPreview() {
    val text = "Sample <link=\\\"DATABANK\\\">Data Banks</link> and <link=\\\"MAP\\\">Map</link>"
    val regex = Regex("<link=([^>]+)>([^<]+)</link>")
    val links = regex.findAll(text)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OniTranslatorTheme(darkTheme = false) {
            Surface {
                LinkSelectorContent(links = links, onSelected = {})
            }
        }
        OniTranslatorTheme(darkTheme = true) {
            Surface {
                LinkSelectorContent(links = links, onSelected = {})
            }
        }
    }
}
