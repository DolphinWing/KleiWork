# Desktop App 優化待辦事項

這份文件記錄了 `desktop-app` 專案可以優化的項目。完成後可以將對應項目勾選或刪除。

### 1. 專案建構與依賴管理 (Build & Dependencies)

- [ ] **版本號管理**: 將 `build.gradle.kts` 中的版本號硬式編碼改為與 Git 整合，使用 Git 標籤 (tag) 和 commit hash 來自動產生版本號，增加可追溯性。
- [x] **依賴版本：遷移至 Gradle Version Catalogs (libs.versions.toml)**：將目前分散在 `gradle.properties` 和 `build.gradle.kts` 中的依賴版本，統一遷移至 `libs.versions.toml`，以實現型別安全的集中管理。
- [x] **Gradle 效能**: 在 `gradle.properties` 中啟用 Configuration Cache (`org.gradle.configuration-cache=true`) 以加速建置流程。
- [ ] **強化安裝程式客製化**：根據需求，進一步配置 `nativeDistributions` 中的各平台打包選項 (例如 Windows 的 MSI 或 Linux 的 DEB)，以增加安裝流程的客製化程度。這將在資源遷移 (尤其是 `iconFile` 路徑更新) 完成後進行。

### 2. 程式碼結構與效能 (Code Structure & Performance)

- [x] **字串取代效能**: 優化 `PoHelper.kt` 中的 `refactor` 函式。將 `forEach` 迴圈取代方式改為使用單一合併的 Regex 或 Map 查表，以提升處理大量取代規則時的效能。
- [x] **移除對 DST 的支援**: 簡化程式碼，將所有與 DST 相關的邏輯和資源移除，專注於 ONI。

### 3. UI 與程式碼可讀性 (UI & Readability)

- [x] **升級本地化方案**: 參考資料：[Multiplatform resources/Resources overview](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources.html)
    - [x] **A. 建立新的資源資料夾結構**：
      * 建立 `src/main/composeResources/drawable` (用於圖片)。
      * 建立 `src/main/composeResources/values` (用於預設的字串)。
      * 建立 `src/main/composeResources/values-zh-rTW` (用於繁體中文的字串)。
    - [x] **B. 移動資源檔案**：
      * 將 `nisbet_ponder.png` 移到 `src/main/composeResources/drawable` 資料夾。
      * 將 `src/main/resources/strings.properties` 的內容轉換成 XML 格式，並放入 `src/main/composeResources/values/strings.xml`。
      * 將 `src/main/resources/strings_zh_TW.properties` 的內容也轉換成 XML 格式，並放入 `src/main/composeResources/values-zh-rTW/strings.xml`。
    - [x] **C. 修改 Gradle 設定**：在 `build.gradle.kts` 中加入啟用 Compose Multiplatform 資源系統所需的設定和依賴。
    - [x] **D. 修改程式碼**：
      * 更新 `Main.kt` 中 `painterResource` 的 `import` 和用法，改用 `org.jetbrains.compose.resources.painterResource`。
      * 移除 `dolphin.desktop.apps.dsttranslate.AppStrings` 這個自定義的字串包裝器檔案 (`AppStrings.kt`)。
      * 將所有用到 `AppStrings.xxx` 的地方，替換為新的 `Res.string.xxx`。
    - [x] **E. 清理舊資源**：移除 `src/main/resources` 下舊的資源檔案（`nisbet_ponder.png`, `strings.properties`, `strings_zh_TW.properties`）以及 `AppStrings.kt` 檔案。
- [x] **增加程式碼註解**: 在複雜的邏輯函式中補充註解，說明其設計目的與演算法思路，方便未來維護。
- [x] **UI 反饋：實作 Snackbar/Toast 通知機制**:
    - [x] 定義 `SnackbarMessage` 資料結構 (包含訊息、動作、持續時間、類型)。
    - [x] 建立 `SnackbarManager` 單例，提供觸發通知的方法。
    - [x] 實作 `SnackbarHost` Composable，用於在 UI 上顯示通知。
    - [x] 將 `SnackbarHost` 整合到 `OniTranslatorApp` 的根 Composable 中。
    - [x] 在關鍵操作（如檔案儲存、設定保存）後，透過 `SnackbarManager` 顯示相關通知。

