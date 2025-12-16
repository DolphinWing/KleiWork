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

- [x] **升級本地化方案**: 移除硬編碼 UI 字串，改用 `ResourceBundle` 和 Kotlin 包裝器 `AppStrings` 實現型別安全且外部可修改的 UI 字串本地化。將 UI 相關字串移至 `strings.properties`。保留 `resources/common/strings.xml` 作為 `DesktopPoHelper` 的 `replacement_list` 配置檔，並維持其外部可修改性。
- [ ] **增加程式碼註解**: 在複雜的邏輯函式 (如 `analyzeText`) 中補充註解，說明其設計目的與演算法思路，方便未來維護。

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

### M2 到 M3 遷移評估

| 檔案路徑                 | M2 元件/API                            | M3 對應項目                                                            | 複雜度   | 備註                                                    |
|:---------------------|:-------------------------------------|:-------------------------------------------------------------------|:------|:------------------------------------------------------|
| **多個檔案**             | `MaterialTheme`                      | `material3.MaterialTheme`                                          | **高** | 核心變更。需要替換主題並將 `colors` 改為 `colorScheme`。              |
| `AppTheme.kt`        | `lightColors()`                      | `material3.lightColorScheme()`                                     | **高** | 需要定義一套新的 M3 顏色方案。                                     |
| **多個檔案**             | `MaterialTheme.colors`               | `MaterialTheme.colorScheme`                                        | **高** | 所有用到 `colors.primary` 等地方都需要改為 `colorScheme.primary`。 |
| `Main.kt`            | `ScrollableTabRow`, `Tab`            | `material3.ScrollableTabRow`, `material3.Tab`                      | 中     | API 有微小變更，例如 `backgroundColor` -> `containerColor`。   |
| `Main.kt`            | `CircularProgressIndicator`          | `material3.CircularProgressIndicator`                              | 低     | 參數基本相同。                                               |
| `DebugSaveDialog.kt` | `AlertDialog`                        | `material3.AlertDialog`                                            | 中     | API 已變更。`buttons` 參數已被移除；按鈕現在需手動放置。                   |
| **多個檔案**             | `Button`, `TextButton`, `IconButton` | `material3.Button`, `material3.TextButton`, `material3.IconButton` | 低     | 大部分相容，但 `ButtonDefaults` 的用法已變更。                      |
| **多個檔案**             | `TextField`                          | `material3.TextField` 或 `material3.OutlinedTextField`              | 中     | `TextFieldDefaults` 的用法已變更；顏色和樣式參數已調整。                |
| **多個檔案**             | `Icon`, `Icons`                      | `material3.Icon`, `material3.Icons`                                | 低     | 大部分相容；通常只需更改匯入語句。                                     |
| `LazyUi.kt`          | `Surface`                            | `material3.Surface`                                                | 低     | 大部分相容；顏色參數可能需要從 `colorScheme` 獲取。                     |
| **多個檔案**             | `Text`                               | `material3.Text`                                                   | 低     | 大部分相容；顏色和樣式應從 M3 主題獲取。                                |
| `Experimental...`    | `ButtonColors`, `ButtonDefaults`     | M3 中有不同的處理方式                                                       | 中     | M3 按鈕的顏色和預設值是透過 `ButtonDefaults` 物件的方法來設定。            |

### 遷移步驟

- [ ] **1. 加入 M3 依賴**: 在 `build.gradle.kts` 中，加入 `compose.material3` 依賴。
- [ ] **2. 更新主題**: 在 `AppTheme.kt` 中，將 `androidx.compose.material.MaterialTheme` 替換為 `androidx.compose.material3.MaterialTheme`，並將 `lightColors()` 替換為 `lightColorScheme()`。定義新的 M3 顏色方案。
- [ ] **3. 遷移 `Main.kt`**: 作為第一步，將 `Main.kt` 中所有 M2 元件遷移到其 M3 對應項目。這包括 `OniTranslatorTheme`、`ScrollableTabRow`、`Tab`、`Text` 和 `CircularProgressIndicator`。
- [ ] **4. 遷移 `*Pane.kt` 和 `*Dialog.kt` 檔案**: 逐步遷移剩餘的 UI 檔案：
    - [ ] `ToolbarPane.kt` (`Icon`, `IconButton`)
    - [ ] `SearchPane.kt` (`Button`, `TextField`, `Icon` 等)
    - [ ] `FilePane.kt` (`TextField`, `Icon`)
    - [ ] `EditorPane.kt` (`Button`, `TextField`, `Icon`)
    - [ ] `ConfigPane.kt` (`TextButton`)
    - [ ] `DebugSaveDialog.kt` (`AlertDialog`, `TextButton`)
- [ ] **5. 最後清理**: 所有元件遷移並驗證後，搜尋並移除任何剩餘的 `androidx.compose.material.*` 用法。
- [ ] **6. (可選) 移除 M2 依賴**: 遷移完成並驗證後，可以從 `build.gradle.kts` 中移除 `compose.material` 依賴。

---

## Compose Multiplatform 資源遷移計畫

本節將詳細說明如何將專案的資源管理從舊有方式遷移到 Compose Multiplatform 統一資源系統，以解決 `painterResource` 棄用問題並支援字串資源。

### 遷移步驟

- [x] **A. 建立新的資源資料夾結構**：
    *   建立 `src/main/composeResources/drawable` (用於圖片)。
    *   建立 `src/main/composeResources/values` (用於預設的字串)。
    *   建立 `src/main/composeResources/values-zh-rTW` (用於繁體中文的字串)。
- [x] **B. 移動資源檔案**：
    *   將 `nisbet_ponder.png` 移到 `src/main/composeResources/drawable` 資料夾。
    *   將 `src/main/resources/strings.properties` 的內容轉換成 XML 格式，並放入 `src/main/composeResources/values/strings.xml`。
    *   將 `src/main/resources/strings_zh_TW.properties` 的內容也轉換成 XML 格式，並放入 `src/main/composeResources/values-zh-rTW/strings.xml`。
- [x] **C. 修改 Gradle 設定**：在 `build.gradle.kts` 中加入啟用 Compose Multiplatform 資源系統所需的設定和依賴。
- [x] **D. 修改程式碼**：
    *   更新 `Main.kt` 中 `painterResource` 的 `import` 和用法，改用 `org.jetbrains.compose.resources.painterResource`。
    *   移除 `dolphin.desktop.apps.dsttranslate.AppStrings` 這個自定義的字串包裝器檔案 (`AppStrings.kt`)。
    *   將所有用到 `AppStrings.xxx` 的地方，替換為新的 `Res.string.xxx`。
- [x] **E. 清理舊資源**：移除 `src/main/resources` 下舊的資源檔案（`nisbet_ponder.png`, `strings.properties`, `strings_zh_TW.properties`）以及 `AppStrings.kt` 檔案。

參考資料：[Multiplatform resources/Resources overview](https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources.html)
