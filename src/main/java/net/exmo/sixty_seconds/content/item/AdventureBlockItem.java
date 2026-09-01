package net.exmo.sixty_seconds.content.item;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.AdventureUsable;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 可在冒险模式下放置的方块物品（放置限制同 {@link SixtySecondsTorchItem}）：
 * 创造模式或本模组活动期间允许放置，其余情况（冒险模式未激活时）禁止。
 */
public class AdventureBlockItem extends BlockItem implements AdventureUsable {
    public AdventureBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (!super.canPlace(context, state)) {
            return false;
        }
        Player player = context.getPlayer();
        return player != null && player.isCreative();
    }
}
