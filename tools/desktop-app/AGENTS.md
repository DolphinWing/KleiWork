# ONI Translator (`tools/desktop-app`) 開發規範

## 1. 模組組織與命名慣例
* **`model/` 維持扁平結構**：不隨意新增泛用子套件（如 `model/ai/`）。
* **不建立 `*Models.kt` 雜物檔**：領域資料類別直接與所屬核心引擎置於同一檔案（如 `GlossaryBank.kt`、`GlossarySensor.kt`、`PromptComposer.kt`、`GeminiClient.kt`）。
* **具體領域命名**：使用具體領域／角色隱喻命名（如 `TextRefinery`、`TagSensor`、`NisbetPeek`、`BrigetteDesk`），避免泛稱 `Ai*` 或 `Util*` 前綴。

## 2. 設定與外部清單設計原則
* **設定檔最小化**：`configs.ini` 與 `ConfigScreen` 僅保存持久性環境設定（路徑、API Key、Model ID、Auto-save）；屬於當下互動的動態勾選開關（如 `PromptContextOptions`）保留在記憶體 UI 狀態中，不寫入設定檔。
* **動態查詢優先於寫死清單**：對會快速演進的外部清單（如 Gemini 模型 ID），優先透過 API 動態取得，不寫死過時版本清單。

## 3. 依賴與打包邊界
* **JSON 處理**：專案啟用 Release ProGuard (`obfuscate = true`)，JSON 組裝與解析優先採用 `kotlinx-serialization-json` 純 AST 模式（`JsonObject` / `buildJsonObject`），避免引入反射式 SDK 或額外編譯器外掛混淆負擔。

## 4. 漸進式文件同步
* 分步驟實作時，**每完成一個 Step 即同步更新 `ARCHITECTURE.md`**（含對應架構說明與 Opt-in 測試／Smoke Test 執行方式），不拖到最後才補寫。
