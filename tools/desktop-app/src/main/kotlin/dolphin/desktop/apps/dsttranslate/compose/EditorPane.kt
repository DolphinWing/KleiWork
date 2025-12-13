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
import dolphin.android.apps.dsttranslate.PoHelper
import dolphin.android.apps.dsttranslate.WordEntry
import dolphin.android.apps.dsttranslate.WordEntry.Companion.dropQuote
import dolphin.desktop.apps.dsttranslate.AppStrings

private fun Color.tinted(visible: Boolean): Color = copy(alpha = if (visible) 1f else .25f)

data class EditorSpec(
    val target: WordEntry = WordEntry.default(),
    val dst: WordEntry? = null,
    val chs: String? = null,
    val cht: String? = null,
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
    mode: PoHelper.Mode = PoHelper.Mode.ONI,
) {
    var text by remember { mutableStateOf(data.target.string()) }
    var nowVisible by remember { mutableStateOf(true) }
    var dstVisible by remember { mutableStateOf(true) }
    var chsVisible by remember { mutableStateOf(true) }
    var chtVisible by remember { mutableStateOf(true) }
    var linkSelector by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .background(Color.White)
            // .verticalScroll(rememberScrollState())
            .padding(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                data.target.key(),
                modifier = Modifier.weight(1f),
                overflow = TextOverflow.Ellipsis,
            )
            IconButton(onClick = { nowVisible = !nowVisible }) {
                Icon(
                    Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = AppTheme.AppColor.green.tinted(nowVisible),
                )
            }
            if (mode == PoHelper.Mode.DST) {
                data.dst?.let { // new item has no previous for reference, need to check source
                    TooltipIconButton(
                        onClick = { dstVisible = !dstVisible },
                        tooltip = AppStrings.tooltip_source_text
                    ) {
                        Icon(
                            Icons.Rounded.Visibility,
                            contentDescription = null,
                            tint = AppTheme.AppColor.orange.tinted(dstVisible)
                        )
                    }
                }
            }
            TooltipIconButton(
                onClick = { chsVisible = !chsVisible },
                tooltip = AppStrings.tooltip_now_text
            ) {
                Icon(
                    Icons.Rounded.Visibility,
                    contentDescription = null,
                    tint = AppTheme.AppColor.blue.tinted(chsVisible)
                )
            }
            if (mode == PoHelper.Mode.DST) {
                TooltipIconButton(
                    onClick = { chtVisible = !chtVisible },
                    tooltip = AppStrings.tooltip_traditional_text
                ) {
                    Icon(
                        Icons.Rounded.Visibility,
                        contentDescription = null,
                        tint = AppTheme.AppColor.purple.tinted(chtVisible)
                    )
                }
            }
        }

        if (chsVisible) {
            TooltipButton(
                onClick = { text = data.chs?.dropQuote() ?: "" },
                tooltip = AppStrings.tooltip_use_this_text,
                modifier = Modifier.fillMaxWidth(),
                enabled = data.chs?.isNotEmpty() == true,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AppTheme.AppColor.blue,
                ),
            ) {
                Text(data.chs?.dropQuote() ?: "", fontSize = AppTheme.largerFontSize())
            }
        }

        if (mode == PoHelper.Mode.DST && chtVisible) {
            TooltipButton(
                onClick = { text = data.cht?.dropQuote() ?: "" },
                tooltip = AppStrings.tooltip_use_this_text,
                modifier = Modifier.fillMaxWidth(),
                enabled = data.cht?.isNotEmpty() == true,
                colors = ButtonDefaults.buttonColors(
                    contentColor = Color.White,
                    backgroundColor = AppTheme.AppColor.purple,
                ),
            ) {
                Text(data.cht?.dropQuote() ?: "", fontSize = AppTheme.largerFontSize())
            }
        }

        if (mode == PoHelper.Mode.DST) {
            data.dst?.let { old ->
                if (dstVisible) {
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TooltipTextButton(
                            onClick = { onCopyToClipboard?.invoke(old.origin()) },
                            tooltip = AppStrings.tooltip_copy_original_text,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = AppTheme.AppColor.orange,
                            ),
                        ) {
                            Text(
                                old.origin(),
                                modifier = Modifier.fillMaxWidth(),
                                fontSize = AppTheme.largerFontSize(),
                            )
                        }
                        TooltipIconButton(
                            onClick = { onTranslate?.invoke(old.origin()) },
                            tooltip = AppStrings.tooltip_send_to_google_translate,
                        ) {
                            Icon(Icons.Rounded.Translate, contentDescription = AppStrings.content_description_translate)
                        }
                    }
                    TooltipButton(
                        onClick = { text = old.string() },
                        tooltip = AppStrings.tooltip_use_this_text,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = AppTheme.AppColor.orange,
                        ),
                    ) {
                        Text(old.string(), fontSize = AppTheme.largerFontSize())
                    }
                }
            }
        }

        if (nowVisible) {
            // use regex to find link content
            // sample: <link=\"DATABANK\">Data Banks</link>
            val regex = Regex("<link=([^>]+)>([^<]+)</link>")
            val links = regex.findAll(data.target.origin())
            println(data.target.origin())
            if (links.count() == 0) {
                println("  No link found.")
            } else {
                links.forEach {
                    println("  ${it.groupValues[1]}: ${it.groupValues[2]}")
                }
            }

            if (linkSelector) {
                Dialog(onDismissRequest = { linkSelector = false }) {
                    AlertRegexLinkSelector(links, onSelected = {
                        onCopyToClipboard?.invoke(it)
                        linkSelector = false
                    })
                }
            }

            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TooltipTextButton(
                    onClick = {
                        var text = "msgctxt \"${data.target.key()}\"\n"
                        text += "msgid \"${data.target.origin()}\"\n"
                        text += "msgstr \"${data.target.string()}\""
                        onCopyToClipboard?.invoke(text)
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
                    onClick = { onCopyToClipboard?.invoke(data.target.origin()) },
                    tooltip = AppStrings.tooltip_copy_original_text
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null)
                }
                TooltipIconButton(
                    onClick = { onTranslate?.invoke(data.target.origin()) },
                    tooltip = AppStrings.tooltip_send_to_google_translate
                ) {
                    Icon(Icons.Rounded.Translate, contentDescription = null)
                }
                TooltipIconButton(
                    onClick = { linkSelector = true },
                    tooltip = AppStrings.tooltip_show_link,
                    enabled = links.count() > 0
                ) {
                    Icon(Icons.Rounded.TextFields, contentDescription = null)
                }
            }
            TooltipButton(
                onClick = { text = data.target.string() },
                tooltip = AppStrings.tooltip_use_this_text,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = AppTheme.AppColor.green),
            ) {
                Text(data.target.string(), fontSize = AppTheme.largerFontSize())
            }
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

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TooltipIconButton(
                onClick = { onCopyToClipboard?.invoke(text) },
                tooltip = AppStrings.tooltip_copy_all
            ) {
                Icon(Icons.Rounded.CopyAll, contentDescription = null)
            }
            TooltipIconButton(
                onClick = { onCopyFromClipboard?.invoke()?.let { result -> text = result } },
                tooltip = AppStrings.tooltip_paste_all
            ) {
                Icon(Icons.Rounded.ContentPaste, contentDescription = null)
            }
            Spacer(modifier = Modifier.requiredWidth(16.dp))
            Button(
                onClick = { onSave?.invoke(data.target.key, "\"$text\"") },
                modifier = Modifier.weight(1f),
            ) {
                Text(AppStrings.button_apply)
            }
            Spacer(modifier = Modifier.requiredWidth(16.dp))
            TextButton(onClick = { onCancel?.invoke() }) {
                Text(AppStrings.button_cancel)
            }
        }
    }
}

@Preview
@Composable
private fun PreviewEditorPaneTargetOnly() {
    DstTranslatorTheme {
        EditorPane(data = EditorSpec())
    }
}

@Preview
@Composable
private fun PreviewEditorPaneWithOriginal() {
    DstTranslatorTheme {
        EditorPane(data = EditorSpec(dst = PreviewDefaults.dst))
    }
}

@Preview
@Composable
private fun PreviewEditorPaneWithChs() {
    DstTranslatorTheme {
        EditorPane(data = EditorSpec(dst = PreviewDefaults.dst, chs = "simplified"))
    }
}

@Preview
@Composable
private fun PreviewEditorPaneWithCht() {
    DstTranslatorTheme {
        EditorPane(
            data = EditorSpec(
                dst = PreviewDefaults.dst,
                chs = "simplified",
                cht = "traditional",
            )
        )
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
    val links =
        regex.findAll("The Moo Biome is the natural habitat of the charismatic <link=\\\"MOO\\\">Gassy Moo</link>, a great source of <link=\\\"METHANE\\\">Natural Gas</link>.")

    DstTranslatorTheme {
        AlertRegexLinkSelector(
            links,
            onSelected = { println(it) }
        )
    }
}
