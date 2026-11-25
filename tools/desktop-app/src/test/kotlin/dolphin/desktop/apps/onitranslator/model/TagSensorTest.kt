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

    @Test
    fun testStyleMismatchBoldToItalicAlignment() {
        val source = "<i>Overheats at:</i> <b>75°C</b>"
        val target = "<b>過熱於：</b> <b>75°C</b>"

        val diag = TagSensor.diagnose(source, target)
        assertTrue(diag.hasIssue)
        assertTrue(diag.hasMismatch)

        val spans = TagSensor.findStyleMismatchSpans(source, target)
        assertEquals(1, spans.size, "Verbatim matching 75°C must be excluded, only <b>過熱於：</b> should be flagged")
        assertEquals("<b>過熱於：</b>", spans[0].text)
        assertEquals("<i>過熱於：</i>", spans[0].replacement)

        val replaced = TagSensor.applyBatchReplacement(target, spans)
        assertEquals("<i>過熱於：</i> <b>75°C</b>", replaced)

        // Now verify diagnostic passes!
        val diagAfter = TagSensor.diagnose(source, replaced)
        assertFalse(diagAfter.hasIssue, "Style should be matched perfectly after alignment")
    }

    @Test
    fun testStyleMismatchWithCompoundStyleTags() {
        val source = "Contains an internal Memory, and will output whatever signal is stored in that Memory.\n\nSignals sent to the Inputs <i>only</i> affect the Memory, and do not pass through to the Output. \n\nSending a <b><style=\"logic_on\">Green Signal</style></b> to the Set Port (S) will set the memory to <b><style=\"logic_on\">Green</style></b>. \n\nSending a <b><style=\"logic_on\">Green Signal</style></b> to the Reset Port (R) will reset the memory back to <b><style=\"logic_off\">Red</style></b>."
        val target = "內含一個記憶體，會輸出其中儲存的訊號。\n\n傳送至輸入端的訊號<b>只會</b>影響記憶體，並不會傳遞至輸出端。\n\n傳送<b><style=\"logic_on\">綠色訊號</style></b>到設定端（S），會設定記憶體為<b><style=\"logic_on\">綠色</style></b>。\n\n傳送<b><style=\"logic_on\">綠色訊號</style></b>到重設端（R），會將記憶體重設為<b><style=\"logic_off\">紅色</style></b>。"

        val diag = TagSensor.diagnose(source, target)
        println("testStyleMismatchWithCompoundStyleTags diag: hasIssue=${diag.hasIssue}, hasMismatch=${diag.hasMismatch}, diffReport=${diag.getDiffReport()}")
        assertTrue(diag.hasIssue)
        assertTrue(diag.hasMismatch)

        val spans = TagSensor.findStyleMismatchSpans(source, target)
        println("testStyleMismatchWithCompoundStyleTags spans count: ${spans.size}, spans=$spans")
        assertEquals(1, spans.size, "Only the plain bold tag '<b>只會</b>' should be flagged, not the 3 matching compound style tags")
        assertEquals("<b>只會</b>", spans[0].text)
        assertEquals("<i>只會</i>", spans[0].replacement)

        val replaced = TagSensor.applyBatchReplacement(target, spans)
        assertFalse(TagSensor.diagnose(source, replaced).hasIssue)
    }

    @Test
    fun testStyleMismatchNonGreedyMultipleTags() {
        val source = "<i>First</i> and <i>Second</i>"
        val target = "<b>第一</b> 和 <b>第二</b>"

        val spans = TagSensor.findStyleMismatchSpans(source, target)
        assertEquals(2, spans.size, "Non-greedy matching must extract 2 distinct spans without swallowing the middle")
        assertEquals("<b>第一</b>", spans[0].text)
        assertEquals("<i>第一</i>", spans[0].replacement)
        assertEquals("<b>第二</b>", spans[1].text)
        assertEquals("<i>第二</i>", spans[1].replacement)

        val batchReplaced = TagSensor.applyBatchReplacement(target, spans)
        assertEquals("<i>第一</i> 和 <i>第二</i>", batchReplaced)
    }

    @Test
    fun testMissingTagAutoWrap() {
        val source = "Emits <link=\"OXYGEN\">Oxygen</link> gas."
        val target = "排放氧氣。"
        val terms = listOf(
            GlossaryTerm("ELEMENTS", "Oxygen", "氧氣", "STRINGS.ELEMENTS.OXYGEN.NAME")
        )

        val spans = TagSensor.findMissingWrapSpans(source, target, terms)
        assertEquals(1, spans.size)
        assertEquals("氧氣", spans[0].text)
        assertEquals("<link=\"OXYGEN\">氧氣</link>", spans[0].replacement)

        val wrapped = TagSensor.applyBatchReplacement(target, spans)
        assertEquals("排放<link=\"OXYGEN\">氧氣</link>。", wrapped)

        val diag = TagSensor.diagnose(source, wrapped)
        assertFalse(diag.hasIssue, "Tag mismatch should be resolved after wrapping")
    }

    @Test
    fun testNewlineCountAndMismatch() {
        val singleMatch = TagSensor.diagnose("Line 1\\nLine 2", "第一行\\n第二行")
        assertFalse(singleMatch.hasIssue)
        assertEquals(1, singleMatch.sourceTags[TagSensor.NEWLINE_TOKEN])
        assertEquals(1, singleMatch.targetTags[TagSensor.NEWLINE_TOKEN])

        val doubleMatch = TagSensor.diagnose("Para 1\\n\\nPara 2", "第一段\\n\\n第二段")
        assertFalse(doubleMatch.hasIssue)
        assertEquals(2, doubleMatch.sourceTags[TagSensor.NEWLINE_TOKEN])

        val missingSingle = TagSensor.diagnose("Line 1\\nLine 2", "單行被合併")
        assertTrue(missingSingle.hasMismatch)
        assertTrue(missingSingle.hasIssue)
        assertEquals(1 to 0, missingSingle.getDiffReport()[TagSensor.NEWLINE_TOKEN])

        val collapsedDouble = TagSensor.diagnose("Para 1\\n\\nPara 2", "第一段\\n第二段")
        assertTrue(collapsedDouble.hasMismatch)
        assertEquals(2 to 1, collapsedDouble.getDiffReport()[TagSensor.NEWLINE_TOKEN])

        val extraTarget = TagSensor.diagnose("Single line", "多出\\n換行")
        assertTrue(extraTarget.hasMismatch)
        assertEquals(0 to 1, extraTarget.getDiffReport()[TagSensor.NEWLINE_TOKEN])

        val mixedRawAndLiteral = TagSensor.diagnose("A\r\nB\nC", "甲\\n乙\\n丙")
        assertFalse(mixedRawAndLiteral.hasIssue)
        assertEquals(2, mixedRawAndLiteral.sourceTags[TagSensor.NEWLINE_TOKEN])
    }
}
