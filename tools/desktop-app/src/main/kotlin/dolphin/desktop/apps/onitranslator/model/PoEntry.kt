package dolphin.desktop.apps.onitranslator.model

/**
 * A word item in Klei PO file.
 *
 * @property key entry key.
 * @property text entry text (msgctxt). Usually it is the same to the key.
 * @property id entry id (msgid). It presents original text in English.
 * @property str entry value (msgstr). It is also the translated text.
 * @property newly true if the entry is a new one in latest update (missing from strings.po)
 * @property msgidChanged true if the entry's msgid has changed since last translation
 * @property changed non-zero value as last changed time
 */
data class PoEntry(
    val key: String,
    val text: String,
    val id: String,
    var str: String,
    val newly: Boolean = false,
    val msgidChanged: Boolean = false,
    var changed: Long = 0L,
) {
    companion object {
        /**
         * Convert 4-line data from Klei PO file.
         */
        fun from(line1: String, line2: String, line3: String, line4: String): PoEntry? {
            val key = if (line1.startsWith("#.") or line1.startsWith("#:")) {
                line1.substring(3)
            } else line1
            val txt = if (line2.startsWith("msgctxt")) {
                line2.substring(8)
            } else line2
            val id = if (line3.startsWith("msgid")) {
                line3.substring(6)
            } else line3
            val str = if (line4.startsWith("msgstr")) {
                line4.substring(7)
            } else line4
            // We allow empty str now to accurately track existing keys in strings.po
            return if (key.isNotEmpty() && id.isNotEmpty()) {
                PoEntry(key, txt, id, str)
            } else null
        }

        /**
         * Drop quote from string start and string end.
         *
         * @return string without leading and trailing quote
         */
        fun String.dropQuote(): String {
            return if (this.startsWith("\"") && this.endsWith("\"")) {
                this.drop(1).dropLast(1)
            } else this
        }
    }

    constructor(key: String, id: String, newly: Boolean = true) : this(key, key, id, id, newly)

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + id.hashCode()
        result = 31 * result + str.hashCode()
        result = 31 * result + newly.hashCode()
        result = 31 * result + msgidChanged.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PoEntry) return false
        return (other.key == this.key && other.id == this.id && other.str == this.str &&
                other.newly == this.newly && other.msgidChanged == this.msgidChanged)
    }

    /**
     * @return entry key
     */
    fun key(): String = key.dropQuote()

    /**
     * @return entry text before translation
     */
    fun msgId(): String = id.dropQuote()

    /**
     * @return entry text after translation
     */
    fun msgStr(): String = str.dropQuote()
}
