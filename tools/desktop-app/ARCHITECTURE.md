# ONI Translator Architecture Guide

這份文件旨在協助人類開發者（以及未來的 AI 助手）快速理解 **ONI Translator** 專案的技術架構、設計模式與程式碼組織。

---

## 1. 專案概述 (Overview)

**ONI Translator** 是一個跨平台（目前專注於 Windows）的桌面應用程式，專門用於協助翻譯遊戲《缺氧 (Oxygen Not Included)》的語言檔案 (`.po`)。

它不只是一個純文字編輯器，還整合了：
- **簡繁轉換**：自動將簡體中文來源轉換為繁體中文。
- **詞彙替換**：透過 `replacement_strings.xml` 定義專有詞彙的自動修正規則。
- **安全暫存**：支援閒置自動存檔與手動草稿管理，並具備版本衝突視覺提示。
- **差異比對**：能識別原廠檔案的變更 (msgid change)，確保翻譯能追蹤英文原文的更新。
- **Tag Sensor (標籤感測器)**：自動對比原文與譯文的標籤與換行 (`\n`) 數量，確保 `link`, `style`, `color` 等關鍵標籤與段落換行對稱。
- **Glossary Sensor (詞彙感測器)**：具備標籤優先與貪婪長詞比對演算法，檢驗 AI 譯文與既有專有名詞的一致性，支援「一鍵套用規範詞」與「認可新詞（審閱保留）」雙態決策閉環。
- **強韌解析**：採用狀態機 (State-machine) 解析器，支援 PO 檔案 Header 處理與不規則空行。

---

## 2. 技術堆疊 (Tech Stack)

*   **語言**: Kotlin (JVM)
*   **UI 框架**: Compose Multiplatform (Compose Desktop)
*   **UI 設計系統**: Material Design 3
*   **建構工具**: Gradle (Kotlin DSL), Version Catalogs (`libs.versions.toml`)
*   **關鍵函式庫**:
    *   `opencc4j`: 簡繁轉換核心。
    *   `kotlinx.coroutines`: 非同步任務處理。
    *   `kotlinx-serialization-json`: 純 JSON AST 模式（`JsonObject` / `buildJsonObject`，免編譯外掛、零反射與零 `$$serializer` ProGuard 規則），供 Gemini API 通訊與穩定打包使用。
    *   JDK `java.net.http.HttpClient`: 輕量非同步 HTTPS 客戶端。
    *   **Compose Resources**: 用於型別安全的多語系、圖片與字體管理。

---

## 3. 架構模式 (Architectural Pattern)

本專案採用 **Model-View-Intent (MVI)** 架構，確保資料流向單一且狀態可預測。

### 核心元件

1.  **View (UI)**
    *   負責渲染介面與捕捉使用者操作。
    *   **不包含業務邏輯**。
    *   透過 `onEvent: (AppEvent) -> Unit` 將操作意圖傳遞給 ViewModel。
    *   訂閱 ViewModel 的 `StateFlow` (`AppState`, `UiState`) 來更新畫面。
    *   **設計語彙**：遵循 Material Design 3，使用「藍 (Primary)、紫 (Secondary)、金 (Tertiary)」作為核心色調，分別代表「核心功能、層次區分、活力焦點」。
    *   *進入點*: `OniTranslatorApp.kt`

2.  **ViewModel (`OniTranslatorViewModel`)**
    *   **Single Source of Truth**：持有並管理唯一的 `AppState`。
    *   **Intent Handler**：透過 `onEvent(event: AppEvent)` 接收並處理所有意圖。
    *   **Coordinator**：協調 `PoHelper` (資料處理)、`ConfigManager` (設定) 與 UI 狀態的同步。

3.  **Model (Data Layer)**
    *   負責實際的資料讀寫與運算。
    *   **`PoHelper`**: 核心邏輯所在。負責讀取 `.po` 檔、合併草稿、執行簡繁轉換、寫入檔案。
    *   **`ConfigManager`**: 負責 `configs.ini` 的讀寫與視窗狀態保存。
    *   **`TextRefinery`**: 負責封裝 `opencc4j` 與 `DataBank` 的精煉邏輯，將原始文本轉化為目標翻譯。
    *   **`DataBank`**: 負責從 `replacement_strings.xml` 載入替換規則與詞彙對照表。

### 資料流向 (Unidirectional Data Flow)

1.  **UI Event**: 使用者點擊「儲存」 -> 發送 `AppEvent.OnSaveFile`。
2.  **Processing**: ViewModel 接收 Event -> 呼叫 `PoHelper.writeTranslationFile()`。
3.  **State Update**: ViewModel 根據結果 -> 更新 `AppState` (例如顯示 Snackbar 或更新 Log)。
4.  **UI Render**: UI 觀察到 `AppState` 變更 -> 重新繪製畫面。

---

## 4. 關鍵模組詳解

### 4.1. 狀態管理 (`AppState` & `UiState`)

為了分離「核心資料」與「介面狀態」，我們將 State 分為兩層：

