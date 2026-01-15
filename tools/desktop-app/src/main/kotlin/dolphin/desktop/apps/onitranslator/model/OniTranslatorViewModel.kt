package dolphin.desktop.apps.onitranslator.model

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import dolphin.desktop.apps.onitranslator.app.AppEvent
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.toast_write_failed
import dolphin.desktop.apps.onitranslator.generated.resources.toast_write_success
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

data class SaveData(val realFile: File, val draftFile: File)

/**
 * The main ViewModel for the OniTranslator application.
 * It orchestrates data loading, state management, and business logic, connecting
 * the UI with the data layers (ConfigManager, PoHelper, etc.).
 */
class OniTranslatorViewModel(appVersion: String, private val debugMode: Boolean) {
    private val _state = MutableStateFlow(AppState(appVersion = appVersion))
    val state: StateFlow<AppState> = _state.asStateFlow()

    private var helper: PoHelper? = null
    private var textRefinery: TextRefinery? = null

    // A mutable state for windowConfig to be used by window state remembering
    private val _windowConfig = mutableStateOf(WindowConfig())
    private val windowConfig: State<WindowConfig> = _windowConfig

    private val scope = CoroutineScope(Dispatchers.Default)
    private var autoClearJob: Job? = null

    init {
        scope.launch {
            loadInitialData()
        }
    }

    fun onEvent(event: AppEvent) {
        scope.launch {
            when (event) {
                // Search & Source
                is AppEvent.Search.TextChange -> search(event.text, event.type)
                is AppEvent.Search.ActiveChange -> onSearchActiveChange(event.isActive)
                is AppEvent.File.RefreshSource -> translate()

                // File I/O
                is AppEvent.File.SaveDraft -> saveFileInternal(useCache = true)
                is AppEvent.File.Save -> onSaveFileRequest(event.useCache)

                // Configuration
                is AppEvent.Config.Change -> saveConfig(event.configs)
                is AppEvent.Config.Saved -> {
                    saveConfig(event.configs)
                    updateUiState { it.copy(dialogState = null) }
                }

                // Editor
                is AppEvent.Editor.Select -> loadEditorData(event.entry)
                is AppEvent.Editor.Save -> edit(event.entry.key, event.newText)

                // UI & System
                is AppEvent.Ui -> onUiEvent(event)
            }
        }
    }

    private suspend fun onUiEvent(event: AppEvent.Ui) {
        when (event) {
            is AppEvent.Ui.ShowConfig ->
                updateUiState { it.copy(dialogState = OniDialogState.ConfigDialog(state.value.configs)) }
            is AppEvent.Ui.ShowDebugSaveDialog -> {
                val save = requestSaveData()
                if (save != null) {
                    updateUiState {
                        it.copy(
                            dialogState = OniDialogState.DebugSaveDialog(
                                realFileName = save.realFile.absolutePath,
                                draftFileName = save.draftFile.absolutePath
                            )
                        )
                    }
                }
            }
            is AppEvent.Ui.ShowLogWindow ->
                updateUiState { it.copy(dialogState = OniDialogState.LogWindow(logs = state.value.logs)) }
            is AppEvent.Ui.DismissDialog -> updateUiState { it.copy(dialogState = null) }
            is AppEvent.Ui.UpdateState -> updateUiState { event.uiState }
            is AppEvent.Ui.CopyToClipboard -> copyToClipboard(event.text)
            is AppEvent.Ui.ChangeTheme -> {
                updateUiState { it.copy(darkTheme = event.dark) }
                val newConfig = _windowConfig.value.copy(darkTheme = event.dark)
                _windowConfig.value = newConfig
                saveConfig(state.value.configs)
            }
        }
    }

    private suspend fun onSearchActiveChange(isActive: Boolean) {
        if (isActive) {
            updateUiState { it.copy(searchState = it.searchState.copy(isActive = true)) }
        } else {
            // clear search text when closing search
            search("")
            updateUiState { it.copy(searchState = it.searchState.copy(isActive = false)) }
        }
    }

    private suspend fun onSaveFileRequest(useCache: Boolean) {
        val uiState = state.value.uiState
        if (debugMode && uiState.dialogState == null) {
            val save = requestSaveData()
            if (save != null) {
                val dialog = OniDialogState.DebugSaveDialog(
                    realFileName = save.realFile.absolutePath,
                    draftFileName = save.draftFile.absolutePath
                )
                updateUiState { it.copy(dialogState = dialog) }
            }
        } else {
            saveFileInternal(useCache)
            updateUiState { it.copy(dialogState = null) }
        }
    }

    private suspend fun saveFileInternal(useCache: Boolean) {
        val (path, cost) = save(useCache)
        if (cost > 0) {
            SnackbarManager.showMessage(Res.string.toast_write_success, path, cost)
        } else {
            SnackbarManager.showMessage(Res.string.toast_write_failed)
        }
    }

    private fun loadEditorData(entry: WordEntry?) {
        val data = requestEditorData(entry)
        updateUiState { it.copy(editorData = data) }
    }

    private fun copyToClipboard(text: String) {
        copyToSystemClipboard(text)
        SnackbarManager.showMessage(text)
    }

