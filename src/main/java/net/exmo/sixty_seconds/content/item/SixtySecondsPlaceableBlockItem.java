package net.exmo.sixty_seconds.content.item;

import net.exmo.sixty_seconds.bridge.AdventureUsable;
import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.logic.SixtySecondsBuildRules;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
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
        // 判定应基于“实际放置位置”，而非点到的方块：点在混凝土顶面时，放置位置在混凝土之上，
        // 其下方才是标记。同时兼容点可替换方块（放置位置即点击位置）的情况。
        BlockPos clickPos = context.getClickedPos();
        Direction face = context.getClickedFace();
        boolean allowed = SixtySecondsBuildRules.canPlaceAt(context.getLevel(), clickPos)
                || SixtySecondsBuildRules.canPlaceAt(context.getLevel(), clickPos.relative(face));
        if (!allowed && player != null && !context.getLevel().isClientSide()) {
            player.displayClientMessage(net.minecraft.network.chat.Component
                    .translatable("message.sixty_seconds.sixty_seconds.place_need_marker"), true);
        }
        return allowed;
    }
}
