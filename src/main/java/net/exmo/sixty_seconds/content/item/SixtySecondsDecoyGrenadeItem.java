package net.exmo.sixty_seconds.content.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import java.util.List;

/**
 * 诱饵弹（独立的投掷物，区别于标记弹/闪光弹）：
 * <ul>
 *   <li>落地不造成任何伤害、不点燃、不致盲</li>
 *   <li>落地清空范围内怪物的仇恨并把它们吸引到爆点（伪装吸引）</li>
 *   <li>播放原版烟花爆炸声</li>
 * </ul>
 * 爆炸/吸引/音效逻辑全部复用 {@link SixtySecondsGrenadeItem#explode}（零伤分支）。
 */
public class SixtySecondsDecoyGrenadeItem extends SixtySecondsGrenadeItem {

    public SixtySecondsDecoyGrenadeItem(Item.Properties properties) {
        super(properties, 8.0D, 0.0F, 0, false, false);
    }

    @Override
    public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.sixty_seconds.sixty_seconds_decoy_flare"));
        tooltip.add(Component.translatable("tooltip.sixty_seconds.sixty_seconds_decoy_flare.2"));
    }
}
