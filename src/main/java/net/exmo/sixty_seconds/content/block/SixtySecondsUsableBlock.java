package net.exmo.sixty_seconds.content.block;

import net.exmo.sixty_seconds.logic.SixtySecondsStations;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 普通功能方块（研究台 / 拆解台）的基类：交互方式对齐 SixtySeconds 的
 * {@code EntityInteractionBlock}——{@code useItemOn} 交给 {@code useWithoutItem}，
 * 后者<b>客户端返回 SUCCESS</b>（消费交互、确保右键包发送到服务端），服务端再执行
 * {@link SixtySecondsStations#serverOpen}。这样 60s 模式强制的冒险模式下也能稳定打开界面。
 */
public class SixtySecondsUsableBlock extends Block {
    public SixtySecondsUsableBlock(Properties properties) {
        super(properties);
    }

    @Override
    public ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hitResult) {
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        return SixtySecondsStations.serverOpen(player, level, state, hitResult);
    }
}
