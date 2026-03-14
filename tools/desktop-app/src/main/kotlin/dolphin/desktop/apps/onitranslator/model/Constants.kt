package dolphin.desktop.apps.onitranslator.model

/**
 * Constants for INI configuration keys.
 */
object Incs {
    const val CONFIG_FILE_NAME = "configs.ini"
    const val KEY_STRING_MAP = "stringMap"
    const val KEY_WORKSHOP_DIR_ONI = "workshopDir_oni"
    const val KEY_ASSETS_DIR_ONI = "assetsDir_oni"
    const val KEY_GLOSSARY_DIR = "glossaryDir"
    const val KEY_WINDOW_POS_X = "windowPosX"
    const val KEY_WINDOW_POS_Y = "windowPosY"
    const val KEY_WINDOW_WIDTH = "windowWidth"
    const val KEY_WINDOW_HEIGHT = "windowHeight"
    const val KEY_DARK_THEME = "darkTheme"
}

/**
 * Constants for system property keys.
 */
object SystemProperties {
    const val USER_HOME = "user.home"
    const val OS_NAME = "os.name"
}

/**
 * Constants for file and directory paths.
 */
object FilePaths {
    const val DEFAULT_CONFIG_PATH = "/common/configs.ini" // Path within resources
    const val STRINGS_XML_NAME = "strings.xml"
    const val APP_DATA_FOLDER_LEGACY = "dst-translator"
    const val APP_DATA_FOLDER = "oni-translator"
}
