package net.exmo.sixty_seconds.island;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;

/**
 * 海洋维度<b>海床（水下）</b>的 20 种地形。改写自原本平铺沙地的 {@code writeSeafloor}：
 * 按 128 格粗粒度网格确定性地选取地形类型（20 种在地图各处散布出现），为每列生成起伏的
 * 海床高度、对应色板，以及海草/珊瑚/热泉等水下装饰。所有地形都保持在水面以下，不会形成新岛屿。
 *
 * <p>装饰与高度均用确定性噪声（复用 {@link SixtySecondsIslandGenerator#fbm}），保证同一世界重进一致，
 * 也便于日后海图重采样绘制海床地貌。
 */
public final class OceanSeabedTerrain {

    private OceanSeabedTerrain() {
    }

    /** 海床基岩与沙层基准（与 OceanChunkGenerator 一致）。 */
    public static final int BEDROCK_Y = 10;
    public static final int SAND_TOP = 12;
    /** 地形选取的粗网格边长（方块）。 */
    private static final int CELL = 128;

    public enum Type {
        ABYSSAL_PLAIN, KELP_FOREST, CORAL_FIELD, TRENCH, RIDGE, SINKHOLE,
        HYDRO_VENT, MUD_FLAT, ROCKY_OUTCROP, ICEFLOOR, PRISMARINE_FIELD,
        GRAVEL_BASIN, SUNKEN_FOREST, CRYSTAL_BED, TUBE_WORM_FIELD, SPONGE_BED,
        WRECK_FIELD, ANCIENT_PAVEMENT, SCULK_ABYSS, LAVA_SEEP
    }

    /** 装饰种类（与 applyColumn 中的 deco 对应）。 */
    private static final int DECO_NONE = 0, DECO_KELP = 1, DECO_CORAL = 2, DECO_VENT = 3,
            DECO_ICE = 4, DECO_PRISMARINE = 5, DECO_GRAVEL = 6, DECO_FOREST = 7,
            DECO_CRYSTAL = 8, DECO_TUBE = 9, DECO_SPONGE = 10, DECO_WRECK = 11,
            DECO_PAVEMENT = 12, DECO_SCULK = 13, DECO_LAVA = 14;

    /** 按粗网格确定性选取该列所属地形。 */
    public static Type pick(long worldSeed, int x, int z) {
        long cellX = Math.floorDiv(x, CELL);
        long cellZ = Math.floorDiv(z, CELL);
        long h = worldSeed ^ (cellX * 0x9E3779B97F4A7C15L) ^ (cellZ * 0xBF58476D1CE4E5B9L);
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        int idx = (int) Long.remainderUnsigned(h, Type.values().length);
        return Type.values()[idx];
    }

