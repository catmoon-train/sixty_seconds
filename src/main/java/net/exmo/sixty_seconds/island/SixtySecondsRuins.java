package net.exmo.sixty_seconds.island;

import net.exmo.sixty_seconds.SixtySecondsBalance;
import net.exmo.sixty_seconds.island.SixtySecondsIslandGenerator.Placer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 海岛废墟建筑：39 个程序化模板（编号 0..38），每岛按等级抽若干个散布在地表，
 * 全部经 {@link Placer} 写入（自动快照，随海岛还原一起回滚）。
 * 每处废墟旁保底一个物资箱（高等级岛概率升级为高级箱）。
 * <p>
 * <b>生态联动分布</b>：抽取不再均匀随机——每种岛屿 {@link SixtySecondsIsland.Type} 拥有
 * 一组偏好模板（见 {@link #PREFERRED}），每次放置以 70% 概率从该生态偏好池抽取、30% 概率
 * 从全池抽取，保证风格统一又不失多样性；模板在同岛内不重复。
 * <ul>
 *   <li>0 坍塌石屋 · 1 瞭望塔 · 2 石环祭坛 · 3 沉船残骸(滩) · 4 废弃码头(滩) · 5 灯塔残基(滩)</li>
 *   <li>6 半埋神殿 · 7 破败仓库 · 8 水井营地 · 9 荒废墓地 · 10 前哨围墙 · 11 教堂残骸</li>
 *   <li>12 渔夫小屋(滩) · 13 烽火台 · 14 矿洞入口 · 15 救生艇(滩) · 16 晾晒架营地 · 17 藤蔓雕像</li>
 *   <li>18 倒塌桥梁(滩) · 19 风暴避难所 · 20 废弃温室 · 21 海盗藏宝 · 22 图腾柱 · 23 篝火圈</li>
 *   <li>24 残破风车 · 25 石拱门 · 26 兽骨堆 · 27 陷坑陷阱 · 28 沉没钟楼 · 29 海上钻井残骸(滩)</li>
 *   <li>30 绞刑架 · 31 冰晶祭坛 · 32 黑曜石熔炉 · 33 珊瑚拱(滩)</li>
 *   <li>34 晶簇尖峰 · 35 献祭祭坛 · 36 破损投石机 · 37 帆匠棚(滩) · 38 菌林</li>
 * </ul>
 */
public final class SixtySecondsRuins {

    public static final int TEMPLATE_COUNT = 40;
    /** 滩涂系模板（选点贴近岸线、要求低海拔）。 */
    private static final boolean[] SHORE = new boolean[TEMPLATE_COUNT];

    /** 每种生态类型偏好抽取的模板（联动分布）。空数组=无偏好（纯随机）。 */
    private static final java.util.Map<SixtySecondsIsland.Type, int[]> PREFERRED = new java.util.EnumMap<>(SixtySecondsIsland.Type.class);

    /**
     * 专属模板：仅在该生态类型岛上生成、且保证至少出现一次（绕过随机抽取）。
     * 这些模板对其它类型不可见，不会混入普通抽取池。
     */
    private static final java.util.Map<SixtySecondsIsland.Type, int[]> EXCLUSIVE = new java.util.EnumMap<>(SixtySecondsIsland.Type.class);

    static {
        SHORE[3] = SHORE[4] = SHORE[5] = true;   // 沉船、废弃码头、灯塔残基
        SHORE[12] = SHORE[15] = SHORE[18] = true; // 渔夫小屋、救生艇、断桥
        SHORE[29] = SHORE[33] = SHORE[37] = true; // 海上钻井残骸、珊瑚拱、帆匠棚

        PREFERRED.put(SixtySecondsIsland.Type.TROPICAL,
                new int[]{20, 16, 12, 3, 17, 22, 0, 7});
        PREFERRED.put(SixtySecondsIsland.Type.MARSH,
                new int[]{9, 8, 17, 16, 26, 7});
        PREFERRED.put(SixtySecondsIsland.Type.VOLCANIC,
                new int[]{32, 26, 27, 30, 19, 21, 2});
        PREFERRED.put(SixtySecondsIsland.Type.CORAL,
                new int[]{33, 12, 3, 4, 15, 5, 37});
        PREFERRED.put(SixtySecondsIsland.Type.FROST,
                new int[]{31, 1, 13, 2, 8, 25, 28});
        PREFERRED.put(SixtySecondsIsland.Type.PLATEAU,
                new int[]{25, 2, 1, 10, 11, 36});
        PREFERRED.put(SixtySecondsIsland.Type.JUNGLE,
                new int[]{17, 20, 22, 16, 14});
        PREFERRED.put(SixtySecondsIsland.Type.BARREN,
                new int[]{26, 27, 30, 2, 25, 35});
        PREFERRED.put(SixtySecondsIsland.Type.RUINS,
                new int[]{0, 7, 11, 6, 10, 28, 13});
        PREFERRED.put(SixtySecondsIsland.Type.QUARANTINE,
                new int[]{19, 30, 26, 21, 29, 32});
        PREFERRED.put(SixtySecondsIsland.Type.OIL,
                new int[]{29, 19, 21, 30, 26, 32});
        PREFERRED.put(SixtySecondsIsland.Type.MILITARY,
                new int[]{10, 19, 27, 1, 30, 36});
        PREFERRED.put(SixtySecondsIsland.Type.ABYSS,
                new int[]{27, 30, 26, 32, 21, 35});
        PREFERRED.put(SixtySecondsIsland.Type.INFERNO,
                new int[]{32, 27, 30, 26, 19, 35});
        PREFERRED.put(SixtySecondsIsland.Type.FORSAKEN,
                new int[]{30, 26, 21, 19, 29, 35});
        PREFERRED.put(SixtySecondsIsland.Type.CRYSTAL,
                new int[]{34, 2, 13, 25, 17, 31});
        PREFERRED.put(SixtySecondsIsland.Type.SWAMP,
                new int[]{9, 17, 16, 26, 38, 8});
        PREFERRED.put(SixtySecondsIsland.Type.MESA,
                new int[]{25, 2, 7, 0, 36, 13});
        PREFERRED.put(SixtySecondsIsland.Type.ASHEN,
                new int[]{26, 30, 2, 25, 27, 35});
        PREFERRED.put(SixtySecondsIsland.Type.SCULK,
                new int[]{39});
        // 幽匿神殿：仅 SCULK 岛生成、且必出现一次
        EXCLUSIVE.put(SixtySecondsIsland.Type.SCULK, new int[]{39});
        // EVACUATION 不在 PREFERRED 中（撤离岛本就不调 placeAll）
    }

    private SixtySecondsRuins() {
    }

    /**
     * 每岛放置废墟：数量随等级（2..4 处），模板不重复抽取，滩涂系模板落在岸边。
     * <p>生态联动：70% 概率从本岛 {@link SixtySecondsIsland.Type} 偏好池抽取（见 {@link #PREFERRED}），
     * 30% 概率从全池抽取以保证多样性；偏好池用尽后自动回退到全池。
     * <p>专属模板（见 {@link #EXCLUSIVE}）仅在该生态类型的岛上生成，且保证至少出现一次，
     * 不会混入其它类型的抽取池。
     */
    public static void placeAll(Placer p, SixtySecondsIsland island) {
        RandomSource rng = RandomSource.create(island.seed ^ 0x521A5L);
        int count = Math.min(TEMPLATE_COUNT, 2 + island.level / 2 + rng.nextInt(2));

        // 本岛可抽取的模板：全模板去掉「非本类型的专属模板」（专属模板仅对归属类型可见）。
        java.util.List<Integer> allowed = new ArrayList<>();
        java.util.Set<Integer> exclusiveOthers = new java.util.HashSet<>();
        for (var e : EXCLUSIVE.entrySet()) {
            if (e.getKey() != island.type) {
                for (int t : e.getValue()) exclusiveOthers.add(t);
            }
        }
        for (int i = 0; i < TEMPLATE_COUNT; i++) {
            if (!exclusiveOthers.contains(i)) allowed.add(i);
        }

        int[] preferred = PREFERRED.getOrDefault(island.type, new int[0]);
        boolean hasPref = preferred.length > 0;

        java.util.Set<Integer> used = new java.util.HashSet<>();
        int placed = 0, guard = 0;

        // 先强制放置本类型专属模板（保证至少出现一次）
        for (int t : EXCLUSIVE.getOrDefault(island.type, new int[0])) {
            if (used.contains(t)) continue;
            BlockPos origin = findSpot(p, island, rng, SHORE[t]);
            if (origin != null) {
                placeOne(p, island, rng, t, origin);
                used.add(t);
                placed++;
            }
        }

        while (placed < count && guard++ < 400) {
            int template;
            if (hasPref && rng.nextFloat() < 0.7F) {
                java.util.List<Integer> pool = new ArrayList<>();
                for (int t : preferred) if (!used.contains(t) && allowed.contains(t)) pool.add(t);
                if (pool.isEmpty()) {
                    template = pickUnused(rng, used, allowed);
                } else {
                    template = pool.get(rng.nextInt(pool.size()));
                }
            } else {
                template = pickUnused(rng, used, allowed);
            }
            if (template < 0) break;
            used.add(template);

            BlockPos origin = findSpot(p, island, rng, SHORE[template]);
            if (origin == null) {
                continue;
            }
            placeOne(p, island, rng, template, origin);
            placed++;
        }
    }

    /** 在 origin 处建一处废墟模板，并按密度规则附带一个物资箱。 */
    private static void placeOne(Placer p, SixtySecondsIsland island, RandomSource rng,
            int template, BlockPos origin) {
        build(p, template, origin, rng, island.level);
        // 废墟物资箱：密度遵循 populate 的 SUPPLY_BOX_DENSITY 系数（不再保底），
        // 类型/上锁/随机规则与 populate 完全一致（高级升级按等级、82% 上锁、15% 随机）。
        BlockPos boxSpot = nearbyAir(p, origin, rng);
        if (boxSpot != null && rng.nextFloat() < SixtySecondsBalance.SUPPLY_BOX_DENSITY) {
            boolean advanced = rng.nextFloat()
                    < SixtySecondsBalance.SUPPLY_BOX_ADVANCED_PER_LEVEL * island.level;
            boolean asRandom = rng.nextFloat() < SixtySecondsBalance.SUPPLY_BOX_RANDOM_RATE;
            boolean locked = !asRandom && rng.nextFloat() < SixtySecondsBalance.SUPPLY_BOX_LOCK_RATE;
            SixtySecondsIslandGenerator.placeSupplyBox(p, boxSpot, advanced
                    ? (locked
                        ? net.exmo.sixty_seconds.registry.ModBlocks.SIXTY_SECONDS_SUPPLY_BOX_ADVANCED_LOCKED
                        : net.exmo.sixty_seconds.registry.ModBlocks.SIXTY_SECONDS_SUPPLY_BOX_ADVANCED)
                    : (asRandom
                        ? net.exmo.sixty_seconds.registry.ModBlocks.SIXTY_SECONDS_LOW_TIER_RANDOM_SUPPLY_BOX
                        : (locked
                            ? net.exmo.sixty_seconds.registry.ModBlocks.SIXTY_SECONDS_SUPPLY_BOX_LOCKED
                            : net.exmo.sixty_seconds.registry.ModBlocks.SIXTY_SECONDS_SUPPLY_BOX)),
                    rng.nextBoolean() ? "material" : "tool");
        }
    }

    /** 从 allowed 模板池随机取一个尚未使用的模板；用尽返回 -1。 */
    private static int pickUnused(RandomSource rng, java.util.Set<Integer> used, java.util.List<Integer> allowed) {
        if (used.size() >= allowed.size()) return -1;
        int t;
        do { t = allowed.get(rng.nextInt(allowed.size())); } while (used.contains(t));
        return t;
    }

    private static BlockPos findSpot(Placer p, SixtySecondsIsland island, RandomSource rng,
            boolean shore) {
        for (int attempt = 0; attempt < 20; attempt++) {
            BlockPos ground = SixtySecondsIslandGenerator.randomGround(p, island, rng,
                    shore ? 0.55 : 0.1, shore ? 0.95 : 0.6);
            if (ground == null) {
                continue;
            }
            if (shore && ground.getY() > island.seaY + 3) {
                continue; // 滩涂系要求低海拔
            }
            return ground;
        }
        return null;
    }

    private static BlockPos nearbyAir(Placer p, BlockPos origin, RandomSource rng) {
        for (int attempt = 0; attempt < 12; attempt++) {
            BlockPos pos = origin.offset(rng.nextInt(9) - 4, 0, rng.nextInt(9) - 4);
            for (int dy = 2; dy >= -2; dy--) {
                BlockPos candidate = pos.above(dy);
                if (p.getBlockState(candidate).isAir()
                        && p.getBlockState(candidate.below()).isSolidRender(null, candidate.below())) {
                    return candidate;
                }
            }
        }
        return null;
    }

    /** 建指定编号的模板（origin = 地表空气格）。 */
    public static void build(Placer p, int template, BlockPos origin, RandomSource rng, int islandLevel) {
        switch (template) {
            case 0 -> cottage(p, origin, rng, islandLevel);
            case 1 -> watchtower(p, origin, rng, islandLevel);
            case 2 -> stoneCircle(p, origin, rng, islandLevel);
            case 3 -> shipwreck(p, origin, rng);
            case 4 -> dock(p, origin, rng);
            case 5 -> lighthouse(p, origin, rng, islandLevel);
            case 6 -> temple(p, origin, rng, islandLevel);
            case 7 -> warehouse(p, origin, rng);
            case 8 -> wellCamp(p, origin, rng);
            case 9 -> graveyard(p, origin, rng);
            case 10 -> outpost(p, origin, rng, islandLevel);
            case 11 -> chapel(p, origin, rng, islandLevel);
            case 12 -> fishermanHut(p, origin, rng, islandLevel);
            case 13 -> signalBeacon(p, origin, rng, islandLevel);
            case 14 -> mineEntrance(p, origin, rng, islandLevel);
            case 15 -> lifeboat(p, origin, rng);
            case 16 -> dryingRackCamp(p, origin, rng);
            case 17 -> overgrownStatue(p, origin, rng, islandLevel);
            case 18 -> collapsedBridge(p, origin, rng, islandLevel);
            case 19 -> stormShelter(p, origin, rng, islandLevel);
            case 20 -> greenhouse(p, origin, rng);
            case 21 -> pirateCache(p, origin, rng);
            case 22 -> totemPole(p, origin, rng, islandLevel);
            case 24 -> windmill(p, origin, rng, islandLevel);
            case 25 -> stoneArch(p, origin, rng, islandLevel);
            case 26 -> bonePile(p, origin, rng);
            case 27 -> pitTrap(p, origin, rng, islandLevel);
            case 28 -> sunkenBell(p, origin, rng, islandLevel);
            case 29 -> oilRig(p, origin, rng, islandLevel);
            case 30 -> gallows(p, origin, rng);
            case 31 -> iceAltar(p, origin, rng, islandLevel);
            case 32 -> obsidianForge(p, origin, rng, islandLevel);
            case 33 -> coralArch(p, origin, rng);
            case 34 -> crystalSpires(p, origin, rng);
            case 35 -> sacrificialAltar(p, origin, rng);
            case 36 -> brokenCatapult(p, origin, rng);
            case 37 -> sailMaker(p, origin, rng);
            case 38 -> fungusGrove(p, origin, rng, islandLevel);
            case 39 -> sculkShrine(p, origin, rng);
            default -> campfireCircle(p, origin, rng);
        }
    }

    // ── 材料/几何小工具 ────────────────────────────────────────────────────

    /** 风化石料：苔藓比例随岛等级升高。 */
    private static BlockState stone(RandomSource rng, int islandLevel) {
        if (islandLevel >= 5 && rng.nextFloat() < 0.4F) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        return rng.nextFloat() < 0.2F + islandLevel * 0.08F
                ? Blocks.MOSSY_COBBLESTONE.defaultBlockState()
                : Blocks.COBBLESTONE.defaultBlockState();
    }

    private static BlockState brick(RandomSource rng, int islandLevel) {
        float r = rng.nextFloat();
        if (r < 0.15F + islandLevel * 0.05F) {
            return Blocks.MOSSY_STONE_BRICKS.defaultBlockState();
        }
        return r < 0.5F ? Blocks.CRACKED_STONE_BRICKS.defaultBlockState()
                : Blocks.STONE_BRICKS.defaultBlockState();
    }

    /** 地基垫层：footprint 内 y-1 处若非实心则垫上。 */
    private static void pad(Placer p, BlockPos origin, int halfW, int halfD, BlockState block) {
        for (int dx = -halfW; dx <= halfW; dx++) {
            for (int dz = -halfD; dz <= halfD; dz++) {
                BlockPos below = origin.offset(dx, -1, dz);
                if (!p.getBlockState(below).isSolidRender(null, below)) {
                    p.set(below, block);
                }
            }
        }
    }

    /** 残墙矩形：周边一圈，高度随机坍塌（0..hMax），留门洞。 */
    private static void ruinedWalls(Placer p, BlockPos origin, int halfW, int halfD, int hMax,
            RandomSource rng, int islandLevel) {
        for (int dx = -halfW; dx <= halfW; dx++) {
            for (int dz = -halfD; dz <= halfD; dz++) {
                boolean edge = Math.abs(dx) == halfW || Math.abs(dz) == halfD;
                if (!edge) {
                    continue;
                }
                if (dz == halfD && Math.abs(dx) <= 1) {
                    continue; // 南侧门洞
                }
                int h = 1 + rng.nextInt(hMax);
                for (int y = 0; y < h; y++) {
                    p.set(origin.offset(dx, y, dz), stone(rng, islandLevel));
                }
            }
        }
    }

    // ── 12 模板 ──────────────────────────────────────────────────────────

    /** 0 坍塌石屋：7×9 残墙 + 半塌屋梁 + 屋内杂物。 */
    private static void cottage(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 3, 4, Blocks.COBBLESTONE.defaultBlockState());
        ruinedWalls(p, origin, 3, 4, 4, rng, lvl);
        for (int dx = -2; dx <= 2; dx++) { // 断梁
            if (rng.nextFloat() < 0.6F) {
                p.set(origin.offset(dx, 3, 0), Blocks.OAK_LOG.defaultBlockState());
            }
        }
        p.set(origin.offset(1, 0, -2), Blocks.BARREL.defaultBlockState());
        if (rng.nextBoolean()) {
            p.set(origin.offset(-2, 0, 1), Blocks.HAY_BLOCK.defaultBlockState());
        }
    }

    /** 1 瞭望塔：3×3 塔身 8~11 高，顶部坍塌开口，内嵌立足层。 */
    private static void watchtower(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 2, 2, Blocks.COBBLESTONE.defaultBlockState());
        int height = 8 + rng.nextInt(4);
        for (int y = 0; y < height; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                    if (!edge) {
                        if (y == height - 2) {
                            p.set(origin.offset(dx, y, dz), Blocks.OAK_PLANKS.defaultBlockState()); // 眺望层
                        }
                        continue;
                    }
                    if (y == 0 && dz == 1 && dx == 0) {
                        continue; // 入口
                    }
                    // 顶部随机坍塌
                    if (y >= height - 2 && rng.nextFloat() < 0.45F) {
                        continue;
                    }
                    p.set(origin.offset(dx, y, dz), brick(rng, lvl));
                }
            }
        }
        p.set(origin.offset(0, height - 1, 0), Blocks.LANTERN.defaultBlockState());
    }

    /** 2 石环祭坛：半径 5 的立柱环 + 中心凿制石英祭台。 */
    private static void stoneCircle(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            BlockPos base = origin.offset((int) Math.round(Math.cos(angle) * 5), 0,
                    (int) Math.round(Math.sin(angle) * 5));
            int h = 1 + rng.nextInt(3);
            for (int y = 0; y < h; y++) {
                p.set(base.above(y), stone(rng, lvl));
            }
        }
        p.set(origin, Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        p.set(origin.above(), Blocks.QUARTZ_SLAB.defaultBlockState());
    }

    /** 3 沉船残骸：搁浅的木船壳（龙骨 + 两舷 + 断桅）。 */
    private static void shipwreck(Placer p, BlockPos origin, RandomSource rng) {
        BlockState hull = Blocks.DARK_OAK_PLANKS.defaultBlockState();
        BlockState frame = Blocks.DARK_OAK_LOG.defaultBlockState();
        for (int dz = -6; dz <= 6; dz++) {
            int width = Math.abs(dz) >= 5 ? 1 : 2;
            for (int dx = -width; dx <= width; dx++) {
                p.set(origin.offset(dx, 0, dz), hull); // 船底
                if (Math.abs(dx) == width && rng.nextFloat() < 0.75F) {
                    p.set(origin.offset(dx, 1, dz), hull); // 舷侧（缺口=破损）
                    if (Math.abs(dz) <= 3 && rng.nextFloat() < 0.5F) {
                        p.set(origin.offset(dx, 2, dz), hull);
                    }
                }
            }
        }
        for (int y = 1; y <= 3; y++) { // 断桅
            p.set(origin.offset(0, y, -1), frame);
        }
        p.set(origin.offset(1, 3, -1), frame);
        p.set(origin.offset(0, 1, 3), Blocks.BARREL.defaultBlockState());
    }

    /** 4 废弃码头：栈桥伸向海面，木桩 + 缺板。 */
    private static void dock(Placer p, BlockPos origin, RandomSource rng) {
        // 栈桥朝离岛心方向延伸会更自然，这里固定 +X 方向（岛面选点已贴岸）
        for (int dx = 0; dx <= 12; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (rng.nextFloat() < 0.18F) {
                    continue; // 缺板
                }
                p.set(origin.offset(dx, 0, dz), Blocks.OAK_PLANKS.defaultBlockState());
            }
            if (dx % 3 == 0) { // 木桩
                for (int dy = -1; dy >= -4; dy--) {
                    BlockPos pile = origin.offset(dx, dy, -1);
                    if (p.getBlockState(pile).isSolidRender(null, pile)) {
                        break;
                    }
                    p.set(pile, Blocks.OAK_FENCE.defaultBlockState());
                }
                p.set(origin.offset(dx, 1, 1), Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        p.set(origin.offset(12, 1, -1), Blocks.LANTERN.defaultBlockState());
    }

    /** 5 灯塔残基：圆形石砖塔基 6 高、断口，顶灯还亮着。 */
    private static void lighthouse(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 3, 3, Blocks.STONE_BRICKS.defaultBlockState());
        int height = 6 + rng.nextInt(3);
        for (int y = 0; y < height; y++) {
            for (int dx = -3; dx <= 3; dx++) {
                for (int dz = -3; dz <= 3; dz++) {
                    double dist = Math.sqrt(dx * dx + dz * dz);
                    if (dist < 2.2 || dist > 3.2) {
                        continue;
                    }
                    if (y == 0 && dz == 3 && Math.abs(dx) <= 1) {
                        continue; // 门
                    }
                    if (y >= height - 2 && rng.nextFloat() < 0.5F) {
                        continue; // 断口
                    }
                    p.set(origin.offset(dx, y, dz),
                            y % 3 == 2 ? Blocks.WHITE_CONCRETE.defaultBlockState() : brick(rng, lvl));
                }
            }
        }
        p.set(origin.above(height), Blocks.GLOWSTONE.defaultBlockState());
    }

    /** 6 半埋神殿：下沉 2 格的砂石平台 + 四角残柱 + 阶梯。 */
    private static void temple(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        BlockPos base = origin.below(2);
        for (int dx = -5; dx <= 5; dx++) {
            for (int dz = -5; dz <= 5; dz++) {
                p.set(base.offset(dx, 0, dz), Blocks.SANDSTONE.defaultBlockState());
                p.set(base.offset(dx, 1, dz), Blocks.AIR.defaultBlockState());
                p.set(base.offset(dx, 2, dz), Blocks.AIR.defaultBlockState());
            }
        }
        for (int cx = -4; cx <= 4; cx += 8) {
            for (int cz = -4; cz <= 4; cz += 8) {
                int h = 2 + rng.nextInt(3);
                for (int y = 1; y <= h; y++) {
                    p.set(base.offset(cx, y, cz), Blocks.CHISELED_SANDSTONE.defaultBlockState());
                }
            }
        }
        p.set(base.offset(0, 1, 0), Blocks.GOLD_BLOCK.defaultBlockState()); // 祭台残金
        for (int dz = 6; dz <= 7; dz++) { // 没入地面的台阶
            p.set(base.offset(0, dz - 5, dz), Blocks.SANDSTONE.defaultBlockState());
            p.set(base.offset(-1, dz - 5, dz), Blocks.SANDSTONE.defaultBlockState());
            p.set(base.offset(1, dz - 5, dz), Blocks.SANDSTONE.defaultBlockState());
        }
    }

    /** 7 破败仓库：木框架 9×7，屋顶塌了一半，货箱散落。 */
    private static void warehouse(Placer p, BlockPos origin, RandomSource rng) {
        pad(p, origin, 4, 3, Blocks.OAK_PLANKS.defaultBlockState());
        for (int dx = -4; dx <= 4; dx += 8) {
            for (int dz = -3; dz <= 3; dz += 6) {
                for (int y = 0; y < 4; y++) {
                    p.set(origin.offset(dx, y, dz), Blocks.OAK_LOG.defaultBlockState());
                }
            }
        }
        for (int dx = -4; dx <= 4; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                boolean edge = Math.abs(dx) == 4 || Math.abs(dz) == 3;
                if (edge && rng.nextFloat() < 0.55F) {
                    p.set(origin.offset(dx, 0, dz), Blocks.OAK_PLANKS.defaultBlockState());
                    if (rng.nextFloat() < 0.5F) {
                        p.set(origin.offset(dx, 1, dz), Blocks.OAK_PLANKS.defaultBlockState());
                    }
                }
                if (dx <= 0 && rng.nextFloat() < 0.8F) { // 半边屋顶尚存
                    p.set(origin.offset(dx, 4, dz), Blocks.OAK_SLAB.defaultBlockState());
                }
            }
        }
        for (int i = 0; i < 3; i++) {
            p.set(origin.offset(rng.nextInt(7) - 3, 0, rng.nextInt(5) - 2),
                    Blocks.BARREL.defaultBlockState());
        }
    }

    /** 8 水井营地：石井（含水）+ 熄灭营火 + 坐凳原木。 */
    private static void wellCamp(Placer p, BlockPos origin, RandomSource rng) {
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                if (edge) {
                    p.set(origin.offset(dx, 0, dz), Blocks.COBBLESTONE.defaultBlockState());
                    p.set(origin.offset(dx, -1, dz), Blocks.COBBLESTONE.defaultBlockState());
                } else {
                    p.set(origin.offset(dx, -1, dz), Blocks.WATER.defaultBlockState());
                    p.set(origin.offset(dx, -2, dz), Blocks.COBBLESTONE.defaultBlockState());
                }
            }
        }
        p.set(origin.offset(-1, 1, -1), Blocks.OAK_FENCE.defaultBlockState());
        p.set(origin.offset(1, 1, 1), Blocks.OAK_FENCE.defaultBlockState());
        BlockPos camp = origin.offset(4, 0, 0);
        p.set(camp, Blocks.CAMPFIRE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
        p.set(camp.offset(0, 0, 2), Blocks.OAK_LOG.defaultBlockState());
        p.set(camp.offset(0, 0, -2), Blocks.OAK_LOG.defaultBlockState());
    }

    /** 9 荒废墓地：两排土坟 + 残碑 + 枯灌木。 */
    private static void graveyard(Placer p, BlockPos origin, RandomSource rng) {
        for (int row = 0; row < 2; row++) {
            for (int i = 0; i < 4; i++) {
                BlockPos grave = origin.offset(i * 3 - 4, 0, row * 4 - 2);
                p.set(grave, Blocks.COARSE_DIRT.defaultBlockState());
                p.set(grave.offset(0, 0, 1), Blocks.COARSE_DIRT.defaultBlockState());
                if (rng.nextFloat() < 0.8F) {
                    p.set(grave.offset(0, 1, -1), Blocks.COBBLESTONE_WALL.defaultBlockState());
                }
                if (rng.nextFloat() < 0.3F) {
                    p.set(grave.above(), Blocks.DEAD_BUSH.defaultBlockState());
                }
            }
        }
    }

    /** 10 前哨围墙：9×9 半塌石墙环 + 一角塔基 + 铁栅门洞。 */
    private static void outpost(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        ruinedWalls(p, origin, 4, 4, 3, rng, lvl);
        for (int y = 0; y < 5; y++) { // 角塔
            for (int dx = 3; dx <= 4; dx++) {
                for (int dz = 3; dz <= 4; dz++) {
                    if (y >= 3 && rng.nextFloat() < 0.4F) {
                        continue;
                    }
                    p.set(origin.offset(dx, y, dz), brick(rng, lvl));
                }
            }
        }
        p.set(origin.offset(0, 0, 4), Blocks.IRON_BARS.defaultBlockState());
        p.set(origin.offset(0, 1, 4), Blocks.IRON_BARS.defaultBlockState());
        p.set(origin.offset(-2, 0, 0), Blocks.BARREL.defaultBlockState());
    }

    /** 11 教堂残骸：8×13 石砖长厅，山墙 + 断拱 + 祭坛烛台。 */
    private static void chapel(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 3, 6, Blocks.STONE_BRICKS.defaultBlockState());
        for (int dz = -6; dz <= 6; dz++) {
            for (int dx = -3; dx <= 3; dx += 6) {
                int h = dz <= -4 ? 5 : 2 + rng.nextInt(3); // 北端山墙最高，向南塌
                for (int y = 0; y < h; y++) {
                    p.set(origin.offset(dx, y, dz), brick(rng, lvl));
                }
            }
        }
        for (int dx = -3; dx <= 3; dx++) { // 北山墙封口 + 断拱
            for (int y = 0; y < 6 - Math.abs(dx); y++) {
                p.set(origin.offset(dx, y, -6), brick(rng, lvl));
            }
        }
        p.set(origin.offset(0, 0, -5), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        p.set(origin.offset(0, 1, -5), Blocks.CANDLE.defaultBlockState());
        for (int dz = -3; dz <= 3; dz += 2) { // 断长椅
            if (rng.nextFloat() < 0.7F) {
                p.set(origin.offset(-1, 0, dz), Blocks.OAK_SLAB.defaultBlockState());
                p.set(origin.offset(1, 0, dz), Blocks.OAK_SLAB.defaultBlockState());
            }
        }
    }

    // ── 新增模板 12..23 ────────────────────────────────────────────────────

    /** 12 渔夫小屋：临水小木棚 5×4，半塌屋顶 + 渔具杂物。 */
    private static void fishermanHut(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 2, 2, Blocks.OAK_PLANKS.defaultBlockState());
        for (int dz = -2; dz <= 2; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                if (!edge) continue;
                if (dz == 2 && dx == 0) continue; // 门
                int h = 1 + rng.nextInt(3);
                for (int y = 0; y < h; y++) {
                    if (y >= 2 && rng.nextFloat() < 0.5F) continue;
                    p.set(origin.offset(dx, y, dz), Blocks.OAK_PLANKS.defaultBlockState());
                }
            }
        }
        // 半边残顶
        for (int dx = -2; dx <= 0; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (rng.nextFloat() < 0.65F)
                    p.set(origin.offset(dx, 2, dz), Blocks.OAK_SLAB.defaultBlockState());
            }
        }
        p.set(origin.offset(1, 0, 1), Blocks.BARREL.defaultBlockState());
        p.set(origin.offset(-1, 0, -1), Blocks.COBWEB.defaultBlockState());
        if (rng.nextBoolean())
            p.set(origin.offset(0, 0, 0), Blocks.CRAFTING_TABLE.defaultBlockState());
    }

    /** 13 信号烽火台：5..8 高石柱，顶置熄灭的营火（可被玩家点燃求援）。 */
    private static void signalBeacon(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 1, 1, Blocks.COBBLESTONE.defaultBlockState());
        int h = 5 + rng.nextInt(4);
        for (int y = 0; y < h; y++) {
            p.set(origin.above(y), brick(rng, lvl));
            // 侧面随机缺块（风化）
            if (y > 0 && y < h - 1 && rng.nextFloat() < 0.12F) {
                Direction dir = randomHorizontal(rng);
                p.set(origin.relative(dir).above(y), Blocks.AIR.defaultBlockState());
            }
        }
        p.set(origin.above(h), Blocks.CAMPFIRE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
    }

    /** 14 废弃矿洞入口：山体掏出的 3×3 洞口，木梁支撑，洞内一段隧道。 */
    private static void mineEntrance(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        // 挖出入口隧道（向 -Z 方向）
        for (int dz = 0; dz <= 4; dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dy = 0; dy <= 2; dy++) {
                    if (dz == 0 && (Math.abs(dx) == 1 && dy <= 1)) continue; // 门框留石
                    BlockPos tp = origin.offset(dx, dy, -dz);
                    p.set(tp, Blocks.AIR.defaultBlockState());
                }
            }
            // 木支撑框每 2 格一道
            if (dz % 2 == 1 || dz == 4) {
                for (int dx = -1; dx <= 1; dx++) {
                    for (int dy = 0; dy <= 2; dy++) {
                        boolean frame = Math.abs(dx) == 1 || dy == 2;
                        if (frame)
                            p.set(origin.offset(dx, dy, -dz), Blocks.OAK_LOG.defaultBlockState());
                    }
                }
            }
        }
        // 洞口上方横木
        p.set(origin.offset(0, 2, 0), Blocks.OAK_LOG.defaultBlockState());
        p.set(origin.offset(-1, 2, 0), Blocks.OAK_LOG.defaultBlockState());
        p.set(origin.offset(1, 2, 0), Blocks.OAK_LOG.defaultBlockState());
        // 矿车/铁轨残骸
        p.set(origin.offset(0, 0, -3), Blocks.RAIL.defaultBlockState());
        p.set(origin.offset(0, 0, -4), Blocks.RAIL.defaultBlockState());
        if (rng.nextBoolean())
            p.set(origin.offset(1, 0, -3), Blocks.CHEST.defaultBlockState());
    }

    /** 15 搁浅救生艇：4×2 小木船搁浅在滩头，半截入沙。 */
    private static void lifeboat(Placer p, BlockPos origin, RandomSource rng) {
        BlockState hull = Blocks.SPRUCE_PLANKS.defaultBlockState();
        for (int dz = -1; dz <= 1; dz++) {
            for (int dx = -2; dx <= 2; dx++) {
                boolean rim = Math.abs(dx) == 2;
                if (rim) {
                    p.set(origin.offset(dx, 0, dz), hull);
                    if (Math.abs(dz) <= 1 && rng.nextFloat() < 0.55F)
                        p.set(origin.offset(dx, 1, dz), hull);
                } else {
                    p.set(origin.offset(dx, 0, dz), hull);
                }
            }
        }
        // 船尾板 + 断桨
        p.set(origin.offset(0, 1, -1), Blocks.SPRUCE_SLAB.defaultBlockState());
        p.set(origin.offset(0, 1, 1), Blocks.SPRUCE_SLAB.defaultBlockState());
        if (rng.nextBoolean())
            p.set(origin.offset(2, 1, 0), Blocks.OAK_FENCE.defaultBlockState()); // 残桨
    }

    /** 16 晾晒架营地：两根木柱 + 横梁 + 蛛网/线模拟晾晒物，旁有木桶。 */
    private static void dryingRackCamp(Placer p, BlockPos origin, RandomSource rng) {
        pad(p, origin, 3, 2, Blocks.COARSE_DIRT.defaultBlockState());
        // 两排晾晒架
        for (int rack = 0; rack < 2; rack++) {
            int dz = rack * 3 - 1;
            for (int y = 0; y < 3; y++)
                p.set(origin.offset(-2, y, dz), Blocks.OAK_FENCE.defaultBlockState());
            for (int y = 0; y < 3; y++)
                p.set(origin.offset(2, y, dz), Blocks.OAK_FENCE.defaultBlockState());
            p.set(origin.offset(-2, 2, dz), Blocks.OAK_SLAB.defaultBlockState());
            p.set(origin.offset(2, 2, dz), Blocks.OAK_SLAB.defaultBlockState());
            // 横绳
            p.set(origin.offset(-1, 2, dz), Blocks.TRIPWIRE.defaultBlockState());
            p.set(origin.offset(0, 2, dz), Blocks.TRIPWIRE.defaultBlockState());
            p.set(origin.offset(1, 2, dz), Blocks.TRIPWIRE.defaultBlockState());
            // 晾晒物（随机蛛网）
            if (rng.nextFloat() < 0.5F)
                p.set(origin.offset(0, 1, dz), Blocks.COBWEB.defaultBlockState());
        }
        p.set(origin.offset(0, 0, 2), Blocks.BARREL.defaultBlockState());
    }

    /** 17 藤蔓覆盖的雕像：3 高石像底座，风化残缺，挂满藤蔓。 */
    private static void overgrownStatue(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 1, 1, Blocks.STONE_BRICKS.defaultBlockState());
        // 底座
        for (int dx = -1; dx <= 1; dx++)
            for (int dz = -1; dz <= 1; dz++)
                p.set(origin.offset(dx, 0, dz), Blocks.STONE_BRICKS.defaultBlockState());
        // 雕像主体
        p.set(origin.offset(0, 1, 0), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
        p.set(origin.offset(0, 2, 0), Blocks.STONE_BRICK_WALL.defaultBlockState());
        // 头部（残缺）
        if (rng.nextFloat() < 0.65F)
            p.set(origin.offset(0, 3, 0), Blocks.PLAYER_HEAD.defaultBlockState());
        // 藤蔓覆盖
        for (int y = 0; y <= 3; y++) {
            for (Direction dir : Direction.Plane.HORIZONTAL) {
                if (rng.nextFloat() < 0.3F + lvl * 0.08F) {
                    BlockPos vine = origin.relative(dir).above(y);
                    if (p.getBlockState(vine).isAir())
                        p.set(vine, Blocks.VINE.defaultBlockState());
                }
            }
        }
    }

    /** 18 倒塌桥梁：断桥向海面伸出 8 格后断裂，木桩 + 缺板 + 水中残柱。 */
    private static void collapsedBridge(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        // 桥面向 +X 延伸
        for (int dx = 0; dx <= 8; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (dx >= 5 && rng.nextFloat() < 0.3F) continue; // 后半更破
                if (rng.nextFloat() < 0.12F) continue; // 随机缺板
                p.set(origin.offset(dx, 0, dz), Blocks.OAK_PLANKS.defaultBlockState());
            }
            // 护栏残桩
            p.set(origin.offset(dx, 1, -2), Blocks.OAK_FENCE.defaultBlockState());
            p.set(origin.offset(dx, 1, 2), Blocks.OAK_FENCE.defaultBlockState());
            // 桥墩（入水）
            if (dx % 2 == 0) {
                for (int dy = -1; dy >= -4; dy--)
                    p.set(origin.offset(dx, dy, -1), Blocks.OAK_LOG.defaultBlockState());
                for (int dy = -1; dy >= -4; dy--)
                    p.set(origin.offset(dx, dy, 1), Blocks.OAK_LOG.defaultBlockState());
            }
        }
        // 断裂处残骸散落水中
        p.set(origin.offset(8, -1, 0), Blocks.OAK_SLAB.defaultBlockState());
        p.set(origin.offset(9, -1, -1), Blocks.OAK_PLANKS.defaultBlockState());
    }

    /** 19 风暴避难所：半地下入口 3×3 铁门 + 向下阶梯 + 铁块墙壁。 */
    private static void stormShelter(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        // 入口框体
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = 0; dy <= 2; dy++) {
                boolean frame = Math.abs(dx) == 1 || dy == 2;
                if (frame)
                    p.set(origin.offset(dx, dy, 0), Blocks.IRON_BLOCK.defaultBlockState());
            }
        }
        // 向下阶梯（origin 处原为空气，向下挖）
        for (int dy = 0; dy >= -5; dy--) {
            p.set(origin.offset(0, dy, 1), Blocks.AIR.defaultBlockState());
            p.set(origin.offset(1, dy, 1), Blocks.AIR.defaultBlockState());
            p.set(origin.offset(-1, dy, 1), Blocks.AIR.defaultBlockState());
            // 阶梯
            p.set(origin.offset(0, dy, 2), Blocks.STONE_BRICKS.defaultBlockState());
            p.set(origin.offset(0, dy, 0), Blocks.STONE_BRICKS.defaultBlockState());
            p.set(origin.offset(-1, dy, 2), Blocks.STONE_BRICKS.defaultBlockState());
            p.set(origin.offset(1, dy, 2), Blocks.STONE_BRICKS.defaultBlockState());
        }
        // 底部小空间
        for (int dx = -2; dx <= 2; dx++) {
            p.set(origin.offset(dx, -5, 2), Blocks.STONE_BRICKS.defaultBlockState());
            if (Math.abs(dx) <= 1) p.set(origin.offset(dx, -5, 1), Blocks.AIR.defaultBlockState());
        }
        if (rng.nextBoolean())
            p.set(origin.offset(0, -4, 0), Blocks.CHEST.defaultBlockState());
    }

    /** 20 废弃温室：玻璃+铁框 6×4 大棚，屋顶塌了大半，内部残存花盆。 */
    private static void greenhouse(Placer p, BlockPos origin, RandomSource rng) {
        pad(p, origin, 3, 2, Blocks.IRON_BLOCK.defaultBlockState());
        // 铁框架
        for (int dx = -3; dx <= 3; dx += 6) {
            for (int dz = -2; dz <= 2; dz += 4) {
                for (int y = 0; y < 3; y++)
                    p.set(origin.offset(dx, y, dz), Blocks.IRON_BLOCK.defaultBlockState());
            }
        }
        // 玻璃墙（残破）
        for (int dz = -2; dz <= 2; dz++) {
            for (int y = 0; y <= 2; y++) {
                if (rng.nextFloat() < 0.35F) continue;
                p.set(origin.offset(-3, y, dz), Blocks.GLASS_PANE.defaultBlockState());
                p.set(origin.offset(3, y, dz), Blocks.GLASS_PANE.defaultBlockState());
            }
        }
        // 半边玻璃顶
        for (int dx = -2; dx <= 0; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                if (rng.nextFloat() < 0.4F) continue;
                p.set(origin.offset(dx, 3, dz), Blocks.GLASS.defaultBlockState());
            }
        }
        // 花盆
        p.set(origin.offset(-1, 0, 0), Blocks.FLOWER_POT.defaultBlockState());
        if (rng.nextBoolean())
            p.set(origin.offset(1, 0, 0), Blocks.DEAD_BUSH.defaultBlockState());
    }

    /** 21 海盗藏宝处：岩石裂缝中埋藏的宝箱 + 散落金块/骨块。 */
    private static void pirateCache(Placer p, BlockPos origin, RandomSource rng) {
        // 岩石围成掩护
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) >= 2 || Math.abs(dz) >= 2;
                if (!edge) continue;
                if (dz == -2 && dx == 0) continue; // 裂缝入口
                for (int y = 0; y <= 1; y++) {
                    if (rng.nextFloat() < 0.25F) continue;
                    p.set(origin.offset(dx, y, dz), Blocks.MOSSY_COBBLESTONE.defaultBlockState());
                }
            }
        }
        // 宝箱埋在半腰
        p.set(origin.offset(0, -1, 0), Blocks.SAND.defaultBlockState());
        p.set(origin, Blocks.CHEST.defaultBlockState());
        // 散落财物
        p.set(origin.offset(1, 0, 1), Blocks.GOLD_ORE.defaultBlockState());
        if (rng.nextFloat() < 0.4F)
            p.set(origin.offset(-1, 0, 1), Blocks.BONE_BLOCK.defaultBlockState());
        if (rng.nextFloat() < 0.25F)
            p.set(origin.offset(0, 1, 0), Blocks.SKELETON_SKULL.defaultBlockState());
    }

    /** 22 图腾柱：5..7 高木柱，不同高度段交替用不同原木 + 雕刻南瓜顶。 */
    private static void totemPole(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 1, 1, Blocks.COBBLESTONE.defaultBlockState());
        int h = 5 + rng.nextInt(3);
        for (int y = 0; y < h; y++) {
            BlockState log = switch (y % 3) {
                case 0 -> Blocks.OAK_LOG.defaultBlockState();
                case 1 -> Blocks.SPRUCE_LOG.defaultBlockState();
                default -> Blocks.CHERRY_LOG.defaultBlockState();
            };
            p.set(origin.above(y), log);
            // 侧面雕刻（不同方向小突起）
            if (y % 2 == 1) {
                Direction dir = randomHorizontal(rng);
                BlockPos side = origin.relative(dir).above(y);
                if (p.getBlockState(side).isAir())
                    p.set(side, Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        // 顶饰
        if (rng.nextFloat() < 0.5F)
            p.set(origin.above(h), Blocks.CARVED_PUMPKIN.defaultBlockState());
        else
            p.set(origin.above(h), Blocks.LANTERN.defaultBlockState());
        // 低等级岛图腾柱底部有祭品
        if (lvl <= 2 && rng.nextBoolean())
            p.set(origin.offset(1, 0, 0), Blocks.POPPY.defaultBlockState());
    }

    /** 23 篝火集结点：圆形原木座围绕熄灭营火，地面有踩踏痕迹。 */
    private static void campfireCircle(Placer p, BlockPos origin, RandomSource rng) {
        // 踩踏地面
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                double dist = Math.sqrt(dx * dx + dz * dz);
                if (dist <= 3.5 && p.getBlockState(origin.offset(dx, -1, dz)).is(Blocks.GRASS_BLOCK))
                    p.set(origin.offset(dx, -1, dz), Blocks.COARSE_DIRT.defaultBlockState());
            }
        }
        // 中心营火（已熄）
        p.set(origin, Blocks.CAMPFIRE.defaultBlockState()
                .setValue(net.minecraft.world.level.block.CampfireBlock.LIT, false));
        // 环绕原木坐凳
        for (int i = 0; i < 8; i++) {
            double angle = Math.PI * 2 * i / 8;
            int dx = (int) Math.round(Math.cos(angle) * 2.5);
            int dz = (int) Math.round(Math.sin(angle) * 2.5);
            if (rng.nextFloat() < 0.8F)
                p.set(origin.offset(dx, 0, dz), Blocks.OAK_LOG.defaultBlockState());
        }
        // 柴火堆
        p.set(origin.offset(2, 0, 0), Blocks.OAK_LOG.defaultBlockState());
        p.set(origin.offset(-2, 0, 1), Blocks.OAK_LOG.defaultBlockState());
    }

    // ── 新增模板 24..33（跨风格补充）──────────────────────────────────────

    /** 24 残破风车：石砖塔身 6~9 高，顶部断叶片（旋转木架残破）。 */
    private static void windmill(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 2, 2, Blocks.COBBLESTONE.defaultBlockState());
        int height = 6 + rng.nextInt(4);
        for (int y = 0; y < height; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                    if (!edge) {
                        continue;
                    }
                    if (y == 0 && dz == 1 && dx == 0) {
                        continue; // 门
                    }
                    if (y >= height - 2 && rng.nextFloat() < 0.4F) {
                        continue; // 顶部风化缺口
                    }
                    p.set(origin.offset(dx, y, dz), brick(rng, lvl));
                }
            }
        }
        // 顶部断叶片：仅保留 1~2 片
        int blades = 1 + rng.nextInt(2);
        for (int i = 0; i < blades; i++) {
            double angle = Math.PI * 2 * i / 4 + rng.nextDouble();
            int len = 3 + rng.nextInt(2);
            for (int s = 1; s <= len; s++) {
                int bx = (int) Math.round(Math.cos(angle) * s);
                int bz = (int) Math.round(Math.sin(angle) * s);
                p.set(origin.offset(bx, height, bz), Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        p.set(origin.above(height), Blocks.SPRUCE_PLANKS.defaultBlockState());
    }

    /** 25 石拱门：装饰性地标，横跨小径的残破拱（两侧柱 + 顶石）。 */
    private static void stoneArch(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 3, 1, Blocks.STONE_BRICKS.defaultBlockState());
        int h = 4 + rng.nextInt(2);
        for (int y = 0; y < h; y++) {
            if (rng.nextFloat() >= 0.35F) {
                p.set(origin.offset(-3, y, 0), brick(rng, lvl));
            }
            if (rng.nextFloat() >= 0.35F) {
                p.set(origin.offset(3, y, 0), brick(rng, lvl));
            }
        }
        // 拱顶石（残缺）
        for (int dx = -2; dx <= 2; dx++) {
            if (rng.nextFloat() < 0.5F) {
                p.set(origin.offset(dx, h, 0), brick(rng, lvl));
            }
        }
        p.set(origin.offset(0, h + 1, 0), Blocks.CHISELED_STONE_BRICKS.defaultBlockState());
    }

    /** 26 兽骨堆：诡异场景，散落骨头/骨块 + 中央巨大颅骨。 */
    private static void bonePile(Placer p, BlockPos origin, RandomSource rng) {
        pad(p, origin, 2, 2, Blocks.COARSE_DIRT.defaultBlockState());
        for (int i = 0; i < 6; i++) {
            BlockPos b = origin.offset(rng.nextInt(5) - 2, 0, rng.nextInt(5) - 2);
            p.set(b, rng.nextBoolean() ? Blocks.BONE_BLOCK.defaultBlockState()
                    : Blocks.COBBLESTONE.defaultBlockState());
        }
        // 中央颅骨
        p.set(origin, Blocks.SKELETON_SKULL.defaultBlockState());
        p.set(origin.above(), Blocks.BONE_BLOCK.defaultBlockState());
        if (rng.nextBoolean()) {
            p.set(origin.offset(0, 1, 1), Blocks.SKELETON_SKULL.defaultBlockState());
        }
    }

    /** 27 陷坑陷阱：危险地形，地表挖出尖刺坑（木刺+深坑），警示作用。 */
    private static void pitTrap(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        // 挖坑（3×3，深 3）
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                for (int dy = 0; dy >= -3; dy--) {
                    p.set(origin.offset(dx, dy, dz), Blocks.AIR.defaultBlockState());
                }
                // 坑底木刺
                p.set(origin.offset(dx, -3, dz), Blocks.OAK_FENCE.defaultBlockState());
            }
        }
        // 坑沿残木围栏（破损）
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                if (edge && rng.nextFloat() < 0.5F) {
                    p.set(origin.offset(dx, 0, dz), Blocks.OAK_FENCE.defaultBlockState());
                }
            }
        }
        if (rng.nextBoolean()) {
            p.set(origin.above(), Blocks.TRIPWIRE.defaultBlockState());
        }
    }

    /** 28 沉没钟楼：半陷地下的石砖钟楼，顶部悬一口钟（已静默）。 */
    private static void sunkenBell(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        BlockPos base = origin.below(2); // 半埋
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                boolean edge = Math.abs(dx) == 2 || Math.abs(dz) == 2;
                if (!edge) {
                    continue;
                }
                for (int y = 0; y <= 5; y++) {
                    if (rng.nextFloat() < 0.2F) {
                        continue;
                    }
                    p.set(base.offset(dx, y, dz), brick(rng, lvl));
                }
            }
        }
        // 悬钟（石砖梁 + 钟）
        p.set(base.offset(0, 5, 0), Blocks.OAK_LOG.defaultBlockState());
        p.set(base.offset(0, 6, 0), Blocks.BELL.defaultBlockState());
    }

    /** 29 海上钻井残骸(滩)：锈蚀铁架平台，向海面伸出，残破管线和油桶。 */
    private static void oilRig(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 2, 2, Blocks.IRON_BLOCK.defaultBlockState());
        int h = 4 + rng.nextInt(3);
        for (int y = 0; y < h; y++) {
            for (int dx = -1; dx <= 1; dx++) {
                for (int dz = -1; dz <= 1; dz++) {
                    boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                    if (!edge) {
                        continue;
                    }
                    if (y == 0 && dz == 0 && dx == 0) {
                        continue;
                    }
                    if (rng.nextFloat() < 0.3F) {
                        continue; // 锈蚀缺口
                    }
                    p.set(origin.offset(dx, y, dz), Blocks.IRON_BARS.defaultBlockState());
                }
            }
        }
        // 伸出海面的栈桥
        for (int dx = 2; dx <= 7; dx++) {
            if (rng.nextFloat() < 0.25F) {
                continue;
            }
            p.set(origin.offset(dx, 0, 0), Blocks.IRON_BLOCK.defaultBlockState());
        }
        p.set(origin.offset(1, h, 0), Blocks.LANTERN.defaultBlockState());
        p.set(origin.offset(-1, 0, -1), Blocks.BARREL.defaultBlockState());
    }

    /** 30 绞刑架：两根立柱 + 横梁 + 垂下锁链，诡异地标。 */
    private static void gallows(Placer p, BlockPos origin, RandomSource rng) {
        pad(p, origin, 2, 1, Blocks.OAK_PLANKS.defaultBlockState());
        for (int y = 0; y < 5; y++) {
            p.set(origin.offset(-1, y, 0), Blocks.OAK_LOG.defaultBlockState());
            p.set(origin.offset(1, y, 0), Blocks.OAK_LOG.defaultBlockState());
        }
        p.set(origin.offset(-1, 4, 0), Blocks.OAK_LOG.defaultBlockState());
        p.set(origin.offset(0, 4, 0), Blocks.OAK_LOG.defaultBlockState());
        p.set(origin.offset(1, 4, 0), Blocks.OAK_LOG.defaultBlockState());
        // 垂下锁链/绳
        for (int y = 3; y >= 1; y--) {
            if (rng.nextFloat() < 0.4F) {
                continue;
            }
            p.set(origin.offset(0, y, 0), Blocks.CHAIN.defaultBlockState());
        }
        if (rng.nextBoolean()) {
            p.set(origin.offset(0, 0, 1), Blocks.SKELETON_SKULL.defaultBlockState());
        }
    }

    /** 31 冰晶祭坛：FROST 风，凿制石英+冰构成的小祭坛，中央浮冰核心。 */
    private static void iceAltar(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 2, 2, Blocks.PACKED_ICE.defaultBlockState());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                BlockState m = edge ? Blocks.QUARTZ_BLOCK.defaultBlockState()
                        : Blocks.ICE.defaultBlockState();
                p.set(origin.offset(dx, 0, dz), m);
                if (edge && rng.nextFloat() < 0.5F) {
                    p.set(origin.offset(dx, 1, dz), Blocks.QUARTZ_PILLAR.defaultBlockState());
                }
            }
        }
        p.set(origin, Blocks.BLUE_ICE.defaultBlockState());
        p.set(origin.above(), Blocks.SEA_LANTERN.defaultBlockState());
    }

    /** 32 黑曜石熔炉：VOLCANIC 风，黑曜石炉体 + 熔岩核心 + 裂纹发光。 */
    private static void obsidianForge(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 2, 2, Blocks.OBSIDIAN.defaultBlockState());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                if (edge) {
                    p.set(origin.offset(dx, 0, dz), Blocks.OBSIDIAN.defaultBlockState());
                    if (rng.nextFloat() < 0.6F) {
                        p.set(origin.offset(dx, 1, dz), Blocks.OBSIDIAN.defaultBlockState());
                    }
                }
            }
        }
        // 熔岩核心
        p.set(origin, Blocks.LAVA.defaultBlockState());
        p.set(origin.above(), Blocks.GLOWSTONE.defaultBlockState());
        // 裂纹余烬
        for (int i = 0; i < 3; i++) {
            BlockPos e = origin.offset(rng.nextInt(3) - 1, 0, rng.nextInt(3) - 1);
            if (p.getBlockState(e).isAir()) {
                p.set(e, Blocks.MAGMA_BLOCK.defaultBlockState());
            }
        }
    }

    /** 33 珊瑚拱(滩)：CORAL 风，彩色珊瑚构成的低拱，点缀海扇与海晶。 */
    private static void coralArch(Placer p, BlockPos origin, RandomSource rng) {
        BlockState[] corals = {
                Blocks.RED_CONCRETE.defaultBlockState(),
                Blocks.BLUE_CONCRETE.defaultBlockState(),
                Blocks.PINK_CONCRETE.defaultBlockState(),
                Blocks.YELLOW_CONCRETE.defaultBlockState()
        };
        pad(p, origin, 2, 1, corals[rng.nextInt(corals.length)]);
        int h = 3 + rng.nextInt(2);
        for (int y = 0; y < h; y++) {
            BlockState c = corals[rng.nextInt(corals.length)];
            if (rng.nextFloat() >= 0.3F) {
                p.set(origin.offset(-2, y, 0), c);
            }
            if (rng.nextFloat() >= 0.3F) {
                p.set(origin.offset(2, y, 0), c);
            }
        }
        for (int dx = -1; dx <= 1; dx++) {
            if (rng.nextFloat() < 0.5F) {
                p.set(origin.offset(dx, h, 0), corals[rng.nextInt(corals.length)]);
            }
        }
        p.set(origin.offset(0, h + 1, 0), Blocks.SEA_LANTERN.defaultBlockState());
    }

    /** 34 晶簇尖峰：CRYSTAL 风，紫水晶母岩基座 + 数根石英/紫晶尖峰，顶部发光芽。 */
    private static void crystalSpires(Placer p, BlockPos origin, RandomSource rng) {
        pad(p, origin, 2, 2, Blocks.AMETHYST_BLOCK.defaultBlockState());
        int[] xs = {-1, 0, 1, -1, 1};
        int[] zs = {-1, 0, 1, 1, -1};
        for (int i = 0; i < 5; i++) {
            int h = 2 + rng.nextInt(4);
            BlockPos base = origin.offset(xs[i], 0, zs[i]);
            for (int y = 0; y < h; y++) {
                p.set(base.offset(0, y, 0), rng.nextFloat() < 0.5F
                        ? Blocks.QUARTZ_PILLAR.defaultBlockState()
                        : Blocks.AMETHYST_BLOCK.defaultBlockState());
            }
            p.set(base.offset(0, h, 0), rng.nextFloat() < 0.5F
                    ? Blocks.AMETHYST_CLUSTER.defaultBlockState()
                    : Blocks.SMALL_AMETHYST_BUD.defaultBlockState());
        }
    }

    /** 35 献祭祭坛：诡异通用风，黑石台座 + 红石沟槽 + 四角骨火盆。 */
    private static void sacrificialAltar(Placer p, BlockPos origin, RandomSource rng) {
        pad(p, origin, 2, 2, Blocks.POLISHED_BLACKSTONE.defaultBlockState());
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                boolean edge = Math.abs(dx) == 1 || Math.abs(dz) == 1;
                BlockState m = edge ? Blocks.POLISHED_BLACKSTONE.defaultBlockState()
                        : Blocks.REDSTONE_BLOCK.defaultBlockState();
                p.set(origin.offset(dx, 0, dz), m);
            }
        }
        p.set(origin, Blocks.CRYING_OBSIDIAN.defaultBlockState());
        p.set(origin.above(), Blocks.SOUL_LANTERN.defaultBlockState());
        for (int i = 0; i < 4; i++) {
            int dx = (i & 1) == 0 ? -1 : 1;
            int dz = (i & 2) == 0 ? -1 : 1;
            p.set(origin.offset(dx, 1, dz), Blocks.BONE_BLOCK.defaultBlockState());
        }
    }

    /** 36 破损投石机：PLATEAU/MILITARY 风，木架 + 石配重 + 抛臂，半塌。 */
    private static void brokenCatapult(Placer p, BlockPos origin, RandomSource rng) {
        pad(p, origin, 2, 1, Blocks.SPRUCE_PLANKS.defaultBlockState());
        p.set(origin.offset(-1, 1, 0), Blocks.SPRUCE_LOG.defaultBlockState());
        p.set(origin.offset(1, 1, 0), Blocks.SPRUCE_LOG.defaultBlockState());
        p.set(origin.offset(0, 3, 0), Blocks.SPRUCE_LOG.defaultBlockState()); // 抛臂支点
        p.set(origin.offset(0, 3, 1), Blocks.SPRUCE_FENCE.defaultBlockState());
        p.set(origin.offset(0, 4, -1), Blocks.STONE.defaultBlockState()); // 配重
        if (rng.nextFloat() < 0.5F) {
            p.set(origin.offset(2, 0, 0), Blocks.COBBLESTONE.defaultBlockState()); // 散落弹石
        }
    }

    /** 37 帆匠棚(滩)：CORAL/海风，晾网木架 + 工作台 + 桶，低矮贴近岸线。 */
    private static void sailMaker(Placer p, BlockPos origin, RandomSource rng) {
        pad(p, origin, 2, 1, Blocks.OAK_PLANKS.defaultBlockState());
        for (int z = -1; z <= 1; z++) {
            p.set(origin.offset(-1, 1, z), Blocks.OAK_FENCE.defaultBlockState());
            p.set(origin.offset(1, 1, z), Blocks.OAK_FENCE.defaultBlockState());
        }
        p.set(origin.offset(-1, 2, 0), Blocks.OAK_FENCE.defaultBlockState());
        p.set(origin.offset(1, 2, 0), Blocks.OAK_FENCE.defaultBlockState());
        p.set(origin, Blocks.CRAFTING_TABLE.defaultBlockState());
        p.set(origin.offset(1, 0, 1), Blocks.BARREL.defaultBlockState());
        if (rng.nextBoolean()) {
            p.set(origin.offset(0, 1, 1), Blocks.LANTERN.defaultBlockState());
        }
    }

    /** 38 菌林：SWAMP/JUNGLE 风，菌丝地表 + 巨型蘑菇丛 + 孢子光。 */
    private static void fungusGrove(Placer p, BlockPos origin, RandomSource rng, int lvl) {
        pad(p, origin, 2, 2, Blocks.MYCELIUM.defaultBlockState());
        for (int i = 0; i < 3 + rng.nextInt(2); i++) {
            int dx = rng.nextInt(3) - 1;
            int dz = rng.nextInt(3) - 1;
            int h = 3 + rng.nextInt(3);
            BlockPos stem = origin.offset(dx, 0, dz);
            for (int y = 0; y < h; y++) {
                p.set(stem.offset(0, y, 0), Blocks.MUSHROOM_STEM.defaultBlockState());
            }
            BlockState cap = rng.nextBoolean()
                    ? Blocks.RED_MUSHROOM_BLOCK.defaultBlockState()
                    : Blocks.BROWN_MUSHROOM_BLOCK.defaultBlockState();
            p.set(stem.offset(0, h, 0), cap);
        }
        p.set(origin.above(1), Blocks.SHROOMLIGHT.defaultBlockState());
    }

    /**
     * 39 幽匿神殿：仅 SCULK 岛生成的专属模板。
     * 由幽匿块基座 + 幽匿脉络铺地 + 幽匿感测体/催发体环绕 + 中央幽匿尖啸体构成，
     * 四角立柱点缀幽匿脉络，氛围死寂回响。
     */
    private static void sculkShrine(Placer p, BlockPos origin, RandomSource rng) {
        BlockState sculk = Blocks.SCULK.defaultBlockState();
        BlockState vein = Blocks.SCULK_VEIN.defaultBlockState();
        BlockState sensor = Blocks.SCULK_SENSOR.defaultBlockState();
        BlockState catalyst = Blocks.SCULK_CATALYST.defaultBlockState();
        BlockState shrieker = Blocks.SCULK_SHRIEKER.defaultBlockState();

        // 基座：3×3 幽匿块，表面覆一层幽匿脉络
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                p.set(origin.offset(dx, 0, dz), sculk);
                p.set(origin.offset(dx, 1, dz), vein);
            }
        }
        // 四角催发体
        int[] c = {-1, 1};
        for (int dx : c) {
            for (int dz : c) {
                p.set(origin.offset(dx, 1, dz), catalyst);
            }
        }
        // 四边中点感测体
        p.set(origin.offset(-1, 1, 0), sensor);
        p.set(origin.offset(1, 1, 0), sensor);
        p.set(origin.offset(0, 1, -1), sensor);
        p.set(origin.offset(0, 1, 1), sensor);
        // 中央尖啸体（核心），上方悬一段幽匿脉络
        p.set(origin, sculk);
        p.set(origin.above(1), shrieker);
        p.set(origin.above(2), vein);
        // 立柱：四角向上延伸幽匿块，顶端点脉络
        for (int dx : c) {
            for (int dz : c) {
                int h = 2 + rng.nextInt(2);
                for (int y = 2; y <= h; y++) {
                    p.set(origin.offset(dx, y, dz), sculk);
                }
                p.set(origin.offset(dx, h + 1, dz), vein);
            }
        }
    }

    /** 供枯树断枝用的水平随机方向（避免引 Direction 泛滥）。 */
    static Direction randomHorizontal(RandomSource rng) {
        return Direction.Plane.HORIZONTAL.getRandomDirection(rng);
    }
}
