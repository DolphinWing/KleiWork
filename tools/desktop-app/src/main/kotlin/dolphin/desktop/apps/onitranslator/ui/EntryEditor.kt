@file:OptIn(ExperimentalMaterial3Api::class)

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
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dolphin.desktop.apps.onitranslator.app.AppEvent
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.button_apply
import dolphin.desktop.apps.onitranslator.generated.resources.button_cancel
import dolphin.desktop.apps.onitranslator.generated.resources.label_translated_text
import dolphin.desktop.apps.onitranslator.generated.resources.nisbet_ponder
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_copy_this_text
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_peek_close
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_peek_open
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_show_link
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_smart_paste
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_toggle_simplified
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_toggle_translated
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_undo_paste
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_use_this_text
import dolphin.desktop.apps.onitranslator.model.EditorData
import dolphin.desktop.apps.onitranslator.model.EntryTagType
import dolphin.desktop.apps.onitranslator.model.PoEntry
import dolphin.desktop.apps.onitranslator.model.TagDiagnostic
import dolphin.desktop.apps.onitranslator.model.TagSensor
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme
import dolphin.desktop.apps.onitranslator.widget.TooltipIconButton
import kotlinx.coroutines.CoroutineScope
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

/**
 * Internal logic for handling Smart Paste from clipboard.
 */
