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
- **Tag Sensor (標籤感測器)**：自動對比原文與譯文的標籤數量，確保 `link`, `style`, `color` 等關鍵標籤對稱。
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
    *   `filteredList`: 當前顯示的編輯列表 (`EditorData`)。
    *   `changedList`: 本次工作階段 (Session) 實際修改過的 Key 清單 (`List<String>`)。
    *   `hasDraft`: 目前硬碟上是否存在實體草稿檔。
    *   `uiState`: 包含下方的 UI 狀態。
*   **`UiState`**: 純 UI 相關的短暫狀態。
    *   `isLoading`: 是否正在處理中。
    *   `processStatus`: 即時狀態訊息（如：儲存中...）。
    *   `searchState`: 搜尋關鍵字、結果與是否啟用搜尋模式。
    *   `editorData`: 右側編輯器當前選中的資料。
    *   `dialogState`: 控制對話框 (Config, Save Confirm, Log Window, Jump Dialog, Exit Confirm Dialog) 的顯示。

### 4.2. 翻譯與檔案處理 (`PoHelper`)

這是本專案最核心的「大腦」。它維護了四個主要來源 (Maps)：
1.  **`Source`** (`templateMap`): 來自 `strings_template.pot`，決定了輸出的順序與 Key。
2.  **`ChsRef`** (`simplifiedMap`): 來自原廠簡中檔案，作為翻譯參考。
3.  **`PoSave`** (`translatedEntries`): 來自遊戲目錄下的 `strings.po`（已儲存的譯文）。
4.  **`Draft`** (`draftEntries`): 來自系統暫存目錄下的 `.po` 暫存檔。

**合併邏輯**：Draft > PoSave > ChsRef (converted) > Source (English)。

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

### 4.4. 列表顯示與搜尋邏輯 (List & Search Logic)

1.  **待辦模式 (Default / Diff Mode)**
    *   **觸發時機**：搜尋模式未啟用時。
    *   **頂端狀態欄 (Toolbar Status)**：顯示「全部: %1$d, 已變更: %2$d」，分別對應待辦項目總數 (`filteredList.size`) 與本次工作階段實際已翻譯項目數 (`changedList.size`)。
    *   **視覺標記**：
        *   `MODIFIED`: 內容與正式檔 `PoSave` 不同時顯示。
        *   `NEW`: 範本中新增的 Key。
        *   `Draft Chip`: 下方顯示草稿檔中的原始內容，供比對。
        *   **卡片修改狀態**：由各項目實體之 `target.changed > 0` 獨立驅動卡片高亮邊框，解耦平行陣列索引依賴。

2.  **全覽模式 (Search / Library Mode)**
    *   **觸發時機**：啟用搜尋模式 (`isActive = true`)。
    *   **行為**：自動隱藏 `MODIFIED`/`NEW` 標籤，提供純淨的字典查詢體驗。

3.  **重新整理 (Refresh)**：
    *   觸發 `RefreshSource` 時會清空編輯器狀態，確保 UI 一致性。

### 4.5. 開發與除錯 (Debug Mode)
*   **Debug Mode**：透過 `OniTranslatorViewModel(debugMode = true)` 啟動。
*   **行為差異**：存檔路徑會導向系統暫存目錄，並彈出 `DebugSaveDialog` 確認路徑。

### 4.6. UI 互動與視覺安全感

*   **視覺對位系統**：
    *   編輯器下方列出 `Source`, `ChsRef`, `PoSave` 三重參考，並可手動切換顯示。
    *   列表項目採用 Tertiary 色系標記「已修改」狀態，與 `PoSave` 標籤產生視覺連動。

*   **AI 協作與智慧貼上**：
    *   支援從剪貼簿提取 `msgstr` 並自動解析轉義字元，實現一鍵同步。

*   **NisbetPeek 預覽系統**：
    *   模擬遊戲內渲染效果（顏色、連結、插值）。
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

