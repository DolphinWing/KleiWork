package dolphin.desktop.apps.onitranslator.model

/**
 * Toggles controlling which auxiliary context blocks are injected into a prompt.
 */
data class PromptContextOptions(
    val includeSiblings: Boolean = true,
    val includeRecent: Boolean = false,
)

/**
 * A sibling entry sharing the same parent key prefix (e.g., `.NAME` paired with `.DESC`).
 */
data class SiblingContext(
    val key: String,
    val suffix: String,
    val sourceText: String,
    val currentTranslation: String?,
    val chsReference: String? = null,
)

/**
 * A recently finalized translation from the current editing session.
 */
data class RecentTranslationContext(
    val key: String,
    val sourceText: String,
    val translatedText: String,
)

/**
 * Snapshot of the dynamic context selected for a prompt, used for UI transparency and inspection.
 */
data class PromptContextSnapshot(
    val matchedTerms: List<GlossaryTerm> = emptyList(),
    val siblings: List<SiblingContext> = emptyList(),
    val recent: List<RecentTranslationContext> = emptyList(),
)

/**
 * Complete assembled prompt pair and context metadata ready to send to Gemini.
 */
data class TranslationBrief(
    val systemInstruction: String,
    val userPrompt: String,
    val snapshot: PromptContextSnapshot,
)

/**
 * Pure prompt assembly engine for ONI localization requests.
 *
 * Dynamically retrieves per-entry glossary terms, sibling key context, optional recent
 * session translations, and `msgidChanged` diff baselines without sending the entire glossary.
 */
object PromptComposer {
    const val SYSTEM_PROMPT_RESOURCE = "files/brigette_system_prompt.md"
    const val DEFAULT_MAX_GLOSSARY_TERMS = 25
    const val DEFAULT_MAX_SIBLINGS = 4
    const val DEFAULT_MAX_RECENT = 3

