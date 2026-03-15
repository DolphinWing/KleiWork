using System;
using System.IO;
using FontLoader.Config;
using TMPro;
using UnityEngine;

namespace FontLoader.Utils
{
    public static class FontUtil
    {
        public static TMP_FontAsset LoadFontAsset(FontConfig config)
        {
            try
            {
                var platform = Application.platform == RuntimePlatform.WindowsPlayer ? "win": "other";
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

                // font.faceInfo.scale = config.Scale;

                //if (Application.platform == RuntimePlatform.LinuxPlayer) {
                //    var sourceFont = Resources.Load<TMP_FontAsset>("RobotoCondensed-Regular");
                //    if (sourceFont != null)
                //    {
                //        font.material.shader = sourceFont.material.shader;
                //    }
                //}

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
    }
}
