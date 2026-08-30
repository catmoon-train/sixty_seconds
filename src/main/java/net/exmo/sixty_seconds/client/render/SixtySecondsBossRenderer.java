package net.exmo.sixty_seconds.client.render;

import net.exmo.sixty_seconds.client.model.SixtySecondsBossModel;
import net.exmo.sixty_seconds.entity.SixtySecondsBossEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 末日60秒 Boss 渲染器（重构后）。
 *
 * <p>模型 {@link SixtySecondsBossModel} 内含 10 组几何：
 * {@code RAVAGER} 使用僵尸人形组（唯一保留僵尸外观的 Boss），
 * 其余 9 个 Boss（colossus / necromancer / plaguebearer / specter / inferno /
 * frostbite / swarmkeeper / stormherald / voidweaver）均为完全独立建模。
 *
 * <p>不再叠加 {@code SixtySecondsTraitsLayer}：特征几何已由独立建模直接体现。
 */
public class SixtySecondsBossRenderer
        extends MobRenderer<SixtySecondsBossEntity, SixtySecondsBossModel> {

    public SixtySecondsBossRenderer(EntityRendererProvider.Context context) {
        super(context, new SixtySecondsBossModel(SixtySecondsBossModel.createLayer().bakeRoot()), 0.7F);
    }

    @Override
    public ResourceLocation getTextureLocation(SixtySecondsBossEntity entity) {
        return entity.textureLocation();
    }
}
