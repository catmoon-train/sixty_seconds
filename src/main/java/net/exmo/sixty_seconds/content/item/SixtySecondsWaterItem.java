package net.exmo.sixty_seconds.content.item;

import net.exmo.sixty_seconds.SixtySecondsMod;
import net.exmo.sixty_seconds.bridge.stubs.CocktailItem;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/**
 * 末日60秒模式的水物品（小/中/高三级）。继承 {@code CocktailItem} 获得饮用动画/音效；
 * 饮用后由 {@code SixtySecondsConsumeMixin → SixtySecondsConsumables.onConsume} 按 {@link #thirstRestore} 恢复口渴值。
 */
public class SixtySecondsWaterItem extends CocktailItem {
    /** 等级：small / medium / high。 */
    public final String tier;
    /** 恢复的口渴值。 */
    public final int thirstRestore;

    public SixtySecondsWaterItem(net.minecraft.world.item.Item.Properties properties, String tier, int thirstRestore) {
        super(properties);
        this.tier = tier;
        this.thirstRestore = thirstRestore;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemUtils.startUsingInstantly(level, player, hand);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 32;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level world, LivingEntity user) {
        super.finishUsingItem(stack, world, user);
        // CocktailItem 不消耗、水又无 FOOD 组件（原版也不消耗），须在此扣减，否则可无限饮用
        stack.consume(1, user);
        return stack;
    }
}
