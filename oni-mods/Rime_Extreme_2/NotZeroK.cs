using HarmonyLib;
using Klei.CustomSettings;
using PeterHan.PLib.Core;
using PeterHan.PLib.Database;
using PeterHan.PLib.Options;
using PeterHan.PLib.PatchManager;
using ProcGen;
using ProcGenGame;
using System;
using System.Collections.Generic;
using System.Reflection;
using System.Reflection.Emit;

namespace NotZeroK
{
    class NotZeroK : KMod.UserMod2
    {
        public static bool IsMe()
        {
            SettingLevel current = CustomGameSettings.Instance.GetCurrentQualitySetting((SettingConfig)CustomGameSettingConfigs.ClusterLayout);
            if (current == null) return false; // unknown cluster

            ClusterLayout clusterData = SettingsCache.clusterLayouts.GetClusterData(current.id);
            //Debug.Log("Not0K " + clusterData.GetCoordinatePrefix());
            string prefix = clusterData.GetCoordinatePrefix();
            return IsAbzModded(prefix); // we only cares about ABZ
        }

        private static bool IsAbzModded(string prefix)
        {
            if (prefix.StartsWith("ABZ-TG")) return true; // base game and spaced out classic style
            if (prefix.StartsWith("M-ABZ-TG")) return true; // spaced out style
            return false;
        }

        public static bool IsMyWorld(ProcGen.World world)
        {
            return world != null && world.name.StartsWith("NotZeroK.WorldConstants");
        }

        public const Temperature.Range TG_AbsoluteZero = (Temperature.Range)30; // not to clash with other mods
        public const Temperature.Range TG_SuperCold = (Temperature.Range)29; // not to clash with other mods

        public override void OnLoad(Harmony harmony)
        {
            base.OnLoad(harmony);
            PUtil.InitLibrary();
            new PLocalization().Register();
            new POptions().RegisterOptions(this, typeof(NotZeroOptions));
            new PPatchManager(harmony).RegisterPatchClass(typeof(NotZeroK));

            // refs: Baator
            AddHashToTable(TG_AbsoluteZero, "AbsoluteZero");
            AddHashToTable(TG_SuperCold, "SuperCold");
        }

        /// <summary>
		/// Registers the strings used in this mod.
		/// </summary>
		[PLibMethod(RunAt.AfterDbInit)]
        internal static void InitStrings()
        {
            Strings.Add("Not 0k, But Pretty Cool Place worldgen", WorldConstants.MOD_NAME);
            Strings.Add("Pretty Cool. But 0K Not Included", WorldConstants.MOD_DESC);
        }

        [HarmonyPatch(typeof(Enum), "ToString", new Type[] { })]
        public static class Temperatures_ToString_Patch
        {
            internal static Dictionary<Temperature.Range, string> temperatureTable = new Dictionary<Temperature.Range, string>();

            public static bool Prefix(ref Enum __instance, ref string __result) =>
                !(__instance is Temperature.Range) ||
                !temperatureTable.TryGetValue((Temperature.Range)__instance, out __result);
        }

        [HarmonyPatch(typeof(Enum), "Parse", new Type[] { typeof(Type), typeof(string), typeof(bool) })]
        public static class Temperatures_Parse_Patch
        {
            internal static Dictionary<string, object> temperatureReverseTable = new Dictionary<string, object>();

            public static bool Prefix(Type enumType, string value, ref object __result) =>
                !enumType.Equals(typeof(Temperature.Range)) ||
                !temperatureReverseTable.TryGetValue(value, out __result);
        }

        private static void AddHashToTable(Temperature.Range hash, string id)
        {
            Temperatures_ToString_Patch.temperatureTable.Add(hash, id);
            Temperatures_Parse_Patch.temperatureReverseTable.Add(id, (object)hash);
        }

        /// <summary>
        /// Retrieves the "minimum" temperature of an element on stock worlds. However, on
        /// 100 K, returns 1 K to disable the check.
        /// </summary>
        /// <param name="element">The element to look up.</param>
        /// <param name="worldGen">The currently generating world.</param>
        /// <returns>The minimum temperature to be used for world gen.</returns>
        private static float GetMinTemperature(Element element, WorldGen worldGen)
        {
            var world = worldGen?.Settings?.world;
            return NotZeroK.IsMyWorld(world) ? 1.0f : element.lowTemp;
        }

        // refs: https://github.com/peterhaneve/ONIMods/blob/main/Challenge100K/Challenge100K.cs
        /// <summary>
        /// Applied to TerrainCell to allow elements to be spawned in at lower than their
        /// normal transition temperature (and thus instantly freeze).
        /// </summary>
        [HarmonyPatch(typeof(TerrainCell), "ApplyBackground")]
        public static class TerrainCell_ApplyBackground_Patch
        {
            internal static IEnumerable<CodeInstruction> Transpiler(
                    IEnumerable<CodeInstruction> method)
            {
                var target = typeof(Element).GetFieldSafe(nameof(Element.lowTemp), false);
                var replacement = typeof(NotZeroK).GetMethodSafe(nameof(
                    GetMinTemperature), true, typeof(Element), typeof(WorldGen));
                foreach (var instruction in method)
                    if (instruction.opcode == OpCodes.Ldfld &&
                        target != null && target == (FieldInfo)instruction.operand)
                    {
                        // With the Element on the stack, push the WorldGen (first arg)
                        yield return new CodeInstruction(OpCodes.Ldarg_1);
                        // Replacement for "Element.lowTemp"
                        yield return new CodeInstruction(OpCodes.Call, replacement);
                    }
                    else
                        yield return instruction;
            }
        }

