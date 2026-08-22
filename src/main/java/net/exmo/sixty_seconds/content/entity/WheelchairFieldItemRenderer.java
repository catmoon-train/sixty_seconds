package net.exmo.sixty_seconds.content.entity;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemEntityRenderer;

/**
 * 场地道具实体 {@link WheelchairFieldItemEntity} 的渲染器。
 * 该实体继承 ItemEntity 并通过 setItem 携带展示用物品（糖/铁锭/羽毛），
 * 直接复用原版 ItemEntityRenderer（基于 ItemEntity、非泛型）即可正确渲染对应道具。
 */
public class WheelchairFieldItemRenderer extends ItemEntityRenderer {
    public WheelchairFieldItemRenderer(EntityRendererProvider.Context context) {
        super(context);
    }
}
