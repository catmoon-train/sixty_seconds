package net.exmo.sixty_seconds.mixin;

import com.mojang.datafixers.util.Pair;
import mcjty.lostcities.varia.ChunkCoord;
import mcjty.lostcities.worldgen.ChunkDriver;
import mcjty.lostcities.worldgen.ChunkHeightmap;
import mcjty.lostcities.worldgen.LostCityTerrainFeature;
import mcjty.lostcities.worldgen.lost.BuildingInfo;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BiomeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.server.level.WorldGenRegion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;

/**
 * 城市边缘的地表过渡修复。
 * <p>
 * Lost Cities 在城市边界（border 列）只铺边界方块，与野外地表衔接处会出现
 * 泥土/砂砾裸露的断层，观感生硬。本 mixin 在每条 border 列生成完成后，
 * 向街区外的一侧采样野外真实地表（顶层方块 + 下方垫层），把它作为“盖帽”
 * 写回边界列顶端，使城市地面与周边生物群系地表自然衔接。
 * <p>
 * 细节处理：
 * <ul>
 *   <li>只在 default / spheres 两种城市布局下生效，其他布局（太空、洞穴等）的
 *       地表语义完全不同，不适用；</li>
 *   <li>角点采样可能落在对角区块上，而 WorldGenRegion 对越界查询会抛异常——
 *       先用 accessor 检查目标区块是否在合法生成区域内，不可用时退回相邻的
 *       侧向区块，两侧都不可用则放弃（保持原样）；</li>
 *   <li>禁用泥土/砂砾/缠根泥土作为候选——它们正是断层观感的来源；候选不足时
 *       按生物群系兜底（海洋→砂/砂岩，河流→草方块，最终兜底石头）；</li>
 *   <li>写入位置从 groundY 向下吸附到第一个实体方块——7.4.x 的 groundY 有时
 *       指向草丛等非实体植被，直接写会把植物替换掉，留下悬空的盖帽；</li>
 *   <li>盖帽上方是水时放弃，避免在水下封出诡异的天窗；</li>
 *   <li>候选取样点用基于世界种子的确定性噪声挑选，同一位置永远得到同一结果，
 *       保证多次生成的区块边界一致。</li>
 * </ul>
 */
@Mixin(value = LostCityTerrainFeature.class, remap = false)
public class LostCityCityEdgeBlendMixin {

    @Inject(method = "generateBorder", at = @At("TAIL"), remap = false)
    private void ss$capBlendSurface(BuildingInfo info, boolean canDoParks, int x, int z,
                                    BuildingInfo adjacent, ChunkHeightmap heightmap, CallbackInfo ci) {
        if (!info.profile.isDefault() && !info.profile.isSpheres()) {
            return;
        }

        ChunkDriver driver = ss$getDriver();
        if (driver == null) {
            return;
        }

        WorldGenLevel world = info.provider.getWorld();
        if (world == null) {
            return;
        }

        int groundY = info.getCityGroundLevel();
        int minY = world.getMinBuildHeight();
        if (groundY <= minY + 1) {
            return;
        }

        ChunkCoord c = info.coord;
        int wx = (c.chunkX() << 4) + x;
        int wz = (c.chunkZ() << 4) + z;

        // 只处理真正贴边的列：x/z 为 0 或 15 时向外侧采样
        int dx = 0;
        int dz = 0;
        if (x == 0) dx = -1;
        else if (x == 15) dx = 1;
        if (z == 0) dz = -1;
        else if (z == 15) dz = 1;

        if (dx == 0 && dz == 0) {
            return;
        }

        int sx = wx + dx;
        int sz = wz + dz;

        // 角点可能探到不在本批生成区域内的对角区块：退回侧向，否则放弃
        if (!ss$hasChunkForBlock(world, sx, sz)) {
            if (dx != 0 && dz != 0) {
                if (ss$hasChunkForBlock(world, wx + dx, wz)) {
                    sz = wz;
                    dz = 0;
                } else if (ss$hasChunkForBlock(world, wx, wz + dz)) {
                    sx = wx;
                    dx = 0;
                } else {
                    return;
                }
            } else {
                return;
            }
        }

        Pair<BlockState, BlockState> candidate = ss$pickSurfaceCandidate(world, sx, sz, dx, dz, wx, wz);
        if (candidate == null || candidate.getFirst() == null) {
            return;
        }

        // groundY 有时指向非实体植被（草丛等）：向下吸附到第一个实体方块再写，
        // 否则会替换掉植物并留下悬空盖帽
        int placeY = groundY;
        while (placeY > minY + 1) {
            BlockPos probe = new BlockPos(wx, placeY, wz);
            BlockState probeState = world.getBlockState(probe);
            if (!world.getFluidState(probe).isEmpty()) {
                return;
            }
            if (!probeState.isAir() && !probeState.getCollisionShape(world, probe).isEmpty()) {
                break;
            }
            placeY--;
        }

        BlockPos capPos = new BlockPos(wx, placeY, wz);
        BlockPos above = capPos.above();
        if (world.getFluidState(above).is(Fluids.WATER)) {
            return;
        }

        driver.current(x, placeY, z).block(candidate.getFirst());

        if (candidate.getSecond() != null && placeY - 1 > minY) {
            driver.current(x, placeY - 1, z).block(candidate.getSecond());
        }
    }

