package dolphin.desktop.apps.onitranslator.model

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.nio.charset.StandardCharsets

abstract class PoHelper {
    companion object {
        private const val ONI_PO_TEMPLATE = "strings_template.pot"
        private const val ONI_CHS_PO = "strings_preinstalled_zh_klei.po"
        const val ONI_PO = "strings.po"
    }

    protected val replaceList: MutableList<Pair<String, String>> = mutableListOf()
    private var replacementMap = mapOf<String, String>()
    private var replacementRegex: Regex? = null

    protected fun setupReplacements() {
        replacementMap = try {
            replaceList.toMap()
        } catch (e: Exception) {
            println("Failed to replacement map: ${e.message}")
            emptyMap()
        }
        if (replacementMap.isNotEmpty()) {
            val regexPattern = replacementMap.keys.joinToString("|") { Regex.escape(it) }
            replacementRegex = Regex(regexPattern)
        }
    }

    protected var replace3dot: String = ""
    private val replace6dot: String
        get() = "$replace3dot$replace3dot"
    protected var replaceLeftBracket: String = ""
    protected var replaceRightBracket: String = ""

    private val simplifiedMap = HashMap<String, WordEntry>()

    /**
     * @param key entry key
     * @return word entry from official simplified chinese po file
     */
    fun simplified(key: String): WordEntry? = simplifiedMap[key]

    private val templateMap = HashMap<String, WordEntry>()

    /**
     * @param key entry key
     * @return word entry from template file
     */
    fun templated(key: String): WordEntry? = templateMap[key]

    private val translatedEntries = HashMap<String, WordEntry>()

    /**
     * @param key entry key
     * @return word entry from translated file
     */
    fun translated(key: String): WordEntry? = translatedEntries[key]

    private val wordList: MutableList<WordEntry> = mutableListOf()

    /**
     * @return full word list entry
     */
    fun allValues(): List<WordEntry> = wordList

    /**
     * A debug log output implementation.
     *
     * @param message log message to standard output
     */
    protected abstract fun log(message: String)

    /**
     * Prepare the helper instance. Usually call this when init.
     */
    abstract fun prepare()

    protected fun loadFromReader(reader: BufferedReader): List<WordEntry> {
        val list = mutableListOf<WordEntry>()
        try {
            reader.use { r ->
                var line = r.readLine()
                while (line != null) {
                    if (!line.startsWith("#")) {
                        line = r.readLine()
                        continue // bypass some invalid header
                    }

                    val line1 = line
                    var line2 = r.readLine() ?: break // msgctxt
                    var line3 = r.readLine()
                    while (line3 != null && !line3.startsWith("msgid")) {
                        line2 = line2.dropLast(1) + line3.drop(1)
                        line3 = r.readLine()
                    }
                    if (line3 == null) break

                    var line4 = r.readLine()
                    while (line4 != null && !line4.startsWith("msgstr")) {
                        line3 = line3.dropLast(1) + line4.drop(1)
                        line4 = r.readLine()
                    }
                    if (line4 == null) break

                    var currentLine = r.readLine()
                    while (!currentLine.isNullOrEmpty()) {
                        line4 = line4.dropLast(1) + currentLine.drop(1)
                        currentLine = r.readLine()
                    }

                    WordEntry.from(line1, line2, line3, line4)?.let { entry ->
                        list.add(entry)
                    } ?: log("invalid input: $line1")

                    line = currentLine // Continue from the line after the entry
                }
            }
        } catch (e: Exception) {
            log("Exception: ${e.message}")
        }
        return list
    }

