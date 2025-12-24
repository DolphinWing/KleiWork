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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.model.AppState
import dolphin.desktop.apps.onitranslator.model.EntryTagType
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme
import dolphin.desktop.apps.onitranslator.widget.TextTag

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OniTranslatorBottomBar(state: AppState, modifier: Modifier = Modifier) {
    val list by remember(state.uiState.searchState.text, state.uiState.searchState.results, state.filteredList) {
        mutableStateOf(if (state.uiState.searchState.text.isBlank()) state.filteredList else state.uiState.searchState.results)
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
                text = if (state.uiState.isLoading) "Loading..." else "${list.size} items",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            VerticalDivider(modifier = Modifier.size(1.dp, 16.dp))
            Spacer(Modifier.width(4.dp))
            EntryTagType.entries.forEach { type ->
                TextTag(tagType = type)
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
                OniTranslatorBottomBar(state = appState)
                OniTranslatorBottomBar(state = appState.copy(uiState = appState.uiState.copy(isLoading = true)))
            }
        }
    }
}