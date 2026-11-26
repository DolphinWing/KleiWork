package dolphin.desktop.apps.onitranslator.model

import dolphin.desktop.apps.onitranslator.ui.OniToken
import dolphin.desktop.apps.onitranslator.ui.hasOniSyntaxError
import dolphin.desktop.apps.onitranslator.ui.toOniTokens

/**
 * Type of tag discrepancy or repair action.
 */
enum class TagIssueType {
    STYLE_MISMATCH,   // e.g. <b>...</b> instead of <i>...</i>
    MISSING_TAG_WRAP, // e.g. unlinked term or missing <style="..."> wrap present in source
    NEWLINE_MISMATCH, // e.g. \n <-> \n\n run length mismatch or excess \n in target
}

/**
 * Represents a contiguous block of one or more consecutive line breaks in PO text.
 */
data class NewlineRun(
    val range: IntRange,
    val count: Int,
    val raw: String,
)

/**
 * Represents a localized tag candidate span in the target text that can be repaired.
 */
data class TagCandidateSpan(
    val range: IntRange,
    val text: String,
    val replacement: String,
    val issueType: TagIssueType,
    val description: String,
)

/**
 * Tag Sensor: The specialized diagnostic tool for tag consistency.
 */
object TagSensor {
    const val NEWLINE_TOKEN = "\\n"

    /**
     * Finds contiguous blocks of consecutive line breaks in [text], treating both literal `\n`
     * escapes and raw `\r\n` / `\r` / `\n` characters equivalently.
     */
    fun findNewlineRuns(text: String): List<NewlineRun> {
        if (text.isEmpty()) return emptyList()
        val runs = mutableListOf<NewlineRun>()
        var i = 0
        while (i < text.length) {
            var runStart = -1
            var runEnd = -1
            var runCount = 0
            while (i < text.length) {
                val tokenLen = when {
                    text[i] == '\r' && i + 1 < text.length && text[i + 1] == '\n' -> 2
                    text[i] == '\r' || text[i] == '\n' -> 1
                    text[i] == '\\' && i + 1 < text.length && text[i + 1] == 'n' -> 2
                    else -> 0
                }
                if (tokenLen == 0) break
                if (runStart < 0) runStart = i
                runEnd = i + tokenLen - 1
                runCount++
                i += tokenLen
            }
            if (runCount > 0) {
                runs.add(
                    NewlineRun(
                        range = runStart..runEnd,
                        count = runCount,
                        raw = text.substring(runStart, runEnd + 1),
                    )
                )
            } else {
                i++
            }
        }
        return runs
    }

    /**
     * Counts logical line breaks in [text], treating both literal `\n` escapes and
     * raw `\r\n` / `\r` / `\n` characters equivalently.
     */
    fun countNewlines(text: String): Int = findNewlineRuns(text).sumOf { it.count }

    /**
     * Counts the opening tags, dynamic tokens, and line breaks in a string.
     */
    fun countTags(text: String): Map<String, Int> {
        val tokens = text.toOniTokens()
        // println("TagSensor tokens: $tokens")
        val tags = tokens.filterIsInstance<OniToken.OpeningTag>().map { it.name }
        val dynamics = tokens.filterIsInstance<OniToken.Dynamic>().map { it.raw }
        val counts = (tags + dynamics).groupingBy { it }.eachCount().toMutableMap()
        val newlines = countNewlines(text)
        if (newlines > 0) {
            counts[NEWLINE_TOKEN] = newlines
        }
        return counts
    }

    /**
     * Diagnoses the tag consistency between source and target.
     */
    fun diagnose(source: String, target: String): TagDiagnostic {
        return TagDiagnostic(
            sourceTags = countTags(source),
            targetTags = countTags(target),
            sourceError = source.hasOniSyntaxError(),
            targetError = target.hasOniSyntaxError()
        )
    }

    private val FORMATTED_TAG_REGEX = Regex("""<(b|i)>(.*?)</\1>""", RegexOption.DOT_MATCHES_ALL)
    private val INNER_TAG_SIG_REGEX = Regex("""^<([a-zA-Z0-9_]+)(?:\s*=\s*["']?([^"'>\s]*)["']?)?>""")

    private data class FormattedTagSpan(
        val range: IntRange,
        val tag: String, // "b" or "i"
        val text: String,
        val inner: String,
        val compoundTagSignature: String?,
    )

