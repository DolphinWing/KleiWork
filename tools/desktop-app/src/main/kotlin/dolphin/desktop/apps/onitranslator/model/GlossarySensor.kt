package dolphin.desktop.apps.onitranslator.model

/**
 * Diagnostic result of checking a single translation entry against the glossary.
 *
 * @param conflicts List of terminology discrepancies detected between source and target.
 * @param isAccepted Whether the user has explicitly reviewed and accepted the new terminology.
 */
data class GlossaryDiagnostic(
    val conflicts: List<GlossaryConflict> = emptyList(),
    val isAccepted: Boolean = false,
) {
    /** True if there is at least one terminology discrepancy. */
    val hasConflict: Boolean get() = conflicts.isNotEmpty()

    /** True if there are conflicts that have NOT been reviewed/accepted by the user. */
    val isUnreviewed: Boolean get() = hasConflict && !isAccepted
}

/**
 * Details of a specific terminology conflict.
 *
 * @param term The canonical glossary term definition.
 * @param canonicalTranslation The expected traditional Chinese translation.
 * @param matchedSourceText The actual English substring or tag text found in the source.
 */
data class GlossaryConflict(
    val term: GlossaryTerm,
    val canonicalTranslation: String = term.canonicalCht,
    val matchedSourceText: String = term.english,
    val linkTarget: String? = null,
)

/**
 * Core scanning engine for terminology consistency.
 *
 * Features:
 * 1. Tag-First Matching: Prioritizes explicit `<link="KEY">` tags in ONI source text.
 * 2. Greedy Longest-Match-First: Matches longer multi-word phrases first to eliminate
 *    false positives on substrings (e.g., "Cool Steam Vent" takes precedence over "Steam").
 * 3. Non-overlapping Span Tracking: Claims character ranges to ensure shorter terms
 *    do not clash with already-identified parent terms.
 */
object GlossarySensor {

    private val linkTagRegex = Regex("""<link="([^"]+)">([^<]+)</link>""", RegexOption.IGNORE_CASE)

    /**
     * Diagnoses terminology consistency between [sourceText] and [targetText].
     *
     * @param sourceText English source string (msgid).
     * @param targetText Traditional Chinese translated string (msgstr).
     * @param terms Glossary terms, sorted by English length descending.
     * @param isAccepted Whether this entry has already been marked as accepting new terms.
     */
    fun diagnose(
        sourceText: String,
        targetText: String,
        terms: List<GlossaryTerm>,
        isAccepted: Boolean = false,
    ): GlossaryDiagnostic {
        // If untranslated or terms are empty, no conflicts can be diagnosed
        if (targetText.isBlank() || terms.isEmpty() || sourceText.isBlank()) {
            return GlossaryDiagnostic(isAccepted = isAccepted)
        }

        val conflicts = mutableListOf<GlossaryConflict>()
        val claimedSpans = mutableListOf<IntRange>()
        val identifiedTermKeys = mutableSetOf<String>()

        // 1. Tag-First: Scan explicit ONI link tags
        linkTagRegex.findAll(sourceText).forEach { match ->
            val linkTarget = match.groupValues[1]
            val linkText = match.groupValues[2]

            // Find matching term either by key path, key suffix, or English text
            val matchedTerm = terms.firstOrNull { term ->
                term.key.equals(linkTarget, ignoreCase = true) ||
                    term.key.endsWith(".$linkTarget", ignoreCase = true) ||
                    term.english.equals(linkText, ignoreCase = true)
            }

            if (matchedTerm != null) {
                claimedSpans.add(match.range)
                identifiedTermKeys.add(matchedTerm.key.ifBlank { matchedTerm.english })

                // Verify if targetText contains the canonical translation
                if (!targetText.contains(matchedTerm.canonicalCht)) {
                    conflicts.add(
                        GlossaryConflict(
                            term = matchedTerm,
                            canonicalTranslation = matchedTerm.canonicalCht,
                            matchedSourceText = linkText,
                            linkTarget = linkTarget,
                        )
                    )
                }
            }
        }

        // 2. Greedy Plain Text Matching: Scan remaining source text by descending term length
        // Multi-word compound rule: Unlinked plain text matching is only performed for terms
        // with 2 or more words (e.g. "Cool Steam Vent", "Natural Gas").
        // Single-word terms (e.g. "All", "Building", "Water") are only matched when explicitly
        // tagged with <link="..."> to prevent false positives on common words or verbs.
        for (term in terms) {
            // Skip if this term was already identified in the tag phase
            val termIdentifier = term.key.ifBlank { term.english }
            if (identifiedTermKeys.contains(termIdentifier)) continue

            // Must have at least 2 words for unlinked plain text matching
            val isMultiWord = term.english.trim().split(Regex("""\s+""")).filter { it.isNotBlank() }.size >= 2
            if (!isMultiWord) continue

            // Require word boundary for English words
            val pattern = """\b${Regex.escape(term.english)}\b"""
            val regex = try {
                Regex(pattern, RegexOption.IGNORE_CASE)
            } catch (_: Exception) {
                continue
            }

            val matches = regex.findAll(sourceText).toList()
            for (match in matches) {
                // If this match overlaps with any previously claimed span, skip it
                val overlaps = claimedSpans.any { span ->
                    match.range.first <= span.last && match.range.last >= span.first
                }
                if (overlaps) continue

                // Claim this span
                claimedSpans.add(match.range)
                identifiedTermKeys.add(termIdentifier)

                // Check if target translation contains canonical Chinese text
                if (!targetText.contains(term.canonicalCht)) {
                    conflicts.add(
                        GlossaryConflict(
                            term = term,
                            canonicalTranslation = term.canonicalCht,
                            matchedSourceText = match.value,
                            linkTarget = null,
                        )
                    )
                }
                break // One conflict per unique term per entry is sufficient
            }
        }

        return GlossaryDiagnostic(
            conflicts = conflicts,
            isAccepted = isAccepted,
        )
    }

