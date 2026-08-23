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
        // 注意：海床 + 岛屿骨架 + 装饰 + 废墟 + 物资箱全部在 worldgen 阶段由 SixtySecondsOceanFeature
        // 逐 chunk 写入 ChunkAccess primer（不触发任何更新），随玩家探索逐块出现，
        // 因此进游戏后不再有「大面积一次性建造」，这里无需再做任何排程。
    }

    /** 确定性规划某区域内生成的岛屿列表（供海洋地形 Feature 共用）。 */
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
}
