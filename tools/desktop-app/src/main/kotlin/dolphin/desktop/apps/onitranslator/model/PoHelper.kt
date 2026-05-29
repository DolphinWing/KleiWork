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

    fun sourceEntry(key: String): PoEntry? = templateMap[key]
    fun chsEntry(key: String): PoEntry? = simplifiedMap[key]
    fun draftEntry(key: String): PoEntry? = draftEntries[key]
    fun poEntry(key: String): PoEntry? = translatedEntries[key]
    fun allEntries(): List<PoEntry> = entryList.toList()
    fun hasDraft(): Boolean = getCachedFile().exists()

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun log(message: String, type: LogType = LogType.Info) {
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
        var shouldRefineQuotes = false

        val draftEntry = draftEntries[key]
        val existingTranslation = translatedEntries[key]

        // 1. Identify if it's a completely new key
        if (existingTranslation == null) {
            isNewly = true
            shouldRefineQuotes = true
        }

        // 2. Identify if the original English text (msgid) has changed
        if (existingTranslation != null && existingTranslation.id != templateEntry.id) {
            log("Found msgid changed for key '$key': ${existingTranslation.id} -> ${templateEntry.id}")
            isMsgidChanged = true
            shouldRefineQuotes = true
        }

        // 3. Determine the string content based on priority: Draft > Existing > Simplified fallback
        if (draftEntry != null && draftEntry.str.isNotEmpty()) {
            newStr = draftEntry.str
        }

        if (newStr.isEmpty() && existingTranslation != null) {
            newStr = existingTranslation.str
        }

        // Fallback to Simplified Chinese conversion or msgid if absolutely no translation exists
        if (newStr.isEmpty()) {
            newStr = simplifiedMap[key]?.str ?: templateEntry.msgId()
            newStr = TextRefinery.sc2tc(newStr)
            shouldRefineQuotes = true
        }

        // Apply custom refinery rules
        newStr = textRefinery.refactor(newStr)
        if (shouldRefineQuotes) {
            newStr = textRefinery?.refineQuotes(newStr) ?: newStr
        }

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

    private enum class ActiveField { NONE, CTX, ID, STR }

    fun sanitizeLoadedQuote(input: String): String = input.replace("\\\\\"", "\\\"")

    fun escapePoQuote(input: String): String = input.replace(Regex("(?<!\\\\)\""), "\\\\\"")

    fun parsePoFile(reader: BufferedReader): List<PoEntry> {
        val list = mutableListOf<PoEntry>()
        var line: String?

        var currentKey = ""
        val currentCtxt = StringBuilder()
        val currentId = StringBuilder()
        val currentStr = StringBuilder()
        var activeField = ActiveField.NONE

        fun saveEntry() {
            if (currentKey.isNotEmpty()) {
                list.add(
                    PoEntry(
                        key = currentKey,
                        text = currentCtxt.toString(),
                        id = currentId.toString(),
                        str = currentStr.toString()
                    )
                )
            }
        }

        fun extractContent(trimmed: String, prefix: String): String {
            val content = trimmed.removePrefix(prefix).trim()
            return if (content.startsWith("\"") && content.endsWith("\"")) {
                content.substring(1, content.length - 1)
            } else content
        }

        while (reader.readLine().also { line = it } != null) {
            val trimmedLine = line!!.trim()
            if (trimmedLine.isEmpty()) continue

            when {
                trimmedLine.startsWith("#. ") || trimmedLine.startsWith("#: ") -> {
                    saveEntry()
                    currentKey = trimmedLine.substring(3).trim()
                    currentCtxt.clear()
                    currentId.clear()
                    currentStr.clear()
                    activeField = ActiveField.NONE
                }

                trimmedLine.startsWith("msgctxt ") -> {
                    activeField = ActiveField.CTX
                    currentCtxt.append(sanitizeLoadedQuote(extractContent(trimmedLine, "msgctxt")))
                }

                trimmedLine.startsWith("msgid ") -> {
                    activeField = ActiveField.ID
                    currentId.append(sanitizeLoadedQuote(extractContent(trimmedLine, "msgid")))
                }

                trimmedLine.startsWith("msgstr ") -> {
                    activeField = ActiveField.STR
                    currentStr.append(sanitizeLoadedQuote(extractContent(trimmedLine, "msgstr")))
                }

                trimmedLine.startsWith("\"") && trimmedLine.endsWith("\"") -> {
                    val content = trimmedLine.substring(1, trimmedLine.length - 1)
                    val sanitized = sanitizeLoadedQuote(content)
                    when (activeField) {
                        ActiveField.CTX -> currentCtxt.append(sanitized)
                        ActiveField.ID -> currentId.append(sanitized)
                        ActiveField.STR -> currentStr.append(sanitized)
                        ActiveField.NONE -> {}
                    }
                }
            }
        }

        saveEntry()
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

    suspend fun writeTranslationFile(output: File, list: List<PoEntry> = entryList): Boolean =
        withContext(Dispatchers.IO) {
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
                    val formattedStr = "\"${escapePoQuote(entry.str)}\""
                    val templateId = templateMap[entry.key]?.id ?: entry.id
                    val formattedId = "\"${escapePoQuote(templateId)}\""
                    val formattedCtxt = "\"${escapePoQuote(entry.text)}\""

                    writer.newLine()
                    writer.appendLine("#. ${entry.key}")
                    writer.appendLine("msgctxt $formattedCtxt")
                    writer.appendLine("msgid $formattedId")
                    writer.appendLine("msgstr $formattedStr")
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

    /**
     * Exports a terminology glossary to a TSV file.
     */
    suspend fun exportGlossary(): File? = withContext(Dispatchers.IO) {
        val dir = configs.glossaryDir.ifBlank { configs.oniAssetsDir }
        val outputFile = File(dir, "glossary.tsv")
        val seenNames = mutableSetOf<String>()
        val results = mutableListOf<String>()

        // Header
        results.add("Category\tEnglish\tChinese\tPath")

        val singleLevelExceptions = listOf("BLUEPRINTS", "ELEMENTS", "WORLD_TRAITS", "WORLDS")
        val blacklist = listOf(
            "MISC.", "GAMEPLAY_EVENTS.", "BLUEPRINTS", "ROOMS.DETAILS", "INPUT_BINDINGS.",
            "UI.SPACEARTIFACTS", "UI.KEEPSAKES", "UI.OUTFITS", "UI.SANDBOXTOOLS",
            ".STATUSITEMS", "ROOMS.CRITERIA",
            "UI.FRONTEND", "UI.TOOLTIPS", ".FACADES", ".FACADE"
        )
        val tagRegex = Regex("<[^>]+>")

        entryList.forEach { entry ->
            val key = entry.key()
            val isBlacklisted = blacklist.any { key.contains(it) }

            if (key.endsWith(".NAME") && !isBlacklisted) {
                val eng = entry.msgId().replace(tagRegex, "").replace("\\\"", "\"").trim()
                val cht = entry.msgStr().replace(tagRegex, "").replace("\\\"", "\"").trim()

                // Filter out entries with colons or placeholders
                if (eng.contains(":") || eng.contains("{") || eng.contains("}")) {
                    return@forEach
                }

                if (eng.isNotBlank() && cht.isNotBlank() && eng != cht && !seenNames.contains(eng)) {
                    seenNames.add(eng)

                    // Category logic
                    val parts = key.split(".")
                    val category = if (parts.size >= 3) {
                        val mainCat = parts[1]
                        if (mainCat in singleLevelExceptions) mainCat else "${parts[1]}.${parts[2]}"
                    } else if (parts.size >= 2) {
                        parts[1]
                    } else "GENERAL"

                    results.add("$category\t$eng\t$cht\t$key")
                }
            }
        }

        return@withContext try {
            outputFile.writeText(results.joinToString("\n"), StandardCharsets.UTF_8)
            log("Glossary exported to ${outputFile.absolutePath}")
            outputFile
        } catch (e: Exception) {
            log("Failed to export glossary: ${e.message}", LogType.Error)
            null
        }
    }

    fun isConfigValid(): Boolean = configs.isValid()

    /**
     * Deletes the temporary draft file and clears the memory cache.
     */
    suspend fun clearDrafts(): Boolean = withContext(Dispatchers.IO) {
        val cachedFile = getCachedFile()
        draftEntries.clear()
        if (cachedFile.exists()) {
            val deleted = cachedFile.delete()
            if (deleted) {
                log("All drafts cleared.")
            } else {
                log("Failed to clear drafts.", LogType.Warning)
            }
            return@withContext deleted
        }
        log("No draft file to clear.")
        return@withContext true
    }
}
