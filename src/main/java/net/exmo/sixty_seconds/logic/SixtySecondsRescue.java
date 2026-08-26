package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.bridge.GameUtils;
import net.exmo.sixty_seconds.SixtySecondsBalance;
import net.exmo.sixty_seconds.SixtySecondsPhase;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.content.entity.SixtySecondsFlyingVehicleEntity;
import net.exmo.sixty_seconds.lostcities.SixtySecondsLostCitiesStarMap;
import net.exmo.sixty_seconds.registry.ModEntities;
import net.exmo.sixty_seconds.registry.ModItems;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;

import java.util.UUID;

/**
 * 救援信标（工程学科技合成）：仅当使用者是<b>队伍中唯一存活成员</b>时，可于游戏日<b>户外</b>激活 →
 * 全服广播 + {@link SixtySecondsBalance#RESCUE_COUNTDOWN_TICKS} 倒计时；激活时给使用者打上「救援标记」。
 * <ul>
 *   <li>被标记的玩家<b>进入撤离点建筑即可直接撤离</b>（与直升机撤离点一致的撤离）；</li>
 *   <li>倒计时归零 → 在玩家身旁<b>生成一架救援直升机</b>并<b>附赠 2 桶航空煤油</b>，作为自救载具；
 *       不再像旧版那样直接判幸存者胜利。</li>
 * </ul>
 * 期间激活队伍全灭则救援取消。全局单槽会话（一局同时只有一次救援呼叫）。
 */
public final class SixtySecondsRescue {
    private static long endTick = -1;
    private static int teamId = -1;
    /** 救援呼叫持有者（使用者）的 UUID；倒计时归零时在其身旁生成救援直升机。 */
    private static UUID rescuePlayer = null;

    private SixtySecondsRescue() {
    }

    public static boolean isActive() {
        return endTick >= 0;
    }

    /** 激活信标（由物品调用；调用方已校验相位/位置）。 */
    public static void activate(ServerLevel level, ServerPlayer player) {
        endTick = level.getGameTime() + SixtySecondsBalance.RESCUE_COUNTDOWN_TICKS;
        teamId = SixtySecondsStatsComponent.KEY.get(player).teamId;
        rescuePlayer = player.getUUID();
        int seconds = SixtySecondsBalance.RESCUE_COUNTDOWN_TICKS / 20;
        broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.rescue_activated",
                player.getGameProfile().getName(), seconds).withStyle(ChatFormatting.GOLD));
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** 游戏日每 tick 推进：队伍全灭→取消；被标记者进入撤离点→直接撤离；到点→生成直升机+燃料（不再判胜利）。 */
    public static void tick(ServerLevel level, SixtySecondsState.Data data) {
        if (endTick < 0 || data.phase != SixtySecondsPhase.DAY) {
            return;
        }
        long now = level.getGameTime();
        if (now % 20 != 0) {
            return;
        }
        // 激活队伍全灭 → 救援取消
        if (!teamAlive(level, data)) {
            broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.rescue_cancelled")
                    .withStyle(ChatFormatting.RED));
            reset();
            return;
        }
        ServerPlayer rescuer = rescuePlayer != null ? (ServerPlayer) level.getPlayerByUUID(rescuePlayer) : null;
        boolean rescuerValid = rescuer != null
                && !rescuer.isSpectator()
                && GameUtils.isPlayerAliveAndSurvival(rescuer)
                && !SixtySecondsStatsComponent.KEY.get(rescuer).monster;
        // 被标记的玩家进入撤离点建筑 → 与撤离点一致地直接撤离（切旁观并计入直升机撤离名单）
        if (rescuerValid
                && SixtySecondsLostCitiesStarMap.isEvacuationPoint(level, rescuer.blockPosition())) {
            data.helicopterEvacuated.add(rescuer.getUUID());
            rescuer.setGameMode(GameType.SPECTATOR);
            broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.rescue_evacuated_you",
                    rescuer.getGameProfile().getName()).withStyle(ChatFormatting.GREEN));
            reset();
            return;
        }
        long remaining = endTick - now;
        if (remaining <= 0) {
            // 倒计时归零：在玩家身旁生成救援直升机并附赠 2 桶航空煤油（不再直接判胜利）。
            if (rescuerValid) {
                spawnHelicopter(level, rescuer);
                giveAviationKerosene(rescuer, 2);
                broadcast(level, Component.translatable("message.sixty_seconds.sixty_seconds.rescue_helicopter_arrived",
                        rescuer.getGameProfile().getName()).withStyle(ChatFormatting.GOLD));
            }
            reset();
            return;
        }
        // 最后 10 秒每秒报数，其余每 30 秒提示
        int seconds = (int) (remaining / 20);
        if (seconds <= 10 || seconds % 30 == 0) {
            for (ServerPlayer player : level.players()) {
                player.displayClientMessage(Component.translatable(
                        "message.sixty_seconds.sixty_seconds.rescue_countdown", seconds)
                        .withStyle(seconds <= 10 ? ChatFormatting.RED : ChatFormatting.YELLOW), true);
            }
        }
    }

    private static boolean teamAlive(ServerLevel level, SixtySecondsState.Data data) {
        SixtySecondsState.TeamData team = data.teams.get(teamId);
        if (team == null) {
            return false;
        }
        for (UUID uuid : team.members) {
            if (level.getPlayerByUUID(uuid) instanceof ServerPlayer member
                    && GameUtils.isPlayerAliveAndSurvival(member)
                    && !SixtySecondsStatsComponent.KEY.get(member).monster) {
                return true;
            }
        }
        return false;
    }

    public static void reset() {
        endTick = -1;
        teamId = -1;
        rescuePlayer = null;
    }

    /** 在玩家身旁生成一架空载救援直升机（受重力自然降落）。 */
    private static void spawnHelicopter(ServerLevel level, ServerPlayer player) {
        SixtySecondsFlyingVehicleEntity heli = ModEntities.SIXTY_SECONDS_HELICOPTER.create(level);
        if (heli == null) {
            return;
        }
        BlockPos p = player.blockPosition();
        heli.setPos(p.getX() + 2.0, p.getY() + 1.0, p.getZ() + 2.0);
        level.addFreshEntity(heli);
    }

    /** 向玩家背包塞入 count 桶航空煤油，背包满则掉落在脚下。 */
    private static void giveAviationKerosene(ServerPlayer player, int count) {
        ItemStack stack = new ItemStack(ModItems.SIXTY_SECONDS_AVIATION_KEROSENE, count);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    private static void broadcast(ServerLevel level, Component message) {
        for (ServerPlayer player : level.players()) {
            player.displayClientMessage(message, false);
        }
    }
}
