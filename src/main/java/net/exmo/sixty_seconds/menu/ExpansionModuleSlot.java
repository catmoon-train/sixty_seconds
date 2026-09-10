package net.exmo.sixty_seconds.menu;

import net.exmo.sixty_seconds.logic.SixtySecondsExpansionStorage;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** The single expansion-module socket shared by both inventory menus. */
public final class ExpansionModuleSlot extends Slot {
    public ExpansionModuleSlot(Container container, int x, int y) {
        super(container, 0, x, y);
    }

    @Override
    public boolean mayPlace(ItemStack stack) {
        return SixtySecondsExpansionStorage.isModule(stack)
                && !SixtySecondsExpansionStorage.hasContents(getItem());
    }

    @Override
    public boolean mayPickup(Player player) {
        return !SixtySecondsExpansionStorage.hasContents(getItem()) && super.mayPickup(player);
    }

    @Override
    public int getMaxStackSize() {
        return 1;
    }
}
