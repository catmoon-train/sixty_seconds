package net.exmo.sixty_seconds.content.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * 撬箱起子：用于撬开上锁的物资箱（普通锁），每次撬锁消耗 1 点耐久。
 */
public class SixtySecondsBoxPryItem extends Item {
    public SixtySecondsBoxPryItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.sixty_seconds.sixty_seconds.box_pry")
                .withStyle(ChatFormatting.GRAY));
    }
}