### 4. 現代化與自動化 (Modernization & Automation)

- [ ] **程式碼品質：整合 Ktlint 自動化程式碼風格檢查**: 引入 `ktlint` Gradle 插件來自動格式化 Kotlin 程式碼並確保風格一致，提升程式碼品質與可讀性。
- [ ] **建構環境一致性：設定 Gradle Java Toolchain**: 在 `build.gradle.kts` 中設定 Java Toolchain，讓 Gradle 自動下載並使用指定的 JDK 版本，解決開發者間環境不一 (`JAVA_HOME`) 的問題，確保建構的穩定性與可預測性。
- [x] **依賴管理：整合 Gradle Versions Plugin**: 加入 `com.github.ben-manes.versions` 插件，並透過 `./gradlew dependencyUpdates` 指令定期檢查依賴庫的更新，以利專案維持在最新的狀態。
- [ ] **軟體品質：加入單元測試 (Unit Tests)**: 引入 `JUnit 5` 測試框架，為核心邏輯 (例如 `PoHelper.kt`, `Ini.kt`) 撰寫單元測試，以確保程式碼變更時的穩定性與正確性。
- [x] **自動化：更新 GitHub Actions CI 工作流程**: 優化並更新現有的 CI (Continuous Integration) 工作流程，包含升級 Actions 版本、整合 Gradle 快取，並確保在每次推送到 Git 倉庫時自動執行編譯 (`./gradlew build`) 和測試。
- [ ] **資料層與狀態管理重構**:
    - [x] 全面審視並重構 `PoData.kt`，使其 API 更符合新的 UI 架構和單向資料流 (UDF) 原則。
        - [x] **引入 `AppState` data class**：將 `PoDataModel` 中的 `MutableStateFlow` 都放到這個 data class 中，用一個 `StateFlow<AppState>` 來管理所有的狀態。
        - [x] **將 `loadIni` 和 `loadIniAndPo` 的回傳值改為 `Unit`**：將回傳值改為透過 `StateFlow` 來傳遞，讓 `PoDataModel` 的 API 更簡潔。
        - [x] **將 `save` 函式的回傳值改為 `Unit`**：將回傳值改為透過 UI 事件來處理，例如顯示一個 Snackbar 或 Toast。
        - [x] **將 `translate` 函式的回傳值改為 `Unit`**：將回傳值改為透過 UI 事件來處理。
        - [x] **將 `search` 和 `searchType` 合併**：將它們合併成一個 `updateSearch` 函式，並用一個 `SearchState` data class 來管理搜尋相關的狀態。
    - [x] **ViewModel 與資料層重構**:
        - [x] **更名**: 將 `PoDataModel.kt` 檔案與 `class` 更名為 `OniTranslatorViewModel.kt`，以明確其 ViewModel 職責。
        - [x] **關注點分離與依賴注入 (DI)**:
            - [x] **`ReplacementLoader.kt`**: 建立此類別，專責尋找及解析 `strings.xml` 以提供 `replacementMap`。
            - [x] **`TextConverter.kt`**: 將此從 `object` 改為 `class`，在其建構子中接收 `replacementMap`，專責文字轉換。
            - [x] **`PoHelper.kt`**: 重構此類別，使其建構子依賴 `Configs` 與 `TextConverter` 實例，專注於 PO 檔的核心業務邏輯。
            - [x] **`OniTranslatorViewModel.kt`**: 作為總指揮官，重構其內部邏輯以符合新的 DI 流程。
        - [x] **函式拆分與清理**:
            - [x] 在 `PoHelper.kt` 中，將 `runTranslationProcess` 拆分為數個小型私有函式。
            - [x] 刪除已無作用的 `DesktopPoHelper.kt` 檔案 (若還存在)。
    - [x] **將事件處理邏輯集中到 ViewModel (MVI 模式)**:
        - [x] 在 `OniTranslatorViewModel` 中建立 `onEvent(event: AppEvent)` 的單一入口函式。
        - [x] 將 `Main.kt` 中的 `handleAppEvent` 邏輯完整遷移至 `ViewModel`。
        - [x] 移除 `OniTranslatorApp.kt` 中的 `handleUiEvents` 函式，將其邏輯合併至 `ViewModel`。
        - [x] 修改 `Main.kt` 和 `OniTranslatorApp.kt`，使其直接呼叫 `viewModel.onEvent()`。

