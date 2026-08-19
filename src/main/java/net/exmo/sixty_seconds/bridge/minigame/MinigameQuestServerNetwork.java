package net.exmo.sixty_seconds.bridge.minigame;

import net.exmo.sixty_seconds.bridge.SREPlayerMinigameTaskComponent;
import net.exmo.sixty_seconds.logic.SixtySecondsMinigameRotation;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class MinigameQuestServerNetwork {
    private MinigameQuestServerNetwork() {
    }

    public static void sendOpenConfig(ServerPlayer player, BlockPos pos, MinigameQuestBlockEntity be) {
        player.displayClientMessage(Component.literal("Minigame: " + be.getMinigameId()), true);
    }

    public static void complete(ServerPlayer player, BlockPos pos) {
        if (!(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (SixtySecondsMinigameRotation.tryReward(level, pos, player)) {
            SREPlayerMinigameTaskComponent.KEY.get(player).addTokens(1);
            player.displayClientMessage(
                    Component.translatable("message.sixty_seconds.sixty_seconds.minigame_complete"), true);
        }
    }
}
