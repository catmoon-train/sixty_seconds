package net.exmo.sixty_seconds.menu;

import net.exmo.sixty_seconds.logic.SixtySecondsExpansionStorage;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
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

    /**
     * Handles the socket with the ordinary container carried-stack rules.
     * PetiteInventory's custom placement packet only understands the vanilla
     * PlayerInventory and cannot write this component-backed slot.
     */
    public static boolean handleClick(AbstractContainerMenu menu, ExpansionModuleSlot slot,
                                      int button, ClickType clickType, Player player) {
        if (clickType != ClickType.PICKUP || (button != 0 && button != 1)) {
            return false;
        }
        ItemStack carried = menu.getCarried();
        ItemStack stored = slot.getItem();
        if (carried.isEmpty()) {
            if (stored.isEmpty() || !slot.mayPickup(player)) {
                return true;
            }
            menu.setCarried(slot.container.removeItemNoUpdate(slot.getContainerSlot()));
            slot.setChanged();
            return true;
        }
        if (!stored.isEmpty() || !slot.mayPlace(carried)) {
            return true;
        }

        ItemStack equipped = carried.split(1);
        slot.set(equipped);
        slot.setChanged();
        menu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        return true;
    }
}