    private fun updateStatus(message: String) {
        updateUiState { it.copy(processStatus = message) }

        // Auto-clear status after 3 seconds if not empty
        autoClearJob?.cancel()
        if (message.isNotBlank()) {
            autoClearJob = scope.launch {
                delay(3000)
                updateUiState { it.copy(processStatus = "") }
            }
        }
    }

    private suspend fun loadInitialData() {
        val (configs, winConfig) = ConfigManager.load()
        _windowConfig.value = winConfig
        _state.update {
            it.copy(
                configs = configs,
                uiState = it.uiState.copy(
                    windowSize = winConfig.toDpSize(),
                    windowPosition = winConfig.toWindowPosition(),
                    darkTheme = winConfig.darkTheme,
                )
            )
        }

        val dataBank = DataBank(configs).load()
        val refinery = TextRefinery(dataBank)
        textRefinery = refinery

        val newHelper = PoHelper(configs, refinery, debugMode)
        helper = newHelper

        // Collect flows from the new helper instance
        scope.launch {
            newHelper.loading.collect { loading ->
                updateUiState { it.copy(isLoading = loading) }
            }
        }
        scope.launch {
            newHelper.logs.collect { logs ->
                _state.update { it.copy(logs = logs) }
                val message = logs.lastOrNull()?.message ?: ""
                updateStatus(message)
            }
        }

        if (newHelper.isConfigValid()) {
            translate()
        } else {
            updateUiState { it.copy(dialogState = OniDialogState.ConfigDialog(configs)) }
        }
    }

    private fun updateUiState(block: (UiState) -> UiState) {
        _state.update { it.copy(uiState = block(it.uiState)) }
    }

    private suspend fun saveConfig(newConfigs: Configs) = withContext(Dispatchers.IO) {
        ConfigManager.save(newConfigs, windowConfig.value)
        // Reload everything with the new configuration
        loadInitialData()
    }

    private suspend fun translate() = withContext(Dispatchers.IO) {
        updateStatus("Translating...")
        helper?.runTranslationProcess()
        refreshDataSource()
        val results = helper?.allValues()?.mapNotNull { entry -> requestEditorData(entry) } ?: emptyList()
        updateUiState { it.copy(searchState = it.searchState.copy(results = results)) }
    }

    private suspend fun edit(key: String, value: String) {
        helper?.update(key, value)
        refreshDataSource()
    }

    private suspend fun refreshDataSource() = withContext(Dispatchers.IO) {
        val filtered = helper?.buildChangeList()?.mapNotNull { requestEditorData(it) } ?: emptyList()
        val list = filtered.map { it.target.changed }
        _state.update { it.copy(filteredList = filtered, changedList = list) }
    }

    private suspend fun save(cacheIt: Boolean = false): Pair<String, Long> = withContext(Dispatchers.IO) {
        val h = helper ?: return@withContext Pair("", -1L)
        val start = System.currentTimeMillis()
        val exported = h.getOutputFile(cacheIt)
        val result = h.writeTranslationFile(output = exported)
        val cost = System.currentTimeMillis() - start
        return@withContext Pair(exported.absolutePath, if (result) cost else -1)
    }

    private suspend fun search(text: String, type: SearchType = state.value.uiState.searchState.type) = withContext(Dispatchers.IO) {
        updateUiState { it.copy(searchState = it.searchState.copy(text = text, type = type)) }
        val searchResult = helper?.allValues()?.filter { item ->
            when (type) {
                SearchType.Origin -> item.origin()
                SearchType.Key -> item.key()
                SearchType.Text -> item.translated()
            }.contains(text, ignoreCase = true)
        }?.mapNotNull { entry -> requestEditorData(entry) } ?: emptyList()
        updateUiState { it.copy(searchState = it.searchState.copy(results = searchResult)) }
    }

    suspend fun rememberLastWindowState(windowState: WindowState) {
        val pos = windowState.position
        val size = windowState.size
        if (pos is WindowPosition.Absolute) {
            val newWindowConfig = WindowConfig(
                x = pos.x.value,
                y = pos.y.value,
                width = size.width.value,
                height = size.height.value,
                darkTheme = state.value.uiState.darkTheme,
            )
            _windowConfig.value = newWindowConfig
            ConfigManager.save(state.value.configs, newWindowConfig)
        }
    }

    private fun requestEditorData(entry: WordEntry?): EditorData? {
        val h = helper ?: return null
        return if (entry == null) null else {
            val key = entry.key
            val templateText = h.templateText(key)?.origin() ?: entry.origin()
            val referenceText = h.simplified(key)?.translated()
            val draftText = h.drafted(key)?.translated()
            EditorData(entry, templateText, referenceText, draftText)
        }
    }

    private fun requestSaveData(): SaveData? {
        val h = helper ?: return null
        val file1 = h.getOutputFile(false)
        val file2 = h.getOutputFile(true)
        return SaveData(file1, file2)
    }

    fun onConvert(text: String): String {
        val converter = textRefinery ?: return text
        val traditional = TextRefinery.sc2tc(text)
        return converter.refactor(traditional)
    }

    /**
     * Copying text to the clipboard using Java AWT.
     */
    private fun copyToSystemClipboard(text: String) {
        val stringSelection = java.awt.datatransfer.StringSelection(text)
        val clipboard: java.awt.datatransfer.Clipboard = java.awt.Toolkit.getDefaultToolkit().systemClipboard
        clipboard.setContents(stringSelection, null)
    }
}
