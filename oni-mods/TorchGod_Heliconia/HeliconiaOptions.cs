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
		
		public static LocString InstantModeTitle = (LocString)"Instant temperature balanced";
        public static LocString InstantModeTooltip = (LocString)"Instant temperature balanced right after worldgen";

        [Option("Heliconia.HeliconiaOptions.GiftedAreaTitle", "Heliconia.HeliconiaOptions.GiftedAreaTooltip", "STRINGS.UI.DETAILTABS.SIMPLEINFO.GROUPNAME_WORLDTRAITS")]
        [JsonProperty]
        public bool Shelter { get; set; }

        [Option("Heliconia.HeliconiaOptions.CrittersCaveTitle", "Heliconia.HeliconiaOptions.CrittersCaveTooltip", "STRINGS.UI.DETAILTABS.SIMPLEINFO.GROUPNAME_WORLDTRAITS")]
        [JsonProperty]
        public bool Critter { get; set; }
		
		[Option("Heliconia.HeliconiaOptions.InstantModeTitle", "Heliconia.HeliconiaOptions.InstantModeTooltip", "STRINGS.UI.DETAILTABS.SIMPLEINFO.GROUPNAME_WORLDTRAITS")]
        [JsonProperty]
        public bool InstantMode { get; set; }

        public static LocString MAP_MODE = (LocString)"Temperature Mode";
        public static LocString MAP_MODE_DESC = (LocString)"Select starting biome temperature";

        public static LocString MAP_MODE_BALANCED = (LocString)"Balanced";
        public static LocString MAP_MODE_BALANCED_DESC = (LocString)"balanced but still quite a chanllege.";

        public static LocString MAP_MODE_EASY = (LocString)"Easy";
        public static LocString MAP_MODE_EASY_DESC = (LocString)"a bit taste of Heliconia";

        public static LocString MAP_MODE_CRAZY = (LocString)"Crazy";
        public static LocString MAP_MODE_CRAZY_DESC = (LocString)"I bet you are crazy about this game.";

        public static LocString MAP_MODE_INSANE = (LocString)"Insane";
        public static LocString MAP_MODE_INSANE_DESC = (LocString)"Only hardcore gamer can do this.";

        public enum MapMode
        {
            [Option("Heliconia.HeliconiaOptions.MAP_MODE_BALANCED", "Heliconia.HeliconiaOptions.MAP_MODE_BALANCED_DESC")]
            Balanced,

            [Option("Heliconia.HeliconiaOptions.MAP_MODE_EASY", "Heliconia.HeliconiaOptions.MAP_MODE_EASY_DESC")]
            Easy,

            [Option("Heliconia.HeliconiaOptions.MAP_MODE_CRAZY", "Heliconia.HeliconiaOptions.MAP_MODE_CRAZY_DESC")]
            Crazy,

			[Option("Heliconia.HeliconiaOptions.MAP_MODE_INSANE", "Heliconia.HeliconiaOptions.MAP_MODE_INSANE_DESC")]
            Insane,
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
