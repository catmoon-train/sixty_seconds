package net.exmo.sixty_seconds.command;

import net.exmo.sixty_seconds.bridge.fabric.CommandRegistrationCallback;
import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.exmo.sixty_seconds.config.SixtySecondsConfigStore;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import static net.minecraft.commands.Commands.literal;

/**
 * {@code /60s auto} 集中管理本模式的“便利/自动”类开关：
 * <ul>
 *   <li>{@code /60s auto}             —— 查看当前 死亡自动复活 / 新玩家自动入队 两个开关状态</li>
 *   <li>{@code /60s auto revive on|off} —— 死亡后一段时间自动复活的开关</li>
 *   <li>{@code /60s auto join on|off}   —— 新玩家加入自动入队的开关</li>
 * </ul>
 * 两开关默认均为开启。配置即时生效（自动复活/自动入队逻辑每 tick 实时读取配置），需要 2 级权限（OP）。
 */
public final class SixtySecondsAutoCommand {
    private SixtySecondsAutoCommand() {
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("60s")
                        .then(literal("auto")
                                .requires(source -> source.hasPermission(2))
                                .executes(context -> status(context.getSource()))
                                .then(literal("revive")
                                        .then(literal("on").executes(context -> set(context.getSource(), true, true)))
                                        .then(literal("off").executes(context -> set(context.getSource(), true, false))))
                                .then(literal("join")
                                        .then(literal("on").executes(context -> set(context.getSource(), false, true)))
                                        .then(literal("off").executes(context -> set(context.getSource(), false, false)))))));
    }

    /** 查看两个开关的当前状态。 */
    private static int status(CommandSourceStack source) {
        SixtySecondsConfig config = SixtySecondsConfigStore.current(source.getLevel())
                .orElseGet(SixtySecondsConfig::new);
        source.sendSuccess(() -> Component.translatable(config.autoReviveEnabled
                ? "message.sixty_seconds.sixty_seconds.auto_revive_enabled"
                : "message.sixty_seconds.sixty_seconds.auto_revive_disabled")
                .withStyle(ChatFormatting.AQUA), false);
        source.sendSuccess(() -> Component.translatable(config.autoJoinEnabled
                ? "message.sixty_seconds.sixty_seconds.auto_join_enabled"
                : "message.sixty_seconds.sixty_seconds.auto_join_disabled")
                .withStyle(ChatFormatting.AQUA), false);
        return 1;
    }

    /**
     * 修改开关并落盘。
     *
     * @param revive  true=修改自动复活开关；false=修改自动入队开关
     * @param enabled 目标状态
     */
    private static int set(CommandSourceStack source, boolean revive, boolean enabled) {
        ServerLevel level = source.getLevel();
        SixtySecondsConfig config = SixtySecondsConfigStore.current(level).orElseGet(SixtySecondsConfig::new);
        if (revive) {
            config.autoReviveEnabled = enabled;
        } else {
            config.autoJoinEnabled = enabled;
        }
        SixtySecondsConfigStore.save(level, config);
        String key = revive
                ? (enabled ? "message.sixty_seconds.sixty_seconds.auto_revive_enabled"
                           : "message.sixty_seconds.sixty_seconds.auto_revive_disabled")
                : (enabled ? "message.sixty_seconds.sixty_seconds.auto_join_enabled"
                           : "message.sixty_seconds.sixty_seconds.auto_join_disabled");
        source.sendSuccess(() -> Component.translatable(key)
                .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED), true);
        return 1;
    }
}
