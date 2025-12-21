package dolphin.desktop.apps.onitranslator.pane

// Import the new M3ToolbarPane
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.model.EditorData
import dolphin.desktop.apps.onitranslator.model.EntryTagType
import dolphin.desktop.apps.onitranslator.model.PoDataModel
import dolphin.desktop.apps.onitranslator.model.WordEntry
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorM3Theme
import dolphin.desktop.apps.onitranslator.widget.TextTag
import dolphin.desktop.apps.onitranslator.widget.shimmerEffect

@Composable
fun EntryListPane(
    dataModel: PoDataModel,
    list: List<WordEntry>,
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    searchText: String = "",
    onEdit: (WordEntry) -> Unit,
) {
    val changedList by dataModel.changedList.collectAsState()
    val isLoading by dataModel.helper.loading.collectAsState()

    if (isLoading && searchText.isBlank()) {
        // Show Skeleton Screen only when not searching
        LazyColumn(modifier = modifier) {
            items(10) {
                EntryViewPlaceholder()
            }
        }
    } else {
        // Show actual list
        LazyColumn(modifier = modifier, state = state) {
            itemsIndexed(list) { index, entry ->
                val isSearchMode = searchText.isNotBlank()
                val originalIndex = if (isSearchMode) -1 else index
                val changedValue = if (isSearchMode) 0L else changedList.getOrNull(index) ?: 0L

                val viewData = EditorData(
                    target = entry,
                    templateText = dataModel.helper.templated(entry.key)?.origin() ?: entry.origin(),
                    referenceText = dataModel.helper.simplified(entry.key)?.translated(),
                    draftText = dataModel.helper.translated(entry.key)?.translated(),
                )

                M3EntryView(
                    data = viewData,
                    onItemClick = { onEdit.invoke(it) },
                    index = originalIndex, // Pass original index or -1 if not available
                    changed = changedValue,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun M3EntryView(
    data: EditorData,
    modifier: Modifier = Modifier,
    onItemClick: (WordEntry) -> Unit,
    index: Int,
    changed: Long,
) {
    val isChanged = changed > 0
    val cardBorderColor =
        if (isChanged) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline.copy(
            alpha = 0.2f
        )

    OutlinedCard(
        onClick = { onItemClick.invoke(data.target) },
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
        border = BorderStroke(if (isChanged) 1.5.dp else 1.dp, cardBorderColor)
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
            )

            Spacer(Modifier.height(8.dp))

            // Tags for other versions
            Column(
                horizontalAlignment = Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                // Templated
                TextTag(
                    text = data.templateText,
                    containerColor = EntryTagType.Templated.containerColor(MaterialTheme.colorScheme),
                    contentColor = EntryTagType.Templated.contentColor(MaterialTheme.colorScheme),
                )
                // Simplified
                data.referenceText?.let {
                    TextTag(
                        text = it,
                        containerColor = EntryTagType.Simplified.containerColor(MaterialTheme.colorScheme),
                        contentColor = EntryTagType.Simplified.contentColor(MaterialTheme.colorScheme),
                    )
                }
                // Old translated
                data.draftText?.let {
                    TextTag(
                        text = it,
                        containerColor = EntryTagType.Translated.containerColor(MaterialTheme.colorScheme),
                        contentColor = EntryTagType.Translated.contentColor(MaterialTheme.colorScheme),
                    )
                }
            }
        }
    }
}

@Composable
private fun EntryViewPlaceholder(modifier: Modifier = Modifier) {
    OutlinedCard(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp, horizontal = 8.dp),
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

    Column(modifier = Modifier.padding(16.dp).width(400.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        arrayOf(false, true).forEach { darkTheme ->
            OniTranslatorM3Theme(darkTheme = darkTheme) {
                Surface {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        M3EntryView(
                            data = defaultViewData,
                            onItemClick = {},
                            index = ++index,
                            changed = 0L,
                        )
                        M3EntryView(
                            data = newEntryViewData,
                            onItemClick = {},
                            index = ++index,
                            changed = 1L, // Mark as changed
                        )
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
        OniTranslatorM3Theme(darkTheme = false) {
            EntryViewPlaceholder()
            EntryViewPlaceholder()
        }
        OniTranslatorM3Theme(darkTheme = true) {
            EntryViewPlaceholder()
            EntryViewPlaceholder()
        }
    }
}
