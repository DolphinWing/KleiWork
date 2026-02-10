# 專案資料夾結構與參考資料說明

## 📂 專案核心資料夾 (Project Core Folders)

本專案的主要程式碼、設定與輸出檔案位於以下資料夾：

| 資料夾路徑 | 內容描述 | Steam ID | 備註 |
| :--- | :--- | :--- | :--- |
| `./workshop-2906930548` | Oxygen Not Included 正體中文 | 2906930548 | 翻譯模組 |
| `./workshop-3046440992` | Oxygen Not Included 字型模組。新 | 3046440992 | 搭配翻譯模組使用的字型模組 |
| `./workshop-3413401611` | Oxygen Not Included Not 0K, But Pretty Cool Place | 3413401611 | 地圖模組，代號 ABZ |
| `./workshop-superconductive` | Oxygen Not Included Moonlet Cluster - Superconductive | 3418019940 | 地圖模組 |
| `./workshop-3430682737` | Oxygen Not Included Voidria | 3430682737 | 地圖模組，代號 VOA |
| `./workshop-heliconia` | Oxygen Not Included Heliconia | 3663491695 | 地圖模組，代號 HCA |
| `./tools/android-app` | 我的翻譯小工具 | 專門將 Klei 官方翻譯檔案和我的翻譯模組載入，讓我在版本更新時比較容易編輯的小工具，目前已經停止維護 |
| `./tools/desktop-app` | 我的翻譯小工具 | 專門將 Klei 官方翻譯檔案和我的翻譯模組載入，讓我在版本更新時比較容易編輯的小工具 |
| `./oni-mods/FontLoader` | workshop-3046440992 使用的模組原始碼 | 字型模組。新的模組需要 Harmony 載入的部分 |
| `./oni-mods/Rime_Extreme_2` | workshop-3413401611 使用的模組原始碼 | Not 0K, But Pretty Cool Place 模組需要 Harmony 載入的部分 |
| `./oni-mods/Niobium_Start` | workshop-superconductive 使用的模組原始碼 | Moonlet Cluster - Superconductive 模組需要 Harmony 載入的部分 |
| `./oni-mods/TorchGod_Voidria` | workshop-3430682737 使用的模組原始碼 | Voidria 模組需要 Harmony 載入的部分 |
| `./oni-mods/TorchGod_Heliconia` | workshop-heliconia 使用的模組原始碼 | Heliconia 模組需要 Harmony 載入的部分 |

## 📚 參考與靜態資料 (Reference and Static Data)

以下資料夾包含本專案運行所需的外部、靜態或參考資料。**這些資料夾內的內容將保持只讀，專案運行時不會對其進行任何修改。**

| 資料夾路徑 | 內容描述 | **使用限制** |
| :--- | :--- | :--- |
| `./oni-assets` | Oxygen Not Included 的原始、靜態參考文件、CSV 或 JSON 資料集。 | **只讀 (Read-Only)。** |
| `./dst-assets` | Don't Starve Together 的原始、靜態參考文件、CSV 或 JSON 資料集。 | **只讀 (Read-Only)。** |

---

### 關鍵指令說明

為確保資料完整性，所有與 `./oni-assets` 相關的腳本都必須遵循以下原則：

* **禁止寫入：** 絕對不允許任何程式碼向 `./oni-assets` 或其子資料夾寫入、刪除或修改任何檔案。
* **數據複製：** 如果需要處理或清洗參考資料，請先將相關檔案複製到 `./tmp` 資料夾下進行操作，並在操作完畢後刪除。
