package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.exmo.sixty_seconds.config.SixtySecondsConfigStore;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.exmo.sixty_seconds.network.SixtySecondsHelicopterS2CPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;
import net.minecraft.world.phys.AABB;

/**
 * 直升机撤离系统：<b>全局单架</b>，最后一天白天开始时在管理员预设的 {@code helicopterLandingPos}
 * 处抵达。玩家可在撤离区内等待，全天显示倒计时 HUD。最后一天结束时（游戏结算前），
 * 仍在撤离区内的所有幸存玩家（未倒地、非怪化）统一撤离获胜。
 */
public final class SixtySecondsHelicopterEvac {

    /** 撤离区半径（格）。 */
    public static final int EVAC_RADIUS = 8;

    private SixtySecondsHelicopterEvac() {
    }

    /**
     * 直升机抵达：记录撤离结束时间戳（本日 phaseEndTick），广播 S2C 包含总秒数与剩余秒数。
     */
    public static void arrive(ServerLevel level, SixtySecondsState.Data data, BlockPos landingPos) {
        data.helicopterArrived = true;
        data.helicopterEvacuated.clear();

        // 撤离在当日结束时结算（phaseEndTick 是当天结束 tick）
        long now = level.getGameTime();
        long remainingTicks = Math.max(0, data.phaseEndTick - now);
        int totalSec = (int) (remainingTicks / 20);
        int remainingSec = totalSec;

        level.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("message.sixty_seconds.sixty_seconds.helicopter_arrive",
                        landingPos.getX(), landingPos.getY(), landingPos.getZ())
                        .withStyle(ChatFormatting.GOLD), false);

        SixtySecondsHelicopterS2CPacket.broadcastArrive(
                level.players(), landingPos, EVAC_RADIUS, totalSec, remainingSec);
    }

    private static BlockPos landingPos(ServerLevel level) {
        var config = SixtySecondsConfigStore.current(level).orElse(null);
        if (config == null || config.helicopterLandingPos == null) return BlockPos.ZERO;
        return config.helicopterLandingPos.toBlockPos();
    }

    /**
     * 每 tick 同步剩余倒计时给客户端（每秒一次），不执行撤离。
     */
    public static void tick(ServerLevel level, SixtySecondsState.Data data) {
        if (!data.helicopterArrived) return;
        if (level.getGameTime() % 20 != 0) return; // 每秒同步

        long remainingTicks = Math.max(0, data.phaseEndTick - level.getGameTime());
        int remainingSec = (int) (remainingTicks / 20);
        int totalSec = remainingSec; // 简化为当前剩余=总剩余

        var pos = landingPos(level);
        if (!pos.equals(BlockPos.ZERO)) {
            SixtySecondsHelicopterS2CPacket.broadcastArrive(
                    level.players(), pos, EVAC_RADIUS, totalSec, remainingSec);
        }

        // 撤离点建筑（evacuationpoint）进入/离开提示：逐玩家判定，避免全局画圆。
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) {
                data.evacBuildingZone.remove(player.getUUID());
                continue;
            }
            boolean inside = net.exmo.sixty_seconds.lostcities.SixtySecondsLostCitiesStarMap
                    .isEvacuationPoint(level, player.blockPosition());
            boolean was = data.evacBuildingZone.contains(player.getUUID());
            if (inside && !was) {
                data.evacBuildingZone.add(player.getUUID());
                player.displayClientMessage(
                        Component.translatable("message.sixty_seconds.sixty_seconds.evac_point_enter")
                                .withStyle(ChatFormatting.AQUA), true);
            } else if (!inside && was) {
                data.evacBuildingZone.remove(player.getUUID());
                player.displayClientMessage(
                        Component.translatable("message.sixty_seconds.sixty_seconds.evac_point_leave")
                                .withStyle(ChatFormatting.GRAY), true);
            }
        }
    }

    /**
     * 最终撤离：游戏结算前调用。遍历所有在线幸存玩家，在撤离区内的标记为已撤离并切旁观。
     * 返回成功撤离的人数。
     */
    public static int finalizeEvac(ServerLevel level, SixtySecondsState.Data data) {
        if (!data.helicopterArrived) return 0;

        var pos = landingPos(level);
        // 既有直升机降落点则正常撤离；无降落点但世界里有撤离点建筑（evacuationpoint）时，
        // 仍以该建筑内部作为撤离区，仅不渲染圆形 HUD。
        boolean useLandingZone = !pos.equals(BlockPos.ZERO);

        AABB zone = useLandingZone ? new AABB(pos).inflate(EVAC_RADIUS) : null;
        int evacuated = 0;

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) continue;
            var stats = SixtySecondsStatsComponent.KEY.get(player);
            if (stats.downed || stats.monster) continue;
            // 撤离区 = 直升机降落点半径范围 或 60秒模组自带的撤离点建筑（evacuationpoint）内部
            boolean inZone = (zone != null && zone.contains(player.getX(), player.getY(), player.getZ()))
                    || net.exmo.sixty_seconds.lostcities.SixtySecondsLostCitiesStarMap
                            .isEvacuationPoint(level, player.blockPosition());
            if (!inZone) continue;

            data.helicopterEvacuated.add(player.getUUID());
            player.setGameMode(net.minecraft.world.level.GameType.SPECTATOR);
            player.displayClientMessage(
                    Component.translatable("message.sixty_seconds.sixty_seconds.helicopter_evac_you")
                            .withStyle(ChatFormatting.GREEN), false);
            evacuated++;
        }

        if (evacuated > 0) {
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable("message.sixty_seconds.sixty_seconds.helicopter_final",
                            evacuated).withStyle(ChatFormatting.GOLD), false);
        } else {
            level.getServer().getPlayerList().broadcastSystemMessage(
                    Component.translatable("message.sixty_seconds.sixty_seconds.helicopter_no_evac")
                            .withStyle(ChatFormatting.RED), false);
        }

        // 清空客户端渲染（有降落点时渲染过圆形 HUD，无降落点时无圆心可清，跳过）
        if (useLandingZone) {
            SixtySecondsHelicopterS2CPacket.broadcastArrive(
                    level.players(), pos, EVAC_RADIUS, 0, 0);
        }

        return evacuated;
    }
}
