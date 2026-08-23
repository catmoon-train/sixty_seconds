package net.exmo.sixty_seconds.lostcities;

import mcjty.lostcities.api.LostCityEvent;
import mcjty.lostcities.setup.Registration;
import mcjty.lostcities.worldgen.LostCityTerrainFeature;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.exmo.sixty_seconds.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.common.NeoForge;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * 让 LostCities 在生成建筑时按建筑星级自动撒 60秒 物资箱：
 * <ul>
 *   <li>星级（1..5）越高，撒的数量越多、高级箱占比越大；</li>
 *   <li>约 70% 为上锁箱（{@code *_LOCKED} 变体），需撬锁；</li>
 *   <li>箱子贴地面生成（脚下实心、头顶空气），且水平至少 2 侧为空气，避免贴墙体/悬空/贴天花板；</li>
 *   <li>任意两箱水平间距 ≥ 4 格，避免扎堆过密；</li>
 *   <li>无星级 / 撤离点 / 安全区（{@code star <= 0}）不生成。</li>
 * </ul>
 * 在 {@code CharacteristicsEvent} 缓存本 chunk 的建筑名与城市海拔，到 {@code PostGenCityChunkEvent}
 * （建筑已生成完、primer 可写）时按星级把箱子写进 primer，整段流程只在 chunk 生成时发生一次，天然不重复。
 */
public final class SixtySecondsLostCityLootGen {

    /** 普通/高级物资箱的抽类别池（与全局 loot 表的非空投 categories 对齐；airdrop 为空投专属，不在此处）。 */
    private static final String[] CATEGORIES = {"food", "water", "medicine", "tool", "material", "weapon"};

    /** 上锁箱比例。 */
    private static final float LOCK_RATIO = 0.7f;

    /** 任意两箱水平最小间距的平方（格）。 */
    private static final int MIN_SPACING_SQ = 4 * 4;

    /** 每个 chunk 最多撒的箱子数（随星级线性增长，这里给出上限保护）。 */
    private static final int MAX_BOXES = 15;

    private SixtySecondsLostCityLootGen() {
    }

    public static void register() {
        // 不再依赖 CharacteristicsEvent：它的 buildingType 只在「建筑主 chunk」有值，
        // 导致跨多 chunk 的多层建筑只有主 chunk 那一格能撒箱，其余楼层全无 —— 现象就是
        // 「多层 buildings 反而没生成物资箱」。改用 PostGen 时直接反查 BuildingInfo。
        NeoForge.EVENT_BUS.addListener(SixtySecondsLostCityLootGen::onPostGen);
    }

    @SubscribeEvent
    private static void onPostGen(LostCityEvent.PostGenCityChunkEvent event) {
        WorldGenLevel world = event.getWorld();
        int chunkX = event.getChunkX();
        int chunkZ = event.getChunkZ();

        IDimensionInfo dimInfo = Registration.LOSTCITY_FEATURE.get().getDimensionInfo(world);
        if (dimInfo == null) {
            return;
        }
        ChunkCoord coord = new ChunkCoord(dimInfo.getType(), chunkX, chunkZ);
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, dimInfo);

        // 直接在 PostGen 阶段用 BuildingInfo 反查本 chunk 所属建筑名。
        // BuildingInfo.buildingType 是从建筑主 chunk 继承下来的（多区块建筑所有 chunk 共享），
        // 因此无论本 chunk 是不是「主 chunk」都能拿到正确的建筑名 —— 解决多层建筑漏箱问题。
        ResourceLocation id = info.getBuildingId();
        if (id == null) {
            return; // 街道/空地：无建筑
        }
        String name = id.getPath();
        // 星级 ≤ 0（无星级 / 撤离点 UNGRADED / 安全区 SAFE_STAR）不生成物资箱
        int star = SixtySecondsLostCitiesStarMap.starForBuildingName(name);
        if (star <= 0) {
            return;
        }

        ChunkAccess primer = event.getChunkAccess();
        RandomSource rng = world.getRandom();
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        // 楼层实际世界 Y 由 LostCities 的 BuildingInfo 给出（已含 cityLevel*FLOORHEIGHT），
        // 不要再自己用 cityLevel*6 近似，否则要么扫不到建筑、要么把箱子撒到建筑外地表。
        // 必须用 LostCities 真实的 FLOORHEIGHT（6），之前误写成 8 会导致多层建筑楼层错位，
        // 越往上层 isFloor 匹配率越低，最终高层整层扫不到合法落箱点。
        int floorHeight = LostCityTerrainFeature.FLOORHEIGHT;
        int groundY = info.getCityGroundLevel();
        int bottomY = groundY - Math.max(0, info.cellars) * floorHeight; // 含地下室底
        int topY = groundY + Math.max(1, info.getNumFloors()) * floorHeight;

