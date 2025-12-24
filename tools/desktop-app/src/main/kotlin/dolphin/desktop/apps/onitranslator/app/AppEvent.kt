package dolphin.desktop.apps.onitranslator.app

import dolphin.desktop.apps.onitranslator.model.Configs
import dolphin.desktop.apps.onitranslator.model.SearchType
import dolphin.desktop.apps.onitranslator.model.UiState
import dolphin.desktop.apps.onitranslator.model.WordEntry

sealed interface AppEvent {
    // Search Actions
    sealed interface Search : AppEvent {
        data class ActiveChange(val isActive: Boolean) : Search
        data class TextChange(val text: String, val type: SearchType) : Search
    }

    // File Operations
    sealed interface File : AppEvent {
        object SaveDraft : File
        data class Save(val useCache: Boolean) : File
        object RefreshSource : File
    }

    // Configuration Actions
    sealed interface Config : AppEvent {
        data class Change(val configs: Configs) : Config
        data class Saved(val configs: Configs) : Config
    }

    // Editor Actions
    sealed interface Editor : AppEvent {
        data class Select(val entry: WordEntry?) : Editor
        data class Save(val entry: WordEntry, val newText: String) : Editor

        // data class Convert(val text: String) : Editor // If needed as an event, though usually a direct call
    }

    // UI & System Actions
    sealed interface Ui : AppEvent {
        data class UpdateState(val uiState: UiState) : Ui
        data class CopyToClipboard(val text: String) : Ui
        
        // Dialog Control
        object ShowConfig : Ui
        object ShowDebugSaveDialog : Ui
        object ShowLogWindow : Ui
        object DismissDialog : Ui
    }
}