private fun performSmartPaste(
    scope: CoroutineScope,
    clipboard: Clipboard,
    target: PoEntry,
    onParsed: (String) -> Unit,
    onSave: (PoEntry, String) -> Unit,
    onError: (String) -> Unit
) {
    scope.launch {
        try {
            // Using the modern LocalClipboard API
            // Ref: https://medium.com/@yamin.khan.mahdi/reading-clipboard-text-across-all-platforms-in-compose-multiplatform-cmp-7474ffc03f09
            clipboard.getClipEntry()?.let { clipEntry ->
                val transferable = clipEntry.nativeClipEntry as? Transferable
                if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    val clipboardText = transferable.getTransferData(DataFlavor.stringFlavor) as? String
                    if (clipboardText != null) {
                        extractMsgStr(clipboardText)?.let { parsedText ->
                            onParsed(parsedText)
                            onSave(target, parsedText)
                        } ?: onError("Invalid msgstr format. $clipboardText")
                    } else {
                        onError("No clip entry available.")
                    }
                } else {
                    onError("No such clip entry.")
                }
            } ?: onError("No clip entry found.")
        } catch (e: Exception) {
            onError(e.message ?: "fail to get clip entry")
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
    var showPoSave by remember { mutableStateOf(true) }
    var isPeeking by remember { mutableStateOf(false) }
    val isChanged = editedText != (entry.target.str.trim())
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // emotion state is now encapsulated
    val emotion = remember(isPeeking, editedText) {
        if (isPeeking) NisbetEmotion.random(editedText) else null
    }

    // Live tag sensor diagnostic
    val liveDiagnostic = remember(editedText, entry.sourceText) {
        TagSensor.diagnose(entry.sourceText, editedText)
    }

    LaunchedEffect(entry.target.key) {
        editedText = entry.target.msgStr()
        backupText = null
        isPeeking = false
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
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
                chsRefsVisible = showRefs,
                poSaveVisible = showPoSave,
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

            EditorActionRow(
                entry = entry,
                editedText = editedText,
                showRefs = showRefs,
                onShowRefsChange = { showRefs = it },
                showPoSave = showPoSave,
                onShowPoSaveChange = { showPoSave = it },
                isPeeking = isPeeking,
                onPeekingChange = { isPeeking = it },
                backupText = backupText,
                onEditedTextChange = { editedText = it },
                onBackupTextChange = { backupText = it },
                isChanged = isChanged,
                onEvent = onEvent,
                clipboard = clipboard,
                scope = scope,
                diagnostic = liveDiagnostic
            )
        }

        NisbetPeekDrawer(
            visible = isPeeking && editedText.shouldPeek(liveDiagnostic),
            text = editedText,
            emotion = emotion,
            diagnostic = liveDiagnostic,
            onDismiss = { isPeeking = false },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}

@Composable
private fun EditorActionRow(
    entry: EditorData,
    editedText: String,
    showRefs: Boolean,
    onShowRefsChange: (Boolean) -> Unit,
    showPoSave: Boolean,
    onShowPoSaveChange: (Boolean) -> Unit,
    isPeeking: Boolean,
    onPeekingChange: (Boolean) -> Unit,
    backupText: String?,
    onEditedTextChange: (String) -> Unit,
    onBackupTextChange: (String?) -> Unit,
    isChanged: Boolean,
    onEvent: (AppEvent) -> Unit,
    clipboard: Clipboard,
    scope: CoroutineScope,
    diagnostic: TagDiagnostic? = null,
) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        entry.chsReference?.let {
            SimpleSwitch(checked = showRefs, onCheckedChange = onShowRefsChange, type = EntryTagType.ChsRef)
        }
        Spacer(Modifier.width(8.dp))
        entry.poText?.let {
            SimpleSwitch(checked = showPoSave, onCheckedChange = onShowPoSaveChange, type = EntryTagType.PoSave)
        }

        Spacer(Modifier.width(16.dp))

        if (editedText.shouldPeek(diagnostic)) {
            TooltipIconButton(
                icon = if (isPeeking) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                tooltip = if (isPeeking) stringResource(Res.string.tooltip_peek_close) else stringResource(Res.string.tooltip_peek_open),
                position = TooltipAnchorPosition.Above,
                onClick = { onPeekingChange(!isPeeking) }
            )
        }

        TooltipIconButton(
            icon = Icons.Rounded.AutoAwesome,
            tooltip = stringResource(Res.string.tooltip_smart_paste),
            position = TooltipAnchorPosition.Above,
            onClick = {
                performSmartPaste(
                    scope = scope,
                    clipboard = clipboard,
                    target = entry.target,
                    onParsed = { parsed ->
                        onBackupTextChange(editedText)
                        onEditedTextChange(parsed)
                    },
                    onSave = { target, text -> onEvent(AppEvent.Editor.Save(target, text)) },
                    onError = { onEvent(AppEvent.Editor.SmartCopyError(it)) }
                )
            }
        )

        if (backupText != null) {
            TooltipIconButton(
                icon = Icons.Rounded.SettingsBackupRestore,
                tooltip = stringResource(Res.string.tooltip_undo_paste),
                position = TooltipAnchorPosition.Above,
                onClick = {
                    onEditedTextChange(backupText)
                    onEvent(AppEvent.Editor.Save(entry.target, backupText))
                    onBackupTextChange(null)
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
    chsRefsVisible: Boolean = true,
    poSaveVisible: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ReferenceView(
            entry.sourceText, type = EntryTagType.Source,
            onCopyToClipboard = { text ->
                val textToCopy = text.ifBlank {
                    var t = "msgctxt \"${entry.target.key()}\"\n"
                    t += "msgid \"${entry.sourceText}\"\n"
                    t += "msgstr \"${entry.target.msgStr()}\""
                    t
                }
                onCopyToClipboard.invoke(textToCopy)
            }
        )
        entry.chsReference?.let { text ->
            if (text.isNotBlank() && chsRefsVisible) {
                ReferenceView(
                    text,
                    type = EntryTagType.ChsRef,
                    onReplace = { onReplace.invoke(text) },
                )
            }
        }
        entry.poText?.let { text ->
            if (text.isNotBlank() && poSaveVisible) {
                ReferenceView(
                    text,
                    type = EntryTagType.PoSave,
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
                            EntryTagType.Source,
                            onCopyToClipboard = {})
                        ReferenceView("simplified text", EntryTagType.ChsRef, onReplace = {})
                        ReferenceView("translated text", EntryTagType.PoSave, onReplace = {})
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
                EntryTagType.ChsRef -> stringResource(Res.string.tooltip_toggle_simplified)
                EntryTagType.PoSave -> stringResource(Res.string.tooltip_toggle_translated)
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
