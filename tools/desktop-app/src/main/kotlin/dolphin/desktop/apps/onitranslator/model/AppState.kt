package dolphin.desktop.apps.onitranslator.model

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition

/**
 * The root state container for the entire application.
 * All data that needs to be preserved or reacted to by the UI is stored here.
 */
data class AppState(
    val configs: Configs = Configs(),
    // The current list of editor entries being displayed (after filtering/searching)
    val filteredList: List<EditorData> = emptyList(),
    // IDs of entries that have been modified but not yet committed to file
    val changedList: List<Long> = emptyList(),
    val logs: List<LogEntry> = emptyList(),
    val appVersion: String = "0.0.0",
    val uiState: UiState = UiState(),
)

/**
 * Transient UI state that handles visibility, progress, and layout properties.
 */
data class UiState(
    val isLoading: Boolean = false,
    val processStatus: String = "",
    val searchState: SearchState = SearchState(),
    // The specific entry currently being edited in the editor panel
    val editorData: EditorData? = null,
    // Determines which overlay or dialog is currently active
    val dialogState: OniDialogState? = null,
    val windowPosition: WindowPosition = WindowPosition.PlatformDefault,
    val windowSize: DpSize = DpSize.Unspecified,
)

/**
 * Encapsulates the state of the search functionality.
 */
data class SearchState(
    val isActive: Boolean = false,
    val type: SearchType = SearchType.Key,
    val text: String = "",
    val results: List<EditorData> = emptyList(),
)

/**
 * Defines the possible states for application-level dialogs and overlays.
 */
sealed interface OniDialogState {
    data class DebugSaveDialog(val realFileName: String = "", val draftFileName: String = "") : OniDialogState
    data class ConfigDialog(val configs: Configs) : OniDialogState
    data class LogWindow(val logs: List<LogEntry>) : OniDialogState
}

