package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.bridge.GameUtils;
import net.exmo.sixty_seconds.SixtySecondsBalance;
import net.exmo.sixty_seconds.SixtySecondsDayCycle;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.registry.ModBlocks;
import net.exmo.sixty_seconds.registry.ModItems;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

/**
 * 「低语怪」玩法（已不依赖亮度刷新）：
 * <ul>
 *   <li>晚上，在玩家实际所在的家（庇护所/住宅）内、玩家身边随机生成<b>低语怪</b>
 *       （无 AI 不攻击，但 4 格内每秒掉 san）；玩家点亮身边（电灯/火把等光源）则附近不刷、已刷的被驱散。</li>
 *   <li>清晨换日（第 2 天起）不再扫描暗角：只要「庇护所/住宅内有点亮的电灯」
 *       或「队伍内有人背包带手电筒/火把」，即视为已照亮，全队免罚；否则 san
 *       -{@link SixtySecondsBalance#DARK_DAWN_SAN_PENALTY}。</li>
 * </ul>
 * 反制：放置电灯/火把照亮身边；手电筒右键驱散；低语怪可近战驱散。
 */
public final class SixtySecondsWhisperSystem {
    public static final String WHISPER_TAG = "sixty_seconds_whisper";
    private static final Map<ServerLevel, List<UUID>> WHISPERS = new WeakHashMap<>();
    private static final int SAMPLES_PER_BOX = 12;
    /** 低语怪刷新时偏向「玩家所在侧」的采样半径（格）：在玩家身边这一圈优先刷，强化「家中黑暗角落」压迫感。 */
    private static final double SPAWN_BIAS_RADIUS = 6.0;
    /** 最强光源(发光 15)能照亮的最远曼哈顿距离 = 15 − 阈值(6) = 9；超出此距的光源无法压制低语怪。 */
    private static final int LAMP_REACH = 15 - SixtySecondsBalance.WHISPER_LIGHT_THRESHOLD;

    private SixtySecondsWhisperSystem() {
    }

    public static void tick(ServerLevel level) {
        SixtySecondsState.Data data = SixtySecondsState.get(level);
        long now = level.getGameTime();
        List<UUID> whispers = WHISPERS.computeIfAbsent(level, ignored -> new ArrayList<>());
        if (!SixtySecondsDayCycle.isNight(data, now)) {
            if (!whispers.isEmpty()) {
                clear(level);
            }
            return;
        }
        if (now % SixtySecondsBalance.WHISPER_SPAWN_INTERVAL == 0) {
            trySpawn(level, data, whispers);
        }
        if (now % 20 == 0) {
            drainSan(level, whispers);
            tickFlashlight(level);
            // 仅当本维度确实存在低语怪时才做光源驱散（逐队伍一次性收集光源方块，无怪时跳过避免空耗）
            if (!whispers.isEmpty()) {
                dispelLit(level, whispers, data);
            }
        }
    }

