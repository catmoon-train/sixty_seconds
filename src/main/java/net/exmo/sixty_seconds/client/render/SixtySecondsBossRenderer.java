package net.exmo.sixty_seconds.client.render;

import net.exmo.sixty_seconds.entity.SixtySecondsBossEntity;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 末日60秒 尸潮领主（Boss）渲染器。
 *
 * <p>模型严格沿用原版僵尸 {@link ZombieModel}（基于 {@link ModelLayers#ZOMBIE} 烘焙）；
 * 贴图沿用 Boss 自有纹理 {@link SixtySecondsBossEntity#textureLocation()}
 * （sixty_seconds_boss_*.png 等变体图，非原版僵尸贴图）。
 * <p>
 * 仅该 Boss（{@code SIXTY_SECONDS_BOSS}）使用原版僵尸模型，不影响其它 Boss
 * （海洋 Boss 等仍各自使用独立模型与渲染器）。
 */
public class SixtySecondsBossRenderer
        extends MobRenderer<SixtySecondsBossEntity, ZombieModel<SixtySecondsBossEntity>> {

    public SixtySecondsBossRenderer(EntityRendererProvider.Context context) {
        super(context, new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE)), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(SixtySecondsBossEntity entity) {
        // 仅模型用原版僵尸，贴图保持 Boss 自有纹理
        return entity.textureLocation();
    }
}
