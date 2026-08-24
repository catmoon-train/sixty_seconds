package net.exmo.sixty_seconds.island;

import com.mojang.logging.LogUtils;
import net.exmo.sixty_seconds.SixtySeconds;
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
 * 海洋世界地形生成 Feature（镜像 LostCities 的做法：在 worldgen 的 FEATURES 阶段，
 * 通过 {@link BulkSectionAccess} 把海洋海床 + 岛屿骨架写入 {@link ChunkAccess} primer，
 * 且每次写入都 <b>不触发任何方块更新</b>，因此不会在已加载区块上同步 setBlock，
 * 也不会让主线程在生成阶段被占满导致卡死）。
 *
 * <p>仅在 {@code sixty_seconds:ocean} 维度内触发（由数据包 dimension/ocean.json 加载），
 * 与主世界互不干扰。
 */
public final class SixtySecondsOceanFeature extends Feature<NoneFeatureConfiguration> {

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 基岩层所在 Y。 */
    static final int BEDROCK_Y = 10;
    /** 沙子层 Y 范围（含）。 */
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
        if (serverLevel.dimension() != SixtySeconds.OCEAN_DIMENSION) {
            return false;
        }
        // 海洋维度始终生成海岛地形（使用配置中的种子/海平面/岛屿数量，缺失时回退默认值）
        SixtySecondsConfig config = SixtySecondsConfigStore.current(serverLevel)
                .orElseGet(SixtySecondsConfig::new);

        BlockPos origin = ctx.origin();
        // raw_generation step + placed_feature(count) 下 origin 落在 chunk 内随机位置，
        // 需换算回 chunk 原点（0/16 对齐），保证每区块精确覆盖自身 16×16
        int cx = Math.floorDiv(origin.getX(), 16) * 16;
        int cz = Math.floorDiv(origin.getZ(), 16) * 16;
        int seaY = config.oceanSeaY;
        long worldSeed = serverLevel.getSeed();

        int x0 = cx;
        int z0 = cz;
        int x1 = cx + 15;
        int z1 = cz + 15;

        try (BulkSectionAccess bsa = new BulkSectionAccess(level)) {
            // 1) 海床骨架：每列从 Y=0 到 seaY 重写（基岩/沙/空/水）
            writeSeafloor(bsa, x0, z0, x1, z1, seaY);
            // 2) 岛屿骨架 + 装饰 + 废墟 + 物资箱：全部随本区块逐块写入 primer，
            //    与 LostCities 一致地「一块块」出现，进游戏后不再有大面积一次性建造。
            writeIslands(bsa, serverLevel, config, x0, z0, x1, z1, worldSeed);
        }
        return true;
    }

    /** 写海床 primer（不触发更新）。 */
    private void writeSeafloor(BulkSectionAccess bsa, int x0, int z0, int x1, int z1, int seaY) {
        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState sand = Blocks.SAND.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        int topY = Math.max(seaY, SAND_TOP);
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                for (int y = 0; y <= topY; y++) {
                    BlockState state;
                    if (y < BEDROCK_Y) {
                        state = air;       // Y<10 留空
                    } else if (y == BEDROCK_Y) {
                        state = bedrock;   // Y=10 基岩
                    } else if (y <= SAND_TOP) {
                        state = sand;      // Y=11/12 沙子
                    } else {
                        state = water;     // Y=13..seaY 水
                    }
                    setPrimer(bsa, x, y, z, state);
                }
            }
        }
    }

    /** 写岛屿骨架 + 装饰 + 废墟 + 物资箱 primer（不触发更新，逐块）。 */
    private void writeIslands(BulkSectionAccess bsa, ServerLevel level, SixtySecondsConfig config,
                              int x0, int z0, int x1, int z1, long worldSeed) {
        int regX0 = Math.floorDiv(x0 - NEIGHBOR_MARGIN, REGION);
        int regX1 = Math.floorDiv(x1 + NEIGHBOR_MARGIN, REGION);
        int regZ0 = Math.floorDiv(z0 - NEIGHBOR_MARGIN, REGION);
        int regZ1 = Math.floorDiv(z1 + NEIGHBOR_MARGIN, REGION);
        for (int rx = regX0; rx <= regX1; rx++) {
            for (int rz = regZ0; rz <= regZ1; rz++) {
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
                    SixtySecondsIslandGenerator.buildPatchPrimer(bsa, island, x0, z0, x1, z1);
                    // 装饰（树/岩石/植被）与废墟/物资箱：经 PrimerPlacer 逐块写入，越界列自动跳过，
                    // 因此每座岛只落成本区块内的那一块，随玩家探索逐 chunk 出现。
                    SixtySecondsIslandGenerator.PrimerPlacer placer =
                            new SixtySecondsIslandGenerator.PrimerPlacer(bsa, x0, z0);
                    SixtySecondsIslandGenerator.decorate(placer, island);
                    SixtySecondsRuins.placeAll(placer, island);
                }
            }
        }
    }

    /** 直接写 primer（不触发更新），参照 LostCities 的 SectionCache.setBlock 写法。 */
    private static void setPrimer(BulkSectionAccess bsa, int x, int y, int z, BlockState state) {
        LevelChunkSection section = bsa.getSection(new BlockPos(x, y, z));
        if (section != null) {
            section.setBlockState(x & 15, y & 15, z & 15, state, false);
        }
    }
}