核心邏輯（如 `TagSensor`, `NisbetPeek` 解析層，以及 `PoHelper` 的多行解析與引號轉義機制）配備單元測試，確保複雜標籤語法與字串處理的穩健性。

### 4.9. 排版精煉與引號轉換 (Quote Refactoring)

為了使翻譯後的字串符合中文直角引號排版規範，`TextRefinery` 整合了防禦性引號轉換機制：
* **對稱引號精煉**：在譯文進行 Refinery 的最後階段，會自動將成對的英文引號（同時支援轉義的 `\"` 與一般的 `"`）交替替換為 `「` 與 `」`。
* **防禦性偶數判定**：僅在非 Tag 的純文字（Plain Text）區段偵測到**偶數個且大於 0** 的引號總數時，才觸發替換。若引號數為奇數（代表譯文配對殘缺），則保持原樣不做更動，避免破壞排版。
* **標籤安全防護 (Token-free 狀態機)**：為避免毀壞 `<link=\"...\">` 等標籤內的屬性引號，內部採用了極輕量的括號狀態機（`inTag` 開關）。當指針處於 `<` 與 `>` 區間時，自動忽略任何引號計算與替換。同時此設計完全獨立於 `OniToken` 之外，避免了重組時字面 `\\n` 被誤還原成實際換行的副作用。
* **限定套用範圍 (Git Diff & 歷史保護)**：為保護歷史特意留存的引號並避免產生大量 Git Diff，自動引號精煉**僅限於新項目**（即 Newly 新增項目、MsgidChanged 英文變更項目、與 Fallback 遞補項目）。當載入已翻譯的歷史項目時，系統會維持其原貌。此外，當使用者點選「套用簡中參考」手動轉換時，亦會套用此精煉機制。

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
* **防禦性決策閉環 (Review & Keep)**：
    * 捨棄高風險的全域批次強制取代，採用條目級審核。
    * **一鍵套用規範詞 (Safe Apply)**：五層替換階梯（Tag 內置換 $\rightarrow$ 英文置換 $\rightarrow$ 簡中對照 $\rightarrow$ 異體字轉換 $\rightarrow$ $\ge 50\%$ 重疊度模糊邊界擴展）。若無法安全定位，絕不盲目覆蓋破壞原文，改走系統剪貼簿複製與通知提醒。
    * **認可新詞（審閱保留）**：保留當前譯文不更動，條目被標記為已認可，隨時可復原。
* **伴隨檔跨階段持久化 (Companion Sidecar)**：
    * 伴隨草稿在 `%TEMP%\strings.po.terms` 記錄已認可新詞之 Key 清單。
    * 跨天重啟時無縫還原決策狀態；正式存檔或清除草稿時一併同步清理。
* **三層視覺防禦與過濾**：
    * **編輯器**：即時動態提示橫幅（⚠️ 未審閱衝突 vs ℹ️ 已認可新詞）與一鍵處置按鈕。
    * **卡片清單**：右上角 Chip 標示 `⚠️ 術語衝突` 或 `ℹ️ 採用新詞`。
    * **全域檢索**：`SearchType.Glossary` 一鍵篩選所有術語差異項目，供日後覆核調閱。

---

## 5. 目錄結構 (Directory Structure)

```text
src/main/kotlin/dolphin/desktop/apps/onitranslator/
├── app/          # 應用程式入口與全域 UI (Main, App, TopBar, BottomBar)
├── model/        # 核心邏輯與資料模型 (ViewModel, PoHelper, PoEntry, GlossaryBank, GlossarySensor)
├── ui/           # 主要 UI 畫面 (EntryBrowser, EntryEditor, ConfigScreen)
├── theme/        # Material 3 主題設定
├── widget/       # 共用 UI 元件
└── generated/    # Compose Resources 自動生成的代碼
```

---

## 6. 未來擴充指引

*   **文件同步 (Critical)**：任何涉及架構調整、邏輯變更或新功能實作，**必須同步更新本文件**。
*   **自動備份建議**：若未來需實作版本回溯，可考慮在手動儲存草稿時產生 `.po.bak` 檔案。