        // refs: https://github.com/peterhaneve/ONIMods/blob/main/Challenge100K/Challenge100K.cs
        /// <summary>
        /// Applied to TerrainCell to "fix" the temperature range of everything.
        /// </summary>
        [HarmonyPatch(typeof(TerrainCell), "GetTemperatureRange", typeof(WorldGen))]
        public static class TerrainCell_GetTemperatureRange_Patch
        {
            private static Temperature.Range GetStartingBiomeTemperature(Temperature.Range temp)
            {
                var options = POptions.ReadSettings<NotZeroOptions>();
                NotZeroOptions.MapMode mode = options != null ? options.Mode : NotZeroOptions.MapMode.Balanced;
                if (temp == Temperature.Range.Mild)
                {
                    // starting biome keeps dupes survival
                    switch (mode)
                    {
                        case NotZeroOptions.MapMode.Crazy:
                            return TG_SuperCold;
                        case NotZeroOptions.MapMode.Easy:
                            return temp;
                        default:
                            return Temperature.Range.VeryCold;
                    }
                }
                return mode == NotZeroOptions.MapMode.Crazy ? TG_SuperCold : temp; // Override temp
            }

            /// <summary>
            /// Applied after GetTemperatureRange runs.
            /// </summary>
            internal static void Postfix(WorldGen worldGen, ref Temperature.Range __result)
            {
                if (worldGen.Settings == null) return; // no need to check it

                if (NotZeroK.IsMe() == false) return; // we only cares about ABZ

                var world = worldGen.Settings?.world;
                if (world == null) return; // unknown world

                var temp = __result; // override all temperatures
                if (temp >= Temperature.Range.ExtremelyCold && temp <= Temperature.Range.ExtremelyHot)
                {
                    if (worldGen.isStartingWorld || NotZeroK.IsMyWorld(world))
                    {
                        __result = GetStartingBiomeTemperature(temp);
                    }
                    else
                    {
                        //PUtil.LogDebug("override Temperature.Range." + temp);
                        __result = TG_AbsoluteZero; // Override temp
                    }
                }
            }
        }

        /// <summary>
        /// Applied to MutatedWorldData() to manipulate POIs.
        /// </summary>
        [HarmonyPatch(typeof(MutatedWorldData), MethodType.Constructor, typeof(ProcGen.World),
            typeof(List<WorldTrait>), typeof(List<WorldTrait>))]
        public static class MutatedWorldData_Constructor_Patch
        {
            internal static void Postfix(MutatedWorldData __instance)
            {
                var options = POptions.ReadSettings<NotZeroOptions>();
                if (options == null)
                {
                    //return; // no need to change anything
                    options = new NotZeroOptions();
                }

                var world = __instance.world;
                if (NotZeroK.IsMyWorld(world) == false) return; // don't bother

                var dlcMixing = CustomGameSettings.Instance.GetCurrentDlcMixingIds();
                var frosty = dlcMixing.Contains(DlcManager.DLC2_ID);
                var history = dlcMixing.Contains(DlcManager.DLC4_ID);
                PUtil.LogDebug("DLC mixing: 2=" + frosty + ", 4=" + history);

                var removing = new List<ProcGen.World.TemplateSpawnRules>();
                if (world.worldTemplateRules != null)
                    foreach (var rule in world.worldTemplateRules)
                    {
                        if (rule.ruleId?.StartsWith("abz_shelter") == true)
                        {
                            PUtil.LogDebug("... checking " + rule.ruleId);
                            if (options.Shelter == false) removing.Add(rule);
                        }
                        if (rule.ruleId?.StartsWith("abz_critter") == true)
                        {
                            PUtil.LogDebug("... checking " + rule.ruleId);
                            if (options.Critter == false)
                                removing.Add(rule);
                            else
                            {
                                if (frosty)
                                {
                                    rule.names.Add("dlc2::critters/tg_bammoth");
                                    rule.names.Add("dlc2::critters/tg_flox");
                                    rule.names.Add("dlc2::critters/tg_sugar_bug_seagul");
                                    PUtil.LogDebug("... add frosty caves");
                                }

                                if (history)
                                {
                                    rule.names.Add("dlc4::critters/pp_jawbo_pool");
                                    rule.names.Add("dlc4::critters/pp_rhex_dartle");
                                    rule.names.Add("dlc4::critters/pp_mos_lure");
                                    rule.names.Add("dlc4::critters/pp_fly_lumb_ovagro");
                                }
                            }
                        }
                    }

                if (removing.Count > 0) // remove them from list
                    foreach (var rule in removing)
                    {
                        world.worldTemplateRules?.Remove(rule);
                    }
            }
        }

        [HarmonyPatch(typeof(ClusterPOIManager), "RegisterTemporalTear")]
        public static class ClusterPOIManager_RegisterTemporalTear_Patch
        {
            public static void Postfix(TemporalTear temporalTear, ClusterPOIManager __instance)
            {
                // PUtil.LogDebug("ClusterPOIManager_RegisterTemporalTear_Patch.Postfix");

                if (NotZeroK.IsMe() == false) return; // we only cares about ABZ

                var options = POptions.ReadSettings<NotZeroOptions>() ?? new NotZeroOptions();
                if (options.Critter)
                {
                    PUtil.LogDebug("Spawn all to save all backwalls devs need.");
                    SaveGame.Instance.worldGenSpawner.SpawnEverything();
                }
            }
        }
    }
}
