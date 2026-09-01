package net.exmo.sixty_seconds.client.render;

import net.exmo.sixty_seconds.client.model.SixtySecondsBossModel;
import net.exmo.sixty_seconds.entity.SixtySecondsBossEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.ZombieModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.PoseStack;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * 末日60秒 Boss 渲染器。
 *
 * <p><b>仅「尸潮领主」(BossVariant.RAVAGER) 严格沿用原版僵尸模型 {@link ZombieModel}</b>
 * （基于 {@link ModelLayers#ZOMBIE} 烘焙），贴图仍用其自有纹理
 * {@link SixtySecondsBossEntity#textureLocation()}（sixty_seconds_boss_ravager.png）。
 * <p>其余 Boss 变体（巨像/亡灵术士/疫病者/鬼魅/熔渊暴君/霜噬守望/虫潮之主/雷霆传令/虚空织者）
 * 一律沿用自定义建模 {@link SixtySecondsBossModel}，互不影响。
 * <p>模型按实体制变体在 {@link #getModel()} 中切换；渲染器本身仍只注册给
 * {@code SIXTY_SECONDS_BOSS} 一种实体，但通过变体区分保证只动了尸潮领主一个。
 */
public class SixtySecondsBossRenderer
        extends MobRenderer<SixtySecondsBossEntity, EntityModel<SixtySecondsBossEntity>> {

    /** 自定义建模（其它所有 Boss 变体使用）。 */
    private final SixtySecondsBossModel customModel;
    /** 原版僵尸模型（仅尸潮领主 RAVAGER 使用）。 */
    private final ZombieModel<SixtySecondsBossEntity> zombieModel;
    /** 当前正在渲染的实体，供 {@link #getModel()} 按变体挑选模型。 */
    private SixtySecondsBossEntity current;

    public SixtySecondsBossRenderer(EntityRendererProvider.Context context) {
        super(context, new SixtySecondsBossModel(SixtySecondsBossModel.createLayer().bakeRoot()), 0.7F);
        this.customModel = (SixtySecondsBossModel) super.getModel(); // 取构造时传入的自定义模型
        this.zombieModel = new ZombieModel<>(context.bakeLayer(ModelLayers.ZOMBIE));
    }

    @Override
    public void render(SixtySecondsBossEntity entity, float yaw, float partialTicks,
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        this.current = entity;
        super.render(entity, yaw, partialTicks, poseStack, bufferSource, packedLight);
    }

    @Override
    public EntityModel<SixtySecondsBossEntity> getModel() {
        // 仅尸潮领主（RAVAGER）切换到原版僵尸模型，其余变体保持自定义建模
        if (current != null && current.getBossVariant() == SixtySecondsBossEntity.BossVariant.RAVAGER) {
            return zombieModel;
        }
        return customModel;
    }

    @Override
    public ResourceLocation getTextureLocation(SixtySecondsBossEntity entity) {
        // 贴图沿用各 Boss 变体自有纹理（尸潮领主用 sixty_seconds_boss_ravager.png，非原版僵尸图）
        return entity.textureLocation();
    }
}
