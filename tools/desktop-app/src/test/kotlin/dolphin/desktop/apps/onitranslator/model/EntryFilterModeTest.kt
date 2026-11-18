package dolphin.desktop.apps.onitranslator.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EntryFilterModeTest {

    @Test
    fun testFilterModeEntries() {
        val modes = EntryFilterMode.entries
        assertEquals(4, modes.size)
        assertTrue(modes.contains(EntryFilterMode.Pending))
        assertTrue(modes.contains(EntryFilterMode.Diagnostic))
        assertTrue(modes.contains(EntryFilterMode.Glossary))
        assertTrue(modes.contains(EntryFilterMode.All))
    }

    @Test
    fun testDefaultAppStateHasPendingMode() {
        val state = AppState()
        assertEquals(EntryFilterMode.Pending, state.filterMode)
    }
}