    private val linkTagRegex = Regex(
        """<link\s*=\s*\\*"?([^\\">]+)\\*"?>\s*([^<]*?)\s*</link>""",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val anyTagRegex = Regex("""<[^>]+>""")

    private val siblingSuffixPriority = mapOf(
        "NAME" to 0,
        "TITLE" to 1,
        "SUBTITLE" to 2,
        "DESC" to 3,
        "DESCRIPTION" to 4,
        "EFFECT" to 5,
        "TOOLTIP" to 6,
        "BODY" to 7,
    )

    /**
     * Extracts glossary terms relevant to [sourceText] using a tag-first + longest-match-first strategy.
     *
     * Pass 1 prioritizes explicit `<link="ID">Text</link>` references.
     * Pass 2 scans non-tag plain text using word boundaries, preferring longer phrases first.
     */
    fun extractRelevantGlossary(
        sourceText: String,
        glossaryTerms: List<GlossaryTerm>,
        maxTerms: Int = DEFAULT_MAX_GLOSSARY_TERMS,
    ): List<GlossaryTerm> {
        if (sourceText.isBlank() || glossaryTerms.isEmpty() || maxTerms <= 0) return emptyList()

        val result = mutableListOf<GlossaryTerm>()
        val seenIds = mutableSetOf<String>()
        val claimedSpans = mutableListOf<IntRange>()

        fun addTerm(term: GlossaryTerm): Boolean {
            val id = term.key.ifBlank { term.english.lowercase() }
            if (!seenIds.add(id)) return false
            result.add(term)
            return true
        }

        // Pass 1: Explicit <link="ID">Text</link> matching
        if (sourceText.contains("<link", ignoreCase = true)) {
            val matches = linkTagRegex.findAll(sourceText).toList()
            for (match in matches) {
                claimedSpans.add(match.range)
                if (result.size >= maxTerms) continue

                val linkTarget = match.groupValues[1].trim()
                val linkText = match.groupValues[2].trim()

                val matchedTerm = glossaryTerms.firstOrNull { term ->
                    term.key.equals(linkTarget, ignoreCase = true) ||
                        term.key.endsWith(".$linkTarget", ignoreCase = true) ||
                        term.key.endsWith(".$linkTarget.NAME", ignoreCase = true)
                } ?: glossaryTerms.firstOrNull { term ->
                    linkText.isNotBlank() && term.english.equals(linkText, ignoreCase = true)
                }

                if (matchedTerm != null) {
                    addTerm(matchedTerm)
                }
            }
        }

        if (result.size >= maxTerms) return result

        // Protect all XML-like markup tags so attributes/tag names (e.g., <style="KKeyword">)
        // are never matched by plain-text glossary scanning.
        anyTagRegex.findAll(sourceText).forEach { match ->
            claimedSpans.add(match.range)
        }

        // Pass 2: Plain-text longest-match-first scan
        val sortedByLength = glossaryTerms.sortedByDescending { it.english.length }
        for (term in sortedByLength) {
            if (result.size >= maxTerms) break
            if (term.english.length < 3) continue
            val id = term.key.ifBlank { term.english.lowercase() }
            if (id in seenIds) continue
            if (!sourceText.contains(term.english, ignoreCase = true)) continue

            val nonOverlapping = term.wordRegex.findAll(sourceText).filter { match ->
                claimedSpans.none { span -> match.range.first <= span.last && match.range.last >= span.first }
            }.toList()
            if (nonOverlapping.isEmpty()) continue

            nonOverlapping.forEach { claimedSpans.add(it.range) }
            addTerm(term)
        }

        return result
    }

    /**
     * Finds direct sibling entries sharing the same parent key prefix (e.g., `.NAME`, `.DESC`, `.EFFECT`).
     * Works regardless of whether the translator edits `.NAME` before `.DESC` or in reverse order.
     */
    fun findSiblingEntries(
        currentKey: String,
        allEntries: List<PoEntry>,
        chsLookup: (String) -> String? = { null },
        maxSiblings: Int = DEFAULT_MAX_SIBLINGS,
    ): List<SiblingContext> {
        val cleanKey = currentKey.trim('"')
        if (cleanKey.isBlank() || allEntries.isEmpty() || maxSiblings <= 0) return emptyList()

        val parentPrefix = cleanKey.substringBeforeLast('.', "")
        // Require at least 3 dot-separated segments in the parent prefix (e.g., STRINGS.ELEMENTS.OXYGEN)
        // to avoid pulling overly broad groups like STRINGS.UI.
        if (parentPrefix.isBlank() || parentPrefix.count { it == '.' } < 2) return emptyList()

        val prefixWithDot = "$parentPrefix."
        return allEntries.asSequence()
            .mapNotNull { entry ->
                val entryKey = entry.key()
                if (entryKey == cleanKey || !entryKey.startsWith(prefixWithDot)) return@mapNotNull null
                val suffix = entryKey.removePrefix(prefixWithDot)
                // Direct siblings only (exclude nested grandchildren)
                if (suffix.isBlank() || suffix.contains('.')) return@mapNotNull null

                val source = entry.msgId()
                val currentStr = entry.msgStr()
                val translated = currentStr.takeIf { it.isNotEmpty() && it != source }
                val chsRef = chsLookup(entryKey)?.takeIf { it.isNotEmpty() }

                SiblingContext(
                    key = entryKey,
                    suffix = suffix,
                    sourceText = source,
                    currentTranslation = translated,
                    chsReference = chsRef,
                )
            }
            .sortedWith(
                compareBy<SiblingContext>(
                    { siblingSuffixPriority[it.suffix.uppercase()] ?: 10 },
                    { it.suffix.uppercase() },
                )
            )
            .take(maxSiblings)
            .toList()
    }

    /**
     * Composes the full [TranslationBrief] (system instruction, user prompt, and context snapshot)
     * for the given [entry]. Preserves exact whitespace and literal `\n` escapes in PO strings.
     */
    fun compose(
        entry: EditorData,
        systemInstruction: String,
        glossaryTerms: List<GlossaryTerm> = emptyList(),
        allEntries: List<PoEntry> = emptyList(),
        chsLookup: (String) -> String? = { null },
        recentTranslations: List<RecentTranslationContext> = emptyList(),
        options: PromptContextOptions = PromptContextOptions(),
        maxGlossaryTerms: Int = DEFAULT_MAX_GLOSSARY_TERMS,
        maxSiblings: Int = DEFAULT_MAX_SIBLINGS,
        maxRecent: Int = DEFAULT_MAX_RECENT,
    ): TranslationBrief {
        val currentKey = entry.target.key()
        val matchedTerms = extractRelevantGlossary(entry.sourceText, glossaryTerms, maxGlossaryTerms)
        val siblings = if (options.includeSiblings) {
            findSiblingEntries(currentKey, allEntries, chsLookup, maxSiblings)
        } else {
            emptyList()
        }
        val recent = if (options.includeRecent && maxRecent > 0) {
            recentTranslations
                .filter { it.key != currentKey && it.translatedText.isNotEmpty() }
                .take(maxRecent)
        } else {
            emptyList()
        }

        val userPrompt = buildString {
            appendLine("## Target Entry")
            appendLine("```po")
            appendLine("#. $currentKey")
            appendLine("msgctxt \"$currentKey\"")
            appendLine("msgid \"${entry.sourceText}\"")
            appendLine("msgstr \"${entry.target.msgStr()}\"")
            appendLine("```")

            if (!entry.oldSourceText.isNullOrEmpty()) {
                appendLine()
                appendLine("## Source Change Diff (msgidChanged)")
                appendLine("- Previous English (old msgid): \"${entry.oldSourceText}\"")
                if (!entry.poText.isNullOrEmpty()) {
                    appendLine("- Existing Translation (PoSave): \"${entry.poText}\"")
                }
            }

            if (!entry.chsReference.isNullOrEmpty()) {
                appendLine()
                appendLine("## Official Simplified Chinese Reference")
                appendLine("\"${entry.chsReference}\"")
            }

            if (matchedTerms.isNotEmpty()) {
                appendLine()
                appendLine("## Locked Glossary Terms")
                matchedTerms.forEach { term ->
                    val keySuffix = if (term.key.isNotBlank()) " (${term.key})" else ""
                    appendLine("- ${term.english} => ${term.canonicalCht}$keySuffix")
                }
            }

            if (siblings.isNotEmpty()) {
                appendLine()
                appendLine("## Related Sibling Entries")
                siblings.forEach { sibling ->
                    appendLine("- [${sibling.suffix}] ${sibling.key}")
                    appendLine("  English: \"${sibling.sourceText}\"")
                    if (sibling.currentTranslation != null) {
                        appendLine("  Current Translation: \"${sibling.currentTranslation}\"")
                    } else {
                        appendLine("  Current Translation: (not yet translated)")
                        sibling.chsReference?.let { chs ->
                            appendLine("  ChsRef: \"$chs\"")
                        }
                    }
                }
            }

            if (recent.isNotEmpty()) {
                appendLine()
                appendLine("## Recent Session Translations")
                recent.forEach { item ->
                    appendLine("- ${item.key}")
                    appendLine("  English: \"${item.sourceText}\"")
                    appendLine("  Finalized Translation: \"${item.translatedText}\"")
                }
            }
        }.trimEnd()

        return TranslationBrief(
            systemInstruction = systemInstruction.trim(),
            userPrompt = userPrompt,
            snapshot = PromptContextSnapshot(
                matchedTerms = matchedTerms,
                siblings = siblings,
                recent = recent,
            ),
        )
    }
}
