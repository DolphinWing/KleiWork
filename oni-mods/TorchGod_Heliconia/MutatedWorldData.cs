using HarmonyLib;
using PeterHan.PLib.Core;
using ProcGen;
using System.Collections.Generic;

namespace Heliconia
{
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
            // var type = Heliconia.IdentifyWorld(world);
            // PUtil.LogDebug(world.name + ": " + type);

            if (Heliconia.IsHcaWorld(world) == false) return; // no need to check further
            PUtil.LogDebug("Checking for " + world.name);

            var dlcMixing = CustomGameSettings.Instance.GetCurrentDlcMixingIds();
            var frosty = dlcMixing.Contains(DlcManager.DLC2_ID);
            var history = dlcMixing.Contains(DlcManager.DLC4_ID);
            PUtil.LogDebug("DLC mixing: 2=" + frosty + ", 4=" + history);

            var options = HeliconiaOptions.GetInstance();
            PUtil.LogDebug("Heliconia Shelter=" + options.Shelter + ", Critter=" + options.Critter);
            PUtil.LogDebug("  MapMode=" + options.Mode + ", InstantMode=" + options.InstantMode);

            var removing = new List<ProcGen.World.TemplateSpawnRules>();
            if (world.worldTemplateRules != null)
            {
                foreach (var rule in world.worldTemplateRules)
                {
                    if (rule.ruleId?.StartsWith("hca_shelter") == true)
                    {
                        if (options.Shelter == false)
                        {
                            removing.Add(rule);
                        }
                        else if (options.Mode == HeliconiaOptions.MapMode.TabeYuriko)
                        {
                            var r = false;
                            r |= rule.names.Remove("expansion1::bases/HcaShelterSoR20"); // low ration version
                            r |= rule.names.Remove("expansion1::bases/HcaShelterSoL20"); // low ration version
                            r |= rule.names.Remove("bases/HcaShelterR20"); // low ration version
                            r |= rule.names.Remove("bases/HcaShelterL20"); // low ration version
                            PUtil.LogDebug("... Removing low ration support. RESPECT! " + r);
                        }
                        else
                        {
                            var r = false;
                            r |= rule.names.Remove("expansion1::bases/HcaShelterSoR80"); // extra ration version
                            r |= rule.names.Remove("expansion1::bases/HcaShelterSoL80"); // extra ration version
                            r |= rule.names.Remove("bases/HcaShelterR80"); // extra ration version
                            r |= rule.names.Remove("bases/HcaShelterL80"); // extra ration version
                            PUtil.LogDebug("... Removing high ration support. " + r);
                        }
                    }
                    if (rule.ruleId?.StartsWith("hca_critter") == true)
                    {
                        PUtil.LogDebug("... checking " + rule.ruleId);
                        if (options.Critter == false)
                        {
                            removing.Add(rule);
                        }
                        else if (rule.ruleId?.StartsWith("hca_critters_base") == true) // only in base asteroid
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
                                PUtil.LogDebug("... add history caves");
                            }
                        }
                    }
                }
            }

            if (removing.Count > 0) // remove them from list
            {
                foreach (var rule in removing)
                {
                    world.worldTemplateRules?.Remove(rule);
                    PUtil.LogDebug("Removing " + rule.ruleId);
                }
            }
        }
    }
}
