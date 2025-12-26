package dolphin.desktop.apps.onitranslator.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.debug_save_dialog_title
import dolphin.desktop.apps.onitranslator.model.AppState
import dolphin.desktop.apps.onitranslator.model.LogEntry
import dolphin.desktop.apps.onitranslator.model.LogType
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
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val isDark = state.uiState.darkTheme ?: isSystemInDarkTheme()

    OniTranslatorTheme(darkTheme = isDark) {
        val snackbarHostState = remember { SnackbarHostState() }

        Box(modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)) {
            Scaffold(
                topBar = {
                    OniTranslatorTopBar(state = state, onEvent = onEvent, scrollBehavior = scrollBehavior)
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
                        visible = state.uiState.searchState.isActive, // This should come from AppState
                        enter = expandVertically(),
                        exit = shrinkVertically()
                    ) {
                        SearchTypeRow(
                            selected = state.uiState.searchState.type,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        ) { searchType ->
                            onEvent(AppEvent.Search.TextChange(state.uiState.searchState.text, searchType))
                        }
                    }
                    Row(modifier = Modifier.fillMaxSize()) {
                        EntryListPane(
                            state = state,
                            onEvent = onEvent,
                            modifier = Modifier.weight(0.4f),
                        )
                        VerticalDivider(
                            modifier = Modifier.fillMaxHeight().width(1.dp),
                            color = MaterialTheme.colorScheme.outline
                        )
                        EditorPane(
                            entry = state.uiState.editorData,
                            modifier = Modifier.weight(0.6f),
                            onEvent = onEvent,
                            onConvert = onEditorConvert,
                        )
                    }
                }
            }

            M3DialogHost(state, onEvent = onEvent)

            if (state.uiState.isLoading && !state.uiState.searchState.isActive) { // Don't show loading overlay when search is active
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
                onDismissRequest = { onEvent(AppEvent.Ui.DismissDialog) },
                onSave = { useCache -> onEvent(AppEvent.File.Save(useCache)) },
                cacheFileName = dialogState.draftFileName,
                realFileName = dialogState.realFileName,
            )

        is OniDialogState.ConfigDialog ->
            Dialog(onDismissRequest = { onEvent(AppEvent.Ui.DismissDialog) }) {
                ConfigDialogContent(
                    configs = state.configs,
                    onConfigChange = { onEvent(AppEvent.Config.Change(it)) },
                    onConfigSaved = { onEvent(AppEvent.Config.Saved(it)) },
                    onDismissRequest = { onEvent(AppEvent.Ui.DismissDialog) },
                )
            }

        is OniDialogState.LogWindow ->
            Dialog(
                onDismissRequest = { onEvent(AppEvent.Ui.DismissDialog) },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                LogWindowContent(
                    logs = dialogState.logs,
                    modifier = Modifier.fillMaxSize(.8f),
                )
            }

        else -> { /* null */
        }
    }
}

@Composable
private fun LogWindowContent(
    logs: List<LogEntry>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Activity Logs",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Surface(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                color = MaterialTheme.colorScheme.inverseOnSurface.copy(alpha = 0.8f),
                shape = MaterialTheme.shapes.medium
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs.size) { index ->
                        val log = logs[index]
                        LogItemView(log)
                    }
                }
            }
        }
    }
}

@Composable
private fun LogItemView(log: LogEntry) {
    val color = when (log.type) {
        LogType.Error -> MaterialTheme.colorScheme.error
        LogType.Warning -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f) // Faded Info
    }

    // Simple time format: HH:mm:ss
    val time = java.time.Instant.ofEpochMilli(log.timestamp)
        .atZone(java.time.ZoneId.systemDefault())
        .toLocalTime()
        .let { String.format("%02d:%02d:%02d", it.hour, it.minute, it.second) }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "[$time]",
            style = MaterialTheme.typography.labelSmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = log.message,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = color,
            modifier = Modifier.weight(1f)
        )
    }
}

@Preview
@Composable
private fun LogWindowContentPreview() {
    val logs = listOf(
        LogEntry("Loading Simplified Chinese PO..."),
        LogEntry("Simplified Chinese PO size: 15420"),
        LogEntry("Loading PO Template..."),
        LogEntry("Found msgid changed for key 'STRINGS.UI.DRAFT'", type = LogType.Warning),
        LogEntry("Failed to load strings.po: File not found", type = LogType.Error),
        LogEntry("Translation process finished in 150 ms")
    )
    Column {
        arrayOf(false, true).forEach {
            OniTranslatorTheme(darkTheme = it) {
                Surface(Modifier.weight(1f)) {
                    LogWindowContent(logs, modifier = Modifier.padding(8.dp))
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

