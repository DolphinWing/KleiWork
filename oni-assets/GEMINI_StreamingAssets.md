# ONI World Generation Architecture Report

**Author**: Brigette Aurora  
**Date**: 2026-02-04  
**Context**: Oxygen Not Included World Gen Assets Analysis & Implementation Lessons

## 1. System Overview

ONI world generation is a multi-layered procedural system based on **Noise Fields** (terrain shape) and **Template Injection** (buildings/features).

**Core Data Flow:**  
`Cluster` (Star Map) -> `World` (Asteroid) -> `Subworld` (Zone) -> `Biome` (Material) & `Feature` (Terrain)

---

## 2. Directory Map

*   **`worldgen/`**: Core definitions.
    *   `worlds/`: Asteroid rules, sizing, and zone placement (Rings).
    *   `clusters/`: DLC star cluster layouts and world placements.
    *   `subworlds/`: Zone parameters (Temperature, Density, Biome selection).
    *   `biomes/`: Material brushes (Element distribution and ratios).
    *   `features/`: Natural terrain features (Geodes, Lakes, Boulders).
    *   `noise/`: Mathematical noise parameters for terrain shape.
    *   `borders.yaml`: Mapping of border tags to specific elements.
    *   `temperatures.yaml`: Mapping of temperature range names to Kelvin values.
*   **`templates/`**: Prefabs for POIs, Geysers, and Bases.
*   **`elements/`**: Material physical properties (Solid/Liquid/Gas).

---

## 3. Implementation Lessons (Critical Knowledge)

### 3.1 Border System (`borderOverride`)
*   **The Trap**: `borderOverride` in a Subworld file does **NOT** accept Element IDs directly.
*   **The Fix**: It accepts **Tags** defined in `worldgen/borders.yaml`.
*   **Common Tags**: `rocky` (Granite/Igneous), `hardToDig` (Abyssalite), `softRock` (Sedimentary/Mafic), `NONE`.

### 3.2 Liquid Generation (`massOverride`)
*   **The Trap**: Procedural liquids often generate at low mass, appearing as small droplets.
*   **The Fix**: Use `massOverride` in the Biome definition close to the element's `maxMass`.

### 3.3 Noise Directionality & Scaling
*   **Rotation**: Use `transformerType: RotatePoint` with `vector: Y: 90` to reliably change flow direction.
*   **Frequency**: High Frequency (0.3+) = small/dense. Low Frequency (<0.1) = large/sweeping.

### 3.4 World Rules & Template Logic (`listRule`)
`listRule` defines how templates are selected from the `names` list.

*   **`GuaranteeOne`**: Randomly picks one from the list and ensures it spawns successfully.
*   **`GuaranteeSome`**: Ensures `someCount` items from the list spawn.
*   **`GuaranteeAll`**: Ensures every item in the list spawns.
*   **`TryOne`**: Tries to spawn one item once. If it fails (due to overlap), it does not retry.
*   **`GuaranteeSomeTryMore`**: Combines a minimum guarantee (`someCount`) with optional extras (`moreCount`).
*   **Parameters**:
    *   `times`: Number of times the rule is executed.
    *   `priority`: Higher numbers spawn first. Use `100+` for guaranteed POIs.
    *   `useRelaxedFiltering`: Reduces spawning restrictions (e.g., temperature) to increase success rate.

---

## 4. Reference: zoneType List

`zoneType` determines background art, ambience, and POI allowed rules.

### 4.1 Base Game (Universal)
*   **`Sandstone`**: Start area, mild blue/yellow tone.
*   **`Forest`**: Greenery, nature ambience.
*   **`ToxicJungle`**: Purple, spore visual effects.
*   **`BoggyMarsh`**: Yellow-green, slimy feel.
*   **`MagmaCore`**: Red, heat warning ambience.
*   **`OilField`**: Dark/Black background with oil droplets.
*   **`Space`**: Black background, starry sky.
*   **`FrozenWastes`**: Blue/White, cold wind ambience.
*   **`Rust`**: Orange/Red, metallic feel.
*   **`Ocean`**: Blue, underwater ambience.
*   **`Barren`**: Grey/Brown rock, dry feel.

### 4.2 DLC (Expansion1) Exclusive
*   **`Wasteland`**: Orange/Yellow, radioactive particle effects.
*   **`Radioactive`**: Neon green/yellow, glowing feel.
*   **`Swamp`**: Weird slug swamp style.
*   **`Niobium`**: Deep purple/magenta, extreme heat feel.
*   **`Regolith`**: Grey dust.
*   **`Metallic`**: Shiny, mechanical debris background.
*   **`Moo`**: Pink/Fleshy.

---

## 5. Reference: Noise List

