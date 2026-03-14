package dolphin.desktop.apps.onitranslator.model

import dolphin.desktop.apps.onitranslator.ui.toOniTokens
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TagSensorTest {

    @Test
    fun testCountTags() {
        val text = "<link=\"DATABANK\">Data</link> and <style=\"KKeyword\">Skill</style> and <b>Bold</b>"
        val counts = TagSensor.countTags(text)
        
        assertEquals(1, counts["link"])
        assertEquals(1, counts["style"])
        assertEquals(1, counts["b"])
        assertEquals(0, counts["i"] ?: 0)
    }

    @Test
    fun testCountMultipleTags() {
        val text = "<b>One</b> and <b>Two</b> and <i>Three</i>"
        val counts = TagSensor.countTags(text)
        
        assertEquals(2, counts["b"])
        assertEquals(1, counts["i"])
    }

    @Test
    fun testDiagnoseMismatch() {
        val source = "Pick <color=#ffff00>Blueprint</color>"
        val target = "選擇藍圖" // Missing color tag
        
        val diag = TagSensor.diagnose(source, target)
        
        assertTrue(diag.hasMismatch)
        assertTrue(diag.hasIssue)
        assertEquals(1, diag.getDiffReport()["color"]?.first)
        assertEquals(0, diag.getDiffReport()["color"]?.second)
    }

    @Test
    fun testDiagnoseMatch() {
        val source = "Pick <color=#ffff00>Blueprint</color>"
        val target = "選擇 <color=#ffff00>藍圖</color>"
        
        val diag = TagSensor.diagnose(source, target)
        
        assertFalse(diag.hasMismatch)
        assertFalse(diag.hasIssue)
    }

    @Test
    fun testSourceError() {
        val source = "Unclosed <b>tag"
        val target = "未關閉的 <b>標籤"
        
        val diag = TagSensor.diagnose(source, target)
        
        assertTrue(diag.sourceError)
        assertTrue(diag.targetError)
        assertFalse(diag.hasMismatch) // Counts are same, but syntax is wrong
        assertTrue(diag.hasIssue)
    }

    @Test
    fun testNestedTags() {
        val text = "<b><color=#ff0000>Nested</color></b>"
        val counts = TagSensor.countTags(text)
        
        assertEquals(1, counts["b"])
        assertEquals(1, counts["color"])
    }

    @Test
    fun testDynamicTokens() {
        val source = "Press {Hotkey} to move"
        val target = "按移動" // Missing {Hotkey}
        
        val diag = TagSensor.diagnose(source, target)
        
        assertTrue(diag.hasMismatch)
        assertTrue(diag.hasIssue)
        assertEquals(1, diag.getDiffReport()["{Hotkey}"]?.first)
        assertEquals(0, diag.getDiffReport()["{Hotkey}"]?.second)
    }

    @Test
    fun testDynamicTokensMatch() {
        val source = "Press {Hotkey} to move"
        val target = "按 {Hotkey} 移動"
        
        val diag = TagSensor.diagnose(source, target)
        
        assertFalse(diag.hasIssue)
    }

    @Test
    fun testUserReportedCase() {
        val source = "<smallcaps>[Voice Recognition Initialized]\\n[Subject Identified: B111]</smallcaps>\\n\\n[LOG BEGINS]\\n\\n[A throat clears.]\\n\\nB111: We are now reliably printing healthy, living subjects, though all have exhibited unusual qualities as a result of the cloning process.\\n\\n[Squeaking sounds can be heard.]\\n\\nB111: Unusual vocalizations, benign growths, and missing appendages have been seen in all subjects thus far, to varying degrees of severity. It seems that bypassing or accelerating juvenility halts certain critical stages of development. Brain function, however, appears typical.\\n\\n[Squeaking.]\\n\\nB111: T-They also seem quite happy.\\n\\nB111: Dr. Broussard, signing off.\\n\\n[LOG ENDS]"
        val target = "<smallcaps>[語音辨識系統初始化完成]\\n[主體已識別：B111]</smallcaps>\\n\\n[日誌開始]\\n\\n[清嗓聲。]\\n\\nB111：我們現在可以可靠地列印出健康、有生命的實驗體，雖然複製的結果都表現出了不尋常的品質。\\n\\n[可以聽到吱吱聲。]\\n\\nB111：到目前為止，所有的實驗體都可以見到不同程度的異常發聲、良性生長與肢體殘缺。繞過或加速幼年期似乎會阻止實驗體某些關鍵的發展階段。不過，大腦功能似乎完整。\\n\\n[吱吱聲。]\\n\\nB111：他－他們似乎相當高興。\\n\\nB111：Broussard 博士，簽字。\\n\\n[日誌結束]"
        
        val diag = TagSensor.diagnose(source, target)
        assertFalse(diag.hasIssue, "Should not have issues with smallcaps.")
    }

    @Test
    fun testEmailInAngleBrackets() {
        // Use symmetrical tags for this test
        val source = "To: <b>Dr. B</b><color=#ffffff><size=12> <obroussard@gravitas.nova></size></color>"
        val target = "收件：<b>Dr. B</b><color=#ffffff><size=12> <obroussard@gravitas.nova></size></color>"
        
        val counts = TagSensor.countTags(source)
        assertEquals(1, counts["b"])
        assertEquals(1, counts["color"])
        assertEquals(1, counts["size"])
        assertFalse(counts.containsKey("obroussard@gravitas.nova"), "Email address should not be a tag")
        
        val diag = TagSensor.diagnose(source, target)
        assertFalse(diag.hasIssue, "Email address in angle brackets should not cause issues.")
    }

    @Test
    fun testComplexEmailHeader() {
        // Even if Klei's tags are malformed, TagSensor should at least not count the email as a tag
        val source = "<smallcaps>To: <ob@g.n></smallcaps>"
        val target = "<smallcaps>收件：<ob@g.n></smallcaps>"
        
        val diag = TagSensor.diagnose(source, target)
        assertFalse(diag.hasIssue, "Complex email header should be valid if tags are balanced.")
    }
}
