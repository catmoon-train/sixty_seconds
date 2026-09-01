package net.exmo.sixty_seconds.client.render;

import net.exmo.sixty_seconds.client.model.OceanTitanModel;
import net.exmo.sixty_seconds.entity.OceanTitanEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 海洋霸主渲染器（10 个独立建模 Boss，非僵尸人形）。
 * 模型按变体切换可见性，贴图按变体切换。
 */
public class OceanTitanRenderer extends MobRenderer<OceanTitanEntity, OceanTitanModel> {

    public OceanTitanRenderer(EntityRendererProvider.Context context) {
        super(context, new OceanTitanModel(OceanTitanModel.createLayer().bakeRoot()), 0.9F);
    }

    @Override
    public ResourceLocation getTextureLocation(OceanTitanEntity entity) {
        return entity.textureLocation();
    }

    @Override
    protected boolean shouldShowName(OceanTitanEntity entity) {
        return false;
    }
}
