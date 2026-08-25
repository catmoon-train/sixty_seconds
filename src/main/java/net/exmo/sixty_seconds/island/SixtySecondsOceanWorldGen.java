package net.exmo.sixty_seconds.island;

import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 海洋世界（海岛地图）的程序化生成辅助：负责把世界按“区域”划分，
 * 并在每个区域内确定性地规划岛屿中心点，供 {@link SixtySecondsOceanFeature} 使用。
 *
 * <p>该海洋世界现在是一个独立维度（{@code sixty_seconds:ocean}），不再改写主世界。
 */
public final class SixtySecondsOceanWorldGen {
    private SixtySecondsOceanWorldGen() {
    }

    /** 单区域边长（区块数，旧常量，保留以备兼容）。 */
    public static final int REGION = 32;
    /** 海洋维度单区域边长（方块数）：每 4096×4096 方块区域按 {@code oceanIslandCount} 生成岛屿。
     *  放大区域以容纳大岛 + 至少 500 格间隔（避免单区域放不下而回退堆叠 / 跨区截断）。 */
    public static final int REGION_BLOCKS = 4096;
    /** 区域邻接外扩方块数（岛屿半径可能跨区，生成时向四周多算一圈）。 */
    public static final int NEIGHBOR_MARGIN = 512;

    /** 单区域内岛屿列表的缓存（按区域哈希）。 */
    private static final Map<Long, List<SixtySecondsIsland>> REGIONS = new HashMap<>();

