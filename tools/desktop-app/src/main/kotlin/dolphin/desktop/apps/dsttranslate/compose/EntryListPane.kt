package dolphin.desktop.apps.dsttranslate.compose

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dolphin.android.apps.dsttranslate.WordEntry
import dolphin.desktop.apps.dsttranslate.PoDataModel
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.content_no_items
import org.jetbrains.compose.resources.stringResource

@Composable
fun EntryListPane(
    model: PoDataModel,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    callback: ToolbarCallback? = null,
    spec: ToolbarSpec = ToolbarSpec(),
    onEdit: ((WordEntry) -> Unit)? = null,
) {
    val dataList by model.filteredList.collectAsState()
    val changedList by model.changedList.collectAsState()

    Column(modifier = modifier) {
        ToolbarPane(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            filteredList = dataList,
            changedList = changedList,
            callback = callback,
            spec = spec,
        )
        if (spec.enabled && (dataList.isEmpty())) {
            Text(stringResource(Res.string.content_no_items), modifier = Modifier.padding(8.dp), textAlign = TextAlign.Center)
        }
        if (!spec.enabled) {
            Spacer(modifier = Modifier.requiredHeight(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            Spacer(modifier = Modifier.requiredHeight(16.dp))
        }
        LazyScrollableColumn(dataList, modifier = Modifier.weight(1f), state = state) { index, entry ->
            EntryView(
                origin = entry,
                modifier = Modifier.fillMaxWidth(),
                templated = model.helper.templated(entry.key),
                translated = model.helper.translated(entry.key),
                simplified = model.helper.simplified(entry.key),
                onItemClick = { item -> onEdit?.invoke(item) },
                index = index,
                changed = changedList[index],
            )
        }
    }
}

@Composable
fun EntryView(
    origin: WordEntry,
    modifier: Modifier = Modifier,
    translated: WordEntry? = null,
    simplified: WordEntry? = null,
    templated: WordEntry? = null,
    onItemClick: ((WordEntry) -> Unit)? = null,
    index: Int = 0,
    changed: Long = 0L,
) {
    // val entry by remember { mutableStateOf(origin) }
    val changedColor = if (changed > 0) Color.Gray else Color.LightGray
    // println("${origin.key()}: ${chs?.string()} ${cht?.string()} ${origin.string()}")

    Column(
        modifier = modifier
            // .background(if (changed > 0) Color.Yellow.copy(alpha = .1f) else Color.Transparent)
            .clickable(onClick = { onItemClick?.invoke(origin) })
            .padding(2.dp)
            .border(if (changed > 0) 2.dp else 1.dp, changedColor, RoundedCornerShape(4.dp))
            .padding(vertical = 2.dp, horizontal = 6.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                origin.key(),
                color = Color.DarkGray,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                fontSize = 14.sp, // AppTheme.fontSize(),
            )
            Text(
                (index + 1).toString(), // String.format("%05d", index),
                color = if (origin.newly) Color.Red else Color.LightGray,
                fontSize = 14.sp,
            )
        }
        Text(
            templated?.origin() ?: origin.origin(),
            color = AppTheme.AppColor.purple,
            fontSize = AppTheme.largerFontSize(),
        )

        simplified?.let { source ->
            Text(source.translated(), fontSize = AppTheme.largerFontSize(), color = AppTheme.AppColor.blue)
        }
        if (translated?.translated()?.isNotEmpty() == true) {
            Text(
                translated.translated(),
                color = AppTheme.AppColor.orange,
                fontSize = AppTheme.largerFontSize(),
            )
        }
        Text(
            origin.translated(),
            color = AppTheme.AppColor.green,
            fontSize = AppTheme.largerFontSize(),
        )
    }
}

@Preview
@Composable
private fun PreviewEntryViewOrigin() {
    OniTranslatorTheme {
        EntryView(origin = WordEntry.default())
    }
}

@Preview
@Composable
private fun PreviewEntryViewAll() {
    OniTranslatorTheme {
        EntryView(
            origin = WordEntry.default(),
            translated = PreviewDefaults.translated,
            simplified = PreviewDefaults.simplified,
            templated = PreviewDefaults.template,
        )
    }
}