    private fun extractFormattedSpans(text: String): List<FormattedTagSpan> {
        if (text.isEmpty()) return emptyList()
        return FORMATTED_TAG_REGEX.findAll(text).map { match ->
            val tag = match.groupValues[1]
            val inner = match.groupValues[2]
            val trimmedInner = inner.trim()
            val sigMatch = INNER_TAG_SIG_REGEX.find(trimmedInner)
            val compoundSig = sigMatch?.let {
                val tagName = it.groupValues[1].lowercase()
                val attrVal = it.groupValues.getOrNull(2)?.lowercase() ?: ""
                if (attrVal.isNotEmpty()) "$tagName=$attrVal" else tagName
            }
            FormattedTagSpan(
                range = match.range,
                tag = tag,
                text = match.value,
                inner = inner,
                compoundTagSignature = compoundSig,
            )
        }.toList()
    }

    /**
     * Finds style mismatches between source and target (e.g. official <i> vs target <b>).
     */
    fun findStyleMismatchSpans(source: String, target: String): List<TagCandidateSpan> {
        if (source.isEmpty() || target.isEmpty()) return emptyList()
        val sourceTags = countTags(source)
        val targetTags = countTags(target)
        val sourceI = sourceTags["i"] ?: 0
        val targetI = targetTags["i"] ?: 0
        val sourceB = sourceTags["b"] ?: 0
        val targetB = targetTags["b"] ?: 0

        val sourceSpans = extractFormattedSpans(source)
        val targetSpans = extractFormattedSpans(target)

        // Situation 1: Source has more <i> and Target has more <b> (convert <b>...</b> to <i>...</i>)
        if (sourceI > targetI && targetB > sourceB) {
            val quota = minOf(sourceI - targetI, targetB - sourceB)
            val availableSourceB = sourceSpans.filter { it.tag == "b" }.toMutableList()
            val unsatisfiedTargetB = mutableListOf<FormattedTagSpan>()

            for (tgt in targetSpans.filter { it.tag == "b" }) {
                // Pass 1: Verbatim inner content match (e.g. <b>75°C</b>)
                val verbatimIdx = availableSourceB.indexOfFirst { it.inner.trim() == tgt.inner.trim() }
                if (verbatimIdx >= 0) {
                    availableSourceB.removeAt(verbatimIdx)
                    continue
                }
                // Pass 2: Compound tag signature match (e.g. <b><style="logic_on">...</style></b>)
                if (tgt.compoundTagSignature != null) {
                    val compIdx = availableSourceB.indexOfFirst { it.compoundTagSignature == tgt.compoundTagSignature }
                    if (compIdx >= 0) {
                        availableSourceB.removeAt(compIdx)
                        continue
                    }
                }
                unsatisfiedTargetB.add(tgt)
            }

            return unsatisfiedTargetB.take(quota).map { tgt ->
                TagCandidateSpan(
                    range = tgt.range,
                    text = tgt.text,
                    replacement = "<i>${tgt.inner}</i>",
                    issueType = TagIssueType.STYLE_MISMATCH,
                    description = "<b>...</b> ➜ <i>...</i>"
                )
            }.filterNonOverlappingTagSpans()
        }
        // Situation 2: Source has more <b> and Target has more <i> (convert <i>...</i> to <b>...</b>)
        else if (sourceB > targetB && targetI > sourceI) {
            val quota = minOf(sourceB - targetB, targetI - sourceI)
            val availableSourceI = sourceSpans.filter { it.tag == "i" }.toMutableList()
            val unsatisfiedTargetI = mutableListOf<FormattedTagSpan>()

            for (tgt in targetSpans.filter { it.tag == "i" }) {
                // Pass 1: Verbatim inner content match
                val verbatimIdx = availableSourceI.indexOfFirst { it.inner.trim() == tgt.inner.trim() }
                if (verbatimIdx >= 0) {
                    availableSourceI.removeAt(verbatimIdx)
                    continue
                }
                // Pass 2: Compound tag signature match
                if (tgt.compoundTagSignature != null) {
                    val compIdx = availableSourceI.indexOfFirst { it.compoundTagSignature == tgt.compoundTagSignature }
                    if (compIdx >= 0) {
                        availableSourceI.removeAt(compIdx)
                        continue
                    }
                }
                unsatisfiedTargetI.add(tgt)
            }

            return unsatisfiedTargetI.take(quota).map { tgt ->
                TagCandidateSpan(
                    range = tgt.range,
                    text = tgt.text,
                    replacement = "<b>${tgt.inner}</b>",
                    issueType = TagIssueType.STYLE_MISMATCH,
                    description = "<i>...</i> ➜ <b>...</b>"
                )
            }.filterNonOverlappingTagSpans()
        }

        return emptyList()
    }

