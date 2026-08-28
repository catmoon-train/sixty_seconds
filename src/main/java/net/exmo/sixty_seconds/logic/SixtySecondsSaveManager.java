package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.SixtySecondsPhase;
import net.exmo.sixty_seconds.bridge.GameUtils;
import net.exmo.sixty_seconds.bridge.SixtySecGameTimeComponent;
import net.exmo.sixty_seconds.bridge.SixtySecGameWorldComponent;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraft.world.phys.AABB;

import java.io.InputStream;
import java.io.OutputStream;
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
 * 末日60秒存档系统：把整局游戏进度（世界状态 + 每位玩家的背包与角色状态 + 职业分配）序列化到
 * 世界目录下的 {@code sixty_seconds_save.dat}。
 *
 * <ul>
 *   <li>{@link #save} 立即保存；{@link #autoSaveIfNeeded} 在游戏进行中每 5 分钟自动保存一次。</li>
 *   <li>{@link #delete} 在一局游戏结束时调用，自动删除上一局存档。</li>
 *   <li>{@link #onPlayerJoin} 在世界重载后首位玩家加入时自动重开本局并恢复上一把进度，
 *       使“玩家/主机退出存档再进入”能继续上一局。</li>
 * </ul>
 *
 * <p>恢复采用“沿用布局 + 覆盖进度”策略：世界里的住宅/避难所是上一局留下的<b>真实方块</b>
 * （本模组不做地形还原、不回滚），因此续档时<b>不再重新建图</b>，而是直接沿用存档记录的队伍划分与
 * 建筑坐标（住宅/避难所出生点与范围盒、回家门、夜袭门、房车安全点等），
 * 只把天数/阶段/队伍数值/科技/供电/家门/玩家背包与状态覆盖回去。</p>
 */
public final class SixtySecondsSaveManager {
    private static final String FILE_NAME = "sixty_seconds_save.dat";
    /** 自动存档间隔：5 分钟（tick）。 */
    private static final long AUTO_SAVE_INTERVAL = 5L * 60 * 20;

    /** 建图完成回调中等待覆盖的存档快照。 */
    private static volatile SavedGame pendingSnapshot = null;
    /**
     * {@link #pendingSnapshot} 所属世界的标识（世界根目录绝对路径）。
     *
     * <p>这些状态是 <b>static 的，不会被世界切换重置</b>：单人游戏里「退出到主菜单 → 新建/打开另一个世界」
     * 仍在同一个 JVM 进程内，静态字段原样保留。若上一世界的快照残留到新世界，
     * {@link #applyPendingOverlay} 就会把旧存档的进度与玩家组件覆盖到全新的一局上，
     * 表现为天数为 0、状态栏/HUD 不显示、背包只剩 2 格等随机症状。
     * 故快照必须绑定世界，取用时校验。</p>
     */
    private static volatile String pendingWorldId = null;
    /** 防止同一进程内重复触发恢复。 */
    private static volatile boolean resumeTriggered = false;
    /** 建图时尚未上线的玩家：待其加入时再恢复。 */
    private static final Map<UUID, PlayerSave> offlineRestores = new HashMap<>();
    private static final Map<ServerLevel, Long> lastAutoSave = new WeakHashMap<>();

    private SixtySecondsSaveManager() {
    }

    // ── 路径 ──────────────────────────────────────────────────────────
    private static Path savePath(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }

    public static boolean hasSave(ServerLevel level) {
        try {
            return Files.exists(savePath(level));
        } catch (Exception e) {
            return false;
        }
    }

    public static void delete(ServerLevel level) {
        try {
            Files.deleteIfExists(savePath(level));
        } catch (Exception ignored) {
        }
        pendingSnapshot = null;
        pendingWorldId = null;
        resumeTriggered = false;
        offlineRestores.clear();
    }

    // ── 立即保存 ──────────────────────────────────────────────────────
    public static void save(ServerLevel level) {
        try {
            SavedGame snap = buildSnapshot(level);
            writeSnapshot(level, snap);
        } catch (Exception e) {
            System.err.println("[SixtySecondsSaveManager] 保存失败: " + e);
            e.printStackTrace();
        }
    }

    // ── 自动存档（每 tick 调用） ──────────────────────────────────────
    public static void autoSaveIfNeeded(ServerLevel level) {
        if (!SixtySecondsMod.RUNNING || !SixtySecondsMod.isActive(level)) {
            return;
        }
        long now = level.getGameTime();
        Long last = lastAutoSave.get(level);
        if (last == null || now - last >= AUTO_SAVE_INTERVAL) {
            lastAutoSave.put(level, now);
            save(level);
        }
    }

    // ── 玩家加入世界时调用 ────────────────────────────────────────────
    public static void onPlayerJoin(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        if (!(level instanceof ServerLevel sl) || sl.dimension() != Level.OVERWORLD) {
            return;
        }
        // 重载世界后，若存在存档且当前没有进行中的游戏，自动重开并恢复上一局
        if (!SixtySecondsMod.RUNNING && hasSave(sl) && !resumeTriggered) {
            resume(sl);
        }
        // 离线玩家（建图时尚未上线）的恢复缓存
        PlayerSave ps = offlineRestores.remove(player.getUUID());
        if (ps != null) {
            restorePlayer(player, ps, sl);
        }
    }

    private static void resume(ServerLevel level) {
        resumeTriggered = true;
        try {
            SavedGame snap = readSnapshot(level);
            if (snap == null) {
                resumeTriggered = false;
                return;
            }
            pendingSnapshot = snap;
            // 快照与世界绑定：后续 takeResumeLayout / applyPendingOverlay / isResuming 都会校验，
            // 一旦进程内换了世界，这份快照会被自动丢弃而不是污染新局。
            pendingWorldId = worldIdOf(level);
            // 不限制参与玩家：重载后首位在线的非旁观玩家即可开局（minPlayerCount=1）
            GameUtils.startGame(level, SixtySecondsMod.MODE, snap.minutes);
        } catch (Exception e) {
            System.err.println("[SixtySecondsSaveManager] 恢复失败: " + e);
            e.printStackTrace();
            resumeTriggered = false;
            pendingSnapshot = null;
            pendingWorldId = null;
        }
    }

    /**
     * 世界标识：取世界根目录的绝对路径。
     * 单人游戏里换存档 / 新建世界都会得到不同路径，可据此判断快照是否属于当前世界。
     */
    private static String worldIdOf(ServerLevel level) {
        try {
            return level.getServer().getWorldPath(LevelResource.ROOT).toAbsolutePath().normalize().toString();
        } catch (Exception e) {
            // 兜底：拿不到路径时退化为「维度 + 世界名」，至少能区分主世界与其他维度
            return level.dimension().location() + "@" + level.getServer().getWorldData().getLevelName();
        }
    }

    /**
     * 取出属于<b>当前世界</b>的待覆盖快照；不属于则丢弃残留并返回 null。
     *
     * <p>这是跨世界静态残留的唯一防线：任何消费 {@link #pendingSnapshot} 的入口都必须走这里，
     * 不能直接读字段。</p>
     */
    private static SavedGame pendingFor(ServerLevel level) {
        SavedGame snap = pendingSnapshot;
        if (snap == null) {
            return null;
        }
        String current = worldIdOf(level);
        if (pendingWorldId == null || !pendingWorldId.equals(current)) {
            System.out.println("[SixtySecondsSaveManager] 丢弃不属于当前世界的待恢复快照"
                    + "（残留自 " + pendingWorldId + "，当前为 " + current + "），本局按全新开局处理。");
            pendingSnapshot = null;
            pendingWorldId = null;
            return null;
        }
        return snap;
    }

    /** 当前是否处于「重载世界后恢复上一局」流程（开局回调据此跳过重新建图等副作用）。 */
    public static boolean isResuming(ServerLevel level) {
        return pendingFor(level) != null;
    }

    /** 清空全部运行时状态：进程内换世界 / 关服时调用，杜绝静态残留跨局生效。 */
    public static void resetRuntimeState() {
        pendingSnapshot = null;
        pendingWorldId = null;
        resumeTriggered = false;
        offlineRestores.clear();
        lastAutoSave.clear();
    }

    /**
     * 续档恢复：把存档中的<b>队伍划分与建筑坐标</b>写回本局 data，并返回「teamId → 该队在线成员」。
     *
     * <p>世界里的住宅/避难所是上一局留下的真实方块（本模组不做地形还原、不回滚），
     * 因此续档时必须沿用存档中的坐标，<b>绝不能重新克隆一套</b>——否则会在玩家脚下再堆一栋房子，
     * 而上一局建好的房子也不会消失。</p>
     *
     * <p>队伍槽位按存档的 teamId 原样重建，保证建筑坐标与队伍一一对应；
     * 存档里没有的新玩家稍后由调用方补进成员最少的队。</p>
     *
     * @return null 表示当前不在恢复流程、或存档不含布局（旧存档）——调用方应回退到正常开局建图。
     */
    public static Map<Integer, List<UUID>> takeResumeLayout(ServerLevel level, SixtySecondsState.Data data) {
        SavedGame snap = pendingFor(level);
        if (snap == null || !snap.hasLayout || snap.teams == null || snap.teams.isEmpty()) {
            return null;
        }
        Map<Integer, List<UUID>> byTeam = new LinkedHashMap<>();
        data.teams.clear();
        for (TeamSave st : snap.teams) {
            SixtySecondsState.TeamData team = new SixtySecondsState.TeamData(st.teamId);
            applyTeamLayout(team, st);
            data.teams.put(st.teamId, team);
            List<UUID> online = new ArrayList<>();
            for (UUID uuid : st.members) {
                if (level.getPlayerByUUID(uuid) != null) {
                    online.add(uuid);
                }
            }
            byTeam.put(st.teamId, online);
        }
        System.out.println("[SixtySecondsSaveManager] 续档：沿用上一局建筑布局，"
                + data.teams.size() + " 队，跳过重新建图。");
        return byTeam;
    }

    /**
     * 由 {@code SixtySecondsManager.onBuildComplete} 在建图完成后调用，把存档进度覆盖到新建的本局上。
     */
    public static void applyPendingOverlay(ServerLevel level, SixtySecondsState.Data freshData) {
        // 注意：必须先校验世界。直接读字段会把上一世界的残留快照盖到全新的一局上。
        SavedGame snap = pendingFor(level);
        if (snap == null) {
            return;
        }
        pendingSnapshot = null;
        pendingWorldId = null;
        try {
            freshData.dayNumber = snap.dayNumber;
            freshData.phase = snap.phase;
            freshData.phaseEndTick = snap.phaseEndTick;
            freshData.lastDayStage = snap.lastDayStage;
            freshData.lastNpcRvSpawnDay = snap.lastNpcRvSpawnDay;
            freshData.helicopterArrived = snap.helicopterArrived;
            freshData.helicopterEvacuated.clear();
            freshData.helicopterEvacuated.addAll(snap.helicopterEvacuated);

            // 各队进度（建筑坐标已由 takeResumeLayout 沿用存档，这里只覆盖数值型进度）
            for (SixtySecondsState.TeamData ft : freshData.teams.values()) {
                TeamSave st = snap.teamById(ft.teamId);
                if (st != null) {
                    applyTeam(ft, st);
                }
            }

            // 玩家背包 + 状态
            for (PlayerSave ps : snap.players) {
                ServerPlayer p = level.getServer().getPlayerList().getPlayer(ps.uuid);
                if (p != null) {
                    restorePlayer(p, ps, level);
                } else {
                    offlineRestores.put(ps.uuid, ps);
                }
            }

            SixtySecondsManager.syncDayNumber(level, freshData, freshData.dayNumber);
            SixtySecondsManager.broadcast(level,
                    Component.translatable("message.sixty_seconds.sixty_seconds.restored_progress",
                            freshData.dayNumber, freshData.phase).withStyle(ChatFormatting.GREEN));
            System.out.println("[SixtySecondsSaveManager] 已恢复上一局进度：第 " + freshData.dayNumber + " 天 / " + freshData.phase);
            // 立即把恢复后的进度落盘，避免 5 分钟内再次退出时进度回退
            writeSnapshot(level, snap);
        } catch (Exception e) {
            System.err.println("[SixtySecondsSaveManager] 覆盖进度失败: " + e);
            e.printStackTrace();
        }
    }

    // ── 进度覆盖助手 ──────────────────────────────────────────────────
    private static void applyTeam(SixtySecondsState.TeamData ft, TeamSave st) {
        ft.members.clear();
        ft.members.addAll(st.members);
        ft.storedSupplies.clear();
        ft.storedSupplies.addAll(st.storedSupplies);
        ft.unlockedTech.clear();
        ft.unlockedTech.addAll(st.unlockedTech);
        ft.rvRespawnCooldown = st.rvRespawnCooldown;
        ft.powerEndTick = st.powerEndTick;
        ft.doorHp = st.doorHp;
        ft.doorMaxHp = st.doorMaxHp;
        ft.doorBroken = st.doorBroken;
        ft.doorLevel = st.doorLevel;
        ft.ironReinforceCount = st.ironReinforceCount;
        ft.alarmTonight = st.alarmTonight;
        ft.lureTonight = st.lureTonight;
        ft.doorLockEndTick = st.doorLockEndTick;
        ft.doorLockTier = st.doorLockTier;
        ft.doorTrapEndTick = st.doorTrapEndTick;
        ft.dailyModifiers.clear();
        ft.dailyModifiers.putAll(st.dailyModifiers);
        ft.sisterOutside = st.sisterOutside;
        ft.sisterUUID = st.sisterUUID;
    }

    /** 把存档中的建筑布局（位置类字段）覆盖回队伍：续档沿用上一局留在世界里的住宅/避难所。 */
    private static void applyTeamLayout(SixtySecondsState.TeamData ft, TeamSave st) {
        ft.residentialSpawn = st.residentialSpawn;
        ft.shelterSpawn = st.shelterSpawn;
        ft.residentialBox = st.residentialBox;
        ft.shelterBox = st.shelterBox;
        ft.searchZoneSpawn = st.searchZoneSpawn;
        ft.searchZoneBox = st.searchZoneBox;
        ft.returnDoorPos = st.returnDoorPos;
        ft.doorPos = st.doorPos;
        ft.rvLastSafePos = st.rvLastSafePos;
        ft.searchDoors.clear();
        ft.searchDoors.putAll(st.searchDoors);
    }

    private static void restorePlayer(ServerPlayer p, PlayerSave ps, ServerLevel level) {
        try {
            p.getInventory().load(ps.inventory);
            SixtySecondsStatsComponent comp = SixtySecondsStatsComponent.KEY.get(p);
            comp.readFromSyncNbt(ps.stats, level.registryAccess());
            // 必须同步给客户端：readFromSyncNbt 只改了服务端组件，
            // 不 sync 的话客户端仍是建图时那一份初值，HUD / 状态栏 / 背包显示会与服务端不一致。
            comp.sync();
        } catch (Exception e) {
            System.err.println("[SixtySecondsSaveManager] 恢复玩家 " + ps.uuid + " 失败: " + e);
            e.printStackTrace();
        }
    }

    // ── 快照构建 ──────────────────────────────────────────────────────
    private static SavedGame buildSnapshot(ServerLevel level) {
        SixtySecondsState.Data data = SixtySecondsState.get(level);
        HolderLookup.Provider provider = level.registryAccess();
        SavedGame g = new SavedGame();
        g.minutes = SixtySecGameTimeComponent.KEY.get(level).getResetTime();
        g.dayNumber = data.dayNumber;
        g.phase = data.phase;
        g.phaseEndTick = data.phaseEndTick;
        g.lastDayStage = data.lastDayStage;
        g.lastNpcRvSpawnDay = data.lastNpcRvSpawnDay;
        g.helicopterArrived = data.helicopterArrived;
        g.helicopterEvacuated = new ArrayList<>(data.helicopterEvacuated);

        g.teams = new ArrayList<>();
        for (SixtySecondsState.TeamData t : data.teams.values()) {
            g.teams.add(buildTeam(t, provider));
        }

        g.hasLayout = true;
        g.buildAnchor = net.exmo.sixty_seconds.SixtySecondsMod.PREBUILT_ANCHOR;

        g.players = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            SixtySecondsStatsComponent comp = SixtySecondsStatsComponent.KEY.get(p);
            if (comp.teamId < 0) {
                continue; // 仅保存参战玩家
            }
            PlayerSave ps = new PlayerSave();
            ps.uuid = p.getUUID();
            ps.inventory = p.getInventory().save(new ListTag());
            CompoundTag st = new CompoundTag();
            comp.writeToSyncNbt(st, provider);
            ps.stats = st;
            g.players.add(ps);
        }
        return g;
    }

    private static TeamSave buildTeam(SixtySecondsState.TeamData t, HolderLookup.Provider provider) {
        TeamSave s = new TeamSave();
        s.teamId = t.teamId;
        s.members = new ArrayList<>(t.members);
        s.storedSupplies = new ArrayList<>(t.storedSupplies);
        s.unlockedTech = new HashSet<>(t.unlockedTech);
        s.rvRespawnCooldown = t.rvRespawnCooldown;
        s.powerEndTick = t.powerEndTick;
        s.doorHp = t.doorHp;
        s.doorMaxHp = t.doorMaxHp;
        s.doorBroken = t.doorBroken;
        s.doorLevel = t.doorLevel;
        s.ironReinforceCount = t.ironReinforceCount;
        s.alarmTonight = t.alarmTonight;
        s.lureTonight = t.lureTonight;
        s.doorLockEndTick = t.doorLockEndTick;
        s.doorLockTier = t.doorLockTier;
        s.doorTrapEndTick = t.doorTrapEndTick;
        s.dailyModifiers = new HashMap<>(t.dailyModifiers);
        s.sisterOutside = t.sisterOutside;
        s.sisterUUID = t.sisterUUID;
        // 建筑布局：续档时沿用世界里的既有住宅/避难所，不重新克隆
        s.residentialSpawn = t.residentialSpawn;
        s.shelterSpawn = t.shelterSpawn;
        s.residentialBox = t.residentialBox;
        s.shelterBox = t.shelterBox;
        s.searchZoneSpawn = t.searchZoneSpawn;
        s.searchZoneBox = t.searchZoneBox;
        s.returnDoorPos = t.returnDoorPos;
        s.doorPos = t.doorPos;
        s.rvLastSafePos = t.rvLastSafePos;
        s.searchDoors = new HashMap<>(t.searchDoors);
        return s;
    }

    // ── NBT 读写 ──────────────────────────────────────────────────────
    private static void writeSnapshot(ServerLevel level, SavedGame g) throws Exception {
        HolderLookup.Provider provider = level.registryAccess();
        CompoundTag root = new CompoundTag();
        root.putInt("minutes", g.minutes);
        root.putInt("dayNumber", g.dayNumber);
        root.putString("phase", g.phase.name());
        root.putLong("phaseEndTick", g.phaseEndTick);
        root.putInt("lastDayStage", g.lastDayStage);
        root.putInt("lastNpcRvSpawnDay", g.lastNpcRvSpawnDay);
        root.putBoolean("helicopterArrived", g.helicopterArrived);
        root.put("helicopterEvacuated", uuidList(g.helicopterEvacuated));
        root.putBoolean("hasLayout", g.hasLayout);
        if (g.buildAnchor != null) {
            root.putLong("buildAnchor", g.buildAnchor.asLong());
        }
        ListTag teams = new ListTag();
        for (TeamSave t : g.teams) {
            teams.add(writeTeam(t, provider));
        }
        root.put("teams", teams);
        ListTag pls = new ListTag();
        for (PlayerSave p : g.players) {
            pls.add(writePlayer(p));
        }
        root.put("players", pls);

        Path path = savePath(level);
        Files.createDirectories(path.getParent());
        try (OutputStream os = Files.newOutputStream(path)) {
            NbtIo.writeCompressed(root, os);
        }
    }

    private static SavedGame readSnapshot(ServerLevel level) {
        Path path = savePath(level);
        if (!Files.exists(path)) {
            return null;
        }
        try (InputStream is = Files.newInputStream(path)) {
            CompoundTag root = NbtIo.readCompressed(is, NbtAccounter.create(Long.MAX_VALUE));
            SavedGame g = new SavedGame();
            g.minutes = root.getInt("minutes");
            g.dayNumber = root.getInt("dayNumber");
            g.phase = SixtySecondsPhase.valueOf(root.getString("phase"));
            g.phaseEndTick = root.getLong("phaseEndTick");
            g.lastDayStage = root.getInt("lastDayStage");
            g.lastNpcRvSpawnDay = root.getInt("lastNpcRvSpawnDay");
            g.helicopterArrived = root.getBoolean("helicopterArrived");
            g.helicopterEvacuated = readUuidList(root.getList("helicopterEvacuated", Tag.TAG_STRING));
            g.hasLayout = root.getBoolean("hasLayout");
            if (root.contains("buildAnchor")) {
                g.buildAnchor = BlockPos.of(root.getLong("buildAnchor"));
            }
            g.teams = new ArrayList<>();
            for (Tag t : root.getList("teams", Tag.TAG_COMPOUND)) {
                g.teams.add(readTeam((CompoundTag) t, level.registryAccess()));
            }
            g.players = new ArrayList<>();
            for (Tag t : root.getList("players", Tag.TAG_COMPOUND)) {
                g.players.add(readPlayer((CompoundTag) t));
            }
            return g;
        } catch (Exception e) {
            System.err.println("[SixtySecondsSaveManager] 读取存档失败: " + e);
            return null;
        }
    }

    private static CompoundTag writeTeam(TeamSave t, HolderLookup.Provider provider) {
        CompoundTag c = new CompoundTag();
        c.putInt("teamId", t.teamId);
        c.put("members", uuidList(t.members));
        ListTag sup = new ListTag();
        for (ItemStack s : t.storedSupplies) {
            sup.add(s.save(provider));
        }
        c.put("storedSupplies", sup);
        c.put("unlockedTech", stringList(new ArrayList<>(t.unlockedTech)));
        c.putInt("rvRespawnCooldown", t.rvRespawnCooldown);
        c.putLong("powerEndTick", t.powerEndTick);
        c.putInt("doorHp", t.doorHp);
        c.putInt("doorMaxHp", t.doorMaxHp);
        c.putBoolean("doorBroken", t.doorBroken);
        c.putInt("doorLevel", t.doorLevel);
        c.putInt("ironReinforceCount", t.ironReinforceCount);
        c.putBoolean("alarmTonight", t.alarmTonight);
        c.putBoolean("lureTonight", t.lureTonight);
        c.putLong("doorLockEndTick", t.doorLockEndTick);
        c.putInt("doorLockTier", t.doorLockTier);
        c.putLong("doorTrapEndTick", t.doorTrapEndTick);
        ListTag dm = new ListTag();
        for (Map.Entry<String, Double> e : t.dailyModifiers.entrySet()) {
            CompoundTag d = new CompoundTag();
            d.putString("key", e.getKey());
            d.putDouble("value", e.getValue());
            dm.add(d);
        }
        c.put("dailyModifiers", dm);
        c.putBoolean("sisterOutside", t.sisterOutside);
        if (t.sisterUUID != null) {
            c.putString("sisterUUID", t.sisterUUID.toString());
        }
        // 建筑布局（位置类字段）
        if (t.residentialSpawn != null) {
            c.putLong("residentialSpawn", t.residentialSpawn.asLong());
        }
        if (t.shelterSpawn != null) {
            c.putLong("shelterSpawn", t.shelterSpawn.asLong());
        }
        if (t.residentialBox != null) {
            writeBox(c, "residentialBox", t.residentialBox);
        }
        if (t.shelterBox != null) {
            writeBox(c, "shelterBox", t.shelterBox);
        }
        if (t.searchZoneSpawn != null) {
            c.putLong("searchZoneSpawn", t.searchZoneSpawn.asLong());
        }
        if (t.searchZoneBox != null) {
            writeBox(c, "searchZoneBox", t.searchZoneBox);
        }
        if (t.returnDoorPos != null) {
            c.putLong("returnDoorPos", t.returnDoorPos.asLong());
        }
        if (t.doorPos != null) {
            c.putLong("doorPos", t.doorPos.asLong());
        }
        if (t.rvLastSafePos != null) {
            c.putLong("rvLastSafePos", t.rvLastSafePos.asLong());
        }
        ListTag doors = new ListTag();
        for (Map.Entry<BlockPos, SixtySecondsState.TeamData.SearchLink> e : t.searchDoors.entrySet()) {
            CompoundTag d = new CompoundTag();
            d.putLong("door", e.getKey().asLong());
            if (e.getValue().spawn() != null) {
                d.putLong("spawn", e.getValue().spawn().asLong());
            }
            if (e.getValue().box() != null) {
                writeBox(d, "box", e.getValue().box());
            }
            doors.add(d);
        }
        c.put("searchDoors", doors);
        return c;
    }

    private static void writeBox(CompoundTag c, String key, AABB box) {
        ListTag l = new ListTag();
        for (double v : new double[] { box.minX, box.minY, box.minZ, box.maxX, box.maxY, box.maxZ }) {
            l.add(net.minecraft.nbt.DoubleTag.valueOf(v));
        }
        c.put(key, l);
    }

    @org.jetbrains.annotations.Nullable
    private static AABB readBox(CompoundTag c, String key) {
        ListTag l = c.getList(key, Tag.TAG_DOUBLE);
        if (l.size() < 6) {
            return null;
        }
        double[] v = new double[6];
        for (int i = 0; i < 6; i++) {
            v[i] = ((net.minecraft.nbt.DoubleTag) l.get(i)).getAsDouble();
        }
        return new AABB(v[0], v[1], v[2], v[3], v[4], v[5]);
    }

    private static TeamSave readTeam(CompoundTag c, HolderLookup.Provider provider) {
        TeamSave t = new TeamSave();
        t.teamId = c.getInt("teamId");
        t.members = readUuidList(c.getList("members", Tag.TAG_STRING));
        t.storedSupplies = new ArrayList<>();
        for (Tag s : c.getList("storedSupplies", Tag.TAG_COMPOUND)) {
            t.storedSupplies.add(ItemStack.parse(provider, (CompoundTag) s).orElse(ItemStack.EMPTY));
        }
        t.unlockedTech = new HashSet<>(readStringList(c.getList("unlockedTech", Tag.TAG_STRING)));
        t.rvRespawnCooldown = c.getInt("rvRespawnCooldown");
        t.powerEndTick = c.getLong("powerEndTick");
        t.doorHp = c.getInt("doorHp");
        t.doorMaxHp = c.getInt("doorMaxHp");
        t.doorBroken = c.getBoolean("doorBroken");
        t.doorLevel = c.getInt("doorLevel");
        t.ironReinforceCount = c.getInt("ironReinforceCount");
        t.alarmTonight = c.getBoolean("alarmTonight");
        t.lureTonight = c.getBoolean("lureTonight");
        t.doorLockEndTick = c.getLong("doorLockEndTick");
        t.doorLockTier = c.getInt("doorLockTier");
        t.doorTrapEndTick = c.getLong("doorTrapEndTick");
        t.dailyModifiers = new HashMap<>();
        for (Tag d : c.getList("dailyModifiers", Tag.TAG_COMPOUND)) {
            CompoundTag dc = (CompoundTag) d;
            t.dailyModifiers.put(dc.getString("key"), dc.getDouble("value"));
        }
        t.sisterOutside = c.getBoolean("sisterOutside");
        if (c.contains("sisterUUID")) {
            t.sisterUUID = UUID.fromString(c.getString("sisterUUID"));
        }
        // 建筑布局（位置类字段）
        if (c.contains("residentialSpawn")) {
            t.residentialSpawn = BlockPos.of(c.getLong("residentialSpawn"));
        }
        if (c.contains("shelterSpawn")) {
            t.shelterSpawn = BlockPos.of(c.getLong("shelterSpawn"));
        }
        if (c.contains("residentialBox")) {
            t.residentialBox = readBox(c, "residentialBox");
        }
        if (c.contains("shelterBox")) {
            t.shelterBox = readBox(c, "shelterBox");
        }
        if (c.contains("searchZoneSpawn")) {
            t.searchZoneSpawn = BlockPos.of(c.getLong("searchZoneSpawn"));
        }
        if (c.contains("searchZoneBox")) {
            t.searchZoneBox = readBox(c, "searchZoneBox");
        }
        if (c.contains("returnDoorPos")) {
            t.returnDoorPos = BlockPos.of(c.getLong("returnDoorPos"));
        }
        if (c.contains("doorPos")) {
            t.doorPos = BlockPos.of(c.getLong("doorPos"));
        }
        if (c.contains("rvLastSafePos")) {
            t.rvLastSafePos = BlockPos.of(c.getLong("rvLastSafePos"));
        }
        t.searchDoors = new HashMap<>();
        for (Tag d : c.getList("searchDoors", Tag.TAG_COMPOUND)) {
            CompoundTag dc = (CompoundTag) d;
            if (!dc.contains("door")) {
                continue;
            }
            BlockPos door = BlockPos.of(dc.getLong("door"));
            BlockPos spawn = dc.contains("spawn") ? BlockPos.of(dc.getLong("spawn")) : null;
            AABB box = dc.contains("box") ? readBox(dc, "box") : null;
            t.searchDoors.put(door, new SixtySecondsState.TeamData.SearchLink(spawn, box));
        }
        return t;
    }

    private static CompoundTag writePlayer(PlayerSave p) {
        CompoundTag c = new CompoundTag();
        c.putString("uuid", p.uuid.toString());
        c.put("inventory", p.inventory);
        c.put("stats", p.stats);
        return c;
    }

    private static PlayerSave readPlayer(CompoundTag c) {
        PlayerSave p = new PlayerSave();
        p.uuid = UUID.fromString(c.getString("uuid"));
        p.inventory = c.getList("inventory", Tag.TAG_COMPOUND);
        p.stats = c.getCompound("stats");
        return p;
    }

    private static ListTag uuidList(List<UUID> list) {
        ListTag l = new ListTag();
        for (UUID u : list) {
            l.add(StringTag.valueOf(u.toString()));
        }
        return l;
    }

    private static List<UUID> readUuidList(ListTag list) {
        List<UUID> r = new ArrayList<>();
        for (Tag t : list) {
            r.add(UUID.fromString(((StringTag) t).getAsString()));
        }
        return r;
    }

    private static ListTag stringList(List<String> list) {
        ListTag l = new ListTag();
        for (String s : list) {
            l.add(StringTag.valueOf(s));
        }
        return l;
    }

    private static List<String> readStringList(ListTag list) {
        List<String> r = new ArrayList<>();
        for (Tag t : list) {
            r.add(((StringTag) t).getAsString());
        }
        return r;
    }

    // ── 快照数据 ──────────────────────────────────────────────────────
    private static final class SavedGame {
        int minutes;
        int dayNumber;
        SixtySecondsPhase phase;
        long phaseEndTick;
        int lastDayStage;
        int lastNpcRvSpawnDay;
        boolean helicopterArrived;
        List<UUID> helicopterEvacuated;
        List<TeamSave> teams;
        List<PlayerSave> players;
        /** 是否记录了建筑布局；旧存档没有该字段，续档时回退到正常开局建图。 */
        boolean hasLayout = false;
        /** 建图锚点（诊断/核对用，恢复不依赖它）。 */
        BlockPos buildAnchor;

        TeamSave teamById(int id) {
            for (TeamSave t : teams) {
                if (t.teamId == id) {
                    return t;
                }
            }
            return null;
        }
    }

    private static final class TeamSave {
        int teamId;
        List<UUID> members;
        List<ItemStack> storedSupplies;
        Set<String> unlockedTech;
        int rvRespawnCooldown;
        long powerEndTick;
        int doorHp;
        int doorMaxHp;
        boolean doorBroken;
        int doorLevel;
        int ironReinforceCount;
        boolean alarmTonight;
        boolean lureTonight;
        long doorLockEndTick;
        int doorLockTier;
        long doorTrapEndTick;
        Map<String, Double> dailyModifiers;
        boolean sisterOutside;
        UUID sisterUUID;

        // ── 建筑布局（续档复用世界里的既有建筑，不重新克隆）──────────────
        BlockPos residentialSpawn;
        BlockPos shelterSpawn;
        AABB residentialBox;
        AABB shelterBox;
        BlockPos searchZoneSpawn;
        AABB searchZoneBox;
        BlockPos returnDoorPos;
        BlockPos doorPos;
        BlockPos rvLastSafePos;
        Map<BlockPos, SixtySecondsState.TeamData.SearchLink> searchDoors = new HashMap<>();
    }

    private static final class PlayerSave {
        UUID uuid;
        ListTag inventory;
        CompoundTag stats;
    }
}
