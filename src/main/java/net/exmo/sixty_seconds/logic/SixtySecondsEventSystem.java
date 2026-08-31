package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.bridge.GameUtils;
import net.exmo.sixty_seconds.SixtySecondsBalance;
import net.exmo.sixty_seconds.arena.SixtySecondsSearchZones;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.exmo.sixty_seconds.registry.ModItems;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * 天气/环境事件系统。每 {@link SixtySecondsBalance#EVENT_CHECK_INTERVAL} 尝试从事件池随机触发：
 * <ul>
 *   <li><b>污雨</b>：野外（搜索区）无雨伞每秒额外污染；下雨天气。</li>
 *   <li><b>浓烟</b>：不在家（住宅/避难所外）每 2 秒额外污染 + 失明脉冲；雨伞无效。</li>
 *   <li><b>寒潮</b>：不在家者饥饿加速消耗 + 缓慢。</li>
 *   <li><b>空投物资</b>（瞬发）：每队避难所出生点旁落一箱随机物资（按 loot 表多类别掷出）。</li>
 * </ul>
 */
public final class SixtySecondsEventSystem {
    public enum EventType {
        POLLUTION_RAIN, SMOG, COLD_SNAP, AIRDROP,
        /** 酸雾：户外中毒 + 持续扣血 */
        ACID_FOG,
        /** 电磁风暴：全队虚弱 + 怪物强化 + 理智下降 */
        ELECTROMAGNETIC_STORM,
        /** 虫潮：户外缓慢 + 持续受击掉血 */
        SWARM,
        /** 热浪：全队虚弱 + 口渴消耗加速 */
        HEAT_WAVE,
        // ── 第二批事件 ──
        /** 沙尘暴：户外失明 */
        SANDSTORM,
        /** 地震：瞬发型，全图玩家摇晃+反胃，持续15秒（不破坏地形） */
        EARTHQUAKE,
        /** 流星雨：户外随机被火球砸中扣血（不放置方块/火） */
        METEOR_SHOWER,
        /** 孢子迷雾：户外反胃+理智下降，室内免疫 */
        SPORE_FOG,
        /** 冰雹：户外持续微弱伤害 */
        HAIL,
        /** 血月：仅在夜间事件池中出现，怪物攻击力强化 + 理智下降 */
        BLOOD_MOON,
        /** 辐射泄漏：全队污染缓慢上升（屋内也受影响但减半） */
        RADIATION_LEAK,
        /** 浓雾：能见度极低 */
        DENSE_FOG,
        /** 雷暴：户外随机落雷 + 雷暴天气（雨天+雷） */
        THUNDERSTORM,
        /** 潮涌：靠近水体者被水流冲击并溺水扣血（海洋主题） */
        TIDAL_SURGE,
        /** 虚空裂隙：仅户外玩家，每 15 秒有 5% 几率被随机传送 */
        VOID_RIFT,
        /** 火山灰：户外污染上升 + 偶尔窒息 */
        ASH_FALL,
        /** 火雨：户外玩家周期着火 + 火焰伤害 */
        FIRE_RAIN,
        /** 水晶风暴：户外被冰晶切割 + 缓慢 */
        CRYSTAL_STORM,
        /** 剧毒孢子：户外中毒 + 理智下降 */
        TOXIC_SPORE,
        /** 太阳耀斑：户外暴晒着火 + 口渴加速 */
        SOLAR_FLARE,
        /** 灵魂风：户外虚弱 + 反胃 + 理智下降 */
        SOUL_WIND,
        /** 余烬风暴：户外火焰伤害 + 烟尘致盲 */
        EMBER_STORM,
        // ── 第三批事件：雨类（可被雨伞抵御，每 12 秒施加一次负面效果）──
        /** 酸雨：户外无伞者每 12 秒腐蚀扣血 + 污染 */
        ACID_RAIN,
        /** 毒雨：户外无伞者每 12 秒中毒 I（5 秒） */
        POISON_RAIN,
        /** 霜雨：户外无伞者每 12 秒缓慢 + 寒冷伤害 */
        FROST_RAIN,
        /** 粘液雨：户外无伞者每 12 秒缓慢 + 反胃 */
        SLIME_RAIN,
        /** 电雨：户外无伞者每 12 秒虚弱 */
        SPARK_RAIN
    }

    private static final Map<ServerLevel, Active> ACTIVE = new WeakHashMap<>();
    /** 天气预报调度：dayNumber → EventType（null 表示"晴朗"） */
    private static final Map<ServerLevel, Map<Integer, EventType>> SCHEDULED = new WeakHashMap<>();

    private record Active(EventType type, long endTick) {
    }

    private SixtySecondsEventSystem() {
    }

    /** 收音机：当前进行中事件的播报键（无事件返回 null）。 */
    public static String activeEventKey(ServerLevel level) {
        Active active = ACTIVE.get(level);
        if (active == null) {
            return null;
        }
        return switch (active.type) {
            case POLLUTION_RAIN -> "message.sixty_seconds.sixty_seconds.event_pollution_rain_start";
            case SMOG -> "message.sixty_seconds.sixty_seconds.event_smog_start";
            case COLD_SNAP -> "message.sixty_seconds.sixty_seconds.event_cold_start";
            case ACID_FOG -> "message.sixty_seconds.sixty_seconds.event_acid_fog_start";
            case ELECTROMAGNETIC_STORM -> "message.sixty_seconds.sixty_seconds.event_em_storm_start";
            case SWARM -> "message.sixty_seconds.sixty_seconds.event_swarm_start";
            case HEAT_WAVE -> "message.sixty_seconds.sixty_seconds.event_heat_wave_start";
            case SANDSTORM -> "message.sixty_seconds.sixty_seconds.event_sandstorm_start";
            case EARTHQUAKE -> "message.sixty_seconds.sixty_seconds.event_earthquake_start";
            case METEOR_SHOWER -> "message.sixty_seconds.sixty_seconds.event_meteor_shower_start";
            case SPORE_FOG -> "message.sixty_seconds.sixty_seconds.event_spore_fog_start";
            case HAIL -> "message.sixty_seconds.sixty_seconds.event_hail_start";
            case BLOOD_MOON -> "message.sixty_seconds.sixty_seconds.event_blood_moon_start";
            case RADIATION_LEAK -> "message.sixty_seconds.sixty_seconds.event_radiation_leak_start";
            case DENSE_FOG -> "message.sixty_seconds.sixty_seconds.event_dense_fog_start";
            case THUNDERSTORM -> "message.sixty_seconds.sixty_seconds.event_thunderstorm_start";
            case TIDAL_SURGE -> "message.sixty_seconds.sixty_seconds.event_tidal_surge_start";
            case VOID_RIFT -> "message.sixty_seconds.sixty_seconds.event_void_rift_start";
            case ASH_FALL -> "message.sixty_seconds.sixty_seconds.event_ash_fall_start";
            case FIRE_RAIN -> "message.sixty_seconds.sixty_seconds.event_fire_rain_start";
            case CRYSTAL_STORM -> "message.sixty_seconds.sixty_seconds.event_crystal_storm_start";
            case TOXIC_SPORE -> "message.sixty_seconds.sixty_seconds.event_toxic_spore_start";
            case SOLAR_FLARE -> "message.sixty_seconds.sixty_seconds.event_solar_flare_start";
            case SOUL_WIND -> "message.sixty_seconds.sixty_seconds.event_soul_wind_start";
            case EMBER_STORM -> "message.sixty_seconds.sixty_seconds.event_ember_storm_start";
            case ACID_RAIN -> "message.sixty_seconds.sixty_seconds.event_acid_rain_start";
            case POISON_RAIN -> "message.sixty_seconds.sixty_seconds.event_poison_rain_start";
            case FROST_RAIN -> "message.sixty_seconds.sixty_seconds.event_frost_rain_start";
            case SLIME_RAIN -> "message.sixty_seconds.sixty_seconds.event_slime_rain_start";
            case SPARK_RAIN -> "message.sixty_seconds.sixty_seconds.event_spark_rain_start";
            default -> null;
        };
    }

    public static void tick(ServerLevel level) {
        long now = level.getGameTime();
        Active active = ACTIVE.get(level);
        if (active != null) {
            if (now >= active.endTick) {
                endEvent(level, active);
                ACTIVE.remove(level);
            } else {
                applyEvent(level, active, now);
            }
            return;
        }
        // 优先检查当天是否有预报安排的天气
        SixtySecondsState.Data stateData = SixtySecondsState.get(level);
        Map<Integer, EventType> schedule = SCHEDULED.get(level);
        if (schedule != null && schedule.containsKey(stateData.dayNumber)) {
            // 当天已由 startDay / 热线预报明确安排（含「晴朗」null）：
            // 触发对应事件；若为晴朗则不再随机补足，保证末日日报预报与实际一致。
            EventType scheduled = schedule.remove(stateData.dayNumber);
            if (scheduled != null) {
                startEvent(level, scheduled, now);
            }
            return;
        }
        if (now % SixtySecondsBalance.EVENT_CHECK_INTERVAL == 0
                && level.getRandom().nextDouble() < SixtySecondsBalance.EVENT_CHANCE) {
            // 根据昼夜选择不同的事件池
            boolean isNight = level.isNight();
            if (isNight) {
                if (level.getRandom().nextDouble() < 0.2) {
                    startEvent(level, EventType.BLOOD_MOON, now);
                    return;
                }
            }
            // 通用事件池（排除血月和空投——空投有独立触发概率）
            EventType[] allTypes = EventType.values();
            List<EventType> filtered = new java.util.ArrayList<>();
            for (EventType t : allTypes) {
                if (t == EventType.BLOOD_MOON || t == EventType.AIRDROP) continue;
                filtered.add(t);
            }
            EventType[] pool = filtered.toArray(new EventType[0]);
            startEvent(level, pool[level.getRandom().nextInt(pool.length)], now);
        }
    }

    /** 是否为下雨型自然事件（触发世界下雨）。与 startEvent 中 setWeatherParameters(..., true, ...) 保持一致。 */
    public static boolean isRainEventType(EventType type) {
        return type == EventType.POLLUTION_RAIN || type == EventType.HAIL || type == EventType.THUNDERSTORM
                || type == EventType.ACID_RAIN || type == EventType.POISON_RAIN
                || type == EventType.FROST_RAIN || type == EventType.SLIME_RAIN
                || type == EventType.SPARK_RAIN;
    }

    private static void startEvent(ServerLevel level, EventType type, long now) {
        switch (type) {
            case POLLUTION_RAIN -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.POLLUTION_RAIN_DURATION));
                level.setWeatherParameters(0, SixtySecondsBalance.POLLUTION_RAIN_DURATION, true, false);
            }
            case SMOG -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.SMOG_DURATION));
            }
            case COLD_SNAP -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.COLD_SNAP_DURATION));
            }
            case ACID_FOG -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
            }
            case ELECTROMAGNETIC_STORM -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
            }
            case SWARM -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
            }
            case HEAT_WAVE -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
            }
            case SANDSTORM -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.SANDSTORM_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_sandstorm_start")
                        .withStyle(ChatFormatting.GOLD));
            }
            case EARTHQUAKE -> {
                // 瞬发型事件：15秒摇晃，不进长时间ACTIVE但阻止刷其他事件
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EARTHQUAKE_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_earthquake_start")
                        .withStyle(ChatFormatting.RED));
                // 对全体玩家施加反胃效果（模拟地震摇晃感，不破坏任何方块）
                for (ServerPlayer p : level.players()) {
                    if (!GameUtils.isPlayerEliminated(p)) {
                        p.addEffect(new MobEffectInstance(MobEffects.CONFUSION,
                                SixtySecondsBalance.EARTHQUAKE_DURATION, 1, false, false, false));
                    }
                }
            }
            case METEOR_SHOWER -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.METEOR_SHOWER_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_meteor_shower_start")
                        .withStyle(ChatFormatting.RED));
            }
            case SPORE_FOG -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.SPORE_FOG_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_spore_fog_start")
                        .withStyle(ChatFormatting.DARK_GREEN));
            }
            case HAIL -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.HAIL_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_hail_start")
                        .withStyle(ChatFormatting.AQUA));
                // 冰雹时设为雨天
                level.setWeatherParameters(20 * 60 * 6, 0, true, false);
            }
            case BLOOD_MOON -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.BLOOD_MOON_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_blood_moon_start")
                        .withStyle(ChatFormatting.DARK_RED));
            }
            case RADIATION_LEAK -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.RADIATION_LEAK_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_radiation_leak_start")
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
            case DENSE_FOG -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.DENSE_FOG_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_dense_fog_start")
                        .withStyle(ChatFormatting.GRAY));
            }
            case THUNDERSTORM -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_thunderstorm_start")
                        .withStyle(ChatFormatting.GOLD));
                level.setWeatherParameters(0, SixtySecondsBalance.EVENT_BASE_DURATION, true, true);
            }
            case TIDAL_SURGE -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_tidal_surge_start")
                        .withStyle(ChatFormatting.AQUA));
            }
            case VOID_RIFT -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_void_rift_start")
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
            case ASH_FALL -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_ash_fall_start")
                        .withStyle(ChatFormatting.GRAY));
            }
            case FIRE_RAIN -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_fire_rain_start")
                        .withStyle(ChatFormatting.GOLD));
            }
            case CRYSTAL_STORM -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_crystal_storm_start")
                        .withStyle(ChatFormatting.AQUA));
            }
            case TOXIC_SPORE -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_toxic_spore_start")
                        .withStyle(ChatFormatting.GREEN));
            }
            case SOLAR_FLARE -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_solar_flare_start")
                        .withStyle(ChatFormatting.GOLD));
            }
            case SOUL_WIND -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_soul_wind_start")
                        .withStyle(ChatFormatting.WHITE));
            }
            case EMBER_STORM -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.EVENT_BASE_DURATION));
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_ember_storm_start")
                        .withStyle(ChatFormatting.GOLD));
            }
            // ── 第三批：雨类天气（下雨型，可被雨伞抵御）──
            case ACID_RAIN -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.ACID_RAIN_DURATION));
                level.setWeatherParameters(0, SixtySecondsBalance.ACID_RAIN_DURATION, true, false);
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_acid_rain_start")
                        .withStyle(ChatFormatting.GREEN));
            }
            case POISON_RAIN -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.POISON_RAIN_DURATION));
                level.setWeatherParameters(0, SixtySecondsBalance.POISON_RAIN_DURATION, true, false);
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_poison_rain_start")
                        .withStyle(ChatFormatting.DARK_GREEN));
            }
            case FROST_RAIN -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.FROST_RAIN_DURATION));
                level.setWeatherParameters(0, SixtySecondsBalance.FROST_RAIN_DURATION, true, false);
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_frost_rain_start")
                        .withStyle(ChatFormatting.AQUA));
            }
            case SLIME_RAIN -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.SLIME_RAIN_DURATION));
                level.setWeatherParameters(0, SixtySecondsBalance.SLIME_RAIN_DURATION, true, false);
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_slime_rain_start")
                        .withStyle(ChatFormatting.GREEN));
            }
            case SPARK_RAIN -> {
                ACTIVE.put(level, new Active(type, now + SixtySecondsBalance.SPARK_RAIN_DURATION));
                level.setWeatherParameters(0, SixtySecondsBalance.SPARK_RAIN_DURATION, true, false);
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_spark_rain_start")
                        .withStyle(ChatFormatting.YELLOW));
            }
            case AIRDROP -> airdrop(level); // 瞬发，不进 ACTIVE
        }
        if (type != EventType.AIRDROP) {
            net.exmo.sixty_seconds.weather.WeatherSync.send(level, type, false);
        }
    }

    private static void applyEvent(ServerLevel level, Active active, long now) {
        if (now % 20 != 0) {
            return;
        }
        SixtySecondsState.Data data = SixtySecondsState.get(level);
        for (ServerPlayer player : level.players()) {
            if (GameUtils.isPlayerEliminated(player)) {
                continue;
            }
            SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
            if (stats.downed || stats.monster) {
                continue;
            }
            boolean inHome = isInHome(player, data, stats.teamId);
            switch (active.type) {
                case POLLUTION_RAIN -> {
                    if (!SixtySecondsSearchZones.isInSearchZone(player) || hasUmbrella(player)
                            || hasGasMask(player)) {
                        continue;
                    }
                    // 无伞淋污雨：每 10 秒结算一次（增速较旧版 2/秒 已降 65%）
                    if (now % (20 * 10) == 0) {
                        addPollution(level, stats, SixtySecondsBalance.POLLUTION_RAIN_GAIN_PER_10S);
                    }
                }
                case SMOG -> {
                    if (inHome || hasGasMask(player)) {
                        continue; // 防毒面具：浓烟免疫（雨伞对浓烟无效）
                    }
                    // 浓烟视野限制：15 级失明，每秒刷新（离开浓烟/事件结束后约 1 秒自动消退）
                    if (now % 20 == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 25, 14, false, false, false));
                    }
                    // 每 2 秒结算一次（增速较旧版 1/秒 已降 50%）
                    if (now % (20 * 2) == 0) {
                        addPollution(level, stats, SixtySecondsBalance.SMOG_POLLUTION_PER_2S);
                    }
                }
                case COLD_SNAP -> {
                    if (inHome) {
                        continue;
                    }
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0,
                            false, false, false));
                    if (now % (20 * 10) == 0) {
                        stats.hunger = Math.max(0, stats.hunger - SixtySecondsBalance.COLD_HUNGER_PER_10S);
                        stats.sync();
                    }
                }
                case ACID_FOG -> {
                    // 酸雾：每10秒掉1点健康 + 1点污染；室内或防毒面具免疫
                    if (!inHome && !hasGasMask(player) && now % (20 * 10) == 0) {
                        player.hurt(player.damageSources().magic(), 1.0F);
                        addPollution(level, stats, 1);
                    }
                }
                case ELECTROMAGNETIC_STORM -> {
                    // 电磁风暴：全员虚弱 + 理智下降 + 怪物获得强化
                    if (!player.hasEffect(MobEffects.WEAKNESS)) {
                        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, false, false));
                    }
                    if (now % (20 * 15) == 0) {
                        stats.sanity = Math.max(0, stats.sanity - 3);
                        stats.sync();
                    }
                    // 电磁风暴：全队虚弱 + 理智下降；怪物攻击力强化由 SixtySecondsHealthSystem.applyInjury 依据 isMonsterStrengthBoosted 实现
                }
                case SWARM -> {
                    // 虫潮：户外缓慢 + 持续受击
                    player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 0,
                            false, false, false));
                    if (!inHome && now % (20 * 8) == 0) {
                        player.hurt(player.damageSources().generic(), 0.5F);
                    }
                    if (now % (20 * 10) == 0) {
                        stats.sanity = Math.max(0, stats.sanity - 2);
                        stats.sync();
                    }
                }
                case HEAT_WAVE -> {
                    // 热浪：全员虚弱 + 口渴消耗加速
                    if (!player.hasEffect(MobEffects.WEAKNESS)) {
                        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 80, 0, false, false, false));
                    }
                    // 每 5 秒额外消耗口渴
                    if (now % (20 * 10) == 0) {
                        stats.thirst = Math.max(0, stats.thirst - 1);
                        stats.sync();
                    }
                }
                case SANDSTORM -> {
                    // 沙尘暴：户外失明；室内或防毒面具（滤尘）免疫
                    if (inHome || hasGasMask(player)) continue;
                    player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0,
                            false, false, false));
                }
                case EARTHQUAKE -> {
                    // 地震：瞬发型，在 startEvent 中已施加反胃效果
                    // tick 中不做额外处理，仅持续至 endTick
                    continue;
                }
                case THUNDERSTORM -> {
                    // 雷暴：户外玩家周期性被落雷击中（落雷本身造成范围伤害）
                    if (inHome) continue;
                    // 手持雨伞会像避雷针一样大幅吸引落雷：无伞 10%，持伞 60%
                    double lightningChance = hasUmbrella(player) ? 0.6 : 0.1;
                    if (now % (20 * 10) == 0 && level.getRandom().nextDouble() < lightningChance
                            && level.canSeeSky(player.blockPosition())) {
                        net.minecraft.world.entity.LightningBolt bolt = new net.minecraft.world.entity.LightningBolt(
                                net.minecraft.world.entity.EntityType.LIGHTNING_BOLT, level);
                        bolt.setPos(player.getX(), player.getY(), player.getZ());
                        level.addFreshEntity(bolt);
                        if (hasUmbrella(player)) {
                            player.displayClientMessage(Component.translatable(
                                    "message.sixty_seconds.sixty_seconds.event_thunderstorm_umbrella_hit")
                                    .withStyle(ChatFormatting.RED), true);
                        }
                    }
                    if (now % (20 * 10) == 0) {
                        stats.sanity = Math.max(0, stats.sanity - 2);
                        stats.sync();
                    }
                }
                case TIDAL_SURGE -> {
                    // 潮涌：靠近水体者被水流冲击并溺水扣血（海洋主题）
                    if (now % (20 * 3) == 0 && isNearWater(level, player)) {
                        player.hurt(player.damageSources().drown(), 2.0F);
                        player.push(0.0, 0.5, 0.0);
                        player.displayClientMessage(
                                Component.translatable("message.sixty_seconds.sixty_seconds.event_tidal_surge_hit")
                                        .withStyle(ChatFormatting.AQUA), true);
                    }
                }
                case VOID_RIFT -> {
                    // 虚空裂隙：仅户外玩家，每 15 秒有 5% 几率被随机传送
                    if (inHome) continue;
                    if (now % (20 * 15) == 0 && level.getRandom().nextDouble() < 0.05
                            && level.canSeeSky(player.blockPosition())) {
                        double nx = player.getX() + (level.getRandom().nextDouble() - 0.5) * 16.0;
                        double nz = player.getZ() + (level.getRandom().nextDouble() - 0.5) * 16.0;
                        player.teleportTo(nx, player.getY(), nz);
                        player.displayClientMessage(
                                Component.translatable("message.sixty_seconds.sixty_seconds.event_void_rift_hit")
                                        .withStyle(ChatFormatting.DARK_PURPLE), true);
                    }
                }
                case ASH_FALL -> {
                    // 火山灰：户外无防毒面具者缓慢污染 + 每 10 秒窒息扣血
                    if (!inHome && !hasGasMask(player) && now % (20 * 3) == 0) {
                        addPollution(level, stats, 1);
                    }
                    // 窒息：仅户外且无防毒面具者，每 10 秒扣 1 点生命（不会直接致死）
                    if (!inHome && !hasGasMask(player) && now % (20 * 10) == 0) {
                        player.hurt(player.damageSources().generic(), 1.0F);
                        player.displayClientMessage(Component.translatable(
                                "message.sixty_seconds.sixty_seconds.event_ash_fall_hit").withStyle(ChatFormatting.GRAY), true);
                    }
                }
                case FIRE_RAIN -> {
                    // 火雨：户外玩家每 10 秒着火 + 火焰伤害
                    if (inHome) continue;
                    if (now % (20 * 10) == 0) {
                        player.setRemainingFireTicks(60);
                        player.hurt(player.damageSources().onFire(), 1.0F);
                        player.displayClientMessage(Component.translatable(
                                "message.sixty_seconds.sixty_seconds.event_fire_rain_hit").withStyle(ChatFormatting.GOLD), true);
                    }
                }
                case CRYSTAL_STORM -> {
                    // 水晶风暴：户外玩家每 8 秒被冰晶切割 + 缓慢
                    if (inHome) continue;
                    if (now % (20 * 8) == 0) {
                        player.hurt(player.damageSources().generic(), 1.5F);
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, false));
                    }
                }
                case TOXIC_SPORE -> {
                    // 剧毒孢子：户外玩家每 10 秒获得 5 秒中毒 I；防毒面具免疫
                    if (inHome || hasGasMask(player)) continue;
                    if (now % (20 * 10) == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1, false, false, false));
                    }
                }
                case SOLAR_FLARE -> {
                    // 太阳耀斑：户外玩家每 8 秒暴晒着火 + 口渴加速
                    if (inHome) continue;
                    if (now % (20 * 8) == 0) {
                        player.setRemainingFireTicks(40);
                        stats.thirst = Math.max(0, stats.thirst - 1);
                        stats.sync();
                    }
                }
                case SOUL_WIND -> {
                    // 灵魂风：户外玩家虚弱 + 反胃 + 掉理智；室内或防毒面具（隔绝怨气）免疫
                    if (inHome || hasGasMask(player)) continue;
                    if (now % (20 * 6) == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0, false, false, false));
                        stats.sanity = Math.max(0, stats.sanity - 2);
                        stats.sync();
                    }
                }
                case EMBER_STORM -> {
                    // 余烬风暴：户外玩家每 4 秒吸入烟尘污染 +1 并陷入 3 秒黑暗；防毒面具可防污染
                    if (inHome) continue;
                    if (now % (20 * 4) == 0) {
                        if (!hasGasMask(player)) {
                            addPollution(level, stats, 1);
                        }
                        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, false));
                    }
                }
                // ── 第三批：雨类天气（户外无伞者每 12 秒施加一次负面效果）──
                case ACID_RAIN -> {
                    // 酸雨：户外无伞者每 12 秒腐蚀扣血 + 污染
                    if (inHome || hasUmbrella(player)) continue;
                    if (now % (20 * 12) == 0) {
                        player.hurt(player.damageSources().generic(), 1.0F);
                        addPollution(level, stats, 1);
                    }
                }
                case POISON_RAIN -> {
                    // 毒雨：户外无伞者每 12 秒中毒 I（5 秒）
                    if (inHome || hasUmbrella(player)) continue;
                    if (now % (20 * 12) == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1, false, false, false));
                    }
                }
                case FROST_RAIN -> {
                    // 霜雨：户外无伞者每 12 秒缓慢 + 寒冷伤害
                    if (inHome || hasUmbrella(player)) continue;
                    if (now % (20 * 12) == 0) {
                        player.hurt(player.damageSources().generic(), 1.0F);
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 0, false, false, false));
                    }
                }
                case SLIME_RAIN -> {
                    // 粘液雨：户外无伞者每 12 秒缓慢 + 反胃
                    if (inHome || hasUmbrella(player)) continue;
                    if (now % (20 * 12) == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 80, 1, false, false, false));
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 60, 0, false, false, false));
                    }
                }
                case SPARK_RAIN -> {
                    // 电雨：户外无伞者每 12 秒虚弱
                    if (inHome || hasUmbrella(player)) continue;
                    if (now % (20 * 12) == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 120, 0, false, false, false));
                    }
                }
                case METEOR_SHOWER -> {
                    // 流星雨：户外随机被火球砸中（不放置方块/火）
                    if (inHome) continue;
                    if (now % (20 * 12) == 0
                            && level.getRandom().nextDouble() < SixtySecondsBalance.METEOR_HIT_CHANCE) {
                        player.hurt(player.damageSources().onFire(), 4.0F);
                        player.setRemainingFireTicks(60); // 着火3秒，不放置火方块
                        player.displayClientMessage(
                                Component.translatable("message.sixty_seconds.sixty_seconds.event_meteor_hit")
                                        .withStyle(ChatFormatting.RED), true);
                    }
                }
                case SPORE_FOG -> {
                    // 孢子迷雾：户外反胃+理智下降，室内免疫；防毒面具免疫
                    if (inHome || hasGasMask(player)) continue;
                    if (!player.hasEffect(MobEffects.CONFUSION)) {
                        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 200, 0,
                                false, false, false));
                    }
                    if (now % (20 * 10) == 0) {
                        stats.sanity = Math.max(0, stats.sanity - 2);
                        stats.sync();
                    }
                }
                case HAIL -> {
                    // 冰雹：户外持续伤害（间隔拉大 + 碎冰音效）
                    if (inHome) continue;
                    if (now % (20 * 10) == 0) {
                        player.hurt(player.damageSources().generic(), SixtySecondsBalance.HAIL_DAMAGE_PER_2S);
                        player.level().playSound(null, player.getX(), player.getY(), player.getZ(),
                                net.minecraft.sounds.SoundEvents.GLASS_BREAK,
                                net.minecraft.sounds.SoundSource.WEATHER, 0.4F, 1.5F);
                    }
                }
                case BLOOD_MOON -> {
                    // 血月：全员理智缓慢下降；怪物攻击力强化由 SixtySecondsHealthSystem.applyInjury 依据 isBloodMoon 实现
                    if (now % (20 * 15) == 0) {
                        stats.sanity = Math.max(0, stats.sanity - 2);
                        stats.sync();
                    }
                }
                case RADIATION_LEAK -> {
                    // 辐射泄漏：全员污染缓慢上升（屋内减半）
                    if (now % (20 * 10) == 0) {
                        int pollutionGain = inHome ? SixtySecondsBalance.RADIATION_POLLUTION_PER_10S / 2
                                : SixtySecondsBalance.RADIATION_POLLUTION_PER_10S;
                        if (inHome && pollutionGain == 0 && level.getRandom().nextBoolean()) {
                            pollutionGain = 1; // 屋内每20秒有50%概率+1污染
                        }
                        addPollution(level, stats, pollutionGain);
                    }
                }
                case DENSE_FOG -> {
                    // 浓雾：户外能见度极低；室内或防毒面具（滤雾）免疫
                    if (inHome || hasGasMask(player)) continue;
                    if (now % 20 == 0) {
                        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0,
                                false, false, false));
                    }
                }
                default -> {
                }
            }
        }
    }

    private static void airdrop(ServerLevel level) {
        if (SixtySecondsAirdrop.dropRandom(level)) {
            broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_airdrop")
                    .withStyle(ChatFormatting.GOLD));
            subtitleAll(level, "message.sixty_seconds.sixty_seconds.event_airdrop_name",
                    "message.sixty_seconds.sixty_seconds.event_airdrop", ChatFormatting.GOLD);
        }
    }

    private static void endEvent(ServerLevel level, Active active) {
        // 自然事件结束只清事件槽（forced=false），绝不影响指令预览槽
        net.exmo.sixty_seconds.weather.WeatherSync.send(level, null, false);
        switch (active.type) {
            case POLLUTION_RAIN -> {
                level.setWeatherParameters(0, 0, false, false);
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_pollution_rain_end")
                        .withStyle(ChatFormatting.GRAY));
            }
            case SMOG -> {
                for (ServerPlayer p : level.players()) {
                    p.removeEffect(MobEffects.BLINDNESS);
                }
                broadcast(level, Component.translatable(
                        "message.sixty_seconds.sixty_seconds.event_smog_end").withStyle(ChatFormatting.GRAY));
            }
            case COLD_SNAP -> broadcast(level, Component.translatable(
                    "message.sixty_seconds.sixty_seconds.event_cold_end").withStyle(ChatFormatting.GRAY));
            case ACID_FOG -> {
                for (ServerPlayer p : level.players()) {
                    p.removeEffect(MobEffects.POISON);
                }
                broadcast(level, Component.translatable(
                        "message.sixty_seconds.sixty_seconds.event_acid_fog_end").withStyle(ChatFormatting.GRAY));
            }
            case ELECTROMAGNETIC_STORM -> {
                for (ServerPlayer p : level.players()) {
                    p.removeEffect(MobEffects.WEAKNESS);
                }
                broadcast(level, Component.translatable(
                        "message.sixty_seconds.sixty_seconds.event_em_storm_end").withStyle(ChatFormatting.GRAY));
            }
            case SWARM -> broadcast(level, Component.translatable(
                    "message.sixty_seconds.sixty_seconds.event_swarm_end").withStyle(ChatFormatting.GRAY));
            case HEAT_WAVE -> {
                for (ServerPlayer p : level.players()) {
                    p.removeEffect(MobEffects.WEAKNESS);
                }
                broadcast(level, Component.translatable(
                        "message.sixty_seconds.sixty_seconds.event_heat_wave_end").withStyle(ChatFormatting.GRAY));
            }
            case SANDSTORM -> {
                for (ServerPlayer p : level.players()) {
                    p.removeEffect(MobEffects.BLINDNESS);
                    p.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
                }
                broadcast(level, Component.translatable(
                        "message.sixty_seconds.sixty_seconds.event_sandstorm_end").withStyle(ChatFormatting.GRAY));
            }
            case EARTHQUAKE -> {
                for (ServerPlayer p : level.players()) {
                    p.removeEffect(MobEffects.CONFUSION);
                }
                broadcast(level, Component.translatable(
                        "message.sixty_seconds.sixty_seconds.event_earthquake_end").withStyle(ChatFormatting.GRAY));
            }
            case METEOR_SHOWER -> broadcast(level, Component.translatable(
                    "message.sixty_seconds.sixty_seconds.event_meteor_shower_end").withStyle(ChatFormatting.GRAY));
            case SPORE_FOG -> {
                for (ServerPlayer p : level.players()) {
                    p.removeEffect(MobEffects.CONFUSION);
                }
                broadcast(level, Component.translatable(
                        "message.sixty_seconds.sixty_seconds.event_spore_fog_end").withStyle(ChatFormatting.GRAY));
            }
            case HAIL -> {
                broadcast(level, Component.translatable(
                        "message.sixty_seconds.sixty_seconds.event_hail_end").withStyle(ChatFormatting.GRAY));
                level.setWeatherParameters(20 * 60 * 10, 0, false, false);
            }
            case BLOOD_MOON -> broadcast(level, Component.translatable(
                    "message.sixty_seconds.sixty_seconds.event_blood_moon_end").withStyle(ChatFormatting.GRAY));
            case RADIATION_LEAK -> broadcast(level, Component.translatable(
                    "message.sixty_seconds.sixty_seconds.event_radiation_leak_end").withStyle(ChatFormatting.GRAY));
            case DENSE_FOG -> {
                for (ServerPlayer p : level.players()) {
                    p.removeEffect(MobEffects.BLINDNESS);
                }
                broadcast(level, Component.translatable(
                        "message.sixty_seconds.sixty_seconds.event_dense_fog_end").withStyle(ChatFormatting.GRAY));
            }
            case THUNDERSTORM -> {
                broadcast(level, Component.translatable(
                        "message.sixty_seconds.sixty_seconds.event_thunderstorm_end").withStyle(ChatFormatting.GRAY));
            }
            case TIDAL_SURGE -> {
                broadcast(level, Component.translatable(
                        "message.sixty_seconds.sixty_seconds.event_tidal_surge_end").withStyle(ChatFormatting.GRAY));
            }
            case VOID_RIFT -> {
                broadcast(level, Component.translatable(
                        "message.sixty_seconds.sixty_seconds.event_void_rift_end").withStyle(ChatFormatting.GRAY));
            }
            case ASH_FALL -> broadcast(level, Component.translatable(
                    "message.sixty_seconds.sixty_seconds.event_ash_fall_end").withStyle(ChatFormatting.GRAY));
            case FIRE_RAIN -> broadcast(level, Component.translatable(
                    "message.sixty_seconds.sixty_seconds.event_fire_rain_end").withStyle(ChatFormatting.GRAY));
            case CRYSTAL_STORM -> broadcast(level, Component.translatable(
                    "message.sixty_seconds.sixty_seconds.event_crystal_storm_end").withStyle(ChatFormatting.GRAY));
            case TOXIC_SPORE -> broadcast(level, Component.translatable(
                    "message.sixty_seconds.sixty_seconds.event_toxic_spore_end").withStyle(ChatFormatting.GRAY));
            case SOLAR_FLARE -> broadcast(level, Component.translatable(
                    "message.sixty_seconds.sixty_seconds.event_solar_flare_end").withStyle(ChatFormatting.GRAY));
            case SOUL_WIND -> broadcast(level, Component.translatable(
                    "message.sixty_seconds.sixty_seconds.event_soul_wind_end").withStyle(ChatFormatting.GRAY));
            case EMBER_STORM -> broadcast(level, Component.translatable(
                    "message.sixty_seconds.sixty_seconds.event_ember_storm_end").withStyle(ChatFormatting.GRAY));
            case ACID_RAIN -> {
                level.setWeatherParameters(0, 0, false, false);
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_acid_rain_end")
                        .withStyle(ChatFormatting.GREEN));
            }
            case POISON_RAIN -> {
                level.setWeatherParameters(0, 0, false, false);
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_poison_rain_end")
                        .withStyle(ChatFormatting.DARK_GREEN));
            }
            case FROST_RAIN -> {
                level.setWeatherParameters(0, 0, false, false);
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_frost_rain_end")
                        .withStyle(ChatFormatting.AQUA));
            }
            case SLIME_RAIN -> {
                level.setWeatherParameters(0, 0, false, false);
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_slime_rain_end")
                        .withStyle(ChatFormatting.GREEN));
            }
            case SPARK_RAIN -> {
                level.setWeatherParameters(0, 0, false, false);
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.event_spark_rain_end")
                        .withStyle(ChatFormatting.YELLOW));
            }
            default -> {
            }
        }
    }

    private static void addPollution(ServerLevel level, SixtySecondsStatsComponent stats, int amount) {
        // 难度加成：所有污染来源统一按难度放大（难度 8 起封顶 ×2.5）
        amount = SixtySecondsDifficulty.scalePollutionGain(level, amount);
        int before = stats.pollution;
        stats.pollution = Math.min(SixtySecondsStatsComponent.MAX, stats.pollution + amount);
        if (stats.pollution != before) {
            stats.sync();
        }
    }

    private static boolean isInHome(ServerPlayer player, SixtySecondsState.Data data, int teamId) {
        SixtySecondsState.TeamData team = data.teams.get(teamId);
        if (team == null) {
            return false;
        }
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        return (team.residentialBox != null && team.residentialBox.contains(x, y, z))
                || (team.shelterBox != null && team.shelterBox.contains(x, y, z));
    }

    private static boolean hasUmbrella(ServerPlayer player) {
        return player.getMainHandItem().is(ModItems.SIXTY_SECONDS_UMBRELLA)
                || player.getOffhandItem().is(ModItems.SIXTY_SECONDS_UMBRELLA);
    }

    /** 防毒面具（头部佩戴）：污雨/浓烟污染免疫。 */
    private static boolean hasGasMask(ServerPlayer player) {
        return player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)
                .is(ModItems.SIXTY_SECONDS_GAS_MASK);
    }

    /** 玩家是否身处或紧邻水体（3×3×3 范围检测流体）。用于潮涌事件。 */
    private static boolean isNearWater(ServerLevel level, ServerPlayer player) {
        net.minecraft.core.BlockPos center = player.blockPosition();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    net.minecraft.core.BlockPos p = center.offset(dx, dy, dz);
                    if (level.getFluidState(p).getType() != net.minecraft.world.level.material.Fluids.EMPTY) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void broadcast(ServerLevel level, Component message) {
        for (ServerPlayer player : level.players()) {
            player.displayClientMessage(message, false);
        }
    }

    private static void subtitleAll(ServerLevel level, String titleKey, String subtitleKey, ChatFormatting color) {
        Component title = Component.translatable(titleKey).withStyle(color);
        Component subtitle = Component.translatable(subtitleKey).withStyle(color);
        for (ServerPlayer player : level.players()) {
            player.connection.send(new ClientboundSetTitleTextPacket(title));
            player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        }
    }

    /** 检查当前是否有增强怪物的事件（电磁风暴或血月） */
    public static boolean isMonsterStrengthBoosted(ServerLevel level) {
        Active active = ACTIVE.get(level);
        return active != null && (active.type == EventType.ELECTROMAGNETIC_STORM
                || active.type == EventType.BLOOD_MOON);
    }

    /** 检查当前是否处于血月事件中 */
    public static boolean isBloodMoon(ServerLevel level) {
        Active active = ACTIVE.get(level);
        return active != null && active.type == EventType.BLOOD_MOON;
    }

    /** 返回当前进行中事件的类型（无事件返回 null） */
    public static EventType activeEventType(ServerLevel level) {
        Active active = ACTIVE.get(level);
        return active != null ? active.type : null;
    }

    /** 检查当前是否处于辐射泄漏事件中 */
    public static boolean isRadiationLeak(ServerLevel level) {
        Active active = ACTIVE.get(level);
        return active != null && active.type == EventType.RADIATION_LEAK;
    }

    public static void reset(ServerLevel level) {
        ACTIVE.remove(level);
        SCHEDULED.remove(level);
    }

    // ═══════════════════════════════════════════════════════════
    // 天气预报调度
    // ═══════════════════════════════════════════════════════════

    /** 预报用的事件类型池（排除瞬发型和不适合预报的） */
    static final EventType[] FORECASTABLE_TYPES = {
            EventType.POLLUTION_RAIN, EventType.SMOG, EventType.COLD_SNAP,
            EventType.ACID_FOG, EventType.ELECTROMAGNETIC_STORM, EventType.SWARM,
            EventType.HEAT_WAVE, EventType.SANDSTORM, EventType.METEOR_SHOWER,
            EventType.SPORE_FOG, EventType.HAIL, EventType.DENSE_FOG,
            EventType.RADIATION_LEAK, EventType.BLOOD_MOON,
            EventType.ASH_FALL, EventType.FIRE_RAIN, EventType.CRYSTAL_STORM,
            EventType.TOXIC_SPORE, EventType.SOLAR_FLARE, EventType.SOUL_WIND,
            EventType.EMBER_STORM, EventType.ACID_RAIN, EventType.POISON_RAIN,
            EventType.FROST_RAIN, EventType.SLIME_RAIN, EventType.SPARK_RAIN
    };

    /** 安排某天必定触发的事件（null 表示晴朗，不强制触发） */
    public static void scheduleForDay(ServerLevel level, int dayNumber, EventType type) {
        SCHEDULED.computeIfAbsent(level, k -> new java.util.HashMap<>()).put(dayNumber, type);
    }

    /** 获取某天的预报事件（null 表示晴朗） */
    public static EventType getScheduledForDay(ServerLevel level, int dayNumber) {
        Map<Integer, EventType> map = SCHEDULED.get(level);
        return map != null ? map.get(dayNumber) : null;
    }

    /** 获取预报事件的语言键（null 表示晴朗） */
    public static String getScheduledEventKey(ServerLevel level, int dayNumber) {
        EventType type = getScheduledForDay(level, dayNumber);
        if (type == null) return null;
        return switch (type) {
            case POLLUTION_RAIN -> "message.sixty_seconds.sixty_seconds.event_pollution_rain_name";
            case SMOG -> "message.sixty_seconds.sixty_seconds.event_smog_name";
            case COLD_SNAP -> "message.sixty_seconds.sixty_seconds.event_cold_name";
            case ACID_FOG -> "message.sixty_seconds.sixty_seconds.event_acid_fog_name";
            case ELECTROMAGNETIC_STORM -> "message.sixty_seconds.sixty_seconds.event_em_storm_name";
            case SWARM -> "message.sixty_seconds.sixty_seconds.event_swarm_name";
            case HEAT_WAVE -> "message.sixty_seconds.sixty_seconds.event_heat_wave_name";
            case SANDSTORM -> "message.sixty_seconds.sixty_seconds.event_sandstorm_name";
            case METEOR_SHOWER -> "message.sixty_seconds.sixty_seconds.event_meteor_shower_name";
            case SPORE_FOG -> "message.sixty_seconds.sixty_seconds.event_spore_fog_name";
            case HAIL -> "message.sixty_seconds.sixty_seconds.event_hail_name";
            case DENSE_FOG -> "message.sixty_seconds.sixty_seconds.event_dense_fog_name";
            case RADIATION_LEAK -> "message.sixty_seconds.sixty_seconds.event_radiation_leak_name";
            case BLOOD_MOON -> "message.sixty_seconds.sixty_seconds.event_blood_moon_name";
            case THUNDERSTORM -> "message.sixty_seconds.sixty_seconds.event_thunderstorm_name";
            case TIDAL_SURGE -> "message.sixty_seconds.sixty_seconds.event_tidal_surge_name";
            case VOID_RIFT -> "message.sixty_seconds.sixty_seconds.event_void_rift_name";
            case ASH_FALL -> "message.sixty_seconds.sixty_seconds.event_ash_fall_name";
            case FIRE_RAIN -> "message.sixty_seconds.sixty_seconds.event_fire_rain_name";
            case CRYSTAL_STORM -> "message.sixty_seconds.sixty_seconds.event_crystal_storm_name";
            case TOXIC_SPORE -> "message.sixty_seconds.sixty_seconds.event_toxic_spore_name";
            case SOLAR_FLARE -> "message.sixty_seconds.sixty_seconds.event_solar_flare_name";
            case SOUL_WIND -> "message.sixty_seconds.sixty_seconds.event_soul_wind_name";
            case EMBER_STORM -> "message.sixty_seconds.sixty_seconds.event_ember_storm_name";
            case ACID_RAIN -> "message.sixty_seconds.sixty_seconds.event_acid_rain_name";
            case POISON_RAIN -> "message.sixty_seconds.sixty_seconds.event_poison_rain_name";
            case FROST_RAIN -> "message.sixty_seconds.sixty_seconds.event_frost_rain_name";
            case SLIME_RAIN -> "message.sixty_seconds.sixty_seconds.event_slime_rain_name";
            case SPARK_RAIN -> "message.sixty_seconds.sixty_seconds.event_spark_rain_name";
            default -> null;
        };
    }
}
