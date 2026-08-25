package net.exmo.sixty_seconds.content.item;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.Tier;

/**
 * 60s 重锤（继承原版 MaceItem）。不同等级通过构造参数区分基础伤害与攻速。
 */
public class SixtySecondsMaceItem extends MaceItem {
    public SixtySecondsMaceItem(Tier tier, float attackDamage, float attackSpeed, Properties properties) {
        super(tier, attackDamage, attackSpeed, properties);
    }
}
