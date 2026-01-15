package dolphin.desktop.apps.onitranslator.model

import java.io.File

/**
 * Data class to hold configuration paths.
 * @param dataBankPath Path to replacement_strings.xml.
 * @param oniWorkshopDir Path to the ONI workshop directory.
 * @param oniAssetsDir Path to the ONI assets directory.
 */
data class Configs(
    val dataBankPath: String = "",
    val oniWorkshopDir: String = "",
    val oniAssetsDir: String = "",
) {
    fun isValid(): Boolean {
        return dataBankPath.isNotBlank() && File(dataBankPath).isFile &&
                oniWorkshopDir.isNotBlank() && File(oniWorkshopDir).isDirectory &&
                oniAssetsDir.isNotBlank() && File(oniAssetsDir).isDirectory
    }
}
