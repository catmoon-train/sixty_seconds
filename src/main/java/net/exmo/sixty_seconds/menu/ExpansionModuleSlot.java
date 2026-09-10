package net.exmo.sixty_seconds.menu;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.logic.SixtySecondsExpansionStorage;
import net.minecraft.core.registries.BuiltInRegistries;
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
        SixtySeconds.LOGGER.info(
                "[60s][ExpansionDebug] socket click: player={}, side={}, menu={}, menuSlot={}, "
                        + "button={}, carried={}, stored={}, carriedIsModule={}, storedHasContents={}",
                player.getGameProfile().getName(),
                player.level().isClientSide ? "client" : "server",
                menu.getClass().getSimpleName(),
                menu.slots.indexOf(slot),
                button,
                describe(carried),
                describe(stored),
                SixtySecondsExpansionStorage.isModule(carried),
                SixtySecondsExpansionStorage.hasContents(stored));
        if (player.level().isClientSide) {
            // The client menu must not mutate the component-backed module.
            // AbstractContainerScreen will still send the click packet, and
            // the server will perform the authoritative write below.  If the
            // client predicts this write, the client shows a fake module while
            // the server still has the module on the carried stack.
            SixtySeconds.LOGGER.info(
                    "[60s][ExpansionDebug] client prediction suppressed; waiting for server result");
            return true;
        }
        if (carried.isEmpty()) {
            if (stored.isEmpty() || !slot.mayPickup(player)) {
                SixtySeconds.LOGGER.warn(
                        "[60s][ExpansionDebug] socket take rejected: storedEmpty={}, mayPickup={}, stored={}",
                        stored.isEmpty(), slot.mayPickup(player), describe(stored));
                return true;
            }
            menu.setCarried(slot.container.removeItemNoUpdate(slot.getContainerSlot()));
            slot.setChanged();
            SixtySeconds.LOGGER.info(
                    "[60s][ExpansionDebug] socket take accepted: carriedAfter={}, storedAfter={}",
                    describe(menu.getCarried()), describe(slot.getItem()));
            return true;
        }
        boolean mayPlace = slot.mayPlace(carried);
        if (!stored.isEmpty() || !mayPlace) {
            SixtySeconds.LOGGER.warn(
                    "[60s][ExpansionDebug] socket place rejected: storedEmpty={}, mayPlace={}, "
                            + "isModule={}, carried={}, stored={}",
                    stored.isEmpty(), mayPlace, SixtySecondsExpansionStorage.isModule(carried),
                    describe(carried), describe(stored));
            return true;
        }

        ItemStack equipped = carried.split(1);
        SixtySeconds.LOGGER.info(
                "[60s][ExpansionDebug] writing module to socket: equipped={}, carriedBefore={}, "
                        + "containerSlot={}",
                describe(equipped), describe(carried), slot.getContainerSlot());
        slot.set(equipped);
        slot.setChanged();
        menu.setCarried(carried.isEmpty() ? ItemStack.EMPTY : carried);
        SixtySeconds.LOGGER.info(
                "[60s][ExpansionDebug] socket place finished: carriedAfter={}, storedAfter={}, "
                        + "storedHasContents={}, capacity={}",
                describe(menu.getCarried()), describe(slot.getItem()),
                SixtySecondsExpansionStorage.hasContents(slot.getItem()),
                SixtySecondsExpansionStorage.capacity(slot.getItem()));
        return true;
    }

    private static String describe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "EMPTY";
        }
        return BuiltInRegistries.ITEM.getKey(stack.getItem())
                + "x" + stack.getCount()
                + "[cap=" + SixtySecondsExpansionStorage.capacity(stack)
                + ",contents=" + SixtySecondsExpansionStorage.hasContents(stack) + "]";
    }
}
