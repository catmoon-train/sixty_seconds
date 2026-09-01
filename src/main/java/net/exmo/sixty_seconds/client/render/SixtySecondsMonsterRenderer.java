package net.exmo.sixty_seconds.client.render;

import net.exmo.sixty_seconds.client.model.SixtySecondsMobModelV2;
import net.exmo.sixty_seconds.entity.SixtySecondsMonsterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/** Renderer for the 18 independent ordinary mob forms. */
public class SixtySecondsMonsterRenderer
        extends MobRenderer<SixtySecondsMonsterEntity, SixtySecondsMobModelV2> {

    public SixtySecondsMonsterRenderer(EntityRendererProvider.Context context) {
        super(context, new SixtySecondsMobModelV2(SixtySecondsMobModelV2.createLayer().bakeRoot()), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(SixtySecondsMonsterEntity entity) {
        return entity.textureLocation();
    }

    @Override
    protected boolean shouldShowName(SixtySecondsMonsterEntity entity) {
        return false;
    }
}
