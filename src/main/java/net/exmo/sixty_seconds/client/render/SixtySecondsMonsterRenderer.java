package net.exmo.sixty_seconds.client.render;

import net.exmo.sixty_seconds.client.model.SixtySecondsCreatureModel;
import net.exmo.sixty_seconds.entity.SixtySecondsMonsterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 末日60秒自研小怪渲染器（重构后）。
 *
 * <p>模型 {@link SixtySecondsCreatureModel} 内含 16 组几何：
 * <ul>
 *   <li>1 组僵尸人形，供 3 个最基础小怪（SHAMBLER / RUNNER / BRUTE）共用；</li>
 *   <li>15 组完全独立的几何，对应 SPITTER / STALKER / HOWLER / BLOATER /
 *       JUGGERNAUT / CINDERLING / FROSTLING / HUSKBRUTE / RAVENOR / WAILER /
 *       BURSTER / GOREHOUND / SHADOWMUTE / BONELORD / SPINEWALKER。</li>
 * </ul>
 * 由模型自身按实体变体切换可见性，贴图按变体切换
 * （{@link SixtySecondsMonsterEntity#textureLocation()}）。
 */
public class SixtySecondsMonsterRenderer
        extends MobRenderer<SixtySecondsMonsterEntity, SixtySecondsCreatureModel> {

    public SixtySecondsMonsterRenderer(EntityRendererProvider.Context context) {
        super(context, new SixtySecondsCreatureModel(SixtySecondsCreatureModel.createLayer().bakeRoot()), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(SixtySecondsMonsterEntity entity) {
        return entity.textureLocation();
    }

    /**
     * 小怪即使被准星选中也不显示名称牌。
     * 原版 EntityRenderer 会在准星指向拥有自定义名称的实体时绕过实体的 shouldShowName()，
     * 因此仅在实体上调用 setCustomNameVisible(false) 仍然无法屏蔽鼠标指向时的名称。
     */
    @Override
    protected boolean shouldShowName(SixtySecondsMonsterEntity entity) {
        return false;
    }
}
