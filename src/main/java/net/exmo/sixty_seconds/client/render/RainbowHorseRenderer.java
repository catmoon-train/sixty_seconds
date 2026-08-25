package net.exmo.sixty_seconds.client.render;

import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.exmo.sixty_seconds.entity.RainbowHorseEntity;

public class RainbowHorseRenderer extends AbstractHorseRenderer<RainbowHorseEntity, HorseModel<RainbowHorseEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("sixty_seconds", "textures/entity/rainbow_horse.png");

    public RainbowHorseRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HorseModel<>(pContext.bakeLayer(ModelLayers.HORSE)), 1.1F);
    }

    @Override
    public ResourceLocation getTextureLocation(RainbowHorseEntity entity) {
        return TEXTURE;
    }
}
