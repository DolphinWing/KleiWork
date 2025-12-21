package dolphin.desktop.apps.onitranslator.model

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.window.WindowPosition

data class AppState(
    val configs: Configs = Configs(),
    val filteredList: List<EditorData> = emptyList(),
    val changedList: List<Long> = emptyList(),
    val searchType: SearchType = SearchType.Key,
    val searchText: String = "",
    val searchList: List<EditorData> = emptyList(),
    val isLoading: Boolean = false,
    val windowPosition: WindowPosition = WindowPosition.PlatformDefault,
    val windowSize: DpSize = DpSize.Unspecified,
    val uiState: UiState = UiState(),
    val appVersion: String = "0.0.0",
)

data class UiState(
    val isSearchActive: Boolean = false,
    val editorData: EditorData? = null,
    val dialogState: OniDialogState? = null,
)

sealed interface OniDialogState {
    data class DebugSaveDialog(val realFileName: String = "", val draftFileName: String = "") : OniDialogState
    data class ConfigDialog(val configs: Configs) : OniDialogState
}

