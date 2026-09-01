package net.exmo.sixty_seconds.menu;

import net.exmo.sixty_seconds.content.block_entity.SupplyBoxBlockEntity;
import net.exmo.sixty_seconds.content.item.SixtySecondsLootMagnifierItem;
import net.exmo.sixty_seconds.registry.ModMenuTypes;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * 物资箱容器搜刮形式的菜单。箱内物资以放大镜占位，左键搜刮后由服务端替换为真实战利品。
 * <p>
 * 服务端构造时直接复用方块实体的 {@link SupplyBoxBlockEntity#getSearchItems()} 列表（持久化、可同步）；
 * 客户端构造时用一个空的同尺寸列表，物品由服务端通过菜单槽位同步下发。
 */
public class SupplySearchMenu extends AbstractContainerMenu {
    public static final int CONTAINER_ROWS = 3;
    public static final int CONTAINER_COLS = 9;
    public static final int CONTAINER_SIZE = CONTAINER_ROWS * CONTAINER_COLS;

    private final Container container;
    private final SupplyBoxBlockEntity be;

    private SupplySearchMenu(int syncId, Inventory inv, SupplyBoxBlockEntity be, NonNullList<ItemStack> items) {
        super(ModMenuTypes.SUPPLY_SEARCH.get(), syncId);
        this.be = be;
        this.container = new SearchContainer(be, items);
        int slot = 0;
        for (int r = 0; r < CONTAINER_ROWS; r++) {
            for (int c = 0; c < CONTAINER_COLS; c++) {
                this.addSlot(new SearchSlot(this.container, slot++, 8 + c * 18, 18 + r * 18));
            }
        }
        // 玩家背包
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 9; c++) {
                this.addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
            }
        }
        // 快捷栏
        for (int c = 0; c < 9; c++) {
            this.addSlot(new Slot(inv, c, 8 + c * 18, 142));
        }
    }

    /** 服务端创建。 */
    public static SupplySearchMenu createServer(int syncId, Inventory inv, SupplyBoxBlockEntity be) {
        return new SupplySearchMenu(syncId, inv, be, be.getSearchItems());
    }

    /** 客户端重建（无方块实体，仅用于接收同步）。 */
    public SupplySearchMenu(int syncId, Inventory inv) {
        this(syncId, inv, null, NonNullList.withSize(CONTAINER_SIZE, ItemStack.EMPTY));
    }

    public SupplyBoxBlockEntity getBlockEntity() {
        return be;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = this.slots.get(index);
        if (slot == null || !slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack item = slot.getItem();
        // 放大镜禁止移动
        if (SixtySecondsLootMagnifierItem.isMagnifier(item)) {
            return ItemStack.EMPTY;
        }
        ItemStack original = item.copy();
        if (index < CONTAINER_SIZE) {
            // 从箱内移动到玩家背包
            if (!this.moveItemStackTo(item, CONTAINER_SIZE, this.slots.size(), true)) {
                return ItemStack.EMPTY;
            }
            slot.setByPlayer(item.isEmpty() ? ItemStack.EMPTY : item);
        } else {
            // 不允许把物品放进物资箱
            return ItemStack.EMPTY;
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (be == null) {
            return true;
        }
        // 玩家离箱超过 8 格（64 平方）则关闭界面
        return player.distanceToSqr(be.getBlockPos().getCenter()) <= 64.0;
    }

    /** 放大镜槽位：不可取出、不可放入，仅可被左键点击触发搜刮（搜刮由界面层拦截处理）。 */
    static class SearchSlot extends Slot {
        SearchSlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPickup(Player player) {
            return !SixtySecondsLootMagnifierItem.isMagnifier(getItem());
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return false;
        }
    }

    /** 以方块实体物品列表为后端存储的容器；服务端下同步改动会写回并标记方块实体已变更。 */
    static class SearchContainer implements Container {
        private final SupplyBoxBlockEntity be;
        private final NonNullList<ItemStack> items;

        SearchContainer(SupplyBoxBlockEntity be, NonNullList<ItemStack> items) {
            this.be = be;
            this.items = items;
        }

        @Override
        public int getContainerSize() {
            return items.size();
        }

        @Override
        public boolean isEmpty() {
            for (ItemStack s : items) {
                if (!s.isEmpty()) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public ItemStack getItem(int i) {
            return items.get(i);
        }

        @Override
        public ItemStack removeItem(int i, int c) {
            ItemStack s = items.get(i);
            if (s.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack out = s.split(c);
            if (s.isEmpty()) {
                items.set(i, ItemStack.EMPTY);
            }
            onChanged();
            return out;
        }

        @Override
        public ItemStack removeItemNoUpdate(int i) {
            ItemStack s = items.get(i);
            items.set(i, ItemStack.EMPTY);
            return s;
        }

        @Override
        public void setItem(int i, ItemStack s) {
            items.set(i, s);
            onChanged();
        }

        @Override
        public void setChanged() {
            onChanged();
        }

        @Override
        public void clearContent() {
            items.clear();
        }

        @Override
        public void startOpen(Player p) {
        }

        @Override
        public void stopOpen(Player p) {
        }

        @Override
        public boolean stillValid(Player player) {
            return true;
        }

        private void onChanged() {
            if (be != null) {
                be.setChanged();
                // 一次性箱：箱内物资全部被取走后移除方块（参考旧版搜刮完成后的移除逻辑）
                if (be.isOneShot() && be.isSearchItemsEmpty()) {
                    be.removeOneShotBox();
                }
            }
        }
    }
}
