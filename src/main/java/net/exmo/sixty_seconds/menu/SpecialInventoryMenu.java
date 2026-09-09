package net.exmo.sixty_seconds.menu;

import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.logic.SixtySecondsExtraInventory;
import net.exmo.sixty_seconds.logic.SixtySecondsInventoryLimit;
import net.exmo.sixty_seconds.registry.ModMenuTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
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
    private final SixtySecondsExtraInventory.ContainerView extra;
    /** The server-authoritative number of extension slots in this menu. */
    private final int unlockedExtraSlots;

    public SpecialInventoryMenu(int id, Inventory inventory) {
        this(id, inventory, getUnlockedExtraSlots(inventory.player));
    }

    public SpecialInventoryMenu(int id, Inventory inventory, int unlockedExtraSlots) {
        super(ModMenuTypes.SPECIAL_INVENTORY.get(), id);
        this.player = inventory.player;
        this.extra = new SixtySecondsExtraInventory.ContainerView(player);
        this.unlockedExtraSlots = clampUnlockedExtraSlots(unlockedExtraSlots);

        // The old inventory limiter uses barrier stacks as temporary locks.
        // They are not real items and must not occupy the 27 base backpack
        // slots in the new menu.
        for (int slot = 9; slot <= 35; slot++) {
            if (inventory.getItem(slot).is(net.minecraft.world.item.Items.BARRIER)) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }

        // Main inventory: the 27 normal backpack slots.  PetiteInventory
        // treats these coordinates as a real 9x3 grid; the 18px spacing is
        // intentionally left untouched for its footprint/click mixins.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new PlayerSlot(inventory, 9 + row * 9 + col,
                        190 + col * 18, 39 + row * 18));
            }
        }
        // Only unlocked extra slots are added to the menu.  Locked slots must
        // not be represented by fake/off-screen coordinates because PetiteInventory
        // builds its grid from every storage Slot it sees.
        int unlocked = this.unlockedExtraSlots;
        for (int extraIndex = 0; extraIndex < unlocked; extraIndex++) {
            int row = extraIndex / 9;
            int col = extraIndex % 9;
            addSlot(new ExtraSlot(extra, extraIndex,
                    190 + col * 18, 93 + row * 18, extraIndex));
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

    private final class ExtraSlot extends Slot {
        private final int extraIndex;

        ExtraSlot(SixtySecondsExtraInventory.ContainerView container, int index,
                int x, int y, int extraIndex) {
            super(container, index, x, y);
            this.extraIndex = extraIndex;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return extraIndex < unlockedExtraSlots() && !stack.isEmpty();
        }

        @Override
        public boolean mayPickup(Player player) {
            return extraIndex < unlockedExtraSlots();
        }
    }
}
