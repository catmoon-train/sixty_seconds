package net.exmo.sixty_seconds.island;

import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.XoroshiroRandomSource;

import java.util.ArrayList;
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

    /** 单区域边长（区块数）。 */
    public static final int REGION = 32;
    /** 区域邻接外扩区块数（岛屿半径可能跨区，生成时向四周多算一圈）。 */
    public static final int NEIGHBOR_MARGIN = 1;

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
        int n = Math.max(0, (int) (config.oceanIslandCount * (0.5 + 0.5 * rng.nextDouble())));
        List<SixtySecondsIsland> islands = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            int cx = regionX * REGION + 2 + rng.nextInt(REGION - 4);
            int cz = regionZ * REGION + 2 + rng.nextInt(REGION - 4);
            int radius = 6 + rng.nextInt(10);
            SixtySecondsIsland isl = new SixtySecondsIsland();
            isl.centerX = cx;
            isl.centerZ = cz;
            isl.radius = radius;
            isl.seaY = config.oceanSeaY;
            islands.add(isl);
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
