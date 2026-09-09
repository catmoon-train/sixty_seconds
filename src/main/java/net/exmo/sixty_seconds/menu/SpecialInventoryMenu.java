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

/**
 * Server-side menu for the Tarkov-style 60 Seconds inventory.
 *
 * <p>The first 36 slots are the normal player inventory, while slots 36..53
 * are backed by {@link SixtySecondsStatsComponent#extraInventory}.  The latter
 * are deliberately kept in the menu even when locked; locked slots are hidden
 * by the screen and rejected by the server-side slot checks.</p>
 */
public class SpecialInventoryMenu extends AbstractContainerMenu {
    public static final int PLAYER_MAIN_START = 0;
    public static final int PLAYER_MAIN_END = 27;
    public static final int EXTRA_START = 27;
    public static final int EXTRA_END = 45;
    public static final int HOTBAR_START = 45;
    public static final int HOTBAR_END = 54;
    public static final int ARMOR_START = 54;
    public static final int OFFHAND_SLOT = 58;

    private final Player player;
    private final SixtySecondsExtraInventory.ContainerView extra;

    public SpecialInventoryMenu(int id, Inventory inventory) {
        super(ModMenuTypes.SPECIAL_INVENTORY.get(), id);
        this.player = inventory.player;
        this.extra = new SixtySecondsExtraInventory.ContainerView(player);

        // Main inventory: the 27 normal backpack slots.
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new PlayerSlot(inventory, 9 + row * 9 + col,
                        216 + col * 18, 48 + row * 18));
            }
        }
        // Extra inventory: two additional rows, initially locked.
        for (int row = 0; row < 2; row++) {
            for (int col = 0; col < 9; col++) {
                int extraIndex = row * 9 + col;
                boolean visible = extraIndex < unlockedExtraSlots();
                addSlot(new ExtraSlot(extra, extraIndex,
                        visible ? 216 + col * 18 : -1000,
                        visible ? 102 + row * 18 : -1000, extraIndex));
            }
        }
        // Hotbar, placed below the equipment column by the client layout.
        for (int col = 0; col < 9; col++) {
            addSlot(new PlayerSlot(inventory, col, 18 + col * 18, 222));
        }
        for (int i = 0; i < 4; i++) {
            EquipmentSlot slot = switch (i) {
                case 0 -> EquipmentSlot.HEAD;
                case 1 -> EquipmentSlot.CHEST;
                case 2 -> EquipmentSlot.LEGS;
                default -> EquipmentSlot.FEET;
            };
            addSlot(new EquipmentPlayerSlot(inventory, 36 + (3 - i),
                    i % 2 == 0 ? 36 : 72,
                    146 + (i / 2) * 20, slot));
        }
        addSlot(new PlayerSlot(inventory, 40, 108, 156));
    }

    public Player getPlayer() {
        return player;
    }

    public int unlockedExtraSlots() {
        return Math.min(SixtySecondsInventoryLimit.MAX_EXTRA_UNLOCK,
                Math.max(0, SixtySecondsStatsComponent.KEY.get(player).extraUnlockedSlots));
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack original = source.getItem().copy();
        ItemStack moving = source.getItem();
        boolean moved;
        if (index >= EXTRA_START && index < EXTRA_END) {
            moved = moveItemStackTo(moving, PLAYER_MAIN_START, HOTBAR_END, false);
        } else {
            moved = moveItemStackTo(moving, EXTRA_START, EXTRA_END, false)
                    || moveItemStackTo(moving, PLAYER_MAIN_START, HOTBAR_END, false);
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
