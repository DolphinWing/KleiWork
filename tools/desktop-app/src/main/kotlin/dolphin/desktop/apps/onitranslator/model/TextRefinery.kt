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
     * Also refines paired double quotes (both raw " and escaped \") to Chinese corner brackets 「」.
     *
     * @param source The source string to perform replacements on.
     * @return The string after all replacements have been applied.
     */
    fun refactor(source: String): String {
        return replacementRegex?.replace(source) { matchResult ->
            dataBank[matchResult.value] ?: matchResult.value
        } ?: source
    }

    fun refineQuotes(source: String): String {
        var quoteCount = 0
        var inTag = false
        var i = 0
        val len = source.length

        fun getQuoteSpan(index: Int): Int {
            if (index < len && source[index] == '"') return 1
            if (index + 1 < len && source[index] == '\\' && source[index + 1] == '"') return 2
            return 0
        }

        while (i < len) {
            val c = source[i]
            if (c == '<') {
                inTag = true
                i++
            } else if (c == '>') {
                inTag = false
                i++
            } else if (!inTag) {
                val span = getQuoteSpan(i)
                if (span > 0) {
                    quoteCount++
                    i += span
                } else {
                    i++
                }
            } else {
                i++
            }
        }

        if (quoteCount == 0 || quoteCount % 2 != 0) {
            return source
        }

        val sb = StringBuilder()
        inTag = false
        var isOpening = true
        i = 0
        while (i < len) {
            val c = source[i]
            if (c == '<') {
                inTag = true
                sb.append(c)
                i++
            } else if (c == '>') {
                inTag = false
                sb.append(c)
                i++
            } else if (!inTag) {
                val span = getQuoteSpan(i)
                if (span > 0) {
                    sb.append(if (isOpening) '「' else '」')
                    isOpening = !isOpening
                    i += span
                } else {
                    sb.append(c)
                    i++
                }
            } else {
                sb.append(c)
                i++
            }
        }
        return sb.toString()
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
