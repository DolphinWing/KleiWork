using HarmonyLib;
using Klei.CustomSettings;
using PeterHan.PLib.Core;
using PeterHan.PLib.Database;
using PeterHan.PLib.Options;
using PeterHan.PLib.PatchManager;
using ProcGen;

namespace Heliconia
{
    class Heliconia : KMod.UserMod2
    {
        public static LocString NAME = (LocString)"Heliconia";
        public static LocString MOD_DESC = (LocString)"Hell as hot as possible.";
        public static LocString DESCRIPTION = (LocString)"Hell as hot as possible.\n\n<smallcaps>Duplicants MUST work to DEATH to make the colony thrive again.</smallcaps>";
        public static LocString BIOME_DESC = (LocString)"Hell as hot as possible.";
        public static LocString UTILITY_DESC = (LocString)"Hell as hot as possible.";

        public static LocString WARP_NAME = (LocString)"Heliconia Warp";
        public static LocString WARP_DESCRIPTION = (LocString)"Heliconia warp world";

        public override void OnLoad(Harmony harmony)
        {
            base.OnLoad(harmony);
            PUtil.InitLibrary();
            new PLocalization().Register();
            new POptions().RegisterOptions(this, typeof(HeliconiaOptions));
            new PPatchManager(harmony).RegisterPatchClass(typeof(Heliconia));

            HeliconiaTemperature.AddToHashTable();
        }

        /// <summary>
		/// Registers the strings used in this mod.
		/// </summary>
		[PLibMethod(RunAt.AfterDbInit)]
        internal static void InitStrings()
        {
            Strings.Add("Heliconia worldgen", NAME);
            Strings.Add("Hell as hot as possible.", MOD_DESC);
            Strings.Add("STRINGS.CLUSTER_NAMES.HELICONIA.NAME", NAME);
            Strings.Add("STRINGS.CLUSTER_NAMES.HELICONIA.DESCRIPTION", DESCRIPTION);
            Strings.Add("STRINGS.SUBWORLDS.HELICONIA.NAME", NAME);
            Strings.Add("STRINGS.SUBWORLDS.HELICONIA.DESC", BIOME_DESC);
            Strings.Add("STRINGS.SUBWORLDS.HELICONIA.UTILITY", UTILITY_DESC);
            Strings.Add("STRINGS.WORLDS.HELICONIA.NAME", NAME);
            Strings.Add("STRINGS.WORLDS.HELICONIA.DESCRIPTION", DESCRIPTION);
            Strings.Add("STRINGS.WORLDS.HELICONIASO.NAME", NAME);
            Strings.Add("STRINGS.WORLDS.HELICONIASO.DESCRIPTION", DESCRIPTION);
            Strings.Add("STRINGS.WORLDS.HELICONIAWARP.NAME", WARP_NAME);
            Strings.Add("STRINGS.WORLDS.HELICONIAWARP.DESCRIPTION", WARP_DESCRIPTION);

            //var sprite = Assets.GetSprite("biomeIconSpace");
            //if (sprite != null)
            //{
            //    Assets.Sprites.Add("biomeIconHeliconia", sprite);
            //}
        }

        public static bool IsHcaCluster()
        {
            SettingLevel current = CustomGameSettings.Instance.GetCurrentQualitySetting((SettingConfig)CustomGameSettingConfigs.ClusterLayout);
            if (current == null) return false; // unknown cluster

            ClusterLayout clusterData = SettingsCache.clusterLayouts.GetClusterData(current.id);
            string prefix = clusterData.GetCoordinatePrefix();
            return prefix.StartsWith("HCA-TG-"); // B: base game. C: Spaced Out classic. M: Spaced Out style.
        }

        public static bool IsHcaWorld(ProcGen.World world)
        {
            return world != null && world.name.StartsWith("Heliconia.");
        }

        [HarmonyPatch(typeof(ColonyDestinationSelectScreen), "OnSpawn")]
        public static class ColonyDestinationSelectScreen_OnSpawn_Patch
        {
            public static void Prefix()
            {
                //PUtil.LogDebug("ColonyDestinationSelectScreen_OnSpawn_Patch.Prefix");
            }
        }
    }
}