        // 收集本 chunk 内所有合法地板候选点（贴地、非贴墙、不悬空/不天花板）
        List<BlockPos> candidates = new ArrayList<>();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = baseX + lx;
                int z = baseZ + lz;
                for (int y = bottomY - 1; y <= topY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (isFloor(primer, pos) && hasOpenSides(primer, pos)) {
                        candidates.add(pos);
                    }
                }
            }
        }
        if (candidates.isEmpty()) {
            return;
        }

        // 用 worldgen 随机源打乱候选顺序，保证分布随机且确定性可复现
        Collections.shuffle(candidates, new Random(rng.nextLong()));

        int count = Math.min(star, MAX_BOXES);
        List<BlockPos> placed = new ArrayList<>();
        for (BlockPos pos : candidates) {
            if (placed.size() >= count) {
                break;
            }
            if (tooClose(placed, pos)) {
                continue;
            }
            // 关键：不能现在直接写 primer —— PostGenCityChunkEvent 之后 LostCities 还会跑
            // generateRuins / generateRubble / generateStuff 等阶段，会把箱子位置覆盖成
            // stone/dirt/air/water，导致最终方块实体与方块不匹配而刷 WARN，且箱子消失。
            // 改走 LostCities 的延迟 todo（ChunkFixer 阶段、真实 chunk 已成型时执行），
            // 与 LostCities 自己的 loot/spawner 做法一致：先 setBlock 再 setBlockEntityNbt。
            boolean advanced = rng.nextFloat() < (0.1f + 0.12f * star);
            boolean locked = rng.nextFloat() < LOCK_RATIO;
            String category = CATEGORIES[rng.nextInt(CATEGORIES.length)];
            info.addPostTodo(pos, () -> placeBox(info, pos, advanced, locked, category));
            placed.add(pos);
        }
    }

    /**
     * 在 ChunkFixer 阶段（真实 chunk 已成型、所有地形生成完成）放置物资箱。
     * 必须先 setBlock 再 setBlockEntityNbt，保证方块与方块实体状态一致，避免 "does not allow it" 的 WARN。
     */
    private static void placeBox(BuildingInfo info, BlockPos pos, boolean advanced, boolean locked, String category) {
        WorldGenLevel inWorld = info.provider.getWorld();
        // 二次校验：延迟到 ChunkFixer 才执行，若此时脚下已不是实心（地形被后续阶段改动），
        // 放下去会悬空，直接跳过该候选点。
        if (!inWorld.getBlockState(pos).isAir() || inWorld.getBlockState(pos.below()).isAir()) {
            return;
        }
        Block block = advanced
                ? (locked ? ModBlocks.SIXTY_SECONDS_SUPPLY_BOX_ADVANCED_LOCKED : ModBlocks.SIXTY_SECONDS_SUPPLY_BOX_ADVANCED)
                : (locked ? ModBlocks.SIXTY_SECONDS_SUPPLY_BOX_LOCKED : ModBlocks.SIXTY_SECONDS_SUPPLY_BOX);
        // 先放方块，再挂方块实体（状态匹配）
        inWorld.setBlock(pos, block.defaultBlockState(), Block.UPDATE_ALL);
        CompoundTag tag = new CompoundTag();
        tag.putString("id", BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(ModBlocks.SIXTY_SECONDS_SUPPLY_BOX_ENTITY).toString());
        tag.putInt("x", pos.getX());
        tag.putInt("y", pos.getY());
        tag.putInt("z", pos.getZ());
        tag.putString("Category", category);
        tag.putInt("BonusRolls", 1);
        tag.putBoolean("OneShot", false);
        tag.putBoolean("Unlocked", !locked); // 上锁箱保持上锁，未锁箱显式开放
        inWorld.getChunk(pos).setBlockEntityNbt(tag);
    }

    /** primer 中该格是否为合法落箱点：当前空气、脚下实心、头顶空气（贴地面，不悬空/不天花板）。 */
    private static boolean isFloor(ChunkAccess primer, BlockPos pos) {
        if (!primer.getBlockState(pos).isAir()) {
            return false;
        }
        if (primer.getBlockState(pos.below()).isAir()) {
            return false;
        }
        return primer.getBlockState(pos.above()).isAir();
    }

    /** 放置格水平四向至少 2 侧为空气：避免物资箱卡在墙体/窗台角落。 */
    private static boolean hasOpenSides(ChunkAccess primer, BlockPos pos) {
        int open = 0;
        for (Direction d : Direction.Plane.HORIZONTAL) {
            if (primer.getBlockState(pos.relative(d)).isAir()) {
                open++;
            }
        }
        return open >= 2;
    }

    /** 与已放置点水平距离过近（< 4 格）则返回 true。 */
    private static boolean tooClose(List<BlockPos> placed, BlockPos pos) {
        for (BlockPos p : placed) {
            int dx = p.getX() - pos.getX();
            int dz = p.getZ() - pos.getZ();
            if (dx * dx + dz * dz < MIN_SPACING_SQ) {
                return true;
            }
        }
        return false;
    }
}
