package net.exmo.sixty_seconds.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.bridge.fabric.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

/**
 * {@code /60s export_building <name> <x1> <y1> <z1> <x2> <y2> <z2>} 与
 * {@code /60s export_building <name> here <radius>} —— 把你在游戏里手动搭好的建筑（方块区域）
 * 扫描成 LostCities 的 {@code parts} 字符画 JSON 草稿，写到世界目录
 * {@code sixty_seconds_exports/<name>_export.json}，方便后续整理成正式的 LostCities 建筑定义。
 *
 * <p><b>重要说明（为什么只能做"草稿"）：</b>
 * LostCities 的建筑不是结构 NBT，而是由 JSON 定义 + 字符画 parts 在启动时程序化生成的，
 * 它没有"把已生成方块反推成建筑定义"的运行时 API。本命令做的是"建造辅助"：
 * <ol>
 *   <li>按 16×16（LostCities part 固定尺寸）把区域切块，每块生成一份 part 草稿；</li>
 *   <li>扫描区域内出现的每种方块，分配一个占位字符（编号），并附 blockPalette 对照表；</li>
 *   <li>同时给出一份 {@code /60s_area region add ... 0} 命令文本，让你一键把该区域划为 0 级安全区。</li>
 * </ol>
 * 你拿到草稿后，需把占位字符替换成所用 citystyle 的 palette 字母（如 {@code #}=墙、{@code a}=地板），
 * 并把 part 文件放进数据包 {@code data/lostcities/lostcities/parts/}，再写 {@code buildings/<name>.json}
 * 接入城市建筑池——这一步属于"路线 A"，本命令不自动完成。
 *
 * <p>空气用空格 {@code ' '} 表示（与 LostCities 一致）。区域超过 16×16 会切成多块，每块独立命名。
 */
public final class SixtySecondsExportBuildingCommand {
    private SixtySecondsExportBuildingCommand() {
    }

