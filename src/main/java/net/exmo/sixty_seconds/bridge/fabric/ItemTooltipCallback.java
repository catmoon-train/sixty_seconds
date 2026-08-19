package net.exmo.sixty_seconds.bridge.fabric;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

public interface ItemTooltipCallback {
    Event<ItemTooltipCallback> EVENT = new Event<>();

    void getTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag type, List<Component> lines);
}
