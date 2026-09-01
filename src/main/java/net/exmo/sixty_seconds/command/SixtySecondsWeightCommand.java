package net.exmo.sixty_seconds.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.fabric.CommandRegistrationCallback;
import net.exmo.sixty_seconds.network.OpenWeightConfigS2CPacket;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfig;
import net.exmo.sixty_seconds.weights.SixtySecondsWeightConfigStore;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import static net.minecraft.commands.Commands.literal;

/**
 * {@code /60s weight} 指令：
 * <ul>
 *   <li>{@code on} / {@code off}：开关负重系统。</li>
 *   <li>{@code reload}：从本地 JSON 重新加载配置。</li>
 *   <li>{@code config}：打开快速配置面板（需要玩家在线）。</li>
 * </ul>
 */
public final class SixtySecondsWeightCommand {

    private SixtySecondsWeightCommand() {
    }

    public static void register() {
        LiteralArgumentBuilder<CommandSourceStack> node = literal("60s")
                .then(literal("weight")
                        .requires(source -> source.hasPermission(2))
                        .then(literal("on").executes(SixtySecondsWeightCommand::enable))
                        .then(literal("off").executes(SixtySecondsWeightCommand::disable))
                        .then(literal("reload").executes(SixtySecondsWeightCommand::reload))
                        .then(literal("config").executes(SixtySecondsWeightCommand::openConfig)));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(node));
    }

    private static int enable(CommandContext<CommandSourceStack> ctx) {
        SixtySecondsWeightConfig cfg = current(ctx);
        cfg.enabled = true;
        SixtySecondsWeightConfigStore.save(ctx.getSource().getServer(), cfg);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.sixty_seconds.weight.cmd_enabled"), true);
        return 1;
    }

    private static int disable(CommandContext<CommandSourceStack> ctx) {
        SixtySecondsWeightConfig cfg = current(ctx);
        cfg.enabled = false;
        SixtySecondsWeightConfigStore.save(ctx.getSource().getServer(), cfg);
        ctx.getSource().sendSuccess(() -> Component.translatable("message.sixty_seconds.weight.cmd_disabled"), true);
        return 1;
    }

    private static int reload(CommandContext<CommandSourceStack> ctx) {
        SixtySecondsWeightConfigStore.load(ctx.getSource().getServer());
        ctx.getSource().sendSuccess(() -> Component.translatable("message.sixty_seconds.weight.cmd_reloaded"), true);
        return 1;
    }

    private static int openConfig(CommandContext<CommandSourceStack> ctx) {
        ServerPlayer player = ctx.getSource().getPlayer();
        if (player == null) {
            ctx.getSource().sendFailure(Component.translatable("message.sixty_seconds.weight.cmd_player_only"));
            return 0;
        }
        SixtySecondsWeightConfig cfg = current(ctx);
        player.connection.send(OpenWeightConfigS2CPacket.of(cfg));
        return 1;
    }

    private static SixtySecondsWeightConfig current(CommandContext<CommandSourceStack> ctx) {
        SixtySecondsWeightConfig cfg = SixtySecondsWeightConfigStore.get(ctx.getSource().getServer());
        // 复制一份避免直接在缓存实例上改动
        SixtySecondsWeightConfig copy = new SixtySecondsWeightConfig();
        copy.enabled = cfg.enabled;
        copy.backpackMultiplier = cfg.backpackMultiplier;
        copy.handMultiplier = cfg.handMultiplier;
        copy.maxLoad = cfg.maxLoad;
        copy.speedPenaltyEnabled = cfg.speedPenaltyEnabled;
        copy.speedPenaltyPerLoad = cfg.speedPenaltyPerLoad;
        copy.defaultWeight = cfg.defaultWeight;
        copy.tagWeights = new java.util.LinkedHashMap<>(cfg.tagWeights);
        copy.itemWeights = new java.util.LinkedHashMap<>(cfg.itemWeights);
        return copy;
    }
}
