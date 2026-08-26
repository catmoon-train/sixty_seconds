package net.exmo.sixty_seconds.mixin;

import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;



@Mixin(LevelChunk.class)
public abstract class LevelChunkSpawnerWarningMixin {

    @Inject(
        method = "setBlockEntity(Lnet/minecraft/world/level/block/entity/BlockEntity;)V",
        at = @At("HEAD"),
        cancellable = true
    )
    private void sixtySecondsSuppressSpawnerMismatchWarning(BlockEntity blockEntity, CallbackInfo ci) {
        if (blockEntity instanceof SpawnerBlockEntity) {
            // 注意：mixin 中 this 的类型是 mixin 类本身，并非 LevelChunk，需转型后访问其方法。
            LevelChunk self = (LevelChunk) (Object) this;
            BlockState current = self.getBlockState(blockEntity.getBlockPos());
            if (!current.is(Blocks.SPAWNER)) {
                ci.cancel();
            }
        }
    }
}
