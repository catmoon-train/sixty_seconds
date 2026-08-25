package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.SixtySecondsPhase;
import net.exmo.sixty_seconds.bridge.GameUtils;
import net.exmo.sixty_seconds.bridge.SixtySecGameTimeComponent;
import net.exmo.sixty_seconds.bridge.SixtySecGameWorldComponent;
import net.exmo.sixty_seconds.bridge.SixtySecRole;
import net.exmo.sixty_seconds.bridge.stubs.SixtySecRoles;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.LevelResource;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
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
 * <p>恢复采用“重开 + 覆盖进度”策略：先走正常开局建图（保证所有子系统的临时结构齐备），
 * 建图完成后再把存档中的进度（天数/阶段/队伍数值/科技/供电/家门/玩家背包与状态/职业）覆盖回去。
 * 地图坐标来自新建的地图（旧地图已不存在），故位置类字段不恢复。</p>
 */
public final class SixtySecondsSaveManager {
    private static final String FILE_NAME = "sixty_seconds_save.dat";
    /** 自动存档间隔：5 分钟（tick）。 */
    private static final long AUTO_SAVE_INTERVAL = 5L * 60 * 20;

    /** 建图完成回调中等待覆盖的存档快照。 */
    private static volatile SavedGame pendingSnapshot = null;
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
            // 不限制参与玩家：重载后首位在线的非旁观玩家即可开局（minPlayerCount=1）
            GameUtils.startGame(level, SixtySecondsMod.MODE, snap.minutes);
        } catch (Exception e) {
            System.err.println("[SixtySecondsSaveManager] 恢复失败: " + e);
            e.printStackTrace();
            resumeTriggered = false;
            pendingSnapshot = null;
        }
    }

    /**
     * 由 {@code SixtySecondsManager.onBuildComplete} 在建图完成后调用，把存档进度覆盖到新建的本局上。
     */
    public static void applyPendingOverlay(ServerLevel level, SixtySecondsState.Data freshData) {
        SavedGame snap = pendingSnapshot;
        pendingSnapshot = null;
        if (snap == null) {
            return;
        }
        try {
            freshData.dayNumber = snap.dayNumber;
            freshData.phase = snap.phase;
            freshData.phaseEndTick = snap.phaseEndTick;
            freshData.lastDayStage = snap.lastDayStage;
            freshData.lastNpcRvSpawnDay = snap.lastNpcRvSpawnDay;
            freshData.helicopterArrived = snap.helicopterArrived;
            freshData.helicopterEvacuated.clear();
            freshData.helicopterEvacuated.addAll(snap.helicopterEvacuated);
            freshData.usedAwakenRoles.clear();
            freshData.usedAwakenRoles.addAll(snap.usedAwakenRoles);

            // 各队进度（保留新建地图的坐标，仅覆盖数值型进度）
            for (SixtySecondsState.TeamData ft : freshData.teams.values()) {
                TeamSave st = snap.teamById(ft.teamId);
                if (st != null) {
                    applyTeam(ft, st);
                }
            }

            // 职业分配
            SixtySecGameWorldComponent comp = SixtySecGameWorldComponent.KEY.get(level);
            comp.setRoles(snap.roles);

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

    private static void restorePlayer(ServerPlayer p, PlayerSave ps, ServerLevel level) {
        try {
            p.getInventory().load(ps.inventory);
            SixtySecondsStatsComponent comp = SixtySecondsStatsComponent.KEY.get(p);
            comp.readFromSyncNbt(ps.stats, level.registryAccess());
            if (ps.role != null) {
                SixtySecGameWorldComponent.KEY.get(level).setRole(p, ps.role);
            }
        } catch (Exception e) {
            System.err.println("[SixtySecondsSaveManager] 恢复玩家 " + ps.uuid + " 失败: " + e);
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
        g.usedAwakenRoles = new ArrayList<>(data.usedAwakenRoles);

        g.teams = new ArrayList<>();
        for (SixtySecondsState.TeamData t : data.teams.values()) {
            g.teams.add(buildTeam(t, provider));
        }

        g.roles = new HashMap<>(SixtySecGameWorldComponent.KEY.get(level).getRoles());

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
            ps.role = SixtySecGameWorldComponent.KEY.get(level).getRole(p);
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
        root.put("usedAwakenRoles", stringList(g.usedAwakenRoles));
        ListTag teams = new ListTag();
        for (TeamSave t : g.teams) {
            teams.add(writeTeam(t, provider));
        }
        root.put("teams", teams);
        ListTag roles = new ListTag();
        for (Map.Entry<UUID, SixtySecRole> e : g.roles.entrySet()) {
            CompoundTag rc = new CompoundTag();
            rc.putString("uuid", e.getKey().toString());
            rc.putString("role", e.getValue().getIdentifier().toString());
            roles.add(rc);
        }
        root.put("roles", roles);
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
            g.usedAwakenRoles = readStringList(root.getList("usedAwakenRoles", Tag.TAG_STRING));
            g.teams = new ArrayList<>();
            for (Tag t : root.getList("teams", Tag.TAG_COMPOUND)) {
                g.teams.add(readTeam((CompoundTag) t, level.registryAccess()));
            }
            g.roles = new HashMap<>();
            for (Tag t : root.getList("roles", Tag.TAG_COMPOUND)) {
                CompoundTag rc = (CompoundTag) t;
                g.roles.put(UUID.fromString(rc.getString("uuid")), lookupRole(rc.getString("role")));
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
        return c;
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
        return t;
    }

    private static CompoundTag writePlayer(PlayerSave p) {
        CompoundTag c = new CompoundTag();
        c.putString("uuid", p.uuid.toString());
        c.put("inventory", p.inventory);
        c.put("stats", p.stats);
        if (p.role != null) {
            c.putString("role", p.role.getIdentifier().toString());
        }
        return c;
    }

    private static PlayerSave readPlayer(CompoundTag c) {
        PlayerSave p = new PlayerSave();
        p.uuid = UUID.fromString(c.getString("uuid"));
        p.inventory = c.getList("inventory", Tag.TAG_COMPOUND);
        p.stats = c.getCompound("stats");
        p.role = c.contains("role") ? lookupRole(c.getString("role")) : null;
        return p;
    }

    private static SixtySecRole lookupRole(String id) {
        return SixtySecRoles.ROLES.get(ResourceLocation.parse(id));
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
        List<String> usedAwakenRoles;
        List<TeamSave> teams;
        Map<UUID, SixtySecRole> roles;
        List<PlayerSave> players;

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
    }

    private static final class PlayerSave {
        UUID uuid;
        ListTag inventory;
        CompoundTag stats;
        SixtySecRole role;
    }
}
