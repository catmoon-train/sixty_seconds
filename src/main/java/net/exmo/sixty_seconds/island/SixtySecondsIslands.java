package net.exmo.sixty_seconds.island;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.exmo.sixty_seconds.SixtySecondsBalance;
import net.exmo.sixty_seconds.entity.SixtySecondsBossEntity;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.entity.SixtySecondsMonsterEntity;
import net.exmo.sixty_seconds.arena.SixtySecondsSearchZones;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.logic.SixtySecondsPveSystem;
import net.exmo.sixty_seconds.network.SixtySecondsSeaChartArrivalS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsSeaChartPositionsS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsSeaChartReturnCancelS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsSeaChartReturnStartS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsSeaChartS2CPacket;
import net.exmo.sixty_seconds.network.SixtySecondsSeaChartSailStartS2CPacket;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.exmo.sixty_seconds.bridge.stubs.SubtitleCommand;
import net.exmo.sixty_seconds.bridge.fabric.UseItemCallback;
import net.exmo.sixty_seconds.bridge.fabric.ServerLivingEntityEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;
import net.exmo.sixty_seconds.SixtySeconds;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 60s 海岛远征总控（指令开关，独立于对局；对局中提供玩法钩子）：
 * <ul>
 *   <li><b>生成/还原</b>：{@code /60s island start|stop} 异步建造/回滚整片群岛
 *       （{@link SixtySecondsIslandGenerator}）；元数据落盘 {@code sixty_seconds_islands.json}，重启不丢。</li>
 *   <li><b>登岛提示</b>：踏上新岛 → SubTitle 报幕岛名+危险等级，4 级以上红色特别警报 + 音效；
 *       首次登岛为全队解锁该岛并刷一小队守岛怪。</li>
 *   <li><b>海图</b>：{@link SixtySecondsSeaChartS2CPacket} 把岛屿元数据（含解锁迷雾）同步给客户端
 *       SeaChartScreen；点击已解锁岛屿=扬帆（{@link #sail}）。</li>
 *   <li><b>情报解锁</b>：探索踏足 / 收音机侦听（{@link #tryRadioIntel}，每队每日一次）/
 *       每日事件（{@code SixtySecondsDailyEvents} 的 island_* 事件）三条途径点亮海图。</li>
 *   <li><b>等级联动</b>：{@code SixtySecondsAreaLevels.levelAt} 优先返回岛屿等级——物资箱稀有度
 *       与 PVE 游荡怪强度自动随岛等级缩放。</li>
 * </ul>
 */
public final class SixtySecondsIslands {

    public static final String LANG = SixtySecondsIsland.LANG;
    public static final String FILE_NAME = "sixty_seconds_islands.json";
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    /** 扬帆冷却（tick）。 */
    private static final int SAIL_COOLDOWN_TICKS = 20 * 10;
    /** 扬帆划船动画时长（tick）。 */
    public static final int SAIL_DURATION_TICKS = SixtySecondsBalance.ISLAND_SAIL_DURATION_TICKS;
    /** 登岛后禁止返航的锁定时长（tick）。 */
    private static final int LANDING_RETURN_LOCK_TICKS = SixtySecondsBalance.ISLAND_LANDING_RETURN_LOCK_TICKS;
    /** 收音机侦听到岛屿情报的概率（每队每日一次机会）。 */
    private static final double RADIO_INTEL_CHANCE = 0.45;
    /** 战斗状态持续 tick（受伤/攻击后 X 秒内视为战斗中）。 */
    private static final long COMBAT_TIMEOUT_TICKS = 20 * 5;
    /** 返回住所倒计时 tick（10 秒划船动画）。 */
    public static final int RETURN_DURATION_TICKS = 20 * 10;
    /** 返回住所时需站在登岛点周围多少格以内。 */
    private static final double RETURN_NEARBY_RANGE = 16.0;
    /** 玩家间最小间隔（生成不同登岛落点时）。 */
    private static final int PLAYER_SPAWN_MIN_SEPARATION = 4;

    private static final Map<ServerLevel, Data> STATES = new WeakHashMap<>();

    private SixtySecondsIslands() {
    }

    /** 落盘部分（Gson）。 */
    public static class SaveData {
        public boolean enabled = false;
        public List<SixtySecondsIsland> islands = new ArrayList<>();
    }

    /** 每世界运行态。 */
    public static final class Data {
        public final SaveData save = new SaveData();
        public boolean building = false;
        /** teamId → 已解锁岛 id（海图可见）。 */
        public final Map<Integer, Set<Integer>> teamUnlocked = new HashMap<>();
        /** teamId → 已亲自踏足的岛 id。 */
        public final Map<Integer, Set<Integer>> teamVisited = new HashMap<>();
        /** 玩家 → 当前所在岛 id（登岛沿检测）。 */
        public final Map<UUID, Integer> lastIsland = new HashMap<>();
        /** teamId → 已用收音机侦听情报的游戏日。 */
        public final Map<Integer, Integer> radioIntelDay = new HashMap<>();
        /** 本局已刷过守岛怪的岛 id。 */
        public final Set<Integer> guardSpawned = new HashSet<>();
        /** 玩家 → 扬帆冷却截止 tick。 */
        public final Map<UUID, Long> sailCooldown = new HashMap<>();
        /** 玩家 → 本次登岛的安全落点（每人不同）。 */
        public final Map<UUID, BlockPos> playerArrivalPositions = new HashMap<>();
        /** 玩家 → 最后一次受伤/攻击的 tick（用于脱战检测）。 */
        public final Map<UUID, Long> playerLastCombatTick = new HashMap<>();
        /** 玩家 → 返回住所预约截止 tick（倒计时中；0 或已过期=空闲）。 */
        public final Map<UUID, Long> playerReturningUntil = new HashMap<>();
        /** 玩家 → 扬帆去程预约（划船动画中；到期传送上岛）。 */
        public final Map<UUID, SailOrder> playerSailing = new HashMap<>();
        /** 玩家 → 登岛返航锁截止 tick：刚登岛的 30 秒内不许返航（{@link #LANDING_RETURN_LOCK_TICKS}）。 */
        public final Map<UUID, Long> playerReturnLockUntil = new HashMap<>();
        /** 正开着海图的玩家（{@code SeaChartWatchC2SPacket} 登记）：只对这些人每秒推队友点位。 */
        public final Set<UUID> chartWatchers = new HashSet<>();
        /** 生成快照（还原用，仅本进程内存）。 */
        public final LinkedHashMap<BlockPos, SixtySecondsIslandGenerator.Snapshot> snapshots =
                new LinkedHashMap<>();
        private boolean loaded = false;
    }

    /** 一次在途的扬帆去程：目的岛、到期 tick、出发坐标（结算航程消耗用）。 */
    public record SailOrder(int islandId, long untilTick, BlockPos origin) {
    }

    public static Data get(ServerLevel level) {
        Data data = STATES.computeIfAbsent(level, ignored -> new Data());
        if (!data.loaded) {
            data.loaded = true;
            load(level, data);
        }
        return data;
    }

    public static boolean enabled(ServerLevel level) {
        return get(level).save.enabled;
    }

    /**
     * 海图是否允许扬帆传送与返航（按图配置 {@code seaChartTeleportEnabled}，默认<b>关</b>）。
     * 关闭时海图只是导航图——岛屿/庇护所/队友照常显示，但要自己乘船去、走门回家。
     */
    public static boolean teleportAllowed(ServerLevel level) {
        return net.exmo.sixty_seconds.config.SixtySecondsConfigStore.current(level)
                .map(config -> config.seaChartTeleportEnabled)
                .orElse(false);
    }

    /**
     * 海洋（海岛）维度专用：维度首次 tick 时若群岛尚未生成，则按海洋配置异步生成。
     * 使登岛检测、扬帆、返航等海岛远征玩法在该维度内开箱即用，与主世界对局互不干扰。
     */
    public static void ensureOceanStarted(ServerLevel level) {
        Data data = get(level);
        if (data.building || data.save.enabled) {
            return;
        }
        // 海洋维度：岛屿地形由 SixtySecondsOceanFeature 在区块生成阶段按 planRegion 确定性写入，
        // 这里只补齐玩法元数据（海图/登岛/返航）。扫描出生点周边若干区域一次性登记，
        // 与地形共用同一套 planRegion（同 seed/同坐标），保证海图与实地完全一致。
        net.exmo.sixty_seconds.config.SixtySecondsConfig config = net.exmo.sixty_seconds.config.SixtySecondsConfigStore
                .current(level).orElseGet(net.exmo.sixty_seconds.config.SixtySecondsConfig::new);
        List<SixtySecondsIsland> all = new ArrayList<>();
        int scan = 3;
        for (int rx = -scan; rx <= scan; rx++) {
            for (int rz = -scan; rz <= scan; rz++) {
                all.addAll(SixtySecondsOceanWorldGen.planRegion(rx, rz, config, level.getSeed()));
            }
        }
        data.save.islands = all;
        data.save.enabled = true;
        save(level, data);
        syncChartAll(level);
    }

    /** {@code /60s sea_teleport off} 时把在途的扬帆/返航一并作废，并重发海图刷新客户端按钮态。 */
    public static void onTeleportToggled(ServerLevel level, boolean enabled) {
        Data data = STATES.get(level);
        if (data != null && !enabled) {
            for (ServerPlayer player : level.players()) {
                if (data.playerSailing.remove(player.getUUID()) != null
                        || data.playerReturningUntil.remove(player.getUUID()) != null) {
                    SixtySecondsSeaChartReturnCancelS2CPacket.send(player);
                    player.displayClientMessage(Component.translatable(LANG + "teleport_disabled")
                            .withStyle(ChatFormatting.YELLOW), true);
                }
            }
        }
        syncChartAll(level);
    }

    /** 坐标所在岛的危险等级；不在任何岛单元格内返回 0（{@code SixtySecondsAreaLevels} 反查用）。 */
    public static int levelAt(ServerLevel level, BlockPos pos) {
        Data data = STATES.get(level);
        if (data == null || !data.save.enabled) {
            return 0;
        }
        for (SixtySecondsIsland island : data.save.islands) {
            if (island.inCell(pos)) {
                return island.level;
            }
        }
        return 0;
    }

    /** 全群岛外包盒（扬帆后的活动限制盒；含所有单元格）。 */
    public static AABB regionBox(Data data) {
        AABB union = null;
        for (SixtySecondsIsland island : data.save.islands) {
            AABB cell = island.cellBox();
            union = union == null ? cell : union.minmax(cell);
        }
        return union;
    }

    // ── 注册（init 一次）：收音机侦听钩子 + 战斗追踪 ──────────────────────────

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            if (world instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer
                    && player.getItemInHand(hand).getItem()
                            instanceof net.exmo.sixty_seconds.content.item.RadioItem) {
                tryRadioIntel(serverLevel, serverPlayer);
            }
            return InteractionResultHolder.pass(ItemStack.EMPTY);
        });

        // 战斗状态追踪：受伤/攻击时标记（用于脱战检测）
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamageTaken, damageTaken,
                blocked) -> {
            if (!blocked && entity instanceof ServerPlayer player) {
                markCombat(player);
            }
            // 攻击者也标记（如果伤害来源是玩家）
            if (source.getEntity() instanceof ServerPlayer attacker) {
                markCombat(attacker);
            }
        });
    }



    private static net.exmo.sixty_seconds.config.SixtySecondsConfig.Vec vec(int x, int y, int z) {
        return new net.exmo.sixty_seconds.config.SixtySecondsConfig.Vec(x, y, z);
    }

    private static void clearRuntime(Data data) {
        data.teamUnlocked.clear();
        data.teamVisited.clear();
        data.lastIsland.clear();
        data.radioIntelDay.clear();
        data.guardSpawned.clear();
        data.sailCooldown.clear();
        data.playerArrivalPositions.clear();
        data.playerLastCombatTick.clear();
        data.playerReturningUntil.clear();
        data.playerSailing.clear();
        data.playerReturnLockUntil.clear();
        data.chartWatchers.clear();
    }

    /** 开局钩子（{@code SixtySecondsManager.begin}）：清跨局解锁态，为每队默认解锁全部 1 级港湾岛。 */
    public static void onGameStart(ServerLevel level) {
        Data data = get(level);
        data.teamUnlocked.clear();
        data.teamVisited.clear();
        data.lastIsland.clear();
        data.radioIntelDay.clear();
        data.guardSpawned.clear();
        data.sailCooldown.clear();
        data.playerSailing.clear();
        data.playerReturnLockUntil.clear();
        data.playerReturningUntil.clear();
        data.chartWatchers.clear();
        if (!data.save.enabled) {
            return;
        }
        SixtySecondsState.Data state = SixtySecondsState.get(level);
        for (Integer teamId : state.teams.keySet()) {
            Set<Integer> unlocked = data.teamUnlocked.computeIfAbsent(teamId, ignored -> new HashSet<>());
            for (SixtySecondsIsland island : data.save.islands) {
                if (island.level <= 1) {
                    unlocked.add(island.id);
                }
            }
        }
    }

    // ── tick：登岛沿检测 + 返回住所倒计时完成（Manager DAY 相位每 tick 调，内部 10 tick 一次）──────────

    public static void tick(ServerLevel level) {
        Data data = STATES.get(level);
        if (data == null || !data.save.enabled || data.save.islands.isEmpty()
                || level.getGameTime() % 10 != 0) {
            return;
        }
        long now = level.getGameTime();

        tickChartWatchers(level, data, now);

        // 扬帆去程倒计时：到期落地上岛（中断条件与返航一致——受伤即取消，防止用划船动画躲伤害）
        for (ServerPlayer player : level.players()) {
            SailOrder order = data.playerSailing.get(player.getUUID());
            if (order == null) {
                continue;
            }
            if (now >= order.untilTick()) {
                completeSail(player, order);
            } else if (isInCombat(player)) {
                data.playerSailing.remove(player.getUUID());
                SixtySecondsSeaChartReturnCancelS2CPacket.send(player);
                player.displayClientMessage(Component.translatable(LANG + "sail_cancelled_combat")
                        .withStyle(ChatFormatting.RED), true);
            }
        }

        // 返回住所倒计时检查：到期执行传送，或被中断
        for (ServerPlayer player : level.players()) {
            Long returningUntil = data.playerReturningUntil.get(player.getUUID());
            if (returningUntil == null) {
                continue;
            }
            if (now >= returningUntil) {
                completeReturn(player);
            } else {
                // 中断检查：受伤则取消
                if (isInCombat(player)) {
                    cancelReturn(player,
                            Component.translatable(LANG + "return_cancelled_combat").withStyle(ChatFormatting.RED));
                    continue;
                }
                // 中断检查：离开登岛点太远
                BlockPos arrival = data.playerArrivalPositions.get(player.getUUID());
                if (arrival != null) {
                    double dx = player.getX() - (arrival.getX() + 0.5);
                    double dz = player.getZ() - (arrival.getZ() + 0.5);
                    if (dx * dx + dz * dz > RETURN_NEARBY_RANGE * RETURN_NEARBY_RANGE) {
                        cancelReturn(player,
                                Component.translatable(LANG + "return_cancelled_moved").withStyle(ChatFormatting.RED));
                    }
                }
            }
        }

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator()) {
                continue;
            }
            BlockPos pos = player.blockPosition();
            SixtySecondsIsland current = null;
            for (SixtySecondsIsland island : data.save.islands) {
                if (island.isOnIsland(pos)) {
                    current = island;
                    break;
                }
            }
            Integer lastId = data.lastIsland.get(player.getUUID());
            if (current == null) {
                data.lastIsland.remove(player.getUUID());
                continue;
            }
            if (lastId != null && lastId == current.id) {
                continue;
            }
            data.lastIsland.put(player.getUUID(), current.id);
            onLanded(level, data, player, current);
        }

        tickHardcoreGarrison(level, data);
    }

    /**
     * 炼狱岛驻守 Boss 的「远离消失 / 靠近去重重生」管理（每 10 tick 一次）。
     * <p>去重依据：维度内按 {@link SixtySecondsBossEntity#getHomeIslandId()} 索引存活驻守 Boss。
     * 玩家全部远离岛心 {@code >HARDCORE_BOSS_DESPAWN_DIST} 时让驻守 Boss 消失；
     * 玩家靠近 {@code <HARDCORE_BOSS_SPAWN_DIST} 且岛上无存活驻守 Boss 时重建一只。</p>
     */
    private static void tickHardcoreGarrison(ServerLevel level, Data data) {
        // 无炼狱岛时直接短路，避免无谓的实体扫描
        boolean anyHardcore = false;
        for (SixtySecondsIsland isl : data.save.islands) {
            if (isl.hardcore && isl.bossVariant != null) {
                anyHardcore = true;
                break;
            }
        }
        if (!anyHardcore) {
            return;
        }
        // 1) 收集维度内所有存活的驻守 Boss（按岛 id 索引，天然去重）
        //    按 Boss 类索引查询（Level.getEntities 内部按实体类存储，比遍历全维度所有实体高效）
        Map<Integer, SixtySecondsBossEntity> garrison = new java.util.HashMap<>();
        net.minecraft.world.level.entity.EntityTypeTest<net.minecraft.world.entity.Entity, SixtySecondsBossEntity> test =
                net.minecraft.world.level.entity.EntityTypeTest.forClass(SixtySecondsBossEntity.class);
        net.minecraft.world.phys.AABB big = net.minecraft.world.phys.AABB.ofSize(
                net.minecraft.world.phys.Vec3.ZERO, 3.0E7, 3.0E7, 3.0E7);
        java.util.List<SixtySecondsBossEntity> bossList = new java.util.ArrayList<>();
        level.getEntities(test, big, boss -> true, bossList);
        for (SixtySecondsBossEntity boss : bossList) {
            if (!boss.isRemoved() && boss.isGarrisonBoss()) {
                garrison.putIfAbsent(boss.getHomeIslandId(), boss);
            }
        }
        double despawnSqr = SixtySecondsBalance.HARDCORE_BOSS_DESPAWN_DIST
                * SixtySecondsBalance.HARDCORE_BOSS_DESPAWN_DIST;
        double spawnSqr = SixtySecondsBalance.HARDCORE_BOSS_SPAWN_DIST
                * SixtySecondsBalance.HARDCORE_BOSS_SPAWN_DIST;
        for (SixtySecondsIsland island : data.save.islands) {
            if (!island.hardcore || island.bossVariant == null) {
                continue;
            }
            // 2) 该岛最近玩家与岛心的水平距离平方
            double nearestSqr = Double.MAX_VALUE;
            for (ServerPlayer player : level.players()) {
                if (player.isSpectator()) {
                    continue;
                }
                double dx = player.getX() - island.centerX;
                double dz = player.getZ() - island.centerZ;
                double d = dx * dx + dz * dz;
                if (d < nearestSqr) {
                    nearestSqr = d;
                }
            }
            SixtySecondsBossEntity existing = garrison.get(island.id);
            if (existing != null && !existing.isRemoved()) {
                // 有驻守 Boss：全部玩家远离则消失
                if (nearestSqr > despawnSqr) {
                    existing.discard();
                }
            } else if (nearestSqr <= spawnSqr) {
                // 无存活驻守 Boss 且玩家靠近：去重后重建一只
                int bossLevel = Mth.clamp(island.level - 1 + SixtySecondsBalance.HARDCORE_BOSS_LEVEL_BONUS,
                        1, SixtySecondsBalance.AREA_BOSS_MAX_LEVEL);
                BlockPos bossSpot = new BlockPos(island.centerX, island.seaY + 1, island.centerZ);
                SixtySecondsBossEntity boss = SixtySecondsPveSystem.spawnBoss(
                        level, bossSpot, bossLevel, false, island.bossVariant, false);
                if (boss != null) {
                    boss.setHomeIslandId(island.id);
                }
            }
        }
    }

    // ── 海图点位订阅（庇护所 + 队友；只在有人开着海图时推）──────────────────────

    /** 海图开屏/关屏：登记或注销观看者；订阅时立刻回推一份，免得开屏第一秒空着。 */
    public static void setChartWatching(ServerPlayer player, boolean watching) {
        Data data = get(player.serverLevel());
        if (watching) {
            data.chartWatchers.add(player.getUUID());
            SixtySecondsSeaChartPositionsS2CPacket.send(player);
        } else {
            data.chartWatchers.remove(player.getUUID());
        }
    }

    /** 每秒给开着海图的玩家推一份庇护所/队友点位；下线的观看者顺手清掉。 */
    private static void tickChartWatchers(ServerLevel level, Data data, long now) {
        if (data.chartWatchers.isEmpty() || now % 20 != 0) {
            return;
        }
        data.chartWatchers.removeIf(uuid -> {
            ServerPlayer watcher = level.getServer().getPlayerList().getPlayer(uuid);
            if (watcher == null) {
                return true;
            }
            SixtySecondsSeaChartPositionsS2CPacket.send(watcher);
            return false;
        });
    }

    /** 登岛：SubTitle 报幕 + 高危警报 + 全队解锁 + 首登刷守岛怪 + 区域地图切到本岛。 */
    private static void onLanded(ServerLevel level, Data data, ServerPlayer player, SixtySecondsIsland island) {
        // 撤离点岛屿：不触发常规探岛流程（不刷守岛怪、不返航锁）；最后一天登岛即撤离
        if (island.isEvacuation || island.type == SixtySecondsIsland.Type.EVACUATION) {
            var state = SixtySecondsState.get(level);
            var stats = SixtySecondsStatsComponent.KEY.get(player);
            boolean lastDay = stats.totalDays > 0 && stats.dayNumber >= stats.totalDays;
            SubtitleCommand.sendToPlayerTop(player,
                    Component.literal(island.name().getString()).withStyle(ChatFormatting.GOLD, ChatFormatting.BOLD),
                    Component.translatable(LANG + (lastDay ? "evac_now" : "evac_wait"))
                            .withStyle(ChatFormatting.YELLOW),
                    90, false);
            if (lastDay && !state.helicopterEvacuated.contains(player.getUUID())) {
                state.helicopterEvacuated.add(player.getUUID());
                player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
                player.displayClientMessage(
                        Component.translatable(LANG + "evac_success").withStyle(ChatFormatting.GREEN), false);
                level.getServer().getPlayerList().broadcastSystemMessage(
                        Component.translatable(LANG + "evac_broadcast", player.getDisplayName())
                                .withStyle(ChatFormatting.GOLD),
                        false);
            }
            return;
        }

        int lv = island.level;
        ChatFormatting nameColor = lv >= 4 ? ChatFormatting.RED : lv >= 3 ? ChatFormatting.GOLD
                : ChatFormatting.AQUA;
        Component main = island.name().copy().withStyle(nameColor, ChatFormatting.BOLD);
        // 危险等级按「星级对应的颜色」逐档上色（与星图 StarRegion.STAR_COLORS 完全一致：绿→青→金→橙→红）
        final int[] STAR_COLORS = {
                0xFF55CC55, 0xFF4AB8C0, 0xFFFFD700, 0xFFE07B39, 0xFFD94040
        };
        int subColor = STAR_COLORS[Math.max(0, Math.min(lv, STAR_COLORS.length) - 1)];
        Component sub = Component.translatable(LANG + "level", lv)
                .withColor(subColor);
        SubtitleCommand.sendToPlayerTop(player, main, sub, 90, false);
        // 登岛返航锁：上岛后 30 秒内不许返航——否则「扬帆→抓一箱→立刻撤」能把海岛刷成零风险自助餐。
        // 用 gameTime 时间戳记一次，到点自然失效（不 tick 递减、不同步；见 ai_doc.md）。
        data.playerReturnLockUntil.put(player.getUUID(), level.getGameTime() + LANDING_RETURN_LOCK_TICKS);
        if (lv >= 4) {
            // 高危岛特别提醒：追加红色警报字幕 + 阴森音效
            SubtitleCommand.sendToPlayerTop(player,
                    Component.translatable(LANG + "enter_danger").withStyle(ChatFormatting.DARK_RED,
                            ChatFormatting.BOLD),
                    Component.translatable(LANG + "enter_danger_sub").withStyle(ChatFormatting.RED),
                    70, false);
            player.playNotifySound(SoundEvents.ELDER_GUARDIAN_CURSE, SoundSource.HOSTILE, 0.8F, 0.85F);
        } else {
            player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7F, 0.8F);
        }
        // 区域地图切到本岛单元格
        net.exmo.sixty_seconds.network.SixtySecondsMapZoneS2CPacket.ensureZone(
                player, island.cellBox(), island.dockPos(), false);
        // 全队解锁 + 踏足登记
        int teamId = SixtySecondsStatsComponent.KEY.get(player).teamId;
        if (teamId >= 0) {
            data.teamVisited.computeIfAbsent(teamId, ignored -> new HashSet<>()).add(island.id);
            unlockForTeam(level, teamId, island,
                    Component.translatable(LANG + "unlocked_by_visit", island.name(), island.level));
        }
        // 首登守岛怪：一小队，等级越高越强（每岛每局一次；后续增援靠 PVE 游荡怪）
        // 海洋维度没有对局进行中，但同样需要在首次登岛时刷守岛怪
        if ((SixtySecondsMod.isActive(level) || level.dimension() == SixtySeconds.OCEAN_DIMENSION)
                && data.guardSpawned.add(island.id)
                && SixtySecondsPveSystem.pveEnabled(level)) {
            RandomSource rng = level.random;
            // 炼狱岛：守岛怪更多、用更强 variant、血量额外加成；并固定驻守一只 Boss
            int pack = 1 + island.level + rng.nextInt(2);
            double hpMult = 1.0 + 0.15 * (island.level - 1);
            if (island.hardcore) {
                pack += SixtySecondsBalance.HARDCORE_GUARD_EXTRA;
                hpMult *= SixtySecondsBalance.HARDCORE_GUARD_HEALTH_MULT;
            }
            SixtySecondsIslandGenerator.LevelPlacer placer =
                    new SixtySecondsIslandGenerator.LevelPlacer(level, new java.util.LinkedHashMap<>());
            for (int i = 0; i < pack; i++) {
                BlockPos spot = SixtySecondsIslandGenerator.randomGround(placer, island, rng, 0.1, 0.7);
                if (spot != null) {
                    SixtySecondsMonsterEntity.Variant v;
                    if (island.hardcore) {
                        v = SixtySecondsBalance.HARDCORE_GUARD_VARIANTS[
                                rng.nextInt(SixtySecondsBalance.HARDCORE_GUARD_VARIANTS.length)];
                    } else {
                        v = SixtySecondsIslandGenerator.rollVariant(rng, island.level);
                    }
                    SixtySecondsPveSystem.createMonster(level, spot, v, hpMult, 1.0);
                }
            }
            // 炼狱岛固定驻守一只 Boss（规划阶段决定的变体；首登一次性）
            if (island.hardcore && island.bossVariant != null) {
                int bossLevel = Mth.clamp(island.level - 1 + SixtySecondsBalance.HARDCORE_BOSS_LEVEL_BONUS,
                        1, SixtySecondsBalance.AREA_BOSS_MAX_LEVEL);
                BlockPos bossSpot = SixtySecondsIslandGenerator.randomGround(
                        placer, island, rng, 0.1, 0.6);
                if (bossSpot == null) {
                    bossSpot = new BlockPos(island.centerX, island.seaY + 1, island.centerZ);
                }
                SixtySecondsBossEntity boss = SixtySecondsPveSystem.spawnBoss(
                        level, bossSpot, bossLevel, false, island.bossVariant, false);
                if (boss != null) {
                    boss.setHomeIslandId(island.id);
                }
                // 炼狱岛登场额外红色警报，强调驻守 Boss
                SubtitleCommand.sendToPlayerTop(player,
                        Component.translatable(LANG + "enter_hardcore").withStyle(ChatFormatting.DARK_RED,
                                ChatFormatting.BOLD),
                        Component.translatable(LANG + "enter_hardcore_sub").withStyle(ChatFormatting.RED),
                        80, false);
                player.playNotifySound(SoundEvents.WITHER_SPAWN, SoundSource.HOSTILE, 1.0F, 0.9F);
            }
        }
    }

    // ── 解锁（消息系统的统一出口）───────────────────────────────────────────

    /** 为一支队伍解锁一座岛：首次解锁时向全队播报 {@code message} 并重发海图。 */
    public static boolean unlockForTeam(ServerLevel level, int teamId, SixtySecondsIsland island,
            Component message) {
        Data data = get(level);
        Set<Integer> unlocked = data.teamUnlocked.computeIfAbsent(teamId, ignored -> new HashSet<>());
        if (!unlocked.add(island.id)) {
            return false;
        }
        SixtySecondsState.TeamData team = SixtySecondsState.get(level).teams.get(teamId);
        if (team != null) {
            for (UUID uuid : team.members) {
                ServerPlayer member = level.getServer().getPlayerList().getPlayer(uuid);
                if (member != null) {
                    member.displayClientMessage(message.copy().withStyle(ChatFormatting.GOLD), false);
                    member.playNotifySound(SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.PLAYERS,
                            0.8F, 1.4F);
                    syncChart(member, false);
                }
            }
        }
        return true;
    }

    /**
     * 情报解锁一座随机未解锁岛（收音机/每日事件共用）。返回解锁的岛，全部已解锁返回 null。
     * 低等级岛更容易被打听到（权重 = 6 - level）。
     */
    public static SixtySecondsIsland unlockRandomLocked(ServerLevel level, int teamId, String reasonKey) {
        Data data = get(level);
        if (!data.save.enabled) {
            return null;
        }
        Set<Integer> unlocked = data.teamUnlocked.computeIfAbsent(teamId, ignored -> new HashSet<>());
        List<SixtySecondsIsland> locked = new ArrayList<>();
        int totalWeight = 0;
        for (SixtySecondsIsland island : data.save.islands) {
            if (!unlocked.contains(island.id)) {
                locked.add(island);
                totalWeight += 6 - island.level;
            }
        }
        if (locked.isEmpty()) {
            return null;
        }
        int roll = level.random.nextInt(Math.max(1, totalWeight));
        SixtySecondsIsland picked = locked.get(locked.size() - 1);
        for (SixtySecondsIsland island : locked) {
            roll -= 6 - island.level;
            if (roll < 0) {
                picked = island;
                break;
            }
        }
        unlockForTeam(level, teamId, picked,
                Component.translatable(reasonKey, picked.name(), picked.level));
        return picked;
    }

    /** 每日事件效果入口：为该队解锁一座岛；没有可解锁的返回 false（事件文案自行兜底）。 */
    public static boolean intelEvent(ServerLevel level, SixtySecondsState.TeamData team) {
        return get(level).save.enabled
                && unlockRandomLocked(level, team.teamId, LANG + "unlocked_by_intel") != null;
    }

    /** 收音机侦听：海岛开启的对局中，每队每日一次机会，成功则解锁一座岛。 */
    private static void tryRadioIntel(ServerLevel level, ServerPlayer player) {
        Data data = STATES.get(level);
        boolean ocean = level.dimension() == SixtySeconds.OCEAN_DIMENSION;
        // 海洋维度没有对局进行中，但收音机侦听仍可用（按天推进无效时视为第 1 天）
        if (data == null || !data.save.enabled
                || (!SixtySecondsMod.isActive(level) && !ocean)) {
            return;
        }
        int teamId = SixtySecondsStatsComponent.KEY.get(player).teamId;
        if (teamId < 0) {
            return;
        }
        int day = SixtySecondsState.get(level).dayNumber;
        if (day <= 0) {
            // 海洋维度按天推进无效，使用第 1 天情报
            day = 1;
        }
        Integer lastDay = data.radioIntelDay.get(teamId);
        if (lastDay != null && lastDay >= day) {
            return; // 今天已侦听过，静默（对讲机原功能不受影响）
        }
        data.radioIntelDay.put(teamId, day);
        if (level.random.nextDouble() < RADIO_INTEL_CHANCE) {
            SixtySecondsIsland unlocked = unlockRandomLocked(level, teamId, LANG + "unlocked_by_radio");
            if (unlocked != null) {
                return;
            }
        }
        player.displayClientMessage(
                Component.translatable(LANG + "radio_noise").withStyle(ChatFormatting.GRAY), false);
        player.playNotifySound(SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.4F, 0.6F);
    }

    // ── 扬帆 / 返航 ──────────────────────────────────────────────────────

    /**
     * 扬帆前往指定岛（海图点击/命令）：要求海岛开启、{@code sea_teleport} 开关打开、对局进行中、
     * 玩家已出门探索、目标岛已解锁、未处于战斗中。
     * <p>
     * 校验通过后<b>不立即传送</b>，而是启动 {@link #SAIL_DURATION_TICKS} 的划船去程动画
     * （与返航同一套演出），到期由 {@link #tick} 调 {@link #completeSail} 落地——
     * 出海是有过程的，点一下就瞬移到对岸没有代价感。创造模式跳过全部校验与动画，直接落地。
     * </p>
     */
    public static void sail(ServerPlayer player, int islandId) {
        ServerLevel level = player.serverLevel();
        Data data = get(level);
        boolean ocean = level.dimension() == SixtySeconds.OCEAN_DIMENSION;
        if (!data.save.enabled || data.building) {
            player.displayClientMessage(Component.translatable(LANG + "sail_unavailable")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        SixtySecondsIsland island = byId(data, islandId);
        if (island == null) {
            return;
        }
        boolean creative = player.isCreative();
        if (creative) {
            // 创造=管理员巡查：不走动画/消耗，直接落地
            completeSail(player, new SailOrder(islandId, 0L, player.blockPosition().immutable()));
            return;
        }
        // sea_teleport 关闭 = 只能自己开船去，海图不提供扬帆；海洋维度内扬帆始终可用
        if (!teleportAllowed(level) && !ocean) {
            player.displayClientMessage(Component.translatable(LANG + "sail_teleport_disabled")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        // 主世界要求「对局进行中且在搜索区」；海洋维度内身处即视为在外海探索
        if ((!SixtySecondsMod.isActive(level) && !ocean)
                || (!SixtySecondsSearchZones.isInSearchZone(player) && !ocean)) {
            player.displayClientMessage(Component.translatable(LANG + "sail_not_in_zone")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
        if (stats.downed) {
            return;
        }
        int teamId = stats.teamId;
        Set<Integer> unlocked = data.teamUnlocked.get(teamId);
        // 海洋维度内所有岛默认可达（无需先为某队解锁）；主世界仍需本队已解锁
        if (!ocean && (teamId < 0 || unlocked == null || !unlocked.contains(island.id))) {
            player.displayClientMessage(Component.translatable(LANG + "sail_locked")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        long now = level.getGameTime();
        // 已在划船（去程或回程）中：不允许再点一次
        if (data.playerSailing.containsKey(player.getUUID())) {
            player.displayClientMessage(Component.translatable(LANG + "sail_already")
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        Long returningUntil = data.playerReturningUntil.get(player.getUUID());
        if (returningUntil != null && now < returningUntil) {
            player.displayClientMessage(Component.translatable(LANG + "return_already")
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        // 战斗中不许扬帆——否则划船动画就成了脱战外挂
        if (isInCombat(player)) {
            player.displayClientMessage(Component.translatable(LANG + "sail_in_combat")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        Long cooldownEnd = data.sailCooldown.get(player.getUUID());
        if (cooldownEnd != null && now < cooldownEnd) {
            player.displayClientMessage(Component.translatable(LANG + "sail_cooldown",
                    (int) Math.ceil((cooldownEnd - now) / 20.0)).withStyle(ChatFormatting.YELLOW), true);
            return;
        }
        data.sailCooldown.put(player.getUUID(), now + SAIL_COOLDOWN_TICKS);

        // 记下出发点：到岸时按「出发点 → 登岛点」的水平距离结算航程消耗
        data.playerSailing.put(player.getUUID(),
                new SailOrder(islandId, now + SAIL_DURATION_TICKS, player.blockPosition().immutable()));
        SixtySecondsSeaChartSailStartS2CPacket.send(player, SAIL_DURATION_TICKS, islandId);
        level.playSound(null, player.blockPosition(), SoundEvents.BOAT_PADDLE_WATER, SoundSource.PLAYERS,
                0.8F, 1.0F);
        player.displayClientMessage(Component.translatable(LANG + "sail_started", island.name())
                .withStyle(ChatFormatting.AQUA), true);
    }

    /**
     * 扬帆到岸（由 {@link #tick} 倒计时触发，或创造模式直接调用）：
     * 生成<b>每人不同</b>的安全登岛落点、传送、把活动限制盒切成整片群岛（可乘船在岛间穿行）、
     * 同步登岛坐标给客户端海图，并结算本趟航程的污染与饱食/口渴消耗。
     */
    private static void completeSail(ServerPlayer player, SailOrder order) {
        ServerLevel level = player.serverLevel();
        Data data = get(level);
        data.playerSailing.remove(player.getUUID());
        SixtySecondsIsland island = byId(data, order.islandId());
        if (island == null) {
            return;
        }
        boolean creative = player.isCreative();

        BlockPos uniqueArrival = findUniqueArrivalSpot(level, data, island, player.getUUID());
        player.teleportTo(level, uniqueArrival.getX() + 0.5D, uniqueArrival.getY(),
                uniqueArrival.getZ() + 0.5D, player.getYRot(), player.getXRot());
        data.playerArrivalPositions.put(player.getUUID(), uniqueArrival.immutable());

        AABB region = regionBox(data);
        if (!creative && region != null) {
            SixtySecondsSearchZones.updateConfine(player, region, uniqueArrival);
        }
        SixtySecondsSeaChartArrivalS2CPacket.send(player, uniqueArrival);

        if (!creative) {
            applyTripCost(player, order.origin(), uniqueArrival);
        }

        level.playSound(null, uniqueArrival, SoundEvents.BOAT_PADDLE_WATER, SoundSource.PLAYERS, 0.8F, 1.0F);
        player.displayClientMessage(Component.translatable(LANG + "sail_success", island.name())
                .withStyle(ChatFormatting.AQUA), true);
    }

    /**
     * 结算一趟航程（去程/回程各算一次）的代价：固定累积一点污染，
     * 并按<b>水平航距</b>线性折算饱食度与口渴值消耗（每 100 格的量见 {@code SixtySecondsBalance.ISLAND_TRIP_*}，
     * 各自封顶，防跨群岛长途一趟饿死）。四值均 clamp 到 0。
     */
    private static void applyTripCost(ServerPlayer player, BlockPos from, BlockPos to) {
        double dx = to.getX() - from.getX();
        double dz = to.getZ() - from.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);
        double per100 = distance / 100.0;
        int hunger = Math.min(SixtySecondsBalance.ISLAND_TRIP_HUNGER_CAP,
                (int) Math.ceil(per100 * SixtySecondsBalance.ISLAND_TRIP_HUNGER_PER_100_BLOCKS));
        int thirst = Math.min(SixtySecondsBalance.ISLAND_TRIP_THIRST_CAP,
                (int) Math.ceil(per100 * SixtySecondsBalance.ISLAND_TRIP_THIRST_PER_100_BLOCKS));

        SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
        stats.hunger = Math.max(0, stats.hunger - hunger);
        stats.thirst = Math.max(0, stats.thirst - thirst);
        stats.pollution = Math.min(100, stats.pollution + SixtySecondsBalance.ISLAND_TRIP_POLLUTION);
        stats.sync();

        player.displayClientMessage(Component.translatable(LANG + "trip_cost",
                (int) distance, hunger, thirst, SixtySecondsBalance.ISLAND_TRIP_POLLUTION)
                .withStyle(ChatFormatting.GRAY), false);
    }

    /**
     * 为该玩家在此岛上找一个与其他玩家不同的安全登岛落点。
     * 在 dockPos 周围搜索，优先避开已被占用的坐标。
     */
    private static BlockPos findUniqueArrivalSpot(ServerLevel level, Data data, SixtySecondsIsland island,
            UUID playerUuid) {
        BlockPos dock = island.dockPos();
        RandomSource rng = RandomSource.create(level.random.nextLong() ^ playerUuid.hashCode());

        // 收集已被其他玩家占用的登岛落点
        Set<BlockPos> occupied = new HashSet<>();
        for (Map.Entry<UUID, BlockPos> entry : data.playerArrivalPositions.entrySet()) {
            if (!entry.getKey().equals(playerUuid)) {
                occupied.add(entry.getValue());
            }
        }

        // 在 dockPos 周围搜索（±12 格随机偏移），优先找没人占用的安全点
        BlockPos best = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            int dx = rng.nextIntBetweenInclusive(-12, 12);
            int dz = rng.nextIntBetweenInclusive(-12, 12);
            BlockPos candidate = dock.offset(dx, 0, dz);

            // 确保在岛上（有陆地）
            if (!island.isOnIsland(candidate)) {
                continue;
            }

            BlockPos safe = SixtySecondsSearchZones.findSafeSpot(level, candidate);
            if (safe == null) {
                continue;
            }

            // 检查与其他玩家落点的距离
            boolean tooClose = false;
            for (BlockPos occ : occupied) {
                if (safe.distSqr(occ) < PLAYER_SPAWN_MIN_SEPARATION * PLAYER_SPAWN_MIN_SEPARATION) {
                    tooClose = true;
                    break;
                }
            }

            if (!tooClose) {
                return safe;
            }
            if (best == null) {
                best = safe;
            }
        }

        // 兜底：找不到不冲突的点就用 dock 安全校正
        return best != null ? best : SixtySecondsSearchZones.findSafeSpot(level, dock);
    }

    public static SixtySecondsIsland byId(Data data, int islandId) {
        for (SixtySecondsIsland island : data.save.islands) {
            if (island.id == islandId) {
                return island;
            }
        }
        return null;
    }

    // ── 战斗状态追踪 ─────────────────────────────────────────────────────

    /**
     * 标记玩家进入战斗（用于脱战检测）。
     * 在受伤/攻击事件中调用；战斗状态保持 {@link #COMBAT_TIMEOUT_TICKS} tick。
     */
    public static void markCombat(ServerPlayer player) {
        Data data = STATES.get(player.serverLevel());
        if (data == null) {
            return;
        }
        data.playerLastCombatTick.put(player.getUUID(), player.serverLevel().getGameTime());
    }

    /**
     * 检查玩家是否处于战斗中。
     */
    public static boolean isInCombat(ServerPlayer player) {
        Data data = STATES.get(player.serverLevel());
        if (data == null) {
            return false;
        }
        Long last = data.playerLastCombatTick.get(player.getUUID());
        if (last == null) {
            return false;
        }
        return (player.serverLevel().getGameTime() - last) < COMBAT_TIMEOUT_TICKS;
    }

    // ── 返回住所（新：脱战校验 + 位置校验 + 10s 划船动画）───────────

    /**
     * 客户端海图「返回住所」请求——由 {@code SixtySecondsSeaChartReturnC2SPacket} 触发。
     * <p>
     * 校验：1) 未处于战斗中；2) 玩家在登岛点附近（{@link #RETURN_NEARBY_RANGE} 格内）；
     * 3) 不处于冷却/倒计时中；4) 已在搜索区（出门探索状态）。
     * </p>
     * <p>
     * 校验通过后向客户端发送 {@link SixtySecondsSeaChartReturnStartS2CPacket}
     * 启动 10 秒划船动画；同时记录 {@code playerReturningUntil} 截止 tick。
     * 动画期间 tick 倒计时，到期后执行真正的传送回家。
     * </p>
     */
    public static void requestReturn(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Data data = get(level);
        boolean ocean = level.dimension() == SixtySeconds.OCEAN_DIMENSION;
        if (!data.save.enabled || data.building) {
            player.displayClientMessage(Component.translatable(LANG + "return_unavailable")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        // 海洋维度返航 = 回主世界出生点，无需搜索区/登岛点等限制
        if (ocean) {
            long now = level.getGameTime();
            data.playerReturningUntil.put(player.getUUID(), now + RETURN_DURATION_TICKS);
            SixtySecondsSeaChartReturnStartS2CPacket.send(player, RETURN_DURATION_TICKS);
            level.playSound(null, player.blockPosition(), SoundEvents.BOAT_PADDLE_WATER,
                    SoundSource.PLAYERS, 0.8F, 1.0F);
            player.displayClientMessage(Component.translatable(LANG + "return_started")
                    .withStyle(ChatFormatting.AQUA), true);
            return;
        }

        // sea_teleport 关闭 = 海图不提供返航，得自己开船回去走门
        if (!player.isCreative() && !teleportAllowed(level)) {
            player.displayClientMessage(Component.translatable(LANG + "return_teleport_disabled")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        // 必须在搜索区（出门探索状态）
        if (!SixtySecondsSearchZones.isInSearchZone(player)) {
            player.displayClientMessage(Component.translatable(LANG + "return_need_explore")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        // 登岛返航锁：刚上岛的 30 秒内不许撤
        long lockUntil = data.playerReturnLockUntil.getOrDefault(player.getUUID(), 0L);
        if (level.getGameTime() < lockUntil) {
            player.displayClientMessage(Component.translatable(LANG + "return_landing_lock",
                    (int) Math.ceil((lockUntil - level.getGameTime()) / 20.0))
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        // 战斗状态检查
        if (isInCombat(player)) {
            player.displayClientMessage(Component.translatable(LANG + "return_in_combat")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        // 登岛点位置检查
        BlockPos arrival = data.playerArrivalPositions.get(player.getUUID());
        if (arrival == null) {
            player.displayClientMessage(Component.translatable(LANG + "return_no_arrival")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }
        double dx = player.getX() - (arrival.getX() + 0.5);
        double dz = player.getZ() - (arrival.getZ() + 0.5);
        if (dx * dx + dz * dz > RETURN_NEARBY_RANGE * RETURN_NEARBY_RANGE) {
            player.displayClientMessage(Component.translatable(LANG + "return_need_nearby")
                    .withStyle(ChatFormatting.RED), true);
            return;
        }

        // 是否已在返回倒计时中
        long now = level.getGameTime();
        Long returningUntil = data.playerReturningUntil.get(player.getUUID());
        if (returningUntil != null && now < returningUntil) {
            player.displayClientMessage(Component.translatable(LANG + "return_already")
                    .withStyle(ChatFormatting.YELLOW), true);
            return;
        }

        // 检查归来冷却（复用搜索区冷却）
        SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
        if (now < stats.exploreCooldownEndTick) {
            int seconds = (int) Math.ceil((stats.exploreCooldownEndTick - now) / 20.0D);
            player.displayClientMessage(
                    Component.translatable("message.sixty_seconds.sixty_seconds.return_cooldown", seconds), true);
            return;
        }

        // 启动返回倒计时
        data.playerReturningUntil.put(player.getUUID(), now + RETURN_DURATION_TICKS);

        // 发 S2C 包启动客户端划船动画
        SixtySecondsSeaChartReturnStartS2CPacket.send(player, RETURN_DURATION_TICKS);

        // 播放划船音效
        level.playSound(null, player.blockPosition(), SoundEvents.BOAT_PADDLE_WATER,
                SoundSource.PLAYERS, 0.8F, 1.0F);

        player.displayClientMessage(Component.translatable(LANG + "return_started")
                .withStyle(ChatFormatting.AQUA), true);
    }

    /**
     * 完成返回：真正把玩家传送回家（由 tick 倒计时触发）。
     */
    private static void completeReturn(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Data data = get(level);

        // 清除本岛状态
        BlockPos origin = data.playerArrivalPositions.remove(player.getUUID());
        data.playerReturningUntil.remove(player.getUUID());
        data.playerReturnLockUntil.remove(player.getUUID());

        // 绕过归来冷却（已在 requestReturn 中校验过）
        SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
        stats.exploreCooldownEndTick = 0;
        stats.sync();

        // 海洋维度返航 = 回到主世界出生点
        if (level.dimension() == SixtySeconds.OCEAN_DIMENSION) {
            ServerLevel overworld = level.getServer().getLevel(Level.OVERWORLD);
            BlockPos home = overworld != null ? overworld.getSharedSpawnPos() : player.blockPosition();
            if (origin == null) {
                origin = player.blockPosition();
            }
            if (overworld != null) {
                BlockPos safe = SixtySecondsSearchZones.findSafeSpot(overworld, home);
                player.teleportTo(overworld, safe.getX() + 0.5, safe.getY(), safe.getZ() + 0.5,
                        player.getYRot(), player.getXRot());
            }
            if (!player.isCreative()) {
                applyTripCost(player, origin, home);
            }
            player.displayClientMessage(Component.translatable(LANG + "return_complete")
                    .withStyle(ChatFormatting.GREEN), true);
            return;
        }

        // 回程航距按「登岛点 → 本队庇护所」算（returnPlayer 的落点就是庇护所出生点）；
        // 拿不到队伍信息时退回玩家当前位置，至少不会算成 0 距离白嫖一趟
        SixtySecondsState.TeamData team = SixtySecondsState.get(level).teams.get(stats.teamId);
        BlockPos home = team != null && team.shelterSpawn != null ? team.shelterSpawn : player.blockPosition();
        if (origin == null) {
            origin = player.blockPosition();
        }

        // 走搜索区回家流程
        SixtySecondsSearchZones.returnPlayer(player);
        if (!player.isCreative()) {
            applyTripCost(player, origin, home);
        }

        player.displayClientMessage(Component.translatable(LANG + "return_complete")
                .withStyle(ChatFormatting.GREEN), true);
    }

    /**
     * 取消返回（受伤/离开登岛点/海图关停等原因）。
     */
    private static void cancelReturn(ServerPlayer player, Component reason) {
        ServerLevel level = player.serverLevel();
        Data data = get(level);
        data.playerReturningUntil.remove(player.getUUID());
        player.displayClientMessage(reason, true);
        // 通知客户端取消划船动画
        SixtySecondsSeaChartReturnCancelS2CPacket.send(player);
        // 播放取消音效
        level.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(),
                SoundSource.PLAYERS, 0.5F, 0.6F);
    }

    // ── 海图同步 ─────────────────────────────────────────────────────────

    /** 给一名玩家同步海图（openScreen=true 时客户端直接打开界面）。 */
    public static void syncChart(ServerPlayer player, boolean openScreen) {
        SixtySecondsSeaChartS2CPacket.send(player, openScreen);
    }

    public static void syncChartAll(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            syncChart(player, false);
        }
    }

    // ── 落盘 ────────────────────────────────────────────────────────────

    private static Path path(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }

    private static void load(ServerLevel level, Data data) {
        Path file = path(level);
        if (!Files.exists(file)) {
            return;
        }
        try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            SaveData loaded = GSON.fromJson(reader, SaveData.class);
            if (loaded != null) {
                data.save.enabled = loaded.enabled;
                data.save.islands = loaded.islands != null ? loaded.islands : new ArrayList<>();
            }
        } catch (IOException | RuntimeException e) {
            SixtySeconds.LOGGER.warn("[60s] Failed to read {}: {}", FILE_NAME, e.toString());
        }
    }

    public static void save(ServerLevel level, Data data) {
        Path file = path(level);
        try {
            Files.createDirectories(file.getParent());
            try (Writer writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                GSON.toJson(data.save, writer);
            }
        } catch (IOException e) {
            SixtySeconds.LOGGER.warn("[60s] Failed to write {}: {}", FILE_NAME, e.toString());
        }
    }
}
