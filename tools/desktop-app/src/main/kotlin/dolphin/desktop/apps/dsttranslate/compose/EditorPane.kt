package dolphin.desktop.apps.dsttranslate.compose

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ContentPaste
import androidx.compose.material.icons.rounded.CopyAll
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import dolphin.android.apps.dsttranslate.WordEntry
import dolphin.android.apps.dsttranslate.WordEntry.Companion.dropQuote
import dolphin.desktop.apps.dsttranslate.AppStrings

private fun Color.tinted(visible: Boolean): Color = copy(alpha = if (visible) 1f else .25f)

data class EditorSpec(
    val target: WordEntry = WordEntry.default(),
    val simplifiedToTraditional: String? = null,
    val templateContent: String? = null,
)

@Composable
fun EditorPane(
    data: EditorSpec,
    modifier: Modifier = Modifier,
    onSave: ((String, String) -> Unit)? = null,
    onCopyToClipboard: ((String) -> Unit)? = null,
    onTranslate: ((String) -> Unit)? = null,
    onCopyFromClipboard: (() -> String)? = null,
    onCancel: (() -> Unit)? = null,
) {
    var text by remember { mutableStateOf(data.target.translated()) }
    var nowVisible by remember { mutableStateOf(true) }
    var chsVisible by remember { mutableStateOf(data.simplifiedToTraditional?.isNotEmpty() == true) }

    Column(
        modifier = modifier
            .background(Color.White)
            // .verticalScroll(rememberScrollState())
            .padding(8.dp),
    ) {
        EditorTopBar(
            title = data.target.key(),
            modifier = Modifier.fillMaxWidth(),
            currentViewVisible = nowVisible,
            onToggleCurrentView = { nowVisible = !nowVisible },
            referenceViewVisible = chsVisible,
            onToggleReferenceView = { chsVisible = !chsVisible },
        )

        if (chsVisible) {
            TooltipButton(
                onClick = { text = data.simplifiedToTraditional?.dropQuote() ?: "" },
                tooltip = AppStrings.tooltip_use_this_text,
                modifier = Modifier.fillMaxWidth(),
                enabled = data.simplifiedToTraditional?.isNotEmpty() == true,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AppTheme.AppColor.blue,
                ),
            ) {
                Text(data.simplifiedToTraditional?.dropQuote() ?: "", fontSize = AppTheme.largerFontSize())
            }
        }

        if (nowVisible) {
            EditorTemplateContent(
                data = data,
                modifier = Modifier.fillMaxWidth(),
                onCopyToClipboard = { content -> onCopyToClipboard?.invoke(content) },
                onTranslate = onTranslate,
                onReplace = { text = data.target.translated() }
            )
        }

        TextField(
            value = text,
            onValueChange = { str -> text = str },
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(vertical = 8.dp),
            textStyle = TextStyle.Default.copy(fontSize = AppTheme.largerFontSize()),
            // singleLine = true,
        )

        EditorBottomBar(
            modifier = Modifier.fillMaxWidth(),
            onCopyToClipboard = { onCopyToClipboard?.invoke(text) },
            onCopyFromClipboard = { onCopyFromClipboard?.invoke()?.let { result -> text = result } },
            onCancel = { onCancel?.invoke() },
            onSave = { onSave?.invoke(data.target.key, "\"$text\"") },
        )
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    modifier: Modifier = Modifier,
    currentViewVisible: Boolean = true,
    onToggleCurrentView: () -> Unit = {},
    referenceViewVisible: Boolean = true,
    onToggleReferenceView: () -> Unit = {},
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            modifier = Modifier.weight(1f),
            overflow = TextOverflow.Ellipsis,
        )

        TooltipIconButton(
            onClick = onToggleReferenceView,
            tooltip = AppStrings.tooltip_simplified_chinese_text
        ) {
            Icon(
                Icons.Rounded.Visibility,
                contentDescription = null,
                tint = AppTheme.AppColor.blue.tinted(referenceViewVisible)
            )
        }

        TooltipIconButton(
            onClick = onToggleCurrentView,
            tooltip = AppStrings.tooltip_now_text
        ) {
            Icon(
                Icons.Rounded.Visibility,
                contentDescription = null,
                tint = AppTheme.AppColor.green.tinted(currentViewVisible),
            )
        }
    }
}

