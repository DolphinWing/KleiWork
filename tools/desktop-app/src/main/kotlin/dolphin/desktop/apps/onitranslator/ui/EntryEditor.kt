@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)

package dolphin.desktop.apps.onitranslator.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.Clipboard
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dolphin.desktop.apps.onitranslator.app.AppEvent
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.button_apply
import dolphin.desktop.apps.onitranslator.generated.resources.button_cancel
import dolphin.desktop.apps.onitranslator.generated.resources.content_description_clear
import dolphin.desktop.apps.onitranslator.generated.resources.glossary_accepted_info
import dolphin.desktop.apps.onitranslator.generated.resources.glossary_accept_term
import dolphin.desktop.apps.onitranslator.generated.resources.glossary_applied_success
import dolphin.desktop.apps.onitranslator.generated.resources.glossary_apply_canonical
import dolphin.desktop.apps.onitranslator.generated.resources.glossary_conflict_alert
import dolphin.desktop.apps.onitranslator.generated.resources.glossary_reset_canonical
import dolphin.desktop.apps.onitranslator.generated.resources.glossary_undo_apply
import dolphin.desktop.apps.onitranslator.generated.resources.ic_auto_awesome
import dolphin.desktop.apps.onitranslator.generated.resources.ic_close
import dolphin.desktop.apps.onitranslator.generated.resources.ic_content_copy
import dolphin.desktop.apps.onitranslator.generated.resources.ic_content_paste
import dolphin.desktop.apps.onitranslator.generated.resources.ic_done
import dolphin.desktop.apps.onitranslator.generated.resources.ic_link
import dolphin.desktop.apps.onitranslator.generated.resources.ic_report
import dolphin.desktop.apps.onitranslator.generated.resources.ic_settings_backup_restore
import dolphin.desktop.apps.onitranslator.generated.resources.ic_visibility
import dolphin.desktop.apps.onitranslator.generated.resources.ic_visibility_off
import dolphin.desktop.apps.onitranslator.generated.resources.label_translated_text
import dolphin.desktop.apps.onitranslator.generated.resources.nisbet_ponder
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_copy_canonical
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
import dolphin.desktop.apps.onitranslator.model.GlossaryConflict
import dolphin.desktop.apps.onitranslator.model.GlossaryDiagnostic
import dolphin.desktop.apps.onitranslator.model.GlossarySensor
import dolphin.desktop.apps.onitranslator.model.GlossaryTerm
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
    terms: List<GlossaryTerm> = emptyList(),
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
            EntryEditorSection(entry, onEvent, onConvert, modifier, terms)
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
    terms: List<GlossaryTerm> = emptyList(),
) {
    var editedText by remember { mutableStateOf(entry.target.msgStr()) }
    var backupText by remember { mutableStateOf<String?>(null) }
    var undoCanonicalState by remember { mutableStateOf<Pair<String, String>?>(null) }
    var showRefs by remember { mutableStateOf(false) }
    var showPoSave by remember { mutableStateOf(false) }
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

    // Live glossary sensor diagnostic
    val liveGlossaryDiag = remember(editedText, entry.sourceText, terms, entry.target.glossaryDiag?.isAccepted) {
        if (terms.isNotEmpty()) {
            GlossarySensor.diagnose(
                sourceText = entry.sourceText,
                targetText = editedText,
                terms = terms,
                isAccepted = entry.target.glossaryDiag?.isAccepted == true
            )
        } else {
            entry.target.glossaryDiag
        }
    }

    LaunchedEffect(entry.target.key) {
        editedText = entry.target.msgStr()
        backupText = null
        undoCanonicalState = null
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
                onValueChange = {
                    editedText = it
                    undoCanonicalState = null
                },
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

            GlossarySensorBanner(
                diagnostic = liveGlossaryDiag,
                undoState = undoCanonicalState,
                onApplyCanonical = { conflict ->
                    val applied = GlossarySensor.applyCanonical(editedText, conflict, entry.chsReference)
                    if (applied != editedText) {
                        backupText = editedText
                        undoCanonicalState = Pair(editedText, conflict.canonicalTranslation)
                        editedText = applied
                        onEvent(AppEvent.Editor.Save(entry.target, applied))
                    } else {
                        onEvent(AppEvent.Ui.CopyToClipboard(conflict.canonicalTranslation))
                    }
                },
                onUndoCanonical = {
                    undoCanonicalState?.let { (prevText, _) ->
                        editedText = prevText
                        onEvent(AppEvent.Editor.Save(entry.target, prevText))
                        undoCanonicalState = null
                        backupText = null
                    }
                },
                onDismissUndo = {
                    undoCanonicalState = null
                },
                onCopyToClipboard = { text ->
                    onEvent(AppEvent.Ui.CopyToClipboard(text))
                },
                onAcceptNewTerm = {
                    onEvent(AppEvent.Glossary.AcceptNewTerm(entry.target.key))
                },
                onResetToCanonical = {
                    onEvent(AppEvent.Glossary.ResetToCanonical(entry.target.key))
                }
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
                painter = painterResource(if (isPeeking) Res.drawable.ic_visibility_off else Res.drawable.ic_visibility),
                tooltip = if (isPeeking) stringResource(Res.string.tooltip_peek_close) else stringResource(Res.string.tooltip_peek_open),
                position = TooltipAnchorPosition.Above,
                onClick = { onPeekingChange(!isPeeking) }
            )
        }

        TooltipIconButton(
            painter = painterResource(Res.drawable.ic_auto_awesome),
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
                painter = painterResource(Res.drawable.ic_settings_backup_restore),
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
        entry.oldSourceText?.let { text ->
            if (text.isNotBlank()) {
                ReferenceView(
                    text,
                    type = EntryTagType.OldSource
                )
            }
        }
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
                if (links.count() > 0) {
                    TooltipIconButton(
                        painter = painterResource(Res.drawable.ic_link),
                        tooltip = stringResource(Res.string.tooltip_show_link),
                    ) {
                        linkSelector = !linkSelector
                    }
                }
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_content_copy),
                    tooltip = stringResource(Res.string.tooltip_copy_this_text),
                ) {
                    listener.invoke("")
                }
            }
            onReplace?.let { listener ->
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_content_paste),
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
                        ReferenceView("old template text", EntryTagType.OldSource)
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
            val link = it.groupValues[1].trim { c -> c == '"' || c == '\\' }
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onSelected(link) }) {
                    Text(link)
                }
                IconButton(onClick = { onSelected("<link=\\\"${link}\\\"></link>") }) {
                    Icon(painterResource(Res.drawable.ic_link), contentDescription = null)
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

@Composable
private fun GlossarySensorBanner(
    diagnostic: GlossaryDiagnostic?,
    undoState: Pair<String, String>?,
    onApplyCanonical: (GlossaryConflict) -> Unit,
    onUndoCanonical: () -> Unit,
    onDismissUndo: () -> Unit,
    onCopyToClipboard: (String) -> Unit,
    onAcceptNewTerm: () -> Unit,
    onResetToCanonical: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // In-place Undo Banner: shown immediately after auto-applying a canonical term
    if (undoState != null) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painterResource(Res.drawable.ic_done),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(Res.string.glossary_applied_success, undoState.second),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                OutlinedButton(
                    onClick = onUndoCanonical,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Text(
                        text = stringResource(Res.string.glossary_undo_apply),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                TooltipIconButton(
                    painter = painterResource(Res.drawable.ic_close),
                    tooltip = stringResource(Res.string.content_description_clear),
                    position = TooltipAnchorPosition.Above,
                    onClick = onDismissUndo
                )
            }
        }
        return
    }

    if (diagnostic == null || !diagnostic.hasConflict) return

    if (diagnostic.isUnreviewed) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                diagnostic.conflicts.forEach { conflict ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            painterResource(Res.drawable.ic_report),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = stringResource(
                                Res.string.glossary_conflict_alert,
                                conflict.matchedSourceText,
                                conflict.canonicalTranslation
                            ),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        // Always-visible copy canonical to clipboard button
                        TooltipIconButton(
                            painter = painterResource(Res.drawable.ic_content_copy),
                            tooltip = stringResource(Res.string.tooltip_copy_canonical, conflict.canonicalTranslation),
                            position = TooltipAnchorPosition.Above,
                            onClick = { onCopyToClipboard(conflict.canonicalTranslation) }
                        )
                        Button(
                            onClick = { onApplyCanonical(conflict) },
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                            shape = MaterialTheme.shapes.small,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            )
                        ) {
                            Text(
                                text = stringResource(Res.string.glossary_apply_canonical, conflict.canonicalTranslation),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(
                        onClick = onAcceptNewTerm,
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = stringResource(Res.string.glossary_accept_term),
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    } else if (diagnostic.isAccepted) {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painterResource(Res.drawable.ic_done),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                val canonicals = diagnostic.conflicts.joinToString(", ") { it.canonicalTranslation }
                Text(
                    text = stringResource(Res.string.glossary_accepted_info, canonicals),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onResetToCanonical,
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(Res.string.glossary_reset_canonical),
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun EntryEditorPreview() {
    val entry = EditorData(
        target = PoEntry(
            key = "STRINGS.BUILDINGS.PREFABS.BASE_OUTHOUSE.NAME",
            text = "STRINGS.BUILDINGS.PREFABS.BASE_OUTHOUSE.NAME",
            id = "Outhouse",
            str = "戶外廁所",
            msgidChanged = true
        ),
        sourceText = "Outhouse (New)",
        oldSourceText = "Outhouse",
        chsReference = "旱厕",
        poText = "戶外廁所"
    )
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OniTranslatorTheme(darkTheme = false) {
            Surface {
                EntryEditor(entry = entry, onEvent = {})
            }
        }
        OniTranslatorTheme(darkTheme = true) {
            Surface {
                EntryEditor(entry = entry, onEvent = {})
            }
        }
    }
}

