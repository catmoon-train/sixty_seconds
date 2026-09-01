package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.Sixty_seconds;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.exmo.sixty_seconds.config.SixtySecondsConfigStore;
import net.exmo.sixty_seconds.entity.OceanSeaMonsterEntity;
import net.exmo.sixty_seconds.entity.OceanSharkEntity;
import net.exmo.sixty_seconds.entity.OceanFloorMonsterEntity;
import net.exmo.sixty_seconds.entity.OceanFaunaEntity;
import net.exmo.sixty_seconds.entity.OceanTitanEntity;
import net.exmo.sixty_seconds.init.ModOceanEntities;
import net.exmo.sixty_seconds.registry.ModEntities;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.exmo.sixty_seconds.registry.ModEffects;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * 海洋生物刷新系统。
 *
 * <h3>鲨鱼（受控刷新，上限/节奏在本类内把关）</h3>
 * <p>{@code ocean_shark} 不再走 biome_modifier 的 add_spawns（自定义海洋生成器在
 * CHUNK_GENERATION 阶段会把鲨鱼刷在虚空 Y 并瞬间死亡）；改由本类在海洋维度
 * {@code tick} 内以受控概率刷新，数量上限与局部密度上限读取
 * {@code Sixty_seconds.SHARK_GLOBAL_CAP} / {@code SHARK_AREA_CAP} / {@code SHARK_AREA_RADIUS}，
 * 避免“一进海域就刷一大堆、数量无上限”。变体稀有度由实体 {@code finalizeSpawn} 内按天数随机决定。
 *
 * <h3>海怪 KRAKEN / SERPENT（手动刷，含出场特效）</h3>
 * <p>保留手动刷怪以维持播报/浓雾/音效等出场特效。</p>
 *
 * <h3>利维坦 LEVIATHAN（定时刷）</h3>
 * <p>仅当玩家处于<b>海洋维度</b>时，在游戏天数达到 {@link #LEVIATHAN_PERIOD_DAYS}（6）及其倍数天（6、12、18…）刷一只；
 * 与鲨鱼/海怪独立、不计入刷怪上限、可与其他 Boss 共存，优先刷在玩家附近海域。游戏开始首日（非倍数天）不会刷。</p>
 *
 * <h3>难度按天数比例递增</h3>
 * <p>基础概率 × dayRatio = 当前概率。dayRatio = currentDay / totalDays。
 * 前四天有额外压降（dayRatio × 0.3），保证前几天几乎不刷强怪。
 *
 * <h3>海怪出场特效</h3>
 * <ul>
 *   <li>全服金色播报</li>
 *   <li>附近玩家获得 {@code VISION_FOG} 浓雾效果（8s）</li>
 *   <li>深海守卫号角音效</li>
 *   <li>海浪粒子爆发</li>
 * </ul>
 */
public final class OceanCreatureSpawner {

    public static final int CHECK_INTERVAL = 20 * 14;

    /** 利维坦（LEVIATHAN）固定刷新周期：仅在游戏天数为 {@code 6} 及其倍数（6、12、18…）时刷新（固定刷新，不计入深海 Boss 全局唯一限制）。 */
    public static final int LEVIATHAN_PERIOD_DAYS = 6;

    private static final int MAX_NEARBY_MONSTERS = 2;
    private static final double NEARBY_RADIUS = 64.0;
    private static final double MONSTER_FOG_RADIUS = 32.0; // 海怪浓雾作用半径
    private static final int FLOOR_MONSTER_AREA_CAP = 4;  // 海底小怪局部上限
    private static final int FAUNA_AREA_CAP = 5;          // 海洋生物群系（第二批次）局部上限
    private static final int TITAN_CAP = 1;               // 海洋霸主全局上限（同时最多 1 只）

    private static final int SPAWN_MIN_DIST = 14;
    private static final int SPAWN_MAX_DIST = 36;

    private OceanCreatureSpawner() {}

    /**
     * 各维度当前存活鲨鱼数（受控刷新上限用）。通过在鲨鱼加入/离开维度时增减维护，
     * 避免 {@code tick} 每 14 秒用全世界巨型 AABB 全量扫描实体（O(全部实体)，实体多时极卡）。
     * 维度卸载后随 WeakHashMap 的 key 被 GC 自然回收。
     */
    private static final Map<ServerLevel, Integer> SHARK_COUNT = new WeakHashMap<>();

    /**
     * 对所有在线存活探索中玩家做海洋生物刷新判定。
     * 刷新概率 = 基础概率 × dayRatio（前四天额外 ×0.3）。
     */
    public static void tick(ServerLevel level) {
        SixtySecondsConfig config = SixtySecondsConfigStore.current(level).orElse(null);
        if (config == null || !config.oceanCreaturesEnabled) return;

        // 游戏天数以主对局（主世界）为准：海洋维度自身不推进天数，故取主世界的日数，
        // 使海洋模式刷怪强度随主对局天数正常变化，不受所在维度影响。
        boolean inOcean = level.dimension() == SixtySeconds.OCEAN_DIMENSION;
        int dayNumber = resolveGameDay(level, config);

        // ── 天数比例：dayRatio = currentDay / totalDays ─────────────
        double dayRatio = (double) dayNumber / Math.max(1, config.totalDays);
        boolean night = level.isNight();

        // 前四天额外压降 ×0.3（保证前几天几乎不刷强怪）
        double earlyDayMult = dayNumber <= 4 ? 0.3 : 1.0;
        // 海怪基础概率
        double monsterBase = (night ? 0.042 : 0.007) * dayRatio * earlyDayMult;

        RandomSource random = level.getRandom();
        double spawnMult = net.exmo.sixty_seconds.traits.SixtySecondsTraitSystem.spawnMultiplier(level);

        // 利维坦定时刷新（与鲨鱼/海怪独立，可共存；仅海洋维度 + 第6天/倍数天）
        tickLeviathan(level, dayNumber, inOcean);

        // 世界范围内鲨鱼总数（受 SHARK_GLOBAL_CAP 约束）—— 由计数器维护，不再全图扫描
        int globalSharks = getSharkCount(level);

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()
                    || !net.exmo.sixty_seconds.bridge.GameUtils.isPlayerAliveAndSurvival(player)) {
                continue;
            }
            if (!net.exmo.sixty_seconds.arena.SixtySecondsSearchZones.isInSearchZone(player) && !inOcean) {
                continue;
            }
            if (!isNearOpenWater(level, player.blockPosition())) {
                continue;
            }

            int nearbyMonsters = countNearby(level, player, OceanSeaMonsterEntity.class, NEARBY_RADIUS);

            // ── 海怪刷新（KRAKEN / SERPENT，含出场特效）───────────────────
            if (nearbyMonsters < MAX_NEARBY_MONSTERS && random.nextDouble() < monsterBase * spawnMult) {
                BlockPos spot = findWaterSpot(level, player.blockPosition(),
                        SPAWN_MIN_DIST + 8, SPAWN_MAX_DIST + 12, random);
                if (spot != null) {
                    OceanSeaMonsterEntity monster = spawnSeaMonster(level, spot, random, dayRatio,
                            net.minecraft.util.Mth.clamp(1 + (int) (dayRatio * 4), 1,
                                    net.exmo.sixty_seconds.SixtySecondsBalance.BOSS_MAX_LEVEL), null);
                    if (monster != null) {
                        announceSeaMonster(level, monster, player);
                    }
                }
            }

            // ── 鲨鱼刷新 ───────────────────────────────────────────────
            // 不再依赖 biome_modifier 的 add_spawns：自定义海洋生成器在 CHUNK_GENERATION
            // 阶段会把鲨鱼刷在虚空 Y 并瞬间“掉出世界”死亡，且每区块都刷 → 洪流。
            // 改为这里可控刷新：Y 由 findWaterSpot 保证落在真实水面，受总数/局部/天数约束。
            int areaSharks = countNearby(level, player, OceanSharkEntity.class, Sixty_seconds.SHARK_AREA_RADIUS);
            if (globalSharks < Sixty_seconds.SHARK_GLOBAL_CAP
                    && areaSharks < Sixty_seconds.SHARK_AREA_CAP
                    && random.nextDouble() < 0.35 * dayRatio * earlyDayMult * spawnMult) {
                BlockPos spot = findWaterSpot(level, player.blockPosition(),
                        SPAWN_MIN_DIST, SPAWN_MAX_DIST, random);
                if (spot != null) {
                    spawnShark(level, spot, random, dayRatio);
                }
            }
        }

        // ── 深海 Boss 刷新（ABYSS_KRAKEN / TRENCH_SERPENT / SUNKEN_LEVIATHAN）──
        // 规则：全局仅存在一个（所有深海 Boss 共享位置）；前三天不刷；一天至多尝试刷新一次；
        // 有概率（随难度/天数浮动，非必刷）；仅在玩家真正处于海里、贴海底时刷新（房子/庇护所内不刷）。
        {
            SixtySecondsState.Data data = SixtySecondsState.get(level);
            if (dayNumber > 3) {
                int deepSeaCount = countDeepSeaBosses(level);
                boolean attemptedToday = data.deepSeaBossLastAttemptDay == dayNumber;
                if (deepSeaCount < 1 && !attemptedToday) {
                    ServerPlayer trigger = null;
                    for (ServerPlayer player : level.players()) {
                        if (player.isSpectator() || player.isCreative()
                                || !net.exmo.sixty_seconds.bridge.GameUtils.isPlayerAliveAndSurvival(player)) {
                            continue;
                        }
                        if (!player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) continue; // 必须在水里，排除房子/庇护所
                        if (!isNearSeafloor(level, player.blockPosition())) continue;
                        trigger = player;
                        break;
                    }
                    if (trigger != null) {
                        // 概率随天数（dayRatio）与难度/特质（spawnMult）浮动；非必刷
                        double prob = 0.18 * dayRatio * earlyDayMult * spawnMult;
                        if (random.nextDouble() < prob) {
                            OceanSeaMonsterEntity boss = spawnSeafloorBoss(level, trigger.blockPosition(), random);
                            if (boss != null) announceSeaMonster(level, boss, trigger);
                        }
                        data.deepSeaBossLastAttemptDay = dayNumber; // 一天至多尝试一次
                    }
                }
            }
        }

        // ── 海底小怪刷新（仅贴近海底的玩家附近触发，远离海底由实体自行消失）──
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()
                    || !net.exmo.sixty_seconds.bridge.GameUtils.isPlayerAliveAndSurvival(player)) {
                continue;
            }
            if (!isNearSeafloor(level, player.blockPosition())) continue;
            int nearbyFloor = countNearby(level, player, OceanFloorMonsterEntity.class, NEARBY_RADIUS);
            if (nearbyFloor < FLOOR_MONSTER_AREA_CAP
                    && random.nextDouble() < 0.06 * dayRatio * earlyDayMult * spawnMult) {
                BlockPos spot = findSeafloorSpot(level, player.blockPosition(),
                        SPAWN_MIN_DIST, SPAWN_MAX_DIST, random);
                if (spot != null) spawnFloorMonster(level, spot, random);
            }
        }

        // ── 海洋生物群系（第二批次 10 变体）：水中自然刷新，与海底小怪共用上限口径 ──
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()
                    || !net.exmo.sixty_seconds.bridge.GameUtils.isPlayerAliveAndSurvival(player)) {
                continue;
            }
            int nearbyFauna = countNearby(level, player, OceanFaunaEntity.class, NEARBY_RADIUS);
            if (nearbyFauna < FAUNA_AREA_CAP
                    && random.nextDouble() < 0.05 * dayRatio * earlyDayMult * spawnMult) {
                BlockPos spot = findWaterSpot(level, player.blockPosition(),
                        SPAWN_MIN_DIST, SPAWN_MAX_DIST, random);
                if (spot != null) spawnOceanFauna(level, spot, random);
            }
        }

        // ── 海洋霸主（10 个独立建模 Boss）：低概率、全局限 1 只 ──
        if (countNearbyTitans(level) < TITAN_CAP
                && random.nextDouble() < 0.0035 * dayRatio * earlyDayMult * spawnMult) {
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator() || player.isCreative()
                        || !net.exmo.sixty_seconds.bridge.GameUtils.isPlayerAliveAndSurvival(player)) {
                    continue;
                }
                BlockPos spot = findWaterSpot(level, player.blockPosition(),
                        SPAWN_MIN_DIST + 8, SPAWN_MAX_DIST + 8, random);
                if (spot == null) continue;
                OceanTitanEntity.Variant[] vs = OceanTitanEntity.Variant.values();
                int lv = net.minecraft.util.Mth.clamp(1 + (int) (dayRatio * 4), 1,
                        net.exmo.sixty_seconds.SixtySecondsBalance.BOSS_MAX_LEVEL);
                spawnTitan(level, spot, vs[random.nextInt(vs.length)], lv);
                break;
            }
        }
    }

    /** 统计当前维度的海洋霸主数量。 */
    private static int countNearbyTitans(ServerLevel level) {
        int n = 0;
        for (net.minecraft.world.entity.Entity e : level.getAllEntities()) {
            if (e instanceof OceanTitanEntity && e.isAlive()) n++;
        }
        return n;
    }

    /**
     * 解析当前应使用的“游戏天数”：
     * 海洋模式启动时，海洋维度即是对局主维度（所有玩家都在海洋维度，与主世界无关，
     * 天数在海洋维度自身推进），故优先取“当前维度”的日数；
     * 普通模式对局在主世界、玩家赴海洋远征时，再回退取主世界日数。
     * 两者均无有效日数时回退 totalDays。
     */
    private static int resolveGameDay(ServerLevel level, @Nullable SixtySecondsConfig config) {
        int totalDays = config != null ? config.totalDays : 7;
        // 1) 优先当前维度（海洋模式：海洋即主维度，自身推进天数）
        int here = SixtySecondsState.get(level).dayNumber;
        if (here > 0) return here;
        // 2) 回退主世界（普通模式：对局在主世界，海洋维度只是远征附加维度）
        ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
        if (overworld != null) {
            int d = SixtySecondsState.get(overworld).dayNumber;
            if (d > 0) return d;
        }
        return Math.max(1, totalDays);
    }

    /**
     * 利维坦（LEVIATHAN）固定刷新规则：
     * <ul>
     *   <li>仅在玩家处于<b>海洋维度</b>（{@code inOcean}）时才允许刷新；</li>
     *   <li>仅在游戏天数达到 {@link #LEVIATHAN_PERIOD_DAYS}（6）及其<b>倍数天</b>（6、12、18…）刷新，此前不刷；</li>
     *   <li>当天只刷新一次（记录 {@code leviathanLastSpawnDay}），避免一日内重复触发；</li>
     * </ul>
     * 与鲨鱼/海怪完全独立，不计入 {@code MAX_NEARBY_MONSTERS} 上限，可与其他 Boss 共存。
     * 优先刷新在最近存活玩家的附近开阔海域。
     *
     * @param dayNumber 当前由对局推进的游戏天数（SixtySecondsState.Data.dayNumber）
     * @param inOcean   当前维度是否为海洋维度（只有海洋维度才允许利维坦固定刷新）
     */
    private static void tickLeviathan(ServerLevel level, int dayNumber, boolean inOcean) {
        SixtySecondsState.Data data = SixtySecondsState.get(level);
        if (data == null) return;
        // ① 仅当玩家处于海洋维度时才允许利维坦固定刷新
        if (!inOcean) return;
        // ② 仅在第 LEVIATHAN_PERIOD_DAYS（6）天及其倍数天刷新（此前不刷，杜绝游戏开始首日即刷）
        if (dayNumber < LEVIATHAN_PERIOD_DAYS) return;
        if (dayNumber % LEVIATHAN_PERIOD_DAYS != 0) return;
        // ③ 当天已刷新过则跳过，避免一日内重复刷新
        if (data.leviathanLastSpawnDay == dayNumber) return;

        // 选最近存活玩家作为刷新锚点；无人则本轮跳过（等下次 tick）
        ServerPlayer target = null;
        double best = Double.MAX_VALUE;
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()
                    || !net.exmo.sixty_seconds.bridge.GameUtils.isPlayerAliveAndSurvival(player)) {
                continue;
            }
            double d = player.distanceToSqr(player);
            if (d < best) {
                best = d;
                target = player;
            }
        }
        if (target == null) return;
        BlockPos spot = findWaterSpot(level, target.blockPosition(),
                SPAWN_MIN_DIST, SPAWN_MAX_DIST, level.getRandom());
        if (spot == null) return;
        OceanSeaMonsterEntity monster = ModOceanEntities.OCEAN_SEA_MONSTER.create(level);
        if (monster == null) return;
        monster.moveTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5,
                level.getRandom().nextFloat() * 360.0F, 0.0F);
        monster.applyVariant(OceanSeaMonsterEntity.Variant.LEVIATHAN);
        monster.finalizeSpawn(level, level.getCurrentDifficultyAt(spot), MobSpawnType.COMMAND, null);
        level.addFreshEntity(monster);
        data.leviathanLastSpawnDay = dayNumber;
        announceSeaMonster(level, monster, target);
    }

    // ══════════════════════════════════════════════════════════════
    //  鲨鱼生成
    // ══════════════════════════════════════════════════════════════

    @Nullable
    public static OceanSharkEntity spawnShark(ServerLevel level, BlockPos waterPos,
            RandomSource random, double dayRatio) {
        OceanSharkEntity shark = ModOceanEntities.OCEAN_SHARK.create(level);
        if (shark == null) {
            return null;
        }
        shark.moveTo(waterPos.getX() + 0.5, waterPos.getY(), waterPos.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);

        // 变体概率随天数提升：前期只有小鲨，后期有大白鲨
        OceanSharkEntity.Variant variant;
        float r = random.nextFloat();
        if (r < 0.04 * dayRatio) {
            variant = OceanSharkEntity.Variant.MEGALODON; // 极低概率，随天数上升
        } else if (r < 0.07 + 0.12 * dayRatio) {
            variant = OceanSharkEntity.Variant.GREAT_WHITE;
        } else if (r < 0.18 + 0.20 * dayRatio) {
            variant = OceanSharkEntity.Variant.HAMMERHEAD;
        } else if (r < 0.40 + 0.20 * dayRatio) {
            variant = OceanSharkEntity.Variant.TIGER_SHARK;
        } else {
            variant = OceanSharkEntity.Variant.REEF_SHARK;
        }
        shark.applyVariant(variant);
        // 手动/指令来源用 COMMAND：直接完成 finalizeSpawn，保证指令必定生效
        shark.finalizeSpawn(level, level.getCurrentDifficultyAt(waterPos),
                MobSpawnType.COMMAND, null);
        level.addFreshEntity(shark);
        return shark;
    }

    // ══════════════════════════════════════════════════════════════
    //  海怪生成（KRAKEN / SERPENT；利维坦改由 tickLeviathan 定时刷）
    // ══════════════════════════════════════════════════════════════

    @Nullable
    public static OceanSeaMonsterEntity spawnSeaMonster(ServerLevel level, BlockPos waterPos,
            RandomSource random, double dayRatio, int bossLevel,
            @Nullable OceanSeaMonsterEntity.Variant forced) {
        OceanSeaMonsterEntity monster = ModOceanEntities.OCEAN_SEA_MONSTER.create(level);
        if (monster == null) return null;
        monster.moveTo(waterPos.getX() + 0.5, waterPos.getY(), waterPos.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);

        // forced != null 时按指定变体生成（指令召唤）；否则按 dayRatio 随机（自然刷新）
        OceanSeaMonsterEntity.Variant variant;
        if (forced != null) {
            variant = forced;
        } else if (random.nextFloat() < 0.20 + 0.15 * dayRatio) {
            variant = OceanSeaMonsterEntity.Variant.SERPENT;
        } else {
            variant = OceanSeaMonsterEntity.Variant.KRAKEN;
        }
        monster.applyVariant(variant, bossLevel);
        monster.finalizeSpawn(level, level.getCurrentDifficultyAt(waterPos),
                MobSpawnType.NATURAL, null);
        level.addFreshEntity(monster);
        return monster;
    }

    // ══════════════════════════════════════════════════════════════
    //  海怪出场特效：浓雾 + 音乐 + 全图播报 + 文字提醒
    // ══════════════════════════════════════════════════════════════

    private static void announceSeaMonster(ServerLevel level, OceanSeaMonsterEntity monster,
            ServerPlayer triggeringPlayer) {
        String variantKey = monster.getVariant().nameKey();

        // ① 全服金色播报
        Component globalMsg = Component.translatable(
                "message.sixty_seconds.ocean.monster_spawned",
                Component.translatable(variantKey))
                .withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD);
        level.getServer().getPlayerList().broadcastSystemMessage(globalMsg, false);

        // ② 触发玩家个人文字警告（醒目红色）
        triggeringPlayer.displayClientMessage(
                Component.translatable("message.sixty_seconds.ocean.monster_sighted")
                        .withStyle(ChatFormatting.RED, ChatFormatting.BOLD), true);
        triggeringPlayer.displayClientMessage(
                Component.translatable("message.sixty_seconds.ocean.monster_warning")
                        .withStyle(ChatFormatting.DARK_RED), false);

        // ③ 浓雾：给附近所有玩家套 VISION_FOG（level=0 = 2格视野，8秒）
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && p.distanceToSqr(monster) < MONSTER_FOG_RADIUS * MONSTER_FOG_RADIUS) {
                // VISION_FOG amplifier 0 → 雾距 2 格（最强），时长 8 秒
                p.addEffect(new MobEffectInstance(ModEffects.VISION_FOG, 20 * 8, 0,
                        false, true, true));
                p.displayClientMessage(
                        Component.translatable("message.sixty_seconds.ocean.fog_warning")
                                .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC), true);
            }
        }

        // ④ 吓人音效：对触发玩家播深海守卫警告
        triggeringPlayer.playNotifySound(SoundEvents.ELDER_GUARDIAN_AMBIENT,
                SoundSource.HOSTILE, 1.0F, 0.6F);
        // 第二声音效延迟（深沉的号角）
        level.getServer().tell(new net.minecraft.server.TickTask(20, () -> {
            if (monster.isAlive()) {
                for (ServerPlayer p : level.players()) {
                    if (!p.isSpectator() && p.distanceToSqr(monster) < 48.0 * 48.0) {
                        p.playNotifySound(SoundEvents.WARDEN_NEARBY_CLOSE,
                                SoundSource.HOSTILE, 0.4F, 0.3F);
                    }
                }
            }
        }));

        // ⑤ 海浪粒子爆发（在怪物位置）
        level.sendParticles(net.minecraft.core.particles.ParticleTypes.BUBBLE_COLUMN_UP,
                monster.getX(), monster.getY() + 2, monster.getZ(),
                40, 3.0, 1.5, 3.0, 0.05);
    }

    // ══════════════════════════════════════════════════════════════
    //  水域检测工具
    // ══════════════════════════════════════════════════════════════

    // ── 海底 Boss 相关工具 ──────────────────────────────────────────────

    /** 玩家是否贴近海底（其 XZ 列上距最近实心方块 ≤ 18 格）。 */
    private static boolean isNearSeafloor(ServerLevel level, BlockPos center) {
        int min = level.getMinBuildHeight();
        int floor = min;
        for (int y = center.getY(); y > min; y--) {
            if (level.getBlockState(new BlockPos(center.getX(), y, center.getZ())).isSolid()) { floor = y; break; }
        }
        return Math.abs(center.getY() - floor) <= 18;
    }

    /** 海底 Boss 同时存活上限：随天数升高（1→3）。 */
    private static int seafloorBossCap(int dayNumber) {
        return Math.min(3, 1 + dayNumber / 5);
    }

    /** 当前存活的深海 Boss 数量（ABYSS_KRAKEN / TRENCH_SERPENT / SUNKEN_LEVIATHAN），
     *  用于全局唯一限制；利维坦（LEVIATHAN）不计入。 */
    private static int countDeepSeaBosses(ServerLevel level) {
        int n = 0;
        for (var e : level.getEntities().getAll()) {
            if (e instanceof OceanSeaMonsterEntity m) {
                OceanSeaMonsterEntity.Variant v = m.getVariant();
                if (v == OceanSeaMonsterEntity.Variant.ABYSS_KRAKEN
                        || v == OceanSeaMonsterEntity.Variant.TRENCH_SERPENT
                        || v == OceanSeaMonsterEntity.Variant.SUNKEN_LEVIATHAN) {
                    n++;
                }
            }
        }
        return n;
    }

    /** 当前维度存活的海洋 Boss（含海底 Boss）总数。 */
    private static int oceanBossCount(ServerLevel level) {
        int n = 0;
        for (Entity e : level.getEntities().getAll()) {
            if (e instanceof OceanSeaMonsterEntity) n++;
        }
        return n;
    }

    /** 在玩家脚下的海底附近生成一只随机海底 Boss。 */
    @Nullable
    private static OceanSeaMonsterEntity spawnSeafloorBoss(ServerLevel level, BlockPos anchor, RandomSource random) {
        int min = level.getMinBuildHeight();
        int floor = min;
        for (int y = anchor.getY(); y > min; y--) {
            if (level.getBlockState(new BlockPos(anchor.getX(), y, anchor.getZ())).isSolid()) { floor = y; break; }
        }
        BlockPos spawn = new BlockPos(anchor.getX(), floor + 3, anchor.getZ());
        OceanSeaMonsterEntity monster = ModOceanEntities.OCEAN_SEA_MONSTER.create(level);
        if (monster == null) return null;
        OceanSeaMonsterEntity.Variant[] seafloor = {
                OceanSeaMonsterEntity.Variant.ABYSS_KRAKEN,
                OceanSeaMonsterEntity.Variant.TRENCH_SERPENT,
                OceanSeaMonsterEntity.Variant.SUNKEN_LEVIATHAN };
        monster.applyVariant(seafloor[random.nextInt(seafloor.length)]);
        monster.moveTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);
        monster.finalizeSpawn(level, level.getCurrentDifficultyAt(spawn), MobSpawnType.EVENT, null);
        level.addFreshEntity(monster);
        return monster;
    }

    /** 在玩家脚下的海底附近生成一只随机海底小怪。 */
    @Nullable
    public static OceanFloorMonsterEntity spawnFloorMonster(ServerLevel level, BlockPos waterPos, RandomSource random) {
        OceanFloorMonsterEntity monster = ModEntities.OCEAN_FLOOR_MONSTER.create(level);
        if (monster == null) return null;
        OceanFloorMonsterEntity.Variant[] variants = OceanFloorMonsterEntity.Variant.values();
        monster.applyVariant(variants[random.nextInt(variants.length)]);
        monster.moveTo(waterPos.getX() + 0.5, waterPos.getY(), waterPos.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);
        monster.finalizeSpawn(level, level.getCurrentDifficultyAt(waterPos), MobSpawnType.NATURAL, null);
        level.addFreshEntity(monster);
        return monster;
    }

    /**
     * 生成海洋霸主（10 个独立建模 Boss 之一）。<b>生成方式与既有 Boss 一致</b>：
     * 全服播报坐标 + 音效 + {@code BossEvent} 血条登记。
     */
    @Nullable
    public static OceanTitanEntity spawnTitan(ServerLevel level, BlockPos pos,
                                              OceanTitanEntity.Variant variant, int bossLevel) {
        OceanTitanEntity titan = ModEntities.OCEAN_TITAN.create(level);
        if (titan == null) return null;
        titan.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        titan.applyVariant(variant, bossLevel);
        level.addFreshEntity(titan);

        // 血条由各实体自管理（见 OceanTitanEntity.bossEvent），此处只做世界播报
        net.minecraft.network.chat.Component message = net.minecraft.network.chat.Component
                .translatable("message.sixty_seconds.ocean.titan_awaken",
                        net.minecraft.network.chat.Component.translatable(variant.nameKey()),
                        bossLevel, pos.getX(), pos.getY(), pos.getZ());
        for (ServerPlayer player : level.players()) {
            player.displayClientMessage(message, false);
            player.playNotifySound(net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                    net.minecraft.sounds.SoundSource.HOSTILE, 0.7F, 0.75F);
        }
        return titan;
    }

    /** 海洋霸主血条现由各实体自行管理（见 OceanTitanEntity.bossEvent）。 */

    /**
     * 在给定水体位置生成一只<b>随机</b>海洋生物群系生物（第二批次 10 变体之一）。
     * 具体变体由指令侧通过 {@code applyVariant} 覆盖；自然刷新则在此随机挑选。
     */
    @Nullable
    public static OceanFaunaEntity spawnOceanFauna(ServerLevel level, BlockPos waterPos, RandomSource random) {
        OceanFaunaEntity mob = ModEntities.OCEAN_FAUNA.create(level);
        if (mob == null) return null;
        OceanFaunaEntity.Variant[] variants = OceanFaunaEntity.Variant.values();
        mob.applyVariant(variants[random.nextInt(variants.length)]);
        mob.moveTo(waterPos.getX() + 0.5, waterPos.getY(), waterPos.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);
        mob.finalizeSpawn(level, level.getCurrentDifficultyAt(waterPos), MobSpawnType.NATURAL, null);
        level.addFreshEntity(mob);
        return mob;
    }

    /** 在玩家 XZ 附近找一块贴近海底的水方块位置。 */
    @Nullable
    private static BlockPos findSeafloorSpot(ServerLevel level, BlockPos anchor,
            int minDist, int maxDist, RandomSource random) {
        double angle = random.nextDouble() * Math.PI * 2;
        double dist = minDist + random.nextDouble() * (maxDist - minDist);
        int x = anchor.getX() + (int) (Math.cos(angle) * dist);
        int z = anchor.getZ() + (int) (Math.sin(angle) * dist);
        int min = level.getMinBuildHeight();
        int floor = min;
        for (int y = anchor.getY(); y > min; y--) {
            if (level.getBlockState(new BlockPos(x, y, z)).isSolid()) { floor = y; break; }
        }
        BlockPos pos = new BlockPos(x, floor + 2, z);
        if (!level.getFluidState(pos).is(FluidTags.WATER)) return null;
        return pos;
    }

    private static boolean isNearOpenWater(ServerLevel level, BlockPos center) {
        for (int dx = -4; dx <= 4; dx += 2) {
            for (int dz = -4; dz <= 4; dz += 2) {
                BlockPos check = center.offset(dx * 8, 0, dz * 8);
                if (!level.hasChunkAt(check)) continue;
                BlockPos surface = findWaterSurface(level, check);
                if (surface != null) return true;
            }
        }
        return false;
    }

    @Nullable
    public static BlockPos findWaterSpot(ServerLevel level, BlockPos near,
            int minDist, int maxDist, RandomSource random) {
        for (int attempt = 0; attempt < 12; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2;
            double dist = minDist + random.nextDouble() * (maxDist - minDist);
            int x = near.getX() + (int) (Math.cos(angle) * dist);
            int z = near.getZ() + (int) (Math.sin(angle) * dist);
            if (!level.hasChunkAt(new BlockPos(x, near.getY(), z))) continue;
            BlockPos surface = findWaterSurface(level, new BlockPos(x, near.getY(), z));
            if (surface != null) return surface;
        }
        return null;
    }

    @Nullable
    private static BlockPos findWaterSurface(ServerLevel level, BlockPos column) {
        int startY = column.getY() + 6;
        int endY = column.getY() - 8;
        for (int y = startY; y >= endY; y--) {
            BlockPos pos = new BlockPos(column.getX(), y, column.getZ());
            if (!level.getFluidState(pos).is(FluidTags.WATER)) continue;
            boolean surface = level.getBlockState(pos.above()).isAir()
                    && level.getBlockState(pos.above(2)).isAir();
            boolean deep = level.getFluidState(pos.below()).is(FluidTags.WATER)
                    && level.getFluidState(pos.below(2)).is(FluidTags.WATER)
                    && level.getFluidState(pos.below(3)).is(FluidTags.WATER);
            if (surface && deep) return pos;
            break;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private static <T extends net.minecraft.world.entity.Entity> int countNearby(
            ServerLevel level, ServerPlayer player, Class<T> clazz, double radius) {
        int count = 0;
        for (T e : level.getEntitiesOfClass(clazz, player.getBoundingBox().inflate(radius))) {
            count++;
        }
        return count;
    }

    /** 当前维度存活鲨鱼数（由 onSharkJoined / onSharkLeft 维护，O(1)）。 */
    private static int getSharkCount(ServerLevel level) {
        return SHARK_COUNT.getOrDefault(level, 0);
    }

    /** 鲨鱼加入维度（刷出 / 从存档加载 / 指令召唤）时 +1。由 NeoForgeEvents 在实体加入时调用。 */
    public static void onSharkJoined(ServerLevel level) {
        SHARK_COUNT.merge(level, 1, Integer::sum);
    }

    /** 鲨鱼离开维度（死亡 / 失活 / 卸载）时 -1。由 NeoForgeEvents 在实体离开时调用。 */
    public static void onSharkLeft(ServerLevel level) {
        int v = SHARK_COUNT.getOrDefault(level, 0) - 1;
        SHARK_COUNT.put(level, Math.max(0, v));
    }
}
