package net.exmo.sixty_seconds.command;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.fabric.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import static net.minecraft.commands.Commands.literal;

/**
 * {@code /60s save} 立即保存当前整局游戏进度（世界状态 + 每位玩家的背包与状态 + 职业分配）。
 * 需要 2 级权限（OP）。
 */
public final class SixtySecondsSaveCommand {
    private SixtySecondsSaveCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("60s")
                        .then(literal("save")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> {
                                    CommandSourceStack source = context.getSource();
                                    ServerLevel level = source.getLevel();
                                    ServerLevel main = level.getServer().getLevel(net.minecraft.world.level.Level.OVERWORLD);
                                    if (!SixtySecondsMod.RUNNING || main == null || !SixtySecondsMod.isActive(main)) {
                                        source.sendFailure(Component.translatable(
                                                "message.sixty_seconds.sixty_seconds.save_no_game")
                                                .withStyle(ChatFormatting.RED));
                                        return 0;
                                    }
                                    net.exmo.sixty_seconds.logic.SixtySecondsSaveManager.save(level);
                                    source.sendSuccess(
                                            () -> Component.translatable(
                                                    "message.sixty_seconds.sixty_seconds.save_done")
                                                    .withStyle(ChatFormatting.GREEN), false);
                                    return 1;
                                }))
        ));
    }
}
