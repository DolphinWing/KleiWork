package dolphin.desktop.apps.onitranslator.model

import java.io.File
import java.io.InputStream
import java.nio.charset.StandardCharsets

/**
 * Manages linguistic rules, character variants, and word boundary delimiters
 * for terminology consistency and fuzzy substring analysis.
 *
 * Rules are externalized to `glossary_rules.txt`.
 */
object LinguisticRules {

    private const val RESOURCE_PATH = "composeResources/dolphin.desktop.apps.onitranslator.generated.resources/files/glossary_rules.txt"
    private const val SOURCE_FALLBACK_PATH = "src/main/composeResources/files/glossary_rules.txt"

    private val _characterVariants = mutableListOf<Pair<Char, Char>>()
    val characterVariants: List<Pair<Char, Char>> get() = _characterVariants

    private val _boundaryChars = mutableSetOf<Char>()
    val boundaryChars: Set<Char> get() = _boundaryChars

    init {
        loadRules()
    }

    fun isDelim(c: Char): Boolean = c in _boundaryChars

    /**
     * Generates all character variant forms of [sub] based on loaded character variants.
     */
    fun getVariants(sub: String): List<String> {
        val variants = mutableSetOf(sub)
        for ((from, to) in _characterVariants) {
            if (sub.contains(from)) {
                variants.add(sub.replace(from, to))
            }
        }
        return variants.toList()
    }

    /**
     * Loads rules from the resource file, with fail-safe fallbacks.
     */
    fun loadRules(customStream: InputStream? = null) {
        _characterVariants.clear()
        _boundaryChars.clear()

        val stream = customStream
            ?: Thread.currentThread().contextClassLoader.getResourceAsStream(RESOURCE_PATH)
            ?: javaClass.classLoader.getResourceAsStream(RESOURCE_PATH)
            ?: tryOpenSourceFile()

        if (stream != null) {
            try {
                parseRules(stream)
            } catch (_: Exception) {
                loadHardcodedFallback()
            }
        } else {
            loadHardcodedFallback()
        }
    }

    private fun tryOpenSourceFile(): InputStream? {
        val file = File(SOURCE_FALLBACK_PATH)
        return if (file.exists() && file.isFile) file.inputStream() else null
    }

    private fun parseRules(stream: InputStream) {
        val text = stream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        var currentSection = ""

        text.lineSequence().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach

            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                currentSection = trimmed.substring(1, trimmed.length - 1).lowercase()
                return@forEach
            }

            when (currentSection) {
                "variants" -> {
                    val parts = trimmed.split("|")
                    if (parts.size == 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty()) {
                        val c1 = parts[0][0]
                        val c2 = parts[1][0]
                        _characterVariants.add(c1 to c2)
                        _characterVariants.add(c2 to c1)
                    }
                }
                "delimiters" -> {
                    trimmed.forEach { c ->
                        if (c != '\r' && c != '\n') {
                            _boundaryChars.add(c)
                        }
                    }
                }
            }
        }
        // Always include whitespace control characters
        _boundaryChars.addAll(listOf(' ', '\t', '\r', '\n'))
    }

    private fun loadHardcodedFallback() {
        listOf('氣' to '汽', '汽' to '氣', '裡' to '裏', '裏' to '裡', '線' to '綫', '綫' to '線').forEach {
            _characterVariants.add(it)
        }
        val defaultDelims = "，。、；：！？（）()[]{}\"'\\/+=|~`@#$%^&* \t\r\n的了著過在從向與和或及由將把被讓給至到使令是有無非一二三四五六七八九十個座只台條位件種項處顆塊片根支中內外上下前後這那此請保持會可能以為當若如但而且並每各其該等甚已未須需要應即便隨復再更越極"
        defaultDelims.forEach { _boundaryChars.add(it) }
    }
}
