package dolphin.desktop.apps.onitranslator.model

import java.io.File

/**
 * Data class to hold configuration paths.
 * @param stringMap Path to strings.xml.
 * @param oniWorkshopDir Path to the ONI workshop directory.
 * @param oniAssetsDir Path to the ONI assets directory.
 */
data class Configs(
    val stringMap: String = "",
    val oniWorkshopDir: String = "",
    val oniAssetsDir: String = "",
) {
    /**
     * Secondary constructor to initialize Configs from an Ini object.
     * @param ini The Ini object containing configuration data.
     */
    constructor(ini: Ini) : this(
        ini.stringMap,
        ini.oniWorkshopDir,
        ini.oniAssetsDir,
    )

    fun isValid(): Boolean {
        return stringMap.isNotBlank() && File(stringMap).isFile &&
                oniWorkshopDir.isNotBlank() && File(oniWorkshopDir).isDirectory &&
                oniAssetsDir.isNotBlank() && File(oniAssetsDir).isDirectory
    }
}
