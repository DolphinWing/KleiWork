# ONI Translator Architecture Guide

這份文件旨在協助人類開發者（以及未來的 AI 助手）快速理解 **ONI Translator** 專案的技術架構、設計模式與程式碼組織。

---

## 1. 專案概述 (Overview)

**ONI Translator** 是一個跨平台（目前專注於 Windows）的桌面應用程式，專門用於協助翻譯遊戲《缺氧 (Oxygen Not Included)》的語言檔案 (`.po`)。

它不只是一個純文字編輯器，還整合了：
- **簡繁轉換**：自動將簡體中文來源轉換為繁體中文。
- **詞彙替換**：透過 `replacement_strings.xml` 定義專有詞彙的自動修正規則。
- **草稿機制**：支援未完成的編輯作業自動存檔。
- **差異比對**：能識別原廠檔案的變更 (msgid change)，確保翻譯能追蹤英文原文的更新。
- **Tag Sensor (標籤感測器)**：自動對比原文與譯文的標籤數量，確保 `link`, `style`, `color` 等關鍵標籤對稱。
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
    *   `filteredList`: 當前顯示的編輯列表。
    *   `uiState`: 包含下方的 UI 狀態。
*   **`UiState`**: 純 UI 相關的短暫狀態。
    *   `isLoading`: 是否正在處理中。
    *   `processStatus`: 即時狀態訊息（如：儲存中...）。
    *   `searchState`: 搜尋關鍵字、結果與是否啟用搜尋模式。
    *   `editorData`: 右側編輯器當前選中的資料。
    *   `dialogState`: 控制對話框 (Config, Save Confirm) 的顯示。
    *   **效能優化註記**：針對單純的視覺變更（如切換主題），ViewModel 採用 `updateThemeConfig` 僅寫入設定檔而不觸發重讀；僅在涉及檔案路徑變更 (`saveConfig`) 時才會執行全域資料重載。

### 4.2. 翻譯與檔案處理 (`PoHelper`)

這是本專案最核心的「大腦」。它維護了四個主要的 Map：
1.  `templateMap` (`LinkedHashMap`): 來自 `strings_template.pot`，決定了輸出的順序與 Key。
2.  `simplifiedMap`: 來自原廠簡中檔案，作為翻譯參考。
3.  `translatedEntries`: 來自使用者目前的 `strings.po`。
4.  `draftEntries`: 來自暫存檔 (Draft)。

**合併邏輯**：Draft > Existing Translation > Simplified (converted) > Template Origin。

**差異判定 (Diff Logic)**：
為了精確篩選出「待辦項目」，`PoHelper` 採用以下判定公式：
*   **Newly**: Key 存在於 Template 但完全不存在於 `strings.po`。
*   **MsgidChanged**: Key 在兩邊皆存在，但英文原文 (`msgid`) 已變動。
*   **DraftChanged**: 暫存檔內容與正式翻譯檔內容不一致。

### 4.3. 資源與日誌系統

#### 多語系與檔案資源管理 (Resources)
專案全面採用 Compose Multiplatform 資源系統：
*   **字串資源**：定義於 `src/main/composeResources/values/strings.xml`，透過 `Res.string.xxx` 存取。
*   **檔案資源**：定義於 `src/main/composeResources/files/`，包含預設設定 (`configs.ini`) 與替換規則 (`replacement_strings.xml`)，透過 `Res.readBytes("files/xxx")` 讀取。這確保了跨平台打包時檔案的正確性。

#### 即時回饋機制
為了讓使用者知道程式在背後做了什麼：
*   `PoHelper` 內部產生 `LogEntry` (Info/Warning/Error)。
*   `ViewModel` 監聽 `PoHelper.logs`，並將最新的一條訊息轉發到 `UiState.processStatus`。
*   **`OniTranslatorBottomBar`** 負責呈現此狀態，並具備 Fallback 邏輯：
    1.  優先顯示 `processStatus`。
    2.  若為空，顯示 `isLoading` 狀態。
    3.  閒置時顯示當前項目總數。