    /**
     * Helper to safely replace or inject the canonical term into [currentText].
     *
     * 1. If an explicit `<link>` for the term exists, updates the inner text.
     * 2. If the English term is directly present in the translation, replaces it.
     * 3. If Simplified Chinese reference is available, checks OpenCC translated variants.
     * 4. Checks common phonetic/character swaps (e.g. 氣 <-> 汽).
     * 5. Fallback: fuzzy matches candidate noun phrase via longest common substring.
     */
    fun applyCanonical(
        currentText: String,
        conflict: GlossaryConflict,
        chsReference: String? = null,
    ): String {
        val canonical = conflict.canonicalTranslation

        // 1. Tag-First: match <link="TARGET"> in currentText
        val linkTargets = mutableSetOf<String>()
        conflict.linkTarget?.let { linkTargets.add(it) }
        if (conflict.term.key.isNotBlank()) {
            linkTargets.add(conflict.term.key)
            val parts = conflict.term.key.split(".")
            val shortId = parts.lastOrNull { it != "NAME" && it.isNotBlank() }
            if (shortId != null) linkTargets.add(shortId)
        }
        if (conflict.term.english.isNotBlank()) {
            linkTargets.add(conflict.term.english)
        }

        for (target in linkTargets) {
            val tagRegex = Regex("""(<link="(?i)${Regex.escape(target)}">)([^<]*)(</link>)""")
            if (tagRegex.containsMatchIn(currentText)) {
                return tagRegex.replace(currentText, "$1$canonical$3")
            }
        }

        // 2. Direct English match in currentText
        val englishPattern = """\b${Regex.escape(conflict.matchedSourceText)}\b"""
        val englishRegex = Regex(englishPattern, RegexOption.IGNORE_CASE)
        if (englishRegex.containsMatchIn(currentText)) {
            return englishRegex.replace(currentText, canonical)
        }
        if (conflict.term.english != conflict.matchedSourceText) {
            val fullEngRegex = Regex("""\b${Regex.escape(conflict.term.english)}\b""", RegexOption.IGNORE_CASE)
            if (fullEngRegex.containsMatchIn(currentText)) {
                return fullEngRegex.replace(currentText, canonical)
            }
        }

        // 3. Simplified Chinese Reference Match (OpenCC)
        if (!chsReference.isNullOrBlank()) {
            conflict.linkTarget?.let { target ->
                val chsTagRegex = Regex("""<link="(?i)${Regex.escape(target)}">([^<]+)</link>""")
                chsTagRegex.find(chsReference)?.groupValues?.get(1)?.let { chsTerm ->
                    val tcVariant = TextRefinery.sc2tc(chsTerm)
                    if (currentText.contains(tcVariant)) {
                        return currentText.replace(tcVariant, canonical)
                    }
                    if (currentText.contains(chsTerm)) {
                        return currentText.replace(chsTerm, canonical)
                    }
                }
            }
        }

        // 4. Character Swap Variants (e.g. 氣 <-> 汽)
        val charSwaps = listOf('氣' to '汽', '汽' to '氣', '裡' to '裏', '裏' to '裡', '線' to '綫', '綫' to '線')
        for ((from, to) in charSwaps) {
            if (canonical.contains(from)) {
                val variant = canonical.replace(from, to)
                if (currentText.contains(variant)) {
                    return currentText.replace(variant, canonical)
                }
            }
        }

        // 5. Longest Common Substring Heuristic (len >= 2)
        val bestMatch = findBestFuzzySubstring(currentText, canonical)
        if (bestMatch != null) {
            return currentText.replaceRange(bestMatch, canonical)
        }

        return currentText
    }

