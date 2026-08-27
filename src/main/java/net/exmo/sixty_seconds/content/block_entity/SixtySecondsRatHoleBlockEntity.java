package net.exmo.sixty_seconds.content.block_entity;

import net.exmo.sixty_seconds.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 老鼠洞方块实体：仅记录「上一次掏洞的游戏时刻」，用于实现「每天只能掏一次」的冷却。
 */
public class SixtySecondsRatHoleBlockEntity extends BlockEntity {
    private long lastLootTick = 0L;

    public SixtySecondsRatHoleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlocks.SIXTY_SECONDS_RAT_HOLE_ENTITY, pos, state);
    }

    public long getLastLootTick() {
        return lastLootTick;
    }

    public void setLastLootTick(long value) {
        this.lastLootTick = value;
        setChanged();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("LastLootTick", lastLootTick);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        this.lastLootTick = tag.contains("LastLootTick") ? tag.getLong("LastLootTick") : 0L;
    }
}
