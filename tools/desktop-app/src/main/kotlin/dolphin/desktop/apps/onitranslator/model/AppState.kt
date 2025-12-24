package dolphin.desktop.apps.onitranslator.model

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition

data class AppState(
    val configs: Configs = Configs(),
    val filteredList: List<EditorData> = emptyList(),
    val changedList: List<Long> = emptyList(),
    val logs: List<LogEntry> = emptyList(),
    val appVersion: String = "0.0.0",
    val uiState: UiState = UiState(),
)

data class UiState(
    val isLoading: Boolean = false,
    val processStatus: String = "",
    val searchState: SearchState = SearchState(),
    val editorData: EditorData? = null,
    val dialogState: OniDialogState? = null,
    val windowPosition: WindowPosition = WindowPosition.PlatformDefault,
    val windowSize: DpSize = DpSize.Unspecified,
)

data class SearchState(
    val isActive: Boolean = false,
    val type: SearchType = SearchType.Key,
    val text: String = "",
    val results: List<EditorData> = emptyList(),
)

sealed interface OniDialogState {
    data class DebugSaveDialog(val realFileName: String = "", val draftFileName: String = "") : OniDialogState
    data class ConfigDialog(val configs: Configs) : OniDialogState

    data class LogWindow(val logs: List<LogEntry>) : OniDialogState
}

