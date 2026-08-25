package net.exmo.sixty_seconds.content.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;

/**
 * 60s 重锤，继承原版 MaceItem。
 * 基础伤害/攻速由 MaceItem 锁定为原版重锤数值（无法分档）；
 * 可分级的维度：耐久（getMaxDamage）、猛击落距加伤、击退、盾牌击落概率。
 * 钢制参数与原版一致，铁制更低、合金更高。
 */
public class SixtySecondsMaceItem extends MaceItem {
    private final int maxDamage;
    /** 落距加伤倍率：钢制=1.0 与原版一致，铁制更低、合金更高 */
    private final float smashDamageScale;
    /** 击退倍率（原版基准 0.6） */
    private final float knockbackScale;
    /** 命中击落盾牌概率（原版 0.3） */
    private final float shieldChance;

    public SixtySecondsMaceItem(int maxDamage, float smashDamageScale, float knockbackScale,
                                float shieldChance, Item.Properties properties) {
        super(properties);
        this.maxDamage = maxDamage;
        this.smashDamageScale = smashDamageScale;
        this.knockbackScale = knockbackScale;
        this.shieldChance = shieldChance;
    }

    @Override
    public int getMaxDamage(ItemStack stack) {
        return maxDamage;
    }

    @Override
    public boolean canDisableShield(ItemStack stack, ItemStack shield, LivingEntity entity, LivingEntity attacker) {
        return entity.level().getRandom().nextFloat() < shieldChance;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (attacker.fallDistance > 0.0F && !attacker.onGround() && !attacker.isFallFlying() && !attacker.isShiftKeyDown()) {
            int fall = (int) attacker.fallDistance;
            // 落距加伤（按档位倍率，钢制=原版基准）
            target.hurt(attacker.damageSources().mobAttack(attacker), attacker.fallDistance * smashDamageScale);
            // 击退，沿用原版公式并乘档位倍率
            float f = attacker.fallDistance > 5.0F ? 1.0F : 0.5F;
            target.knockback((double) (0.6F * (float) fall * f * knockbackScale), attacker);
        }
        // 手动损耗耐久（不调 super.hurtEnemy，避免原版 smash 二次加伤）
        stack.hurtAndBreak(1, attacker, EquipmentSlot.MAINHAND);
        return true;
    }
}
