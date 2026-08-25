package net.exmo.sixty_seconds.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.config.SixtySecondsConfig;
import net.exmo.sixty_seconds.config.SixtySecondsConfigStore;
import net.exmo.sixty_seconds.bridge.fabric.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * /60s export_template <residential|shelter> <name>
 *
 * <p>把当前已在配置里登记（{@code /60s_area template}）的住宅/庇护所模板区，连同方块实体数据
 * （箱子内容物、展示框、旗帜等）一起，作为 Minecraft 原生结构模板（标准 {@code .nbt}，含 palette/blocks/nbt）
 * 导出到世界存档的 {@code sixty_seconds_templates/<name>.nbt}。同时把文件名回写到当前地图配置
 * （{@code residentialTemplateFile}/{@code shelterTemplateFile}）。</p>
 *
 * <p>开局建图时 {@code SixtySecondsArena} 会优先按该文件生成（保留箱子内容），不再依赖世界里的手搭模板区。
 * 导出时坐标基准为模板盒 min，故生成时最低层会贴到生成偏移处（住宅/庇护所默认落在 y≈0 附近）。</p>
 */
public final class SixtySecondsExportTemplateCommand {

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("60s_export_template")
                        .requires(source -> source.hasPermission(2))
                        .then(Commands.argument("kind", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("residential");
                                    builder.suggest("shelter");
                                    return builder.buildFuture();
                                })
                                .then(Commands.argument("name", StringArgumentType.word())
                                        .executes(SixtySecondsExportTemplateCommand::run)))));
    }

    private static int run(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        CommandSourceStack src = ctx.getSource();
        ServerLevel level = src.getLevel();
        String kind = StringArgumentType.getString(ctx, "kind");
        String name = StringArgumentType.getString(ctx, "name");

        if (!kind.equals("residential") && !kind.equals("shelter")) {
            src.sendFailure(Component.translatable("commands.60s.export_template.bad_kind"));
            return 0;
        }

        var configOpt = SixtySecondsConfigStore.load(level);
        if (configOpt.isEmpty()) {
            src.sendFailure(Component.translatable("commands.60s.export_template.no_config"));
            return 0;
        }
        SixtySecondsConfig config = configOpt.get();

        var region = kind.equals("residential") ? config.residentialTemplate : config.shelterTemplate;
        if (region == null) {
            src.sendFailure(Component.translatable("commands.60s.export_template.not_registered", kind));
            return 0;
        }

        BlockPos min = region.min.toBlockPos();
        BlockPos max = region.max.toBlockPos();
        BlockPos p1 = new BlockPos(Math.min(min.getX(), max.getX()), Math.min(min.getY(), max.getY()), Math.min(min.getZ(), max.getZ()));
        BlockPos p2 = new BlockPos(Math.max(min.getX(), max.getX()), Math.max(min.getY(), max.getY()), Math.max(min.getZ(), max.getZ()));
        int sizeX = p2.getX() - p1.getX() + 1;
        int sizeY = p2.getY() - p1.getY() + 1;
        int sizeZ = p2.getZ() - p1.getZ() + 1;

        // 构建标准 Minecraft 结构 NBT：palette + blocks（含每个方块的 nbt 方块实体数据）
        CompoundTag root = new CompoundTag();
        root.putInt("DataVersion", net.minecraft.SharedConstants.getProtocolVersion());
        root.put("size", new IntArrayTag(new int[]{sizeX, sizeY, sizeZ}));

        ListTag blocks = new ListTag();
        ListTag palette = new ListTag();
        Map<BlockState, Integer> palIdx = new LinkedHashMap<>();

        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                for (int z = 0; z < sizeZ; z++) {
                    BlockPos world = new BlockPos(p1.getX() + x, p1.getY() + y, p1.getZ() + z);
                    BlockState state = level.getBlockState(world);
                    BlockEntity be = level.getBlockEntity(world);
                    if (state.isAir() && be == null) {
                        continue;
                    }
                    int idx = palIdx.computeIfAbsent(state, k -> {
                        palette.add(NbtUtils.writeBlockState(k));
                        return palette.size() - 1;
                    });
                    CompoundTag entry = new CompoundTag();
                    entry.putIntArray("pos", new int[]{x, y, z});
                    entry.putInt("state", idx);
                    if (be != null) {
                        entry.put("nbt", be.saveWithFullMetadata(level.registryAccess()));
                    }
                    blocks.add(entry);
                }
            }
        }
        root.put("blocks", blocks);
        root.put("palette", palette);

        Path dir = level.getServer().getWorldPath(LevelResource.ROOT).resolve("sixty_seconds_templates");
        try {
            Files.createDirectories(dir);
        } catch (java.io.IOException e) {
            src.sendFailure(Component.translatable("commands.60s.export_template.mkdir_fail", e.getMessage()));
            return 0;
        }

        Path nbtPath = dir.resolve(name + ".nbt");
        try {
            NbtIo.writeCompressed(root, nbtPath);
        } catch (java.io.IOException e) {
            src.sendFailure(Component.translatable("commands.60s.export_template.save_fail", e.getMessage()));
            return 0;
        }

        if (kind.equals("residential")) {
            config.residentialTemplateFile = name;
        } else {
            config.shelterTemplateFile = name;
        }
        SixtySecondsConfigStore.save(level, config);

        src.sendSuccess(() -> Component.translatable("commands.60s.export_template.done",
                kind, name, dir.getFileName() + "/" + name + ".nbt", sizeX, sizeY, sizeZ), false);
        return 1;
    }
}
