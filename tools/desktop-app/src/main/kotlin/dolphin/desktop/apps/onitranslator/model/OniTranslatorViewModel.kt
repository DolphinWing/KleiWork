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
    private var textConverter: TextConverter? = null

    // A mutable state for windowConfig to be used by window state remembering
    private val _windowConfig = mutableStateOf(WindowConfig())
    private val windowConfig: State<WindowConfig> = _windowConfig

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch {
            loadInitialData()
        }
    }

    fun onEvent(event: AppEvent) {
        scope.launch {
            when (event) {
                is AppEvent.UiEvent -> handleUiEvent(event)
                is AppEvent.OnUiStateChange -> onUiStateChange(event.uiState)
                is AppEvent.OnCopyToClipboard -> {
                    copyToSystemClipboard(event.text)
                    SnackbarManager.showMessage(event.text)
                }
                is AppEvent.OnRefreshSource -> translate()
                is AppEvent.OnSearchTextChange -> search(event.text, event.searchType)
                is AppEvent.OnSearchActiveChange -> {
                    if (event.isActive) {
                        onUiStateChange(state.value.uiState.copy(isSearchActive = true))
                    } else {
                        search("") // clear search text
                        onUiStateChange(state.value.uiState.copy(isSearchActive = false))
                    }
                }
                is AppEvent.OnSaveDraft -> {
                    val (path, cost) = save(true) // Always cache for draft
                    if (cost > 0) {
                        SnackbarManager.showMessage(Res.string.toast_write_success, path, cost)
                    } else {
                        SnackbarManager.showMessage(Res.string.toast_write_failed)
                    }
                }
                is AppEvent.OnSaveFile -> {
                    val uiState = state.value.uiState
                    if (debugMode && uiState.dialogState == null) {
                        val save = requestSaveData()
                        if (save != null) {
                            val data = OniDialogState.DebugSaveDialog(
                                realFileName = save.realFile.absolutePath,
                                draftFileName = save.draftFile.absolutePath
                            )
                            onUiStateChange(uiState.copy(dialogState = data))
                        }
                    } else {
                        val (path, cost) = save(event.useCache)
                        if (cost > 0) {
                            SnackbarManager.showMessage(Res.string.toast_write_success, path, cost)
                        } else {
                            SnackbarManager.showMessage(Res.string.toast_write_failed)
                        }
                        onUiStateChange(uiState.copy(dialogState = null))
                    }
                }
                is AppEvent.OnConfigSaved -> {
                    saveConfig(event.configs)
                    onUiStateChange(state.value.uiState.copy(dialogState = null))
                }
                is AppEvent.OnConfigChange -> saveConfig(event.configs)
                is AppEvent.OnEditEntry -> {
                    val data = requestEditorData(event.entry)
                    onUiStateChange(state.value.uiState.copy(editorData = data))
                }
                is AppEvent.OnSaveEntry -> edit(event.entry.key, event.newText)
            }
        }
    }

    private fun handleUiEvent(event: AppEvent.UiEvent) {
        var uiState = state.value.uiState
        uiState = when (event) {
            is AppEvent.UiEvent.OnShowConfig ->
                uiState.copy(dialogState = OniDialogState.ConfigDialog(state.value.configs))
            is AppEvent.UiEvent.OnShowDebugSaveDialog -> {
                val save = requestSaveData()
                if (save != null) {
                    uiState.copy(
                        dialogState = OniDialogState.DebugSaveDialog(
                            realFileName = save.realFile.absolutePath,
                            draftFileName = save.draftFile.absolutePath
                        )
                    )
                } else {
                    uiState
                }
            }
            is AppEvent.UiEvent.OnDismissDialog -> uiState.copy(dialogState = null)
        }
        onUiStateChange(uiState)
    }

    private suspend fun loadInitialData() {
        val (configs, winConfig) = ConfigManager.load()
        _windowConfig.value = winConfig
        _state.update { it.copy(configs = configs, windowSize = winConfig.toDpSize(), windowPosition = winConfig.toWindowPosition()) }

        val replacementMap = ReplacementLoader(configs).load()
        val converter = TextConverter(replacementMap)
        textConverter = converter

        val newHelper = PoHelper(configs, converter, debugMode)
        helper = newHelper

        // Collect flows from the new helper instance
        scope.launch { newHelper.loading.collect { loading -> _state.update { it.copy(isLoading = loading) } } }
        scope.launch { newHelper.status.collect { status -> if (status.isNotBlank()) SnackbarManager.showMessage(status) } }

        if (newHelper.isConfigValid()) {
            translate()
        } else {
            _state.update { it.copy(uiState = it.uiState.copy(dialogState = OniDialogState.ConfigDialog(configs))) }
        }
    }

    private fun onUiStateChange(uiState: UiState) {
        _state.update { it.copy(uiState = uiState) }
    }

    private suspend fun saveConfig(newConfigs: Configs) = withContext(Dispatchers.IO) {
        ConfigManager.save(newConfigs, windowConfig.value)
        // Reload everything with the new configuration
        loadInitialData()
    }

    private suspend fun translate() = withContext(Dispatchers.IO) {
        helper?.runTranslationProcess()
        refreshDataSource()
        _state.update { it.copy(searchList = helper?.allValues()?.mapNotNull { entry -> requestEditorData(entry) } ?: emptyList()) }
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

    private suspend fun search(text: String, type: SearchType = state.value.searchType) = withContext(Dispatchers.IO) {
        _state.update { it.copy(searchText = text, searchType = type) }
        val searchResult = helper?.allValues()?.filter { item ->
            when (type) {
                SearchType.Origin -> item.origin()
                SearchType.Key -> item.key()
                SearchType.Text -> item.translated()
            }.contains(text, ignoreCase = true)
        }?.mapNotNull { entry -> requestEditorData(entry) } ?: emptyList()
        _state.update { it.copy(searchList = searchResult) }
    }

    suspend fun rememberLastWindowState(windowState: WindowState) {
        val pos = windowState.position
        val size = windowState.size
        if (pos is WindowPosition.Absolute) {
            val newWindowConfig = WindowConfig(
                x = pos.x.value,
                y = pos.y.value,
                width = size.width.value,
                height = size.height.value
            )
            _windowConfig.value = newWindowConfig
            ConfigManager.save(state.value.configs, newWindowConfig)
        }
    }

    private fun requestEditorData(entry: WordEntry?): EditorData? {
        val h = helper ?: return null
        return if (entry == null) null else {
            val key = entry.key
            val templateText = h.templated(key)?.origin() ?: entry.origin()
            val referenceText = h.simplified(key)?.translated()
            val draftText = h.translated(key)?.translated()
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
        val converter = textConverter ?: return text
        val traditional = TextConverter.sc2tc(text)
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