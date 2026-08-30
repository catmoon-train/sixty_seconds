package net.exmo.sixty_seconds.island;

import com.mojang.logging.LogUtils;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.exmo.sixty_seconds.config.SixtySecondsConfigStore;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import org.slf4j.Logger;

import java.util.List;

/**
 * 海洋世界地形生成（worldgen feature 路径）。
 *
 * <p>该 feature 挂在 biome_modifier 的 {@code raw_generation} step 上，仅当所在维度使用会执行
 * biome feature 的生成器（如 noise 生成器）时才会触发。海洋维度因此改用 noise 生成器而非 flat，
 * 以保证本 feature 能正常生成海床与岛屿。
 */
public final class SixtySecondsOceanFeature extends Feature<NoneFeatureConfiguration> {

    private static final Logger LOGGER = LogUtils.getLogger();

    static final int BEDROCK_Y = 10;
    static final int SAND_TOP = 12;
    /** 区域网格边长（方块），需与 SixtySecondsOceanWorldGen.REGION_BLOCKS 保持一致。 */
    static final int REGION = SixtySecondsOceanWorldGen.REGION_BLOCKS;
    /** 岛屿 cell 额外的搜索半径（格）。 */
    static final int NEIGHBOR_MARGIN = SixtySecondsOceanWorldGen.NEIGHBOR_MARGIN;

    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, SixtySeconds.MOD_ID);

    public static final SixtySecondsOceanFeature INSTANCE = new SixtySecondsOceanFeature();
    public static final net.neoforged.neoforge.registries.DeferredHolder<Feature<?>, SixtySecondsOceanFeature>
            HOLDER = FEATURES.register("ocean_feature", () -> INSTANCE);

    private SixtySecondsOceanFeature() {
        super(NoneFeatureConfiguration.CODEC);
    }

    public static void register(IEventBus bus) {
        FEATURES.register(bus);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> ctx) {
        WorldGenLevel level = ctx.level();
        if (level.isClientSide() || !(level instanceof ServerLevel serverLevel)) {
            return false;
        }
        if (!serverLevel.dimension().equals(SixtySeconds.OCEAN_DIMENSION)) {
            return false;
        }
        BlockPos origin = ctx.origin();
        // raw_generation step + placed_feature(count) 下 origin 落在 chunk 内随机位置，
        // 需换算回 chunk 原点（0/16 对齐），保证每区块精确覆盖自身 16×16
        int cx = Math.floorDiv(origin.getX(), 16) * 16;
        int cz = Math.floorDiv(origin.getZ(), 16) * 16;
        generateChunk(serverLevel, cx, cz);
        return true;
    }

    /**
     * 为某个 chunk（以 chunk 原点 cx/cz 给出）生成海床与岛屿。供 biome_modifier 注入的 feature
     * 以及自定义 ChunkGenerator 的 applyBiomeDecoration 直接手动调用，确保岛屿一定生成。
     */
    public static void generateChunk(WorldGenLevel level, int cx, int cz) {
        ServerLevel serverLevel = level.getLevel();
        if (!serverLevel.dimension().equals(SixtySeconds.OCEAN_DIMENSION)) {
            return;
        }
        SixtySecondsConfig config = SixtySecondsConfigStore.current(serverLevel)
                .orElseGet(SixtySecondsConfig::new);
        int seaY = config.oceanSeaY;
        long worldSeed = serverLevel.getSeed();
        try (BulkSectionAccess bsa = new BulkSectionAccess(level)) {
            int x0 = cx, z0 = cz, x1 = cx + 15, z1 = cz + 15;
            // 1) 海床骨架（含 20 种水下地形）
            writeSeafloor(bsa, x0, z0, x1, z1, seaY, worldSeed);
            // 1b) 海床废墟城市（20 种建筑，物资箱多于海岛）
            writeSeabedCities(bsa, x0, z0, x1, z1, worldSeed, seaY);
            // 2) 岛屿骨架 + 装饰 + 废墟 + 物资箱
            writeIslands(bsa, serverLevel, config, x0, z0, x1, z1, worldSeed);
        }
    }

    /** 写海床 + 岛屿（经 BulkSectionAccess 直接写 primer，无更新开销，feature 内高效）。 */
    private static void writeIslands(BulkSectionAccess bsa, ServerLevel level,
                              SixtySecondsConfig config, int x0, int z0, int x1, int z1, long worldSeed) {
        // 关键修复：planRegion 以 regionX*REGION 为原点，故 chunk 原点直接 floorDiv 对齐，
        // 不可再加 NEIGHBOR_MARGIN（512），否则地形会与海图坐标错位。
        int regX0 = Math.floorDiv(x0, REGION);
        int regX1 = Math.floorDiv(x1, REGION);
        int regZ0 = Math.floorDiv(z0, REGION);
        int regZ1 = Math.floorDiv(z1, REGION);
        // 额外纳入相邻区域：岛屿单元格可能跨入本 chunk，保证边界处的岛不被截断
        for (int rx = regX0 - 1; rx <= regX1 + 1; rx++) {
            for (int rz = regZ0 - 1; rz <= regZ1 + 1; rz++) {
                List<SixtySecondsIsland> islands = SixtySecondsOceanWorldGen.planRegion(rx, rz, config, worldSeed);
                for (SixtySecondsIsland island : islands) {
                    int cell = island.radius + SixtySecondsIslandGenerator.WATER_SKIRT;
                    if (x1 < island.centerX - cell || x0 > island.centerX + cell) {
                        continue;
                    }
                    if (z1 < island.centerZ - cell || z0 > island.centerZ + cell) {
                        continue;
                    }
                    // 岛屿骨架（陆地/岸边）
                    SixtySecondsIslandGenerator.buildPatchPrimer(bsa, islands, island, x0, z0, x1, z1);
                    // 装饰（树/岩石/植被）与废墟/物资箱：经 PrimerPlacer 逐块写入，越界列自动跳过，
                    // 因此每座岛只落成本区块内的那一块，随玩家探索逐 chunk 出现。
                    SixtySecondsIslandGenerator.PrimerPlacer placer =
                            new SixtySecondsIslandGenerator.PrimerPlacer(bsa, x0, z0);
                    SixtySecondsIslandGenerator.decorate(placer, island);
                    if (!island.isEvacuation) {
                        SixtySecondsRuins.placeAll(placer, island);
                    }
                }
            }
        }
    }

    /** 写海床 primer（含 20 种水下地形，不触发更新）。 */
    private static void writeSeafloor(BulkSectionAccess bsa, int x0, int z0, int x1, int z1, int seaY, long worldSeed) {
        OceanSeabedTerrain.applyChunk(bsa, x0, z0, x1, z1, seaY, worldSeed);
    }

    /** 在海床上确定性散布废墟城市建筑（每座 2~4 个物资箱）。 */
    private static void writeSeabedCities(BulkSectionAccess bsa, int x0, int z0, int x1, int z1,
            long worldSeed, int seaY) {
        int gMinX = Math.floorDiv(x0, OceanSeabedRuins.SPACING) - 1;
        int gMaxX = Math.floorDiv(x1, OceanSeabedRuins.SPACING) + 1;
        int gMinZ = Math.floorDiv(z0, OceanSeabedRuins.SPACING) - 1;
        int gMaxZ = Math.floorDiv(z1, OceanSeabedRuins.SPACING) + 1;
        SixtySecondsIslandGenerator.PrimerPlacer placer = new SixtySecondsIslandGenerator.PrimerPlacer(bsa, x0, z0);
        for (int gx = gMinX; gx <= gMaxX; gx++) {
            for (int gz = gMinZ; gz <= gMaxZ; gz++) {
                OceanSeabedRuins.Placement b = OceanSeabedRuins.plan(gx, gz, worldSeed);
                if (b == null) continue;
                int half = 12;
                if (x1 < b.centerX - half || x0 > b.centerX + half) continue;
                if (z1 < b.centerZ - half || z0 > b.centerZ + half) continue;
                OceanSeabedRuins.placeAll(placer, b, seaY);
            }
        }
    }

    private static void setPrimer(BulkSectionAccess bsa, int x, int y, int z, BlockState state) {
        LevelChunkSection section = bsa.getSection(new BlockPos(x, y, z));
        if (section != null) {
            section.setBlockState(x & 15, y & 15, z & 15, state, false);
        }
    }
}