*   **`AppState`**: 全域應用程式狀態。
    *   `configs`: 設定檔資料。
    *   `logs`: 歷史日誌 (`List<LogEntry>`)。
    *   `filteredList`: 當前顯示的編輯列表 (`EditorData`)。統一由單一響應式管線 (`computeFilteredList()`) 運算生產，為 UI 唯一之 Single Source of Truth。
    *   `changedList`: 本次工作階段 (Session) 實際修改過的 Key 清單 (`List<String>`)。
    *   `filterMode`: 列表當前篩選模式 (`EntryFilterMode`: `Pending`, `Diagnostic`, `Glossary`, `All`)。
    *   `hasDraft`: 目前硬碟上是否存在實體草稿檔。
    *   `diagnosticCount`: 全域標籤待修條目總數（即時統計全量 `allEntries()`）。
    *   `glossaryCount`: 全域未審閱詞彙衝突待處理總數。
    *   `geminiSessionStats`: 本次工作階段之 Gemini API 累計用量統計（`requestCount`、Prompt/Candidate/Thoughts/Total Token 與總耗時）。
    *   `uiState`: 包含下方的 UI 狀態。
*   **`UiState`**: 純 UI 相關的短暫狀態。
    *   `isLoading`: 是否正在處理中。
    *   `processStatus`: 即時狀態訊息（如：儲存中...）。
    *   `searchState`: 搜尋關鍵字與是否啟用搜尋模式 (`isActive`, `text`)。
    *   `editorData`: 右側編輯器當前選中的資料。
    *   `dialogState`: 控制對話框 (Config, Save Confirm, Log Window, Jump Dialog, Exit Confirm Dialog) 的顯示。
    *   `brigetteDesk`: `BrigetteDesk` AI 翻譯疊層狀態（可見性、目標 Key、記憶體動態 `PromptContextOptions`、快照、Prompt 預覽、載入計時、結果與錯誤）。
    *   `geminiModelsState`: 設定頁載入可用模型列表之載入狀態、模型清單與錯誤訊息。

### 4.2. 翻譯與檔案處理 (`PoHelper`)

這是本專案最核心的「大腦」。它維護了四個主要來源 (Maps)：
1.  **`Source`** (`templateMap`): 來自 `strings_template.pot`，決定了輸出的順序與 Key。
2.  **`ChsRef`** (`simplifiedMap`): 來自原廠簡中檔案，作為翻譯參考。
3.  **`PoSave`** (`translatedEntries`): 來自遊戲目錄下的 `strings.po`（已儲存的譯文）。
4.  **`Draft`** (`draftEntries`): 來自系統暫存目錄下的 `.po` 暫存檔。

**合併邏輯**：Draft > PoSave > ChsRef (converted) > Source (English)。

**雙結構索引 (Ordered List + O(1) Lookup Map)**：
*   **`entryList`**：依 `templateMap` 順序建立與保存，確保 UI 列表與寫出 `.po` 檔案的順序 100% 遵循 `strings_template.pot`；透過 `allEntries()` 對外提供唯讀視圖 (`Collections.unmodifiableList`)，避免無謂陣列複製。
*   **`entryMap`**：同步維護的 `HashMap<String, PoEntry>` 旁路索引，支援 `entry(key)`、`update()`、`acceptTerm()` 與 `resetTerm()` 之 $O(1)$ 即時定位。

**差異判定 (Diff Logic)**：
為了精確篩選出「待辦項目」，`PoHelper` 採用以下判定公式：
*   **Newly**: Key 存在於 Template 但完全不存在於 `strings.po`。
*   **MsgidChanged**: Key 在兩邊皆存在，但英文原文 (`msgid`) 已變動。
*   **Modified**: 目前記憶體內容與 `PoSave` (正式檔) 不一致。

**多行與轉義處理 (Multi-line & Escape Design)**：
為了維持記憶體中資料結構的純粹性 (Single Source of Truth)，`PoHelper` 實作了雙向的 PO 字串處理機制：
*   **記憶體乾淨格式**：載入時，透過狀態機解析器排除外層雙引號與轉義，直接儲存為乾淨的字串。
*   **編輯器友善（保留 `\n`）**：在 Unescape 過程中，刻意不處理 `\n` 換行符，使其在記憶體與 UI 編輯器中均以字面的 `\n` 符號形式呈現，方便翻譯者精確對位。
*   **安全雙向轉義**：解析時僅對雙引號（`\"` 轉為 `"`）進行解碼，寫檔時（`writeEntryToFile`）再將其轉回 `\"` 並包上引號，徹底消除多行解析時容易產生的冗餘引號 `""` 錯誤。
*   **空白與縮排保留政策 (Whitespace Preservation)**：為防範格式被破壞，載入合併時嚴禁調用 `.trim()` 剪裁字串，並統一以 `.isEmpty()` / `.isNotEmpty()` 代替 `.isBlank()` 作為空值檢查。這確保了 Klei 原廠用作列表縮排的前導空白（如 `    • `）以及故意留空的 `msgid " "` 佔位符能被 100% 完整保留。

### 4.3. 資源與日誌系統

#### 多語系與檔案資源管理 (Resources)
專案全面採用 Compose Multiplatform 資源系統：
*   **字串資源**：定義於 `src/main/composeResources/values/strings.xml`，支援多國語系切換。
*   **檔案資源**：預設設定與替換規則透過 `Res.readBytes` 讀取，確保跨平台打包一致性。

#### 即時回饋機制
為了讓使用者知道程式在背後做了什麼：
*   `PoHelper` 內部產生 `LogEntry` (Info/Warning/Error)，並開放 `log()` 方法供 ViewModel 呼叫。
*   **`OniTranslatorBottomBar`** 負責呈現最新狀態，閒置時顯示當前項目總數。

### 4.4. 列表篩選與搜尋架構 (List Filtering & Search Architecture)

