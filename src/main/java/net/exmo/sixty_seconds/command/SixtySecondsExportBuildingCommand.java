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

        // 方块种类 → 递增数字索引（无上限）。空气固定为 -1。
        Map<ResourceLocation, Integer> palette = new LinkedHashMap<>();
        int[] counter = {0};

        // 结果结构。
        JsonObject root = new JsonObject();
        root.addProperty("note",
                "LostCities 建筑草稿（无方块种类上限）。slices[y][z][x] 为数字索引(-1=空气)，"
                        + "对照 blockPalette 还原方块。prettySlices[y][z] 为空格分隔的数字预览("
                        + ".=空气)便于肉眼查看。索引需替换为所用 citystyle 的 palette 字母后才能作为正式 part。");
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

                // 数字索引 slices（无上限）：slices[y] = [z行数字数组...]。
                JsonArray slices = new JsonArray();
                // 空格分隔的预览字符串 prettySlices：pretty[y] = [z行字符串...]。
                JsonArray pretty = new JsonArray();
                for (int y = minY; y <= maxY; y++) {
                    JsonArray slicesRow = new JsonArray();   // 本层 y 的数字行集合
                    JsonArray prettyRow = new JsonArray();   // 本层 y 的预览行集合
                    for (int z = bz; z < bz + lz; z++) {
                        JsonArray cellNums = new JsonArray(); // 本 z 行的数字（x 方向）
                        StringBuilder prettyCell = new StringBuilder();
                        for (int x = bx; x < bx + lx; x++) {
                            BlockState st = level.getBlockState(new BlockPos(x, y, z));
                            int idx;
                            if (st.isAir() || st.getBlock() == Blocks.AIR) {
                                idx = -1;
                            } else {
                                ResourceLocation id = BuiltInRegistries.BLOCK.getKey(st.getBlock());
                                Integer known = palette.get(id);
                                if (known == null) {
                                    known = counter[0]++;
                                    palette.put(id, known);
                                }
                                idx = known;
                            }
                            cellNums.add(idx);
                            if (prettyCell.length() > 0) {
                                prettyCell.append(' ');
                            }
                            prettyCell.append(idx);
                        }
                        slicesRow.add(cellNums);
                        prettyRow.add(prettyCell.toString());
                    }
                    slices.add(slicesRow);
                    pretty.add(prettyRow);
                }
                part.add("slices", slices);
                part.add("prettySlices", pretty);
                parts.add(part);
            }
        }

        root.add("parts", parts);

        // blockPalette 对照表（索引 → 方块 ID）。
        JsonObject blockPalette = new JsonObject();
        for (Map.Entry<ResourceLocation, Integer> e : palette.entrySet()) {
            blockPalette.addProperty(String.valueOf(e.getValue()), e.getKey().toString());
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

            source.sendSuccess(() -> Component.literal(
                    "[export_building] 已导出 " + parts.size() + " 个 part 草稿（区域 "
                            + sizeX + "×" + sizeY + "×" + sizeZ + "，方块种类 " + palette.size() + "）")
                    .withStyle(ChatFormatting.GREEN), false);
            source.sendSuccess(() -> Component.literal("文件： " + out).withStyle(ChatFormatting.GRAY), false);
            source.sendSuccess(() -> Component.literal("划 0 级安全区命令（复制执行）：").withStyle(ChatFormatting.YELLOW), false);
            source.sendSuccess(() -> Component.literal(regionCmd).withStyle(ChatFormatting.AQUA), false);
            SixtySeconds.LOGGER.info("[60s] export_building '{}' -> {} (parts={}, blocks={})",
                    name, out, parts.size(), palette.size());
            return 1;
        } catch (Exception e) {
            source.sendFailure(Component.literal("[export_building] 写文件失败： " + e.getMessage())
                    .withStyle(ChatFormatting.RED));
            SixtySeconds.LOGGER.warn("[60s] export_building 写文件失败", e);
            return 0;
        }
    }
}
