package net.exmo.sixty_seconds.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.exmo.sixty_seconds.bridge.fabric.CommandRegistrationCallback;
import net.exmo.sixty_seconds.logic.SixtySecondsEventSystem;
import net.exmo.sixty_seconds.weather.WeatherSync;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /60s weather list                      —— 列出可用天气
 * /60s weather <名称> [分钟]             —— 开启指定天气预览（默认 5 分钟，即使 60 秒游戏未进行也可使用）
 * /60s weather clear                     —— 清除天气预览
 * <p>
 * 机制：下雨型自然事件（污雨/冰雹）触发时自动让世界下雨，再由客户端 Mixin 封掉原版
 * 雨雪渲染并替换为主题粒子；非下雨型事件触发时自动停雨，仅以主题粒子呈现。
 */
public final class SixtySecondsWeatherCommand {
    private SixtySecondsWeatherCommand() {
    }

    private static SixtySecondsEventSystem.EventType resolve(String name) {
        String n = name.trim().toLowerCase(Locale.ROOT);
        for (SixtySecondsEventSystem.EventType t : SixtySecondsEventSystem.EventType.values()) {
            if (t.name().toLowerCase(Locale.ROOT).equals(n)) {
                return t;
            }
        }
        return null;
    }

    private static final SuggestionProvider<CommandSourceStack> SUGGEST = (ctx, builder) ->
            SharedSuggestionProvider.suggest(
                    Arrays.stream(SixtySecondsEventSystem.EventType.values())
                            .map(t -> t.name().toLowerCase(Locale.ROOT)),
                    builder);

    private static int list(CommandContext<CommandSourceStack> ctx) {
        String names = Arrays.stream(SixtySecondsEventSystem.EventType.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        ctx.getSource().sendSuccess(() -> Component.literal("可用天气: " + names), false);
        return 1;
    }

    private static int clear(CommandContext<CommandSourceStack> ctx) {
        ServerLevel level = ctx.getSource().getLevel();
        level.setWeatherParameters(6000, 0, false, false);
        WeatherSync.clear(level);
        ctx.getSource().sendSuccess(() -> Component.literal("已清除天气预览"), true);
        return 1;
    }

    private static int apply(CommandContext<CommandSourceStack> ctx, int minutes) {
        String name = StringArgumentType.getString(ctx, "weather");
        SixtySecondsEventSystem.EventType type = resolve(name);
        if (type == null) {
            ctx.getSource().sendFailure(Component.literal("未知天气: " + name + "（用 /60s weather list 查看）"));
            return 0;
        }
        if (type == SixtySecondsEventSystem.EventType.AIRDROP) {
            ctx.getSource().sendFailure(Component.literal("空投不支持粒子天气预览"));
            return 0;
        }

        ServerLevel level = ctx.getSource().getLevel();
        int duration = Math.max(1, minutes) * 1200; // 1 分钟 = 1200 tick

        // 下雨型自然事件：触发时自动下雨（由 Mixin 封掉原版渲染并替换为主题粒子）；
        // 非下雨型：自动停雨，仅以粒子呈现。
        if (SixtySecondsEventSystem.isRainEventType(type)) {
            level.setWeatherParameters(0, duration, true, false);
        } else {
            level.setWeatherParameters(0, 0, false, false);
        }
        WeatherSync.force(level, type);

        // 到点自动清除粒子标记
        MinecraftServer server = level.getServer();
        if (server != null) {
            server.tell(new TickTask(server.getTickCount() + duration, () -> WeatherSync.clear(level)));
        }

        ctx.getSource().sendSuccess(
                () -> Component.literal("已开启天气预览: " + type.name() + " （" + minutes + " 分钟）"), true);
        return 1;
    }

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("60s")
                        .then(literal("weather")
                                .requires(source -> source.hasPermission(2))
                                .then(literal("list").executes(SixtySecondsWeatherCommand::list))
                                .then(literal("clear").executes(SixtySecondsWeatherCommand::clear))
                                .then(argument("weather", StringArgumentType.string()).suggests(SUGGEST)
                                        .then(argument("minutes", IntegerArgumentType.integer(1, 120))
                                                .executes(c -> apply(c, IntegerArgumentType.getInteger(c, "minutes"))))
                                        .executes(c -> apply(c, 5)))
                        )
        ));
    }
}