1.  **左側常駐篩選標籤 (Permanent Filter Tabs / `EntryFilterBar`)**
    *   **設計動機**：過往「標籤診斷」與「詞彙衝突」附屬於頂端搜尋列，導致校對者必須展開搜尋介面才能檢視問題。重構後將其提升為左側列表頂端的常駐過濾標籤 (`EntryFilterMode`)，與待辦項目具備對等之第一級公民地位。
    *   **四種過濾模式**：
        *   `待處理 (Pending)`：預設檢視，精確載入範本新增 (`newly`)、原文變動 (`msgidChanged`) 與記憶體異動 (`modified`) 之待辦清單 (`buildChangeList()`)。
        *   `標籤診斷 (Diagnostic)`：篩選全量條目中存在標籤語法錯誤或對稱性問題之項目；Tab 上即時呈現全域待修總數 `(count)` 與琥珀金高亮圖示。
        *   `詞彙衝突 (Glossary)`：篩選全量條目中包含未審閱專有名詞衝突之項目；Tab 上即時呈現待處理總數 `(count)` 與警示紅圖示。
        *   `全部 (All)`：直接瀏覽完整 PO 字典庫，無需啟用搜尋即可翻閱全量條目。
    *   **單一響應式管線 (Single Reactive Pipeline)**：所有過濾邏輯統一收斂於 ViewModel 的 `computeFilteredList()`，輸出唯一之 `state.filteredList`，消除過往 UI 端 `results` 與 `filteredList` 的平行狀態與判斷分岐。同時區分 **資料異動路徑 (`refreshDataSource`)** 與 **純視圖過濾路徑 (`refreshFilter`)**，使搜尋打字與 Tab 切換不重跑全域統計與磁碟 I/O。

2.  **頂端狀態欄與卡片指示 (Toolbar Status & Card Indicators)**
    *   **頂端狀態欄 (Toolbar Status)**：顯示「全部: %1$d, 已變更: %2$d」，分別對應當前篩選列表總數 (`filteredList.size`) 與本次工作階段實際已翻譯項目數 (`changedList.size`)。
    *   **卡片指示器與膠囊標籤 (Header Badges & Icons)**：
        *   `NEW`: 採用 `errorContainer` 半透明背景之圓角膠囊（Pill Badge），標示範本新增 Key。
        *   `MODIFIED`: 採用 `tertiaryContainer` 半透明背景之圓角膠囊，內容與正式檔 `PoSave` 不同時顯示。
        *   🟠 **標籤診斷指示**：`ic_sensor_window`（16dp），存在語法錯誤或標籤對稱性問題時呈現。
        *   🔴 **詞彙衝突指示**：`ic_report`（16dp），僅在存在**未審閱詞彙衝突**時強高亮提示；已審閱保留之新詞不顯示圖示，確保卡片右上角只聚焦於待處置問題。
        *   `Draft Chip`: 卡片下方顯示草稿檔中的原始內容，供精確比對。
        *   **卡片修改狀態**：由各項目實體之 `target.changed > 0` 獨立驅動卡片高亮邊框，解耦平行陣列索引依賴。

3.  **搜尋欄解耦與全域關鍵字搜尋 (Universal Keyword Search)**
    *   **介面精簡**：徹底移除橫跨左右兩欄之滑動式 `SearchTypeRow`，頂端搜尋欄回歸為純粹、即時的關鍵字查詢。
    *   **全域比對**：輸入搜尋關鍵字時，自動同步比對條目之 Key、英文原文 (`msgId`) 與中文譯文 (`msgStr`)，無需手動切換搜尋類別。
    *   **雙軌文字檢索 (Dual-track Matching)**：文本比對採「保留標籤本體（支援搜尋 `logic_on`、`<link>` 等樣式屬性與 ID）」與「去標籤純文字（支援跨標籤長句檢索）」雙軌並行機制，徹底杜絕標籤內部字串無法被檢索之問題。
    *   **正交交集搜尋 (Compound / Orthogonal Search)**：搜尋與分類 Tab 具備正交性。過濾母體由當前 Filter Tab 決定（例如在「標籤診斷」模式下搜尋，僅於標籤錯誤項目中檢索關鍵字；在「全部」下搜尋則檢索全量字典）。切換 Filter Tab 時完整保留搜尋文字並即時換算交集，不再強制中斷搜尋狀態。

4.  **重新整理 (Refresh)**：
    *   觸發 `RefreshSource` 時會清空編輯器狀態，確保 UI 一致性。

### 4.5. 開發與除錯 (Debug Mode)
*   **Debug Mode**：透過 `OniTranslatorViewModel(debugMode = true)` 啟動。
*   **行為差異**：存檔路徑會導向系統暫存目錄，並彈出 `DebugSaveDialog` 確認路徑。

### 4.6. UI 互動與視覺安全感

*   **視覺對位系統**：
    *   編輯器下方列出 `Source`, `ChsRef`, `PoSave` 三重參考，並可手動切換顯示。
    *   列表項目採用 Tertiary 色系標記「已修改」狀態，與 `PoSave` 標籤產生視覺連動。

*   **AI 協作與寫入/檢視動作分離 (Action Separation)**：
    *   支援從剪貼簿提取 `msgstr` 並自動解析轉義字元，實現一鍵同步。
    *   **物理空間隔離防誤觸**：編輯器底部操作列將破壞性之「智慧貼上（Write Action）」保留於左側輔助區，將非破壞性之「NisbetPeek（Inspection Action）」移至右側決策區（靠近取消與套用，並以 VerticalDivider 區隔），徹底杜絕誤觸改寫內文之風險。

