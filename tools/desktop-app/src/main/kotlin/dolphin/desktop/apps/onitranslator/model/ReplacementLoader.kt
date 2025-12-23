package dolphin.desktop.apps.onitranslator.model

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.io.File
import java.io.InputStream
import javax.xml.parsers.SAXParserFactory

/**
 * A loader responsible for finding and parsing the `strings.xml` file
 * to provide a map of replacement strings.
 */
class ReplacementLoader(private val configs: Configs) {

    /**
     * Loads the replacement strings map.
     * It tries to find the `strings.xml` file from the configured path,
     * then falls back to a default bundled resource, and finally checks the workshop directory.
     *
     * @return A map of strings to be replaced.
     */
    fun load(): Map<String, String> {
        val replacementMap = HashMap<String, String>()
        val xmlFile = findReplacementXml()
        if (xmlFile.exists()) {
            try {
                SAXParserFactory.newInstance().newSAXParser().parse(xmlFile, SaxDocumentHandler(replacementMap))
            } catch (e: Exception) {
                // TODO: Replace with error handling via StateFlow
                println("SAXParser Exception: ${e.message}")
            }
        } else {
            // TODO: Replace with error handling via StateFlow
            println("Replacement XML not found: ${xmlFile.absolutePath}")
        }
        return replacementMap
    }

    private fun findReplacementXml(): File {
        println("configs.stringMap = ${configs.stringMap}")
        println("configs.oniWorkshopDir = ${configs.oniWorkshopDir}")
        println("configs.oniAssetDir = ${configs.oniAssetsDir}")

        // 1. Try stringMap from configs if it's a valid file
        if (configs.stringMap.isNotBlank()) {
            val configFile = File(configs.stringMap)
            if (configFile.exists() && configFile.isFile) {
                return configFile
            }
        }

        // 2. Try to load from asset dir (a common user scenario)
        val workshopFile = File(configs.oniAssetsDir, FilePaths.STRINGS_XML_NAME)
        if (workshopFile.exists()) {
            return workshopFile
        }

        // 3. Fallback to the default bundled resource
        val resourceStream: InputStream? = this::class.java.getResourceAsStream(FilePaths.DEFAULT_CONFIG_PATH)
        if (resourceStream != null) {
            // SAX parser needs a File, so we write the resource stream to a temporary file.
            val tempFile = File.createTempFile("strings_default", ".xml")
            tempFile.deleteOnExit() // Ensure temp file is cleaned up on JVM exit
            tempFile.outputStream().use { output ->
                resourceStream.copyTo(output)
            }
            return tempFile
        }

        // 4. Return a non-existent file handle as a last resort, load() will handle the error.
        return workshopFile // Re-using handle to indicate the expected location
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
