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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dolphin.android.apps.dsttranslate.PoHelper
import dolphin.android.apps.dsttranslate.WordEntry
import dolphin.desktop.apps.dsttranslate.AppStrings
import dolphin.desktop.apps.dsttranslate.PoDataModel

@Composable
fun EntryListPane(
    model: PoDataModel,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    callback: ToolbarCallback? = null,
    spec: ToolbarSpec = ToolbarSpec(),
    onEdit: ((WordEntry) -> Unit)? = null,
) {
    val dataList = model.filteredList.collectAsState()
    val changedList = model.changedList.collectAsState()

    Column(modifier = modifier) {
        ToolbarPane(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            filteredList = dataList.value,
            changedList = changedList.value,
            callback = callback,
            spec = spec,
        )
        if (spec.enabled && (dataList.value.isEmpty())) {
            Text(AppStrings.content_no_items, modifier = Modifier.padding(8.dp), textAlign = TextAlign.Center)
        }
        if (!spec.enabled) {
            Spacer(modifier = Modifier.requiredHeight(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator()
            }
            Spacer(modifier = Modifier.requiredHeight(16.dp))
        }
        LazyScrollableColumn(dataList.value, modifier = Modifier.weight(1f), state = state) { index, entry ->
            // val dst = remember { helper.dst(entry.key) }
            // val chs = remember { helper.chs(entry.key) }
            // val cht = remember { helper.cht(entry.key) }
            EntryView(
                origin = entry,
                modifier = Modifier.fillMaxWidth(),
                dst = model.helper.dst(entry.key), // dst,
                chs = model.helper.chs(entry.key), // chs,
                cht = model.helper.cht(entry.key), // cht,
                onItemClick = { item -> onEdit?.invoke(item) },
                index = index,
                changed = changedList.value[index],
                mode = model.appMode.value,
            )
        }
    }
}

@Composable
fun EntryView(
    origin: WordEntry,
    modifier: Modifier = Modifier,
    dst: WordEntry? = null,
    chs: WordEntry? = null,
    cht: WordEntry? = null,
    onItemClick: ((WordEntry) -> Unit)? = null,
    index: Int = 0,
    changed: Long = 0L,
    mode: PoHelper.Mode = PoHelper.Mode.DST,
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
        chs?.let { source ->
            Text(source.origin(), fontSize = AppTheme.largerFontSize())
        }
        if (cht?.string()?.isNotEmpty() == true) {
            Text(
                // if (dst?.id == origin.id) dst.string() else dst?.origin() ?: origin.origin(),
                cht.string(),
                color = AppTheme.AppColor.purple,
                fontSize = AppTheme.largerFontSize(),
            )
        } else if (mode == PoHelper.Mode.ONI) {
            Text(
                cht?.origin() ?: origin.origin(),
                color = AppTheme.AppColor.purple,
                fontSize = AppTheme.largerFontSize(),
            )
        }
        if (dst?.string()?.isNotEmpty() == true) {
            Text(
                dst.string(),
                color = AppTheme.AppColor.orange,
                fontSize = AppTheme.largerFontSize(),
            )
        }
        Text(
            origin.string(),
            color = AppTheme.AppColor.green,
            fontSize = AppTheme.largerFontSize(),
        )
    }
}

@Preview
@Composable
private fun PreviewEntryViewOrigin() {
    DstTranslatorTheme {
        EntryView(origin = WordEntry.default())
    }
}

@Preview
@Composable
private fun PreviewEntryViewAll() {
    DstTranslatorTheme {
        EntryView(
            origin = WordEntry.default(),
            dst = PreviewDefaults.dst,
            chs = PreviewDefaults.chs,
            cht = PreviewDefaults.cht,
        )
    }
}
