package net.exmo.sixty_seconds.client.render;

import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.exmo.sixty_seconds.entity.SuperPigHorseEntity;

public class SuperPigHorseRenderer extends AbstractHorseRenderer<SuperPigHorseEntity, HorseModel<SuperPigHorseEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("sixty_seconds", "textures/entity/super_pig_horse.png");

    public SuperPigHorseRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HorseModel<>(pContext.bakeLayer(ModelLayers.HORSE)), 1.3F);
    }

    @Override
    public ResourceLocation getTextureLocation(SuperPigHorseEntity entity) {
        return TEXTURE;
    }
}
