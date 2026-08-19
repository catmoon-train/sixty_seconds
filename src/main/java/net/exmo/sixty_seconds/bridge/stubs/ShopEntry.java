package net.exmo.sixty_seconds.bridge.stubs;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ShopEntry {
    private final ItemStack stack;

    public ShopEntry(ItemStack stack) {
        this.stack = stack;
    }

    public ItemStack stack() {
        return stack.isEmpty() ? Items.AIR.getDefaultInstance() : stack;
    }
}