    /** LostCities part 固定为 16×16。 */
    private static final int CHUNK = 16;

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
                literal("60s").then(literal("export_building")
                        .requires(source -> source.hasPermission(2))
                        .then(argument("name", StringArgumentType.word())
                                .then(argument("x1", IntegerArgumentType.integer())
                                        .then(argument("y1", IntegerArgumentType.integer())
                                                .then(argument("z1", IntegerArgumentType.integer())
                                                        .then(argument("x2", IntegerArgumentType.integer())
                                                                .then(argument("y2", IntegerArgumentType.integer())
                                                                        .then(argument("z2", IntegerArgumentType.integer())
                                                                                .executes(ctx -> exportAbsolute(ctx))
                                                                        )
                                                                )
                                                        )
                                                )
                                        )
                                )
                                .then(literal("here")
                                        .then(argument("radius", IntegerArgumentType.integer(1, 64))
                                                .executes(ctx -> exportHere(ctx))
                                        )
                                )
                        )
                )
        ));
    }

    private static int exportAbsolute(CommandContext<CommandSourceStack> ctx) {
        return export(ctx,
                arg(ctx, "x1"), arg(ctx, "y1"), arg(ctx, "z1"),
                arg(ctx, "x2"), arg(ctx, "y2"), arg(ctx, "z2"));
    }

    private static int exportHere(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack src = ctx.getSource();
        BlockPos p = BlockPos.containing(src.getPosition());
        int r = IntegerArgumentType.getInteger(ctx, "radius");
        return export(ctx,
                p.getX() - r, p.getY() - r, p.getZ() - r,
                p.getX() + r, p.getY() + r, p.getZ() + r);
    }

    private static int arg(CommandContext<CommandSourceStack> ctx, String n) {
        return IntegerArgumentType.getInteger(ctx, n);
    }

    private static int export(CommandContext<CommandSourceStack> ctx,
                              int x1, int y1, int z1, int x2, int y2, int z2) {
        CommandSourceStack source = ctx.getSource();
        String name = StringArgumentType.getString(ctx, "name");
        ServerLevel level = source.getLevel();

        // 归一化对角坐标。
        int minX = Math.min(x1, x2), maxX = Math.max(x1, x2);
        int minY = Math.min(y1, y2), maxY = Math.max(y1, y2);
        int minZ = Math.min(z1, z2), maxZ = Math.max(z1, z2);
        int sizeX = maxX - minX + 1, sizeY = maxY - minY + 1, sizeZ = maxZ - minZ + 1;

        // 方块种类 → 单字符编码（无实际上限）。前 62 种用 0-9A-Za-z，
        // 超出后用 Unicode 私有区(U+E000 起，6400+ 字符)继续分配，每格仍只占 1 字符。
        // 这样 slices 直接是 LostCities 的字符画格式，无需后续数字→字符转换。
        Map<ResourceLocation, String> palette = new LinkedHashMap<>();
        final String base62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        int baseIdx = 0;
        int privIdx = 0; // 私有区偏移，从 0xE000 起

        // 结果结构。
        JsonObject root = new JsonObject();
        root.addProperty("note",
                "LostCities 建筑导出（已转换为标准 part 字符画格式，无方块种类上限）。"
                        + "slices[y][z] 为第 y 层(从下往上)第 z 行(x 方向)的字符画，每格 1 字符，空格=空气。"
                        + "超出 62 种方块的编码使用 Unicode 私有区字符(U+E000 起)，需在所用 citystyle 的 palette 中"
                        + "登记为对应方块后才能作为正式 part 加载，详见 blockPalette。");
        root.addProperty("name", name);
        root.addProperty("chunk", CHUNK);
        root.addProperty("sourceBox", minX + "," + minY + "," + minZ + " -> " + maxX + "," + maxY + "," + maxZ);
        JsonArray parts = new JsonArray();

        // 按 16×16 水平块切分。
        for (int bx = minX; bx <= maxX; bx += CHUNK) {
            for (int bz = minZ; bz <= maxZ; bz += CHUNK) {
                int lx = Math.min(CHUNK, maxX - bx + 1);
                int lz = Math.min(CHUNK, maxZ - bz + 1);
                JsonObject part = new JsonObject();
                String partName = name + "_" + (bx - minX) / CHUNK + "_" + (bz - minZ) / CHUNK;
                part.addProperty("part", partName);
                part.addProperty("originWorldX", bx);
                part.addProperty("originWorldZ", bz);
                part.addProperty("xsize", lx);
                part.addProperty("zsize", lz);

                // 标准 part 字符画：slices[y] = [z行字符串...]，每格 1 字符。
                JsonArray slices = new JsonArray();
                for (int y = minY; y <= maxY; y++) {
                    JsonArray row = new JsonArray();
                    for (int z = bz; z < bz + lz; z++) {
                        StringBuilder line = new StringBuilder();
                        for (int x = bx; x < bx + lx; x++) {
                            BlockState st = level.getBlockState(new BlockPos(x, y, z));
                            if (st.isAir() || st.getBlock() == Blocks.AIR) {
                                line.append(' ');
                                continue;
                            }
                            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(st.getBlock());
                            String ch = palette.get(id);
                            if (ch == null) {
                                if (baseIdx < base62.length()) {
                                    ch = String.valueOf(base62.charAt(baseIdx++));
                                } else {
                                    ch = new String(new int[]{0xE000 + privIdx++}, 0, 1);
                                }
                                palette.put(id, ch);
                            }
                            line.append(ch);
                        }
                        row.add(line.toString());
                    }
                    slices.add(row);
                }
                part.add("slices", slices);
                parts.add(part);
            }
        }

        root.add("parts", parts);

        // blockPalette 对照表（字符 → 方块 ID）。
        JsonObject blockPalette = new JsonObject();
        for (Map.Entry<ResourceLocation, String> e : palette.entrySet()) {
            blockPalette.addProperty(e.getValue(), e.getKey().toString());
        }
        root.add("blockPalette", blockPalette);

        // 0 级安全区命令（用原始包围盒，level=0）。
        String regionCmd = "/60s_area region add "
                + minX + " " + minY + " " + minZ + " "
                + maxX + " " + maxY + " " + maxZ + " 0 "
                + name + "_safe";
        root.addProperty("suggestedSafeZoneCommand", regionCmd);

        // 写入世界目录 sixty_seconds_exports/。
        Path dir = level.getServer().getWorldPath(LevelResource.ROOT).resolve("sixty_seconds_exports");
        try {
            Files.createDirectories(dir);
            Path out = dir.resolve(name + "_export.json");
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            Files.writeString(out, gson.toJson(root), StandardCharsets.UTF_8);

            source.sendSuccess(() -> Component.translatable("commands.60s.export_building.done",
                    parts.size(), sizeX, sizeY, sizeZ, palette.size())
                    .withStyle(ChatFormatting.GREEN), false);
            source.sendSuccess(() -> Component.translatable("commands.60s.export_building.file", out)
                    .withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.translatable("commands.60s.export_building.safe_cmd")
                    .withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(() -> Component.literal(regionCmd).withStyle(ChatFormatting.AQUA), false);
            SixtySeconds.LOGGER.info("[60s] export_building '{}' -> {} (parts={}, blocks={})",
                    name, out, parts.size(), palette.size());
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.translatable("commands.60s.export_building.fail", e.getMessage())
                    .withStyle(ChatFormatting.RED));
            SixtySeconds.LOGGER.warn("[60s] export_building 写文件失败", e);
            return 0;
        }
    }
}
