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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.debug_save_dialog_title
import dolphin.desktop.apps.onitranslator.model.AppState
import dolphin.desktop.apps.onitranslator.model.OniDialogState
import dolphin.desktop.apps.onitranslator.model.SearchType
import dolphin.desktop.apps.onitranslator.pane.ConfigDialogContent
import dolphin.desktop.apps.onitranslator.pane.EditorPane
import dolphin.desktop.apps.onitranslator.pane.EntryListPane
import dolphin.desktop.apps.onitranslator.theme.OniTranslatorTheme
import dolphin.desktop.apps.onitranslator.widget.OniSnackbarHost
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OniTranslatorApp(
    state: AppState,
    onEvent: (AppEvent) -> Unit,
    onEditorConvert: (String) -> String = { it },
) {
    fun handleUiEvents(event: AppEvent) {
        if (event is AppEvent.UiEvent) {
            var uiState = state.uiState
            uiState = when (event) {
                is AppEvent.UiEvent.OnShowConfig ->
                    uiState.copy(dialogState = OniDialogState.ConfigDialog(state.configs))

                is AppEvent.UiEvent.OnShowDebugSaveDialog ->
                    uiState.copy(dialogState = OniDialogState.DebugSaveDialog())

                is AppEvent.UiEvent.OnDismissDialog ->
                    uiState.copy(dialogState = null)
            }
            onEvent(AppEvent.OnUiStateChange(uiState))
        } else {
            onEvent(event)
        }
    }

    OniTranslatorTheme {
        val snackbarHostState = remember { SnackbarHostState() }

        Box(modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    OniTranslatorTopBar(state = state, onEvent = ::handleUiEvents)
                },
                bottomBar = {
                    OniTranslatorBottomBar(state = state)
                },
                snackbarHost = { OniSnackbarHost(snackbarHostState) },
                containerColor = MaterialTheme.colorScheme.background,
                contentColor = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.fillMaxSize()
            ) { paddingValues ->
                Column(Modifier.padding(paddingValues)) {
                    AnimatedVisibility(
                        visible = state.uiState.isSearchActive, // This should come from AppState
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SearchType.entries.forEach { type ->
                                FilterChip(
                                    selected = state.searchType == type,
                                    onClick = {
                                        onEvent(AppEvent.OnSearchTypeChange(type))
                                    },
                                    label = { Text(type.name) }
                                )
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxSize()) {
                        EntryListPane(
                            state = state,
                            onEvent = ::handleUiEvents,
                            modifier = Modifier.weight(0.4f),
                        )
                        VerticalDivider(modifier = Modifier.fillMaxHeight().width(1.dp))
                        EditorPane(
                            entry = state.uiState.editorData,
                            modifier = Modifier.weight(0.6f),
                            onEvent = onEvent,
                            onConvert = onEditorConvert,
                        )
                    }
                }
            }

            M3DialogHost(state, onEvent = ::handleUiEvents)

            if (state.isLoading && !state.uiState.isSearchActive) { // Don't show loading overlay when search is active
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
private fun M3DialogHost(state: AppState, onEvent: (AppEvent) -> Unit) {
    when (val dialogState = state.uiState.dialogState) {
        is OniDialogState.DebugSaveDialog ->
            // val cacheFile = dataModel.helper.getOutputFile(true) // Should be in AppState
            // val file = dataModel.helper.getOutputFile(false) // Should be in AppState
            M3DebugSaveDialog(
                onDismissRequest = { onEvent(AppEvent.UiEvent.OnDismissDialog) },
                onSave = { useCache -> onEvent(AppEvent.OnSaveFile(useCache)) },
                cacheFileName = dialogState.draftFileName,
                realFileName = dialogState.realFileName,
            )

        is OniDialogState.ConfigDialog ->
            Dialog(onDismissRequest = { onEvent(AppEvent.UiEvent.OnDismissDialog) }) {
                ConfigDialogContent(
                    configs = state.configs,
                    onConfigChange = { onEvent(AppEvent.OnConfigChange(it)) },
                    onConfigSaved = { onEvent(AppEvent.OnConfigSaved(it)) },
                    onDismissRequest = { onEvent(AppEvent.UiEvent.OnDismissDialog) },
                )
            }

        else -> { /* null */
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

