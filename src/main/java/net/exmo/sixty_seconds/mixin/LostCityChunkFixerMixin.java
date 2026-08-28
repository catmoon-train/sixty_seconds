package net.exmo.sixty_seconds.mixin;

import mcjty.lostcities.worldgen.ChunkFixer;
import mcjty.lostcities.worldgen.LostCityTerrainFeature;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.IDimensionInfo;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.exmo.sixty_seconds.lostcities.SixtySecondsLostCitiesStarMap;
import net.exmo.sixty_seconds.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 在 LostCities 处理每一个城市区块时（{@code ChunkFixer.executePostTodo}，世界生成阶段）为建筑区块
 * 规划并延迟放置 60秒 物资箱。落箱逻辑完全在本 mixin 内完成，不依赖任何外部手动放置代码。
 *
 * <p>为什么用 mixin 而不是 NeoForge 事件：{@code executePostTodo} 在 chunk 生成时必然被调用（无论该区块
 * 是否自带 LostCities 战利品），因此本 mixin 对<b>所有</b>建筑区块都能可靠触发，不依赖事件总线的注册时机。
 * 物资箱真正落块仍走 LostCities 自己的延迟 todo（与 LostCities 的 loot/spawner 完全一致），
 * 保证方块与方块实体在真实 chunk 成型后才写入，不会被后续地形阶段覆盖。</p>
 */
@Mixin(ChunkFixer.class)
public class LostCityChunkFixerMixin {

    /** 普通/高级物资箱的抽类别池（与全局 loot 表的非空投 categories 对齐；airdrop 为空投专属，不在此处）。 */
    private static final String[] CATEGORIES = {"food", "water", "medicine", "tool", "material", "weapon"};

    /** 上锁箱比例。 */
    private static final float LOCK_RATIO = 0.7f;

    /** 每个 chunk 最多撒的箱子数（密度上限保护）。 */
    private static final int MAX_BOXES = 30;
    /** 密度倍率：实际撒箱数 = max(1, 星级) * DENSITY。 */
    private static final int DENSITY = 3;

    @Inject(method = "executePostTodo", at = @At("HEAD"))
    private static void sixty_seconds_planSupplyBoxes(ChunkCoord coord, IDimensionInfo provider, CallbackInfo ci) {
        BuildingInfo info = BuildingInfo.getBuildingInfo(coord, provider);
        if (info.getBuildingId() == null) {
            return; // 街道/空地：无建筑，交给 executePostTodo 内部判断即可
        }
        WorldGenLevel world = provider.getWorld();
        planSupplyBoxes(info, coord.chunkX(), coord.chunkZ(), world);
    }

