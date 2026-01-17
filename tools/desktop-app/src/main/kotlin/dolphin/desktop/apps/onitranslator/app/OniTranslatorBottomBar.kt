package dolphin.desktop.apps.onitranslator.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.BottomAppBarDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.status_items_count
import dolphin.desktop.apps.onitranslator.generated.resources.status_loading
import dolphin.desktop.apps.onitranslator.model.AppState
import dolphin.desktop.apps.onitranslator.model.EntryTagType
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme
import dolphin.desktop.apps.onitranslator.widget.EntryTagChip
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OniTranslatorBottomBar(state: AppState, modifier: Modifier = Modifier) {
    // Derived state for the item list size
    val listSize = if (state.uiState.searchState.text.isBlank()) {
        state.filteredList.size
    } else {
        state.uiState.searchState.results.size
    }

    // Determine the status text: prefer processStatus, fallback to item count
    val statusText = if (state.uiState.processStatus.isNotBlank()) {
        state.uiState.processStatus
    } else if (state.uiState.isLoading) {
        stringResource(Res.string.status_loading)
    } else {
        stringResource(Res.string.status_items_count, listSize)
    }

    Surface(
        modifier = modifier,
        color = BottomAppBarDefaults.containerColor,
        tonalElevation = BottomAppBarDefaults.ContainerElevation,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = statusText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            VerticalDivider(modifier = Modifier.size(1.dp, 16.dp))
            Spacer(Modifier.width(4.dp))
            EntryTagType.entries.forEach { type ->
                EntryTagChip(tagType = type)
                Spacer(Modifier.width(4.dp))
            }
            VerticalDivider(modifier = Modifier.size(1.dp, 16.dp))
            Spacer(Modifier.width(8.dp))
            Text(
                state.appVersion,
                color = MaterialTheme.colorScheme.onSurface,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Preview
@Composable
private fun OniTranslatorBottomBarPreview() {
    val appState = AppState(appVersion = "2.0.0")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        arrayOf(false, true).forEach { dark ->
            OniTranslatorTheme(darkTheme = dark) {
                Surface {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OniTranslatorBottomBar(state = appState)
                        OniTranslatorBottomBar(state = appState.copy(uiState = appState.uiState.copy(isLoading = true)))
                        OniTranslatorBottomBar(
                            state = appState.copy(
                                uiState = appState.uiState.copy(processStatus = "Saving file...")
                            )
                        )
                    }
                }
            }
        }
    }
}