    /**
     * Finds terms in target that were wrapped with `<link>` in source, but left unlinked in target.
     * Supports both unescaped (`"`) and PO-escaped (`\"`) link attributes.
     */
    fun findMissingWrapSpans(
        source: String,
        target: String,
        terms: List<GlossaryTerm>,
    ): List<TagCandidateSpan> {
        if (source.isEmpty() || target.isEmpty() || terms.isEmpty()) return emptyList()
        val linkTagRegex = Regex("""<link\s*=\s*(\\*")([^\\">]+)\\*">([^<]+)</link>""", RegexOption.IGNORE_CASE)
        val sourceLinks = linkTagRegex.findAll(source).toList()
        if (sourceLinks.isEmpty()) return emptyList()

        val result = mutableListOf<TagCandidateSpan>()

        for (match in sourceLinks) {
            val quote = match.groupValues[1]
            val linkTarget = match.groupValues[2]
            val linkText = match.groupValues[3]

            val matchedTerm = terms.firstOrNull { term ->
                term.key.equals(linkTarget, ignoreCase = true) ||
                    term.key.endsWith(".$linkTarget", ignoreCase = true) ||
                    term.english.equals(linkText, ignoreCase = true)
            } ?: continue

            val canonical = matchedTerm.canonicalCht
            if (canonical.isEmpty()) continue

            // If target already contains this link tag around the term, skip
            val existingLinkRegex = Regex("""<link\s*=\s*\\*"(?i)${Regex.escape(linkTarget)}\\*">\s*${Regex.escape(canonical)}\s*</link>""")
            if (existingLinkRegex.containsMatchIn(target)) continue

            // Check if canonical exists in target outside tags
            val safeRanges = findSafePlainTextRanges(target, canonical)
            for (range in safeRanges) {
                val wrapReplacement = "<link=$quote$linkTarget$quote>$canonical</link>"
                result.add(
                    TagCandidateSpan(
                        range = range,
                        text = canonical,
                        replacement = wrapReplacement,
                        issueType = TagIssueType.MISSING_TAG_WRAP,
                        description = "+ <link=\"$linkTarget\">$canonical</link>"
                    )
                )
            }
        }

        return result.filterNonOverlappingTagSpans()
    }

    /**
     * Finds plain-text substrings in [target] that correspond to `<style="...">` wrapped spans in [source]
     * when the target is missing those style wrappers. Uses conservative matching via glossary terms,
     * [chsReference] same-attribute `<style>` values (via OpenCC), and literal text matching.
     */
    fun findMissingStyleWrapSpans(
        source: String,
        target: String,
        terms: List<GlossaryTerm>,
        chsReference: String? = null,
    ): List<TagCandidateSpan> {
        if (source.isEmpty() || target.isEmpty() || !source.contains("<style", ignoreCase = true)) {
            return emptyList()
        }
        val styleTagRegex = Regex("""<style\s*=\s*(\\*")([^\\">]+)\\*">([^<]+)</style>""", RegexOption.IGNORE_CASE)
        val sourceStyles = styleTagRegex.findAll(source).toList()
        if (sourceStyles.isEmpty()) return emptyList()

        val sourceCountsByAttr = sourceStyles.groupingBy { it.groupValues[2].lowercase() }.eachCount()
        val targetCountsByAttr = styleTagRegex.findAll(target)
            .groupingBy { it.groupValues[2].lowercase() }
            .eachCount()
        val remainingQuota = sourceCountsByAttr.mapValues { (attr, srcCount) ->
            (srcCount - (targetCountsByAttr[attr] ?: 0)).coerceAtLeast(0)
        }.toMutableMap()
        if (remainingQuota.values.none { it > 0 }) return emptyList()

        val chsByAttr = if (!chsReference.isNullOrBlank() && chsReference.contains("<style", ignoreCase = true)) {
            styleTagRegex.findAll(chsReference).groupBy(
                keySelector = { it.groupValues[2].lowercase() },
                valueTransform = { it.groupValues[3].trim() },
            )
        } else {
            emptyMap()
        }

        val result = mutableListOf<TagCandidateSpan>()
        val claimedRanges = mutableListOf<IntRange>()
        val seenOrdinalByAttr = mutableMapOf<String, Int>()

        for (match in sourceStyles) {
            val quote = match.groupValues[1]
            val styleAttr = match.groupValues[2]
            val attrLower = styleAttr.lowercase()
            val quota = remainingQuota[attrLower] ?: 0
            if (quota <= 0) continue

            val ordinal = seenOrdinalByAttr[attrLower] ?: 0
            seenOrdinalByAttr[attrLower] = ordinal + 1

            val innerEng = match.groupValues[3].trim()
            if (innerEng.isEmpty()) continue

            val candidates = linkedSetOf<String>()
            val matchedTerm = terms.firstOrNull { it.english.equals(innerEng, ignoreCase = true) }
            if (matchedTerm != null && matchedTerm.canonicalCht.isNotBlank()) {
                candidates.add(matchedTerm.canonicalCht)
            }
            val chsList = chsByAttr[attrLower].orEmpty()
            val orderedChs = buildList {
                chsList.getOrNull(ordinal)?.let { add(it) }
                chsList.forEachIndexed { idx, s -> if (idx != ordinal) add(s) }
            }
            for (chsInner in orderedChs) {
                if (chsInner.isBlank()) continue
                val tcVariant = TextRefinery.sc2tc(chsInner).trim()
                if (tcVariant.isNotEmpty()) candidates.add(tcVariant)
                candidates.add(chsInner)
            }
            candidates.add(innerEng)

            var selectedRanges: List<IntRange> = emptyList()
            var selectedText = ""
            for (candidate in candidates) {
                val safe = findSafePlainTextRanges(target, candidate).filter { r ->
                    claimedRanges.none { c -> r.first <= c.last && r.last >= c.first }
                }
                if (safe.isNotEmpty()) {
                    selectedRanges = safe
                    selectedText = candidate
                    break
                }
            }

            if (selectedRanges.isNotEmpty()) {
                val toEmit = selectedRanges.take(quota)
                for (r in toEmit) {
                    claimedRanges.add(r)
                    result.add(
                        TagCandidateSpan(
                            range = r,
                            text = selectedText,
                            replacement = "<style=$quote$styleAttr$quote>$selectedText</style>",
                            issueType = TagIssueType.MISSING_TAG_WRAP,
                            description = "+ <style=\"$styleAttr\">$selectedText</style>",
                        )
                    )
                }
                remainingQuota[attrLower] = quota - toEmit.size
            }
        }

        return result.filterNonOverlappingTagSpans()
    }

