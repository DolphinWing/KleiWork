package dolphin.desktop.apps.onitranslator.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.debug_save_dialog_title
import dolphin.desktop.apps.onitranslator.model.PoDataModel
import dolphin.desktop.apps.onitranslator.model.WordEntry
import dolphin.desktop.apps.onitranslator.pane.ConfigDialogContent
import dolphin.desktop.apps.onitranslator.model.Configs
import dolphin.desktop.apps.onitranslator.model.EditorData
import dolphin.desktop.apps.onitranslator.pane.EditorPane
import dolphin.desktop.apps.onitranslator.pane.EntryListPane
import dolphin.desktop.apps.onitranslator.model.SearchType
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorM3Theme
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OniTranslatorApp(
    dataModel: PoDataModel,
    onCopyTo: (String) -> Unit,
    onCopyFrom: () -> String,
    debug: Boolean = false,
    appVersion: String = "x.x.x",
) {
    OniTranslatorM3Theme {
        val coroutineScope = rememberCoroutineScope()
        var selectedEntry by remember { mutableStateOf<WordEntry?>(null) }
        val loading by dataModel.helper.loading.collectAsState()
        var searchText by remember { mutableStateOf("") }
        var isSearchActive by remember { mutableStateOf(false) }
        val searchType by dataModel.searchType.collectAsState()
        val list by if (searchText.isBlank()) {
            dataModel.filteredList.collectAsState()
        } else {
            dataModel.searchList.collectAsState()
        }
        var bundleData by remember { mutableStateOf<EditorData?>(null) }
        var useDebugCache by remember { mutableStateOf(false) }
        var showConfigDialog by remember { mutableStateOf(false) }
        var configsState by remember { mutableStateOf(Configs()) } // Mutable state for config dialog

        fun saveEntryList(useCache: Boolean) {
            coroutineScope.launch {
                val (path, time) = dataModel.save(useCache)
                println("File saved to $path in $time ms")
            }
        }

        LaunchedEffect(Unit) {
            dataModel.loadIni() // Load only ini first
            configsState = Configs(dataModel.helper.ini) // Initialize configsState
            if (!dataModel.helper.isConfigValid()) {
                showConfigDialog = true
            } else {
                dataModel.loadIniAndPo() // Load PO only if initial config is valid
            }
        }

        LaunchedEffect(showConfigDialog) {
            if (!showConfigDialog && dataModel.filteredList.value.isEmpty() && dataModel.helper.isConfigValid()) {
                // If config dialog closed, list is empty, and config is valid (meaning user fixed it)
                dataModel.loadIniAndPo() // Load PO files now
            }
        }

        LaunchedEffect(searchText, searchType) {
            dataModel.search(searchText, searchType)
        }

        LaunchedEffect(selectedEntry) {
            val entry = selectedEntry
            if (entry != null) {
                val templateText = dataModel.helper.templated(entry.key)?.origin() ?: entry.origin()
                val referenceText = dataModel.helper.simplified(entry.key)?.translated()
                val draftText = dataModel.helper.translated(entry.key)?.translated()
                bundleData = EditorData(entry, templateText, referenceText, draftText)
            } else {
                bundleData = null
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    OniTranslatorTopBar(
                        isSearchActive = isSearchActive,
                        onSearchActiveChange = { isSearchActive = it },
                        searchText = searchText,
                        onSearchTextChange = { searchText = it },
                        onSaveFile = {
                            if (debug) {
                                useDebugCache = true
                            } else {
                                saveEntryList(false)
                            }
                        },
                        onSaveDraft = { saveEntryList(true) },
                        onShowConfig = { showConfigDialog = true } // New: show config dialog
                    )
                },
                bottomBar = {
                    OniTranslatorBottomBar(
                        isLoading = loading,
                        listSize = list.size,
                        versionText = if (debug) "${appVersion}D" else appVersion,
                    )
                },
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
                Column(Modifier.padding(paddingValues)) {
                    AnimatedVisibility(
                        visible = isSearchActive,
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchType.entries.forEach { type ->
                                FilterChip(
                                    selected = searchType == type,
                                    onClick = {
                                        coroutineScope.launch { dataModel.searchType(type) }
                                    },
                                    label = { Text(type.name) }
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxSize()) {
                        EntryListPane(
                            dataModel = dataModel,
                            list = list,
                            searchText = searchText,
                            modifier = Modifier.weight(0.4f),
                            onEdit = { entry -> selectedEntry = entry }
                        )
                        VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
                        EditorPane(
                            entry = bundleData,
                            modifier = Modifier.weight(0.6f),
                            onSave = { entry, newText ->
                                coroutineScope.launch {
                                    dataModel.edit(entry.key, newText)
                                }
                            },
                            onCopyToClipboard = onCopyTo,
                            onConvert = { text -> dataModel.helper.convert(text) },
                            onCancel = { selectedEntry = null }
                        )
                    }
                }
            }

            if (useDebugCache) {
                val cacheFile = dataModel.helper.getOutputFile(true)
                val file = dataModel.helper.getOutputFile(false)
                M3DebugSaveDialog(
                    onDismissRequest = { useDebugCache = false },
                    onSave = { useCache ->
                        saveEntryList(useCache)
                        useDebugCache = false
                    },
                    cacheFileName = cacheFile.absolutePath,
                    realFileName = file.absolutePath,
                )
            }

            if (showConfigDialog) {
                Dialog(onDismissRequest = { showConfigDialog = false }) {
                    ConfigDialogContent(
                        configs = configsState,
                        onConfigChange = { configsState = it },
                        onConfigSaved = { configs ->
                            coroutineScope.launch {
                                dataModel.saveConfig(configs)
                                showConfigDialog = false
                            }
                        },
                        onDismissRequest = { showConfigDialog = false },
                    )
                }
            }


            if (loading && !isSearchActive) { // Don't show loading overlay when search is active
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
        }
    }
}

@Composable
private fun M3DebugSaveDialog(
    onDismissRequest: () -> Unit,
    onSave: (Boolean) -> Unit,
    cacheFileName: String,
    realFileName: String,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            Text(
                stringResource(Res.string.debug_save_dialog_title, cacheFileName),
                style = MaterialTheme.typography.bodyMedium
            )
        },
        confirmButton = {
            Button(onClick = { onSave(true) }) {
                Text(cacheFileName)
            }
        },
        dismissButton = {
            TextButton(onClick = { onSave(false) }) {
                Text(realFileName)
            }
        },
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    )
}
