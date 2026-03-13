using HarmonyLib;
using Klei.CustomSettings;
using PeterHan.PLib.Core;
using PeterHan.PLib.Database;
using PeterHan.PLib.Options;
using PeterHan.PLib.PatchManager;
using ProcGen;
using System.Collections.Generic;

namespace Voidria
{
    class Voidria : KMod.UserMod2
    {
        public static LocString NAME = (LocString)"Voidria";
        public static LocString MOD_DESC = (LocString)"Hopeless void. Resources scarced and limited. GEYSERS NOT INCLUDED.";
        public static LocString DESCRIPTION = (LocString)"Hopeless void. Resources scarced and limited. GEYSERS NOT INCLUDED.\n\n<smallcaps>Duplicants MUST work to DEATH to make the colony thrive again.</smallcaps>";
        public static LocString BIOME_DESC = (LocString)"Seriously, I feel like in space.";
        public static LocString UTILITY_DESC = (LocString)"Much much care must be taken to ensure <link=\"ELEMENTSLIQUID\">Liquids</link> or <link=\"ELEMENTSGAS\">Gases</link> are not sucked out into the <link=\"VACUUM\">Vacuum</link> of space.";

        public static LocString WARP_NAME = (LocString)"Rocker";
        public static LocString WARP_DESC = (LocString)"A tiny rock needs one small step.";

        public static LocString LAND_NAME = (LocString)"Landing Zone";
        public static LocString LAND_DESC = (LocString)"A tiny rock to land your little rocket.";

        public override void OnLoad(Harmony harmony)
        {
            base.OnLoad(harmony);
            PUtil.InitLibrary();
            new PLocalization().Register();
            new POptions().RegisterOptions(this, typeof(VoidriaOptions));
            new PPatchManager(harmony).RegisterPatchClass(typeof(Voidria));
        }

        /// <summary>
		/// Registers the strings used in this mod.
		/// </summary>
		[PLibMethod(RunAt.AfterDbInit)]
        internal static void InitStrings()
        {
            Strings.Add("Voidria worldgen", NAME);
            Strings.Add("Hopeless void. Resources scarced and limited.", MOD_DESC);
            Strings.Add("STRINGS.CLUSTER_NAMES.VOIDRIA.NAME", NAME);
            Strings.Add("STRINGS.CLUSTER_NAMES.VOIDRIA.DESCRIPTION", DESCRIPTION);
            Strings.Add("STRINGS.SUBWORLDS.VOIDRIA.NAME", NAME);
            Strings.Add("STRINGS.SUBWORLDS.VOIDRIA.DESC", BIOME_DESC);
            Strings.Add("STRINGS.SUBWORLDS.VOIDRIA.UTILITY", UTILITY_DESC);
            Strings.Add("STRINGS.WORLDS.TINYLANDINGZONE.NAME", LAND_NAME);
            Strings.Add("STRINGS.WORLDS.TINYLANDINGZONE.DESCRIPTION", LAND_DESC);
            Strings.Add("STRINGS.WORLDS.TINYWARPSURFACE.NAME", WARP_NAME);
            Strings.Add("STRINGS.WORLDS.TINYWARAPSURFACE.DESCRIPTION", WARP_DESC);
            Strings.Add("STRINGS.WORLDS.VOIDRIA.NAME", NAME);
            Strings.Add("STRINGS.WORLDS.VOIDRIA.DESCRIPTION", DESCRIPTION);
            Strings.Add("STRINGS.WORLDS.VOIDRIASO.NAME", NAME);
            Strings.Add("STRINGS.WORLDS.VOIDRIASO.DESCRIPTION", DESCRIPTION);
            Strings.Add("STRINGS.WORLDS.VOIDRIAMINI.NAME", NAME);
            Strings.Add("STRINGS.WORLDS.VOIDRIAMINI.DESCRIPTION", DESCRIPTION);

            var sprite = Assets.GetSprite("biomeIconSpace");
            if (sprite != null)
            {
                Assets.Sprites.Add("biomeIconVoidria", sprite);
            }
        }

        public static bool IsVoaCluster()
        {
            SettingLevel current = CustomGameSettings.Instance.GetCurrentQualitySetting((SettingConfig)CustomGameSettingConfigs.ClusterLayout);
            if (current == null) return false; // unknown cluster

            ClusterLayout clusterData = SettingsCache.clusterLayouts.GetClusterData(current.id);
            string prefix = clusterData.GetCoordinatePrefix();
            return prefix.StartsWith("VOA-TG-"); // B: base game. C: Spaced Out classic. M: Spaced Out style.
        }

        [HarmonyPatch(typeof(ColonyDestinationSelectScreen), "OnSpawn")]
        public static class ColonyDestinationSelectScreen_OnSpawn_Patch
        {
            public static void Prefix()
            {
                //PUtil.LogDebug("ColonyDestinationSelectScreen_OnSpawn_Patch.Prefix");
            }
        }

        [HarmonyPatch(typeof(ClusterPOIManager), "RegisterTemporalTear")]
        public static class ClusterPOIManager_RegisterTemporalTear_Patch
        {
            public static void Postfix(TemporalTear temporalTear, ClusterPOIManager __instance)
            {
                //PUtil.LogDebug("ClusterPOIManager_RegisterTemporalTear_Patch.Postfix");

                if (IsVoaCluster() == false) return; // don't care about other clusters

                var options = VoidriaOptions.GetInstance();
                if (!options.EnableBackground && options.SaveCritters)
                {
                    PUtil.LogDebug("Spawn all to save all backwalls devs need.");
                    SaveGame.Instance.worldGenSpawner.SpawnEverything();
                }
                if (options.EnableTearOpener) return; // player will do by themselves
                if (temporalTear.IsOpen() == false)
                {
                    temporalTear.Open();
                    PUtil.LogDebug("Open Temporal Tear");
                }
            }
        }
    }
}
