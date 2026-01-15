package dolphin.desktop.apps.onitranslator.model

import dolphin.desktop.apps.onitranslator.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.File
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

/**
 * A loader responsible for finding and parsing the `strings.xml` file
 * to provide a map of replacement strings.
 */
class DataBank(private val configs: Configs) {

    /**
     * Loads the replacement strings map.
     * It tries to find the `strings.xml` file from the configured path,
     * then falls back to a default bundled resource, and finally checks the workshop directory.
     *
     * @return A map of strings to be replaced.
     */
    suspend fun load(): Map<String, String> = withContext(Dispatchers.IO) {
        val replacementMap = HashMap<String, String>()
        val xmlFile = findReplacementXml()

        try {
            val inputStream: InputStream = if (xmlFile != null && xmlFile.exists()) {
                xmlFile.inputStream()
            } else {
                val bytes = Res.readBytes("files/replacement_strings.xml")
                ByteArrayInputStream(bytes)
            }

            val parser = SAXParserFactory.newInstance().newSAXParser()
            parser.parse(inputStream, SaxDocumentHandler(replacementMap))
        } catch (e: Exception) {
            // TODO: Replace with error handling via StateFlow
            println("SAXParser Exception: ${e.message}")
        }

        replacementMap
    }

    private fun findReplacementXml(): File? {
        println("configs.stringMap = ${configs.dataBankPath}")
        println("configs.oniWorkshopDir = ${configs.oniWorkshopDir}")
        println("configs.oniAssetDir = ${configs.oniAssetsDir}")

        // 1. Try stringMap from configs if it's a valid file
        if (configs.dataBankPath.isNotBlank()) {
            val configFile = File(configs.dataBankPath)
            if (configFile.exists() && configFile.isFile) {
                return configFile
            }
        }

        // 2. Try to load from asset dir (a common user scenario)
        val workshopFile = File(configs.oniAssetsDir, FilePaths.STRINGS_XML_NAME)
        if (workshopFile.exists()) {
            return workshopFile
        }

        return null
    }

    /**
     * SAX Handler to parse the strings.xml content.
     */
    private class SaxDocumentHandler(private val map: HashMap<String, String>) : DefaultHandler() {
        private var currentTag: String = ""
        private var currentName: String? = null
        private var isReplacementList = false

        override fun startElement(
            uri: String?,
            localName: String?,
            qName: String?,
            attributes: Attributes?
        ) {
            currentTag = qName ?: ""
            if (currentTag == "string-array") {
                if (attributes?.getValue("name") == "replacement_list") {
                    isReplacementList = true
                }
            }
            if (attributes != null) {
                currentName = attributes.getValue("name")
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            val content = String(ch, start, length).trim()
            if (content.isEmpty()) return

            when {
                currentTag == "string" && currentName?.startsWith("replacement") == true -> {
                    map[currentName!!] = content
                }

                currentTag == "item" && isReplacementList -> {
                    // Create a unique key for each item in the replacement list
                    map["entry-${map.size}"] = content
                }
            }
        }

        override fun endElement(uri: String?, localName: String?, qName: String?) {
            if (qName == "string-array") {
                isReplacementList = false
            }
            currentTag = ""
            currentName = null
        }
    }
}
