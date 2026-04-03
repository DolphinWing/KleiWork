package dolphin.desktop.apps.onitranslator.model

data class EditorData(
    val target: PoEntry,
    val templateText: String,
    val referenceText: String? = null,
    val draftText: String? = null,
    val officialText: String? = null,
)
