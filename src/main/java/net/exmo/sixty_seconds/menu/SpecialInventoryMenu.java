package net.exmo.sixty_seconds.menu;

import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.logic.SixtySecondsExtraInventory;
import net.exmo.sixty_seconds.logic.SixtySecondsInventoryLimit;
import net.exmo.sixty_seconds.registry.ModMenuTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;

/**
 * Server-side menu for the Tarkov-style 60 Seconds inventory.
 *
 * <p>The first 27 slots are the normal player backpack.  Unlocked extension
 * slots are then backed by {@link SixtySecondsStatsComponent#extraInventory};
 * locked extension slots are not added to the menu at all, so PetiteInventory
 * cannot mistake hidden slots for real grid cells.</p>
 */
public class SpecialInventoryMenu extends AbstractContainerMenu {
    public static final int PLAYER_MAIN_START = 0;
    public static final int PLAYER_MAIN_END = 27;
    public static final int EXTRA_START = 27;

    private final Player player;
    /**
     * One logical 54-slot backpack.  PetiteInventory must see the vanilla
     * 27 slots and the expansion slots as one container/grid; exposing them as
     * two containers makes its layered grid remapping disagree with the
     * server menu slot indices and causes ghost items.
     */
    private final BackpackContainer backpack;
    /** The server-authoritative number of extension slots in this menu. */
    private final int unlockedExtraSlots;

    public SpecialInventoryMenu(int id, Inventory inventory) {
        this(id, inventory, getUnlockedExtraSlots(inventory.player));
    }

