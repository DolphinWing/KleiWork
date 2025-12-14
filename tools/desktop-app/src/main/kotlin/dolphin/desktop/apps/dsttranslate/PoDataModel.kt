package dolphin.desktop.apps.dsttranslate

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.WindowPosition
import androidx.compose.ui.window.WindowState
import dolphin.android.apps.dsttranslate.WordEntry
import dolphin.desktop.apps.dsttranslate.compose.Configs
import dolphin.desktop.apps.dsttranslate.compose.EditorSpec
import dolphin.desktop.apps.dsttranslate.compose.SearchType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext

class PoDataModel(val helper: DesktopPoHelper) {
    val configs = MutableStateFlow(Configs())
    val filteredList = MutableStateFlow(emptyList<WordEntry>())
    val changedList = MutableStateFlow(emptyList<Long>())
    val searchType = MutableStateFlow(SearchType.Key)
    val searchText = MutableStateFlow("")
    val searchList = MutableStateFlow(emptyList<WordEntry>())

    suspend fun loadIni() : Pair<WindowPosition, DpSize> = withContext(Dispatchers.IO) {
        helper.loadXml() // setup replacement at launch
        configs.emit(Configs(helper.ini))
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
        return@withContext Pair(position, size)
    }

    suspend fun loadIniAndPo() = withContext(Dispatchers.IO) {
        loadIni() // loadIniAndPo
        helper.runTranslationProcess() // setup replacement at launch
        refreshDataSource() // loadPo
        searchList.emit(helper.allValues()) // loadPo
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

    private suspend fun refreshDataSource() {
        val list = ArrayList<Long>()
        val filtered = helper.buildChangeList()
        filtered.forEach { item -> list.add(item.changed) }
        println("refreshDataSource: ${filtered.size}")
        filteredList.emit(filtered)
        changedList.emit(list)
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
        searchType.emit(type)
        search(searchText.value)
    }

    /**
     * Search text in map
     *
     * @param text target text
     * @param type search type
     */
    suspend fun search(text: String, type: SearchType = searchType.value) {
        searchText.emit(text)
        searchList.emit(helper.allValues().filter { item ->
            when (type) {
                SearchType.Origin -> item.origin()
                SearchType.Key -> item.key()
                SearchType.Text -> item.translated()
            }.contains(text, ignoreCase = true)
        })
    }

    /**
     * Make a new [EditorSpec] to editor
     *
     * @param entry target word
     * @return new entry to editor
     */
    fun requestEdit(entry: WordEntry): EditorSpec {
        // ONI: update entry id to template one
        val entry1 = entry.copy(id = helper.templated(entry.key)?.id ?: entry.id)
        return EditorSpec(
            entry1,
            simplifiedToTraditional = helper.sc2tc(helper.simplified(entry.key)?.str ?: ""),
            templateContent = helper.templated(entry.key)?.id,
        )
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
}
