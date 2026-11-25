package dolphin.desktop.apps.onitranslator.app

import dolphin.desktop.apps.onitranslator.model.Configs
import dolphin.desktop.apps.onitranslator.model.EditorData
import dolphin.desktop.apps.onitranslator.model.EntryFilterMode
import dolphin.desktop.apps.onitranslator.model.UiState
import dolphin.desktop.apps.onitranslator.model.PoEntry
import dolphin.desktop.apps.onitranslator.model.GlossaryConflict
import dolphin.desktop.apps.onitranslator.model.PromptContextOptions

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
        data class TextChange(val text: String) : Search
    }

    /**
     * Entry browser list filtering events.
     */
    sealed interface Browser : AppEvent {
        data class FilterChange(val mode: EntryFilterMode) : Browser
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

        // Query Google AI Studio for available generateContent models
        data class FetchGeminiModels(val apiKey: String) : Config
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
        object ShowJumpDialog : Ui
        object ShowExitConfirmDialog : Ui
        object DismissDialog : Ui
        data class ChangeTheme(val dark: Boolean) : Ui
    }

    /**
     * Terminology consistency and glossary actions.
     */
    sealed interface Glossary : AppEvent {
        data class ApplyCanonical(val entry: PoEntry, val conflict: GlossaryConflict) : Glossary
        data class AcceptNewTerm(val entryKey: String) : Glossary
        data class ResetToCanonical(val entryKey: String) : Glossary
    }

    /**
     * BrigetteDesk Gemini translation assistant actions.
     */
    sealed interface Brigette : AppEvent {
        data class Request(
            val entry: EditorData,
            val optionsOverride: PromptContextOptions? = null,
        ) : Brigette
        object Cancel : Brigette
        data class UpdateContextOptions(val options: PromptContextOptions) : Brigette
        data class SelectOption(val index: Int) : Brigette
        data class ApplyOption(val entry: PoEntry, val text: String) : Brigette
        object Dismiss : Brigette
    }
}
