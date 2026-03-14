package dolphin.desktop.apps.onitranslator.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

data class LogEntry(
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: LogType = LogType.Info
)

enum class LogType { Info, Warning, Error }

/**
 * Core helper class for processing ONI translation files (.po, .pot).
 * This class orchestrates the loading of template, simplified chinese, and existing translation files,
 * then merges them into a final word list for the editor.
 *
 * @param configs The application path configurations.
 * @param textRefinery A helper for performing text transformations.
 * @param debug A flag to indicate if running in debug mode, affects output file paths.
 */
class PoHelper(
    private val configs: Configs,
    private val textRefinery: TextRefinery,
    private val debug: Boolean = false,
) {
    companion object {
        private const val ONI_PO_TEMPLATE = "strings_template.pot"
        private const val ONI_CHS_PO = "strings_preinstalled_zh_klei.po"
        const val ONI_PO = "strings.po"
        private const val MAX_LOG_SIZE = 200
    }

    private val templateMap = LinkedHashMap<String, PoEntry>()
    private val simplifiedMap = HashMap<String, PoEntry>()
    private val translatedEntries = HashMap<String, PoEntry>()
    private val draftEntries = HashMap<String, PoEntry>()
    private val entryList: MutableList<PoEntry> = mutableListOf()

    fun templateText(key: String): PoEntry? = templateMap[key]
    fun simplified(key: String): PoEntry? = simplifiedMap[key]
    fun drafted(key: String): PoEntry? = draftEntries[key]
    fun allValues(): List<PoEntry> = entryList.toList()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    private fun log(message: String, type: LogType = LogType.Info) {
        val entry = LogEntry(message, type = type)
        _logs.update { currentList ->
            val newList = currentList + entry
            if (newList.size > MAX_LOG_SIZE) newList.takeLast(MAX_LOG_SIZE) else newList
        }
        println("[${type.name}] $message")
    }

    /**
     * The main orchestration function to run the entire translation process.
     */
    suspend fun runTranslationProcess(): Long = withContext(Dispatchers.IO) {
        _loading.emit(true)
        val startTime = System.currentTimeMillis()

        loadAllSources()
        buildPoList()

        val cost = System.currentTimeMillis() - startTime
        log("Translation process finished in $cost ms")
        _loading.emit(false)
        return@withContext cost
    }

    private suspend fun loadAllSources() {
        log("Loading Simplified Chinese PO...")
        simplifiedMap.clear()
        simplifiedMap.putAll(loadAssetFile(ONI_CHS_PO).associateBy { it.key })
        log("Simplified Chinese PO size: ${simplifiedMap.size}")

        log("Loading PO Template...")
        templateMap.clear()
        templateMap.putAll(loadAssetFile(ONI_PO_TEMPLATE).associateBy { it.key })
        log("PO Template size: ${templateMap.size}")

        log("Loading existing translations (strings.po)...")
        translatedEntries.clear()
        translatedEntries.putAll(loadAssetFile(ONI_PO).associateBy { it.key })
        log("Previous translations (strings.po) size: ${translatedEntries.size}")

        val draftFile = getCachedFile()
        if (draftFile.exists()) {
            log("Loading Draft...")
            draftEntries.clear()
            draftEntries.putAll(loadFile(draftFile).associateBy { it.key })
            log("Draft entries size: ${draftEntries.size}")
        }
    }

    private suspend fun buildPoList() {
        log("Building word list (Template: ${templateMap.size}, Existing: ${translatedEntries.size})...")
        entryList.clear()

        // Check for removed keys (present in strings.po but not in template)
        translatedEntries.keys.forEach { key ->
            if (!templateMap.containsKey(key)) {
                log("Entry '$key' is removed from template.", LogType.Info)
            }
        }

        // Process ALL entries from the template to maintain a 1:1 mapping
        templateMap.values.forEach { templateEntry ->
            val newEntry = buildPoEntry(templateEntry)
            entryList.add(newEntry)
        }
        log("Final word list size: ${entryList.size}")
    }

    /**
     * Builds a single [PoEntry] by merging data from template, simplified, and translated sources.
     * The priority of content is: Draft > Existing Translation > Simplified (converted) > Template Origin.
     */
    private fun buildPoEntry(templateEntry: PoEntry): PoEntry {
        val key = templateEntry.key
        var isNewly = false
        var isMsgidChanged = false
        var newStr = ""

        val draftEntry = draftEntries[key]
        val existingTranslation = translatedEntries[key]

        // 1. Identify if it's a completely new key
        if (existingTranslation == null) {
            isNewly = true
        }

        // 2. Identify if the original English text (msgid) has changed
        if (existingTranslation != null && existingTranslation.id != templateEntry.id) {
            log("Found msgid changed for key '$key': ${existingTranslation.id} -> ${templateEntry.id}")
            isMsgidChanged = true
        }

        // 3. Determine the string content based on priority: Draft > Existing > Simplified fallback
        if (draftEntry != null && draftEntry.str.isNotBlank()) {
            newStr = draftEntry.str.trim()
        }

        if (newStr.isBlank() && existingTranslation != null) {
            newStr = existingTranslation.str.trim()
        }

        // Fallback to Simplified Chinese conversion or msgid if absolutely no translation exists
        if (newStr.isBlank()) {
            newStr = simplifiedMap[key]?.str ?: templateEntry.msgId()
            newStr = TextRefinery.sc2tc(newStr)
        }

        // Apply custom refinery rules
        newStr = textRefinery.refactor(newStr)

        return PoEntry(
            key = key,
            text = templateEntry.text,
            id = templateEntry.id,
            str = newStr,
            newly = isNewly,
            msgidChanged = isMsgidChanged
        ).apply {
            diagnostic = TagSensor.diagnose(msgId(), msgStr())
        }
    }

    private fun loadAssetFile(name: String): List<PoEntry> {
        val dir = if (name == ONI_PO) configs.oniWorkshopDir else configs.oniAssetsDir
        val file = File(dir, name)
        return loadFile(file)
    }

    private fun loadFile(file: File): List<PoEntry> {
        log("Loading file: ${file.absolutePath}")
        return if (file.exists()) {
            try {
                BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)).use { reader ->
                    parsePoFile(reader)
                }
            } catch (e: Exception) {
                log("Failed to load ${file.name}: ${e.message}", LogType.Error)
                emptyList()
            }
        } else {
            log("File not found: ${file.absolutePath}", LogType.Error)
            emptyList()
        }
    }

    private fun parsePoFile(reader: BufferedReader): List<PoEntry> {
        val list = mutableListOf<PoEntry>()
        var line: String?
        
        var currentKey = ""
        var currentCtxt = ""
        var currentId = ""
        var currentStr = ""
        
        while (reader.readLine().also { line = it } != null) {
            val trimmedLine = line!!.trim()
            
            when {
                trimmedLine.startsWith("#. ") || trimmedLine.startsWith("#: ") -> {
                    // Start of a new entry, but first save the previous one if it exists
                    if (currentKey.isNotEmpty()) {
                        PoEntry.from("#. $currentKey", currentCtxt, currentId, currentStr)?.let { list.add(it) }
                    }
                    currentKey = trimmedLine.substring(3).trim()
                    currentCtxt = ""
                    currentId = ""
                    currentStr = ""
                }
                trimmedLine.startsWith("msgctxt ") -> {
                    currentCtxt = trimmedLine
                }
                trimmedLine.startsWith("msgid ") -> {
                    currentId = trimmedLine
                }
                trimmedLine.startsWith("msgstr ") -> {
                    currentStr = trimmedLine
                }
                // Handle basic multi-line (very simple implementation)
                trimmedLine.startsWith("\"") -> {
                    when {
                        currentStr.isNotEmpty() && currentId.isNotEmpty() -> currentStr += trimmedLine
                        currentId.isNotEmpty() -> currentId += trimmedLine
                        currentCtxt.isNotEmpty() -> currentCtxt += trimmedLine
                    }
                }
            }
        }
        
        // Don't forget the last entry
        if (currentKey.isNotEmpty()) {
            PoEntry.from("#. $currentKey", currentCtxt, currentId, currentStr)?.let { list.add(it) }
        }
        
        return list
    }

    fun update(key: String, value: String) {
        entryList.find { it.key == key }?.apply {
            str = value
            changed = System.currentTimeMillis()
            diagnostic = TagSensor.diagnose(msgId(), msgStr())
            log("Updated '$key'.")
        }
    }

    /**
     * Filters entries that require user attention in the 'To-Do' mode.
     * Formula: (newly || msgidChanged || hasActualDraftChange || modifiedInSession) && msgidNotEmpty
     */
    fun buildChangeList(): List<PoEntry> = entryList.filter { entry ->
        val existingStr = translatedEntries[entry.key]?.str?.trim()
        val draftStr = draftEntries[entry.key]?.str?.trim()

        // Check if there is a real difference between draft and existing translation
        val hasActualDraftChange = draftStr != null && draftStr != existingStr

        val isTarget = entry.newly || entry.msgidChanged || entry.changed > 0 || hasActualDraftChange

        // Only show items that actually have an English source text to translate
        isTarget && entry.msgId().isNotBlank()
    }

    suspend fun writeTranslationFile(output: File, list: List<PoEntry> = entryList): Boolean = withContext(Dispatchers.IO) {
        _loading.emit(true)
        val start = System.currentTimeMillis()
        val result = writeEntryToFile(output, list)
        val cost = System.currentTimeMillis() - start
        log("Wrote to ${output.absolutePath} in $cost ms. Result: $result")

        // If we are saving to the real location (not cache), delete the draft file
        val cachedFile = getCachedFile()
        if (result && output.absolutePath != cachedFile.absolutePath && cachedFile.exists()) {
            if (cachedFile.delete()) {
                log("Deleted draft file: ${cachedFile.absolutePath}")
            } else {
                log("Failed to delete draft file: ${cachedFile.absolutePath}", LogType.Warning)
            }
        }

        _loading.emit(false)
        return@withContext result
    }

    private fun writeEntryToFile(output: File, list: List<PoEntry>): Boolean {
        if (list.isEmpty()) return false
        try {
            output.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                // TODO: Make these header strings constants
                writer.appendLine("\"Language: zh-tw\"")
                writer.appendLine("\"POT Version: 2.0\"")
                writer.appendLine("Application: Oxygen Not Included")
                writer.appendLine("Last-Translator: DolphinWing")
                writer.appendLine("MIME-Version: 1.0")
                writer.appendLine("Content-Type: text/plain; charset=UTF-8")
                list.forEach { entry ->
                    val str = if (entry.str.startsWith("\"") && entry.str.endsWith("\"")) entry.str else "\"${entry.str}\""
                    writer.newLine()
                    writer.appendLine("#. ${entry.key}")
                    writer.appendLine("msgctxt ${entry.text}")
                    writer.appendLine("msgid ${templateMap[entry.key]?.id ?: entry.id}")
                    writer.appendLine("msgstr $str")
                }
            }
            return true
        } catch (e: Exception) {
            log("Failed to write to file ${output.absolutePath}: ${e.message}", LogType.Error)
            return false
        }
    }

    fun getOutputFile(cached: Boolean = debug): File = if (cached) {
        getCachedFile()
    } else {
        File(configs.oniWorkshopDir, ONI_PO)
    }

    private fun getCachedFile(): File = File(System.getProperty("java.io.tmpdir"), ONI_PO)

    fun isConfigValid(): Boolean = configs.isValid()
}