    public SpecialInventoryMenu(int id, Inventory inventory, int unlockedExtraSlots) {
        super(ModMenuTypes.SPECIAL_INVENTORY.get(), id);
        this.player = inventory.player;
        this.backpack = new BackpackContainer(player);
        this.unlockedExtraSlots = clampUnlockedExtraSlots(unlockedExtraSlots);

        // The old inventory limiter uses barrier stacks as temporary locks.
        // They are not real items and must not occupy the 27 base backpack
        // slots in the new menu.
        for (int slot = 9; slot <= 35; slot++) {
            if (inventory.getItem(slot).is(net.minecraft.world.item.Items.BARRIER)) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }

        // The normal backpack and unlocked expansion slots are one continuous
        // 9-column grid backed by real storage.  Locked slots are not added.
        int visibleBackpackSlots = PLAYER_MAIN_END + this.unlockedExtraSlots;
        for (int row = 0; row < Math.ceil(visibleBackpackSlots / 9.0); row++) {
            for (int col = 0; col < 9; col++) {
                int backpackIndex = row * 9 + col;
                if (backpackIndex >= visibleBackpackSlots) {
                    break;
                }
                addSlot(new BackpackSlot(backpack, backpackIndex,
                        190 + col * 18, 39 + row * 18));
            }
        }
        // Hotbar, placed below the equipment column by the client layout.
        for (int col = 0; col < 9; col++) {
            addSlot(new PlayerSlot(inventory, col, 14 + col * 18, 194));
        }
        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = switch (i) {
                case 0 -> EquipmentSlot.HEAD;
                case 1 -> EquipmentSlot.CHEST;
                case 2 -> EquipmentSlot.LEGS;
                default -> EquipmentSlot.FEET;
            };
            addSlot(new EquipmentPlayerSlot(inventory, 36 + (3 - i),
                    i % 2 == 0 ? 103 : 139,
                    44 + (i / 2) * 38, slot));
        }
        addSlot(new PlayerSlot(inventory, 40, 139, 120));
    }

    /** Factory used by NeoForge's menu packet; the count is sent by the server. */
    public static SpecialInventoryMenu fromNetwork(int id, Inventory inventory,
                                                    RegistryFriendlyByteBuf buffer) {
        int unlocked = buffer == null
                ? getUnlockedExtraSlots(inventory.player)
                : buffer.readVarInt();
        return new SpecialInventoryMenu(id, inventory, unlocked);
    }

    public static int getUnlockedExtraSlots(Player player) {
        return clampUnlockedExtraSlots(
                SixtySecondsStatsComponent.KEY.get(player).extraUnlockedSlots);
    }

    private static int clampUnlockedExtraSlots(int slots) {
        return Math.min(SixtySecondsInventoryLimit.MAX_EXTRA_UNLOCK, Math.max(0, slots));
    }

    public Player getPlayer() {
        return player;
    }

    public int unlockedExtraSlots() {
        return unlockedExtraSlots;
    }

    public int extraEnd() {
        return EXTRA_START + unlockedExtraSlots();
    }

    public int hotbarStart() {
        return extraEnd();
    }

    public int hotbarEnd() {
        return hotbarStart() + 9;
    }

    public int armorStart() {
        return hotbarEnd();
    }

    public int offhandSlot() {
        return armorStart() + 4;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack original = source.getItem().copy();
        ItemStack moving = source.getItem();
        boolean moved;
        if (index >= EXTRA_START && index < extraEnd()) {
            moved = moveItemStackTo(moving, PLAYER_MAIN_START, PLAYER_MAIN_END, false)
                    || moveItemStackTo(moving, hotbarStart(), hotbarEnd(), false);
        } else if (index >= PLAYER_MAIN_START && index < PLAYER_MAIN_END) {
            // Never include the source main-inventory range in its own target.
            moved = moveItemStackTo(moving, EXTRA_START, extraEnd(), false)
                    || moveItemStackTo(moving, hotbarStart(), hotbarEnd(), false);
        } else if (index >= hotbarStart() && index < hotbarEnd()) {
            // Never include the source hotbar range in its own target.
            moved = moveItemStackTo(moving, EXTRA_START, extraEnd(), false)
                    || moveItemStackTo(moving, PLAYER_MAIN_START, PLAYER_MAIN_END, false);
        } else {
            moved = moveItemStackTo(moving, EXTRA_START, extraEnd(), false)
                    || moveItemStackTo(moving, PLAYER_MAIN_START, PLAYER_MAIN_END, false)
                    || moveItemStackTo(moving, hotbarStart(), hotbarEnd(), false);
        }
        if (!moved) return ItemStack.EMPTY;
        if (moving.isEmpty()) source.setByPlayer(ItemStack.EMPTY);
        else source.setChanged();
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == this.player && player.isAlive();
    }

    private final class PlayerSlot extends Slot {
        PlayerSlot(Inventory inventory, int index, int x, int y) {
            super(inventory, index, x, y);
        }
    }

    private final class EquipmentPlayerSlot extends Slot {
        private final EquipmentSlot equipmentSlot;

        EquipmentPlayerSlot(Inventory inventory, int index, int x, int y, EquipmentSlot equipmentSlot) {
            super(inventory, index, x, y);
            this.equipmentSlot = equipmentSlot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return stack.canEquip(equipmentSlot, player);
        }

        @Override
        public int getMaxStackSize() {
            return 1;
        }
    }

    private static final class BackpackSlot extends Slot {
        BackpackSlot(BackpackContainer container, int index, int x, int y) {
            super(container, index, x, y);
        }
    }

    /** Maps menu backpack index 0..53 to vanilla 9..35 and extra 0..26. */
    private static final class BackpackContainer implements Container {
        private static final int VANILLA_MAIN_SIZE = 27;
        private static final int TOTAL_SIZE = VANILLA_MAIN_SIZE + SixtySecondsExtraInventory.SIZE;

        private final Player player;
        private final SixtySecondsExtraInventory.ContainerView extra;

        BackpackContainer(Player player) {
            this.player = player;
            this.extra = new SixtySecondsExtraInventory.ContainerView(player);
        }

        @Override
        public int getContainerSize() {
            return TOTAL_SIZE;
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < TOTAL_SIZE; i++) {
                if (!getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            if (slot < 0 || slot >= TOTAL_SIZE) {
                return ItemStack.EMPTY;
            }
            return slot < VANILLA_MAIN_SIZE
                    ? player.getInventory().getItem(9 + slot)
                    : extra.getItem(slot - VANILLA_MAIN_SIZE);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            if (slot < 0 || slot >= TOTAL_SIZE) {
                return ItemStack.EMPTY;
            }
            return slot < VANILLA_MAIN_SIZE
                    ? player.getInventory().removeItem(9 + slot, amount)
                    : extra.removeItem(slot - VANILLA_MAIN_SIZE, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            if (slot < 0 || slot >= TOTAL_SIZE) {
                return ItemStack.EMPTY;
            }
            return slot < VANILLA_MAIN_SIZE
                    ? player.getInventory().removeItemNoUpdate(9 + slot)
                    : extra.removeItemNoUpdate(slot - VANILLA_MAIN_SIZE);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot < 0 || slot >= TOTAL_SIZE) {
                return;
            }
            // A Slot may pass a mutable stack object that is still used by
            // the carried stack or another client-side menu mirror.  Store an
            // owned copy so one click cannot mutate two views of the item.
            ItemStack owned = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            if (slot < VANILLA_MAIN_SIZE) {
                player.getInventory().setItem(9 + slot, owned);
            } else {
                extra.setItem(slot - VANILLA_MAIN_SIZE, owned);
            }
        }

        @Override
        public void setChanged() {
            player.getInventory().setChanged();
        }

        @Override
        public void clearContent() {
            for (int i = 0; i < VANILLA_MAIN_SIZE; i++) {
                player.getInventory().setItem(9 + i, ItemStack.EMPTY);
            }
            extra.clearContent();
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