    private fun writeEntryToFile(
        output: File = getOutputFile(),
        list: List<WordEntry> = wordList
    ): Boolean {
        if (list.isEmpty()) return false // no list, don't write
        try { // http://stackoverflow.com/a/1053474
            output.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
                writer.appendLine("\"Language: zh-tw\"")
                writer.appendLine("\"POT Version: 2.0\"")
                writer.appendLine("Application: Oxygen Not Included")
                writer.appendLine("Last-Translator: DolphinWing")
                writer.appendLine("MIME-Version: 1.0")
                writer.appendLine("Content-Type: text/plain; charset=UTF-8")
                list.forEach { entry ->
                    val str =
                        if (entry.str.startsWith("\"") && entry.str.endsWith("\"")) entry.str else "\"${entry.str}\""
                    writer.newLine()
                    writer.appendLine("#. ${entry.key}")
                    writer.appendLine("msgctxt ${entry.text}")
                    writer.appendLine("msgid ${templateMap[entry.key]?.id ?: entry.id}")
                    writer.appendLine("msgstr $str")
                }
            }
        } catch (e: Exception) {
            // e.printStackTrace()
            log("writeStringToFile: ${e.message}")
            return false
        }
        log("write to ${output.absolutePath} with ${output.length()} done")
        return true
    }

    /**
     * Implementation of loading asset file to memory
     *
     * @param name asset name
     * @return word entry list
     */
    abstract fun loadAssetFile(name: String): List<WordEntry>

    private val processStatus = MutableStateFlow("")

    /**
     * Process status
     */
    val status: StateFlow<String> = processStatus

    /**
     * Loading status. True means the app is processing data.
     */
    val loading = MutableStateFlow(true)

    /**
     * Load chs and cht translation file to app.
     *
     * @return total process time
     */
    suspend fun runTranslationProcess(): Long = withContext(Dispatchers.IO) {
        // log("run translation")
        loading.emit(true)
        val start = System.currentTimeMillis()

        val chsPoFile = ONI_CHS_PO
        processStatus.emit("load $chsPoFile")
        val simplifiedEntries = loadAssetFile(chsPoFile)
        simplifiedMap.clear()
        simplifiedEntries.forEach { entry ->
            // entry.str = sc2tc(entry.str).trim() // translate to traditional
            simplifiedMap[entry.key] = entry
        }
        val stop1 = System.currentTimeMillis()
        log("ONI simplified chinese size: ${simplifiedEntries.size} (${stop1 - start} ms)")

        val chtPoFile = ONI_PO_TEMPLATE
        processStatus.emit("load $chtPoFile")
        val templateEntries = loadAssetFile(chtPoFile)
        templateMap.clear()
        templateEntries.forEach { entry ->
            templateMap[entry.key] = entry
        }
        val stop2 = System.currentTimeMillis()
        log("ONI traditional chinese size: ${templateEntries.size} (${stop2 - start} ms)")

        val outputPoFile = ONI_PO
        processStatus.emit("load $outputPoFile")
        translatedEntries.clear()
        loadAssetFile(outputPoFile).filter { entry ->
            entry.id != "\"\"" && entry.str != "\"\""
        }.forEach { entry ->
            translatedEntries[entry.key] = entry
        }
        val stop3 = System.currentTimeMillis()
        log("previous data size: ${translatedEntries.size} (${stop3 - stop1} ms)")

        processStatus.emit("prepare word list")
        wordList.clear()
        templateEntries.filter { entry ->
            (entry.translated().isEmpty() && entry.origin().isNotEmpty()) // no translation
                    && !entry.translated().startsWith("only_used_by") // from dst
        }.forEachIndexed { index, entry ->
            var newly = false
            var str = ""
            if (translatedEntries.containsKey(entry.key)) {
                val str1 = translatedEntries[entry.key]?.str ?: ""
                if (str1.isNotEmpty()) str = str1.trim()
            } else {
                processStatus.emit("${entry.key} (${index + 1}/${simplifiedEntries.size})")
            }
            if (str.isEmpty()) { // not in the translated po
                newly = true
                if (simplifiedMap.containsKey(entry.key)) {
                    str = simplifiedMap[entry.key]?.str ?: ""
                }
                if (str.isEmpty())
                    str = entry.origin()
                str = sc2tc(str)
            }
            str = refactor(str)
            // make sure id changed will be shown
            val id = translatedEntries[entry.key]?.id ?: entry.id
            if (id != entry.id) {
                log("found $id changed to ${entry.id}")
                newly = true // mark that it is changed
            }
            addToTodoList(WordEntry(entry.key, entry.text, id, str, newly))
        }

        val stop4 = System.currentTimeMillis()
        log("new list size: ${wordList.size} (${stop4 - stop3} ms)")

        writeEntryToFile(getCachedFile(), wordList) // runTranslationProcess
        val cost = System.currentTimeMillis() - start
        log("write data done. $cost ms")
        processStatus.emit("")
        loading.emit(false) // complete
        return@withContext cost
    }

    /**
     * Add an entry to the database
     *
     * @param entry new word
     */
    fun addToTodoList(entry: WordEntry) {
        wordList.add(entry)
    }

    /**
     * Write all word entries to a file
     *
     * @param output destination file
     * @param list word entry list
     * @return true if file written is success
     */
    suspend fun writeTranslationFile(
        output: File = getOutputFile(),
        list: List<WordEntry> = wordList
    ): Boolean = withContext(Dispatchers.IO) {
        loading.emit(true)
        val start = System.currentTimeMillis()
        val result = writeEntryToFile(output, list) // writeTranslationFile
        val cost = System.currentTimeMillis() - start
        log("write data done. $cost ms")
        loading.emit(false) // complete
        return@withContext result
    }

    /**
     * @return actual output file
     */
    abstract fun getOutputFile(): File

    /**
     * @return cache file
     */
    abstract fun getCachedFile(): File

    /**
     * Convert simplified chinese to traditional chinese
     *
     * @param str simplified chinese text
     * @return traditional chinese text
     */
    abstract fun sc2tc(str: String): String

    protected fun refactor(src: String): String = replacementRegex?.replace(src) {
        replacementMap[it.value] ?: it.value
    } ?: src

    /**
     * Build a list with changed entries
     *
     * @return word list with change items
     */
    fun buildChangeList(): List<WordEntry> = wordList.filter { entry ->
        entry.changed > 0 || entry.newly
    }

    /**
     * Update text of specific word entry
     *
     * @param key entry key
     * @param value entry text
     */
    fun update(key: String, value: String) {
        wordList.find { entry -> entry.key == key }?.apply {
            str = value
            changed = System.currentTimeMillis() // set new change time
            println("set new $key to $str at $changed")
        }
    }
}
