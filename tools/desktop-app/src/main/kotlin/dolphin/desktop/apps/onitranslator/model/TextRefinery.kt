package dolphin.desktop.apps.onitranslator.model

import com.github.houbb.opencc4j.util.ZhTwConverterUtil

/**
 * A class responsible for text transformations, including
 * Simplified-to-Traditional Chinese conversion and custom string replacements.
 *
 * @param dataBank A map where keys are strings to be found and values are their replacements.
 */
class TextRefinery(private val dataBank: Map<String, String>) {
    private val replacementRegex: Regex? = if (dataBank.isNotEmpty()) {
        val regexPattern = dataBank.keys.joinToString("|") { Regex.escape(it) }
        Regex(regexPattern)
    } else {
        null
    }

    /**
     * Performs a series of string replacements based on the map provided at construction.
     *
     * @param source The source string to perform replacements on.
     * @return The string after all replacements have been applied.
     */
    fun refactor(source: String): String {
        return replacementRegex?.replace(source) { matchResult ->
            dataBank[matchResult.value] ?: matchResult.value
        } ?: source
    }

    companion object {
        /**
         * Converts a string from Simplified Chinese to Traditional Chinese.
         * @param str The string in Simplified Chinese.
         * @return The string in Traditional Chinese.
         */
        fun sc2tc(str: String): String {
            return ZhTwConverterUtil.toTraditional(str)
        }
    }
}