`biomeNoise` determines the shape of the terrain caves and solid blocks.

### 5.1 Base Game (`worldgen/noise/`)
*   **`noise/Default`**: Balanced mix of caves and solids. Standard terrain.
*   **`noise/DefaultCave`**: Classic winding cave systems.
*   **`noise/OilPockets`**: Large, isolated circular pockets enclosed in solid. (Low Frequency).
*   **`noise/MagmaVent`**: Vertical flow structures. Good for rising heat columns.
*   **`noise/Twisty`**: Highly winding, thin, vein-like tunnels. High complexity.
*   **`noise/SandstoneStart`**: Blocky, rectangular platform-style blocks.
*   **`noise/subworldJungle`**: **Voronoi** cellular structure. Organic, irregular chambers.
*   **`noise/subworldRust`**: Flattened (squashed) horizontal pockets. Layered look.

### 5.2 DLC Exclusive (`dlc/expansion1/worldgen/noise/`)
*   **`expansion1::noise/SmallCaves`**: Frequent, small isolated holes. Sponge-like.
*   **`expansion1::noise/LargeCaves`**: Massive open-air caverns.
*   **`expansion1::noise/Sponge`**: Extremely porous and broken. Swiss-cheese look.
*   **`expansion1::noise/Swamp`**: Irregular, thick organic-looking chunks.
*   **`expansion1::noise/Metallic`**: Sharp, flattened, layered geometry.
*   **`expansion1::noise/Radioactive`**: High-detail, rough edges (sinusoidal fractal).

---

## 6. Advanced Implementation Lessons (2026 Update)

### 6.1 Template Placement: Global vs. Local
*   **The Problem**: Using global `worldTemplateRules` (in `worlds/`) for critical structures (like Start Shelters) often fails in crowded rings due to lack of valid contiguous space or noise fragmentation.
*   **The Fix**: Use **`tag`** inside a dedicated Subworld file.
    *   Create a "Shelter Zone" subworld with high `density` and moderate `avoidRadius`. Define a special tag `HCA_Shelter` for this subworld.
    *   Define the poi inside the world's YAML :
        ```yaml
  - names:
      - expansion1::bases/HcaShelterSO
    listRule: GuaranteeOne
    ruleId: hca_shelter
    priority: 500
    allowedCellsFilter:
      - command: Replace
        tagcommand: AtTag
        tag: HCA_Shelter
        ```
    *   This guarantees placement because the subworld effectively "reserves" the space specifically for the template during generation. (Put the POI directly in the dedicated subworld.)

### 6.2 The `centralFeature` Limitation
*   **Limitation**: `centralFeature` expects a `Feature` type geometry definition, NOT a direct Template path. While some versions support `type: Template`, it is less reliable than `subworldTemplateRules`.
*   **Modding Control**: `centralFeature` is protected in C#. To modify it via Harmony (e.g., for toggle options), use **Reflection** or **Traverse** (`traverse.Property("centralFeature").SetValue(null)`).

### 6.3 DLC Biome Referencing (The `expansion1::` Prefix Requirement)
*   **The Rule**: When defining a Subworld inside the DLC (`expansion1`) directory, all Biome references defined within the DLC's `biomes/*.yaml` **MUST** include the `expansion1::` prefix.
*   **The Reality**: If the prefix is missing, the game engine will fail to resolve the path relative to the `worldgen` root, leading to "Node didn't get assigned a biome" or file-not-found errors.
    *   **Correct**: `name: expansion1::biomes/MyMod/MyBiome`
    *   **Incorrect**: `name: biomes/MyMod/MyBiome`
    *   *Note*: This applies to all assets (Templates, Noise, Features) referenced within DLC-specific YAML files.

### 6.4 Noise & Core Layering
*   **Tilted Layers**: `noise/subworldFrozen` uses `RotatePoint: 60` and extreme `Scale2d` to create diagonal bands. This is superior for holding liquids (Magma/Molten Metal) compared to pockets or vertical flows.
*   **Biome Element Order**: The **first element** in a Biome list often dominates the generation if noise values are skewed low.
    *   *Strategy*: Place the primary visual element (e.g., Iron) first, followed by the structural element (e.g., Obsidian).
    *   *Shielding*: To create a heat shield, use a distinct band (0.15~0.3) of `Katairite` (Abyssalite) in the biome definition.

### 6.5 Horizontal Layering Design
*   **Concept**: To create a "sedimentary" or "compressed core" look, noise must be extremely stretched along the X-axis and compressed on the Y-axis.
*   **Formula**:
    *   `modifyType: Scale2d` with `X: 5.0 - 20.0` and `Y: 0.1 - 0.5`.
    *   Must be followed by `transformerType: RotatePoint` with `vector: Y: 90` to align the stretched bands horizontally.
