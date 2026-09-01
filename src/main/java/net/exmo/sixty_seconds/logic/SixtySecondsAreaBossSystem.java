package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.SixtySecondsBalance;
import net.exmo.sixty_seconds.SixtySecondsDayCycle;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.exmo.sixty_seconds.config.SixtySecondsConfigStore;
import net.exmo.sixty_seconds.entity.SixtySecondsBossEntity;
import net.exmo.sixty_seconds.island.SixtySecondsIsland;
import net.exmo.sixty_seconds.island.SixtySecondsIslands;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 区域固定 Boss 系统（与 {@link SixtySecondsPveSystem} 夜晚「尸潮领主」并行，互不挤占名额）：
 * <ul>
 *   <li><b>4-5 星区域 / 4-5 星岛屿固定 Boss</b>：在白天相位周期性（每 5 秒）检查并<b>固定刷新</b>
 *       （不掷概率、与游戏天数无关）。每个 4/5 星区域或岛屿对应至多一只存活 Boss；缺位时立即补刷，
 *       确保玩家进入该区域时 Boss 必然存在。</li>
 *   <li><b>世界上限</b>：同时存活的区域 Boss 总数受 {@link SixtySecondsBalance#AREA_BOSS_WORLD_CAP}
 *       限制。超出上限时暂停刷新；系统会<b>及时清理不活跃 Boss</b>（所在区块未加载，或长时间无玩家在附近且
 *       久未被攻击），释放名额，待玩家重入区域时重新刷新。</li>
 *   <li><b>击杀冷却</b>：若某区域/岛屿的 Boss 被玩家击杀，则 {@link SixtySecondsBalance#AREA_BOSS_KILL_COOLDOWN_DAYS}
 *       天内该区域不再刷新（玩家在此期间进入该区域不会看到 Boss）。</li>
 *   <li><b>1-5 星「伤害 Boss」</b>：每局仅一只，第 {@link SixtySecondsBalance#DAMAGE_BOSS_SPAWN_DAY}
 *       天降临（保留原有逻辑）。</li>
 * </ul>
 * 区域 Boss 均 <b>不登记</b>进 {@code SixtySecondsPveSystem.ACTIVE_BOSS} 全局唯一锁，可多只并存。
 */
public final class SixtySecondsAreaBossSystem {

    /** 区域固定 Boss 实体 tag（区分于夜晚尸潮领主，便于局末清理与按区域查存活）。 */
    public static final String AREA_BOSS_TAG = "sixty_seconds_area_boss";
    /** 区域固定 Boss 归属区域 tag 前缀（后接区域标识：override_<i> 或 island_<id>）。 */
    public static final String AREA_BOSS_REGION_TAG_PREFIX = "sixty_seconds_area_boss_region_";

    /** level → 上次执行扫描的 gameTime（用于限频，避免每 tick 都跑刷新/清理）。 */
    private static final Map<ServerLevel, Long> LAST_CHECK = new WeakHashMap<>();
    /** level → 已刷过伤害 Boss（每局仅一只）。 */
    private static final Set<ServerLevel> DAMAGE_BOSS_SPAWNED = new HashSet<>();
    /** Boss UUID → 最近一次受玩家攻击的 gameTime（用于不活跃回收判定）。 */
    private static final Map<UUID, Long> LAST_ATTACK_TICK = new HashMap<>();
    /** Boss UUID → 最近一次有玩家在其附近的 gameTime（用于不活跃回收判定）。 */
    private static final Map<UUID, Long> LAST_PLAYER_NEARBY_TICK = new HashMap<>();

    private SixtySecondsAreaBossSystem() {
    }

    /** 主 tick（由 {@code SixtySecondsManager} DAY 相位每 tick 调）。 */
    public static void tick(ServerLevel level) {
        SixtySecondsState.Data data = SixtySecondsState.get(level);
        if (data == null) {
            return;
        }
        long now = level.getGameTime();
        // 夜晚子相位有尸潮领主，区域 Boss 不抢占节奏；仅在白天子相位运行
        if (SixtySecondsDayCycle.isNight(data, now)) {
            return;
        }
        // 限频：每 AREA_BOSS_CHECK_INTERVAL tick 才执行一次刷新/清理
        long last = LAST_CHECK.getOrDefault(level, -1L);
        if (now - last < SixtySecondsBalance.AREA_BOSS_CHECK_INTERVAL) {
            return;
        }
        LAST_CHECK.put(level, now);

        cleanupInactiveBosses(level, now);
        tryAreaBosses(level, data, now);
        // 伤害 Boss（每局一只，自带 once-only 保护；白天相位下到对应天数即刷）
        tryDamageBoss(level, data);
    }

    // ── 4-5 星区域 / 岛屿固定 Boss ──────────────────────────────────────
    private static void tryAreaBosses(ServerLevel level, SixtySecondsState.Data data, long now) {
        int live = countLiveAreaBosses(level);
        if (live >= SixtySecondsBalance.AREA_BOSS_WORLD_CAP) {
            return; // 世界名额已满，等待清理不活跃 Boss 释放
        }
        List<RegionSpawn> regions = collectRegions(level);
        for (RegionSpawn r : regions) {
            if (live >= SixtySecondsBalance.AREA_BOSS_WORLD_CAP) {
                break;
            }
            // 该区域已有存活 Boss → 跳过
            if (hasLiveBoss(level, r.key)) {
                continue;
            }
            // 击杀冷却中（玩家杀死了该区域 Boss，冷却期内不刷）
            Integer killedDay = data.areaBossKillCooldownDay.get(r.key);
            if (killedDay != null && data.dayNumber - killedDay < SixtySecondsBalance.AREA_BOSS_KILL_COOLDOWN_DAYS) {
                continue;
            }
            BlockPos spot = resolveRegionSpot(level, r.box);
            if (spot == null) {
                continue;
            }
            int bossLevel = Mth.clamp(r.level - 1, 1, SixtySecondsBalance.AREA_BOSS_MAX_LEVEL);
            SixtySecondsBossEntity.BossVariant variant = SixtySecondsPveSystem.pickBossVariantPublic(
                    level.random, data.dayNumber);
            SixtySecondsBossEntity boss = SixtySecondsPveSystem.spawnBoss(
                    level, spot, bossLevel, false, variant, false);
            if (boss == null) {
                continue;
            }
            boss.addTag(AREA_BOSS_TAG);
            boss.addTag(AREA_BOSS_REGION_TAG_PREFIX + r.key);
            live++;
        }
    }

    /** 枚举所有需要固定 Boss 的 4/5 星区域与 4/5 星岛屿。 */
    private static List<RegionSpawn> collectRegions(ServerLevel level) {
        List<RegionSpawn> out = new ArrayList<>();
        SixtySecondsConfig config = SixtySecondsConfigStore.current(level).orElse(null);
        if (config != null && config.areaLevelOverrides != null) {
            for (int i = 0; i < config.areaLevelOverrides.size(); i++) {
                SixtySecondsConfig.LevelRegion region = config.areaLevelOverrides.get(i);
                if (region == null || region.level < SixtySecondsBalance.AREA_BOSS_MIN_AREA_LEVEL) {
                    continue;
                }
                AABB box = regionBox(region);
                if (box == null) {
                    continue;
                }
                out.add(new RegionSpawn("override_" + i, boxToCenter(box), box, region.level));
            }
        }
        // 4/5 星岛屿
        for (SixtySecondsIsland island : SixtySecondsIslands.islandList(level)) {
            if (island == null || island.level < SixtySecondsBalance.AREA_BOSS_MIN_AREA_LEVEL) {
                continue;
            }
            AABB box = island.cellBox();
            if (box == null) {
                continue;
            }
            BlockPos center = new BlockPos(island.centerX, island.seaY, island.centerZ);
            out.add(new RegionSpawn("island_" + island.id, center, box, island.level));
        }
        return out;
    }

    private static BlockPos boxToCenter(AABB box) {
        return BlockPos.containing(box.getCenter());
    }

    /** 统计当前世界存活的区域 Boss 数量（世界名额用）。 */
    private static int countLiveAreaBosses(ServerLevel level) {
        int n = 0;
        for (Entity e : level.getAllEntities()) {
            if (e instanceof SixtySecondsBossEntity boss && boss.isAlive() && !boss.isRemoved()
                    && boss.getTags().contains(AREA_BOSS_TAG)) {
                n++;
            }
        }
        return n;
    }

    /** 该区域标识是否已有存活 Boss。 */
    private static boolean hasLiveBoss(ServerLevel level, String key) {
        String full = AREA_BOSS_REGION_TAG_PREFIX + key;
        for (Entity e : level.getAllEntities()) {
            if (e instanceof SixtySecondsBossEntity boss && boss.isAlive() && !boss.isRemoved()
                    && boss.getTags().contains(full)) {
                return true;
            }
        }
        return false;
    }

    /** 读取 Boss 归属的区域标识（无则 null）。 */
    public static String regionKeyOf(SixtySecondsBossEntity boss) {
        for (String t : boss.getTags()) {
            if (t.startsWith(AREA_BOSS_REGION_TAG_PREFIX)) {
                return t.substring(AREA_BOSS_REGION_TAG_PREFIX.length());
            }
        }
        return null;
    }

    /** Boss 受玩家攻击时由实体回调：刷新受击时间戳（避免被不活跃回收）。 */
    public static void noteBossAttacked(SixtySecondsBossEntity boss) {
        if (boss.level() instanceof ServerLevel sl) {
            LAST_ATTACK_TICK.put(boss.getUUID(), sl.getGameTime());
        }
    }

    /** Boss 死亡时由实体回调：记录该区域击杀冷却（玩家击杀则在冷却期内不再刷新）。 */
    public static void onAreaBossKilled(ServerLevel level, SixtySecondsBossEntity boss) {
        String key = regionKeyOf(boss);
        if (key == null) {
            return;
        }
        SixtySecondsState.Data data = SixtySecondsState.get(level);
        if (data != null) {
            data.areaBossKillCooldownDay.put(key, data.dayNumber);
        }
    }

    /** 清理不活跃区域 Boss：区块未加载，或长时间无玩家在附近且久未被攻击 → 移除（释放世界名额）。 */
    private static void cleanupInactiveBosses(ServerLevel level, long now) {
        int r2 = SixtySecondsBalance.AREA_BOSS_PLAYER_RADIUS * SixtySecondsBalance.AREA_BOSS_PLAYER_RADIUS;
        List<SixtySecondsBossEntity> toRemove = new ArrayList<>();
        for (Entity e : level.getAllEntities()) {
            if (!(e instanceof SixtySecondsBossEntity boss) || !boss.getTags().contains(AREA_BOSS_TAG)) {
                continue;
            }
            UUID id = boss.getUUID();
            // 更新「玩家在附近」时间戳
            boolean near = false;
            for (ServerPlayer p : level.players()) {
                if (p.distanceToSqr(boss) < r2) {
                    near = true;
                    break;
                }
            }
            if (near) {
                LAST_PLAYER_NEARBY_TICK.put(id, now);
            }
            long lastNear = LAST_PLAYER_NEARBY_TICK.getOrDefault(id, 0L);
            long lastAtk = LAST_ATTACK_TICK.getOrDefault(id, 0L);
            boolean chunkUnloaded = !level.hasChunkAt(boss.blockPosition());
            boolean inactive = (now - lastNear > SixtySecondsBalance.AREA_BOSS_INACTIVE_DESPAWN_TICKS)
                    && (now - lastAtk > SixtySecondsBalance.AREA_BOSS_INACTIVE_DESPAWN_TICKS);
            if (chunkUnloaded || inactive) {
                toRemove.add(boss);
                LAST_ATTACK_TICK.remove(id);
                LAST_PLAYER_NEARBY_TICK.remove(id);
            }
        }
        for (SixtySecondsBossEntity boss : toRemove) {
            if (!boss.isRemoved()) {
                boss.discard();
            }
        }
    }

    /** 区域落点：优先取落在该区域盒内的已登记 Boss 刷新点；否则在盒内随机选可站立落点。 */
    private static BlockPos resolveRegionSpot(ServerLevel level, AABB box) {
        BlockPos bound = findBoundSpawnPoint(level, box);
        if (bound != null) {
            return bound;
        }
        if (box == null) {
            return null;
        }
        for (int attempt = 0; attempt < 16; attempt++) {
            int x = (int) Math.round(box.minX + level.getRandom().nextDouble() * (box.maxX - box.minX));
            int z = (int) Math.round(box.minZ + level.getRandom().nextDouble() * (box.maxZ - box.minZ));
            int midY = (int) Math.round((box.minY + box.maxY) / 2.0);
            for (int dy = 16; dy >= -16; dy--) {
                BlockPos pos = new BlockPos(x, midY + dy, z);
                if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                        && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
                    return pos;
                }
            }
        }
        return null;
    }

    // ── 1-5 星「伤害 Boss」（每局一只）──────────────────────────────────
    private static void tryDamageBoss(ServerLevel level, SixtySecondsState.Data data) {
        if (DAMAGE_BOSS_SPAWNED.contains(level)) {
            return;
        }
        if (data.dayNumber < SixtySecondsBalance.DAMAGE_BOSS_SPAWN_DAY - SixtySecondsDifficulty.bossEarlyDayShift(SixtySecondsDifficulty.get(level))) {
            return;
        }
        BlockPos spot = resolveDamageSpot(level, data);
        if (spot == null) {
            return;
        }
        // 伤害 Boss：破坏者变体 + 终焉级数值，但等级取当前天数（封顶 5），近战固定伤害由 tag 兜底
        int bossLevel = Mth.clamp(data.dayNumber, 1, SixtySecondsBalance.BOSS_MAX_LEVEL);
        SixtySecondsBossEntity boss = SixtySecondsPveSystem.spawnBoss(
                level, spot, bossLevel, true, SixtySecondsBossEntity.BossVariant.RAVAGER, false);
        if (boss == null) {
            return;
        }
        boss.addTag(SixtySecondsBossEntity.DAMAGE_BOSS_TAG);
        // 覆盖名字为「灾祸领主」（固定伤害 Boss 专属称谓）
        Component name = Component.translatable("entity.sixty_seconds.sixty_seconds_damage_boss", bossLevel)
                .withStyle(ChatFormatting.DARK_PURPLE);
        boss.setCustomName(name);
        boss.setCustomNameVisible(false);
        DAMAGE_BOSS_SPAWNED.add(level);
        // 全服播报
        Component message = Component.translatable(
                "message.sixty_seconds.sixty_seconds.damage_boss_spawned",
                spot.getX(), spot.getY(), spot.getZ()).withStyle(ChatFormatting.DARK_PURPLE);
        for (ServerPlayer player : level.players()) {
            player.displayClientMessage(message, false);
            player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.4F);
        }
    }

    /** 伤害 Boss 落点：优先取任意已登记 Boss 刷新点；否则某队伍探索区锚点附近随机远刷。 */
    private static BlockPos resolveDamageSpot(ServerLevel level, SixtySecondsState.Data data) {
        BlockPos bound = findBoundSpawnPoint(level, null);
        if (bound != null) {
            return bound;
        }
        List<SixtySecondsState.TeamData> withAnchor = new ArrayList<>();
        for (SixtySecondsState.TeamData team : data.teams.values()) {
            BlockPos anchor = SixtySecondsDefenseSystem.assaultAnchor(team);
            if (anchor != null) {
                withAnchor.add(team);
            }
        }
        if (withAnchor.isEmpty()) {
            return null;
        }
        SixtySecondsState.TeamData team = withAnchor.get(level.getRandom().nextInt(withAnchor.size()));
        BlockPos anchor = SixtySecondsDefenseSystem.assaultAnchor(team);
        AABB zone = team.searchZoneBox;
        return SixtySecondsPveSystem.findSpawnSpot(level, anchor,
                SixtySecondsBalance.AMBIENT_SPAWN_MIN_DIST,
                SixtySecondsBalance.AMBIENT_SPAWN_RAND_DIST, 6, 24, zone, data);
    }

    // ── 工具 ────────────────────────────────────────────────────────────
    /** 在 config.bossSpawnPoints 中找一个落在指定盒内的刷新点；box=null 时取第一个刷新点。 */
    private static BlockPos findBoundSpawnPoint(ServerLevel level, AABB box) {
        SixtySecondsConfig config = SixtySecondsConfigStore.current(level).orElse(null);
        if (config == null || config.bossSpawnPoints == null || config.bossSpawnPoints.isEmpty()) {
            return null;
        }
        for (SixtySecondsConfig.Vec v : config.bossSpawnPoints) {
            if (v == null) {
                continue;
            }
            BlockPos pos = v.toBlockPos();
            if (box == null || box.contains(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5)) {
                // 校验落点可站立（否则就近找地面）
                return sanitizeSpot(level, pos);
            }
        }
        return null;
    }

    /** 落点不可站立时，就近垂直扫描找可站立格；仍不可站立则原样返回（spawnBoss 内部 setPos 兜底）。 */
    private static BlockPos sanitizeSpot(ServerLevel level, BlockPos pos) {
        if (level.getBlockState(pos).isAir() && level.getBlockState(pos.above()).isAir()
                && level.getBlockState(pos.below()).isSolidRender(level, pos.below())) {
            return pos;
        }
        for (int dy = 8; dy >= -8; dy--) {
            BlockPos p = pos.above(dy);
            if (level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir()
                    && level.getBlockState(p.below()).isSolidRender(level, p.below())) {
                return p;
            }
        }
        return pos;
    }

    /** LevelRegion 两角取正序得到 AABB；任一角缺失返回 null。 */
    private static AABB regionBox(SixtySecondsConfig.LevelRegion region) {
        if (region == null || region.min == null || region.max == null) {
            return null;
        }
        return new AABB(
                Math.min(region.min.x, region.max.x), Math.min(region.min.y, region.max.y),
                Math.min(region.min.z, region.max.z),
                Math.max(region.min.x, region.max.x) + 1, Math.max(region.min.y, region.max.y) + 1,
                Math.max(region.min.z, region.max.z) + 1);
    }

    /** 局末清理：清掉所有区域固定 Boss + 伤害 Boss，重置追踪表。 */
    public static void reset(ServerLevel level) {
        List<Entity> toRemove = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof SixtySecondsBossEntity
                    && (entity.getTags().contains(AREA_BOSS_TAG)
                            || entity.getTags().contains(SixtySecondsBossEntity.DAMAGE_BOSS_TAG))) {
                toRemove.add(entity);
            }
        }
        for (Entity entity : toRemove) {
            if (!entity.isRemoved()) {
                entity.discard();
            }
        }
        LAST_CHECK.remove(level);
        LAST_ATTACK_TICK.clear();
        LAST_PLAYER_NEARBY_TICK.clear();
        DAMAGE_BOSS_SPAWNED.remove(level);
    }

    /** 区域刷新描述（区域标识 + 包围盒 + 星级）。 */
    private static final class RegionSpawn {
        final String key;
        final BlockPos center;
        final AABB box;
        final int level;

        RegionSpawn(String key, BlockPos center, AABB box, int level) {
            this.key = key;
            this.center = center;
            this.box = box;
            this.level = level;
        }
    }
}
