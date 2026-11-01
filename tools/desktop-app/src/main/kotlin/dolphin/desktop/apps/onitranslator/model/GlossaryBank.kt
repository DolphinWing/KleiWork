package dolphin.desktop.apps.onitranslator.model

import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Represents a single terminology entry from the glossary.
 *
 * @param category Category or domain of the term (e.g. "BUILDINGS", "ELEMENTS").
 * @param english The original English term (e.g. "Cool Steam Vent").
 * @param canonicalCht The standard traditional Chinese translation (e.g. "低溫蒸氣噴孔").
 * @param key The ONI string key path (e.g. "STRINGS.BUILDINGS.PREFABS.COOLSTEAMVENT.NAME").
 */
data class GlossaryTerm(
    val category: String,
    val english: String,
    val canonicalCht: String,
    val key: String,
)

/**
 * Manages the in-memory dictionary of approved terminology.
 * Loads directly from `glossary.tsv` or falls back to extracting from existing PO entries.
 */
class GlossaryBank {
    companion object {
        /**
         * Whitelist of string key segments representing genuine game entity terminology.
         * Filters out UI filter buttons, chore groups, and action menus.
         */
        val ENTITY_WHITELIST = listOf(
            "BUILDINGS.PREFABS.",
            "ELEMENTS.",
            "CREATURES.SPECIES.",
            "ITEMS.",
            "EQUIPMENT.PREFABS.",
            "DUPLICANTS.DISEASES.",
            "DUPLICANTS.TRAITS.",
            "RESEARCH.TECHS.",
            "ROOMS.TYPES.",
            "WORLD_TRAITS.",
            "WORLDS.",
            "SUBWORLDS.",
            "ROBOTS.MODELS.",
        )
    }

    private val _terms = mutableListOf<GlossaryTerm>()

    /**
     * Terms sorted by English length descending to support greedy longest-match-first matching.
     */
    val terms: List<GlossaryTerm> get() = _terms

    private val termMap = mutableMapOf<String, GlossaryTerm>()

    /**
     * Loads terminology from a TSV file (exported glossary).
     * Expected format: Category \t English \t Chinese \t Path
     *
     * @param tsvFile The TSV file to load from.
     * @return True if loaded successfully with at least one term.
     */
    fun loadFromTsv(tsvFile: File): Boolean {
        if (!tsvFile.exists() || !tsvFile.isFile) return false
        return try {
            val lines = tsvFile.readLines(StandardCharsets.UTF_8)
            val parsed = mutableListOf<GlossaryTerm>()
            lines.forEachIndexed { index, line ->
                // Skip TSV header line if present
                if (index == 0 && line.startsWith("Category\t")) return@forEachIndexed
                val parts = line.split("\t")
                if (parts.size >= 3) {
                    val category = parts[0].trim()
                    val english = parts[1].trim()
                    val cht = parts[2].trim()
                    val key = if (parts.size >= 4) parts[3].trim() else ""
                    if (english.isNotBlank() && cht.isNotBlank()) {
                        parsed.add(GlossaryTerm(category, english, cht, key))
                    }
                }
            }
            if (parsed.isNotEmpty()) {
                setTerms(parsed)
                true
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Fallback mechanism: extracts terms from in-memory `.NAME` PO entries if `glossary.tsv` does not exist.
     * Uses strict entity whitelist to avoid extracting generic UI or chore items.
     */
    fun fallbackFromPoEntries(entries: List<PoEntry>) {
        val singleLevelExceptions = listOf("BLUEPRINTS", "ELEMENTS", "WORLD_TRAITS", "WORLDS")
        val tagRegex = Regex("<[^>]+>")
        val seen = mutableSetOf<String>()
        val parsed = mutableListOf<GlossaryTerm>()

        entries.forEach { entry ->
            val key = entry.key()
            val isWhitelisted = ENTITY_WHITELIST.any { key.contains(it) }
            if (key.endsWith(".NAME") && isWhitelisted) {
                val eng = entry.msgId().replace(tagRegex, "").replace("\\\"", "\"").trim()
                val cht = entry.msgStr().replace(tagRegex, "").replace("\\\"", "\"").trim()
                if (eng.contains(":") || eng.contains("{") || eng.contains("}")) return@forEach
                if (eng.isNotBlank() && cht.isNotBlank() && eng != cht && seen.add(eng)) {
                    val parts = key.split(".")
                    val category = if (parts.size >= 3) {
                        val mainCat = parts[1]
                        if (mainCat in singleLevelExceptions) mainCat else "${parts[1]}.${parts[2]}"
                    } else if (parts.size >= 2) {
                        parts[1]
                    } else "GENERAL"
                    parsed.add(GlossaryTerm(category, eng, cht, key))
                }
            }
        }
        setTerms(parsed)
    }

    /**
     * Replaces the current terms list and updates the lookup indices.
     */
    fun setTerms(newTerms: List<GlossaryTerm>) {
        _terms.clear()
        // Sort descending by length of English term to prioritize greedy longest matching
        _terms.addAll(newTerms.sortedByDescending { it.english.length })
        termMap.clear()
        _terms.forEach { term ->
            termMap[term.english.lowercase()] = term
        }
    }

    /**
     * Finds a term by English text (case-insensitive).
     */
    fun findByEnglish(english: String): GlossaryTerm? = termMap[english.lowercase()]

    /**
     * Finds a term by ONI key path.
     */
    fun findByKey(key: String): GlossaryTerm? = _terms.firstOrNull { it.key.equals(key, ignoreCase = true) }
}