*   **NisbetPeek 預覽系統與 Codex 實體透視**：
    *   **遊戲語意樣式精準還原**：支援 `<style="logic_on">`（電路綠）、`<style="logic_off">`（警示紅）、`<style="KKeyword">`（官方 Codex 青藍色 `#26C6DA`）、`<style="flavor_text/lore">`（背景故事斜體灰）。
    *   **連結膠囊化樣式**：`<link>` 呈現專屬粉紅底線與微底色膠囊襯底。
    *   **Codex 實體目標透視卡片 (Link Target IDs)**：自動提取文字內所有 `<link="ID">` 實體，於預覽區下方以 FlowRow 條列顯示 `[文字 ➜ TARGET_ID]`（如 `門 ➜ BUILDING_DOOR`），方便校對者一覽百科連結指向是否正確。
    *   內建 `Tag Sensor` 診斷，即時提示標籤不對稱或語法錯誤。

*   **列表導航與桌面捲軸**：
    *   **原生拖曳捲軸**：採用 Compose Desktop 原生 `VerticalScrollbar` + `rememberScrollbarAdapter`，支援滑鼠拖曳 Thumb 快速滑動與軌道分頁點擊。
    *   **精確跳轉 (Jump to Item)**：右上角選單提供「前往項目…」，輸入 1-based 編號即可觸發平滑捲動直達目標，並自動載入右側編輯器。

### 4.7. 草稿與進度管理 (Draft Management)

為了確保翻譯作業的安全性，專案實作了多層防護：
*   **閒置自動暫存**：
    *   可於設定中開啟，支援 1-30 分鐘閒置觸發。
    *   採用 Debounce 機制，每次編輯後重設計時器，僅在使用者停下工作時執行背景寫入。
*   **行為安全優化**：
    *   「刪除草稿」僅移除硬碟實體檔案，**保留記憶體中的編輯進度**，避免做白工。
    *   選單按鈕會根據硬碟上是否存在草稿檔自動切換 `Enabled` 狀態。
*   **Companion Sidecar 跨階段進度還原**：
    *   **伴隨檔案**：草稿儲存時，同步在暫存目錄寫入 `strings.po.keys`（Line-delimited Plain Text，記錄已被使用者編輯過之 Key 清單）。
    *   **無縫還原**：跨天重新啟動載入草稿時，由伴隨檔讀取 Key 清單並重設 `entry.changed > 0`，精確還原「已變更」統計與卡片高亮狀態。
    *   **原子化清理**：當儲存正式檔或點選刪除草稿時，伴隨檔與草稿 PO 一併清除，徹底杜絕髒狀態殘留。
*   **關閉應用程式安全防護 (Close Confirmation)**：
    *   **無髒資料直接關閉**：若 `changedList.isEmpty()`，點擊關閉視窗按鈕 (X) 立即秒關，零摩擦。
    *   **三態確認對話框**：若存在未儲存之異動 (`changedList.isNotEmpty()`)，攔截關閉事件並跳出確認對話框，提供「儲存草稿並結束」、「捨棄變更並結束」與「取消」，保障使用者對於進度落地或放棄修改之完整掌控權。

### 4.8. 品質保證 (Testing)

核心邏輯（如 `TagSensor`, `NisbetPeek` 解析層，`PoHelper` 的多行解析與引號轉義機制，以及 `PromptComposer` 與 `GeminiClient`）配備單元測試，確保複雜標籤語法、字串處理與 API 通訊的穩健性；實機連線測試另有 Opt-in Live Smoke Test（詳見第 4.14 節）。

### 4.9. 排版精煉與引號轉換 (Quote Refactoring & Display Standardization)

為了使翻譯後的字串符合中文直角引號排版規範，`TextRefinery` 整合了防禦性引號轉換機制：
* **對稱引號精煉**：在譯文進行 Refinery 的最後階段，會自動將成對的英文引號（同時支援轉義的 `\"` 與一般的 `"`）交替替換為 `「` 與 `」`。
* **防禦性偶數判定**：僅在非 Tag 的純文字（Plain Text）區段偵測到**偶數個且大於 0** 的引號總數時，才觸發替換。若引號數為奇數（代表譯文配對殘缺），則保持原樣不做更動，避免破壞排版。
* **標籤安全防護 (Token-free 狀態機)**：為避免毀壞 `<link=\"...\">` 等標籤內的屬性引號，內部採用了極輕量的括號狀態機（`inTag` 開關）。當指針處於 `<` 與 `>` 區間時，自動忽略任何引號計算與替換。同時此設計完全獨立於 `OniToken` 之外，避免了重組時字面 `\\n` 被誤還原成實際換行的副作用。
* **限定套用範圍 (Git Diff & 歷史保護)**：為保護歷史特意留存的引號並避免產生大量 Git Diff，自動引號精煉**僅限於新項目**（即 Newly 新增項目、MsgidChanged 英文變更項目、與 Fallback 遞補項目）。當載入已翻譯的歷史項目時，系統會維持其原貌。此外，當使用者點選「套用簡中參考」手動轉換時，亦會套用此精煉機制。
* **UI 資源與引號顯示規範**：
    * 繁體中文資源檔（`values-zh-rTW/strings.xml`）全面統一採用國字方角括號「%1$s」（如 `套用「%1$s」`、`詞彙衝突：原文包含「%1$s」，規範譯名為「%2$s」`）。
    * 英文資源檔（`values/strings.xml`）採用標準 XML 實體 `&quot;%1$s&quot;`。
    * 程式碼插值前（如 `EntryEditor`）一律落實 Unescape，過濾 PO 檔原生反斜線，徹底杜絕 UI 介面洩漏 `\"` 字面雜質。

