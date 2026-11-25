package dolphin.desktop.apps.onitranslator.model

import java.io.File

/**
 * Data class to hold configuration paths.
 * @param dataBankPath Path to replacement_strings.xml.
 * @param oniWorkshopDir Path to the ONI workshop directory.
 * @param oniAssetsDir Path to the ONI assets directory.
 * @param glossaryDir
 */
data class Configs(
    val dataBankPath: String = "",
    val oniWorkshopDir: String = "",
    val oniAssetsDir: String = "",
    val glossaryDir: String = oniAssetsDir, // by default
    val autoSaveEnabled: Boolean = false,
    val autoSaveIntervalMinutes: Int = 5,
    val geminiApiKey: String = "",
    val geminiModel: String = "",
) {
    fun isValid(): Boolean {
        return dataBankPath.isNotBlank() && File(dataBankPath).isFile &&
                oniWorkshopDir.isNotBlank() && File(oniWorkshopDir).isDirectory &&
                oniAssetsDir.isNotBlank() && File(oniAssetsDir).isDirectory
    }

    /**
     * Resolves the active Gemini API key, falling back to the `GEMINI_API_KEY`
     * environment variable when the configured field is left blank.
     */
    fun effectiveGeminiApiKey(envProvider: (String) -> String? = System::getenv): String {
        return geminiApiKey.trim().ifBlank { envProvider("GEMINI_API_KEY")?.trim().orEmpty() }
    }
}
