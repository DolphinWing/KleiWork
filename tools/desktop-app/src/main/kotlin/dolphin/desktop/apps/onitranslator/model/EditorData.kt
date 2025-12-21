package dolphin.desktop.apps.onitranslator.model

data class EditorData(
    val target: WordEntry,
    val templateText: String,
    val referenceText: String? = null,
    val draftText: String? = null,
)
