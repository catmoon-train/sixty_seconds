package net.exmo.sixty_seconds.bridge.minigame;

import net.exmo.sixty_seconds.bridge.SixtySecPlayerMinigameTaskComponent;
import net.exmo.sixty_seconds.bridge.fabric.ServerPlayNetworking;
import net.exmo.sixty_seconds.logic.SixtySecondsMinigameRotation;
import net.exmo.sixty_seconds.network.MinigameQuestPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * 小游戏任务点方块 — 服务端网络处理
 * 完成通知统一走 {@link MinigameQuestPayload.CompleteGame} → {@link #handleCompleteGame}，
 * 由 {@link SixtySecondsMinigameRotation#tryReward} 做「50s 限时窗口 + 每队每周期一次」门控，
 * 通过才给 {@link SixtySecPlayerMinigameTaskComponent#addTokens} 发放游戏币。
 */
public final class MinigameQuestServerNetwork {
    private MinigameQuestServerNetwork() {
    }

    /** 由 {@link net.exmo.sixty_seconds.network.ModNetwork#handleC2S} 在服务端主线程分发的入口。 */
    public static void handle(CustomPacketPayload payload, ServerPlayNetworking.Context ctx) {
        if (payload instanceof MinigameQuestPayload.SaveConfig p) {
            handleSaveConfig(p, ctx.player());
        } else if (payload instanceof MinigameQuestPayload.CompleteGame p) {
            handleCompleteGame(p, ctx.player());
        }
    }

    private static void handleSaveConfig(MinigameQuestPayload.SaveConfig payload, ServerPlayer player) {
        BlockEntity be = player.level().getBlockEntity(payload.pos());
        if (be instanceof MinigameQuestBlockEntity questBe) {
            questBe.loadConfigFromTag(payload.data());
            questBe.sync();
        }
    }

    /** 小游戏完成 — 统一触发标识。 */
    private static void handleCompleteGame(MinigameQuestPayload.CompleteGame payload, ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        BlockPos pos = payload.pos();
        BlockEntity be = level.getBlockEntity(pos);
        if (!(be instanceof MinigameQuestBlockEntity questBe)) {
            return;
        }

        // 统一完成标识：blockEvent(type=1, data=0)
        level.blockEvent(pos, questBe.getBlockState().getBlock(), 1, 0);
        questBe.setChanged();
        // 完成反馈：在方块处显示庆祝粒子 + 音效
        double cx = pos.getX() + 0.5;
        double cy = pos.getY() + 0.5;
        double cz = pos.getZ() + 0.5;
        level.sendParticles(ParticleTypes.HAPPY_VILLAGER, cx, cy, cz, 24, 0.4, 0.5, 0.4, 0.0);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, cx, cy + 0.3, cz, 18, 0.4, 0.5, 0.4, 0.15);
        level.sendParticles(ParticleTypes.END_ROD, cx, cy + 0.5, cz, 10, 0.3, 0.4, 0.3, 0.05);
        level.playSound(null, pos, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 0.8F, 1.2F);

        // 奖励门控：50s 限时窗口 + 每队每刷新周期一次，超时/重复的提示由 tryReward 内部发出
        if (!SixtySecondsMinigameRotation.tryReward(level, pos, player)) {
            return;
        }
        // 发放游戏币
        SixtySecPlayerMinigameTaskComponent.KEY.get(player).addTokens(1);
        player.displayClientMessage(
                Component.translatable("message.sixty_seconds.sixty_seconds.minigame_complete"), true);
    }

    /** 发送打开配置界面（服务端→客户端，只发纯数据） */
    public static void sendOpenConfig(ServerPlayer player, BlockPos pos, MinigameQuestBlockEntity entity) {
        CompoundTag data = new CompoundTag();
        data.putString("MinigameId", entity.getMinigameId());
        data.putInt("MarkerColor", entity.getMarkerColor());
        data.putBoolean("IsTaskMarker", entity.isTaskMarker());
        ServerPlayNetworking.send(player, new MinigameQuestPayload.OpenConfig(pos, data));
    }

    /** 发送打开小游戏界面（服务端→客户端） */
    public static void sendOpenGame(ServerPlayer player, BlockPos pos, String minigameId) {
        ServerPlayNetworking.send(player, new MinigameQuestPayload.OpenGame(pos, minigameId));
    }
}
