package dolphin.desktop.apps.onitranslator.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Drafts
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.model.AppState
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OniTranslatorTopBar(
    state: AppState,
    onEvent: (AppEvent) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    if (state.uiState.isSearchActive) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
            DockedSearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = state.searchText,
                        onQueryChange = { onEvent(AppEvent.OnSearchTextChange(it, state.searchType)) },
                        onSearch = { onEvent(AppEvent.OnSearchTextChange(it, state.searchType)) }, // Not used as we don't have expanded search
                        expanded = false,
                        onExpandedChange = { onEvent(AppEvent.OnSearchActiveChange(it)) }, // Corrected to use onSearchActiveChange
                        placeholder = { Text("Search...") },
                        leadingIcon = {
                            IconButton(onClick = { onEvent(AppEvent.OnSearchActiveChange(false)) }) {
                                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back") // Corrected icon
                            }
                        },
                        trailingIcon = {
                            if (state.searchText.isNotEmpty()) {
                                IconButton(onClick = { onEvent(AppEvent.OnSearchTextChange("", state.searchType)) }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                expanded = false,
                onExpandedChange = { onEvent(AppEvent.OnSearchActiveChange(it)) }, // Corrected to use onSearchActiveChange
                modifier = Modifier.fillMaxWidth()
            ) {
                // Search results would go here, but we are doing a docked search bar
                // that filters the main list, so this content is not used.
            }
        }
    } else {
        TopAppBar(
            title = { Text("Oni Translator") },
            actions = {
                IconButton(onClick = { onEvent(AppEvent.OnSearchActiveChange(true)) }) {
                    Icon(Icons.Rounded.Search, contentDescription = "Search")
                }
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "More actions")
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Save file") },
                        onClick = {
                            onEvent(AppEvent.OnSaveFile(false)) // false for regular save
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Rounded.Save, contentDescription = "Save file") }
                    )
                    DropdownMenuItem(
                        text = { Text("Draft") },
                        onClick = {
                            onEvent(AppEvent.OnSaveDraft)
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Rounded.Drafts, contentDescription = "Done file") }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            onEvent(AppEvent.UiEvent.OnShowConfig)
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = "Settings") }
                    )
                }
            }
        )
    }
}

@Preview
@Composable
private fun OniTranslatorTopBarPreview() {
    val appState = AppState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        arrayOf(false, true).forEach { dark ->
            OniTranslatorTheme(darkTheme = dark) {
                OniTranslatorTopBar(
                    state = appState,
                    onEvent = {},
                )
                OniTranslatorTopBar(
                    state = appState.copy(uiState = appState.uiState.copy(isSearchActive = true)),
                    onEvent = {},
                )
            }
        }
    }
}
