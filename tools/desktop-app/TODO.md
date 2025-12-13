# Desktop App 優化待辦事項

這份文件記錄了 `desktop-app` 專案可以優化的項目。完成後可以將對應項目勾選或刪除。

### 1. 專案建構與依賴管理 (Build & Dependencies)

- [ ] **版本號管理**: 將 `build.gradle.kts` 中的版本號硬式編碼改為與 Git 整合，使用 Git 標籤 (tag) 和 commit hash 來自動產生版本號，增加可追溯性。
- [x] **依賴版本：遷移至 Gradle Version Catalogs (libs.versions.toml)**：將目前分散在 `gradle.properties` 和 `build.gradle.kts` 中的依賴版本，統一遷移至 `libs.versions.toml`，以實現型別安全的集中管理。
- [x] **Gradle 效能**: 在 `gradle.properties` 中啟用 Configuration Cache (`org.gradle.configuration-cache=true`) 以加速建置流程。

### 2. 程式碼結構與效能 (Code Structure & Performance)

- [x] **字串取代效能**: 優化 `PoHelper.kt` 中的 `refactor` 函式。將 `forEach` 迴圈取代方式改為使用單一合併的 Regex 或 Map 查表，以提升處理大量取代規則時的效能。
- [ ] **檔案路徑處理**: 重構 `Ini.kt` 和 `DesktopPoHelper.kt` 中寫死的相對路徑。考慮改用 ClassLoader 的資源讀取機制，避免因目錄結構變更導致程式出錯。
- [ ] **錯誤處理**: 改善通用的 `catch (e: Exception)` 區塊。改為捕捉更具體的例外類型，並將重要的錯誤訊息顯示在 UI 上，提升使用者體驗。
- [ ] **移除 Magic Strings**: 將程式碼中硬式編碼的字串 (如 Regex、URL) 定義為 `const val` 常數，增加程式碼的可讀性與可維護性。
- [ ] **移除對 DST 的支援**: 簡化程式碼，將所有與 DST 相關的邏輯和資源移除，專注於 ONI。

### 3. UI 與程式碼可讀性 (UI & Readability)

- [ ] **升級本地化方案**: 將目前 `ResourceBundle` 和自定義 `strings.xml` 的作法，升級為 `moko-resources` 函式庫，以實現型別安全且符合 KMP 最佳實踐的跨平台資源管理。
- [ ] **增加程式碼註解**: 在複雜的邏輯函式 (如 `analyzeText`) 中補充註解，說明其設計目的與演算法思路，方便未來維護。

### 4. 現代化與自動化 (Modernization & Automation)

- [ ] **程式碼品質：整合 Ktlint 自動化程式碼風格檢查**: 引入 `ktlint` Gradle 插件來自動格式化 Kotlin 程式碼並確保風格一致，提升程式碼品質與可讀性。
- [ ] **建構環境一致性：設定 Gradle Java Toolchain**: 在 `build.gradle.kts` 中設定 Java Toolchain，讓 Gradle 自動下載並使用指定的 JDK 版本，解決開發者間環境不一 (`JAVA_HOME`) 的問題，確保建構的穩定性與可預測性。
- [x] **依賴管理：整合 Gradle Versions Plugin**: 加入 `com.github.ben-manes.versions` 插件，並透過 `./gradlew dependencyUpdates` 指令定期檢查依賴庫的更新，以利專案維持在最新的狀態。
- [ ] **軟體品質：加入單元測試 (Unit Tests)**: 引入 `JUnit 5` 測試框架，為核心邏輯 (例如 `PoHelper.kt`, `Ini.kt`) 撰寫單元測試，以確保程式碼變更時的穩定性與正確性。
- [x] **自動化：更新 GitHub Actions CI 工作流程**: 優化並更新現有的 CI (Continuous Integration) 工作流程，包含升級 Actions 版本、整合 Gradle 快取，並確保在每次推送到 Git 倉庫時自動執行編譯 (`./gradlew build`) 和測試。
