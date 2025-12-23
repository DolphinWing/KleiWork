package dolphin.desktop.apps.onitranslator.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

/**
 * Core helper class for processing ONI translation files (.po, .pot).
 * This class orchestrates the loading of template, simplified chinese, and existing translation files,
 * then merges them into a final word list for the editor.
 *
 * @param configs The application path configurations.
 * @param textConverter A helper for performing text transformations.
 * @param debug A flag to indicate if running in debug mode, affects output file paths.
 */
class PoHelper(
    private val configs: Configs,
    private val textConverter: TextConverter,
    private val debug: Boolean = false,
) {
    companion object {
        private const val ONI_PO_TEMPLATE = "strings_template.pot"
        private const val ONI_CHS_PO = "strings_preinstalled_zh_klei.po"
        const val ONI_PO = "strings.po"
    }

    private val simplifiedMap = HashMap<String, WordEntry>()
    private val templateMap = HashMap<String, WordEntry>()
    private val translatedEntries = HashMap<String, WordEntry>()
    private val wordList: MutableList<WordEntry> = mutableListOf()

    fun simplified(key: String): WordEntry? = simplifiedMap[key]
    fun templated(key: String): WordEntry? = templateMap[key]
    fun translated(key: String): WordEntry? = translatedEntries[key]
    fun allValues(): List<WordEntry> = wordList.toList()

    private val _status = MutableStateFlow("")
    val status: StateFlow<String> = _status

    private val _loading = MutableStateFlow(true)
    val loading: StateFlow<Boolean> = _loading

    private fun log(message: String) {
        println(message) // TODO: Replace with error handling via StateFlow
    }

    /**
     * The main orchestration function to run the entire translation process.
     */
    suspend fun runTranslationProcess(): Long = withContext(Dispatchers.IO) {
        _loading.emit(true)
        val startTime = System.currentTimeMillis()

        loadAllSources()
        buildWordList()

        writeEntryToFile(getCachedFile(), wordList)
        val cost = System.currentTimeMillis() - startTime
        log("Translation process finished in $cost ms")
        _status.emit("")
        _loading.emit(false)
        return@withContext cost
    }

    private suspend fun loadAllSources() {
        _status.emit("Loading Simplified Chinese PO...")
        simplifiedMap.putAll(loadAssetFile(ONI_CHS_PO).associateBy { it.key })
        log("Simplified Chinese PO size: ${simplifiedMap.size}")

        _status.emit("Loading PO Template...")
        templateMap.putAll(loadAssetFile(ONI_PO_TEMPLATE).associateBy { it.key })
        log("PO Template size: ${templateMap.size}")

        _status.emit("Loading existing translations...")
        translatedEntries.putAll(
            loadAssetFile(ONI_PO).filter { it.id != "\"\"" && it.str != "\"\"" }.associateBy { it.key }
        )
        log("Previous translations size: ${translatedEntries.size}")
    }

    private suspend fun buildWordList() {
        _status.emit("Building word list...")
        wordList.clear()
        templateMap.values.filter { entry ->
            (entry.translated().isEmpty() && entry.origin().isNotEmpty()) && // no translation
                    !entry.translated().startsWith("only_used_by") // from dst
        }.forEach { entry ->
            val newEntry = buildWordEntry(entry)
            wordList.add(newEntry)
        }
        log("New word list size: ${wordList.size}")
    }

    /**
     * Builds a single [WordEntry] by merging data from template, simplified, and translated sources.
     */
    private fun buildWordEntry(templateEntry: WordEntry): WordEntry {
        val key = templateEntry.key
        var isNewly = false
        var newStr = ""

        val existingTranslation = translatedEntries[key]
        if (existingTranslation != null) {
            newStr = existingTranslation.str.trim()
        }

        if (newStr.isEmpty()) {
            isNewly = true
            newStr = simplifiedMap[key]?.str ?: templateEntry.origin()
            newStr = TextConverter.sc2tc(newStr)
        }

        newStr = textConverter.refactor(newStr)

        val id = existingTranslation?.id ?: templateEntry.id
        if (id != templateEntry.id) {
            log("Found msgid changed for key '$key': ${id} -> ${templateEntry.id}")
            isNewly = true
        }

        return WordEntry(key, templateEntry.text, templateEntry.id, newStr, isNewly)
    }

    private fun loadAssetFile(name: String): List<WordEntry> {
        val dir = if (name == ONI_PO) configs.oniWorkshopDir else configs.oniAssetsDir
        val file = File(dir, name)
        log("Loading asset: ${file.absolutePath}")
        return if (file.exists()) {
            try {
                BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)).use { reader ->
                    parsePoFile(reader)
                }
            } catch (e: Exception) {
                log("Failed to load $name: ${e.message}")
                emptyList()
            }
        } else {
            log("Asset not found: ${file.absolutePath}")
            emptyList()
        }
    }

    private fun parsePoFile(reader: BufferedReader): List<WordEntry> {
        // A more robust implementation would handle multi-line msgid/msgstr and other edge cases.
        // For now, this simple 4-line parser is retained.
        val list = mutableListOf<WordEntry>()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            if (!line!!.startsWith("#.")) continue

            val line1 = line!!
            val line2 = reader.readLine() ?: break // msgctxt
            val line3 = reader.readLine() ?: break // msgid
            val line4 = reader.readLine() ?: break // msgstr

            WordEntry.from(line1, line2, line3, line4)?.let { list.add(it) }
                ?: log("Invalid PO entry starting with: $line1")
        }
        return list
    }

    fun update(key: String, value: String) {
        wordList.find { it.key == key }?.apply {
            str = value
            changed = System.currentTimeMillis()
            log("Updated '$key' at $changed")
        }
    }

    fun buildChangeList(): List<WordEntry> = wordList.filter { it.changed > 0 || it.newly }

    suspend fun writeTranslationFile(output: File, list: List<WordEntry> = wordList): Boolean = withContext(Dispatchers.IO) {
        _loading.emit(true)
        val start = System.currentTimeMillis()
        val result = writeEntryToFile(output, list)
        val cost = System.currentTimeMillis() - start
        log("Wrote to ${output.absolutePath} in $cost ms. Success: $result")
        _loading.emit(false)
        return@withContext result
    }

    private fun writeEntryToFile(output: File, list: List<WordEntry>): Boolean {
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
            log("Failed to write to file ${output.absolutePath}: ${e.message}")
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
