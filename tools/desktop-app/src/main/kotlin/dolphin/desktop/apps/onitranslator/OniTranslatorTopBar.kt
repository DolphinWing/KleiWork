package dolphin.desktop.apps.onitranslator

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
import dolphin.desktop.apps.onitranslator.compose.OniTranslatorM3Theme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OniTranslatorTopBar(
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    searchText: String,
    onSearchTextChange: (String) -> Unit,
    onSaveFile: () -> Unit,
    onShowConfig: () -> Unit,
    onSaveDraft: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }

    if (isSearchActive) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)) {
            DockedSearchBar(
                inputField = {
                    SearchBarDefaults.InputField(
                        query = searchText,
                        onQueryChange = onSearchTextChange,
                        onSearch = onSearchTextChange, // Not used as we don't have expanded search
                        expanded = false,
                        onExpandedChange = { onSearchActiveChange(it) }, // Corrected to use onSearchActiveChange
                        placeholder = { Text("Search...") },
                        leadingIcon = {
                            IconButton(onClick = { onSearchActiveChange(false) }) {
                                Icon(Icons.Rounded.ArrowBackIosNew, contentDescription = "Back") // Corrected icon
                            }
                        },
                        trailingIcon = {
                            if (searchText.isNotEmpty()) {
                                IconButton(onClick = { onSearchTextChange("") }) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                expanded = false,
                onExpandedChange = { onSearchActiveChange(it) }, // Corrected to use onSearchActiveChange
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
                IconButton(onClick = { onSearchActiveChange(true) }) {
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
                            onSaveFile()
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Rounded.Save, contentDescription = "Save file") }
                    )
                    DropdownMenuItem(
                        text = { Text("Draft") },
                        onClick = {
                            onSaveDraft()
                            menuExpanded = false
                        },
                        leadingIcon = { Icon(Icons.Rounded.Drafts, contentDescription = "Done file") }
                    )
                    DropdownMenuItem(
                        text = { Text("Settings") },
                        onClick = {
                            onShowConfig()
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
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        arrayOf(false, true).forEach { dark ->
            OniTranslatorM3Theme(darkTheme = dark) {
                OniTranslatorTopBar(
                    isSearchActive = false,
                    onSearchActiveChange = {},
                    searchText = "",
                    onSearchTextChange = {},
                    onSaveFile = { },
                    onSaveDraft = { },
                    onShowConfig = { },
                )
                OniTranslatorTopBar(
                    isSearchActive = true,
                    onSearchActiveChange = {},
                    searchText = "STRINGS.",
                    onSearchTextChange = {},
                    onSaveFile = {},
                    onSaveDraft = { },
                    onShowConfig = { },
                )
            }
        }
    }
}
