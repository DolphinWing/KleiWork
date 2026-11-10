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
 * Represents a localized candidate text span within a translation target string
 * that has been identified for terminology replacement.
 *
 * @param range The exact character index range within the target string.
 * @param text The current text at this range.
 * @param replacement The canonical replacement string.
 * @param conflict The associated glossary conflict.
 */
data class GlossaryCandidateSpan(
    val range: IntRange,
    val text: String,
    val replacement: String,
    val conflict: GlossaryConflict,
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

        // 1. Tag-First: Scan explicit ONI link tags (fast short-circuit if no tag present)
        if (sourceText.contains("<link", ignoreCase = true)) {
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

                    val conflict = GlossaryConflict(
                        term = matchedTerm,
                        canonicalTranslation = matchedTerm.canonicalCht,
                        matchedSourceText = linkText,
                        linkTarget = linkTarget,
                    )
                    // Verify if targetText contains the canonical translation, or still has candidate spans
                    if (!targetText.contains(matchedTerm.canonicalCht) || findCandidateSpans(targetText, conflict).isNotEmpty()) {
                        conflicts.add(conflict)
                    }
                }
            }
        }

        // 2. Greedy Plain Text Matching: Scan remaining source text by descending term length
        // Multi-word compound rule: Unlinked plain text matching is only performed for terms
        // with 2 or more words (e.g. "Cool Steam Vent", "Natural Gas").
        // Single-word terms (e.g. "All", "Building", "Water") are only matched when explicitly
        // tagged with <link="..."> to prevent false positives on common words or verbs.
        // Fast bail-out: If sourceText has no spaces, it cannot contain any multi-word term.
        if (sourceText.contains(' ')) {
            for (term in terms) {
                // Must have at least 2 words for unlinked plain text matching
                if (!term.isMultiWord) continue

                // Skip if this term was already identified in the tag phase
                val termIdentifier = term.key.ifBlank { term.english }
                if (identifiedTermKeys.contains(termIdentifier)) continue

                // Fast bail-out: Substring check before running cached regex
                if (!sourceText.contains(term.english, ignoreCase = true)) continue

                val matches = term.wordRegex.findAll(sourceText).toList()
                for (match in matches) {
                    // If this match overlaps with any previously claimed span, skip it
                    val overlaps = claimedSpans.any { span ->
                        match.range.first <= span.last && match.range.last >= span.first
                    }
                    if (overlaps) continue

                    // Claim this span
                    claimedSpans.add(match.range)
                    identifiedTermKeys.add(termIdentifier)

                    val conflict = GlossaryConflict(
                        term = term,
                        canonicalTranslation = term.canonicalCht,
                        matchedSourceText = match.value,
                        linkTarget = null,
                    )
                    // Check if target translation contains canonical Chinese text, or still has candidate spans
                    if (!targetText.contains(term.canonicalCht) || findCandidateSpans(targetText, conflict).isNotEmpty()) {
                        conflicts.add(conflict)
                    }
                    break // One conflict per unique term per entry is sufficient
                }
            }
        }

        return GlossaryDiagnostic(
            conflicts = conflicts,
            isAccepted = isAccepted,
        )
    }

    /**
     * Finds all candidate text spans in [targetText] that should be replaced with the canonical term.
     * Guarantees that returned spans are non-overlapping and sorted by range.first.
     */
    fun findCandidateSpans(
        targetText: String,
        conflict: GlossaryConflict,
        chsReference: String? = null,
    ): List<GlossaryCandidateSpan> {
        if (targetText.isEmpty()) return emptyList()
        val canonical = conflict.canonicalTranslation
        val result = mutableListOf<GlossaryCandidateSpan>()

        // 1. Tag-First: match <link="TARGET"> in targetText
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
            tagRegex.findAll(targetText).forEach { match ->
                val innerRange = match.groups[2]!!.range
                val innerText = match.groups[2]!!.value
                if (innerText != canonical) {
                    result.add(
                        GlossaryCandidateSpan(
                            range = innerRange,
                            text = innerText,
                            replacement = canonical,
                            conflict = conflict,
                        )
                    )
                }
            }
        }
        if (result.isNotEmpty()) {
            return result.filterNonOverlapping()
        }

        // 2. Direct English match in targetText
        val englishPattern = """\b${Regex.escape(conflict.matchedSourceText)}\b"""
        val englishRegex = Regex(englishPattern, RegexOption.IGNORE_CASE)
        englishRegex.findAll(targetText).forEach { match ->
            result.add(
                GlossaryCandidateSpan(
                    range = match.range,
                    text = match.value,
                    replacement = canonical,
                    conflict = conflict,
                )
            )
        }
        if (conflict.term.english != conflict.matchedSourceText) {
            val fullEngRegex = Regex("""\b${Regex.escape(conflict.term.english)}\b""", RegexOption.IGNORE_CASE)
            fullEngRegex.findAll(targetText).forEach { match ->
                result.add(
                    GlossaryCandidateSpan(
                        range = match.range,
                        text = match.value,
                        replacement = canonical,
                        conflict = conflict,
                    )
                )
            }
        }
        if (result.isNotEmpty()) {
            return result.filterNonOverlapping()
        }

        // 3. Simplified Chinese Reference Match (OpenCC)
        if (!chsReference.isNullOrBlank()) {
            conflict.linkTarget?.let { target ->
                val chsTagRegex = Regex("""<link="(?i)${Regex.escape(target)}">([^<]+)</link>""")
                chsTagRegex.find(chsReference)?.groupValues?.get(1)?.let { chsTerm ->
                    val tcVariant = TextRefinery.sc2tc(chsTerm)
                    if (tcVariant.isNotEmpty() && tcVariant != canonical) {
                        var idx = 0
                        while (idx < targetText.length) {
                            val matchIdx = targetText.indexOf(tcVariant, idx)
                            if (matchIdx < 0) break
                            val range = matchIdx until (matchIdx + tcVariant.length)
                            result.add(
                                GlossaryCandidateSpan(
                                    range = range,
                                    text = tcVariant,
                                    replacement = canonical,
                                    conflict = conflict,
                                )
                            )
                            idx = matchIdx + tcVariant.length
                        }
                    }
                    if (chsTerm.isNotEmpty() && chsTerm != tcVariant && chsTerm != canonical) {
                        var idx = 0
                        while (idx < targetText.length) {
                            val matchIdx = targetText.indexOf(chsTerm, idx)
                            if (matchIdx < 0) break
                            val range = matchIdx until (matchIdx + chsTerm.length)
                            result.add(
                                GlossaryCandidateSpan(
                                    range = range,
                                    text = chsTerm,
                                    replacement = canonical,
                                    conflict = conflict,
                                )
                            )
                            idx = matchIdx + chsTerm.length
                        }
                    }
                }
            }
        }
        if (result.isNotEmpty()) {
            return result.filterNonOverlapping()
        }

        // 4. Character Swap Variants (e.g. 氣 <-> 汽)
        val charSwaps = LinguisticRules.characterVariants
        for ((from, to) in charSwaps) {
            if (canonical.contains(from)) {
                val variant = canonical.replace(from, to)
                if (variant != canonical) {
                    var idx = 0
                    while (idx < targetText.length) {
                        val matchIdx = targetText.indexOf(variant, idx)
                        if (matchIdx < 0) break
                        val range = matchIdx until (matchIdx + variant.length)
                        result.add(
                            GlossaryCandidateSpan(
                                range = range,
                                text = variant,
                                replacement = canonical,
                                conflict = conflict,
                            )
                        )
                        idx = matchIdx + variant.length
                    }
                }
            }
        }
        if (result.isNotEmpty()) {
            return result.filterNonOverlapping()
        }

        // 5. Longest Common Substring Heuristic (len >= 2)
        val fuzzyRanges = findAllFuzzySubstrings(targetText, canonical)
        for (r in fuzzyRanges) {
            val spanText = targetText.substring(r)
            if (spanText != canonical) {
                result.add(
                    GlossaryCandidateSpan(
                        range = r,
                        text = spanText,
                        replacement = canonical,
                        conflict = conflict,
                    )
                )
            }
        }

        return result.filterNonOverlapping()
    }

    /**
     * Finds all candidate text spans across all conflicts in [conflicts].
     * Filters out any overlapping ranges across different conflicts, sorted by range.first.
     */
    fun findAllCandidateSpans(
        targetText: String,
        conflicts: List<GlossaryConflict>,
        chsReference: String? = null,
    ): List<GlossaryCandidateSpan> {
        if (targetText.isEmpty() || conflicts.isEmpty()) return emptyList()
        val allSpans = mutableListOf<GlossaryCandidateSpan>()
        for (conflict in conflicts) {
            allSpans.addAll(findCandidateSpans(targetText, conflict, chsReference))
        }
        return allSpans.filterNonOverlapping()
    }

    /**
     * Replaces all candidate spans in [text] using Right-to-Left (RTL) replacement.
     * Because replacements are executed from highest index to lowest index,
     * earlier character offsets remain unaffected and strictly accurate.
     */
    fun applyBatchReplacement(
        text: String,
        spans: List<GlossaryCandidateSpan>,
    ): String {
        if (spans.isEmpty() || text.isEmpty()) return text
        val validSpans = spans.filterNonOverlapping().sortedByDescending { it.range.first }
        var result = text
        for (span in validSpans) {
            if (span.range.first in 0..result.length &&
                span.range.last < result.length &&
                span.range.first <= span.range.last
            ) {
                result = result.replaceRange(span.range, span.replacement)
            }
        }
        return result
    }

    /**
     * Replaces a single candidate span in [text].
     */
    fun applySingleReplacement(
        text: String,
        span: GlossaryCandidateSpan,
    ): String {
        if (span.range.first in 0..text.length &&
            span.range.last < text.length &&
            span.range.first <= span.range.last
        ) {
            return text.replaceRange(span.range, span.replacement)
        }
        return text
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
        val spans = findCandidateSpans(currentText, conflict, chsReference)
        return if (spans.isNotEmpty()) {
            applyBatchReplacement(currentText, spans)
        } else {
            currentText
        }
    }

    fun findBestFuzzySubstring(text: String, canonical: String): IntRange? {
        return findAllFuzzySubstrings(text, canonical).firstOrNull()
    }

    fun findAllFuzzySubstrings(text: String, canonical: String): List<IntRange> {
        if (canonical.length < 3 || text.isEmpty()) return emptyList()

        // Track ranges that already match canonical exactly so we never flag them
        val canonicalOccurrences = mutableListOf<IntRange>()
        var cIdx = 0
        while (cIdx < text.length) {
            val fIdx = text.indexOf(canonical, cIdx)
            if (fIdx < 0) break
            canonicalOccurrences.add(fIdx until (fIdx + canonical.length))
            cIdx = fIdx + canonical.length
        }

        val ranges = mutableListOf<IntRange>()

        for (subLen in (canonical.length - 1) downTo 2) {
            // Require overlap ratio >= 50% to prevent reckless overwriting
            if (subLen.toDouble() / canonical.length < 0.5) continue

            for (start in 0..(canonical.length - subLen)) {
                val sub = canonical.substring(start, start + subLen)
                val subVariants = LinguisticRules.getVariants(sub)
                for (v in subVariants) {
                    var searchFrom = 0
                    while (searchFrom < text.length) {
                        val idx = text.indexOf(v, searchFrom)
                        if (idx < 0) break

                        searchFrom = idx + v.length.coerceAtLeast(1)

                        // If this match is already inside an exact canonical term, ignore it
                        if (canonicalOccurrences.any { idx in it }) {
                            continue
                        }

                        val prefixCharsNeeded = start
                        val suffixCharsNeeded = canonical.length - (start + subLen)

                        var left = idx
                        var expandedLeft = 0
                        if (prefixCharsNeeded > 0) {
                            while (left > 0 && expandedLeft < prefixCharsNeeded) {
                                val prevChar = text[left - 1]
                                if (LinguisticRules.isDelim(prevChar)) break
                                left--
                                expandedLeft++
                            }
                        }

                        var right = idx + v.length
                        var expandedRight = 0
                        if (suffixCharsNeeded > 0) {
                            while (right < text.length && expandedRight < suffixCharsNeeded) {
                                val nextChar = text[right]
                                if (LinguisticRules.isDelim(nextChar)) break
                                right++
                                expandedRight++
                            }
                        }

                        // Prevent incorrect expansions: if canonical expects prefix/suffix characters,
                        // the text must also have corresponding non-delimiter characters to be replaced.
                        if (prefixCharsNeeded > 0 && expandedLeft == 0) continue
                        if (suffixCharsNeeded > 0 && expandedRight == 0) continue

                        val range = left until right
                        if (range.count() >= 2) {
                            val matchedText = text.substring(range)
                            if (matchedText != canonical) {
                                ranges.add(range)
                            }
                        }
                    }
                }
            }
            if (ranges.isNotEmpty()) {
                break
            }
        }
        return ranges.filterNonOverlappingRanges()
    }
}

/**
 * Filters out overlapping candidate spans, preferring earlier and longer spans.
 */
fun List<GlossaryCandidateSpan>.filterNonOverlapping(): List<GlossaryCandidateSpan> {
    if (size <= 1) return this
    val sorted = this.sortedWith(compareBy({ it.range.first }, { -(it.range.last - it.range.first) }))
    val result = mutableListOf<GlossaryCandidateSpan>()
    var lastClaimedEnd = -1
    for (span in sorted) {
        if (span.range.first > lastClaimedEnd) {
            result.add(span)
            lastClaimedEnd = span.range.last
        }
    }
    return result
}

/**
 * Filters out overlapping IntRanges, preferring earlier and longer ranges.
 */
fun List<IntRange>.filterNonOverlappingRanges(): List<IntRange> {
    if (size <= 1) return this
    val sorted = this.sortedWith(compareBy({ it.first }, { -(it.last - it.first) }))
    val result = mutableListOf<IntRange>()
    var lastClaimedEnd = -1
    for (range in sorted) {
        if (range.first > lastClaimedEnd) {
            result.add(range)
            lastClaimedEnd = range.last
        }
    }
    return result
}
