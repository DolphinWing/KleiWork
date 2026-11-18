package dolphin.desktop.apps.onitranslator.model

/**
 * Filter modes for the entry browser list.
 */
enum class EntryFilterMode {
    /**
     * Pending items: newly added, msgid changed, modified in session, or has draft.
     */
    Pending,

    /**
     * Entries with tag syntax or style mismatches detected by TagSensor.
     */
    Diagnostic,

    /**
     * Entries with unreviewed terminology conflicts detected by GlossarySensor.
     */
    Glossary,

    /**
     * All entries in the PO translation file.
     */
    All,
}
