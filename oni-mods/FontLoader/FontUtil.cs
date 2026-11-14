using System;
using System.IO;
using System.Reflection;
using FontLoader.Config;
using TMPro;
using UnityEngine;

namespace FontLoader.Utils
{
    public static class FontUtil
    {
        private static string GetPlatformFolder()
        {
            // 包含 Editor 環境的判斷，確保在開發階段的 Behavior 一致
            return Application.platform switch
            {
                RuntimePlatform.WindowsPlayer => "win",
                RuntimePlatform.WindowsEditor => "win",

                RuntimePlatform.OSXPlayer => "mac",
                RuntimePlatform.OSXEditor => "mac",

                RuntimePlatform.LinuxPlayer => "linux",
                RuntimePlatform.LinuxEditor => "linux",
                _ => "other"
            };
        }

        public static TMP_FontAsset LoadFontAsset(FontConfig config)
        {
            try
            {
                var platform = GetPlatformFolder();
                // Application.platform == RuntimePlatform.WindowsPlayer ? "win": "other";
                if (platform == null || platform == "other")
                {
                    // 明確告知是因為平台不支援而返回 null，而不是檔案找不到
                    Debug.LogError($"[FontLoader] Aborting load. Platform {Application.platform} is intentionally not supported.");
                    return null;
                }

                var fileName = (config.Filename == "openhuninn") ? "openhuninn.u6" : config.Filename;
                var assetPath = Path.Combine(ConfigManager.Instance.configPath, "Assets", platform, fileName);
                Debug.Log($"[FontLoader] {platform} {assetPath}");

                AssetBundle ab = AssetBundle.LoadFromFile(assetPath);

                if (ab == null) {
                    Debug.LogWarning("[FontLoader] Unable to load font asset.");
                    return null;
                }

                // 2. 獲取字體
                var assets = ab.GetAllAssetNames();
                if (assets == null || assets.Length <= 0)
                {
                    Debug.LogWarning($"[FontLoader] Unable to load font asset. {assets?.Length}");
                    ab.Unload(true);
                    return null;
                }

                Debug.Log($"[FontLoader] {assets[0]}");
                var font = ab.LoadAsset<TMP_FontAsset>(assets[0]);
                if (font == null)
                {
                    Debug.LogWarning("[FontLoader] TMP_FontAsset not found in bundle.");
                    ab.Unload(true);
                    return null;
                }

                // 修復 AssetBundle 的 Shader（不限 Linux，Windows Unity 6 同樣需要）
                var sourceFont = Localization.FontAsset ?? Resources.Load<TMP_FontAsset>("RobotoCondensed-Regular") ?? TMP_Settings.GetFontAsset();
                if (sourceFont != null && sourceFont.material != null && font.material != null)
                {
                    font.material.shader = sourceFont.material.shader;
                }

                ApplyFontScaleSafely(font, config.Scale);
                
                Debug.Log($"[FontLoader] Font Name: {font.name}");
                ab.LoadAllAssets();
                //Debug.Log($"[FontLoader] Atlas: {font.atlasTexture?.name ?? "NULL!"}");
                //Debug.Log($"[FontLoader] Material Shader: {font.material?.shader?.name ?? "NULL!"}");
                //Debug.Log($"[FontLoader] Character Count: {font.characterTable?.Count ?? 0}");
                Debug.Log($"[FontLoader] Font Name: {font.name} LoadAllAssets.");
                return font;
            }
            catch (Exception e)
            {
                Debug.LogError($"[FontLoader] {e.Message}");
            }

            // AssetBundle.UnloadAllAssetBundles(false);
            return null;
        }

        private static void ApplyFontScaleSafely(TMP_FontAsset font, float scale)
        {
            if (Math.Abs(scale - 1.0f) < 0.001f) return;

            try
            {
                var fontInfoProp = typeof(TMP_FontAsset).GetProperty("fontInfo", BindingFlags.Public | BindingFlags.Instance);
                if (fontInfoProp != null)
                {
                    var fontInfo = fontInfoProp.GetValue(font);
                    if (fontInfo != null)
                    {
                        var scaleField = fontInfo.GetType().GetField("Scale", BindingFlags.Public | BindingFlags.Instance);
                        if (scaleField != null)
                        {
                            scaleField.SetValue(fontInfo, scale);
                            return;
                        }
                    }
                }

                var faceInfoProp = typeof(TMP_FontAsset).GetProperty("faceInfo", BindingFlags.Public | BindingFlags.Instance);
                if (faceInfoProp != null)
                {
                    var faceInfo = faceInfoProp.GetValue(font);
                    if (faceInfo != null)
                    {
                        var scaleProp = faceInfo.GetType().GetProperty("scale", BindingFlags.Public | BindingFlags.Instance);
                        if (scaleProp != null && scaleProp.CanWrite)
                        {
                            scaleProp.SetValue(faceInfo, scale);
                            faceInfoProp.SetValue(font, faceInfo);
                        }
                    }
                }
            }
            catch (Exception ex)
            {
                Debug.LogWarning($"[FontLoader] Could not apply custom font scale: {ex.Message}");
            }
        }
    }
}
