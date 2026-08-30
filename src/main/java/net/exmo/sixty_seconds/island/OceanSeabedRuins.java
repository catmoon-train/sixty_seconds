package net.exmo.sixty_seconds.island;

import net.exmo.sixty_seconds.island.SixtySecondsIslandGenerator.Placer;
import net.exmo.sixty_seconds.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

/**
 * 海洋维度<b>海床上的废墟城市</b>：20 种独特建筑模板。与海平面以上的 {@link SixtySecondsRuins}
 * （岛屿废墟）区分——这里所有建筑都坐落于海床（水下），且每栋放置 2~4 个物资箱，数量明显多于海岛（1 个），
 * 符合"物资箱比海岛多"的要求。
 *
 * <p>建筑以确定性网格散布于海床（每约 180 格一座，约 37% 留空形成间隔），每座由世界种子确定性决定类型与朝向抖动，
 * 保证同一世界稳定重现。
 */
public final class OceanSeabedRuins {

    private OceanSeabedRuins() {
    }

    public static final int TEMPLATE_COUNT = 20;
    /** 城市建筑网格间距（方块）。 */
    public static final int SPACING = 180;

    /** 建筑落位信息（由 {@link #plan} 确定性生成）。 */
    public static final class Placement {
        public final int type;
        public final int centerX, centerZ;
        public final long seed;
        public Placement(int type, int centerX, int centerZ, long seed) {
            this.type = type;
            this.centerX = centerX;
            this.centerZ = centerZ;
            this.seed = seed;
        }
    }

    private static final String[] CATS = {"food", "water", "medicine", "tool", "material", "weapon"};
    private static final BlockState SAND = Blocks.SAND.defaultBlockState();
    private static final BlockState STONE = Blocks.STONE.defaultBlockState();
    private static final BlockState STONE_BRICK = Blocks.STONE_BRICKS.defaultBlockState();
    private static final BlockState MOSS = Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
    private static final BlockState PRISM = Blocks.PRISMARINE.defaultBlockState();
    private static final BlockState DPRISM = Blocks.DARK_PRISMARINE.defaultBlockState();
    private static final BlockState IRON = Blocks.IRON_BARS.defaultBlockState();
    private static final BlockState GLASS = Blocks.GLASS.defaultBlockState();

    /** 按网格确定性规划一座建筑；返回 null 表示该格留空。 */
    public static Placement plan(int gx, int gz, long worldSeed) {
        long h = worldSeed ^ (gx * 0x9E3779B97F4A7C15L) ^ (gz * 0xBF58476D1CE4E5B9L);
        h ^= (h >>> 33);
        h *= 0xFF51AFD7ED558CCDL;
        h ^= (h >>> 33);
        if ((h & 7) < 3) return null; // 约 37% 留空
        int type = (int) Long.remainderUnsigned(h >>> 3, TEMPLATE_COUNT);
        int jxoff = (int) Long.remainderUnsigned(h >>> 8, 80) - 40;
        int jzoff = (int) Long.remainderUnsigned(h >>> 16, 80) - 40;
        int cx = gx * SPACING + 90 + jxoff;
        int cz = gz * SPACING + 90 + jzoff;
        long seed = h ^ 0xC17AB1EL;
        return new Placement(type, cx, cz, seed);
    }

    /** 放置单座建筑（仅写入与 chunk 交叠的方块，由 Placer 负责裁剪）。 */
    public static void placeAll(Placer p, Placement b, int seaY) {
        RandomSource rng = RandomSource.create(b.seed);
        // 海床基准：沙层顶在 y=12，建筑从 y=13（其上方空气）起建
        BlockPos o = new BlockPos(b.centerX, 13, b.centerZ);
        build(p, b.type, o, rng, seaY);
    }

    // ----------------------------------------------------------------- 建筑模板

    private static void build(Placer p, int type, BlockPos o, RandomSource rng, int seaY) {
        switch (type) {
            case 0 -> domedHabitat(p, o, rng);
            case 1 -> pillarHall(p, o, rng);
            case 2 -> brokenDome(p, o, rng);
            case 3 -> sunkenTower(p, o, rng);
            case 4 -> archGate(p, o, rng);
            case 5 -> amphitheater(p, o, rng);
            case 6 -> warehouseBlock(p, o, rng);
            case 7 -> statuePlaza(p, o, rng);
            case 8 -> streetBlock(p, o, rng);
            case 9 -> fountainSquare(p, o, rng);
            case 10 -> barracks(p, o, rng);
            case 11 -> templeRuin(p, o, rng);
            case 12 -> lighthouseBase(p, o, rng);
            case 13 -> pipelineJunction(p, o, rng);
            case 14 -> cryoVault(p, o, rng);
            case 15 -> observatory(p, o, rng);
            case 16 -> marketStalls(p, o, rng);
            case 17 -> bridgeSpan(p, o, rng);
            case 18 -> cistern(p, o, rng);
            case 19 -> cathedral(p, o, rng);
            default -> warehouseBlock(p, o, rng);
        }
    }

