package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.server.level.ServerPlayer;

/** The 27 slots that extend the vanilla 27-slot backpack to 54. */
public final class SixtySecondsExtraInventory {
    public static final int SIZE = 27;

    private SixtySecondsExtraInventory() {
    }

    public static NonNullList<ItemStack> slots(Player player) {
        return SixtySecondsStatsComponent.KEY.get(player).extraInventory;
    }

    /** Container view used by the server menu and by the client menu mirror. */
    public static final class ContainerView implements Container {
        private final Player player;

        public ContainerView(Player player) {
            this.player = player;
        }

        private NonNullList<ItemStack> items() {
            return slots(player);
        }

        @Override
        public int getContainerSize() {
            return SIZE;
        }

        @Override
        public boolean isEmpty() {
            return items().stream().allMatch(ItemStack::isEmpty);
        }

        @Override
        public ItemStack getItem(int slot) {
            return slot >= 0 && slot < SIZE ? items().get(slot) : ItemStack.EMPTY;
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            if (slot < 0 || slot >= SIZE) return ItemStack.EMPTY;
            ItemStack result = items().get(slot).split(amount);
            setChanged();
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            if (slot < 0 || slot >= SIZE) return ItemStack.EMPTY;
            ItemStack result = items().get(slot).copy();
            items().set(slot, ItemStack.EMPTY);
            setChanged();
            return result;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot < 0 || slot >= SIZE) return;
            items().set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            setChanged();
        }

        @Override
        public void setChanged() {
            player.getInventory().setChanged();
            if (player instanceof ServerPlayer) {
                SixtySecondsStatsComponent.KEY.get(player).sync();
            }
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < SIZE; i++) items().set(i, ItemStack.EMPTY);
            setChanged();
        }

        @Override
        public void startOpen(Player player) {
        }

        @Override
        public void stopOpen(Player player) {
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }
    }
}