    /** 为一整块 chunk 生成海床（含地形起伏与装饰），替代原 writeSeafloor。 */
    public static void applyChunk(BulkSectionAccess bsa, int x0, int z0, int x1, int z1,
            int seaY, long worldSeed) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                applyColumn(bsa, worldSeed, x, z, seaY);
            }
        }
    }

    private static void applyColumn(BulkSectionAccess bsa, long worldSeed, int x, int z, int seaY) {
        Type t = pick(worldSeed, x, z);

        int offMin, offMax, deco;
        BlockState top, sub, core;
        switch (t) {
            case ABYSSAL_PLAIN -> { offMin = 0; offMax = 1; deco = DECO_NONE;
                top = Blocks.SAND.defaultBlockState(); sub = Blocks.SAND.defaultBlockState(); core = Blocks.DIRT.defaultBlockState(); }
            case KELP_FOREST -> { offMin = 0; offMax = 2; deco = DECO_KELP;
                top = Blocks.SAND.defaultBlockState(); sub = Blocks.SAND.defaultBlockState(); core = Blocks.DIRT.defaultBlockState(); }
            case CORAL_FIELD -> { offMin = -1; offMax = 1; deco = DECO_CORAL;
                top = Blocks.SAND.defaultBlockState(); sub = Blocks.SANDSTONE.defaultBlockState(); core = Blocks.STONE.defaultBlockState(); }
            case TRENCH -> { offMin = -7; offMax = -3; deco = DECO_NONE;
                top = Blocks.SAND.defaultBlockState(); sub = Blocks.SAND.defaultBlockState(); core = Blocks.STONE.defaultBlockState(); }
            case RIDGE -> { offMin = 4; offMax = 8; deco = DECO_NONE;
                top = Blocks.STONE.defaultBlockState(); sub = Blocks.COBBLESTONE.defaultBlockState(); core = Blocks.STONE.defaultBlockState(); }
            case SINKHOLE -> { offMin = -6; offMax = -2; deco = DECO_NONE;
                top = Blocks.SAND.defaultBlockState(); sub = Blocks.SAND.defaultBlockState(); core = Blocks.DIRT.defaultBlockState(); }
            case HYDRO_VENT -> { offMin = 1; offMax = 3; deco = DECO_VENT;
                top = Blocks.OBSIDIAN.defaultBlockState(); sub = Blocks.BASALT.defaultBlockState(); core = Blocks.BLACKSTONE.defaultBlockState(); }
            case MUD_FLAT -> { offMin = 0; offMax = 1; deco = DECO_NONE;
                top = Blocks.MUD.defaultBlockState(); sub = Blocks.MUD.defaultBlockState(); core = Blocks.CLAY.defaultBlockState(); }
            case ROCKY_OUTCROP -> { offMin = 1; offMax = 4; deco = DECO_NONE;
                top = Blocks.COBBLESTONE.defaultBlockState(); sub = Blocks.MOSSY_COBBLESTONE.defaultBlockState(); core = Blocks.STONE.defaultBlockState(); }
            case ICEFLOOR -> { offMin = 0; offMax = 1; deco = DECO_ICE;
                top = Blocks.PACKED_ICE.defaultBlockState(); sub = Blocks.BLUE_ICE.defaultBlockState(); core = Blocks.PACKED_ICE.defaultBlockState(); }
            case PRISMARINE_FIELD -> { offMin = 0; offMax = 1; deco = DECO_PRISMARINE;
                top = Blocks.PRISMARINE.defaultBlockState(); sub = Blocks.PRISMARINE.defaultBlockState(); core = Blocks.DARK_PRISMARINE.defaultBlockState(); }
            case GRAVEL_BASIN -> { offMin = -1; offMax = 0; deco = DECO_GRAVEL;
                top = Blocks.GRAVEL.defaultBlockState(); sub = Blocks.GRAVEL.defaultBlockState(); core = Blocks.STONE.defaultBlockState(); }
            case SUNKEN_FOREST -> { offMin = 0; offMax = 1; deco = DECO_FOREST;
                top = Blocks.SAND.defaultBlockState(); sub = Blocks.SAND.defaultBlockState(); core = Blocks.DIRT.defaultBlockState(); }
            case CRYSTAL_BED -> { offMin = 0; offMax = 2; deco = DECO_CRYSTAL;
                top = Blocks.AMETHYST_BLOCK.defaultBlockState(); sub = Blocks.AMETHYST_BLOCK.defaultBlockState(); core = Blocks.STONE.defaultBlockState(); }
            case TUBE_WORM_FIELD -> { offMin = 0; offMax = 1; deco = DECO_TUBE;
                top = Blocks.SAND.defaultBlockState(); sub = Blocks.SAND.defaultBlockState(); core = Blocks.DIRT.defaultBlockState(); }
            case SPONGE_BED -> { offMin = 0; offMax = 1; deco = DECO_SPONGE;
                top = Blocks.SAND.defaultBlockState(); sub = Blocks.SAND.defaultBlockState(); core = Blocks.DIRT.defaultBlockState(); }
            case WRECK_FIELD -> { offMin = 0; offMax = 1; deco = DECO_WRECK;
                top = Blocks.SAND.defaultBlockState(); sub = Blocks.SAND.defaultBlockState(); core = Blocks.STONE.defaultBlockState(); }
            case ANCIENT_PAVEMENT -> { offMin = 0; offMax = 1; deco = DECO_PAVEMENT;
                top = Blocks.STONE_BRICKS.defaultBlockState(); sub = Blocks.STONE_BRICKS.defaultBlockState(); core = Blocks.STONE.defaultBlockState(); }
            case SCULK_ABYSS -> { offMin = 0; offMax = 1; deco = DECO_SCULK;
                top = Blocks.SCULK.defaultBlockState(); sub = Blocks.DEEPSLATE.defaultBlockState(); core = Blocks.DEEPSLATE.defaultBlockState(); }
            case LAVA_SEEP -> { offMin = 1; offMax = 3; deco = DECO_LAVA;
                top = Blocks.MAGMA_BLOCK.defaultBlockState(); sub = Blocks.BASALT.defaultBlockState(); core = Blocks.BLACKSTONE.defaultBlockState(); }
            default -> { offMin = 0; offMax = 1; deco = DECO_NONE;
                top = Blocks.SAND.defaultBlockState(); sub = Blocks.SAND.defaultBlockState(); core = Blocks.DIRT.defaultBlockState(); }
        }

        double n = SixtySecondsIslandGenerator.fbm(worldSeed, x * 0.05, z * 0.05, 3);
        int ho = (int) Math.round((offMin + (offMax - offMin) * n));
        int topY = Math.max(BEDROCK_Y + 1, SAND_TOP + ho);
        topY = Math.min(topY, seaY - 1); // 始终在水下

        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState air = Blocks.AIR.defaultBlockState();
        for (int y = 0; y <= seaY; y++) {
            BlockState state;
            if (y < BEDROCK_Y) {
                state = air;
            } else if (y == BEDROCK_Y) {
                state = bedrock;
            } else if (y <= topY) {
                if (y == topY) {
                    state = top;
                } else if (y >= topY - 2) {
                    state = sub;
                } else {
                    state = core;
                }
            } else {
                state = water;
            }
            setPrimer(bsa, x, y, z, state);
        }

        decorateColumn(bsa, deco, worldSeed, x, z, topY, seaY);
    }

    private static void decorateColumn(BulkSectionAccess bsa, int deco, long worldSeed,
            int x, int z, int topY, int seaY) {
        long h = worldSeed ^ (x * 0x85EBCA6BL) ^ (z * 0xC2B2AE35L);
        h ^= (h >>> 33);
        h *= 0x27D4EB2FL;
        h ^= (h >>> 33);
        java.util.Random rng = new java.util.Random(h);

        int waterTop = seaY; // 水体内可装饰高度上限
        switch (deco) {
            case DECO_KELP -> {
                if (rng.nextFloat() < 0.5F) {
                    int hgt = 2 + rng.nextInt(4);
                    for (int y = topY + 1; y <= topY + hgt && y < waterTop; y++) {
                        setPrimer(bsa, x, y, z, Blocks.KELP.defaultBlockState());
                    }
                }
            }
            case DECO_CORAL -> {
                if (rng.nextFloat() < 0.55F) {
                    BlockState c = switch (rng.nextInt(5)) {
                        case 0 -> Blocks.BRAIN_CORAL_BLOCK.defaultBlockState();
                        case 1 -> Blocks.TUBE_CORAL_BLOCK.defaultBlockState();
                        case 2 -> Blocks.HORN_CORAL_BLOCK.defaultBlockState();
                        case 3 -> Blocks.FIRE_CORAL_BLOCK.defaultBlockState();
                        default -> Blocks.BUBBLE_CORAL_BLOCK.defaultBlockState();
                    };
                    setPrimer(bsa, x, topY + 1, z, c);
                    if (rng.nextFloat() < 0.5F) {
                        setPrimer(bsa, x, topY + 2, z, Blocks.SEA_PICKLE.defaultBlockState());
                    }
                }
            }
            case DECO_VENT -> {
                if (rng.nextFloat() < 0.4F) {
                    setPrimer(bsa, x, topY + 1, z, Blocks.MAGMA_BLOCK.defaultBlockState());
                    if (rng.nextFloat() < 0.5F) {
                        setPrimer(bsa, x, topY + 2, z, Blocks.BUBBLE_COLUMN.defaultBlockState());
                    }
                }
            }
            case DECO_PRISMARINE -> {
                if (rng.nextFloat() < 0.4F) {
                    setPrimer(bsa, x, topY + 1, z, Blocks.PRISMARINE.defaultBlockState());
                }
            }
            case DECO_ICE -> {
                if (rng.nextFloat() < 0.35F) {
                    setPrimer(bsa, x, topY + 1, z, Blocks.PACKED_ICE.defaultBlockState());
                }
            }
            case DECO_GRAVEL -> {
                if (rng.nextFloat() < 0.35F) {
                    setPrimer(bsa, x, topY + 1, z, Blocks.GRAVEL.defaultBlockState());
                }
            }
            case DECO_FOREST -> {
                if (rng.nextFloat() < 0.3F) {
                    int hgt = 2 + rng.nextInt(3);
                    for (int y = topY + 1; y <= topY + hgt; y++) {
                        setPrimer(bsa, x, y, z, Blocks.OAK_LOG.defaultBlockState());
                    }
                }
            }
            case DECO_CRYSTAL -> {
                if (rng.nextFloat() < 0.4F) {
                    setPrimer(bsa, x, topY + 1, z,
                            rng.nextBoolean() ? Blocks.SMALL_AMETHYST_BUD.defaultBlockState()
                                    : Blocks.AMETHYST_CLUSTER.defaultBlockState());
                }
            }
            case DECO_TUBE -> {
                if (rng.nextFloat() < 0.4F) {
                    setPrimer(bsa, x, topY + 1, z, Blocks.SEA_PICKLE.defaultBlockState());
                    setPrimer(bsa, x, topY + 2, z, Blocks.SEA_PICKLE.defaultBlockState());
                }
            }
            case DECO_SPONGE -> {
                if (rng.nextFloat() < 0.4F) {
                    setPrimer(bsa, x, topY + 1, z, Blocks.SPONGE.defaultBlockState());
                }
            }
            case DECO_WRECK -> {
                if (rng.nextFloat() < 0.3F) {
                    setPrimer(bsa, x, topY + 1, z, Blocks.IRON_BARS.defaultBlockState());
                }
            }
            case DECO_PAVEMENT -> {
                if (rng.nextFloat() < 0.45F) {
                    setPrimer(bsa, x, topY + 1, z, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
                }
            }
            case DECO_SCULK -> {
                if (rng.nextFloat() < 0.4F) {
                    setPrimer(bsa, x, topY + 1, z, Blocks.SCULK_VEIN.defaultBlockState());
                }
            }
            case DECO_LAVA -> {
                if (rng.nextFloat() < 0.4F) {
                    setPrimer(bsa, x, topY + 1, z, Blocks.MAGMA_BLOCK.defaultBlockState());
                }
            }
            default -> { /* 无装饰 */ }
        }
    }

    private static void setPrimer(BulkSectionAccess bsa, int x, int y, int z, BlockState state) {
        LevelChunkSection section = bsa.getSection(new BlockPos(x, y, z));
        if (section != null) {
            section.setBlockState(x & 15, y & 15, z & 15, state, false);
        }
    }
}
