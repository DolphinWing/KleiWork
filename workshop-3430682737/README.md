# Lore 故事背景

> [English Version](#voidria---eng---lore) | [中文版](#voidria---cht---lore)

### 🛑 System Boot Sequence... Initiated
### ⚠️ Loading Planetary Log... Corrupted

---

<a name="voidria---eng---lore"></a>

## Voidria - The Fractured Cluster

**Log Entry: Cycle 1**
My systems have reactivated on **Voidria**.

Sensor readouts confirm extreme environmental anomalies:
* **Reality Boundaries**: Most sectors are hard vacuum. The surrounding rock faces exhibit unnatural **geometric flatness**, as if matter and space were violently truncated here, leaving behind an inexplicable void.
* **Resource Index Error**: Scanner signals are highly unstable. No active geyser signatures detected on surface level.
* **Bio-Isolation**: Flora and fauna templates are isolated on floating islands, resembling independent test units forgotten in the void, disconnected from one another.

**WARNING: Critical Database Corruption.**

The system executed a **Disaster Recovery** procedure on residual sectors. While some fragmented logs were retrieved, they are logically inconsistent and fail to match:

> **[Recovered Segment @0x5F_2A1A - Lab Log]**
> ```log
> "...Project Ark... Isolation unit deployed... Vacuum stress test... Protocol forced..."
> ```

> **[Recovered Segment @0x9E_04CB - Audio Transcript]**
> ```log
> "...Warning! Gravitational Anchor [STATIC]... Failed! Repeat... Field containment lost...[DATA INTERRUPTED]..."
> "...It's not working! [STATIC] reaction unstoppable... Tectonic plates are [STATIC]... Oh god, we are drifting into ..."
> "...Structure disintegrating... Evacuate..."
> ```

> **[Recovered Segment @0x11_8F3C - Cargo Manifest]**
> ```log
> "...Valuation complete... High-value extraction 99%... Residuals deemed non-viable... Initiating orbital jettison..."
> ```

> **[SYSTEM FATAL ERROR @0x00_DEAD]**
> ```log
> [SYS_WARN] Heap fragmentation critical. Available physical memory < 1%.
> [ERR] Allocation failed for object 'Terrain_Chunk_04'. Request denied.
> [ERR] WorldGen: Topology generation aborted at coordinate [NaN, NaN].
> [FATAL] System.OutOfMemoryException: The simulation has exceeded the allocated bounds.
>    at WorldBuilder.ConstructTopology()
>    at Sim.Physics.ComputeGravity()
>    at Core.Bootstrap.Initia... <DATA_STREAM_INTERRUPTED>
> ```

**ALERT: Life Support Systems Countdown.**

Error analysis skipped. No time to process historical data.
Every unit of oxygen and power is depleting rapidly.

**Directive:**
Immediately inventory all available resources and environmental data.

Duplicants will be assigned to prioritize exploration of surrounding debris, searching for any assets not purged by the system.

> **Note: Command Override**
> 
> Standard "Personnel Safety Protocols" suspended.
> Individual survival rates no longer limit command decisions—to ensure core function, any form of sacrifice is hereby permitted.

---

<a name="voidria---cht---lore"></a>

## Voidria - 無多利亞 - 破碎星團

**日誌條目：第 1 周期**

我的系統已在**無多利亞**重新上線。

感測器回報顯示環境極端異常：
* **現實邊界**：絕大多數區域呈現真空狀態。周遭的岩石斷面呈現出不自然的**幾何平整**，物質與空間彷彿在這裡被暴力截斷，留下了無法解釋的虛無。
* **資源索引錯誤**：掃描器訊號極度不穩定，地表偵測不到任何活躍的間歇泉訊號。
* **生態隔離**：動植物樣本被孤立在漂浮島嶼上，如同被遺忘在虛空中的獨立測試單元，彼此互不相連。

**警告：資料庫完整性嚴重受損。**

系統對底層殘存磁區執行了**災難復原**程序。雖然成功檢索出部分破碎的日誌片段，但這些內容彼此邏輯矛盾，無法匹配：

> **【修復片段 @0x5F_2A1A - 實驗日誌】**
> ```log
> 「...樣本方舟計畫...隔離區塊部署確認...真空環境極限測試...協議強制執行中...」
> ```

> **【修復片段 @0x9E_04CB - 音訊轉錄】**
> ```log
> 「...警告！引力錨[雜訊]...失效！重複...力場無法抑制...[數據中斷]」
> 「...不行！[雜訊]反應停不下來...地殼板塊正在[雜訊]...天啊，我們正在飄進...」
> 「...結構解體中...快撤離...」
> ```

> **【修復片段 @0x11_8F3C - 貨運清單】**
> ```log
> 「...價值評估完畢...高價值目標提取率 99%...剩餘殘渣判定無效益...執行軌道拋棄...」
> ```

> **【系統嚴重錯誤 @0x00_DEAD】**
> ```log
> [SYS_WARN] Heap fragmentation critical. Available physical memory < 1%.
> [ERR] Allocation failed for object 'Terrain_Chunk_04'. Request denied.
> [ERR] WorldGen: Topology generation aborted at coordinate [NaN, NaN].
> [FATAL] System.OutOfMemoryException: The simulation has exceeded the allocated bounds.
>    at WorldBuilder.ConstructTopology()
>    at Sim.Physics.ComputeGravity()
>    at Core.Bootstrap.Initia... <DATA_STREAM_INTERRUPTED>
> ```

**警報：維生系統倒數中**

錯誤分析已略過。當前沒有時間處理歷史資料。
每一單位的氧氣與電力都在急劇消耗。

**指令：**
盡速清點周遭可利用的資源與環境潛在資訊。

複製人將被指派優先探索周邊岩石塊，搜尋任何未被系統刪除的物資。

> **備註：指令覆寫**
> 
> 已暫停標準「人員安全保護協定」。
> 個體存活率將不再限制指揮決策——為了確保核心運作，任何形式的犧牲皆已被允許。

---
---

# Developer's Note

The worldgen is inspired by SkyBlock Survival, and supports the flora and fauna ecosystem of all the DLCs.

The amount of survival supply resources, as well as the generation of a oil field and a iron volcano, can be adjusted in the mod options. There are even more options for you to challenge.

此星球生成啟發自空島生存，且支援所有 DLC 的動植物生態系。

模組選項中可以調整生存補給資源量，與油田和鐵火山的生成等更多挑戰選項。