    private ChunkDriver ss$getDriver() {
        try {
            return ((LostCityTerrainFeature) (Object) this).getDriver();
        } catch (Throwable ignored) {
            return null;
        }
    }

    /**
     * 在采样点及其步进方向上收集最多 4 个不重复的地表候选；
     * 无候选时按生物群系兜底，再不行退回石头（保证总是有东西可写）。
     */
    private static Pair<BlockState, BlockState> ss$pickSurfaceCandidate(WorldGenLevel world, int sampleX, int sampleZ,
                                                                        int dx, int dz, int placeX, int placeZ) {
        int stepX = dx == 0 ? 4 : dx * 4;
        int stepZ = dz == 0 ? 4 : dz * 4;

        List<Pair<BlockState, BlockState>> candidates = new ArrayList<>(4);
        ss$addCandidate(world, sampleX, sampleZ, candidates);
        ss$addCandidate(world, sampleX + stepX, sampleZ, candidates);
        ss$addCandidate(world, sampleX, sampleZ + stepZ, candidates);
        ss$addCandidate(world, sampleX + stepX, sampleZ + stepZ, candidates);

        if (candidates.isEmpty()) {
            Pair<BlockState, BlockState> biomeCandidate = ss$biomePaletteCandidate(world, sampleX, sampleZ);
            if (biomeCandidate != null) {
                return biomeCandidate;
            }
            return Pair.of(Blocks.STONE.defaultBlockState(), Blocks.STONE.defaultBlockState());
        }

        double noise = ss$surfaceNoise(world, placeX, placeZ);
        int idx = Mth.clamp((int) (noise * candidates.size()), 0, candidates.size() - 1);
        return candidates.get(idx);
    }

    /** 生物群系兜底：海洋用砂/砂岩，河流用草方块（会被禁用列表拦下时退石头）。 */
    private static Pair<BlockState, BlockState> ss$biomePaletteCandidate(WorldGenLevel world, int sampleX, int sampleZ) {
        var biomeHolder = world.getBiome(new BlockPos(sampleX, 0, sampleZ));

        try {
            if (biomeHolder.is(BiomeTags.IS_OCEAN)) {
                return Pair.of(Blocks.SAND.defaultBlockState(), Blocks.SANDSTONE.defaultBlockState());
            }
            if (biomeHolder.is(BiomeTags.IS_RIVER)) {
                BlockState top = Blocks.GRASS_BLOCK.defaultBlockState();
                if (ss$isForbiddenSurface(top)) {
                    top = Blocks.STONE.defaultBlockState();
                }
                return Pair.of(top, null);
            }
        } catch (Throwable ignored) {
            // 生物群系查询失败 → 返回 null 走石头兜底
        }

        return null;
    }

