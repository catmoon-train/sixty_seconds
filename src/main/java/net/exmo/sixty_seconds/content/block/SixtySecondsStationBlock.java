package net.exmo.sixty_seconds.content.block;

import net.exmo.sixty_seconds.logic.SixtySecondsRecipes;
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
 * 60s 专用合成台功能方块（简易工作台 / 厨房灶台 / 净化台）：
 * 与原版家具合成站（工作台/熔炉/炼药锅…）完全等价的<b>可携带版本</b>——右键打开对应合成站
 * 配方 GUI（{@link net.exmo.sixty_seconds.logic.SixtySecondsStations} 按
 * {@link SixtySecondsRecipes#stationOf} 识别），供避难所里没有对应原版家具时自行制作摆放。
 * 冒险模式仅可放在白色混凝土标记上方（{@code SixtySecondsPlaceableBlockItem}），扳手可拆除返还。
 * <p>
 * 交互方式对齐 SixtySeconds 的 {@code EntityInteractionBlock}：{@code useItemOn} 交给
 * {@code useWithoutItem}，后者<b>客户端返回 SUCCESS</b>（消费交互、确保右键包发送到服务端），
 * 服务端再执行 {@link SixtySecondsStations#serverOpen}。这样 60s 模式强制的冒险模式下也能稳定打开界面。
 */
public class SixtySecondsStationBlock extends Block {
    private final SixtySecondsRecipes.Station station;

    public SixtySecondsStationBlock(Properties properties, SixtySecondsRecipes.Station station) {
        super(properties);
        this.station = station;
    }

    public SixtySecondsRecipes.Station station() {
        return station;
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
