package net.exmo.sixty_seconds.mixin;

import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * 暴露 {@link WorldGenRegion} 内部的生成区域边界。
 * <p>
 * 城市边缘混合需要在邻区块采样地表方块；WorldGenRegion 对越界查询会直接抛异常，
 * 因此先通过这里拿到合法区块范围，再决定是否采样（或退回到相邻的侧向区块）。
 */
@Mixin(WorldGenRegion.class)
public interface WorldGenRegionAccessor {

    @Accessor("firstPos")
    ChunkPos ss$getFirstPos();

    @Accessor("lastPos")
    ChunkPos ss$getLastPos();
}
