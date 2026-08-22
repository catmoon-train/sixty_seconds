package net.exmo.sixty_seconds.command;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.GameUtils;
import net.exmo.sixty_seconds.bridge.fabric.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import static net.minecraft.commands.Commands.literal;

/**
 * {@code /60s stop} 直接结束当前进行中的本局游戏（清理全局系统 + 删除上一局存档）。
 * 需要 2 级权限（OP）。
 */
public final class SixtySecondsStopCommand {
    private SixtySecondsStopCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("60s")
                        .then(literal("stop")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    ServerLevel level = source.getLevel();
                                    if (!SixtySecondsMod.RUNNING || !SixtySecondsMod.isActive(level)) {
                                        source.sendFailure(Component.translatable(
                                                "message.sixty_seconds.sixty_seconds.stop_no_game")
                                                .withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    GameUtils.stopGame(level);
                                    source.sendSuccess(
                                            () -> Component.translatable(
                                                    "message.sixty_seconds.sixty_seconds.stop_done")
                                                    .withStyle(ChatFormatting.GREEN), true);
                                    return 1;
                                }))
        ));
    }
}
