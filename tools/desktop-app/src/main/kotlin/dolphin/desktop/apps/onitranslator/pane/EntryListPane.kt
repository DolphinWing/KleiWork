package dolphin.desktop.apps.onitranslator.pane

// Import the new M3ToolbarPane
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.app.AppEvent
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.button_refresh
import dolphin.desktop.apps.onitranslator.model.AppState
import dolphin.desktop.apps.onitranslator.model.EditorData
import dolphin.desktop.apps.onitranslator.model.EntryTagType
import dolphin.desktop.apps.onitranslator.model.WordEntry
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme
import dolphin.desktop.apps.onitranslator.widget.TextTag
import dolphin.desktop.apps.onitranslator.widget.TooltipIconButton
import dolphin.desktop.apps.onitranslator.widget.shimmerEffect
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max

@Composable
fun EntryListPane(
    state: AppState,
    onEvent: (AppEvent) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val list by remember(state.uiState.searchState.text, state.uiState.searchState.results, state.filteredList) {
        mutableStateOf(if (state.uiState.searchState.text.isBlank()) state.filteredList else state.uiState.searchState.results)
    }

    Surface(modifier = modifier, color = MaterialTheme.colorScheme.background) {
        if (state.uiState.isLoading && state.uiState.searchState.text.isBlank()) {
            // Show Skeleton Screen only when not searching
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(10) {
                    EntryViewPlaceholder()
                }
            }
        } else if (list.isEmpty()) {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TooltipIconButton(
                    icon = Icons.Rounded.Refresh,
                    tooltip = stringResource(Res.string.button_refresh),
                ) {
                    onEvent(AppEvent.File.RefreshSource)
                }

            }
        } else {
            // Show actual list
            LazyColumn(
                modifier = Modifier.fillMaxSize()
                    .drawOrganicScrollbar(listState, MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                state = listState
            ) {
                itemsIndexed(list) { index, entry ->
                    val isSearchMode = state.uiState.searchState.text.isNotBlank()
                    val originalIndex = if (isSearchMode) -1 else index
                    val changedValue = if (isSearchMode) 0L else state.changedList.getOrNull(index) ?: 0L
                    val selected = state.uiState.editorData == entry
                    M3EntryView(
                        data = entry,
                        onItemClick = { onEvent(AppEvent.Editor.Select(it)) },
                        index = originalIndex, // Pass original index or -1 if not available
                        changed = changedValue,
                        selected = selected,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun M3EntryView(
    data: EditorData,
    modifier: Modifier = Modifier,
    onItemClick: (WordEntry) -> Unit = {},
    index: Int = 0,
    changed: Long = 0L,
    selected: Boolean = false,
) {
    val isChanged = changed > 0
    val cardBorderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else if (isChanged) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    }
    val cardBorderWidth = if (selected) 2.dp else if (isChanged) 1.5.dp else 1.dp

    val cardColors = if (selected) {
        CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    } else {
        CardDefaults.outlinedCardColors()
    }

    OutlinedCard(
        onClick = { onItemClick.invoke(data.target) },
        modifier = modifier.fillMaxWidth(),
        border = BorderStroke(cardBorderWidth, cardBorderColor),
        colors = cardColors,
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
            // Header: Key and Index
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = data.target.key(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace,
                )
                if (data.target.newly) {
                    Text(
                        text = "NEW", // A more explicit "NEW" tag
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
                Text((index + 1).toString(), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(8.dp))

            // Main translated text - give it prominence
            Text(
                text = data.target.translated(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
            )

            Spacer(Modifier.height(8.dp))

            // Tags for other versions
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Templated
                TextTag(tagType = EntryTagType.Templated, text = data.templateText)
                // Simplified
                data.referenceText?.let {
                    TextTag(tagType = EntryTagType.Simplified, text = it)
                }
                // Old translated
                data.draftText?.let {
                    TextTag(tagType = EntryTagType.Translated, text = it)
                }
            }
        }
    }
}

@Composable
private fun EntryViewPlaceholder(modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
            // Header
            Spacer(modifier = Modifier.height(4.dp))
            Box(modifier = Modifier.fillMaxWidth(0.7f).height(12.dp).shimmerEffect())
            Spacer(modifier = Modifier.height(8.dp))
            // Body lines
            Box(modifier = Modifier.fillMaxWidth(0.9f).height(20.dp).shimmerEffect())
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.width(100.dp).height(24.dp).shimmerEffect())
                Box(modifier = Modifier.width(80.dp).height(24.dp).shimmerEffect())
            }
            Spacer(modifier = Modifier.height(4.dp))
        }
    }
}

// Previews for M3EntryListPane components
@Preview
@Composable
private fun M3EntryViewPreview() {
    val sampleEntry = WordEntry("STRINGS.UI.PREVIEW.KEY", "The quick brown fox jumps over the lazy dog.")
    val defaultViewData = EditorData(
        target = sampleEntry.copy(newly = false),
        templateText = "Template text <placeholde r>",
        referenceText = "Simplified",
        draftText = "Translated text that might be a bit long."
    )
    val newEntryViewData = defaultViewData.copy(target = sampleEntry, draftText = null, referenceText = null)
    var index = 0

    Row(modifier = Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        arrayOf(false, true).forEach { darkTheme ->
            OniTranslatorTheme(darkTheme = darkTheme) {
                Surface(color = MaterialTheme.colorScheme.surfaceDim) {
                    Column(Modifier.width(360.dp).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        M3EntryView(data = defaultViewData, index = ++index)
                        M3EntryView(data = newEntryViewData, index = ++index, changed = 1L)
                        M3EntryView(data = defaultViewData, index = ++index, selected = true)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun EntryViewPlaceholderPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        OniTranslatorTheme(darkTheme = false) {
            EntryViewPlaceholder()
            EntryViewPlaceholder()
        }
        OniTranslatorTheme(darkTheme = true) {
            EntryViewPlaceholder()
            EntryViewPlaceholder()
        }
    }
}

@Composable
fun Modifier.drawOrganicScrollbar(state: LazyListState, color: Color = Color.Gray): Modifier {
    // 1. 動態透明度：捲動時浮現，靜止時優雅消失
    val alpha by animateFloatAsState(
        targetValue = if (state.isScrollInProgress) 0.4f else 0f,
        animationSpec = if (state.isScrollInProgress) {
            snap() // 開始捲動瞬間出現
        } else {
            tween(durationMillis = 500, delayMillis = 200) // 結束後停留一下再消失
        },
        label = "scrollbar_alpha"
    )

    return this.drawWithContent {
        drawContent()

        val layoutInfo = state.layoutInfo
        val totalItemsCount = layoutInfo.totalItemsCount
        if (totalItemsCount <= 0) return@drawWithContent

        // 2. 計算比例：避免忽大忽小的物理修正
        val viewportHeight = size.height
        val visibleItemsCount = layoutInfo.visibleItemsInfo.size
        // 確保 Scrollbar 至少有 30.dp 高，不會縮到看不見
        val scrollbarHeight = max(
            (visibleItemsCount.toFloat() / totalItemsCount) * viewportHeight,
            30.dp.toPx()
        )

        val scrollOffset = if (totalItemsCount > visibleItemsCount) {
            (state.firstVisibleItemIndex.toFloat() / (totalItemsCount - visibleItemsCount)) * (viewportHeight - scrollbarHeight)
        } else 0f

        if (alpha > 0f) {
            drawRoundRect(
                color = color.copy(alpha = alpha),
                topLeft = Offset(size.width - 6.dp.toPx(), scrollOffset),
                size = Size(4.dp.toPx(), scrollbarHeight),
                cornerRadius = CornerRadius(2.dp.toPx())
            )
        }
    }
}