    /**
     * 在 LostCities 处理每个城市区块（ChunkFixer.executePostTodo）时调用，
     * 为建筑区块登记撤离点、并按建筑星级规划并延迟放置 60秒 物资箱。
     * <p>
     * 必须经由 LostCities 的延迟 todo（ChunkFixer 阶段、真实 chunk 已成型时执行）放置，
     * 与 LostCities 自己的 loot/spawner 做法一致——否则箱子位置会被后续地形阶段覆盖而消失。
     */
    private static void planSupplyBoxes(BuildingInfo info, int chunkX, int chunkZ, WorldGenLevel world) {
        ResourceLocation id = info.getBuildingId();
        if (id == null) {
            return; // 街道/空地：无建筑
        }
        String name = id.getPath();
        // 撤离点建筑（evacuationpoint）在生成时登记其中心，供指南针/直升机撤离系统直接读取，
        // 避免在物品使用（主线程）时全图扫描 getChunkInfo 造成卡顿。
        if (name.toLowerCase().contains("evac")) {
            SixtySecondsLostCitiesStarMap.registerEvacuationPoint(world.getLevel(),
                    info.getCenter(info.getCityGroundLevel()));
        }
        // 物资箱专用星级：已知建筑用原映射；安全区/撤离点返回 0（不撒箱）；
        // 其余「位于城市建筑内但未登记」的建筑给默认星级——否则 LostCities 绝大多数建筑类型不在白名单里
        // 会被整栋跳过，导致「大多数建筑没有物资箱」。
        int star = SixtySecondsLostCitiesStarMap.lootStarForBuildingName(name);
        if (star <= 0) {
            return;
        }

        RandomSource rng = world.getRandom();
        int baseX = chunkX << 4;
        int baseZ = chunkZ << 4;

        // 楼层实际世界 Y 由 LostCities 的 BuildingInfo 给出（已含 cityLevel*FLOORHEIGHT）。
        int floorHeight = LostCityTerrainFeature.FLOORHEIGHT;
        int groundY = info.getCityGroundLevel();
        int bottomY = groundY - Math.max(0, info.cellars) * floorHeight; // 含地下室底
        int topY = groundY + Math.max(1, info.getNumFloors()) * floorHeight;

        // 收集本 chunk 内所有「不悬空」候选点：要求落脚点是空气、且脚下有实心支撑（不悬空）。
        // 必须显式校验落脚点为空气——否则候选点可能落在墙/地板实心块上，
        // ChunkFixer 阶段的 placeBox 会因落点非空气而跳过，最终一个箱子都放不出来。
        BlockPos[] reservoir = new BlockPos[MAX_BOXES];
        int seen = 0;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (int lx = 0; lx < 16; lx++) {
            for (int lz = 0; lz < 16; lz++) {
                int x = baseX + lx;
                int z = baseZ + lz;
                cursor.set(x, bottomY - 1, z);
                boolean belowAir = world.getBlockState(cursor).isAir();
                for (int y = bottomY; y <= topY; y++) {
                    cursor.set(x, y, z);
                    boolean hereAir = world.getBlockState(cursor).isAir();
                    if (hereAir && !belowAir) {
                        BlockPos hit = new BlockPos(x, y, z);
                        if (seen < MAX_BOXES) {
                            reservoir[seen] = hit;
                        } else {
                            int j = rng.nextInt(seen + 1);
                            if (j < MAX_BOXES) {
                                reservoir[j] = hit;
                            }
                        }
                        seen++;
                    }
                    // 本次的 pos 即下次迭代的 pos.below()，直接复用省掉一次 getBlockState
                    belowAir = hereAir;
                }
            }
        }
        if (seen == 0) {
            return;
        }

        int count = Math.min(MAX_BOXES, Math.max(1, star) * DENSITY); // 密度倍率，显著提升物资箱密度
        int take = Math.min(count, Math.min(seen, MAX_BOXES));
        // 蓄水池内的顺序本身已是随机的，直接取前 take 个
        for (int i = 0; i < take; i++) {
            BlockPos pos = reservoir[i];
            boolean advanced = rng.nextFloat() < (0.1f + 0.12f * star);
            boolean locked = rng.nextFloat() < LOCK_RATIO;
            String category = CATEGORIES[rng.nextInt(CATEGORIES.length)];
            info.addPostTodo(pos, () -> placeBox(info, pos, advanced, locked, category, name, star));
        }
    }

    /**
     * 在 ChunkFixer 阶段（真实 chunk 已成型、所有地形生成完成）放置物资箱。
     * 必须先 setBlock 再 setBlockEntityNbt，保证方块与方块实体状态一致，避免 "does not allow it" 的 WARN。
     */
    private static void placeBox(BuildingInfo info, BlockPos pos, boolean advanced, boolean locked, String category, String buildingName, int starLevel) {
        WorldGenLevel inWorld = info.provider.getWorld();
        // 二次校验：延迟到 ChunkFixer 才执行。同一区块在重新生成/并行生成竞态下 executePostTodo 可能被
        // 再次触发；此时落点已被上一轮放好的箱子占据（非空气），直接跳过即可，既避免重复落箱，也避免重复刷日志。
        // 注意：本校验必须放在日志之前，否则即便跳过也会打印「物资箱落点」造成后台刷屏。
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
}
