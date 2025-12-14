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
    Pair(AppStrings.toolbar_template_text, AppTheme.AppColor.purple),
    Pair(AppStrings.toolbar_simplified_text, AppTheme.AppColor.blue),
    Pair(AppStrings.toolbar_old_translated, AppTheme.AppColor.orange),
    Pair(AppStrings.toolbar_now_translated, AppTheme.AppColor.green),
)

interface ToolbarCallback {
    fun onRefresh()
    fun onSave()
    fun onSearch()
}

data class ToolbarSpec(var enabled: Boolean = true)

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
        ToolbarIconButton(
            Icons.Rounded.Refresh,
            onClick = { callback?.onRefresh() },
            enabled = spec.enabled,
            contentDescription = AppStrings.button_refresh
        )
        ToolbarIconButton(
            Icons.Rounded.Search,
            onClick = { callback?.onSearch() },
            enabled = spec.enabled,
            contentDescription = AppStrings.button_search
        )
        ToolbarIconButton(
            Icons.Rounded.Save,
            onClick = { callback?.onSave() },
            enabled = spec.enabled,
            contentDescription = AppStrings.button_save
        )
    }
}

@Composable
private fun ToolbarIconButton(
    imageVector: ImageVector,
    onClick: () -> Unit,
    enabled: Boolean = true,
    contentDescription: String? = null
) {
    contentDescription?.let { tooltip ->
        TooltipIconButton(onClick = onClick, enabled = enabled, tooltip = tooltip) {
            Icon(
                imageVector,
                contentDescription = null,
                tint = if (enabled) Color.LightGray else Color.DarkGray
            )
        }
    } ?: run {
        IconButton(onClick = onClick, enabled = enabled) {
            Icon(
                imageVector,
                contentDescription = contentDescription,
                tint = if (enabled) Color.LightGray else Color.DarkGray
            )
        }
    }

}

@Preview
@Composable
private fun PreviewToolbarPane() {
    OniTranslatorTheme {
        ToolbarPane(filteredList = listOf(WordEntry.default()), changedList = listOf(0))
    }
}
