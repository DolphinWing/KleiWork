package dolphin.desktop.apps.onitranslator.model

/**
 * Encapsulates the complete set of text data for a single translation entry
 * within the editor, including its source, references, and draft states.
 *
 * @param target The current version of the translation entry.
 * @param sourceText Original English text from template.
 * @param oldSourceText Original English text before updates (if changed).
 * @param chsReference Simplified Chinese reference text (if available).
 * @param poText The existing translation text from the strings.po file.
 * @param draftText The unsaved modification from the draft file.
 */
data class EditorData(
    val target: PoEntry,
    val sourceText: String,
    val oldSourceText: String? = null,
    val chsReference: String? = null,
    val poText: String? = null,
    val draftText: String? = null,
)
