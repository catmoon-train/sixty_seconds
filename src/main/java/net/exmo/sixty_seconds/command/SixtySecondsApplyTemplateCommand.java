package net.exmo.sixty_seconds.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.exmo.sixty_seconds.config.SixtySecondsConfigStore;
import net.exmo.sixty_seconds.bridge.fabric.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * /60s apply_template <residential> <shelter>
 * /60s apply_template list
 *
 * <p>把指定的初始房子 / 避难所模板（不含后缀，对应 {@code sixty_seconds_templates/<name>.nbt}）写入当前地图配置，
 * 下一次开局建图时即使用它们。{@code house1} / {@code shelter1} 为默认模板。便于以后随时切换不同种类的房子/避难所。</p>
 *
 * <p>用法示例：
 * <ul>
 *   <li>{@code /60s apply_template house1 shelter1} — 恢复默认</li>
 *   <li>{@code /60s apply_template cabin2 bunker3} — 改用其它模板</li>
 *   <li>{@code /60s apply_template list} — 列出已导出的模板文件</li>
 * </ul>
 */
public final class SixtySecondsApplyTemplateCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            var root = Commands.literal("60s");
            root.requires(src -> src.hasPermission(2))
                .then(literal("apply_template")
                    // /60s apply_template list
                    .then(literal("list")
                        .executes(SixtySecondsApplyTemplateCommand::list))
                    // /60s apply_template <residential> <shelter>
                    .then(argument("residential", StringArgumentType.word())
                        .then(argument("shelter", StringArgumentType.word())
                            .executes(SixtySecondsApplyTemplateCommand::apply))));
            dispatcher.register(root);
        });
    }

    private static Path templatesDir(ServerLevel level) {
        return level.getServer().getWorldPath(LevelResource.ROOT).resolve("sixty_seconds_templates");
    }

    private static int apply(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        String residential = StringArgumentType.getString(ctx, "residential");
        String shelter = StringArgumentType.getString(ctx, "shelter");

        Optional<SixtySecondsConfig> configOpt = SixtySecondsConfigStore.load(level);
        if (configOpt.isEmpty()) {
            src.sendFailure(Component.translatable("commands.60s.apply_template.no_config"));
            return 0;
        }

        // 校验模板文件是否存在（导出过的才允许应用，避免开局静默回退到世界克隆）
        Path dir = templatesDir(level);
        List<String> missing = new ArrayList<>();
        if (!Files.exists(dir.resolve(residential + ".nbt"))) missing.add(residential);
        if (!Files.exists(dir.resolve(shelter + ".nbt"))) missing.add(shelter);
        if (!missing.isEmpty()) {
            src.sendFailure(Component.translatable("commands.60s.apply_template.missing",
                    String.join(", ", missing)));
            return 0;
        }

        SixtySecondsConfig config = configOpt.get();
        config.residentialTemplateFile = residential;
        config.shelterTemplateFile = shelter;
        SixtySecondsConfigStore.save(level, config);

        src.sendSuccess(() -> Component.translatable("commands.60s.apply_template.done", residential, shelter)
                .withStyle(ChatFormatting.GREEN), true);
        return 1;
    }

    private static int list(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        Path dir = templatesDir(level);

        List<String> names = new ArrayList<>();
        if (Files.exists(dir)) {
            try (var stream = Files.list(dir)) {
                stream.filter(p -> p.toString().endsWith(".nbt"))
                        .map(p -> p.getFileName().toString().replaceAll("\\.nbt$", ""))
                        .sorted()
                        .forEach(names::add);
            } catch (IOException ignored) {
            }
        }

        Optional<SixtySecondsConfig> configOpt = SixtySecondsConfigStore.load(level);
        String curHouse = configOpt.map(c -> c.residentialTemplateFile).orElse(null);
        String curShelter = configOpt.map(c -> c.shelterTemplateFile).orElse(null);

        if (names.isEmpty()) {
            src.sendSuccess(() -> Component.translatable("commands.60s.apply_template.list_empty"), false);
            return 1;
        }

        StringBuilder sb = new StringBuilder();
        sb.append(Component.translatable("commands.60s.apply_template.list_header").getString());
        if (curHouse != null || curShelter != null) {
            sb.append("\n").append(Component.translatable("commands.60s.apply_template.list_current",
                    curHouse == null ? "-" : curHouse, curShelter == null ? "-" : curShelter).getString());
        }
        for (String n : names) {
            List<String> tags = new ArrayList<>();
            if (n.equals(curHouse)) tags.add("house");
            if (n.equals(curShelter)) tags.add("shelter");
            String tag = tags.isEmpty() ? "" : "  §7[" + String.join("/", tags) + "]";
            sb.append("\n - ").append(n).append(tag);
        }
        src.sendSuccess(() -> Component.literal(sb.toString()), false);
        return 1;
    }
}
