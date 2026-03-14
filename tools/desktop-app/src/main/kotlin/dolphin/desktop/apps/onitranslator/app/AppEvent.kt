package dolphin.desktop.apps.onitranslator.app

import dolphin.desktop.apps.onitranslator.model.Configs
import dolphin.desktop.apps.onitranslator.model.SearchType
import dolphin.desktop.apps.onitranslator.model.UiState
import dolphin.desktop.apps.onitranslator.model.PoEntry

/**
 * Represents all possible user interactions and system events that can trigger state changes
 * within the application's MVI-style architecture.
 */
sealed interface AppEvent {
    /**
     * Events related to the search bar and filtering logic.
     */
    sealed interface Search : AppEvent {
        // Toggle the search bar's active focus or visibility state
        data class ActiveChange(val isActive: Boolean) : Search

        // Triggered when user types or changes the search target (e.g., Key, Value)
        data class TextChange(val text: String, val type: SearchType) : Search
    }

    /**
     * File I/O operations for saving progress or refreshing data sources.
     */
    sealed interface File : AppEvent {
        // Save a temporary copy of the current translation work
        object SaveDraft : File

        // Export the translation data to its final destination
        data class Save(val useCache: Boolean) : File

        // Force a reload of the source translation files from disk
        object RefreshSource : File

        // Delete the temporary draft file
        object DeleteDraft : File

        // Export a terminology glossary (TSV)
        object ExportGlossary : File
    }

    /**
     * Configuration management events.
     */
    sealed interface Config : AppEvent {
        // Request to change the current in-memory configuration settings
        data class Change(val configs: Configs) : Config

        // Successfully saved configuration to persistent storage
        data class Saved(val configs: Configs) : Config
    }

    /**
     * Actions performed within the translation editor workspace.
     */
    sealed interface Editor : AppEvent {
        // Select a specific translation entry to display in the editor panel
        data class Select(val entry: PoEntry?) : Editor

        // Update the content of a specific translation entry
        data class Save(val entry: PoEntry, val newText: String) : Editor

        // Smart Copy from AI
        data class SmartCopyError(val message: String) : Editor
    }

    /**
     * Global UI controls and system-level actions.
     */
    sealed interface Ui : AppEvent {
        // Bulk update the transient UI state
        data class UpdateState(val uiState: UiState) : Ui

        // Copy provided text to the system clipboard
        data class CopyToClipboard(val text: String) : Ui

        // Dialog visibility management
        object ShowConfig : Ui
        object ShowDebugSaveDialog : Ui
        object ShowLogWindow : Ui
        object DismissDialog : Ui
        data class ChangeTheme(val dark: Boolean) : Ui
    }
}
