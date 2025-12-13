package dolphin.desktop.apps.dsttranslate

import com.github.houbb.opencc4j.util.ZhTwConverterUtil
import dolphin.android.apps.dsttranslate.PoHelper
import dolphin.android.apps.dsttranslate.WordEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.w3c.dom.Element
import org.w3c.dom.Node
import org.w3c.dom.NodeList
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.SAXParserFactory

typealias SuspectMap = HashMap<Char, ArrayList<WordEntry>>

class DesktopPoHelper(val ini: Ini = Ini(), private val debug: Boolean = false) : PoHelper() {
    override fun log(message: String) {
        println(message)
    }

    override fun prepare() {
        // load replacement from xml
    }

    private val replaceMap = HashMap<String, String>()

    @Suppress("unused")
    private fun loadXmlByDom(xml: File) {
        try {
            // See https://stackoverflow.com/a/7373596
            val dbf = DocumentBuilderFactory.newInstance()
            val dom = dbf.newDocumentBuilder().parse(xml)

            dom?.getElementsByTagName("string-array")?.let { list ->
                (list.item(0) as? Element)?.let { replacementList ->
                    replacementList.getElementsByTagName("item").items()
                        .forEachIndexed { index, node ->
                            replaceMap["entry-$index"] = node.content()
                        }
                    // println("  found ${items.length} entries")
                }
            }
            dom?.getElementsByTagName("string")?.items()?.forEach { node ->
                val key = node.attribute("name")
                // println("{node.attribute("name")}: ${node.content()}")
                if (key.startsWith("replacement")) {
                    replaceMap[key] = node.content()
                }
            }
        } catch (e: Exception) {
            println("replacement: ${e.message}")
        }
    }

    /**
     * https://mkyong.com/java/how-to-read-xml-file-in-java-sax-parser/
     */
    private fun loadXmlBySax(xml: File) {
        val factory = SAXParserFactory.newInstance()
        try {
            factory.newSAXParser().parse(xml, SaxDocumentHandler(replaceMap))
        } catch (e: Exception) {
            println("SAXParser: ${e.message}")
        }
    }

    private class SaxDocumentHandler(private val map: HashMap<String, String>) : DefaultHandler() {
        var tag: String = ""
        var name: String? = null

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String?,
            attributes: Attributes?
        ) {
            // println("startElement: $uri: $localName: $qName")
            qName?.let { tag = it }
            attributes?.let { a ->
                repeat(a.length) { index ->
                    // println("${a.getQName(index)}: ${a.getValue(index)}")
                    if (a.getQName(index) == "name") name = a.getValue(index)
                }
            } ?: kotlin.run { name = null }
        }

        override fun characters(ch: CharArray?, start: Int, length: Int) {
            if (tag.isNotEmpty() && ch != null) {
                val content = ch.joinToString("").substring(start, start + length)
                // println("<$tag name='${name ?: ""}'>$content</$tag>")
                putMap(name ?: "", content, start)
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            // println("endElement: $uri: $localName: $qName")
            if (tag == qName) tag = ""
        }

        private fun putMap(name: String, value: String, start: Int) {
            if (tag == "string" && name.startsWith("replacement")) {
                map[name] = value.trim()
            }
            if (tag == "item" && name == "replacement_list") {
                map["entry-$start"] = value
            }
        }
    }

    private fun findReplacementXml(): File {
        // load from ini
        if (/*ini.isLinux &&*/ File(ini.dstStringMap).exists()) {
            return File(ini.dstStringMap)
        }
        val s = File.separator
        // locate debug build
        if (File(ini.workingDir, "resources").exists()) {
            val file = File("${ini.workingDir}${s}resources${s}common${s}strings.xml")
            if (file.exists()) {
                ini.dstStringMap = file.absolutePath // debug build
                return file
            }
        }
        // locate release build
        if (File(ini.workingDir, "app").exists()) {
            val file = File("${ini.workingDir}${s}app${s}resources${s}strings.xml")
            if (file.exists()) {
                ini.dstStringMap = file.absolutePath // release build
                return file
            }
        }
        // locate in workshop dir
        return File(ini.dstWorkshopDir, "strings.xml")
    }

    suspend fun loadXml() = withContext(Dispatchers.IO) {
        ini.load() // load ini
        // loadXmlByDom(findReplaceXml()) // load from replacement xml
        loadXmlBySax(findReplacementXml())
        replaceList.clear() // reset list from loading XML
        replaceMap.filter { entry -> entry.key.startsWith("entry-") }
            .map { entry ->
                val pair = entry.value.split("|")
                Pair(pair[0], pair[1])
            }
            .forEach { entry -> replaceList.add(entry) }
        // println("replace list: ${replaceList.size}")
        replace3dot = replaceMap["replacement_3dot"] ?: ""
        replaceLeftBracket = replaceMap["replacement_left_bracket"] ?: ""
        replaceRightBracket = replaceMap["replacement_right_bracket"] ?: ""
        setupReplacements()
    }

