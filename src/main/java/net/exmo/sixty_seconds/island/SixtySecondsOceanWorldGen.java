package net.exmo.sixty_seconds.island;

import com.mojang.logging.LogUtils;
import net.exmo.sixty_seconds.SixtySecondsOceanSetup;
import net.exmo.sixty_seconds.bridge.GameUtils;
import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.exmo.sixty_seconds.config.SixtySecondsConfigStore;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.ChunkEvent;
import net.neoforged.neoforge.event.level.LevelEvent;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 海洋世界地形改写器（镜像 LostCities 的思路：世界创建后，区块首次加载时整体改写为海洋地图）。
 *
 * <p>当 {@link SixtySecondsConfig#oceanMode} 为真时，主世界每一个区块在首次加载时被改写为：
 * <ul>
 *   <li>Y=10 处一层基岩；</li>
 *   <li>Y=11、Y=12 两层沙子；</li>
 *   <li>Y&lt;10 不生成任何方块（留空，供模板生成避难所/初始房子）；</li>
 *   <li>Y=13..seaY 为水，Y&gt;seaY 为空气（整张世界默认全是海）；</li>
 *   <li>按种子在整张世界确定性地散布海面岛屿（含物资箱，见 {@link SixtySecondsIslandGenerator}）。</li>
 * </ul>
 *
 * <p>岛屿采用「区域网格 + 确定性分布」：每 {@value #REGION} 格为一个区域，区域内岛屿的
 * 位置/半径/类型由区域坐标与 {@code oceanSeed}/{@code worldSeed} 决定，因此客户端海图与
 * 服务端地形一致、且无限世界也能稳定复现。
 */
public final class SixtySecondsOceanWorldGen {
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 区域网格边长（格）。 */
    public static final int REGION = 1024;
    /** 基岩层所在 Y。 */
    public static final int BEDROCK_Y = 10;
    /** 沙子层 Y 范围（含）。 */
    public static final int SAND_TOP = 12;
    /** 每区域最多生成的岛屿数量上限（防止过密）。 */
    private static final int MAX_ISLANDS_PER_REGION = 3;
    /** 岛屿 cell 额外的搜索半径（格），覆盖相邻区域可能伸过来的岛。 */
    static final int NEIGHBOR_MARGIN = 400;

    /** 已排程建造的岛屿（防重复）：Level → 已排程的 island id 集合。 */
    private static final Map<Level, Set<Long>> SCHEDULED = new WeakHashMap<>();
    /** 是否已注册事件（仅注册一次）。 */
    private static final AtomicBoolean REGISTERED = new AtomicBoolean(false);

    private SixtySecondsOceanWorldGen() {
    }

    /** 在模组初始化时调用，确保监听器只注册一次。 */
    public static void register() {
        if (REGISTERED.compareAndSet(false, true)) {
            NeoForge.EVENT_BUS.addListener(SixtySecondsOceanWorldGen::onLevelLoad);
            NeoForge.EVENT_BUS.addListener(SixtySecondsOceanWorldGen::onChunkLoad);
            LOGGER.info("ocean_world.registered");
        }
    }

    /**
     * 主世界加载时：若客户端创建世界界面勾选了「60秒·海洋」（单人/局域网内嵌服务端同 JVM 可读），
     * 则将参数写入世界配置，使后续区块加载即被改写为海洋地图。
     */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }
        if (!SixtySecondsOceanSetup.enabled) {
            return;
        }
        SixtySecondsConfig config = SixtySecondsConfigStore.load(serverLevel).orElseGet(SixtySecondsConfig::new);
        config.oceanMode = true;
        config.oceanSeed = SixtySecondsOceanSetup.oceanSeed;
        config.oceanIslandCount = SixtySecondsOceanSetup.oceanIslandCount;
        config.oceanSeaY = SixtySecondsOceanSetup.oceanSeaY;
        config.oceanSpawnY = SixtySecondsOceanSetup.oceanSeaY;
        SixtySecondsConfigStore.save(serverLevel, config);
        // 后续每个区块加载时由 onChunkLoad 自动改写为海洋地图（含出生点区块）
        SixtySecondsOceanSetup.reset();
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (!(event.getLevel() instanceof ServerLevel serverLevel)) {
            return;
        }
        if (serverLevel.dimension() != Level.OVERWORLD) {
            return;
        }
        SixtySecondsConfig config = SixtySecondsConfigStore.load(serverLevel).orElse(null);
        if (config == null || !config.oceanMode) {
            return;
        }
        // 重新落盘，确保 ocean 标记持久化（创建世界阶段可能只写了内存标记）
        SixtySecondsConfigStore.save(serverLevel, config);

        // Ocean 地形（海床 + 岛屿骨架）已经在 worldgen 阶段的 SixtySecondsOceanFeature 里
        // 写入 ChunkAccess primer（不触发任何更新），不会再在已加载区块上同步 setBlock，
        // 因此不会卡主线程/卡死生成进度。这里只负责排程岛屿的装饰 + 物资箱建造。
        LevelChunk chunk = (LevelChunk) event.getChunk();
        int cx = chunk.getPos().getMinBlockX();
        int cz = chunk.getPos().getMinBlockZ();
        long worldSeed = serverLevel.getSeed();
        applyChunkDecorations(serverLevel, config, cx, cz, worldSeed);
    }

    /**
     * 对已加载区块落入的岛屿排程装饰 + 物资箱建造（异步跨 tick，不阻塞生成）。
     * 地形本身由 {@code SixtySecondsOceanFeature} 在 worldgen 阶段写好。
     */
    private static void applyChunkDecorations(ServerLevel serverLevel, SixtySecondsConfig config,
                                               int cx, int cz, long worldSeed) {
        int regX0 = Math.floorDiv(cx - NEIGHBOR_MARGIN, REGION);
        int regX1 = Math.floorDiv(cx + 15 + NEIGHBOR_MARGIN, REGION);
        int regZ0 = Math.floorDiv(cz - NEIGHBOR_MARGIN, REGION);
        int regZ1 = Math.floorDiv(cz + 15 + NEIGHBOR_MARGIN, REGION);
        for (int rx = regX0; rx <= regX1; rx++) {
            for (int rz = regZ0; rz <= regZ1; rz++) {
                List<SixtySecondsIsland> islands = planRegion(rx, rz, config, worldSeed);
                for (SixtySecondsIsland island : islands) {
                    int cell = island.radius + SixtySecondsIslandGenerator.WATER_SKIRT;
                    if (cx + 15 < island.centerX - cell || cx > island.centerX + cell) {
                        continue;
                    }
                    if (cz + 15 < island.centerZ - cell || cz > island.centerZ + cell) {
                        continue;
                    }
                    // 排程整岛装饰 + 物资箱（仅一次）
                    scheduleIsland(serverLevel, island);
                }
            }
        }
    }

    /** 确定性规划某区域内生成的岛屿列表（供海洋地形 Feature 与装饰排程共用）。 */
    public static List<SixtySecondsIsland> planRegion(int rx, int rz, SixtySecondsConfig config, long worldSeed) {
        long seed = config.oceanSeed ^ (rx * 73856093L) ^ (rz * 19349663L) ^ (worldSeed * 83492791L);
        net.minecraft.util.RandomSource rng = net.minecraft.util.RandomSource.create(seed);
        int count = Math.min(MAX_ISLANDS_PER_REGION, Math.max(0, config.oceanIslandCount));
        // 用区域确定性噪声决定实际生成几座（保证分布自然）
        int n = (int) (count * (0.5F + 0.5F * rng.nextFloat()));
        n = Math.min(n, MAX_ISLANDS_PER_REGION);
        java.util.List<SixtySecondsIsland> result = new java.util.ArrayList<>();
        int regOriginX = rx * REGION;
        int regOriginZ = rz * REGION;
        for (int i = 0; i < n; i++) {
            SixtySecondsIsland island = new SixtySecondsIsland();
            island.id = (int) ((rx * 73856093L + rz * 19349663L + i * 83492791L) & 0x7FFFFFFF);
            island.level = Math.max(1, Math.min(5, config.oceanIslandLevel + (rng.nextInt(3) - 1)));
            // 大小：默认中型，半径稳定便于海图复现
            island.size = SixtySecondsIsland.Size.MEDIUM;
            int baseRadius = 200;
            island.radius = baseRadius
                    + rng.nextInt(SixtySecondsIsland.Size.MEDIUM.radiusVariance + 1)
                    + island.level * SixtySecondsIsland.Size.MEDIUM.levelRadiusBonus;
            island.type = null; // 纯 level 色板，最稳
            island.seed = rng.nextLong();
            island.seaY = config.oceanSeaY;
            island.centerX = regOriginX + 120 + rng.nextInt(REGION - 240);
            island.centerZ = regOriginZ + 120 + rng.nextInt(REGION - 240);
            island.shelterDoorX = island.centerX;
            island.shelterDoorZ = island.centerZ;
            island.shelterDoorY = config.oceanSeaY;
            result.add(island);
        }
        return result;
    }

    /** 排程整岛装饰 + 物资箱建造（仅一次）。 */
    private static void scheduleIsland(ServerLevel level, SixtySecondsIsland island) {
        Set<Long> set = SCHEDULED.computeIfAbsent(level, k -> java.util.concurrent.ConcurrentHashMap.newKeySet());
        if (!set.add((long) island.id)) {
            return;
        }
        SixtySecondsIslandGenerator.queueBuild(level, java.util.List.of(island),
                new java.util.LinkedHashMap<>(), true, null);
    }
}
