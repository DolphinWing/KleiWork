using Newtonsoft.Json;
using PeterHan.PLib.Options;

namespace Heliconia
{
    [JsonObject(MemberSerialization.OptIn)]
    [ModInfo("https://github.com/DolphinWing/KleiWork/tree/master/workshop-heliconia")]
    class HeliconiaOptions
    {
        public static LocString GiftedAreaTitle = (LocString)"Treasure Room";
        public static LocString GiftedAreaTooltip = (LocString)"Discover gifts from the developer";

        public static LocString CrittersCaveTitle = (LocString)"Critter Shalter";
        public static LocString CrittersCaveTooltip = (LocString)"Let critters have their own pleasure";

        [Option("Heliconia.HeliconiaOptions.GiftedAreaTitle", "Heliconia.HeliconiaOptions.GiftedAreaTooltip", "STRINGS.UI.DETAILTABS.SIMPLEINFO.GROUPNAME_WORLDTRAITS")]
        [JsonProperty]
        public bool Shelter { get; set; }

        [Option("Heliconia.HeliconiaOptions.CrittersCaveTitle", "Heliconia.HeliconiaOptions.CrittersCaveTooltip", "STRINGS.UI.DETAILTABS.SIMPLEINFO.GROUPNAME_WORLDTRAITS")]
        [JsonProperty]
        public bool Critter { get; set; }

        public enum MapMode
        {
            [Option("Heliconia.HeliconiaOptions.MAP_MODE_BALANCED", "Heliconia.HeliconiaOptions.MAP_MODE_BALANCED_DESC")]
            Balanced,

            [Option("Heliconia.HeliconiaOptions.MAP_MODE_EASY", "Heliconia.HeliconiaOptions.MAP_MODE_EASY_DESC")]
            Easy,

            [Option("Heliconia.HeliconiaOptions.MAP_MODE_CRAZY", "Heliconia.HeliconiaOptions.MAP_MODE_CRAZY_DESC")]
            Crazy
        }

        [Option("Heliconia.HeliconiaOptions.MAP_MODE", "Heliconia.HeliconiaOptions.MAP_MODE_DESC")]
        [JsonProperty]
        public MapMode Mode { get; set; }


        public HeliconiaOptions()
        {
            Shelter = true; // MutatedWorldData_Constructor_Patch
            Critter = true; // MutatedWorldData_Constructor_Patch
            Mode = MapMode.Balanced; // TerrainCell_GetTemperatureRange_Patch
        }

        internal static HeliconiaOptions GetInstance()
        {
            var options = POptions.ReadSettings<HeliconiaOptions>();
            return options ?? new HeliconiaOptions();
        }
    }
}
