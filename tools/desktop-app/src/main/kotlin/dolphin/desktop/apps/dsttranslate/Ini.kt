package dolphin.desktop.apps.dsttranslate

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileInputStream
import java.io.FileWriter
import java.io.InputStreamReader

/**
 * App config ini handler
 *
 * @property workingDir app working dir
 * @param os os name
 */
class Ini(
    val workingDir: String = System.getProperty("user.dir"),
    private val os: String = System.getProperty("os.name") ?: "Linux",
) {
    private val homeConfigs: File
        get() {
            val s = File.separator
            val pFolder = if (isLinux) {
                File("${System.getProperty("user.home")}${s}.config${s}")
            } else {
                File("${System.getProperty("user.home")}${s}AppData${s}Local${s}")
            }
            val dstFolder = File(pFolder, "dst-translator")
            val oniFolder = File(pFolder, "oni-translator")
            try {
                if (!oniFolder.exists()) {
                    if (dstFolder.exists()) {
                        dstFolder.renameTo(oniFolder)
                    } else {
                        oniFolder.mkdirs()
                    }
                }
                return oniFolder // make sure it exists
            } catch (e: Exception) {
                println("no such folder ${e.message}")
            }
            return File(System.getProperty("user.home")) // it must exist
        }

    private val configFile: File
        get() = File(homeConfigs, "configs.ini")

    val isLinux: Boolean = os.startsWith("Linux") || os.startsWith("Ubuntu")

    /**
     * User workshop code folder
     */
    var oniWorkshopDir: String = ""

    /**
     * Klei PO file source folder
     */
    var oniAssetsDir: String = ""

    /**
     * Replacement strings
     */
    var stringMap: String = ""

    /**
     * Window position and size
     */
    var windowPosX: Float = 0f
    var windowPosY: Float = 0f
    var windowWidth: Float = 0f
    var windowHeight: Float = 0f

    /**
     * Load ini file
     */
    suspend fun load() = withContext(Dispatchers.IO) {
        if (!configFile.exists()) {
            println("load ${configFile.absolutePath} failed")
            // try to copy one from resource
            if (huntForReleaseConfig().exists()) {
                huntForReleaseConfig().copyTo(configFile)
            } else if (huntForDebugConfig().exists()) {
                huntForDebugConfig().copyTo(configFile)
            }
            return@withContext
        }
        try {
            val reader = BufferedReader(InputStreamReader(FileInputStream(configFile), "UTF-8"))
            try {
                // do reading, usually loop until end of file reading
                var line: String? = reader.readLine()
                while (line != null) {
                    // println("line: $line")
                    parseIni(line)
                    line = reader.readLine()
                }
            } catch (e: Exception) {
                println("Exception: ${e.message}")
            }
            reader.close()
        } catch (e: Exception) {
            println("close: ${e.message}")
        }
    }

    private fun huntForReleaseConfig(): File {
        val s = File.separator
        return File("${workingDir}${s}app${s}resources${s}${configFile.name}")
    }

    private fun huntForDebugConfig(): File {
        val s = File.separator
        return File("${workingDir}${s}resources${s}common${s}${configFile.name}")
    }

    private fun parseIni(line: String) {
        if (line.contains("=")) {
            val data = line.split("=")
            val value = if (data.size > 1) data[1] else ""
            when (data[0]) {
                "stringMap" -> stringMap = value
                "workshopDir_oni" -> oniWorkshopDir = value
                "assetsDir_oni" -> oniAssetsDir = value
                "windowPosX" -> windowPosX = value.toFloatOrNull() ?: 0f
                "windowPosY" -> windowPosY = value.toFloatOrNull() ?: 0f
                "windowWidth" -> windowWidth = value.toFloatOrNull() ?: 0f
                "windowHeight" -> windowHeight = value.toFloatOrNull() ?: 0f
            }
        } else {
            println("invalid line: $line")
        }
    }

    /**
     * Save ini file
     */
    @Suppress("MemberVisibilityCanBePrivate")
    suspend fun save() = withContext(Dispatchers.IO) {
        val builder = StringBuilder()
        builder.append("stringMap=$stringMap\n")
        builder.append("workshopDir_oni=$oniWorkshopDir\n")
        builder.append("assetsDir_oni=$oniAssetsDir\n")
        builder.append("windowPosX=$windowPosX\n")
        builder.append("windowPosY=$windowPosY\n")
        builder.append("windowWidth=$windowWidth\n")
        builder.append("windowHeight=$windowHeight\n")
        val content = builder.toString()
        try { // http://stackoverflow.com/a/1053474
            val writer = BufferedWriter(FileWriter(configFile))
            writer.write(content, 0, content.length)
            writer.close()
        } catch (e: Exception) {
            e.printStackTrace()
            println("writeStringToFile: " + e.message)
        }
    }

    private suspend fun updateMaps(srcFile: File) = withContext(Dispatchers.IO) {
        if (!srcFile.exists()) return@withContext
        val map = File(homeConfigs, "strings.xml")
        srcFile.copyTo(target = map, overwrite = true)
        stringMap = map.absolutePath
        save() // write configs
    }

    /**
     * Apply configs changed
     *
     * @param workingDirOni app working dir
     * @param assetsDirOni source Klei PO assets
     * @param stringMap refactor list
     */
    suspend fun apply(
        stringMap: String? = null,
        workingDirOni: String? = null,
        assetsDirOni: String? = null,
    ) = withContext(Dispatchers.IO) {
        if (stringMap != null && stringMap != this@Ini.stringMap) {
            this@Ini.updateMaps(File(stringMap))
        }
        workingDirOni?.let { dir -> this@Ini.oniWorkshopDir = dir }
        assetsDirOni?.let { dir -> this@Ini.oniAssetsDir = dir }
        save()
    }
}
