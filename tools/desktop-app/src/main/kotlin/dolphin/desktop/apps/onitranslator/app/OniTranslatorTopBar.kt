package dolphin.desktop.apps.onitranslator.app

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBackIosNew
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.Drafts
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Report
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.button_refresh
import dolphin.desktop.apps.onitranslator.generated.resources.button_search
import dolphin.desktop.apps.onitranslator.generated.resources.content_description_back
import dolphin.desktop.apps.onitranslator.generated.resources.content_description_clear
import dolphin.desktop.apps.onitranslator.generated.resources.content_description_more_actions
import dolphin.desktop.apps.onitranslator.generated.resources.menu_draft
import dolphin.desktop.apps.onitranslator.generated.resources.menu_save_file
import dolphin.desktop.apps.onitranslator.generated.resources.menu_settings
import dolphin.desktop.apps.onitranslator.generated.resources.menu_show_logs
import dolphin.desktop.apps.onitranslator.generated.resources.placeholder_search
import dolphin.desktop.apps.onitranslator.generated.resources.toolbar_status
import dolphin.desktop.apps.onitranslator.generated.resources.tooltip_theme_toggle
import dolphin.desktop.apps.onitranslator.model.AppState
import dolphin.desktop.apps.onitranslator.model.SearchType
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme
import dolphin.desktop.apps.onitranslator.widget.TooltipIconButton
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OniTranslatorTopBar(
    state: AppState,
    onEvent: (AppEvent) -> Unit,
    scrollBehavior: TopAppBarScrollBehavior? = null,
) {
    Column {
        if (state.uiState.searchState.isActive) {
            SearchTopBar(
                searchText = state.uiState.searchState.text,
                onSearchTextChange = { onEvent(AppEvent.Search.TextChange(it, state.uiState.searchState.type)) },
                onActiveChange = { onEvent(AppEvent.Search.ActiveChange(it)) }
            )
        } else {
            val filteredList = state.filteredList
            val changedList = state.filteredList

            TopAppBar(
                title = { Text(stringResource(Res.string.toolbar_status, filteredList.size, changedList.size)) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                ),
                actions = {
                    TooltipIconButton(
                        icon = Icons.Rounded.Search,
                        tooltip = stringResource(Res.string.button_search)
                    ) {
                        onEvent(AppEvent.Search.ActiveChange(true))
                    }

                    val isDark = state.uiState.darkTheme ?: isSystemInDarkTheme()
                    TooltipIconButton(
                        icon = if (isDark) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        tooltip = stringResource(Res.string.tooltip_theme_toggle)
                    ) {
                        onEvent(AppEvent.Ui.ChangeTheme(!isDark))
                    }

                    MoreActionsMenu(onEvent)
                },
                scrollBehavior = scrollBehavior
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .5f))
        }
    }
}

@Composable
private fun MoreActionsMenu(onEvent: (AppEvent) -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }

    Box {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(
                Icons.Rounded.MoreVert,
                contentDescription = stringResource(Res.string.content_description_more_actions)
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.menu_save_file)) },
                onClick = {
                    onEvent(AppEvent.File.Save(false)) // false for regular save
                    menuExpanded = false
                },
                leadingIcon = { Icon(Icons.Rounded.Save, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.menu_draft)) },
                onClick = {
                    onEvent(AppEvent.File.SaveDraft)
                    menuExpanded = false
                },
                leadingIcon = { Icon(Icons.Rounded.Drafts, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.button_refresh)) },
                onClick = {
                    onEvent(AppEvent.File.RefreshSource)
                    menuExpanded = false
                },
                leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.menu_show_logs)) },
                onClick = {
                    onEvent(AppEvent.Ui.ShowLogWindow)
                    menuExpanded = false
                },
                leadingIcon = { Icon(Icons.Rounded.Report, contentDescription = null) }
            )
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.menu_settings)) },
                onClick = {
                    onEvent(AppEvent.Ui.ShowConfig)
                    menuExpanded = false
                },
                leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchTopBar(
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onActiveChange: (Boolean) -> Unit,
) {
    // Local state to prevent cursor jumping due to async state updates
    var query by remember { mutableStateOf(searchText) }

    // Sync from external state only when necessary (e.g. clear button)
    // We avoid syncing when the difference might be due to typing lag
    LaunchedEffect(searchText) {
        if (searchText != query && searchText.isEmpty()) {
            query = ""
        }
    }

    Surface(color = MaterialTheme.colorScheme.surfaceContainer) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 8.dp, horizontal = 12.dp)) {
            DockedSearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = query,
                        onQueryChange = {
                            query = it
                            onSearchTextChange(it)
                        },
                        onSearch = { onSearchTextChange(it) },
                        expanded = false,
                        onExpandedChange = onActiveChange,
                        placeholder = { Text(stringResource(Res.string.placeholder_search)) },
                        leadingIcon = {
                            IconButton(onClick = { onActiveChange(false) }) {
                                Icon(
                                    Icons.Rounded.ArrowBackIosNew,
                                    contentDescription = stringResource(Res.string.content_description_back)
                                )
                            }
                        },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = {
                                    query = ""
                                    onSearchTextChange("")
                                }) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = stringResource(Res.string.content_description_clear)
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                expanded = false,
                onExpandedChange = onActiveChange,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Search results would go here, but we are doing a docked search bar
                // that filters the main list, so this content is not used.
            }
        }
    }
}

@Composable
fun SearchTypeRow(selected: SearchType, modifier: Modifier = Modifier, onClick: (SearchType) -> Unit) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SearchType.entries.forEach { type ->
            FilterChip(
                selected = selected == type,
                onClick = { onClick.invoke(type) },
                label = { Text(type.name) },
                shape = CircleShape,
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = Color.Transparent,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
                border = FilterChipDefaults.filterChipBorder(
                    enabled = true,
                    selected = selected == type,
                    borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    selectedBorderColor = Color.Transparent,
                    borderWidth = 1.dp,
                    selectedBorderWidth = 0.dp,
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun OniTranslatorTopBarPreview() {
    val appState = AppState()

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        arrayOf(false, true).forEach { dark ->
            OniTranslatorTheme(darkTheme = dark) {
                Surface {
                    Column {
                        OniTranslatorTopBar(
                            state = appState,
                            onEvent = {},
                        )
                        OniTranslatorTopBar(
                            state = appState.copy(
                                uiState = appState.uiState.copy(
                                    searchState = appState.uiState.searchState.copy(isActive = true)
                                )
                            ),
                            onEvent = {},
                        )
                        SearchTypeRow(selected = SearchType.Text, onClick = {})
                    }
                }
            }
        }
    }
}
