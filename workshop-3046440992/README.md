[字型模組。新](https://steamcommunity.com/sharedfiles/filedetails/?id=3046440992) by DolphinWing

需要使用 [正體中文](https://steamcommunity.com/sharedfiles/filedetails/?id=2906930548) by DolphinWing 或其他正體中文包翻譯才有效果

* 2024/11/25 針對 2024 十一月的生活質量更新的 Unity 調整，升級到 .Net Framework 4.8
* 2025/05/26 針對 2025 推出的史前行星包部分缺少的文字做更新。更新[粉圓體](https://github.com/justfont/open-huninn-font/releases/tag/v2.1) v2.1 (2024/09/19)
* 2026/03/21 針對 2026 三月推出的 Unity 6/.netstandard 2.1 升級。

此為古靈精怪的 [字型模組](https://steamcommunity.com/sharedfiles/filedetails/?id=2119648603) 改良版，本包的原始碼請參考 [GitHub](https://github.com/DolphinWing/KleiWork/tree/master/workshop-3046440992)

#### 手動更換字型檔的步驟
- 安裝 Unity 6.0 和 TextMesh Pro，並匯出字型素材。
  - 參考 qbane 寫的 [README](https://github.com/qbane/ONI-Mods)。
- 將字型素材放到 Steam 模組的資料夾。(以 Windows 系統為例：我的文件/Klei/OxygenNotIncluded/mods/Steam/3046440992。)
  - Windows 的放到 Assets/win 資料夾內。
  - Ubuntu 或 macOS 放到 Assets/other 資料夾內。
- 修改 [config.json](config.json) 裡面 filename 指定的檔案。
- 重新打開遊戲。如果有載入指定的字型就會看到，否則會載入模組內預設的版本。

#### 參考資料：
* 古靈精怪 [字型模組](https://steamcommunity.com/workshop/filedetails/?id=2119648603) & [GitHub](https://github.com/dershiuan/ONI-Mods/tree/v2.0.4/FontLoader)
* qbane Reddit Post [Made a font loader mod available on Linux and macOS](https://www.reddit.com/r/Oxygennotincluded/comments/orijbl/made_a_font_loader_mod_available_on_linux_and/) and [GitHub](https://github.com/qbane/ONI-Mods)
* miZyind [正體中文字體用語翻譯包](https://steamcommunity.com/sharedfiles/filedetails/?id=2070840646) 的 [討論版](https://steamcommunity.com/workshop/filedetails/discussion/2070840646/3044978964803635873)
