using HarmonyLib;
using PeterHan.PLib.Core;
using PeterHan.PLib.Options;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace Heliconia
{
    [HarmonyPatch(typeof(ClusterPOIManager), "RegisterTemporalTear")]
    public static class ClusterPOIManager_RegisterTemporalTear_Patch
    {
        public static void Postfix(TemporalTear temporalTear, ClusterPOIManager __instance)
        {
            // PUtil.LogDebug("ClusterPOIManager_RegisterTemporalTear_Patch.Postfix");

            if (Heliconia.IsHcaCluster() == false) return; // we only cares about ABZ

            var options = HeliconiaOptions.GetInstance();
            if (options.Critter && options.SpawnAll)
            {
                PUtil.LogDebug("Spawn all to save all backwalls devs need.");
                SaveGame.Instance.worldGenSpawner.SpawnEverything();
            }
        }
    }
}
