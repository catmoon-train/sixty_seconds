package net.exmo.sixty_seconds.menu;

import com.sighs.petiteinventory.api.ItemArea;
import com.sighs.petiteinventory.api.PetiteInventoryApi;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.logic.SixtySecondsExtraInventory;
import net.exmo.sixty_seconds.logic.SixtySecondsExpansionModuleContainer;
import net.exmo.sixty_seconds.logic.SixtySecondsExpansionStorage;
import net.exmo.sixty_seconds.logic.SixtySecondsInventoryLimit;
import net.exmo.sixty_seconds.registry.ModMenuTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.Container;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
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
    private final SixtySecondsExpansionModuleContainer moduleContainer;
    private final ExpansionModuleSlot moduleSlot;

    public SpecialInventoryMenu(int id, Inventory inventory) {
        this(id, inventory, getUnlockedExtraSlots(inventory.player));
    }

    public SpecialInventoryMenu(int id, Inventory inventory, int unlockedExtraSlots) {
        super(ModMenuTypes.SPECIAL_INVENTORY.get(), id);
        this.player = inventory.player;
        this.unlockedExtraSlots = clampUnlockedExtraSlots(unlockedExtraSlots);
        this.backpack = new BackpackContainer(player, this.unlockedExtraSlots);
        this.moduleContainer = new SixtySecondsExpansionModuleContainer(player);

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
        this.moduleSlot = (ExpansionModuleSlot) addSlot(
                new ExpansionModuleSlot(moduleContainer, 139, 158));
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
        return clampUnlockedExtraSlots(SixtySecondsExpansionStorage.capacity(
                SixtySecondsStatsComponent.KEY.get(player).expansionModule));
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
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        // Do not let the client run AbstractContainerMenu's local prediction
        // for the component-backed backpack.  PetiteInventory may also try
        // to predict a multi-cell click, which used to result in the same
        // stack being written to the expansion component while it remained
        // on the cursor.  The packet is still sent by AbstractContainerScreen
        // after this method returns; the server executes the real click and
        // sends the authoritative result back to the client.
        if (player.level().isClientSide
                && slotId >= 0 && slotId < slots.size()
                && slots.get(slotId) != moduleSlot) {
            return;
        }
        if (slotId >= 0 && slotId < slots.size() && slots.get(slotId) == moduleSlot
                ) {
            if (ExpansionModuleSlot.handleClick(this, moduleSlot, button, clickType, player)) {
                broadcastChanges();
                return;
            }
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        if (index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot source = slots.get(index);
        if (!source.hasItem() || !source.mayPickup(player)) return ItemStack.EMPTY;
        if (source instanceof ExpansionModuleSlot) return ItemStack.EMPTY;
        ItemStack original = source.getItem().copy();
        ItemStack moving = source.getItem();
        boolean moved;
        if (index >= EXTRA_START && index < extraEnd()) {
            moved = moveIntoBackpack(moving, PLAYER_MAIN_START, PLAYER_MAIN_END)
                    || moveItemStackTo(moving, hotbarStart(), hotbarEnd(), false);
        } else if (index >= PLAYER_MAIN_START && index < PLAYER_MAIN_END) {
            // Never include the source main-inventory range in its own target.
            moved = moveIntoBackpack(moving, EXTRA_START, extraEnd())
                    || moveItemStackTo(moving, hotbarStart(), hotbarEnd(), false);
        } else if (index >= hotbarStart() && index < hotbarEnd()) {
            // Never include the source hotbar range in its own target.
            moved = moveIntoBackpack(moving, EXTRA_START, extraEnd())
                    || moveIntoBackpack(moving, PLAYER_MAIN_START, PLAYER_MAIN_END);
        } else {
            moved = moveIntoBackpack(moving, EXTRA_START, extraEnd())
                    || moveIntoBackpack(moving, PLAYER_MAIN_START, PLAYER_MAIN_END)
                    || moveItemStackTo(moving, hotbarStart(), hotbarEnd(), false);
        }
        if (!moved) return ItemStack.EMPTY;
        // ContainerView#getItem returns an owned copy when the backpack is
        // module-backed.  Calling only setChanged() would therefore leave
        // the original source stack untouched after a partial merge and
        // duplicate the transferred items on the next sync.
        source.setByPlayer(moving.isEmpty() ? ItemStack.EMPTY : moving);
        return original;
    }

    /**
     * Moves a stack into the backpack while respecting PetiteInventory's
     * footprint.  Only the anchor cell stores the stack; the other cells are
     * occupied logically by its ItemArea and must remain empty in storage.
     */
    private boolean moveIntoBackpack(ItemStack moving, int start, int end) {
        if (moving.isEmpty() || start >= end) return false;

        // First merge into existing stacks.  Merging does not need a new
        // footprint because the destination already owns one.
        boolean moved = false;
        for (int i = start; i < end && !moving.isEmpty(); i++) {
            Slot target = slots.get(i);
            ItemStack existing = target.getItem();
            if (existing.isEmpty()
                    || !ItemStack.isSameItemSameComponents(existing, moving)
                    || !target.mayPlace(moving)) {
                continue;
            }
            int max = Math.min(target.getMaxStackSize(), existing.getMaxStackSize());
            int amount = Math.min(moving.getCount(), Math.max(0, max - existing.getCount()));
            if (amount <= 0) continue;
            existing.grow(amount);
            target.setByPlayer(existing);
            moving.shrink(amount);
            moved = true;
        }
        if (moving.isEmpty()) return true;

        ItemArea area = PetiteInventoryApi.getItemArea(moving);
        int width = Math.max(1, area.width());
        int height = Math.max(1, area.height());
        for (int anchor = start; anchor < end; anchor++) {
            if (!fitsBackpackArea(anchor, start, end, width, height)) continue;
            Slot target = slots.get(anchor);
            if (!target.mayPlace(moving) || !target.getItem().isEmpty()) continue;

            target.setByPlayer(moving.copy());
            moving.setCount(0);
            return true;
        }
        return moved;
    }

    private boolean fitsBackpackArea(int anchor, int rangeStart, int rangeEnd,
                                     int width, int height) {
        if (anchor < rangeStart || anchor >= rangeEnd || width > 9) return false;
        int anchorRow = anchor / 9;
        int anchorCol = anchor % 9;
        for (int dy = 0; dy < height; dy++) {
            for (int dx = 0; dx < width; dx++) {
                int col = anchorCol + dx;
                int logical = anchor + dy * 9 + dx;
                if (col >= 9 || logical < rangeStart || logical >= rangeEnd
                        || logical >= extraEnd()) {
                    return false;
                }
                if (isBackpackCellOccupied(logical, anchor)) return false;
            }
        }
        return anchorRow + height <= (extraEnd() + 8) / 9;
    }

    /** Checks logical occupancy, including the invisible cells of existing footprints. */
    private boolean isBackpackCellOccupied(int logical, int ignoredAnchor) {
        for (int other = PLAYER_MAIN_START; other < extraEnd(); other++) {
            ItemStack existing = slots.get(other).getItem();
            if (existing.isEmpty()) continue;
            ItemArea area = PetiteInventoryApi.getItemArea(existing);
            int width = Math.max(1, area.width());
            int height = Math.max(1, area.height());
            int row = other / 9;
            int col = other % 9;
            int logicalRow = logical / 9;
            int logicalCol = logical % 9;
            if (logicalRow >= row && logicalRow < row + height
                    && logicalCol >= col && logicalCol < col + width) {
                return true;
            }
        }
        return false;
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

        @Override
        public boolean mayPlace(ItemStack stack) {
            return !SixtySecondsExpansionStorage.isModule(stack)
                    && !(stack.getItem() instanceof net.exmo.sixty_seconds.content.item.SixtySecondsBackpackItem);
        }
    }

    /** Maps menu backpack index 0..53 to vanilla 9..35 and extra 0..26. */
    private static final class BackpackContainer implements Container {
        private static final int VANILLA_MAIN_SIZE = 27;

        private final Player player;
        private final SixtySecondsExtraInventory.ContainerView extra;
        private final int extensionSize;

        BackpackContainer(Player player, int extensionSize) {
            this.player = player;
            this.extra = new SixtySecondsExtraInventory.ContainerView(player);
            this.extensionSize = extensionSize;
        }

        private int totalSize() {
            return VANILLA_MAIN_SIZE + extensionSize;
        }

        @Override
        public int getContainerSize() {
            return totalSize();
        }

        @Override
        public boolean isEmpty() {
            for (int i = 0; i < totalSize(); i++) {
                if (!getItem(i).isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int slot) {
            if (slot < 0 || slot >= totalSize()) {
                return ItemStack.EMPTY;
            }
            return slot < VANILLA_MAIN_SIZE
                    ? player.getInventory().getItem(9 + slot)
                    : extra.getItem(slot - VANILLA_MAIN_SIZE);
        }

        @Override
        public ItemStack removeItem(int slot, int amount) {
            if (slot < 0 || slot >= totalSize()) {
                return ItemStack.EMPTY;
            }
            return slot < VANILLA_MAIN_SIZE
                    ? player.getInventory().removeItem(9 + slot, amount)
                    : extra.removeItem(slot - VANILLA_MAIN_SIZE, amount);
        }

        @Override
        public ItemStack removeItemNoUpdate(int slot) {
            if (slot < 0 || slot >= totalSize()) {
                return ItemStack.EMPTY;
            }
            return slot < VANILLA_MAIN_SIZE
                    ? player.getInventory().removeItemNoUpdate(9 + slot)
                    : extra.removeItemNoUpdate(slot - VANILLA_MAIN_SIZE);
        }

        @Override
        public void setItem(int slot, ItemStack stack) {
            if (slot < 0 || slot >= totalSize()) {
                return;
            }
            // A Slot may pass a mutable stack object that is still used by
            // the carried stack or another client-side menu mirror.  Store an
            // owned copy so one click cannot mutate two views of the item.
            ItemStack owned = stack.isEmpty() ? ItemStack.EMPTY : stack.copy();
            if (slot < VANILLA_MAIN_SIZE) {
                // A footprint is never allowed to be split across the
                // native and module-backed halves. If PetiteInventory's
                // anchor is on the last native row and its footprint would
                // cross the seam, route the complete stack into the module.
                // This is done at the authoritative container boundary, so
                // it also applies to shift-click and server automation.
                if (routeCrossBoundaryToModule(slot, owned)) {
                    return;
                }
                player.getInventory().setItem(9 + slot, owned);
            } else {
                extra.setItem(slot - VANILLA_MAIN_SIZE, owned);
            }
        }

        private boolean routeCrossBoundaryToModule(int slot, ItemStack stack) {
            if (stack.isEmpty() || extensionSize <= 0) return false;
            ItemArea area = PetiteInventoryApi.getItemArea(stack);
            int width = Math.max(1, area.width());
            int height = Math.max(1, area.height());
            int row = slot / 9;
            int col = slot % 9;
            boolean crosses = false;
            for (int dy = 0; dy < height; dy++) {
                for (int dx = 0; dx < width; dx++) {
                    if (row + dy >= 3) {
                        crosses = true;
                    }
                }
            }
            if (!crosses) return false;
            int target = findModuleAnchor(width, height);
            if (target < 0) return true;
            player.getInventory().setItem(9 + slot, ItemStack.EMPTY);
            extra.setItem(target - VANILLA_MAIN_SIZE, stack);
            return true;
        }

        private int findModuleAnchor(int width, int height) {
            int rows = extensionSize / 9;
            for (int row = 0; row + height <= rows; row++) {
                for (int col = 0; col + width <= 9; col++) {
                    int anchor = row * 9 + col;
                    boolean free = true;
                    for (int dy = 0; dy < height && free; dy++) {
                        for (int dx = 0; dx < width; dx++) {
                            if (!extra.getItem(anchor + dy * 9 + dx).isEmpty()) {
                                free = false;
                                break;
                            }
                        }
                    }
                    if (free) return VANILLA_MAIN_SIZE + anchor;
                }
            }
            return -1;
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
