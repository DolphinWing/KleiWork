package dolphin.desktop.apps.dsttranslate.compose

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dolphin.android.apps.dsttranslate.WordEntry
import dolphin.desktop.apps.dsttranslate.AppStrings

private val textMap = listOf(
    Pair(AppStrings.toolbar_old_text, AppTheme.AppColor.blue),
    Pair(AppStrings.toolbar_now_text, AppTheme.AppColor.purple),
    Pair(AppStrings.toolbar_old, AppTheme.AppColor.orange),
    Pair(AppStrings.toolbar_now, AppTheme.AppColor.green),
)

interface ToolbarCallback {
    fun onRefresh()
    fun onSave()
    fun onSearch()
    fun onAnalyze()
}

data class ToolbarSpec(var enabled: Boolean = true, var enableAnalyze: Boolean = false)

@Composable
fun ToolbarPane(
    modifier: Modifier = Modifier,
    filteredList: List<WordEntry>? = null,
    changedList: List<Long>? = null,
    callback: ToolbarCallback? = null,
    spec: ToolbarSpec = ToolbarSpec(),
) {
    Row(
        modifier = modifier
            .background(MaterialTheme.colors.primary)
            .padding(vertical = 4.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val changed = changedList?.filter { it > 0L } ?: arrayListOf()
        Text(
            AppStrings.toolbar_status(filteredList?.size ?: 0, changed.size),
            modifier = Modifier.weight(1f),
            fontSize = AppTheme.largerFontSize(),
            color = MaterialTheme.colors.onPrimary,
        )
        if (spec.enableAnalyze) {
            ToolbarIconButton(Icons.Rounded.Analytics, onClick = { callback?.onAnalyze() }, enabled = spec.enabled)
            Spacer(modifier = Modifier.requiredWidth(8.dp))
        }
        textMap.forEach { (title, color) ->
            Text(
                text = title,
                modifier = Modifier.background(color).padding(vertical = 4.dp, horizontal = 8.dp),
                fontSize = AppTheme.largerFontSize(),
                // fontFamily = FontFamily.Monospace,
                textAlign = TextAlign.Center,
            )
        }
        Spacer(modifier = Modifier.requiredWidth(8.dp))
        ToolbarIconButton(Icons.Rounded.Refresh, onClick = { callback?.onRefresh() }, enabled = spec.enabled)
        ToolbarIconButton(Icons.Rounded.Search, onClick = { callback?.onSearch() }, enabled = spec.enabled)
        ToolbarIconButton(Icons.Rounded.Save, onClick = { callback?.onSave() }, enabled = spec.enabled)
    }
}

@Composable
private fun ToolbarIconButton(imageVector: ImageVector, onClick: () -> Unit, enabled: Boolean = true) {
    IconButton(onClick = onClick, enabled = enabled) {
        Icon(imageVector, contentDescription = null, tint = if (enabled) Color.LightGray else Color.DarkGray)
    }
}

@Preview
@Composable
private fun PreviewToolbarPane() {
    DstTranslatorTheme {
        ToolbarPane(filteredList = listOf(WordEntry.default()), changedList = listOf(0))
    }
}
