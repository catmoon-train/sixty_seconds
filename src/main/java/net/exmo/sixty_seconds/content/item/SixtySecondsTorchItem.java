package net.exmo.sixty_seconds.content.item;

import net.exmo.sixty_seconds.bridge.AdventureUsable;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.StandingAndWallBlockItem;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.List;

/**
 * 火把（放置原版火把方块）：照亮家中黑暗角落防止夜间刷低语怪 / 清晨黑暗惩罚
 * （见 {@link net.exmo.sixty_seconds.logic.SixtySecondsWhisperSystem}）。
 * 占 1 格物资；冒险模式可放置，仅限本模式（不限白色混凝土——照明需要自由摆放）。
 */
public class SixtySecondsTorchItem extends StandingAndWallBlockItem implements AdventureUsable {

    public SixtySecondsTorchItem(Properties properties) {
        super(Blocks.TORCH, Blocks.WALL_TORCH, properties, Direction.DOWN);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.item.Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.sixty_seconds.sixty_seconds.sleep_avoid_unease").withStyle(net.minecraft.ChatFormatting.GRAY));
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (!super.canPlace(context, state)) {
            return false;
        }
        Player player = context.getPlayer();
        return (player != null && player.isCreative()) || SixtySecondsMod.isActive(context.getLevel());
    }
}
