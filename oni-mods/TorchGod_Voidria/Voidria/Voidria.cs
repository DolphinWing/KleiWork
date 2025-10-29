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

        /// <summary>
        /// Applied to MutatedWorldData() to remove all geysers on hard mode on 100 K.
        /// </summary>
        [HarmonyPatch(typeof(MutatedWorldData), MethodType.Constructor, typeof(ProcGen.World),
            typeof(List<WorldTrait>), typeof(List<WorldTrait>))]
        public static class MutatedWorldData_Constructor_Patch
        {
            /// <summary>
            /// Applied after the constructor runs.
            /// </summary>
            internal static void Postfix(MutatedWorldData __instance)
            {
                var world = __instance.world;
                if (world.name.StartsWith("Voidria.Voidria.") == false) return; // no need to check further
                //PUtil.LogDebug("Checking for " + world.name);

                var options = VoidriaOptions.GetInstance();
                //if (options == null) return; // no need to change anything

                var spaced = DlcManager.IsContentSubscribed(DlcManager.EXPANSION1_ID);
                var frosty = DlcManager.IsContentSubscribed(DlcManager.DLC2_ID);
                var bionic = DlcManager.IsContentSubscribed(DlcManager.DLC3_ID);
                var history = DlcManager.IsContentSubscribed(DlcManager.DLC4_ID);
                //PUtil.LogDebug("DLC own: " + spaced + ", " + frosty + ", " + bionic);

                var dlcMixing = CustomGameSettings.Instance.GetCurrentDlcMixingIds();
                frosty = dlcMixing.Contains(DlcManager.DLC2_ID);
                bionic = dlcMixing.Contains(DlcManager.DLC3_ID);
                history = dlcMixing.Contains(DlcManager.DLC4_ID);
                //PUtil.LogDebug("DLC mixing: " + spaced + ", " + frosty + ", " + bionic);

                var stories = CustomGameSettings.Instance.GetCurrentStories();
#if DEBUG
                foreach (var story in stories)
                {
                    PUtil.LogDebug("story: " + story);
                }
#endif

#if DEBUG
                var teleporter = CustomGameSettings.Instance.GetCurrentQualitySetting(CustomGameSettingConfigs.Teleporters);
                PUtil.LogDebug("teleporter: " + teleporter.coordinate_value);
#endif
                var rules = world.worldTemplateRules;
                if (rules != null)
                {
                    List<ProcGen.World.TemplateSpawnRules> removed = new List<ProcGen.World.TemplateSpawnRules>();
                    foreach (var rule in rules)
                    {
#if DEBUG
                        PUtil.LogDebug("==>" + rule.ruleId);
                        var names = rule.names;
                        foreach (var n in names)
                        {
                            PUtil.LogDebug("  " + n);
                        }
#endif

                        if (rule.ruleId?.StartsWith("tg_geyser_molten_iron") == true)
                        {
                            if (options != null && options?.EnableIronVolcano == false)
                            {
                                if (history) 
                                {
                                    // replace molten iron volcano with jawbo gifts
                                    rule.names.Remove("geysers/molten_iron");
                                    rule.names.Add("dlc4::critters/pp_jawbo_gift1");
                                    rule.names.Add("dlc4::critters/pp_jawbo_gift2");
                                }
                                else
                                {
                                    removed.Add(rule);
                                    PUtil.LogDebug("... remove iron volcano");
                                }
                            }
                        }
                        if (rule.names.Contains("poi/oil/small_oilpockets_geyser_a"))
                        {
                            if (options.EnableOilReservoir == false)
                            {
                                removed.Add(rule);
                                PUtil.LogDebug("... remove oil pocket geyser");
                            }
                        }

                        if (rule.ruleId?.StartsWith("tg_Story_") == true)
                        {
                            var ruleId = rule.ruleId.Substring(9);
                            if (stories.Contains(ruleId) == false)
                            {
                                removed.Add(rule);
                                PUtil.LogDebug("... remove " + ruleId);
                            }
                        }

                        if (rule.ruleId?.StartsWith("tg_Critter_") == true)
                        {
                            if (options.EnableCritters == false)
                            {
                                removed.Add(rule);
                                PUtil.LogDebug("... remove " + rule.ruleId);
                            }
                            else if (rule.ruleId?.StartsWith("tg_Critter_Vanilla") == true)
                            {
                                if (frosty)
                                {
                                    PUtil.LogDebug("... add frosty critters");
                                    rule.names.Add("dlc2::critters/tg_flox");
                                    rule.names.Add("dlc2::critters/tg_sugar_bug_seagul");
                                }
                                if (bionic)
                                {
                                    PUtil.LogDebug("... add bionic bases");
                                    rule.names.Add("dlc3::base/bb_remote_dock");
                                    rule.names.Add("dlc3::base/bb_remote_worker");
                                }
                                if (history)
                                {
                                    PUtil.LogDebug("... add prehistory critters");
                                    rule.names.Add("dlc4::critters/pp_jawbo_pool");
                                    rule.names.Add("dlc4::critters/pp_rhex_dartle");
                                    rule.names.Add("dlc4::critters/pp_mos_lure");
                                }
                            }
                            else if (rule.ruleId?.StartsWith("tg_Critter_Meat") == true)
                            {
                                if (frosty)
                                {
                                    // make mole and bammoth closer to the start
                                    rule.names.Add("dlc2::critters/tg_bammoth");
                                }
                                if (history)
                                {
                                    // make lumb and butterfly closer to the start
                                    rule.names.Add("dlc4::critters/pp_fly_lumb_ovagro");
                                }
                            }
                        }

                        if (rule.ruleId?.StartsWith("tg_gift") == true)
                        {
                            if (options.EnableGift == false)
                            {
                                removed.Add(rule);
                                PUtil.LogDebug("... remove " + rule.ruleId);
                            }
                            else if (rule.ruleId?.StartsWith("tg_gift_base") == true)
                            {
                                if (frosty)
                                {
                                    rule.names.Add("dlc2::bases/tg_wood_pile");
                                    PUtil.LogDebug("... add Frosty Wood pile");
                                }
                                else
                                {
                                    rule.names.Add("dlc2::bases/tg_granite");
                                    PUtil.LogDebug("... add Granite for buildings");
                                }
                            }
                        }

                        if (rule.ruleId?.StartsWith("temporalTear_opener") == true)
                        {
                            if (options.EnableTearOpener == false)
                            {
                                removed.Add(rule);
                                PUtil.LogDebug("... remove " + rule.ruleId);
                            }
                        }
                    }

                    if (removed.Count > 0) // remove them from list
                        foreach (var rule in removed)
                        {
                            world.worldTemplateRules?.Remove(rule);
                        }
                }

                var cells = world.unknownCellsAllowedSubworlds;
                if (cells != null)
                {
                    List<ProcGen.World.AllowedCellsFilter> removed = new List<ProcGen.World.AllowedCellsFilter>();
                    foreach (var unknownCell in cells)
                    {
#if DEBUG
                        PUtil.LogDebug("==>" + unknownCell.tag + " " + unknownCell.tagcommand);
                        var names = unknownCell.subworldNames;
                        foreach (var n in names)
                        {
                            PUtil.LogDebug("  " + n);
                        }
#endif
                        // Ring 1
                        if (unknownCell.tagcommand == ProcGen.World.AllowedCellsFilter.TagCommand.DistanceFromTag && 
                            unknownCell.tag == "AtStart" && 
                            unknownCell.minDistance == 1 && unknownCell.maxDistance == 999)
                        {
                            if (options.EnableBackground) 
                            {
                                unknownCell.subworldNames.Remove("subworlds/space/Space");
                            }
                            else
                            {
                                unknownCell.subworldNames.Remove("subworlds/Voidria/voa_SpaceWithBg");
                            }
                        }
                        // Core
                        if (unknownCell.tagcommand == ProcGen.World.AllowedCellsFilter.TagCommand.DistanceFromTag &&
                            unknownCell.tag == "AtDepths" && 
                            unknownCell.minDistance == 0 && unknownCell.maxDistance == 0)
                        {
                            if (options.EnableBackground)
                            {
                                unknownCell.subworldNames.Remove("subworlds/space/Space"); 
                            }
                            else
                            {
                                unknownCell.subworldNames.Remove("subworlds/Voidria/voa_SpaceWithBg");
                            }
                        }
                        // Surface
                        if (unknownCell.tagcommand == ProcGen.World.AllowedCellsFilter.TagCommand.DistanceFromTag &&
                            unknownCell.tag == "AtSurface" &&
                            unknownCell.minDistance == 1 && unknownCell.maxDistance == 1)
                        {
                            if (options.EnableBackground)
                            {
                                unknownCell.subworldNames.Clear();
                                unknownCell.subworldNames.Add("subworlds/space/SpaceNoBorder"); 
                            }
                        }
                    }
                }
            }
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
