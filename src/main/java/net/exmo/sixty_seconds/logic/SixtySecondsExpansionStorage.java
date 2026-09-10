package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.content.item.SixtySecondsUnlockItem;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.ItemContainerContents;
import net.minecraft.world.item.ItemStack;

/** Authoritative storage for an inventory expansion module. */
public final class SixtySecondsExpansionStorage {
    public static final int MAX_SLOTS = 27;

    private SixtySecondsExpansionStorage() {
    }

    public static boolean isModule(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SixtySecondsUnlockItem;
    }

    public static int capacity(ItemStack stack) {
        if (stack.getItem() instanceof SixtySecondsUnlockItem module) {
            return Math.min(MAX_SLOTS, Math.max(0, module.getUnlockSlots() * 9));
        }
        return 0;
    }

    public static NonNullList<ItemStack> read(ItemStack module) {
        NonNullList<ItemStack> result = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);
        if (isModule(module)) {
            module.getOrDefault(DataComponents.CONTAINER, ItemContainerContents.EMPTY).copyInto(result);
        }
        return result;
    }

    public static void write(ItemStack module, NonNullList<ItemStack> contents) {
        if (!isModule(module)) {
            return;
        }
        NonNullList<ItemStack> trimmed = NonNullList.withSize(MAX_SLOTS, ItemStack.EMPTY);
        int capacity = capacity(module);
        for (int i = 0; i < capacity && i < contents.size(); i++) {
            ItemStack stack = contents.get(i);
            trimmed.set(i, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
        }
        module.set(DataComponents.CONTAINER, ItemContainerContents.fromItems(trimmed));
    }

    public static boolean hasContents(ItemStack module) {
        if (!isModule(module)) {
            return false;
        }
        NonNullList<ItemStack> contents = read(module);
        int capacity = capacity(module);
        for (int i = 0; i < capacity; i++) {
            if (!contents.get(i).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
