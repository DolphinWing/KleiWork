package dolphin.desktop.apps.onitranslator.model

import dolphin.desktop.apps.onitranslator.ui.OniToken
import dolphin.desktop.apps.onitranslator.ui.hasOniSyntaxError
import dolphin.desktop.apps.onitranslator.ui.toOniTokens

/**
 * Tag Sensor: The specialized diagnostic tool for tag consistency.
 */
object TagSensor {
    /**
     * Counts the opening tags and dynamic tokens in a string.
     */
    fun countTags(text: String): Map<String, Int> {
        val tokens = text.toOniTokens()
        // println("TagSensor tokens: $tokens")
        val tags = tokens.filterIsInstance<OniToken.OpeningTag>().map { it.name }
        val dynamics = tokens.filterIsInstance<OniToken.Dynamic>().map { it.raw }
        return (tags + dynamics).groupingBy { it }.eachCount()
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