    override fun loadAssetFile(name: String, mode: Mode): ArrayList<WordEntry> {
        if (name == DST_PO) return loadFile(ini.dstWorkshopDir, name)
        if (name == ONI_PO) return loadFile(ini.oniWorkshopDir, name)
        return when (mode) {
            Mode.ONI -> loadFile(ini.oniAssetsDir, name)
            else -> loadFile(ini.dstAssetsDir, name)
        }
    }

    private fun loadFile(dir: String, name: String): ArrayList<WordEntry> {
        log("load asset: $dir${File.separator}$name")
        val file = File(dir, name)
        val list: ArrayList<WordEntry> = if (file.exists()) try {
            // Read UTF-8 https://stackoverflow.com/a/14918597
            val reader = BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8))
            loadFromReader(reader)
        } catch (e: Exception) {
            ArrayList()
        } else {
            ArrayList()
        }
        log("asset: done with $name (${list.size})")
        return list
    }

    override fun getOutputFile(mode: Mode): File = getOutputFile(debug, mode)

    fun getOutputFile(cached: Boolean, mode: Mode = Mode.DST): File = if (cached) getCachedFile(mode) else {
        if (mode == Mode.DST)
            File(ini.dstWorkshopDir, DST_PO)
        else
            File(ini.oniWorkshopDir, ONI_PO)
    }

    override fun getCachedFile(mode: Mode): File =
        File(System.getProperty("java.io.tmpdir"), if (mode == Mode.DST) DST_PO else ONI_PO)

    override fun sc2tc(str: String): String {
        return ZhTwConverterUtil.toTraditional(str)
    }

    private fun NodeList.items(): List<Node> {
        val list = java.util.ArrayList<Node>()
        repeat(this.length) { list.add(item(it)) }
        return list
    }

    private fun Node.content(): String =
        (this as? Element)?.textContent?.dropWhile { it == ' ' }?.trim() ?: ""

    private fun Node.attribute(key: String): String = (this as? Element)?.getAttribute(key) ?: ""

//    fun supportShrinkText(): Boolean {
//        val workshop = File(ini.workshopDir)
//        val fonts = File(workshop.parentFile, "fonts")
//        return File(fonts, "Taiwan4818.txt").exists()
//    }

    private fun Char.valid(): Boolean =
        this != ' ' && this != '\t' && this != '\n' && this != '\r'

    suspend fun analyzeText(): Pair<Int, SuspectMap> = withContext(Dispatchers.IO) {
        loading.emit(true)
        val map = LinkedHashMap<Char, Char>() // use map to drop duplicated char
        // load Taiwan 4818 common characters
        val githubRoot = File(ini.dstWorkshopDir).parentFile
        val dstAsset = File(githubRoot, "dst-assets")
        val sample = File(dstAsset, "Taiwan4818.txt")
        if (sample.exists()) { // add Taiwan4818.txt
            val reader = BufferedReader(InputStreamReader(FileInputStream(sample), StandardCharsets.UTF_8))
            try {
                var line: String? = ""//reader.readLine()
                while (line != null) {
                    line.filter { char -> char.valid() }.forEach { char -> map[char] = char }
                    line = reader.readLine()
                }
            } catch (e: Exception) {
                log("Exception: ${e.message}")
            } finally {
                reader.close()
            }
        }

        val suspects = SuspectMap()
        // check our text files
        allValues().filter { entry ->
            val text = entry.string().trim().filter { char -> char.valid() }
                .filterNot { char -> map.containsKey(char) }
                .filter { char -> char.toString() != replace3dot }
            if (text.isNotEmpty()) {
                text.forEach { char ->
                    suspects.putIfAbsent(char, ArrayList())
                    if (suspects[char]?.any { e -> entry.key() == e.key() } == false) {
                        suspects[char]?.add(entry)
                        println("$char: ${entry.string()}")
                    }
                }
            }
            text.isNotEmpty()
        }

        // put all characters into map
        allValues().forEach { entry ->
            entry.string().filter { char -> char.valid() }.forEach { char -> map[char] = char }
        }
        log("found ${map.size} text")

        // output content map
        val contentFile = if (sample.exists()) {
            File(sample.parent, "dst_cht.txt")
        } else {
            File(ini.workingDir, "dst_cht.txt")
        }
        // write to file
        try { // http://stackoverflow.com/a/1053474
            val writer = BufferedWriter(FileWriter(contentFile))
            map.map { entry -> entry.value }.sortedBy { it.inc() }.forEach { char ->
                writer.write(char.toString())
                writer.newLine()
            }
            writer.close()
            // writer = null
        } catch (e: Exception) {
            log("exportText: ${e.message}")
        }
        log("write to ${contentFile.absolutePath} with ${contentFile.length()} done")
        loading.emit(false)
        return@withContext Pair(map.size, suspects)
    }
}
