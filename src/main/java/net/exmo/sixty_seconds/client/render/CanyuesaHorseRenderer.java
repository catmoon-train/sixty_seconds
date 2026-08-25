package net.exmo.sixty_seconds.client.render;

import net.minecraft.client.model.HorseModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.AbstractHorseRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.exmo.sixty_seconds.entity.CanyuesaHorseEntity;

public class CanyuesaHorseRenderer extends AbstractHorseRenderer<CanyuesaHorseEntity, HorseModel<CanyuesaHorseEntity>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("sixty_seconds", "textures/entity/canyuesa_horse.png");

    public CanyuesaHorseRenderer(EntityRendererProvider.Context pContext) {
        super(pContext, new HorseModel<>(pContext.bakeLayer(ModelLayers.HORSE)), 1.1F);
    }

    @Override
    public ResourceLocation getTextureLocation(CanyuesaHorseEntity entity) {
        return TEXTURE;
    }
}