### 5. 日誌系統 (Logging System)

- [x] **實作進階日誌系統 (Advanced Logging System)**: 建立一個即時且可回溯的日誌機制，整合至 `PoHelper` 與 UI。
    - [x] **架構設計 (Architectural Design)**:
        - [x] 定義 `LogEntry` data class (message, timestamp, type: Info/Warning/Error)。
        - [x] 在 `PoHelper` 中引入 `_logs: MutableStateFlow<List<LogEntry>>` 用於保存歷史紀錄。
        - [x] 改造 `log()` 函式，使其同時更新 `_logs` (歷史列表) 與 `_status` (即時狀態)。
        - [x] 實作容量控制機制 (例如保留最近 100 條)，避免記憶體無限膨脹。
        - [x] **有序輸出**：將 `templateMap` 改為 `LinkedHashMap` 以確保輸出順序。
        - [x] **草稿機制**：實作優先讀取與寫入後清除 Draft 的邏輯。
    - [x] **ViewModel 整合 (ViewModel Integration)**:
        - [x] 將 `status` 狀態管理從 `PoHelper` 移至 `OniTranslatorViewModel`。
        - [x] ViewModel 訂閱 `PoHelper.logs`，並自動更新自身的 `status` (取最新一筆) 與 `logs`。
        - [x] ViewModel 提供 `updateStatus(message)` 介面供 UI 外部手動更新狀態。
    - [x] **UI 整合 (UI Integration)**:
        - [x] ViewModel 暴露 `logs: StateFlow<List<LogEntry>>` 給 UI。
        - [x] Status Bar 顯示 ViewModel 的 `status`。
        - [x] TopBar 展開 Dropdown Menu/Popup，以 LazyColumn 顯示完整 Log 歷史列表。

---

## Material 2 到 Material 3 遷移計畫

本節概述了將專案 UI 從 Jetpack Compose Material 2 (M2) 遷移到 Material 3 (M3) 的計畫和步驟。

**目標**：使應用程式的介面外觀現代化、解決棄用警告，並符合 Google 最新的設計標準。

### M2 到 M3 遷移步驟

**開發流程共識**：我們將採用兩階段遷移策略。
1.  **第一階段 (UI 優先)**：專注於使用 M3 元件完成所有 UI 的靜態佈局和刻畫，確保介面達到理想的現代化設計。在此階段，我們會建立新的 M3 Composable，但暫時不與 `PoDataModel` 的複雜邏輯深度耦合。
2.  **第二階段 (邏輯與狀態重構)**：在 UI 佈局完全確定後，回頭重構 `PoDataModel` 和 `StateFlow`，使其完美適配新的 UI 架構，並移除舊的 M2 UI 程式碼。

