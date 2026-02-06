using HarmonyLib;
using PeterHan.PLib.Core;
using PeterHan.PLib.Options;
using ProcGen;
using ProcGenGame;
using System;
using System.Collections.Generic;
using System.Reflection;
using System.Reflection.Emit;

namespace Heliconia
{
    class HeliconiaTemperature
    {
        public const Temperature.Range TG_SuperHot = (Temperature.Range)31; // not to clash with other mods
        public const Temperature.Range TG_SuperSuperHot = (Temperature.Range)32; // not to clash with other mods
        public const Temperature.Range TG_ExtremeHot = (Temperature.Range)33; // not to clash with other mods

        public static void AddToHashTable() 
        {
            AddHashToTable(TG_SuperHot, "TG_SuperHot");
            AddHashToTable(TG_SuperSuperHot, "TG_SuperSuperHot");
            AddHashToTable(TG_ExtremeHot, "TG_ExtremeHot");
        }

        private static void AddHashToTable(Temperature.Range hash, string id)
        {
            Temperatures_ToString_Patch.temperatureTable.Add(hash, id);
            Temperatures_Parse_Patch.temperatureReverseTable.Add(id, (object)hash);
        }

        /// <summary>
        /// Retrieves the "minimum" temperature of an element on stock worlds.
        /// </summary>
        /// <param name="element">The element to look up.</param>
        /// <param name="worldGen">The currently generating world.</param>
        /// <returns>The minimum temperature to be used for world gen.</returns>
        private static float GetMinTemperature(Element element, WorldGen worldGen)
        {
            var world = worldGen?.Settings?.world;
            return Heliconia.IsHcaWorld(world) ? 1000.0f : element.lowTemp;
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
                var replacement = typeof(HeliconiaTemperature).GetMethodSafe(nameof(
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

    // refs: https://github.com/peterhaneve/ONIMods/blob/main/Challenge100K/Challenge100K.cs
    /// <summary>
    /// Applied to TerrainCell to "fix" the temperature range of everything.
    /// </summary>
    [HarmonyPatch(typeof(TerrainCell), "GetTemperatureRange", typeof(WorldGen))]
    public static class TerrainCell_GetTemperatureRange_Patch
    {
        private static Temperature.Range GetStartingBiomeTemperature(Temperature.Range temp)
        {
            var options = POptions.ReadSettings<HeliconiaOptions>();
            HeliconiaOptions.MapMode mode = options != null ? options.Mode : HeliconiaOptions.MapMode.Balanced;
            if (temp == Temperature.Range.Mild)
            {
                // starting biome keeps dupes survival
                switch (mode)
                {
                    case HeliconiaOptions.MapMode.Crazy:
                        return HeliconiaTemperature.TG_SuperSuperHot;
                    case HeliconiaOptions.MapMode.Easy:
                        return temp;
                    default:
                        return Temperature.Range.HumanHot;
                }
            }
            return mode == HeliconiaOptions.MapMode.Crazy ? HeliconiaTemperature.TG_SuperSuperHot : temp;
        }

        /// <summary>
        /// Applied after GetTemperatureRange runs.
        /// </summary>
        internal static void Postfix(WorldGen worldGen, ref Temperature.Range __result)
        {
            if (worldGen.Settings == null) return; // no need to check it

            if (Heliconia.IsHcaCluster() == false) return; // we only cares about our clusters

            var world = worldGen.Settings?.world;
            if (world == null) return; // unknown world

            var temp = __result; // override all temperatures
            if (temp >= Temperature.Range.ExtremelyCold && temp <= Temperature.Range.VeryHot)
            {
                if (worldGen.isStartingWorld /*|| NotZeroK.IsMyWorld(world)*/)
                {
                    __result = GetStartingBiomeTemperature(temp);
                }
                else
                {
                    __result = HeliconiaTemperature.TG_SuperSuperHot; // Override temp
                }
            }
        }
    }
}
