package dolphin.desktop.apps.onitranslator.model

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
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

class PoDataModel(private val helper: DesktopPoHelper, appVersion: String) {
    private val _state = MutableStateFlow(AppState(appVersion = appVersion))
    val state: StateFlow<AppState> = _state.asStateFlow()

    init {
        // FIXME: https://github.com/JetBrains/compose-multiplatform/issues/4603
        // CoroutineScope(Dispatchers.IO).launch { loadIni() }

        CoroutineScope(Dispatchers.Default).launch {
            helper.loading.collect { loading ->
                _state.value = state.value.copy(isLoading = loading)
            }
        }
    }

    fun onUiStateChange(uiState: UiState) {
        _state.update { it.copy(uiState = uiState) }
    }

    suspend fun loadIni(): Pair<WindowPosition, DpSize> = withContext(Dispatchers.IO) {
        helper.loadXml() // setup replacement at launch
        _state.update { it.copy(configs = Configs(helper.ini)) }
        val position = if (helper.ini.windowPosX > 0 && helper.ini.windowPosY > 0) {
            WindowPosition.Absolute(helper.ini.windowPosX.dp, helper.ini.windowPosY.dp)
        } else {
            WindowPosition.PlatformDefault
        }
        val size = if (helper.ini.windowWidth > 0 && helper.ini.windowHeight > 0) {
            DpSize(helper.ini.windowWidth.dp, helper.ini.windowHeight.dp)
        } else {
            DpSize(1200.dp, 800.dp)
        }
        println("position = $position, size = $size")
        _state.update { it.copy(windowPosition = position, windowSize = size) }
        return@withContext Pair(position, size)
    }

    suspend fun loadIniAndPo() = withContext(Dispatchers.IO) {
        loadIni() // loadIniAndPo
        translate() // loadIniAndPo
        _state.update { it.copy(searchList = helper.allValues().map { entry -> requestEditorData(entry)!! }) } // loadPo
    }

    /**
     * Save configs to disk
     *
     * @param configs new config
     */
    suspend fun saveConfig(configs: Configs) = withContext(Dispatchers.IO) {
        helper.ini.apply(
            stringMap = configs.stringMap,
            workingDirOni = configs.oniWorkshopDir,
            assetsDirOni = configs.oniAssetsDir,
        )
        loadIniAndPo() // saveConfig
    }

    /**
     * Run translation process
     *
     * @return cost time of translation process
     */
    suspend fun translate(): Long = withContext(Dispatchers.IO) {
        val cost = helper.runTranslationProcess() // setup replacement at launch
        refreshDataSource() // translate
        return@withContext cost
    }

    /**
     * Edit the value
     *
     * @param key entry key
     * @param value new entry value
     */
    suspend fun edit(key: String, value: String) {
        helper.update(key, value)
        refreshDataSource() // edit
    }

    private suspend fun refreshDataSource() = withContext(Dispatchers.IO) {
        val filtered = helper.buildChangeList().map { requestEditorData(it)!! }
        val list = filtered.map { it.target.changed }
        println("refreshDataSource: ${filtered.size}")
        _state.update { it.copy(filteredList = filtered, changedList = list) }
    }

    /**
     * Export translation file
     *
     * @param cacheIt true if we want just to cache the file
     * @return output path and time cost. if export file failed, time cost will be negative.
     */
    suspend fun save(cacheIt: Boolean = false): Pair<String, Long> = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        val exported = helper.getOutputFile(cacheIt)
        val result = helper.writeTranslationFile(output = exported)
        val cost = System.currentTimeMillis() - start
        return@withContext Pair(exported.absolutePath, if (result) cost else -1)
    }

    /**
     * Change search type
     *
     * @param type new search type
     */
    suspend fun searchType(type: SearchType) {
        _state.update { it.copy(searchType = type) }
        search(state.value.searchText, type)
    }

    /**
     * Search text in map
     *
     * @param text target text
     * @param type search type
     */
    suspend fun search(text: String, type: SearchType = state.value.searchType) = withContext(Dispatchers.IO) {
        _state.update { it.copy(searchText = text) }
        _state.update {
            it.copy(searchList = helper.allValues().filter { item ->
                when (type) {
                    SearchType.Origin -> item.origin()
                    SearchType.Key -> item.key()
                    SearchType.Text -> item.translated()
                }.contains(text, ignoreCase = true)
            }.map { entry -> requestEditorData(entry)!! })
        }
    }

    suspend fun rememberLastWindowState(windowState: WindowState) {
        val pos = windowState.position
        val size = windowState.size
        println("close window: $pos, $size")
        if (pos is WindowPosition.Absolute) {
            helper.ini.windowPosX = pos.x.value
            helper.ini.windowPosY = pos.y.value
            helper.ini.windowWidth = size.width.value
            helper.ini.windowHeight = size.height.value
            helper.ini.save()
        }
    }

    fun requestEditorData(entry: WordEntry?): EditorData? = if (entry == null) null else {
        val key = entry.key
        val templateText = helper.templated(key)?.origin() ?: entry.origin()
        val referenceText = helper.simplified(key)?.translated()
        val draftText = helper.translated(key)?.translated()
        EditorData(entry, templateText, referenceText, draftText)
    }

    fun requestSaveData(): SaveData {
        val file1 = helper.getOutputFile(false)
        val file2 = helper.getOutputFile(true)
        return SaveData(file1, file2)
    }

    fun onConvert(text: String): String = helper.convert(text)
}
