package net.exmo.sixty_seconds.logic;

import net.exmo.sixty_seconds.bridge.GameUtils;
import net.exmo.sixty_seconds.SixtySecondsPhase;
import net.exmo.sixty_seconds.component.FamilyPosition;
import net.exmo.sixty_seconds.component.SixtySecondsStatsComponent;
import net.exmo.sixty_seconds.arena.SixtySecondsSearchZones;
import net.exmo.sixty_seconds.state.SixtySecondsState;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.exmo.sixty_seconds.menu.SpecialInventoryMenu;

/**
 * 背包槽位限制（用屏障占位 + 弹出手动放入；仅 60s 模式相位机驱动，结束清除，不改共享代码）：
 * <ul>
 *   <li><b>准备阶段</b>：按家庭身份携带上限——父亲 8 格、其余 2 格（{@link FamilyPosition#carryLimit}），其余槽位屏障覆盖。</li>
 *   <li><b>游戏日</b>：可用两排（0–17），其余屏障覆盖。</li>
 *   <li><b>背包扩容</b>：玩家可使用扩容模块解锁额外槽位（最多 {@link #MAX_EXTRA_UNLOCK} 格），
 *       存储在 {@link SixtySecondsStatsComponent#extraUnlockedSlots}。</li>
 * </ul>
 */
public final class SixtySecondsInventoryLimit {
    /** 游戏日可用槽位数（快捷栏 + 主背包第一排）。 */
    public static final int DAY_ALLOWED_SLOTS = 18;
    /** 游戏日非父亲成员的可用槽位数（13 格）。 */
    public static final int NON_FATHER_DAY_SLOTS = 13;
    public static final int LAST_MAIN_SLOT = 35;
    /** 扩容模块最多可解锁的额外槽位数。 */
    public static final int MAX_EXTRA_UNLOCK = 27;