*   完整 Log 列表保留在 `AppState.logs` 供查閱。

### 4.4. 列表顯示與搜尋邏輯 (List & Search Logic)

應用程式採用兩種模式來呈現資料，以平衡「工作效率」與「瀏覽需求」：

1.  **待辦模式 (Default / Diff Mode)**
    *   **觸發時機**：搜尋模式未啟用時（預設狀態）。
    *   **顯示內容**：僅顯示符合「差異判定」公式的項目：`(Newly || MsgidChanged || DraftChanged || SessionModified) && OriginalNotEmpty`。
    *   **目的**：讓使用者專注於需要處理的差異，排除掉 2 萬多條已完成或無須翻譯的背景資料。

2.  **全覽模式 (Search / Library Mode)**
    *   **觸發時機**：啟用搜尋模式 (`AppEvent.Search.ActiveChange(true)`)。
    *   **顯示內容**：
        *   **剛進入時**：自動觸發空字串搜尋，載入**所有**資料項目。
        *   **輸入文字時**：根據關鍵字在所有資料中進行篩選 (`searchState.results`)。
    *   **目的**：提供完整的字典查詢功能，允許使用者檢視未變更的原廠翻譯。

3.  **空狀態與刷新 (Empty State & Refresh)**
    *   **空狀態**：當待辦列表為空時，顯示「一切就緒」的視覺引導，避免畫面過於單調。
    *   **刷新機制**：支援透過空狀態按鈕或頂部選單 (`MoreActionsMenu`) 觸發 `AppEvent.File.RefreshSource`，強制重讀檔案以同步外部變更。

### 4.5. 開發與除錯 (Debug Mode)
*   **Debug Mode**：透過 `OniTranslatorViewModel(debugMode = true)` 啟動。
*   **行為差異**：
    *   **存檔路徑**：檔案會寫入系統暫存目錄 (Temp)，而非遊戲真實目錄，避免開發時汙染環境。
    *   **除錯對話框**：存檔時會跳出 `DebugSaveDialog` 顯示寫入路徑。

### 4.6. UI 互動細節 (UI Interactions)

*   **Tooltip 系統**：
    *   為了提升辨識度，全域採用「反轉色 (Inverse Surface)」與較大的字體 (`bodyMedium`)。
    *   **邊界優化**：在靠近 Status Bar 的編輯器開關處，Tooltip 會自動調整至上方 (`Above`) 顯示，避免視覺遮擋。

*   **AI 協作工作流 (AI-Assisted Workflow)**：
    *   **智慧貼上 (Smart Paste)**：專為 Gemini Gems 網頁版設計。點擊魔法按鈕後，程式會自動從剪貼簿提取 `msgstr` 內容，解析轉義字元，並自動執行 `Save` 事件以達到「一鍵同步」。
    *   **反悔機制 (Smart Undo)**：在智慧貼上後，會自動保存貼上前的手動內容。點擊「復原」按鈕可一鍵將文字框與檔案狀態同時還原。
    *   **技術實作**：採用現代化的 `LocalClipboard` API 配合 Java AWT `Transferable` 轉型，實現非同步且穩定的剪貼簿存取。

