package net.exmo.sixty_seconds.client.render;

import net.exmo.sixty_seconds.client.model.OceanFloorMonsterModel;
import net.exmo.sixty_seconds.entity.OceanFloorMonsterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 海底小型怪物渲染器。
 *
 * 模型 {@link OceanFloorMonsterModel} 内含 6 套独立几何（寄居蟹 / 海胆 / 鳗鱼 / 怨灵 / 潜伏者 / 守护者），
 * 由模型自身按变体切换可见性；贴图按变体切换（{@link OceanFloorMonsterEntity#textureLocation()}）。
 */
public class OceanFloorMonsterRenderer extends MobRenderer<OceanFloorMonsterEntity, OceanFloorMonsterModel> {

    public OceanFloorMonsterRenderer(EntityRendererProvider.Context context) {
        super(context, new OceanFloorMonsterModel(OceanFloorMonsterModel.createLayer().bakeRoot()), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(OceanFloorMonsterEntity entity) {
        return entity.textureLocation();
    }
}