    /**
     * Builds actionable newline repair candidates when source and target newline runs differ.
     * - Case A (same number of newline runs, different run lengths such as `\n` vs `\n\n`): align runs 1:1.
     * - Case B (target has more total newlines than source): emit per-run removal candidates without batch delete.
     * - Case C (target has fewer runs than source): return empty so valid breaks are never offered for deletion.
     */
    fun findNewlineSpans(source: String, target: String): List<TagCandidateSpan> {
        val sourceRuns = findNewlineRuns(source)
        val targetRuns = findNewlineRuns(target)
        val sourceTotal = sourceRuns.sumOf { it.count }
        val targetTotal = targetRuns.sumOf { it.count }

        if (sourceTotal == targetTotal && sourceRuns.map { it.count } == targetRuns.map { it.count }) {
            return emptyList()
        }

        // Case A: Equal run count with individual run-length mismatch (e.g. \n <-> \n\n)
        if (sourceRuns.isNotEmpty() && sourceRuns.size == targetRuns.size) {
            return targetRuns.mapIndexedNotNull { idx, tgtRun ->
                val expectedCount = sourceRuns[idx].count
                if (tgtRun.count == expectedCount) {
                    null
                } else {
                    val replacement = "\\n".repeat(expectedCount)
                    val actualDisplay = "\\n".repeat(tgtRun.count)
                    TagCandidateSpan(
                        range = tgtRun.range,
                        text = tgtRun.raw,
                        replacement = replacement,
                        issueType = TagIssueType.NEWLINE_MISMATCH,
                        description = "#${idx + 1}: $actualDisplay ➜ $replacement",
                    )
                }
            }
        }

        // Case B: Target has excess newlines compared to source -> offer individual removal candidates
        if (targetTotal > sourceTotal && targetRuns.isNotEmpty()) {
            return targetRuns.mapIndexed { idx, tgtRun ->
                val actualDisplay = "\\n".repeat(tgtRun.count)
                TagCandidateSpan(
                    range = tgtRun.range,
                    text = tgtRun.raw,
                    replacement = "",
                    issueType = TagIssueType.NEWLINE_MISMATCH,
                    description = "#${idx + 1}: − $actualDisplay",
                )
            }
        }

        // Case C: Missing entire newline run in target -> highlight via dual-pane UI without destructive chips
        return emptyList()
    }

