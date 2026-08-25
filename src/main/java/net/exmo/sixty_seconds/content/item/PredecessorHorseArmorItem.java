package net.exmo.sixty_seconds.content.item;

import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.AnimalArmorItem;

/**
 * 前人留下的马铠（马铠物品，{@code AnimalArmorItem.BodyType.EQUESTRIAN}）。
 * 装备到 {@code SuperPigHorseEntity} / {@code RainbowHorseEntity} / {@code CanyuesaHorseEntity}
 * 上时，对应马实体会读取本物品的 {@code healthBonus} / {@code speedMultiplier} 字段并套用加成。
 * 由 {@code HorseArmorLayer} 跳过渲染护甲层（避免紫块），功能加成仍由马实体读取生效。
 */
public class PredecessorHorseArmorItem extends AnimalArmorItem {
    public float speedMultiplier = 0.12F;
    public int healthBonus = 12;

    public PredecessorHorseArmorItem(Item.Properties pProperties) {
        super(ArmorMaterials.DIAMOND, AnimalArmorItem.BodyType.EQUESTRIAN, false, pProperties);
    }
}
