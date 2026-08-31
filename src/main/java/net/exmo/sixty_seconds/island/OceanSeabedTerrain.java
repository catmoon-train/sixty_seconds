package net.exmo.sixty_seconds.island;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.BulkSectionAccess;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.util.Mth;

/**
 * 海洋维度<b>海床（水下）</b>的 20 种地形。改写自原本平铺沙地的 {@code writeSeafloor}：
 * 用连续噪声确定性地选取地形类型并生成起伏的海床高度、对应色板，以及海草/珊瑚/热泉等水下装饰。
 * 所有地形都保持在水面以下，不会形成新岛屿。
 *
 * <p>地貌选择使用低频连续噪声，地形类型沿噪声等值线自然过渡（不再是大块 128 格硬边格子）；
 * 高度在场底偏移（level）之间按相邻地貌平滑插值，并叠加多倍频起伏噪声，形成连续的山脊/海沟过渡。
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

    private static final long TYPE_SEED = 0x9E3779B1L;
    private static final long FINE_SEED = 0x2E2E2EL;

    public enum Type {
        ABYSSAL_PLAIN, KELP_FOREST, CORAL_FIELD, TRENCH, RIDGE, SINKHOLE,
        HYDRO_VENT, MUD_FLAT, ROCKY_OUTCROP, ICEFLOOR, PRISMARINE_FIELD,
        GRAVEL_BASIN, SUNKEN_FOREST, CRYSTAL_BED, TUBE_WORM_FIELD, SPONGE_BED,
        WRECK_FIELD, ANCIENT_PAVEMENT, SCULK_ABYSS, LAVA_SEEP
    }

    /** 装饰种类（与 decorateColumn 中的 deco 对应）。 */
    private static final int DECO_NONE = 0, DECO_KELP = 1, DECO_CORAL = 2, DECO_VENT = 3,
            DECO_ICE = 4, DECO_PRISMARINE = 5, DECO_GRAVEL = 6, DECO_FOREST = 7,
            DECO_CRYSTAL = 8, DECO_TUBE = 9, DECO_SPONGE = 10, DECO_WRECK = 11,
            DECO_PAVEMENT = 12, DECO_SCULK = 13, DECO_LAVA = 14;

    /** 单种地貌的数据：场底中心高度 level、起伏幅度 amp、装饰、顶/次/核方块。 */
    private record Cell(double level, double amp, int deco, BlockState top, BlockState sub, BlockState core) {
    }

    private static Cell cell(Type t) {
        int deco = DECO_NONE;
        double level = 0, amp = 1.0;
        BlockState top = Blocks.SAND.defaultBlockState();
        BlockState sub = Blocks.SAND.defaultBlockState();
        BlockState core = Blocks.DIRT.defaultBlockState();
        switch (t) {
            case ABYSSAL_PLAIN -> { level = 0; amp = 1.0; }
            case KELP_FOREST -> { level = 1; amp = 1.0; deco = DECO_KELP; }
            case CORAL_FIELD -> { level = 0; amp = 1.0; deco = DECO_CORAL;
                sub = Blocks.SANDSTONE.defaultBlockState(); core = Blocks.STONE.defaultBlockState(); }
            case TRENCH -> { level = -5; amp = 2.0; }
            case RIDGE -> { level = 6; amp = 2.0; top = Blocks.STONE.defaultBlockState();
                sub = Blocks.COBBLESTONE.defaultBlockState(); }
            case SINKHOLE -> { level = -4; amp = 2.0; }
            case HYDRO_VENT -> { level = 2; amp = 1.0; deco = DECO_VENT;
                top = Blocks.OBSIDIAN.defaultBlockState(); sub = Blocks.BASALT.defaultBlockState();
                core = Blocks.BLACKSTONE.defaultBlockState(); }
            case MUD_FLAT -> { level = 0; amp = 1.0; top = Blocks.MUD.defaultBlockState();
                sub = Blocks.MUD.defaultBlockState(); core = Blocks.CLAY.defaultBlockState(); }
            case ROCKY_OUTCROP -> { level = 3; amp = 1.5; top = Blocks.COBBLESTONE.defaultBlockState();
                sub = Blocks.MOSSY_COBBLESTONE.defaultBlockState(); }
            case ICEFLOOR -> { level = 0; amp = 1.0; deco = DECO_ICE;
                top = Blocks.PACKED_ICE.defaultBlockState(); sub = Blocks.BLUE_ICE.defaultBlockState(); }
            case PRISMARINE_FIELD -> { level = 0; amp = 1.0; deco = DECO_PRISMARINE;
                top = Blocks.PRISMARINE.defaultBlockState(); core = Blocks.DARK_PRISMARINE.defaultBlockState(); }
            case GRAVEL_BASIN -> { level = -1; amp = 1.0; deco = DECO_GRAVEL;
                top = Blocks.GRAVEL.defaultBlockState(); }
            case SUNKEN_FOREST -> { level = 1; amp = 1.0; deco = DECO_FOREST; }
            case CRYSTAL_BED -> { level = 1; amp = 1.0; deco = DECO_CRYSTAL;
                top = Blocks.AMETHYST_BLOCK.defaultBlockState(); }
            case TUBE_WORM_FIELD -> { level = 1; amp = 1.0; deco = DECO_TUBE; }
            case SPONGE_BED -> { level = 1; amp = 1.0; deco = DECO_SPONGE; }
            case WRECK_FIELD -> { level = 0; amp = 1.0; deco = DECO_WRECK; core = Blocks.STONE.defaultBlockState(); }
            case ANCIENT_PAVEMENT -> { level = 0; amp = 1.0; deco = DECO_PAVEMENT;
                top = Blocks.STONE_BRICKS.defaultBlockState(); core = Blocks.STONE.defaultBlockState(); }
            case SCULK_ABYSS -> { level = 0; amp = 1.0; deco = DECO_SCULK;
                top = Blocks.SCULK.defaultBlockState(); sub = Blocks.DEEPSLATE.defaultBlockState(); }
            case LAVA_SEEP -> { level = 2; amp = 1.0; deco = DECO_LAVA;
                top = Blocks.MAGMA_BLOCK.defaultBlockState(); sub = Blocks.BASALT.defaultBlockState();
                core = Blocks.BLACKSTONE.defaultBlockState(); }
        }
        return new Cell(level, amp, deco, top, sub, core);
    }

    /** 连续地貌选择结果：相邻两地貌 A、B 及其混合权重 w。 */
    private static final class Sel {
        final Type a, b;
        final double w;
        Sel(Type a, Type b, double w) { this.a = a; this.b = b; this.w = w; }
    }

    /** 连续地貌选择：返回相邻两地貌与混合权重，w<0.5 用 A、否则用 B 的方块/装饰。 */
    private static Sel select(long worldSeed, int x, int z) {
        Type[] vals = Type.values();
        int n = vals.length;
        double s = SixtySecondsIslandGenerator.fbm(worldSeed ^ TYPE_SEED, x * 0.013, z * 0.013, 4);
        s = s * 0.5 + 0.5; // 0..1
        double sp = s * (n - 1);
        int i = (int) Math.floor(sp);
        if (i < 0) i = 0;
        if (i > n - 2) i = n - 2;
        double frac = sp - i;
        double w = frac * frac * (3.0 - 2.0 * frac); // smoothstep
        return new Sel(vals[i], vals[i + 1], w);
    }

    /** 供建筑放置查询：返回某列海床顶面 y（不含建筑/装饰）。 */
    public static int topAt(long worldSeed, int x, int z, int seaY) {
        return columnTopY(worldSeed, x, z, seaY);
    }

    /** 公共查询：返回该列主导地貌（用于海图绘制）。 */
    public static Type pick(long worldSeed, int x, int z) {
        Sel sel = select(worldSeed, x, z);
        return sel.w < 0.5 ? sel.a : sel.b;
    }

    public static void applyChunk(BulkSectionAccess bsa, int x0, int z0, int x1, int z1,
            int seaY, long worldSeed) {
        for (int x = x0; x <= x1; x++) {
            for (int z = z0; z <= z1; z++) {
                applyColumn(bsa, worldSeed, x, z, seaY);
            }
        }
    }

    private static int columnTopY(long worldSeed, int x, int z, int seaY) {
        Sel sel = select(worldSeed, x, z);
        Cell ca = cell(sel.a), cb = cell(sel.b);
        double level = Mth.lerp((float) sel.w, ca.level, cb.level);
        double amp = Mth.lerp((float) sel.w, ca.amp, cb.amp);
        double n = SixtySecondsIslandGenerator.fbm(worldSeed, x * 0.05, z * 0.05, 3); // 0..1
        double ripple = SixtySecondsIslandGenerator.fbm(worldSeed ^ FINE_SEED, x * 0.15, z * 0.15, 2) - 0.5;
        int ho = (int) Math.round(level + (n * 2.0 - 1.0) * amp + ripple * 2.0);
        int topY = Math.max(BEDROCK_Y + 1, SAND_TOP + ho);
        topY = Math.min(topY, seaY - 1); // 始终在水下
        return topY;
    }

    private static void applyColumn(BulkSectionAccess bsa, long worldSeed, int x, int z, int seaY) {
        Sel sel = select(worldSeed, x, z);
        Cell dom = cell(sel.w < 0.5 ? sel.a : sel.b);
        int topY = columnTopY(worldSeed, x, z, seaY);

        BlockState bedrock = Blocks.BEDROCK.defaultBlockState();
        BlockState water = Blocks.WATER.defaultBlockState();
        for (int y = 0; y <= seaY; y++) {
            BlockState state;
            if (y < BEDROCK_Y) {
                state = Blocks.AIR.defaultBlockState();
            } else if (y == BEDROCK_Y) {
                state = bedrock;
            } else if (y <= topY) {
                if (y == topY) {
                    state = dom.top;
                } else if (y >= topY - 2) {
                    state = dom.sub;
                } else {
                    state = dom.core;
                }
            } else {
                state = water;
            }
            setPrimer(bsa, x, y, z, state);
        }

        decorateColumn(bsa, dom.deco, worldSeed, x, z, topY, seaY);
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
            case DECO_ICE -> {
                if (rng.nextFloat() < 0.35F) {
                    setPrimer(bsa, x, topY + 1, z, Blocks.PACKED_ICE.defaultBlockState());
                }
            }
            case DECO_PRISMARINE -> {
                if (rng.nextFloat() < 0.4F) {
                    setPrimer(bsa, x, topY + 1, z, Blocks.PRISMARINE.defaultBlockState());
                }
            }
            case DECO_GRAVEL -> {
                if (rng.nextFloat() < 0.35F) {
                    setPrimer(bsa, x, topY + 1, z, Blocks.GRAVEL.defaultBlockState());
                }
            }
            case DECO_FOREST -> {
                if (rng.nextFloat() < 0.4F) {
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
