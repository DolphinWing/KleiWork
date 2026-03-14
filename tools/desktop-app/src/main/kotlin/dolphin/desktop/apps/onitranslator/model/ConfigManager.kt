package dolphin.desktop.apps.onitranslator.model

import dolphin.desktop.apps.onitranslator.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * A stateless manager for handling the `configs.ini` file.
 * It is responsible for loading and saving configuration data from/to the filesystem.
 */
object ConfigManager {
    private val osName by lazy { System.getProperty(SystemProperties.OS_NAME, "Linux") }
    private val isLinux by lazy { osName.startsWith("Linux") || osName.startsWith("Ubuntu") }

    private val configDirectory: File
        get() {
            val s = File.separator
            val home = System.getProperty(SystemProperties.USER_HOME)
            val parentFolder = if (isLinux) {
                File("$home${s}.config$s")
            } else {
                File("$home${s}AppData${s}Local$s")
            }

            val oniFolder = File(parentFolder, FilePaths.APP_DATA_FOLDER)
            if (!oniFolder.exists()) {
                val legacyFolder = File(parentFolder, FilePaths.APP_DATA_FOLDER_LEGACY)
                if (legacyFolder.exists()) {
                    legacyFolder.renameTo(oniFolder)
                } else {
                    oniFolder.mkdirs()
                }
            }
            return oniFolder
        }

    private val configFile: File
        get() = File(configDirectory, Incs.CONFIG_FILE_NAME)

    /**
     * Loads configurations from the `configs.ini` file.
     * If the file doesn't exist, a default one is created from resources.
     *
     * @return A [Pair] containing the loaded [Configs] and [WindowConfig].
     */
    suspend fun load(): Pair<Configs, WindowConfig> = withContext(Dispatchers.IO) {
        if (!configFile.exists()) {
            copyDefaultConfig()
        }

        val props = mutableMapOf<String, String>()
        try {
            configFile.useLines { lines ->
                lines.filter { it.contains("=") }.forEach { line ->
                    val parts = line.split("=", limit = 2)
                    if (parts.size == 2) {
                        props[parts[0]] = parts[1]
                    }
                }
            }
        } catch (e: Exception) {
            println("Failed to read config file: ${e.message}")
        }


        val configs = Configs(
            dataBankPath = props[Incs.KEY_STRING_MAP] ?: "",
            oniWorkshopDir = props[Incs.KEY_WORKSHOP_DIR_ONI] ?: "",
            oniAssetsDir = props[Incs.KEY_ASSETS_DIR_ONI] ?: "",
            glossaryDir = props[Incs.KEY_GLOSSARY_DIR] ?: "",
        )

        val windowConfig = WindowConfig(
            x = props[Incs.KEY_WINDOW_POS_X]?.toFloatOrNull() ?: 0f,
            y = props[Incs.KEY_WINDOW_POS_Y]?.toFloatOrNull() ?: 0f,
            width = props[Incs.KEY_WINDOW_WIDTH]?.toFloatOrNull() ?: 1200f,
            height = props[Incs.KEY_WINDOW_HEIGHT]?.toFloatOrNull() ?: 800f,
            darkTheme = props[Incs.KEY_DARK_THEME]?.toBooleanStrictOrNull(),
        )

        return@withContext Pair(configs, windowConfig)
    }

    /**
     * Saves the given configurations to the `configs.ini` file.
     *
     * @param configs The [Configs] object to save.
     * @param windowConfig The [WindowConfig] object to save.
     */
    suspend fun save(configs: Configs, windowConfig: WindowConfig) = withContext(Dispatchers.IO) {
        val content = buildString {
            appendLine("${Incs.KEY_STRING_MAP}=${configs.dataBankPath}")
            appendLine("${Incs.KEY_WORKSHOP_DIR_ONI}=${configs.oniWorkshopDir}")
            appendLine("${Incs.KEY_ASSETS_DIR_ONI}=${configs.oniAssetsDir}")
            appendLine("${Incs.KEY_GLOSSARY_DIR}=${configs.glossaryDir}")
            appendLine("${Incs.KEY_WINDOW_POS_X}=${windowConfig.x}")
            appendLine("${Incs.KEY_WINDOW_POS_Y}=${windowConfig.y}")
            appendLine("${Incs.KEY_WINDOW_WIDTH}=${windowConfig.width}")
            appendLine("${Incs.KEY_WINDOW_HEIGHT}=${windowConfig.height}")
            if (windowConfig.darkTheme != null) {
                appendLine("${Incs.KEY_DARK_THEME}=${windowConfig.darkTheme}")
            }
        }
        try {
            configFile.writeText(content)
        } catch (e: Exception) {
            // TODO: Replace with error handling via StateFlow
            e.printStackTrace()
            println("Failed to write to config file: ${e.message}")
        }
    }

    private suspend fun copyDefaultConfig() {
        try {
            val bytes = Res.readBytes("files/configs.ini")
            configFile.writeBytes(bytes)
        } catch (e: Exception) {
            println("Failed to copy default config: ${e.message}")
        }
    }
}
