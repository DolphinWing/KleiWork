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
*   **The Fix**: Use **`subworldTemplateRules`** inside a dedicated Subworld file.
    *   Create a "Shelter Zone" subworld with high `density` and moderate `avoidRadius`.
    *   Define the template inside the subworld's YAML:
        ```yaml
        subworldTemplateRules:
          - names: [ "expansion1::bases/MyShelter" ]
            listRule: GuaranteeOne
            priority: 500
        ```
    *   This guarantees placement because the subworld effectively "reserves" the space specifically for the template during generation.

### 6.2 The `centralFeature` Limitation
*   **Limitation**: `centralFeature` expects a `Feature` type geometry definition, NOT a direct Template path. While some versions support `type: Template`, it is less reliable than `subworldTemplateRules`.
*   **Modding Control**: `centralFeature` is protected in C#. To modify it via Harmony (e.g., for toggle options), use **Reflection** or **Traverse** (`traverse.Property("centralFeature").SetValue(null)`).

### 6.3 DLC Biome Referencing (`expansion1::` Prefix Trap)
*   **The Trap**: When defining a Subworld in the DLC folder, one might assume Biome references need the `expansion1::` prefix (e.g., `name: expansion1::biomes/MyBiome`).
*   **The Reality**: **DO NOT** use the prefix for Biome names inside Subworld files.
    *   Correct: `name: biomes/MyMod/MyBiome`
    *   Incorrect: `name: expansion1::biomes/MyMod/MyBiome` (Causes "Assert failed: Node didn't get assigned a biome" crash).
    *   *Reason*: The prefix is only for file loading paths (in `subworldFiles`), not for the internal ID key used by the generator once loaded.

### 6.4 Noise & Core Layering
*   **Tilted Layers**: `noise/subworldFrozen` uses `RotatePoint: 60` and extreme `Scale2d` to create diagonal bands. This is superior for holding liquids (Magma/Molten Metal) compared to pockets or vertical flows.
*   **Biome Element Order**: The **first element** in a Biome list often dominates the generation if noise values are skewed low.
    *   *Strategy*: Place the primary visual element (e.g., Iron) first, followed by the structural element (e.g., Obsidian).
    *   *Shielding*: To create a heat shield, use a distinct band (0.15~0.3) of `Katairite` (Abyssalite) in the biome definition.