    private SixtySecondsInventoryLimit() {
    }

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % 10 != 0) {
            return;
        }
        SixtySecondsState.Data data = SixtySecondsState.get(level);
        // Outside an active round there is no 60 Seconds carry limit.  This
        // is also required for the explicit /60s inventory command: items
        // placed in its extra slots must survive closing the menu before the
        // round has started.
        if (data.phase == SixtySecondsPhase.INACTIVE
                || data.phase == SixtySecondsPhase.FINISHED) {
            for (ServerPlayer player : level.players()) {
                clearMainBarriers(player);
            }
            return;
        }
        boolean prep = data.phase == SixtySecondsPhase.PREPARATION;
        for (ServerPlayer player : level.players()) {
            if (GameUtils.isPlayerSpectatingOrCreative(player)) {
                continue;
            }
            // Once the player is back in the shelter during a real game day,
            // the Tarkov-style menu owns the full 27+27 inventory.  Do not
            // reapply the legacy barrier/carry-limit system after that menu
            // closes, otherwise items placed in the custom inventory appear
            // to have no backing slots and are dropped on the next tick.
            if (data.phase == SixtySecondsPhase.DAY
                    && SixtySecondsDailyEvents.isPlayerInShelter(player)) {
                clearMainBarriers(player);
                continue;
            }
            // The special menu exposes the complete base backpack.  The
            // legacy barrier system must not turn those 27 slots back into
            // fake locked slots while the menu is open.
            if (player.containerMenu instanceof SpecialInventoryMenu) {
                clearMainBarriers(player);
                continue;
            }
            int allowed;
            if (prep) {
                allowed = carryLimit(player);
            } else {
                // 游戏日：父亲 18 格，其他成员 13 格
                FamilyPosition position = SixtySecondsStatsComponent.KEY.get(player).familyPosition;
                allowed = (position == FamilyPosition.FATHER) ? DAY_ALLOWED_SLOTS : NON_FATHER_DAY_SLOTS;
            }
            // 加上已解锁的额外槽位（最多 18 格额外解锁，总计不超过 36 格）
            int extra = SixtySecondsExpansionStorage.capacity(
                    SixtySecondsStatsComponent.KEY.get(player).expansionModule);
            allowed = Math.min(allowed + extra, LAST_MAIN_SLOT + 1);
            enforce(player, allowed);
        }
    }

    private static int carryLimit(ServerPlayer player) {
        FamilyPosition position = SixtySecondsStatsComponent.KEY.get(player).familyPosition;
        return position == null ? FamilyPosition.MOTHER.carryLimit : position.carryLimit;
    }

    private static void enforce(ServerPlayer player, int firstRestrictedSlot) {
        Inventory inventory = player.getInventory();
        // 放行范围扩大时（准备 2/8 格 → 游戏日 18 格）清掉旧屏障，否则玩家仍只剩一排可用
        for (int slot = 0; slot < firstRestrictedSlot && slot <= LAST_MAIN_SLOT; slot++) {
            if (isBarrier(inventory.getItem(slot))) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        for (int slot = firstRestrictedSlot; slot <= LAST_MAIN_SLOT; slot++) {
            ItemStack current = inventory.getItem(slot);
            if (isBarrier(current)) {
                continue;
            }
            if (!current.isEmpty()) {
                ItemStack real = current.copy();
                inventory.setItem(slot, barrier()); // 先占位，避免 add 又落回受限槽
                inventory.add(real);
                if (!real.isEmpty()) {
                    player.drop(real, false);
                }
            } else {
                inventory.setItem(slot, barrier());
            }
        }
    }

    /** 结束/退出模式时清除屏障占位（只删屏障，不动真实物品）。 */
    public static void clear(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            Inventory inventory = player.getInventory();
            for (int slot = 0; slot <= LAST_MAIN_SLOT; slot++) {
                if (isBarrier(inventory.getItem(slot))) {
                    inventory.setItem(slot, ItemStack.EMPTY);
                }
            }
        }
    }

    /**
     * 屏障占位槽不可点击/快速移动（LimitedInventoryScreen 与任何含玩家背包槽位的容器界面统一生效）。
     * 由 {@code AbstractContainerMenuMixin.doClick} 在两端 HEAD 调用，返回 true 则取消本次点击
     * （QUICK_MOVE/THROW/SWAP 都从 doClick 进入，一并覆盖；数字键/副手键换入屏障槽也拦截）。
     */
    public static boolean shouldBlockClick(AbstractContainerMenu menu, int slotIndex, int button,
            ClickType clickType, Player player) {
        if (GameUtils.isPlayerSpectatingOrCreative(player)) {
            return false;
        }
        // The custom menu has its own 27-slot base backpack plus the separate
        // extra-inventory container.  Never apply vanilla barrier locking to
        // either side of that menu.
        if (menu instanceof SpecialInventoryMenu) {
            return false;
        }
        // During the first house-search phase allow shift-click/quick-move
        // from a supply container.  The old barrier slots still cannot be
        // used as a source, but a container item may be routed into the
        // currently available vanilla slots without requiring a manual drag.
        if (clickType == ClickType.QUICK_MOVE && player instanceof ServerPlayer serverPlayer) {
            SixtySecondsState.Data data = SixtySecondsState.get(serverPlayer.serverLevel());
            boolean houseSearch = data.phase == SixtySecondsPhase.PREPARATION
                    || SixtySecondsSearchZones.isInSearchZone(serverPlayer);
            if (houseSearch) {
                if (slotIndex >= 0 && slotIndex < menu.slots.size()) {
                    Slot source = menu.slots.get(slotIndex);
                    if (source.container == player.getInventory() && isBarrier(source.getItem())) {
                        return true;
                    }
                }
                return false;
            }
        }
        if (slotIndex >= 0 && slotIndex < menu.slots.size()) {
            Slot slot = menu.slots.get(slotIndex);
            if (slot.container == player.getInventory() && isBarrier(slot.getItem())) {
                return true;
            }
        }
        return clickType == ClickType.SWAP && button >= 0 && button < player.getInventory().getContainerSize()
                && isBarrier(player.getInventory().getItem(button));
    }

    /**
     * Whether the first house-search phase must use vanilla one-cell
     * inventory behavior.  PetiteInventory's server-side quick-move service
     * is independent of its client screen toggle, so this check is shared by
     * the PetiteInventory compatibility mixins as well.
     */
    public static boolean isPetiteInventoryDisabled(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        SixtySecondsState.Data data = SixtySecondsState.get(serverPlayer.serverLevel());
        return data.phase == SixtySecondsPhase.PREPARATION
                || SixtySecondsSearchZones.isInSearchZone(serverPlayer);
    }

    private static ItemStack barrier() {
        return new ItemStack(Items.BARRIER);
    }

    private static void clearMainBarriers(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot <= LAST_MAIN_SLOT; slot++) {
            if (isBarrier(inventory.getItem(slot))) {
                inventory.setItem(slot, ItemStack.EMPTY);
            }
        }
    }

    private static boolean isBarrier(ItemStack stack) {
        return stack.is(Items.BARRIER);
    }
}
