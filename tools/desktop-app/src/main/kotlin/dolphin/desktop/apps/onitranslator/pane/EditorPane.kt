package dolphin.desktop.apps.onitranslator.pane

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.EditOff
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_copy_this_text
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_show_link
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_use_this_text
import dolphin.desktop.apps.onitranslator.model.EditorData
import dolphin.desktop.apps.onitranslator.model.EntryTagType
import dolphin.desktop.apps.onitranslator.model.WordEntry
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorM3Theme
import dolphin.desktop.apps.onitranslator.widget.TooltipIconButton
import org.jetbrains.compose.resources.stringResource

@Composable
fun EditorPane(
    entry: EditorData?,
    modifier: Modifier = Modifier,
    onCopyToClipboard: (String) -> Unit = {},
    onConvert: (String) -> String = { it },
    onSave: (WordEntry, String) -> Unit = { _, _ -> },
    onCancel: () -> Unit = {},
) {
    if (entry == null) {
        Column(
            modifier = modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Rounded.EditOff,
                contentDescription = null,
                modifier = Modifier.size(128.dp),
                tint = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.25f)
            )
        }
    } else {
        var editedText by remember { mutableStateOf(entry.target.translated()) }
        val isChanged = editedText != entry.target.translated()

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

            // Reference Section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                ReferenceView(
                    entry.templateText, type = EntryTagType.Templated,
                    onCopyToClipboard = { text ->
                        if (text.isNotBlank()) {
                            onCopyToClipboard.invoke("")
                        } else {
                            var text = "msgctxt \"${entry.target.key()}\"\n"
                            text += "msgid \"${entry.templateText}\"\n"
                            text += "msgstr \"${entry.target.translated()}\""
                            onCopyToClipboard.invoke(text)
                        }
                    }
                )
                entry.referenceText?.let { text ->
                    if (text.isNotBlank()) {
                        ReferenceView(
                            text,
                            type = EntryTagType.Simplified,
                            onReplace = { editedText = onConvert(text) },
                        )
                    }
                }
                entry.draftText?.let { text ->
                    if (text.isNotBlank()) {
                        ReferenceView(
                            text,
                            type = EntryTagType.Translated,
                            onReplace = { editedText = onConvert(text) },
                        )
                    }
                }
            }

            OutlinedTextField(
                value = editedText,
                onValueChange = { editedText = it },
                label = { Text("Translated Text") },
                modifier = Modifier.fillMaxWidth().weight(1f),
                minLines = 5,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = { onSave(entry.target, editedText) },
                    enabled = isChanged,
                ) {
                    Text("Apply")
                }
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
        shape = MaterialTheme.shapes.small,
        color = type.containerColor(MaterialTheme.colorScheme),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = type.contentColor(MaterialTheme.colorScheme),
                modifier = Modifier.weight(1f),
            )

            onCopyToClipboard?.let { listener ->
                TooltipIconButton(
                    icon = Icons.Rounded.ContentCopy,
                    tooltip = stringResource(Res.string.tooltip_copy_this_text),
                    onClick = { listener.invoke("") },
                )
                if (links.count() > 0) {
                    TooltipIconButton(
                        icon = Icons.Rounded.Link,
                        tooltip = stringResource(Res.string.tooltip_show_link),
                        onClick = { linkSelector = !linkSelector }
                    )
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

@Composable
private fun AlertRegexLinkSelector(links: Sequence<MatchResult>, onSelected: (String) -> Unit) {
    Column(modifier = Modifier.background(Color.White).padding(16.dp)) {
        links.forEach {
            val link = it.groupValues[1].substring(2, it.groupValues[1].length - 2)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { onSelected(link) }) {
                    Text(link)
                }
                IconButton(onClick = { onSelected("<link=\\\"${link}\\\"</link>") }) {
                    Icon(Icons.Rounded.Link, contentDescription = null)
                }
            }
        }
    }
}

@Preview
@Composable
private fun M3EditorPanePreviewEmpty() {
    Column {
        arrayOf(false, true).forEach { darkTheme ->
            OniTranslatorM3Theme(darkTheme) {
                Surface {
                    EditorPane(entry = null, modifier = Modifier.height(240.dp))
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

    // Light Theme Preview
    OniTranslatorM3Theme(darkTheme = false) {
        Surface {
            EditorPane(entry = data)
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
    val data = EditorData(sampleEntry, "sample text")

    // Dark Theme Preview
    OniTranslatorM3Theme(darkTheme = true) {
        Surface {
            EditorPane(entry = data)
        }
    }
}

@Preview
@Composable
private fun M3EditorPanePreviewReferenceView() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        arrayOf(false, true).forEach { darkTheme ->
            OniTranslatorM3Theme(darkTheme) {
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
