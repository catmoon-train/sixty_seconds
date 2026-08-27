package net.exmo.sixty_seconds.content.item;

import net.exmo.sixty_seconds.bridge.AdventureUsable;
import net.exmo.sixty_seconds.logic.SixtySecondsDailyEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 铺路方块 / 铺路灯笼等物品：冒险模式可放置（配合 CAN_PLACE_ON 组件），
 * <b>不受白混凝土标记限制</b>，但额外禁止在庇护所（含住宅）范围内放置。
 * 即在庇护所外的任意合法位置均可摆放。
 */
public class SixtySecondsShelterPlaceableBlockItem extends BlockItem implements AdventureUsable {
    public SixtySecondsShelterPlaceableBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    protected boolean canPlace(BlockPlaceContext context, BlockState state) {
        // 先走原版放置合法性（能否存活 / 碰撞是否阻挡），再叠加庇护所限制
        if (!super.canPlace(context, state)) {
            return false;
        }
        Player player = context.getPlayer();
        if (player instanceof ServerPlayer serverPlayer
                && SixtySecondsDailyEvents.isPlayerInShelter(serverPlayer)) {
            if (!context.getLevel().isClientSide()) {
                player.displayClientMessage(Component.translatable(
                        "message.sixty_seconds.sixty_seconds.place_disabled_in_shelter").withStyle(ChatFormatting.RED), true);
            }
            return false;
        }
        return true;
    }
}
