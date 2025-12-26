package dolphin.desktop.apps.onitranslator.pane

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
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.Link
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_show_link
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_use_this_text
import dolphin.desktop.apps.onitranslator.model.EditorData
import dolphin.desktop.apps.onitranslator.model.EntryTagType
import dolphin.desktop.apps.onitranslator.model.WordEntry
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme
import dolphin.desktop.apps.onitranslator.widget.TooltipIconButton
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditorPane(
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
            EditorSection(entry, onEvent, onConvert, modifier)
        }
    }
}

@Preview
@Composable
private fun M3EditorPanePreviewEmpty() {
    Column {
        arrayOf(false, true).forEach { darkTheme ->
            OniTranslatorTheme(darkTheme) {
                Surface {
                    EditorPane(entry = null, modifier = Modifier.height(320.dp), onEvent = {})
                }
            }
        }
    }
}

@Preview
@Composable
private fun M3EditorPanePreviewLight() {
    val sampleEntry = WordEntry(
        key = "STRINGS.key",
        id = "This is sample text",
    )
    val data = EditorData(sampleEntry, "sample text", "simplified text", "draftText text")

    OniTranslatorTheme(darkTheme = false) {
        Surface {
            EditorPane(entry = data, onEvent = {})
        }
    }
}

@Preview
@Composable
private fun M3EditorPanePreviewDark() {
    val sampleEntry = WordEntry(
        key = "STRINGS.key",
        text = "STRING.key",
        id = "This is sample text",
        str = "This is translated text",
    )
    val data = EditorData(sampleEntry, "sample text", "reference text")

    OniTranslatorTheme(darkTheme = true) {
        Surface {
            EditorPane(entry = data, onEvent = {})
        }
    }
}

@Composable
private fun EditorSection(
    entry: EditorData,
    onEvent: (AppEvent) -> Unit,
    onConvert: (String) -> String,
    modifier: Modifier = Modifier,
) {
    var editedText by remember { mutableStateOf(entry.target.translated()) }
    val isChanged = editedText != entry.target.translated()
    var showRefs by remember { mutableStateOf(true) }
    var showDraft by remember { mutableStateOf(true) }

    LaunchedEffect(entry) {
        editedText = entry.target.translated()
    }

    Column(
        modifier = modifier
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

        Row(modifier = Modifier.fillMaxWidth()) {
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
            entry.templateText, type = EntryTagType.Templated,
            onCopyToClipboard = { text ->
                val textToCopy = text.ifBlank {
                    var t = "msgctxt \"${entry.target.key()}\"\n"
                    t += "msgid \"${entry.templateText}\"\n"
                    t += "msgstr \"${entry.target.translated()}\""
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
            AlertRegexLinkSelector(links, onSelected = {
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
private fun M3EditorPanePreviewReferenceView() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        arrayOf(false, true).forEach { darkTheme ->
            OniTranslatorTheme(darkTheme) {
                Surface {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ReferenceView(
                            "template text <link=\\\"DATABANK\\\">Data Banks</link>",
                            EntryTagType.Templated,
                            onCopyToClipboard = {})
                        ReferenceView("simplified text", EntryTagType.Simplified, onReplace = {})
                        ReferenceView("draft text", EntryTagType.Translated, onReplace = {})
                    }
                }
            }
        }
    }
}

@Composable
private fun SimpleSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    type: EntryTagType,
    modifier: Modifier = Modifier
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

@Preview
@Composable
private fun M3EditorPanePreviewSimpleSwitch() {
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
private fun AlertRegexLinkSelector(links: Sequence<MatchResult>, onSelected: (String) -> Unit) {
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
private fun M3EditorPanePreviewLinkSelector() {
    val text = "Sample <link=\\\"DATABANK\\\">Data Banks</link> and <link=\\\"MAP\\\">Map</link>"
    val regex = Regex("<link=([^>]+)>([^<]+)</link>")
    val links = regex.findAll(text)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OniTranslatorTheme(darkTheme = false) {
            Surface {
                AlertRegexLinkSelector(links = links, onSelected = {})
            }
        }
        OniTranslatorTheme(darkTheme = true) {
            Surface {
                AlertRegexLinkSelector(links = links, onSelected = {})
            }
        }
    }
}
