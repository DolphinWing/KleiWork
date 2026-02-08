using HarmonyLib;
using Klei.CustomSettings;
using PeterHan.PLib.Core;
using PeterHan.PLib.Database;
using PeterHan.PLib.Options;
using PeterHan.PLib.PatchManager;
using ProcGen;
using System;

namespace Heliconia
{
    public enum AsteroidType
    {
        HeliconiaBase,          // All Heliconia-branded starts worlds
        HeliconiaWarp,          // All Heliconia-branded warp worlds

        // Klei Outer Asteroids (Expansion 1)
        TundraMoonlet,
        MarshyMoonlet,
        NiobiumMoonlet,
        WaterMoonlet,
        MooMoonlet,
        RegolithMoonlet,

        Unknown
    }

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

        public static AsteroidType IdentifyWorld(ProcGen.World world)
        {
            if (world == null) return AsteroidType.Unknown;

            string name = world.name.ToUpper();

            if (name.StartsWith("HELICONIA."))
            {
                // [11:45:06.026] [1] [INFO] [PLib/Heliconia] Heliconia.Heliconia.NAME: HeliconiaBase
                // [11:45:06.104] [1] [INFO] [PLib/Heliconia] Heliconia.Heliconia.WARP_NAME: HeliconiaWarp
                return name.Contains("WARP") ? AsteroidType.HeliconiaWarp : AsteroidType.HeliconiaBase;
            }

            // [11:45:06.340][1][INFO][PLib / Heliconia] STRINGS.WORLDS.MARSHYMOONLET.NAME: MarshyMoonlet
            // [11:45:06.409][1][INFO][PLib / Heliconia] STRINGS.WORLDS.NIOBIUMMOONLET.NAME: NiobiumMoonlet
            // [11:45:06.477][1][INFO][PLib / Heliconia] STRINGS.WORLDS.MOOMOONLET.NAME: MooMoonlet
            // [11:45:06.548][1][INFO][PLib / Heliconia] STRINGS.WORLDS.WATERMOONLET.NAME: WaterMoonlet
            // [11:45:06.715][1][INFO][PLib / Heliconia] STRINGS.WORLDS.REGOLITHMOONLET.NAME: RegolithMoonlet
            if (name.Contains("TUNDRA")) return AsteroidType.TundraMoonlet;
            if (name.Contains("MARSHY")) return AsteroidType.MarshyMoonlet;
            if (name.Contains("NIOBIUM")) return AsteroidType.NiobiumMoonlet;
            if (name.Contains("WATER")) return AsteroidType.WaterMoonlet;
            if (name.Contains("REGOLITH")) return AsteroidType.RegolithMoonlet;
            if (name.Contains("MOOMOON")) return AsteroidType.MooMoonlet;

            return AsteroidType.Unknown;
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
