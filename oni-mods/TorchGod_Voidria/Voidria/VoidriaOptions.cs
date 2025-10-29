using Newtonsoft.Json;
using PeterHan.PLib.Options;

namespace Voidria
{
    [JsonObject(MemberSerialization.OptIn)]
    [ModInfo("https://github.com/DolphinWing/KleiWork/tree/master/workshop-3430682737")]
    class VoidriaOptions
    {
        public static LocString GiftedAreaTitle = (LocString)"Treasure Room";
        public static LocString GiftedAreaTooltip = (LocString)"Discover gifts from the developer";

        public static LocString TearOpenerTitle = (LocString)"Temporal Tear Opener";
        public static LocString TearOpenerTooltip = (LocString)"I will decide when to open Temporal Tear";

        public static LocString BackgroundTitle = (LocString)"Voidria's Back";
        public static LocString BackgroundTooltip = (LocString)"Choose between space exposure or happy background";

        public static LocString IronVolcanoTitle = (LocString)"Iron Volcano";
        public static LocString IronVolcanoTooltip = (LocString)"With so much iron makes life easier";
 
        public static LocString OilReservoirTitle = (LocString)"Oil Reservoir";
        public static LocString OilReservoirTooltip = (LocString)"Water In, Crude Oil Out. Easy Livin'";

        public static LocString CrittersCaveTitle = (LocString)"Critter Shalter";
        public static LocString CrittersCaveTooltip = (LocString)"Let critters have their own pleasure";

        public static LocString SaveCrittersTitle = (LocString)"Save Critters";
        public static LocString SaveCrittersTooltip = (LocString)"Let critters live like they should be";

        [Option("Voidria.VoidriaOptions.GiftedAreaTitle", "Voidria.VoidriaOptions.GiftedAreaTooltip", "STRINGS.UI.DETAILTABS.SIMPLEINFO.GROUPNAME_WORLDTRAITS")]
        [JsonProperty]
        public bool EnableGift { get; set; }

        [Option("Voidria.VoidriaOptions.TearOpenerTitle", "Voidria.VoidriaOptions.TearOpenerTooltip", "STRINGS.UI.DETAILTABS.SIMPLEINFO.GROUPNAME_WORLDTRAITS")]
        [JsonProperty]
        public bool EnableTearOpener { get; set; }

        [Option("Voidria.VoidriaOptions.BackgroundTitle", "Voidria.VoidriaOptions.BackgroundTooltip", "STRINGS.UI.DETAILTABS.SIMPLEINFO.GROUPNAME_WORLDTRAITS")]
        [JsonProperty]
        public bool EnableBackground { get; set; }

        [Option("Voidria.VoidriaOptions.IronVolcanoTitle", "Voidria.VoidriaOptions.IronVolcanoTooltip", "STRINGS.UI.DETAILTABS.SIMPLEINFO.GROUPNAME_GEYSERS")]
        [JsonProperty]
        public bool EnableIronVolcano { get; set; }

        [Option("Voidria.VoidriaOptions.OilReservoirTitle", "Voidria.VoidriaOptions.OilReservoirTooltip", "STRINGS.UI.DETAILTABS.SIMPLEINFO.GROUPNAME_GEYSERS")]
        [JsonProperty]
        public bool EnableOilReservoir { get; set; }

        [Option("Voidria.VoidriaOptions.CrittersCaveTitle", "Voidria.VoidriaOptions.CrittersCaveTooltip", "STRINGS.UI.DETAILTABS.SIMPLEINFO.GROUPNAME_LIFE")]
        [JsonProperty]
        public bool EnableCritters { get; set; }

        [Option("Voidria.VoidriaOptions.SaveCrittersTitle", "Voidria.VoidriaOptions.SaveCrittersTooltip", "STRINGS.UI.DETAILTABS.SIMPLEINFO.GROUPNAME_LIFE")]
        [JsonProperty]
        public bool SaveCritters { get; set; }

        public VoidriaOptions()
        {
            EnableGift = true;
            EnableIronVolcano = false;
            EnableOilReservoir = true;
            EnableCritters = true;
            SaveCritters = true;
            EnableTearOpener = true;
            EnableBackground = false;
        }

        internal static VoidriaOptions GetInstance()
        {
            var options = POptions.ReadSettings<VoidriaOptions>();
            return options ?? new VoidriaOptions();
        }
    }
}