    private static void domedHabitat(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 5, 5);
        walls(p, o, 5, 5, 0, 4, PRISM);
        // 圆顶：用阶梯状缩进模拟
        for (int i = 1; i <= 4; i++) {
            int r = 5 - i;
            walls(p, o, r, r, 4 + i, 4 + i, DPRISM);
        }
        set(p, o, 0, 6, 0, Blocks.SEA_LANTERN.defaultBlockState());
        scatterBoxes(p, o, rng, 3, 2);
    }

    private static void pillarHall(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 7, 4);
        walls(p, o, 7, 4, 0, 5, STONE_BRICK);
        for (int dx = -6; dx <= 6; dx += 3) {
            col(p, o, dx, -4, 0, 6, Blocks.QUARTZ_PILLAR.defaultBlockState());
            col(p, o, dx, 4, 0, 6, Blocks.QUARTZ_PILLAR.defaultBlockState());
        }
        set(p, o, 0, 6, 0, Blocks.SEA_LANTERN.defaultBlockState());
        scatterBoxes(p, o, rng, 3, 3);
    }

    private static void brokenDome(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 6, 6);
        walls(p, o, 6, 6, 0, 3, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
        // 破损顶：只部分封顶
        walls(p, o, 6, 6, 4, 4, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
        set(p, o, -3, 5, -3, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
        set(p, o, 4, 5, 2, Blocks.CRACKED_STONE_BRICKS.defaultBlockState());
        scatterBoxes(p, o, rng, 3, 2);
    }

    private static void sunkenTower(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 3, 3);
        walls(p, o, 3, 3, 0, 14, STONE_BRICK);
        // 顶部倒塌缺口
        set(p, o, 0, 14, 0, Blocks.AIR.defaultBlockState());
        set(p, o, 2, 13, 2, Blocks.AIR.defaultBlockState());
        set(p, o, 0, 15, 0, Blocks.SEA_LANTERN.defaultBlockState());
        scatterBoxes(p, o, rng, 2, 1);
    }

    private static void archGate(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 7, 3);
        col(p, o, -6, 0, 0, 8, STONE_BRICK);
        col(p, o, 6, 0, 0, 8, STONE_BRICK);
        for (int y = 6; y <= 8; y++) for (int dx = -6; dx <= 6; dx++) set(p, o, dx, y, 0, STONE_BRICK);
        set(p, o, 0, 9, 0, Blocks.SEA_LANTERN.defaultBlockState());
        scatterBoxes(p, o, rng, 2, 2);
    }

    private static void amphitheater(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 8, 8);
        for (int tier = 0; tier < 4; tier++) {
            int r = 8 - tier;
            walls(p, o, r, r, tier, tier, MOSS);
        }
        set(p, o, 0, 0, 0, Blocks.SEA_LANTERN.defaultBlockState());
        scatterBoxes(p, o, rng, 3, 3);
    }

    private static void warehouseBlock(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 6, 5);
        box(p, o, -6, -5, 6, 5, 0, 5, Blocks.POLISHED_ANDESITE.defaultBlockState());
        // 货架隔断
        for (int dx = -4; dx <= 4; dx += 4) col(p, o, dx, 0, 0, 5, Blocks.OAK_PLANKS.defaultBlockState());
        set(p, o, 0, 6, 0, Blocks.SEA_LANTERN.defaultBlockState());
        scatterBoxes(p, o, rng, 3, 4); // 仓库物资最丰富
    }

    private static void statuePlaza(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 7, 7);
        col(p, o, 0, 0, 0, 7, Blocks.QUARTZ_BLOCK.defaultBlockState());
        set(p, o, 0, 8, 0, Blocks.SMOOTH_QUARTZ.defaultBlockState());
        scatterBoxes(p, o, rng, 3, 2);
    }

    private static void streetBlock(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 8, 8);
        for (int dx = -8; dx <= 8; dx += 4) walls(p, o, 1, 8, 0, 4, STONE_BRICK);
        for (int dz = -8; dz <= 8; dz += 4) walls(p, o, 8, 1, 0, 4, MOSS);
        scatterBoxes(p, o, rng, 4, 3);
    }

    private static void fountainSquare(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 6, 6);
        walls(p, o, 6, 6, 0, 1, STONE_BRICK);
        col(p, o, 0, 0, 0, 1, Blocks.SMOOTH_STONE.defaultBlockState());
        set(p, o, 0, 2, 0, Blocks.WATER.defaultBlockState());
        set(p, o, 0, 3, 0, Blocks.SEA_LANTERN.defaultBlockState());
        scatterBoxes(p, o, rng, 3, 2);
    }

    private static void barracks(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 7, 4);
        box(p, o, -7, -4, 7, 4, 0, 3, Blocks.BRICKS.defaultBlockState());
        for (int dz = -4; dz <= 4; dz += 2) col(p, o, 0, dz, 0, 3, Blocks.BRICKS.defaultBlockState());
        scatterBoxes(p, o, rng, 3, 2);
    }

    private static void templeRuin(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 6, 6);
        walls(p, o, 6, 6, 0, 4, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        for (int dx = -5; dx <= 5; dx += 5) col(p, o, dx, -6, 0, 7, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        for (int dx = -5; dx <= 5; dx += 5) col(p, o, dx, 6, 0, 7, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        for (int y = 5; y <= 7; y++) { set(p, o, -5, y, -6, Blocks.CHISELED_STONE_BRICKS.defaultBlockState()); set(p, o, 5, y, 6, Blocks.CHISELED_STONE_BRICKS.defaultBlockState()); }
        set(p, o, 0, 8, 0, Blocks.SEA_LANTERN.defaultBlockState());
        scatterBoxes(p, o, rng, 3, 3);
    }

    private static void lighthouseBase(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 3, 3);
        walls(p, o, 3, 3, 0, 12, Blocks.WHITE_CONCRETE.defaultBlockState());
        for (int y = 0; y <= 12; y += 3) walls(p, o, 3, 3, y, y, Blocks.RED_CONCRETE.defaultBlockState());
        set(p, o, 0, 13, 0, Blocks.SEA_LANTERN.defaultBlockState());
        scatterBoxes(p, o, rng, 2, 1);
    }

    private static void pipelineJunction(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 6, 6);
        for (int dx = -6; dx <= 6; dx++) col(p, o, dx, 0, 0, 2, Blocks.COPPER_BLOCK.defaultBlockState());
        for (int dz = -6; dz <= 6; dz++) col(p, o, 0, dz, 0, 2, Blocks.COPPER_BLOCK.defaultBlockState());
        set(p, o, 0, 3, 0, Blocks.COPPER_BLOCK.defaultBlockState());
        scatterBoxes(p, o, rng, 3, 2);
    }

    private static void cryoVault(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 4, 4);
        box(p, o, -4, -4, 4, 4, 0, 5, Blocks.PACKED_ICE.defaultBlockState());
        set(p, o, 0, 6, 0, Blocks.BLUE_ICE.defaultBlockState());
        scatterBoxes(p, o, rng, 2, 3);
    }

    private static void observatory(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 5, 5);
        walls(p, o, 5, 5, 0, 4, Blocks.POLISHED_DEEPSLATE.defaultBlockState());
        for (int i = 1; i <= 3; i++) walls(p, o, 5 - i, 5 - i, 4 + i, 4 + i, Blocks.DEEPSLATE_TILES.defaultBlockState());
        set(p, o, 0, 8, 0, Blocks.AMETHYST_BLOCK.defaultBlockState());
        scatterBoxes(p, o, rng, 2, 2);
    }

    private static void marketStalls(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 7, 5);
        for (int dx = -6; dx <= 6; dx += 3) {
            col(p, o, dx, -5, 0, 2, Blocks.OAK_FENCE.defaultBlockState());
            col(p, o, dx, 5, 0, 2, Blocks.OAK_FENCE.defaultBlockState());
            for (int dz = -5; dz <= 5; dz++) set(p, o, dx, 3, dz, Blocks.OAK_SLAB.defaultBlockState());
        }
        scatterBoxes(p, o, rng, 4, 4);
    }

    private static void bridgeSpan(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 9, 2);
        for (int dx = -9; dx <= 9; dx++) {
            set(p, o, dx, 0, -2, Blocks.STONE_BRICKS.defaultBlockState());
            set(p, o, dx, 0, 2, Blocks.STONE_BRICKS.defaultBlockState());
            set(p, o, dx, 1, -2, Blocks.STONE_BRICKS.defaultBlockState());
            set(p, o, dx, 1, 2, Blocks.STONE_BRICKS.defaultBlockState());
        }
        scatterBoxes(p, o, rng, 3, 2);
    }

    private static void cistern(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 5, 5);
        walls(p, o, 5, 5, 0, 4, Blocks.MOSSY_COBBLESTONE.defaultBlockState());
        // 内部水窖
        for (int dx = -4; dx <= 4; dx++) for (int dz = -4; dz <= 4; dz++) set(p, o, dx, 1, dz, Blocks.WATER.defaultBlockState());
        scatterBoxes(p, o, rng, 2, 2);
    }

    private static void cathedral(Placer p, BlockPos o, RandomSource rng) {
        pad(p, o, 7, 7);
        walls(p, o, 7, 7, 0, 9, Blocks.COBBLESTONE.defaultBlockState());
        walls(p, o, 3, 3, 9, 14, Blocks.COBBLESTONE.defaultBlockState());
        for (int y = 10; y <= 14; y += 2) set(p, o, 0, y, 3, Blocks.GLASS.defaultBlockState());
        set(p, o, 0, 15, 0, Blocks.SEA_LANTERN.defaultBlockState());
        scatterBoxes(p, o, rng, 4, 3);
    }

    // ----------------------------------------------------------------- 通用工具

    private static void pad(Placer p, BlockPos o, int hw, int hd) {
        for (int dx = -hw; dx <= hw; dx++)
            for (int dz = -hd; dz <= hd; dz++)
                set(p, o, dx, -1, dz, SAND); // 加固海床底面（y=12）
    }

    private static void col(Placer p, BlockPos o, int dx, int dz, int y0, int y1, BlockState s) {
        for (int y = y0; y <= y1; y++) set(p, o, dx, y, dz, s);
    }

    private static void walls(Placer p, BlockPos o, int hw, int hd, int y0, int y1, BlockState s) {
        for (int y = y0; y <= y1; y++) {
            for (int dx = -hw; dx <= hw; dx++) {
                set(p, o, dx, y, -hd, s);
                set(p, o, dx, y, hd, s);
            }
            for (int dz = -hd; dz <= hd; dz++) {
                set(p, o, -hw, y, dz, s);
                set(p, o, hw, y, dz, s);
            }
        }
    }

    private static void box(Placer p, BlockPos o, int x0, int z0, int x1, int z1, int y0, int y1, BlockState s) {
        for (int dx = x0; dx <= x1; dx++)
            for (int dz = z0; dz <= z1; dz++)
                for (int y = y0; y <= y1; y++)
                    set(p, o, dx, y, dz, s);
    }

    private static void set(Placer p, BlockPos o, int dx, int dy, int dz, BlockState s) {
        p.set(o.offset(dx, dy, dz), s);
    }

    /** 在占地内随机散布 2~4 个物资箱（多于海岛的 1 个）。 */
    private static void scatterBoxes(Placer p, BlockPos o, RandomSource rng, int spread, int count) {
        int n = count + rng.nextInt(2); // 2..count+1（至少 2，至多 count+1）
        List<BlockPos> spots = new ArrayList<>();
        int tries = 0;
        while (spots.size() < n && tries < 40) {
            tries++;
            int dx = rng.nextInt(spread * 2 + 1) - spread;
            int dz = rng.nextInt(spread * 2 + 1) - spread;
            BlockPos c = o.offset(dx, 1, dz);
            if (!spots.contains(c)) spots.add(c);
        }
        for (BlockPos c : spots) {
            Block block = randomBoxBlock(rng);
            String cat = CATS[rng.nextInt(CATS.length)];
            SixtySecondsIslandGenerator.placeSupplyBox(p, c, block, cat);
        }
    }

    /** 海底废墟物资更丰富：更高概率高级箱、更低概率上锁。 */
    private static Block randomBoxBlock(RandomSource rng) {
        boolean advanced = rng.nextFloat() < 0.55F;
        boolean asRandom = rng.nextFloat() < 0.2F;
        boolean locked = !asRandom && rng.nextFloat() < 0.3F;
        if (advanced) {
            return locked ? ModBlocks.SIXTY_SECONDS_SUPPLY_BOX_ADVANCED_LOCKED
                    : ModBlocks.SIXTY_SECONDS_SUPPLY_BOX_ADVANCED;
        }
        if (asRandom) return ModBlocks.SIXTY_SECONDS_LOW_TIER_RANDOM_SUPPLY_BOX;
        return locked ? ModBlocks.SIXTY_SECONDS_SUPPLY_BOX_LOCKED : ModBlocks.SIXTY_SECONDS_SUPPLY_BOX;
    }
}
