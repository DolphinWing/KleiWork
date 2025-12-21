# Desktop App 優化待辦事項

這份文件記錄了 `desktop-app` 專案可以優化的項目。完成後可以將對應項目勾選或刪除。

### 1. 專案建構與依賴管理 (Build & Dependencies)

- [ ] **版本號管理**: 將 `build.gradle.kts` 中的版本號硬式編碼改為與 Git 整合，使用 Git 標籤 (tag) 和 commit hash 來自動產生版本號，增加可追溯性。
- [x] **依賴版本：遷移至 Gradle Version Catalogs (libs.versions.toml)**：將目前分散在 `gradle.properties` 和 `build.gradle.kts` 中的依賴版本，統一遷移至 `libs.versions.toml`，以實現型別安全的集中管理。
- [x] **Gradle 效能**: 在 `gradle.properties` 中啟用 Configuration Cache (`org.gradle.configuration-cache=true`) 以加速建置流程。
- [ ] **強化安裝程式客製化**：根據需求，進一步配置 `nativeDistributions` 中的各平台打包選項 (例如 Windows 的 MSI 或 Linux 的 DEB)，以增加安裝流程的客製化程度。這將在資源遷移 (尤其是 `iconFile` 路徑更新) 完成後進行。

### 2. 程式碼結構與效能 (Code Structure & Performance)

- [x] **字串取代效能**: 優化 `PoHelper.kt` 中的 `refactor` 函式。將 `forEach` 迴圈取代方式改為使用單一合併的 Regex 或 Map 查表，以提升處理大量取代規則時的效能。
- [ ] **檔案路徑處理**: 重構 `Ini.kt` 和 `DesktopPoHelper.kt` 中寫死的相對路徑。考慮改用 ClassLoader 的資源讀取機制，避免因目錄結構變更導致程式出錯。
- [ ] **錯誤處理**: 改善通用的 `catch (e: Exception)` 區塊。改為捕捉更具體的例外類型，並將重要的錯誤訊息顯示在 UI 上，提升使用者體驗。
- [ ] **移除 Magic Strings**: 將程式碼中硬式編碼的字串 (如 Regex、URL) 定義為 `const val` 常數，增加程式碼的可讀性與可維護性。
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
- [ ] **增加程式碼註解**: 在複雜的邏輯函式 (如 `analyzeText`) 中補充註解，說明其設計目的與演算法思路，方便未來維護。
- [ ] **UI 反饋：實作 Snackbar/Toast 通知機制**:
    - [ ] 定義 `SnackbarMessage` 資料結構 (包含訊息、動作、持續時間、類型)。
    - [ ] 建立 `SnackbarManager` 單例，提供觸發通知的方法。
    - [ ] 實作 `SnackbarHost` Composable，用於在 UI 上顯示通知。
    - [ ] 將 `SnackbarHost` 整合到 `OniTranslatorApp` 的根 Composable 中。
    - [ ] 在關鍵操作（如檔案儲存、設定保存）後，透過 `SnackbarManager` 顯示相關通知。

### 4. 現代化與自動化 (Modernization & Automation)

- [ ] **程式碼品質：整合 Ktlint 自動化程式碼風格檢查**: 引入 `ktlint` Gradle 插件來自動格式化 Kotlin 程式碼並確保風格一致，提升程式碼品質與可讀性。
- [ ] **建構環境一致性：設定 Gradle Java Toolchain**: 在 `build.gradle.kts` 中設定 Java Toolchain，讓 Gradle 自動下載並使用指定的 JDK 版本，解決開發者間環境不一 (`JAVA_HOME`) 的問題，確保建構的穩定性與可預測性。
- [x] **依賴管理：整合 Gradle Versions Plugin**: 加入 `com.github.ben-manes.versions` 插件，並透過 `./gradlew dependencyUpdates` 指令定期檢查依賴庫的更新，以利專案維持在最新的狀態。
- [ ] **軟體品質：加入單元測試 (Unit Tests)**: 引入 `JUnit 5` 測試框架，為核心邏輯 (例如 `PoHelper.kt`, `Ini.kt`) 撰寫單元測試，以確保程式碼變更時的穩定性與正確性。
- [x] **自動化：更新 GitHub Actions CI 工作流程**: 優化並更新現有的 CI (Continuous Integration) 工作流程，包含升級 Actions 版本、整合 Gradle 快取，並確保在每次推送到 Git 倉庫時自動執行編譯 (`./gradlew build`) 和測試。

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
- [ ] **7. (第二階段) 資料層與狀態管理重構**:
    - [ ] 全面審視並重構 `PoDataModel.kt`，使其 API 更符合新的 UI 架構和單向資料流 (UDF) 原則。
    - [ ] 優化 `Ini.kt` 和 `DesktopPoHelper.kt` 的資料解析與檔案處理邏輯。
    - [ ] **實作「儲存草稿」機制**: (這是第二階段的內容，目前尚未開始)
- [ ] **8. (可選) 最後清理**: 所有遷移和重構完成後，搜尋並移除任何剩餘的 M2 依賴和舊 UI 檔案。
- [ ] **9. (可選) 移除 M2 依賴**: 遷移完成並驗證後，可以從 `build.gradle.kts` 中移除 `compose.material` 依賴。
