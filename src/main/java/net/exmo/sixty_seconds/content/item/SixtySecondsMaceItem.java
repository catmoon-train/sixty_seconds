package net.exmo.sixty_seconds.content.item;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.enchantment.EnchantmentHelper;

/**
 * 60s 重锤，继承原版 MaceItem。
 * 基础伤害/攻速设为原版重锤数值（5.0 / -3.4），不分级；
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
        // 落空猛击：从高处下落且未落地、未滑翔、未潜行时触发
        if (attacker.fallDistance > 0.0F && !attacker.onGround() && !attacker.isFallFlying() && !attacker.isShiftKeyDown()) {
            // 重击伤害 = 原版基准 3 + 落距*档位倍率 + 攻击力（钢制倍率=1.0 与原版一致）
            float attackDamage = (float) attacker.getAttributeValue(Attributes.ATTACK_DAMAGE);
            float smash = 3.0F + attacker.fallDistance * smashDamageScale + attackDamage;
            target.hurt(attacker.damageSources().mobAttack(attacker), smash);
            if (attacker.level() instanceof ServerLevel serverLevel) {
                EnchantmentHelper.doPostAttackEffects(serverLevel, target, attacker.damageSources().mobAttack(attacker));
            }
            // 原版砸地音效
            attacker.level().playSound(null, attacker.getX(), attacker.getY(), attacker.getZ(),
                    SoundEvents.MACE_SMASH_GROUND, attacker.getSoundSource(), 1.0F, 1.0F);
            // 击退，按档位倍率缩放（原版基准 0.6 * 伤害）
            target.knockback((double) (0.6F * smash * knockbackScale),
                    (double) Mth.sin(attacker.getYRot() * (float) (Math.PI / 180.0)),
                    (double) (-Mth.cos(attacker.getYRot() * (float) (Math.PI / 180.0))));
            // 地面砸击：目标水平动量减半，呈现"砸住"手感
            target.setDeltaMovement(target.getDeltaMovement().multiply(0.5, 0.5, 0.5));
            return true;
        }
        // 非猛击：交还原版逻辑（耐久由原版攻击链的 postHurtEnemy 统一处理，
        // 物品已通过 durability 组件设为可损耗，避免与显式 hurtAndBreak 重复扣减）
        return super.hurtEnemy(stack, target, attacker);
    }
}