    public static List<SixtySecondsIsland> planRegion(int regionX, int regionZ, SixtySecondsConfig config, long worldSeed) {
        long key = ((long) regionX << 32) ^ (long) regionZ;
        List<SixtySecondsIsland> cached = REGIONS.get(key);
        if (cached != null) {
            return cached;
        }
        long hash = hashRegion(regionX, regionZ, worldSeed);
        WorldgenRandom rng = new WorldgenRandom(new XoroshiroRandomSource(hash));
        int count = Math.max(1, config.oceanIslandCount);
        int seaY = config.oceanSeaY > 0 ? config.oceanSeaY : 80;
        int base = SixtySecondsIslandGenerator.DEFAULT_BASE_RADIUS;
        int originX = regionX * REGION_BLOCKS;
        int originZ = regionZ * REGION_BLOCKS;
        // 边距：既让整座岛（岸线外延约 1.35×半径）落在区域内（避免跨区截断），
        // 又保证相邻区域边界上的岛心相距 ≥ 所需最小间隔（避免跨区连成一片）。
        int maxR = (int) (SixtySecondsIslandGenerator.DEFAULT_BASE_RADIUS
                * SixtySecondsIsland.Size.LARGE.radiusMult)
                + SixtySecondsIsland.Size.LARGE.levelRadiusBonus * 5
                + SixtySecondsIsland.Size.LARGE.radiusVariance;            // 大岛可能的最大半径 ≈ 396
        int landReach = (int) (maxR * 1.35);                              // 岸线最大外延
        int crossNeed = maxR * 2 + SixtySecondsIslandGenerator.WATER_SKIRT * 2 + 16
                + Math.max((int) (maxR * 2 * SixtySecondsIslandGenerator.ISLAND_SPACING_MULT),
                SixtySecondsIslandGenerator.ISLAND_MIN_GAP);             // 两最大岛所需最小中心距
        int margin = Math.max(landReach + SixtySecondsIslandGenerator.WATER_SKIRT + 8,
                crossNeed / 2 + 8);                                      // ≈ 680
        List<Integer> prefixes = new ArrayList<>();
        for (int i = 0; i < SixtySecondsIsland.NAME_PREFIX_COUNT; i++) {
            prefixes.add(i);
        }
        Collections.shuffle(prefixes, new java.util.Random(rng.nextLong()));
        List<SixtySecondsIsland> islands = new ArrayList<>();
        int patIdx = 0;
        for (int i = 0; i < count; i++) {
            SixtySecondsIsland island = new SixtySecondsIsland();
            island.id = regionX * 100000 + regionZ * 1000 + i;
            // 大小 / 等级（与主世界 plan 同构）
            if (i == 0) {
                island.size = SixtySecondsIsland.Size.MEDIUM;
                island.level = 1;
            } else {
                float r = rng.nextFloat();
                if (r < 0.45F) {
                    island.size = SixtySecondsIsland.Size.SMALL;
                    island.level = 1 + rng.nextInt(2);
                } else if (r < 0.80F) {
                    island.size = SixtySecondsIsland.Size.MEDIUM;
                    island.level = SixtySecondsIslandGenerator.LEVEL_PATTERN[patIdx % SixtySecondsIslandGenerator.LEVEL_PATTERN.length];
                    patIdx++;
                } else {
                    island.size = SixtySecondsIsland.Size.LARGE;
                    island.level = 2 + rng.nextInt(4);
                }
            }
            island.namePrefix = prefixes.get(i % prefixes.size());
            island.nameSuffix = rng.nextInt(SixtySecondsIsland.NAME_SUFFIX_COUNT);
            island.seed = rng.nextLong();
            island.seaY = seaY;
            // 生态类型（首岛恒为热带，新手友好）
            island.type = SixtySecondsIslandGenerator.assignType(rng, island.level, i == 0);
            SixtySecondsIsland.Size sz = island.size;
            island.radius = (int) (base * sz.radiusMult)
                    + island.level * sz.levelRadiusBonus
                    + rng.nextInt(sz.radiusVariance + 1);
            // 区域内拒绝采样：保证与已放置岛的最小边缘间隔（含 ISLAND_MIN_GAP 下限与倍数稀疏）。
            // 放不下时逐步缩小边距、扩大可放置域重试，避免直接堆到区域中心导致岛屿连片融合。
            boolean placed = false;
            int m = margin;
            for (int round = 0; round < 8 && !placed; round++) {
                int lo = originX + m, hi = originX + REGION_BLOCKS - m;
                int lz = originZ + m, hz = originZ + REGION_BLOCKS - m;
                if (hi <= lo || hz <= lz) { m = Math.max(0, m - 80); continue; }
                for (int attempt = 0; attempt < 60; attempt++) {
                    int x = lo + rng.nextInt(hi - lo + 1);
                    int z = lz + rng.nextInt(hz - lz + 1);
                    boolean ok = true;
                    for (SixtySecondsIsland other : islands) {
                        double extra = Math.max((island.radius + other.radius)
                                * SixtySecondsIslandGenerator.ISLAND_SPACING_MULT,
                                SixtySecondsIslandGenerator.ISLAND_MIN_GAP);
                        double need = island.radius + other.radius
                                + SixtySecondsIslandGenerator.WATER_SKIRT * 2 + 16 + extra;
                        if (other.distSqr(x, z) < need * need) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) {
                        island.centerX = x;
                        island.centerZ = z;
                        placed = true;
                        break;
                    }
                }
                if (!placed) m = Math.max(0, m - 80);
            }
            if (!placed) {
                // 极端配置（单区域塞入过多大岛）下的兜底：放区域中心，保证至少有岛
                island.centerX = originX + REGION_BLOCKS / 2;
                island.centerZ = originZ + REGION_BLOCKS / 2;
            }
            island.dockX = island.centerX;
            island.dockY = seaY;
            island.dockZ = island.centerZ;
            islands.add(island);
        }
        // 稀有撤离点岛屿：每片群岛以低概率<b>额外生成</b>一座专门的撤离点岛（不改造现有岛）。
        if (rng.nextFloat() < Math.max(0.0F, Math.min(1.0F, config.evacuationIslandChance))) {
            SixtySecondsIsland evac = new SixtySecondsIsland();
            evac.id = (regionX * 100000 + regionZ * 1000) + 90000 + islands.size();
            evac.evacNameIndex = rng.nextInt(SixtySecondsIsland.EVAC_NAME_COUNT);
            evac.namePrefix = rng.nextInt(SixtySecondsIsland.NAME_PREFIX_COUNT);
            evac.nameSuffix = rng.nextInt(SixtySecondsIsland.NAME_SUFFIX_COUNT);
            evac.seed = rng.nextLong();
            evac.seaY = seaY;
            evac.level = 1;                                  // 撤离点岛为安全等级，便于登岛
            evac.type = SixtySecondsIsland.Type.EVACUATION;  // 专门的撤离点岛屿类型
            evac.isEvacuation = true;
            SixtySecondsIsland.Size sz = evac.size;
            evac.radius = (int) (base * sz.radiusMult) + sz.levelRadiusBonus + rng.nextInt(sz.radiusVariance + 1);
            boolean placed = false;
            int m = margin;
            for (int round = 0; round < 8 && !placed; round++) {
                int lo = originX + m, hi = originX + REGION_BLOCKS - m;
                int lz = originZ + m, hz = originZ + REGION_BLOCKS - m;
                if (hi <= lo || hz <= lz) { m = Math.max(0, m - 80); continue; }
                for (int attempt = 0; attempt < 60; attempt++) {
                    int x = lo + rng.nextInt(hi - lo + 1);
                    int z = lz + rng.nextInt(hz - lz + 1);
                    boolean ok = true;
                    for (SixtySecondsIsland other : islands) {
                        double extra = Math.max((evac.radius + other.radius)
                                * SixtySecondsIslandGenerator.ISLAND_SPACING_MULT,
                                SixtySecondsIslandGenerator.ISLAND_MIN_GAP);
                        double need = evac.radius + other.radius
                                + SixtySecondsIslandGenerator.WATER_SKIRT * 2 + 16 + extra;
                        if (other.distSqr(x, z) < need * need) {
                            ok = false;
                            break;
                        }
                    }
                    if (ok) {
                        evac.centerX = x;
                        evac.centerZ = z;
                        placed = true;
                        break;
                    }
                }
                if (!placed) m = Math.max(0, m - 80);
            }
            if (!placed) {
                evac.centerX = originX + REGION_BLOCKS / 2 + base;
                evac.centerZ = originZ + REGION_BLOCKS / 2 + base;
            }
            evac.dockX = evac.centerX;
            evac.dockY = seaY;
            evac.dockZ = evac.centerZ;
            islands.add(evac);
        }
        REGIONS.put(key, islands);
        return islands;
    }

    private static long hashRegion(int regionX, int regionZ, long worldSeed) {
        long h = worldSeed ^ ((long) regionX * 0x9E3779B97F4A7C15L) ^ ((long) regionZ * 0xBF58476D1CE4E5B9L);
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        return h;
    }

    public static void resetRegion(int regionX, int regionZ) {
        REGIONS.remove(((long) regionX << 32) ^ (long) regionZ);
    }
}