*   **Frequency Impact**: Higher frequency (0.8+) creates thinner, more frequent "veins" of metal, perfect for high-mass elements (`massOverride: 4000`) where you want visual density without excessive resource clusters.

### 6.6 Diagnosing World Gaps (The "Vacuum Triangle" Problem)
*   **Observation**: Regular geometric (triangular/trapezoidal) gaps showing background vacuum or flat colors.
*   **Cause**: **Subworld Allocation Failure**. The Voronoi diagram failed to assign a subworld to that specific coordinate because sample points (Seeds) were too sparse or `avoidRadius` was too large for the available space.
*   **The Fixes**:
    1.  **Fallback Subworld**: Add a generic subworld (e.g., `subworlds/magma/Bottom`) to the `AtDepths` or relevant layer in `worlds/*.yaml`'s `unknownCellsAllowedSubworlds` list. This acts as a "canvas" if specific subworlds fail to claim space.
    2.  **Density Tuning**: Lower `avoidRadius` (to 15.0 - 20.0) and ensure `density` is moderate (30 - 50). Avoid extreme values like `density: 80` + `radius: 30`.
    3.  **`minChildCount`**: Set to at least 4-8 to force the subworld to branch out and occupy more space.

### 6.7 POI-Friendly Noise Architecture
*   **The Conflict**: Complex, fragmented noise (like high-frequency fractal) breaks subworlds into small pieces, preventing large rectangular Templates (e.g., 12x6 POIs) from finding valid placement space.
*   **The Solution**: Use a low-frequency, highly stretched noise (X: 20+, Y: 0.1) specifically for "container" subworlds. This creates broad, flat "runways" that maximize the success rate of horizontal POI generation.
*   **Rule of Thumb**: Template size should never exceed 50% of the typical subworld height/width defined by the noise scale.

### 6.9 Template vs. Feature 混用策略 (Hybrid Generation)
*   **Template (POI)**: 用於建立具備**結構性**的視覺焦點（如包含底座、牆壁、液體的多層熔爐）。適合固定尺寸、人造感強的物件。
*   **Feature**: 用於建立**自然有機**的細節（如隨機形狀的液體坑、礦脈）。
    *   *優點*: 使用 `shape: Blob` 可以產生比 POI 更自然的邊緣。
    *   *組合策略*: 在核心區域使用高 Priority 的大型 POI (12x6 或 8x3) 作為地標，並搭配大量的低 Priority 小型 Feature (blobSize: 1.5~2.5) 填充背景空間。
    *   *成果*: 此舉能同時解決地圖過於「方正」的問題，並在保持資源密度的同時大幅提升視覺上的破碎感與探索樂趣。

### 6.10 Feature 的液體控制
*   在固體核心（如 Obsidian/Katairite）中使用液體 Feature 時，Feature 會自動替換原有固體格，形成自然的液體口袋。
*   **Mass & Temp**: 務必在 Feature 的 `overrides` 中顯式定義 `massOverride` 與 `temperatureOverride`，否則生成的液體可能會因為預設值過低而顯得稀薄，或因溫度不匹配造成地圖生成後的熱震盪。

### 6.11 Subworld 防護標籤 (Protective Tags) —— 真空缺口的關鍵解法
*   **核心發現**: 地圖生成中出現的幾何三角形或梯形「真空缺口」，通常是因為全域特質 (World Traits) 試圖在核心層挖洞或撒點，但與自訂 Subworld 衝突導致生成失敗。
*   **解決方案**: 必須在 Subworld 的 `tags` 列表裡加上防護標籤來「防禦」這些全域規則。
*   **`IgnoreCaveOverride`**: 防止程序化洞穴生成器在實心核心裡挖洞，是消除不規則真空塊的主力。
*   **`NoGlobalFeatureSpawning`**: 禁止全域 Feature（如隨機晶洞、火山）在區域內生成，確保水平分層不被切斷。
*   **`NoGravitasFeatures`**: 禁止生成 Gravitas 設施殘骸。
*   **結論**: **這三者是維持高密度核心層視覺完整性的唯一解法**，設定正確後即可解決 90% 以上的區域遺失問題。

### 6.12 Template 實體序列化規則
*   **報錯診斷**: 若出現 `Requested value 'X' was not found during deserialization`，通常是因為將實體 ID 放錯了列表。
*   **規則**: 
    *   `cells`: 僅用於地塊元素。
    *   `buildings`: 用於建築物。
    *   **`otherEntities`**: 所有植物 (EvilFlower)、動物、間歇泉或特殊物件必須放在此列表中，且需補齊 `units`, `rottable: {}`, `amounts: []`, `storage: []` 等屬性。

