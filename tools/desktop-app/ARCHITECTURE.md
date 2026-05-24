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
    *   `hasDraft`: 目前硬碟上是否存在實體草稿檔。
    *   `uiState`: 包含下方的 UI 狀態。
*   **`UiState`**: 純 UI 相關的短暫狀態。
    *   `isLoading`: 是否正在處理中。
    *   `processStatus`: 即時狀態訊息（如：儲存中...）。
    *   `searchState`: 搜尋關鍵字、結果與是否啟用搜尋模式。
    *   `editorData`: 右側編輯器當前選中的資料。
    *   `dialogState`: 控制對話框 (Config, Save Confirm) 的顯示。

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
    *   **視覺標記**：
        *   `MODIFIED`: 內容與正式檔 `PoSave` 不同時顯示。
        *   `NEW`: 範本中新增的 Key。
        *   `Draft Chip`: 下方顯示草稿檔中的原始內容，供比對。

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

### 4.7. 草稿與進度管理 (Draft Management)

為了確保翻譯作業的安全性，專案實作了多層防護：
*   **閒置自動暫存**：
    *   可於設定中開啟，支援 1-30 分鐘閒置觸發。
    *   採用 Debounce 機制，每次編輯後重設計時器，僅在使用者停下工作時執行背景寫入。
*   **行為安全優化**：
    *   「刪除草稿」僅移除硬碟實體檔案，**保留記憶體中的編輯進度**，避免做白工。
    *   選單按鈕會根據硬碟上是否存在草稿檔自動切換 `Enabled` 狀態。

### 4.8. 品質保證 (Testing)

核心邏輯（如 `TagSensor`, `NisbetPeek` 解析層，以及 `PoHelper` 的多行解析與引號轉義機制）配備單元測試，確保複雜標籤語法與字串處理的穩健性。


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

*   **文件同步 (Critical)**：任何涉及架構調整、邏輯變更或新功能實作，**必須同步更新本文件**。
*   **自動備份建議**：若未來需實作版本回溯，可考慮在手動儲存草稿時產生 `.po.bak` 檔案。
