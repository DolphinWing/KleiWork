package dolphin.desktop.apps.onitranslator.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.AssignmentTurnedIn
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SensorWindow
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.app.AppEvent
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.button_refresh
import dolphin.desktop.apps.onitranslator.generated.resources.button_search
import dolphin.desktop.apps.onitranslator.generated.resources.content_description_tag_sensor_warning
import dolphin.desktop.apps.onitranslator.generated.resources.empty_list_message
import dolphin.desktop.apps.onitranslator.generated.resources.empty_list_title
import dolphin.desktop.apps.onitranslator.model.AppState
import dolphin.desktop.apps.onitranslator.model.EditorData
import dolphin.desktop.apps.onitranslator.model.EntryTagType
import dolphin.desktop.apps.onitranslator.model.PoEntry
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme
import dolphin.desktop.apps.onitranslator.widget.EntryTagChip
import org.jetbrains.compose.resources.stringResource
import kotlin.math.max

@Composable
fun EntryBrowser(
    state: AppState,
    onEvent: (AppEvent) -> Unit,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
) {
    val list by remember(
        state.uiState.searchState.isActive,
        state.uiState.searchState.text,
        state.uiState.searchState.results,
        state.filteredList
    ) {
        mutableStateOf(if (state.uiState.searchState.isActive) state.uiState.searchState.results else state.filteredList)
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
                    EntryItemPlaceholder()
                }
            }
        } else if (list.isEmpty()) {
            Box(modifier = modifier.fillMaxSize().padding(32.dp), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Rounded.AssignmentTurnedIn,
                        contentDescription = null,
                        modifier = Modifier.width(64.dp).height(64.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                    )
                    Text(
                        text = stringResource(Res.string.empty_list_title),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(Res.string.empty_list_message),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth(0.8f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        ElevatedButton(
                            onClick = { onEvent(AppEvent.File.RefreshSource) },
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.button_refresh))
                        }

                        ElevatedButton(
                            onClick = { onEvent(AppEvent.Search.ActiveChange(true)) },
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp)
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(Res.string.button_search))
                        }
                    }
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
                    EntryItemView(
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
private fun EntryItemView(
    data: EditorData,
    modifier: Modifier = Modifier,
    onItemClick: (PoEntry) -> Unit = {},
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

                // Tag Sensor Indicator
                data.target.diagnostic?.let { diag ->
                    if (diag.hasIssue) {
                        val tint = if (diag.hasMismatch) OniColor.Warning else OniColor.Highlight
                        Icon(
                            Icons.Rounded.SensorWindow,
                            contentDescription = stringResource(Res.string.content_description_tag_sensor_warning),
                            tint = tint,
                            modifier = Modifier.padding(horizontal = 4.dp).width(14.dp).height(14.dp)
                        )
                    }
                }

                Text((index + 1).toString(), style = MaterialTheme.typography.labelSmall)
            }

            Spacer(Modifier.height(8.dp))

            // Main translated text - give it prominence
            Text(
                text = data.target.msgStr(),
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
                // Original
                EntryTagChip(tagType = EntryTagType.Original, text = data.templateText)
                // Simplified
                data.referenceText?.let {
                    EntryTagChip(tagType = EntryTagType.Simplified, text = it)
                }
                // Old translated
                data.draftText?.let {
                    EntryTagChip(tagType = EntryTagType.Translated, text = it)
                }
            }
        }
    }
}

@Composable
private fun EntryItemPlaceholder(modifier: Modifier = Modifier) {
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
private fun EntryItemPreview() {
    val sampleEntry = PoEntry("STRINGS.UI.PREVIEW.KEY", "The quick brown fox jumps over the lazy dog.")
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
                        EntryItemView(data = defaultViewData, index = ++index)
                        EntryItemView(data = newEntryViewData, index = ++index, changed = 1L)
                        EntryItemView(data = defaultViewData, index = ++index, selected = true)
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun EntryItemPlaceholderPreview() {
    Column(modifier = Modifier.padding(16.dp)) {
        OniTranslatorTheme(darkTheme = false) {
            EntryItemPlaceholder()
            EntryItemPlaceholder()
        }
        OniTranslatorTheme(darkTheme = true) {
            EntryItemPlaceholder()
            EntryItemPlaceholder()
        }
    }
}

fun Modifier.shimmerEffect(): Modifier = composed {
    background(
        brush = Brush.linearGradient(
            colors = listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
            ),
            start = Offset.Zero,
            end = Offset(x = 500f, y = 500f) // Adjust for effect size
        )
    )
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
