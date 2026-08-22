package net.exmo.sixty_seconds.content.entity;

import net.exmo.sixty_seconds.SixtySeconds;
import net.exmo.sixty_seconds.content.entity.WheelchairEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;

public class WheelchairEntityRenderer extends LivingEntityRenderer<WheelchairEntity, WheelchairEntityModel> {
    private static final ResourceLocation TEXTURE = SixtySeconds.id("textures/entity/wheelchair.png");

    public WheelchairEntityRenderer(EntityRendererProvider.Context context) {
        super(context, new WheelchairEntityModel(context.bakeLayer(WheelchairEntityModel.LAYER_LOCATION)), 0.5f);
    }

    @Override
    public ResourceLocation getTextureLocation(WheelchairEntity entity) {
        return TEXTURE;
    }
}