using System;
using System.IO;
using System.Reflection;
using FontLoader.Config;
using FontLoader.Utils;
using HarmonyLib;
using TMPro;

namespace FontLoader
{
    public class FontLoaderPatches : KMod.UserMod2
    {
        private static readonly string ns = MethodBase.GetCurrentMethod().DeclaringType.Namespace;
        public static string rootPath;
        private static FontConfig fc;
        private static TMP_FontAsset font;

        public override void OnLoad(Harmony harmony)
        {
            try
            {
                harmony.PatchAll();
            }
            catch (Exception e)
            {
                Debug.LogError($"[{ns}] Harmony Patch failed: {e}");
            }

            // 2. 檢查 mod 實例是否存在 (Unity 6 穩定性檢查)
            if (this.mod == null || this.mod.file_source == null)
            {
                Debug.LogError($"[{ns}] Mod or FileSource is null! Something is wrong with the Mod Loader.");
                return;
            }

            try
            {
                LoadMyFont();
            }
            catch (Exception e)
            {
                Debug.LogError($"[{ns}] Critical error in OnLoad: {e.Message}");
            }
        }

        private void LoadMyFont()
        {
            rootPath = mod.file_source.GetRoot();
            ConfigManager.Instance.configPath = mod.file_source.GetRoot();
            fc = ConfigManager.Instance.LoadConfigFile() ?? ConfigManager.Instance.LoadDefault();
            font = FontUtil.LoadFontAsset(fc);

            if (font == null)
            {
                Debug.LogWarning($"[{ns}] Load font asset fail.");
            }
            else
            {
                // 取得 TextMeshPro 的全域設定
                var settings = TMP_Settings.GetFontAsset();
                if (settings != null)
                {
                    // 如果 Fallback 表裡還沒有你的字體，就塞進去
                    // 建議 Insert(0, ...) 確保它在搜尋的第一順位
                    if (!settings.fallbackFontAssetTable.Contains(font))
                    {
                        settings.fallbackFontAssetTable.Insert(0, font);
                        Debug.Log($"[{ns}] Successfully injected {font.name} into TMP Fallback Table!");
                    }
                }
                else
                {
                    Debug.LogWarning($"[{ns}] Could not find TMP_Settings. Font might not show up.");
                }
            }
        }

        [HarmonyPatch(typeof(Localization))]
        [HarmonyPatch(nameof(Localization.GetLocale))]
        [HarmonyPatch(new Type[] { typeof(string[]) })]
        public static class Localization_GetLocale_Patch
        {
            public static void Postfix(ref Localization.Locale __result)
            {
                try
                {
                    if (font == null)
                    {
                        return;
                    }

                    var Language = fc.Code.Equals("zh") ? Localization.Language.Chinese : Localization.Language.Unspecified;
                    var Direction = fc.LeftToRight ? Localization.Direction.LeftToRight : Localization.Direction.RightToLeft;
                    __result = new Localization.Locale(Language, Direction, fc.Code, font.name);
                }
                catch (Exception ex)
                {
                    DebugUtil.LogWarningArgs(new object[] { ex });
                }
            }
        }

        [HarmonyPatch(typeof(Db), "Initialize")]
        public static class Db_Initialize_Patch
        {
            public static void Postfix()
            {
                Debug.Log($"[{ns}] Db_Initialize_Patch: {rootPath}");
                // 1. 先取得當前 Mod 的父目錄 (即 1066780 資料夾)
                string workshopRoot = Path.GetDirectoryName(rootPath);

                // 2. 組合目標 Mod 的資料夾名稱 (2906930548)
                string targetModPath = Path.Combine(workshopRoot, "2906930548");

                // 3. 組合最終檔案路徑
                string str = Path.Combine(targetModPath, "strings.po");
                if (File.Exists(str))
                {
                    Debug.Log($"[{ns}] Db_Initialize_Patch: {str}");
                    var dist = Localization.LoadStringsFile(str, false);
                    if (dist != null && fc.InitString)
                    {
                        Debug.Log($"[{ns}] Db_Initialize_Patch: Found strings " + dist.Count);
                        Localization.OverloadStrings(dist);
                    }
                }
                else
                {
                    Debug.LogWarning($"[{ns}] Not subscribe to Nisbet's Traditional Chinese Pack.");
                }
            }
        }
    }
}