    /** 手电筒（手持，另免疫低语怪掉 san）/ 夜视镜（头部佩戴）：夜间获得夜视。 */
    private static void tickFlashlight(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            boolean goggles = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD)
                    .is(net.exmo.sixty_seconds.registry.ModItems.SIXTY_SECONDS_NIGHT_GOGGLES);
            if (holdsFlashlight(player) || goggles) {
                player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                        net.minecraft.world.effect.MobEffects.NIGHT_VISION, 20 * 15, 0, false, false, false));
            }
        }
    }

    private static boolean holdsFlashlight(ServerPlayer player) {
        return player.getMainHandItem().is(net.exmo.sixty_seconds.registry.ModItems.SIXTY_SECONDS_FLASHLIGHT)
                || player.getOffhandItem().is(net.exmo.sixty_seconds.registry.ModItems.SIXTY_SECONDS_FLASHLIGHT);
    }

    /**
     * 手电筒驱散（{@code SixtySecondsFlashlightItem} 右键调用）：用强光赶走玩家周围 {@code radius} 格内的低语怪，
     * 返回实际驱散数量（0 = 附近没有，调用方据此不扣电量）。
     * <p>
     * 用 AABB 定向查询而非遍历 {@code getAllEntities()}：既省开销，拿到的又是<b>快照列表</b>，
     * 逐个 discard 不会并发修改实体存储（曾因边遍历边 discard 吐 null NPE 崩服）。
     */
    public static int dispelNear(ServerLevel level, ServerPlayer player, double radius) {
        List<Vex> found = level.getEntitiesOfClass(Vex.class, player.getBoundingBox().inflate(radius),
                vex -> vex.getTags().contains(WHISPER_TAG));
        if (found.isEmpty()) {
            return 0;
        }
        List<UUID> tracked = WHISPERS.get(level);
        for (Vex vex : found) {
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                    vex.getX(), vex.getY() + 0.4D, vex.getZ(), 20, 0.25, 0.4, 0.25, 0.02);
            if (tracked != null) {
                tracked.remove(vex.getUUID()); // 同步移出追踪表，避免残留 UUID
            }
            vex.discard();
        }
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.VEX_DEATH, SoundSource.PLAYERS, 0.8F, 1.4F);
        return found.size();
    }

    private static void trySpawn(ServerLevel level, SixtySecondsState.Data data, List<UUID> whispers) {
        for (SixtySecondsState.TeamData team : data.teams.values()) {
            if (!hasOnlineMember(level, team)) {
                continue;
            }
            if (countTeamWhispers(level, whispers, team) >= SixtySecondsBalance.WHISPER_MAX_PER_TEAM) {
                continue;
            }
            // 刷新不再依赖亮度：只在玩家实际所在的盒（庇护所 / 住宅）内、玩家身边刷，
            // 且只刷在「无电灯 / 火把等光源」的站位——玩家点亮身边即不刷、已刷的也会被电灯驱散（见 dispelLit）。
            // 玩家不在盒内（如在外探索）则不刷，避免绕去其他暗角落。
            BlockPos bias = playerInsideBox(level, team, team.shelterBox);
            BlockPos dark = findSpawnSpot(level, team.shelterBox, bias);
            if (dark == null) {
                bias = playerInsideBox(level, team, team.residentialBox);
                dark = findSpawnSpot(level, team.residentialBox, bias);
            }
            if (dark == null) {
                continue;
            }
            Vex vex = EntityType.VEX.create(level);
            if (vex == null) {
                continue;
            }
            vex.setPos(dark.getX() + 0.5D, dark.getY() + 0.6D, dark.getZ() + 0.5D);
            vex.setNoAi(true);
            vex.setSilent(true);
            vex.setPersistenceRequired();
            vex.addTag(WHISPER_TAG);
            vex.setCustomName(Component.translatable("entity.sixty_seconds.sixty_seconds_whisper"));
            vex.setCustomNameVisible(false);
            level.addFreshEntity(vex);
            whispers.add(vex.getUUID());
            for (UUID uuid : team.members) {
                if (level.getPlayerByUUID(uuid) instanceof ServerPlayer member) {
                    member.playNotifySound(SoundEvents.AMBIENT_CAVE.value(), SoundSource.HOSTILE, 0.7F, 0.6F);
                    member.displayClientMessage(Component
                            .translatable("message.sixty_seconds.sixty_seconds.whisper_spawned")
                            .withStyle(ChatFormatting.DARK_GRAY), true);
                }
            }
        }
    }

    private static void drainSan(ServerLevel level, List<UUID> whispers) {
        for (Iterator<UUID> it = whispers.iterator(); it.hasNext();) {
            Entity entity = level.getEntity(it.next());
            if (entity == null || !entity.isAlive()) {
                it.remove();
                continue;
            }
            for (ServerPlayer player : level.players()) {
                if (GameUtils.isPlayerEliminated(player)
                        || player.distanceToSqr(entity) > SixtySecondsBalance.WHISPER_RANGE_SQR) {
                    continue;
                }
                SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
                // 出门探索（搜索区）的玩家不受低语影响——低语是「家中黑暗角落」的威胁
                if (stats.downed || stats.monster || holdsFlashlight(player)
                        || net.exmo.sixty_seconds.arena.SixtySecondsSearchZones.isInSearchZone(player)) {
                    continue;
                }
                stats.sanity = Math.max(0, stats.sanity - SixtySecondsBalance.WHISPER_SAN_DRAIN_PER_SEC);
                stats.sync();
                if (level.getGameTime() % (20 * 5) == 0) {
                    player.displayClientMessage(Component
                            .translatable("message.sixty_seconds.sixty_seconds.whisper_drain")
                            .withStyle(ChatFormatting.DARK_GRAY), true);
                }
            }
        }
    }

    /**
     * 清晨换日（第 2 天起）的「未照亮」惩罚：不再扫描暗角，改为状态检测——
     * 只要「庇护所/住宅内有点亮的电灯」或「队伍内有人背包带手电筒/火把」即视为已照亮、全队免罚；
     * 否则全队 san -{@link SixtySecondsBalance#DARK_DAWN_SAN_PENALTY}。
     */
    public static void applyDawnDarkPenalty(ServerLevel level, SixtySecondsState.Data data) {
        for (SixtySecondsState.TeamData team : data.teams.values()) {
            if (!hasOnlineMember(level, team)) {
                continue;
            }
            // 队伍内有人携带手电筒/火把 → 视为已照亮，免罚（最廉价，先判）
            if (memberCarriesLight(level, team)) {
                continue;
            }
            // 庇护所或住宅内有点亮的电灯（LIT 状态）→ 视为已照亮，免罚
            if (hasLitLamp(level, team.shelterBox) || hasLitLamp(level, team.residentialBox)) {
                continue;
            }
            for (UUID uuid : team.members) {
                if (level.getPlayerByUUID(uuid) instanceof ServerPlayer member
                        && GameUtils.isPlayerAliveAndSurvival(member)) {
                    SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(member);
                    stats.sanity = Math.max(0, stats.sanity - SixtySecondsBalance.DARK_DAWN_SAN_PENALTY);
                    stats.sync();
                    member.displayClientMessage(Component
                            .translatable("message.sixty_seconds.sixty_seconds.dark_dawn_penalty",
                                    SixtySecondsBalance.DARK_DAWN_SAN_PENALTY)
                            .withStyle(ChatFormatting.DARK_PURPLE), false);
                }
            }
        }
    }

    /** 队伍内是否有人背包（含主手/副手/盔甲槽，{@code getItems()} 覆盖全部）携带手电筒或火把。 */
    private static boolean memberCarriesLight(ServerLevel level, SixtySecondsState.TeamData team) {
        for (UUID uuid : team.members) {
            ServerPlayer player = (ServerPlayer) level.getPlayerByUUID(uuid);
            if (player == null || !GameUtils.isPlayerAliveAndSurvival(player)) {
                continue;
            }
            Inventory inv = player.getInventory();
            for (ItemStack stack : inv.items) {
                if (stack.is(ModItems.SIXTY_SECONDS_FLASHLIGHT) || stack.is(ModItems.SIXTY_SECONDS_TORCH)) {
                    return true;
                }
            }
            for (ItemStack stack : inv.armor) {
                if (stack.is(ModItems.SIXTY_SECONDS_FLASHLIGHT) || stack.is(ModItems.SIXTY_SECONDS_TORCH)) {
                    return true;
                }
            }
            for (ItemStack stack : inv.offhand) {
                if (stack.is(ModItems.SIXTY_SECONDS_FLASHLIGHT) || stack.is(ModItems.SIXTY_SECONDS_TORCH)) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 盒内是否存在「点亮的电灯」（LIT 状态的电灯/探照灯方块，亮时发光）。null 盒返回 false。 */
    private static boolean hasLitLamp(ServerLevel level, AABB box) {
        if (box == null) {
            return false;
        }
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int x = (int) Math.floor(box.minX); x <= (int) Math.ceil(box.maxX); x++) {
            for (int y = (int) Math.floor(box.minY); y <= (int) Math.ceil(box.maxY); y++) {
                for (int z = (int) Math.floor(box.minZ); z <= (int) Math.ceil(box.maxZ); z++) {
                    BlockState st = level.getBlockState(cursor.set(x, y, z));
                    if ((st.getBlock() == ModBlocks.SIXTY_SECONDS_LAMP
                            || st.getBlock() == ModBlocks.SIXTY_SECONDS_FLOODLIGHT)
                            && st.getValue(BlockStateProperties.LIT)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** 取队伍中位于指定盒（庇护所/住宅）内的在线存活玩家脚底坐标，作为低语怪刷新的「玩家侧」偏向中心。 */
    private static BlockPos playerInsideBox(ServerLevel level, SixtySecondsState.TeamData team, AABB box) {
        if (box == null) {
            return null;
        }
        for (UUID uuid : team.members) {
            ServerPlayer player = (ServerPlayer) level.getPlayerByUUID(uuid);
            if (player != null && GameUtils.isPlayerAliveAndSurvival(player)
                    && box.contains(player.position())) {
                return player.blockPosition();
            }
        }
        return null;
    }

    /**
     * 低语怪刷新点（不依赖亮度）：只在玩家所在盒内、玩家身边 {@code SPAWN_BIAS_RADIUS} 格采样，
     * 且只取「可站立」且「附近无电灯 / 火把等光源」的站位。玩家点亮身边后附近皆被照亮 → 采样不到可刷点 → 不刷；
     * 已存在的低语怪也会被 {@link #dispelLit} 驱散。玩家不在盒内（如在外探索）返回 null（不去其他暗角落刷）。
     * <p>性能：把玩家身边一圈的发光方块一次性收集（{@link #lampBlocks}），再对每个采样点只做廉价距离判断，
     * 避免每个采样点都扫 ~3900 方块（旧 {@link #litByNearbySource} 做法，12 采样点 × 3900）。
     */
    private static BlockPos findSpawnSpot(ServerLevel level, AABB box, BlockPos bias) {
        if (box == null || bias == null
                || !box.contains(bias.getX() + 0.5, bias.getY() + 0.5, bias.getZ() + 0.5)) {
            return null;
        }
        // 一次性收集玩家身边的发光方块（电灯/火把/灯笼…），刷新点只取不被其照亮的站位
        List<int[]> lamps = lampBlocks(level, bias, (int) (SPAWN_BIAS_RADIUS + LAMP_REACH));
        double r = Math.min(SPAWN_BIAS_RADIUS, Math.min((box.maxX - box.minX) / 2,
                Math.min((box.maxY - box.minY) / 2, (box.maxZ - box.minZ) / 2)));
        for (int i = 0; i < SAMPLES_PER_BOX; i++) {
            double rx = bias.getX() + (level.getRandom().nextDouble() * 2 - 1) * r;
            double ry = bias.getY() + (level.getRandom().nextDouble() * 2 - 1) * r;
            double rz = bias.getZ() + (level.getRandom().nextDouble() * 2 - 1) * r;
            double x = Math.max(box.minX, Math.min(box.maxX - 1e-9, rx));
            double y = Math.max(box.minY, Math.min(box.maxY - 1e-9, ry));
            double z = Math.max(box.minZ, Math.min(box.maxZ - 1e-9, rz));
            BlockPos pos = new BlockPos((int) Math.floor(x), (int) Math.floor(y), (int) Math.floor(z));
            if (isValidStandingSpot(level, pos) && !isLitByLamps(pos, lamps)) {
                return pos;
            }
        }
        return null;
    }

    /** 可站立站位：空气 + 下方实心（刷新已不依赖亮度，故不检查亮度）。 */
    private static boolean isValidStandingSpot(ServerLevel level, BlockPos pos) {
        return level.getBlockState(pos).isAir()
                && level.getBlockState(pos.below()).isSolidRender(level, pos.below());
    }

    /** 取 center 周围 radius 曼哈顿范围内所有发光方块（电灯/火把/灯笼/探照灯等）的位置与发光强度 [x,y,z,emission]。 */
    private static List<int[]> lampBlocks(ServerLevel level, BlockPos center, int radius) {
        List<int[]> lamps = new ArrayList<>();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (Math.abs(dx) + Math.abs(dy) + Math.abs(dz) > radius) {
                        continue; // 仅取曼哈顿球内，避免无谓的方块读取
                    }
                    int emission = level.getBlockState(cursor.set(
                            center.getX() + dx, center.getY() + dy, center.getZ() + dz)).getLightEmission();
                    if (emission > 0) {
                        lamps.add(new int[]{cursor.getX(), cursor.getY(), cursor.getZ(), emission});
                    }
                }
            }
        }
        return lamps;
    }

    /** pos 是否被 lamps 中任一光源照亮（曼哈顿光衰减：发光强度 − 曼哈顿距离 ≥ 阈值）。 */
    private static boolean isLitByLamps(BlockPos pos, List<int[]> lamps) {
        for (int[] l : lamps) {
            int md = Math.abs(l[0] - pos.getX()) + Math.abs(l[1] - pos.getY()) + Math.abs(l[2] - pos.getZ());
            if (l[3] - md >= SixtySecondsBalance.WHISPER_LIGHT_THRESHOLD) {
                return true;
            }
        }
        return false;
    }

    /**
     * 电灯 / 火把等光源驱散：逐队伍把「玩家身边发光方块」收集一次（而非每只怪扫 ~3900 方块），
     * 清除被照亮的低语怪（玩家点亮身边即驱散，已刷出的怪不再滞留）。仅在本维度存在低语怪时被 tick 调用。
     */
    private static void dispelLit(ServerLevel level, List<UUID> whispers, SixtySecondsState.Data data) {
        for (SixtySecondsState.TeamData team : data.teams.values()) {
            BlockPos p = playerInsideBox(level, team, team.shelterBox);
            if (p == null) {
                p = playerInsideBox(level, team, team.residentialBox);
            }
            if (p == null) {
                continue;
            }
            List<int[]> lamps = lampBlocks(level, p, (int) (SPAWN_BIAS_RADIUS + LAMP_REACH));
            if (lamps.isEmpty()) {
                continue;
            }
            for (Iterator<UUID> it = whispers.iterator(); it.hasNext();) {
                UUID id = it.next();
                Entity entity = level.getEntity(id);
                if (entity == null || !entity.isAlive()) {
                    it.remove();
                    continue;
                }
                if ((team.shelterBox != null && team.shelterBox.contains(entity.position()))
                        || (team.residentialBox != null && team.residentialBox.contains(entity.position()))) {
                    if (isLitByLamps(entity.blockPosition(), lamps)) {
                        level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD,
                                entity.getX(), entity.getY() + 0.4D, entity.getZ(), 12, 0.25, 0.4, 0.25, 0.02);
                        entity.discard();
                        it.remove();
                    }
                }
            }
        }
    }

    private static int countTeamWhispers(ServerLevel level, List<UUID> whispers, SixtySecondsState.TeamData team) {
        int count = 0;
        for (UUID uuid : whispers) {
            Entity entity = level.getEntity(uuid);
            if (entity == null || !entity.isAlive()) {
                continue;
            }
            if ((team.shelterBox != null && team.shelterBox.contains(entity.position()))
                    || (team.residentialBox != null && team.residentialBox.contains(entity.position()))) {
                count++;
            }
        }
        return count;
    }

    private static boolean hasOnlineMember(ServerLevel level, SixtySecondsState.TeamData team) {
        for (UUID uuid : team.members) {
            if (level.getPlayerByUUID(uuid) instanceof ServerPlayer player
                    && GameUtils.isPlayerAliveAndSurvival(player)) {
                return true;
            }
        }
        return false;
    }

    public static void clear(ServerLevel level) {
        List<UUID> whispers = WHISPERS.get(level);
        if (whispers != null) {
            for (UUID uuid : whispers) {
                Entity entity = level.getEntity(uuid);
                if (entity != null) {
                    entity.discard();
                }
            }
            whispers.clear();
        }
        clearTaggedFailsafe(level);
    }

    /**
     * Failsafe：清除所有带 WHISPER_TAG 但未被追踪列表覆盖的低语怪
     * （例如被枪/手雷击杀后从列表移除但实体仍未 discard、WeakHashMap 条目回收等边缘情况）。
     * 必须<b>先收集再删除</b>：一边遍历 {@code getAllEntities()} 一边 discard 会并发修改实体存储，
     * 迭代器可能吐出 null（NPE 崩服实录：crash-2026-07-14_03.05.39）。
     */
    private static void clearTaggedFailsafe(ServerLevel level) {
        List<Entity> toRemove = new ArrayList<>();
        for (Entity entity : level.getAllEntities()) {
            if (entity != null && entity.getTags().contains(WHISPER_TAG)) {
                toRemove.add(entity);
            }
        }
        for (Entity entity : toRemove) {
            if (!entity.isRemoved()) {
                entity.discard();
            }
        }
    }
}