@Composable
private fun EditorTemplateContent(
    data: EditorSpec,
    modifier: Modifier = Modifier,
    onCopyToClipboard: (String) -> Unit = {},
    onTranslate: ((String) -> Unit)? = null,
    onReplace: () -> Unit = {},
) {
    var linkSelector by remember { mutableStateOf(false) }

    // use regex to find link content
    // sample: <link=\"DATABANK\">Data Banks</link>
    val regex = Regex("<link=([^>]+)>([^<]+)</link>")
    val links = regex.findAll(data.target.origin())
//    println(data.target.origin())
//    if (links.count() == 0) {
//        println("  No link found.")
//    } else {
//        links.forEach {
//            println("  ${it.groupValues[1]}: ${it.groupValues[2]}")
//        }
//    }

    if (linkSelector) {
        Dialog(onDismissRequest = { linkSelector = false }) {
            AlertRegexLinkSelector(links, onSelected = {
                onCopyToClipboard.invoke(it)
                linkSelector = false
            })
        }
    }

    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TooltipTextButton(
            onClick = {
                var text = "msgctxt \"${data.target.key()}\"\n"
                text += "msgid \"${data.target.origin()}\"\n"
                text += "msgstr \"${data.target.translated()}\""
                onCopyToClipboard.invoke(text)
            },
            tooltip = AppStrings.tooltip_copy_this_text,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.textButtonColors(
                contentColor = AppTheme.AppColor.green,
            ),
        ) {
            Text(
                data.target.origin(),
                modifier = Modifier.fillMaxWidth(),
                fontSize = AppTheme.largerFontSize(),
            )
        }
        TooltipIconButton(
            onClick = onReplace,
            tooltip = data.target.translated(),
        ) {
            Icon(
                Icons.Rounded.TextFields,
                contentDescription = AppStrings.tooltip_use_this_text,
                tint = AppTheme.AppColor.green
            )
        }
        TooltipIconButton(
            onClick = { onCopyToClipboard.invoke(data.target.origin()) },
            tooltip = AppStrings.tooltip_copy_original_text
        ) {
            Icon(Icons.Rounded.ContentCopy, contentDescription = null)
        }
        onTranslate?.let { listener ->
            TooltipIconButton(
                onClick = { listener.invoke(data.target.origin()) },
                tooltip = AppStrings.tooltip_send_to_google_translate
            ) {
                Icon(Icons.Rounded.Translate, contentDescription = null)
            }
        }
        TooltipIconButton(
            onClick = { linkSelector = true },
            tooltip = AppStrings.tooltip_show_link,
            enabled = links.count() > 0
        ) {
            Icon(Icons.Rounded.TextFields, contentDescription = null)
        }
    }
}

@Composable
private fun EditorBottomBar(
    modifier: Modifier = Modifier,
    onCopyToClipboard: () -> Unit = {},
    onCopyFromClipboard: () -> Unit = { },
    onCancel: () -> Unit = {},
    onSave: () -> Unit = { },
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        TooltipIconButton(
            onClick = onCopyToClipboard,
            tooltip = AppStrings.tooltip_copy_all
        ) {
            Icon(Icons.Rounded.CopyAll, contentDescription = null)
        }
        TooltipIconButton(
            onClick = onCopyFromClipboard,
            tooltip = AppStrings.tooltip_paste_all
        ) {
            Icon(Icons.Rounded.ContentPaste, contentDescription = null)
        }
        Spacer(modifier = Modifier.requiredWidth(16.dp))
        Button(
            onClick = onSave,
            modifier = Modifier.weight(1f),
        ) {
            Text(AppStrings.button_apply)
        }
        Spacer(modifier = Modifier.requiredWidth(16.dp))
        TextButton(onClick = onCancel) {
            Text(AppStrings.button_cancel)
        }
    }
}

@Preview
@Composable
private fun PreviewEditorPaneTargetOnly() {
    OniTranslatorTheme {
        EditorPane(data = EditorSpec())
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

@Composable
@Preview
private fun PreviewAlertRegexLinkSelector() {
    val regex = Regex("<link=([^>]+)>([^<]+)</link>")
    val sampleText = "The Moo Biome is the natural habitat of the charismatic <link=\\\"MOO\\\">Gassy Moo</link>," +
            " a great source of <link=\\\"METHANE\\\">Natural Gas</link>."
    val links = regex.findAll(sampleText)

    OniTranslatorTheme {
        AlertRegexLinkSelector(
            links,
            onSelected = { println(it) }
        )
    }
}
