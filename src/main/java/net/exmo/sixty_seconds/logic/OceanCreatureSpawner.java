package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.exmo.sixty_seconds.config.SixtySecondsConfigStore;
import net.exmo.sixty_seconds.entity.OceanSeaMonsterEntity;
import net.exmo.sixty_seconds.entity.OceanSharkEntity;
import net.exmo.sixty_seconds.init.ModOceanEntities;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.MobSpawnType;
import net.exmo.sixty_seconds.registry.ModEffects;
import org.jetbrains.annotations.Nullable;

/**
 * 海洋生物刷新系统。
 *
 * <h3>鲨鱼 / 海盗 NPC（数据驱动刷新）</h3>
 * <p>{@code ocean_shark} 与海盗 NPC 已改为 NeoForge {@code biome_modifier} + {@code forge:add_spawns}
 * 数据驱动自然刷新（见 {@code data/sixty_seconds/forge/biome_modifier/ocean_creatures.json}），
 * 变体稀有度由实体 {@code finalizeSpawn} 内按原概率随机决定。本类不再手动刷鲨鱼。
 *
 * <h3>海怪 KRAKEN / SERPENT（手动刷，含出场特效）</h3>
 * <p>保留手动刷怪以维持播报/浓雾/音效等出场特效。</p>
 *
 * <h3>利维坦 LEVIATHAN（定时刷）</h3>
 * <p>游戏开始时（dayNumber 未记录过）刷第一只，之后每 {@link #LEVIATHAN_PERIOD_DAYS}（6）个由对局推进的游戏日刷一只，
 * 与鲨鱼/海怪独立、不计入刷怪上限、可与其他 Boss 共存，优先刷在玩家附近海域。</p>
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

    /** 利维坦（LEVIATHAN）刷新周期：每 6 个由对局推进的游戏日（dayNumber）刷一只。 */
    public static final int LEVIATHAN_PERIOD_DAYS = 6;

    private static final int MAX_NEARBY_SHARKS = 5;
    private static final int MAX_NEARBY_MONSTERS = 2;
    private static final double NEARBY_RADIUS = 64.0;
    private static final double MONSTER_FOG_RADIUS = 32.0; // 海怪浓雾作用半径

    private static final int SPAWN_MIN_DIST = 14;
    private static final int SPAWN_MAX_DIST = 36;

    private OceanCreatureSpawner() {}

    /**
     * 对所有在线存活探索中玩家做海洋生物刷新判定。
     * 刷新概率 = 基础概率 × dayRatio（前四天额外 ×0.3）。
     */
    public static void tick(ServerLevel level) {
        SixtySecondsConfig config = SixtySecondsConfigStore.current(level).orElse(null);
        if (config == null || !config.oceanCreaturesEnabled) return;

        SixtySecondsState.Data data = SixtySecondsState.get(level);
        if (data == null) return;
        // 海洋（海岛）维度没有对局推进的游戏日：用 totalDays 作为等效难度（始终满强度）
        boolean inOcean = level.dimension() == SixtySeconds.OCEAN_DIMENSION;
        int dayNumber = data.dayNumber > 0 ? data.dayNumber : Math.max(1, config.totalDays);

        // ── 天数比例：dayRatio = currentDay / totalDays ─────────────
        double dayRatio = (double) dayNumber / Math.max(1, config.totalDays);
        boolean night = level.isNight();

        // 前四天额外压降 ×0.3（保证前几天几乎不刷强怪）
        double earlyDayMult = dayNumber <= 4 ? 0.3 : 1.0;
        // 海怪基础概率（鲨鱼已改数据驱动刷新，不在此手动刷）
        double monsterBase = (night ? 0.042 : 0.007) * dayRatio * earlyDayMult;

        RandomSource random = level.getRandom();

        // 利维坦定时刷新（与鲨鱼/海怪独立，可共存）
        tickLeviathan(level, dayNumber);

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
            // 注：鲨鱼 ocean_shark 已通过 biome_modifier 数据驱动自然刷新，这里不再手动刷。
            if (nearbyMonsters < MAX_NEARBY_MONSTERS && random.nextDouble() < monsterBase) {
                BlockPos spot = findWaterSpot(level, player.blockPosition(),
                        SPAWN_MIN_DIST + 8, SPAWN_MAX_DIST + 12, random);
                if (spot != null) {
                    OceanSeaMonsterEntity monster = spawnSeaMonster(level, spot, random, dayRatio);
                    if (monster != null) {
                        announceSeaMonster(level, monster, player);
                    }
                }
            }
        }
    }

    /**
     * 利维坦定时刷新：游戏开始时（dayNumber 未记录过）刷第一只，之后每跨过
     * {@link #LEVIATHAN_PERIOD_DAYS}（6）个由对局推进的游戏日（dayNumber）刷一只。
     * 与鲨鱼/海怪完全独立，不计入 {@code MAX_NEARBY_MONSTERS} 上限，可与其他 Boss 共存。
     * 优先刷新在最近存活玩家的附近开阔海域。
     *
     * @param dayNumber 当前由对局推进的游戏天数（SixtySecondsState.Data.dayNumber）
     */
    private static void tickLeviathan(ServerLevel level, int dayNumber) {
        SixtySecondsState.Data data = SixtySecondsState.get(level);
        if (data == null) return;
        boolean neverSpawned = data.leviathanLastSpawnDay == Integer.MIN_VALUE;
        if (!neverSpawned && dayNumber - data.leviathanLastSpawnDay < LEVIATHAN_PERIOD_DAYS) {
            return;
        }
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
        if (shark == null) return null;
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
        shark.finalizeSpawn(level, level.getCurrentDifficultyAt(waterPos),
                MobSpawnType.NATURAL, null);
        level.addFreshEntity(shark);
        return shark;
    }

    // ══════════════════════════════════════════════════════════════
    //  海怪生成（KRAKEN / SERPENT；利维坦改由 tickLeviathan 定时刷）
    // ══════════════════════════════════════════════════════════════

    @Nullable
    public static OceanSeaMonsterEntity spawnSeaMonster(ServerLevel level, BlockPos waterPos,
            RandomSource random, double dayRatio) {
        OceanSeaMonsterEntity monster = ModOceanEntities.OCEAN_SEA_MONSTER.create(level);
        if (monster == null) return null;
        monster.moveTo(waterPos.getX() + 0.5, waterPos.getY(), waterPos.getZ() + 0.5,
                random.nextFloat() * 360.0F, 0.0F);

        // 利维坦不自然刷新 —— 只由指令生成
        OceanSeaMonsterEntity.Variant variant;
        float r = random.nextFloat();
        if (r < 0.20 + 0.15 * dayRatio) {
            variant = OceanSeaMonsterEntity.Variant.SERPENT;
        } else {
            variant = OceanSeaMonsterEntity.Variant.KRAKEN;
        }
        monster.applyVariant(variant);
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
}
