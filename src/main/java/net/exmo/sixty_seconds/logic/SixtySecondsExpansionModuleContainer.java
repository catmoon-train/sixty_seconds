package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.NonNullList;

/**
 * One-slot container that stores the equipped expansion module on the player
 * component. It extends Inventory deliberately: PetiteInventory excludes
 * Inventory-backed slots from its storage grid, so the socket remains a normal
 * one-slot menu slot instead of being mistaken for a multi-cell backpack slot.
 */
public final class SixtySecondsExpansionModuleContainer
        extends net.minecraft.world.entity.player.Inventory {
    private final Player player;

    public SixtySecondsExpansionModuleContainer(Player player) {
        super(player);
        this.player = player;
    }

    private SixtySecondsStatsComponent stats() {
        return SixtySecondsStatsComponent.KEY.get(player);
    }

    @Override public int getContainerSize() { return 1; }
    @Override public boolean isEmpty() { return stats().expansionModule.isEmpty(); }
    @Override public ItemStack getItem(int slot) {
        return slot == 0 ? stats().expansionModule.copy() : ItemStack.EMPTY;
    }
    @Override public ItemStack removeItem(int slot, int amount) {
        if (slot != 0 || stats().expansionModule.isEmpty()) return ItemStack.EMPTY;
        ItemStack current = stats().expansionModule;
        ItemStack result = current.split(amount);
        if (current.isEmpty()) stats().expansionModule = ItemStack.EMPTY;
        setChanged();
        return result;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        if (slot != 0) return ItemStack.EMPTY;
        ItemStack result = stats().expansionModule.copy();
        stats().expansionModule = ItemStack.EMPTY;
        SixtySeconds.LOGGER.info(
                "[60s][ExpansionDebug] module container removeItemNoUpdate: player={}, removed={}, "
                        + "componentAfter=EMPTY",
                player.getGameProfile().getName(), describe(result));
        setChanged();
        return result;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot != 0) return;
        SixtySeconds.LOGGER.info(
                "[60s][ExpansionDebug] module container setItem: player={}, incoming={}, "
                        + "componentBefore={}",
                player.getGameProfile().getName(), describe(stack), describe(stats().expansionModule));
        SixtySecondsStatsComponent stats = stats();
        if (SixtySecondsExpansionStorage.isModule(stack)) {
            ItemStack equipped = stack.copy();
            // One-time migration from the old component-backed extension.
            // Never keep two authoritative copies: once a module is equipped,
            // the legacy list is emptied even if it was already stale.
            if (!SixtySecondsExpansionStorage.hasContents(equipped)) {
                NonNullList<ItemStack> contents = SixtySecondsExpansionStorage.read(equipped);
                int capacity = SixtySecondsExpansionStorage.capacity(equipped);
                for (int i = 0; i < stats.extraInventory.size(); i++) {
                    ItemStack legacy = stats.extraInventory.get(i);
                    if (legacy.isEmpty()) continue;
                    boolean moved = false;
                    for (int target = 0; target < capacity; target++) {
                        if (contents.get(target).isEmpty()) {
                            contents.set(target, legacy.copy());
                            moved = true;
                            break;
                        }
                    }
                    if (!moved && player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                        serverPlayer.drop(legacy.copy(), false);
                    }
                }
                SixtySecondsExpansionStorage.write(equipped, contents);
            }
            for (int i = 0; i < stats.extraInventory.size(); i++) {
                stats.extraInventory.set(i, ItemStack.EMPTY);
            }
            stats.expansionModule = equipped;
        } else {
            stats.expansionModule = ItemStack.EMPTY;
        }
        SixtySeconds.LOGGER.info(
                "[60s][ExpansionDebug] module container setItem finished: player={}, "
                        + "componentAfter={}, capacity={}, hasContents={}, extraInventoryNonEmpty={}",
                player.getGameProfile().getName(), describe(stats.expansionModule),
                SixtySecondsExpansionStorage.capacity(stats.expansionModule),
                SixtySecondsExpansionStorage.hasContents(stats.expansionModule),
                stats.extraInventory.stream().anyMatch(item -> !item.isEmpty()));
        setChanged();
    }
    @Override public void setChanged() {
        player.getInventory().setChanged();
        stats().sync();
    }
    @Override public void clearContent() {
        stats().expansionModule = ItemStack.EMPTY;
        setChanged();
    }
    @Override public void startOpen(Player player) { }
    @Override public void stopOpen(Player player) { }
    @Override public boolean stillValid(Player player) { return player == this.player; }

    private static String describe(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "EMPTY";
        }
        return stack.getItem() + "x" + stack.getCount()
                + "[cap=" + SixtySecondsExpansionStorage.capacity(stack)
                + ",contents=" + SixtySecondsExpansionStorage.hasContents(stack) + "]";
    }
}
