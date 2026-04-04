using Newtonsoft.Json;
using PeterHan.PLib.Options;

namespace NotZeroK
{
    [JsonObject(MemberSerialization.OptIn)]
    [ModInfo("https://github.com/DolphinWing/KleiWork/tree/master/workshop-3413401611")]
    class NotZeroOptions
    {
        [Option("NotZeroK.WorldConstants.MAP_MODE", "NotZeroK.WorldConstants.MAP_MODE_DESC")]
        [JsonProperty]
        public MapMode Mode { get; set; }

#if ENABLE_INSTANT_MODE
        [Option("Instant Mode", "You will run out of time!")]
        [JsonProperty]
        public bool InstantMode { get; set; }
#endif // ENABLE_INSTANT_MODE

        [Option("NotZeroK.WorldConstants.SHELTER", "NotZeroK.WorldConstants.SHELTER_DESC")]
        [JsonProperty]
        public bool Shelter { get; set; }

        [Option("NotZeroK.WorldConstants.CRITTER", "NotZeroK.WorldConstants.CRITTER_DESC")]
        [JsonProperty]
        public bool Critter { get; set; }

        [Option("NotZeroK.WorldConstants.CRITTER_SAVE", "NotZeroK.WorldConstants.CRITTER_SAVE_DESC")]
        [JsonProperty]
        public bool SpawnAll { get; set; }

        public NotZeroOptions()
        {
            Mode = MapMode.Balanced; // TerrainCell_GetTemperatureRange_Patch
            Shelter = true; // MutatedWorldData_Constructor_Patch
            Critter = true; // MutatedWorldData_Constructor_Patch
            SpawnAll = true; // ClusterPOIManager_RegisterTemporalTear_Patch

#if ENABLE_INSTANT_MODE
            InstantMode = true; // TerrainCell_ApplyBackground_Patch
#endif // ENABLE_INSTANT_MODE
        }

        public enum MapMode
        {
            [Option("NotZeroK.WorldConstants.MAP_MODE_BALANCED", "NotZeroK.WorldConstants.MAP_MODE_BALANCED_DESC")]
            Balanced,

            [Option("NotZeroK.WorldConstants.MAP_MODE_EASY", "NotZeroK.WorldConstants.MAP_MODE_EASY_DESC")]
            Easy,

            [Option("NotZeroK.WorldConstants.MAP_MODE_CRAZY", "NotZeroK.WorldConstants.MAP_MODE_CRAZY_DESC")]
            Crazy
        }
    }
}
