package dolphin.desktop.apps.onitranslator.model

import dolphin.desktop.apps.onitranslator.ui.OniToken
import dolphin.desktop.apps.onitranslator.ui.hasOniSyntaxError
import dolphin.desktop.apps.onitranslator.ui.toOniTokens

/**
 * Type of tag discrepancy or repair action.
 */
enum class TagIssueType {
    STYLE_MISMATCH,   // e.g. <b>...</b> instead of <i>...</i>
    MISSING_TAG_WRAP, // e.g. unlinked term that source has <link> for
}

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
     * Counts logical line breaks in [text], treating both literal `\n` escapes and
     * raw `\r\n` / `\r` / `\n` characters equivalently.
     */
    fun countNewlines(text: String): Int {
        if (text.isEmpty()) return 0
        var count = 0
        var i = 0
        while (i < text.length) {
            when (text[i]) {
                '\r' -> {
                    count++
                    if (i + 1 < text.length && text[i + 1] == '\n') i++
                }
                '\n' -> count++
                '\\' -> {
                    if (i + 1 < text.length && text[i + 1] == 'n') {
                        count++
                        i++
                    }
                }
            }
            i++
        }
        return count
    }

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
                    description = "對齊官方標籤: <b>...</b> ➜ <i>...</i>"
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
                    description = "對齊官方標籤: <i>...</i> ➜ <b>...</b>"
                )
            }.filterNonOverlappingTagSpans()
        }

        return emptyList()
    }

    /**
     * Finds terms in target that were wrapped with <link> in source, but left unlinked in target.
     */
    fun findMissingWrapSpans(
        source: String,
        target: String,
        terms: List<GlossaryTerm>,
    ): List<TagCandidateSpan> {
        if (source.isEmpty() || target.isEmpty() || terms.isEmpty()) return emptyList()
        val linkTagRegex = Regex("""<link="([^"]+)">([^<]+)</link>""", RegexOption.IGNORE_CASE)
        val sourceLinks = linkTagRegex.findAll(source).toList()
        if (sourceLinks.isEmpty()) return emptyList()

        val result = mutableListOf<TagCandidateSpan>()

        for (match in sourceLinks) {
            val linkTarget = match.groupValues[1]
            val linkText = match.groupValues[2]

            val matchedTerm = terms.firstOrNull { term ->
                term.key.equals(linkTarget, ignoreCase = true) ||
                    term.key.endsWith(".$linkTarget", ignoreCase = true) ||
                    term.english.equals(linkText, ignoreCase = true)
            } ?: continue

            val canonical = matchedTerm.canonicalCht
            if (canonical.isEmpty()) continue

            // If target already contains this link tag around the term, skip
            val existingLinkRegex = Regex("""<link="(?i)${Regex.escape(linkTarget)}">\s*${Regex.escape(canonical)}\s*</link>""")
            if (existingLinkRegex.containsMatchIn(target)) continue

            // Check if canonical exists in target outside tags
            val safeRanges = findSafePlainTextRanges(target, canonical)
            for (range in safeRanges) {
                val wrapReplacement = """<link="$linkTarget">$canonical</link>"""
                result.add(
                    TagCandidateSpan(
                        range = range,
                        text = canonical,
                        replacement = wrapReplacement,
                        issueType = TagIssueType.MISSING_TAG_WRAP,
                        description = "自動包裹: <link=\"$linkTarget\">$canonical</link>"
                    )
                )
            }
        }

        return result.filterNonOverlappingTagSpans()
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
        // Also check if range is already enclosed inside a <link...>...</link>
        val linkRegex = Regex("""<link="[^"]*">(.*?)</link>""", RegexOption.IGNORE_CASE)
        for (m in linkRegex.findAll(text)) {
            if (range.first >= m.range.first && range.last <= m.range.last) {
                return true
            }
        }
        return false
    }

    /**
     * Unified search for all tag candidate spans (style mismatches & missing wraps).
     */
    fun findCandidateSpans(
        source: String,
        target: String,
        terms: List<GlossaryTerm> = emptyList(),
    ): List<TagCandidateSpan> {
        val styles = findStyleMismatchSpans(source, target)
        val wraps = findMissingWrapSpans(source, target, terms)
        return (styles + wraps).filterNonOverlappingTagSpans()
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