### 4.10. 詞彙一致性感測與管理 (Terminology Consistency & Glossary Sensor)

為了防止 AI 輔助翻譯時發生譯名漂移或未遵循專案既有術語，系統整合了高精度的詞彙感測引擎：
* **知識庫雙向閉環 (Glossary Reuse)**：
    * 啟動時優先載入 `glossary.tsv`（由工具自身匯出，可在外部 Excel/VS Code 編輯維護）。
    * **實體白名單過濾 (Entity Whitelist)**：匯出與提取時限定收錄真實遊戲實體名詞（如 `BUILDINGS.PREFABS`, `ELEMENTS`, `CREATURES.SPECIES`, `ITEMS`, `EQUIPMENT.PREFABS`, `DUPLICANTS.DISEASES`, `DUPLICANTS.TRAITS`, `RESEARCH.TECHS`, `ROOMS.TYPES`, `WORLD_TRAITS`, `WORLDS`, `SUBWORLDS`, `ROBOTS.MODELS`），徹底杜絕 UI 圖層、工作動詞（如 `All`, `Building`, `None`）等非名詞雜質。
    * 若 `glossary.tsv` 不存在，自動由 PO 條目中提取符合白名單之 `.NAME` 作為記憶體內 fallback 基準線。
* **精準無衝突偵測 (Zero-Clash Detection)**：
    * **Tag 優先**：解析 `<link="KEY">` 標籤，直接鎖定遊戲專有名詞。
    * **雙詞複合門檻 (Multi-word Compound Rule)**：無 `<link>` 標籤之純文字僅比對 $\ge 2$ 個英文單字之複合名詞（如 `Cool Steam Vent`, `Natural Gas`）；單一英文單字（如 `Water`, `Steam`）因歧義度高，限定只在標籤存在時比對，徹底排除敘述文法句中的假警報。
    * **貪婪長詞優先 (Longest-Match-First)**：依英文長度降序排列，透過非重疊字元跨度 (Non-overlapping Span Tracking) 與 Regex 單字邊界比對，徹底消除「Cool Steam Vent」與「Steam」的子字串誤判。
    * **滿足度計數 (Satisfaction Count, $C \ge S$)**：精確統計來源出現次數 $S$ 與譯文中標準詞出現次數 $C$。若 $C \ge S$（且無標籤內文字錯位），視為該條目之術語已全數滿足，主動跳過診斷警報，根絕不必要的假警報。
    * **已知標準詞保護 (Known Canonical Protection)**：在候選跨度搜尋與模糊比對時，詞庫中所有已知的合法標準詞均被納入保護區間（`protectedOccurrences`）。比對游標嚴禁自其他標準詞內部發起，向左/向右擴展亦嚴禁跨入其他標準詞，徹底解決共享前綴之複合名詞（如 `輸送帶軌道` vs `輸送帶存儲箱`）互相判定為錯字並互相吞噬的互咬問題。
* **防禦性決策閉環 (Review & Keep)**：
    * 捨棄高風險的全域批次強制取代，採用條目級審核。
    * **一鍵套用規範詞 (Safe Apply)**：五層替換階梯（Tag 內置換 $\rightarrow$ 英文置換 $\rightarrow$ 簡中對照 $\rightarrow$ 異體字轉換 $\rightarrow$ $\ge 50\%$ 重疊度模糊邊界擴展）。若無法安全定位，絕不盲目覆蓋破壞原文，改走系統剪貼簿複製與通知提醒。
    * **標籤硬防禦 (Tag Boundary Guard)**：模糊邊界擴展時嚴格鎖定 `<` 與 `>`，絕不跨入任何標籤語法結構，防止標籤破裂。
    * **認可新詞（審閱保留）**：保留當前譯文不更動，條目被標記為已認可，隨時可復原。
* **伴隨檔跨階段持久化 (Companion Sidecar)**：
    * 伴隨草稿在 `%TEMP%\strings.po.terms` 記錄已認可新詞之 Key 清單。
    * 跨天重啟時無縫還原決策狀態；正式存檔或清除草稿時一併同步清理。
* **三層視覺防禦與過濾**：
    * **編輯器**：即時動態提示橫幅（⚠️ 未審閱衝突 vs ℹ️ 已認可新詞）與一鍵處置按鈕。
    * **卡片清單**：右上角 Chip 標示 `⚠️ 術語衝突` 或 `ℹ️ 採用新詞`。
    * **全域檢索**：`SearchType.Glossary` 一鍵篩選所有術語差異項目，供日後覆核調閱。

### 4.11. 互動式診斷編輯器與視覺化修復 (Interactive Diagnostic Editor & Quick-Fix)

