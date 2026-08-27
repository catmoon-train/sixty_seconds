package net.exmo.sixty_seconds.content.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/** 采掘剪刀：继承原版剪刀行为，统一加 tooltip，并声明「无法在庇护所内使用」。 */
public class MiningShearsItem extends ShearsItem {
    public MiningShearsItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.sixty_seconds.sixty_seconds.mining_tool").withStyle(ChatFormatting.GRAY));
    }
}
