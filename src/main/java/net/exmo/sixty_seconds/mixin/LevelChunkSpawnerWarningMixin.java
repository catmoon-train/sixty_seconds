package net.exmo.sixty_seconds.mixin;

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
            BlockState current = this.getBlockState(blockEntity.getBlockPos());
            if (!current.is(blockEntity.getBlockState().getBlock())) {
                ci.cancel();
            }
        }
    }
}
