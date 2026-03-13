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
                var assetPath = Path.Combine(ConfigManager.Instance.configPath, "Assets", platform, config.Filename);
                Debug.Log("[FontLoader] " + platform + " " + assetPath);

                AssetBundle ab = AssetBundle.LoadFromFile(assetPath);

                if (ab == null) {
                    Debug.LogWarning("[FontLoader] Unable to load font asset.");
                    return null;
                }

                // 2. 獲取字體
                var font = ab.LoadAsset<TMP_FontAsset>(ab.GetAllAssetNames()[0]);
                if (font == null)
                {
                    Debug.LogWarning("[FontLoader] TMP_FontAsset not found in bundle.");
                    ab.Unload(true);
                    return null;
                }

                // font.faceInfo.scale = config.Scale;

                if (Application.platform == RuntimePlatform.LinuxPlayer) {
                    var sourceFont = Resources.Load<TMP_FontAsset>("RobotoCondensed-Regular");
                    if (sourceFont != null)
                    {
                        font.material.shader = sourceFont.material.shader;
                    }
                }
                
                return font;
            }
            catch (Exception e)
            {
                Debug.LogError($"[FontLoader] {e.Message}");
            }

            AssetBundle.UnloadAllAssetBundles(false);
            return null;
        }
    }
}