    private fun findBestFuzzySubstring(text: String, canonical: String): IntRange? {
        if (canonical.length < 3 || text.isEmpty()) return null

        for (subLen in (canonical.length - 1) downTo 2) {
            // Require overlap ratio >= 50% to prevent reckless overwriting
            if (subLen.toDouble() / canonical.length < 0.5) continue

            for (start in 0..(canonical.length - subLen)) {
                val sub = canonical.substring(start, start + subLen)
                val subVariants = listOf(sub, sub.replace('氣', '汽'), sub.replace('汽', '氣'))
                for (v in subVariants) {
                    val idx = text.indexOf(v)
                    if (idx >= 0) {
                        val prefixCharsNeeded = start
                        val suffixCharsNeeded = canonical.length - (start + subLen)

                        var left = idx
                        var expandedLeft = 0
                        while (left > 0 && expandedLeft < prefixCharsNeeded + 1) {
                            val prevChar = text[left - 1]
                            if (isDelim(prevChar)) break
                            left--
                            expandedLeft++
                        }

                        var right = idx + v.length
                        var expandedRight = 0
                        while (right < text.length && expandedRight < suffixCharsNeeded + 1) {
                            val nextChar = text[right]
                            if (isDelim(nextChar)) break
                            right++
                            expandedRight++
                        }

                        // Prevent incorrect expansions: if canonical expects prefix/suffix characters,
                        // the text must also have corresponding non-delimiter characters to be replaced.
                        if (prefixCharsNeeded > 0 && expandedLeft == 0) continue
                        if (suffixCharsNeeded > 0 && expandedRight == 0) continue

                        val range = left until right
                        if (range.count() >= 2) {
                            return range
                        }
                    }
                }
            }
        }
        return null
    }

    private val BOUNDARY_CHARS = setOf(
        '的', '了', '著', '過', '在', '從', '向', '與', '和', '或', '及', '由',
        '將', '把', '被', '讓', '給', '至', '到', '使', '令', '是', '有', '無',
        '非', '一', '二', '三', '四', '五', '六', '七', '八', '九', '十', '個',
        '座', '只', '台', '條', '位', '件', '種', '項', '處', '顆', '塊', '片',
        '根', '支', '中', '內', '外', '上', '下', '前', '後', '這', '那', '此',
        '，', '。', '、', '；', '：', '！', '？', '（', '）', '(', ')', '[',
        ']', '{', '}', '<', '>', '"', '\'', '\\', '/', '+', '=', '|', '~',
        '`', '@', '#', '$', '%', '^', '&', '*', ' ', '\t', '\r', '\n'
    )

    private fun isDelim(c: Char): Boolean = c in BOUNDARY_CHARS
}
