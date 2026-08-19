package net.exmo.sixty_seconds.content.item;

import net.exmo.sixty_seconds.bridge.AdventureUsable;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.logic.SixtySecondsBuildRules;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 60s 功能方块的物品：冒险模式可放置（{@link AdventureUsable}），
 * 但仅限本模式内且符合 {@link SixtySecondsBuildRules}（白色混凝土标记上方 2 格内）；
 * 创造模式不受限。用扳手（{@code sixty_seconds_wrench}）可拆除返还。
 */
public class SixtySecondsPlaceableBlockItem extends BlockItem implements AdventureUsable {

    public SixtySecondsPlaceableBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        if (!super.canPlace(context, state)) {
            return false;
        }
        Player player = context.getPlayer();
        if (player != null && player.isCreative()) {
            return true;
        }
        if (!SixtySecondsMod.isActive(context.getLevel())) {
            return false;
        }
        boolean allowed = SixtySecondsBuildRules.canPlaceAt(context.getLevel(), context.getClickedPos());
        if (!allowed && player != null && !context.getLevel().isClientSide()) {
            player.displayClientMessage(net.minecraft.network.chat.Component
                    .translatable("message.sixty_seconds.sixty_seconds.place_need_marker"), true);
        }
        return allowed;
    }
}