為了消除翻譯者手動對位與漏改多重重複詞彙的負擔，`EntryEditor` 整合了行內可視化標記與「點擊即修復」互動工具列：
* **行內即時高亮 (`DiagnosticVisualTransformation`)**：
    * 採用裝飾層 `VisualTransformation` 與 1:1 `OffsetMapping.Identity`，徹底杜絕游標跳動與漂移。
    * 內建防禦性邊界保護（`coerceIn(0, length)`），確保高頻打字與反白刪除時不拋出越界例外。
    * **四色語意高亮**：
        * 🔴 **紅色**：純文字名詞衝突（Glossary Plain-text Conflict）
        * 🟣 **紫色**：標籤內文字錯位（Situation A: Tag Text Conflict，如 `<link="POWER">電力</link>` ➜ `電網`）
        * 🔵 **藍色**：標籤缺失待包裹（Situation B: Missing Tag Wrap，如 `氧氣` ➜ `<link="OXYGEN">氧氣</link>`）
        * 🟠 **橙色**：標籤樣式不符（Situation C: Style Mismatch，如官方 `<i>` vs 歷史 `<b>`）
* **多處重複與 RTL 倒序批次置換**：
    * 透過 `findCandidateSpans` 抓取本文內所有出現點之非重疊字元跨度（Non-overlapping Spans）。
    * 採用由右至左（Right-to-Left, RTL）執行批次替換，確保低位索引絕對不跑位。
    * 單條多處重複時提供「⚡ 一鍵全部替換」與個別「[#1]、[#2]」單點處置按鈕。
* **外置語言學規則庫 (`glossary_rules.txt` & `LinguisticRules`)**：
    * 將同音通假字對（如 `氣|汽`、`裡|裏`、`線|綫`）與斷詞邊界標點／虛詞／量詞完全從演算法抽離，儲存於 `src/main/composeResources/files/glossary_rules.txt`，確保核心程式碼 0 寫死中文字。
* **互動快速修復工具列 (`FlowRow`)**：
    * 採用 `FlowRow` 自然折行排版，徹底防止小視窗水平溢出。
    * 具備 Hover 聚焦連動（滑鼠移到 Chip 時，輸入框中對應文字加粗並加深底色）。
    * 提供一鍵修復、純文字複製、認可新詞與完整 Undo 復原機制。

### 4.12. 標籤樣式感測與防禦機制 (Tag Sensor Style Alignment & Multi-pass Protection)

為了防止在對齊官方 `<i>` 與歷史 `<b>` 樣式時發生「誤判複合標籤」或「過度替換」，`TagSensor` 實作了三層防禦架構：
* **標籤特徵解析 (Tag Feature Extraction)**：
    * 透過 `extractFormattedSpans` 為每個 `<b>` 和 `<i>` 提取內部複合標籤特徵（`compoundTagSignature`，如 `style=logic_on`、`style=logic_off`、`color=#...`）。
    * 純文字格式標籤標記為 `null`，使單純粗體/斜體與複合樣式在資料特徵層徹底解耦。
* **多階段排除與滿足配對 (Multi-pass Exclusion & Satisfaction Pairing)**：
    * **Pass 1（字面完全相符排除）**：標籤內部文字完全一致者（如 `<b>75°C</b>` 對 `<b>75°C</b>`），視為已滿足（Satisfied）並 1:1 排除，避免無效替換。
    * **Pass 2（複合標籤結構相符排除）**：若譯文與原文的複合標籤特徵數量 1:1 對應（如譯文有 3 個 `<b><style="logic_on">`，原文亦有 3 個 `<b><style="logic_on">`），直接 1:1 抵銷並排除，絕不誤判定為 `<b> ➜ <i>` 待修候選。
    * **Pass 3（孤兒標籤篩選）**：經前兩輪過濾後，僅保留真正未被原文格式滿足的標籤（如 `<b>只會</b>` 對應 `<i>only</i>`）。
* **換行對稱檢查 (`NEWLINE_TOKEN = "\\n"`)**：
    * `countTags()` 同步統計所有邏輯換行符（相容字面 `\n` 與實體 `\r\n` / `\r` / `\n`），納入全庫 `TagDiagnostic`、卡片警示、`NisbetPeek` 報告與 `BrigetteDesk` 候選卡檢驗，確保單換行或雙換行不被誤刪或增減。
* **配額守衛 (Quota Guard)**：
    * 最終候選數量嚴格受限於整體標籤差額配額：
      $$\text{quota} = \min(|source_i - target_i|,\, |source_b - target_b|)$$
    * 徹底杜絕舊版 Greedy 全域 Regex 導致候選數量暴增、一鍵套用反而改壞正確標籤的嚴重副作用。

### 4.13. AI 翻譯輔助與 Prompt 引擎 (BrigetteDesk — Step 1: `PromptComposer`)

為了在串接 Google AI Studio 時兼顧翻譯品質、術語一致性與 Free Tier Token 節制，系統將 Prompt 組裝抽離為純函式模組 [`PromptComposer`](src/main/kotlin/dolphin/desktop/apps/onitranslator/model/PromptComposer.kt)：
* **API 專用系統指令 (`brigette_system_prompt.md`)**：
    * 提煉自 `brigette-oni-localization.md`，完整保留 Brigette Aurora 人格、六項在地化鐵律（術語鎖定、標籤完整保留、台灣慣用語、標點與直角引號規範、角色名保留英文、純淨 PO 輸出）與「不以問句結尾」規範，同時移除網頁 Gem 專屬的 Google Drive / Skills 工具指令並定義 Structured JSON 輸出契約。
* **動態單條術語檢索 (`extractRelevantGlossary`)**：
    * 嚴禁全量傳送 `glossary.tsv`。採兩階段過濾：優先解析 `<link="ID">` 精準命中規範詞，再保護所有 XML 樣式標籤後以 word-boundary 掃描純文字（依英文長度降序、上限 25 條）。
* **正反序無關之同組上下文 (`findSiblingEntries`)**：
    * 依 `msgctxt` 父層前綴（至少三層結構）自動撈取直接兄弟條目（依 `.NAME` / `.TITLE` / `.DESC` / `.EFFECT` / `.TOOLTIP` 優先排序），無論譯者先翻標題或先翻說明皆可維持稱呼與語氣一致。
* **差異與歷史保留 (`compose`)**：
    * 當條目處於 `msgidChanged` 時自動注入 `oldSourceText` 與 `poText` 供模型做最小幅微調；所有 PO 字串比照 4.2 節空白保留政策，絕不 `.trim()` 並完整保留字面 `\n`。
    * 支援透過 `PromptContextOptions` 獨立開關「同組關聯詞條」與「近期譯文」。

### 4.14. Gemini API 通訊層與實機驗證 (BrigetteDesk — Step 2: `GeminiClient`)

為確保發佈打包（Release ProGuard）的穩定性並維持極簡依賴樹，系統透過 [`GeminiClient`](src/main/kotlin/dolphin/desktop/apps/onitranslator/model/GeminiClient.kt) 直接對接 Google AI Studio REST API：
* **零反射序列化與輕量傳輸**：
    * 採用 JDK 內建 `java.net.http.HttpClient` 搭配 `kotlinx-serialization-json` 純 JSON AST 解碼（免編譯器外掛、零反射與零 `$$serializer` 混淆風險），避免引入大型 SDK 的傳遞依賴。
    * API Key 僅透過 `x-goog-api-key` HTTP Header 傳遞，並於所有錯誤訊息及原始 Body 紀錄中強制遮罩為 `[REDACTED]`。
* **Structured Output 與 Thinking 過濾**：
    * `generationConfig` 指定 `responseMimeType = "application/json"` 並強制附上 OpenAPI `responseSchema`，確保回傳固定結構（`matchedTerms`、`analysis`、`options`、`recommendedIndex`、`conclusion`）。
    * 解析 `candidates[0].content.parts` 時自動過濾 `thought == true` 思考片段，於 `recommendedIndex` 越界時自動鉗位，並將 `options[].text` 內被 JSON 反序列化為實體斷行的 `\r\n` / `\r` / `\n` 正規化還原為 PO 字面 `\n`（`PoHelper.escapePoValue` 寫檔時亦提供最終防線）。
* **非同步取消、動態模型查詢與 429 配額解析**：
    * 透過 `suspendCancellableCoroutine` 橋接 `HttpClient.sendAsync`，協程取消時立即呼叫 `future.cancel(true)` 中斷網路連線；同時提取 `usageMetadata`（含 `promptTokenCount`、`candidatesTokenCount`、`thoughtsTokenCount`、`totalTokenCount`）與毫秒耗時 `elapsedMs`。
    * `listModels` 查詢 `GET /v1beta/models`，自動篩選支援 `generateContent` 之 `gemini-*` 純文字候選模型（排除 `nano-banana`、`-image`、`imagen`、`veo`、`-tts`、`-audio`、`-live`、`robotics`、`computer-use`、`deep-research`、`embedding` 等非翻譯特化子模型；過濾為空時仍安全降級）供設定頁即時挑選。
    * 遭遇 `HTTP 429` 時解析 `RetryInfo.retryDelay`（含 `Retry-After` Header fallback）與 `QuotaFailure.violations[0].quotaId`，區分每分鐘上限 (`PerMinute`) 與每日額度 (`PerDay`)。
* **Opt-in 實機煙霧測試 (Live Smoke Test)**：
    * [`GeminiClientTest`](src/test/kotlin/dolphin/desktop/apps/onitranslator/model/GeminiClientTest.kt) 內建 `testLiveSmokeAgainstAiStudio`，平時執行 `.\gradlew.bat test` 會透過 `Assumptions.assumeTrue` 自動跳過，不影響離線測試。
    * 需針對 Free Tier 或付費模型做實機驗證時，於 PowerShell 設定環境變數後單獨執行：
      ```powershell
      $env:GEMINI_API_KEY="YOUR_AI_STUDIO_KEY"
      $env:GEMINI_LIVE_TEST="true"
      # 選用，預設為 gemini-2.5-flash
      $env:GEMINI_MODEL="gemini-2.5-flash"
      .\gradlew.bat test --tests "*GeminiClientTest.testLiveSmokeAgainstAiStudio*" --info
      ```

### 4.15. 設定整合與 MVI 狀態協調 (BrigetteDesk — Step 3)

Step 3 完成設定持久化與 [`OniTranslatorViewModel`](src/main/kotlin/dolphin/desktop/apps/onitranslator/model/OniTranslatorViewModel.kt) 之全線 MVI 資料流串接：
* **設定持久化與環境變數 Fallback**：
    * `configs.ini` 僅儲存 `geminiApiKey` 與 `geminiModel`（`PromptContextOptions` 保持編輯當下之記憶體動態開關，不污染設定檔）。
    * `Configs.effectiveGeminiApiKey()` 在設定欄位留空時自動 fallback 讀取環境變數 `GEMINI_API_KEY`。
    * [`ConfigScreen`](src/main/kotlin/dolphin/desktop/apps/onitranslator/ui/ConfigScreen.kt) 支援 API Key 遮罩切換、`ExposedDropdownMenuBox` 載入並選取 AI Studio 最新模型或手動輸入 Model ID；僅異動 Gemini 設定時維持 `pathChanged = false`，不觸發 PO 檔重載。
* **MVI 生命週期與競態防護**：
    * `AppEvent.Brigette` 涵蓋 `Open`（僅在本機組裝並開啟預覽）、`UpdateContextOptions`、`UpdatePrompt`、`ResetPrompt`、`Request`、`Cancel`、`SelectOption`、`ApplyOption`、`Dismiss`。
    * 切換條目（`Editor.Select`）、取消或關閉疊層時立即取消 `aiRequestJob`；非同步回應返回時核對 `entryKey` 一致性才更新狀態，防止舊條目覆寫。
* **近期譯文視窗與 Session 統計**：
    * ViewModel 維護上限 3 筆的 `recentTranslations`（隨 `edit()` 更新並去重），供 `PromptComposer` 動態取用。
    * 每次翻譯請求成功自動累加 `AppState.geminiSessionStats`，並將精簡遙測寫入 `PoHelper.log()`（嚴禁寫入 API Key）；`ApplyOption` 比照智慧貼上直接呼叫 `edit()` 落地並觸發 AutoSave。

### 4.16. BrigetteDesk 疊層介面與統計呈現 (Step 4)

Step 4 完成 [`BrigetteDeskDrawer`](src/main/kotlin/dolphin/desktop/apps/onitranslator/ui/BrigetteDesk.kt) 疊層 UI、編輯器觸發入口與底部狀態列累計呈現：
* **預覽後確認再發 (Stage-then-Send)、響應式版面與互斥疊層**：
    * [`EntryEditor`](src/main/kotlin/dolphin/desktop/apps/onitranslator/ui/EntryEditor.kt) 於 `EntryTagType.Source` 原文參考卡片右側（緊鄰複製按鈕）新增 `BrigetteDesk` 入口按鈕，點擊僅先在本機跑 `PromptComposer` 並展開疊層預覽（不立即消耗 API 額度），與 `NisbetPeekDrawer` 採互斥顯示。
    * 疊層寬度採響應式滿版設計（左側保留 `20.dp` 視覺縫隙與圓角邊框以維持疊層層次感並保持左側列表可見），隨視窗縮放自動延展。
* **四態疊層、可編修 Prompt 與遊戲內預覽**：
    * **動態 Context 晶片列 (`DeskContextChip`)**：採用 `.clip(shape)` 嚴格裁切圓角之自訂晶片（避免桌面端 Material `FilterChip` 預設觸控熱區在滑鼠懸停時出現方形灰底邊界），於記憶體內即時切換「同組詞條」與「近期譯文」開關並立即在本機重算 Prompt 與命中數量（0 API 成本），支援展開命中的「術語表」。
    * **Ready 預覽態**：輸入框自適應撐滿剩餘垂直空間，直接顯示可編輯之 Prompt 預覽區（含字元數統計與一鍵重設），讓譯者在發送前確認並微調指令，按下「發送至 Gemini」才正式發出請求。
    * **Loading 態**：顯示 `NisbetThinking` 插圖、進度條、每 100ms 更新之經過秒數與「取消」按鈕。
    * **Success 態**：顯示 Brigette 分析與結語，並將 2–3 組評分候選譯文渲染為可點擊選項卡；每張卡片即時執行 `TagSensor.diagnose()` 與 `GlossarySensor.diagnose()` 呈現標籤、術語與換行一致徽章，並提供行內 **`NisbetPeekCard` 遊戲預覽切換**。亦支援展開並微調 Prompt 後再次重送。
    * **Error 態**：區分缺 Key／未選模型／驗證失敗（引導開啟設定）與 HTTP 429 配額類型（`PerMinute` 等待秒數 vs `PerDay` 每日上限）。
* **一鍵套用與累計統計**：
    * 點擊「套用所選譯文」比照智慧貼上同步備份 `backupText`（相容既有復原按鈕）、寫入編輯器並自動存檔；疊層頁尾左下角即時顯示 Session 累計請求數／Token（含懸停明細），結果態並同時呈現單次耗時與 Token 明細，[`OniTranslatorBottomBar`](src/main/kotlin/dolphin/desktop/apps/onitranslator/app/OniTranslatorBottomBar.kt) 亦同步呈現本次 Session 累計統計。

---

## 5. 目錄結構 (Directory Structure)

```text
src/main/kotlin/dolphin/desktop/apps/onitranslator/
├── app/          # 應用程式入口與全域 UI (Main, App, TopBar, BottomBar)
├── model/        # 核心邏輯與資料模型 (ViewModel, PoHelper, PoEntry, GlossaryBank, GlossarySensor, TagSensor, LinguisticRules, PromptComposer, GeminiClient)
├── ui/           # 主要 UI 畫面 (EntryBrowser, EntryEditor, ConfigScreen, NisbetPeek, BrigetteDesk, DiagnosticVisualTransformation)
├── theme/        # Material 3 主題設定
├── widget/       # 共用 UI 元件
└── generated/    # Compose Resources 自動生成的代碼
```

---

## 6. 未來擴充指引

*   **文件同步 (Critical)**：任何涉及架構調整、邏輯變更或新功能實作，**必須同步更新本文件**。
*   **自動備份建議**：若未來需實作版本回溯，可考慮在手動儲存草稿時產生 `.po.bak` 檔案。
