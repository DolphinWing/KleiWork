using System;
using System.IO;
using System.Reflection;
using Newtonsoft.Json;

namespace FontLoader.Config
{
    public class FontConfig
    {
        public string Filename { get; set; }
        public string Code { get; set; }
        public bool LeftToRight { get; set; }
        public float Scale { get; set; }
        public bool InitString { get; set; }

        public FontConfig(string filename, string code, bool leftToRight, float scale)
        {
            this.Filename = filename;
            this.Code = code;
            this.LeftToRight = leftToRight;
            this.Scale = scale;
            this.InitString = true;
        }
    }

    public class ConfigManager
    {
        private readonly string ns = MethodBase.GetCurrentMethod().DeclaringType.Namespace;
        private static readonly object _lock = new Object();
        private static ConfigManager instance;
        public string configPath {get; set;}

        public static ConfigManager Instance
        {
            get
            {
                lock (_lock)
                {
                    if (instance == null)
                    {
                        instance = new ConfigManager();
                    }
                    return instance;
                }
            }
        }

        public FontConfig LoadConfigFile()
        {
            FontConfig fc;
            try
            {
                using (var r = new StreamReader(Path.Combine(configPath, "config.json")))
                {
                    var json = r.ReadToEnd();
                    fc = JsonConvert.DeserializeObject<FontConfig>(json);
                }
            }
            catch (Exception e)
            {
                Debug.Log($"[{ns}] Load config failure...Excption: {e.Message}");
                Debug.Log($"[{ns}] Use default config.");
                fc = LoadDefault();
            }
            return fc;
        }

        public FontConfig LoadDefault()
        {
            return new FontConfig("font", "zh", true, 1);
        }
    }
}
