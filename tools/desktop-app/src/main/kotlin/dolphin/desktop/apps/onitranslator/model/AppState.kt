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
    // Keys of entries that have been modified in this session
    val changedList: List<String> = emptyList(),
    // Active filter mode for the entry list
    val filterMode: EntryFilterMode = EntryFilterMode.Pending,
    val logs: List<LogEntry> = emptyList(),
    val appVersion: String = "0.0.0",
    val hasDraft: Boolean = false,
    val glossaryTerms: List<GlossaryTerm> = emptyList(),
    val diagnosticCount: Int = 0,
    val glossaryCount: Int = 0,
    val geminiSessionStats: GeminiSessionStats = GeminiSessionStats(),
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
    val brigetteDesk: BrigetteDeskState = BrigetteDeskState(),
    val geminiModelsState: GeminiModelsState = GeminiModelsState(),
    val windowPosition: WindowPosition = WindowPosition.PlatformDefault,
    val windowSize: DpSize = WindowConfig().toDpSize(),
    val darkTheme: Boolean? = null,
)

/**
 * Cumulative Gemini API usage telemetry for the current application session.
 */
data class GeminiSessionStats(
    val requestCount: Int = 0,
    val totalPromptTokens: Int = 0,
    val totalCandidateTokens: Int = 0,
    val totalThoughtsTokens: Int = 0,
    val totalTokens: Int = 0,
    val totalElapsedMs: Long = 0L,
) {
    fun record(usage: GeminiUsage): GeminiSessionStats = copy(
        requestCount = requestCount + 1,
        totalPromptTokens = totalPromptTokens + usage.promptTokens,
        totalCandidateTokens = totalCandidateTokens + usage.candidateTokens,
        totalThoughtsTokens = totalThoughtsTokens + usage.thoughtsTokens,
        totalTokens = totalTokens + usage.totalTokens,
        totalElapsedMs = totalElapsedMs + usage.elapsedMs,
    )
}

/**
 * Transient state for loading available Gemini models in ConfigScreen.
 */
data class GeminiModelsState(
    val isLoading: Boolean = false,
    val models: List<GeminiModelInfo> = emptyList(),
    val error: String? = null,
)

/**
 * Transient overlay state for the BrigetteDesk AI translation assistant.
 */
data class BrigetteDeskState(
    val visible: Boolean = false,
    val entryKey: String = "",
    val contextOptions: PromptContextOptions = PromptContextOptions(),
    val contextSnapshot: PromptContextSnapshot? = null,
    val builtPromptPreview: String = "",
    val editablePrompt: String = "",
    val isLoading: Boolean = false,
    val startedAtMs: Long = 0L,
    val result: GeminiTranslationResult? = null,
    val selectedOptionIndex: Int = 0,
    val error: GeminiError? = null,
) {
    val isPromptEdited: Boolean
        get() = editablePrompt != builtPromptPreview
}

/**
 * Encapsulates the state of the search functionality.
 */
data class SearchState(
    val isActive: Boolean = false,
    val text: String = "",
)

/**
 * Defines the possible states for application-level dialogs and overlays.
 */
sealed interface OniDialogState {
    data class DebugSaveDialog(val realFileName: String = "", val draftFileName: String = "") : OniDialogState
    data class ConfigDialog(val configs: Configs) : OniDialogState
    data class LogWindow(val logs: List<LogEntry>) : OniDialogState
    data class JumpDialog(val totalCount: Int) : OniDialogState
    data class ExitConfirmDialog(val changedCount: Int) : OniDialogState
}

