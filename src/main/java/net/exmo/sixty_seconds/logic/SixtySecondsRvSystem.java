package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.arena.SixtySecondsSearchZones;
import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.exmo.sixty_seconds.config.SixtySecondsConfigStore;
import net.exmo.sixty_seconds.content.entity.SixtySecondsRvEntity;
import net.exmo.sixty_seconds.content.entity.SixtySecondsRvPart;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.island.SixtySecondsIsland;
import net.exmo.sixty_seconds.island.SixtySecondsIslands;
import net.exmo.sixty_seconds.island.SixtySecondsIslandGenerator;
import net.exmo.sixty_seconds.registry.ModEntities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * 房车模式（{@link SixtySecondsConfig#rvEnabled}）的服务端生命周期系统：每队常驻一辆房车。
 * <ul>
 *   <li><b>生成</b>：建图完成后按队生成——刷新点优先取 {@link SixtySecondsConfig#rvSpawnPoints}（按队序号，
 *       绝对坐标），未配置则在该队住宅出生点旁找安全落点作为兼容回退。</li>
 *   <li><b>常驻加载</b>：房车所在区块被强制加载（复用 {@code setChunkForced} 记账），无人在旁也保持运行。</li>
 *   <li><b>丢失恢复</b>：房车被移除（/kill、掉出世界等）后按刷新点重生。血量归零只停机、实体不消失，
 *       故不会触发重生（见 {@link SixtySecondsRvEntity#die}）。</li>
 *   <li><b>坠坑/虚空</b>：记录最近一次「脚下有支撑」的安全落点，落入深坑或跌至虚空时整车传回该点。</li>
 * </ul>
 * 状态存于内存 {@link SixtySecondsState}（不落盘），故本系统只在一局游戏内有效——与 60s 其余系统一致。
 */
public final class SixtySecondsRvSystem {
    private SixtySecondsRvSystem() {
    }

    /** 强制加载的区块环半径（1 = 房车所在 3×3 区块，容纳 4.8 宽的车体跨区块碰撞）。 */
    private static final int FORCE_RADIUS = 1;
    /** 上一次安全落点低于当前高度超过此格数即判定坠坑，整车回退。 */
    private static final int PIT_DROP_THRESHOLD = 6;
    /**
     * 房车车体净空高度（格）。车体 {@code sized(4.8f, 3.2f)}（见 {@code ModEntities#HOLD_RV}），
     * 向上取整得 4；房车比玩家高得多，落点必须按这个高度校验，不能沿用「双格净空」。
     */
    private static final int RV_CLEAR_HEIGHT = 4;
    /** 与住宅/避难所外墙保持的水平间距（格），避免车体蹭墙。 */
    private static final int RV_STRUCTURE_MARGIN = 2;
    /** 从结构体外缘向外搜索房车落点的最大扩展圈数。 */
    private static final int RV_SEARCH_RINGS = 24;
    /** 地形基准高度之下允许再往下找支撑的格数（街道被炸出坑、或有下沉庭院时向下兜底）。 */
    private static final int RV_SCAN_BELOW = 10;
    /** 地形基准高度之上允许再往上找落脚点的格数（地表堆了废墟残骸时向上抬）。 */
    private static final int RV_SCAN_ABOVE = 8;

    /**
     * 地形基准高度缓存：(x,z) → 站立高度。
     *
     * <p>{@code getBaseHeight} 要迭代整根噪声列，代价不小；环形搜索里同一列可能反复命中，故按列缓存。
     * 每次搜索开始由 {@link #beginGroundCache()} 清空。</p>
     */
    private static final Map<Long, Integer> GROUND_CACHE = new HashMap<>();

    private static void beginGroundCache() {
        GROUND_CACHE.clear();
    }

    private static long colKey(int x, int z) {
        return ((long) x << 32) ^ (z & 0xFFFFFFFFL);
    }

    /**
     * 地形基准高度（脚部站立 Y）——取自 LostCities 的做法。
     *
     * <p>LostCities 在 {@code ChunkHeightmap#calculateAccurateHeight} 与
     * {@code LostCityTerrainFeature#getHeightmap} 中用它判定地表：
     * <pre>{@code
     * generator.getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, region, randomState)
     * }</pre>
     * 返回的是<b>世界生成器算出的地形高度</b>（第一个阻挡方块的上一格），
     * 只反映自然地形，<b>不包含任何后来叠加上去的建筑方块</b>。
     * 因此在 LostCities 城市里它就是<b>街道地面</b>的高度，而不是屋顶——这正是房车该停的地方。</p>
     *
     * <p>对比 {@code Heightmap.Types.MOTION_BLOCKING_NO_LEAVES}（当前最高的阻挡方块）：
     * 那一档会把房屋的屋顶、高架桥都算进去，用它定位房车就会把车生成在楼顶/室内。</p>
     */
    private static int baseGroundY(ServerLevel level, int x, int z) {
        long key = colKey(x, z);
        Integer cached = GROUND_CACHE.get(key);
        if (cached != null) {
            return cached;
        }
        int y;
        try {
            ServerChunkCache source = level.getChunkSource();
            y = source.getGenerator().getBaseHeight(x, z, Heightmap.Types.OCEAN_FLOOR_WG, level,
                    source.randomState());
        } catch (Exception e) {
            // 生成器不支持（如自定义扁平生成器）时退回当前地表
            y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
        }
        y = Mth.clamp(y, level.getMinBuildHeight() + 1, level.getMaxBuildHeight() - RV_CLEAR_HEIGHT - 1);
        GROUND_CACHE.put(key, y);
        return y;
    }
    /** 每 20 tick（1s）跑一次巡检：重生、强载跟随、坠坑回退。 */
    private static final int UPKEEP_INTERVAL = 20;
    /** 已扫除孤立房车的 Level（防每局多次全量扫描）。 */
    private static final Set<ServerLevel> ORPHAN_SWEPT = Collections.newSetFromMap(new WeakHashMap<>());

    // ─────────────────────────────────────────────────────────────────
    // 生命周期
    // ─────────────────────────────────────────────────────────────────

    /** 建图完成、玩家已传送进家后调用：清掉上一局残留房车，再按队生成本局房车。 */
    public static void onGameStart(ServerLevel level, SixtySecondsState.Data data) {
        SixtySecondsConfig config = SixtySecondsConfigStore.current(level).orElse(null);
        boolean ocean = SixtySeconds.isOcean(level);
        // ocean 模式：房车落在各队 1 级港湾岛陆地（避水），回同维度庇护所，不受 rvEnabled 限制
        if (!ocean && (config == null || !config.rvEnabled)) {
            return;
        }
        // ocean 模式：先为各队计算 1 级港湾岛上的陆地落点（自动搜索、避水），不再依赖手动 rvSpawnPoints
        if (ocean) {
            SixtySecondsIslands.Data islandData = SixtySecondsIslands.get(level);
            List<SixtySecondsIsland> lvl1 = islandData.save.islands.stream()
                    .filter(i -> i.level == 1).collect(java.util.stream.Collectors.toList());
            List<SixtySecondsState.TeamData> teams = new ArrayList<>(data.teams.values());
            RandomSource rng = level.getRandom();
            for (int i = 0; i < teams.size(); i++) {
                SixtySecondsState.TeamData team = teams.get(i);
                if (team.shelterSpawn == null) continue;
                SixtySecondsIsland island = lvl1.isEmpty() ? null : lvl1.get(i % lvl1.size());
                if (island == null) continue;
                BlockPos spot = findIslandRvSpot(level, island, rng);
                if (spot != null) data.oceanRvSpots.put(team.teamId, spot);
            }
        }
        ORPHAN_SWEPT.remove(level);
        discardAllRvs(level);
        int index = 0;
        for (SixtySecondsState.TeamData team : data.teams.values()) {
            team.rvEntityUuid = null;
            team.rvForcedChunkX = Integer.MIN_VALUE;
            team.rvForcedChunkZ = Integer.MIN_VALUE;
            team.rvLastSafePos = null;
            spawnRv(level, config, data, team, index);
            index++;
        }
        SixtySeconds.LOGGER.info("[60s] RV mode: spawned persistent RVs for {} teams.", data.teams.size());
    }

    /** 游戏结束/重置：解除本系统的所有强载区块并清除房车实体与相关状态。 */
    public static void reset(ServerLevel level) {
        SixtySecondsState.Data data = SixtySecondsState.get(level);
        for (SixtySecondsState.TeamData team : data.teams.values()) {
            releaseForced(level, team);
            team.rvEntityUuid = null;
            team.rvForcedChunkX = Integer.MIN_VALUE;
            team.rvForcedChunkZ = Integer.MIN_VALUE;
            team.rvLastSafePos = null;
            team.rvRespawnCooldown = 0;
        }
        ORPHAN_SWEPT.remove(level);
        discardAllRvs(level);
    }

    // ─────────────────────────────────────────────────────────────────
    // 巡检
    // ─────────────────────────────────────────────────────────────────

    /** 由 {@link SixtySecondsManager#tick} 相位无关段调用（建图期 phase=INACTIVE 时不会进来）。 */
    public static void tick(ServerLevel level, SixtySecondsState.Data data) {
        if (level.getGameTime() % UPKEEP_INTERVAL != 0) {
            return;
        }
        SixtySecondsConfig config = SixtySecondsConfigStore.current(level).orElse(null);
        if (config == null || !config.rvEnabled) {
            return;
        }
        // 每局首次巡检：扫除上一把残留的孤立房车（teamId 不属于当前任何队伍）
        if (!ORPHAN_SWEPT.contains(level)) {
            ORPHAN_SWEPT.add(level);
            discardOrphanedRvs(level, data);
        }
        int index = 0;
        for (SixtySecondsState.TeamData team : data.teams.values()) {
            // 冷却递减
            if (team.rvRespawnCooldown > 0) {
                team.rvRespawnCooldown--;
            }
            SixtySecondsRvEntity rv = resolveRv(level, team);
            if (rv == null && team.rvRespawnCooldown <= 0) {
                rv = spawnRv(level, config, data, team, index);
                // 生成成功→重置冷却；失败→冷却 5 秒防无限重试
                team.rvRespawnCooldown = (rv != null) ? 0 : 100;
            }
            if (rv != null) {
                upkeep(level, team, rv);
            }
            index++;
        }
    }

    /** 单车巡检：强载跟随 + 坠坑/虚空回退 + 刷新安全落点。 */
    private static void upkeep(ServerLevel level, SixtySecondsState.TeamData team, SixtySecondsRvEntity rv) {
        // 强载区块跟随房车移动
        followForced(level, team, rv);

        double voidFloor = level.getMinBuildHeight() + 2;
        boolean inVoid = rv.getY() < voidFloor;
        // 脱困绞盘：从更浅的坑里就回退（普通 6 格 → 3 格）
        int pitThreshold = rv.hasPart(SixtySecondsRvPart.WINCH) ? 3 : PIT_DROP_THRESHOLD;
        boolean inDeepPit = team.rvLastSafePos != null
                && rv.getY() < team.rvLastSafePos.getY() - pitThreshold;
        if (inVoid || inDeepPit) {
            recover(level, team, rv);
            return;
        }
        // 卡进方块（车体被地形埋住）：轻微上抬到头顶净空处，防止无法启动/看门狗
        if (rv.getPassengers().isEmpty() && rv.isInWall() && team.rvLastSafePos != null) {
            recover(level, team, rv);
            return;
        }
        // 房车停在有支撑的地面上 → 记录为最近安全落点（坠坑时回退到此）
        if (rv.onGround() && !rv.isInWater()) {
            BlockPos below = rv.blockPosition().below();
            if (!level.getBlockState(below).getCollisionShape(level, below).isEmpty()) {
                team.rvLastSafePos = rv.blockPosition();
            }
        }
    }

    /** 整车传回最近安全落点（无安全落点则不动，避免把车扔进未知区）。 */
    private static void recover(ServerLevel level, SixtySecondsState.TeamData team, SixtySecondsRvEntity rv) {
        if (team.rvLastSafePos == null) {
            return;
        }
        BlockPos safe = SixtySecondsSearchZones.findSafeSpot(level, team.rvLastSafePos);
        rv.setDeltaMovement(Vec3.ZERO);
        rv.fallDistance = 0.0F;
        rv.teleportTo(safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5);
        followForced(level, team, rv);
    }

    // ─────────────────────────────────────────────────────────────────
    // 生成 / 解析
    // ─────────────────────────────────────────────────────────────────

    /** 解析某队的房车实体（供门菜单「外出探索」把落点改到房车处）；取不到返回 null。 */
    public static SixtySecondsRvEntity getTeamRv(ServerLevel level, SixtySecondsState.TeamData team) {
        return resolveRv(level, team);
    }

    /** 解析本队房车实体：优先按记录的 UUID，取不到（被移除）返回 null。 */
    private static SixtySecondsRvEntity resolveRv(ServerLevel level, SixtySecondsState.TeamData team) {
        if (team.rvEntityUuid == null) {
            return null;
        }
        Entity entity = level.getEntity(team.rvEntityUuid);
        if (entity instanceof SixtySecondsRvEntity rv && !rv.isRemoved()) {
            return rv;
        }
        return null;
    }

    /** 按刷新点生成一辆房车并登记；刷新点解析失败返回 null。
     *  先生成实体前强制加载目标区块，防止实体被挂在未加载区块上导致立即移除。 */
    private static SixtySecondsRvEntity spawnRv(ServerLevel level, SixtySecondsConfig config,
            SixtySecondsState.Data data, SixtySecondsState.TeamData team, int index) {
        BlockPos spawn = resolveSpawn(level, config, data, team, index);
        if (spawn == null) {
            return null;
        }
        // 先强制加载目标区块再创建实体（防竞态：实体挂到未加载区块后被移除）
        forceChunksAt(level, team, spawn);
        SixtySecondsRvEntity rv = ModEntities.SIXTY_SECONDS_RV.create(level);
        if (rv == null) {
            releaseForced(level, team);
            return null;
        }
        rv.setTeamId(team.teamId);
        rv.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5, 0.0F, 0.0F);
        rv.setPersistenceRequired(); // 不因距离/难度自然消失
        level.addFreshEntity(rv);
        team.rvEntityUuid = rv.getUUID();
        team.rvLastSafePos = spawn;
        return rv;
    }

    /** 强制加载目标位置周围 FORCE_RADIUS 环区块。 */
    private static void forceChunksAt(ServerLevel level, SixtySecondsState.TeamData team, BlockPos pos) {
        releaseForced(level, team);
        ChunkPos cur = new ChunkPos(pos);
        for (int dx = -FORCE_RADIUS; dx <= FORCE_RADIUS; dx++) {
            for (int dz = -FORCE_RADIUS; dz <= FORCE_RADIUS; dz++) {
                level.setChunkForced(cur.x + dx, cur.z + dz, true);
            }
        }
        team.rvForcedChunkX = cur.x;
        team.rvForcedChunkZ = cur.z;
    }

    /** 刷新点：ocean 模式用各队岛屿陆地落点；否则配置的 {@code rvSpawnPoints[index]} 优先，
     *  再回退到<b>住宅/避难所墙体之外的地表</b>（房车宽 4.8、高 3.2，绝不能生成在房子内部）。 */
    private static BlockPos resolveSpawn(ServerLevel level, SixtySecondsConfig config,
            SixtySecondsState.Data data, SixtySecondsState.TeamData team, int index) {
        // ocean 模式优先用各队在岛屿陆地上的自动落点（已在 onGameStart 预计算，避水）
        if (SixtySeconds.isOcean(level) && data.oceanRvSpots.containsKey(team.teamId)) {
            BlockPos spot = data.oceanRvSpots.get(team.teamId);
            BlockPos safe = SixtySecondsSearchZones.findSafeSpot(level, spot);
            return safe != null ? safe : spot;
        }
        if (config != null && config.rvSpawnPoints != null && index < config.rvSpawnPoints.size()
                && config.rvSpawnPoints.get(index) != null) {
            BlockPos configured = config.rvSpawnPoints.get(index).toBlockPos();
            // 配置点是「人站得住」的坐标；房车体积远大于玩家，必须就近扩到能容纳车体的落点
            return fitRvNear(level, configured);
        }
        if (team.residentialSpawn != null) {
            BlockPos outside = findOutsideRvSpot(level, team);
            if (outside != null) {
                return outside;
            }
            // 兜底：住宅外确实找不到（如结构体被地形完全包住）时，沿用旧行为在出生点旁落脚
            return SixtySecondsSearchZones.findSafeSpot(level, team.residentialSpawn.offset(3, 0, 3));
        }
        return null;
    }

    /**
     * 在住宅/避难所<b>结构体之外的地表</b>找一个能容纳房车的落脚点。
     *
     * <p>此前直接用 {@link SixtySecondsSearchZones#findSafeSpot}（双格净空 + 脚下有支撑）而已，
     * 而室内天然满足该条件，于是房车被生成在房子里。这里改为：
     * <ol>
     *   <li>取住宅/避难所范围盒（{@code residentialBox}/{@code shelterBox}）的并集作为结构体轮廓，
     *       向外留 {@value #RV_STRUCTURE_MARGIN} 格间距——仅作快速预筛，跳过大量无谓的地形查询；</li>
     *   <li>由内向外逐圈取候选 (x,z)，每根柱子用 LostCities 的地形基准高度
     *       （{@link #baseGroundY}，自然地形/街道地面，不含建筑方块）定位地表；</li>
     *   <li>校验车体放得下（先试 5×5 占地，再放宽到 3×3）——
     *       这一步是真正判据：建筑物正上方会被建筑方块占满净空而落选，
     *       所以在 LostCities 城市里房车只会停在街道上，绝不会上屋顶、也不会进室内。</li>
     * </ol>
     */
    private static BlockPos findOutsideRvSpot(ServerLevel level, SixtySecondsState.TeamData team) {
        BlockPos anchor = team.residentialSpawn;
        if (anchor == null) {
            return null;
        }
        AABB res = team.residentialBox;
        AABB shel = team.shelterBox;
        // 结构体轮廓：住宅与避难所的并集（二者可能不在同一处）
        double minX = Math.min(res != null ? res.minX : anchor.getX(), shel != null ? shel.minX : anchor.getX());
        double maxX = Math.max(res != null ? res.maxX : anchor.getX(), shel != null ? shel.maxX : anchor.getX());
        double minZ = Math.min(res != null ? res.minZ : anchor.getZ(), shel != null ? shel.minZ : anchor.getZ());
        double maxZ = Math.max(res != null ? res.maxZ : anchor.getZ(), shel != null ? shel.maxZ : anchor.getZ());
        double topY = Math.max(res != null ? res.maxY : anchor.getY(), shel != null ? shel.maxY : anchor.getY());
        double bottomY = Math.min(res != null ? res.minY : anchor.getY(), shel != null ? shel.minY : anchor.getY());
        if (res == null && shel == null) {
            // 没有范围盒（老存档/跳过建图）时，按出生点周围兜底成一个假想结构体
            minX = anchor.getX() - 6; maxX = anchor.getX() + 7;
            minZ = anchor.getZ() - 6; maxZ = anchor.getZ() + 7;
            topY = anchor.getY() + 5; bottomY = anchor.getY() - 5;
        }

        int cx = (int) Math.floor((minX + maxX) / 2.0);
        int cz = (int) Math.floor((minZ + maxZ) / 2.0);
        int halfX = (int) Math.ceil((maxX - minX) / 2.0);
        int halfZ = (int) Math.ceil((maxZ - minZ) / 2.0);
        beginGroundCache();

        for (int ring = RV_STRUCTURE_MARGIN; ring <= RV_STRUCTURE_MARGIN + RV_SEARCH_RINGS; ring++) {
            BlockPos best = null;
            double bestDist = Double.MAX_VALUE;
            int ex = halfX + ring;
            int ez = halfZ + ring;
            for (int dx = -ex; dx <= ex; dx++) {
                for (int dz = -ez; dz <= ez; dz++) {
                    // 只取当前圈的外沿，内圈上一轮已经试过
                    if (Math.abs(dx) < ex && Math.abs(dz) < ez) {
                        continue;
                    }
                    int x = cx + dx;
                    int z = cz + dz;
                    BlockPos feet = surfaceAt(level, x, z);
                    if (feet == null) {
                        continue;
                    }
                    // 落点高度仍落在结构体竖向区间内 → 多半是楼板/屋顶，跳过。
                    // 水平「结构体外」判定只作快速预筛，真正的判据是 fitsRv 的车体净空校验。
                    if (feet.getY() + RV_CLEAR_HEIGHT > bottomY && feet.getY() < topY
                            && x >= minX - RV_STRUCTURE_MARGIN && x < maxX + RV_STRUCTURE_MARGIN
                            && z >= minZ - RV_STRUCTURE_MARGIN && z < maxZ + RV_STRUCTURE_MARGIN) {
                        continue;
                    }
                    if (!fitsRv(level, feet, 2) && !fitsRv(level, feet, 1)) {
                        continue;
                    }
                    double d = feet.distSqr(anchor);
                    if (d < bestDist) {
                        bestDist = d;
                        best = feet;
                    }
                }
            }
            if (best != null) {
                return best;
            }
        }
        return null;
    }

    /** 配置刷新点：若原地放不下车体，就近向外找一个能容纳车体的地表落点。 */
    private static BlockPos fitRvNear(ServerLevel level, BlockPos target) {
        BlockPos base = SixtySecondsSearchZones.findSafeSpot(level, target);
        if (base != null && (fitsRv(level, base, 2) || fitsRv(level, base, 1))) {
            return base;
        }
        beginGroundCache();
        for (int ring = 2; ring <= 12; ring++) {
            for (int dx = -ring; dx <= ring; dx++) {
                for (int dz = -ring; dz <= ring; dz++) {
                    if (Math.max(Math.abs(dx), Math.abs(dz)) < ring) {
                        continue; // 只扫当前圈外沿
                    }
                    BlockPos feet = surfaceAt(level, target.getX() + dx, target.getZ() + dz);
                    if (feet != null && (fitsRv(level, feet, 2) || fitsRv(level, feet, 1))) {
                        return feet;
                    }
                }
            }
        }
        return base;
    }

    /**
     * 求该列的落脚点：以 LostCities 的<b>地形基准高度</b>为锚，就近微调到真实表面。
     *
     * <p>{@link #baseGroundY} 给出的是自然地形（街道地面）的站立高度。真实表面可能因
     * 废墟残骸略高、因战损坑洞略低，故以基准为中心在
     * {@code [base - RV_SCAN_BELOW, base + RV_SCAN_ABOVE]} 内找离基准最近的可站立位，
     * <b>先向上、后向下</b>：
     * <ol>
     *   <li><b>向上</b>：基准位被残骸/车辆占据时抬到杂物顶面（最多抬 {@value #RV_SCAN_ABOVE} 格）；
     *       基准位本身空着则直接命中——这是绝大多数情况；</li>
     *   <li><b>向下</b>：向上整段都被实心塞满（基准被埋），或街道被炸穿形成坑洞时，
     *       往下落到坑底的支撑面上（最多降 {@value #RV_SCAN_BELOW} 格）。</li>
     * </ol>
     * 关键是<b>绝不会扫到屋顶</b>——锚点只认自然地形，
     * 而建筑方块不属于地形，向上 8 格已是放宽上限（车高也就 3.2 格）。</p>
     */
    private static BlockPos surfaceAt(ServerLevel level, int x, int z) {
        int base = baseGroundY(level, x, z);
        int below = Math.max(base - RV_SCAN_BELOW, level.getMinBuildHeight() + 1);
        int above = Math.min(base + RV_SCAN_ABOVE, level.getMaxBuildHeight() - RV_CLEAR_HEIGHT - 1);

        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        // 1) 先向上：基准位空着则直接命中；被占则抬到杂物顶面
        for (int feetY = base; feetY <= above; feetY++) {
            if (canStandAt(level, p, x, feetY, z)) {
                return new BlockPos(x, feetY, z);
            }
        }
        // 2) 再向下：向上整段被埋 / 街道被炸穿时，落到坑底的支撑面上
        for (int feetY = base - 1; feetY >= below; feetY--) {
            if (canStandAt(level, p, x, feetY, z)) {
                return new BlockPos(x, feetY, z);
            }
        }
        return null;
    }

    /**
     * 该站立位是否可用：<b>脚下一格</b>实心、无流体、有碰撞体（撑得住车），
     * 且<b>站立位本身</b>无碰撞体（车放得进去）。车体净空由 {@link #fitsRv} 另行校验。
     */
    private static boolean canStandAt(ServerLevel level, BlockPos.MutableBlockPos p, int x, int feetY, int z) {
        p.set(x, feetY - 1, z);
        BlockState ground = level.getBlockState(p);
        if (ground.isAir() || !ground.getFluidState().isEmpty()
                || ground.getCollisionShape(level, p).isEmpty()) {
            return false;
        }
        p.set(x, feetY, z);
        return level.getBlockState(p).getCollisionShape(level, p).isEmpty();
    }

    /**
     * 该落点能否容纳车体：{@code (2*radius+1)}² 底面 × {@value #RV_CLEAR_HEIGHT} 格净空（无碰撞、无流体），
     * 且脚下中心格实心无流体。
     */
    private static boolean fitsRv(ServerLevel level, BlockPos feet, int radius) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                for (int dy = 0; dy < RV_CLEAR_HEIGHT; dy++) {
                    p.set(feet.getX() + dx, feet.getY() + dy, feet.getZ() + dz);
                    BlockState s = level.getBlockState(p);
                    if (!s.getFluidState().isEmpty() || !s.getCollisionShape(level, p).isEmpty()) {
                        return false;
                    }
                }
            }
        }
        p.set(feet.getX(), feet.getY() - 1, feet.getZ());
        BlockState below = level.getBlockState(p);
        return below.getFluidState().isEmpty() && !below.getCollisionShape(level, p).isEmpty();
    }

    /** ocean 模式：在岛屿陆地（避水）上找一个双格净空、脚下有支撑的房车落点。 */
    private static BlockPos findIslandRvSpot(ServerLevel level, SixtySecondsIsland island, RandomSource rng) {
        for (int a = 0; a < 64; a++) {
            double ang = rng.nextDouble() * Math.PI * 2.0;
            double rad = Math.sqrt(rng.nextDouble()) * (island.radius * 0.9);
            int x = island.centerX + (int) Math.round(Math.cos(ang) * rad);
            int z = island.centerZ + (int) Math.round(Math.sin(ang) * rad);
            if (island.distSqr(x, z) > (double) island.radius * island.radius) {
                continue;
            }
            int y = findSurfaceY(level, x, z,
                    island.seaY + SixtySecondsIslandGenerator.HEIGHT_ABOVE_SEA - 2,
                    island.seaY - SixtySecondsIslandGenerator.DEPTH_BELOW_SEA);
            if (y < 0) {
                continue;
            }
            BlockPos feet = new BlockPos(x, y, z);
            if (!level.getFluidState(feet).isEmpty() || !level.getFluidState(feet.above()).isEmpty()) {
                continue;
            }
            if (!level.getBlockState(feet).isAir() || !level.getBlockState(feet.above()).isAir()) {
                continue;
            }
            return feet;
        }
        return null;
    }

    /** 在 [bottomY, topY] 区间内自顶向下扫描，返回首个「非空气、非流体、有碰撞」方块之上的 Y（脚部站立位）。 */
    private static int findSurfaceY(ServerLevel level, int x, int z, int topY, int bottomY) {
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int y = topY; y >= bottomY; y--) {
            p.set(x, y, z);
            BlockState s = level.getBlockState(p);
            if (!s.isAir() && s.getFluidState().isEmpty() && !s.getCollisionShape(level, p).isEmpty()) {
                return y + 1;
            }
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────
    // 强制加载区块（复用 setChunkForced；单车一环，随车移动搬迁）
    // ─────────────────────────────────────────────────────────────────

    /** 让强载环跟随房车当前所在区块；跨区块时先解除旧环再强载新环。 */
    private static void followForced(ServerLevel level, SixtySecondsState.TeamData team, Entity rv) {
        ChunkPos cur = new ChunkPos(rv.blockPosition());
        if (team.rvForcedChunkX == cur.x && team.rvForcedChunkZ == cur.z) {
            return;
        }
        releaseForced(level, team);
        for (int dx = -FORCE_RADIUS; dx <= FORCE_RADIUS; dx++) {
            for (int dz = -FORCE_RADIUS; dz <= FORCE_RADIUS; dz++) {
                level.setChunkForced(cur.x + dx, cur.z + dz, true);
            }
        }
        team.rvForcedChunkX = cur.x;
        team.rvForcedChunkZ = cur.z;
    }

    /** 解除本队房车此前强载的区块环。 */
    private static void releaseForced(ServerLevel level, SixtySecondsState.TeamData team) {
        if (team.rvForcedChunkX == Integer.MIN_VALUE) {
            return;
        }
        for (int dx = -FORCE_RADIUS; dx <= FORCE_RADIUS; dx++) {
            for (int dz = -FORCE_RADIUS; dz <= FORCE_RADIUS; dz++) {
                level.setChunkForced(team.rvForcedChunkX + dx, team.rvForcedChunkZ + dz, false);
            }
        }
        team.rvForcedChunkX = Integer.MIN_VALUE;
        team.rvForcedChunkZ = Integer.MIN_VALUE;
    }

    /** 清除世界中所有房车实体（先收集后 discard，避免遍历中并发修改 NPE）。 */
    private static void discardAllRvs(ServerLevel level) {
        List<Entity> toRemove = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof SixtySecondsRvEntity) {
                toRemove.add(entity);
            }
        }
        for (Entity entity : toRemove) {
            if (!entity.isRemoved()) {
                entity.discard();
            }
        }
    }

    /** 扫除上一把残留的孤立房车——teamId 不属于当前任何队伍的实体。每局首次巡检调用一次。 */
    private static void discardOrphanedRvs(ServerLevel level, SixtySecondsState.Data data) {
        // 收集当前有效 teamId
        Set<Integer> validIds = new java.util.HashSet<>();
        for (SixtySecondsState.TeamData team : data.teams.values()) {
            validIds.add(team.teamId);
        }
        List<Entity> orphans = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof SixtySecondsRvEntity rv && !validIds.contains(rv.teamId())) {
                orphans.add(entity);
            }
        }
        if (!orphans.isEmpty()) {
            for (Entity entity : orphans) {
                if (!entity.isRemoved()) {
                    entity.discard();
                }
            }
        }
    }
}
