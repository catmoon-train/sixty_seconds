package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayConnectionEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 掉线备份/重连恢复系统：游戏进行中玩家掉线时自动快照其<b>背包 + 全部 60s 状态</b>；
 * 若其在<b>本局结束前</b>重连，恢复背包与状态并重新入队（队伍成员表本就保留 UUID）。
 * <p>
 * 需要它的原因：{@code SixtySecondsStatsComponent} 是局内状态、刻意不落盘（重登即重置），
 * 掉线重连会丢失 队伍/身份/五值/倒地 等一切。备份表按局清空（{@code begin}/{@code stopGame}），
 * 不会把上一局的快照恢复到下一局。管理员也可用 {@code /60s backup} 手动 保存/恢复/查看。
 */
public final class SixtySecondsReconnect {
    private static final Map<UUID, SixtySecondsSaveManager.PlayerSnapshot> BACKUPS = new HashMap<>();

    private SixtySecondsReconnect() {
    }

    /** 模组初始化时注册一次。 */
    public static void register() {
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer player = handler.getPlayer();
            ServerLevel main = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (SixtySecondsMod.RUNNING && main != null && SixtySecondsMod.isActive(main)) {
                save(player);
            }
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer player = handler.getPlayer();
            ServerLevel main = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
            if (SixtySecondsMod.RUNNING && main != null && SixtySecondsMod.isActive(main)
                    && BACKUPS.containsKey(player.getUUID())) {
                // 推迟一 tick：等玩家完全初始化（背包/组件就绪）再恢复
                server.execute(() -> {
                    ServerPlayer online = server.getPlayerList().getPlayer(player.getUUID());
                    if (online != null && SixtySecondsMod.RUNNING) {
                        restore(online);
                    }
                });
            }
        });
    }

    /** 快照玩家背包与全部 60s 状态（重复调用覆盖旧快照）。仅对已入队玩家有效。 */
    public static boolean save(ServerPlayer player) {
        SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
        if (stats.teamId < 0) {
            return false; // 未入队（旁观等）无需备份
        }
        BACKUPS.put(player.getUUID(), SixtySecondsSaveManager.capturePlayerSnapshot(
                player, player.serverLevel().registryAccess()));
        return true;
    }

    /** 恢复背包与状态（快照消耗掉）；dayNumber/phaseEndTick 用当前世界值刷新（离线期间可能换日）。 */
    public static boolean restore(ServerPlayer player) {
        SixtySecondsSaveManager.PlayerSnapshot s = BACKUPS.remove(player.getUUID());
        if (s == null) {
            return false;
        }
        // 与退出存档使用完全相同的原版玩家 NBT + 模组状态恢复流程。
        SixtySecondsSaveManager.restorePlayerSnapshot(player, s, player.serverLevel());
        SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
        // 换日相关用当前值刷新，避免离线期间过期
        if (player.level() instanceof ServerLevel level) {
            ServerLevel main = level.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
            ServerLevel progressLevel = main != null ? main : level;
            SixtySecondsState.Data data = SixtySecondsState.get(progressLevel);
            stats.dayNumber = data.dayNumber;
            stats.totalDays = SixtySecondsManager.totalDays(progressLevel); // HUD「第 X/N 天」的 N（可配置）
            stats.phaseEndTick = data.phaseEndTick;
            // 重发区域地图范围（客户端断线时已清空）
            SixtySecondsState.TeamData team = data.teams.get(stats.teamId);
            if (team != null) {
                net.exmo.sixty_seconds.network.SixtySecondsMapZoneS2CPacket.send(
                        player, team.shelterBox, team.shelterSpawn, true);
            }

            // 只有已经进入复活等待的玩家才保持旁观模式；正常存活玩家必须保留存档中的模式。
            if (stats.reviveEndTick > 0L) {
                player.setGameMode(GameType.SPECTATOR);
            }
        }
        stats.sync();
        player.displayClientMessage(Component.translatable(
                "message.sixty_seconds.sixty_seconds.reconnect_restored").withStyle(ChatFormatting.GREEN), false);
        return true;
    }

    public static boolean hasBackup(UUID uuid) {
        return BACKUPS.containsKey(uuid);
    }

    public static int backupCount() {
        return BACKUPS.size();
    }

    static java.util.Collection<SixtySecondsSaveManager.PlayerSnapshot> snapshotsForSave() {
        return java.util.List.copyOf(BACKUPS.values());
    }

    /** 换局清空：上一局的快照不得泄漏到下一局。 */
    public static void reset() {
        BACKUPS.clear();
    }
}
