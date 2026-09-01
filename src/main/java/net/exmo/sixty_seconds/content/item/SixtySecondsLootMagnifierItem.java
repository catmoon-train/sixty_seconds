package net.exmo.sixty_seconds.content.item;

import net.exmo.sixty_seconds.SixtySecondsBalance;
import net.exmo.sixty_seconds.registry.ModItems;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * 物资箱搜刮（容器形式）里的「放大镜」占位物：
 * <ul>
 *   <li>不能被以任何方式取下 / 丢弃 / 移动到背包，只能被左键点击触发搜刮。</li>
 *   <li>其内部 {@link DataComponents#CUSTOM_DATA} 保存了真实战利品与搜刮所需 tick 数，
 *       左键读条完成后由服务端替换为对应战利品。</li>
 * </ul>
 */
public class SixtySecondsLootMagnifierItem extends Item {
    public SixtySecondsLootMagnifierItem(Item.Properties props) {
        super(props.stacksTo(1));
    }

    @Override
    public Component getName(ItemStack stack) {
        // 放大镜本身不暴露真实战利品名称
        return Component.translatable("item.sixty_seconds.loot_magnifier.prompt");
    }

    /** 放大镜永远不能掉落到世界。 */
    @Override
    public boolean onDroppedByPlayer(ItemStack stack, Player player) {
        return false;
    }

    public static boolean isMagnifier(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof SixtySecondsLootMagnifierItem;
    }

    /** 构造一个放大镜占位物，内部保存真实战利品与搜刮 tick 数。 */
    public static ItemStack create(ServerLevel level, ItemStack loot, int searchTicks) {
        ItemStack mag = new ItemStack(ModItems.LOOT_MAGNIFIER);
        CompoundTag root = new CompoundTag();
        root.put("Loot", loot.save(level.registryAccess()));
        root.putInt("SearchTicks", searchTicks);
        mag.set(DataComponents.CUSTOM_DATA, CustomData.of(root));
        return mag;
    }

    /** 取回放大镜内藏的真实战利品。 */
    public static ItemStack getLoot(ServerLevel level, ItemStack mag) {
        CustomData cd = mag.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            return ItemStack.EMPTY;
        }
        CompoundTag root = cd.copyTag();
        if (root.contains("Loot")) {
            return ItemStack.parse(level.registryAccess(), root.getCompound("Loot")).orElse(ItemStack.EMPTY);
        }
        return ItemStack.EMPTY;
    }

    /** 取回搜刮所需 tick 数（受「搜刮物资箱的速度」影响后的值）。 */
    public static int getSearchTicks(ItemStack mag) {
        CustomData cd = mag.get(DataComponents.CUSTOM_DATA);
        if (cd == null) {
            return SixtySecondsBalance.SUPPLY_SEARCH_BASE_TICKS;
        }
        int t = cd.copyTag().getInt("SearchTicks");
        return t <= 0 ? SixtySecondsBalance.SUPPLY_SEARCH_BASE_TICKS : t;
    }
}
