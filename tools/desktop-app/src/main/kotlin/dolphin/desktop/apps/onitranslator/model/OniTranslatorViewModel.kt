package dolphin.desktop.apps.onitranslator.model

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import dolphin.desktop.apps.onitranslator.app.AppEvent
import dolphin.desktop.apps.onitranslator.generated.resources.Res
import dolphin.desktop.apps.onitranslator.generated.resources.brigette_handoff_copied
import dolphin.desktop.apps.onitranslator.generated.resources.toast_draft_cleared
import dolphin.desktop.apps.onitranslator.generated.resources.toast_glossary_copied
import dolphin.desktop.apps.onitranslator.generated.resources.toast_write_failed
import dolphin.desktop.apps.onitranslator.generated.resources.toast_write_success
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val geminiClient = GeminiClient()
    private var cachedSystemPrompt: String? = null

    // A mutable state for windowConfig to be used by window state remembering
    private val _windowConfig = mutableStateOf(WindowConfig())
    private val windowConfig: State<WindowConfig> = _windowConfig

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val ioMutex = Mutex()
    private var autoClearJob: Job? = null
    private var autoSaveJob: Job? = null
    private var aiRequestJob: Job? = null
    private var modelListJob: Job? = null
    private val recentTranslations = ArrayDeque<RecentTranslationContext>()

    init {
        scope.launch {
            loadInitialData()
        }
    }

    fun onEvent(event: AppEvent) {
        scope.launch {
            when (event) {
                // Search & Source
                is AppEvent.Browser.FilterChange -> onFilterModeChange(event.mode)
                is AppEvent.Search.TextChange -> search(event.text)
                is AppEvent.Search.ActiveChange -> onSearchActiveChange(event.isActive)
                is AppEvent.File.RefreshSource -> translate()

                // File I/O
                is AppEvent.File.SaveDraft -> saveFileInternal(useCache = true)
                is AppEvent.File.Save -> onSaveFileRequest(event.useCache)
                is AppEvent.File.DeleteDraft -> {
                    autoSaveJob?.cancel()
                    val result = ioMutex.withLock {
                        helper?.clearDrafts() == true
                    }
                    if (result) {
                        SnackbarManager.showMessage(Res.string.toast_draft_cleared)
                        refreshDataSource()
                    }
                }

                is AppEvent.File.ExportGlossary -> {
                    val file = helper?.exportGlossary()
                    if (file != null) {
                        val terms = helper?.glossaryBank?.terms ?: emptyList()
                        _state.update { it.copy(glossaryTerms = terms) }
                        refreshDataSource()
                        SnackbarManager.showMessage(Res.string.toast_write_success, file.name, 0)
                    } else {
                        SnackbarManager.showMessage(Res.string.toast_write_failed)
                    }
                }

                // Configuration
                is AppEvent.Config.Change -> {
                    _state.update { it.copy(configs = event.configs) }
                }
                is AppEvent.Config.Saved -> {
                    modelListJob?.cancel()
                    val oldConfigs = state.value.configs
                    val newConfigs = event.configs
                    _state.update { it.copy(configs = newConfigs) }

                    // Only reload data if paths have changed
                    val pathChanged = oldConfigs.oniWorkshopDir != newConfigs.oniWorkshopDir ||
                            oldConfigs.oniAssetsDir != newConfigs.oniAssetsDir ||
                            oldConfigs.dataBankPath != newConfigs.dataBankPath

                    if (!newConfigs.autoSaveEnabled) {
                        autoSaveJob?.cancel()
                    }
                    saveConfig(newConfigs, shouldReload = pathChanged)
                    updateUiState { it.copy(dialogState = null) }
                }
                is AppEvent.Config.FetchGeminiModels -> fetchGeminiModels(event.apiKey)

                // Editor
                is AppEvent.Editor.Select -> onSelectEditorEntry(event.entry)
                is AppEvent.Editor.Save -> edit(event.entry.key, event.newText)
                is AppEvent.Editor.SmartCopyError -> SnackbarManager.showMessage(event.message)

                // Glossary
                is AppEvent.Glossary.ApplyCanonical -> {
                    val chs = helper?.chsEntry(event.entry.key)?.msgStr()
                    val oldText = event.entry.msgStr()
                    val newText = GlossarySensor.applyCanonical(oldText, event.conflict, chs)
                    if (newText != oldText) {
                        edit(event.entry.key, newText)
                    } else {
                        // Fallback: unable to safely auto-replace; copy canonical term to clipboard and notify
                        copyToSystemClipboard(event.conflict.canonicalTranslation)
                        SnackbarManager.showMessage(Res.string.toast_glossary_copied, event.conflict.canonicalTranslation)
                    }
                }
                is AppEvent.Glossary.AcceptNewTerm -> {
                    helper?.acceptTerm(event.entryKey)
                    refreshDataSource()
                    scheduleAutoSave()
                }
                is AppEvent.Glossary.ResetToCanonical -> {
                    helper?.resetTerm(event.entryKey)
                    refreshDataSource()
                    scheduleAutoSave()
                }

                // BrigetteDesk AI Assistant
                is AppEvent.Brigette -> onBrigetteEvent(event)

                // UI & System
                is AppEvent.Ui -> onUiEvent(event)
            }
        }
    }

    private suspend fun onUiEvent(event: AppEvent.Ui) {
        when (event) {
            is AppEvent.Ui.ShowConfig ->
                updateUiState {
                    it.copy(
                        dialogState = OniDialogState.ConfigDialog(state.value.configs),
                        geminiModelsState = it.geminiModelsState.copy(error = null),
                    )
                }

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

            is AppEvent.Ui.ShowJumpDialog -> {
                val count = state.value.filteredList.size
                if (count > 0) {
                    updateUiState { it.copy(dialogState = OniDialogState.JumpDialog(totalCount = count)) }
                }
            }

            is AppEvent.Ui.ShowExitConfirmDialog -> {
                val count = state.value.changedList.size
                if (count > 0) {
                    updateUiState { it.copy(dialogState = OniDialogState.ExitConfirmDialog(changedCount = count)) }
                }
            }

            is AppEvent.Ui.DismissDialog -> updateUiState { it.copy(dialogState = null) }
            is AppEvent.Ui.UpdateState -> updateUiState { event.uiState }
            is AppEvent.Ui.CopyToClipboard -> copyToClipboard(event.text)
            is AppEvent.Ui.ChangeTheme -> {
                updateUiState { it.copy(darkTheme = event.dark) }
                val newConfig = _windowConfig.value.copy(darkTheme = event.dark)
                _windowConfig.value = newConfig
                // Only save the config, do not reload data
                updateThemeConfig(newConfig)
            }
        }
    }

    private suspend fun updateThemeConfig(newConfig: WindowConfig) = withContext(Dispatchers.IO) {
        ConfigManager.save(state.value.configs, newConfig)
    }

    private suspend fun onFilterModeChange(mode: EntryFilterMode) {
        if (state.value.filterMode == mode) return
        _state.update { it.copy(filterMode = mode) }
        refreshFilter()
    }

    private suspend fun onSearchActiveChange(isActive: Boolean) {
        if (state.value.uiState.searchState.isActive == isActive) return
        updateUiState {
            it.copy(
                searchState = it.searchState.copy(
                    isActive = isActive,
                    text = if (!isActive) "" else it.searchState.text
                )
            )
        }
        refreshFilter()
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

    private suspend fun saveFileInternal(useCache: Boolean) = ioMutex.withLock {
        if (!useCache) {
            autoSaveJob?.cancel()
        }
        saveFileLocked(useCache)
    }

    private suspend fun saveFileLocked(useCache: Boolean) {
        val (path, cost) = save(useCache)
        if (cost > 0) {
            SnackbarManager.showMessage(Res.string.toast_write_success, path, cost)
            if (!useCache) {
                // Formal save committed all changes to strings.po: sync in-memory baselines
                helper?.commitFormalSave()
            }
            refreshDataSource()
        } else {
            SnackbarManager.showMessage(Res.string.toast_write_failed)
        }
    }

    suspend fun saveDraftSync(): Boolean {
        autoSaveJob?.cancel()
        return ioMutex.withLock {
            val (_, cost) = save(cacheIt = true)
            cost > 0
        }
    }

    private fun loadEditorData(entry: PoEntry?) {
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

    private suspend fun saveConfig(newConfigs: Configs, shouldReload: Boolean = true) = withContext(Dispatchers.IO) {
        ConfigManager.save(newConfigs, windowConfig.value)
        if (shouldReload) {
            // Reload everything with the new configuration
            loadInitialData()
        }
    }

    private suspend fun translate() = withContext(Dispatchers.IO) {
        updateStatus("Translating...")
        helper?.runTranslationProcess()
        val terms = helper?.glossaryBank?.terms ?: emptyList()
        _state.update { it.copy(glossaryTerms = terms) }
        refreshDataSource()
    }

    private suspend fun edit(key: String, value: String) {
        helper?.update(key, value)
        recordRecentTranslation(key, value)
        refreshDataSource()
        scheduleAutoSave()
    }

    private fun recordRecentTranslation(key: String, value: String) {
        val cleanKey = key.trim('"')
        val sourceText = helper?.sourceEntry(key)?.msgId() ?: helper?.entry(key)?.msgId() ?: ""
        if (cleanKey.isBlank() || value.isBlank() || value == sourceText) return

        recentTranslations.removeAll { it.key == cleanKey }
        recentTranslations.addFirst(
            RecentTranslationContext(
                key = cleanKey,
                sourceText = sourceText,
                translatedText = value,
            )
        )
        while (recentTranslations.size > PromptComposer.DEFAULT_MAX_RECENT) {
            recentTranslations.removeLast()
        }
    }

    private fun onSelectEditorEntry(entry: PoEntry?) {
        val targetKey = entry?.key().orEmpty()
        val currentDesk = state.value.uiState.brigetteDesk
        if (currentDesk.entryKey.isNotBlank() && currentDesk.entryKey != targetKey) {
            aiRequestJob?.cancel()
            updateUiState {
                it.copy(
                    brigetteDesk = BrigetteDeskState(contextOptions = it.brigetteDesk.contextOptions)
                )
            }
        }
        loadEditorData(entry)
    }

    private fun fetchGeminiModels(rawApiKey: String) {
        modelListJob?.cancel()
        val effectiveKey = rawApiKey.trim().ifBlank { state.value.configs.effectiveGeminiApiKey() }
        updateUiState {
            it.copy(geminiModelsState = it.geminiModelsState.copy(isLoading = true, error = null))
        }
        modelListJob = scope.launch {
            val response = withContext(Dispatchers.IO) {
                geminiClient.listModels(effectiveKey)
            }
            when (response) {
                is GeminiModelListResponse.Success -> {
                    updateUiState {
                        it.copy(
                            geminiModelsState = GeminiModelsState(
                                isLoading = false,
                                models = response.models,
                                error = null,
                            )
                        )
                    }
                    helper?.log("Loaded ${response.models.size} Gemini models.")
                }

                is GeminiModelListResponse.Failure -> {
                    updateUiState {
                        it.copy(
                            geminiModelsState = it.geminiModelsState.copy(
                                isLoading = false,
                                error = response.error.message,
                            )
                        )
                    }
                    helper?.log("Failed to load Gemini models: ${response.error.message}", LogType.Warning)
                }
            }
        }
    }

    private suspend fun loadSystemPrompt(): String {
        cachedSystemPrompt?.let { return it }
        val loaded = withContext(Dispatchers.IO) {
            Res.readBytes(PromptComposer.SYSTEM_PROMPT_RESOURCE).decodeToString()
        }
        cachedSystemPrompt = loaded
        return loaded
    }

    private suspend fun composeBrief(entry: EditorData, options: PromptContextOptions): TranslationBrief {
        val systemPrompt = loadSystemPrompt()
        val recentSnapshot = recentTranslations.toList()
        val allEntries = helper?.allEntries().orEmpty()
        val glossaryTerms = state.value.glossaryTerms
        return PromptComposer.compose(
            entry = entry,
            systemInstruction = systemPrompt,
            glossaryTerms = glossaryTerms,
            allEntries = allEntries,
            chsLookup = { key -> helper?.chsEntry(key)?.msgStr() },
            recentTranslations = recentSnapshot,
            options = options,
        )
    }

    private suspend fun onBrigetteEvent(event: AppEvent.Brigette) {
        when (event) {
            is AppEvent.Brigette.Open -> {
                aiRequestJob?.cancel()
                val targetKey = event.entry.target.key()
                val options = state.value.uiState.brigetteDesk.contextOptions
                val brief = try {
                    composeBrief(event.entry, options)
                } catch (e: Exception) {
                    updateUiState {
                        it.copy(
                            brigetteDesk = it.brigetteDesk.copy(
                                visible = true,
                                entryKey = targetKey,
                                contextOptions = options,
                                isLoading = false,
                                rounds = emptyList(),
                                pendingFollowUp = "",
                                error = GeminiError.ParseError("Failed to load system prompt: ${e.message}"),
                            )
                        )
                    }
                    return
                }
                updateUiState {
                    it.copy(
                        brigetteDesk = BrigetteDeskState(
                            visible = true,
                            entryKey = targetKey,
                            contextOptions = options,
                            contextSnapshot = brief.snapshot,
                            builtPromptPreview = brief.userPrompt,
                            editablePrompt = brief.userPrompt,
                            isLoading = false,
                            startedAtMs = 0L,
                            rounds = emptyList(),
                            selectedRoundIndex = 0,
                            selectedOptionIndex = 0,
                            pendingFollowUp = "",
                            error = null,
                        )
                    )
                }
            }

            is AppEvent.Brigette.Request -> {
                aiRequestJob?.cancel()
                val targetKey = event.entry.target.key()
                val currentDesk = state.value.uiState.brigetteDesk
                val options = event.optionsOverride ?: currentDesk.contextOptions
                val baseBrief = try {
                    composeBrief(event.entry, options)
                } catch (e: Exception) {
                    updateUiState {
                        it.copy(
                            brigetteDesk = it.brigetteDesk.copy(
                                visible = true,
                                entryKey = targetKey,
                                contextOptions = options,
                                isLoading = false,
                                rounds = emptyList(),
                                error = GeminiError.ParseError("Failed to load system prompt: ${e.message}"),
                            )
                        )
                    }
                    return
                }

                val effectivePrompt = event.promptOverride?.takeIf { it.isNotBlank() }
                    ?: currentDesk.editablePrompt.takeIf { it.isNotBlank() && currentDesk.entryKey == targetKey }
                    ?: baseBrief.userPrompt
                val brief = baseBrief.copy(userPrompt = effectivePrompt)

                val configs = state.value.configs
                val effectiveKey = configs.effectiveGeminiApiKey()
                val model = configs.geminiModel.trim()

                if (effectiveKey.isBlank()) {
                    updateUiState {
                        it.copy(
                            brigetteDesk = it.brigetteDesk.copy(
                                visible = true,
                                entryKey = targetKey,
                                contextOptions = options,
                                contextSnapshot = baseBrief.snapshot,
                                builtPromptPreview = baseBrief.userPrompt,
                                editablePrompt = effectivePrompt,
                                isLoading = false,
                                rounds = emptyList(),
                                pendingFollowUp = "",
                                error = GeminiError.MissingApiKey(),
                            )
                        )
                    }
                    return
                }

                if (model.isBlank()) {
                    updateUiState {
                        it.copy(
                            brigetteDesk = it.brigetteDesk.copy(
                                visible = true,
                                entryKey = targetKey,
                                contextOptions = options,
                                contextSnapshot = baseBrief.snapshot,
                                builtPromptPreview = baseBrief.userPrompt,
                                editablePrompt = effectivePrompt,
                                isLoading = false,
                                rounds = emptyList(),
                                pendingFollowUp = "",
                                error = GeminiError.ModelNotFound("Gemini model ID is not configured.", 404),
                            )
                        )
                    }
                    return
                }

                updateUiState {
                    it.copy(
                        brigetteDesk = it.brigetteDesk.copy(
                            visible = true,
                            entryKey = targetKey,
                            contextOptions = options,
                            contextSnapshot = baseBrief.snapshot,
                            builtPromptPreview = baseBrief.userPrompt,
                            editablePrompt = effectivePrompt,
                            isLoading = true,
                            startedAtMs = System.currentTimeMillis(),
                            rounds = emptyList(),
                            selectedRoundIndex = 0,
                            selectedOptionIndex = 0,
                            pendingFollowUp = "",
                            error = null,
                        )
                    )
                }

                aiRequestJob = scope.launch {
                    val response = withContext(Dispatchers.IO) {
                        geminiClient.generateTranslation(
                            brief = brief,
                            apiKey = effectiveKey,
                            model = model,
                        )
                    }
                    val latestDesk = state.value.uiState.brigetteDesk
                    if (latestDesk.entryKey != targetKey || !latestDesk.visible) return@launch

                    when (response) {
                        is GeminiResponse.Success -> {
                            val res = response.result
                            val round1 = BrigetteRound(
                                userPrompt = effectivePrompt,
                                isFollowUp = false,
                                result = res,
                            )
                            _state.update {
                                it.copy(
                                    geminiSessionStats = it.geminiSessionStats.record(res.usage),
                                    uiState = it.uiState.copy(
                                        brigetteDesk = it.uiState.brigetteDesk.copy(
                                            isLoading = false,
                                            rounds = listOf(round1),
                                            selectedRoundIndex = 0,
                                            selectedOptionIndex = res.payload.recommendedIndex,
                                            pendingFollowUp = "",
                                            error = null,
                                        )
                                    )
                                )
                            }
                            helper?.log(
                                "BrigetteDesk [${res.usage.modelName}] $targetKey: ${res.usage.elapsedMs}ms, ${res.usage.totalTokens} tokens"
                            )
                        }

                        is GeminiResponse.Failure -> {
                            updateUiState {
                                it.copy(
                                    brigetteDesk = it.brigetteDesk.copy(
                                        isLoading = false,
                                        error = response.error,
                                    )
                                )
                            }
                            helper?.log(
                                "BrigetteDesk failed for $targetKey: ${response.error.message}",
                                LogType.Warning,
                            )
                        }
                    }
                }
            }

            is AppEvent.Brigette.FollowUp -> {
                val trimmedMessage = event.message.trim()
                val currentDesk = state.value.uiState.brigetteDesk
                if (trimmedMessage.isBlank() || currentDesk.rounds.isEmpty()) return

                aiRequestJob?.cancel()

                val configs = state.value.configs
                val effectiveKey = configs.effectiveGeminiApiKey()
                val model = configs.geminiModel.trim()

                if (effectiveKey.isBlank()) {
                    updateUiState {
                        it.copy(
                            brigetteDesk = it.brigetteDesk.copy(
                                isLoading = false,
                                pendingFollowUp = trimmedMessage,
                                error = GeminiError.MissingApiKey(),
                            )
                        )
                    }
                    return
                }

                if (model.isBlank()) {
                    updateUiState {
                        it.copy(
                            brigetteDesk = it.brigetteDesk.copy(
                                isLoading = false,
                                pendingFollowUp = trimmedMessage,
                                error = GeminiError.ModelNotFound("Gemini model ID is not configured.", 404),
                            )
                        )
                    }
                    return
                }

                val systemPrompt = try {
                    loadSystemPrompt()
                } catch (e: Exception) {
                    updateUiState {
                        it.copy(
                            brigetteDesk = it.brigetteDesk.copy(
                                isLoading = false,
                                pendingFollowUp = trimmedMessage,
                                error = GeminiError.ParseError("Failed to load system prompt: ${e.message}"),
                            )
                        )
                    }
                    return
                }

                val history = currentDesk.rounds.flatMap { round ->
                    listOf(
                        GeminiTurn(GeminiTurn.ROLE_USER, round.userPrompt),
                        GeminiTurn(GeminiTurn.ROLE_MODEL, round.result.rawJson),
                    )
                }
                val targetKey = currentDesk.entryKey
                val baseRoundCount = currentDesk.rounds.size
                val brief = TranslationBrief(
                    systemInstruction = systemPrompt,
                    userPrompt = trimmedMessage,
                    snapshot = currentDesk.contextSnapshot ?: PromptContextSnapshot(),
                )

                updateUiState {
                    it.copy(
                        brigetteDesk = it.brigetteDesk.copy(
                            isLoading = true,
                            startedAtMs = System.currentTimeMillis(),
                            pendingFollowUp = trimmedMessage,
                            error = null,
                        )
                    )
                }

                aiRequestJob = scope.launch {
                    val response = withContext(Dispatchers.IO) {
                        geminiClient.generateTranslation(
                            brief = brief,
                            apiKey = effectiveKey,
                            model = model,
                            history = history,
                            followUpMessage = trimmedMessage,
                        )
                    }
                    val latestDesk = state.value.uiState.brigetteDesk
                    if (latestDesk.entryKey != targetKey ||
                        !latestDesk.visible ||
                        latestDesk.rounds.size != baseRoundCount
                    ) {
                        return@launch
                    }

                    when (response) {
                        is GeminiResponse.Success -> {
                            val res = response.result
                            val nextRound = BrigetteRound(
                                userPrompt = trimmedMessage,
                                isFollowUp = true,
                                result = res,
                            )
                            _state.update {
                                val updatedRounds = it.uiState.brigetteDesk.rounds + nextRound
                                it.copy(
                                    geminiSessionStats = it.geminiSessionStats.record(res.usage),
                                    uiState = it.uiState.copy(
                                        brigetteDesk = it.uiState.brigetteDesk.copy(
                                            isLoading = false,
                                            rounds = updatedRounds,
                                            selectedRoundIndex = updatedRounds.lastIndex,
                                            selectedOptionIndex = res.payload.recommendedIndex,
                                            pendingFollowUp = "",
                                            error = null,
                                        )
                                    )
                                )
                            }
                            helper?.log(
                                "BrigetteDesk [R${baseRoundCount + 1} ${res.usage.modelName}] $targetKey: ${res.usage.elapsedMs}ms, ${res.usage.totalTokens} tokens"
                            )
                        }

                        is GeminiResponse.Failure -> {
                            updateUiState {
                                it.copy(
                                    brigetteDesk = it.brigetteDesk.copy(
                                        isLoading = false,
                                        error = response.error,
                                    )
                                )
                            }
                            helper?.log(
                                "BrigetteDesk follow-up failed for $targetKey: ${response.error.message}",
                                LogType.Warning,
                            )
                        }
                    }
                }
            }

            is AppEvent.Brigette.Cancel -> {
                aiRequestJob?.cancel()
                updateUiState {
                    it.copy(
                        brigetteDesk = it.brigetteDesk.copy(
                            isLoading = false,
                            error = GeminiError.Cancelled(),
                        )
                    )
                }
            }

            is AppEvent.Brigette.UpdateContextOptions -> {
                val brief = try {
                    composeBrief(event.entry, event.options)
                } catch (_: Exception) {
                    null
                }
                updateUiState {
                    it.copy(
                        brigetteDesk = it.brigetteDesk.copy(
                            contextOptions = event.options,
                            contextSnapshot = brief?.snapshot ?: it.brigetteDesk.contextSnapshot,
                            builtPromptPreview = brief?.userPrompt ?: it.brigetteDesk.builtPromptPreview,
                            editablePrompt = brief?.userPrompt ?: it.brigetteDesk.editablePrompt,
                        )
                    )
                }
            }

            is AppEvent.Brigette.UpdatePrompt -> {
                updateUiState {
                    it.copy(brigetteDesk = it.brigetteDesk.copy(editablePrompt = event.text))
                }
            }

            is AppEvent.Brigette.ResetPrompt -> {
                updateUiState {
                    it.copy(
                        brigetteDesk = it.brigetteDesk.copy(editablePrompt = it.brigetteDesk.builtPromptPreview)
                    )
                }
            }

            is AppEvent.Brigette.SelectOption -> {
                updateUiState {
                    val desk = it.brigetteDesk
                    val lastRound = desk.rounds.lastIndex.coerceAtLeast(0)
                    val targetRound = (event.roundIndex ?: lastRound).coerceIn(0, lastRound)
                    it.copy(
                        brigetteDesk = desk.copy(
                            selectedRoundIndex = targetRound,
                            selectedOptionIndex = event.index,
                        )
                    )
                }
            }

            is AppEvent.Brigette.ApplyOption -> {
                edit(event.entry.key, event.text)
                updateUiState {
                    it.copy(brigetteDesk = it.brigetteDesk.copy(visible = false, isLoading = false))
                }
            }

            is AppEvent.Brigette.CopyHandoff -> {
                val currentDesk = state.value.uiState.brigetteDesk
                if (currentDesk.rounds.isNotEmpty()) {
                    val initialPrompt = currentDesk.rounds.first().userPrompt.ifBlank {
                        currentDesk.editablePrompt
                    }
                    val markdown = PromptComposer.formatHandoffMarkdown(
                        entryKey = currentDesk.entryKey,
                        initialPrompt = initialPrompt,
                        rounds = currentDesk.rounds,
                        selectedRoundIndex = currentDesk.selectedRoundIndex,
                        selectedOptionIndex = currentDesk.selectedOptionIndex,
                        draftFollowUp = event.draftFollowUp,
                    )
                    copyToSystemClipboard(markdown)
                    SnackbarManager.showMessage(Res.string.brigette_handoff_copied)
                }
            }

            is AppEvent.Brigette.Dismiss -> {
                aiRequestJob?.cancel()
                updateUiState {
                    it.copy(brigetteDesk = it.brigetteDesk.copy(visible = false, isLoading = false))
                }
            }
        }
    }

    private fun scheduleAutoSave() {
        val configs = state.value.configs
        if (!configs.autoSaveEnabled) return

        autoSaveJob?.cancel()
        autoSaveJob = scope.launch {
            delay(configs.autoSaveIntervalMinutes * 60 * 1000L)
            ioMutex.withLock {
                // Re-check inside lock to avoid resurrecting drafts after a concurrent formal save
                if (state.value.changedList.isNotEmpty()) {
                    saveFileLocked(useCache = true)
                    helper?.log("Auto-saved draft after ${configs.autoSaveIntervalMinutes} minutes of idle.")
                }
            }
        }
    }

    /**
     * Refreshes global counters, draft status, active editor data, and the filtered list after data mutations.
     */
    private suspend fun refreshDataSource() = withContext(Dispatchers.IO) {
        val h = helper ?: return@withContext
        val all = h.allEntries()
        val diagCount = all.count { it.diagnostic?.hasIssue == true }
        val glossCount = all.count { it.glossaryDiag?.isUnreviewed == true }
        val hasDraft = h.hasDraft()
        val changedKeys = all.filter { it.changed > 0 }.map { it.key }

        val computed = computeFilteredList(h, all)

        val currentKey = state.value.uiState.editorData?.target?.key
        val refreshedEditorData = currentKey?.let { key ->
            h.entry(key)?.let { requestEditorData(it) }
        }

        _state.update {
            it.copy(
                filteredList = computed,
                changedList = changedKeys,
                hasDraft = hasDraft,
                diagnosticCount = diagCount,
                glossaryCount = glossCount,
                uiState = it.uiState.copy(
                    editorData = refreshedEditorData,
                )
            )
        }
    }

    /**
     * Re-evaluates only the visible filtered list for pure view changes (search keystrokes, tab switches).
     */
    private suspend fun refreshFilter() = withContext(Dispatchers.IO) {
        val h = helper ?: return@withContext
        val computed = computeFilteredList(h, h.allEntries())
        _state.update { it.copy(filteredList = computed) }
    }

    private fun computeFilteredList(h: PoHelper, all: List<PoEntry>): List<EditorData> {
        val sState = state.value.uiState.searchState
        val filterMode = state.value.filterMode

        // 1. Determine base entries by active filterMode
        val baseEntries: List<PoEntry> = when (filterMode) {
            EntryFilterMode.Pending -> h.buildChangeList()
            EntryFilterMode.Diagnostic -> all.filter { it.diagnostic?.hasIssue == true }
            EntryFilterMode.Glossary -> all.filter { it.glossaryDiag?.isUnreviewed == true }
            EntryFilterMode.All -> all
        }

        // 2. If search is active with a query, perform compound/orthogonal filtering
        if (sState.isActive && sState.text.isNotBlank()) {
            val query = sState.text
            val cleanQuery = query.normalizeTextForSearch()
            val normalizedKeyQuery = query.normalizeKeyForSearch()
            val searchResult = baseEntries.filter { item ->
                item.key().contains(query, ignoreCase = true) ||
                item.key().normalizeKeyForSearch().contains(normalizedKeyQuery, ignoreCase = true) ||
                item.msgId().matchesSearch(cleanQuery) ||
                item.msgStr().matchesSearch(cleanQuery)
            }
            return searchResult.mapNotNull { requestEditorData(it) }
        }

        return baseEntries.mapNotNull { requestEditorData(it) }
    }

    private suspend fun save(cacheIt: Boolean = false): Pair<String, Long> = withContext(Dispatchers.IO) {
        val h = helper ?: return@withContext Pair("", -1L)
        val start = System.currentTimeMillis()
        val exported = h.getOutputFile(cacheIt)
        val result = h.writeTranslationFile(output = exported)
        val cost = System.currentTimeMillis() - start
        return@withContext Pair(exported.absolutePath, if (result) cost else -1)
    }

    private suspend fun search(text: String) =
        withContext(Dispatchers.IO) {
            updateUiState { it.copy(searchState = it.searchState.copy(text = text)) }
            refreshFilter()
        }

    suspend fun rememberLastWindowState(windowState: WindowState) {
        autoSaveJob?.cancel()
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

    private fun requestEditorData(entry: PoEntry?): EditorData? {
        val h = helper ?: return null
        return if (entry == null) null else {
            val key = entry.key
            val sourceText = h.sourceEntry(key)?.msgId() ?: entry.msgId()
            val oldSourceText = if (entry.msgidChanged) h.poEntry(key)?.msgId()?.takeIf { it.isNotBlank() } else null
            val chsReference = h.chsEntry(key)?.msgStr()?.takeIf { it.isNotBlank() }
            val draftText = h.draftEntry(key)?.msgStr()?.takeIf { it.isNotBlank() }
            val poText = h.poEntry(key)?.msgStr()?.takeIf { it.isNotBlank() }
            EditorData(
                target = entry,
                sourceText = sourceText,
                oldSourceText = oldSourceText,
                chsReference = chsReference,
                poText = poText,
                draftText = draftText
            )
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
        val replaced = converter.refactor(traditional)
        return converter.refineQuotes(replaced)
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

private val MULTI_SPACE_REGEX = Regex("\\s+")
private val TAG_REGEX = Regex("<[^>]+>")

internal fun String.normalizeTextForSearch(): String {
    val unescapedQuotes = this.replace("\\\"", "\"")
    val noNewlines = unescapedQuotes.replace("\\n", " ").replace("\n", " ").replace("\r", " ").replace("\t", " ")
    return noNewlines.replace(MULTI_SPACE_REGEX, " ").trim()
}

internal fun String.stripTags(): String {
    return this.replace(TAG_REGEX, "")
}

internal fun String.matchesSearch(cleanQuery: String): Boolean {
    val raw = this.normalizeTextForSearch()
    if (raw.contains(cleanQuery, ignoreCase = true)) return true
    val stripped = raw.stripTags()
    return stripped.contains(cleanQuery, ignoreCase = true)
}

internal fun String.normalizeKeyForSearch(): String {
    return this.replace(".", "").replace("_", "").replace(" ", "").trim()
}
