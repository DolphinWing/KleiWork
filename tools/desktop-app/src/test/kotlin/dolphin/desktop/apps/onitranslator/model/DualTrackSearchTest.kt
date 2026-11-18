package dolphin.desktop.apps.onitranslator.model

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DualTrackSearchTest {

    @Test
    fun testSearchMatchesTagStyleAttribute() {
        val text = "Turn <style=\"logic_on\">Active</style>"
        val query = "logic_on".normalizeTextForSearch()
        assertTrue(text.matchesSearch(query), "Should match style attribute 'logic_on'")
    }

    @Test
    fun testSearchMatchesLinkTargetId() {
        val text = "Click <link=\"BUILDING_DOOR\">Door</link> to inspect"
        val query = "BUILDING_DOOR".normalizeTextForSearch()
        assertTrue(text.matchesSearch(query), "Should match link target ID 'BUILDING_DOOR'")
    }

    @Test
    fun testSearchMatchesAcrossTags() {
        val text = "Turn <style=\"logic_on\">Active</style> now"
        val query = "Turn Active now".normalizeTextForSearch()
        assertTrue(text.matchesSearch(query), "Should match natural text across tags via stripped track")
    }

    @Test
    fun testSearchMatchesPlainText() {
        val text = "Oxygen is essential for Duplicants"
        val query = "oxygen".normalizeTextForSearch()
        assertTrue(text.matchesSearch(query), "Should match plain text case-insensitively")
    }

    @Test
    fun testSearchNoMatch() {
        val text = "Turn <style=\"logic_on\">Active</style>"
        val query = "missing_word".normalizeTextForSearch()
        assertFalse(text.matchesSearch(query), "Should not match absent text")
    }

    @Test
    fun testKeySearchNormalization() {
        val key = "STRINGS.BUILDINGS.PREFABS.DOOR.NAME"
        val fuzzyQuery = "DOORNAME".normalizeKeyForSearch()
        val normalizedKey = key.normalizeKeyForSearch()
        assertTrue(normalizedKey.contains(fuzzyQuery, ignoreCase = true), "Should match normalized key without dots")
    }
}
