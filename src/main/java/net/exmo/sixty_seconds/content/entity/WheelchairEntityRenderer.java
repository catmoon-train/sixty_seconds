package net.exmo.sixty_seconds.content.entity;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.content.entity.WheelchairEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/** 客户端实体渲染器：仅客户端存在，专用服务端不会加载。 */
@OnlyIn(Dist.CLIENT)
public class WheelchairEntityRenderer extends LivingEntityRenderer<WheelchairEntity, WheelchairEntityModel> {
    private static final ResourceLocation TEXTURE = SixtySeconds.id("textures/entity/wheelchair.png");

    public WheelchairEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new WheelchairEntityModel(context.bakeLayer(WheelchairEntityModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(WheelchairEntity entity) {
        return TEXTURE;
    }

    @Override
    protected boolean shouldShowName(WheelchairEntity entity) {
        return false;
    }
}
