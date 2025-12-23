package dolphin.desktop.apps.onitranslator.app

import dolphin.desktop.apps.onitranslator.model.AppState
import dolphin.desktop.apps.onitranslator.model.Configs
import dolphin.desktop.apps.onitranslator.model.SearchType
import dolphin.desktop.apps.onitranslator.model.UiState
import dolphin.desktop.apps.onitranslator.model.WordEntry

sealed interface AppEvent {
    data class OnSearchActiveChange(val isActive: Boolean) : AppEvent
    data class OnSearchTextChange(val text: String, val searchType: SearchType) : AppEvent

    object OnSaveDraft : AppEvent
    data class OnSaveFile(val useCache: Boolean) : AppEvent
    data class OnConfigSaved(val configs: Configs) : AppEvent
    data class OnConfigChange(val configs: Configs) : AppEvent

    data class OnEditEntry(val entry: WordEntry?) : AppEvent
    data class OnSaveEntry(val entry: WordEntry, val newText: String) : AppEvent

    data class OnUiStateChange(val uiState: UiState) : AppEvent
    data class OnCopyToClipboard(val text: String) : AppEvent

    object OnRefreshSource : AppEvent

    sealed interface UiEvent : AppEvent {
        object OnShowConfig : UiEvent
        object OnShowDebugSaveDialog : UiEvent
        object OnDismissDialog : UiEvent
    }
}
