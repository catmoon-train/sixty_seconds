package net.exmo.sixty_seconds.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.fabric.CommandRegistrationCallback;
import net.exmo.sixty_seconds.loot.SixtySecondsLootFormConfig;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;

import static net.minecraft.commands.Commands.literal;

/**
 * {@code /60s lootform <timed|container>}：切换物资箱搜刮形式。
 * <ul>
 *   <li>{@code timed}：旧版「站在箱前定时读条」形式。</li>
 *   <li>{@code container}：新容器形式（默认）。</li>
 * </ul>
 */
public final class SixtySecondsLootFormCommand {

    private SixtySecondsLootFormCommand() {
    }

    public static void register() {
        LiteralArgumentBuilder<CommandSourceStack> node = literal("60s")
                .then(literal("lootform")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("form", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("timed");
                                    builder.suggest("container");
                                    return builder.buildFuture();
                                })
                                .executes(SixtySecondsLootFormCommand::set)));
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(node));
    }

    private static int set(CommandContext<CommandSourceStack> ctx) {
        String form = StringArgumentType.getString(ctx, "form");
        MinecraftServer server = ctx.getSource().getServer();
        if (!"timed".equals(form) && !"container".equals(form)) {
            ctx.getSource().sendFailure(Component.translatable("command.sixty_seconds.lootform.usage"));
            return 0;
        }
        SixtySecondsLootFormConfig.Data d = SixtySecondsLootFormConfig.get(server);
        d.form = form;
        SixtySecondsLootFormConfig.save(server, d);
        ctx.getSource().sendSuccess(() -> Component.translatable("command.sixty_seconds.lootform.set", form), true);
        return 1;
    }
}
