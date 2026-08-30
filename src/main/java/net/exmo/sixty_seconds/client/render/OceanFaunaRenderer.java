package net.exmo.sixty_seconds.client.render;

import net.exmo.sixty_seconds.client.model.OceanFaunaModel;
import net.exmo.sixty_seconds.entity.OceanFaunaEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 海洋生物群系渲染器（第二批次，10 变体）。
 *
 * <p>模型 {@link OceanFaunaModel} 内含 10 套独立几何（蝠鲼 / 水母 / 巨型乌贼 / 河豚 /
 * 海星 / 海马 / 狮子鱼 / 铁甲蟹 / 鹦鹉螺 / 梭鱼），由模型自身按变体切换可见性；
 * 贴图按变体切换（{@link OceanFaunaEntity#textureLocation()}）。
 */
public class OceanFaunaRenderer extends MobRenderer<OceanFaunaEntity, OceanFaunaModel> {

    public OceanFaunaRenderer(EntityRendererProvider.Context context) {
        super(context, new OceanFaunaModel(OceanFaunaModel.createLayer().bakeRoot()), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(OceanFaunaEntity entity) {
        return entity.textureLocation();
    }
}