    private static void ss$addCandidate(WorldGenLevel world, int x, int z, List<Pair<BlockState, BlockState>> candidates) {
        if (!ss$hasChunkForBlock(world, x, z)) {
            return;
        }
        Pair<BlockState, BlockState> candidate = ss$sampleSurfaceCandidate(world, x, z);
        if (candidate == null || candidate.getFirst() == null) {
            return;
        }
        if (ss$isForbiddenSurface(candidate.getFirst())) {
            return;
        }
        for (Pair<BlockState, BlockState> existing : candidates) {
            if (existing.getFirst() == candidate.getFirst()) {
                return;
            }
        }
        candidates.add(candidate);
    }

    /** 泥土系地表正是断层观感的来源，全部排除在候选之外。 */
    private static boolean ss$isForbiddenSurface(BlockState state) {
        return state.is(Blocks.DIRT)
            || state.is(Blocks.COARSE_DIRT)
            || state.is(Blocks.ROOTED_DIRT)
            || state.is(Blocks.GRAVEL);
    }

    /**
     * 自上而下找采样点的实体顶层方块与其下的第一层不同方块（垫层）。
     * 采样路径上任何流体/越界异常都按“该点不可用”返回。
     */
    private static Pair<BlockState, BlockState> ss$sampleSurfaceCandidate(WorldGenLevel world, int x, int z) {
        int surfaceY;
        try {
            surfaceY = world.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        } catch (RuntimeException e) {
            return null;
        }
        int minY = world.getMinBuildHeight();

        BlockState top = null;
        BlockState under = null;

        int startY = surfaceY - 1;
        int bottomY = Math.max(minY + 1, startY - 32);
        for (int y = startY; y >= bottomY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state;
            try {
                state = world.getBlockState(pos);
            } catch (RuntimeException e) {
                return null;
            }
            if (!world.getFluidState(pos).isEmpty() || state.isAir() || state.getCollisionShape(world, pos).isEmpty()) {
                continue;
            }
            top = state;
            break;
        }

        if (top == null) {
            return null;
        }

        for (int y = startY - 1; y >= bottomY; y--) {
            BlockPos pos = new BlockPos(x, y, z);
            BlockState state;
            try {
                state = world.getBlockState(pos);
            } catch (RuntimeException e) {
                return Pair.of(top, null);
            }
            if (!world.getFluidState(pos).isEmpty() || state.isAir() || state.getCollisionShape(world, pos).isEmpty()) {
                continue;
            }
            if (state != top) {
                under = state;
                break;
            }
        }

        if (under != null && ss$isForbiddenSurface(under)) {
            under = null;
        }

        return Pair.of(top, under);
    }

    private static boolean ss$hasChunkForBlock(WorldGenLevel world, int blockX, int blockZ) {
        try {
            int chunkX = blockX >> 4;
            int chunkZ = blockZ >> 4;
            if (world instanceof WorldGenRegion region) {
                // hasChunk(int,int) 是官方公开的区域范围判定（越界返回 false 而非抛异常）。
                return region.hasChunk(chunkX, chunkZ);
            }
            return world.hasChunk(chunkX, chunkZ);
        } catch (Throwable t) {
            return false;
        }
    }

    /** 基于世界种子的确定性平滑噪声：同一坐标永远返回同一值，保证边界生成可复现。 */
    private static double ss$surfaceNoise(WorldGenLevel world, int x, int z) {
        long seed = world.getSeed();
        double noise1 = Mth.sin(x * 0.01f + seed * 0.001f) * 0.5 + 0.5;
        double noise2 = Mth.cos(z * 0.01f + seed * 0.001f) * 0.5 + 0.5;
        double noise3 = Mth.sin((x + z) * 0.007f + seed * 0.001f) * 0.5 + 0.5;

        return (noise1 + noise2 + noise3) / 3.0;
    }
}
