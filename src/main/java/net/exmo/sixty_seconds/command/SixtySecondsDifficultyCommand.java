package net.exmo.sixty_seconds.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.exmo.sixty_seconds.bridge.fabric.CommandRegistrationCallback;
import net.exmo.sixty_seconds.logic.SixtySecondsDifficulty;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * {@code /60s difficulty get}            —— 查看当前难度及各项实际倍率
 * {@code /60s difficulty set <0..10>}    —— 设置难度（0 = 默认，即当前各项指标）
 */
public final class SixtySecondsDifficultyCommand {
    private SixtySecondsDifficultyCommand() {
    }

    private static int get(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        int difficulty = SixtySecondsDifficulty.get(level);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.sixty_seconds.difficulty.current", difficulty), false);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                        "command.sixty_seconds.difficulty.line_drain",
                        mult(SixtySecondsDifficulty.drainMultiplier(difficulty)))
                .withStyle(ChatFormatting.YELLOW), false);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                        "command.sixty_seconds.difficulty.line_pollution",
                        mult(SixtySecondsDifficulty.pollutionMultiplier(difficulty)))
                .withStyle(ChatFormatting.YELLOW), false);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                        "command.sixty_seconds.difficulty.line_mob",
                        mult(SixtySecondsDifficulty.mobStatMultiplier(difficulty)))
                .withStyle(ChatFormatting.YELLOW), false);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                        "command.sixty_seconds.difficulty.line_sick",
                        (int) Math.round(SixtySecondsDifficulty.sickChanceBonus(difficulty) * 100))
                .withStyle(ChatFormatting.YELLOW), false);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                        "command.sixty_seconds.difficulty.line_weather",
                        (int) Math.round(SixtySecondsDifficulty.clearWeatherChance(difficulty) * 100))
                .withStyle(ChatFormatting.YELLOW), false);
        return 1;
    }

    private static int set(CommandContext<CommandSourceStack> ctx) {
        int value = IntegerArgumentType.getInteger(ctx, "level");
        ServerLevel level = ctx.getSource().getLevel();
        SixtySecondsDifficulty.set(level, value);
        int applied = SixtySecondsDifficulty.get(level);
        ctx.getSource().sendSuccess(() -> Component.translatable(
                "command.sixty_seconds.difficulty.set", applied), true);
        return 1;
    }

    private static String mult(double multiplier) {
        return String.format("×%.2f", multiplier);
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("60s")
                        .then(literal("difficulty")
                                .requires(source -> source.hasPermission(2))
                                .then(literal("get").executes(SixtySecondsDifficultyCommand::get))
                                .then(literal("set")
                                        .then(argument("level",
                                                IntegerArgumentType.integer(SixtySecondsDifficulty.MIN,
                                                        SixtySecondsDifficulty.MAX_LEVEL))
                                                .executes(SixtySecondsDifficultyCommand::set))))));
    }
}
