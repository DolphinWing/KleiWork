# ONI Translator Architecture Guide

這份文件旨在協助人類開發者（以及未來的 AI 助手）快速理解 **ONI Translator** 專案的技術架構、設計模式與程式碼組織。

---

## 1. 專案概述 (Overview)

**ONI Translator** 是一個跨平台（目前專注於 Windows）的桌面應用程式，專門用於協助翻譯遊戲《缺氧 (Oxygen Not Included)》的語言檔案 (`.po`)。

它不只是一個純文字編輯器，還整合了：
- **簡繁轉換**：自動將簡體中文來源轉換為繁體中文。
- **詞彙替換**：透過 `strings.xml` 定義專有詞彙的自動修正規則。
- **草稿機制**：支援未完成的編輯作業自動存檔。
- **差異比對**：能識別原廠檔案的變更 (msgid change)。

---

## 2. 技術堆疊 (Tech Stack)

*   **語言**: Kotlin (JVM)
*   **UI 框架**: Compose Multiplatform (Compose Desktop)
*   **UI 設計系統**: Material Design 3
*   **建構工具**: Gradle (Kotlin DSL), Version Catalogs (`libs.versions.toml`)
*   **關鍵函式庫**:
    *   `opencc4j`: 簡繁轉換核心。
    *   `kotlinx.coroutines`: 非同步任務處理。

---

## 3. 架構模式 (Architectural Pattern)

本專案採用 **Model-View-Intent (MVI)** 架構，確保資料流向單一且狀態可預測。

### 核心元件

1.  **View (UI)**
    *   負責渲染介面與捕捉使用者操作。
    *   **不包含業務邏輯**。
    *   透過 `onEvent: (AppEvent) -> Unit` 將操作意圖傳遞給 ViewModel。
    *   訂閱 ViewModel 的 `StateFlow` (`AppState`, `UiState`) 來更新畫面。
    *   *進入點*: `OniTranslatorApp.kt`

2.  **ViewModel (`OniTranslatorViewModel`)**
    *   **Single Source of Truth**：持有並管理唯一的 `AppState`。
    *   **Intent Handler**：透過 `onEvent(event: AppEvent)` 接收並處理所有意圖。
    *   **Coordinator**：協調 `PoHelper` (資料處理)、`ConfigManager` (設定) 與 UI 狀態的同步。

3.  **Model (Data Layer)**
    *   負責實際的資料讀寫與運算。
    *   **`PoHelper`**: 核心邏輯所在。負責讀取 `.po` 檔、合併草稿、執行簡繁轉換、寫入檔案。
    *   **`ConfigManager`**: 負責 `configs.ini` 的讀寫與視窗狀態保存。
    *   **`TextConverter`**: 封裝 `opencc4j` 與 `ReplacementLoader` 的轉換邏輯。

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
    *   `searchState`: 搜尋關鍵字、結果與是否啟用搜尋模式。
    *   `editorData`: 右側編輯器當前選中的資料。
    *   `dialogState`: 控制對話框 (Config, Save Confirm) 的顯示。

### 4.2. 翻譯與檔案處理 (`PoHelper`)

這是本專案最核心的「大腦」。它維護了四個主要的 Map：
1.  `templateMap` (`LinkedHashMap`): 來自 `strings_template.pot`，決定了輸出的順序與 Key。
2.  `simplifiedMap`: 來自原廠簡中檔案，作為翻譯參考。
3.  `translatedEntries`: 來自使用者目前的 `strings.po`。
4.  `draftEntries`: 來自暫存檔 (Draft)。

**合併邏輯**：Draft > Existing Translation > Simplified (converted) > Template Origin。

### 4.3. 日誌系統 (Logging System)

為了讓使用者知道程式在背後做了什麼（例如「自動修正了某個詞」或「存檔成功」），我們設計了一套即時日誌系統：

*   `PoHelper` 內部產生 `LogEntry` (Info/Warning/Error)。
*   `ViewModel` 監聽 `PoHelper.logs`，並將最新的一條 Log 轉發到 `UiState.processStatus` 顯示於狀態列。
*   完整 Log 列表保留在 `AppState.logs` 供查閱。

---

## 5. 目錄結構 (Directory Structure)

```text
src/main/kotlin/dolphin/desktop/apps/onitranslator/
├── app/          # 應用程式入口與全域 UI (Main, App, TopBar, BottomBar)
├── model/        # 核心邏輯與資料模型 (ViewModel, PoHelper, Data Classes)
├── pane/         # 主要 UI 區塊 (EntryList, Editor, Config)
├── theme/        # Material 3 主題設定
├── widget/       # 共用 UI 元件
└── generated/    # Compose Resources 自動生成的代碼
```

---

## 6. 未來擴充指引

*   若要新增 UI 功能：請先在 `AppEvent` 定義意圖，再到 `ViewModel` 實作邏輯，最後更新 `UiState`。
*   若要修改翻譯邏輯：請專注於 `PoHelper`，避免將邏輯洩漏到 ViewModel。
*   若要調整樣式：請優先使用 `MaterialTheme` 的 tokens，保持設計一致性。