*   **NisbetPeek 視覺預覽系統**：
    *   **架構設計 (Logic/UI Separation)**：系統分為純邏輯解析層 (`toOniTokens`) 與 UI 渲染層 (`peek`)。解析層將原始碼拆解為 `OniToken` 列表，完全不依賴 UI 框架，便於執行高效率的單元測試。
    *   **標籤守護 (Tag Safety)**：渲染引擎內建 `tagStack` 機制，能精確處理巢狀標籤並修正未閉合或錯位的語法（如 Klei 原廠文件中的 Bug）。
    *   **智慧感應與偵錯 (Smart Sensor & Debug)**：系統除了自動偵測預覽時機外，還能預檢語法錯誤 (`hasOniSyntaxError`)。未知標籤會以「紅色底線」高亮，多餘或錯誤標籤則會以「淡紅色」顯示。
    *   **情緒化回饋 (Emotional Interaction)**：整合 `NisbetEmotion` 選擇器。當偵測到語法錯誤時，Nisbet 會自動切換至「抱歉 (NisbetSorry)」表情，並給予針對性的糾錯建議語錄；正常時則隨機展現期待、興奮或沉思等動態情緒。
    *   **渲染規格**：定義了獨立的「ONI 調色盤 (OniColor)」，模擬遊戲中連結 (Pink)、警告 (Red)、關鍵字 (Orange) 與插值 (Blue) 的真實視覺感。

    *   **Tag Sensor 診斷系統 (Tag Sensor Diagnostic)**：
        *   **自動對帳**：內建於 `PoHelper` 的處理流程中。自動提取並比對 `msgid` 與 `msgstr` 的標籤 (`OpeningTag`) 與動態變數 (`{Hotkey}`) 數量。
        *   **智慧偵測**：解析器會自動識別並忽略被 `< >` 包裹的 Email 地址，減少在 Lore 條目中的誤報。
        *   **雙重判定**：區分「原文錯誤 (Source Issue)」與「譯文偏差 (Mismatch)」，讓使用者能快速識別是 Klei 的鍋還是自己的筆誤。
        *   **即時回饋**：編輯器支援 Live Diagnostic，在 `NisbetPeek` 抽屜中即時顯示字級較大、清晰易讀的差異報告。
        *   **搜尋整合**：在「全覽模式」下可透過「標籤診斷」Chip 快速過濾出所有有問題的條目。

    *   **NisbetPeek 渲染增強**：
        *   **Small Caps 支援**：實作 `smallcaps` 標籤的 OpenType 渲染 (`smcp`)。
        *   **強制預覽**：當診斷系統偵測到標籤問題時，即便字串簡短，系統也會強制允許開啟預覽，確保使用者能看見診斷報告。

### 4.7. 草稿與進度管理 (Draft Management)

為了確保翻譯作業的靈活性與安全性：
*   **自動暫存**：所有編輯變更會即時寫入系統暫存目錄的 `strings.po` 檔案。
*   **狀態列同步**：左下角狀態列會根據當前模式（待辦或搜尋）自動切換顯示數量，提供精確的進度回饋。

### 4.8. 品質保證 (Testing)

核心邏輯（如 `TagSensor`）配備了完整的單元測試套件 (`TagSensorTest.kt`)，涵蓋標籤計數、變數比對、Email 偵測與各種邊界情況，確保解析邏輯的穩健性。


---

## 5. 目錄結構 (Directory Structure)

```text
src/main/kotlin/dolphin/desktop/apps/onitranslator/
├── app/          # 應用程式入口與全域 UI (Main, App, TopBar, BottomBar)
├── model/        # 核心邏輯與資料模型 (ViewModel, PoHelper, PoEntry)
├── ui/           # 主要 UI 畫面 (EntryBrowser, EntryEditor, ConfigScreen)
├── theme/        # Material 3 主題設定
├── widget/       # 共用 UI 元件
└── generated/    # Compose Resources 自動生成的代碼
```

---

## 6. 未來擴充指引

*   **文件同步 (Critical for AI & Human)**：
    *   任何涉及架構調整、邏輯變更或新功能實作，**必須同步更新本文件**。
    *   **給 AI 的指令**：在完成上述類型的任務後，**請務必檢查並提醒使用者**更新此文檔，或主動提出更新建議。
*   若要新增 UI 功能：請先在 `AppEvent` 定義意圖，再到 `ViewModel` 實作邏輯，最後更新 `UiState`。
*   若要修改翻譯邏輯：請專注於 `PoHelper`，避免將邏輯洩漏到 ViewModel。
*   若要調整樣式：請優先使用 `MaterialTheme` 的 tokens，保持設計一致性。