- [x] **1. 加入 M3 依賴**: 在 `build.gradle.kts` 中，加入 `compose.material3` 依賴。
- [x] **2. 更新主題**: 在 `AppTheme.kt` 中，將 `androidx.compose.material.MaterialTheme` 替換為 `androidx.compose.material3.MaterialTheme`，並將 `lightColors()` 替換為 `lightColorScheme()`。定義新的 M3 顏色方案。
- [x] **3. 建立新的應用程式根元件**: 將 `Main.kt` 中的 `App` Composable 抽離至 `dolphin.desktop.apps.onitranslator` 套件下的 `OniTranslatorApp.kt`，並讓 `Main.kt` 中的 `main()` 函式呼叫新的 `OniTranslatorApp`。
- [x] **4. 遷移 `Main.kt` (舊 Tab)**: 參考 `Main.kt` 中功能，重新使用其 M3 對應項目取代舊有的所有 M2 元件，並調整結構。
- [x] **5. 頂層架構重構：引入 Scaffold 與主從式佈局**: 這是實現現代化 UI 的核心步驟。
    - [x] 在 `OniTranslatorApp.kt` 中使用 `Scaffold` 元件作為根佈局。
    - [x] **TopAppBar**:
        - [x] 在 `Scaffold` 的 `topBar` 中實作一個全域的 `TopAppBar`。
        - [x] `TopAppBar` 需要能夠在「一般標題模式」和「搜尋模式 (`SearchBar`)」之間切換。
        - [x] 在「搜尋模式」下，`SearchBar` 下方應包含一組 `FilterChip`，用於切換搜尋類型 (Key, Origin, Text)。
    - [x] **主從式佈局 (Master-Detail)**:
        - [x] 在 `Scaffold` 的 `content` 區域，使用 `Row` 實現主從式佈局。
        - [x] 左側 (Master) 為 `M3EntryListPane`。
        - [x] 右側 (Detail) 為 `M3EditorPane`。
    - [x] **StatusBar**:
        - [x] (可選) 在 `Scaffold` 的 `bottomBar` 中實作一個全域的 `StatusBar`，用於顯示 App 狀態或編輯提示。
    - [x] **狀態管理**:
        - [x] 在 `OniTranslatorApp` 的頂層管理 `selectedEntry: State<WordEntry?>` 和 `searchText: State<String>` 等 UI 狀態。
- [x] **6. 各 `Pane` 的 M3 適配與遷移**: 根據新的頂層架構，遷移或建立各個 `Pane`。
    - [x] `EntryListPane.kt`:
        - [x] 建立 `M3EntryListPane`。
        - [x] 功能擴充：讓其能根據傳入的 `searchText` 決定是顯示完整列表還是搜尋結果。
        - [x] 功能調整：`onEdit` 回調僅更新頂層的 `selectedEntry` 狀態。
        - [ ] (可選) 為當前選中的列表項增加視覺高亮效果。
    - [x] `ConfigPane.kt`: (已重新檢視並整合)
        - [x] 建立 `M3ConfigPane`。
        - [x] 移除其內部的 `Surface`，使其能融入父容器背景。
        - [x] 建立私有的 `M3FileChooser` 元件。
        - [x] 完成「快速設定」和「手動設定」的佈局。
        - [x] **整合 Config 頁面至主要 UI**：
            - [x] 在 `OniTranslatorApp.kt` 中，新增 `showConfigDialog` 狀態來控制 Config 頁面的顯示。
            - [x] 在 `PoDataModel` 的 `loadIniAndPo()` 完成後，檢查設定是否有效（使用 `DesktopPoHelper.isConfigValid()`）。若無效，自動顯示 Config 頁面。
            - [x] 將 `M3ConfigPane` 放在 `Dialog` 中顯示，由 `showConfigDialog` 控制。
            - [x] `M3ConfigPane` 增加了 `onConfigSaved` 和 `onDismissRequest` 回呼，以處理儲存和關閉邏輯。
            - [x] 在 `OniTranslatorTopBar` 的溢出選單中，新增「設定」進入點，點擊後會顯示 Config 頁面。
    - [x] `EditorPane.kt`:
        - [x] 建立 `M3EditorPane` 檔案。
        - [x] `M3EditorPane` **不包含**自身的 `Toolbar`。
        - [x] 介面根據傳入的 `selectedEntry` 顯示，若為 `null` 則顯示提示。
        - [x] 使用 M3 元件 (`OutlinedTextField`, `TextButton`, `Icon` 等) 設計編輯器 UI。
    - [x] `SearchPane.kt`:
        - [x] **計畫變更**：**不再**建立獨立的 `M3SearchPane` 檔案。其功能被整合到 `TopAppBar` 的 `SearchBar` 和 `M3EntryListPane` 的搜尋結果顯示模式中。
    - [x] `DebugSaveDialog.kt` (`AlertDialog`, `TextButton`)
        - [x] 已經在 `OniTranslatorApp.kt` 中整合了 `M3DebugSaveDialog`。
- [x] **8. (可選) 最後清理**: 所有遷移和重構完成後，搜尋並移除任何剩餘的 M2 依賴和舊 UI 檔案。
- [x] **9. (可選) 移除 M2 依賴**: 遷移完成並驗證後，可以從 `build.gradle.kts` 中移除 `compose.material` 依賴。