    /**
     * Returns non-overlapping ranges in [text] for complete `<tag>...</tag>` elements whose names
     * appear in [tagNames] (e.g. `"link"`, `"style"`), used to highlight missing wrappers in the Source pane.
     */
    fun findTagRangesByNames(text: String, tagNames: Set<String>): List<IntRange> {
        if (text.isEmpty() || tagNames.isEmpty()) return emptyList()
        val ranges = mutableListOf<IntRange>()
        for (name in tagNames) {
            val escaped = Regex.escape(name)
            val regex = Regex("""<$escaped\b[^>]*>.*?</$escaped>""", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            regex.findAll(text).forEach { ranges.add(it.range) }
        }
        return ranges.sortedBy { it.first }
    }

    private fun findSafePlainTextRanges(text: String, targetSubstring: String): List<IntRange> {
        if (targetSubstring.isEmpty() || text.isEmpty()) return emptyList()
        val ranges = mutableListOf<IntRange>()
        var idx = 0
        while (idx < text.length) {
            val foundIdx = text.indexOf(targetSubstring, idx)
            if (foundIdx < 0) break
            val range = foundIdx until (foundIdx + targetSubstring.length)
            if (!isRangeInsideAnyTag(text, range)) {
                ranges.add(range)
            }
            idx = foundIdx + targetSubstring.length
        }
        return ranges
    }

    private fun isRangeInsideAnyTag(text: String, range: IntRange): Boolean {
        // Check if range is inside < ... > (attribute or tag name)
        var inAngle = false
        for (i in text.indices) {
            if (text[i] == '<') inAngle = true
            if (inAngle && (i in range)) return true
            if (text[i] == '>') inAngle = false
        }
        // Also check if range is already enclosed inside <link...>...</link> or <style...>...</style>
        val wrappedRegex = Regex(
            """<(?:link|style)\s*=\s*\\*"[^"]*\\*">(.*?)</(?:link|style)>""",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        for (m in wrappedRegex.findAll(text)) {
            if (range.first >= m.range.first && range.last <= m.range.last) {
                return true
            }
        }
        return false
    }

    /**
     * Unified search for all tag candidate spans (style mismatches, missing link/style wraps, & newlines).
     */
    fun findCandidateSpans(
        source: String,
        target: String,
        terms: List<GlossaryTerm> = emptyList(),
        chsReference: String? = null,
    ): List<TagCandidateSpan> {
        val styles = findStyleMismatchSpans(source, target)
        val linkWraps = findMissingWrapSpans(source, target, terms)
        val styleWraps = findMissingStyleWrapSpans(source, target, terms, chsReference)
        val newlines = findNewlineSpans(source, target)
        return (styles + linkWraps + styleWraps + newlines).filterNonOverlappingTagSpans()
    }

    /**
     * Replaces all tag candidate spans in [text] using Right-to-Left (RTL) replacement.
     */
    fun applyBatchReplacement(
        text: String,
        spans: List<TagCandidateSpan>,
    ): String {
        if (spans.isEmpty() || text.isEmpty()) return text
        val sorted = spans.filterNonOverlappingTagSpans().sortedByDescending { it.range.first }
        var result = text
        for (span in sorted) {
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
     * Replaces a single tag candidate span in [text].
     */
    fun applySingleReplacement(
        text: String,
        span: TagCandidateSpan,
    ): String {
        if (span.range.first in 0..text.length &&
            span.range.last < text.length &&
            span.range.first <= span.range.last
        ) {
            return text.replaceRange(span.range, span.replacement)
        }
        return text
    }
}

/**
 * Data class to hold tag diagnostic results.
 */
data class TagDiagnostic(
    val sourceTags: Map<String, Int>,
    val targetTags: Map<String, Int>,
    val sourceError: Boolean = false,
    val targetError: Boolean = false,
) {
    /**
     * True if there is a mismatch between source and target tags.
     */
    val hasMismatch: Boolean
        get() = sourceTags != targetTags

    /**
     * True if there is any issue (source error or mismatch).
     */
    val hasIssue: Boolean
        get() = sourceError || targetError || hasMismatch

    /**
     * Helper to get a human-readable difference report.
     */
    fun getDiffReport(): Map<String, Pair<Int, Int>> {
        val allTags = sourceTags.keys + targetTags.keys
        return allTags.associateWith { tag ->
            (sourceTags[tag] ?: 0) to (targetTags[tag] ?: 0)
        }.filter { it.value.first != it.value.second }
    }
}

/**
 * Filters out overlapping tag candidate spans.
 */
fun List<TagCandidateSpan>.filterNonOverlappingTagSpans(): List<TagCandidateSpan> {
    if (size <= 1) return this
    val sorted = this.sortedWith(compareBy({ it.range.first }, { -(it.range.last - it.range.first) }))
    val result = mutableListOf<TagCandidateSpan>()
    var lastClaimedEnd = -1
    for (span in sorted) {
        if (span.range.first > lastClaimedEnd) {
            result.add(span)
            lastClaimedEnd = span.range.last
        }
    }
    return result
}
