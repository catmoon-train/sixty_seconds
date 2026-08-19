package net.exmo.sixty_seconds.bridge.minigame;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.awt.Color;

public interface TaskInstinctShowableInterface {
    int taskInstinctId();

    boolean shouldRenderTaskInstinct(Level level, BlockState state, BlockPos pos, Player player);

    Color taskInstinctRenderColor(BlockState state, BlockPos pos, Player player);
}
