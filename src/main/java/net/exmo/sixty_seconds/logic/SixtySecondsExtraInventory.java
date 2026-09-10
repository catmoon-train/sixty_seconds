package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.SixtySeconds;
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
        SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
        return SixtySecondsExpansionStorage.isModule(stats.expansionModule)
                ? SixtySecondsExpansionStorage.read(stats.expansionModule)
                : stats.extraInventory;
    }

    /** Container view used by the server menu and by the client menu mirror. */
    public static final class ContainerView implements Container {
        private final Player player;

        public ContainerView(Player player) {
            this.player = player;
        }

        private NonNullList<ItemStack> items() {
            SixtySecondsStatsComponent stats = SixtySecondsStatsComponent.KEY.get(player);
            return SixtySecondsExpansionStorage.isModule(stats.expansionModule)
                    ? SixtySecondsExpansionStorage.read(stats.expansionModule)
                    : stats.extraInventory;
        }

        private boolean moduleBacked() {
            return SixtySecondsExpansionStorage.isModule(
                    SixtySecondsStatsComponent.KEY.get(player).expansionModule);
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
            if (slot < 0 || slot >= SIZE) return ItemStack.EMPTY;
            NonNullList<ItemStack> items = items();
            return items.get(slot).copy();
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            if (slot < 0 || slot >= SIZE) return ItemStack.EMPTY;
            NonNullList<ItemStack> items = items();
            ItemStack before = items.get(slot).copy();
            boolean backed = moduleBacked();
            ItemStack result = items.get(slot).split(amount);
            if (backed) saveModule(items);
            SixtySeconds.LOGGER.info(
                    "[60s][ExpansionDebug] ExtraInventory.removeItem: player={}, side={}, "
                            + "extraIndex={}, amount={}, moduleBacked={}, before={}, removed={}, after={}",
                    player.getGameProfile().getName(), player.level().isClientSide ? "client" : "server",
                    slot, amount, backed, describe(before), describe(result), describe(items.get(slot)));
            setChanged();
            return result;
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            if (slot < 0 || slot >= SIZE) return ItemStack.EMPTY;
            NonNullList<ItemStack> items = items();
            ItemStack before = items.get(slot).copy();
            boolean backed = moduleBacked();
            ItemStack result = items.get(slot).copy();
            items.set(slot, ItemStack.EMPTY);
            if (backed) saveModule(items);
            SixtySeconds.LOGGER.info(
                    "[60s][ExpansionDebug] ExtraInventory.removeItemNoUpdate: player={}, side={}, "
                            + "extraIndex={}, moduleBacked={}, before={}, removed={}, after=EMPTY",
                    player.getGameProfile().getName(), player.level().isClientSide ? "client" : "server",
                    slot, backed, describe(before), describe(result));
            setChanged();
            return result;
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot < 0 || slot >= SIZE) return;
            NonNullList<ItemStack> items = items();
            ItemStack before = items.get(slot).copy();
            boolean backed = moduleBacked();
            SixtySeconds.LOGGER.info(
                    "[60s][ExpansionDebug] ExtraInventory.setItem: player={}, side={}, "
                            + "extraIndex={}, moduleBacked={}, before={}, incoming={}",
                    player.getGameProfile().getName(), player.level().isClientSide ? "client" : "server",
                    slot, backed, describe(before), describe(stack));
            items.set(slot, stack.isEmpty() ? ItemStack.EMPTY : stack.copy());
            if (backed) saveModule(items);
            SixtySeconds.LOGGER.info(
                    "[60s][ExpansionDebug] ExtraInventory.setItem finished: player={}, "
                            + "extraIndex={}, after={}, moduleComponent={}",
                    player.getGameProfile().getName(), slot, describe(items.get(slot)),
                    describe(SixtySecondsStatsComponent.KEY.get(player).expansionModule));
            setChanged();
        }

        private void saveModule(NonNullList<ItemStack> items) {
            SixtySecondsExpansionStorage.write(
                    SixtySecondsStatsComponent.KEY.get(player).expansionModule, items);
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
            NonNullList<ItemStack> items = items();
            for (int i = 0; i < SIZE; i++) items.set(i, ItemStack.EMPTY);
            if (moduleBacked()) saveModule(items);
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

        private static String describe(ItemStack stack) {
            if (stack == null || stack.isEmpty()) return "EMPTY";
            return stack.getItem() + "x" + stack.getCount();
        }
    }
}
